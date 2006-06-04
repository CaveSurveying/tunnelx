////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2006  Martin Green.
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
import java.util.Iterator;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D; 
import java.awt.geom.Line2D; 

//
//
// GraphicsAbstraction
//
//

/////////////////////////////////////////////
// This will eventually be the base class for all graphics interacton
public class GraphicsAbstraction
{
	Graphics2D g2d;
	GraphicsAbstraction(Graphics2D pg2d)
	{
		g2d = pg2d;
	}

	private void setColor(Color c)
	{
		g2d.setColor(c);
	}
	private void setStroke(BasicStroke bs)
	{
		g2d.setStroke(bs);
	}
	private void draw(Shape shape)
	{
		g2d.draw(shape);
	}
	private void fill(Shape shape)
	{
		g2d.fill(shape);
	}
	private void setFont(Font font)
	{
		g2d.setFont(font);
	}
	private void drawString(String s, Float x, Float y)
	{
		g2d.drawString(s, x, y);
	}
	private Shape getClip()
	{
		return g2d.getClip();
	}
	private void setClip(Shape shape)
	{
		g2d.setClip(shape);
	}
	private void clip(Shape shape)
	{
		g2d.clip(shape);
	}
	private AffineTransform getTransform()
	{
		return g2d.getTransform();
	}
	private void setTransform (AffineTransform at)
	{
		g2d.setTransform(at);
	}
	void transform (AffineTransform at)
	{
		g2d.transform(at);
	}
	Boolean hit(Rectangle rect, Shape shape, Boolean bool)
	{
		return g2d.hit(rect, shape, bool);
	}
	//Alogrithems to handle clipping
	private GeneralPath viewclip = null;
	private void setSymbolClip(GeneralPath gparea)
	{
		clip(gparea);//Intersects the current clip with gparea
	}
	void startSymbols(OneSArea osa, Boolean clip)
	{
		if (clip)
			clip(osa.gparea);//Intersects the current clip with gparea
	}
	void startSymbols(Vector osas, Boolean clip)
	{
		if (clip)
		{
			Iterator it = osas.iterator();
			while(it.hasNext())
				clip(((OneSArea)it.next()).gparea);//Intersects the current clip with gparea
		}
	}
	void endSymbols()
	{
		setClip(viewclip);
	}

	AffineTransform satrans = null;
	void startFrame(OneSArea osa, AffineTransform at)
	{
		satrans = getTransform(); 
		transform(at);
		viewclip = osa.gparea;//Sets viewclip within the object
		setClip(viewclip);
	}
	void endFrame()
	{
		viewclip = null;
		setClip(viewclip);
		setTransform(satrans); 
	}

	void drawString(String s, LabelFontAttr lfa, float x, float y)
		{drawString(s, lfa, x, y, null);}
	void drawString(String s, LabelFontAttr lfa, float x, float y, Color color)
	{
		setFont(lfa.fontlab);
		setColor(color != null ? color : lfa.labelcolour);
		drawString(s, x, y);
	}

	void drawlabel(PathLabelDecode pld, LineStyleAttr linestyleattr, float x, float y)
		{drawlabel(pld, linestyleattr, x, y, null);}
	void drawlabel(PathLabelDecode pld, LineStyleAttr linestyleattr, float x, float y, Color color)
	{
		// draw the box outline
		if ((pld.bboxpresent) && (pld.rectdef != null))
		{
			drawShape(pld.rectdef, linestyleattr, color);
		}
		//draw the text
		for (int i = 0; i < pld.ndrawlablns; i++)
			drawString(pld.drawlablns[pld.ndrawlablns - i - 1], pld.labfontattr, x + pld.drawlabxoff, y - pld.fmdescent + pld.drawlabyoff - pld.lnspace * i, color);
		//Draw arrow
		if (pld.barrowpresent)
			for (int i = 0; i < pld.arrowdef.length; i++)
			{	
				drawShape(pld.arrowdef[i], linestyleattr, color);
			}
	}
	void drawPath(OnePath op, LineStyleAttr linestyleattr)
		{drawPath(op, linestyleattr, null);}
	void drawPath(OnePath op, LineStyleAttr linestyleattr, Color color)
	{
		assert linestyleattr.strokecolour != null;
		assert op != null;

		// set the colour
		setColor(color != null ? color : linestyleattr.strokecolour);
		if (op.linestyle == SketchLineStyle.SLS_FILLED)
		{
			fill(op.gp);
			return;
		}

		// set the stroke
		assert linestyleattr.linestroke != null;
		setStroke(linestyleattr.linestroke);

		// special spiked type things
		if (linestyleattr.spikeheight != 0.0F)
		{
			assert ((op.linestyle == SketchLineStyle.SLS_PITCHBOUND) || (op.linestyle == SketchLineStyle.SLS_CEILINGBOUND));
			drawDottedPath(op, linestyleattr.strokewidth / 2, linestyleattr.gapleng, linestyleattr.spikegap, linestyleattr.spikeheight);
		}

		// other visible strokes
		else
			draw(op.gp);
	}
	void drawShape(Shape shape, LineStyleAttr linestyleattr)
		{drawShape(shape, linestyleattr, null);}

	void drawShape(Shape shape, LineStyleAttr linestyleattr, Color color) //Just used for odd things like dotted cut out rectangles
	{
		// set the colour
		setColor(color != null ? color : linestyleattr.strokecolour);
		// set the stroke
		assert linestyleattr.linestroke != null;
		setStroke(linestyleattr.linestroke);
		//Draw the shape
		draw(shape);
	}

	/////////////////////////////////////////////
	void drawDottedPath(OnePath op, float flatness, float gapleng, float spikegap, float spikeheight)
	{
		float[] coords = new float[6];
		float[] pco = new float[op.nlines * 6 + 2];


		// maybe we will do this without flattening paths in the future.
		FlatteningPathIterator fpi = new FlatteningPathIterator(op.gp.getPathIterator(null), flatness);
		if (fpi.currentSegment(coords) != PathIterator.SEG_MOVETO)
			TN.emitProgError("move to not first");

		// put in the moveto.
		float lx = coords[0];
		float ly = coords[1];
		// (gapleng == 0.0F) means pitch bound.
		int scanmode = (gapleng == 0.0F ? 1 : 0); // 0 for blank, 1 for approaching a spike, 2 for leaving a spike.
		float dotleng = spikegap - gapleng;
		assert dotleng > 0.0;
		float scanlen = dotleng / 2;

		fpi.next();
		while (!fpi.isDone())
		{
			int curvtype = fpi.currentSegment(coords);

			//if (curvtype == PathIterator.SEG_LINETO)
			if (curvtype != PathIterator.SEG_LINETO)
				TN.emitProgError("Flattened not lineto");

			// measure the distance to the coords.
			float vx = coords[0] - lx;
			float vy = coords[1] - ly;
			float dfco = (float)Math.sqrt(vx * vx + vy * vy);
			float lam = 0.0F;
			float dfcoR = dfco;
			float lxR = lx;
			float lyR = ly;
			boolean bCont = false;

			while ((scanlen <= dfcoR) && (lam != 1.0F) && (dfcoR != 0.0F))
			{
				// find the lam where this ends
				float lam1 = Math.min(1.0F, lam + scanlen / dfco);
				float lx1 = lx + vx * lam1;
				float ly1 = ly + vy * lam1;
				if (scanmode != 0)
				{
					draw(new Line2D.Float(lxR, lyR, lx1, ly1));
				}

				lxR = lx1;
				lyR = ly1;
				lam = lam1;
				dfcoR -= scanlen;

				// spike if necessary
				if (scanmode == 1)
				{
					// right hand spike.
					if (spikeheight != 0.0F)
					{
						draw(new Line2D.Float(lxR, lyR, lxR - vy * spikeheight / dfco, lyR + vx * spikeheight / dfco));
					}

					if (gapleng != 0.0F)
					{
						scanmode = 2;
						scanlen = spikegap / 2;
					}
					else 
						scanlen = spikegap;
				}
				else if (scanmode == 0)
				{
					scanlen = spikegap / 2;
					scanmode = 1;
				}
				else
				{
					scanlen = dotleng;
					scanmode = 0;
				}
			}

			if (scanmode != 0)
			{
				draw(new Line2D.Float(lxR, lyR, coords[0], coords[1]));
			}

			scanlen -= dfcoR;

			lx = coords[0];
			ly = coords[1];

			fpi.next();
		}
	}

	/////////////////////////////////////////////
	void drawHatchedArea(OneSArea osa, int isa, int nsa)
	{
		if (osa.gparea == null)
			return;

		// make the hatch path.
		GeneralPath gphatch;
		{
			gphatch = new GeneralPath();

			// find the region we will make our parallel lines in.
			Rectangle2D r2d = osa.rboundsarea;
			double midx = r2d.getX() + r2d.getWidth() / 2;
			double midy = r2d.getY() + r2d.getHeight() / 2;
			double mrad = Math.sqrt(r2d.getWidth() * r2d.getWidth() + r2d.getHeight() * r2d.getHeight()) / 2;

			double mtheta = Math.PI * isa / nsa + 0.12345F;
			double vx = Math.cos(mtheta);
			double vy = Math.sin(mtheta);

			double sp = SketchLineStyle.strokew * 5.0F;
			int gg = (int)(mrad / sp + 1.0F);
			for (int i = -gg; i <= gg; i++)
			{
				double scx = midx + vy * sp * i;
				double scy = midy - vx * sp * i;

				gphatch.moveTo((float)(scx - vx * mrad), (float)(scy - vy * mrad));
				gphatch.lineTo((float)(scx + vx * mrad), (float)(scy + vy * mrad));
			}
		}


		// we have the hatching path.  now draw it clipped.  Sybmol Clip is used as hatching works simialarly to symbols
		startSymbols(osa, true);

		drawShape(gphatch, (isa % 2) == 0 ? SketchLineStyle.linestylehatch1 : SketchLineStyle.linestylehatch2);

		endSymbols();
	}
	void fillArea(Vector osas, Color color)
	{
		Iterator it = osas.iterator();
		while(it.hasNext())
			fillArea((OneSArea)(it.next()), color);
	}

	void fillArea(OneSArea osa, Color color)
	{
		setColor(color);
		fill(osa.gparea);
	}
	
	void drawSymbol(SSymbSing ssing, LineStyleAttr lsaline, LineStyleAttr lsafilled)
	{
		for (int j = 0; j < ssing.viztranspaths.size(); j++)
		{
			OnePath sop = (OnePath)ssing.viztranspaths.elementAt(j);
			if (sop != null)
			{
				if (sop.linestyle == SketchLineStyle.SLS_FILLED)
					drawPath(sop, lsafilled);
				else if (sop.linestyle != SketchLineStyle.SLS_CONNECTIVE)
					drawPath(sop, lsaline);
				else if ((sop.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (sop.plabedl != null) && (sop.plabedl.labfontattr != null))
					sop.paintLabel(this, false);  // how do we know what font to use?  should be from op!
			}
		}
	}
}
