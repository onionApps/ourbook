# Network.onion Bots
Network.onion supports the operation of automated bots. Some can be created by simply installing Network.onion on a device or emulator and enabling the respective bot mode.

## FriendBot
Install Network.onion on a device or emulator, go to "Settings", and enable "FriendBot Mode". The device will now automatically accept all friend requests to help other users find friends. 

## WallBot
Go to "Settings", enable "WallBot Mode", enter the URL of an RSS feed, and set an update interval. Network.onion will now periodically poll the RSS feed and post all new items to your wall. To create more advanced WallBots, you can of course implement your own RSS feed using your favourite scripting language. 

## ChatBot
Enable "ChatBot Mode" and enter the URL of an HTTP server. Network.onion will now send all incoming messages to that HTTP server and forward responses to the chat partner.

Parameters:

- a: address
- n: name
- t: timestamp
- m: message
