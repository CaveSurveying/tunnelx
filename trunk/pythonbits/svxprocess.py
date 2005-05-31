import os
import sys
import re
import string

cavernexecutable = "C:\Program Files\Survex\Survex1.0.34\cavern.exe"

from svxrecords import svxleg, svxleginfo, svxblock
from svxload import svxloader
from svxpassages import PassageInfo
from svxnetwork import EquateNodes, StationCalculation, Getlegbbox




# find the settings, which were made on the command line
sfile = sys.argv[1]
if re.match("--", sfile):
	sfile = sys.argv[2]
	bsetting = sys.argv[1]
else:
	bsetting = ""

# find the root name of the survex file
sfileroot = re.match("(.*?)\.svx$", sfile).group(1)

# cross-section generating case, or leave output file as none
bcrosssections = bsetting == "--crosssections"
belevations = bsetting == "--elevations"
bpassages = bsetting == "--passages"
if bcrosssections or belevations:
	# generate all the file names
	xcfileroot = "%s__%s" % (sfileroot, (belevations and "ELEV" or "XC"))
	xcfilesvx = "%s.svx" % xcfileroot
	xcfileerr = "%s.err" % xcfileroot
	xcfilelog = "%s.log" % xcfileroot
	print "writing:", xcfilesvx
	fxcout = open(xcfilesvx, "w")
else:
	fxcout = None

# load in the file
svxf = svxloader(fxcout, belevations)
svxf.readsvxfilerecurse(sfile)
assert len(svxf.svxstack) == 1

# cross sections generated.  Close file and process
if fxcout:
	fxcout.close()
	os.execl(cavernexecutable, cavernexecutable, '"%s"'%xcfilesvx, "--output=.", "")
	print "We don't normally get here because cavern hard-exits out and aborts the script"
	sys.exit(0)


# now build up the graph of the legs
legs = EquateNodes(svxf.rootblock)
StationCalculation(legs)
bbox = Getlegbbox(legs)

cavedatabasefile = "C:/Data/www/From Web/xml/cavedb/houping.xml"

# we have the survex leg data loaded into memory.  Now continue processing it.
htmlout = "%s__passages.html" % sfileroot
print "writing", htmlout
#svxf.LinkLegsToStations()
fout = open(htmlout, "w")
imdire = "images"
if not os.path.isdir(imdire):
	print "To get images, make a directory '%s'" % imdire
	imdire= None

PassageInfo(legs, fout, bbox, imdire, sfileroot, cavedatabasefile)
fout.close()


