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

import java.util.Vector; 
import java.util.Random;
import java.io.IOException;
import java.lang.StringBuffer; 
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.Color;

import java.io.IOException;



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
		for (int j = 0; j < oss.gsym.vpaths.size(); j++)
		{
			OnePath path = (OnePath)oss.gsym.vpaths.elementAt(j);
			if (((path.linestyle >= SketchLineStyle.SLS_WALL) && (path.linestyle <= SketchLineStyle.SLS_DETAIL)) || (path.linestyle == SketchLineStyle.SLS_FILLED) || ((path.linestyle != SketchLineStyle.SLS_CENTRELINE) && (path.plabedl != null)))
			{
				OnePath tpath = new OnePath(path, paxistrans);
				viztranspaths.add(tpath);
			}
		}
	}
};

/////////////////////////////////////////////
// persistant class for storing stuff to make symbol layout easily complex.
// if there weren't so many, these would be local variables in the functions.
class SSymbScratch
{
	AffineTransform affscratch = new AffineTransform(); // to make the rotation
	Random ran = new Random(); // to keep the random generator

	OnePath apath;

	double apx;
	double apy;
	double lenapsq;
	double lenap;

	double psx;
	double psy;
	double lenpssq;
	double lenps;


	// used in rotation.
	double lenpsap;
	double dotpsap;
	double dotpspap;

	// lattice marking
	int ilatu;
	int ilatv;

	// for pullbacks
	double pbx; // pullback position
	double pby;
	double pox; // push out position (which we extend beyond).
	double poy;
	double pleng;

	AffineTransform affnonlate = new AffineTransform(); // non-translation

	int placeindex = 0; // layout index variables.
	int noplaceindexlimitpullback = 12; // layout index variables.
	int noplaceindexlimitrand = 20; // layout index variables.


	/////////////////////////////////////////////
	void InitAxis(OneSSymbol oss, boolean bResetRand)
	{
		apath = oss.gsym.GetAxisPath();

		apx = apath.pnend.pn.getX() - apath.pnstart.pn.getX();
		apy = apath.pnend.pn.getY() - apath.pnstart.pn.getY();
		lenapsq = apx * apx + apy * apy;
		lenap = Math.sqrt(lenapsq);

		psx = oss.paxis.getX2() - oss.paxis.getX1();
		psy = oss.paxis.getY2() - oss.paxis.getY1();
		lenpssq = psx * psx + psy * psy;
		lenps = Math.sqrt(lenpssq);


		// used in rotation.
		if (oss.bRotateable)
		{
			lenpsap = lenps * lenap;
			dotpsap = (lenpsap != 0.0F ? (psx * apx + psy * apy) / lenpsap : 1.0F);
			dotpspap = (lenpsap != 0.0F ? (-psx * apy + psy * apx) / lenpsap : 1.0F);
		}

		// reset the random seed to make this reproduceable.  .
		if (bResetRand)
		{
			ran.setSeed(Double.doubleToRawLongBits(apx + apy));
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
			ran.nextInt();
		}
	}


	/////////////////////////////////////////////
	void BuildAxisTrans(AffineTransform paxistrans, OneSSymbol oss, int locindex)
	{
		// position
		// lattice translation.

		if ((locindex != 0) && (oss.posdeviationprop != 0.0F))
		{
			double pdisp = ran.nextGaussian() * 0.5F * oss.posdeviationprop;
			double adisp = ran.nextGaussian() * 0.5F * oss.posdeviationprop + 0.5F;

			// pull more to the middle of the line.
			double radisp = Math.min(1.0, Math.max(0.0, (adisp + 0.5F) * 0.5F));
			pbx = oss.paxis.getX1() + radisp * psx;
			pby = oss.paxis.getY1() + radisp * psy;

			pox = oss.paxis.getX1() + adisp * psx + pdisp * psy;
			poy = oss.paxis.getY1() + adisp * psy - pdisp * psx;
		}
		else
		{
			//paxistrans.setToTranslation(oss.paxis.getX1(), oss.paxis.getY1());

			pbx = oss.paxis.getX1();
			pby = oss.paxis.getY1();
			pox = oss.paxis.getX2();
			poy = oss.paxis.getY2();
		}

		// we add a lattice translation onto the results of the above
		// this means we can have a lattice that is slightly jiggled at each point.
		if (oss.bLattice && (locindex != 0))
		{
			LatticePT(locindex);

			pox += apx * ilatu + apy * ilatv;
			poy += apy * ilatu - apx * ilatv;
		}

		// find the length of this pushline
		double pxv = pox - pbx;
		double pyv = poy - pby;
		double plengsq = pxv * pxv + pyv * pyv;
		pleng = Math.sqrt(plengsq);

		// rotation.
		if (oss.bRotateable)
		{
			double a, b;
			if ((oss.posangledeviation != 0.0F) && (locindex != 0))
			{
				double angdev = (oss.posangledeviation == 10.0F ? ran.nextDouble() * Math.PI * 2 : ran.nextGaussian() * oss.posangledeviation);
				double ca = Math.cos(angdev);
				double sa = Math.sin(angdev);
				a = ca * dotpsap + sa * dotpspap;
				b = -sa * dotpsap + ca * dotpspap;
			}
			else
			{
				a = dotpsap;
				b = dotpspap;
			}

			affnonlate.setTransform(a, b, -b, a, 0.0F, 0.0F);
		}
		else
			affnonlate.setToIdentity();

		// scaling
		double lenap = Math.sqrt(lenapsq);
		if (oss.bScaleable)
		{
			double sca = lenps / lenap;
			affnonlate.scale(sca, sca);
		}

		if (oss.bShrinkby2)
		{
			double sca = (ran.nextDouble() + 1.0F) / 2;
			affnonlate.scale(sca, sca);
		}

		affnonlate.translate(-apath.pnend.pn.getX(), -apath.pnend.pn.getY());

		// concatenate the default translation
		paxistrans.setToTranslation(pox, poy);
		paxistrans.concatenate(affnonlate);
	}

	/////////////////////////////////////////////
	void BuildAxisTransT(AffineTransform paxistrans, double lam)
	{
		// concatenate the default translation
		paxistrans.setToTranslation(pbx * (1.0 - lam) + pox * lam, pby * (1.0 - lam) + poy * lam);
		paxistrans.concatenate(affnonlate);
	}

	/////////////////////////////////////////////
	void LatticePT(int lat)
	{
		// find lattice dimension
		// we may want to work our lattice in the direction of the axis if we have sense.
		int ld = 1;
		while (lat >= ld * ld)
			ld += 2;
		int lr = (ld - 1) / 2;
		int latp = lat - (ld - 2) * (ld - 2);
		if (latp < ld)
		{
			ilatu = lr;
        	ilatv = latp - lr;
 		}
 		else if (latp < 2 * ld - 1)
 		{
			ilatv = lr;
        	ilatu = -(latp - ld + 1 - lr);
 		}
		else if (latp < 3 * ld - 2)
		{
			ilatu = -lr;
        	ilatv = -(latp - 2 * ld + 2 - lr);
		}
		else
		{
			ilatv = -lr;
        	ilatu = latp - 3 * ld + 3 - lr;
		}
		// System.out.println("lattice  " + lat + "  " + ld + "  " + ilatu + "  " + ilatv);
	}
}


/////////////////////////////////////////////
class OneSSymbol extends SSymbolBase
{
	// when we have multisumbols, up to the transformed paths,
	// the info based on the pos of the axis could be shared.

	// arrays of sketch components.

	// location definition
	Line2D paxis;

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
	// this function is going to go
	void IncrementMultiplicity(int ic)
	{
		if (ic < 0)
		{
			TN.emitMessage("Decrement multiplicity not coded");
			return;
		}

		// sort out the axis
		if (gsym != null)
			sscratch.InitAxis(this, (symbmult.size() == 0));

		// add in a whole bunch of (provisional) positions.
		while (ic > 0)
		{
			SSymbSing ssing = new SSymbSing();

			if (gsym != null)
			{
				sscratch.BuildAxisTrans(ssing.paxistrans, this, symbmult.size());
				ssing.MakeTransformedPaths(this, ic);
			}

			symbmult.addElement(ssing);
			ic--;
		}
	}

	/////////////////////////////////////////////
	void RefreshSymbol(OneTunnel vgsymbols)
	{
		// no valid positions
		nsmposvalid = 0;

		// sort out the axis
		sscratch.InitAxis(this, true);

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
		if (!bAllowedOutsideArea)
		{
			awork.add(ssing.atranscliparea);
			awork.subtract(lsaarea);
			if (!awork.isEmpty())
				return false; // the area goes outside.
  		}

		// but if symbol entirely outside, no point in having it here.
		else if (bTrimByArea)
		{
			awork.add(ssing.atranscliparea);
			awork.intersect(lsaarea);
			if (awork.isEmpty())
				return false;
			awork.reset();
		}


		// we will now find all the symbols which are in an area which overlaps this one.
		// and work on those that have already been layed, checking for interference.
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
	// layout variables marking out how many attempts at laying out we've tried.
	static double pulltolerance = 0.05; // 5cm.

	/////////////////////////////////////////////
	// this does pullback in a line, but also copes with the case where no pulling happens.
	boolean RelaySymbolT(SSymbSing ssing, Area lsaarea, Vector ssymbinterf)
	{
		sscratch.placeindex++;

		// make transformed location for lam0.
		double lam1 = (bPushout ? 2.0 : 1.0); // goes out twice as far.

		sscratch.BuildAxisTransT(ssing.paxistrans, lam1);

		// case of no clipping area associated to the symbol.
		if (gsym.cliparea == null)
		{
			ssing.transcliparea = null;
        	ssing.atranscliparea = null;
			return true;
		}

		ssing.transcliparea = (GeneralPath)gsym.cliparea.gparea.clone();
		ssing.transcliparea.transform(ssing.paxistrans);

		// make the area
		ssing.atranscliparea = new Area(ssing.transcliparea);
		boolean lam1valid = IsSymbolsPositionValid(lsaarea, ssing, ssymbinterf);

		// cache the results at lam1.
		GeneralPath lam1transcliparea = ssing.transcliparea;
		Area lam1atranscliparea = ssing.atranscliparea;

		// no pullback case
		if ((!bPullback && !bPushout) || (sscratch.pleng * 2 <= pulltolerance))
			return lam1valid;

		sscratch.placeindex++;

		// this is a pull/push type.  record where we are going towards (the push-out direction).
		double lam0 = (bPullback ? 0.0 : 1.0);

		sscratch.BuildAxisTransT(ssing.paxistrans, lam0);

		// could check containment in boundary box too, to speed things up.
		ssing.transcliparea = (GeneralPath)gsym.cliparea.gparea.clone();
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
			if (sscratch.pleng * (lam1 - lam0) <= pulltolerance)
				break;

			sscratch.placeindex++;

			double lammid = (lam0 + lam1) / 2;

			sscratch.BuildAxisTransT(ssing.paxistrans, lammid);

			ssing.transcliparea = (GeneralPath)gsym.cliparea.gparea.clone();
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
		for (int ip = 0; ip < sscratch.noplaceindexlimitrand; ip++)
		{
			sscratch.BuildAxisTrans(ssing.paxistrans, this, sscratch.placeindex);
			if (RelaySymbolT(ssing, lsaarea, ssymbinterf))
			{
				ssing.MakeTransformedPaths(this, sscratch.placeindex);
				return true;
			}

			// quit if not a random moving type.
			if (!bMoveable && !bLattice)
				break;
		}
		return false;
	}


	/////////////////////////////////////////////
	static Vector ssymbinterf = new Vector(); // list of interfering symbols
	void RelaySymbolsPosition(SketchSymbolAreas sksya, int iconncompareaindex)
	{
		// start with no valid positions
		nsmposvalid = 0;

		Area lsaarea = sksya.GetCCArea(iconncompareaindex);
		if ((lsaarea == null) || (gsym == null))
			return; // no areas to be in.

		// sort out the axis
		sscratch.InitAxis(this, true);

        // fetch the list of paths which can have interfering symbols (really a subset of the components)
		// this should contain itself
		sksya.GetInterferingSymbols(ssymbinterf, iconncompareaindex);

		// add in a whole bunch of (provisional) positions.
		sscratch.placeindex = 0; // layout index variables.
		sscratch.noplaceindexlimitpullback = 20; // layout index variables.
		sscratch.noplaceindexlimitrand = 20;

		while (nsmposvalid < nmultiplicity)
		{
			// roll on new symbols as we run further up the array.
			if (nsmposvalid == symbmult.size())
				symbmult.addElement(new SSymbSing());

			SSymbSing ssing = (SSymbSing)symbmult.elementAt(nsmposvalid);
			if (RelaySymbol(ssing, lsaarea, ssymbinterf))
				nsmposvalid++;
			else
				break;
		}

		if (sscratch.placeindex > 1)
			TN.emitMessage("S:" + gsymname + "  placeindex  " + sscratch.placeindex + " of symbols " + nsmposvalid);
	}




	/////////////////////////////////////////////
	static Color colsymoutline = new Color(1.0F, 0.8F, 0.8F);
	static Color colsymactivearea = new Color(1.0F, 0.2F, 1.0F, 0.16F);
	static Color colgreysym = new Color(0.6F, 0.6F, 0.6F, 0.33F);
// much of this needs upgrading with the new colouring
	void paintW(Graphics2D g2D, boolean bActive, boolean bProperSymbolRender)
	{
		//System.out.println("symbval " + symbmult.size() + " " + nsmposvalid);
		for (int ic = 0; ic < symbmult.size(); ic++)
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);

			// proper symbols, paint out the background.
			if (bProperSymbolRender)
			{
				if (ic >= nsmposvalid)
					break;

				g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]);
				//g2D.setColor(colsymoutline); // to see it in pink.
				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_SYMBOLOUTLINE]);

				// this blanks out the background and draws a fattening of the outer area.
				// we could make this fattening included in the clip area in the first place.
				//g2D.draw(ssing.transcliparea);
				//g2D.fill(ssing.transcliparea); // (martin said take out this whitening as nothing overlaps)
				g2D.setColor(SketchLineStyle.linestylecolprint);
			}
			else
				g2D.setColor(ic >= nsmposvalid ? colgreysym : SketchLineStyle.linestylecols[SketchLineStyle.SLS_DETAIL]);

			if (bActive)
				g2D.setColor(SketchLineStyle.linestylecolactive);

			for (int j = 0; j < ssing.viztranspaths.size(); j++)
			{
				OnePath op = (OnePath)ssing.viztranspaths.elementAt(j);
				if (op != null)
				{
					if (bProperSymbolRender)
					{
						g2D.setColor(SketchLineStyle.linestylecolprint);
						if (op.linestyle == SketchLineStyle.SLS_FILLED)
							g2D.fill(op.gp);
						else
						{
							g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
							g2D.draw(op.gp);
						}
						if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
							op.paintLabel(g2D, false);
					}
					else
						op.paintWnosetcol(g2D, true, bActive);
				}
			}
		}
	}


	/////////////////////////////////////////////
	OneSSymbol()
	{
	}

	/////////////////////////////////////////////
	OneSSymbol(float[] pco, int nlines, float zalt)
	{
		paxis = new Line2D.Float(pco[0], pco[1], pco[nlines * 2], pco[nlines * 2 + 1]);
	}
}
