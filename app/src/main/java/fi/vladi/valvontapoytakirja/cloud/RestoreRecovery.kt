package fi.vladi.valvontapoytakirja.cloud
import java.io.File

/** Before accessing Context.filesDir, which may create an empty directory. */
internal fun recoverInterruptedRestore(current: File) {
    val previous = File(current.parentFile, "tarkastus_restore_previous")
    val staging = File(current.parentFile, "tarkastus_restore_staging")
    if (previous.exists()) {
        if (!current.exists()) {
            check(previous.renameTo(current)) { "Original data retained in recovery directory" }
        } else {
            // Ambiguous commit: preserve both versions rather than silently deleting either.
            error("Both restored and original data exist")
        }
    }
    if (staging.exists()) {
        check(current.isDirectory) { "No original data; staged data retained" }
        check(staging.deleteRecursively()) { "Cannot remove incomplete staging" }
    }
}
