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
	boolean bTangentsValid = false; 
	float tanangstart; 
	float tanangend; 

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

		String lplabel = sketchdisplay.sketchlinestyle.pthlabel.getText().trim(); 
		bRes |= !(lplabel.equals(plabel == null ? "" : plabel)); 
		plabel = (lplabel.length() == 0 ? null : lplabel); 
		
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

		// also have decided to default the spline style off at all times by default.  
		// we can introduce a spline-all later.  
		sketchdisplay.sketchlinestyle.pthsplined.setSelected(false); 
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
	float[] ToCoords()
	{
		float[] coords = new float[6]; 
		float[] pco = new float[nlines * 2 + 2]; 
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
				pco[i * 2 + 2] = coords[0]; 
				pco[i * 2 + 3] = coords[1]; 
			}
			else if (curvtype == PathIterator.SEG_QUADTO) 
			{
				pco[i * 2 + 2] = coords[2]; 
				pco[i * 2 + 3] = coords[3]; 
			}
			else if (curvtype == PathIterator.SEG_CUBICTO) 
			{
				pco[i * 2 + 2] = coords[4]; 
				pco[i * 2 + 3] = coords[5]; 
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
	OnePath FuseNode(OnePathNode pnconnect, OnePath op2)  
	{
		boolean breflect1 = (pnconnect != pnend); 
		boolean breflect2 = (pnconnect != op2.pnstart); 

		// make the new path 					
		OnePath respath = new OnePath(breflect1 ? pnend : pnstart); 
		respath.linestyle = linestyle; 

		float[] pco = ToCoords(); 
		for (int i = 1; i <= nlines; i++) 
		{
			int ir = (breflect1 ? nlines - i : i); 
			respath.LineTo(pco[ir * 2 + 0], pco[ir * 2 + 1]); 
		}
		
		float[] pco2 = op2.ToCoords(); 
		for (int i = 0; i < op2.nlines; i++) 
		{
			int ir = (breflect2 ? op2.nlines - i : i); 
			respath.LineTo(pco2[ir * 2 + 0], pco2[ir * 2 + 1]); 
		}

		respath.EndPath(breflect2 ? op2.pnstart : op2.pnend);  
		return respath; 
	}

	/////////////////////////////////////////////
	OnePath SplitNode(float ptx, float pty, float scale) 
	{
		float[] pco = ToCoords(); 

		float lam = 999.0F; 
		int ilam = -1; 
		float distsq = scale * scale;  
		for (int i = 0; i < nlines; i++) 
		{
			float vx = pco[i * 2 + 2] - pco[i * 2]; 
			float vy = pco[i * 2 + 3] - pco[i * 2 + 1]; 
			float pvx = ptx - pco[i * 2]; 
			float pvy = pty - pco[i * 2 + 1]; 
			float vsq = vx * vx + vy * vy; 
			float pdv = vx * pvx + vy * pvy; 
			float llam = Math.min(1.0F, Math.max(0.0F, (vsq == 0.0F ? 0.5F : pdv / vsq))); 

			float nptx = vx * llam - pvx; 
			float npty = vy * llam - pvy; 
			float dnptsq = nptx * nptx + npty * npty; 
			
			if (dnptsq < distsq) 
			{
				ilam = i; 
				lam = llam; 
				distsq = dnptsq; 
			}
		}

		if (ilam == -1) 
			return null; 

		// rephase to front of section.  
		if (lam == 1.0F) 
		{
			ilam++; 
			lam = 0.0F; 
		}

		// no chopping if on end of a line.  
		if ((lam == 0.0F) && ((ilam == 0) || (ilam == nlines))) 
			return null; 

		// make new node 
		OnePathNode pnmid = new OnePathNode(pco[ilam * 2] * (1.0F - lam) + pco[ilam * 2 + 2] * lam, pco[ilam * 2 + 1] * (1.0F - lam) + pco[ilam * 2 + 3] * lam, pnstart.zalt, pnstart.bzaltset); 

		// make the new path 					
		OnePath currgenend = new OnePath(pnmid); 
		currgenend.linestyle = linestyle; 

		for (int i = ilam; i < nlines - 1; i++) 
			currgenend.LineTo(pco[i * 2 + 2], pco[i * 2 + 3]); 
		currgenend.EndPath(pnend); 

		// now redo this edge.  
		gp.reset(); 
		bWantSplined = false; 
		nlines = 0; 
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY()); 
		if (lam == 0.0F) // don't need last point. 
			ilam--; 
		for (int i = 0; i < ilam; i++) 
			LineTo(pco[i * 2 + 2], pco[i * 2 + 3]); 
		EndPath(pnmid); 

		bTangentsValid = false; 

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


		float[] pco = ToCoords(); 
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
		float[] pco = ToCoords(); 
		bSplined = lbSplined; 

		if (bReflect)  
		{
			OnePathNode pnt = pnstart; 
			pnstart = pnend; 
			pnend = pnt; 

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
		}

		LoadFromCoords(pco); 
	}

	/////////////////////////////////////////////
	void LoadFromCoords(float[] pco)  
	{
		gp.reset(); 
		gp.moveTo(pco[0], pco[1]); 

		if (!bSplined) 
		{
			// non-splined
			for (int i = 0; i < nlines; i++) 
				gp.lineTo(pco[i * 2 + 2], pco[i * 2 + 3]); 
			return; 
		}		


		// now create splines 

		// Make a tangent at each node.  
		float[] tpco = new float[nlines * 2 + 2]; 

		float ptanx = -99999; 
		float ptany = -99999; 

		// the before point and the off end point.  
		float xm1; 
		float ym1; 
		if (pnstart == pnend)  // single loop type 
		{
			xm1 = pco[(nlines - 1) * 2]; 
			ym1 = pco[(nlines - 1) * 2 + 1]; 
		}
		else // in the future we'll search for the the best continuation.  
		{
			xm1 = pco[0]; 
			ym1 = pco[1]; 
		}


		float xp1; 
		float yp1; 
		if (pnstart == pnend)  // single loop type 
		{
			xp1 = pco[2]; 
			yp1 = pco[3]; 
		}
		else // in the future we'll search for the the best continuation.  
		{
			xp1 = pco[nlines * 2]; 
			yp1 = pco[nlines * 2 + 1]; 
		}

		// put in all the segments.  
		for (int i = 0; i <= nlines; i++) 
		{
			//TN.emitMessage("node " + String.valueOf(i)); 
			int ip = Math.max(0, i - 1); 
			int in = Math.min(nlines, i + 1); 

			float xv0 = pco[i * 2] - (i != 0 ? pco[(i - 1) * 2] : xm1); 
			float yv0 = pco[i * 2 + 1] - (i != 0 ? pco[(i - 1) * 2 + 1] : ym1); 

			float xv1 = (i != nlines ? pco[(i + 1) * 2] : xp1) - pco[i * 2]; 
			float yv1 = (i != nlines ? pco[(i + 1) * 2 + 1] : yp1) - pco[i * 2 + 1]; 

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
				gp.curveTo(pco[(i - 1) * 2] + ptanx * tfac, pco[(i - 1) * 2 + 1] + ptany * tfac, 
						   pco[i * 2] - ntanx * tfac, pco[i * 2 + 1] - ntany * tfac, 
						   pco[i * 2], pco[i * 2 + 1]); 
			
			}

			ptanx = ntanx; 
			ptany = ntany; 
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
			else if (!bSymbolType)
				TN.emitWarning("Label missing on centreline"); 
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
		float[] pco = ToCoords(); // not spline (respline on loading).  


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
		float[] pco = ToCoords(); // not spline (respline on loading).  
		for (int i = 0; i <= nlines; i++)
			los.WriteLine(String.valueOf(pco[i * 2]) + "  " + String.valueOf(pco[i * 2 + 1])); 

		los.WriteLine("}"); 
	}



	/////////////////////////////////////////////
	void DrawLabel(Graphics2D g2D)  
	{
		String labspread = TNXML.xrawextracttext(plabel, TNXML.sSPREAD); 
		if (labspread == null)
		{
			g2D.drawString(plabel, (float)pnstart.pn.getX(), (float)pnstart.pn.getY()); 
			return; 
		}
		
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

			float[] pco = ToCoords(); // not spline for now.  
		
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

			while ((scanlen <= dfcoR) && (lam != 1.0F))  
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
		g2D.setColor(bSActive ? SketchLineStyle.linestylecolactive : (bProperRender ? SketchLineStyle.linestylecolprint : SketchLineStyle.linestylecols[linestyle])); 

/*
if (bProperRender)  
{
	if (linestyle == SketchLineStyle.SLS_PITCHBOUND) 
		paintWdotted(g2D, TN.strokew / 2, 0.0F, TN.strokew * 4, TN.strokew * 2);   
	else if (linestyle == SketchLineStyle.SLS_CEILINGBOUND)  
		paintWdotted(g2D, TN.strokew / 2, TN.strokew * 3, TN.strokew * 4, TN.strokew * 2);  
	else if (linestyle == SketchLineStyle.SLS_ESTWALL)  
		paintWdotted(g2D, TN.strokew / 2, TN.strokew * 3, TN.strokew * 4, 0.0F);  
	else   
		paintWdotted(g2D, TN.strokew / 2, 0.0F, 1000.0F, 0.0F);  
}
*/

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
			if ((!bHideMarkers && !bProperRender) || (linestyle != SketchLineStyle.SLS_INVISIBLE) || bSActive) 
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
		gp.lineTo(x, y); 
		nlines++; 
	}

	

	/////////////////////////////////////////////
	Point2D BackOne() 
	{
		int Nnlines = nlines - 1; 
		if (Nnlines >= 0) 
		{
			// fairly desperate measures here.  almost worth making a new genpath and iterating through it.  
			float[] pco = ToCoords(); 

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
			pnend = new OnePathNode((float)pcp.getX(), (float)pcp.getY(), pnstart.zalt, pnstart.bzaltset); 

			pnend = lpnend; 
			if (((float)pcp.getX() != (float)pnend.pn.getX()) || ((float)pcp.getY() != (float)pnend.pn.getY()))  
				LineTo((float)pnend.pn.getX(), (float)pnend.pn.getY()); 
		}

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
		pnstart = lpnstart; 
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY()); 
	}

	/////////////////////////////////////////////
	// making centreline types  
	OnePath(OnePathNode lpnstart, OnePathNode lpnend, String lab) 
	{
		linestyle = SketchLineStyle.SLS_CENTRELINE;	
		pnstart = lpnstart; 
		gp.moveTo((float)pnstart.pn.getX(), (float)pnstart.pn.getY()); 
		EndPath(lpnend); 
		plabel = lab; 
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
		if (!bTangentsValid) 
		{
			float[] pco = ToCoords(); 
			tanangstart = (float)Vec3.Arg(pco[2] - pco[0], pco[3] - pco[1]); 
			tanangend = (float)Vec3.Arg(pco[nlines * 2 - 2] - pco[nlines * 2], pco[nlines * 2 - 1] - pco[nlines * 2 + 1]); 
			bTangentsValid = true; 
		}
		return(bForward ? tanangstart : tanangend); 
	}


	/////////////////////////////////////////////
	// for making the vizpaths.  
	OnePath(OnePath path) 
	{
		gp = (GeneralPath)path.gp.clone(); 
		linestyle = path.linestyle; 
	}
}

