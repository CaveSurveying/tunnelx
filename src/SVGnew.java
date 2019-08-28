////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2012  Julian Todd.
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

import java.awt.geom.Rectangle2D; 
import java.util.Map;
import java.io.IOException; 

////////////////////////////////////////////////////////////////////////////////
class SVGnew
{
    LineOutputStream los; 
    boolean btransparentbackground; 
    float scalefactor; 
    int irenderingquality; 
    Rectangle2D printrect; 
    Map<String, SubsetAttrStyle> subsetattrstylesmap; 
    
    ////////////////////////////////////////////////////////////////////////////////
    SVGnew(FileAbstraction fa, boolean lbtransparentbackground, float lscalefactor, Rectangle2D lprintrect, int lirenderingquality, Map<String, SubsetAttrStyle> lsubsetattrstylesmap) throws IOException
    {
        los = new LineOutputStream(fa, "UTF-8"); 
        btransparentbackground = lbtransparentbackground; 
        scalefactor = lscalefactor; 
        printrect = lprintrect; 
        irenderingquality = lirenderingquality; 
        subsetattrstylesmap = lsubsetattrstylesmap; 
    }
    
   	void writeheader() throws IOException
	{
		TNXML.chconvleng = TNXML.chconvlengWSP; // a complete hack to stop &space; getting in here

		double widthmm = (printrect.getWidth() / TN.CENTRELINE_MAGNIFICATION) / scalefactor * 1000; 
		double heightmm = (printrect.getHeight() / TN.CENTRELINE_MAGNIFICATION) / scalefactor * 1000; 
        TN.emitMessage("Scalefactor " + scalefactor + "  paperwidth="+widthmm +"mm paperheight="+heightmm +"mm"); 

		los.WriteLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		String viewbox = "0 0 " + String.valueOf(printrect.getWidth()) + " " + String.valueOf(printrect.getHeight());
		los.WriteLine(TNXML.xcomopen(0, "svg", "width", Double.toString(widthmm) + "mm", "height", Double.toString(heightmm) + "mm", "viewBox", viewbox, "version", "1.1", "xmlns", "http://www.w3.org/2000/svg", "xmlns:xlink", "http://www.w3.org/1999/xlink"));
        los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		los.WriteLine(TNXML.xcom(1, "rect", "x", "0", "y", "0", "width", String.valueOf(printrect.getWidth()), "height", String.valueOf(printrect.getHeight()), "fill", "none", "stroke", "blue"));
	}

    /////////////////////////////////////////////
    void writestyles() throws IOException
    {
		los.Write(TNXML.xcomopen(0, "style", "type", "text/css")); 
        los.WriteLine("<![CDATA["); 
        los.WriteLine(".c1	{ stroke: #000000; stroke-width: 0.6px; stroke-linecap: round; fill: none }"); 
        los.Write("]]>"); 
		los.WriteLine(TNXML.xcomclose(0, "style")); 
    }

    /////////////////////////////////////////////
	void DrawSketch(OneSketch tsketch) throws IOException
    {
        writeheader(); 
		los.WriteLine(TNXML.xcomopen(0, "defs")); 
        writestyles(); 
		los.WriteLine(TNXML.xcomopen(1, "g", "id", "main")); 
        for (OnePath op : tsketch.vpaths)
            los.WriteLine(TNXML.xcom(2, "path", "class", "c1", "d", op.svgdvalue(0.0F, 0.0F))); 
		los.WriteLine(TNXML.xcomclose(1, "g")); 
		los.WriteLine(TNXML.xcomclose(0, "defs")); 
        los.WriteLine(TNXML.xcom(1, "use", "xlink:href", "#main", "transform", "translate(500, 500)")); 
		los.WriteLine(TNXML.xcomclose(0, "svg"));
    }
}
