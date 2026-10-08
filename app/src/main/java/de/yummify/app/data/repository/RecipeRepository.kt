package de.yummify.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.local.SampleData
import de.yummify.app.data.model.Ingredient
import de.yummify.app.data.model.Recipe
import de.yummify.app.data.remote.NotionRecipeReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

data class RecipeState(
    val recipes: List<Recipe> = emptyList(),
    val loading: Boolean = false,
    /** Set when the last refresh failed; [recipes] then still holds the last good list. */
    val error: String? = null,
    val fromNotion: Boolean = false,
    val lastSync: LocalDateTime? = null
)

/**
 * Single source of recipes for all screens. Loads once per Notion connection, keeps the last
 * good list on disk for offline starts, and stores favorites locally.
 */
class RecipeRepository internal constructor(
    context: Context,
    private val readerFactory: (token: String, databaseId: String) -> NotionRecipeReader = { t, d -> NotionRecipeReader(t, d) }
) {
    private val prefs = context.getSharedPreferences("yummify_recipes", Context.MODE_PRIVATE)
    private val config = UserPreferencesRepository.getInstance(context)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMutex = Mutex()

    private val _favorites = MutableStateFlow(prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private var raw: List<Recipe> = emptyList()
    private val _state = MutableStateFlow(RecipeState())
    val state: StateFlow<RecipeState> = _state.asStateFlow()

    private val configured get() = config.preferences.value.let { it.tokenInput.isNotBlank() && it.databaseIdInput.isNotBlank() }
    private fun reader() = config.preferences.value.let { readerFactory(it.tokenInput, it.databaseIdInput) }
    private fun cacheKey() = "recipes_${formatNotionId(config.preferences.value.databaseIdInput)}"

    init {
        scope.launch {
            config.preferences.map { it.tokenInput.trim() to formatNotionId(it.databaseIdInput) }.distinctUntilChanged().collect {
                loadCached()
                refresh()
            }
        }
    }

    private fun loadCached() {
        raw = if (!configured) SampleData.recipes else runCatching {
            prefs.getString(cacheKey(), null)?.let { gson.fromJson<List<Recipe>>(it, object : TypeToken<List<Recipe>>() {}.type) }
        }.getOrNull().orEmpty().map { it.normalized() }
        _state.value = RecipeState(withFavorites(raw), fromNotion = configured,
            lastSync = prefs.getString("${cacheKey()}_time", null)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() })
    }

    private fun withFavorites(list: List<Recipe>) = _favorites.value.let { favs -> list.map { it.copy(isFavorite = it.id in favs) } }

    /** Reloads from Notion. Returns null on success, otherwise the error message (which is also put into [state]). */
    suspend fun refresh(): String? = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            if (!configured) {
                raw = SampleData.recipes
                _state.value = RecipeState(withFavorites(raw))
                return@withLock null
            }
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val loaded = reader().fetchAll()
                val now = LocalDateTime.now()
                raw = loaded
                prefs.edit().putString(cacheKey(), gson.toJson(loaded)).putString("${cacheKey()}_time", now.toString()).apply()
                _state.value = RecipeState(withFavorites(raw), fromNotion = true, lastSync = now)
                null
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                val message = e.message ?: "Notion ist nicht erreichbar."
                _state.value = _state.value.copy(loading = false, error = message)
                message
            }
        }
    }

    fun refreshAsync() { scope.launch { refresh() } }

    /** Full recipe including the page body. Falls back to the cached list entry when offline. */
    suspend fun recipe(id: String): Recipe = withContext(Dispatchers.IO) {
        // Right after start the cache may not be loaded yet; demo recipes are always available.
        val cached = raw.firstOrNull { it.id == id } ?: if (!configured) SampleData.recipes.firstOrNull { it.id == id } else null
        val result = if (!configured) cached ?: throw IllegalArgumentException("Rezept nicht gefunden.")
        else try { reader().fetchRecipe(id) } catch (e: CancellationException) { throw e
        } catch (e: Exception) { cached ?: throw e }
        result.copy(isFavorite = id in _favorites.value)
    }

    fun addCreated(recipe: Recipe) {
        raw = listOf(recipe) + raw.filterNot { it.id == recipe.id }
        prefs.edit().putString(cacheKey(), gson.toJson(raw)).apply()
        _state.value = _state.value.copy(recipes = withFavorites(raw))
    }

    fun toggleFavorite(id: String) {
        val updated = if (id in _favorites.value) _favorites.value - id else _favorites.value + id
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
        _favorites.value = updated
        _state.value = _state.value.copy(recipes = withFavorites(raw))
    }

    /** Returns false when the recipe is not from Notion or the database has no "Geplant am" column. */
    suspend fun setPlannedDate(recipeId: String, date: LocalDate?): Boolean = withContext(Dispatchers.IO) {
        if (!configured || raw.none { it.id == recipeId && it.notionPageId != null }) false
        else reader().updatePlannedDate(recipeId, date)
    }

    suspend fun setRating(recipeId: String, rating: Int) = withContext(Dispatchers.IO) {
        require(configured) { "Bewertungen werden in Notion gespeichert. Bitte zuerst Notion einrichten." }
        reader().updateRating(recipeId, rating)
        raw = raw.map { if (it.id == recipeId) it.copy(score = rating.toDouble()) else it }
        _state.value = _state.value.copy(recipes = withFavorites(raw))
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"

        @Volatile private var instance: RecipeRepository? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: RecipeRepository(context.applicationContext).also { instance = it }
        }

        /**
         * Accepts a raw ID (with or without dashes) or any Notion link, including
         * "notion.so/workspace/Rezepte-<id>?v=…". Unrecognized input is returned trimmed.
         */
        fun formatNotionId(rawInput: String): String {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return ""
            val segment = trimmed.substringBefore('?').substringBefore('#').trimEnd('/').substringAfterLast('/')
            val hex = Regex("[0-9a-fA-F]{32}")
            val compact = segment.replace("-", "")
            val id = when {
                compact.matches(hex) -> compact
                segment.takeLast(32).matches(hex) -> segment.takeLast(32)
                else -> return trimmed
            }.lowercase()
            return "${id.substring(0, 8)}-${id.substring(8, 12)}-${id.substring(12, 16)}-${id.substring(16, 20)}-${id.substring(20)}"
        }

        /** One ingredient per line. A single line may also list ingredients separated by commas. */
        fun parseIngredients(text: String): List<Ingredient> {
            val parts = if (text.contains('\n')) text.lines() else text.split(Regex(""",(?!\d)"""))
            return parts.map { it.trim() }.filter { it.isNotEmpty() }.map { parseIngredientLine(it) }.filter { it.name.isNotBlank() }
        }

        private val unicodeFractions = mapOf('½' to 0.5, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3, '¼' to 0.25, '¾' to 0.75, '⅛' to 0.125)

        private const val NUMBER = """\d+(?:[.,]\d+)?"""
        private val mixedFraction = Regex("""^(\d+)\s+(\d+)/(\d+)\s*(.*)$""")
        private val simpleFraction = Regex("""^(\d+)/(\d+)\s*(.*)$""")
        private val unicodeFraction = Regex("""^(\d+)?\s*([½⅓⅔¼¾⅛])\s*(.*)$""")
        private val range = Regex("""^($NUMBER)\s*[-–]\s*($NUMBER)\s*(.*)$""")
        private val plainNumber = Regex("""^($NUMBER)\s*(.*)$""")
        private fun decimal(value: String) = value.replace(',', '.').toDouble()

        /** Understands "200 g", "1,5 kg", "1/2 TL", "1 1/2 EL", "½ Bund", "1½ Tassen" and "2-3 Zehen" (ranges use the larger amount). */
        fun parseIngredientLine(line: String): Ingredient {
            val trimmed = line.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim()
            if (trimmed.isBlank()) return Ingredient(name = "", amount = 0.0, unit = "")
            val (amount, rest) = mixedFraction.find(trimmed)?.destructured?.let { (whole, num, den, rest) ->
                if (den.toDouble() == 0.0) return Ingredient(name = trimmed, amount = 0.0, unit = "")
                whole.toDouble() + num.toDouble() / den.toDouble() to rest
            } ?: simpleFraction.find(trimmed)?.destructured?.let { (num, den, rest) ->
                if (den.toDouble() == 0.0) return Ingredient(name = trimmed, amount = 0.0, unit = "")
                num.toDouble() / den.toDouble() to rest
            } ?: unicodeFraction.find(trimmed)?.destructured?.let { (whole, fraction, rest) ->
                (whole.toDoubleOrNull() ?: 0.0) + unicodeFractions.getValue(fraction.first()) to rest
            } ?: range.find(trimmed)?.destructured?.let { (low, high, rest) ->
                maxOf(decimal(low), decimal(high)) to rest
            } ?: plainNumber.find(trimmed)?.destructured?.let { (value, rest) ->
                decimal(value) to rest
            } ?: return Ingredient(name = trimmed, amount = 0.0, unit = "")
            val (unit, name) = extractUnitAndName(rest.trim())
            return Ingredient(name = name, amount = amount, unit = unit)
        }

        private val commonUnits = setOf(
            "g", "kg", "ml", "l", "dl", "cl",
            "el", "tl", "esslöffel", "teelöffel",
            "prise", "prisen", "pck", "packung", "packungen", "päckchen",
            "dosen", "dose", "becher", "glas", "gläser", "zehe", "zehen",
            "stk", "stück", "scheibe", "scheiben", "bund", "kästchen", "handvoll",
            "tbsp", "tsp", "cup", "cups", "oz", "lb"
        )

        private fun extractUnitAndName(rest: String): Pair<String, String> {
            if (rest.isBlank()) return "" to ""
            val parts = rest.split(Regex("""\s+"""), limit = 2)
            val firstWord = parts[0].lowercase().removeSuffix(".")
            return if (firstWord in commonUnits) parts[0] to parts.getOrNull(1).orEmpty() else "" to rest
        }
    }
}

/** Gson ignores Kotlin defaults, so cached entries from older versions may hold nulls in non-null fields. */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
internal fun Recipe.normalized(): Recipe = copy(
    title = title ?: "", description = description ?: "", imageUrl = imageUrl ?: "", category = category ?: "all",
    tags = tags ?: emptyList(), ingredients = ingredients ?: emptyList(), instructions = instructions ?: emptyList()
)
