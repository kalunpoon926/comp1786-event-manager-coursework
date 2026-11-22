# COMP1786 Event Manager coursework extension

This repository contains the Java Android Admin app, a Cloudflare Workers + D1 API, and a .NET MAUI User app. The API is deployed at https://comp1786-event-cloud-api.comp1786-event-cloud.workers.dev.

Start with [TEAM_HANDOFF_SETUP_GUIDE.md](TEAM_HANDOFF_SETUP_GUIDE.md) for prerequisites, builds, Cloudflare access, deployment, and the end-to-end checklist.

The Admin upload token is held separately by the project owner and is not in this repository. The provided APKs are debug builds; they are not included in Git.

Current verification: Worker integration test passed on the deployed API; both Android apps built and launched on an emulator; Admin upload was visible in MAUI, favourites survived a restart, and Admin deletion removed the test event from cloud. Older SQLite migration and full screen-by-screen testing remain to be checked.
