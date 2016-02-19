--- UNDER CONSTRUCTION ---


# Network.onion

## Fully distributed peer-to-peer social network using onion routing

![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/netfungra9.png)

Conventional social networks store all user data on central servers. This leads to several issues. Information has to be sold to advertising companies to finance the centralized server infrastructure. Users are exposed to surveillance and censorship. In some cases, users are even subject to political persecution.

Network.onion is different! All your data is stored on your own device. No central servers are needed. Devices communicate with each other over a decentralized peer-to-peer network. All communication is anonymized and encrypted via Onion Routing (Tor).

- Anonymous, peer-to-peer, fully decentralized
- Data remains on the users' own devices
- All communication sent through Tor anonymously (onion routing)
- Full end-to-end encryption
- Completely free
- Post text and images
- Share content
- Friend list
- Personal profile
- Private chat


![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/s0.png)
![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/s1.png)
![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/s2.png)
![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/s3.png)


![](https://raw.githubusercontent.com/onionApps/Network.onion/master/gfx/network01edit.jpg)



http://play.google.com/store/apps/details?id=onion.network

http://onionapps.github.io/Network.onion/

Author: http://github.com/onionApps - jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf




## Finding Friends
- Meet them in real life and scan their QR code
- Send them a standard invitation message via Email / SMS / instant messengers / etc. 
- Post your Network.onion URL on the web
- Search for other Network.onion users who have posted their URLs or IDs on the web
- Share your Network.onion URL or ID on other social networks
- Let them know your 16 character ID
- Search the web for Network.onion FriendBots



## Building
- Clone this repository
- (Optionally, replace the included Tor binaries at /bin/ with your own and run /bin/pack.sh)
- (Get Android Studio)
- Open project
- Hit the run button

Tor binaries: This repository already contains Tor binaries for a few different platforms and Android versions. If you want to build everything from source, you'd have to replace them. Building Tor for Android can be a bit tricky, but there are a few instructions on the web. If your Network.onion build only needs to work on one platform, you can simply replace all four files with the same binary; overhead will be compressed away. You could probably also extract the Tor binary from some other Android app if you trust it more and drop it in here. 



## Bots
Network.onion supports the operation of automated bots. Some can be created by simply installing Network.onion on a device or emulator and enabling the respective bot mode. Others need a bit of programming.

#### FriendBots
Install Network.onion on a device or emulator, go to "Settings", and enable "FriendBot Mode". The device will now automatically accept all friend requests to help other users find friends.

#### WallBots
Go to "Settings", enable "WallBot Mode", enter the URL of an RSS feed, and set an update interval. Network.onion will now periodically poll the RSS feed and post all new items to your wall. To create more advanced WallBots, you can of course implement your own RSS feed using your favourite scripting language. 

#### ChatBots
Enable "ChatBot Mode" and enter the URL of an HTTP server. Network.onion will now send all incoming messages to that HTTP server and forward responses to the chat partner.

Parameters:

- a: address
- n: name
- t: timestamp
- m: message
