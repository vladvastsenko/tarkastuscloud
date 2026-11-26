package fi.vladi.valvontapoytakirja

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import fi.vladi.tarkastuspoytakirja.storage.SharedDocumentsArchive
import java.io.File

internal fun exportCustomPdf(context: Context, store: CustomStore, report: CustomDocument) {
    val pdf = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 12f }
    var pageNumber = 0
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(595,842,++pageNumber).create())
    var y = 44f
    fun finish() { paint.textSize = 10f; page.canvas.drawText(pageNumber.toString(), 550f, 815f, paint); pdf.finishPage(page) }
    fun space(height: Float) { if (y + height > 790f) { finish(); page = pdf.startPage(PdfDocument.PageInfo.Builder(595,842,++pageNumber).create()); y = 44f } }
    fun line(text: String, bold: Boolean = false) {
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.textSize = if (bold) 15f else 12f
        for (paragraph in text.split("\n")) {
            var rest = paragraph
            do {
                paint.textSize = if (bold) 15f else 12f
                val n = if (rest.isEmpty()) 0 else paint.breakText(rest,true,510f,null).coerceAtLeast(1)
                val cut = if (n < rest.length) rest.take(n).lastIndexOf(' ').takeIf { it > 0 } ?: n else n
                space(22f); paint.textSize = if (bold) 15f else 12f
                page.canvas.drawText(rest.take(cut),42f,y,paint); y += 20f
                rest = rest.drop(cut).trimStart()
            } while (rest.isNotEmpty())
        }
    }
    try {
        line(report.template.title,true)
        for (s in report.template.sections) {
            val fields = s.fields.filter { f -> f.kind == CustomKind.HEADING || (report.values[f.id] ?: f.defaultValue).isNotBlank() || !report.attachments[f.id].isNullOrEmpty() }
            if (fields.isEmpty()) continue
            if (s.title.isNotBlank()) line(s.title,true)
            for (f in fields) {
                line(f.label,true)
                val value = report.values[f.id] ?: f.defaultValue
                if (f.kind != CustomKind.HEADING && value.isNotBlank()) line(value)
                for (name in report.attachments[f.id].orEmpty()) {
                    val file = store.image(name)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path,bounds)
                    var sample = 1
                    while (bounds.outWidth/sample > 1600 || bounds.outHeight/sample > 1600) sample *= 2
                    val bitmap = BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("Unreadable image")
                    try { val scale = minOf(510f/bitmap.width,300f/bitmap.height,1f); val w = bitmap.width*scale; val height = bitmap.height*scale; space(height+16); page.canvas.drawBitmap(bitmap,null,RectF(42f,y,42f+w,y+height),paint); y += height+16 } finally { bitmap.recycle() }
                }
            }
        }
        finish()
        val file = File(context.cacheDir,"custom_"+report.id+".pdf")
        try { file.outputStream().use { pdf.writeTo(it) }; check(SharedDocumentsArchive.archiveFile(context,file,"Raportit/PDF",file.name,"application/pdf") != null) } finally { file.delete() }
    } finally { pdf.close() }
}
