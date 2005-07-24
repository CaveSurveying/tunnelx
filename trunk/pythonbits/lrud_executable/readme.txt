svxprocess.exe

Installation:

  Copy the following files into a directory:
    _sre.pyd
    library.zip
    processXC.bat
    python23.dll
    svxprocess.exe
    w9xpopen.exe
  Edit processXC.bat to provide an absolute path to the svxprocess.exe executable file
  Create an association between .svx files and processXC.bat

Usage:

  Right click on a *.svx file
  Select processXC from the menu (or Open With --> processXC)
  Process the resultant *__XC.svx file with cavern in the normal way
  X Sections appear as surface legs

Notes:

  Requires certain conventions in your svx files for this to work smoothly:

    X Sections should be recorded as commented lines
    Data order is left right up down
    Data separated by whitespace