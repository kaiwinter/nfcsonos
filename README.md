# NFC Player

This app starts a Sonos favorite by scanning a NFC tag. 

I started this project to simplify starting audio books on Sonos for the kids. They have a set of printed CD covers from which they can choose what they want to listen to.
Choosing the same on the display of a smartphone or tablet always felt odd somehow. Now they can search through their collection of CD cover cards and pick one. By holding a card on a smartphone the Sonos box starts playing the linked album.

## How to

1. Print a CD cover on some thick paper
2. Stick a NFC tag on it
3. Create a favorite in the Sonos app for a specific album
4. Start the app and login with your Sonos credentials
5. If you own just one Sonos group the app will select this one automatically. If there are more than one group you have to select the one which the app should control.
6. In the app select the pairing button
7. Choose the Sonos favorite from the dropdown and hit the "Pair" button
8. Hold the NFC tag on the phone to link the tag to the favorite
9. Back on the main screen of the app hold the NFC tag on your phone
10. The app starts the favorite on the Sonos box and displays the title and cover of the album

## Technical considerations

### Sonos cloud

The app requires a Sonos login as it controls the box by sending commands to the Sonos cloud. 
In an early stage I was using UPNP commands which were sent to the Sonos box directly (no Sonos login necessary). But that turned out as too slow and too unstable. 
So I decided to send the commands through the Sonos cloud.

### Need for Sonos favorites

The Sonos Cloud API doesn't allow to play a album from e.g. Spotify in a direct way ([see Stackoverflow](https://stackoverflow.com/a/53733774/714965)). The workaround is to create a Sonos favorite of an album.

## TODO

- Set speaker volume to a level which was configured in the app preferences
- After the speaker was unplugged the group ID changes. If a favorite gets started the API returns 410: ERROR_RESOURCE_GONE. Currently the app switches to the DiscoveryActivity and let the user select a new Group. This could be automated by storing the groups `coordinatorId` and finding the new group ID by it.

## Screenshots
![1](https://user-images.githubusercontent.com/110982/110695721-7dedd100-81ea-11eb-9d4c-3b1ee0762495.png)
![2](https://user-images.githubusercontent.com/110982/110695741-85ad7580-81ea-11eb-8ec7-3123c8e2ba00.png)
![3](https://user-images.githubusercontent.com/110982/110695751-87773900-81ea-11eb-9ef8-142b92446ef0.png)
![4](https://user-images.githubusercontent.com/110982/110853909-7abf1780-82b4-11eb-8e79-45b9569e71c7.png)

## Credits

Sounds: https://mixkit.co/  
CD cover placeholder image: [Designed by rawpixel.com / Freepik](http://www.freepik.com)  
App icon made by <a href="https://www.flaticon.com/authors/flat-icons" title="Flat Icons">Flat Icons</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
