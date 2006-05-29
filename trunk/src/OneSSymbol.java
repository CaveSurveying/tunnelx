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
import java.io.IOException;
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
import java.awt.image.Raster;

import java.util.Arrays;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


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
	double[] cumpathleng = new double[256]; // (nodelength, reallength) records the distance to each node along the path, as pairs
		int lencumpathleng = -1; 
	Point2D pathevalpoint = new Point2D.Double(); 
	Point2D pathevaltang = new Point2D.Double(); 

	// for pullbacks
	double pbx; // pullback position
	double pby;
	double pox; // push out position (which we extend beyond).
	double poy;
	double pleng;

	AffineTransform affnonlate = new AffineTransform(); // non-translation

	int placeindex = 0; // layout index variables.
	int placeindexabs = 0; // layout index variable for layoutordered.
	int noplaceindexlimitpullback = 12; // layout index variables.
	int noplaceindexlimitrand = 20; // layout index variables.

	/////////////////////////////////////////////
	// this lists the points of the area relative to vector (lapx, lapy) (which has length llenap) centred on (lilatu, lilatv),
	// and puts them into the array latticpos
	void SetUpLatticeOfArea(Area lsaarea, OneSSymbol oss, double lapx, double lapy, double llenap, double lilatu, double lilatv)
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
		double llenapsq = llenap * llenap;
		try
		{
		for (int ic = 0; ic < 4; ic++)
		{
			posbin.setLocation(((ic == 0) || (ic == 1) ? 0 : latbiweff), ((ic == 0) || (ic == 2) ? 0 : latbiheff));
			afflatbi.inverseTransform(posbin, posbi);
			double u = (lapx * posbi.getX() + lapy * posbi.getY()) / llenapsq - lilatu;
			double v = (lapy * posbi.getX() - lapx * posbi.getY()) / llenapsq - lilatv;

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

		// preview the shape
		/*try {
			FileAbstraction file = FileAbstraction.MakeWritableFileAbstraction("biviewlattice.png");
			TN.emitMessage("Writing png file " + file.getAbsolutePath());
			ImageIO.write(latbi, "png", file.localfile);
		}
		catch (Exception e) { e.printStackTrace(); } */


		// scan the covering lattice
		lenlatticpos = 0;
		Raster latbir = latbi.getRaster();
		for (int iu = (int)ulo; iu <= uhi; iu++)
		for (int iv = (int)vlo; iv <= vhi; iv++)
		{
			if (lenlatticpos == latticpos.length)
				break;
			pox = lapx * (iu + lilatu) + lapy * (iv + lilatv);
			poy = lapy * (iu + lilatu) - lapx * (iv + lilatv);

			posbin.setLocation(pox, poy);
			afflatbi.transform(posbin, posbi);
			int ix = (int)(posbi.getX() + 0.5);
			int iy = (int)(posbi.getY() + 0.5);
			// check transform is in bitmap (adding 1 to y because it aligns it better (why?))
			if ((ix >= 0) && (ix < latbiweff) && (iy + 1 >= 0) && (iy + 1 < latbiheff) &&
				(latbir.getSample(ix, iy + 1, 0) != 0))
			{
				latticpos[lenlatticpos++] = invLatticePT(iu, iv);
			}
		}
		Arrays.sort(latticpos, 0, lenlatticpos); // so closer points to the origin are early
	}

	/////////////////////////////////////////////
	void SetUpPathLength(OnePath lpath)
	{
		lpath.GetCoords();
		lencumpathleng = 0;
		int nsegs = (lpath.bSplined ? 5 : 1);
		double clen = 0.0;
		double prevx = 0.0;
		double prevy = 0.0; 
		for (int i = 0; i < lpath.nlines; i++)
		{
			for (int j = (i == 0 ? 0 : 1); j <= nsegs; j++)
			{
				double tr = (double)j / nsegs; 
				lpath.EvalSeg(pathevalpoint, null, i, tr);
				if ((i != 0) || (j != 0))
				{
					double vx = pathevalpoint.getX() - prevx; 
					double vy = pathevalpoint.getY() - prevy; 
					clen += Math.sqrt(vx * vx + vy * vy);
				}
				cumpathleng[lencumpathleng * 2] = i + tr; 
				cumpathleng[lencumpathleng * 2 + 1] = clen; 
				prevx = pathevalpoint.getX(); 
				prevy = pathevalpoint.getY(); 
				lencumpathleng++;
			}
		}
	}

	/////////////////////////////////////////////
	double ConvertAbstoNodePathLength(double r, OnePath lpath)
	{
		int i; 
		for (i = 1; i < lencumpathleng; i++)
		{
			if (r <= cumpathleng[i * 2 + 1])
			{
				double lam = (r - cumpathleng[i * 2 - 1]) / (cumpathleng[i * 2 + 1] - cumpathleng[i * 2 - 1]);
				return cumpathleng[i * 2 - 2] * (1.0 - lam) + cumpathleng[i * 2] * lam;
			}
		}
		return 0.0;
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

		if (oss.ssb.bBuildSymbolLatticeAcrossArea)
		{
			// dot product against what will be the origin of the lattice to translate into coordinate system
			ilatu0 = (oss.paxis.getX2() * apx + oss.paxis.getY2() * apy) / lenapsq;
			ilatv0 = (oss.paxis.getX2() * apy - oss.paxis.getY2() * apx) / lenapsq;
			if (oss.ssb.bSymbolLatticeAcrossAreaPhased)
			{
				ilatu0 = (int)(ilatu0 + 0.5);
				ilatv0 = (int)(ilatv0 + 0.5);
			}
			if (lsaarea != null)
				SetUpLatticeOfArea(lsaarea, oss, apx, apy, lenap, ilatu0, ilatv0);
		}

		if (oss.ssb.bBuildSymbolSpreadAlongLine)
			SetUpPathLength(oss.op);

		// used in rotation.
		if (oss.ssb.bRotateable)
		{
			lenpsap = lenps * lenap;
			dotpsap = (lenpsap != 0.0F ? (psx * apx + psy * apy) / lenpsap : 1.0F);
			dotpspap = (lenpsap != 0.0F ? (-psx * apy + psy * apx) / lenpsap : 1.0F);
		}
		else
		{
			dotpsap = 1.0; 
			dotpspap = 0.0;
		}

		// reset the random seed to make this reproduceable.
		if (bResetRand)
		{
			ran.setSeed(Double.doubleToRawLongBits(apx + apy));
			ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();ran.nextInt();
		}
	}


	/////////////////////////////////////////////
	boolean BuildAxisTrans(AffineTransform paxistrans, OneSSymbol oss, int locindex)
	{
		// position
		// lattice translation.

		// we add a lattice translation onto the results of the above
		// this means we can have a lattice that is slightly jiggled at each point.

		// pullback point (usually along the axis, unless it's randomized position, then should be closest point)
		pbx = oss.paxis.getX1();
		pby = oss.paxis.getY1();

		// position of the symbol
		pox = oss.paxis.getX2();
		poy = oss.paxis.getY2();

		// lattice types
		if (oss.ssb.bBuildSymbolLatticeAcrossArea)
		{
			int ilat = 0;
			if (oss.ssb.bSymbolLayoutOrdered)
			{
				if ((lenlatticpos > 0) && (locindex >= lenlatticpos))
					return false;
				ilat = latticpos[locindex];
			}
			else if ((locindex != 0) && (lenlatticpos > 0))
				ilat = latticpos[ran.nextInt(lenlatticpos)];
			LatticePT(ilat);  // return values are ilatu/v
			pox = apx * (ilatu + ilatu0) + apy * (ilatv + ilatv0);
			poy = apy * (ilatu + ilatu0) - apx * (ilatv + ilatv0);
		}

		if (oss.ssb.bBuildSymbolSpreadAlongLine)
		{
			// pick a random point on line
			double r;
			if (oss.ssb.bSymbolLayoutOrdered)
			{
				r = locindex * lenap * oss.ssb.faxisscale;
				if (r > cumpathleng[lencumpathleng * 2 - 1])
					return false;
			}
			else
				r = ran.nextDouble() * cumpathleng[lencumpathleng * 2 - 1];
			double t = ConvertAbstoNodePathLength(r, oss.op);
			oss.op.Eval(pathevalpoint, pathevaltang, t);
			pox = pathevalpoint.getX();
			poy = pathevalpoint.getY();
		}

		double tanx = dotpsap;
		double tany = dotpspap;
		if (oss.ssb.bBuildSymbolSpreadAlongLine)
		{
			tanx = pathevaltang.getX();
			tany = pathevaltang.getY();
			double len = Math.sqrt(tanx * tanx + tany * tany);
			tanx /= len;
			tany /= len;
		}

		// random dithering
		if ((locindex != 0) && (oss.ssb.posdeviationprop != 0.0F))
		{
			pbx = pox; // pull-back position is the starting point from which we scatter
			pby = poy;
			double sca = oss.ssb.posdeviationprop * lenap * oss.ssb.faxisscale;
			double scaperp = sca * oss.ssb.faxisscaleperp;
			double aran = ran.nextGaussian() * sca;
			double pran = ran.nextGaussian() * scaperp;

			// force pull-back types to start from full extent
			if ((oss.ssb.faxisscaleperp != 1.0F) && oss.ssb.bPullback)
				pran = (pran > 0.0 ? scaperp : -scaperp);
			pox += tanx * aran + tany * pran;
			poy += tany * aran - tanx * pran;
		}

		// find the length of this pushline
		double pxv = pox - pbx;
		double pyv = poy - pby;
		double plengsq = pxv * pxv + pyv * pyv;
		pleng = Math.sqrt(plengsq);

		// rotation.
		if (oss.ssb.bRotateable)
		{
			double a = tanx;
			double b = tany;
			boolean bMakeUnit = false;
			if ((oss.ssb.posangledeviation == -1.0F) && (locindex != 0))
			{
				double angdev = ran.nextDouble() * Math.PI * 2;
				a = Math.cos(angdev);
				b = Math.sin(angdev);
			}
			else if (oss.ssb.bOrientClosestAlongLine || oss.ssb.bOrientClosestPerpLine)
			{
				double t = oss.op.ClosestPoint(pox, poy, -1.0);
				if (t != -1.0)
				{
					oss.op.Eval(pathevalpoint, pathevaltang, t);
					if (oss.ssb.bOrientClosestAlongLine)
					{
						pbx = pathevalpoint.getX();
						pby = pathevalpoint.getY();
						a = pox - pbx;
						b = poy - pby;
					}
					else
					{
						a = pathevaltang.getX();
						b = pathevaltang.getY();
					}
					pleng = Math.sqrt(a * a + b * b);
					a /= pleng;
					b /= pleng;

					// something wrong to make this necessary
					double s = a;
					a = b;
					b = -s;
				}
				else
					TN.emitWarning("Failed closest point " + pox + "  " + poy);
			}
			else if (oss.ssb.bBuildSymbolSpreadAlongLine)
			{
				if (oss.ssb.posangledeviation == -2.0)
				{
					double s = a;
					a = b;
					b = -s;
				}
			}
			else if ((oss.ssb.posangledeviation != 0.0F) && (locindex != 0))
			{
				double angdev = ran.nextGaussian() * oss.ssb.posangledeviation;
				double ca = Math.cos(angdev);
				double sa = Math.sin(angdev);
				a = ca * dotpsap + sa * dotpspap;
				b = -sa * dotpsap + ca * dotpspap;
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
		return true;
	}

	/////////////////////////////////////////////
	void BuildAxisTransT(AffineTransform paxistrans, double lam)
	{
		// concatenate the default translation
		paxistrans.setToTranslation(pbx * (1.0 - lam) + pox * lam, pby * (1.0 - lam) + poy * lam);
		paxistrans.concatenate(affnonlate);
	}


	/////////////////////////////////////////////
	// this kind of fancy stuff is so we can sort the points 
	// and get the values closer to the origin earlier in the array
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
	static Vector ssymbinterf = new Vector(); // list of interfering symbols
// should this be symbols not paths?
// lose this function.
	void LayoutLatticeSymbols()
	{
		//assert ssymbinterf.contains();// current path
		for (int i = 0; i < sscratch.lenlatticpos; i++)
		{
			if (nsmposvalid == symbmult.size())
				symbmult.addElement(new SSymbSing());
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(nsmposvalid);
			sscratch.BuildAxisTrans(ssing.paxistrans, this, i);

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
/*		if (ssb.iLattice != 0)
		{
			LayoutLatticeSymbols();
			return;
		}
*/

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

			LineStyleAttr linelsa = (bActive ? SketchLineStyle.lineactivestylesymb : (ic < nsmposvalid ? (ic == 0 ? SketchLineStyle.linestylefirstsymb : SketchLineStyle.linestylesymb) : (ic == 0 ? SketchLineStyle.linestylefirstsymbinvalid : SketchLineStyle.linestylesymbinvalid)));
			LineStyleAttr filllsa = (bActive ? SketchLineStyle.fillactivestylesymb : (ic < nsmposvalid ? (ic == 0 ? SketchLineStyle.fillstylefirstsymb : SketchLineStyle.fillstylesymb) : (ic == 0 ? SketchLineStyle.fillstylefirstsymbinvalid : SketchLineStyle.fillstylesymbinvalid)));
			for (int j = 0; j < ssing.viztranspaths.size(); j++)
			{
				OnePath sop = (OnePath)ssing.viztranspaths.elementAt(j);
				if (sop != null)
				{
					if (sop.linestyle == SketchLineStyle.SLS_FILLED)
						ga.drawPath(sop, filllsa);
					else if (sop.linestyle != SketchLineStyle.SLS_CONNECTIVE)
						if (linelsa !=null)
							ga.drawPath(sop, linelsa);
					else if ((sop.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (sop.plabedl != null) && (sop.plabedl.labfontattr != null))
						sop.paintLabel(ga, false);  // how do we know what font to use?  should be from op!
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
		{
			SSymbSing ssing = (SSymbSing)symbmult.elementAt(ic);

			for (int j = 0; j < ssing.viztranspaths.size(); j++)
			{
				OnePath sop = (OnePath)ssing.viztranspaths.elementAt(j);
				if (sop != null)
				{
					//if (bProperSymbolRender)
					//{
						if (sop.linestyle == SketchLineStyle.SLS_FILLED)
							ga.drawPath(sop, lsafilled);
						else if (sop.linestyle != SketchLineStyle.SLS_CONNECTIVE)
							ga.drawPath(sop, lsaline);
						else if ((sop.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (sop.plabedl != null) && (sop.plabedl.labfontattr != null))
							sop.paintLabel(ga, false);  // how do we know what font to use?  should be from op!
					//}
					//else
					//	sop.paintWnosetcol(ga, true, bActive);
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
