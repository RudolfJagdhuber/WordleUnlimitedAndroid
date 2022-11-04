package de.datavisions.wortel.helpers

import org.json.JSONObject
import java.io.Serializable


fun formatGuessList(gameResponse: JSONObject) : ArrayList<DataModels.WordCorrectness>{
    val wcList = ArrayList<DataModels.WordCorrectness>()
    if (gameResponse.isNull("guesses")) return wcList
    val wc = gameResponse.getJSONArray("guesses")
    for (i in 0 until wc.length()) {
        val lcList = ArrayList<DataModels.LetterCorrectness>()
        val lcJSON = wc.getJSONArray(i)
        for (j in 0 until lcJSON.length()) {
            val lc = lcJSON.getJSONObject(j)
            lcList.add(
                DataModels.LetterCorrectness(
                    lc.getString("letter"),
                    lc.getBoolean("correct_position"),
                    lc.getBoolean("different_position")
                )
            )
        }
        wcList.add(DataModels.WordCorrectness(lcList))
    }
    return wcList
}


fun extractStats(gameList: List<DataModels.GameData>) : IntArray {
    // stats of the number of guesses needed.
    // stats[0] := games lost
    // stats[1] := 1 guess needed
    // stats[2] := 2 guesses needed, etc
    val stats: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0)
    for (game: DataModels.GameData in gameList) {
        if (game.solved < 0) stats[0]++
        else if (game.solved == 0) continue
        else stats[game.guesses.size]++
    }
    return stats
}


class DataModels {

    data class UserCredentials(
        val id: String,
        var name: String,
        var password: String,
        var generic: Boolean = true
    ) : Serializable

    data class LetterCorrectness(
        val letter: String,
        val correctPosition: Boolean,
        val differentPosition: Boolean
    ) : Serializable

    data class WordCorrectness(val word: List<LetterCorrectness>) : Serializable

    data class GameData(
        val id: String,
        val player: String,
        val wordId: Int,
        var word: String,
        val length: Int,
        val tries: Int,
        var guesses: List<WordCorrectness>,
        var solved: Int,
        var activeGuess: String = " ".repeat(length),
        var activeBox: Int = 0
    ) : Serializable



}