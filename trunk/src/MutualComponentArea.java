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

// sanwang 10sec on load     15 at z, 67Mb
// 23sec  110Mb  at Updateareas
// 12:27mins  404Mb


import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;



/////////////////////////////////////////////
// single symbol (temporary)
class TSSymbSing
{
	OneSSymbol oss = null;
	Area atranscliparea = null;
	AffineTransform paxistrans = null;
	int splaceindex;
	TSSymbSing(OneSSymbol loss)
	{
		oss = loss;
	}

	void Setpaxistrans(AffineTransform lpaxistrans)
	{
		paxistrans = lpaxistrans;
		if (oss.ssb.gsym.cliparea != null)
		{
			atranscliparea = (Area)oss.ssb.gsym.cliparea.aarea.clone();
			atranscliparea.transform(paxistrans);
		}
	}
}



////////////////////////////////////////////////////////////////////////////////
// this might even be the object that runs in a separate thread and itself goes through the list of MutualComponentAreas
class MutualComponentAreaScratch
{
	List<OneSSymbol> latticesymbols = new ArrayList<OneSSymbol>();
	List<OneSSymbol> othersymbols = new ArrayList<OneSSymbol>();
	List<OneSSymbol> othersymbol = new ArrayList<OneSSymbol>();  // used as an array of one

	// handles the lattices and axes of symbols
	List<SSymbScratch> sscratcharr = new ArrayList<SSymbScratch>();
	boolean bmorethanoneconnectedcomponent;

	// for laying out mixtures of mud and sand.
	// could also use to weight the random selection
	int[] iactivesymbols = new int[200];
	int niactivesymbols = 0;

	List<TSSymbSing> tssymbinterf = new ArrayList<TSSymbSing>();

	/////////////////////////////////////////////
	// the intersecting checking bit.
	boolean IsSymbolsPositionValid(TSSymbSing tssing)
	{
		Area lsaarea = tssing.oss.op.pthcca.saarea;
	 	SSymbolBase ssb = tssing.oss.ssb;

		Area awork = new Area();

		// first check if the symbol is in the area if it's supposed to be
		if (!ssb.bAllowedOutsideArea)
		{
			awork.add(tssing.atranscliparea);
			awork.subtract(lsaarea);
			if (!awork.isEmpty())
				return false; // the area goes outside.
  		}

		// but if symbol entirely outside, no point in having it here.
		else if (ssb.bTrimByArea)
		{
			awork.add(tssing.atranscliparea);
			awork.intersect(lsaarea);
			if (awork.isEmpty())
				return false;
			awork.reset();
		}

		if (ssb.bSymbolinterferencedoesntmatter)  // although other symbols might care if they overlap this one
			return true;

		// this is the bit which we must apply boxing to
		for (TSSymbSing jssing : tssymbinterf)
		{
			if (jssing.atranscliparea == null)
				continue;
			if (bmorethanoneconnectedcomponent && !tssing.oss.op.pthcca.overlapcomp.contains(jssing.oss.op.pthcca))
			{
				//TN.emitMessage("No overlap possible between: " + tssing.oss.ssb.gsymname + " and " + jssing.oss.ssb.gsymname);
				continue;
			}

			awork.add(tssing.atranscliparea);
			awork.intersect(jssing.atranscliparea);
			if (!awork.isEmpty())
				return false;
		}

		return true;
	}

	/////////////////////////////////////////////
	// majority of code is for pullback in a line, but also copes with the case where no pulling happens.
	TSSymbSing MRelaySymbolT(OneSSymbol oss, SSymbScratch sscratch)
	{
		sscratch.placeindex++;
	 	SSymbolBase ssb = oss.ssb;
		TSSymbSing tssing = new TSSymbSing(oss);

		// make transformed location for lam0.
		double lam1 = (ssb.bPushout ? 2.0 : 1.0); // goes out twice as far.
		tssing.Setpaxistrans(sscratch.BuildAxisTransT(lam1));

		// case of no clipping area associated to the symbol.
		if (ssb.gsym.cliparea == null)
			return tssing;

		boolean lam1valid = IsSymbolsPositionValid(tssing);

		// no pullback case
		if ((!ssb.bPullback && !ssb.bPushout) || (sscratch.pleng * 2 <= ssb.pulltolerance))
			return (lam1valid ? tssing : null);

		sscratch.placeindex++;

		// this is a pull/push type.  record where we are going towards (the push-out direction).
		double lam0 = (ssb.bPullback ? 0.0 : 1.0);

		Area lam1atranscliparea = tssing.atranscliparea;
		AffineTransform lam1axistrans = tssing.paxistrans;

		tssing.Setpaxistrans(sscratch.BuildAxisTransT(lam0));
		boolean lam0valid = IsSymbolsPositionValid(tssing);

		// quick return case where we've immediately found a spot.
		if (lam0valid)
			return tssing;

		AffineTransform lam0axistrans = tssing.paxistrans;
		Area lam0atranscliparea = tssing.atranscliparea;

		// should scan along the line looking for a spot.
		if (!lam0valid && !lam1valid)
			TN.emitMessage("Both ends out, should scan");

		// now we enter a loop to narrow down the range.
		for (int ip = 0; ip < sscratch.noplaceindexlimitpullback; ip++)
		{
			TN.emitMessage("lam scan " + lam0 + (lam0valid ? "(*)" : "( )") + "  " + lam1 + (lam1valid ? "(*)" : "( )"));
			// quit if accurate enough
			if (sscratch.pleng * (lam1 - lam0) <= ssb.pulltolerance)
				break;

			sscratch.placeindex++;

			double lammid = (lam0 + lam1) / 2;

			tssing.Setpaxistrans(sscratch.BuildAxisTransT(lammid));
			boolean lammidvalid = IsSymbolsPositionValid(tssing);

			// decide which direction to favour
			// we should be scanning the intermediate places if neither end is in.
			if (lammidvalid)
			{
				lam1 = lammid;
				lam1atranscliparea = tssing.atranscliparea;
				lam1axistrans = tssing.paxistrans;
				lam1valid = lammidvalid;
			}
			else
			{
				lam0 = lammid;
				lam0atranscliparea = tssing.atranscliparea;
				lam0axistrans = tssing.paxistrans;
				lam0valid = lammidvalid;
			}

			if (lam0valid)
				break;
		}


		if (lam0valid)
		{
			tssing.paxistrans = lam0axistrans;
			tssing.atranscliparea = lam0atranscliparea;
			return tssing;
		}
		if (lam1valid)
		{
			tssing.paxistrans = lam1axistrans;
			tssing.atranscliparea = lam1atranscliparea;
			return tssing;
		}

		return null;
	}


	////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////
	// loop over the random variation. (it's a one time loop if it's a single symbol object)
	TSSymbSing MRelaySymbol(OneSSymbol oss, SSymbScratch sscratch)
	{
		// use of sscratch.placeindex is hack over multiple symbols
		for (int ip = 0; ip < sscratch.noplaceindexlimitrand; ip++)
		{
			if (!sscratch.BuildAxisTransSetup(oss, sscratch.placeindexabs)) // changed from placeindex!
				return null;
			sscratch.placeindexabs++;

			TSSymbSing tssing = MRelaySymbolT(oss, sscratch);
			if (tssing != null)
			{
				tssing.splaceindex = sscratch.placeindex;
				return tssing;
			}

			// quit if not a random moving type.
			if (!oss.ssb.bMoveable && (oss.ssb.iLattice == 0))
				break;
		}
		return null;
	}


	/////////////////////////////////////////////
	void MRelaySymbolsPositionBatch(List<OneSSymbol> osslist)
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

			TSSymbSing tssing = MRelaySymbol(oss, sscratch);
			if (tssing != null)
			{
				tssymbinterf.add(tssing);
				oss.nsmposvalid++;
			}
			else
				blayoutmore = false;

			if ((oss.ssb.nmultiplicity != -1) && (oss.nsmposvalid >= oss.ssb.nmultiplicity))
				blayoutmore = false;
			if ((oss.ssb.maxplaceindex != -1) && (sscratch.placeindex > oss.ssb.maxplaceindex))
				blayoutmore = false;

			if (!blayoutmore)
			{
				// kill off this representative
				iactivesymbols[ixa] = iactivesymbols[--niactivesymbols];

				if (sscratch.placeindex > 1)
					TN.emitMessage("S:" + oss.ssb.gsymname + "  placeindex  " +
									((oss.ssb.maxplaceindex != -1) && (sscratch.placeindex > oss.ssb.maxplaceindex) ? "(maxed) ": "") +
									sscratch.placeindex + " of symbols " + oss.nsmposvalid);
			}
			//else
			//	System.out.println("Lay down: " + oss.ssb.gsymname + "  " + oss.nsmposvalid);
  		}

  		// now go through out set of symbols and copy them into the outer thing.
  		// this is where some compression will be possible
		for (OneSSymbol oss : osslist)
		{
			oss.Dnsmposvalid = oss.nsmposvalid;
			oss.nsmposvalid = 0;
		}


// lots of compression on each path can happen, with it merging all into the same gpath or something
		for (TSSymbSing tssing : tssymbinterf)
		{
			if (tssing.oss.nsmposvalid == tssing.oss.symbmult.size())
				tssing.oss.symbmult.addElement(new SSymbSing());
			SSymbSing ssing = (SSymbSing)tssing.oss.symbmult.elementAt(tssing.oss.nsmposvalid);
//			ssing.atranscliparea = tssing.atranscliparea;
			ssing.paxistrans = tssing.paxistrans;
			ssing.MakeTransformedPaths(tssing.oss, tssing.splaceindex);
			tssing.oss.nsmposvalid++;
		}

		for (OneSSymbol oss : osslist)
			assert oss.Dnsmposvalid == oss.nsmposvalid;
	}


	////////////////////////////////////////////////////////////////////////////////
	void SLayoutMutualSymbols(List<OnePath> vmconnpaths, boolean lbmorethanoneconnectedcomponent)
	{
		// sort the symbols into two batches
		//assert latticesymbols.isEmpty() && othersymbols.isEmpty();
		latticesymbols.clear(); // cleanup in case there had been an exception leaving a mess
		othersymbols.clear();
		bmorethanoneconnectedcomponent = lbmorethanoneconnectedcomponent;

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
			MRelaySymbolsPositionBatch(othersymbol);
		}
		othersymbol.clear();
		othersymbols.clear();

		if (!latticesymbols.isEmpty())
			MRelaySymbolsPositionBatch(latticesymbols);
		latticesymbols.clear();
		tssymbinterf.clear();
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
		mcascratch.SLayoutMutualSymbols(vmconnpaths, (ccamutual.size() > 1));
	}
};

