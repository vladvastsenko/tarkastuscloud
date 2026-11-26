package fi.vladi.valvontapoytakirja

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fi.vladi.tarkastuspoytakirja.AppLanguage

private fun <T> List<T>.replaceAt(i: Int, v: T) = toMutableList().apply { this[i] = v }.toList()
private fun <T> List<T>.moveAt(i: Int, delta: Int): List<T> = if (i+delta !in indices) this else toMutableList().apply { add(i+delta, removeAt(i)) }
@Composable
internal fun CustomReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val l = customLabels(AppLanguage.selected(context))
    val store = remember { CustomStore(context) }
    val loaded = remember { runCatching { store.load() } }
    var library by remember { mutableStateOf(loaded.getOrDefault(CustomLibrary())) }
    var template by remember { mutableStateOf<CustomTemplate?>(null) }
    var document by remember { mutableStateOf<CustomDocument?>(null) }
    var message by remember { mutableStateOf(if (loaded.isFailure) l[24] else "") }
    fun persist(next: CustomLibrary) {
        if (loaded.isFailure) { message = l[24]; return }
        runCatching { store.save(next) }.onSuccess { library = next; message = l[23] }.onFailure { message = l[24] }
    }
    fun back() { if (document != null) document = null else if (template != null) template = null else onBack() }
    BackHandler { back() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(l[0], style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = { back() }) { Text(l[16]) }
        if (message.isNotBlank()) Text(message)
        val d = document
        val t = template
        if (d != null) {
            CustomPdfLanguage(d.pdfLanguage.ifBlank { AppLanguage.selected(context) }) { language -> document = document?.copy(pdfLanguage = language) }
            Text(d.template.title, style = MaterialTheme.typography.titleLarge)
            for (section in d.template.sections) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                for (field in section.fields) key(field.id) {
                    val value = d.values[field.id] ?: field.defaultValue
                    CustomValueEditor(field, value, d.attachments[field.id].orEmpty(), l, store,
                        onValue = { v -> document = document?.let { it.copy(values = it.values + (field.id to v)) } },
                        onImages = { v -> document = document?.let { it.copy(attachments = it.attachments + (field.id to v)) } },
                        onError = { message = l[24] })
                }
            }
            fun valid(): Boolean = d.template.sections.flatMap { it.fields }.all { f ->
                !f.required || f.kind == CustomKind.HEADING || if (f.kind in listOf(CustomKind.PHOTOS, CustomKind.SIGNATURE)) !d.attachments[f.id].isNullOrEmpty() else if (f.kind == CustomKind.TABLE) customRows(d.values[f.id] ?: f.defaultValue, customColumns(f).size).any { row -> row.any { it.isNotBlank() } } else (d.values[f.id] ?: f.defaultValue).isNotBlank()
            }
            Button(onClick = { persist(library.copy(documents = library.documents.filterNot { it.id == d.id } + d)) }) { Text(l[14]) }
            Button(onClick = { if (!valid()) message = l[22] else runCatching { exportCustomPdf(context, store, d) }.onSuccess { message = l[23] }.onFailure { message = l[24] } }) { Text(l[15]) }
        } else if (t != null) {
            OutlinedTextField(t.title, { template = t.copy(title = it) }, label = { Text(l[4]) }, modifier = Modifier.fillMaxWidth())
            for ((si, section) in t.sections.withIndex()) key(section.id) {
                fun update(s: CustomSection) { template = t.copy(sections = t.sections.replaceAt(si,s)) }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(section.title, { update(section.copy(title = it)) }, label = { Text(l[5]) })
                    Row { TextButton(onClick = { template = t.copy(sections = t.sections.moveAt(si,-1)) }) { Text("↑") }; TextButton(onClick = { template = t.copy(sections = t.sections.moveAt(si,1)) }) { Text("↓") }; TextButton(onClick = { template = t.copy(sections = t.sections.filterNot { it.id == section.id }) }) { Text(l[17]) } }
                    for ((fi, f) in section.fields.withIndex()) key(f.id) {
                        fun change(v: CustomField) { update(section.copy(fields = section.fields.replaceAt(fi,v))) }
                        OutlinedTextField(f.label, { change(f.copy(label = it)) }, label = { Text(l[8]) })
                        var menu by remember { mutableStateOf(false) }
                        Box { OutlinedButton(onClick = { menu = true }) { Text(l[25+f.kind.ordinal]) }; DropdownMenu(menu,{ menu = false }) { CustomKind.entries.forEach { k -> DropdownMenuItem(text = { Text(l[25+k.ordinal]) }, onClick = { change(f.copy(kind = k)); menu = false }) } } }
                        if (f.kind == CustomKind.PHOTOS) CustomPhotoLayout(f, { change(it) })
                        if (f.kind != CustomKind.HEADING) {
                            Row { Checkbox(f.required,{ change(f.copy(required = it)) }); Text(l[9]) }
                            if (f.kind in listOf(CustomKind.SINGLE,CustomKind.MULTIPLE,CustomKind.TABLE)) OutlinedTextField(f.options.joinToString("\n"), { change(f.copy(options = it.split("\n"))) }, label = { Text(l[10]) })
                            if (f.kind !in listOf(CustomKind.PHOTOS,CustomKind.SIGNATURE,CustomKind.TABLE)) OutlinedTextField(f.defaultValue,{ change(f.copy(defaultValue = it)) },label = { Text(l[11]) })
                        }
                        Row { TextButton(onClick = { update(section.copy(fields = section.fields.moveAt(fi,-1))) }) { Text("↑") }; TextButton(onClick = { update(section.copy(fields = section.fields.moveAt(fi,1))) }) { Text("↓") }; TextButton(onClick = { update(section.copy(fields = section.fields + f.copy(id = customId()))) }) { Text(l[18]) }; TextButton(onClick = { update(section.copy(fields = section.fields.filterNot { it.id == f.id })) }) { Text(l[17]) } }
                        HorizontalDivider()
                    }
                    OutlinedButton(onClick = { update(section.copy(fields = section.fields + CustomField())) }) { Text(l[7]) }
                } }
            }
            OutlinedButton(onClick = { template = t.copy(sections = t.sections + CustomSection()) }) { Text(l[6]) }
            Button(onClick = { persist(library.copy(templates = library.templates.filterNot { it.id == t.id } + t)) }, enabled = t.title.isNotBlank()) { Text(l[12]) }
            Button(onClick = { document = CustomDocument(template = t) }, enabled = t.title.isNotBlank()) { Text(l[13]) }
        } else {
            Button(onClick = { template = CustomTemplate() }) { Text(l[1]) }
            CustomTemplateTransfer(library.templates, { imported -> persist(library.copy(templates = library.templates + imported.map { it.copy(id = customId()) })) }, { message = l[24] }, { message = l[23] })
            Text(l[2], style = MaterialTheme.typography.titleMedium)
            for (item in library.templates) { Text(item.title); Row { TextButton(onClick = { template = item }) { Text(l[12]) }; TextButton(onClick = { document = CustomDocument(template = item) }) { Text(l[13]) }; TextButton(onClick = { template = item.copy(id = customId()) }) { Text(l[18]) } } }
            Text(l[3], style = MaterialTheme.typography.titleMedium)
            for (item in library.documents) OutlinedButton(onClick = { document = item }) { Text(item.template.title) }
        }
    }
}
@Composable
private fun CustomValueEditor(f: CustomField, value: String, images: List<String>, l: List<String>, store: CustomStore, onValue: (String)->Unit, onImages: (List<String>)->Unit, onError: ()->Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching { store.importImage(context,uri) }.onSuccess { onImages(if (f.kind == CustomKind.SIGNATURE) listOf(it) else images + it) }.onFailure { onError() }
    }
    Text(f.label + if (f.required) " *" else "")
    when (f.kind) {
        CustomKind.HEADING -> Unit
        CustomKind.CHECKBOX -> Checkbox(value == "true", { onValue(it.toString()) })
        CustomKind.SINGLE, CustomKind.MULTIPLE -> f.options.filter { it.isNotBlank() }.distinct().forEach { option ->
            val selected = if (f.kind == CustomKind.SINGLE) value == option else option in value.split("\n")
            Row { Checkbox(selected, { checked -> onValue(if (f.kind == CustomKind.SINGLE) { if (checked) option else "" } else { val set = value.split("\n").filter { it.isNotBlank() }.toMutableSet(); if (checked) set.add(option) else set.remove(option); set.joinToString("\n") }) }); Text(option) }
        }
        CustomKind.DATE_TIME -> CustomDateTime(value, onValue)
        CustomKind.TABLE -> CustomTable(f, value, onValue, l)
        CustomKind.SIGNATURE -> CustomSignature(images, store, onImages, onError, l)
        CustomKind.PHOTOS -> CustomPhotos(images, store, onImages, onError, l)
        else -> OutlinedTextField(value,onValue, modifier = Modifier.fillMaxWidth(), singleLine = f.kind !in listOf(CustomKind.LONG_TEXT,CustomKind.TABLE), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (f.kind == CustomKind.NUMBER) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text))
    }
}
