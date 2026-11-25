package fi.vladi.tarkastuspoytakirja.model

object MeasurementLabels {
    private val languages = listOf("fi", "en", "sv", "nb", "da", "et", "lv", "lt", "ru")
    private val labels = mapOf(
        "heading" to listOf("Mittaukset", "Measurements", "Mätningar", "Målinger", "Målinger", "Mõõtmised", "Mērījumi", "Matavimai", "Измерения"),
        "parameter" to listOf("Mitattava suure", "Measured parameter", "Mätparameter", "Måleparameter", "Måleparameter", "Mõõdetav parameeter", "Mērāmais parametrs", "Matuojamas parametras", "Измеряемый параметр"),
        "value" to listOf("Mittausarvo", "Measured value", "Mätvärde", "Måleverdi", "Måleværdi", "Mõõteväärtus", "Izmērītā vērtība", "Išmatuota vertė", "Измеренное значение"),
        "unit" to listOf("Yksikkö", "Unit", "Enhet", "Enhet", "Enhed", "Ühik", "Mērvienība", "Matavimo vienetas", "Единица измерения"),
        "limit" to listOf("Sallittu arvo", "Permitted value", "Tillåtet värde", "Tillatt verdi", "Tilladt værdi", "Lubatud väärtus", "Pieļaujamā vērtība", "Leistina vertė", "Допустимое значение"),
        "add" to listOf("Lisää mittaus", "Add measurement", "Lägg till mätning", "Legg til måling", "Tilføj måling", "Lisa mõõtmine", "Pievienot mērījumu", "Pridėti matavimą", "Добавить измерение")
    )
    fun text(key: String, language: String): String =
        labels[key]?.get(languages.indexOf(language).takeIf { it >= 0 } ?: 0) ?: key
}
