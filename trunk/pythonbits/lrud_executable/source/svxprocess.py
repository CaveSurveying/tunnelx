import sys
import re

from svxload import svxloader

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

# cross sections generated.  Close file.
if fxcout:
	fxcout.close()
