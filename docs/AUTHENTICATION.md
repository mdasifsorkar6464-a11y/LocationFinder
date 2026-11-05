# Authentication and token handling

## Components
- Android UI and authentication flow: app/src/main/java/com/asif/locationfinder/MainActivity.kt
- Account provider: Firebase Authentication via FirebaseAuth.
- Session and location data: Firebase Realtime Database SDK, used by LocationService.kt.
- Privileged Owner operations: callable Cloud Functions in functions/index.js using Firebase Admin SDK.

## Sign-in flow
1. Registration calls createUserWithEmailAndPassword(email, password).
2. The app saves the display name to the Firebase Auth profile, sends an email-verification message, then signs out.
3. Sign-in calls signInWithEmailAndPassword, reloads the Firebase user, and checks isEmailVerified before showing the dashboard. Unverified users are signed out.
4. Password recovery calls sendPasswordResetEmail.
5. After sign-in, the app uses currentUser.uid as the account identifier for session membership and location paths.

## Credentials and tokens
- Passwords are sent to Firebase Authentication through its SDK. The app does not write passwords to Realtime Database or local files.
- Firebase SDK manages ID-token issuance, refresh, and authenticated requests. The Android source does not create custom JWTs or manually manage bearer tokens.
- google-services.json contains public client configuration and a Firebase API key; it is not a service-account private key. Database and server rules provide the security boundary.
- The deployed Functions runtime supplies Admin SDK credentials. No service-account private key should be committed to this repository.
- Owner callable functions check request.auth, the verified-email claim, and the configured Owner email before privileged operations.

## Database authorization
database.rules.json denies root access by default and requires a signed-in account with a verified email for app data access. Session membership and each participant's consent are checked by rules; location access is restricted to members of an active session.

Committing database.rules.json does not publish it to Firebase. Publish the rules separately in Firebase Console or with Firebase CLI, then test with two verified accounts on separate devices before real-world location sharing.

Client-side checks are for user experience; Firebase Database Rules and server-side callable-function checks enforce authorization.