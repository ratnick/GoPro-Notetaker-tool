# README #

NoteTaker

### Get the latest release ###
* Stored locally (contact administrator at <email>)

### What is this repository for? ###

* Version controlling NoteTaker sourcecode
* Distributing the latest executable jar's

### How do I set it up and run it? ###

* Vino/NoteTaker is based on JavaFX, please check the latest requirements for JavaFX.
* Following dependencies need to be manually updated
* - aspectjrt (https://mvnrepository.com/artifact/org.aspectj/aspectjrt)
* - httpcomponents (https://hc.apache.org)
* - isoparser (https://github.com/sannies/mp4parser)
* - jsoup (https://jsoup.org)
* - richtextfx (https://github.com/TomasMikula/RichTextFX)


### Quick Start ###

* When taking notes, obviously you need to be connected to the GoPro camera
* Press connect and start a session.
* Press Shift+Enter to insert a timestamp
* Stop the session when finished (or the camera will continue recording)
* Save the note and the details, but don't set the video location
* Turn off the camera, connect it to the computer with USB and turn it on again
* Choose File->Import...
* If source and destination hasn't been found automatically, select them manually and press Ok 
* After import, check Edit->Edit note details (the video location should match the destination of the imported videos)
* Go to Note->Compile...
* Here you go, your notes with all your timestamps turned into buttons that open the video at the time the timestamp was created

### Some notes about NoteTaker ###

* Each session starts a new recording, so one note can refer several recording.
* GoPro's are chaptering the recordings and only the base file will be visibly referred in the notes but during import, all chaptered videos are merged into the base file.

### Suggested updates ###

* Maven integration 
* Mature RT timestamp magic
* Mature GoPro connectivity (RT status)
* Improve usability
* Support for GoPro 4 and other cameras
* Finalize video merge
* Improve import speed
* Left/Right balance to stereo output

### Who do I talk to? ###

* For questions and comments please contact admin