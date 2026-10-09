package de.yummify.app.data.model

/** One entry that goes to the Home Assistant to-do list. */
data class HomeAssistantEntry(val itemId: String, val name: String, val description: String?) {
    /** Changes when the article or its amount changes, so a later edit is sent again. */
    val signature get() = "$name|${description.orEmpty()}"
}

object HomeAssistantExport {
    /** Open (not checked) entries; the amount becomes the description. [alreadySent] maps item id to signature. */
    fun plan(list: List<ShoppingItem>, alreadySent: Map<String, String>, includeSent: Boolean): List<HomeAssistantEntry> =
        list.filter { !it.isChecked && it.name.isNotBlank() }.map { item ->
            HomeAssistantEntry(item.id, item.name.trim(), item.amountWithUnit.trim().takeIf { it.isNotBlank() })
        }.filter { includeSent || alreadySent[it.itemId] != it.signature }
}
