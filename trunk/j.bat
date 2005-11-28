dir/b symbols > symbols\listdir.txt
python pythonbits\genlistdir.py ../inlet7
python pythonbits\genlistdir.py symbols
rem python pythonbits\genlistdir.py ../surveys/tunneldata
"C:\Program Files\Java\jdk1.5.0_05\bin\jar" cfm tunnel.jar tunnelmanifest.txt Tunnel symbols
