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
    fun table(f: CustomField, value: String) {
        val columns = customColumns(f)
        val rows = customRows(value,columns.size).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return
        // Split wide tables into readable groups of at most four columns.
        for (start in columns.indices step 4) {
            val end = minOf(start+4,columns.size)
            val width = 510f/(end-start)
            fun wrapCell(text: String): List<String> {
                paint.textSize = 10f
                return text.split("\n").flatMap { paragraph ->
                    val lines = mutableListOf<String>(); var rest = paragraph
                    do { val n = if (rest.isEmpty()) 0 else paint.breakText(rest,true,width-12f,null).coerceAtLeast(1); lines.add(rest.take(n)); rest = rest.drop(n) } while (rest.isNotEmpty())
                    lines
                }
            }
            fun drawRow(cells: List<List<String>>, offset: Int, count: Int) {
                val height = count*14f+10f
                cells.forEachIndexed { i,lines ->
                    val x = 42f+i*width
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.6f
                    page.canvas.drawRect(x,y,x+width,y+height,paint)
                    paint.style = Paint.Style.FILL; paint.textSize = 10f
                    lines.drop(offset).take(count).forEachIndexed { j,t -> page.canvas.drawText(t,x+6,y+14+j*14,paint) }
                }
                y += height
            }
            val header = columns.subList(start,end).map { wrapCell(it) }
            val headerCount = minOf(header.maxOf { it.size }, 8)
            fun heading() { space(headerCount*14f+10f); drawRow(header,0,headerCount) }
            heading()
            for (row in rows) {
                val cells = row.subList(start,end).map { wrapCell(it) }
                val total = cells.maxOf { it.size }
                var offset = 0
                while (offset < total) {
                    if (y+24 > 790f) { space(24f); heading() }
                    val count = minOf(total-offset,((790f-y-10f)/14f).toInt().coerceAtLeast(1))
                    drawRow(cells,offset,count); offset += count
                }
            }
            y += 12f
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
                if (f.kind == CustomKind.TABLE) table(f,value) else if (f.kind != CustomKind.HEADING && value.isNotBlank()) line(value)
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
