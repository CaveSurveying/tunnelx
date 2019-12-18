"C:\Program Files\Java\jdk1.6.0\bin\javac" -d . src\*.java
dir/b symbols > symbols\listdir.txt
"C:\Program Files\Java\jdk1.6.0\bin\jar" cmf tunnelmanifest.txt tunnel.jar Tunnel symbols/*.xml symbols/listdir.txt
"C:\Program Files\Java\jdk1.6.0\bin\jar" i tunnel.jar

