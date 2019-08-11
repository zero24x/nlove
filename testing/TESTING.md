## Testing


1. Remove data/profile.json, config/config.json

### Roll

------

1. Start nove.jar
2. Should: Notify about empty profile, ask you to create profile:

   1. Do not fill out anything, press save --> Should: Error
   2. Fill out all fields, press save --> Should: Save profile, connect to network.
3. Start second window, repeat procedure.
4. In Window 1, press match, should open popup with Window 2 profile. Press match 3 times, should always match same profile.
5. Start Window 3, fill out profile, save.
6. In Window 2 should match profile of Window 1 or Window 3, NEVER own profile!

#### Closing test (In match window)

4. Press "Close" button --> Should: Close Window

### Hangout

------

1. Click Hangout button --> Should: Open Hangout window, load old messages.
2. Enter message text, press send --> Should: Write your message in message list as "Sent", after ~ 2min display full msg with timestamp.
3. Enter empty message text -> Should: Warn that messagetext needs to be entered, NOT send message to network.

#### Closing test

4. Press "Close" button --> Should: Close Window