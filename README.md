# highwind

Source code for a simple android app to control first-generation OKAI / MyTIER ES-200G electric scooters.
Because TIER dropped support for this kind of E-scooter a while ago, I created this.

features:
- displays info about the scooter (Lock status, current mileage, total mileage, speed, battery, light status)
- locking / unlocking
- light on / off
- make headlight blink (locate)
- sport mode / speed setting (please respect local laws)

in order to use this, you will need the bluetooth LE MAC Address and the correct password (that was shipped with the scooter)
It can't be used for "hacking" or the like.

You need to edit line 133 for the password and line 138 for the MAC address in the MainActivity.kt
and then compile it and throw it at your android phone :-)

known issues:
- it says "Disconnected" and the app can't connect: reopen the app and make sure to be near the scooter. Of course, the scooter must be charged.
- sometimes, setting the speed doesn't actually change the speed: tap the slider again, that should help
- after restarting the app, the speed visually is set to 21 km/h: it's normal, you can't "read" the current speed setting from the E-scooter

# License

CC-BY-NC-SA - see LICENSE.md
