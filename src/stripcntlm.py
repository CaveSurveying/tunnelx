import re, sys
a = open(sys.argv[1])
b = a.read(); 
a.close()
a = open(sys.argv[1], "w")
a.write(re.sub("\r", "", b))
a.close()

