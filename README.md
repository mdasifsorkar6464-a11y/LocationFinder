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


## Private Owner Dashboard
The private dashboard is hosted at `/owner/` after Firebase Hosting is deployed. It only accepts the verified owner account `mdasifsorkar64@gmail.com`. The server checks the signed-in Firebase ID token; hiding the page or checking an email in browser code is not treated as authorization.

The dashboard currently shows:
- Firebase Authentication user count and each account's display name, email, verification, disabled status, and last sign-in.
- Pending invitation count.
- Sessions active only when both participants and both consent flags are present.
- Owner actions to block or unblock a user account. The Owner account cannot be blocked from this dashboard.
- Audit entries for dashboard access and block/unblock actions.

It deliberately does not show personal live-location coordinates or collect phone/SIM/battery details.

### One-time Firebase deployment
1. In Firebase Console, confirm Email/Password Authentication is enabled for the project.
2. Verify that `mdasifsorkar64@gmail.com` is the Owner account and that its email is verified.
3. Enable Firebase Hosting and Cloud Functions for the project. Firebase may require upgrading the project to the Blaze billing plan to deploy Cloud Functions; review expected usage and billing before enabling it.
4. Install the Firebase CLI on a computer, sign in with an account that has permission to deploy this Firebase project, then run from the repository root:
   ```sh
   npm install -g firebase-tools
   firebase login
   firebase deploy --only functions,hosting,database
   ```
5. Open `https://locationfinder-3df35.web.app/owner/` and sign in with the verified Owner Gmail.

The source code is committed to the development branch, but this does not deploy services to Firebase automatically. Until the Firebase deployment succeeds, the dashboard URL will not be live. Review and publish the database rules separately only after testing the invitation and two-person consent flow on test accounts.
