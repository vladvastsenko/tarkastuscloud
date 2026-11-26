package fi.vladi.valvontapoytakirja

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.vladi.tarkastuspoytakirja.AppLanguage

@Composable
internal fun PremiumRegistrationScreen(onContinue: () -> Unit, onBack: () -> Unit, onFree: () -> Unit) {
    BackHandler(onBack = onBack)
    val terms = premiumTerms(AppLanguage.selected(LocalContext.current))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tarkastus Cloud", style = MaterialTheme.typography.headlineLarge)
        Text("Premium", style = MaterialTheme.typography.headlineMedium)
        Text(terms.first)
        Text(stringResource(R.string.cloud_sync))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(terms.second) }
        OutlinedButton(onClick = onFree, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.guest_continue)) }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}
