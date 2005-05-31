import os
import re
import string

from svxrecords import svxleg, svxleginfo, svxblock

# regexp for matching a LRUD entry, and for a line (four of them)
lrudent = "[(\[]?([\d\.?/\-]+?)[)\]?]?[+p]?(?:\s*[(\[]([\d\.]+)[)\]])?"
rlrud = "(\S+)\s+%s\s+%s\s+%s\s+%s?" % (lrudent, lrudent, lrudent, lrudent)


# main class that handles loading the survex file
class svxloader:
	def __init__(self, fout, belevations):
		self.rootblock = svxblock("", None)
		self.svxcurrent = self.rootblock  # outer survey block
		self.svxstack = [ self.rootblock ]   # stack of survey blocks of nested begins (starts with outer)
		self.allsvxblocks = [ self.rootblock ]
		self.fout = fout
		self.belevations = belevations  # ignores the LRs of the UD

	def handlecommentedline(self, slinec, comment, sline):
		if not self.fout:
			return
		self.fout.write(sline)  # output into the file
		if not comment:
			return
		comm1 = re.match("(\S+)", comment).group(1)
		if comm1 not in self.svxcurrent.stations:
			return

		# got a line with the first word a station.  match the rest
		mlrud = re.match(rlrud, comment)
		if mlrud:
			self.svxcurrent.writexcleg(mlrud, self.fout, self.belevations)
		else:
			pass
			#print "unmatched likely cross-section '%s'" % sline
			#print "'%s'" % slinec

	# implements all survex star commands
	def handlestarcommand(self, starc, stararg, sline, lsfile):
		self.svxcurrent.leginfo = None

		# recursively loads the next file
		if starc == "include":
			if self.fout:
				self.fout.write(";%s" % sline)
			ifile = stararg
			if not re.search("\.svx$", ifile):
				ifile = ifile + ".svx"
			dire = os.path.split(lsfile)[0]
			lifile = os.path.join(dire, ifile)
			self.readsvxfilerecurse(lifile)
			return

		elif starc == "begin":
			self.svxcurrent = svxblock(stararg, self.svxcurrent)
			self.allsvxblocks.append(self.svxcurrent)
			self.svxstack.append(self.svxcurrent)
		elif starc == "end":
			self.svxstack.pop()
			self.svxcurrent = self.svxstack[-1]
			assert len(self.svxstack) >= 1

		elif starc == "flags":
			mflags = re.match("(not\s+)?(.*)$", stararg)
			assert mflags.group(2) in ["splay", "surface", "duplicate"]
			self.svxcurrent.flags[mflags.group(2)] = not mflags.group(1)

		elif starc == "title":
			mttitledq = re.match('"(.*)"$', stararg)
			self.svxcurrent.title = mttitledq and mttitledq.group(1) or stararg
			print "title:", self.svxcurrent.title

		elif starc == "team":
			mteam = re.match('"(.*)"\s*(.*)$', stararg)
			if self.svxcurrent.legs:
				print "team defined after some legs!!!", self.svxcurrent.prefix
			for instr in re.split("\s+", mteam.group(2)):
				assert instr in ["compass", "notes", "tape", "clino", "dog", "elevation"]
				self.svxcurrent.team.append((mteam.group(1), instr))

		elif starc == "instrument":
			minstrument = re.match('"(.*)"\s*(.*)$', stararg)
			assert minstrument.group(2) in ["compass", "tape", "clino"]
			self.svxcurrent.instruments[minstrument.group(2)] = minstrument.group(1)

		elif starc == "calibrate":
			mcalibrate = re.match('(\S+)\s*(.*)$', stararg)
			self.svxcurrent.calibrates[mcalibrate.group(1)] = float(mcalibrate.group(2))

		elif starc == "date":
			self.svxcurrent.date = stararg

		# collect the set and remove duplicates
		elif starc == "equate":
			seq = [ self.svxcurrent.prefix + eq for eq in re.split("\s", stararg) ]
			seq.sort()
			for i in range(len(seq) - 1, 0, -1):
				if seq[i] == seq[i - 1]:
					del seq[i]
			if len(seq) > 1:
				self.svxcurrent.equates.append(seq)

		elif starc == "export":
			pass # not implemented
		elif starc == "entrance":
			pass # not implemented

		else:
			print "Unknown start cmd", starc

		# final output of the original star command
		if self.fout:
			self.fout.write(sline)


	# match a leg
	def	handleleg(self, slinec, sline):
		if not self.svxcurrent.leginfo:
			self.svxcurrent.leginfo = svxleginfo(self.svxcurrent)

		mleg = re.match("(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)$", slinec)
		if not mleg:
			print "failed to match leg structure", slinec
		self.svxcurrent.addleg(mleg.group(1), mleg.group(2), mleg.group(3), mleg.group(4), mleg.group(5))
		if self.fout:
			self.fout.write(sline)

	def readsvxfilerecurse(self, lsfile):
		# scan through the lines of the file
		self.svxcurrent.filename = lsfile
		ssurv = open(lsfile)
		for sline in ssurv.readlines():
			# remove comments
			mslinec = re.match("\s*(.*?)\s*(?:;\s*([\s\S]*?)\s*)?$", sline)
			slinec = mslinec.group(1)
			comment = mslinec.group(2)
			mstar = re.match("\*(\S+)\s*(.*)$", slinec)

			# blank or commented line
			if not slinec:
				self.handlecommentedline(slinec, comment, sline)
			elif mstar:
				self.handlestarcommand(mstar.group(1), mstar.group(2), sline, lsfile)
			else:
				self.handleleg(slinec, sline)








