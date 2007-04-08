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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.JCheckBox;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.io.IOException;

//
//
// OnePath
//
//

/////////////////////////////////////////////
class OnePath
{
	OnePathNode pnstart;
	OnePathNode pnend = null;
	GeneralPath gp = new GeneralPath();
	int nlines = 0;

	// the tangent angles forwards and backwards.
	private boolean bpcotangValid = false;
	private float tanangstart;
	private float tanangend;
	private float[] lpco; // the coords of the lines in generalpath
	float linelength;

	// control points of spline (used for eval).
	float[] lpccon = null;

	// path conditions
	int linestyle; // see SketchLineStyle.
	boolean bSplined = false;  // actual situation of the generalpath
	boolean bWantSplined = false;
	PathLabelDecode plabedl = null;  // set of conditions when centreline or connective

	// links for creating the auto-areas.
	OnePath aptailleft; // path forward in the right hand polygon
	boolean baptlfore;  // is it forward or backward (useful if path starts and ends at same place).

	OnePath apforeright;
	boolean bapfrfore;

	// links to areas on right and left of this path.
	OneSArea karight = null;
	OneSArea kaleft = null;

	// the area this connective line belongs to
	ConnectiveComponentAreas pthcca = null;

	// list of symbols this path contains
	List<OneSSymbol> vpsymbols = new ArrayList<OneSSymbol>();

	// the subsets this path is in (as a string)
	List<String> vssubsets = new ArrayList(); // Strings
	List<SubsetAttr> vssubsetattrs = new ArrayList(); // SubsetAttr (in parallel) from the current style

	SubsetAttr subsetattr = null;  // one chosen from the vector above

	boolean bpathvisiblesubset = false;


	// the tunnel name which we imported this path from
	String importfromname = null;

	// value set by other weighting operations for previewing
	Color zaltcol = null;


	// used in quality drawing to help with white outlines, 0 if untouched, 1 if white outline, 2 if counted, 3 if rendered
	int ciHasrendered = 0;

	// used for refering the the path in SVG files
	String svgid = null;
	
	static boolean bHideSplines = false;  // set from miHideSplines

	/////////////////////////////////////////////
	void SetSubsetAttrs(SubsetAttrStyle sas, OneTunnel vgsymbols)
	{
		vssubsetattrs.clear();
		subsetattr = null;
		if (sas != null)
		{
			for (String ssubset : vssubsets)
	        {
	        	SubsetAttr sa = sas.FindSubsetAttr(ssubset, false);
	        	if (sa != null)
				{
					vssubsetattrs.add(sa);
					subsetattr = sa;
				}
			}
		}

		// fetch default subset in absence
		if (subsetattr == null)
			subsetattr = sas.FindSubsetAttr("default", false);
		if (subsetattr == null)
			TN.emitError("missing default in SubsetAttrStyle");

		GenerateSymbolsFromPath(vgsymbols);

		// fetch label font, finding default if no match or unset.
		if ((plabedl != null) && (plabedl.sfontcode != null))
		{
			plabedl.labfontattr = subsetattr.FindLabelFont(plabedl.sfontcode, false);
			if (plabedl.labfontattr == null)
			{
				//TN.emitWarning("missing fontlabel " + plabedl.sfontcode + " in SubsetAttrStyle " + subsetattr.subsetname);
				plabedl.labfontattr = subsetattr.FindLabelFont("default", false);
			}
		}
	}

	/////////////////////////////////////////////
	boolean IsPathInSubset(String sactive)
	{
		// find if this path is in the subset
		return vssubsets.contains(sactive);
	}

	/////////////////////////////////////////////
	void RemoveFromSubset(String sactive)
	{
		// find if this path is in the subset
		vssubsets.remove(sactive);
		assert !vssubsets.contains(sactive);
	}


	/////////////////////////////////////////////
	boolean AreaBoundingType()
	{
		return ((nlines != 0) &&
				(linestyle != SketchLineStyle.SLS_CENTRELINE) &&
				(linestyle != SketchLineStyle.SLS_CONNECTIVE) /*&&
				(linestyle != SketchLineStyle.SLS_CEILINGBOUND)*/);
		// had taken out filled types, but this broke one of the symbol areas.
	}

	/////////////////////////////////////////////
	boolean IsDropdownConnective()
	{
		return ((linestyle == SketchLineStyle.SLS_CONNECTIVE) && (plabedl != null) && (plabedl.barea_pres_signal == SketchLineStyle.ASE_HCOINCIDE));  
	}

	/////////////////////////////////////////////
	boolean IsZSetNodeConnective()
	{
		return ((linestyle == SketchLineStyle.SLS_CONNECTIVE) && (plabedl != null) && (plabedl.barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE));  
	}

	/////////////////////////////////////////////
	boolean IsElevationPath()
	{
		return ((linestyle == SketchLineStyle.SLS_CONNECTIVE) && (plabedl != null) && (plabedl.barea_pres_signal == SketchLineStyle.ASE_ELEVATIONPATH));  
	}

	/////////////////////////////////////////////
	private void Update_pco()
	{
		// first update the pco list
		lpco = new float[nlines * 2 + 2];

		float[] coords = new float[6];
		PathIterator pi = gp.getPathIterator(null);
		if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO)
			TN.emitProgError("move to not first");

		// put in the moveto.
		lpco[0] = coords[0];
		lpco[1] = coords[1];
		pi.next();
		for (int i = 0; i < nlines; i++)
		{
			if (pi.isDone())
				TN.emitProgError("done before end");
			int curvtype = pi.currentSegment(coords);
			if (curvtype == PathIterator.SEG_LINETO)
			{
				lpco[i * 2 + 2] = coords[0];
				lpco[i * 2 + 3] = coords[1];
			}
			else if (curvtype == PathIterator.SEG_QUADTO)
				TN.emitProgError("No quadric segments");
			else if (curvtype == PathIterator.SEG_CUBICTO)
			{
				lpco[i * 2 + 2] = coords[4];
				lpco[i * 2 + 3] = coords[5];
			}
			else
				TN.emitProgError("not lineto");
			pi.next();
		}
		if (!pi.isDone())
			TN.emitProgError("not done at end");


		// set the caching flag
		SetTangentAngles();
		bpcotangValid = true;

		lpccon = null;
		if (bSplined)
			BuildSplineContolPoints();
	}

	/////////////////////////////////////////////
	private void SetTangentAngles()
	{
		// now do the tangent angles at the endpoints
		if (nlines >= 1)
		{
			tanangstart = (float)Vec3.Arg(lpco[2] - lpco[0], lpco[3] - lpco[1]);
			tanangend = (float)Vec3.Arg(lpco[nlines * 2 - 2] - lpco[nlines * 2], lpco[nlines * 2 - 1] - lpco[nlines * 2 + 1]);
		}

		// newly added lengths here.
		linelength = 0.0F;
		for (int i = 1; i <= nlines; i++)
		{
			float vx = lpco[i * 2] - lpco[i * 2 - 2];
			float vy = lpco[i * 2 + 1] - lpco[i * 2 - 1];
			linelength += (float)Math.sqrt(vx * vx + vy * vy);
		}
	}


	/////////////////////////////////////////////
	float[] GetCoords()
	{
		if (!bpcotangValid)
			Update_pco();
		return lpco;
	}

	/////////////////////////////////////////////
	static Point2D segpt = new Point2D.Double();
	double MeasureSegmentLength(int i)
	{
		if (!bSplined)
		{
			double vx = (lpco[i * 2 + 2] - lpco[i * 2]);
			double vy = (lpco[i * 2 + 3] - lpco[i * 2 + 1]);
			return Math.sqrt(vx * vx + vy * vy);
		}

		double prevx = lpco[i * 2];
		double prevy = lpco[i * 2 + 1];
		int nsegs = 5;
		double res = 0.0;
		for (int j = 1; j <= nsegs; j++)
		{
			double vx, vy;
			if (j < nsegs)
			{
				EvalSeg(segpt, null, i, (double)j / nsegs);
        		vx = segpt.getX() - prevx;
        		vy = segpt.getY() - prevy;
 				prevx = segpt.getX();
 				prevy = segpt.getY();
    		}
			else
			{
				vx = lpco[i * 2 + 2] - prevx;
				vy = lpco[i * 2 + 3] - prevy;
			}
			res += Math.sqrt(vx * vx + vy * vy);
		}
		return res;
	}


	/////////////////////////////////////////////
	void EvalSeg(Point2D res, Point2D tan, int i, double tr)
	{
		// line type straightforward
		if (!bSplined)
		{
			if (res != null)
				res.setLocation(lpco[i * 2] * (1.0 - tr) + lpco[i * 2 + 2] * tr,
								lpco[i * 2 + 1] * (1.0 - tr) + lpco[i * 2 + 3] * tr);
			if (tan != null)
				tan.setLocation(lpco[i * 2 + 2] - lpco[i * 2],
								lpco[i * 2 + 3] - lpco[i * 2 + 1]);
			return;
		}

		// full spline type
		double trsq = tr * tr;
		double trcu = trsq * tr;

		if (res != null)
		{
			double lp0 = -trcu + 3 * trsq - 3 * tr + 1.0;
			double lp1 = 3 * trcu - 6 * trsq + 3 * tr;
			double lp2 = -3 * trcu + 3 * trsq;
			double lp3 = trcu;
			res.setLocation(lpco[i * 2] * lp0 + lpccon[i * 4] * lp1 + lpccon[i * 4 + 2] * lp2 + lpco[i * 2 + 2] * lp3,
							lpco[i * 2 + 1] * lp0 + lpccon[i * 4 + 1] * lp1 + lpccon[i * 4 + 3] * lp2 + lpco[i * 2 + 3] * lp3);
		}

		if (tan != null)
		{
			double ltp0 = -3 * trsq + 6 * tr - 3;
			double ltp1 = 9 * trsq - 12 * tr + 3;
			double ltp2 = -9 * trsq + 6 * tr;
			double ltp3 = 3 * trsq;
			tan.setLocation(lpco[i * 2] * ltp0 + lpccon[i * 4] * ltp1 + lpccon[i * 4 + 2] * ltp2 + lpco[i * 2 + 2] * ltp3,
							lpco[i * 2 + 1] * ltp0 + lpccon[i * 4 + 1] * ltp1 + lpccon[i * 4 + 3] * ltp2 + lpco[i * 2 + 3] * ltp3);
		}
	}

	/////////////////////////////////////////////
	void Eval(Point2D res, Point2D tan, double t)
	{
		if (!bpcotangValid)
			TN.emitProgError("not synched pco");
		int i = (int)t;
		if (i == nlines)
			i--;
		double tr = t - i;
		EvalSeg(res, tan, i, tr);
	}

	/////////////////////////////////////////////
	OnePath FuseNode(OnePathNode pnconnect, OnePath op2)
	{
		boolean breflect1 = (pnconnect != pnend);
		boolean breflect2 = (pnconnect != op2.pnstart);

		// make the new path
		OnePath respath = new OnePath(breflect1 ? pnend : pnstart);
		respath.linestyle = linestyle;

		float[] pco = GetCoords();
		for (int i = 1; i < nlines; i++)
		{
			int ir = (breflect1 ? nlines - i : i);
			respath.LineTo(pco[ir * 2 + 0], pco[ir * 2 + 1]);
		}

		float[] pco2 = op2.GetCoords();
		for (int i = 0; i < op2.nlines; i++)
		{
			int ir = (breflect2 ? op2.nlines - i : i);
			respath.LineTo(pco2[ir * 2 + 0], pco2[ir * 2 + 1]);
		}

		respath.EndPath(breflect2 ? op2.pnstart : op2.pnend);

		respath.bWantSplined = (op2.bSplined && op2.bSplined);
		if (respath.bWantSplined && !OnePath.bHideSplines)
			respath.Spline(respath.bWantSplined, false);

		return respath;
	}


	/////////////////////////////////////////////
	static Point2D cpres = new Point2D.Double();
	static Point2D cptan = new Point2D.Double();
	double ClosestPoint(double ptx, double pty, double scale)
	{
		GetCoords();

		// find closest node within the scale, so we favour splitting at one of them rather than just to the side
		int ilam = -1;
		double scalesq = (scale != -1.0 ? scale * scale : -1.0); 
		double distsq = scalesq;
		if (scale != -1.0)
		{
			for (int i = 0; i <= nlines; i++)
			{
				double pvx = ptx - lpco[i * 2];
				double pvy = pty - lpco[i * 2 + 1];
				double pvsq = pvx * pvx + pvy * pvy;

				if ((distsq == -1.0) || (pvsq < distsq))
				{
					ilam = i;
					distsq = pvsq;
				}
			}
			if (ilam != -1)
				return ilam;
		}

		// not on node.  Find closest line.
		double lam = -1.0;
		for (int i = 0; i < nlines; i++)
		{
			double vx = lpco[i * 2 + 2] - lpco[i * 2];
			double vy = lpco[i * 2 + 3] - lpco[i * 2 + 1];
			double pvx = ptx - lpco[i * 2];
			double pvy = pty - lpco[i * 2 + 1];
			double vsq = vx * vx + vy * vy;
			double pdv = vx * pvx + vy * pvy;
			double llam = Math.min(1.0F, Math.max(0.0F, (vsq == 0.0F ? 0.5F : pdv / vsq)));

			double nptx = vx * llam - pvx;
			double npty = vy * llam - pvy;
			double dnptsq = nptx * nptx + npty * npty;

			if ((i == 0) || (dnptsq < distsq))
			{
				ilam = i;
				lam = llam;
				distsq = dnptsq;
			}
		}

		// if this is non-splined, we have an answer
		if (!bSplined)
			return ((((lam > 0.0) && (lam < 1.0) && (distsq < scalesq)) || (scale == -1.0)) ? ilam + lam : -1.0);

		// splined case; we look for a close evaluation.  hunt by rhapson.
		for (int j = 0; j < 3; j++)
		{
			Eval(cpres, cptan, ilam + lam);
			double pvx = ptx - cpres.getX();
			double pvy = pty - cpres.getY();
			distsq = pvx * pvx + pvy * pvy;
			double pdt = pvx * cptan.getX() + pvy * cptan.getY();
			double tansq = cptan.getX() * cptan.getX() + cptan.getY() * cptan.getY();
			double h = (tansq != 0.0 ? pdt / tansq : 0.0);
System.out.println("iter " + distsq + "  " + h);
			lam += h;
			if (lam < 0.0)
			{
				ilam--;
				lam += 1.0;
				if ((ilam < 0) || (lam < 0.0))
					return -1.0;
			}
			if (lam >= 1.0)
			{
				ilam++;
				lam += 1.0;
				if ((ilam > nlines) || (lam >= 1.0))
					return -1.0;
			}
		}

		// return the value if we are within the scale distance
		return (((scale == -1.0) || (distsq < scalesq)) ? (ilam + lam) : -1.0);
	}

	/////////////////////////////////////////////
	OnePath SplitNode(OnePathNode pnmid, double linesnap_t)
	{
		assert apforeright == null;
		assert aptailleft == null;

		// make the new path
		OnePath currgenend = new OnePath();
		currgenend.linestyle = linestyle;

		// copy over the spline information
		currgenend.bWantSplined = bWantSplined;
		currgenend.bSplined = bSplined;

		// do the end nodes
		currgenend.pnstart = pnmid;
		currgenend.pnend = pnend;
		pnend = pnmid;

		// make the new lists of points

		// the tail end edges
		int ndi = (int)linesnap_t;
		currgenend.nlines = nlines - ndi;
		currgenend.lpco = new float[(nlines + 1) * 2];
		currgenend.pnstart = pnmid;
		currgenend.lpco[0] = (float)pnmid.pn.getX();
		currgenend.lpco[1] = (float)pnmid.pn.getY();
		for (int i = 1; i <= currgenend.nlines; i++)
		{
			currgenend.lpco[i * 2] = lpco[(i + ndi) * 2];
			currgenend.lpco[i * 2 + 1] = lpco[(i + ndi) * 2 + 1];
		}
		if (currgenend.bSplined)
			currgenend.BuildSplineContolPoints();
		currgenend.LoadFromCoords();

		// the current path points
		float[] llpco = lpco;
		nlines = ndi + (ndi == linesnap_t ? 0 : 1);
		lpco = new float[(nlines + 1) * 2];
		for (int i = 0; i < nlines; i++)
		{
			lpco[i * 2] = llpco[i * 2];
			lpco[i * 2 + 1] = llpco[i * 2 + 1];
		}
		lpco[nlines * 2] = (float)pnmid.pn.getX();
		lpco[nlines * 2 + 1] = (float)pnmid.pn.getY();
		lpccon = null;
		if (bSplined)
			BuildSplineContolPoints();
		LoadFromCoords();


		bpcotangValid = false; // reload back just for thoose two numbers??

		return currgenend;
	}


	/////////////////////////////////////////////
	void CopyPathAttributes(OnePath op) // used by fuse and import sketch
	{
		// copy over values.
		linestyle = op.linestyle;
		bWantSplined = op.bWantSplined;
		if (bWantSplined && !OnePath.bHideSplines)
			Spline(bWantSplined, false);
		if (op.plabedl != null)
			plabedl = new PathLabelDecode(op.plabedl);

		assert vssubsets.isEmpty() && vssubsetattrs.isEmpty(); 
		vssubsets.addAll(op.vssubsets);
		vssubsetattrs.addAll(op.vssubsetattrs);

		bpathvisiblesubset = op.bpathvisiblesubset;
		importfromname = op.importfromname;
	}


	/////////////////////////////////////////////
	OnePath WarpPath(OnePathNode pnfrom, OnePathNode pnto, boolean bShearWarp)
	{
		// new endpoint nodes
		OnePathNode npnstart = (pnstart == pnfrom ? pnto : pnstart);
		OnePathNode npnend = (pnend == pnfrom ? pnto : pnend);

		// initial vector
		float xv = (float)(pnend.pn.getX() - pnstart.pn.getX());
		float yv = (float)(pnend.pn.getY() - pnstart.pn.getY());
		float vsq = xv * xv + yv * yv;

		// final vector
		float nxv = (float)(npnend.pn.getX() - npnstart.pn.getX());
		float nyv = (float)(npnend.pn.getY() - npnstart.pn.getY());
		float nvsq = nxv * nxv + nyv * nyv;

		// translation vector
		float xt = (float)(pnto.pn.getX() - pnfrom.pn.getX());
		float yt = (float)(pnto.pn.getY() - pnfrom.pn.getY());


		float[] pco = GetCoords();
		OnePath res = new OnePath(npnstart);

		// translation case (if endpoints match).
		if ((vsq == 0.0F) || (nvsq == 0.0F))
		{
			if ((vsq != 0.0F) || (nvsq != 0.0F))
				TN.emitWarning("Bad warp: only one axis vector is zero length");

			for (int i = 1; i < nlines; i++)
				res.LineTo(pco[i * 2] + xt, pco[i * 2 + 1] + yt);
		}

		// by shearing
		else if (bShearWarp)
		{
			for (int i = 1; i < nlines; i++)
			{
				float vix = pco[i * 2] - (float)pnstart.pn.getX();
				float viy = pco[i * 2 + 1] - (float)pnstart.pn.getY();
				float lam = (vix * xv + viy * yv) / vsq;

				res.LineTo(pco[i * 2] + lam * xt, pco[i * 2 + 1] + lam * yt);
			}
		}

		// rotation case (one endpoint matches)
		else
		{
			for (int i = 1; i < nlines; i++)
			{
				float vix = pco[i * 2] - (float)pnstart.pn.getX();
				float viy = pco[i * 2 + 1] - (float)pnstart.pn.getY();

				float lam = (vix * xv + viy * yv) / vsq;
				float plam = (vix * (-yv) + viy * (xv)) / vsq;

				res.LineTo((float)npnstart.pn.getX() + lam * nxv + plam * (-nyv), (float)npnstart.pn.getY() + lam * nyv + plam * (nxv));
			}
		}


		res.EndPath(npnend);
		res.CopyPathAttributes(this);
		return res;
	}




	/////////////////////////////////////////////
	void Spline(boolean lbSplined, boolean bReflect)
	{
		if (nlines == 0)
			return;

		// could search out paths on other sides of the nodes and make things tangent to them.

		// somehow kill segments that are too short.
		float[] pco = GetCoords();
		bSplined = lbSplined;

		if (bReflect)
		{
			for (int i = 0; i <= nlines / 2; i++)
			{
				int ir = nlines - i;

				float t0 = pco[i * 2 + 0];
				float t1 = pco[i * 2 + 1];

				pco[i * 2 + 0] = pco[ir * 2 + 0];
				pco[i * 2 + 1] = pco[ir * 2 + 1];

				pco[ir * 2 + 0] = t0;
				pco[ir * 2 + 1] = t1;
			}
			lpccon = null;
		}

		if (lpccon == null)
			BuildSplineContolPoints();

		LoadFromCoords();
		SetTangentAngles(); // these need resetting
	}

	/////////////////////////////////////////////
	private void BuildSplineContolPoints()
	{
		lpccon = new float[nlines * 4];

		// Make a tangent at each node.
		float ptanx = -99999;
		float ptany = -99999;

		// the before point and the off end point.
		float xm1;
		float ym1;
		if (pnstart == pnend)  // single loop type
		{
			xm1 = lpco[(nlines - 1) * 2];
			ym1 = lpco[(nlines - 1) * 2 + 1];
		}
		else // in the future we'll search for the the best continuation.
		{
			xm1 = lpco[0];
			ym1 = lpco[1];
		}


		float xp1;
		float yp1;
		if (pnstart == pnend)  // single loop type
		{
			xp1 = lpco[2];
			yp1 = lpco[3];
		}
		else // in the future we'll search for the the best continuation.
		{
			xp1 = lpco[nlines * 2];
			yp1 = lpco[nlines * 2 + 1];
		}

		// put in all the segments.
		for (int i = 0; i <= nlines; i++)
		{
			//TN.emitMessage("node " + String.valueOf(i));
			int ip = Math.max(0, i - 1);
			int in = Math.min(nlines, i + 1);

			float xv0 = lpco[i * 2] - (i != 0 ? lpco[(i - 1) * 2] : xm1);
			float yv0 = lpco[i * 2 + 1] - (i != 0 ? lpco[(i - 1) * 2 + 1] : ym1);

			float xv1 = (i != nlines ? lpco[(i + 1) * 2] : xp1) - lpco[i * 2];
			float yv1 = (i != nlines ? lpco[(i + 1) * 2 + 1] : yp1) - lpco[i * 2 + 1];

			float v0len = (float)Math.sqrt(xv0 * xv0 + yv0 * yv0);
			float v1len = (float)Math.sqrt(xv1 * xv1 + yv1 * yv1);

			if (v0len == 0.0F)
				v0len = 1.0F;
			if (v1len == 0.0F)
				v1len = 1.0F;

			float ntanx = xv0 / v0len + xv1 / v1len;
			float ntany = yv0 / v0len + yv1 / v1len;
			//TN.emitMessage("tan " + String.valueOf(ntanx) + ", " + String.valueOf(ntany));

			// put in the line to this point
			if (i != 0)
			{
				float tfac = Math.min(5.0F, v0len / 4.0F);
				lpccon[(i - 1) * 4] = lpco[(i - 1) * 2] + ptanx * tfac;
				lpccon[(i - 1) * 4 + 1] = lpco[(i - 1) * 2 + 1] + ptany * tfac;

				lpccon[(i - 1) * 4 + 2] = lpco[i * 2] - ntanx * tfac;
				lpccon[(i - 1) * 4 + 3] = lpco[i * 2 + 1] - ntany * tfac;
			}

			ptanx = ntanx;
			ptany = ntany;
		}
	}

	/////////////////////////////////////////////
	void LoadFromCoords()
	{
		gp.reset();
		gp.moveTo(lpco[0], lpco[1]);

		// non-splined
		if (!bSplined)
		{
			for (int i = 1; i <= nlines; i++)
				gp.lineTo(lpco[i * 2], lpco[i * 2 + 1]);
		}

		// splined
		else
		{
			for (int i = 1; i <= nlines; i++)
				gp.curveTo(lpccon[(i - 1) * 4], lpccon[(i - 1) * 4 + 1],
						   lpccon[(i - 1) * 4 + 2], lpccon[(i - 1) * 4 + 3],
						   lpco[i * 2], lpco[i * 2 + 1]);
		}
	}


	/////////////////////////////////////////////
	void UpdateStationLabelsFromCentreline()
	{
		assert (linestyle == SketchLineStyle.SLS_CENTRELINE);
		assert (plabedl != null);
		if ((plabedl.centrelinetail == null) && (plabedl.centrelinehead == null))
			return; 
		assert  ((plabedl.centrelinetail != null) && (plabedl.centrelinehead != null)); 
		assert  (plabedl.centrelineelev == null); 

		String pnlabtail = plabedl.centrelinetail;
		String pnlabhead = plabedl.centrelinehead;

		// put the station labels in . format.
		pnlabtail.replace('|', '.');
		pnlabtail.replace('^', '.');
		pnlabhead.replace('|', '.');
		pnlabhead.replace('^', '.');

		// these warnings are firing because we have vertical legs.
		if ((pnstart.pnstationlabel != null) && !pnstart.pnstationlabel.equals(pnlabtail))
			TN.emitWarning("Mismatch label station tail: " + pnlabtail + "  " + (pnstart.pnstationlabel == null ? "null" : pnstart.pnstationlabel));
		pnstart.pnstationlabel = pnlabtail;

		if ((pnend.pnstationlabel != null) && !pnend.pnstationlabel.equals(pnlabhead))
			TN.emitWarning("Mismatch label station head: " + pnlabhead + "  " + (pnend.pnstationlabel == null ? "null" : pnend.pnstationlabel));
		pnend.pnstationlabel = pnlabhead;
	}

	// joinpath.
	// warp to endpoints.

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, int ind0, int ind1, int indent) throws IOException
	{
		// we should be able to work out automatically which attributes are not necessary by keeping a stack, but not for now.
		if (bWantSplined)
			los.WriteLine(TNXML.xcomopen(indent, TNXML.sSKETCH_PATH, TNXML.sFROM_SKNODE, String.valueOf(ind0), TNXML.sTO_SKNODE, String.valueOf(ind1), TNXML.sSK_LINESTYLE, TNXML.EncodeLinestyle(linestyle), TNXML.sSPLINED, (bWantSplined ? "1" : "0")));
		else
			los.WriteLine(TNXML.xcomopen(indent, TNXML.sSKETCH_PATH, TNXML.sFROM_SKNODE, String.valueOf(ind0), TNXML.sTO_SKNODE, String.valueOf(ind1), TNXML.sSK_LINESTYLE, TNXML.EncodeLinestyle(linestyle)));

		if (plabedl != null)
			plabedl.WriteXML(los, indent + 1);

		// sketch subsets
		for (String ssubset : vssubsets)
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sSKSUBSET, TNXML.sSKSNAME, ssubset));
		if ((importfromname != null) && (importfromname.length() != 0))
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sSKIMPORTFROM, TNXML.sSKSNAME, importfromname));


		// write the pieces.
		float[] pco = GetCoords(); // not spline (respline on loading).


		// first point
		if (pnstart.IsCentrelineNode())
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[0]), TNXML.sPTY, String.valueOf(pco[1]), TNXML.sPTZ, String.valueOf(pnstart.zalt)));
		else
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[0]), TNXML.sPTY, String.valueOf(pco[1])));

		// middle points
		for (int i = 1; i < nlines; i++)
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[i * 2]), TNXML.sPTY, String.valueOf(pco[i * 2 + 1])));

		// end point (this may be a repeat of the first point (in case of a vertical surveyline).
		if (pnend.IsCentrelineNode())
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[nlines * 2]), TNXML.sPTY, String.valueOf(pco[nlines * 2 + 1]), TNXML.sPTZ, String.valueOf(pnend.zalt)));
		else
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[nlines * 2]), TNXML.sPTY, String.valueOf(pco[nlines * 2 + 1])));

		los.WriteLine(TNXML.xcomclose(indent, TNXML.sSKETCH_PATH));
	}



	/////////////////////////////////////////////
	// pull out the rsymbol things
	void GenerateSymbolsFromPath(OneTunnel vgsymbols)
	{
		vpsymbols.clear();
		if ((plabedl == null) || plabedl.vlabsymb.isEmpty())
			return;
		for (String rname : plabedl.vlabsymb)
		{
			SymbolStyleAttr ssa = subsetattr.FindSymbolSpec(rname, 0);
			if (ssa == null)
				continue;

			// this stuff should go...
			float[] pco = GetCoords();

			// now build the symbols defined by the aut-symbol.
			for (SSymbolBase ssb : ssa.ssymbolbs)
			{
				OneSSymbol oss = new OneSSymbol(pco, nlines, 0.0F, ssb, this);
				oss.RefreshSymbol();
				vpsymbols.add(oss);
			}
		}
	}

	/////////////////////////////////////////////
	void paintLabel(GraphicsAbstraction ga, Color col)
	{
		// labfontattr is not set for symbol paths at the moment
		if ((plabedl.labfontattr != null) && (plabedl.labfontattr.labelcolour == null))
			return; // over-ridden example.
		if ((plabedl.drawlab == null) || (plabedl.drawlab.length() == 0))
			return;
		plabedl.UpdateLabel((float)pnstart.pn.getX(), (float)pnstart.pn.getY(), (float)pnend.pn.getX(), (float)pnend.pn.getY());
		ga.drawlabel(plabedl, (float)pnstart.pn.getX(), (float)pnstart.pn.getY(), col); 
	}


	/////////////////////////////////////////////
	// takes in the active flag to draw outline on filled things
	void paintWquality(GraphicsAbstraction ga)
	{
		// non-drawable
		if ((linestyle == SketchLineStyle.SLS_INVISIBLE) || (linestyle == SketchLineStyle.SLS_CONNECTIVE))
			return;
		if (subsetattr.linestyleattrs[linestyle] == null)
		{
			TN.emitWarning("subset linestyle attr for " + linestyle + " missing for "+ subsetattr.subsetname);
			return;
		}

		if (subsetattr.linestyleattrs[linestyle].strokecolour == null)
			return; // hidden

		ga.drawPath(this, subsetattr.linestyleattrs[linestyle]);
 	}



	static Color colshadr = new Color(0.0F, 0.7F, 0.2F, 0.25F);
	static Color colshadl = new Color(0.3F, 0.7F, 0.0F, 0.25F);
	static Line2D.Float mouperplin = new Line2D.Float(); 
	void paintW(GraphicsAbstraction ga, boolean bisSubseted, boolean bSActive)
	{
		LineStyleAttr linestyleattr; 
		Color overloadcol = null; 
		// set the colour
		if (bSActive)
			linestyleattr = SketchLineStyle.ActiveLineStyleAttrs[linestyle]; 
		else if (bisSubseted || (zaltcol != null))
			linestyleattr = SketchLineStyle.inSelSubsetLineStyleAttrs[linestyle]; 
		else
			linestyleattr = SketchLineStyle.notInSelSubsetLineStyleAttrs[linestyle];

		Color col = (zaltcol != null ? zaltcol : linestyleattr.strokecolour);
		ga.drawPath(this, linestyleattr, col);
		
		// the text
		if ((linestyle == SketchLineStyle.SLS_CONNECTIVE) && (plabedl != null) && (plabedl.labfontattr != null))
			paintLabel(ga, col);
			
		// a side dash for pitch boundaries (could refer to a sketchdisplay.miTransitiveSubset.isSelected() type thing)
		if (!bSActive || !((linestyle == SketchLineStyle.SLS_PITCHBOUND) || (linestyle == SketchLineStyle.SLS_CEILINGBOUND)))
			return; 
		PathIterator pi = gp.getPathIterator(null);
		if (pi.currentSegment(moucoords) != PathIterator.SEG_MOVETO)
			return;
		float x0 = moucoords[0]; 
		float y0 = moucoords[1]; 
		pi.next();
		if (pi.isDone())
			return;
		int curvtype = pi.currentSegment(moucoords);
		//if (curvtype != PathIterator.SEG_LINETO)
		float x1 = moucoords[0]; 
		float y1 = moucoords[1]; 
		float xv = x1 - x0; 
		float yv = y1 - y0; 
		float vlen = (float)Math.sqrt(xv * xv + yv * yv); 
		if (vlen == 0.0F)
			return; 

		mouperplin.setLine(x1, y1, x1 - yv * SketchLineStyle.mouperplinlength / vlen, y1 + xv * SketchLineStyle.mouperplinlength / vlen); 
		ga.drawShape(mouperplin, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]); 
	}



	/////////////////////////////////////////////
	static float[] moucoords = new float[6];
	void IntermedLines(GeneralPath moupath, int nmoupathpieces)
	{
		if (nmoupathpieces == 1)
			return;

		PathIterator pi = moupath.getPathIterator(null);
		if (pi.currentSegment(moucoords) != PathIterator.SEG_MOVETO)
		{
			TN.emitProgError("move to not first");
			return;
		}

		// put in the moveto.
		pi.next();
		for (int i = 1; i < nmoupathpieces; i++)
		{
			if (pi.isDone())
			{
				TN.emitProgError("done before end");
				return;
			}
			int curvtype = pi.currentSegment(moucoords);
			if (curvtype != PathIterator.SEG_LINETO)
			{
				TN.emitProgError("not lineto");
				return;
			}

			LineTo(moucoords[0], moucoords[1]);
			pi.next();
		}

		if (pi.currentSegment(moucoords) != PathIterator.SEG_LINETO)
		{
			TN.emitProgError("last straight motion missing");
			return;
		}

		pi.next();
		if (!pi.isDone())
		{
			TN.emitProgError("not done at end Intermedlines");
			return;
		}
	}

	/////////////////////////////////////////////
	void LineTo(float x, float y)
	{
		bpcotangValid = false;
		gp.lineTo(x, y);
		nlines++;
	}



	/////////////////////////////////////////////
	Point2D BackOne()
	{
		bpcotangValid = false;
		int Nnlines = nlines - 1;
		if (Nnlines >= 0)
		{
			// fairly desperate measures here.  almost worth making a new genpath and iterating through it.
			float[] pco = GetCoords();

			gp.reset();
			nlines = 0;
			gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			for (int i = 0; i < Nnlines; i++)
				LineTo(pco[i * 2 + 2], pco[i * 2 + 3]);
		}
		return gp.getCurrentPoint();
	}


	/////////////////////////////////////////////
	boolean EndPath(OnePathNode lpnend)
	{
		bpcotangValid = false;
		if (lpnend == null)
		{
			if (nlines == 0)
				return false;

			Point2D pcp = gp.getCurrentPoint();
			pnend = new OnePathNode((float)pcp.getX(), (float)pcp.getY(), 0.0F);
		}
		else
		{
			Point2D pcp = gp.getCurrentPoint();
			pnend = lpnend;
			if (((float)pcp.getX() != (float)pnend.pn.getX()) || ((float)pcp.getY() != (float)pnend.pn.getY()))
				LineTo((float)pnend.pn.getX(), (float)pnend.pn.getY());
		}

		GetCoords();
		if (bWantSplined && !OnePath.bHideSplines)
			Spline(bWantSplined, false);
		return true;
	}

	/////////////////////////////////////////////
	OnePath()
	{
	}

	/////////////////////////////////////////////
	OnePath(OnePathNode lpnstart)
	{
		bpcotangValid = false;
		pnstart = lpnstart;
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY());
	}

	/////////////////////////////////////////////
	// making centreline types
	OnePath(OnePathNode lpnstart, String ltail, OnePathNode lpnend, String lhead)
	{
		bpcotangValid = false;
		linestyle = SketchLineStyle.SLS_CENTRELINE;
		pnstart = lpnstart;
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY());

		// this is the EndPath function, making sure that zero length centrelines still have two endpoints.
		pnend = lpnend;
		LineTo((float)pnend.pn.getX(), (float)pnend.pn.getY());

		plabedl = new PathLabelDecode(); // centreline type (very clear)
		plabedl.centrelinetail = ltail;
		plabedl.centrelinehead = lhead;

		// set the original length (which never gets updated)
		GetCoords();
	}


	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans)
	{
		if (currtrans == null)
			return gp.getBounds2D();

		// looks pretty horrid way to do it.
		GeneralPath lgp = (GeneralPath)gp.clone();
		lgp.transform(currtrans);
		return lgp.getBounds2D();
	}

	/////////////////////////////////////////////
	float GetTangent(boolean bForward)
	{
		if (!bpcotangValid)
			Update_pco();
		return(bForward ? tanangstart : tanangend);
	}

	/////////////////////////////////////////////
	// this function is crap format
	static float[] CCcoords = new float[6];
	void ToCoordsCubic(float[] pco)
	{
		assert pco.length >= nlines * 6 + 2;

		PathIterator pi = gp.getPathIterator(null);
		int curvtype = pi.currentSegment(CCcoords);
		assert curvtype == PathIterator.SEG_MOVETO;

		// put in the moveto.
		pco[0] = CCcoords[0];
		pco[1] = CCcoords[1];
		pi.next();
		for (int i = 0; i < nlines; i++)
		{
			int i6 = i * 6;
			assert !pi.isDone();
			curvtype = pi.currentSegment(CCcoords);
			if (curvtype == PathIterator.SEG_LINETO)
			{
				pco[i6 + 2] = CCcoords[0];
				pco[i6 + 3] = CCcoords[1];
				pco[i6 + 4] = CCcoords[0];
				pco[i6 + 5] = CCcoords[1];
				pco[i6 + 6] = CCcoords[0];
				pco[i6 + 7] = CCcoords[1];
			}
			else if (curvtype == PathIterator.SEG_CUBICTO)
			{
				pco[i6 + 2] = CCcoords[0];
				pco[i6 + 3] = CCcoords[1];
				pco[i6 + 4] = CCcoords[2];
				pco[i6 + 5] = CCcoords[3];
				pco[i6 + 6] = CCcoords[4];
				pco[i6 + 7] = CCcoords[5];
			}
			else
				assert false;
			pi.next();
		}
		assert pi.isDone();
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


