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
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import java.nio.file.Files

class BackupRoundTripTest {
    @Test fun roundTripAllReportModelsAndImages() {
        val base = Files.createTempDirectory("backup-roundtrip").toFile()
        try {
            val root = File(base,"source").apply { mkdirs() }
            val restored = File(base,"restored").apply { mkdirs() }
            fun file(path: String) = File(root,path).apply { parentFile?.mkdirs() }
            val jpeg = file("tarkastus_reports/test/photos/photo.jpg")
            val png = file("settings/signature.png")
            val image = BufferedImage(32,24,BufferedImage.TYPE_INT_RGB)
            image.setRGB(5,5,0x123456)
            assertTrue(ImageIO.write(image,"jpeg",jpeg))
            assertTrue(ImageIO.write(image,"png",png))
            val settings = AppSettings(inspectorName="Inspector",signatureImagePath=png.path,headerText="Header")
            file("app_settings.json").writeText(Json.encodeToString(AppSettings.serializer(),settings))
            val reports = ReportTypes.ids.map { type ->
                InspectionReport(id=type,reportType=type,siteName="Site",signatureImagePath=png.path,
                    observations=listOf(InspectionObservation(description="Observation",photos=listOf(ReportPhoto(filePath=jpeg.path)))))
            }
            assertEquals(7,reports.size)
            reports.forEach { file("tarkastus_reports/${it.id}.json").writeText(Json.encodeToString(InspectionReport.serializer(),it)) }
            val field = CustomField(id="photo",kind=CustomKind.PHOTOS)
            val doc = CustomDocument(template=CustomTemplate(sections=listOf(CustomSection(fields=listOf(field)))),attachments=mapOf("photo" to listOf("photo.image")))
            jpeg.copyTo(file("custom_reports/photo.image"))
            file("custom_reports/document.json").writeText(Json.encodeToString(CustomDocument.serializer(),doc))
            val archive = File(base,"backup.zip")
            writeBackupArchive(root,archive)
            extractBackup(archive.inputStream(),restored)
            root.walkTopDown().filter { it.isFile }.forEach { original ->
                assertArrayEquals(original.readBytes(),File(restored,original.relativeTo(root).path).readBytes())
            }
            reports.forEach { assertEquals(it,Json.decodeFromString(InspectionReport.serializer(),File(restored,"tarkastus_reports/${it.id}.json").readText())) }
            assertEquals(settings,Json.decodeFromString(AppSettings.serializer(),File(restored,"app_settings.json").readText()))
            assertEquals(doc,Json.decodeFromString(CustomDocument.serializer(),File(restored,"custom_reports/document.json").readText()))
            for (original in listOf(jpeg,png)) {
                val decoded = ImageIO.read(File(restored,original.relativeTo(root).path))
                assertEquals(32,decoded.width); assertEquals(24,decoded.height)
            }
        } finally { base.deleteRecursively() }
    }
}
