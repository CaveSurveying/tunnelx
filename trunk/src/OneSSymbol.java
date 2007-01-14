////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2002  Julian Todd.
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

import java.awt.Graphics2D;
import java.util.Vector;
import java.util.Random;
import java.util.Arrays;
import java.lang.StringBuffer;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.BasicStroke;
import java.awt.Color;



/////////////////////////////////////////////
// single symbol.
class SSymbSing
{
	GeneralPath transcliparea = null;
	Area atranscliparea = null; // area of the above.

	Vector viztranspaths = new Vector();

	// could transform the cliparea here too.

	// this is the transformation from axis of gsym to paxis here.
	AffineTransform paxistrans = new AffineTransform();

	int splaceindex;

	/////////////////////////////////////////////
	void MakeTransformedPaths(OneSSymbol oss, int lsplaceindex)
	{
		splaceindex = lsplaceindex;

		// the transformed paths.
		viztranspaths.clear();
		for (int j = 0; j < oss.ssb.gsym.vpaths.size(); j++)
		{
			OnePath path = (OnePath)oss.ssb.gsym.vpaths.elementAt(j);
			if (((path.linestyle >= SketchLineStyle.SLS_WALL) && (path.linestyle <= SketchLineStyle.SLS_DETAIL)) || (path.linestyle == SketchLineStyle.SLS_FILLED) || ((path.linestyle != SketchLineStyle.SLS_CENTRELINE) && (path.plabedl != null)))
			{
				OnePath tpath = new OnePath(path, paxistrans);
				viztranspaths.add(tpath);
			}
		}
	}
};




/////////////////////////////////////////////
class OneSSymbol
{
	// when we have multisumbols, up to the transformed paths,
	// the info based on the pos of the axis could be shared.

	// arrays of sketch components.

	// location definition
	Line2D paxis;
 	SSymbolBase ssb;
	OnePath op; // used to access the line width for detail lines for the subset associated to this symbol (by connective path).

// the following needs to be repeated in arrays so that we can have multi boulder symbols.
	Vector symbmult = new Vector(); // of SSymbSing given multiplicity.
	int nsmposvalid = 0; // number of symbols whose position is valid for drawing of the multiplicity.


	// these are used to mark the symbols for interference scanning.  more efficient than setting false it as a booleans.
	int ismark = 0; // marks if it has been checked for interference during layout already
	static int ismarkl = 1;
	int islmark = 0; // marks if it has been layed out.
	static int islmarkl = 1;


	// one to do it all for now.
	static SSymbScratch sscratch = new SSymbScratch();


	/////////////////////////////////////////////
	void RefreshSymbol(OneTunnel vgsymbols)
	{
		nsmposvalid = 0;

		// sort out the axis
		sscratch.InitAxis(this, true, null);

		// add in a whole bunch of (provisional) positions.
		for (int ic = 0; ic < symbmult.size(); ic++)
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);
			sscratch.BuildAxisTrans(ssing.paxistrans, this, ic);
			ssing.MakeTransformedPaths(this, ic);
		}
	}

	/////////////////////////////////////////////
	// the intersecting checking bit.
	boolean IsSymbolsPositionValid(Area lsaarea, SSymbSing ssing, Vector ssymbinterf)
	{
		Area awork = new Area();

		// first check if the symbol is in the area if it's supposed to be
		if (!ssb.bAllowedOutsideArea)
		{
			awork.add(ssing.atranscliparea);
			awork.subtract(lsaarea);
			if (!awork.isEmpty())
				return false; // the area goes outside.
  		}

		// but if symbol entirely outside, no point in having it here.
		else if (ssb.bTrimByArea)
		{
			awork.add(ssing.atranscliparea);
			awork.intersect(lsaarea);
			if (awork.isEmpty())
				return false;
			awork.reset();
		}

		if (ssb.bSymbolinterferencedoesntmatter)
			return true;

		// we will now find all the symbols which are in an area which overlaps this one.
		// and work on those that have already been layed, checking for interference.
		// this list of paths contains the current path, so tests against those symbols automatically
		OneSSymbol.ismarkl++;
		for (int i = 0; i < ssymbinterf.size(); i++)
		{
			OnePath op = (OnePath)ssymbinterf.elementAt(i);
			for (int k = 0; k < op.vpsymbols.size(); k++)
			{
				OneSSymbol oss = (OneSSymbol)op.vpsymbols.elementAt(k);

				// check for already layed out, but not tested against before.
				if ((oss.islmark == OneSSymbol.islmarkl) && (oss.ismark != OneSSymbol.ismarkl))
				{
					// now scan through the valid pieces in this symbol.
					// works when scanning self (oss == this) because multi taken only up to nsmposvalid
					for (int j = 0; j < oss.nsmposvalid; j++)
					{
						SSymbSing jssing = (SSymbSing)oss.symbmult.elementAt(j);
						if (jssing.atranscliparea != null)
						{
							awork.add(ssing.atranscliparea);
							awork.intersect(jssing.atranscliparea);
							if (!awork.isEmpty())
								return false;
						}
					}
				}
			}
		}

		return true;
	}



	/////////////////////////////////////////////
	// this does pullback in a line, but also copes with the case where no pulling happens.
	boolean RelaySymbolT(SSymbSing ssing, Area lsaarea, Vector ssymbinterf)
	{
		sscratch.placeindex++;

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
		boolean lam1valid = IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

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
		boolean lam0valid = IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

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
			boolean lammidvalid = IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

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



	/////////////////////////////////////////////
	// loop over the random variation.
	boolean RelaySymbol(SSymbSing ssing, Area lsaarea, Vector ssymbinterf)
	{
		// use of sscratch.placeindex is hack over multiple symbols
		for (int ip = 0; ip < sscratch.noplaceindexlimitrand; ip++)
		{
			if (!sscratch.BuildAxisTrans(ssing.paxistrans, this, sscratch.placeindexabs)) // changed from placeindex!
				return false;
			sscratch.placeindexabs++;
			if (RelaySymbolT(ssing, lsaarea, ssymbinterf))
			{
				ssing.MakeTransformedPaths(this, sscratch.placeindex);
				return true;
			}

			// quit if not a random moving type.
			if (!ssb.bMoveable && (ssb.iLattice == 0))
				break;
		}
		return false;
	}

	/////////////////////////////////////////////

	/////////////////////////////////////////////
	void RelaySymbolsPosition(SketchSymbolAreas sksya, ConnectiveComponentAreas pthcca)
	{
		Vector ssymbinterf = new Vector(); // list of interfering symbols
		// start with no valid positions
		nsmposvalid = 0;

		if ((pthcca == null) || (ssb.gsym == null))
			return; // no areas to be in.
		Area lsaarea = pthcca.saarea;

		// colour filling type.  Hack it in
		if (ssb.symbolareafillcolour != null)
			return;

		// sort out the axis
		sscratch.InitAxis(this, true, lsaarea);

        // fetch the list of paths which can have interfering symbols (really a subset of the components)
		// this should contain itself
		sksya.GetInterferingSymbols(ssymbinterf, pthcca);

		// add in a whole bunch of (provisional) positions.
		sscratch.placeindex = 0; // layout index variables.
		sscratch.placeindexabs = 0;
		sscratch.noplaceindexlimitpullback = 20; // layout index variables.
		sscratch.noplaceindexlimitrand = 20;

		while ((nsmposvalid < ssb.nmultiplicity) || (ssb.nmultiplicity == -1))
		{
			// roll on new symbols as we run further up the array.
			if (nsmposvalid == symbmult.size())
				symbmult.addElement(new SSymbSing());

			SSymbSing ssing = (SSymbSing)symbmult.elementAt(nsmposvalid);
			if (!RelaySymbol(ssing, lsaarea, ssymbinterf))
				break;
			nsmposvalid++;

			if ((ssb.maxplaceindex != -1) && (sscratch.placeindex > ssb.maxplaceindex))
				break;
		}

		if (sscratch.placeindex > 1)
			TN.emitMessage("S:" + ssb.gsymname + "  placeindex  " +
							((ssb.maxplaceindex != -1) && (sscratch.placeindex > ssb.maxplaceindex) ? "(maxed) ": "") +
							sscratch.placeindex + " of symbols " + nsmposvalid);
	}


	/////////////////////////////////////////////
	static Color colsymoutline = new Color(1.0F, 0.8F, 0.8F);
//	static Color colsymactivearea = new Color(1.0F, 0.2F, 1.0F, 0.16F);
	void paintW(GraphicsAbstraction ga, boolean bActive, boolean bProperSymbolRender)
	{

		int nic = (bProperSymbolRender || (nsmposvalid != 0) ? nsmposvalid : symbmult.size());
		for (int ic = 0; ic < nic; ic++)
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);

			LineStyleAttr linelsa = (bActive ? SketchLineStyle.lineactivestylesymb : 
									(ic < nsmposvalid ? 
										(ic == 0 ? SketchLineStyle.linestylefirstsymb : SketchLineStyle.linestylesymb) : 
										(ic == 0 ? SketchLineStyle.linestylefirstsymbinvalid : SketchLineStyle.linestylesymbinvalid)));
			LineStyleAttr filllsa = (bActive ? SketchLineStyle.fillactivestylesymb : 
									(ic < nsmposvalid ? 
										(ic == 0 ? SketchLineStyle.fillstylefirstsymb : SketchLineStyle.fillstylesymb) : 
										(ic == 0 ? SketchLineStyle.fillstylefirstsymbinvalid : SketchLineStyle.fillstylesymbinvalid)));
			for (int j = 0; j < ssing.viztranspaths.size(); j++)
			{
				OnePath sop = (OnePath)ssing.viztranspaths.elementAt(j);
				if (sop != null)
				{
					if (sop.linestyle == SketchLineStyle.SLS_FILLED)
						ga.drawPath(sop, filllsa);
					else if (sop.linestyle != SketchLineStyle.SLS_CONNECTIVE)
					{
						if (linelsa !=null)
							ga.drawPath(sop, linelsa);
					}

					// shouldn't happen
					else if ((sop.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (sop.plabedl != null) && (sop.plabedl.labfontattr != null))
						sop.paintLabel(ga, null);  // how do we know what font to use?  should be from op!
				}
			}
		}
	}

	/////////////////////////////////////////////
	void paintWquality(GraphicsAbstraction ga)
	{
		// demonstrate the mask of the area used to layout the last symbol
		// just a single instant debugging tool
		// ga.drawImage(sscratch.latbi, null, null);

		LineStyleAttr lsaline = op.subsetattr.linestyleattrs[SketchLineStyle.SLS_DETAIL];
		LineStyleAttr lsafilled = op.subsetattr.linestyleattrs[SketchLineStyle.SLS_FILLED];

		//System.out.println("symbval " + symbmult.size() + " " + nsmposvalid);
		int nic = nsmposvalid;
		for (int ic = 0; ic < nic; ic++)
			ga.drawSymbol((SSymbSing)symbmult.elementAt(ic), lsaline, lsafilled);
	}

	/////////////////////////////////////////////
	OneSSymbol()
	{
	}

	/////////////////////////////////////////////
	OneSSymbol(float[] pco, int nlines, float zalt, SSymbolBase lssb, OnePath lop)
	{
		ssb = lssb;
		op = lop;

//		paxis = new Line2D.Float(pco[0], pco[1], pco[nlines * 2], pco[nlines * 2 + 1]);
		paxis = new Line2D.Float(pco[nlines * 2 - 2], pco[nlines * 2 - 1], pco[nlines * 2], pco[nlines * 2 + 1]);

		// sort out the axis
		if (ssb.gsym != null)
		{
			sscratch.InitAxis(this, (symbmult.size() == 0), null);
			// make some provisional positions just to help the display of multiplicity
			int nic = (((ssb.nmultiplicity != -1) && (ssb.nmultiplicity < 2)) ? ssb.nmultiplicity : 2);
			for (int ic = 0; ic < nic; ic++)
			{
				SSymbSing ssing = new SSymbSing();
				sscratch.BuildAxisTrans(ssing.paxistrans, this, symbmult.size());
				ssing.MakeTransformedPaths(this, ic);
				symbmult.addElement(ssing);
			}
		}
	}
}
