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

import java.awt.Shape;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.IOException;

public class SvgGraphics2D extends Graphics2Dadapter
{
	Shape clip = null;
	LineOutputStream los;

	SvgGraphics2D(LineOutputStream llos)
	{
		los = llos;
	}

	// open and close
	void writeheader(float x, float y, float width, float height) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		String viewbox = String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(width) + " " + String.valueOf(height);
		los.WriteLine(TNXML.xcomopen(0, "svg", "width", "12cm", "height", "10cm", "viewBox", viewbox, "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		los.WriteLine(TNXML.xcom(1, "rect", "x", String.valueOf(x), "y", String.valueOf(y), "width", String.valueOf(width), "height", String.valueOf(height), "fill", "none", "stroke", "blue"));
		los.WriteLine(TNXML.xcom(1, "path", "d", "M 100 100 L 300 100 L 200 300 z", "fill", "red", "stroke", "blue", "stroke-width", "3"));
	}
	void writefooter() throws IOException
	{
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}


    public void setColor(Color c)
		{ System.out.println(c.toString()); }
    public void setStroke(Stroke s)
		{ System.out.println(s.toString()); }
    public void setFont(Font f)
		{ System.out.println(f.toString()); }
    public void drawString(String s, float x, float y)
		{ System.out.println(s); }
	public void draw(Shape s)
	{
		writeshape(s, "none", "blue");
    }
	public void fill(Shape s)
	{
		System.out.println(s);
    }

    public void setClip(Shape lclip)
	{
		clip = lclip;
	}
    public Shape getClip()
	{
		return (new AffineTransform()).createTransformedShape(clip);
	}



	////////////////////////////////////////
	// <path d="M 100 100 L 300 100 L 200 300 z" fill="red" stroke="blue" stroke-width="3"/>
	static float[] coords = new float[6];
	private void writeshape(Shape s, String sfill, String sstroke)
	{
		try
		{
		los.Write("<path d=\"");
		PathIterator it = s.getPathIterator(null);
 		while (!it.isDone())
		{
			int type = it.currentSegment(coords);
			if (type == PathIterator.SEG_MOVETO)
			{
				los.Write("M");
				los.Write(coords[0], -coords[1]);
			}
			else if (type == PathIterator.SEG_CLOSE)
			{
				los.Write(" Z");
			}
			else if (type == PathIterator.SEG_LINETO)
			{
				los.Write(" L");
				los.Write(coords[0], -coords[1]);
			}
			else if (type == PathIterator.SEG_CUBICTO)
			{
				los.Write(" L");
//				los.Write(coords[0], -coords[1]);
//				los.Write(coords[2], -coords[3]);
				los.Write(coords[4], -coords[5]);
			}
			it.next();
		}
		los.Write("\"");
		los.Write(TNXML.attribxcom("fill", sfill));
		los.Write(TNXML.attribxcom("stroke", sstroke));
		//los.Write(TNXML.attribxcom("stroke-width", "3"));
		los.WriteLine("/>");
		}
		catch (IOException e)
		{ System.out.println(e.toString()); }
	}
}

