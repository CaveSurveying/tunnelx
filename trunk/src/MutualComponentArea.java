////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2007  Julian Todd.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.util.Vector;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

////////////////////////////////////////////////////////////////////////////////
// this might even be the object that runs in a separate thread and itself goes through the list of MutualComponentAreas
class MutualComponentAreaScratch
{
	List<OneSSymbol> latticesymbols = new ArrayList<OneSSymbol>();
	List<OneSSymbol> othersymbols = new ArrayList<OneSSymbol>();
	List<OneSSymbol> othersymbol = new ArrayList<OneSSymbol>();  // used as an array of one

	// handles the lattices and axes of symbols
	List<SSymbScratch> sscratcharr = new ArrayList<SSymbScratch>();

	// for laying out mixtures of mud and sand.
	// could also use to weight the random selection
	int[] iactivesymbols = new int[200];
	int niactivesymbols = 0;



	/////////////////////////////////////////////
	// this does pullback in a line, but also copes with the case where no pulling happens.
	boolean MRelaySymbolT(OneSSymbol oss, SSymbSing ssing, Area lsaarea, List<OnePath> ssymbinterf, SSymbScratch sscratch)
	{
		sscratch.placeindex++;
	 	SSymbolBase ssb = oss.ssb;

		// make transformed location for lam0.
		double lam1 = (ssb.bPushout ? 2.0 : 1.0); // goes out twice as far.

		sscratch.BuildAxisTransT(ssing.paxistrans, lam1);

		// case of no clipping area associated to the symbol.
		if (ssb.gsym.cliparea == null)
		{
			ssing.transcliparea = null;
        	ssing.atranscliparea = null;
			return true;
		}

		ssing.transcliparea = (GeneralPath)ssb.gsym.cliparea.gparea.clone();
		ssing.transcliparea.transform(ssing.paxistrans);

		// make the area
		ssing.atranscliparea = new Area(ssing.transcliparea);
		boolean lam1valid = oss.IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

		// cache the results at lam1.
		GeneralPath lam1transcliparea = ssing.transcliparea;
		Area lam1atranscliparea = ssing.atranscliparea;

		// no pullback case
		if ((!ssb.bPullback && !ssb.bPushout) || (sscratch.pleng * 2 <= ssb.pulltolerance))
			return lam1valid;

		sscratch.placeindex++;

		// this is a pull/push type.  record where we are going towards (the push-out direction).
		double lam0 = (ssb.bPullback ? 0.0 : 1.0);

		sscratch.BuildAxisTransT(ssing.paxistrans, lam0);

		// could check containment in boundary box too, to speed things up.
		ssing.transcliparea = (GeneralPath)ssb.gsym.cliparea.gparea.clone();
		ssing.transcliparea.transform(ssing.paxistrans);

		// make the area
		ssing.atranscliparea = new Area(ssing.transcliparea);
		boolean lam0valid = oss.IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

		// quick return case where we've immediately found a spot.
		if (lam0valid)
			return true;

		// cache the results at lam0.
		GeneralPath lam0transcliparea = ssing.transcliparea;
		Area lam0atranscliparea = ssing.atranscliparea;


		// should scan along the line looking for a spot.
		if (!lam0valid && !lam1valid)
		{
			TN.emitMessage("Both ends out, should scan");
		}

		// now we enter a loop to narrow down the range.

		for (int ip = 0; ip < sscratch.noplaceindexlimitpullback; ip++)
		{
			TN.emitMessage("lam scan " + lam0 + " " + lam1);
			// quit if accurate enough
			if (sscratch.pleng * (lam1 - lam0) <= ssb.pulltolerance)
				break;

			sscratch.placeindex++;

			double lammid = (lam0 + lam1) / 2;

			sscratch.BuildAxisTransT(ssing.paxistrans, lammid);

			ssing.transcliparea = (GeneralPath)ssb.gsym.cliparea.gparea.clone();
			ssing.transcliparea.transform(ssing.paxistrans);

			// make the area
			ssing.atranscliparea = new Area(ssing.transcliparea);
			boolean lammidvalid = oss.IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

			// decide which direction to favour
			// we should be scanning the intermediate places if neither end is in.
			if (lammidvalid)
			{
				lam1 = lammid;
				lam1transcliparea = ssing.transcliparea;
				lam1atranscliparea = ssing.atranscliparea;
				lam1valid = lammidvalid;
			}
			else
			{
				lam0 = lammid;
				lam0transcliparea = ssing.transcliparea;
				lam0atranscliparea = ssing.atranscliparea;
				lam0valid = lammidvalid;
			}

			if (lam0valid)
				break;
		}


		// now copy out the range.
		if (!lam0valid && !lam1valid)
			return false;

		if (lam0valid)
		{
			sscratch.BuildAxisTransT(ssing.paxistrans, lam0);
			ssing.transcliparea = lam0transcliparea;
			ssing.atranscliparea = lam0atranscliparea;
		}
		else
		{
			sscratch.BuildAxisTransT(ssing.paxistrans, lam1);
			ssing.transcliparea = lam1transcliparea;
			ssing.atranscliparea = lam1atranscliparea;
		}

		return true;
	}


	////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////
	// loop over the random variation.
	boolean MRelaySymbol(OneSSymbol oss, SSymbSing ssing, List<OnePath> ssymbinterf, SSymbScratch sscratch)
	{
		// use of sscratch.placeindex is hack over multiple symbols
		for (int ip = 0; ip < sscratch.noplaceindexlimitrand; ip++)
		{
			if (!sscratch.BuildAxisTrans(ssing.paxistrans, oss, sscratch.placeindexabs)) // changed from placeindex!
				return false;
			sscratch.placeindexabs++;
			if (MRelaySymbolT(oss, ssing, oss.op.pthcca.saarea, ssymbinterf, sscratch))
			{
				ssing.MakeTransformedPaths(oss, sscratch.placeindex);
				return true;
			}

			// quit if not a random moving type.
			if (!oss.ssb.bMoveable && (oss.ssb.iLattice == 0))
				break;
		}
		return false;
	}


	/////////////////////////////////////////////
	void MRelaySymbolsPositionBatch(List<OneSSymbol> osslist, List<OnePath> ssymbinterf)
	{
		// initialize the axes in the scratch areas
		if (osslist.size() > iactivesymbols.length)
			TN.emitWarning("Toomany active symbols");
		niactivesymbols = Math.min(osslist.size(), iactivesymbols.length);
		for (int i = 0; i < niactivesymbols; i++)
		{
			OneSSymbol oss = osslist.get(i);
			if (sscratcharr.size() <= i)
				sscratcharr.add(new SSymbScratch());  // extend the array
			SSymbScratch sscratch = sscratcharr.get(i);
			iactivesymbols[i] = i;

			oss.islmark = OneSSymbol.islmarkl; // comparison against itself.

			assert oss.nsmposvalid == 0; // set outside
			assert ((oss.op.pthcca != null) && (oss.ssb.gsym != null) && (oss.ssb.symbolareafillcolour == null));
			sscratch.InitAxis(oss, true, oss.op.pthcca.saarea);

			sscratch.placeindex = 0; // layout index variables.
			sscratch.placeindexabs = 0;
			sscratch.noplaceindexlimitpullback = 20; // layout index variables.
			sscratch.noplaceindexlimitrand = 20;
		}

		// now reloop and relayout
		while (niactivesymbols != 0)
		{
			// select random symbol from list (this selection will in future be weighted)
			// not perfect when it combines two areas, one of which also has sand, but interesting enough
			int ixa = sscratcharr.get(0).ran.nextInt(niactivesymbols);
			int i = iactivesymbols[ixa];

			OneSSymbol oss = osslist.get(i);
			SSymbScratch sscratch = sscratcharr.get(i);

			boolean blayoutmore = true;
			// roll on new symbols as we run further up the array.
			if (oss.nsmposvalid == oss.symbmult.size())
				oss.symbmult.addElement(new SSymbSing());

			SSymbSing ssing = (SSymbSing)oss.symbmult.elementAt(oss.nsmposvalid);
			if (!MRelaySymbol(oss, ssing, ssymbinterf, sscratch))
				blayoutmore = false;
			oss.nsmposvalid++;
			if ((oss.ssb.nmultiplicity != -1) && (oss.nsmposvalid >= oss.ssb.nmultiplicity))
				blayoutmore = false;
			if ((oss.ssb.maxplaceindex != -1) && (sscratch.placeindex > oss.ssb.maxplaceindex))
				blayoutmore = false;

			// kill off this representative
			if (!blayoutmore)
			{
				iactivesymbols[ixa] = iactivesymbols[--niactivesymbols];

				if (sscratch.placeindex > 1)
					TN.emitMessage("S:" + oss.ssb.gsymname + "  placeindex  " +
									((oss.ssb.maxplaceindex != -1) && (sscratch.placeindex > oss.ssb.maxplaceindex) ? "(maxed) ": "") +
									sscratch.placeindex + " of symbols " + oss.nsmposvalid);
			}
			//else
			//	System.out.println("Lay down: " + oss.ssb.gsymname);
  		}
	}


	////////////////////////////////////////////////////////////////////////////////
	void SLayoutMutualSymbols(List<OnePath> vmconnpaths)
	{
		// sort the symbols into two batches
		//assert latticesymbols.isEmpty() && othersymbols.isEmpty();
		latticesymbols.clear(); // cleanup in case there had been an exception leaving a mess
		othersymbols.clear();

		for (OnePath op : vmconnpaths)
		{
			for (int j = 0; j < op.vpsymbols.size(); j++)
			{
				OneSSymbol oss = (OneSSymbol)op.vpsymbols.elementAt(j);
				oss.nsmposvalid = 0;
				if ((oss.ssb.symbolareafillcolour == null) && (oss.ssb.gsym != null))
				{
					if (oss.ssb.bBuildSymbolLatticeAcrossArea)
						latticesymbols.add(oss);
					else
						othersymbols.add(oss);
				}
			}
		}

		//	for (ConnectiveComponentAreas ccal : op.pthcca.overlapcomp)
		//		ssymbinterf.addAll(ccal.vconnpaths);
		//	assert ssymbinterf.size() <= vmconnpaths.size();
		//	assert vmconnpaths.containsAll(ssymbinterf);
   		// ssymbinterf is smaller than vmconnpaths

		// OneSSymbol.islmarkl++; happened outside in this cycle

		for (OneSSymbol oss : othersymbols)
		{
			othersymbol.clear();
			othersymbol.add(oss); // one element array
			MRelaySymbolsPositionBatch(othersymbol, vmconnpaths);
		}
		othersymbol.clear();
		othersymbols.clear();

		if (!latticesymbols.isEmpty())
			MRelaySymbolsPositionBatch(latticesymbols, vmconnpaths);
		latticesymbols.clear();
	}
}


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// controls the layout in a set of overlapping component areas
class MutualComponentArea
{
	List<ConnectiveComponentAreas> ccamutual = new ArrayList<ConnectiveComponentAreas>();
	SortedSet<OneSArea> osamutual = new TreeSet<OneSArea>();
	List<OnePath> vmconnpaths = new ArrayList<OnePath>();

	static MutualComponentAreaScratch mcascratch = new MutualComponentAreaScratch();

	////////////////////////////////////////////////////////////////////////////////
	boolean hit(GraphicsAbstraction ga, Rectangle windowrect)
	{
		for (OneSArea osa : osamutual)   // could equally loop through ccamutual and look at saarea
		{
			if ((osa.aarea != null) && ga.hit(windowrect, osa.aarea, false))
				return true;
		}
		return false;
	}


	////////////////////////////////////////////////////////////////////////////////
	void LayoutMutualSymbols() // all symbols in this batch
	{
		mcascratch.SLayoutMutualSymbols(vmconnpaths);
	}
};

