package de.datavisions.wordle.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import de.datavisions.wordle.R
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest


const val API_URL = "http://82.165.111.42/wordle/api/"

interface JsonObjectResponse {
    fun onSuccess(res: JSONObject, ctx: Context)
    fun onError(err: VolleyError)
}

interface JsonArrayResponse {
    fun onSuccess(res: JSONArray, ctx: Context)
    fun onError(err: VolleyError)
}

fun String.sha256(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray())
        .fold(StringBuilder()) { sb, it ->
            sb.append("%02x".format(it))
        }.toString()
}


fun registerGeneric(ctx: Context, listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "register_generic/",
        JSONObject()
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}

fun changeUsername(ctx: Context, userId: String, password: String, newName: String,
                   listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "update_username/",
        JSONObject()
            .put("user_id", userId)
            .put("password", password.sha256())
            .put("new_name", newName)
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}

fun changePassword(ctx: Context, userId: String, oldPassword: String, newPassword: String,
                   listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "update_password/",
        JSONObject()
            .put("user_id", userId)
            .put("old_password", oldPassword.sha256())
            .put("new_password", newPassword.sha256())
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}


fun deleteUser(ctx: Context, userId: String, password: String, listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "delete_user/",
        JSONObject()
            .put("user_id", userId)
            .put("password", password.sha256())
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}


fun loginUser(ctx: Context, name: String, password: String, listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "login/",
        JSONObject()
            .put("name", name)
            .put("password", password.sha256())
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}


fun newGame(ctx: Context, userId: String, word_id: Int, length: Int = 5, tries: Int = 5,
            listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "new_game/",
        JSONObject()
            .put("user_id", userId)
            .put("length", length)
            .put("tries", tries)
            .put("word_id", word_id)
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}

fun guess(ctx: Context, gameId: String, guess: String, listener: JsonObjectResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonObjectRequest(
        Request.Method.POST,
        API_URL + "guess/",
        JSONObject()
            .put("game_id", gameId)
            .put("guess", guess)
            .put("token", ctx.getString(R.string.api_token)),
        {success: JSONObject -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}

fun gameList(ctx: Context, userId: String, listener: JsonArrayResponse) {
    VolleySingleton.getInstance(ctx).addToRequestQueue(JsonArrayRequest(
        Request.Method.GET,
        API_URL + "game_list/?user_id=" + userId,
        null,
        {success: JSONArray -> listener.onSuccess(success, ctx)},
        {error: VolleyError -> listener.onError(error)}
    ))
}
