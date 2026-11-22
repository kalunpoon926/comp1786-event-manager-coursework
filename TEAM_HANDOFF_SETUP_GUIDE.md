# Event Manager — teammate setup and handoff

Updated 22 November 2026. This guide covers the **current** Java Android Admin app, Cloudflare Workers + D1 API, and .NET MAUI User app. The older Node/Vercel, Supabase, and Capacitor prototypes are outside this handoff.

## 1. What you are receiving

| Component | Repository folder | Purpose |
|---|---|---|
| Admin | `EventManagerAdmin/` | Native Java Android app. SQLite stores events and sessions; an authenticated snapshot syncs them to the cloud. |
| API | `EventCloudflare/` | Cloudflare Worker plus D1 schema. Public read endpoint for the User app; protected upload endpoint for Admin. |
| User | `EventUserMaui/` | .NET MAUI app for Upcoming events, name/date search, details and persistent favourites. |

GitHub repository: `https://github.com/kalunpoon926/comp1786-event-manager-coursework` (private; the owner must invite each teammate).

Existing cloud URL: `https://comp1786-event-cloud-api.comp1786-event-cloud.workers.dev`  
Cloudflare account ID in `EventCloudflare/wrangler.jsonc`: `2787e3c9a306621ae7798dcdfa2d5562`  
D1 database: `comp1786-event-manager-cw` (`c6d30830-d5d1-43d6-9bea-04f5d826c53d`)

**Credentials:** The Admin upload token is *not* in Git, source ZIPs or APKs. The owner has `Cloud-upload-token-PRIVATE-DO-NOT-SUBMIT.txt` separately. Ask the owner for it through a private channel only if you need Admin upload access. Never put it in a commit, issue, screenshot or coursework submission.

## 2. Owner: grant the right access

1. Invite the teammate to the private GitHub repository: **Settings → Collaborators → Add people**. They must accept the invitation before cloning. [GitHub instructions](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository).
2. If they will maintain or deploy the Worker, invite their own Cloudflare account under **Members** with access to this Worker and D1 database. Do not share your Cloudflare password or browser session. [Cloudflare member instructions](https://developers.cloudflare.com/fundamentals/manage-members/manage/).
3. If they only review code or build the User app, GitHub access and the public API URL are enough. Give the Admin upload token only to someone who must upload data.

## 3. Teammate: clone and check prerequisites

1. Clone the repository after accepting the invite:

   ```bash
   git clone https://github.com/kalunpoon926/comp1786-event-manager-coursework.git
   cd comp1786-event-manager-coursework
   ```

2. Install **Android Studio** with Android SDK platform **36 / API 36.1**, build tools, emulator or a USB-debugging device, and a **JDK 21**. The Admin project uses Gradle 9.5 and Android Gradle Plugin 9.3.3; use its included `gradlew` instead of a system Gradle. Android Studio can manage the SDK packages. [Android SDK tools](https://developer.android.com/tools).
3. Install **.NET SDK 10** and the **MAUI Android workload**. Check with `dotnet --version` and `dotnet workload list`; install the workload with `dotnet workload install maui-android` if missing. The tested SDK was 10.0.404. [Microsoft installation guide](https://learn.microsoft.com/en-us/dotnet/maui/get-started/installation?view=net-maui-10.0).
4. Install **Node.js 20 or later** for the Worker tools. The project pins Wrangler through `package-lock.json`; use `npm ci`.

## 4. Confirm the existing cloud is healthy

Open the health URL in a browser or run:

```bash
curl -f https://comp1786-event-cloud-api.comp1786-event-cloud.workers.dev/health
curl -f https://comp1786-event-cloud-api.comp1786-event-cloud.workers.dev/api/events
```

Expected health response: `{"ok":true}`. The events endpoint returns `{"events":[]}` until an Admin device uploads an Upcoming event. The User app does not need an upload token.

## 5. Build and install the Admin app

1. Open `EventManagerAdmin/` as a project in Android Studio. Let Gradle sync and install any requested Android SDK components.
2. Build **Build → Build APK(s)**, or run:

   ```bash
   cd EventManagerAdmin
   ./gradlew :app:assembleDebug
   ```

   Output: `EventManagerAdmin/app/build/outputs/apk/debug/app-debug.apk`.
3. Install it through Android Studio or, with `adb` on your path:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Open Admin. Go to **⋮ → Cloud settings**. The existing Worker URL is prefilled. Enter the separately supplied upload token and tap **Save**. This sends the current local snapshot. **⋮ → Upload all to cloud** gives a visible success or error message and can be used to retry.

The Admin database is local to each device. Reinstalling over the same package normally preserves its data; uninstalling the app removes it. A new device has a separate snapshot identity. Avoid uninstalling a device that holds the only copy of unsynced data.

## 6. Build and install the .NET MAUI User app

From the repository root:

```bash
cd EventUserMaui
dotnet restore EventUserMaui.csproj -p:TargetFrameworks=net10.0-android
dotnet build EventUserMaui.csproj -f net10.0-android -p:TargetFrameworks=net10.0-android -c Debug
```

If the Android build cannot find Java or the SDK, set `JAVA_HOME` to a JDK 21 and `ANDROID_HOME` to the Android SDK directory, then retry. The project sets `EmbedAssembliesIntoApk=true` for Android so its debug APK can be installed without Visual Studio fast deployment.

Install `EventUserMaui/bin/Debug/net10.0-android/com.benson.eventuser.maui-Signed.apk` with `adb install -r` or Android Studio. The current cloud URL is prefilled in the app; **Cloud settings → Save server** can change it. Tap **Clear** to refresh all events. Only events whose status is `Upcoming` are listed.

## 7. Local Worker development (separate from production)

```bash
cd EventCloudflare
npm ci
npm run migrate:local
```

Create a local-only `EventCloudflare/.dev.vars` file containing `ADMIN_TOKEN=your-local-test-token`, then run `npm run dev`. The file is gitignored. In a second terminal, test `http://127.0.0.1:8787/health`. To run the integration test against this local Worker, set `CLOUD_TEST_URL=http://127.0.0.1:8787` and `TEST_ADMIN_TOKEN` to the same local token, then run `npm test`. The test uploads and removes its own event/session fixture.

On macOS/Linux, one way to run it is:

```bash
CLOUD_TEST_URL=http://127.0.0.1:8787 TEST_ADMIN_TOKEN=your-local-test-token npm test
```

For the Android emulator, `10.0.2.2` reaches the host computer. Real devices need a reachable HTTPS endpoint. Do not switch the production Admin app to a plain HTTP URL; its Cloud settings accepts HTTPS.

## 8. Deploy Worker changes to the existing account

Only do this if you have Cloudflare access and have reviewed the changes:

```bash
cd EventCloudflare
npm ci
npx wrangler login
npx wrangler whoami
npx wrangler d1 migrations list comp1786-event-manager-cw --remote
npm run migrate:remote
npm run deploy
```

`migrate:remote` applies only unapplied migrations to the **live** D1 database. Review new SQL before running it. The existing initial migration has already been applied. `npm run deploy` updates the Worker at the existing URL. [Wrangler login](https://developers.cloudflare.com/workers/wrangler/commands/general/), [D1 migrations](https://developers.cloudflare.com/d1/wrangler-commands/), [Worker deploy and secrets](https://developers.cloudflare.com/workers/wrangler/commands/workers/).

The `ADMIN_TOKEN` secret is already set on the live Worker. Changing it with `npx wrangler secret put ADMIN_TOKEN` immediately invalidates the token saved in every Admin app; each device must enter the new token. Do not run this command during an ordinary code deployment.

If moving to a **different Cloudflare account**, create a new D1 database and update `account_id` and `database_id` in `wrangler.jsonc`; apply the migration and set a fresh `ADMIN_TOKEN`. Then update the default URL in `EventManagerAdmin/app/src/main/res/values/strings.xml` and `EventUserMaui/EventApiClient.cs`, rebuild both apps, and configure Admin with the new token.

## 9. End-to-end handoff test

Use a unique temporary event name and a valid future date in `DD/MM/YYYY`:

1. In Admin, create an **Upcoming** event with name, date, location, organiser and type. Check the confirmation screen, then save.
2. Open the event's **Sessions** screen; add a session with name, time and type. Use **Upload all to cloud** and check that it reports one event and one session.
3. In MAUI, tap **Clear**. Check the event, session count and **Details** screen. Search using the first few letters of the name, then by the exact date.
4. Tap ☆, open **Favourites**, force-close and reopen the User app, then check the favourite is still there.
5. Edit the event or session in Admin, upload and refresh MAUI. Finally delete the event in Admin and confirm it disappears from MAUI after refresh. Deleting the event should remove its sessions.
6. Check invalid input, duplicate name/date, offline upload error/retry, Admin advanced search, session search and CSV export before claiming full stage 0–6 acceptance.

The last cross-app test verified event upload, User display, favourite persistence after restart and event deletion. The complete screen-by-screen checklist and upgrading an existing older SQLite database still need testing. Record those results in the coursework report rather than claiming them as verified today.

## 10. Troubleshooting

| Symptom | Check |
|---|---|
| User app shows no events | Upload at least one **Upcoming** event from Admin; tap **Clear** in User. Check `/health` and `/api/events`. |
| Admin reports HTTP 401 | Re-enter the current private upload token under **Cloud settings**. |
| Admin reports no network | Restore connectivity, then use **Upload all to cloud**. Local SQLite data remains on the device. |
| MAUI APK opens then exits | Rebuild with `EmbedAssembliesIntoApk=true` (already in the project) and install the `-Signed.apk`, not a fast-deployment-only package. |
| Wrangler deploy targets the wrong account | Check `npx wrangler whoami` and the `account_id` in `wrangler.jsonc` before deploying. |
| Git clone says repository not found | Accept the private repository collaborator invitation and sign in to the correct GitHub account. |

## 11. Files to exclude from handoff commits

Never commit `Cloud-upload-token-PRIVATE-DO-NOT-SUBMIT.txt`, `.dev.vars`, `.env`, `node_modules/`, `.wrangler/`, Gradle `.gradle/` or `build/`, MAUI `bin/` or `obj/`, Android `local.properties`, debug APKs, or personal Cloudflare/GitHub credentials. The Git repository's root `.gitignore` covers these generated and secret files.
