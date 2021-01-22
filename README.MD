
# Spotify Music Party

We made this Android application to provide a custom version of a MusicParty. Our idea was to set on simple Server-Client model, where only the Server is able to play the music.

## Installation
1. Go to the [Spotify Developer Page](https://developer.spotify.com/dashboard/)  and create a application to get a. `Client ID` and a `Client Secret` (These will be needed if you wand to build the app local).
**Important:** You have to add your and fingerprint to the application in the developper page.
2. Clone the repository.
3. Rename the `apikey.properties.example` file to `apikey.properties` and add you previously gained `Client ID` and `Client Secret` at the right row inside the quotation marks.
4. Now you can use the default gradle build and debug commands.

### Android requirements
- `Android SDK >= 30`
- The spotify app has to be installed for hosting a server