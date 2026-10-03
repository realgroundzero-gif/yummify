package de.yummify.app.data.model

data class NotionConfig(
    val integrationToken: String = "",
    val recipeDatabaseId: String = "",
    val mealPlanDatabaseId: String = "",
    val shoppingListDatabaseId: String = "",
    val inventoryDatabaseId: String = "",
    val isConfigured: Boolean = false,
    val lastSyncTime: String = "Noch nicht synchronisiert"
)
