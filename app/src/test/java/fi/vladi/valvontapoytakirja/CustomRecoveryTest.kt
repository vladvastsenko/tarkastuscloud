package fi.vladi.valvontapoytakirja
import android.util.AtomicFile
import java.io.File
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CustomRecoveryTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun interruptedReplacementPreservesCommittedDocument() {
        val root = folder.newFolder()
        val store = CustomStore(root)
        val doc = CustomDocument(values = mapOf("field" to "saved text"))
        store.saveDocument(doc)
        store.rememberEditor("d:" + doc.id)
        val record = File(root,"records").listFiles()!!.single { it.name.startsWith("d-") }
        val interrupted = AtomicFile(record).startWrite()
        interrupted.write("partial".toByteArray())
        interrupted.close()
        val reopened = CustomStore(root)
        assertEquals(doc,reopened.load().documents.single())
        assertEquals("d:" + doc.id,reopened.activeEditor())
    }
    @Test fun missingMigrationMarkerDoesNotOverwriteNewerRecord() {
        val root = folder.newFolder()
        val store = CustomStore(root)
        val doc = CustomDocument(values = mapOf("field" to "new"))
        store.saveDocument(doc)
        store.load()
        File(root,"records/ready").delete()
        assertEquals(doc,CustomStore(root).load().documents.single())
    }
}
