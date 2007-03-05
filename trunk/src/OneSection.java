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


//
//
// OneSection
//
//

/////////////////////////////////////////////
class OneSection 
{
	// base station and orientations 
	String station0S; 
	String station1S; 
	float lambda; // the along position between the two stations.  

	String orientstationforeS; 
	String orientstationbackS; 

	// exported name name pairs
	OneTunnel station0ot; 
	String station0EXS; 
	OneTunnel station1ot; 
	String station1EXS; 

	OneTunnel stationforeot; 
	String stationforeEXS; 

	OneTunnel stationbackot; 
	String stationbackEXS; 

	// reference to the base station when we have found it.  
	OneStation station0 = null; 
	OneStation station1 = null; // and take the lambda from above (if it is not a string).  
	OneStation stationfore = null; 
	OneStation stationback = null; 

	Vec3 Loc = new Vec3(); // location of base point.  

	// the orientation of the xsection in string form
	String relorientcompassS; 
	String orientclinoS; 

	// the orientation of the xsection in decimal form 
	float orientcompass = 0.0F;
	float orientclino = 0.0F;

	// the basic polygon
	Vec3 nodes[] = null;	
	Vec3 ncen = new Vec3();	// centre of this set of nodes.  

	// the full matrix that takes (xc, yc, zc) to (ELocX, ELocY, ELocZ)
	Matrix3D rspace = new Matrix3D(); 
	Vec3 vcperp = new Vec3(); 

	// this code used for displaying the XSections
	OneSection xsectionE;	// the xsection this is equated to (normally itself).  

	// the vector in space
	Vec3 ELoc[] = null;	
	Vec3 Encen = new Vec3(); 

	// transformed to screen coordinates.  
	Vec3 RTLoc[] = null; // of type Vec3. 
	Vec3 RTvcperp = new Vec3(); 

	int VAindex; // used by the VRML writer.  


	// number of nodes (controls the arrays of nodes, Eloc and RTLoc.  
	int nnodes = 0; 

	/////////////////////////////////////////////
	void AddNode(Vec3 pt) 
	{
		// extend the array if necessary.  
		if ((nodes == null) || (nnodes >= nodes.length)) 
		{
			Vec3[] newnodes = new Vec3[(nodes != null ? nnodes * 2 : 4)]; 

			// extend the Eloc and RTLoc arrays (don't care about values, but copying them saves some newing).  
			Vec3[] newELoc = new Vec3[newnodes.length]; 
			Vec3[] newRTLoc = new Vec3[newnodes.length]; 

			for (int i = 0; i < nnodes; i++) 
			{
				newnodes[i] = nodes[i]; 
				newELoc[i] = ELoc[i]; 
				newRTLoc[i] = RTLoc[i]; 
			} 
			
			nodes = newnodes; 
			ELoc = newELoc; 
			RTLoc = newRTLoc; 
		}

		// push back the arrays 
		nodes[nnodes] = pt; 
		if (ELoc[nnodes] == null) 
		{
			ELoc[nnodes] = new Vec3(); 
			RTLoc[nnodes] = new Vec3(); 
		}
		nnodes++; 
	}


	/////////////////////////////////////////////
	void WriteSection(LineOutputStream los) throws IOException
	{
		los.WriteLine("*XSection " + " { "); 
		los.WriteLine(station0S + ", " + String.valueOf(1.0F - lambda) + ", " + station1S + ", " + String.valueOf(lambda));  
		los.WriteLine(orientstationforeS + ", " + orientstationbackS + ", " + relorientcompassS + ", " + orientclinoS);

		los.WriteLine("No_nodes " + String.valueOf(nnodes)); 
		for (int i = 0; i < nnodes; i++)
			los.WriteLine((nodes[i]).toString()); 
		los.WriteLine("}"); 
	}

	/////////////////////////////////////////////
	// read section
	OneSection(String lstation0S, String lstation1S, float llambda, String lorientstationforeS, String lorientstationbackS, String lrelorientcompassS, String lorientclinoS)  
	{
		station0S = lstation0S; 
		station1S = lstation1S; 
		lambda = llambda; 

		orientstationforeS = lorientstationforeS; 
		orientstationbackS = lorientstationbackS; 
		relorientcompassS = lrelorientcompassS; 
		orientclinoS = lorientclinoS; 
	}


	/////////////////////////////////////////////
	public boolean ReformRspace()
	{
		if ((station0 == null) || (station1 == null)) 
			return false; 

		// find the location 
		Loc.SetAlong(lambda, station0.Loc, station1.Loc); 

		// find stations in the directions 
		orientcompass = Float.valueOf(relorientcompassS).floatValue();
		if ((stationfore != null) || (stationback != null)) 
		{
			float compass1 = 0.0F; 
			float compass2 = 0.0F; 
			if (stationfore != null) 
				compass1 = Vec3.DegArg(stationfore.Loc.y - Loc.y, stationfore.Loc.x - Loc.x); 
			if (stationback != null) 
				compass2 = Vec3.DegArg(Loc.y - stationback.Loc.y, Loc.x - stationback.Loc.x); 
			else
				compass2 = compass1; 
			if (stationfore == null) 
				compass1 = compass2; 

			orientcompass += (compass1 + compass2 + (Math.abs(compass1 - compass2) > 180.0F ? 360.0F : 0.0F)) / 2; 
		}
		if (orientclinoS.equalsIgnoreCase("up"))
			orientclino = 90.0F; 
		else if (orientclinoS.equalsIgnoreCase("down"))
			orientclino = -90.0F; 
		else if (orientclinoS.equalsIgnoreCase("-"))
			orientclino = 0.0F; 
		else
			orientclino = (Float.valueOf(orientclinoS).floatValue()); 

		// find the central position 
		ncen.SetXYZ(0.0F, 0.0F, 0.0F); 
		for (int i = 0; i < nnodes; i++)
		{
			Vec3 node = nodes[i]; 
			ncen.x += node.x; 
			ncen.y += node.y; 
			ncen.z += node.z; 
		}
		ncen.x /= (float)nnodes; 
		ncen.y /= (float)nnodes; 
		ncen.z /= (float)nnodes; 


		// set up the matrix
		rspace.unit(); 
		rspace.xrotdeg(orientclino - 90.0); 
		rspace.zrotdeg(-orientcompass); 
		rspace.translate(Loc.x, Loc.y, Loc.z); 

		vcperp.x = rspace.xz; 
		vcperp.y = rspace.yz; 
		vcperp.z = rspace.zz;

		// prepare corner indexes array
		int nci = 0; 
		float lwallseg = 0.0F; 

		for (int i = 0; i < nnodes; i++)
		{
			Vec3 node = nodes[i]; 
			Vec3 eloc = ELoc[i]; 
			eloc.x = node.x * rspace.xx + node.y * rspace.xy + node.z * rspace.xz + rspace.xo;
			eloc.y = node.x * rspace.yx + node.y * rspace.yy + node.z * rspace.yz + rspace.yo;
			eloc.z = node.x * rspace.zx + node.y * rspace.zy + node.z * rspace.zz + rspace.zo;
		}

		// transform the node centre.  
		Encen.x = ncen.x * rspace.xx + ncen.y * rspace.xy + ncen.z * rspace.xz + rspace.xo;
		Encen.y = ncen.x * rspace.yx + ncen.y * rspace.yy + ncen.z * rspace.yz + rspace.yo;
		Encen.z = ncen.x * rspace.zx + ncen.y * rspace.zy + ncen.z * rspace.zz + rspace.zo;

		return true; 
	}

	/////////////////////////////////////////////
	// transformed for viewing points
	void SetTLoc(Matrix3D mat)
	{
		for (int i = 0; i < nnodes; i++)
		{
			Vec3 eloc = ELoc[i]; 
			Vec3 rtloc = RTLoc[i]; 

			rtloc.x = (eloc.x * mat.xx + eloc.y * mat.xy + eloc.z * mat.xz + mat.xo);
			rtloc.y = (eloc.x * mat.yx + eloc.y * mat.yy + eloc.z * mat.yz + mat.yo);
			rtloc.z = (eloc.x * mat.zx + eloc.y * mat.zy + eloc.z * mat.zz + mat.zo); 
		}
		RTvcperp.x = (vcperp.x * mat.xx + vcperp.y * mat.xy + vcperp.z * mat.xz); 
		RTvcperp.y = (vcperp.x * mat.yx + vcperp.y * mat.yy + vcperp.z * mat.yz); 
		RTvcperp.z = (vcperp.x * mat.zx + vcperp.y * mat.zy + vcperp.z * mat.zz); 
	}

	/////////////////////////////////////////////
	float sqDist(int mx, int my)
	{
		// should do for edges as well as nodes
		float res = -1; 
		for (int i = 0; i < nnodes; i++)
		{
			Vec3 rtloci = RTLoc[i]; 

			float dx = mx - rtloci.x;
			float dy = my - rtloci.y;
			float sqd = dx * dx + dy * dy; 

			if ((i == 0) || (sqd < res))
				res = sqd; 
			
			int ip = (i == 0 ? nnodes - 1 : i - 1); 

			Vec3 rtlocip = RTLoc[ip]; 
			float vx = rtlocip.x - rtloci.x;
			float vy = rtlocip.y - rtloci.y;

			float lambda = (dx * vx + dy * vy) / (vx * vx + vy * vy);
			if ((lambda > 0.0F) && (lambda < 1.0F))  
			{
				float rx = lambda * vx - dx;
				float ry = lambda * vy - dy;
				float rsq = rx * rx + ry * ry; 
				if (rsq < res) 
					res = rsq; 
			}
		}

		return res; 
	}

	/////////////////////////////////////////////
	void paintW(Graphics g, boolean bActive)
	{
		g.setColor(bActive ? TN.wfmxsectionActive : TN.wfmxsectionInactive); 
		int ip = nnodes - 1; 
		for (int i = 0; i < nnodes; i++)
		{
			Vec3 rtloci = RTLoc[i]; 
			Vec3 rtlocip = RTLoc[ip]; 

			g.drawLine((int)rtlocip.x, (int)rtlocip.y, (int)rtloci.x, (int)rtloci.y); 
			ip = i; 
		}
	}


	/////////////////////////////////////////////
	// builds the default xsection on a station
	OneSection(String lname, OneStation os, float dL, float dR, float dU, float dD)
	{
		station0S = lname; 
		station1S = lname; 
		station0 = os; 
		station1 = os; 

		orientstationbackS = ""; 
		orientstationforeS = ""; 

		// default polygon
		AddNode(new Vec3(dR, dU, 0.0F)); 
		AddNode(new Vec3(dR, -dD, 0.0F)); 
		AddNode(new Vec3(-dL, -dD, 0.0F)); 
		AddNode(new Vec3(-dL, dU, 0.0F)); 
	}


	/////////////////////////////////////////////
	// builds the default xsection on a station
	OneSection(PossibleXSection pxs)
	{
		station0S = pxs.basestationS; 
		station1S = pxs.basestationS; 
		station0 = null; 
		station1 = null; 

		orientstationbackS = ""; 
		orientstationforeS = ""; 

		// default polygon
		AddNode(new Vec3(pxs.R, pxs.U, 0.0F)); 
		AddNode(new Vec3(pxs.R, -pxs.D, 0.0F)); 
		AddNode(new Vec3(-pxs.L, -pxs.D, 0.0F)); 
		AddNode(new Vec3(-pxs.L, pxs.U, 0.0F)); 

		relorientcompassS = "++++"; 
		orientclinoS = pxs.orientclinoS; 
	}




	/////////////////////////////////////////////
	// builds the default type (should do a halfway thingy).  
	OneSection(OneTube tube, float lamalong) 
	{
		// work out a plausible pair and lambda to work from 
		station0 = tube.xsection0.station0; 
		if (tube.xsection1.station1 == station0) 
		{
			if (tube.xsection0.station1 == station0) 
			{
				if (tube.xsection1.station0 == station0) 
				{
					station0 = null; 
					return; 
				}
				station1 = tube.xsection1.station0; 
			}
			else
				station1 = tube.xsection0.station1; 
		}
		else
			station1 = tube.xsection1.station1; 

		station0S = station0.name; 
		station1S = station1.name; 

		// now the lambdas from either end and their relativeness. 
		float lam0 = tube.xsection0.lambda; 
		float lam1 = (tube.xsection1.station0 == station1 ? 1.0F - tube.xsection1.lambda : tube.xsection1.lambda); 
		lambda = lam0 * (1.0F - lamalong) + lam1 * lamalong; 

		orientstationbackS = ""; 
		orientstationforeS = ""; 

		// borrow arrays and vector 
		float[] vf0 = VRMLOutputStream.vf0; 
		float[] vf1 = VRMLOutputStream.vf1; 
		Vec3 vfdiff = VRMLOutputStream.vfdiff; 

		// make up a correspondence out of the VRML code.  
		for (int ic = 0; ic < tube.ntubecorners; ic++)
		{
			int i = tube.cnxs0[ic]; 
			int j = tube.cnxs1[ic]; 
			int in = tube.cnxs0[(ic + 1) % tube.ntubecorners]; 
			int jn = tube.cnxs1[(ic + 1) % tube.ntubecorners]; 

			VRMLOutputStream.Accum(vf0, tube.xsection0, i, in, tube.bTwist0); 
			VRMLOutputStream.Accum(vf1, tube.xsection1, j, jn, tube.bTwist1); 

			// put in points 
			int ivf0 = 0; 
			int ivf1 = 0; 
			while ((vf0[ivf0] != 1.0F) || (vf1[ivf1] != 1.0F))
			{
				// find which to step forward first 
				if ((vf0[ivf0] != vf1[ivf1]) ? (vf0[ivf0] < vf1[ivf1]) : (vf0[ivf0 + 1] < vf1[ivf1 + 1]))
				{
					int ip = (i + (tube.bTwist0 ? -1 : 1) + tube.xsection0.nnodes) % tube.xsection0.nnodes; 
					if (lamalong < 0.5F) 
					{
						float plam = (vf1[ivf1] - vf0[ivf0]) / (vf0[ivf0 + 1] - vf0[ivf0]); 
						if ((plam >= 0.0F) && (plam <= 1.0F)) 
						{
							vfdiff.SetAlong(plam, tube.xsection0.nodes[i], tube.xsection0.nodes[ip]); 
							Vec3 nnd = new Vec3(); 
							nnd.SetAlong(lamalong, vfdiff, tube.xsection1.nodes[j]); 
							AddNode(nnd); 
						}
					}

					i = ip; 
					ivf0++; 
				}

				else
				{
					int jp = (j + (tube.bTwist1 ? -1 : 1) + tube.xsection1.nnodes) % tube.xsection1.nnodes; 
					if (!(lamalong < 0.5F)) 
					{
						float plam = (vf0[ivf0] - vf1[ivf1]) / (vf1[ivf1 + 1] - vf1[ivf1]); 
						if ((plam >= 0.0F) && (plam <= 1.0F)) 
						{
							vfdiff.SetAlong(plam, tube.xsection1.nodes[j], tube.xsection1.nodes[jp]); 
							Vec3 nnd = new Vec3(); 
							nnd.SetAlong(lamalong, tube.xsection0.nodes[i], vfdiff); 
							AddNode(nnd); 
						}
					}

					j = jp; 
					ivf1++; 
				}
			}
		}
	}


	/////////////////////////////////////////////
	void SetDefaultOrientation(List<OneLeg> vlegs)
	{
		// find the other end of the vlegs
		if (station0S.equals(station1S))
		{
			for (OneLeg ol : vlegs) 
			{
				String ext = (station0S.equals(ol.stto) ? ol.stfrom : (station0S.equals(ol.stfrom) ? ol.stto : null));  
				if (ext != null) 
				{
					if (!orientstationbackS.equals("")) 
					{
						orientstationforeS = ext; 
						break; 
					}
					else
						orientstationbackS = ext; 
				}
			}
		}
		else
			orientstationforeS =  (lambda != 1.0F ? station1S : station0S); 

		relorientcompassS = "0.0"; 
		orientclinoS = "0.0"; 
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	void LoadIntoGraphics(ShapeGraphics shp)
	{
		shp.vsgp.clear(); 
		shp.vsgl.clear(); 

		
		ShapeGraphicsLine sgl = new ShapeGraphicsLine(); 
		shp.vsgl.add(sgl); 
		for (int i = 0; i < nnodes; i++)
		{
			Vec3 node = nodes[i]; 
			ShapeGraphicsPoint sgp = new ShapeGraphicsPoint(); 
			sgp.SetXYZ(node.x, node.y, node.z); 
			shp.vsgp.add(sgp); 
			sgl.sgp2 = sgp; 
			sgp.sgl1 = sgl; 

			if (i < nnodes - 1)
			{
				sgl = new ShapeGraphicsLine(); 
				shp.vsgl.add(sgl); 
			}
			else
				sgl = (ShapeGraphicsLine)(shp.vsgl.get(0)); 

			sgp.sgl2 = sgl; 
			sgl.sgp1 = sgp; 
		}
	}

	
	/////////////////////////////////////////////
	// a section in the preview display 
	OneSection(ShapeGraphics shp)
	{
		LoadFromShapeGraphics(shp); 
	}

	/////////////////////////////////////////////
	void LoadFromShapeGraphics(ShapeGraphics shp)
	{
		// first find top point.  
		ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(shp.vsgp.get(0));  
		ShapeGraphicsPoint sgpTop = sgp; 
		do 
		{
			if (sgp.y < sgpTop.y) 
				sgpTop = sgp; 
			sgp = sgp.sgl2.sgp2; 
		}
		while (sgp != shp.vsgp.get(0)); 

		// now work out its orientation.  
		boolean bClockwise; 
		{
			ShapeGraphicsPoint sgpTop2 = sgpTop.sgl2.sgp2; 
			ShapeGraphicsPoint sgpTop1 = sgpTop.sgl1.sgp1; 
			float slop1 = (sgpTop1.x - sgpTop.x) / Math.max(sgpTop1.y - sgpTop.y, 0.00001F); 
			float slop2 = (sgpTop2.x - sgpTop.x) / Math.max(sgpTop2.y - sgpTop.y, 0.00001F); 
			bClockwise = (slop1 > slop2); 
			if (!bClockwise) 
				TN.emitMessage("Dereflecting XSection!!!"); 
		}

		// track around and put in the nodes
		nnodes = 0;  
		sgp = sgpTop;  
		do 
		{
			AddNode(new Vec3(sgp.x, sgp.y, sgp.z)); 
			sgp = (bClockwise ? sgp.sgl2.sgp2 : sgp.sgl1.sgp1); 
		}
		while (sgp != sgpTop); 
	}



	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, int ind) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sXS_STATION0, station0S, TNXML.sXS_STATION1, station1S, TNXML.sXS_STATION_LAM, String.valueOf(lambda))); 
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sXS_STATION_ORIENT_FORE, orientstationforeS, TNXML.sXS_STATION_ORIENT_BACK, orientstationbackS, TNXML.sXS_STATION_ORIENT_REL_COMPASS, relorientcompassS, TNXML.sXS_STATION_ORIENT_CLINO, orientclinoS)); 

		los.WriteLine(TNXML.xcomopen(1, TNXML.sXSECTION, TNXML.sXSECTION_INDEX, String.valueOf(ind))); 

		for (int i = 0; i < nnodes; i++)
			los.WriteLine(TNXML.xcom(2, TNXML.sPOINT, TNXML.sPTX, String.valueOf(nodes[i].x), TNXML.sPTY, String.valueOf(nodes[i].y), TNXML.sPTZ, String.valueOf(nodes[i].z))); 

		los.WriteLine(TNXML.xcomclose(1, TNXML.sXSECTION)); 

		los.WriteLine(TNXML.xcomclose(0, TNXML.sSET)); 
		los.WriteLine(TNXML.xcomclose(0, TNXML.sSET)); 
	}
}

