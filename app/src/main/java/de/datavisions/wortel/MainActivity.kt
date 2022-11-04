package de.datavisions.wortel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.VolleyError
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import de.datavisions.wortel.api.JsonObjectResponse
import de.datavisions.wortel.api.newGame
import de.datavisions.wortel.api.registerGeneric
import de.datavisions.wortel.helpers.DataModels
import de.datavisions.wortel.helpers.DataModels.UserCredentials
import de.datavisions.wortel.helpers.formatGuessList
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var credentials: UserCredentials? = null

    private lateinit var loadingLay: ProgressBar
    lateinit var noInternetLay: LinearLayout
    lateinit var userTV: TextView

    // update the UI after returning from Profile Activity
    private val resetUI = registerForActivityResult(StartActivityForResult()) {
        loadUserCredentials()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadingLay = findViewById(R.id.loading_layout)
        noInternetLay =  findViewById(R.id.no_internet_layout)
        userTV = findViewById(R.id.user_name)
        loadingLay.visibility = View.VISIBLE

        findViewById<LinearLayout>(R.id.user).setOnClickListener {
            resetUI.launch(Intent(this, Profile::class.java))
        }

        findViewById<MaterialButton>(R.id.new_game).setOnClickListener {
            loadingLay.visibility = View.VISIBLE
            newGame(this, credentials!!.id, -1, 5, 6,
                object : JsonObjectResponse {
                    override fun onSuccess(res: JSONObject, ctx: Context) {
                        loadingLay.visibility = View.GONE
                        val gameData = DataModels.GameData(
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
                        startActivity(Intent(ctx, Game::class.java)
                            .putExtra("game_data", gameData))
                    }

                    override fun onError(err: VolleyError) {
                        noInternetLay.visibility = View.VISIBLE
                        loadingLay.visibility = View.GONE
                    }
            })
        }


        findViewById<MaterialButton>(R.id.stats).setOnClickListener {
            startActivity(Intent(this, Stats::class.java)
                .putExtra("user_id", credentials!!.id)
                .putExtra("user_name", credentials!!.name)
            )
        }

        findViewById<MaterialButton>(R.id.howto).setOnClickListener {
            startActivity(Intent(this, Rules::class.java))
        }

        // Only accessible on error
        findViewById<MaterialButton>(R.id.retry).setOnClickListener {
            noInternetLay.visibility = View.GONE
            if (credentials == null) loadUserCredentials()
        }

        // Check if user credentials in SharedPreferences
        loadUserCredentials()
    }

    private fun loadUserCredentials() {
        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val credentialsJson = prefs.getString("user_credentials", "")
        if (credentialsJson.isNullOrEmpty()) {
            // Do register generic
            registerGeneric(this, object: JsonObjectResponse {
                override fun onSuccess(res: JSONObject, ctx: Context) {
                    credentials = UserCredentials(
                        id = res.getString("id"),
                        name = res.getString("name"),
                        password = res.getString("password"),
                        generic = true
                    )
                    prefs.edit().putString("user_credentials", Gson().toJson(credentials)).apply()
                    loadingLay.visibility = View.GONE
                    userTV.text = credentials?.name
                }
                override fun onError(err: VolleyError) {
                    noInternetLay.visibility = View.VISIBLE
                    loadingLay.visibility = View.GONE
                }
            })
            return
        }
        loadingLay.visibility = View.GONE
        credentials = Gson().fromJson(credentialsJson, UserCredentials::class.java)
        userTV.text = credentials?.name
    }


}