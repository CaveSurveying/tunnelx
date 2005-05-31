import os
import sys
import re
import Image

dire = sys.argv[1:] and sys.argv[1] or "."
for ftif in os.listdir(dire):
	if re.search(".tif$", ftif):
		fpng = re.sub(".tif$", ".png", ftif)
		print "opening ", ftif
		im = Image.open(os.path.join(dire, ftif))
		print "  saving ", fpng
		im.save(os.path.join(dire, fpng))

