package fi.vladi.valvontapoytakirja

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.core.content.FileProvider
import fi.vladi.tarkastuspoytakirja.AppLanguage
import fi.vladi.tarkastuspoytakirja.MainActivity as TarkastusMainActivity
import fi.vladi.valvontapoytakirja.cloud.CloudBackupScreen
import fi.vladi.tarkastuspoytakirja.storage.SharedDocumentsArchive
import fi.vladi.valvontapoytakirja.data.ReportRepository
import fi.vladi.valvontapoytakirja.model.InspectorProfile
import fi.vladi.valvontapoytakirja.model.AppSettings
import fi.vladi.valvontapoytakirja.model.CustomerProfile
import fi.vladi.valvontapoytakirja.model.CustomerSite
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = fi.vladi.tarkastuspoytakirja.TarkastusBlueColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var languageChosen by remember { mutableStateOf(AppLanguage.hasSelection(this@MainActivity)) }
                    if (!languageChosen) {
                        FirstLaunchLanguageScreen { language ->
                            AppLanguage.save(this@MainActivity, language)
                            languageChosen = true
                            recreate()
                        }
                    } else {
                        if (BuildConfig.OFFLINE_PREVIEW) UnifiedLauncher(onSignOut = {})
                        else CloudAuthGate { signOut -> UnifiedLauncher(onSignOut = signOut) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstLaunchLanguageScreen(onContinue: (String) -> Unit) {
    val languages = AppLanguage.options
    var selected by remember { mutableStateOf("en") }
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val pictureWidth = minOf(maxWidth, maxHeight * (2f / 3f))
        val pictureHeight = pictureWidth * 1.5f
        Box(modifier = Modifier.width(pictureWidth).height(pictureHeight)) {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.tarkastuscloud),
                contentDescription = "Tarkastus Cloud — Inspect. Document. Share.",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .width(pictureWidth * 0.78f)
                    .padding(bottom = pictureHeight * 0.035f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text(languages.first { it.first == selected }.second + " ▾")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selected = code
                                expanded = false
                            })
                        }
                    }
                }
                Button(onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth()) {
                    Text("START")
                }
            }
        }
    }
}

@Composable
private fun UnifiedLauncher(onSignOut: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { SharedDocumentsArchive.ensureFolder(context) }
    }
    val consent = remember(context) { if (BuildConfig.OFFLINE_PREVIEW) null else AdConsentManager(context as android.app.Activity) }
    LaunchedEffect(consent) { consent?.request() }
    var showSharedSettings by remember { mutableStateOf(false) }
    var showManagement by remember { mutableStateOf(false) }
    var showCloudBackup by remember { mutableStateOf(false) }
    var showUserGuide by remember { mutableStateOf(false) }

    if (showUserGuide) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) { UserGuideScreen(onBack = { showUserGuide = false }) }
            if ((consent?.canRequestAds == true)) TestAdMobBanner()
        }
    } else if (showCloudBackup) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) { CloudBackupScreen(onBack = { showCloudBackup = false }) }
            if ((consent?.canRequestAds == true)) TestAdMobBanner()
        }
    } else if (showSharedSettings) {
        SharedSettingsApp(onBack = { showSharedSettings = false })
    } else if (showManagement) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                ManagementScreen(
            onSettings = {
                showSharedSettings = true
            },
            onCloudBackup = { showCloudBackup = true },
            onUserGuide = { showUserGuide = true },
            onBack = { showManagement = false },
            onSignOut = onSignOut,
            consent = consent
                )
            }
            if ((consent?.canRequestAds == true)) TestAdMobBanner()
        }
    } else {
        UnifiedStartScreen(
            onTarkastus = {
                context.startActivity(Intent(context, TarkastusMainActivity::class.java))
            },
            onManagement = { showManagement = true },
            showAds = (consent?.canRequestAds == true)
        )
    }
}

@Composable
private fun UnifiedStartScreen(
    onTarkastus: () -> Unit,
    onManagement: () -> Unit,
    showAds: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tarkastus Cloud",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.IconButton(onClick = onManagement) {
                Text("⚙", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.choose_report))
        Button(onClick = onTarkastus, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.report_tarkastus))
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showAds) TestAdMobBanner()
    }
}

@Composable
private fun TestAdMobBanner() {
    val context = LocalContext.current
    val adView = remember(context) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            // Official Google test banner ID. Never request live ads in testing builds.
            // Production unit: test ads only
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ManagementScreen(
    onSettings: () -> Unit,
    onCloudBackup: () -> Unit,
    onUserGuide: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    consent: AdConsentManager?
) {
    val context = LocalContext.current
    val guestMode = BuildConfig.OFFLINE_PREVIEW || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null
    var languageMenu by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_sync), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { languageMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(AppLanguage.label(AppLanguage.selected(context)) + " ▾")
            }
            DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                AppLanguage.options.forEach { (code, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        languageMenu = false
                        if (AppLanguage.selected(context) != code) {
                            AppLanguage.save(context, code)
                            (context as? android.app.Activity)?.recreate()
                        }
                    })
                }
            }
        }
        Text(stringResource(R.string.pdf_language_default), fontWeight = FontWeight.Bold)
        Text(AppLanguage.label(AppLanguage.selected(context)))
        if (!guestMode) BiometricSettingsControl()
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shared_settings))
        }
        if (!guestMode) {
            Button(onClick = onCloudBackup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cloud_sync))
            }
        } else {
            Text(stringResource(R.string.guest_local_notice))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← " + stringResource(R.string.back))
        }
        if (!BuildConfig.OFFLINE_PREVIEW) OutlinedButton(onClick = { if (guestMode) onSignOut() else confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (guestMode) stringResource(R.string.guest_sign_in) else stringResource(R.string.sign_out))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUserGuide, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.user_guide_title))
        }
        if (consent?.privacyOptionsRequired == true) {
            OutlinedButton(onClick = { consent.showPrivacyOptions() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ad_privacy_options))
            }
        }
        if (!guestMode) DeleteAccountControl(onDeleted = onSignOut)
    }
}

@Composable
private fun SharedSettingsApp(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ReportRepository(context) }
    var appSettings by remember { mutableStateOf(repository.loadSettings()) }
    var customers by remember { mutableStateOf(repository.listCustomers()) }
    var inspectors by remember { mutableStateOf(repository.listInspectors()) }
    var sites by remember { mutableStateOf(repository.listSites()) }
    fun saveAppSettings(settings: AppSettings) {
        appSettings = settings
        repository.saveSettings(settings)
    }

    val settingsLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            repository.importSettingsLogo(uri)?.let { logo ->
                saveAppSettings(appSettings.copy(logoPath = logo.absolutePath))
            }
        }
    }

    var pendingInspectorSignature by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    val inspectorSignatureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val pending = pendingInspectorSignature
        pendingInspectorSignature = null
        if (uri != null && pending != null) {
            repository.importInspectorSignature(pending.first, uri)?.let { image ->
                pending.second(image.absolutePath)
            }
        }
    }

    val settingsSignatureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) repository.importSettingsSignature(uri)?.let { signature ->
            saveAppSettings(appSettings.copy(signatureImagePath = signature.absolutePath, signatureStrokes = emptyList()))
        }
    }

        SettingsScreen(
            settings = appSettings,
            customers = customers,
            inspectors = inspectors,
            onSaveInspector = { inspector ->
                repository.saveInspector(inspector)
                inspectors = repository.listInspectors()
            },
            onPickInspectorSignature = { inspector, onImported ->
                pendingInspectorSignature = inspector.id to onImported
                inspectorSignatureLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeleteInspector = { id ->
                repository.deleteInspector(id)
                inspectors = repository.listInspectors()
            },
            sites = sites,
            onSettingsChange = ::saveAppSettings,
            onSaveCustomer = { customer ->
                repository.saveCustomer(customer)
                customers = repository.listCustomers()
            },
            onDeleteCustomer = { id ->
                repository.deleteCustomer(id)
                customers = repository.listCustomers()
                sites = repository.listSites()
            },
            onSaveSite = { site ->
                repository.saveSite(site)
                sites = repository.listSites()
            },
            onDeleteSite = { id ->
                repository.deleteSite(id)
                sites = repository.listSites()
            },
            onPickLogo = {
                settingsLogoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveLogo = { saveAppSettings(appSettings.copy(logoPath = "")) },
            onPickSignature = { settingsSignatureLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemoveSignature = { saveAppSettings(appSettings.copy(signatureImagePath = "")) },
            onBack = onBack
        )

}
