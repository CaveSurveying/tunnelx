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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.Raster;

import java.util.Arrays;

import java.io.IOException;
import java.awt.image.BufferedImage;


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
// persistant class for storing stuff to make symbol layout easily complex.
// if there weren't so many, these would be local variables in the functions.
class SSymbScratch
{
	AffineTransform affscratch = new AffineTransform(); // to make the rotation
	Random ran = new Random(); // to keep the random generator

	Line2D axisline = new Line2D.Double();

	// axis definitions
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
	double ilatu0; // lattice phase zero point (integral for type 2)
	double ilatv0;

	// here we should put a buffered image which we then plot the
	// area into at the scale of the lattice in the direction too,
	// and then should read out the lattice positions from the pixel values
	// (should be able to rotate by the angle of the lattice, but too hard)
	BufferedImage latbi = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);//TYPE_BYTE_BINARY
	int latbiweff; // effective dimensions when we only use part of it
	int latbiheff;
	Graphics2D latbiG = (Graphics2D)latbi.getGraphics();
	AffineTransform afflatbiIdent = new AffineTransform(); // identity
	AffineTransform afflatbi = new AffineTransform(); // used to scale the area down
	Point2D posbin = new Point2D.Double();
	Point2D posbi = new Point2D.Double();
	int[] latticpos = new int[4096]; // records the lattice positions which the bitmap says hit the shape
	int lenlatticpos = -1;

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
	void SetUpLatticeOfArea(Area lsaarea, OneSSymbol oss, double lapx, double lapy, double llenap)
	{
		double width = llenap;
		latbiG.setColor(Color.black);
		latbiG.setTransform(afflatbiIdent);
		latbiG.fillRect(0, 0, latbi.getWidth(), latbi.getHeight());

		latbiG.setColor(Color.white);
		Rectangle2D bnd = lsaarea.getBounds2D();
		double borframe = width / 2;
		latbiweff = latbi.getWidth();
		latbiheff = latbi.getHeight();
		double scax = latbiweff / (bnd.getWidth() + 2 * borframe);
		double scay = latbiheff / (bnd.getHeight() + 2 * borframe);
		double sca;
		if (scax <= scay)
		{
			sca = scax;
			latbiheff = Math.min(latbiheff, (int)(latbiheff * scax / scay) + 1);
		}
		else
		{
			sca = scay;
			latbiweff = Math.min(latbiweff, (int)(latbiweff * scay / scax) + 1);
		}
		afflatbi.setToScale(sca, sca);
		afflatbi.translate(borframe - bnd.getX(), borframe - bnd.getY());

		// stroke width should be lenap
		latbiG.setTransform(afflatbi);
		latbiG.fill(lsaarea);
		latbiG.setStroke(new BasicStroke((float)width));
		latbiG.draw(lsaarea); // this gives nasty shape sometimes


		// find the extent in u and v by transforming the four corners
		double ulo=0, uhi=0, vlo=0, vhi=0;
		try
		{
		double llenapsq = llenap * llenap;
		for (int ic = 0; ic < 4; ic++)
		{
			posbin.setLocation(((ic == 0) || (ic == 1) ? 0 : latbiweff), ((ic == 0) || (ic == 2) ? 0 : latbiheff));
			afflatbi.inverseTransform(posbin, posbi);
			double u = (lapx * posbi.getX() + lapy * posbi.getY()) / llenapsq - ilatu0;
			double v = (lapy * posbi.getX() - lapx * posbi.getY()) / llenapsq - ilatv0;

			if ((ic == 0) || (u < ulo))
				ulo = u;
			if ((ic == 0) || (u > uhi))
				uhi = u;
			if ((ic == 0) || (v < vlo))
				vlo = v;
			if ((ic == 0) || (v > vhi))
				vhi = v;
		}
		}
		catch (NoninvertibleTransformException e)
		{ assert false; }

		// scan the covering lattice
		lenlatticpos = 0;
		Raster latbir = latbi.getRaster();
		for (int iu = (int)ulo; iu <= uhi; iu++)
		for (int iv = (int)vlo; iv <= vhi; iv++)
		{
			if (lenlatticpos == latticpos.length)
				break;
			pox = lapx * (iu + ilatu0) + lapy * (iv + ilatv0);
			poy = lapy * (iu + ilatu0) - lapx * (iv + ilatv0);

			posbin.setLocation(pox, poy);
			afflatbi.transform(posbin, posbi);
			int ix = (int)(posbi.getX());
			int iy = (int)(posbi.getY()); // not sure of these numbers
			// check transform is in bitmap
			if ((ix >= 0) && (ix < latbiweff) && (iy >= 0) && (iy < latbiheff) &&
				(latbir.getSample(ix, iy, 0) != 0))
			{
				latticpos[lenlatticpos++] = invLatticePT(iu, iv);
			}
		}
		Arrays.sort(latticpos, 0, lenlatticpos);
	}


	/////////////////////////////////////////////
	void InitAxis(OneSSymbol oss, boolean bResetRand, Area lsaarea)
	{
		if (oss.ssb.gsym == null)
		{
			TN.emitWarning("No sketch for symbol: " + oss.ssb.gsymname);
			return;
		}
		OnePath apath = oss.ssb.gsym.GetAxisPath();
		axisline.setLine(apath.pnstart.pn.getX() * oss.ssb.fpicscale, apath.pnstart.pn.getY() * oss.ssb.fpicscale,
						 apath.pnend.pn.getX() * oss.ssb.fpicscale, apath.pnend.pn.getY() * oss.ssb.fpicscale);

		apx = axisline.getX2() - axisline.getX1();
		apy = axisline.getY2() - axisline.getY1();
		lenapsq = apx * apx + apy * apy;
		lenap = Math.sqrt(lenapsq);

		psx = oss.paxis.getX2() - oss.paxis.getX1();
		psy = oss.paxis.getY2() - oss.paxis.getY1();
		lenpssq = psx * psx + psy * psy;
		lenps = Math.sqrt(lenpssq);


		// set up the lattice stuff
		lenlatticpos = -1;
		if (oss.ssb.iLattice != 0)
		{
			ilatu0 = (oss.paxis.getX2() * apx + oss.paxis.getY2() * apy) / lenapsq;
			ilatv0 = (oss.paxis.getX2() * apy - oss.paxis.getY2() * apx) / lenapsq;
			if (oss.ssb.iLattice == 2)
			{
				ilatu0 = (int)(ilatu0 + 0.5);
				ilatv0 = (int)(ilatv0 + 0.5);
			}
			if (lsaarea != null)
				SetUpLatticeOfArea(lsaarea, oss, apx, apy, lenap);
		}

		// area filling symbols which use lattice as a basis for layout on or near the area
		else if (oss.ssb.nmultiplicity == -1)
		{
			//ilatu0 = (oss.paxis.getX2() * psx + oss.paxis.getY2() * psy) / lenpssq;
			//ilatv0 = (oss.paxis.getX2() * psy - oss.paxis.getY2() * psx) / lenpssq;
			// not user defined axis spacing
			ilatu0 = (int)((oss.paxis.getX2() * apx + oss.paxis.getY2() * apy) / lenapsq + 0.5);
			ilatv0 = (int)((oss.paxis.getX2() * apy - oss.paxis.getY2() * apx) / lenapsq + 0.5);
			if (lsaarea != null)
				//SetUpLatticeOfArea(lsaarea, oss, psx, psy, lenps);
				SetUpLatticeOfArea(lsaarea, oss, apx, apy, lenap);  // not user defined axis
		}

		// used in rotation.
		if (oss.ssb.bRotateable)
		{
			lenpsap = lenps * lenap;
			dotpsap = (lenpsap != 0.0F ? (psx * apx + psy * apy) / lenpsap : 1.0F);
			dotpspap = (lenpsap != 0.0F ? (-psx * apy + psy * apx) / lenpsap : 1.0F);
		}

		// reset the random seed to make this reproduceable.
		if (bResetRand)
		{
			ran.setSeed(Double.doubleToRawLongBits(apx + apy));
			ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();
		}
	}


	/////////////////////////////////////////////
	void BuildAxisTrans(AffineTransform paxistrans, OneSSymbol oss, int locindex)
	{
		// position
		// lattice translation.

		// we add a lattice translation onto the results of the above
		// this means we can have a lattice that is slightly jiggled at each point.

		// pullback point
		pbx = oss.paxis.getX1();
		pby = oss.paxis.getY1();

		// lattice types
		if (oss.ssb.iLattice != 0)
		{
			LatticePT(locindex);
			pox = apx * (ilatu + ilatu0) + apy * (ilatv + ilatv0);
			poy = apy * (ilatu + ilatu0) - apx * (ilatv + ilatv0);
		}

		// the fill area type
		else if ((locindex != 0) && (oss.ssb.nmultiplicity == -1) && (lenlatticpos > 0))
		{
			LatticePT(latticpos[ran.nextInt(lenlatticpos)]);
			//pox = psx * (ilatu + ilatu0) + psy * (ilatv + ilatv0);
			//poy = psy * (ilatu + ilatu0) - psx * (ilatv + ilatv0);
			pox = apx * (ilatu + ilatu0) + apy * (ilatv + ilatv0);
			poy = apy * (ilatu + ilatu0) - apx * (ilatv + ilatv0);

			// radially distributed dithered
			pbx = oss.paxis.getX2();
			pby = oss.paxis.getY2();
			if ((locindex != 0) && (oss.ssb.posdeviationprop != 0.0F))
			{
				double pdisp = ran.nextGaussian();
				double adisp = ran.nextGaussian();

				pox += ran.nextGaussian() * lenap;
				poy += ran.nextGaussian() * lenap;
			}

		}

 		// otherwise centred on the destination
		else
		{
			pox = oss.paxis.getX2();
			poy = oss.paxis.getY2();

			// add a deviation to this
			if ((locindex != 0) && (oss.ssb.posdeviationprop != 0.0F))
			{
				double pdisp = ran.nextGaussian() * 0.5F * oss.ssb.posdeviationprop;
				double adisp = ran.nextGaussian() * 0.5F * oss.ssb.posdeviationprop + 0.5F;

				// pull more to the middle of the line. (for pull back cases, though might just be at destination)
				double radisp = Math.min(1.0, Math.max(0.0, (adisp + 0.5F) * 0.5F));

				pbx += radisp * psx;
				pby += radisp * psy;

				pox -= adisp * psx + pdisp * psy;
				poy -= adisp * psy - pdisp * psx;
			}
		}


		// find the length of this pushline
		double pxv = pox - pbx;
		double pyv = poy - pby;
		double plengsq = pxv * pxv + pyv * pyv;
		pleng = Math.sqrt(plengsq);

		// rotation.
		if (oss.ssb.bRotateable)
		{
			double a, b;
			if ((oss.ssb.posangledeviation != 0.0F) && (locindex != 0))
			{
				double angdev = (oss.ssb.posangledeviation == 10.0F ? ran.nextDouble() * Math.PI * 2 : ran.nextGaussian() * oss.ssb.posangledeviation);
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
		if (oss.ssb.bScaleable)
		{
			double sca = lenps / lenap;
			affnonlate.scale(sca, sca);
		}


		if (oss.ssb.bShrinkby2)
		{
			double sca = (ran.nextDouble() + 1.0F) / 2;
			affnonlate.scale(sca, sca);
		}

		affnonlate.translate(-axisline.getX2(), -axisline.getY2());
		// scale the picture
		if (oss.ssb.fpicscale != 1.0)
			affnonlate.scale(oss.ssb.fpicscale, oss.ssb.fpicscale);

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
	static int invLatticePT(int iu, int iv)
	{
		if ((iu == 0) && (iv == 0))
			return 0;
		int lr = Math.max(Math.abs(iu), Math.abs(iv));
		int ld = 2 * lr + 1;
		int latp;
		if (iu == lr)
		{
			latp = iv + lr;
			assert latp < ld;
 		}
 		else if (iv == lr)
 		{
        	latp = -iu + lr - 1 + ld;
        	assert latp < 2 * ld - 1;
 		}
		else if (iu == -lr)
		{
        	latp = -iv + lr - 2 + 2 * ld;
        	assert latp < 3 * ld - 2;
		}
		else
		{
			assert iv == -lr;
        	latp = iu + lr - 3 + 3 * ld;
		}

		int lat = latp + (ld - 2) * (ld - 2);
		assert lat < ld * ld;
		return lat;
	}


	/////////////////////////////////////////////
	void LatticePT(int lat)
	{
		if (lat == 0)
		{
			ilatu = 0;
        	ilatv = 0;
			return;
		}

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
		assert lat == invLatticePT(ilatu, ilatv);
	}
}


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
			sscratch.BuildAxisTrans(ssing.paxistrans, this, sscratch.placeindex);
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
	static Vector ssymbinterf = new Vector(); // list of interfering symbols
// should this be symbols not paths?
	void LayoutLatticeSymbols()
	{
		//assert ssymbinterf.contains();// current path
		for (int i = 0; i < sscratch.lenlatticpos; i++)
		{
			if (nsmposvalid == symbmult.size())
				symbmult.addElement(new SSymbSing());
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(nsmposvalid);
			sscratch.BuildAxisTrans(ssing.paxistrans, this, sscratch.latticpos[i]);

//			if (RelaySymbolT(ssing, lsaarea, ssymbinterf)) // everything except current symbol
			ssing.MakeTransformedPaths(this, sscratch.placeindex);
			nsmposvalid++;
		}
	}

	/////////////////////////////////////////////
	void RelaySymbolsPosition(SketchSymbolAreas sksya, int iconncompareaindex)
	{
		// start with no valid positions
		nsmposvalid = 0;

		Area lsaarea = sksya.GetCCArea(iconncompareaindex);
		if ((lsaarea == null) || (ssb.gsym == null))
			return; // no areas to be in.

		// colour filling type.  Hack it in
		if (ssb.symbolareafillcolour != null)
			return;

		// sort out the axis
		sscratch.InitAxis(this, true, lsaarea);

        // fetch the list of paths which can have interfering symbols (really a subset of the components)
		// this should contain itself
		sksya.GetInterferingSymbols(ssymbinterf, iconncompareaindex);

		// short version if it's lattice type
		if (ssb.iLattice != 0)
		{
			LayoutLatticeSymbols();
			return;
		}

		// add in a whole bunch of (provisional) positions.
		sscratch.placeindex = 0; // layout index variables.
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
	void paintW(Graphics2D g2D, boolean bActive, boolean bProperSymbolRender)
	{
		// demonstrate the mask of the area used to layout the last symbol
		// just a single instant debugging tool
		// g2D.drawImage(sscratch.latbi, null, null);

		assert !bProperSymbolRender || (ssb.symbolareafillcolour == null);


		//System.out.println("symbval " + symbmult.size() + " " + nsmposvalid);
		int nic = (bProperSymbolRender || (nsmposvalid != 0) ? nsmposvalid : symbmult.size());
		for (int ic = 0; ic < nic; ic++)
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);

			// proper symbols, paint out the background.
			if (bProperSymbolRender)
			{
				// this blanks out the background and draws a fattening of the outer area.
				// we could make this fattening included in the clip area in the first place.
				//g2D.draw(ssing.transcliparea);
				//g2D.fill(ssing.transcliparea); // (martin said take out this whitening as nothing overlaps)

				// what happens to SketchLineStyle.SLS_SYMBOLOUTLINE ?
				g2D.setColor(op.zaltcol == null ? op.subsetattr.linestyleattrs[SketchLineStyle.SLS_DETAIL].strokecolour : op.zaltcol);
				g2D.setStroke(op.subsetattr.linestyleattrs[SketchLineStyle.SLS_DETAIL].linestroke);
			}
			else
			{
				g2D.setColor(ic < nsmposvalid ? (ic == 0 ? SketchLineStyle.linestylefirstsymbcol : SketchLineStyle.linestylesymbcol) : (ic == 0 ? SketchLineStyle.linestylefirstsymbcolinvalid : SketchLineStyle.linestylesymbcolinvalid));
				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_SYMBOLOUTLINE]);
			}

			if (bActive)
				g2D.setColor(SketchLineStyle.linestylecolactive);

			for (int j = 0; j < ssing.viztranspaths.size(); j++)
			{
				OnePath sop = (OnePath)ssing.viztranspaths.elementAt(j);
				if (sop != null)
				{
					if (bProperSymbolRender)
					{
						if (sop.linestyle == SketchLineStyle.SLS_FILLED)
							g2D.fill(sop.gp);
						else if (sop.linestyle != SketchLineStyle.SLS_CONNECTIVE)
							g2D.draw(sop.gp);
						else if ((sop.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (sop.plabedl != null) && (sop.plabedl.labfontattr != null))
							sop.paintLabel(g2D, false);  // how do we know what font to use?  should be from op!
					}
					else
						sop.paintWnosetcol(g2D, true, bActive);
				}
			}
		}
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
