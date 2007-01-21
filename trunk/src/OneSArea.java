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

import java.awt.geom.Line2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D; 
import java.awt.Shape; 
import java.awt.geom.AffineTransform; 
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.io.IOException;

import java.awt.BasicStroke;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.TexturePaint;
import java.awt.Rectangle;

import java.util.List;
import java.util.ArrayList;

//
//
// OneSArea
//
//



/////////////////////////////////////////////
class OneSArea implements Comparable<OneSArea>
{
	// defines the area.
	GeneralPath gparea = null; // if null then nothing should be done with it.
	Area aarea = null;
	Rectangle2D rboundsarea = null;
	float zalt = 0.0F;
	Color zaltcol = null;

	boolean bareavisiblesubset = false;
	Vector vssubsetattrs = new Vector(); // SubsetAttr (in parallel) from the current style
	SubsetAttr subsetattr = null;  // one chosen from the vector above

	// array of RefPathO.
	Vector refpaths = new Vector();
	Vector refpathsub = new Vector(); // subselection without the trees.

	// ConnectiveComponentAreas this area is in
	List<ConnectiveComponentAreas> ccalist = new ArrayList<ConnectiveComponentAreas>();

	// these are used to mark the areas for inclusion in sketchsymbolareas.  more efficient than setting false it as a booleans.
//	int iamark = 0;
//	static int iamarkl = 1;

	// used in the quality rendering for signaling which edges can be drawn once areas on both sides have been done.
	boolean bHasrendered = false;

	// maximized around the contour for right precedence

	// ASE_ type
	int iareapressig = SketchLineStyle.ASE_KEEPAREA; // 0-1 normal, 3 column(rock), 2 pitchhole
	PathLabelDecode pldframesketch = null; // when iareapressig is SketchLineStyle.ASE_SKETCHFRAME, and we have a framed sketch.  This object specifies the transformations
	OneSketch pframesketch = null;
	AffineTransform pframesketchtrans = null;

	// used for refering to the area in SVG files
	String svgid = null;

	/////////////////////////////////////////////
	void paintHatchW(GraphicsAbstraction ga, int isa, int nsa)
	{
		if (gparea != null)
			ga.drawHatchedArea(this, isa, nsa);
	}

	/////////////////////////////////////////////
	// we're going to use this to help sort vareas.  the zalt values better not change throughout the age of this object
	public int compareTo(OneSArea osa)
	{
		if (zalt != osa.zalt)
			return (zalt - osa.zalt < 0.0F ? -1 : 1);
		return hashCode() - osa.hashCode();
	}

	/////////////////////////////////////////////
	// find which subsets this area is in, by looking at the surrounding edges
	// this is different to the isubsetcode thing; something between these two is redundant
	void DecideSubsets(Vector lvssubsets)
	{
		assert lvssubsets.isEmpty();
		for (int i = 0; i < refpathsub.size(); i++)
		{
			// find the intersection between these sets (using string equalities)
			Vector pvssub = ((RefPathO)refpathsub.elementAt(i)).op.vssubsets;
			if (i != 0)
			{
				for (int j = lvssubsets.size() - 1; j >= 0; j--)
				{
					String js = (String)lvssubsets.elementAt(j);
					int k;
					for (k = pvssub.size() - 1; k >= 0; k--)
						if (js.equals((String)pvssub.elementAt(k)))
							break;
					if (k < 0)
						lvssubsets.removeElementAt(j);
				}
			}
			else
				lvssubsets.addAll(pvssub);
		}
	}

	/////////////////////////////////////////////
	int SetSubsetAttrs(boolean bremakesubset, SubsetAttrStyle sas)
	{
		if (bremakesubset)
		{
			vssubsetattrs.clear();
			for (int i = 0; i < refpathsub.size(); i++)
			{
				// find the intersection between these sets (using string equalities)
				Vector pvssub = ((RefPathO)refpathsub.elementAt(i)).op.vssubsetattrs;
				if (i != 0)
				{
					for (int j = vssubsetattrs.size() - 1; j >= 0; j--)
					{
						SubsetAttr js = (SubsetAttr)vssubsetattrs.elementAt(j);
						int k;
						for (k = pvssub.size() - 1; k >= 0; k--)
							if (js == pvssub.elementAt(k))
								break;
						if (k < 0)
							vssubsetattrs.removeElementAt(j);
					}
				}
				else
					vssubsetattrs.addAll(pvssub);
			}
			// no overlapping values, find default
			if (vssubsetattrs.isEmpty())
        		subsetattr = sas.FindSubsetAttr("default", false);
			else
				subsetattr = (SubsetAttr)vssubsetattrs.elementAt(vssubsetattrs.size() - 1); 
			assert subsetattr != null; 
		}

		

		// set the visibility flag
		bareavisiblesubset = true;
		for (int j = 0; j < refpaths.size(); j++)
		{
			OnePath op = ((RefPathO)refpaths.elementAt(j)).op;
			if (!op.bpathvisiblesubset)
				bareavisiblesubset = false;
		}
		return (bareavisiblesubset ? 1 : 0);
	}


	/////////////////////////////////////////////
	void UpdateSketchFrame(OneSketch lpframesketch, float lrealpaperscale)
	{
		pframesketch = lpframesketch;
		if (pldframesketch == null)
			return; 
		pframesketchtrans = new AffineTransform();
		//System.out.println("boundsarea  " + rboundsarea.toString());
		pframesketchtrans.translate(pldframesketch.sfxtrans + rboundsarea.getX(), pldframesketch.sfytrans + rboundsarea.getY());
		if (pldframesketch.sfscaledown != 0.0F)
			pframesketchtrans.scale(lrealpaperscale / pldframesketch.sfscaledown, lrealpaperscale / pldframesketch.sfscaledown);
		if (pldframesketch.sfrotatedeg != 0.0F)
			pframesketchtrans.rotate(pldframesketch.sfrotatedeg * Math.PI / 180);
		//System.out.println("pframesketchtrans   " + pframesketchtrans.toString());
	}


	/////////////////////////////////////////////
	// this function should be a generic one on general paths
	/////////////////////////////////////////////

	// it looks tempting to do this on the refpaths, but since that list is
	// equivalent to the general path, it might as well be keopt simple and not piecewise

	static float[] CText = new float[4];
	static float[] CTdir = new float[4];

	/////////////////////////////////////////////
    static void CommitTriplet(float xp, float yp, float x, float y, float xn, float yn, boolean bFirst)
	{
		boolean bleft = (bFirst || (x <= CText[0]));
		boolean bup = (bFirst || (y >= CText[1]));
		boolean bright = (bFirst || (x >= CText[2]));
		boolean bdown = (bFirst || (y <= CText[3]));
		if (bleft || bup || bright || bdown)
		{
			float vpx = xp - x;
			float vpy = yp - y;
			float vnx = xn - x;
			float vny = yn - y;

			float vextd = vpx * vny - vpy * vnx;
			float vdot = vpx * vnx + vpy * vpy;
			if (bleft)
			{
				CText[0] = x;
				CTdir[0] = vextd;
			}
			if (bup)
			{
				CText[1] = y;
				CTdir[1] = vextd;
			}
			if (bright)
			{
				CText[2] = x;
				CTdir[2] = vextd;
			}
			if (bdown)
			{
				CText[3] = y;
				CTdir[3] = vextd;
			}
		}
	}


	/////////////////////////////////////////////
	static float[] Fcoords = new float[6];
	static float FxL1, FyL1;
	static float FxP2, FyP2;
	static float FxP1, FyP1;
	static float FxP0, FyP0;

	/////////////////////////////////////////////
	static int FindOrientationReliable(GeneralPath gp)
	{
		PathIterator pi = gp.getPathIterator(null);

		int np = -1;
		while (true)
		{
			int curvtype = pi.currentSegment(Fcoords);
			if (curvtype == PathIterator.SEG_CLOSE)
				break;
			np++;
			assert (np != 0) || (curvtype == PathIterator.SEG_MOVETO);

			FxP2 = FxP1;  FyP2 = FyP1;
			FxP1 = FxP0;  FyP1 = FyP0;

			if (curvtype == PathIterator.SEG_CUBICTO)
			{
				FxP0 = Fcoords[4];
				FyP0 = Fcoords[5];
			}
			else
			{
				FxP0 = Fcoords[0];
				FyP0 = Fcoords[1];
			}
			if (np == 1)
			{
				FxL1 = FxP0;
				FyL1 = FyP0;
			}
			if (np >= 2)
			    CommitTriplet(FxP2, FyP2, FxP1, FyP1, FxP0, FyP0, (np == 2));

			pi.next();
		}
		//assert (Fcoords[0] == FcoordsL0[0]) && (Fcoords[1] == FcoordsL0[1]);
	    CommitTriplet(FxP1, FyP1, FxP0, FyP0, FxL1, FyL1, false);

		if (np <= 2)
			return 0;
		boolean bpos = ((CTdir[0] >= 0.0) && (CTdir[1] >= 0.0) && (CTdir[2] >= 0.0) && (CTdir[3] >= 0.0));
		boolean bneg = ((CTdir[0] <= 0.0) && (CTdir[1] <= 0.0) && (CTdir[2] <= 0.0) && (CTdir[3] <= 0.0));
		if (bpos != bneg)
			return (bpos ? 1 : -1);
		return 0;
	}





	/////////////////////////////////////////////
	void Setkapointers(boolean btothis)
	{
		// we should perform the hard task of reflecting certain paths in situ.
		for (int j = 0; j < refpaths.size(); j++)
		{
			// get the ref path.
			RefPathO refpath = (RefPathO)(refpaths.elementAt(j));
			if (refpath.bFore)
			{
				assert refpath.op.karight == (btothis ? null : this);
				refpath.op.karight = (btothis ? this : null);
			}
			else
			{
				assert refpath.op.kaleft == (btothis ? null : this);
				refpath.op.kaleft = (btothis ? this : null);
			}
		}
	}

	/////////////////////////////////////////////
	static float[] pco = null;
	// this makes a mess out of reversing a general path
	void LinkArea()
	{
		assert gparea == null;
		gparea = new GeneralPath(GeneralPath.WIND_EVEN_ODD);

		// we should perform the hard task of reflecting certain paths in situ.
		for (int j = 0; j < refpathsub.size(); j++)
		{
			// get the ref path.
			RefPathO refpath = (RefPathO)(refpathsub.elementAt(j));

			// if going forwards, then everything works
			if (refpath.bFore)
			{
				gparea.append(refpath.op.gp, (j != 0)); // the second parameter is continuation, and avoids repeats at the moveto
				continue;
			}

			// specially decode it if reversed
			if ((pco == null) || (pco.length < refpath.op.nlines * 6 + 2));
				pco = new float[refpath.op.nlines * 6 + 2];
			// this gives an array that is interspersed with the control points
			refpath.op.ToCoordsCubic(pco);

			// now put in the reverse coords.
			if (j == 0)
				gparea.moveTo(pco[refpath.op.nlines * 6], pco[refpath.op.nlines * 6 + 1]);

			for (int i = refpath.op.nlines - 1; i >= 0; i--)
			{
				int i6 = i * 6;
				if ((pco[i6 + 2] == pco[i6]) && (pco[i6 + 3] == pco[i6 + 1])) // and the next point too.
					gparea.lineTo(pco[i6], pco[i6 + 1]);
				else
					gparea.curveTo(pco[i6 + 4], pco[i6 + 5], pco[i6 + 2], pco[i6 + 3], pco[i6], pco[i6 + 1]);
			}
		}
		gparea.closePath();
	}




	/////////////////////////////////////////////
	OneSArea(OnePath lop, boolean lbFore) // edge scans to the right
	{
		// loop round to the start.
		OnePath op = lop;
		boolean bFore = lbFore;
		assert lop.AreaBoundingType();
		iareapressig = SketchLineStyle.ASE_KEEPAREA;  // reset in the loop if anything found
		pldframesketch = null; 
		pframesketch = null; 
		pframesketchtrans = null;
		zalt = 0.0F; // default

		do
		{
			// gone wrong.
			if (op == null)
				break;

			refpaths.addElement(new RefPathO(op, bFore));

			// jumps to the next segment on the next node
			OnePathNode opnN = (bFore ? op.pnend : op.pnstart);
			if (bFore)
			{
				bFore = !op.bapfrfore;
				op = op.apforeright;
			}
			else
			{
				bFore = !op.baptlfore;
				op = op.aptailleft;
			}
			assert opnN == (bFore ? op.pnstart : op.pnend);

			// go round that segment until we find an area bounding type
			while (!op.AreaBoundingType())
			{
				// look for any area killing symbols
				if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null))
				{
					if ((op.plabedl.barea_pres_signal != SketchLineStyle.ASE_HCOINCIDE) && (op.plabedl.barea_pres_signal != SketchLineStyle.ASE_ZSETRELATIVE))
						iareapressig = Math.max(iareapressig, op.plabedl.barea_pres_signal);
					if (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME)
						pldframesketch = op.plabedl; 
				}

				// mark the connective types anyway, as a root-start.
				if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
				{
					if (bFore)
						op.karight = this;
					else
						op.kaleft = this;
				}

				if (!bFore)
				{
					bFore = !op.bapfrfore;
					op = op.apforeright;
				}
				else
				{
					bFore = !op.baptlfore;
					op = op.aptailleft;
				}
				assert opnN == (bFore ? op.pnstart : op.pnend);
			}
		}
		while (!((op == lop) && (bFore == lbFore)));

		// set the pointers from paths to this area
		Setkapointers(true);
		if (op == null)
		{
			assert false;
			return;
		}

		// now make the refpathsub by copying over and removing duplicates (as we track down the back side of a tree).
		for (int i = 0; i < refpaths.size(); i++)
		{
			OnePath opsi = ((RefPathO)refpaths.elementAt(i)).op;
			OnePath opsl = (refpathsub.isEmpty() ? null : ((RefPathO)refpathsub.lastElement()).op);

			if (opsi != opsl)
				refpathsub.addElement(refpaths.elementAt(i));
			else
				refpathsub.removeElementAt(refpathsub.size() - 1);
		}
		// tree duplicates between the beginning and the end
		while ((refpathsub.size() >= 2) && (((RefPathO)refpathsub.firstElement()).op == ((RefPathO)refpathsub.lastElement()).op))
		{
			refpathsub.removeElementAt(refpathsub.size() - 1);
			refpathsub.removeElementAt(0);
		}


    	// this builds the general path which defines the area
		// set up the area if something is empty.
		if (refpathsub.isEmpty())
		{
			iareapressig = SketchLineStyle.ASE_KILLAREA; // don't render (outer tree?)
			return; // it turned out to be just a tree
		}

		// now we construct the general path from the list of untreed areas
		LinkArea();
		try
		{
		aarea = new Area(gparea);
  		}
  		catch (java.lang.InternalError e)  // this is to see a very rare failure in the area generating algorithm
  		{
			TN.emitWarning("Library Error creating Area from boundary");
			System.out.println(e.toString());
//			System.out.println("Number of nodes " + gparea.);
			System.out.println("bounding box " + gparea.getBounds2D());
			aarea = null;
  		}
		//if (refpathsub.size() != refpaths.size())
		//	TN.emitMessage("pathedges " + refpathsub.size() + " over total path edges " + refpaths.size());

		// set the zaltitude by finding the average height
		// (altitude must have been set from the linking already)
		float szalt = 0.0F;
		for (int i = 0; i < refpathsub.size(); i++)
			szalt += ((RefPathO)refpathsub.elementAt(i)).ToNode().zalt;
		if (refpathsub.size() != 0)
			zalt = szalt / refpathsub.size();

		// set the bounds area
		rboundsarea = gparea.getBounds();
	}

	/////////////////////////////////////////////
/*
	boolean AreaBoundsOtherArea(OneSArea posa)
	{
		for (int i = 0; i < refpathsub.size(); i++)
		{
			RefPathO rfo = (RefPathO)refpathsub.elementAt(i);
			assert ((rfo.bFore ? rfo.op.karight : rfo.op.kaleft) == this);
			if ((rfo.bFore ? rfo.op.kaleft : rfo.op.karight) == posa)
				return true;
		}
		return false;
	}
*/
/*
	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans)
	{
		if (currtrans == null)
			return gparea.getBounds();
		GeneralPath gp = (GeneralPath)gparea.clone();
	}
*/

	//////////////////////////////////////////
	void setId(String id)
	{
		this.svgid = id;	
	}
	//////////////////////////////////////////
	String getId()
	{
		return this.svgid;
	}
}

