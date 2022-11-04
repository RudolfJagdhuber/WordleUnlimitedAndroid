package de.datavisions.wortel

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.android.volley.VolleyError
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import de.datavisions.wortel.api.*
import de.datavisions.wortel.helpers.DataModels
import org.json.JSONObject

class Profile : AppCompatActivity() {

    private lateinit var credentials: DataModels.UserCredentials

    private lateinit var loadingLay: ProgressBar
    lateinit var updateBtn: MaterialButton
    lateinit var loginBtn: MaterialButton
    lateinit var usernameT: TextInputEditText
    lateinit var passwordT: TextInputEditText
    lateinit var messageT: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        findViewById<MaterialButton>(R.id.back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.delete).setOnClickListener {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.delete_data_title)
                .setMessage(R.string.delete_data_desc)
                .setPositiveButton(R.string.delete_data_confirm) { _, _ -> deleteUserData() }
                .setNegativeButton(R.string.cancel, null)
                .create()
            alertDialog.show()
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.fgRed))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                getColor(R.color.fgBlack))
        }

        loadingLay = findViewById(R.id.loading_layout)
        updateBtn = findViewById(R.id.change_data)
        loginBtn = findViewById(R.id.login)
        usernameT = findViewById(R.id.name)
        passwordT = findViewById(R.id.password)
        messageT = findViewById(R.id.message)

        loadCredentials()

        passwordT.doAfterTextChanged { checkValidText(false) }
        usernameT.doAfterTextChanged { checkValidText(true) }
        updateBtn.setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            usernameT.clearFocus()
            passwordT.clearFocus()
            if (usernameT.text.toString() != credentials.name) {
                updateUsername(passwordT.text.toString() != credentials.password)
            } else if (passwordT.text.toString() != credentials.password){
                updatePassword()
            }
        }
        loginBtn.setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            usernameT.clearFocus()
            passwordT.clearFocus()
            login()
        }
    }

    private fun checkValidText(isNameNotPassword: Boolean) {
        if (usernameT.text.toString().contains(" ")) {
            usernameT.setText(usernameT.text.toString().trim())
        }
        val new: String = (if (isNameNotPassword) usernameT else passwordT).text.toString()
        if (usernameT.text.toString() == credentials.name &&
            passwordT.text.toString() == credentials.password) {
            loginBtn.isEnabled = false
            updateBtn.isEnabled = false
            messageT.text = getString(R.string.no_change)
            messageT.setTextColor(getColor(R.color.fgRed))
            return
        }
        if (new.isBlank()) {
            loginBtn.isEnabled = false
            updateBtn.isEnabled = false
            (if (isNameNotPassword) usernameT else passwordT).error = getString(R.string.text_empty)
            return
        }
        if (new.length > 200) {
            loginBtn.isEnabled = false
            updateBtn.isEnabled = false
            (if (isNameNotPassword) usernameT else passwordT).error = getString(R.string.text_long)
            return
        }
        if (new.startsWith("user") && isNameNotPassword) {
            loginBtn.isEnabled = false
            updateBtn.isEnabled = false
            usernameT.error = getString(R.string.text_starts_user)
            return
        }
        if (new.startsWith("deleted") && isNameNotPassword) {
            loginBtn.isEnabled = false
            updateBtn.isEnabled = false
            usernameT.error = getString(R.string.text_starts_deleted)
            return
        }
        // Everything OK
        loginBtn.isEnabled = true
        updateBtn.isEnabled = true
        messageT.text = ""
        (if (isNameNotPassword) usernameT else passwordT).error = null
    }

    private fun loadCredentials() {
        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val credentialsJson = prefs.getString("user_credentials", "")
        credentials = Gson().fromJson(credentialsJson, DataModels.UserCredentials::class.java)
        usernameT.setText(credentials.name)
        passwordT.setText(credentials.password)
        // No Login or update if credentials have not changed
        loginBtn.isEnabled = false
        updateBtn.isEnabled = false
        loadingLay.visibility = View.GONE
    }

    private fun updateUsername(alsoUpdatePassword: Boolean = false) {
        loadingLay.visibility = View.VISIBLE
        usernameT.clearFocus()
        changeUsername(this, credentials.id, credentials.password, usernameT.text.toString(),
            object : JsonObjectResponse {
                override fun onSuccess(res: JSONObject, ctx: Context) {
                    // Store new data
                    credentials.name = usernameT.text.toString()
                    getSharedPreferences("data", MODE_PRIVATE).edit()
                        .putString("user_credentials", Gson().toJson(credentials)).apply()
                    if (alsoUpdatePassword) {
                        updatePassword()
                        return
                    }
                    loadCredentials()
                    messageT.text = getString(R.string.data_changed)
                    messageT.setTextColor(getColor(R.color.fgGreen))
                }
                override fun onError(err: VolleyError) {
                    loadingLay.visibility = View.GONE
                    if (err.networkResponse.statusCode == 409) {
                        usernameT.error = getString(R.string.username_taken)
                        return
                    }
                    messageT.text = getString(R.string.no_internet)
                    messageT.setTextColor(getColor(R.color.fgRed))
                }
            })
    }

    private fun updatePassword() {
        loadingLay.visibility = View.VISIBLE
        passwordT.clearFocus()
        changePassword(this, credentials.id, credentials.password, passwordT.text.toString(),
            object : JsonObjectResponse {
                override fun onSuccess(res: JSONObject, ctx: Context) {
                    // Store new data
                    credentials.password = passwordT.text.toString()
                    credentials.generic = false
                    getSharedPreferences("data", MODE_PRIVATE).edit()
                        .putString("user_credentials", Gson().toJson(credentials)).apply()
                    loadCredentials()
                    messageT.text = getString(R.string.data_changed)
                    messageT.setTextColor(getColor(R.color.fgGreen))
                }
                override fun onError(err: VolleyError) {
                    loadingLay.visibility = View.GONE
                    messageT.text = getString(R.string.no_internet)
                    messageT.setTextColor(getColor(R.color.fgRed))
                }
            })
    }

    private fun deleteUserData() {
        loadingLay.visibility = View.VISIBLE
        deleteUser(this, credentials.id, credentials.password, object : JsonObjectResponse {
            override fun onSuccess(res: JSONObject, ctx: Context) {
                loadingLay.visibility = View.GONE
                // Delete credentials from SharedPreferences
                getSharedPreferences("data", MODE_PRIVATE).edit().clear().apply()
                // End the App
                finishAffinity()
            }
            override fun onError(err: VolleyError) {
                loadingLay.visibility = View.GONE
                messageT.text = getString(R.string.no_internet)
            }
        })
    }

    private fun login() {
        loadingLay.visibility = View.VISIBLE
        loginUser(this, usernameT.text.toString(), passwordT.text.toString(),
            object : JsonObjectResponse {
                override fun onSuccess(res: JSONObject, ctx: Context) {
                    loadingLay.visibility = View.GONE
                    credentials = DataModels.UserCredentials(
                        id = res.getString("id"),
                        name = usernameT.text.toString(),
                        password = passwordT.text.toString(),
                        generic = false
                    )
                    // Update credentials in SharedPreferences
                    getSharedPreferences("data", MODE_PRIVATE).edit()
                        .putString("user_credentials", Gson().toJson(credentials)).apply()
                    loadCredentials()
                    messageT.text = getString(R.string.login_ok)
                    messageT.setTextColor(getColor(R.color.fgGreen))
                }
                override fun onError(err: VolleyError) {
                    loadingLay.visibility = View.GONE
                    messageT.setTextColor(getColor(R.color.fgRed))
                    if (err.networkResponse.statusCode == 404) {
                        messageT.text = getString(R.string.wrong_user)
                        return
                    }
                    messageT.text = getString(R.string.no_internet)
                }
            })
    }
}