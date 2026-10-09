package de.yummify.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** A to-do list of the Home Assistant. The Bring integration provides one `todo` entity per Bring list. */
data class HomeAssistantTodoList(val entityId: String, val name: String) {
    val isBring get() = "bring" in entityId.lowercase() || "bring" in name.lowercase()
}

/**
 * Hands items to a Home Assistant to-do list through its REST API (long-lived access token). With the Bring
 * integration this fills the Bring list: the item name becomes the Bring article, the description its note.
 */
class HomeAssistantApi(baseUrl: String, private val token: String, private val client: OkHttpClient = NotionHttp.client) {
    private val root = normalize(baseUrl)
    private val gson = Gson()

    private fun call(path: String, body: Any? = null): String {
        val builder = Request.Builder().url("$root/api/$path")
            .header("Authorization", "Bearer ${token.trim()}").header("Accept", "application/json")
        if (body != null) builder.post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        val response = try { client.newCall(builder.build()).execute() } catch (e: IOException) {
            throw IOException("Home Assistant ist nicht erreichbar. Adresse und Netzwerk prüfen (${e.message ?: "keine Verbindung"}).")
        }
        response.use {
            val raw = it.body?.string().orEmpty()
            when {
                it.isSuccessful -> return raw
                it.code == 401 -> throw IOException("Home Assistant lehnt den Token ab (401). Bitte einen gültigen Langzeit-Zugriffstoken eintragen.")
                it.code == 404 && path.startsWith("services/") -> throw IOException("Der Dienst todo.add_item fehlt. Die Aufgabenlisten brauchen Home Assistant 2023.11 oder neuer.")
                it.code == 400 || it.code == 404 -> throw IOException("Home Assistant meldet ${it.code}: ${raw.take(200)}")
                else -> throw IOException("Home Assistant antwortet mit Fehler ${it.code}.")
            }
        }
    }

    /** Checks address and token. */
    fun check() {
        val raw = call("")   // network and authorization errors are raised here
        val message = runCatching { JsonParser.parseString(raw).asJsonObject["message"]?.asString }.getOrNull()
        require(message != null) { "Das ist keine Home-Assistant-Adresse (keine gültige Antwort von /api/)." }
    }

    fun todoLists(): List<HomeAssistantTodoList> {
        val states: JsonArray = JsonParser.parseString(call("states")).asJsonArray
        return states.map { it.asJsonObject }.filter { it["entity_id"].asString.startsWith("todo.") }.map { state ->
            val id = state["entity_id"].asString
            HomeAssistantTodoList(id, state.getAsJsonObject("attributes")?.get("friendly_name")?.asString ?: id)
        }.sortedWith(compareBy({ !it.isBring }, { it.name.lowercase() }))
    }

    fun addItem(entityId: String, name: String, description: String?) {
        require(name.isNotBlank()) { "Artikelname fehlt." }
        val body = mutableMapOf<String, Any>("entity_id" to entityId, "item" to name.trim())
        if (!description.isNullOrBlank()) body["description"] = description.trim()
        call("services/todo/add_item", body)
    }

    companion object {
        /** Hosts that only exist inside a home network: plain HTTP is accepted for them, everything else needs HTTPS. */
        fun isLocalHost(host: String): Boolean {
            val h = host.lowercase()
            if (h == "localhost" || !h.contains('.') && !h.contains(':')) return true   // "homeassistant"
            if (h.endsWith(".local") || h.endsWith(".lan") || h.endsWith(".home.arpa") || h.endsWith(".internal")) return true
            val p = h.split('.').mapNotNull { it.toIntOrNull() }
            if (p.size == 4 && h.split('.').size == 4) {
                return p[0] == 10 || p[0] == 127 || (p[0] == 192 && p[1] == 168) || (p[0] == 172 && p[1] in 16..31) || (p[0] == 169 && p[1] == 254) ||
                    (p[0] == 100 && p[1] in 64..127)   // Tailscale and carrier-grade NAT
            }
            return false
        }

        /** Normalised base URL without trailing slash; throws a German message for unusable input. */
        fun normalize(input: String): String {
            val text = input.trim().let { if (it.contains("://")) it else "http://$it" }
            val url = text.toHttpUrlOrNull()
            require(url != null) { "Bitte eine gültige Adresse eintragen, z. B. http://homeassistant.local:8123." }
            require(url.scheme == "https" || isLocalHost(url.host)) { "Außerhalb des Heimnetzes ist nur HTTPS erlaubt (z. B. die Nabu-Casa-Adresse)." }
            return url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString().removeSuffix("/")
        }
    }
}
