package de.yummify.app.data.model

/** Values entered in the recipe form, before a Notion page exists. */
data class RecipeDraft(val title: String, val description: String, val servings: Int,
    val category: String, val ingredients: String, val instructions: String, val imageUrl: String = "") {
    fun validate(): String? = when {
        title.isBlank() -> "Bitte einen Rezeptnamen eingeben."
        title.length > 200 -> "Der Rezeptname darf höchstens 200 Zeichen enthalten."
        servings !in 1..20 -> "Bitte 1 bis 20 Portionen angeben."
        description.length > 2000 -> "Die Beschreibung darf höchstens 2000 Zeichen enthalten."
        category.length > 100 || category.contains(',') -> "Bitte eine Kategorie mit höchstens 100 Zeichen ohne Komma angeben."
        ingredients.isBlank() -> "Bitte mindestens eine Zutat eingeben."
        ingredients.length > 20000 -> "Die Zutatenliste ist zu lang (maximal 20000 Zeichen)."
        instructions.isBlank() -> "Bitte die Zubereitung beschreiben."
        steps.size > 100 || steps.any { it.length > 2000 } -> "Bitte höchstens 100 Schritte mit je 2000 Zeichen eingeben."
        imageUrl.isNotBlank() && runCatching { java.net.URI(imageUrl).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false).not() -> "Bitte eine gültige HTTPS-Bildadresse eingeben."
        else -> null
    }
    val steps get() = instructions.lines().map { it.trim() }.filter { it.isNotEmpty() }
}
