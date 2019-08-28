
from svxrecords import svxstation


# build the network of nodes and legs from the survey data we have
def EquateNodes(rootblock):

	# iterate down tree and collect all blocks
	# and legs and equates
	legs = [ ]
	equates = [ ]

	blocks = [ rootblock ]
	ibd = 0
	while ibd < len(blocks):
		legs.extend(blocks[ibd].legs)
		equates.extend(blocks[ibd].equates)
		blocks.extend(blocks[ibd].descblocks)
		ibd += 1

	# find any overlapping equates and merge them
	stationmap = { }
	eqoverlaps = { }
	for i in range(len(equates)):
		eqoverlaps[i] = [ ]
		for eq in equates[i]:
			if eq in stationmap:
				eqoverlaps[stationmap[eq]].append(i)
				print "overlap found"
			else:
				stationmap[eq] = i

	# merge the equates that overlap
	mequates = [ ]
	for i in range(len(equates)):
		if not equates[i]:
			continue
		mequate = [ ]
		meqstack = [ i ]
		while meqstack:
			j = meqstack.pop()
			for eq in equates[j]:
				if eq not in mequate:
					mequate.append(eq)
			meqstack.extend(eqoverlaps[j])
			equates[j] = None
		mequates.append(mequate)

	# now we have non-overlapping sets of stations
	# build a map from station names to stations
	stationmap = { }
	for meq in mequates:
		rsta = svxstation(min(meq))
		for eq in meq:
			assert eq not in stationmap
			stationmap[eq] = rsta

	# go through the legs and link them up
	for leg in legs:
		if leg.sfrom not in stationmap:
			stationmap[leg.sfrom] = svxstation(leg.sfrom)
		leg.sxfrom = stationmap[leg.sfrom]
		leg.sxfrom.legs.append(leg)
		if leg.sto not in stationmap:
			stationmap[leg.sto] = svxstation(leg.sto)
		leg.sxto = stationmap[leg.sto]
		leg.sxto.legs.append(leg)

	return legs



def rgbbox(rg, v):
	if not rg:
		rg.append(v)
		rg.append(v)
	elif v < rg[0]:
		rg[0] = v
	elif v > rg[1]:
		rg[1] = v

def Getlegbbox(legs):
	bbox = [[ ], [ ]]
	for leg in legs:
		rgbbox(bbox[0], leg.sxfrom.pos[0])
		rgbbox(bbox[1], leg.sxfrom.pos[1])
		rgbbox(bbox[0], leg.sxto.pos[0])
		rgbbox(bbox[1], leg.sxto.pos[1])
	return bbox

def StationCalculation(legs):
	npiece = 0  # number of pieces
	for leg in legs:
		if not leg.sxfrom.pos:
			nodestack = [ leg.sxfrom ]
			leg.sxfrom.pos = (npiece * 1000.0, 0.0, 0.0)
			npiece += 1
			while nodestack:
				node = nodestack.pop()
				for nleg in node.legs:
					if nleg.sxfrom == node and not nleg.sxto.pos:
						nleg.sxto.pos = (nleg.sxfrom.pos[0] + nleg.vec[0], nleg.sxfrom.pos[1] + nleg.vec[1], nleg.sxfrom.pos[2] + nleg.vec[2])
						nodestack.append(nleg.sxto)

					if nleg.sxto == node and not nleg.sxfrom.pos:
						nleg.sxfrom.pos = (nleg.sxto.pos[0] - nleg.vec[0], nleg.sxto.pos[1] - nleg.vec[1], nleg.sxto.pos[2] - nleg.vec[2])
						nodestack.append(nleg.sxfrom)
	print npiece

