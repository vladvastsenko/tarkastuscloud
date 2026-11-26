package fi.vladi.valvontapoytakirja

internal fun premiumTerms(language: String): Pair<String, String> = when(language) {
    "fi" -> "Rekisteröityminen on Premium-käyttäjille. Tilaus uusiutuu kuukausittain, kunnes se perutaan. Maksu vahvistetaan erikseen Google Playssa. Kaikki editorit ja PDF-tiedostojen luonti ovat maksuttomia ilman rekisteröitymistä." to "Jatka rekisteröitymiseen"
    "ru" -> "Регистрация предназначена для Premium. Подписка продлевается ежемесячно до отмены. Оплата подтверждается отдельно в Google Play. Все редакторы и создание PDF бесплатны без регистрации." to "Продолжить регистрацию"
    "sv" -> "Registrering gäller Premium. Prenumerationen förnyas varje månad tills den sägs upp. Betalning bekräftas separat i Google Play. Alla redigerare och PDF-skapande är gratis utan registrering." to "Fortsätt till registrering"
    "nb" -> "Registrering gjelder Premium. Abonnementet fornyes månedlig til det sies opp. Betaling bekreftes separat i Google Play. Alle redigeringsverktøy og PDF-oppretting er gratis uten registrering." to "Fortsett til registrering"
    "da" -> "Registrering gælder Premium. Abonnementet fornyes månedligt, indtil det opsiges. Betaling bekræftes særskilt i Google Play. Alle redigeringsværktøjer og PDF-oprettelse er gratis uden registrering." to "Fortsæt til registrering"
    "et" -> "Registreerimine on Premiumi jaoks. Tellimus uueneb iga kuu kuni tühistamiseni. Makse kinnitatakse eraldi Google Plays. Kõik redaktorid ja PDF-ide loomine on tasuta ilma registreerimiseta." to "Jätka registreerimist"
    "lv" -> "Reģistrācija paredzēta Premium. Abonements tiek atjaunots katru mēnesi līdz atcelšanai. Maksājums jāapstiprina atsevišķi Google Play. Visi redaktori un PDF izveide ir bez maksas bez reģistrācijas." to "Turpināt reģistrāciju"
    "lt" -> "Registracija skirta Premium. Prenumerata atnaujinama kas mėnesį iki atšaukimo. Mokėjimas atskirai patvirtinamas Google Play. Visi redaktoriai ir PDF kūrimas nemokami be registracijos." to "Tęsti registraciją"
    "fr" -> "L’inscription est destinée à Premium. L’abonnement est renouvelé chaque mois jusqu’à résiliation. Le paiement est confirmé séparément dans Google Play. Tous les éditeurs et la création de PDF restent gratuits sans inscription." to "Continuer l’inscription"
    "es" -> "El registro es para Premium. La suscripción se renueva mensualmente hasta su cancelación. El pago se confirma por separado en Google Play. Todos los editores y la creación de PDF son gratuitos sin registro." to "Continuar el registro"
    "it" -> "La registrazione è per Premium. L’abbonamento si rinnova ogni mese fino alla disdetta. Il pagamento va confermato separatamente su Google Play. Tutti gli editor e la creazione di PDF sono gratuiti senza registrazione." to "Continua la registrazione"
    "de" -> "Die Registrierung ist für Premium vorgesehen. Das Abo verlängert sich monatlich bis zur Kündigung. Die Zahlung wird separat in Google Play bestätigt. Alle Editoren und die PDF-Erstellung sind ohne Registrierung kostenlos." to "Weiter zur Registrierung"
    "pl" -> "Rejestracja jest przeznaczona dla Premium. Subskrypcja odnawia się co miesiąc do anulowania. Płatność potwierdza się osobno w Google Play. Wszystkie edytory i tworzenie PDF są bezpłatne bez rejestracji." to "Kontynuuj rejestrację"
    else -> "Registration is for Premium. Subscription renews monthly until cancelled. Payment requires separate confirmation in Google Play. All editors and PDF creation remain free without registration." to "Continue to registration"
}
