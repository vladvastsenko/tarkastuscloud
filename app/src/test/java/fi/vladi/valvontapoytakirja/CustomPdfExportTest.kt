package fi.vladi.valvontapoytakirja

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import fi.vladi.valvontapoytakirja.data.ReportRepository
import fi.vladi.valvontapoytakirja.model.AppSettings
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomPdfExportTest {
    @Test fun exportWithAttachmentsAndSettingsPreservesPreviousPdf() {
        val context = RuntimeEnvironment.getApplication()
        val store = CustomStore(context)
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val image = store.saveBitmap(bitmap)
        bitmap.recycle()
        ReportRepository(context).saveSettings(AppSettings(
            logoPath = store.image(image).absolutePath,
            signatureImagePath = store.image(image).absolutePath,
            headerText = "Company header", footerText = "Company footer", inspectorName = "Inspector"
        ))
        val photo = CustomField(label = "Photo", kind = CustomKind.PHOTOS)
        val signature = CustomField(label = "Signature", kind = CustomKind.SIGNATURE)
        val report = CustomDocument(
            template = CustomTemplate(title = "Export test", sections = listOf(CustomSection(fields = listOf(photo, signature)))),
            attachments = mapOf(photo.id to listOf(image), signature.id to listOf(image)),
            customerText = "Customer", siteText = "Site", pdfLanguage = "en"
        )
        store.saveDocument(report)
        val first = File(requireNotNull(exportCustomPdf(context, store, report).path))
        val bytes = first.readBytes()
        assertTrue(bytes.size > 100)
        assertEquals("%PDF-", bytes.copyOfRange(0,5).decodeToString())
        val second = File(requireNotNull(exportCustomPdf(context, store, report.copy(useSharedSettings = false)).path))
        assertNotEquals(first, second)
        assertArrayEquals(bytes, first.readBytes())
        assertEquals("%PDF-", second.readBytes().copyOfRange(0,5).decodeToString())
    }
}
