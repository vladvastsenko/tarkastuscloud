package fi.vladi.valvontapoytakirja.cloud
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.*
class BackupIntegrityTest {
    private fun archive(path: String = "files/photo.image"): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, data) in listOf("TARKASTUSCLOUD_BACKUP_INFO.txt" to "TARKASTUSCLOUD_BACKUP_V1".toByteArray(), path to byteArrayOf(1,2,3,4))) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
    private fun staged(block: (java.io.File) -> Unit) {
        val dir = Files.createTempDirectory("backup-integrity").toFile()
        try { block(dir) } finally { dir.deleteRecursively() }
    }
    @Test fun restoresExactBytes() = staged { dir ->
        extractBackup(archive().inputStream(),dir)
        assertArrayEquals(byteArrayOf(1,2,3,4),java.io.File(dir,"photo.image").readBytes())
    }
    @Test fun rejectsTruncatedDirectory() = staged { dir ->
        val bytes = archive()
        assertThrows(Exception::class.java) { extractBackup(bytes.copyOf(bytes.size-22).inputStream(),dir) }
    }
    @Test fun rejectsDotAlias() = staged { dir ->
        assertThrows(Exception::class.java) { extractBackup(archive("files/./photo.image").inputStream(),dir) }
    }
}
