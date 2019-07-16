# NLove - A new kind of sharing things you love

NLove is a filesharing tool using the NKN protocol.

It is command line based and simple to use.

# Start it:

`java -jar nlove.jar`

# Commands

```
search <searchterm> --> Searches for providers offering files with <searchterm> in the name
"download <fileID> --> Download a file with ID <fileID> provided by any provider
chat <message> --> Write a public message in the lobby chat.
```

# Infrastructure:

The nlove system consists of "clients" and "providers". By default you will join as a client and will be in the lobby where all 
people can chat in public.

## Becoming a file provider

Providers can provide files by putting them in the subfolder "share", so to become a provider put files in this
folder and restart the program.
Clients can issue search commands e.g. "search kitty", the providers will search through all 
files in the shared directory and return all file IDs of the matches to the client.

To download one of this files, enter "download <fileid" (e.g. "download nlove-privoderx938989alljx/kitty.jpg" and the client
will start download this file).

# Limitations:
To prevent abuse (sharing bad things) some search terms are blacklisted.
The network is limited to 1000 participants, we do not want to consume too much bandwidth!