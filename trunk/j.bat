dir/b symbols > symbols\listdir.txt
dir/b tutorials > tutorials\listdir.txt
"C:\Program Files\Java\jdk1.6.0_26\bin\jar" cmf tunnelmanifest.txt tunnel.jar Tunnel symbols/*.xml symbols/*.html symbols/listdir.txt tutorials/*.* tutorials/listdir.txt
REM "C:\Program Files\Java\jdk1.6.0_26\bin\jar" i tunnel.jar
REM "C:\Program Files\Java\jdk1.6.0\bin\jar" tf tunnel.jar

