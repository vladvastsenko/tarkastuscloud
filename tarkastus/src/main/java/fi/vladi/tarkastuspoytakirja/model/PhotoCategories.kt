package fi.vladi.tarkastuspoytakirja.model

object PhotoCategories {
    val ids = listOf("", "before", "after")
    private val languages = listOf("fi", "en", "sv", "nb", "da", "et", "lv", "lt", "ru")
    private val labels = mapOf(
        "category" to listOf("Kuvan tyyppi", "Photo category", "Fotokategori", "Fotokategori", "Fotokategori", "Foto kategooria", "Foto kategorija", "Nuotraukos kategorija", "Категория фотографии"),
        "" to listOf("Valokuvat", "Photos", "Fotografier", "Bilder", "Fotos", "Fotod", "Fotogrāfijas", "Nuotraukos", "Обычные фотографии"),
        "before" to listOf("Ennen korjausta", "Before remediation", "Före åtgärd", "Før utbedring", "Før udbedring", "Enne parandamist", "Pirms novēršanas", "Prieš šalinimą", "До устранения"),
        "after" to listOf("Korjauksen jälkeen", "After remediation", "Efter åtgärd", "Etter utbedring", "Efter udbedring", "Pärast parandamist", "Pēc novēršanas", "Po šalinimo", "После устранения")
    )
    fun normalize(id: String): String = if (id in ids) id else ""
    fun text(id: String, language: String): String =
        (labels[id] ?: labels.getValue(""))[languages.indexOf(language).takeIf { it >= 0 } ?: 0]
}
