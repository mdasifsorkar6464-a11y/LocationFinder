# Location Finder

Android location-sharing app with invitation-based pairing and explicit, separate consent from both participants.

## Privacy and consent
- Sharing an invitation code does not start location sharing.
- The invited person must review the prompt and explicitly accept.
- The inviter must separately grant consent.
- Location updates are enabled only while the session is active and both consent flags are true.
- Either participant can withdraw their own consent; either can stop the whole session.
- Realtime Database rules are provided in `database.rules.json`.

## Firebase setup required before real use
1. Open Firebase Console for project `locationfinder-3df35`.
2. Open Realtime Database → Rules.
3. Replace the current rules with the contents of `database.rules.json` and publish them.
4. Confirm Email/Password sign-in is enabled in Firebase Authentication.
5. Test with two separate accounts and two devices. Do not use real location sharing until the rules are published and tested.

The rules file is versioned in this repository; adding it to GitHub does **not** automatically publish it to Firebase.

## Build
GitHub Actions builds a debug APK on pushes to `main` and `fix/build-and-consent-flow`. Open the Actions tab, choose the successful “Build Location Finder APK” run, and download the `LocationFinder-APK` artifact.

## Notes
The debug APK is for testing. It is not a Play Store release build.
