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

import java.util.Vector;
import java.io.IOException;
import java.lang.String;
import java.awt.geom.PathIterator;

class SVGAreas
{
   private float tunnelunit = 0.1F; //length of tunnel unit in meters
	private float xoffset = 0F;
	private float yoffset = 0F;
	private int id = 0; //The next id to use
	public SVGAreas(LineOutputStream los, Vector vareas) throws IOException
   {
		WriteHeader(los);
		for (int j = 0; j < vareas.size(); j++)
		{
			WriteArea(los, (OneSArea)vareas.elementAt(j));
		}
		WriteFooter(los);
	}

	// open and close
	void WriteHeader(LineOutputStream los) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		los.WriteLine(TNXML.xcomopen(0, "svg", "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Tunnels Areas"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "This file solely contains the calculated areas for Tunnel, you need a view.svg file to see anything."));
		los.WriteLine(TNXML.xcomopen(1, "defs"));
	}

	void WriteFooter(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomclose(1, "defs"));
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}

	static float[] coords = new float[6]; //Used to get the position of line segments
	void WriteArea(LineOutputStream los, OneSArea oa) throws IOException
	{
		//Set svg id to area
		String sid = new String(String.valueOf(this.id));
		this.id=this.id+1;
		oa.setId(sid);
		//Generate list of classes
		String classes = new String("");
		for (int j = 0; j < oa.vssubsetattrs.size(); j++)
		{
			if(j!=0) classes = classes + " ";
			classes = classes + oa.vssubsetattrs.get(j).subsetname;//Why does this not work?
		}
		//Generate d the list of commands to generate points
		String d = new String();
		PathIterator it = oa.gparea.getPathIterator(null);
		for (int j=0;!it.isDone();j=1)
		{
			if(j!=0) d = d + " ";
			int type = it.currentSegment(coords);//coords of the segment are returned
			if (type == PathIterator.SEG_MOVETO)
			{
				d = d + "M" + (coords[0] - xoffset) + " " + (coords[1] - yoffset);
			}
			else if (type == PathIterator.SEG_LINETO)
			{
				d = d + " L" + (coords[0] - xoffset) + " " + (coords[1] - yoffset);
			}
			else if (type == PathIterator.SEG_CUBICTO)
			{
				d = d + " C" + (coords[0] - xoffset) + " " + (coords[1] - yoffset) + " " + (coords[2] - xoffset) + " " + (coords[3] - yoffset) + " " + (coords[4] - xoffset) + " " + (coords[5] - yoffset);
			}
			it.next();
		}
		//Write line
		los.WriteLine(TNXML.xcom(2, "path", "id", sid, "class", classes, "d", d, "z", String.valueOf(oa.zalt)));
	}
}
