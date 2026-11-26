package fi.vladi.valvontapoytakirja

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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

private var activeCustomWorker: kotlinx.coroutines.Job? = null
private val customSaveScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate)

private fun <T> List<T>.replaceAt(i: Int, v: T) = toMutableList().apply { this[i] = v }.toList()
private fun <T> List<T>.moveAt(i: Int, delta: Int): List<T> = if (i+delta !in indices) this else toMutableList().apply { add(i+delta, removeAt(i)) }
@Composable
internal fun CustomReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val language = AppLanguage.selected(context)
    val l = remember(language) { customLabels(language) }
    val store = remember { CustomStore(context.applicationContext) }
    val loadedState by produceState<Result<Pair<CustomLibrary, String?>>?>(null, store) {
        activeCustomWorker?.join()
        value = withContext(Dispatchers.IO) { runCatching { store.load() to store.activeEditor() } }
    }
    val loaded = loadedState
    if (loaded == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        return
    }
    var library by remember { mutableStateOf(loaded.getOrNull()?.first ?: CustomLibrary()) }
    var template by remember { mutableStateOf(library.templates.firstOrNull { "t:" + it.id == loaded.getOrNull()?.second }) }
    var document by remember { mutableStateOf(library.documents.firstOrNull { "d:" + it.id == loaded.getOrNull()?.second }) }
    var message by remember { mutableStateOf(if (loaded.isFailure) l[24] else "") }
    val scope = rememberCoroutineScope()
    val writer = remember { kotlinx.coroutines.sync.Mutex() }
    var pendingWrites by remember { mutableStateOf(0) }
    val queued = remember { CustomWriteQueue<suspend () -> Unit>() }
    val failedKeys = remember { mutableStateMapOf<String, Boolean>() }
    var worker by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun enqueue(action: () -> Unit, key: String = customId(), done: () -> Unit = {}) {
        queued.put(key) { withContext(Dispatchers.IO) { action() }; failedKeys.remove(key); done() }
        pendingWrites = queued.size + 1
        if (worker?.isActive == true) return
        worker = customSaveScope.launch {
            while (queued.isNotEmpty()) {
                kotlinx.coroutines.delay(120)
                val entry = queued.take() ?: break
                val task = entry.second
                try {
                    writer.lock()
                    try { task() } finally { writer.unlock() }
                } catch (e: Exception) {
                    failedKeys[entry.first] = true
                    message = l[24]
                }
                pendingWrites = queued.size + 1
            }
            pendingWrites = 0
        }
        activeCustomWorker = worker
    }
    fun persist(next: CustomLibrary) {
        if (loaded.isFailure) { message = l[24]; return }
        library = next
        enqueue({ store.save(next) }) { message = l[23] }
    }
    LaunchedEffect(Unit) {
        if (loaded.isSuccess) withContext(Dispatchers.IO) { runCatching { store.cleanUnused(loaded.getOrThrow().first) } }
    }
    // Persist each committed editor change atomically. Do not overwrite unreadable libraries.
    fun updateDocument(next: CustomDocument) {
        if (loaded.isFailure) { message = l[24]; return }
        document = next
        enqueue({ store.saveDocument(next); store.rememberEditor("d:" + next.id) }, key = "document:" + next.id)
    }
    fun updateTemplate(next: CustomTemplate) {
        if (loaded.isFailure) { message = l[24]; return }
        template = next
        enqueue({ store.saveTemplate(next); store.rememberEditor("t:" + next.id) }, key = "template:" + next.id)
    }
    var busy by remember { mutableStateOf(false) }
    var imports by remember { mutableStateOf(0) }
    fun back() {
        if (busy || pendingWrites > 0 || imports > 0) return
        val d = document
        val t = template
        if (d != null) {
            if (loaded.isFailure) { message = l[24]; return }
            val next = library.copy(documents = library.documents.filterNot { it.id == d.id } + d)
            enqueue({ store.saveDocument(d) }, key = "document:" + d.id) { if (document == d) { library = next; document = null; enqueue({ store.rememberEditor("") }) } }
        } else if (t != null) {
            if (t.title.isBlank()) { library = library.copy(templates = library.templates.filterNot { it.id == t.id } + t); template = null; enqueue({ store.rememberEditor("") }); return }
            if (loaded.isFailure) { message = l[24]; return }
            val next = library.copy(templates = library.templates.filterNot { it.id == t.id } + t)
            enqueue({ store.saveTemplate(t) }, key = "template:" + t.id) { if (template == t) { library = next; template = null; enqueue({ store.rememberEditor("") }) } }
        } else onBack()
    }
    BackHandler { back() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(l[0], style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(enabled = !busy && imports == 0 && pendingWrites == 0, onClick = { back() }) { Text(l[16]) }
        if (message.isNotBlank()) Text(message)
        if (pendingWrites > 0) LinearProgressIndicator(Modifier.fillMaxWidth())
        val d = document
        val t = template
        if (d != null) {
            CustomPdfLanguage(d.pdfLanguage.ifBlank { AppLanguage.selected(context) }) { language -> document?.let { updateDocument(it.copy(pdfLanguage = language)) } }
            Text(d.template.title, style = MaterialTheme.typography.titleLarge)
            for (section in d.template.sections) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                for (field in section.fields) key(field.id) {
                    val value = d.values[field.id] ?: field.defaultValue
                    CustomValueEditor(field, value, d.attachments[field.id].orEmpty(), l, store,
                        onValue = { v -> document?.let { updateDocument(it.copy(values = it.values + (field.id to v))) } },
                        onImages = { v -> document?.let { updateDocument(it.copy(attachments = it.attachments + (field.id to v))) } },
                        onError = { message = l[24] }, onImport = { started -> imports += if (started) 1 else -1 })
                }
            }
            fun valid(): Boolean = d.template.sections.flatMap { it.fields }.all { f ->
                !f.required || f.kind == CustomKind.HEADING || if (f.kind in listOf(CustomKind.PHOTOS, CustomKind.SIGNATURE)) !d.attachments[f.id].isNullOrEmpty() else if (f.kind == CustomKind.TABLE) customRows(d.values[f.id] ?: f.defaultValue, customColumns(f).size).any { row -> row.any { it.isNotBlank() } } else (d.values[f.id] ?: f.defaultValue).isNotBlank()
            }
            Button(onClick = { enqueue({ store.saveDocument(d); store.rememberEditor("d:" + d.id) }, key = "document:" + d.id) { message = l[23] } }) { Text(l[14]) }
            Button(enabled = !busy && imports == 0 && pendingWrites == 0 && failedKeys.isEmpty(), onClick = {
                if (!valid()) message = l[22] else {
                    busy = true
                    scope.launch {
                        try { withContext(Dispatchers.IO) { exportCustomPdf(context, store, d) }; message = l[23] }
                        catch (e: Exception) { message = l[24] }
                        finally { busy = false }
                    }
                }
            }) { Text(l[15]) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (t != null) {
            OutlinedTextField(t.title, { updateTemplate(t.copy(title = it)) }, label = { Text(l[4]) }, modifier = Modifier.fillMaxWidth())
            for ((si, section) in t.sections.withIndex()) key(section.id) {
                fun update(s: CustomSection) { updateTemplate(t.copy(sections = t.sections.replaceAt(si,s))) }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(section.title, { update(section.copy(title = it)) }, label = { Text(l[5]) })
                    Row { TextButton(onClick = { updateTemplate(t.copy(sections = t.sections.moveAt(si,-1))) }) { Text("↑") }; TextButton(onClick = { updateTemplate(t.copy(sections = t.sections.moveAt(si,1))) }) { Text("↓") }; TextButton(onClick = { updateTemplate(t.copy(sections = t.sections.filterNot { it.id == section.id })) }) { Text(l[17]) } }
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
            OutlinedButton(onClick = { updateTemplate(t.copy(sections = t.sections + CustomSection())) }) { Text(l[6]) }
            Button(onClick = { persist(library.copy(templates = library.templates.filterNot { it.id == t.id } + t)) }, enabled = t.title.isNotBlank()) { Text(l[12]) }
            Button(onClick = { library = library.copy(templates = library.templates.filterNot { it.id == t.id } + t); template = null; updateDocument(CustomDocument(template = t, pdfLanguage = AppLanguage.selected(context))) }, enabled = t.title.isNotBlank()) { Text(l[13]) }
        } else {
            Button(onClick = { updateTemplate(CustomTemplate()) }) { Text(l[1]) }
            CustomTemplateTransfer(library.templates, { imported -> persist(library.copy(templates = library.templates + imported.map { it.copy(id = customId()) })) }, { message = l[24] }, { message = l[23] })
            Text(l[2], style = MaterialTheme.typography.titleMedium)
            for (item in library.templates) { Text(item.title); Row { TextButton(onClick = { updateTemplate(item) }) { Text(l[12]) }; TextButton(onClick = { updateDocument(CustomDocument(template = item, pdfLanguage = AppLanguage.selected(context))) }) { Text(l[13]) }; TextButton(onClick = { updateTemplate(item.copy(id = customId())) }) { Text(l[18]) } } }
            Text(l[3], style = MaterialTheme.typography.titleMedium)
            for (item in library.documents) OutlinedButton(onClick = { updateDocument(item) }) { Text(item.template.title) }
        }
    }
}
@Composable
private fun CustomValueEditor(f: CustomField, value: String, images: List<String>, l: List<String>, store: CustomStore, onValue: (String)->Unit, onImages: (List<String>)->Unit, onError: ()->Unit, onImport: (Boolean)->Unit) {
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
        CustomKind.PHOTOS -> CustomPhotos(images, store, onImages, onError, l, onImport)
        else -> OutlinedTextField(value,onValue, modifier = Modifier.fillMaxWidth(), singleLine = f.kind !in listOf(CustomKind.LONG_TEXT,CustomKind.TABLE), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (f.kind == CustomKind.NUMBER) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text))
    }
}
