/////////////////////////////////////////////////////////////////////////////////
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
import java.io.IOException; 
import java.util.Vector; 


// check the stemmings on the selection of bitmaps 
// (and the default put into the fontcolours)

// all y-coordinates must be negated.  

////////////////////////////////////////////////////////////////////////////////
class TherionLoader
{
	SketchGraphics sketchgraphicspanel; 
	Vector pnodes = new Vector(); 

	// station nodes and their destinations
	Vector vostations = new Vector(); 
	Vector vdstations = new Vector(); 

	/////////////////////////////////////////////
	OnePathNode FindPathNode(float x, float y)
	{
		for (int i = 0; i < pnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)pnodes.elementAt(i); 
			if ((x == opn.pn.getX()) && (y == opn.pn.getY()))
				return opn; 
		}
		OnePathNode res = new OnePathNode(x, y, 0.0F); 
		pnodes.addElement(res); 
		return res; 
	}
	
	
	/////////////////////////////////////////////
	void MakeThLine(LineInputStream los, String type)
	{
		OnePath op = null; 
		float lx = 0.0F; 
		float ly = 0.0F; 
		int inodes = 0; 
		int inodesunsmoothed = 0; 
		while (los.FetchNextLine())
		{
			if (los.w[0].equals("endline"))
				break; 
			if (los.w[0].equals("smooth"))
			{
				if (los.w[1].equals("off"))
					inodesunsmoothed++; 
				continue; 
			}
			int ip = (los.w[4].equals("") ? 0 : 4); 
			lx = Float.parseFloat(los.w[ip]); 
			ly = -Float.parseFloat(los.w[ip + 1]); 
			if (op == null)
			{
				OnePathNode opn = FindPathNode(lx, ly); 
				op = new OnePath(opn); 
				if (type.equals("wall"))
					op.linestyle = SketchLineStyle.SLS_WALL;
				else if (type.equals("pit"))
					op.linestyle = SketchLineStyle.SLS_PITCHBOUND;
				else if (type.equals("rock-border"))
					op.linestyle = SketchLineStyle.SLS_DETAIL;
				else if (type.equals("rock-edge"))
					op.linestyle = SketchLineStyle.SLS_DETAIL;
				else if (type.equals("label"))
					op.linestyle = SketchLineStyle.SLS_CONNECTIVE;
				else if (type.equals("flowstone"))
					op.linestyle = SketchLineStyle.SLS_CONNECTIVE;
				else
					TN.emitWarning("Don't know linestyle: " + type); 
			}
			else
				op.LineTo(lx, ly); 
			inodes++; 
		}
		if (op != null)
		{
			op.EndPath(FindPathNode(lx, ly)); 
			op.bWantSplined = (inodesunsmoothed <= inodes / 2); 
			sketchgraphicspanel.AddPath(op);
		}
	}
	
	/////////////////////////////////////////////
	boolean MatchSNames(String s1, String s2)
	{
		// do we have other delimeters to see here?  
		// we'll have to account for the dot delimeters and the fact that 
		// the endingwith positions should correspond to them properly.  

		int s1l = s1.length(); 
		int s2l = s2.length(); 
		if (s1l == s2l)
			return s1.equals(s2); 
		if (s1l < s2l)
			return s2.charAt(s2l - s1l - 1) == '.' && s2.endsWith(s1); 
		return s1.charAt(s1l - s2l - 1) == '.' && s1.endsWith(s2); 
	}

	/////////////////////////////////////////////
	void MakeThStation(LineInputStream los, String sname, String sx, String sy)
	{
		//point 564.5 -1292.5 station -name darkside.3
		float x = Float.parseFloat(sx); 
		float y = -Float.parseFloat(sy); 
		// find a station that matches this name
		int ifoundstation = -1; 
		for (int i = 0; i < vostations.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vostations.elementAt(i); 
			if (MatchSNames(sname, opn.pnstationlabel))
			{
				assert ifoundstation == -1; // shouldn't get more than one match
				ifoundstation = i; 
			}
		}
		assert ifoundstation != -1; // should get a match
		assert vdstations.elementAt(ifoundstation) == null; 
		vdstations.setElementAt(new OnePathNode(x, y, 0.0F), ifoundstation); 
	}

	/////////////////////////////////////////////
	void MakeThPoint(LineInputStream los, String sname, String sx, String sy)
	{

	}


// a similar function to paintSelectedSketches could be good for reviewing the state of 
// the centreline after import

	/////////////////////////////////////////////
	void CollectStations(OneSketch sketch)
	{
		for (int i = 0; i < sketch.vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)sketchgraphicspanel.tsketch.vnodes.elementAt(i); 
			if (opn.IsCentrelineNode())
			{
				vostations.addElement(opn); 
				vdstations.addElement(null); 
			}
		}
	}



	/////////////////////////////////////////////
	OnePathNode MakeInterpPosition(OnePathNode dporg)
	{ 
TN.emitMessage("interpolating station: " + dporg.pnstationlabel); 
		float totalweight = 0.0F;
		float xsum = 0.0F;
		float ysum = 0.0F;

// We need to use the Proximity Structure thing to handle this.

// find lists of pairs of lines
// minimally connective
// (either from the legs, or generated by nearness if there are not enough of them).  

/*		
		for (int i = 0; i < vostations.size(); i++)
		{
			OnePathNode dporg = (OnePathNode)vostations.elementAt(i); 
			OnePathNode dpdest = (OnePathNode)vdstations.elementAt(i); 
			if (dpdest == null)
				dpdest = MakeInterpPosition(dporg); 
			sketchgraphicspanel.FuseNodes(dporg, dpdest, false); 
		}
*/
		return new OnePathNode(1000.0F, 1000.0F, 0.0F); 
	}
	
	/////////////////////////////////////////////
	void MoveStations()
	{
		for (int i = 0; i < vostations.size(); i++)
		{
			OnePathNode dporg = (OnePathNode)vostations.elementAt(i); 
			OnePathNode dpdest = (OnePathNode)vdstations.elementAt(i); 
			if (dpdest == null)
				dpdest = MakeInterpPosition(dporg); 
			sketchgraphicspanel.FuseNodes(dporg, dpdest, false); 
		}
	}


	/////////////////////////////////////////////
	void ReadAll(LineInputStream los)
	{
		while (los.FetchNextLine())
		{
			if (los.w[0].equals("line"))
				MakeThLine(los, los.w[1]); 
			else if (los.w[0].equals("point"))
			{
				if (los.w[3].equals("station"))
				{
					//point 564.5 -1292.5 station -name darkside.3
					assert los.w[4].equals("-name"); 
					MakeThStation(los, los.w[5], los.w[1], los.w[2]); 
				}
				else
					MakeThPoint(los, los.w[3], los.w[1], los.w[2]); 
			}									  

			//##XTHERION## xth_me_image_insert {0 1 1.0} {0 {}} scans/darkside-stiched.png 0 {}
		}
	} 


	/////////////////////////////////////////////
	public TherionLoader(SketchGraphics lsketchgraphicspanel, FileAbstraction th2file) 
	{
		sketchgraphicspanel = lsketchgraphicspanel; 
		CollectStations(sketchgraphicspanel.tsketch); 
		LineInputStream los = null; 
		try { los = new LineInputStream(th2file, "", ""); }
		catch (IOException e) { TN.emitWarning(e.toString()); }
		ReadAll(los); 
		MoveStations();
	}

}
