import re
import math

# this is only used later on when linking things up
class svxstation:
	def __init__(self, stationname):  # would like to take as a tuple to represent equates properly
		self.stationname = stationname
		self.legs = [ ]
		self.pos = None

# definition of a survey leg
class svxleg:
	def __init__(self, prefix, sfrom, sto, stape, scompass, sclino, leginfo, bforeleg):
		self.sfrom = prefix + sfrom
		self.sto = prefix + sto
		self.sfromr = sfrom # non prefixed versions
		self.stor = sto
		self.bforeleg = bforeleg  # direction into the cave
		self.leginfo = leginfo

		self.tape = float(stape) - leginfo.calibrates["tape"]
		# put in the other calibrates
		self.compass = scompass != "-" and float(scompass) or 0.0
		if re.match("down(?i)", sclino):
			self.clino = -90.0
		elif re.match("up(?i)", sclino):
			self.clino = 90.0
		else:
			self.clino = float(sclino)

		cosclin = math.cos(self.clino * math.pi / 180)
		sinclin = math.sin(self.clino * math.pi / 180)
		coscomp = math.cos(self.compass * math.pi / 180)
		sincomp = math.sin(self.compass * math.pi / 180)
		self.vec = (self.tape * sincomp * cosclin, self.tape * coscomp * cosclin, self.tape * sinclin)

	def gstat(self, station):
		if self.sfromr == station:
			return self.bforeleg and 1 or -1
		if self.stor == station:
			return self.bforeleg and -1 or 1
		return 0


def xcv(xcs):
	if not xcs:
		return -1.0
	if xcs == "?" or xcs == "-":
		return -1.0
	return float(re.sub("/", ".", xcs))

class svxleginfo:
	def __init__(self, svxblock):
		self.team = svxblock.team[:]
		self.date = svxblock.date
		self.flags = svxblock.flags.copy()
		self.calibrates = svxblock.calibrates.copy()
		self.instruments = svxblock.instruments.copy()
		self.title = svxblock.title
		self.filename = svxblock.filename

# a survey block of legs and xsections
class svxblock:
	def __init__(self, lname, prevblock):
		self.name = lname
		self.legs = [ ]
		self.equates = [ ] # list of lists
		self.stations = [ ]
		self.xsections = [ ]
		self.descblocks = [ ] # svx blocks inside this one

		# the object containing all the settings copied out from the
		# survex block at the time of each leg
		self.leginfo = None

		# put in links up and down the tree
		self.upperblock = prevblock
		if prevblock:
			prevblock.descblocks.append(self)

		# initialize all values
		if lname or not prevblock:
			self.team = [ ]
			self.date = ""
			self.flags = { "surface":False, "duplicate":False, "splay":False }
			self.calibrates = { "tape":0.0, "clino":0.0, "declination":0.0, "compass":0.0 }
			self.instruments = {}
			self.title = lname or "root"
			self.filename = ""
			if lname:
				if not prevblock.prefix: # first step down from root
					self.prefix = "%s." % lname
				else:
					self.prefix = "%s%s." % (prevblock.prefix, lname)
			else:  # root case
				self.prefix = ""

		# case of blank begin; copy attributes down
		else:
			self.team = prevblock.team[:]
			self.date = prevblock.date
			self.flags = prevblock.flags.copy()
			self.calibrates = prevblock.calibrates.copy()
			self.instruments = prevblock.instruments.copy()
			self.title = prevblock.title
			self.prefix = prevblock.prefix
			self.filename = prevblock.filename



	def addleg(self, sfrom, sto, stape, scompass, sclino):
		if not self.leginfo:
			self.leginfo = svxleginfo(self)
		bforeleg = True
		if sfrom not in self.stations:
			self.stations.append(sfrom)
			bforeleg = False
		if sto not in self.stations:
			self.stations.append(sto)
			bforeleg = True
		self.legs.append(svxleg(self.prefix, sfrom, sto, stape, scompass, sclino, self.leginfo, bforeleg))


	def findxclegpair(self, station):
		res = None
		prevleg = None
		for leg in self.legs:
			ldir = leg.gstat(station)
			if ldir:
				if not res:
					res = (leg, )
				if len(res) == 1 and prevleg and prevleg.gstat(station) == -ldir:
					res = (prevleg, leg)

			prevleg = leg
		return res

	def writexcfrag(self, station, dist, xcbear, slrud, fout):
		if dist == -1:
			return
		if dist == 0:
			return

		# do the dashed bit (only if it's a long piece)
		if dist < 6 or slrud[0] == "U" or slrud[0] == "D":
			distp = (dist, 0.0)
		else:
			distp = (dist - 1.0, 1.0)

		# use to dot them
		#*flags surface
		#*flags not surface
		fstation = station
		for i in range(2):
			fout.write("*flags%s surface\n" % (i == 1 and " not" or ""))
			tstation = "%s_XC%s%s" % (station, slrud, (i == 0 and "_" or ""))
			if slrud[0] == "L":
				fout.write("%s  %s %.2f %.0f 0\n" % (fstation, tstation, distp[i], ((xcbear + 270) % 360)))
			elif slrud[0] == "R":
				fout.write("%s  %s %.2f %.0f 0\n" % (fstation, tstation, distp[i], ((xcbear + 90) % 360)))
			elif slrud[0] == "U":
				fout.write("%s  %s %.2f - up\n" % (fstation, tstation, distp[i]))
			elif slrud[0] == "D":
				fout.write("%s  %s %.2f - down\n" % (fstation, tstation, distp[i]))
			fstation = tstation

	def writexcleg(self, mlrud, fout, belevations):
		lrudv = [ ]
		(station, sL, sL2, sR, sR2, sU, sU2, sD, sD2) = (mlrud.group(1), \
							xcv(mlrud.group(2)), xcv(mlrud.group(3)), \
							xcv(mlrud.group(4)), xcv(mlrud.group(5)), \
							xcv(mlrud.group(6)), xcv(mlrud.group(7)), \
							xcv(mlrud.group(8)), xcv(mlrud.group(9)))

		# find likely leg pair (usually two consecutive ones)
		xclegpair = self.findxclegpair(station)
		if not xclegpair:
			fout.write(";cannot resolve xc legs\n\n")
			return

		# first leg from station
		xcbear = xclegpair[0].compass
		if not xclegpair[0].bforeleg:
			xcbear = (xcbear + 180.0) % 360

		blargebearingchange = False
		if len(xclegpair) == 1:
			fout.write(";perp to leg %s -> %s\n" % (xclegpair[0].bforeleg and (xclegpair[0].sfrom, xclegpair[0].sto) or (xclegpair[0].sto, xclegpair[0].sfrom)))

		# second leg from station
		else:
			fout.write(";bisector to leg %s -> %s" % (xclegpair[0].bforeleg and (xclegpair[0].sfrom, xclegpair[0].sto) or (xclegpair[0].sto, xclegpair[0].sfrom)))
			fout.write(" -> %s\n" % (xclegpair[1].bforeleg and xclegpair[1].sto or xclegpair[1].sfrom))

			xcbear2 = xclegpair[1].compass
			if not xclegpair[1].bforeleg:
				xcbear2 = (xcbear2 + 180.0) % 360

			# factor out changes in angle of more than 90 degrees
			if xcbear2 > xcbear:
				xcbearT = xcbear2
				xcbear2 = xcbear
				xcbear = xcbearT
			if xcbear2 < xcbear - 90:
				xcbear += 360 # might cross the zero point
				if xcbear < xcbear2 - 90:
					blargebearingchange = True
			if not blargebearingchange:
				xcbear = ((xcbear + xcbear2) / 2) % 360

		if blargebearingchange:
			fout.write(";large bearing change, left-right omitted\n")
		elif not belevations:
			self.writexcfrag(station, sL, xcbear, "L", fout)
			self.writexcfrag(station, sL2, xcbear, "L2", fout)
			self.writexcfrag(station, sR, xcbear, "R", fout)
			self.writexcfrag(station, sR2, xcbear, "R2", fout)
		self.writexcfrag(station, sU, xcbear, "U", fout)
		self.writexcfrag(station, sU2, xcbear, "U2", fout)
		self.writexcfrag(station, sD, xcbear, "D", fout)
		self.writexcfrag(station, sD2, xcbear, "D2", fout)
		fout.write("\n")

