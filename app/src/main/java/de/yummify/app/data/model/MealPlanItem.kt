package de.yummify.app.data.model

enum class MealType(val displayName: String, val timeSlot: String) {
    BREAKFAST("Frühstück", "08:00"),
    LUNCH("Mittagessen", "12:30"),
    DINNER("Abendessen", "19:00"),
    SNACK("Snack", "16:00")
}

data class MealPlanItem(
    val id: String,
    val dayOfWeek: String, // "Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"
    val dayOfMonth: Int, // e.g. 14
    val mealType: MealType,
    val recipeId: String?,
    val recipeTitle: String,
    val imageUrl: String,
    val calories: Int,
    val proteinGrams: Int,
    val cookTimeMinutes: Int,
    val isCooked: Boolean = false
)
