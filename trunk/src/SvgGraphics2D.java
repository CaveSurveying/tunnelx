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
import java.awt.geom.Area;
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
// http://www.w3.org/TR/SVG/style="fill:none;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1".html#PathElement

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

// we could batch up the objects using convd or by tracking them themselves 
// and look for duplicates and use the <use xlink:href="#id"> type to 
// print them twice with the different styles

public class SvgGraphics2D extends Graphics2Dadapter  // instead of Graphics2D because all the abstract functions that need to be overridden
{
	Shape clip = null;

	private LineOutputStream los;	// don't write to this until the end

	private StringBuffer defs = new StringBuffer();
	private StringBuffer premain = new StringBuffer();
	private StringBuffer main = new StringBuffer();

	private Font currfont;
	private int cpcount = 0; // to generate unique ID's for clipping paths

	private float xoffset, yoffset;

	private SvgPathStyleTracker myPST;

    Area totalarea = null; 
	String backmaskcol = null; 
    Area jigsawareaoffset = null; // used to signify we are in laser cutting mode

	SvgGraphics2D(LineOutputStream llos, String lbackmaskcol)
	{
		if (lbackmaskcol != null)
		{
			totalarea = new Area(); 
			backmaskcol = lbackmaskcol; 
		}
		los = llos;
		myPST = new SvgPathStyleTracker();
	}

	// open and close
	void writeheader(float x, float y, float width, float height, float scalefactor) throws IOException
	{
		TNXML.chconvleng = TNXML.chconvlengWSP; // a complete hack to stop &space; getting in here

		float widthmm = (width / TN.CENTRELINE_MAGNIFICATION) / scalefactor * 1000; 
		float heightmm = (height / TN.CENTRELINE_MAGNIFICATION) / scalefactor * 1000; 
        TN.emitMessage("Scalefactor " + scalefactor + "  paperwidth="+widthmm +"mm paperheight="+heightmm +"mm"); 

		xoffset = x;
		yoffset = y;

		los.WriteLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		String viewbox = "0 0 " + String.valueOf(width) + " " + String.valueOf(height);
		los.WriteLine(TNXML.xcomopen(0, "svg", "width", Float.toString(widthmm) + "mm", "height", Float.toString(heightmm) + "mm", "viewBox", viewbox, "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		los.WriteLine(TNXML.xcom(1, "rect", "x", "0", "y", "0", "width", String.valueOf(width), "height", String.valueOf(height), "fill", "none", "stroke", "blue"));
	}

	void writefooter() throws IOException // misnomer now; it doesn't just write the footer
	{
		if (backmaskcol != null)
		{
            if (jigsawareaoffset != null)
                writeshape(jigsawareaoffset, "stroke: #0000FF; fill: none; stroke-width: 0.2mm", premain);
            else
            {
                float strokewidthpt = 4.0F; 
                String style = String.format("stroke: %s; stroke-width: %.1fpx; stroke-linecap: round; stroke-linejoin: round; fill: %s; fill-opacity: 1.0", backmaskcol, strokewidthpt, backmaskcol);
                writeshape(totalarea, style, premain);
            }
		}

		los.WriteLine(TNXML.xcomopen(0,"defs"));
		los.Write(defs.toString());
		los.WriteLine(TNXML.xcomopen(0, "style", "type", "text/css") + "<![CDATA[\n" + myPST.dumpStyles() + "]]>" + TNXML.xcomclose(0, "style"));
		los.WriteLine(TNXML.xcomclose(0, "defs"));

		if (backmaskcol != null)
		{
			los.WriteLine(TNXML.xcomopen(0, "g", "id", "backmask"));
			los.Write(premain.toString());
			los.WriteLine(TNXML.xcomclose(0, "g"));
		}

		los.WriteLine(TNXML.xcomopen(0, "g", "id", "main"));
		los.Write(main.toString());  // writeUTF?
		los.WriteLine(TNXML.xcomclose(0, "g"));
		los.WriteLine(TNXML.xcomclose(0, "svg"));

		TNXML.chconvleng = TNXML.chconvCH.length;
	}


	public void setColor(Color c)
	{
		int rgb = c.getRGB();
		myPST.crgb = String.format("#%06x", Integer.valueOf(rgb & 0xffffff));
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
        if (jigsawareaoffset != null)
            return; 
        String style = myPST.stringifyText(); 
        String sclass = myPST.getClassName(style); 
		main.append(TNXML.xcomopen(0, "text", "x", String.format("%.1f", x - xoffset), "y", String.format("%.1f", y - yoffset), "class", sclass)); 
        TNXML.xmanglxmltextSB(main, s, false); 
        main.append(TNXML.xcomclose(0, "text")); 
        main.append("\n");
	}

	public void draw(Shape s)
	{
		writeshape(s, myPST.stringifyOutline(), main);
	}
	public void fill(Shape s)
	{
        if (jigsawareaoffset != null)
            return; 
		writeshape(s, myPST.stringifyFill(), main);
		if ((backmaskcol != null) && s.getClass().getName().equals("java.awt.geom.Area")) 
            totalarea.add((Area)s); 
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
			writeshape(lclip, null, defs);
			defs.append(TNXML.xcomclose(0, "clipPath")+"\n");
		}
		clip = lclip;
	}

	public Shape getClip()
	{
		return clip;
	}


	////////////////////////////////////////
    String dconv(Shape s)
    {
        StringBuffer sb = new StringBuffer(); 
        PathIterator it = s.getPathIterator(null);
        while (!it.isDone())
        {
            int type = it.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO)
            {
                sb.append("M");
                sb.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
            }
            else if (type == PathIterator.SEG_CLOSE)
            {
                sb.append(" Z");
            }
            else if (type == PathIterator.SEG_LINETO)
            {
                sb.append(" L");
                sb.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
            }
            else if (type == PathIterator.SEG_CUBICTO)
            {
                sb.append(" C");
                sb.append(String.format("%.1f %.1f", coords[0] - xoffset, coords[1] - yoffset));
                sb.append(String.format(" %.1f %.1f", coords[2] - xoffset, coords[3] - yoffset));
                sb.append(String.format(" %.1f %.1f", coords[4] - xoffset, coords[5] - yoffset));
            }
            it.next();
        }
        return sb.toString(); 
    }

	////////////////////////////////////////
	// <path d="M 100 100 L 300 100 L 200 300 z" fill="red" stroke="blue" stroke-width="3"/>
	static float[] coords = new float[6];
	private void writeshape(Shape s, String style, StringBuffer dest)
	{
		dest.append("<path");
		if(dest != defs) 
        {
            String sclass = myPST.getClassName(style); 
            TNXML.sbattribxcom(dest, "class", sclass);
        }
		dest.append(" d=\"");
        dest.append(dconv(s)); 
		dest.append("\"");
		if (clip != null) 
			dest.append(TNXML.attribxcom("clip-path", "url(#cp" + String.valueOf(cpcount) + ")"));
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

	String stringifyFill()
	{
		return String.format("stroke: none; fill: %s; fill-opacity: %f", crgb, calpha);
	}

	String stringifyOutline()
	{
		return String.format("stroke: %s; stroke-width: %.1fpx; stroke-linecap: round; fill: none", crgb, strokewidth);
	}

	String stringifyText()
	{
		if (currfont == null)
		{
			System.out.println("Using null font!");
			return "XXX";
		}
        String fontfamily = currfont.getFamily(); 
		return String.format("font-family: %s; font-size: %.1fpx; font-style: %s; font-weight: %s; fill: %s", fontfamily, currfont.getSize2D(), (currfont.isItalic() ? "italic" : "normal"), (currfont.isBold() ? "bold" : "normal"), crgb);
	}

	public String getClassName(String currstyle)
	{
		int n = stylestack.indexOf(currstyle); 
		if (n == -1)
		{
			n = stylestack.size();
			stylestack.add(currstyle);
		}

		return "c" + String.valueOf(n);
	}


	public String dumpStyles()
	{
		StringBuffer s = new StringBuffer("");
		for(int i = 0; i < stylestack.size(); i++)
		{
			s.append(String.format(".c%d\t{ %s }\n", i, stylestack.get(i)));
		}
		return s.toString();
	}

}

