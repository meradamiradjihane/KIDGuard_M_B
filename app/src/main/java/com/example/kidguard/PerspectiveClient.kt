package com.example.kidguard

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.util.Log

class PerspectiveClient(private val apiKey: String) {

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    fun analyzeText(text: String, callback: (Boolean) -> Unit) {
        val url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=$apiKey"

        val json = JSONObject().apply {
            put("comment", JSONObject().put("text", text))
            put("languages", listOf("fr"))
            put("requestedAttributes", JSONObject().put("TOXICITY", JSONObject()))
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PerspectiveClient", "Erreur réseau : ${e.message}")
                handler.post { callback(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("PerspectiveClient", "Échec réponse : ${response.code}")
                        handler.post { callback(false) }
                        return
                    }

                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val score = jsonResponse
                        .getJSONObject("attributeScores")
                        .getJSONObject("TOXICITY")
                        .getJSONObject("summaryScore")
                        .getDouble("value")

                    Log.d("PerspectiveClient", "Score de toxicité : $score")

                    handler.post {
                        callback(score > 0.8)
                    }
                }
            }
        })
    }
}
