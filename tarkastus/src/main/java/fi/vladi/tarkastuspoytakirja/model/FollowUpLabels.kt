package fi.vladi.tarkastuspoytakirja.model

object FollowUpLabels {
    private val languages = listOf("fi", "en", "sv", "nb", "da", "et", "lv", "lt", "ru")
    val statuses = listOf("", "open", "progress", "resolved", "verified")
    val priorities = listOf("", "low", "normal", "high", "urgent")
    private val labels = mapOf(
        "details" to listOf("Seurantatiedot", "Follow-up details", "Uppföljningsuppgifter", "Oppfølgingsdetaljer", "Opfølgningsoplysninger", "Järelkontrolli andmed", "Pēcpārbaudes dati", "Tolesnės patikros duomenys", "Контроль устранения"),
        "status" to listOf("Tila", "Status", "Status", "Status", "Status", "Olek", "Statuss", "Būsena", "Статус"),
        "priority" to listOf("Kiireellisyys", "Priority", "Prioritet", "Prioritet", "Prioritet", "Prioriteet", "Prioritāte", "Prioritetas", "Приоритет"),
        "responsible" to listOf("Vastuuhenkilö", "Responsible person", "Ansvarig", "Ansvarlig", "Ansvarlig", "Vastutaja", "Atbildīgā persona", "Atsakingas asmuo", "Ответственный"),
        "due" to listOf("Korjaamisen määräaika", "Remediation deadline", "Åtgärdsfrist", "Utbedringsfrist", "Udbedringsfrist", "Parandamise tähtaeg", "Novēršanas termiņš", "Šalinimo terminas", "Срок устранения"),
        "recheck" to listOf("Uusintatarkastus", "Reinspection date", "Efterbesiktning", "Ny kontroll", "Efterkontrol", "Järelkontroll", "Atkārtota pārbaude", "Pakartotinė patikra", "Дата повторной проверки"),
        "open" to listOf("Avoin", "Open", "Öppen", "Åpen", "Åben", "Avatud", "Atvērts", "Atvira", "Открыто"),
        "progress" to listOf("Työn alla", "In progress", "Pågående", "Pågår", "I gang", "Töös", "Procesā", "Vykdoma", "В работе"),
        "resolved" to listOf("Korjattu", "Resolved", "Åtgärdad", "Utbedret", "Udbedret", "Parandatud", "Novērsts", "Pašalinta", "Устранено"),
        "verified" to listOf("Tarkastettu", "Verified", "Verifierad", "Verifisert", "Kontrolleret", "Kontrollitud", "Pārbaudīts", "Patikrinta", "Проверено"),
        "low" to listOf("Matala", "Low", "Låg", "Lav", "Lav", "Madal", "Zema", "Žemas", "Низкий"),
        "normal" to listOf("Normaali", "Normal", "Normal", "Normal", "Normal", "Tavaline", "Normāla", "Įprastas", "Обычный"),
        "high" to listOf("Korkea", "High", "Hög", "Høy", "Høj", "Kõrge", "Augsta", "Aukštas", "Высокий"),
        "urgent" to listOf("Kiireellinen", "Urgent", "Brådskande", "Haster", "Haster", "Kiire", "Steidzama", "Skubus", "Срочный"),
        "none" to listOf("Ei valittu", "Not selected", "Inte valt", "Ikke valgt", "Ikke valgt", "Valimata", "Nav izvēlēts", "Nepasirinkta", "Не выбрано"),
        "clear" to listOf("Tyhjennä", "Clear", "Rensa", "Tøm", "Ryd", "Tühjenda", "Notīrīt", "Išvalyti", "Очистить")
    )
    fun text(key: String, language: String): String =
        labels[key]?.get(languages.indexOf(language).takeIf { it >= 0 } ?: 0) ?: key
    fun value(key: String, language: String): String =
        if (key.isBlank()) "" else text(key, language)
    fun date(value: String): String = if (value.isBlank()) "" else
        runCatching { java.time.LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault(value)
}
