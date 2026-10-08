package de.yummify.app.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import de.yummify.app.data.model.Recipe

/** Draws the share card with plain Canvas calls: no window or Compose host is needed, so it runs on any thread. */
object RecipeCardRenderer {
    const val WIDTH = 1080
    const val MAX_INGREDIENTS = 20

    private const val PAD = 56
    private const val COVER_HEIGHT = 560
    private const val AMOUNT_COLUMN = 250
    private val BACKGROUND = Color.parseColor("#2B2623")
    private val CHIP = Color.parseColor("#3D3632")
    private val TEXT = Color.parseColor("#F7F1EE")
    private val MUTED = Color.parseColor("#A69D97")
    private val ACCENT = Color.parseColor("#F08C65")

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: CharSequence, paint: TextPaint, width: Int, maxLines: Int = Int.MAX_VALUE) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setLineSpacing(0f, 1.08f).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setEllipsize(TextUtils.TruncateAt.END).setMaxLines(maxLines).build()

    /** Rows the card shows; the same list drives measuring and drawing. */
    internal fun ingredientRows(recipe: Recipe, servings: Int): Pair<List<Pair<String, String>>, Int> {
        val multiplier = RecipeShareText.multiplier(recipe, servings)
        val rows = recipe.ingredients.filter { it.name.isNotBlank() }
            .map { RecipeShareText.amountLabel(it, multiplier) to it.name.trim() }
        return rows.take(MAX_INGREDIENTS) to (rows.size - MAX_INGREDIENTS).coerceAtLeast(0)
    }

    fun render(recipe: Recipe, servings: Int, cover: Bitmap?): Bitmap {
        val height = draw(null, recipe, servings, cover)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), recipe, servings, cover)
        return bitmap
    }

    /** Measures when [canvas] is null, draws otherwise. Returns the total height in pixels. */
    private fun draw(canvas: Canvas?, recipe: Recipe, servings: Int, cover: Bitmap?): Int {
        canvas?.drawColor(BACKGROUND)
        var y = 0f
        val contentWidth = WIDTH - 2 * PAD

        if (cover != null) {
            canvas?.let { drawCover(it, cover) }
            y += COVER_HEIGHT
        } else {
            canvas?.drawRect(0f, 0f, WIDTH.toFloat(), 20f, Paint().apply { color = ACCENT })
            y += 20f
        }

        y += 48f
        val title = layout(recipe.title.trim(), paint(68f, TEXT, bold = true), contentWidth, maxLines = 3)
        canvas?.let { it.save(); it.translate(PAD.toFloat(), y); title.draw(it); it.restore() }
        y += title.height + 32f

        // Facts as chips; they wrap to a new row when the line is full.
        val chips = listOfNotNull(
            recipe.cookTimeMinutes?.let { "$it Min." },
            "$servings ${if (servings == 1) "Portion" else "Portionen"}",
            recipe.tags.firstOrNull { it.isNotBlank() && !it.equals("all", true) }
        )
        val chipPaint = paint(34f, TEXT)
        var x = PAD.toFloat()
        val chipHeight = 64f
        chips.forEach { label ->
            val text = TextUtils.ellipsize(label, chipPaint, contentWidth - 56f, TextUtils.TruncateAt.END).toString()
            val chipWidth = chipPaint.measureText(text) + 56f
            if (x + chipWidth > WIDTH - PAD && x > PAD) { x = PAD.toFloat(); y += chipHeight + 16f }
            canvas?.let {
                it.drawRoundRect(RectF(x, y, x + chipWidth, y + chipHeight), chipHeight / 2, chipHeight / 2, Paint().apply { color = CHIP })
                it.drawText(text, x + 28f, y + chipHeight / 2 - (chipPaint.ascent() + chipPaint.descent()) / 2, chipPaint)
            }
            x += chipWidth + 16f
        }
        y += chipHeight + 48f

        val (rows, hidden) = ingredientRows(recipe, servings)
        if (rows.isNotEmpty()) {
            val header = paint(44f, ACCENT, bold = true)
            canvas?.drawText("Zutaten", PAD.toFloat(), y - header.ascent(), header)
            y += header.descent() - header.ascent() + 28f
            val amountPaint = paint(38f, ACCENT, bold = true)
            val namePaint = paint(38f, TEXT)
            rows.forEach { (amount, name) ->
                val nameX = if (rows.any { it.first.isNotEmpty() }) PAD + AMOUNT_COLUMN else PAD
                val nameLayout = layout(name, namePaint, WIDTH - PAD - nameX, maxLines = 2)
                canvas?.let {
                    if (amount.isNotEmpty()) {
                        val fitted = TextUtils.ellipsize(amount, amountPaint, AMOUNT_COLUMN - 24f, TextUtils.TruncateAt.END).toString()
                        it.drawText(fitted, PAD.toFloat(), y + nameLayout.getLineBaseline(0), amountPaint)
                    }
                    it.save(); it.translate(nameX.toFloat(), y); nameLayout.draw(it); it.restore()
                }
                y += nameLayout.height + 18f
            }
            if (hidden > 0) {
                val more = paint(34f, MUTED)
                canvas?.drawText("… und $hidden weitere Zutaten", PAD.toFloat(), y - more.ascent(), more)
                y += more.descent() - more.ascent() + 18f
            }
        }

        y += 36f
        canvas?.drawRect(PAD.toFloat(), y, (WIDTH - PAD).toFloat(), y + 2f, Paint().apply { color = CHIP })
        y += 30f
        val footer = paint(34f, MUTED)
        canvas?.drawText("Yummify", PAD.toFloat(), y - footer.ascent(), footer)
        y += footer.descent() - footer.ascent() + 48f
        return y.toInt()
    }

    /** Center-crops the cover to the header area and fades its lower edge into the card. */
    private fun drawCover(canvas: Canvas, cover: Bitmap) {
        val scale = maxOf(WIDTH.toFloat() / cover.width, COVER_HEIGHT.toFloat() / cover.height)
        val srcWidth = WIDTH / scale
        val srcHeight = COVER_HEIGHT / scale
        val left = (cover.width - srcWidth) / 2
        val top = (cover.height - srcHeight) / 2
        canvas.drawBitmap(
            cover,
            android.graphics.Rect(left.toInt(), top.toInt(), (left + srcWidth).toInt().coerceAtMost(cover.width), (top + srcHeight).toInt().coerceAtMost(cover.height)),
            RectF(0f, 0f, WIDTH.toFloat(), COVER_HEIGHT.toFloat()),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        val fade = android.graphics.LinearGradient(0f, COVER_HEIGHT - 160f, 0f, COVER_HEIGHT.toFloat(), Color.TRANSPARENT, BACKGROUND, android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f, COVER_HEIGHT - 160f, WIDTH.toFloat(), COVER_HEIGHT.toFloat(), Paint().apply { shader = fade })
    }
}
