"C:\Program Files\Java\jdk1.6.0\bin\javac" -d . src\*.java
rem dir/b symbols > symbols\listdir.txt
rem python pythonbits\genlistdir.py ../neil/inlet7
rem python pythonbits\genlistdir.py symbols
rem python pythonbits\genlistdir.py ../surveys/tunneldata
"C:\Program Files\Java\jdk1.6.0\bin\jar" cfm tunnel.jar tunnelmanifest.txt Tunnel symbols
