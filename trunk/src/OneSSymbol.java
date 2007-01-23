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

import java.util.List;


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
	static SSymbScratch Tsscratch = new SSymbScratch();


	/////////////////////////////////////////////
	void RefreshSymbol(OneTunnel vgsymbols)
	{
		nsmposvalid = 0;

		// sort out the axis
		Tsscratch.InitAxis(this, true, null);

		// add in a whole bunch of (provisional) positions.
		for (int ic = 0; ic < symbmult.size(); ic++)
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);
			Tsscratch.BuildAxisTrans(ssing.paxistrans, this, ic);
			ssing.MakeTransformedPaths(this, ic);
		}
	}

	/////////////////////////////////////////////
	// the intersecting checking bit.
	boolean IsSymbolsPositionValid(Area lsaarea, SSymbSing ssing, List<OnePath> ssymbinterf)
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
		for (OnePath op : ssymbinterf)
		{
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
			Tsscratch.InitAxis(this, (symbmult.size() == 0), null);
			// make some provisional positions just to help the display of multiplicity
			int nic = (((ssb.nmultiplicity != -1) && (ssb.nmultiplicity < 2)) ? ssb.nmultiplicity : 2);
			for (int ic = 0; ic < nic; ic++)
			{
				SSymbSing ssing = new SSymbSing();
				Tsscratch.BuildAxisTrans(ssing.paxistrans, this, symbmult.size());
				ssing.MakeTransformedPaths(this, ic);
				symbmult.addElement(ssing);
			}
		}
	}
}
