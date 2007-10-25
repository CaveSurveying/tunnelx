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
	List<SubsetAttr> vssubsetattrs = new ArrayList<SubsetAttr>(); // SubsetAttr (in parallel) from the current style
	SubsetAttr subsetattr = null;  // one chosen from the vector above

	// array of RefPathO.
	List<RefPathO> refpaths = new ArrayList<RefPathO>();
	List<RefPathO> refpathsub = new ArrayList<RefPathO>(); // subselection without the trees.
	List<OnePath> connpathrootscen = new ArrayList<OnePath>(); // used to free up pointer to an area, and for centrelines when they are drawn on top of areas
	
	// ConnectiveComponentAreas this area is in
	List<ConnectiveComponentAreas> ccalist = new ArrayList<ConnectiveComponentAreas>();

	// these are used to mark the areas for inclusion in sketchsymbolareas.  more efficient than setting false it as a booleans.
//	int iamark = 0;
//	static int iamarkl = 1;
	int distinctoaid; // used for the comparator as this is in a hashset
	static int Sdistinctoaid = 1; 
	
	// used in the quality rendering for signaling which edges can be drawn once areas on both sides have been done.
	boolean bHasrendered = false;

	// maximized around the contour for right precedence

	// ASE_ type
	int iareapressig = SketchLineStyle.ASE_KEEPAREA; // 0-1 normal, 3 column(rock), 2 pitchhole
	List<SketchFrameDef> sketchframedefs = null; // when iareapressig is SketchLineStyle.ASE_SKETCHFRAME, and we have a framed sketch.  This object specifies the transformations

	// used for refering to the area in SVG files
	String svgid = null;

	/////////////////////////////////////////////
	void paintHatchW(GraphicsAbstraction ga, int isa)
	{
		if (gparea != null)
			ga.drawHatchedArea(this, isa);
	}

	/////////////////////////////////////////////
	// we're going to use this to help sort vareas.  the zalt values better not change throughout the age of this object
	public int compareTo(OneSArea osa)
	{
		if (zalt != osa.zalt)
			return (zalt - osa.zalt < 0.0F ? -1 : 1);
		return distinctoaid - osa.distinctoaid;   // was: return hashCode() - osa.hashCode(), which caused errors when two distinct areas had the same hashcode and the second didn't make it into vsareas
	}

	/////////////////////////////////////////////
	// find which subsets this area is in, by looking at the surrounding edges
	// this is different to the isubsetcode thing; something between these two is redundant
	void DecideSubsets(List<String> lvssubsets)
	{
		assert lvssubsets.isEmpty();
		for (RefPathO rpo : refpathsub)
		{
			// find the intersection between these sets (using string equalities)
			List<String> pvssub = rpo.op.vssubsets;
			if (!lvssubsets.isEmpty())
			{
				for (int j = lvssubsets.size() - 1; j >= 0; j--)
				{
					if (!pvssub.contains(lvssubsets.get(j)))
						lvssubsets.remove(j);
				}
				if (lvssubsets.isEmpty())
					break; 
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
			int i = 0; 
			for (RefPathO rpo : refpathsub)
			{
				// find the intersection between these sets (using string equalities)
				List<SubsetAttr> pvssub = rpo.op.vssubsetattrs;
				if (i != 0)
				{
					for (int j = vssubsetattrs.size() - 1; j >= 0; j--)
					{
						if (!pvssub.contains(vssubsetattrs.get(j)))
							vssubsetattrs.remove(j);
					}
				}
				else
					vssubsetattrs.addAll(pvssub);
				i++; 
			}
			// no overlapping values, find default
			if (vssubsetattrs.isEmpty())
        		subsetattr = sas.msubsets.get("default");
			else
				subsetattr = vssubsetattrs.get(vssubsetattrs.size() - 1); // gets last one (could choose the highest priority one -- eg one that forces to hide)
			assert subsetattr != null;
		}

		// set the visibility flag
		bareavisiblesubset = true;
		for (RefPathO rpo : refpaths)
		{
			if (!rpo.op.bpathvisiblesubset)
				bareavisiblesubset = false;
		}
		return (bareavisiblesubset ? 1 : 0);
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
	void Setkapointers()
	{
		// we should perform the hard task of reflecting certain paths in situ.
		for (RefPathO refpath : refpaths)
		{
			// get the ref path.
			if (refpath.bFore)
			{
				assert refpath.op.karight == null;
				refpath.op.karight = this;
			}
			else
			{
				assert refpath.op.kaleft == null;
				refpath.op.kaleft = this;
			}
		}
	}

	/////////////////////////////////////////////
	void SetkapointersClear()
	{
		// we should perform the hard task of reflecting certain paths in situ.
		for (RefPathO refpath : refpaths)
		{
			// get the ref path.
			if (refpath.bFore)
			{
				assert refpath.op.karight == this;
				refpath.op.karight = null;
			}
			else
			{
				assert refpath.op.kaleft == this;
				refpath.op.kaleft = null;
			}
		}

		for (OnePath op : connpathrootscen)
		{
			assert (op.linestyle == SketchLineStyle.SLS_CONNECTIVE) || ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.kaleft == this) && (op.karight == this));
			if (op.kaleft == this)
				op.kaleft = null;
			if (op.karight == this)
				op.karight = null;
		}

		for (ConnectiveComponentAreas cca : ccalist)
		{
			assert cca.vconnareas.contains(this);
			cca.vconnareas.remove(this);
			for (OnePath cop : cca.vconnpaths)
			{
				assert (cop.kaleft != this);
				assert (cop.karight != this);
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
		boolean bfirst = true; 
		for (RefPathO refpath : refpathsub)
		{
			// if going forwards, then everything works
			if (refpath.bFore)
			{
				gparea.append(refpath.op.gp, !bfirst); // the second parameter is continuation, and avoids repeats at the moveto
				bfirst = false;
				continue;
			}

			// specially decode it if reversed
			if ((pco == null) || (pco.length < refpath.op.nlines * 6 + 2));
				pco = new float[refpath.op.nlines * 6 + 2];
			// this gives an array that is interspersed with the control points
			refpath.op.ToCoordsCubic(pco);

			// now put in the reverse coords.
			if (bfirst)
			{
				gparea.moveTo(pco[refpath.op.nlines * 6], pco[refpath.op.nlines * 6 + 1]);
				bfirst = false; 
			}
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
	void SetCentrelineThisArea(OnePath op)
	{
		assert op.linestyle == SketchLineStyle.SLS_CENTRELINE;
		assert op.karight == op.kaleft;
		if (op.karight != null)
		{
			if (compareTo(op.karight) <= 0)
			{
				assert zalt <= op.karight.zalt;
				return;
			}
			assert zalt >= op.karight.zalt;
			boolean bD = op.karight.connpathrootscen.remove(op);
			assert bD;
		}
		op.karight = this;
		op.kaleft = this;
		connpathrootscen.add(op);
 	}


	/////////////////////////////////////////////
	void MarkConnectiveRootStart(OnePath op, boolean bFore)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		if (bFore)
			op.karight = this;
		else
			op.kaleft = this;
		connpathrootscen.add(op);
	}


	/////////////////////////////////////////////
	void MarkCentrelineRoot(OnePath op, boolean bFore)
	{
		OnePathNode opn = (bFore ? op.pnstart : op.pnend);
		assert opn.IsCentrelineNode();

		// track round the centreline node
		OnePath opC = op;
		boolean bForeC = bFore;
		do
		{
			if (!bForeC)
			{
				bForeC = !opC.bapfrfore;
				opC = opC.apforeright;
			}
			else
			{
				bForeC = !opC.baptlfore;
				opC = opC.aptailleft;
			}
			assert opn == (bForeC ? opC.pnstart : opC.pnend);
			if (opC.linestyle == SketchLineStyle.SLS_CENTRELINE)
				SetCentrelineThisArea(opC);
		}
		while (!((opC == op) && (bForeC == bFore)));  // end of do loop
	}

	/////////////////////////////////////////////
	// construction from wherever
	OneSArea(OnePath lop, boolean lbFore) // edge scans to the right
	{
		// loop round to the start.
		OnePath op = lop;
		boolean bFore = lbFore;
		assert lop.AreaBoundingType();
		iareapressig = SketchLineStyle.ASE_KEEPAREA;  // reset in the loop if anything found
		sketchframedefs = null;
		zalt = 0.0F; // default

		distinctoaid = Sdistinctoaid++;

		do
		{
			// gone wrong.
			if (op == null)
				break;

			refpaths.add(new RefPathO(op, bFore));

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
					if ((op.plabedl.barea_pres_signal != SketchLineStyle.ASE_HCOINCIDE) && (op.plabedl.barea_pres_signal != SketchLineStyle.ASE_ZSETRELATIVE) && (op.plabedl.barea_pres_signal != SketchLineStyle.ASE_ELEVATIONPATH))
						iareapressig = Math.max(iareapressig, op.plabedl.barea_pres_signal);
					if (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME)
					{
						if (sketchframedefs == null)
							sketchframedefs = new ArrayList<SketchFrameDef>();
						sketchframedefs.add(op.plabedl.sketchframedef);
					}
				}

				// mark the connective types anyway, as a root-start.
				if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
					MarkConnectiveRootStart(op, !bFore);

				if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
					SetCentrelineThisArea(op);

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
			}  // endwhile (!op.AreaBoundingType())
		}
		while (!((op == lop) && (bFore == lbFore)));  // end of do loop

		// set the pointers from paths to this area
		Setkapointers();
		if (op == null)
		{
			assert false;
			return;
		}

		// now make the refpathsub by copying over and removing duplicates (as we track down the back side of a tree).
		for (int i = 0; i < refpaths.size(); i++)
		{
			OnePath opsi = refpaths.get(i).op;
			OnePath opsl = (refpathsub.isEmpty() ? null : refpathsub.get(refpathsub.size() - 1).op);

			if (opsi != opsl)
				refpathsub.add(refpaths.get(i));
			else
				refpathsub.remove(refpathsub.size() - 1);
		}
		// tree duplicates between the beginning and the end
		while ((refpathsub.size() >= 2) && (refpathsub.get(0).op == refpathsub.get(refpathsub.size() - 1).op))
		{
			refpathsub.remove(refpathsub.size() - 1);
			refpathsub.remove(0);
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

		rboundsarea = gparea.getBounds();

		// set the zaltitude by finding the average height
		// (altitude must have been set from the linking already)
		float szalt = 0.0F;
		for (RefPathO rpo : refpathsub)
			szalt += rpo.ToNode().zalt;
		if (refpathsub.size() != 0)
			zalt = szalt / refpathsub.size();

		for (int i = connpathrootscen.size() - 1; i >= 0; i--)
		{
			OnePath llop = connpathrootscen.get(i);
			if (llop.pnstart.IsCentrelineNode())
				MarkCentrelineRoot(llop, true);
			else if (llop.pnend.IsCentrelineNode())
				MarkCentrelineRoot(llop, false);
		}
	}

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

