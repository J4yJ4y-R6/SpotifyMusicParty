
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

## Functionalities

### Party-Owner
- You can become an owner by creating a MusicParty in the main menu.
- You can search for every song and artist available in Spotify by entering the value in the input at the top and clicking on the search button. You can add a song by swiping it in the result window.
- You can start, pause, skip a song or go back to the last song with the controls on under the album-cover on the main page.
- Queue: Toolbar 1. Button
    - See the current state of the queue
    - Change the playlist order by drag and drop
    - Remove a song from the playlist by swiping it to the left
- Settings: Toolbar 2. Button
    - Change the party name, which is displayed to everybody
    - See your IP-Address and your Password to invite other people
    - See a QR-Code which stores your IP-Address and Password for sharing
    - Share information, QR-Code or a link for joining your party
    - Change the party type (currently two options):
        - All-In: All queued songs will be added to the playlist. Only you can edit the playlist.
        - Voting: Every time someone wants to queue a song, a voting will automatically  be created to decide whether the song will be added to the queue. Also there is always a voting to skip the currently playing song.
    - Change voting time (only available in Voting-Mode)
- Voting: Toolbar 3. Button (only visible in Voting-Mode):
    - See all currently open votings:
        - Queue-Voting: A voting for every queue request
        - Skip-Voting: Always one voting, while a song is currently playing, to skip this song
    - You have three options for every voting-card:
        - Thumb-Up: Vote Yes
        - Thumb-Down: Vote No
        - X-Button: Ignore
- Party-People: Toolbar 4. Button
    - See everybody who is currently a part of your party
- Exit: Toolbar 5. Button
    - Save all songs played at the party as a playlist in Spotify and add them to your favorites in our App.
    - End the party. Everybody will be disconnected from the party.
- Favorite-Playlists: Heart-Button (top-right-corner)
    - See a list of your saved playlists
    - By clicking one of the playlist-cards you get three options:
        - Playing the playlist, which will remove the current playlist state
        - Remove the playlist from your favorites
        - Edit the image or the name of your playlist. It will be changed in the MusicParty-App and in Spotify.


### Party-People
- You can become a Party-People by joining a currently open MusicParty by one of the following options:
    - Enter IP-Address and Password in the input fields
    - Scan a QR-Code which will fill the fields
    - Use a link which will fill the fields
- You can enter a name or use a random generated one. Only the owner will see your name.
- You can search for every song and artist available in Spotify by entering the value in the input at the top and clicking on the search button. You can add a song by swiping it in the result window.
- Queue: Toolbar 1. Button
    - See the current state of the queue
- Voting: Toolbar 2. Button (only visible in Voting-Mode):
    - See all currently open votings:
        - Queue-Voting: A voting for every queue request
        - Skip-Voting: Always one voting, while a song is currently playing, to skip this song
    - You have three options for every voting-card:
        - Thumb-Up: Vote Yes
        - Thumb-Down: Vote No
        - X-Button: Ignore
- Exit: Toolbar 5. Button
    - Leave the party