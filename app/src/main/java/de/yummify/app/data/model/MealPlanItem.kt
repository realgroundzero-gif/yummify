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
    val calories: Int?,
    val proteinGrams: Int?,
    val cookTimeMinutes: Int?,
    val isCooked: Boolean = false,
    val plannedDate: String? = null
)

/** Legacy records lacked a year/month: match them only within the current week. */
fun MealPlanItem.matchesDate(date: java.time.LocalDate): Boolean {
    if (!plannedDate.isNullOrBlank()) return plannedDate == date.toString()
    val today = java.time.LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val dayCodes = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    return !date.isBefore(monday) && !date.isAfter(monday.plusDays(6)) &&
        dayOfMonth == date.dayOfMonth && dayOfWeek == dayCodes[date.dayOfWeek.value - 1]
}
