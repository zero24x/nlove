# Provider setup guide

To get started as a file sharing provider we will setup an SFTP (SSH FTP) server.

## 1) Sharing server setup - Windows users

1) Download,install and start CoreFTP server: http://www.coreftp.com/server/
2) Go to Setup > Domains > New, enter as Domain name & Domain IP: "127.0.0.1"
3) Select "base directory" (the directory you want to share), e.g. "C:\temp"
4) Click "Disable FTP", "SSH/SFTP"
5) Press OK twice, Select your new domain, in the "Users" section click: "New". Enter Username, 
Password and click on "Permissions", click "Add" to add the shared directory on your 
computer again, in the right section set permissions as required.
6) Press OK and Start.

## 1) Sharing server setup - Linux users
Here we use ubuntu. Connect to your serves terminal e.g. with SSH and run this commands:

Now our file sharing server is setup locally, we can share it through nlove with others 
to connect to:

## 2) Prepare nlove daemon to let others connect to your shared service

Now we start the nlove provider daemon so others can cannot to our shared service:
Start nlove - described in [README](README.md), section "Getting started"

Enabling provider steps:

1) Enter the command `provider enable`, as port for your local service enter "21" and a fitting banner message e.g. "this server is intended for
sharing kitty photos, uploads please in /upload", when users connect or see your provider in the provider list they will see this message!
2) The software needs a restart now.
3) Done, now others can find your provider with the command `list providers` and connect to it.