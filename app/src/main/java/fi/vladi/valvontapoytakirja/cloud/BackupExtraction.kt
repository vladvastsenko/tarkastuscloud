package fi.vladi.valvontapoytakirja.cloud
import java.io.*
import java.util.zip.ZipInputStream

internal fun extractBackup(input: InputStream, staging: File, maxEntries: Int = 20000, maxBytes: Long = 2L * 1024 * 1024 * 1024) {
        var manifestFound = false
        val seenEntries = hashSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L

            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    require(entryCount <= maxEntries) { "Varmuuskopiossa on liian monta tiedostoa." }

                    val name = entry.name.replace('\\', '/')
                    require(seenEntries.add(name)) { "Varmuuskopiossa on päällekkäinen tiedostopolku." }
                    require(!name.startsWith("/") && name.split('/').none { it == ".." || it.isBlank() }) {
                        "Varmuuskopiossa on virheellinen tiedostopolku."
                    }

                    if (name == "TARKASTUSCLOUD_BACKUP_INFO.txt") {
                        val manifestBytes = ByteArray(128)
                        var count = 0
                        while (count < manifestBytes.size) {
                            val n = zip.read(manifestBytes, count, manifestBytes.size - count)
                            if (n < 0) break
                            count += n
                        }
                        require(count in 1..127 && zip.read() == -1) { "Varmuuskopion tunnistetiedot ovat virheelliset." }
                        val text = String(manifestBytes, 0, count, Charsets.UTF_8).trim()
                        require(text == "TARKASTUSCLOUD_BACKUP_V1") { "Tiedosto ei ole Tarkastus-varmuuskopio." }
                        manifestFound = true
                    } else if (name.startsWith("files/") && name.length > 6) {
                        val relative = name.removePrefix("files/")
                        val destination = File(staging, relative)
                        val canonicalRoot = staging.canonicalFile
                        val canonicalDestination = destination.canonicalFile
                        require(canonicalDestination.path.startsWith(canonicalRoot.path + File.separator)) {
                            "Varmuuskopiossa on kielletty tiedostopolku."
                        }
                        if (entry.isDirectory) {
                            require(zip.read() == -1) { "Directory entry contains data" }
                            destination.mkdirs()
                        } else {
                            destination.parentFile?.mkdirs()
                            FileOutputStream(destination).buffered().use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= maxBytes) {
                                        "Varmuuskopio on liian suuri palautettavaksi."
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                    } else {
                        require(name == "TARKASTUSCLOUD_BACKUP_INFO.txt") { "Varmuuskopio sisältää tuntemattoman tiedoston." }
                    }
                    zip.closeEntry()
                }
            }
            check(manifestFound) { "Varmuuskopion tunnistetietoja ei löytynyt." }
            check(seenEntries.size > 1) { "Varmuuskopio ei sisällä palautettavia tiedostoja." }


}
