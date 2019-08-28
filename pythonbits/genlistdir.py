import os
import sys
import re


def FileType(fil):
	assert os.path.isfile(fil)

	if re.match("\.#", fil):
		return ""
	if re.search("\.svx$(?i)", fil):
		return "SVX"
	if re.search("\.pos$(?i)", fil):
		return "POS"
	if re.search("\.(?:png|gif|jpg)$(?i)", fil):
		return "IMG"
	if not re.search("\.xml$(?i)", fil):
		return ""

	# now we have to open the file slightly to find which type
	fin = open(fil, "r")
	filehead = fin.read(256)
	mftype = re.search("<tunnelxml>\s*<(\w+)", filehead)
	if not mftype:
		assert False  # unknown xml file
		return ""
	if mftype.group(1) == "sketch":
		return "SKETCH"
	if mftype.group(1) == "exports":
		return "EXPORTS"
	if mftype.group(1) == "measurements":
		return "MEASUREMENTS"
	if mftype.group(1) == "fontcolours":
		return "FONTCOLOURS"
	assert False  # unknown xml file
	return ""


# we only want to list a directory if it has something recognizable in it.
def DireListRecurse(dire):
	fils = [ ]
	dirs = [ ]
	for f in os.listdir(dire):
		jf = os.path.join(dire, f)
		if os.path.isdir(jf):
			dirs.append(f)
		elif os.path.isfile(jf):
			ft = FileType(jf)
			if ft:
				fils.append((ft, f))
	if not fils:
		return False

	fout = open(os.path.join(dire, "listdir.txt"), "w")
	for d in dirs:
		if DireListRecurse(os.path.join(dire, d)):
			fout.write("DIR %s\n" % d)
	for f in fils:
		fout.write("%s %s\n" % f)
	return True


if __name__ == "__main__":
	dire = len(sys.argv) == 1 and "." or sys.argv[1]
	DireListRecurse(dire)



#	print sys.argv


