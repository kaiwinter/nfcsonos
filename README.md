# NFC Player

This app starts a Sonos favorite by scanning a NFC tag. 

I started this project to simplify starting audio books on Sonos for the kids. Now they have a set of printed CD covers from which they can choose what they want to listen to.
Choosing the same on the display of a smartphone or tablet always felt odd somehow. Now they can search through their collection of CD cover cards and pick one. By holding the card on a smartphone the Sonos box starts playing the linked album.

## How to

1. Print a CD cover
2. Stick a NFC tag on the back  
(I'm using photographic paper for printing. After sticking the NFC tag on the back I glue a cardboard over it.)

Sonos App

3. Create a favorite in the Sonos app for a specific album  

NFC Controller App

4. Login with your Sonos credentials
5. Select the Sonos group which the app should control
6. Go to the "Pairing" section
7. Choose the Sonos favorite from the dropdown and hit the "Pair" button
8. Hold the NFC tag on (or behind) the phone to link the tag with the favorite

Now you can put the printed CD cover on your phone to start the album on the Sonos box. The app don't have to be opened before doing this.

## Technical considerations

### Sonos cloud

The app requires a Sonos login as it controls the box by sending commands to the Sonos cloud. 
In an early stage I was using UPNP commands which were sent to the Sonos box directly (no Sonos login necessary). But that turned out as too slow and too unstable. 
So I decided to send the commands through the Sonos cloud.

### Need for Sonos favorites

The Sonos Cloud API doesn't allow to play an album from e.g. Spotify in a direct way ([see Stackoverflow](https://stackoverflow.com/a/53733774/714965)). The workaround is to create a Sonos favorite of an album.

## TODO

- Set speaker volume to a level which was configured in the app preferences
- After the speaker was unplugged the group ID changes. If a favorite gets started the API returns 410: ERROR_RESOURCE_GONE. Currently the app switches to the DiscoveryActivity and let the user select a new Group. This could be automated by storing the groups `coordinatorId` and finding the new group ID by it.

## Screenshots
![1](https://user-images.githubusercontent.com/110982/122286942-b4756f00-cef0-11eb-8bba-fd8664c7ebbb.png)
![2](https://user-images.githubusercontent.com/110982/122286947-b5a69c00-cef0-11eb-9451-9c8a32f769f1.png)
![3](https://user-images.githubusercontent.com/110982/122286954-b6d7c900-cef0-11eb-82da-dd8f31f9e807.png)
![4](https://user-images.githubusercontent.com/110982/122286957-b808f600-cef0-11eb-8be8-68188e435e5c.png)


## Credits

Sounds: https://mixkit.co/  
CD cover placeholder image: [Designed by rawpixel.com / Freepik](http://www.freepik.com)  
App icon made by <a href="https://www.flaticon.com/authors/flat-icons" title="Flat Icons">Flat Icons</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
