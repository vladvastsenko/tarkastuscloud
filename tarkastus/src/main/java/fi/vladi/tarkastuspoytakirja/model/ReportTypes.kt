package fi.vladi.tarkastuspoytakirja.model

/** Stable preset IDs; user-entered titles are never translated. */
object ReportTypes {
    val ids = listOf("inspection", "defects", "handover", "warranty", "visit", "supervision", "custom")
    private val languages = listOf("fi", "en", "sv", "nb", "da", "et", "lv", "lt", "ru")
    private val titles = mapOf(
        "inspection" to listOf("Tarkastuspöytäkirja", "Inspection report", "Besiktningsprotokoll", "Befaringsrapport", "Inspektionsrapport", "Ülevaatusakt", "Pārbaudes akts", "Patikros aktas", "Протокол осмотра"),
        "defects" to listOf("Virhe- ja puuteluettelo", "Defects and deficiencies list", "Fel- och bristlista", "Feil- og mangelliste", "Fejl- og mangelliste", "Puuduste nimekiri", "Defektu saraksts", "Defektų sąrašas", "Перечень дефектов"),
        "handover" to listOf("Vastaanottotarkastus", "Handover inspection", "Slutbesiktning", "Overtakelsesbefaring", "Afleveringsforretning", "Vastuvõtuakt", "Pieņemšanas pārbaude", "Priėmimo patikra", "Приемочная проверка"),
        "warranty" to listOf("Takuutarkastus", "Warranty inspection", "Garantibesiktning", "Garantibefaring", "Garantieftersyn", "Garantiikontroll", "Garantijas pārbaude", "Garantinė patikra", "Гарантийная проверка"),
        "visit" to listOf("Työmaakäyntiraportti", "Site visit report", "Byggplatsbesöksrapport", "Byggeplassrapport", "Byggepladsrapport", "Objekti külastuse aruanne", "Objekta apmeklējuma pārskats", "Objekto vizito ataskaita", "Отчет о посещении объекта"),
        "supervision" to listOf("Valvontapöytäkirja", "Supervision report", "Kontrollprotokoll", "Kontrollrapport", "Kontrolrapport", "Järelevalveakt", "Uzraudzības protokols", "Priežiūros protokolas", "Протокол строительного контроля"),
        "custom" to listOf("Oma raportti", "Custom report", "Egen rapport", "Egen rapport", "Egen rapport", "Oma aruanne", "Pielāgots pārskats", "Individuali ataskaita", "Собственный отчет")
    )
    fun title(id: String, language: String): String =
        (titles[id] ?: titles.getValue("inspection"))[languages.indexOf(language).takeIf { it >= 0 } ?: 0]
    fun title(report: InspectionReport, language: String): String =
        report.documentTitle.trim().ifBlank { title(report.reportType, language) }
}
