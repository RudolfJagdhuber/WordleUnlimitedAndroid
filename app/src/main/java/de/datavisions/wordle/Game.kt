package de.datavisions.wordle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.google.android.material.button.MaterialButton
import de.datavisions.wordle.api.JsonObjectResponse
import de.datavisions.wordle.api.guess
import de.datavisions.wordle.api.newGame
import de.datavisions.wordle.helpers.DataModels
import de.datavisions.wordle.helpers.formatGuessList
import org.json.JSONObject


fun highlight(tv: TextView?, lc: DataModels.LetterCorrectness, ctx: Context) {
    tv?.text = lc.letter
    tv?.setTextColor(ContextCompat.getColor(ctx, R.color.bgWhite))
    if (lc.correctPosition) tv?.setBackgroundResource(R.drawable.box_green)
    else if (lc.differentPosition) tv?.setBackgroundResource(R.drawable.box_yellow)
    else tv?.setBackgroundResource(R.drawable.box_gray)
}

fun highlightKey(tv: TextView?, lc: DataModels.LetterCorrectness, ctx: Context) {
    tv?.setTextColor(ContextCompat.getColor(ctx, R.color.bgWhite))
    if (lc.correctPosition) tv?.setBackgroundResource(R.drawable.key_green)
    else if (lc.differentPosition) tv?.setBackgroundResource(R.drawable.key_yellow)
    else tv?.setBackgroundResource(R.drawable.key_gray)
}

fun highlightActive(tv: TextView, letter: String, activeBox: Int, boxNumber: Int, ctx: Context) {
    tv.text = letter
    tv.setTextColor(ContextCompat.getColor(ctx, R.color.fgBlack))
    if (activeBox == boxNumber) tv.setBackgroundResource(R.drawable.box_blue)
    else tv.setBackgroundResource(R.drawable.box_white)
}

fun unhighlight(tv: TextView, ctx: Context) {
    tv.text = ""
    tv.setTextColor(ContextCompat.getColor(ctx, R.color.fgBlack))
    tv.setBackgroundResource(R.drawable.box_white)
}


class Game : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var endscreenLay: ConstraintLayout
    lateinit var keyboardLay: ConstraintLayout
    lateinit var contentLay: ConstraintLayout
    private lateinit var overlayMessage: TextView
    private lateinit var loadingLay: FrameLayout
    private val keyboard = HashMap<String, TextView>()
    lateinit var sendBtn: MaterialButton

    lateinit var game: DataModels.GameData
    lateinit var wordSet: Set<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        findViewById<MaterialButton>(R.id.back).setOnClickListener { finish() }

        sendBtn = findViewById(R.id.send)
        contentLay = findViewById(R.id.content)
        endscreenLay = findViewById(R.id.end_layout)
        keyboardLay = findViewById(R.id.keyboard)
        overlayMessage = findViewById(R.id.overlay_message)
        loadingLay = findViewById(R.id.loading_layout)
        recyclerView = findViewById(R.id.recyclerView)

        loadingLay.visibility = View.GONE
        overlayMessage.visibility = View.GONE
        endscreenLay.visibility = View.GONE

        // Set a global OnClickListener to the Keyboard Inputs
        for (c in "qwertzuiopasdfghjklyxcvbnmüäö") {
            keyboard[c.toString()] = findViewById(resources.getIdentifier(c.toString(),
                "id", packageName))
            keyboard[c.toString()]?.setOnClickListener { insertLetter(c.uppercase()) }
        }
        keyboard["back"] = findViewById(R.id.backspace)
        keyboard["back"]?.setOnClickListener {
            if (game.activeBox == 0) return@setOnClickListener
            game.activeGuess = game.activeGuess.replaceRange(game.activeBox - 1,
                game.activeBox, " ")
            game.activeBox--
            recyclerView.itemAnimator?.changeDuration = 0
            recyclerView.adapter?.notifyItemChanged(game.guesses.size)
            sendBtn.isEnabled = false
        }
        sendBtn.setOnClickListener {
            // Check if proposed word is in word-list
            if (!wordSet.contains(game.activeGuess)) {
                messagePopup(getString(R.string.not_found))
                return@setOnClickListener
            }
            loadingLay.visibility = View.VISIBLE
            sendBtn.isEnabled = false
            guess(this, game.id, game.activeGuess, object: JsonObjectResponse {
                override fun onSuccess(res: JSONObject, ctx: Context) {
                    loadingLay.visibility = View.GONE
                    game.guesses = formatGuessList(res)
                    game.activeGuess = " ".repeat(game.length)
                    game.activeBox = 0
                    game.solved = res.getInt("solved")
                    updateKeyboard(game.guesses)
                    recyclerView.itemAnimator?.changeDuration = 1000
                    recyclerView.adapter?.notifyItemRangeChanged(
                        game.guesses.size - 1,
                        if (game.solved == 0) 2 else 1
                    )
                    if (game.solved != 0) {
                        findViewById<TextView>(R.id.textView).text =
                            getString(R.string.word_id, game.wordId)
                        findViewById<TextView>(R.id.solution).text = res.getString("word")
                        findViewById<MaterialButton>(R.id.info).setOnClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(
                                R.string.explanation_link, res.getString("word").lowercase()))))
                        }
                        if (game.solved > 0) {  // Win
                            findViewById<TextView>(R.id.textView)
                                .setBackgroundResource(R.drawable.medal_green)
                            findViewById<TextView>(R.id.textView)
                                .setTextColor(getColor(R.color.fgGreen))
                            findViewById<TextView>(R.id.message).text = getString(R.string.win)
                            findViewById<TextView>(R.id.stats).text =
                                getString(R.string.moves_of_long, game.solved, game.tries)
                        } else {  // Lose
                            findViewById<TextView>(R.id.textView)
                                .setBackgroundResource(R.drawable.medal_gray)
                            findViewById<TextView>(R.id.textView)
                                .setTextColor(getColor(R.color.fgGray1))
                            findViewById<TextView>(R.id.message).text = getString(R.string.lose)
                            findViewById<TextView>(R.id.stats).text = "---"

                        }
                        keyboardLay.visibility = View.GONE
                        endscreenLay.visibility = View.VISIBLE
                    }
                }
                override fun onError(err: VolleyError) {
                    loadingLay.visibility = View.GONE
                    sendBtn.isEnabled = true
                    messagePopup(getString(R.string.no_internet))
                }
            })

        }

        findViewById<MaterialButton>(R.id.newgame).setOnClickListener {
            loadingLay.visibility = View.VISIBLE
            newGame(this, game.player, -1, 5, 6,
                object : JsonObjectResponse {
                    override fun onSuccess(res: JSONObject, ctx: Context) {
                        loadingLay.visibility = View.GONE
                        val newGame = DataModels.GameData(
                            res.getString("id"),
                            res.getString("player"),
                            res.getInt("word_id"),
                            if (res.isNull("word")) "" else res.getString("word"),
                            res.getInt("length"),
                            res.getInt("tries"),
                            formatGuessList(res),
                            res.getInt("solved"),
                            " ".repeat(res.getInt("length")),
                            0
                        )
                        contentLay.animate().apply {
                            alpha(0f)
                            duration = 300
                            startDelay = 0
                            withEndAction {
                                setupGame(newGame)
                                contentLay.animate().apply {
                                    alpha(1f)
                                    startDelay = 200
                                    duration = 700
                                }
                            }
                        }
                    }
                    override fun onError(err: VolleyError) {
                        loadingLay.visibility = View.GONE
                        messagePopup(getString(R.string.no_internet))
                    }
                })
        }
        // Read all 5 letter words from assets file and load as Set
        wordSet  = application.assets.open("len5.txt").bufferedReader().readLine()
            .split(",").toSet()
        val newGame = intent.getSerializableExtra("game_data") as DataModels.GameData
        setupGame(newGame)
    }


    private fun setupGame(newGame: DataModels.GameData) {
        game = newGame
        sendBtn.isEnabled = game.activeGuess.length == game.length
        if (game.solved != 0) {
            endscreenLay.visibility = View.VISIBLE
            keyboardLay.visibility = View.GONE
        } else {
            endscreenLay.visibility = View.GONE
            keyboardLay.visibility = View.VISIBLE
        }
        for (key in "qwertzuiopasdfghjklyxcvbnmüäö") {
            keyboard[key.toString()]?.setBackgroundResource(R.drawable.key_white)
            keyboard[key.toString()]?.setTextColor(getColor(R.color.fgBlack))
        }
        recyclerView.adapter = RowAdapter(game, this)
        recyclerView.itemAnimator?.changeDuration = 200
    }


    private fun messagePopup(message: String) {
        val buzzer = getSystemService<Vibrator>()
        val pattern = longArrayOf(0, 200, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buzzer?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            buzzer?.vibrate(200)
        }
        overlayMessage.visibility = View.VISIBLE
        overlayMessage.text = message
        overlayMessage.alpha = 0f
        overlayMessage.animate().apply {
            alpha(1f)
            duration = 200
            startDelay = 0
            withEndAction {
                overlayMessage.animate().apply {
                    alpha(0f)
                    startDelay = 500
                    duration = 500
                    withEndAction {
                        overlayMessage.visibility = View.GONE
                    }
                }
            }
        }
    }


    private fun insertLetter(letter: String) {
        if (game.activeBox < game.length) {
            game.activeGuess = game.activeGuess.replaceRange(game.activeBox,
                game.activeBox + 1, letter)
            game.activeBox++
            recyclerView.itemAnimator?.changeDuration = 0
            recyclerView.adapter?.notifyItemChanged(game.guesses.size)
            sendBtn.isEnabled = game.activeGuess.replace(" ", "").length == game.length
        }
    }


    private fun updateKeyboard(guesses: List<DataModels.WordCorrectness>) {
        for (wc in guesses) {
            for (lcr in wc.word) {
                highlightKey(keyboard[lcr.letter.lowercase()], lcr, this)
            }
        }
    }


    class RowAdapter(private val game: DataModels.GameData, private val ctx: Activity,
                     private val n_elements: Int = game.tries) :
        RecyclerView.Adapter<RowAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val boxes: List<TextView> = listOf<TextView>(
                view.findViewById(R.id.letter1),
                view.findViewById(R.id.letter2),
                view.findViewById(R.id.letter3),
                view.findViewById(R.id.letter4),
                view.findViewById(R.id.letter5)
            )
        }


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            return ViewHolder(LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.row5, viewGroup, false))
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(vh: ViewHolder, position: Int) {



            if (position < game.guesses.size) { // Already answered rows
                vh.boxes.forEachIndexed { i, box ->
                    box.setOnClickListener(null)
                    highlight(box, game.guesses[position].word[i], ctx)
                }
                return
            }

            if (position == game.guesses.size) { // Current active row
                vh.boxes.forEachIndexed { i, box ->
                    box.setOnClickListener {
                        game.activeBox = i
                        notifyItemChanged(game.guesses.size)
                    }
                    highlightActive(box, game.activeGuess[i].toString(), game.activeBox, i, ctx)
                }
                return
            }

            if (position > game.guesses.size) { // Unanswered, inactive rows
                vh.boxes.forEach { box -> unhighlight(box, ctx) }
                return
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = n_elements
    }
}