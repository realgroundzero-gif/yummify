# Minification is currently disabled (isMinifyEnabled = false). These rules matter once it is enabled.

# Gson reads and writes these classes by reflection (local storage and the recipe cache).
-keep class de.yummify.app.data.model.** { *; }
-keepattributes Signature, *Annotation*
