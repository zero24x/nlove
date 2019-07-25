# NLove - A new way of sharing files you love

NLove is a filesharing tool using the NKN protocol. It is command line based and simple to use. It works using a reverse proxy (e.g. SFTP server) where you can let others
connect to.

# Installation

1) Install Java 
2) Download latest version from https://github.com/zero24x/nlove/releases/latest/download/nlove.jar
3) In terminal, navigate to download directory and run: `java -Xmx1024m -jar nlove.jar`

# Community chat
Feel free to join our community chat on the NKN Dchat (Channel: #nlove, get Dchat here: https://gitlab.com/losnappas/d-chat)

# Infrastructure:

The nlove system consists of "clients" and "providers". By default you will join as a client and will be in the lobby where all 
people can chat in public or search through the files offered by providers.

## List of providers
To see a list of all providers who share files on their server, enter the command `list providers`, all names (chosen by the providers) of all providers will 
be returned.

## Becoming a provider

IMPORTANT: Do not use multi-port servers like FTP, this application supports single-port mode only e.g. SFTP!

Providers can provide files by putting them in the subfolder "share", so to become a provider put files in this
folder and restart the program.

Clients can issue search commands e.g. "search kitty" in the lobby chat room, the providers respond with search results matching
files in the shared directory and return all file IDs of the matches to the client.

Provider setup steps:

1) Put files in the subfolder "share".
2) Start the sharing server, e.g. SFTP on port 22.
3) Enter the command `provider enable`, you will be asked for a port of your local service (that other users can connect to). E.g. if you want to share an Webserver on Port 80, enter 80 here.
4) Done! Now tell others about your service to they start sharing or maybe they find you using the search - the command `search kitty` lists all 
server having files with this term in their "share" directory.

Note: To change the provider settings e.g. port, edit the file `config/config.json`

To stop being a provider, run `provider disable`.

# Limitations:
The network is limited to 1000 participants, we do not want to consume too much bandwidth!
