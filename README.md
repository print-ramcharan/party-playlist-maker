# Party Playlist Maker

Party Playlist Maker is an Android application that allows you and your friends to collaboratively create, manage, and vote for songs in real time, making any party or gathering musically unforgettable. The app integrates with the [Spotify Web API](https://developer.spotify.com/documentation/web-api/) to stream music, search for tracks, and manage playlists directly from your Spotify account.

---

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Demo Video](#demo-video)
- [Installation](#installation)
- [Spotify API Integration](#spotify-api-integration)
- [Usage](#usage)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

---

## Features

- **Spotify Integration:** Authenticate with Spotify and access your playlists and library.
- **Create Party Playlists:** Start a new playlist session for your event.
- **Invite Friends:** Share a session code or link so others can join and contribute.
- **Song Search:** Search and add songs from Spotify’s vast catalogue.
- **Voting System:** All participants can upvote or downvote songs to influence the play order.
- **Real-Time Sync:** Changes are reflected instantly across all devices in the session.
- **Queue Management:** The playlist is automatically reordered based on votes.
- **User Authentication:** Secure OAuth login with Spotify.
- **Modern UI:** Material Design for a sleek and intuitive user experience.
- **Kotlin-Based:** Built natively for Android in Kotlin, with some Java interoperability.

---

## Screenshots

<!-- Replace with real screenshots -->
| Home Screen | Playlist View | Search & Add Songs | Voting |
|:-----------:|:-------------:|:------------------:|:------:|
| ![Home](docs/screenshots/home.png) | ![Playlist](docs/screenshots/playlist.png) | ![Search](docs/screenshots/search.png) | ![Vote](docs/screenshots/vote.png) |

---

---

## Installation

### Prerequisites

- Android Studio (Giraffe or later recommended)
- Android device or emulator running Android 7.0 (API 24) or higher
- A [Spotify Developer Account](https://developer.spotify.com/dashboard/applications) to obtain API credentials
- (Optional) A real device for Spotify playback

### Steps

1. **Clone the Repo**
   ```sh
   git clone https://github.com/print-ramcharan/party-playlist-maker.git
   cd party-playlist-maker
   ```

2. **Open in Android Studio**
   - Open the project folder in Android Studio.

3. **Configure Spotify API Credentials**
   - Create an app on the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/applications).
   - Add a redirect URI (e.g., `partyplaylistmaker://callback`).
   - Retrieve your `Client ID` and `Client Secret`.

4. **Set Up Environment Variables**
   - In your project, create a file called `local.properties` (if it doesn't exist).
   - Add:
     ```
     SPOTIFY_CLIENT_ID=your_client_id
     SPOTIFY_REDIRECT_URI=partyplaylistmaker://callback
     ```
   - Or, for runtime, update `Constants.kt` or wherever credentials/redirects are referenced.

5. **Build and Run**
   - Click **Run** in Android Studio or use:
     ```sh
     ./gradlew installDebug
     ```

---

## Spotify API Integration

This app uses the [Spotify Android Authentication Library](https://developer.spotify.com/documentation/android/) for login and [Spotify Web API](https://developer.spotify.com/documentation/web-api/) for music search, playback, and playlist management.

- **Authentication:** OAuth 2.0 (Authorization Code Flow)
- **Scopes Required:**
  - `user-read-private`
  - `playlist-modify-public`
  - `playlist-modify-private`
  - `streaming`
  - `user-modify-playback-state`
  - `user-read-playback-state`
- **Redirect URI:** Must match the one set in your Spotify developer dashboard and app code.

### How OAuth Works in the App

1. User clicks "Login with Spotify".
2. App launches Spotify login in a browser.
3. On success, the user is redirected back to the app via the custom URI.
4. Access token is used for all further Spotify API calls.

---

## Usage

1. **Login:** Start the app and log in with your Spotify account.
2. **Create a Party:** Tap "Create Party" to generate a new playlist session.
3. **Invite Friends:** Share the session code or invite link.
4. **Search & Add Songs:** Use the search bar to find and add songs from Spotify.
5. **Vote:** Upvote or downvote songs to change their position in the playlist.
6. **Playback:** Use the in-app player to stream music directly (Spotify Premium required for playback).
7. **End Party:** The session creator can end the party; playlists and votes are saved for future reference.

---

## Architecture

- **Language:** Kotlin (87%), Java (13%)
- **MVVM Pattern:** Ensures separation of concerns and easier testing.
- **Key Libraries:**
  - Spotify Android SDK & Web API
  - Retrofit for network requests
  - Coroutines & Flow for async operations
  - Room for local data storage (optional)
  - Firebase/Firestore (optional, for real-time features)
  - Material Components for UI
  - Glide/Picasso for image loading
- **Modules:**
  - `auth/` - Spotify OAuth login flow
  - `playlist/` - Playlist management and voting
  - `search/` - Song search and add
  - `player/` - Music playback controls
  - `network/` - API clients and models

---

## Configuration

You may need to adjust the following files to match your environment:

- **`Constants.kt`**
  - Set Spotify client ID and redirect URI.
- **`AndroidManifest.xml`**
  - Register your redirect URI intent filter.
- **Spotify Developer Dashboard**
  -
