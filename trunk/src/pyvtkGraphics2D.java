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
import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.awt.geom.GeneralPath;
import java.awt.geom.FlatteningPathIterator;



////////////////////////////////////////
public class pyvtkGraphics2D extends Graphics2Dadapter
{
	Shape clip = null;
	String crgb = "#000000";
	float calpha = 1.0F; //fill-opacity=".5"
	float fpflatness = 0.5F;
	LineOutputStream los;

	////////////////////////////////////////
	pyvtkGraphics2D(LineOutputStream llos)
	{
		los = llos;
	}

	////////////////////////////////////////
	// open and close
	void writeheader(float x, float y, float width, float height) throws IOException
	{
		TNXML.chconvleng = TNXML.chconvlengWSP; // a complete hack to stop &space; getting in here
		los.WriteLine("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		los.WriteLine("");

		//String viewbox = String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(width) + " " + String.valueOf(height);
		//los.WriteLine(TNXML.xcomopen(0, "svg", "width", "12cm", "height", "10cm", "viewBox", viewbox, "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		//los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
		//los.WriteLine(TNXML.xcomtext(1, "desc", "description thing"));

		//los.WriteLine(TNXML.xcom(1, "rect", "x", String.valueOf(x), "y", String.valueOf(y), "width", String.valueOf(width), "height", String.valueOf(height), "fill", "none", "stroke", "blue"));
		los.WriteLine(TNXML.xcomopen(0, "tunnelxpyvtk"));
	}

	////////////////////////////////////////
	void writefooter() throws IOException
	{
		los.WriteLine(TNXML.xcomclose(0, "tunnelxpyvtk"));
		TNXML.chconvleng = TNXML.chconv.length;
	}
    public void setColor(Color c)
	{
		int rgb = c.getRGB();
		crgb = "#" + Integer.toHexString(rgb & 0xffffff);
		calpha = ((rgb >> 24) & 255) / 255.0F;
	}
    public void setStroke(Stroke s)
	{
	}
    public void setFont(Font f)
		{ /*System.out.println(f.toString());*/ }
    public void drawString(String s, float x, float y)
	{
		try
		{
		los.WriteLine(TNXML.xcom(0, "label", "x", String.valueOf(x), "y", String.valueOf(y), "string", s));
		}
		catch (IOException e) {;};
	}
	public void draw(Shape s)
	{
		//writeshape(s, "none", crgb);
    }
	public void fill(Shape s)
	{
		//writeshape(s, crgb, "none");
    }

    public void setClip(Shape lclip)
	{
		clip = lclip;
	}
    public Shape getClip()
	{
		return clip;
	}


	////////////////////////////////////////
	public void writearea(OneSArea osa) throws IOException
	{
		if (osa.subsetattr.areacolour == null)
			return;
		if (SketchLineStyle.bDepthColours)
			setColor(SketchLineStyle.GetColourFromCollam(osa.icollam, true)); 
		else
			setColor(osa.subsetattr.areacolour);
		los.WriteLine(TNXML.xcomopen(0, "pyvtkarea", "colour", crgb));

		// we should perform the hard task of reflecting certain paths in situ.
		for (RefPathO rpo : osa.refpathsub)
			writepath(rpo.op.gp, rpo.op.pnstart.zalt, rpo.op.pnend.zalt, rpo.bFore);
		los.WriteLine(TNXML.xcomclose(0, "pyvtkarea"));
	}

	////////////////////////////////////////
	float[] coords = new float[6];
	public void writepath(GeneralPath gp, float zstart, float zend, boolean bFore) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(1, "path", "zstart", String.valueOf(zstart), "zend", String.valueOf(zend), "reversed", (bFore ? "0" : "1")));
		FlatteningPathIterator fpi = new FlatteningPathIterator(gp.getPathIterator(null), fpflatness);
		int ptcount = 0;
		while (!fpi.isDone())
		{
			int curvtype = fpi.currentSegment(coords);
			assert (curvtype == (ptcount == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO));
			los.WriteLine(TNXML.xcom(2, "pt", "x", String.valueOf(coords[0]), "y", String.valueOf(coords[1])));
			fpi.next();
			ptcount++;
		}
		los.WriteLine(TNXML.xcomclose(1, "path"));
	}
	
	/////////////////////////////////////////////
	static void PrintThisPYVTK(OneSketch tsketch)
	{
		try
		{
		FileAbstraction fout = FileAbstraction.MakeWritableFileAbstraction("pyvtk.xml");
		TN.emitMessage("Writing file " + fout.getName());
		LineOutputStream los = new LineOutputStream(fout);
		pyvtkGraphics2D pyvtk = new pyvtkGraphics2D(los);
		boolean bRefillOverlaps = false;

		Rectangle2D bounds = tsketch.getBounds(false, false);
		pyvtk.writeheader((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight());
		for (OneSArea osa : tsketch.vsareas)
		{
			if (osa.iareapressig == SketchLineStyle.ASE_KEEPAREA)
				pyvtk.writearea(osa);
		}

		//tsketch.paintWquality(svgg, bHideCentreline, bHideMarkers, bHideStationNames);
		pyvtk.writefooter();
		los.close();
		}
		catch (IOException ie)
		{ System.out.println(ie.toString()); }
	}
}


