# Network.onion Bots
Network.onion supports the operation of automated bots. Some can be created by simply installing Network.onion on a device or emulator and enabling the respective bot mode. Others need a bit of programming.

## FriendBot
Install Network.onion on a device or emulator, go to "Settings", and enable "FriendBot Mode". The device will now automatically accept all friend requests. You might also want to set "Accept Messages" to "None".

FriendBots can serve as meeting places for Network.onion users to find new friends. 

## WallBot
Go to "Settings", enable "WallBot Mode", enter the URL of an RSS feed, and set an update interval. Network.onion will now periodically poll the RSS feed and post all new items to your wall. To create more advanced WallBots, you can of course implement your own RSS feed using your favourite scripting language. 

## ChatBot
Enable "ChatBot Mode" and enter the URL of an HTTP server. Network.onion will now send all incoming messages to that HTTP server and forward responses to the chat partner.

Parameters:

``` ?a=<address>&n=<name>&t=<timestamp>&m=<message> ```

|   | Paremeter  |
|---|------------|
| a | address    |
| n | name       |
| t | timestamp  |
| m | message    |

If "Accept Messages" is set to "None", messages aren't shown or saved, but are still relayed to the ChatBot. 

## Running the sample ChatBot
- Start a terminal, clone this repository if you haven't yet, and navigate to this directory
- Install Python and web.py (eg. using ```sudo apt-get install python``` and ```sudo pip install web.py```)
- Run ```python chatbot.py 8080 chatbot-test.txt```
- Install and run Network.onion on a device or emulator
- Enable "ChatBot Mode" and set "ChatBot server" to where you are running it (eg. http://localhost:8080/).

## VirtualBox
You can also run Network.onion on VirtualBox
- Get VirtualBox
- Get Android-x86 4.0 eeepc (http://www.android-x86.org/download)
- Create a Linux box ("Other Linux"), and start or install Android
- Setup NAT to allow ADB (Network -> Advanced -> Port Redirection -> Guest Port = 5555, Host Port = 5555)
- Install Android Debug Tools (eg. ``` sudo apt-get install android-tools-adb ```)
- Connect to the device ``` adb connect localhost:5555 ```
- Navigate to your Network.onion .APK
- Install Network.onion ``` adb install app-release.apk ```
