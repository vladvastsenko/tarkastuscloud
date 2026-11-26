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
internal data class CustomField(val id: String = customId(), val label: String = "", val kind: CustomKind = CustomKind.TEXT, val required: Boolean = false, val options: List<String> = emptyList(), val defaultValue: String = "")
@Serializable
internal data class CustomSection(val id: String = customId(), val title: String = "", val fields: List<CustomField> = emptyList())
@Serializable
internal data class CustomTemplate(val id: String = customId(), val title: String = "", val sections: List<CustomSection> = listOf(CustomSection()))
@Serializable
internal data class CustomDocument(val id: String = customId(), val template: CustomTemplate = CustomTemplate(), val values: Map<String,String> = emptyMap(), val attachments: Map<String,List<String>> = emptyMap())
@Serializable
internal data class CustomLibrary(val templates: List<CustomTemplate> = emptyList(), val documents: List<CustomDocument> = emptyList())
internal class CustomStore(context: Context) {
    private val root = File(context.filesDir, "custom_reports").apply { mkdirs() }
    private val file = AtomicFile(File(root, "library.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(): CustomLibrary = if (file.baseFile.exists()) json.decodeFromString<CustomLibrary>(file.readFully().decodeToString()) else CustomLibrary()
    fun save(value: CustomLibrary) {
        val out = file.startWrite()
        try { out.write(json.encodeToString(value).encodeToByteArray()); file.finishWrite(out) }
        catch (e: Exception) { file.failWrite(out); throw e }
    }
    fun importImage(context: Context, uri: android.net.Uri): String {
        val target = File(root, customId() + ".image")
        try { context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: error("Cannot read image") }
        catch (e: Exception) { target.delete(); throw e }
        return target.name
    }
    fun saveBitmap(bitmap: android.graphics.Bitmap): String {
        val target = File(root, customId() + ".png")
        try { target.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } }
        catch (e: Exception) { target.delete(); throw e }
        return target.name
    }
    fun image(name: String): File = File(root, File(name).name)
}
