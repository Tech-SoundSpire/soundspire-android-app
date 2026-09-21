# iOS Google Sign-In - setup

The code is fully wired. Google sign-in stays inert until you supply an **iOS OAuth client id**
and register it in three places. iOS needs its own client id (the web/`GOOGLE_OAUTH_CLIENT_ID`
one will not work for the native SDK).

## 1. Create an iOS OAuth client id

1. Open the [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials)
   for the same project that owns the web client.
2. **Create Credentials → OAuth client ID → Application type: iOS**.
3. Bundle ID: `com.soundspire.ios`.
4. Copy the resulting **iOS client id**: `<PREFIX>.apps.googleusercontent.com`.
   Its **reversed** form is `com.googleusercontent.apps.<PREFIX>`.

## 2. Put the values in the app

- Root `.env` (read by `generateSharedConfig` → `SharedConfig.GOOGLE_IOS_CLIENT_ID`):
  ```
  GOOGLE_IOS_CLIENT_ID=<PREFIX>.apps.googleusercontent.com
  ```
- `ios/project.yml`, `SoundSpire` target `settings.base` (the URL-scheme for the OAuth callback):
  ```yaml
  GOOGLE_REVERSED_CLIENT_ID: "com.googleusercontent.apps.<PREFIX>"
  ```
  Then re-run `xcodegen generate`.

## 3. Let the backend accept the iOS token

The native SDK mints an ID token whose `aud` is the **iOS** client id. The endpoint
`/api/auth/google/mobile` must include that iOS client id in its list of accepted audiences
(alongside the existing web client id) when it calls `verifyIdToken`. Add it there.

## How it flows in the app

`LoginView` Google button → `GoogleSignInHelper.signIn` (native chooser, config from
`SharedConfig`) → ID token → `AuthModel.handleGoogleIdToken` → shared `AuthViewModel` →
`api.googleMobileAuth` → session cookie set → `RootView` routes to onboarding/main.
`SoundSpireApp.onOpenURL` forwards the OAuth callback to `GIDSignIn`.

Until step 1-3 are done, tapping the button shows "Google sign-in is not configured on this build."
