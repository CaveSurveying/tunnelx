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
	String crgb = "#000000";
	float calpha = 1.0F; //fill-opacity=".5"
	float strokewidth = 1.0F;  // does this have to be int // no, it doesn't (DL)
	LineOutputStream los;
	Font currfont;
	int cpcount = 0; // to generate unique ID's for clipping paths
	boolean bcpactive = false; // XXX this can be done more neatly by just checking whether or not clip == null.

	float xoffset, yoffset;
	/* we shouldn't need this - the SVG file can be in any coordinate system, in principle - but if there is too much of a sideways offset,
	the Adobe Illustrator 10 SVG import filter doesn't work; and anyway this gives us smaller files. */

	float SCALEFACTOR = 500.0F; /* this doesn't matter very much as vector graphics are arbitrarily resizeable,
	it's more like a suggested output size */


	SvgGraphics2D(LineOutputStream llos)
	{
		los = llos;
	}

	// open and close
	void writeheader(float x, float y, float width, float height) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		float widthScaled = (width * 10) / SCALEFACTOR; // in centimetres, not decimetres
		float heightScaled = (height * 10) / SCALEFACTOR; // ditto

		xoffset = x;
		yoffset = y;

		los.WriteLine("<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"no\"?>");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/SVG/DTD/svg10.dtd\">");
//		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>");
//		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
//		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		String viewbox = "0 0 " + String.valueOf(width) + " " + String.valueOf(height);
		los.WriteLine(TNXML.xcomopen(0, "svg", "width", Float.toString(widthScaled) + "cm", "height", Float.toString(heightScaled) + "cm", "viewBox", viewbox, "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		los.WriteLine(TNXML.xcom(1, "rect", "x", "0", "y", "0", "width", String.valueOf(width), "height", String.valueOf(height), "fill", "none", "stroke", "blue"));
	}

	void writefooter() throws IOException
	{
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}


	public void setColor(Color c)
	{
		int rgb = c.getRGB();
		//crgb = String.format("#%6x", Integer.valueOf(rgb & 0xffffff)); // only works in 1.5
		crgb = Integer.toHexString(rgb & 0xffffff);
		while(crgb.length() < 6)
		{
			crgb = "0" + crgb; // XXX yuck! Why is there no lpad() function?
		}
		crgb = "#" + crgb;
		calpha = ((rgb >> 24) & 255) / 255.0F;
	}
	public void setStroke(Stroke s)
	{
		BasicStroke bs = (BasicStroke)s;
		strokewidth = bs.getLineWidth();
	}
	public void setFont(Font f)
	{
		currfont = f;
	}

	public void drawString(String s, float x, float y)
	{
		// XXX need to deal with coloured text
		boolean bBold = currfont.isBold();
		boolean bItalic = currfont.isItalic();
		try
		{
//			los.WriteLine(TNXML.xcomopen(0, "text", "x", Float.toString(x - xoffset), "y", Float.toString(y - yoffset), "font-family", currfont.getFamily(), "font-size", Float.toString(currfont.getSize2D()), "font-style", (bItalic ? "italic" : "normal"), "font-weight", (bBold ? "bold" : "normal"));
//style="font-size:22;fill:rgb(255,255,255)"
			los.Write(TNXML.attribxcom("fill", crgb));
			los.WriteLine(TNXML.xcomopen(0, "text", "x", Float.toString(x - xoffset), "y", Float.toString(y - yoffset), "font-size", Float.toString(currfont.getSize2D()), "fill", crgb));
			los.WriteLine(s);
			los.WriteLine(TNXML.xcomclose(0, "text"));
		}
		catch (IOException e)
		{ System.out.println(e.toString()); }

	}


	public void draw(Shape s)
	{
		writeshape(s, "none", crgb);
	}
	public void fill(Shape s)
	{
		writeshape(s, crgb, "none");
	}

	public void setClip(Shape lclip)
	{
		clip = lclip;
		if(clip == null)
		{
			bcpactive = false;
		}
		else
		{

			try
			{
				bcpactive = false;
				los.WriteLine(TNXML.xcomopen(0, "clipPath", "id", "cp" + Integer.toString(++cpcount)));
				writeshape(lclip, "none", "none");
				bcpactive = true;
				los.WriteLine(TNXML.xcomclose(0, "clipPath"));
			}
			catch (IOException e)
			{ System.out.println(e.toString()); }
		}
	}

	public Shape getClip()
	{
		return clip;
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
					los.Write(coords[0] - xoffset, coords[1] - yoffset);
				}
				else if (type == PathIterator.SEG_CLOSE)
				{
					los.Write(" Z");
				}
				else if (type == PathIterator.SEG_LINETO)
				{
					los.Write(" L");
					los.Write(coords[0] - xoffset, coords[1] - yoffset);
				}
				else if (type == PathIterator.SEG_CUBICTO)
				{
					los.Write(" C");
					los.Write(coords[0] - xoffset, coords[1] - yoffset);
					los.Write(coords[2] - xoffset, coords[3] - yoffset);
					los.Write(coords[4] - xoffset, coords[5] - yoffset);
				}
				it.next();
			}
			los.Write("\"");
			los.Write(TNXML.attribxcom("fill", sfill));
			if ((!sfill.equals("none")) && (calpha != 1.0))
				los.Write(TNXML.attribxcom("fill-opacity", String.valueOf(calpha)));
			if (!sstroke.equals("none"))
			{
				los.Write(TNXML.attribxcom("stroke-width", String.valueOf(strokewidth)));
				los.Write(TNXML.attribxcom("stroke-linecap", "round"));
			}
			los.Write(TNXML.attribxcom("stroke", sstroke));
			//los.Write(TNXML.attribxcom("stroke-width", "3"));
			if(bcpactive) los.Write(TNXML.attribxcom("clip-path", "url(#cp" + String.valueOf(cpcount) + ")"));
			los.WriteLine("/>");
		}
		catch (IOException e)
		{ System.out.println(e.toString()); }
	}
}

