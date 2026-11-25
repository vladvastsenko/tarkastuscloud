# Independent Firebase setup
Create a new Firebase project (suggested ID: tarkastus-pro, or another available unique ID). Do not reuse tarkastus-cloud or project number 425303240003.
Register Android application fi.vladi.tarkastuspro.
Enable Email/Password and Google authentication. Register SHA-1 and SHA-256 of the separate Pro signing certificate, then download the updated google-services.json to app/google-services.json locally.
Keep this file out of Git. Future CI can supply it via PRO_GOOGLE_SERVICES_JSON repository secret.
The current Google Services Gradle plugin intentionally requires a valid configuration. No dummy configuration is provided.
No APK build is authorized yet.
