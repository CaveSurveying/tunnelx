import re
import string
import sys
import os
import Image
import ImageDraw

from svxrecords import svxleg, svxleginfo, svxblock
from svxload import svxloader
from svxnetwork import Getlegbbox


class passagedata:
	def __init__(self, passagename):
		self.passagename = passagename
		self.legs = [ ]
		self.length = 0.0
		self.dates = [ ]
		self.teams = [ ]
		self.joiningpassages = [ ]
		self.description = "<p>No Description</p>"

	def addleg(self, leg):
		self.legs.append(leg)
		if not leg.leginfo.flags["splay"] and not leg.leginfo.flags["duplicate"] and not leg.leginfo.flags["surface"]:
			self.length += leg.tape
		if leg.leginfo.date not in self.dates:
			self.dates.append(leg.leginfo.date)
		for t in leg.leginfo.team:
			if t not in self.teams:
				self.teams.append(t)

		# search all joining legs to this passage
		for e in range(2):
			sx = (e and leg.sxto or leg.sxfrom)
			for jleg in sx.legs:
				if jleg.leginfo.title != self.passagename and jleg.leginfo.title not in self.joiningpassages:
					self.joiningpassages.append(jleg.leginfo.title)


	def write(self, fout, imname, imnamec, passagemap):

		fout.write('<h2><a name="%s"></a>%s</h2>\n' % (self.aref, self.passagename))

		fout.write('<table width="100%"><tr><td width="70%" valign="top">\n')

		fout.write('<table>\n')

		fout.write('<tr><td width="40%"><b>Neighbouring passages:</b></td><td>\n')
		pjpass = None
		for jpass in self.joiningpassages:
			if pjpass:
				fout.write(', ')
			fout.write('<a href="#%s">%s</a>' % (passagemap[jpass].aref, jpass))
			pjpass = jpass
		fout.write('</td></tr>\n')

		fout.write('<tr><td><b>Dates of exploration:</b> </td><td> %s </td></tr>\n' % string.join(self.dates, ", "))
		fout.write('<tr><td><b>Passage length:</b> </td><td> %.1f </td></tr>\n' % self.length)
		fout.write('<tr><td><b>Number of survey legs:</b> </td><td> %d </td></tr>\n' % len(self.legs))

		# build up the bracketed list by merging duplicate names
		fout.write('<tr><td><b>Surveying teams:</b> </td><td>\n')
		self.teams.sort()
		prevt = None
		for t in self.teams:
			if prevt and prevt[0] == t[0]:
				if prevt[1] != t[1]:
					fout.write(", %s" % t[1]);
			else:
				if prevt:
					fout.write("), ")
				fout.write("%s (%s" % t)
			prevt = t
		if prevt:
			fout.write(")")
		fout.write('</td></tr>\n')
		fout.write('</table>\n')

		fout.write('<h3>Description</h3>\n');
		fout.write(self.description)
		fout.write('\n');

		if imname:
			if not imnamec:
				fout.write('</td><td valign="top"><img src="%s">\n' % imname)
			else:
				fout.write('</td><td valign="top"><table><tr><td><img src="%s"></td></tr><tr><td><img src="%s"></td></tr></table>\n' % (imname, imnamec))


		fout.write("</td></tr></table>\n")


def scaxy(x, y, bbox):
	return (int(200 * (x - bbox[0][0]) / (bbox[0][1] - bbox[0][0])), int(200 * (y - bbox[1][1]) / (bbox[1][0] - bbox[1][1])))

def RenderPassage(legs, passagedata, bbox, imname):
	im = Image.new("RGB", (200, 200))
	imd = ImageDraw.Draw(im)
	for leg in legs:
		if leg.leginfo.title == passagedata.passagename:
			imd.setink((105, 255, 80))
		elif leg.leginfo.title in passagedata.joiningpassages:
			imd.setink((111, 50, 120))
		else:
			imd.setink((60, 60, 80))

		p0 = scaxy(leg.sxfrom.pos[0], leg.sxfrom.pos[1], bbox)
		p1 = scaxy(leg.sxto.pos[0], leg.sxto.pos[1], bbox)
		imd.line((p0[0], p0[1], p1[0], p1[1]))
	print "Saving image", imname
	im.save(imname)


def ImportCavePassageData(passagemap, cavedatabasefile):
	print "Scanning '%s' for passage descriptions" % cavedatabasefile

	fin = open(cavedatabasefile)
	cavedata = fin.read()  # the whole thing as a string
	fin.close()

	for pd in re.findall('<Section\s+name="(.*?)"[\s\S]*?<Description>([\s\S]*?)</Description>', cavedata):
		if pd[0] in passagemap:
			# we have a description; mark up the passage names
			# after splitting on the em flags (although they shouldn't be necessary)
			spd = re.split('<em class="passage">(.*?)</em>', pd[1])
			lspd = [ ]
			for p in spd:
				if p in passagemap:
					lspd.append('<a href="#%s">%s</a>' % (passagemap[p].aref, p))
				else:
					lspd.append(p)
			passagemap[pd[0]].description = string.join(lspd)

# function which builds a table of all we know about the passages
def PassageInfo(legs, fout, bbox, imdire, sfileroot, cavedatabasefile):

	# put all the legs into passages
	passagemap = { }
	for leg in legs:
		if leg.leginfo.title not in passagemap:
			passagemap[leg.leginfo.title] = passagedata(leg.leginfo.title)
		passagemap[leg.leginfo.title].addleg(leg)

	# make the internal hyperlinks
	passages = passagemap.keys()
	passages.sort()
	for i in range(len(passages)):
		passage = passagemap[passages[i]]
		passage.aref = "pref%d" % i

	# import any descriptions from the database file
	if cavedatabasefile:
		ImportCavePassageData(passagemap, cavedatabasefile)

	passimname = "passim" + sfileroot[-min(6, len(sfileroot)):]
	n = 1
	for passagen in passages:
		passage = passagemap[passagen]
		if bbox and imdire:
			imname = os.path.join(imdire, "%s%d.png" % (passimname, n))
			RenderPassage(legs, passage, bbox, imname)

			# close-up view of the passage
			imnamec = os.path.join(imdire, "%sC%d.png" % (passimname, n))
			lpbbox = Getlegbbox(passage.legs)
			bboxoff = max(lpbbox[0][1] - lpbbox[0][0], lpbbox[1][1] - lpbbox[1][0])
			pbbox = [[lpbbox[0][0] - bboxoff, lpbbox[0][1] + bboxoff], [lpbbox[1][0] - bboxoff, lpbbox[1][1] + bboxoff]]
			RenderPassage(legs, passage, pbbox, imnamec)
		else:
			imname = None
			imnamec = None
		passage.write(fout, imname, imnamec, passagemap)
		n += 1




