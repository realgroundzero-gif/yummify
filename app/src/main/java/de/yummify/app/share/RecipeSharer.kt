package de.yummify.app.share

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import de.yummify.app.data.model.Recipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** What the share sheet receives: the card image (null if it could not be created) and the text. */
data class ShareContent(val imageUri: Uri?, val text: String, val title: String)

object RecipeSharer {
    private const val COVER_TIMEOUT_MS = 5_000L
    private const val DIRECTORY = "shares"

    fun authority(context: Context) = "${context.packageName}.inventory.files"

    /** Loads the cover for the card. A missing, slow or broken image never blocks sharing. */
    private suspend fun loadCover(context: Context, url: String): Bitmap? {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null
        return try {
            withTimeoutOrNull(COVER_TIMEOUT_MS) {
                val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(RecipeCardRenderer.WIDTH).build()
                (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { null }
    }

    /**
     * Cleans earlier shares, then writes the card for the current servings to the cache.
     * Falls back to text only if the image cannot be created.
     */
    suspend fun prepare(context: Context, recipe: Recipe, servings: Int,
                        cover: suspend (String) -> Bitmap? = { loadCover(context, it) }): ShareContent {
        val text = RecipeShareText.build(recipe, servings)
        val title = recipe.title.trim()
        val uri = try {
            val coverBitmap = recipe.imageUrl.takeIf { it.isNotBlank() }?.let { cover(it) }
            withContext(Dispatchers.Default) {
                val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
                directory.listFiles()?.forEach { it.delete() }
                val card = RecipeCardRenderer.render(recipe, servings, coverBitmap)
                try {
                    val file = File(directory, "rezept-${System.currentTimeMillis()}.png")
                    file.outputStream().use { check(card.compress(Bitmap.CompressFormat.PNG, 100, it)) { "PNG konnte nicht geschrieben werden." } }
                    FileProvider.getUriForFile(context, authority(context), file)
                } finally { card.recycle() }
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            // Sharing still works as text; keep the cause for debugging.
            android.util.Log.w("RecipeSharer", "Rezeptkarte konnte nicht erstellt werden", e)
            null
        }
        return ShareContent(uri, text, title)
    }

    /**
     * Image plus text for chat apps (the text becomes the caption where the app supports it).
     * Apps that only take text are offered the same text through the alternate intent.
     */
    fun launch(context: Context, content: ShareContent) {
        fun textIntent() = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content.text)
            putExtra(Intent.EXTRA_SUBJECT, content.title)
        }
        val send = if (content.imageUri == null) textIntent() else Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, content.imageUri)
            putExtra(Intent.EXTRA_TEXT, content.text)
            putExtra(Intent.EXTRA_SUBJECT, content.title)
            clipData = ClipData.newRawUri(content.title, content.imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Rezept teilen").apply {
            if (content.imageUri != null) {
                putExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayOf(textIntent()))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun share(context: Context, recipe: Recipe, servings: Int) {
        launch(context, prepare(context, recipe, servings))
    }
}
