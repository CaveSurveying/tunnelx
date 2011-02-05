////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2006  Martin Green, Julian Todd
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
import java.lang.String;
import java.awt.geom.PathIterator;

class SVGPaths
{
    private float tunnelunit = 0.1F; //length of tunnel unit in meters
	private float xoffset = 0F;
	private float yoffset = 0F;
	private int id = 0; //The next id to use
	public SVGPaths(LineOutputStream los, List<OnePath> vpaths) throws IOException
    {
		WriteHeader(los);
		for (OnePath op : vpaths)
			WritePath(los, op);
		WriteFooter(los);
	 }

	// open and close
	void WriteHeader(LineOutputStream los) throws IOException
	{
		TNXML.chconvleng = TNXML.chconvlengWSP; // a complete hack to stop &space; getting in here

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		los.WriteLine(TNXML.xcomopen(0, "svg", "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Tunnels Paths"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "This file solely contains the definitions of paths for Tunnel, you need a view.svg file to see anything."));
		los.WriteLine(TNXML.xcomopen(1, "defs"));
	}

	void WriteFooter(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomclose(1, "defs"));
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconvlengWSP;
	}

	static float[] coords = new float[6]; //Used to get the position of line segments
	void WritePath(LineOutputStream los, OnePath op) throws IOException
	{
		//Set svg id to path
		String sid = new String(String.valueOf(this.id));
		id++;
		op.svgid = sid;
		//Generate list of linestyles and classes
		String classes = new String(SketchLineStyle.shortlinestylenames[op.linestyle]);
		for (int j = 0; j < op.vssubsets.size(); j++)
			classes = classes + " " + SketchLineStyle.shortlinestylenames[op.linestyle] + op.vssubsets.get(j);

        String d = op.svgdvalue(xoffset, yoffset); 

		//Set parameters and attributes based on if the heights are set
		int numparam=0;

		// It's poss that after UpdateZnodes all nodes are set
		//if (op.pnstart.bzaltset)
		//	numparam = 10;
		//else
			numparam = 6;//need to determine why bzaltset is not set on 'update node z'
			             //If you update z alt and change the 6 to 10 it does give all z heights
		String parameters[] = {"id", sid, "class", classes, "d", d, "z0", String.valueOf(op.pnstart.zalt), "z1", String.valueOf(op.pnend.zalt)};
		
		//Determine if the path has funny attributes
		if (op.plabedl!=null) 
		{
			los.WriteLine(TNXML.xcomopenN(2, "path", parameters, numparam));
			op.plabedl.WriteXML(los,3,false);
			los.WriteLine(TNXML.xcomclose(2, "path"));
		}
		else
		{
			los.WriteLine(TNXML.xcomN(2, "path", parameters, numparam));
		}
	}
}
