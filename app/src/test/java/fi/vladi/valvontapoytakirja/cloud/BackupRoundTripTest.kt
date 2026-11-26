package fi.vladi.valvontapoytakirja.cloud
import fi.vladi.tarkastuspoytakirja.model.*
import fi.vladi.valvontapoytakirja.CustomDocument
import fi.vladi.valvontapoytakirja.CustomTemplate
import fi.vladi.valvontapoytakirja.CustomSection
import fi.vladi.valvontapoytakirja.CustomField
import fi.vladi.valvontapoytakirja.CustomKind
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupRoundTripTest {
    @Test fun roundTripAllReportModelsAndImages() {
        val base = Files.createTempDirectory("backup-roundtrip").toFile()
        try {
            val root = File(base,"source").apply { mkdirs() }
            val restored = File(base,"restored").apply { mkdirs() }
            fun file(path: String) = File(root,path).apply { parentFile?.mkdirs() }
            val jpeg = file("tarkastus_reports/test/photos/photo.jpg")
            val png = file("settings/signature.png")
            val image = Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
            image.setPixel(5,5,0xff123456.toInt())
            jpeg.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.JPEG,90,it)) }
            png.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it)) }
            val settings = AppSettings(inspectorName="Inspector",signatureImagePath=png.path,headerText="Header")
            file("app_settings.json").writeText(Json.encodeToString(AppSettings.serializer(),settings))
            val reports = ReportTypes.ids.map { type ->
                InspectionReport(id=type,reportType=type,siteName="Site",signatureImagePath=png.path,
                    observations=listOf(InspectionObservation(description="Observation",photos=listOf(ReportPhoto(filePath=jpeg.path)))))
            }
            assertEquals(7,reports.size)
            fun repository(dir: File): fi.vladi.tarkastuspoytakirja.data.ReportRepository {
                val baseContext = org.robolectric.RuntimeEnvironment.getApplication()
                val context = object : android.content.ContextWrapper(baseContext) {
                    override fun getFilesDir(): File = dir
                }
                return fi.vladi.tarkastuspoytakirja.data.ReportRepository(context)
            }
            val sourceRepository = repository(root)
            reports.forEach { sourceRepository.save(it) }
            assertEquals(reports.toSet(),sourceRepository.listReports().toSet())
            val field = CustomField(id="photo",kind=CustomKind.PHOTOS)
            val doc = CustomDocument(template=CustomTemplate(sections=listOf(CustomSection(fields=listOf(field)))),attachments=mapOf("photo" to listOf("photo.image")))
            jpeg.copyTo(file("custom_reports/photo.image"))
            fi.vladi.valvontapoytakirja.CustomStore(File(root,"custom_reports")).saveDocument(doc)
            val archive = File(base,"backup.zip")
            writeBackupArchive(root,archive)
            extractBackup(archive.inputStream(),restored)
            root.walkTopDown().filter { it.isFile }.forEach { original ->
                assertArrayEquals(original.readBytes(),File(restored,original.relativeTo(root).path).readBytes())
            }
            assertEquals(reports.toSet(),repository(restored).listReports().toSet())
            assertEquals(settings,Json.decodeFromString(AppSettings.serializer(),File(restored,"app_settings.json").readText()))
            assertEquals(doc,fi.vladi.valvontapoytakirja.CustomStore(File(restored,"custom_reports")).load().documents.single())
            for (original in listOf(jpeg,png)) {
                val decoded = BitmapFactory.decodeFile(File(restored,original.relativeTo(root).path).path) ?: error("Image decode failed")
                assertEquals(32,decoded.width); assertEquals(24,decoded.height)
            }
        } finally { base.deleteRecursively() }
    }
}
