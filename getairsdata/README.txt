Copyright (C) 2011 Dana Pavel. All rights reserved.

-------------------
About this software
-------------------
This software is meant to be used together with the files obtained from the AIRS platform ().
Its purpose is to parse the AIRS data files and save the data in a MySQL database for easier access and further processing.
The software creates a MySQL database called "airs" with two MySQL tables called "recordings" and "phone_data".

The software contains the following files:
getairsdata.jar - the JAR file that parses data from the AIRS storage and saves it into the MySQL database
airs_settings.txt -  settings file where the AIRS storage path and the database access credentials are read from
airs_sensors.txt - file where all the known AIRS sensors are listed for being read with getairsdata.jar
getairsdata_create.bat - batch file (Windows) that runs the program by re-creating the whole database
getairsdata_update.bat  - batch file (Windows) that runs the program by updating the database with new AIRS data


-------------
Settings file
-------------
In airs_settings.txt file are the following settings:
AIRS_SENSORS - path to the airs_sensors.txt file
AIRS_STORAGE - path for the folder where the AIRS data files are stored
DB_USER - username to access the database (preferably not root)
DB_PASS - password to access the database
DB_PATH - JDBC path to mysql database (should not need change)

----------
Running it
----------
In Windows, run either of the batch files, depending on if you want to re-create all the database entries or if you just 
want to update it when new AIRS files are added to the local AIRS storage.

In non-Windows, run the jar file with the following parameters:
"getairsdata.jar airs_settings.txt -c" for (re)creating the airs database
"getairs.data.jar airs_settings.txt -u" for updating the airs database

--------------------------------
System and software requirements
--------------------------------
The software should run on any platform that runs Java.
The user should make sure that java is installed and the java path properly set up in the system environment variables.
MySQL should also be installed before running this program.

The user has to make sure that the MySQL is properly set up, with the proper username and password (the ones used in the settings file). 
Also, the user has to make sure that the MySQL username has enough rights to create and drop a database.

----------
Disclaimer
----------
This software is provided as is, free of charge. The authors is not responsible for any lost data or other misusages of the provided software.
Also, this is not intended as a fully supported software, so any comments or suggestions will be addressed according to available time.