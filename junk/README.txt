Files locations
---------------
(Julian Todd, 2009-06-21)

The scripts b(.bat) and j(.bat) compile and package a jar file for you, 
r runs it with appropriate command line settings.
Vary these according to which compiler version you have.

The default symbols directory is either ~/.tunnelx/symbols/ 
or /usr/share/tunnelx/symbols/ 
or tunnelx[current directory]/symbols/ [particularly in Windows].

The tmp file used for calling cavern is ~/.tunnelx/tmp/tunnel_tmp_all.svx 
or /tmp/tunnel_tmp_all.svx 
or tunnelx[current directory]/tmp/tunnel_tmp_all.svx

The .jar file version additionally contains its own symbols directory 
so it can run self-contained like an executable.

Settings can be found in TN.java and FileAbstraction.java, 
which include setting the username, project name and password 
(in the case of uploading to troggle capability).




Compiling and running Tunnel
----------------------------

(David Loeffler, Sunday 2006-01-22)

To compile Tunnel on a Windows system, grab a DOS window, change to the Tunnel directory and run the following command:

"C:\Program Files\Java\jdk1.5.0_06\bin\javac" -d . -source 1.5 src\*.java

You'll need to have the Java Development Kit installed to do this; it can be downloaded from java.sun.com. You may need to edit the path to the Java compiler, depending on where the installer puts it. The batch file "b.bat" is provided in case you need to do this frequently; but this will only apply to people doing Tunnel development, who can probably work this out for themselves.

For other operating systems, something similar should work; just run the Java compiler in whatever way you normally would on your system. (If anyone feels like trying this out on a Mac and checking that it works, please do so and get in touch.)

Having compiled Tunnel, you can now run it by issuing the command

"C:\Program Files\Java\jdk1.5.0_06\bin\java" -ea -Xmx300m Tunnel.MainBox

(or an appropriate variant if your Java path is different). If you have a Tunnel XML directory set up already, you can load it automatically by giving the path to it at the end of the command line. The -Xmx300m option gives Tunnel 300 megabytes of memory to play with; it can be quite memory-hungry, particularly when dealing with very large surveys, so crank up this number if it's being slow.
