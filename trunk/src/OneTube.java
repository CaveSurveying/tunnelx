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


import java.awt.Graphics; 
import java.util.List; 
import java.io.IOException; 


/////////////////////////////////////////////
class XElong 
{
	float xhi; 
	float xlo; 
	int cnxshi; 
	int cnxslo; 
	
	int cnxs0hi; 
	int cnxs0lo; 
	float xrg0; 

	int cnxs1hi; 
	int cnxs1lo; 
	float xrg1; 

	Vec3 xvext = new Vec3(); 
	Vec3 xvvax = new Vec3(); 
	Vec3 xvside = new Vec3(); 


	int cnxs0left; 
	int cnxs0right; 

	int cnxs1left; 
	int cnxs1right; 
		

	float FindExtents(OneSection xsection, Vec3 vext) 
	{
		for (int i = 0; i < xsection.nnodes; i++) 
		{
			Vec3 eloc = xsection.ELoc[i]; 
			float xe = vext.Dot(eloc); 
			if ((i == 0) || (xe < xlo)) 
			{
				xlo = xe; 
				cnxslo = i; 
			}
			if ((i == 0) || (xe > xhi)) 
			{
				xhi = xe; 
				cnxshi = i; 
			}
		}

		return xhi - xlo; 
	}

	void ClearBothExtents() 
	{
		xrg0 = -1.0F; 
	}

	void AdaptExtents(OneSection xsection0, OneSection xsection1, Vec3 vext) 
	{
		float lxrg0 = FindExtents(xsection0, vext); 
		int lcnxs0lo = cnxslo; 
		int lcnxs0hi = cnxshi; 

		float lxrg1 = FindExtents(xsection1, vext); 

		// copy in new extents.  
		if ((xrg0 == -1.0F) || (lxrg0 + lxrg1 > xrg0 + xrg1)) 
		{
			xrg0 = lxrg0; 
			cnxs0lo = lcnxs0lo; 
			cnxs0hi = lcnxs0hi; 

			xrg1 = lxrg1; 
			cnxs1lo = cnxslo; 
			cnxs1hi = cnxshi; 
		}
	}

	boolean FindSideExtents(OneSection xsection, int acnxslo, int acnxshi, boolean bTwist) 
	{
		if (acnxslo == acnxshi) 
			return false; 

		Vec3 eloclo = xsection.ELoc[acnxslo]; 
		Vec3 elochi = xsection.ELoc[acnxshi]; 

		xvvax.Diff(eloclo, elochi); 
		xvside.Cross(xvvax, xsection.vcperp); 
		if (!bTwist)
			xvside.Negate(); 
		float xvside0 = xvside.Dot(eloclo); 
		
		// go down the right hand side 
		for (int i = acnxslo; i != acnxshi; i = (i + (bTwist ? -1 : 1) + xsection.nnodes) % xsection.nnodes) 
		{
			Vec3 eloc = xsection.ELoc[i]; 
			float lxhi = xvside.Dot(eloc) - xvside0; 
			if ((i == acnxslo) || (lxhi > xhi)) 
			{
				xhi = lxhi; 
				cnxshi = i; 
			}
		}

		// go up the right hand side 
		for (int i = acnxshi; i != acnxslo; i = (i + (bTwist ? -1 : 1) + xsection.nnodes) % xsection.nnodes) 
		{
			Vec3 eloc = xsection.ELoc[i]; 
			float lxlo = xvside.Dot(eloc) - xvside0; 
			if ((i == acnxshi) || (lxlo < xlo)) 
			{
				xlo = lxlo; 
				cnxslo = i; 
			}
		}

		if ((cnxslo == acnxshi) || (cnxshi == acnxslo)) 
			return false; 
		return true; 
	}


	boolean FindSides(OneSection xsection0, boolean bTwist0, OneSection xsection1, boolean bTwist1) 
	{
		if (!FindSideExtents(xsection0, cnxs0lo, cnxs0hi, bTwist0)) 
			return false; 
		
		cnxs0left = cnxslo; 
		cnxs0right = cnxshi; 

		if (!FindSideExtents(xsection1, cnxs1lo, cnxs1hi, bTwist1)) 
			return false; 
		
		cnxs1left = cnxslo; 
		cnxs1right = cnxshi; 

		return true; 
	}
}; 


//
//
// Tube
//
//
class OneTube 
{
	static int MAX_TUBE_CORNERS = 4; 

	// reference to the base sections. 
	OneSection xsection0 = null; 
	OneSection xsection1 = null; 


	// derived values used for display of the sections (must have applied ReformRSpace).  
	boolean bTwist0; 
	boolean bTwist1; 
	int cnxs0[] = new int[MAX_TUBE_CORNERS]; 
	int cnxs1[] = new int[MAX_TUBE_CORNERS]; 
	int ntubecorners; 


	static float ecnxs0[] = new float[MAX_TUBE_CORNERS]; 
	static float ecnxs1[] = new float[MAX_TUBE_CORNERS]; 

	static Vec3 vaxis = new Vec3(); 
	static Vec3 vech0 = new Vec3(); 
	static Vec3 vech1 = new Vec3(); 
	static Vec3 vech2 = new Vec3(); 
	static Vec3 vechcom = new Vec3(); 

	static XElong xelong = new XElong(); 

	static Vec3 vind1 = new Vec3(); 
	static Vec3 vind2 = new Vec3(); 
	

	// work out a good correspondence between the curves.  
	void ReformTubespace()
	{
		// the centres of the sections must have been derived.  

		// decide twistedness 
		vaxis.Diff(xsection0.Encen, xsection1.Encen); 
		float dp0 = vaxis.Dot(xsection0.vcperp); 
		float dp1 = vaxis.Dot(xsection1.vcperp); 
		bTwist0 = (dp0 <= 0.0F); 
		bTwist1 = (dp1 <= 0.0F); 

		// need to find an vector along which both curves are elongated.  
		float vcdot = xsection0.vcperp.Dot(xsection1.vcperp); 
		if (vcdot > 0.0F)
			vech0.Sum(xsection1.vcperp, xsection0.vcperp); 
		else 
			vech0.Diff(xsection1.vcperp, xsection0.vcperp); 
		vech0.Norm(); 

		//vech0.SetXYZ(vaxis.x, vaxis.y, vaxis.z); 
		//vech0.Norm(); 

		// find a couple of perpendiculars 
		vech1.SetXYZ(vech0.y, -vech0.x, 0.0F); 
		if (vech1.Len() < 0.1F) 
			vech1.SetXYZ(vech0.z, 0.0F, -vech0.x); 
		vech1.Norm(); 
		vech2.Cross(vech0, vech1); 

		// now find a good pair of extents across a small range 
		xelong.ClearBothExtents(); 
		xelong.AdaptExtents(xsection0, xsection1, vech1);  
		xelong.AdaptExtents(xsection0, xsection1, vech2);  

		vechcom.Sum(vech1, vech2);  
		vechcom.Norm(); 
		xelong.AdaptExtents(xsection0, xsection1, vechcom);  
		vechcom.Diff(vech1, vech2); 
		vechcom.Norm(); 
		xelong.AdaptExtents(xsection0, xsection1, vechcom);  

		// give it two or four corners.  
		if (xelong.FindSides(xsection0, bTwist0, xsection1, bTwist1)) 
		{ 
			cnxs0[0] = xelong.cnxs0lo; 
			cnxs1[0] = xelong.cnxs1lo; 
			cnxs0[1] = xelong.cnxs0right; 
			cnxs1[1] = xelong.cnxs1right; 
			cnxs0[2] = xelong.cnxs0hi; 
			cnxs1[2] = xelong.cnxs1hi; 
			cnxs0[3] = xelong.cnxs0left; 
			cnxs1[3] = xelong.cnxs1left; 
			ntubecorners = 4; 
		}
		else
		{
			cnxs0[0] = xelong.cnxs0lo; 
			cnxs1[0] = xelong.cnxs1lo; 
			cnxs0[1] = xelong.cnxs0hi; 
			cnxs1[1] = xelong.cnxs1hi; 
			ntubecorners = 2; 
		}
	}




	// construction
	OneTube(OneSection lxsection0, OneSection lxsection1)
	{
		xsection0 = lxsection0; 
		xsection1 = lxsection1; 
	}


	// draw a straight tube both ends
	void paintW(Graphics g, int state)
	{
		g.setColor(state == 0 ? TN.wfmtubeInactive : (state == -1 ? TN.wfmtubeDel : TN.wfmtubeActive)); 
		for (int ic = 0; ic < ntubecorners; ic++)
		{
			int i = cnxs0[ic]; 
			int j = cnxs1[ic]; 
			Vec3 x0rtloci = xsection0.RTLoc[i];  
			Vec3 x1rtlocj = xsection1.RTLoc[j]; 

			g.drawLine((int)x0rtloci.x, (int)x0rtloci.y, (int)x1rtlocj.x, (int)x1rtlocj.y); 
		}
	}


	// draw a straight tube both ends
	static void paintStraightTube(Graphics g, OneSection lxsection0, OneSection lxsection1, int state)
	{
		g.setColor(state == 0 ? TN.wfmtubeInactive : (state == -1 ? TN.wfmtubeDel : TN.wfmtubeActive)); 

		for (int ic = 0; ic < 4; ic++)
		{
			int i = Math.min(lxsection0.nnodes * ic / 4, lxsection0.nnodes - 1); 
			int j = Math.min(lxsection1.nnodes * ic / 4, lxsection1.nnodes - 1); 

			Vec3 x0rtloci = lxsection0.RTLoc[i]; 
			Vec3 x1rtlocj = lxsection1.RTLoc[j]; 

			g.drawLine((int)x0rtloci.x, (int)x0rtloci.y, (int)x1rtlocj.x, (int)x1rtlocj.y); 
		}
	}


	// draw the cone part when dragging out a tube
	static void paintCone(Graphics g, OneSection xsection, int x, int y)
	{
		g.setColor(TN.wfmpointActive); 

		for (int ic = 0; ic < 4; ic++)
		{
			int i = Math.min(xsection.nnodes * ic / 4, xsection.nnodes - 1); 
			Vec3 rtloc = xsection.RTLoc[i]; 

			g.drawLine((int)rtloc.x, (int)rtloc.y, x, y); 
		}
	}


	/////////////////////////////////////////////
	float lamalong = 0.5F; // local var used to find the initial position of new cross section.  

	/////////////////////////////////////////////
	float sqDist(int mx, int my)
	{
		// measures against the edges
		float rdistsq = -1; 

		for (int ic = 0; ic < ntubecorners; ic++)
		{
			int i = cnxs0[ic]; 
			int j = cnxs1[ic]; 

			Vec3 x0rtloci = xsection0.RTLoc[i]; 
			Vec3 x1rtlocj = xsection1.RTLoc[j]; 

			float vx = x1rtlocj.x - x0rtloci.x;
			float vy = x1rtlocj.y - x0rtloci.y;

			float dx = mx - x0rtloci.x;
			float dy = my - x0rtloci.y;

			float lambda = (dx * vx + dy * vy) / (vx * vx + vy * vy);
			if ((lambda > 0.0F) && (lambda < 1.0F)) 
			{
				float rx = lambda * vx - dx;
				float ry = lambda * vy - dy;

				float idistsq = rx * rx + ry * ry;
				if ((rdistsq == -1) || (idistsq < rdistsq)) 
				{
					rdistsq = idistsq;
					lamalong = lambda; 
				}
			}
		}

		return rdistsq; 
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, List<OneSection> vsections) throws IOException
	{
		int ind0 = vsections.indexOf(xsection0); 
		int ind1 = vsections.indexOf(xsection1); 
		los.WriteLine(TNXML.xcom(0, TNXML.sLINEAR_TUBE, TNXML.sFROM_XSECTION, String.valueOf(ind0), TNXML.sTO_XSECTION, String.valueOf(ind1)));  
	}
}

