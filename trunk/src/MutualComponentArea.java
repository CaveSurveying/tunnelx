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

// with SSymbSing removed
// 21sec  112Mb  at Updateareas
// 13:11mins   153Mb

import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;



/////////////////////////////////////////////
// single symbol (temporary), and used as an element in the boxsetting
class TSSymbSing
{
	OneSSymbol oss = null;
	Area atranscliparea = null;
	AffineTransform paxistrans = null;  // maybe not necessary
	int splaceindex;  // not used

	Rectangle2D abounds = null;
	int xilo, xihi;
	int yilo, yihi;
	boolean bxyouter; // set when we overlap beyond the set of boxes

	int ivisited = 0;

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

	// this is the basic list
	List<TSSymbSing> tssymbinterf = new ArrayList<TSSymbSing>();

	// boxing will be done with a matrix of matrices which we stack up
	List< List<TSSymbSing> > boxarray = new ArrayList< List<TSSymbSing> >();
	Deque< List<TSSymbSing> > sparecells = new ArrayDeque< List<TSSymbSing> >();
	int[] cellsused = new int[5000];
	int ncellsused = 0;

	double boxxlo, boxxhi;
	int boxix;
	double boxylo, boxyhi;
	int boxiy;
	int boxn;
	static double boxeps = 0.0001;

	int Givisited = 1;

	/////////////////////////////////////////////
	List<TSSymbSing> GetBoxCell(int ix, int iy)
	{
		int iin = (ix != -1 ? ix * boxiy + iy : boxn);
		if (boxarray.get(iin) == null)
		{
			if (!sparecells.isEmpty())
				boxarray.set(iin, sparecells.removeFirst());
			else
				boxarray.set(iin, new ArrayList<TSSymbSing>());
			if (ncellsused < cellsused.length)
				cellsused[ncellsused++] = iin;
		}
		return boxarray.get(iin);
	}


	/////////////////////////////////////////////
	void FreeBoxCells()
	{
		if (ncellsused < cellsused.length)
		{
			while (ncellsused != 0)
			{
				int iin = cellsused[--ncellsused];
				boxarray.get(iin).clear();
				sparecells.addFirst(boxarray.get(iin));
				boxarray.set(iin, null);
			}
		}
		else
		{
			for (int iin = 0; iin < boxarray.size(); iin++)
			{
				if (boxarray.get(iin) != null)
				{
 					boxarray.get(iin).clear();
 					sparecells.addFirst(boxarray.get(iin));
					boxarray.set(iin, null);
				}
				ncellsused = 0;
			}
		}
		tssymbinterf.clear();
	}

	/////////////////////////////////////////////
	void BuildBoxset(Rectangle2D mbounds, double cellwidth)
	{
		if (mbounds == null)
		{
			boxix = 0;
			boxn = 0;
			return;
		}
		boxxlo = mbounds.getX();
		boxxhi = boxxlo + mbounds.getWidth();
		boxylo = mbounds.getY();
		boxyhi = boxylo + mbounds.getHeight();

		if (cellwidth == 0.0)
			cellwidth = (boxxhi - boxxlo) / 10.0;
		boxix = Math.min((int)((boxxhi - boxxlo) / cellwidth) + 2, 100);
		boxiy = Math.min((int)((boxyhi - boxylo) / cellwidth) + 2, 100);
		//System.out.println("box xx " + boxxlo + ",, " + boxxhi + "  " + boxix + "-" + boxiy);
		boxn = boxix * boxiy;

		// ensure size
		while (boxarray.size() <= boxn)
			boxarray.add(null);

		Givisited = 1;
	}

	/////////////////////////////////////////////
	static int IBox(double boxwlo, double boxwhi, int boxiw, double w)
	{
		int res = (int)((w - boxwlo) / (boxwhi - boxwlo) * boxiw);
		return (res < 0 ? 0 : (res >= boxiw ? boxiw - 1 : res));
	}

	/////////////////////////////////////////////
	void SetBoxRangeOnSymbol(TSSymbSing tssing)
	{
		tssing.abounds = tssing.atranscliparea.getBounds2D();

		double xlo = tssing.abounds.getX() - boxeps;
		double xhi = tssing.abounds.getX() + tssing.abounds.getWidth() + boxeps;
		double ylo = tssing.abounds.getY() - boxeps;
		double yhi = tssing.abounds.getY() + tssing.abounds.getHeight() + boxeps;

		tssing.bxyouter = ((xlo < boxxlo) || (xhi > boxxhi) || (ylo < boxylo) || (yhi > boxyhi));
		tssing.xilo = IBox(boxxlo, boxxhi, boxix, xlo);
		tssing.xihi = IBox(boxxlo, boxxhi, boxix, xhi);
		tssing.yilo = IBox(boxylo, boxyhi, boxiy, ylo);
		tssing.yihi = IBox(boxylo, boxyhi, boxiy, yhi);

		//System.out.print("BoxiiiI " + tssing.xilo + "|" + tssing.xihi + "   " + tssing.yilo + "|" + tssing.yihi);
	}

	/////////////////////////////////////////////
	void AddInterfToBoxset(TSSymbSing tssing)
	{
		tssymbinterf.add(tssing);
		if (tssing.bxyouter)
			GetBoxCell(-1, -1).add(tssing);
		for (int ix = tssing.xilo; ix <= tssing.xihi; ix++)
		for (int iy = tssing.yilo; iy <= tssing.yihi; iy++)
			GetBoxCell(ix, iy).add(tssing);
	}


	/////////////////////////////////////////////
	boolean CheckJOverlaps(TSSymbSing jssing, TSSymbSing tssing, Area awork)
	{
		if (jssing.ivisited == Givisited)
			return false;
		jssing.ivisited = Givisited;
		if (jssing.atranscliparea == null)
			return false;
			//TN.emitMessage("No overlap possible between: " + tssing.oss.ssb.gsymname + " and " + jssing.oss.ssb.gsymname);
		if (bmorethanoneconnectedcomponent && !tssing.oss.op.pthcca.overlapcomp.contains(jssing.oss.op.pthcca))
			return false;

		if (!tssing.abounds.intersects(jssing.abounds.getX(), jssing.abounds.getY(), jssing.abounds.getWidth(), jssing.abounds.getHeight()))
			return false;

		// intersect the two areas to see if it's non-zero
		awork.add(tssing.atranscliparea);
		awork.intersect(jssing.atranscliparea);
		if (!awork.isEmpty())
			return true;

		return false;
	}

	/////////////////////////////////////////////
	boolean CheckJOverlapsArr(List<TSSymbSing> jssingarr, TSSymbSing tssing, Area awork)
	{
		for (TSSymbSing jssing : jssingarr)
		{
			if (CheckJOverlaps(jssing, tssing, awork))
				return true;
		}
		return false;
	}

	/////////////////////////////////////////////
	// the intersecting checking bit.
	boolean IsSymbolsPositionValid(TSSymbSing tssing)
	{
		SetBoxRangeOnSymbol(tssing);

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

		Givisited++;

		// this is doing the check without boxing
		//if (CheckJOverlapsArr(tssymbinterf, tssing, awork))
		//	return false;
		/*for (TSSymbSing jssing : tssymbinterf)
		{
			if (CheckJOverlaps(jssing, tssing, awork))
				return false;
		}*/

		// this is with boxing
		if (tssing.bxyouter && CheckJOverlapsArr(GetBoxCell(-1, -1), tssing, awork))
			return false;
		for (int ix = tssing.xilo; ix <= tssing.xihi; ix++)
		for (int iy = tssing.yilo; iy <= tssing.yihi; iy++)
		{
			if (CheckJOverlapsArr(GetBoxCell(ix, iy), tssing, awork))
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

		TSSymbSing tssinglam1 = new TSSymbSing(oss);

		// make transformed location for lam0.
		double lam1 = (ssb.bPushout ? 2.0 : 1.0); // goes out twice as far.
		tssinglam1.Setpaxistrans(sscratch.BuildAxisTransT(lam1));

		// case of no clipping area associated to the symbol.
		if (ssb.gsym.cliparea == null)
			return tssinglam1;

		boolean lam1valid = IsSymbolsPositionValid(tssinglam1);

		// no pullback case
		if ((!ssb.bPullback && !ssb.bPushout) || (sscratch.pleng * 2 <= ssb.pulltolerance))
			return (lam1valid ? tssinglam1 : null);

		sscratch.placeindex++;

		// this is a pull/push type.  record where we are going towards (the push-out direction).
		double lam0 = (ssb.bPullback ? 0.0 : 1.0);

		//Area lam1atranscliparea = tssing.atranscliparea;
		//AffineTransform lam1axistrans = tssing.paxistrans;

		TSSymbSing tssinglam0 = new TSSymbSing(oss);
		tssinglam0.Setpaxistrans(sscratch.BuildAxisTransT(lam0));
		boolean lam0valid = IsSymbolsPositionValid(tssinglam0);

		// quick return case where we've immediately found a spot.
		if (lam0valid)
			return tssinglam0;

		//AffineTransform lam0axistrans = tssing.paxistrans;
		//Area lam0atranscliparea = tssing.atranscliparea;

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
			TSSymbSing tssinglammid = new TSSymbSing(oss);

			tssinglammid.Setpaxistrans(sscratch.BuildAxisTransT(lammid));
			boolean lammidvalid = IsSymbolsPositionValid(tssinglammid);

			// decide which direction to favour
			// we should be scanning the intermediate places if neither end is in.
			if (lammidvalid)
			{
				lam1 = lammid;
				//lam1atranscliparea = tssing.atranscliparea;
				//lam1axistrans = tssing.paxistrans;
				tssinglam1 = tssinglammid;
				lam1valid = lammidvalid;
			}
			else
			{
				lam0 = lammid;
				//lam0atranscliparea = tssing.atranscliparea;
				//lam0axistrans = tssing.paxistrans;
				tssinglam0 = tssinglammid;
				lam0valid = lammidvalid;
			}

			if (lam0valid)
				break;
		}


		if (lam0valid)
		{
			//tssing.paxistrans = lam0axistrans;
			//tssing.atranscliparea = lam0atranscliparea;
			return tssinglam0;
		}
		if (lam1valid)
		{
			//tssing.paxistrans = lam1axistrans;
			//tssing.atranscliparea = lam1atranscliparea;
			return tssinglam1;
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

			oss.gpsymps = null;
		}

		// now reloop and relayout, selecting at random
		while (niactivesymbols != 0)
		{
			// select random symbol from list (this selection will in future be weighted)
			// not perfect when it combines two areas, one of which also has sand, but interesting enough
			int ixa = sscratcharr.get(0).ran.nextInt(niactivesymbols);
			int i = iactivesymbols[ixa];

			OneSSymbol oss = osslist.get(i);
			SSymbScratch sscratch = sscratcharr.get(i);

			boolean blayoutmore = true;

			TSSymbSing tssing = MRelaySymbol(oss, sscratch);
			if (tssing != null)
			{
				AddInterfToBoxset(tssing);
				oss.nsmposvalid++;
				tssing.oss.AppendTransformedCopy(tssing.paxistrans);
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
			for (OneSSymbol oss : op.vpsymbols)
			{
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

	Rectangle2D mbounds = null;
	double sumsymdim = 0.0;
	int nsymdim = 0;

	static MutualComponentAreaScratch mcascratch = new MutualComponentAreaScratch();

	////////////////////////////////////////////////////////////////////////////////
	void MergeIn(ConnectiveComponentAreas scca)
	{
		ccamutual.add(scca);
		osamutual.addAll(scca.vconnareas);
		vmconnpaths.addAll(scca.vconnpaths);
		assert (scca.pvconncommutual == null);
		scca.pvconncommutual = this;

		if (scca.saarea != null)
		{
			if (mbounds == null)
				mbounds = scca.saarea.getBounds2D();
			else
				mbounds.add(scca.saarea.getBounds2D());
		}

		// collate some kind of average symbol width and height
		for (OnePath op : scca.vconnpaths)
		{
			for (OneSSymbol oss : op.vpsymbols)
			{
				sumsymdim += oss.ssb.avgsymdim;
				nsymdim += (oss.ssb.avgsymdim != 0.0 ? 1 : 0);
			}
		}
	}


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
		mcascratch.BuildBoxset(mbounds, (nsymdim != 0 ? sumsymdim / nsymdim : 0.0));
		mcascratch.SLayoutMutualSymbols(vmconnpaths, (ccamutual.size() > 1));
		mcascratch.FreeBoxCells();
	}
};

