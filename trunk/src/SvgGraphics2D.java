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

import java.util.ArrayList;
import java.util.List;

// refer to
// http://www.w3.org/TR/SVG/paths.html#PathElement

// need to know about inheritance of attributes (colour, stroke style)
// to save on repetition
// need to know about filling and blending, to save the repeat list for each polygon
// (once with a white shade, once with the colour)
// need to put out the fonts and labels
// need to get the window to be setable.
// need to shift the whole picture close to origin to avoid repeating
// numbers with the same digits.
// need to get symbols drawn as part of subroutine objects so they don't need
// to be respecified each time.

// these measures should help to get a very compact file that can be used
// on the CUCC webpage for the surveys.
// SVG gives possibility of highlighting different areas as the mouse passes
// over them or their labels, and linking to descriptions, or descriptions
// going to views on the svg, animating-wise.

// I(JGT)'ve got other stuff to code with the subsets and loading efficiency.
// Someone else could easily become an SVG expert and take this over,
// as it's reasonably self-contained and I know nothing about it.
// SVG output is called by selecting File | Export J-SVG
// and it writes a file called "ssvg.svg" in your tunnel directory.
// The only commands that I use to make the image are in this
// file; but we can easily upgrade it and add extra signals so
// that it can stream out extra signals to help it make the
// commands that will respond to the mouse dynamically.

// There are many resources on the web with SVG examples.
// It easily has all the power of flash, and can do everything
// you can imagine for stuff to happen in 2D.
// The point of doing it like this rather than using a general
// purpose library is that we can tune it to exactly the way
// Tunnel uses it (or change the way Tunnel renders its
// graphics to make it more appropriate for this) and so
// generate relatively short, dense files.



// DL 8/9/05: For now I have concentrated on getting this to work and to produce
// a graphic that we can import into Illustrator and its ilk, so as to sidestep
// the problems with printing. At the moment the files it generates are bloated
// in the extreme, as every path specifies anew all its attributes. However I have
// read the SVG spec and I have some notions how this situation could be rectified.
// Oh dear, I seem to have volunteered to be resident SVG expert, from a position
// of zero knowledge two days ago...



public class SvgGraphics2D extends Graphics2Dadapter
{
	Shape clip = null;

	private LineOutputStream los;	// don't write to this until the end
	private StringBuffer main;
	private StringBuffer defs;

	private Font currfont;
	private int cpcount = 0; // to generate unique ID's for clipping paths
	//boolean bcpactive = false; // XXX this can be done more neatly by just checking whether or not clip == null.

	private float xoffset, yoffset;
	/* we shouldn't need this - the SVG file can be in any coordinate system, in principle - but if there is too much of a sideways offset,
	 the Adobe Illustrator 10 SVG import filter doesn't work; and anyway this gives us smaller files. */

	private final float SCALEFACTOR = 500.0F; /* this doesn't matter very much as vector graphics are arbitrarily resizeable,
	it's more like a suggested output size */

	private SvgPathStyleTracker myPST;

	SvgGraphics2D(LineOutputStream llos)
	{
		los = llos;
		main = new StringBuffer();
		defs = new StringBuffer();
		myPST = new SvgPathStyleTracker();
	}

	// open and close
	void writeheader(float x, float y, float width, float height) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		float widthScaled = (width * 10) / SCALEFACTOR; // in centimetres, not decimetres
		float heightScaled = (height * 10) / SCALEFACTOR; // ditto

		xoffset = x;
		yoffset = y;

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		String viewbox = "0 0 " + String.valueOf(width) + " " + String.valueOf(height);
		los.WriteLine(TNXML.xcomopen(0, "svg", "width", Float.toString(widthScaled) + "cm", "height", Float.toString(heightScaled) + "cm", "viewBox", viewbox, "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		los.WriteLine(TNXML.xcom(1, "rect", "x", "0", "y", "0", "width", String.valueOf(width), "height", String.valueOf(height), "fill", "none", "stroke", "blue"));
	}

	void writefooter() throws IOException // misnomer now; it doesn't just write the footer
	{
		los.WriteLine(TNXML.xcomopen(0,"defs"));
		los.Write(defs.toString());
		los.Write(TNXML.xcomopen(0, "style", "type", "text/css") + "<![CDATA[\n" + myPST.dumpStyles() + "]]>" + TNXML.xcomclose(0, "style"));
		los.WriteLine(TNXML.xcomclose(0, "defs"));
		los.WriteLine(TNXML.xcomopen(0, "g", "id", "main"));
		los.Write(main.toString());
		los.WriteLine(TNXML.xcomclose(0, "g"));
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}


	public void setColor(Color c)
	{
		int rgb = c.getRGB();
//		myPST.crgb = String.format("#%06x", Integer.valueOf(rgb & 0xffffff));
		myPST.calpha = ((rgb >> 24) & 255) / 255.0F;
	}
	public void setStroke(Stroke s)
	{
		BasicStroke bs = (BasicStroke)s;
		myPST.strokewidth = bs.getLineWidth();
	}
	public void setFont(Font f)
	{
		//System.out.println(String.format("Setting font %s %f", f.getFamily(), f.getSize2D()));
		myPST.currfont = f;
	}

	public void drawString(String s, float x, float y)
	{
//		main.append(TNXML.xcomopen(0, "text", "x", String.format("%.1f", x - xoffset), "y", String.format("%.1f", y - yoffset), "class", myPST.getTextClass()) + s + TNXML.xcomclose(0, "text") + "\n");
	}


	public void draw(Shape s)
	{
		writeshape(s, false, main);
	}
	public void fill(Shape s)
	{
		writeshape(s, true, main);
	}

	public void clip(Shape lclip)
	{
		setClip(lclip); // quick make work
	}
		
	public void setClip(Shape lclip)
	{
		if (lclip != null)
		{
			clip = null; // don't want clipping path itself to be clipped!
			defs.append(TNXML.xcomopen(0, "clipPath", "id", "cp" + Integer.toString(++cpcount))+"\n");
			writeshape(lclip, false, defs);
			defs.append(TNXML.xcomclose(0, "clipPath")+"\n");
		}
		clip = lclip;
	}

	public Shape getClip()
	{
		return clip;
	}


	////////////////////////////////////////
	// <path d="M 100 100 L 300 100 L 200 300 z" fill="red" stroke="blue" stroke-width="3"/>
	static float[] coords = new float[6];
	private void writeshape(Shape s, boolean bFill, StringBuffer dest)
	{
		dest.append("<path d=\"");
		PathIterator it = s.getPathIterator(null);
		while (!it.isDone())
		{
			int type = it.currentSegment(coords);
			if (type == PathIterator.SEG_MOVETO)
			{
				dest.append("M");
//				dest.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
			}
			else if (type == PathIterator.SEG_CLOSE)
			{
				dest.append(" Z");
			}
			else if (type == PathIterator.SEG_LINETO)
			{
				dest.append(" L");
//				dest.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
			}
			else if (type == PathIterator.SEG_CUBICTO)
			{
				dest.append(" C");
//				dest.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
//				dest.append(String.format(" %.1f %.1f", coords[2] - xoffset, coords[3] - yoffset));
//				dest.append(String.format(" %.1f %.1f", coords[4] - xoffset, coords[5] - yoffset));
			}
			it.next();
		}
		dest.append("\"");
//		if(dest != defs) dest.append(TNXML.attribxcom("class", (bFill ? myPST.getFillClass() : myPST.getPathClass())));
//		if(clip != null) dest.append(TNXML.attribxcom("clip-path", "url(#cp" + String.valueOf(cpcount) + ")"));
		dest.append("/>\n");
	}
}

class SvgPathStyleTracker
{

	public String crgb;
	public float calpha;
	public float strokewidth;
	public Font currfont;

	private List<String> stylestack;

	SvgPathStyleTracker()
	{
		stylestack = new ArrayList<String>();
	}

	private String stringifyFill()
	{
//		return String.format("stroke: none; fill: %s; fill-opacity: %f", crgb, calpha);
return "";
	}

	private String stringifyOutline()
	{
//		return String.format("stroke: %s; stroke-width: %f; stroke-linecap: round; fill: none", crgb, strokewidth);
return "";
	}

	private String stringifyText()
	{
		if (currfont == null)
		{
			System.out.println("Using null font!");
			return "XXX";
		}
//		return String.format("font-family: %s; font-size: %f; font-style: %s; font-weight: %s; fill: %s", currfont.getFamily(), currfont.getSize2D(), (currfont.isItalic() ? "italic" : "normal"), (currfont.isBold() ? "bold" : "normal"), crgb);
return ""; 
	}

	public String getClass(String currstyle)
	{
		int n = stylestack.indexOf(currstyle);  // doesn't work for current 1.4 code
		
		if(n == -1)
		{
			stylestack.add(currstyle);
			n = stylestack.size() - 1;
		}

		return "c" + String.valueOf(n);
	}

	public String getPathClass() { return getClass(stringifyOutline()); }
	public String getFillClass() { return getClass(stringifyFill()); }
	public String getTextClass() { return getClass(stringifyText()); }


	public String dumpStyles()
	{
		StringBuffer s = new StringBuffer("");
		for(int i = 0; i < stylestack.size(); i++)
		{
//			s.append(String.format(".c%d\t{ %s }\n", i, stylestack.get(i)));
		}
		return s.toString();
	}

}

