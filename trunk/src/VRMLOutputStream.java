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

import java.util.List;
import java.io.IOException;

//
//
// VRMLOutputStream
//
//
class VRMLOutputStream extends LineOutputStream
{
	boolean bSwapYZ; 

	/////////////////////////////////////////////
	VRMLOutputStream(FileAbstraction savefile, boolean lbSwapYZ) throws IOException
	{
		super(savefile); 
		bSwapYZ = lbSwapYZ; 
		WriteLine("#VRML V2.0 utf8"); 
	}

	Vec3 vt01 = new Vec3(); 
	Vec3 vt02 = new Vec3(); 
	/////////////////////////////////////////////
	void WriteTriangle(int t0, int t1, int t2) throws IOException
	{
		WriteLine(String.valueOf(t1) + ", " + String.valueOf(t0) + ", " + String.valueOf(t2) + ", -1,"); 
		//WriteLine(String.valueOf(t0) + ", " + String.valueOf(t1) + ", " + String.valueOf(t2) + ", -1,"); 
	}
	
	/////////////////////////////////////////////
	void WriteVector(Vec3 vs) throws IOException
	{
		if (bSwapYZ) 
			WriteLine(String.valueOf(vs.x) + " " + String.valueOf(vs.z) + " " + String.valueOf(-vs.y) + ","); 
		else
			WriteLine(String.valueOf(vs.x) + " " + String.valueOf(vs.y) + " " + String.valueOf(vs.z) + ","); 
	}

	/////////////////////////////////////////////
	static Vec3 vfdiff = new Vec3(); 

	/////////////////////////////////////////////
	static void Accum(float[] vf, OneSection xsection, int i, int in, boolean bTwist)
	{
		int nvf = 1; 
		vf[0] = 0.0F; 
		while (i != in)
		{
			int ip1 = (i + (bTwist ? -1 : 1) + xsection.nnodes) % xsection.nnodes; 
			vfdiff.Diff(xsection.ELoc[i], xsection.ELoc[ip1]); 
			vf[nvf] = vf[nvf - 1] + vfdiff.Len(); 
			nvf++; 
			i = ip1; 
		}

		// normalize
		if (vf[nvf - 1] == 0.0F)
			vf[nvf - 1] = 1.0F; 
		for (int j = 0; j < nvf; j++)
			vf[j] /= vf[nvf - 1]; 

		vf[nvf - 1] = 1.0F; 
	}


	static float[] vf0 = new float[100]; 
	static float[] vf1 = new float[100]; 

	/////////////////////////////////////////////
	void WriteTubes(List<OneSection> vsections, List<OneTube> vtubes, float creaseAngle) throws IOException
	{
		WriteLine("Shape"); 
		WriteLine("{"); 
		WriteLine("geometry IndexedFaceSet {"); 

		// write out the coordinates
		WriteLine("coord Coordinate {"); 
		WriteLine("point ["); 
		
		int VAindex = 0; 
		for (int i = 0; i < vsections.size(); i++) 
		{
			OneSection xsection = ((OneSection)(vsections.get(i))); 
			xsection.VAindex = VAindex; 
			for (int j = 0; j < xsection.nnodes; j++)  
				WriteVector(xsection.ELoc[j]); 
			VAindex += xsection.nnodes; 
		}

		WriteLine("]"); 
		WriteLine("}"); 

		WriteLine("coordIndex ["); 
		for (int it = 0; it < vtubes.size(); it++)
		{
			OneTube tube = ((OneTube)(vtubes.get(it))); 

			// replace with new tube if necessary 
			if ((tube.xsection0.xsectionE != null) || (tube.xsection1.xsectionE != null)) 
			{
				tube = new OneTube((tube.xsection0.xsectionE != null ? tube.xsection0.xsectionE : tube.xsection0), (tube.xsection1.xsectionE != null ? tube.xsection1.xsectionE : tube.xsection1)); 
				tube.ReformTubespace(); 
				TN.emitMessage("Making tube to equated xc"); 
			}

			// make the interpolation.  
			for (int ic = 0; ic < tube.ntubecorners; ic++)
			{
				int i = tube.cnxs0[ic]; 
				int j = tube.cnxs1[ic]; 
				int in = tube.cnxs0[(ic + 1) % tube.ntubecorners]; 
				int jn = tube.cnxs1[(ic + 1) % tube.ntubecorners]; 


				Accum(vf0, tube.xsection0, i, in, tube.bTwist0); 
				Accum(vf1, tube.xsection1, j, jn, tube.bTwist1); 

				int ivf0 = 0; 
				int ivf1 = 0; 
				while ((vf0[ivf0] != 1.0F) || (vf1[ivf1] != 1.0F))
				{
					// find which to step forward first 
					if ((vf0[ivf0] != vf1[ivf1]) ? (vf0[ivf0] < vf1[ivf1]) : (vf0[ivf0 + 1] < vf1[ivf1 + 1]))
					{
						int ip = (i + (tube.bTwist0 ? -1 : 1) + tube.xsection0.nnodes) % tube.xsection0.nnodes; 
						WriteTriangle(tube.xsection0.VAindex + i, tube.xsection0.VAindex + ip, tube.xsection1.VAindex + j); 
						i = ip; 
						ivf0++; 
					}

					else
					{
						int jp = (j + (tube.bTwist1 ? -1 : 1) + tube.xsection1.nnodes) % tube.xsection1.nnodes; 
						WriteTriangle(tube.xsection0.VAindex + i, tube.xsection1.VAindex + jp, tube.xsection1.VAindex + j); 
						j = jp; 
						ivf1++; 
					}
				}
			}
		}

		WriteLine("]"); 

		WriteLine("creaseAngle " + String.valueOf(creaseAngle)); 
		WriteLine("solid TRUE"); 

		WriteLine("}"); 

		WriteLine("appearance Appearance {"); 
        WriteLine("material Material {"); 
		WriteLine("emissiveColor 0.25 0 0"); 
		WriteLine("}"); 
		WriteLine("}"); 


		WriteLine("}"); 
	}


	/////////////////////////////////////////////
	void WriteCentreline(List<OneStation> vstations, List<OneLeg> vlegs) throws IOException
	{
		WriteLine("Shape"); 
		WriteLine("{"); 
		WriteLine("geometry IndexedLineSet {"); 

		// write out the coordinates
		WriteLine("coord Coordinate {"); 
		WriteLine("point ["); 
		
		for (int i = 0; i < vstations.size(); i++) 
		{
			OneStation os = vstations.get(i); 
			os.vsig = i; 
			WriteVector(os.Loc); 
		}

		WriteLine("]"); 
		WriteLine("}"); 

		WriteLine("coordIndex ["); 
		for (int il = 0; il < vlegs.size(); il++)
		{
			OneLeg olb = vlegs.get(il); 
			if ((olb.osfrom != null) && (olb.osto != null)) 
				WriteLine(olb.osfrom.vsig + ", " + olb.osto.vsig + ", -1,"); 
		}

		WriteLine("]"); 

		WriteLine("}"); 

		WriteLine("appearance Appearance {"); 
        WriteLine("material Material {"); 
		WriteLine("emissiveColor 0 1 1"); 
		WriteLine("}"); 
		WriteLine("}"); 


		WriteLine("}"); 
	}
}

