# NLove - A new way of sharing files you love

NLove is a filesharing tool using the NKN protocol. It is command line based and simple to use. It works using a reverse proxy (e.g. SFTP server) where you can let others
connect to.

# Getting started

1) Install Java, for linux users the latest java headless is enough!
2) Download latest version from https://github.com/zero24x/nlove/releases/latest/download/nlove.jar
3) In terminal, navigate to download directory and run: `java -Xmx1024m -jar nlove.jar`. (If you have less RAM, change 1024m to something lower)

Or to start in debug mode to see details what is going on: `java -Xmx1024m -jar nlove.jar -debug`

Run on linux as background process:

1) Make sure the tool "screen" is installed (Ubuntu: `apt install screen`)
2) Run `screen -S nl -d -m java -Xmx1024m -jar nlove.jar &`.
3) Now you can run `screen -r nl` to bring the software in the foreground, press `Ctl + A + D` to let it run in the background again. `Ctl + C` to stop it.


# Community chat
Feel free to join our community chat on the NKN Dchat (Channel: #nlove, get Dchat here: https://gitlab.com/losnappas/d-chat)

# Infrastructure:

The nlove system consists of "clients" and "providers". By default you will join as a client and will be in the lobby where all 
people can chat in public or search through the files offered by providers.

## List of providers
To see a list of all providers who share files on their server, enter the command `list providers`, all names (chosen by the providers) of all providers will 
be returned.

## Becoming a provider

Important provider advices: 

1) For file sharing use better FTPS, SFTP (SSH FTP) is not so fast by default.
2) Want to get your file sharing server running quickly now? Check out our [provider setup guide](PROVIDER_SETUP_GUIDE.md).

### Provider commands

To change your provider settings e.g. service port or username, run the command `provider configure`.

To stop being a provider, run `provider disable`.

# Limitations
This software support single port sharing only, so the provider shares 1 port where others can connect to, multi-port protocol like regular FTP
will not work. Feel free to use tunnels e.g. OpenVPN or SSH to be able to have multi-port traffic.