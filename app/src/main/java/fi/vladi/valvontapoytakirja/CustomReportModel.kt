package fi.vladi.valvontapoytakirja

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID

internal fun customId() = UUID.randomUUID().toString()
@Serializable
internal enum class CustomKind { TEXT, LONG_TEXT, NUMBER, DATE_TIME, CHECKBOX, SINGLE, MULTIPLE, PHOTOS, SIGNATURE, HEADING, TABLE }
@Serializable
internal data class CustomField(val id: String = customId(), val label: String = "", val kind: CustomKind = CustomKind.TEXT, val required: Boolean = false, val options: List<String> = emptyList(), val defaultValue: String = "", val photoColumns: Int = 2, val photoRows: Int = 2, val separatePhotoPages: Boolean = false)
@Serializable
internal data class CustomSection(val id: String = customId(), val title: String = "", val fields: List<CustomField> = emptyList())
@Serializable
internal data class CustomTemplate(val id: String = customId(), val title: String = "", val sections: List<CustomSection> = listOf(CustomSection()))
@Serializable
internal data class CustomDocument(val id: String = customId(), val template: CustomTemplate = CustomTemplate(), val values: Map<String,String> = emptyMap(), val attachments: Map<String,List<String>> = emptyMap(), val pdfLanguage: String = "")
@Serializable
internal data class CustomLibrary(val templates: List<CustomTemplate> = emptyList(), val documents: List<CustomDocument> = emptyList())
internal class CustomStore(context: Context) {
    private val root = File(context.filesDir, "custom_reports").apply { mkdirs() }
    private val file = AtomicFile(File(root, "library.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(): CustomLibrary = try { json.decodeFromString<CustomLibrary>(file.readFully().decodeToString()) } catch (e: java.io.FileNotFoundException) { if (file.baseFile.exists()) throw e else CustomLibrary() }
    fun save(value: CustomLibrary) {
        val out = file.startWrite()
        try { out.write(json.encodeToString(value).encodeToByteArray()); file.finishWrite(out) }
        catch (e: Exception) { file.failWrite(out); throw e }
    }
    // Called only on opening the editor, before any new attachments are created.
    // Retain recent unreferenced files to tolerate interrupted imports or recovery.
    fun cleanUnused(value: CustomLibrary) {
        val used = value.documents.flatMap { it.attachments.values.flatten() }.toSet()
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        root.listFiles()?.forEach { candidate ->
            if (candidate.isFile && candidate.name.matches(Regex("[0-9a-fA-F-]{36}\\.(image|png)")) &&
                candidate.name !in used && candidate.lastModified() in 1 until cutoff &&
                candidate.canonicalFile.parentFile == root.canonicalFile) candidate.delete()
        }
    }
    fun importImage(context: Context, uri: android.net.Uri): String {
        val target = File(root, customId() + ".image")
        try { context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { val buffer = ByteArray(8192); var total = 0L; while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 50L * 1024 * 1024); it.write(buffer,0,n) } } } ?: error("Cannot read image") }
        catch (e: Exception) { target.delete(); throw e }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(target.path,bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { target.delete(); error("Invalid image") }
        return target.name
    }
    fun saveBitmap(bitmap: android.graphics.Bitmap): String {
        val target = File(root, customId() + ".png")
        try { target.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } }
        catch (e: Exception) { target.delete(); throw e }
        return target.name
    }
    fun image(name: String): File {
        require(name.matches(Regex("[0-9a-fA-F-]{36}\\.(image|png)"))) { "Invalid attachment name" }
        return File(root,name)
    }
}
