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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.io.IOException;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

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
	private float[] lpccon = null;


	int linestyle; // see SketchLineStyle.
	boolean bSplined = false;

	// this is an xml string which is able to label the head and tail when centreline type.
	String plabel = null;

	boolean bWantSplined;

	int prevlabelcode = 0; // used to tell when to update.
	Vector vlabelpoints;


	// links for creating the auto-areas.
	OnePath aptailleft; // path forward in the right hand polygon
	boolean baptlfore;  // is it forward or backward (useful if path starts and ends at same place).

	OnePath apforeright;
	boolean bapfrfore;

	// links to areas on right and left of this path.
	OneSArea karight;
	OneSArea kaleft;

	// list of symbols this path contains
	Vector vpsymbols = new Vector();

	// value set by the sliders
	boolean bvisiblebyz = true;

    // value set by other weighting operations for previewing
    int icolindex = -1;

	// used to count those already found by the connectives
	int iconncompareaindex = -1; // used by ConnectiveComponentAreas

	/////////////////////////////////////////////
	boolean AreaBoundingType()
	{
		return ((nlines != 0) && (linestyle != SketchLineStyle.SLS_CENTRELINE) && (linestyle != SketchLineStyle.SLS_CONNECTIVE));
		// had taken out filled types, but this broke one of the symbol areas.
	}


	/////////////////////////////////////////////
	void SetParametersIntoBoxes(SketchDisplay sketchdisplay)
	{
		sketchdisplay.ChangePathParams.maskcpp++;
		sketchdisplay.sketchlinestyle.pthsplined.setSelected(bWantSplined);
		sketchdisplay.sketchlinestyle.linestylesel.setSelectedIndex(linestyle);

		// this causes the SetParametersFromBoxes function to get called
		// because of the DocumentEvent
		sketchdisplay.sketchlinestyle.pthlabel.setText(plabel == null ? "" : plabel);
		sketchdisplay.ChangePathParams.maskcpp--;
	}


	/////////////////////////////////////////////
	// returns true if anything actually changed.
	boolean SetParametersFromBoxes(SketchDisplay sketchdisplay)
	{
		boolean bRes = false;

		int llinestyle = sketchdisplay.sketchlinestyle.linestylesel.getSelectedIndex();
		bRes |= (linestyle != llinestyle);
		linestyle = llinestyle;

		bRes |= (bWantSplined != sketchdisplay.sketchlinestyle.pthsplined.isSelected());
		bWantSplined = sketchdisplay.sketchlinestyle.pthsplined.isSelected();

//		String lplabel = sketchdisplay.sketchlinestyle.pthlabel.getText().trim();
//		bRes |= !(lplabel.equals(plabel == null ? "" : plabel));
//		plabel = (lplabel.length() == 0 ? null : lplabel);

		// go and spline it if required
		// (should do this in the redraw actually).
		if ((pnend != null) && (bWantSplined != bSplined))
			Spline(bWantSplined, false);

		return bRes;
	}

	/////////////////////////////////////////////
	// this clears the text and masks propagation of the dangerous centreline style.
	static void ClearSelectionIntoBoxes(SketchDisplay sketchdisplay)
	{
		sketchdisplay.sketchlinestyle.pthlabel.setText("");
		if (sketchdisplay.sketchlinestyle.linestylesel.getSelectedIndex() == SketchLineStyle.SLS_CENTRELINE)
			sketchdisplay.sketchlinestyle.linestylesel.setSelectedIndex(SketchLineStyle.SLS_DETAIL);

		// set the splining by default.
		// except make the splining off if the type is connective, which we don't really want splined since it's distracting.
		sketchdisplay.sketchlinestyle.pthsplined.setSelected(sketchdisplay.miDefaultSplines.isSelected() && (sketchdisplay.sketchlinestyle.linestylesel.getSelectedIndex() != SketchLineStyle.SLS_CONNECTIVE));
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
			float vx = lpco[nlines * 2] - lpco[nlines * 2 - 2];
			float vy = lpco[nlines * 2 + 1] - lpco[nlines * 2 - 1];
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
	void Eval(Point2D res, Point2D tan, double t)
	{
		if (!bpcotangValid)
			TN.emitProgError("not synched pco");
		int i = (int)t;
		if (i == nlines)
			i--;
		double tr = t - i;

		// line type straightforward
		if (!bSplined)
		{
			if (res != null)
				res.setLocation(lpco[i * 2] * (1.0 - tr) + lpco[i * 2 + 2] * tr,
								lpco[i * 2 + 1] * (1.0 - tr) + lpco[i * 2 + 3] * tr);
			if (tan != null)
				tan.setLocation(lpco[i * 2 + 1] - lpco[i * 2],
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
	OnePath FuseNode(OnePathNode pnconnect, OnePath op2)
	{
		boolean breflect1 = (pnconnect != pnend);
		boolean breflect2 = (pnconnect != op2.pnstart);

		// make the new path
		OnePath respath = new OnePath(breflect1 ? pnend : pnstart);
		respath.linestyle = linestyle;

		float[] pco = GetCoords();
		for (int i = 1; i <= nlines; i++)
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
		return respath;
	}


	/////////////////////////////////////////////
	static Point2D cpres = new Point2D.Double();
	static Point2D cptan = new Point2D.Double();
	double ClosestPoint(double ptx, double pty, double scale)
	{
		GetCoords();

		// find closest node within the scale
		int ilam = -1;
		double scalesq = scale * scale;
		double distsq = scalesq;
		for (int i = 0; i <= nlines; i++)
		{
			double pvx = ptx - lpco[i * 2];
			double pvy = pty - lpco[i * 2 + 1];
			double pvsq = pvx * pvx + pvy * pvy;

			if (pvsq < distsq)
			{
				ilam = i;
				distsq = pvsq;
			}
		}
		if (ilam != -1)
			return ilam;


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
			return (((lam > 0.0) && (lam < 1.0) && (distsq < scalesq)) ? ilam + lam : -1.0);

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
		return (distsq < scalesq ? (ilam + lam) : -1.0);
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

		// copy over values.
		res.linestyle = linestyle;
		res.plabel = plabel;

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
	void UpdateStationLabel(boolean bSymbolType)
	{
		if (linestyle == SketchLineStyle.SLS_CENTRELINE)
		{
			if (plabel != null)
			{
				if (bSymbolType)
					TN.emitWarning("Symbol type with label on axis");

				String pnlabtail = TNXML.xrawextracttext(plabel, TNXML.sTAIL);
				String pnlabhead = TNXML.xrawextracttext(plabel, TNXML.sHEAD);

				// put the station labels in . format.
				pnlabtail.replace('|', '.');
				pnlabtail.replace('^', '.');
				pnlabhead.replace('|', '.');
				pnlabhead.replace('^', '.');

				// these warnings are firing because we have vertical legs.
				if (pnlabtail != null)
				{
					if ((pnstart.pnstationlabel != null) && !pnstart.pnstationlabel.equals(pnlabtail))
						TN.emitWarning("Mismatch label station tail: " + plabel + "  " + (pnstart.pnstationlabel == null ? "null" : pnstart.pnstationlabel));
					pnstart.pnstationlabel = pnlabtail;
				}
				else
					TN.emitWarning("Centreline label missing tail: " + plabel);

				if (pnlabhead != null)
				{
					if ((pnend.pnstationlabel != null) && !pnend.pnstationlabel.equals(pnlabhead))
						TN.emitWarning("Mismatch label station head: " + plabel + "  " + (pnend.pnstationlabel == null ? "null" : pnend.pnstationlabel));
					pnend.pnstationlabel = pnlabhead;
				}
				else
					TN.emitWarning("Centreline label missing head: " + plabel);
			}
		}
	}

	// joinpath.
	// warp to endpoints.

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, int ind0, int ind1) throws IOException
	{
		// we should be able to work out automatically which attributes are not necessary by keeping a stack, but not for now.
		if (bWantSplined)
			los.WriteLine(TNXML.xcomopen(1, TNXML.sSKETCH_PATH, TNXML.sFROM_SKNODE, String.valueOf(ind0), TNXML.sTO_SKNODE, String.valueOf(ind1), TNXML.sSK_LINESTYLE, TNXML.EncodeLinestyle(linestyle), TNXML.sSPLINED, (bWantSplined ? "1" : "0")));
		else
			los.WriteLine(TNXML.xcomopen(1, TNXML.sSKETCH_PATH, TNXML.sFROM_SKNODE, String.valueOf(ind0), TNXML.sTO_SKNODE, String.valueOf(ind1), TNXML.sSK_LINESTYLE, TNXML.EncodeLinestyle(linestyle)));

		if (plabel != null)
			los.WriteLine(TNXML.xcomopen(2, TNXML.sLABEL) + plabel + TNXML.xcomclose(0, TNXML.sLABEL));

		// write the pieces.
		float[] pco = GetCoords(); // not spline (respline on loading).


		// first point
		if (pnstart.bzaltset)
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[0]), TNXML.sPTY, String.valueOf(pco[1]), TNXML.sPTZ, String.valueOf(pnstart.zalt)));
		else
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[0]), TNXML.sPTY, String.valueOf(pco[1])));

		// middle points
		for (int i = 1; i < nlines; i++)
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[i * 2]), TNXML.sPTY, String.valueOf(pco[i * 2 + 1])));

		// end point (this may be a repeat of the first point (in case of a vertical surveyline).
		if (pnend.bzaltset)
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[nlines * 2]), TNXML.sPTY, String.valueOf(pco[nlines * 2 + 1]), TNXML.sPTZ, String.valueOf(pnend.zalt)));
		else
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(pco[nlines * 2]), TNXML.sPTY, String.valueOf(pco[nlines * 2 + 1])));


		los.WriteLine(TNXML.xcomclose(1, TNXML.sSKETCH_PATH));
	}


	/////////////////////////////////////////////
	void WritePath(LineOutputStream los, int ind0, int ind1, int ipath) throws IOException
	{
		los.WriteLine("*Path_Sketch " + String.valueOf(ipath) + " { ");
		los.WriteLine(String.valueOf(ind0) + " " + String.valueOf(ind1)); // connecting points

		los.WriteLine("linestyle " + String.valueOf(linestyle) + (bWantSplined ? " S " : " O ") + "\"" + (plabel != null ? plabel : "") + "\"");

		// write the pieces.
		los.WriteLine("No_lines " + String.valueOf(nlines));
		float[] pco = GetCoords(); // not spline (respline on loading).
		for (int i = 0; i <= nlines; i++)
			los.WriteLine(String.valueOf(pco[i * 2]) + "  " + String.valueOf(pco[i * 2 + 1]));

		los.WriteLine("}");
	}


	/////////////////////////////////////////////
	// pull out the rsymbol things
	static String[] val = new String[1];
	static String[] attr = { TNXML.sLRSYMBOL_NAME, };
	void GenerateSymbolsFromPath(OneTunnel vgsymbols)
	{
		vpsymbols.removeAllElements();
		if ((plabel == null) || (plabel.length() == 0))
			return;
		String res = plabel;
		while (true)
		{
			res = TNXML.xrawextractattr(res, val, TNXML.sLRSYMBOL, attr);
			if (res == null)
				break;
			String rname = val[0];

			// find the matching symbol
			AutSymbolAc autsymbol = null;
			for (int k = 0; k < vgsymbols.vautsymbols.size(); k++)
			{
				AutSymbolAc lautsymbol = (AutSymbolAc)vgsymbols.vautsymbols.elementAt(k);
				if (rname.equals(lautsymbol.name))
                    {
                    	autsymbol = lautsymbol;
                    	break;
			    }
			}

			// this stuff should go...
			float[] pco = GetCoords();

			// now build the symbols defined by the aut-symbol.
			for (int j = 0; j < autsymbol.ssba.length; j++)
			{
				OneSSymbol oss = new OneSSymbol(pco, nlines, 0.0F);
				SSymbolBase ssb = autsymbol.ssba[j];
System.out.println("Adding gsym " + ssb.gsymname);
				oss.BSpecSymbol(ssb);
				oss.IncrementMultiplicity(ssb.nmultiplicity);

				oss.paxis = new Line2D.Float(pco[nlines * 2 - 2], pco[nlines * 2 - 1], pco[nlines * 2], pco[nlines * 2 + 1]);
				oss.RefreshSymbol(vgsymbols);

				vpsymbols.addElement(oss);
			}
		}
	}

	/////////////////////////////////////////////
	void DrawLabel(Graphics2D g2D)
	{
		String labspread = TNXML.xrawextracttext(plabel, TNXML.sSPREAD);
		if (labspread == null)
		{
			int ps = plabel.indexOf(TNXML.sLRSYMBOL);
			int pe = plabel.indexOf("/>");

			// standard label drawing
			// (this shall take <br> and <font> changes)
			if ((ps == -1) || (pe == -1))
				g2D.drawString(plabel, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}

		// implements the spread label drawing.
		if ((nlines == 0) || (labspread.length() < 2))
		{
			g2D.drawString(labspread, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}

		// update the label points only when necessary.
		int currlabelcode = (bSplined ? nlines : -nlines);
		if ((currlabelcode != prevlabelcode) || (vlabelpoints == null) || (vlabelpoints.size() != labspread.length()))
		{
			TN.emitMessage("spreading text");
			prevlabelcode = currlabelcode;

			float[] pco = GetCoords(); // not spline for now.

			// measure lengths
			float[] lengp = new float[nlines + 1];
			lengp[0] = 0.0F;
			for (int i = 1; i <= nlines; i++)
			{
				float xv = pco[i * 2] - pco[i * 2 - 2];
				float yv = pco[i * 2 + 1] - pco[i * 2 - 1];
				lengp[i] = lengp[i - 1] + (float)Math.sqrt(xv * xv + yv * yv);
			}

			// make up the labelpoints array.
			if (vlabelpoints == null)
				vlabelpoints = new Vector();
			vlabelpoints.setSize(labspread.length());

			// find the locations.
			int il = 1;
			for (int j = 0; j < labspread.length(); j++)
			{
				float lenb = lengp[nlines] * j / (labspread.length() - 1);
				while ((il < nlines) && (lengp[il] < lenb))
					il++;

				// find the lambda along this line.
				float lamden = lengp[il] - lengp[il - 1];
				float lam = (lamden != 0.0F ? (lengp[il] - lenb) / lamden : 0.0F);
				float tx = lam * pco[il * 2 - 2] + (1.0F - lam) * pco[il * 2];
				float ty = lam * pco[il * 2 - 1] + (1.0F - lam) * pco[il * 2 + 1];

				if (vlabelpoints.elementAt(j) == null)
					vlabelpoints.setElementAt(new Point2D.Float(), j);
				((Point2D)(vlabelpoints.elementAt(j))).setLocation(tx, ty);
			}
		}

		for (int i = 0; i < labspread.length(); i++)
		{
			Point2D pt = (Point2D)(vlabelpoints.elementAt(i));
			g2D.drawString(labspread.substring(i, i + 1), (float)pt.getX(), (float)pt.getY());
		}
	}


	// temporary botched hpgl output
	static float pxhpgl = -1.0F;
	static float pyhpgl = -1.0F;
	static StringBuffer sbhpgl = new StringBuffer();
	static float sca = 10.0F;
	static float xtrans = 0.0F;
	static float ytrans = 0.0F;

	public static void writepointHPGL(boolean bDraw, float x, float y)
	{
		sbhpgl.append(bDraw ? "PD" : "PU");
		sbhpgl.append((int)((x + xtrans) * sca));
		sbhpgl.append(",");
		sbhpgl.append((int)((y + ytrans) * sca));
		sbhpgl.append("; ");
	}

	/////////////////////////////////////////////
	public static void writeedgeHPGL(float x1, float y1, float x2, float y2)
	{
		if ((sbhpgl.length() == 0) || (x1 != pxhpgl) || (y1 != pyhpgl))
			writepointHPGL(false, x1, y1);
		writepointHPGL(true, x2, y2);
	}

	/////////////////////////////////////////////
	String writeHPGL()
	{
		if (linestyle == SketchLineStyle.SLS_PITCHBOUND)
			paintWdotted(null, TN.strokew / 2, 0.0F, TN.strokew * 4, TN.strokew * 2);
		else if (linestyle == SketchLineStyle.SLS_CEILINGBOUND)
			paintWdotted(null, TN.strokew / 2, TN.strokew * 3, TN.strokew * 4, TN.strokew * 2);
		else if (linestyle == SketchLineStyle.SLS_ESTWALL)
			paintWdotted(null, TN.strokew / 2, TN.strokew * 3, TN.strokew * 4, 0.0F);
		else
			paintWdotted(null, TN.strokew / 2, 0.0F, 1000.0F, 0.0F);

		String res = sbhpgl.toString();
		sbhpgl.setLength(0);
		return res;
	}



	/////////////////////////////////////////////
	void paintWdotted(Graphics2D g2D, float flatness, float dottleng, float spikegap, float spikeheight)
	{
		float[] coords = new float[6];
		float[] pco = new float[nlines * 6 + 2];


		// maybe we will do this without flattening paths in the future.
		FlatteningPathIterator fpi = new FlatteningPathIterator(gp.getPathIterator(null), flatness);
		if (fpi.currentSegment(coords) != PathIterator.SEG_MOVETO)
			TN.emitProgError("move to not first");

		// put in the moveto.
		float lx = coords[0];
		float ly = coords[1];
		// (dottleng == 0.0F) means pitch bound.
		int scanmode = (dottleng == 0.0F ? 0 : 1); // 0 for blank, 1 for approaching a spike, 2 for leaving a spike.
		float scanlen = (scanmode == 0 ? dottleng / 2 : spikegap / 2);

		fpi.next();
		while (!fpi.isDone())
		{
			int curvtype = fpi.currentSegment(coords);

			//if (curvtype == PathIterator.SEG_LINETO)
			if (curvtype != PathIterator.SEG_LINETO)
				TN.emitProgError("Flattened not lineto");

			// measure the distance to the coords.
			float vx = coords[0] - lx;
			float vy = coords[1] - ly;
			float dfco = (float)Math.sqrt(vx * vx + vy * vy);
			float lam = 0.0F;
			float dfcoR = dfco;
			float lxR = lx;
			float lyR = ly;
			boolean bCont = false;

			while ((scanlen <= dfcoR) && (lam != 1.0F) && (dfcoR != 0.0F))
			{
				// find the lam where this ends
				float lam1 = Math.min(1.0F, lam + scanlen / dfco);
				float lx1 = lx + vx * lam1;
				float ly1 = ly + vy * lam1;
				if (scanmode != 0)
				{
					if (g2D != null)
						g2D.draw(new Line2D.Float(lxR, lyR, lx1, ly1));
					else
						writeedgeHPGL(lxR, lyR, lx1, ly1);
				}

				lxR = lx1;
				lyR = ly1;
				lam = lam1;
				dfcoR -= scanlen;

				// spike if necessary
				if (scanmode == 1)
				{
					// right hand spike.
					if (spikeheight != 0.0F)
					{
						if (g2D != null)
							g2D.draw(new Line2D.Float(lxR, lyR, lxR - vy * spikeheight / dfco, lyR + vx * spikeheight / dfco));
						else
							writeedgeHPGL(lxR, lyR, lxR - vy * spikeheight / dfco, lyR + vx * spikeheight / dfco);
					}

					if (dottleng != 0.0F)
					{
						scanmode = 2;
						scanlen = spikegap / 2;
					}
					else
						scanlen = spikegap;
				}
				else if (scanmode == 0)
				{
					scanlen = spikegap / 2;
					scanmode = 1;
				}
				else
				{
					scanlen = dottleng;
					scanmode = 0;
				}
			}

			if (scanmode != 0)
			{
				if (g2D != null)
					g2D.draw(new Line2D.Float(lxR, lyR, coords[0], coords[1]));
				else
					writeedgeHPGL(lxR, lyR, coords[0], coords[1]);
			}

			scanlen -= dfcoR;

			lx = coords[0];
			ly = coords[1];

			fpi.next();
		}
	}


	/////////////////////////////////////////////
	void paintW(Graphics2D g2D, boolean bHideMarkers, boolean bSActive, boolean bProperRender)
	{
		// set the colour
		if (bSActive)
			g2D.setColor(SketchLineStyle.linestylecolactive);
		else if (icolindex != -1)
			g2D.setColor(SketchLineStyle.linestylecolsindex[icolindex]);
		else if (bProperRender)
			g2D.setColor(SketchLineStyle.linestylecolprint);
		else
			g2D.setColor(SketchLineStyle.linestylecols[linestyle]);


		// special dotted type things
		if (bProperRender && ((linestyle == SketchLineStyle.SLS_PITCHBOUND) || (linestyle == SketchLineStyle.SLS_CEILINGBOUND)))
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			if (linestyle == SketchLineStyle.SLS_PITCHBOUND)
				paintWdotted(g2D, TN.strokew / 2, 0.0F, TN.strokew * 4, TN.strokew * 2);
			else
				paintWdotted(g2D, TN.strokew / 2, TN.strokew * 3, TN.strokew * 4, TN.strokew * 2);
		}

		// standard drawing.
		else if (gp != null)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[linestyle]);
			if ((!bHideMarkers && !bProperRender) || ((linestyle != SketchLineStyle.SLS_INVISIBLE) && (linestyle != SketchLineStyle.SLS_CONNECTIVE)) || bSActive)
			{
				if ((linestyle != SketchLineStyle.SLS_FILLED) || bSActive)
					g2D.draw(gp);
				else
					g2D.fill(gp);
			}
		}

		// the text
		if ((plabel != null) && (linestyle != SketchLineStyle.SLS_CENTRELINE))
		{
			DrawLabel(g2D);
		}

		// draw in the tangents
		/*
		if (pnend != null)
		{
			g2D.drawLine((int)pnstart.pn.x, (int)pnstart.pn.y, (int)(pnstart.pn.x + 10 * Math.cos(GetTangent(true))), (int)(pnstart.pn.y + 10 * Math.sin(GetTangent(true))));
			g2D.drawLine((int)pnend.pn.x, (int)pnend.pn.y, (int)(pnend.pn.x + 10 * Math.cos(GetTangent(false))), (int)(pnend.pn.y + 10 * Math.sin(GetTangent(false))));
		}
		*/
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
			pnend = new OnePathNode((float)pcp.getX(), (float)pcp.getY(), pnstart.zalt, pnstart.bzaltset);
		}
		else
		{
			Point2D pcp = gp.getCurrentPoint();
			pnend = lpnend;
			if (((float)pcp.getX() != (float)pnend.pn.getX()) || ((float)pcp.getY() != (float)pnend.pn.getY()))
				LineTo((float)pnend.pn.getX(), (float)pnend.pn.getY());
		}

		GetCoords();
		if (bWantSplined)
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
	OnePath(OnePathNode lpnstart, OnePathNode lpnend, String lab)
	{
		bpcotangValid = false;
		linestyle = SketchLineStyle.SLS_CENTRELINE;
		pnstart = lpnstart;
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY());

		// this is the EndPath function, making sure that zero length centrelines still have two endpoints.
		pnend = lpnend;
		LineTo((float)pnend.pn.getX(), (float)pnend.pn.getY());
		plabel = lab;

		// set the original length (which never gets updated)
		GetCoords();
	}


	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans)
	{
		if (currtrans == null)
			return gp.getBounds();

		GeneralPath lgp = (GeneralPath)gp.clone();
		lgp.transform(currtrans);
		return lgp.getBounds();
	}

	/////////////////////////////////////////////
	float GetTangent(boolean bForward)
	{
		if (!bpcotangValid)
			Update_pco();
		return(bForward ? tanangstart : tanangend);
	}

	/////////////////////////////////////////////
	float[] ToCoordsCubic()
	{
		float[] coords = new float[6];
		float[] pco = new float[nlines * 6 + 2];

		PathIterator pi = gp.getPathIterator(null);
		if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO)
		{
			TN.emitProgError("move to not first");
			return null;
		}

		// put in the moveto.
		pco[0] = coords[0];
		pco[1] = coords[1];
		pi.next();
		for (int i = 0; i < nlines; i++)
		{
			if (pi.isDone())
			{
				TN.emitProgError("done before end");
				return null;
			}
			int curvtype = pi.currentSegment(coords);
			if (curvtype == PathIterator.SEG_LINETO)
			{
				pco[i * 6 + 2] = coords[0];
				pco[i * 6 + 3] = coords[1];
				pco[i * 6 + 4] = coords[0];
				pco[i * 6 + 5] = coords[1];
				pco[i * 6 + 6] = coords[0];
				pco[i * 6 + 7] = coords[1];
			}
			else if (curvtype == PathIterator.SEG_CUBICTO)
			{
				pco[i * 6 + 2] = coords[0];
				pco[i * 6 + 3] = coords[1];
				pco[i * 6 + 4] = coords[2];
				pco[i * 6 + 5] = coords[3];
				pco[i * 6 + 6] = coords[4];
				pco[i * 6 + 7] = coords[5];
			}
			else if (curvtype == PathIterator.SEG_QUADTO)
			{
				TN.emitProgError("quad present");
				return null;
			}
			else
			{
				TN.emitProgError("not lineto");
				return null;
			}
			pi.next();
		}
		if (!pi.isDone())
		{
			TN.emitProgError("not done at end");
			return null;
		}


		return pco;
	}


	/////////////////////////////////////////////
	// for making the vizpaths.
	OnePath(OnePath path)
	{
		bpcotangValid = false;
		gp = (GeneralPath)path.gp.clone();
		linestyle = path.linestyle;
		linelength = path.linelength; 
	}
}

