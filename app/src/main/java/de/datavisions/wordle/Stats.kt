package de.datavisions.wordle

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import de.datavisions.wordle.api.JsonArrayResponse
import de.datavisions.wordle.api.gameList
import de.datavisions.wordle.helpers.DataModels
import de.datavisions.wordle.helpers.extractStats
import de.datavisions.wordle.helpers.formatGuessList
import org.json.JSONArray
import kotlin.math.round

class Stats : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingLay: FrameLayout
    lateinit var noInternetLay: LinearLayout
    lateinit var gameList: MutableList<DataModels.GameData>
    lateinit var guessStats: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        val userId: String = intent.getStringExtra("user_id")!!
        val userName: String = intent.getStringExtra("user_name")!!
        loadingLay = findViewById(R.id.loading_layout)
        noInternetLay =  findViewById(R.id.no_internet_layout)
        recyclerView = findViewById(R.id.recyclerView)
        findViewById<TextView>(R.id.toolbar_title).text = getString(R.string.stats_of, userName)
        findViewById<MaterialButton>(R.id.back).setOnClickListener { finish() }
        // Only accessible on error
        findViewById<MaterialButton>(R.id.retry).setOnClickListener {
            noInternetLay.visibility = View.GONE
            loadStats(userId)
        }

        loadStats(userId)
    }


    private fun loadStats(userId: String) {
        loadingLay.visibility = View.VISIBLE
        gameList(this, userId, object : JsonArrayResponse {
            override fun onSuccess(res: JSONArray, ctx: Context) {
                loadingLay.visibility = View.GONE
                gameList = mutableListOf()
                for (i in 0 until res.length()) {
                    val game = res.getJSONObject(i)
                    if (game.getInt("solved") == -1) continue
                    gameList.add(DataModels.GameData(
                        game.getString("id"),
                        game.getString("player"),
                        game.getInt("word_id"),
                        game.getString("word"),
                        game.getInt("length"),
                        game.getInt("tries"),
                        formatGuessList(game),
                        game.getInt("solved"),
                        ""
                    ))
                }
                guessStats = extractStats(gameList)
                displayStats()
            }

            override fun onError(err: VolleyError) {
                noInternetLay.visibility = View.VISIBLE
                loadingLay.visibility = View.GONE
            }

        })
    }

    fun displayStats() {
        findViewById<TextView>(R.id.stats).text = getString(R.string.n_solved, gameList.size, 3726)
        findViewById<TextView>(R.id.val1).text = guessStats[1].toString()
        findViewById<TextView>(R.id.val2).text = guessStats[2].toString()
        findViewById<TextView>(R.id.val3).text = guessStats[3].toString()
        findViewById<TextView>(R.id.val4).text = guessStats[4].toString()
        findViewById<TextView>(R.id.val5).text = guessStats[5].toString()
        findViewById<TextView>(R.id.val6).text = guessStats[6].toString()
        val progressMultiplier: Float = 99.0f / guessStats.max().toFloat()
        findViewById<LinearProgressIndicator>(R.id.p1).progress =
            round(guessStats[1] * progressMultiplier).toInt() + 1
        findViewById<LinearProgressIndicator>(R.id.p2).progress =
            round(guessStats[2] * progressMultiplier).toInt() + 1
        findViewById<LinearProgressIndicator>(R.id.p3).progress =
            round(guessStats[3] * progressMultiplier).toInt() + 1
        findViewById<LinearProgressIndicator>(R.id.p4).progress =
            round(guessStats[4] * progressMultiplier).toInt() + 1
        findViewById<LinearProgressIndicator>(R.id.p5).progress =
            round(guessStats[5] * progressMultiplier).toInt() + 1
        findViewById<LinearProgressIndicator>(R.id.p6).progress =
            round(guessStats[6] * progressMultiplier).toInt() + 1


        recyclerView.adapter = StatsRowAdapter(gameList, this)
    }







    class StatsRowAdapter(private val gameList: List<DataModels.GameData>,
                          private val ctx: Activity) :
        RecyclerView.Adapter<StatsRowAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val id: TextView = view.findViewById(R.id.medal)
            val letter1: TextView = view.findViewById(R.id.l1)
            val letter2: TextView = view.findViewById(R.id.l2)
            val letter3: TextView = view.findViewById(R.id.l3)
            val letter4: TextView = view.findViewById(R.id.l4)
            val letter5: TextView = view.findViewById(R.id.l5)
            val stats: TextView = view.findViewById(R.id.stats)
            val info: MaterialButton = view.findViewById(R.id.info)
            val view: MaterialButton = view.findViewById(R.id.view)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            return ViewHolder(
                LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.stats_element, viewGroup, false))
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            val game: DataModels.GameData = gameList[position]
            vh.id.text = ctx.getString(R.string.word_id, game.wordId)
            vh.letter1.text = game.word[0].toString()
            vh.letter2.text = game.word[1].toString()
            vh.letter3.text = game.word[2].toString()
            vh.letter4.text = game.word[3].toString()
            vh.letter5.text = game.word[4].toString()
            vh.stats.text = ctx.getString(R.string.moves_of, game.guesses.size, game.tries)

            vh.info.setOnClickListener {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ctx.getString(
                    R.string.explanation_link, game.word.lowercase()))))
            }
            vh.view.setOnClickListener {
                showGameDetailsDialog(game)
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = gameList.size

        private fun showGameDetailsDialog(game: DataModels.GameData) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.stats_view)
            val recyclerView: RecyclerView = dialog.findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(ctx)
            recyclerView.adapter = Game.RowAdapter(game, ctx, game.guesses.size)
            val okBtn: MaterialButton = dialog.findViewById(R.id.ok)
            okBtn.setOnClickListener { dialog.dismiss() }
            dialog.show()

        }
    }


}