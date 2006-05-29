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

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;

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

	void setColor(Color c)
	{
		g2d.setColor(c);
	}
	void setStroke(BasicStroke bs)
	{
		g2d.setStroke(bs);
	}
	void draw(Shape shape)
	{
		g2d.draw(shape);
	}
	void fill(Shape shape)
	{
		g2d.fill(shape);
	}
	void setFont(Font font)
	{
		g2d.setFont(font);
	}
	void drawString(String s, Float x, Float y)
	{
		g2d.drawString(s, x, y);
	}
	Shape getClip()
	{
		return g2d.getClip();
	}
	void setClip(Shape shape)
	{
		g2d.setClip(shape);
	}
	AffineTransform getTransform()
	{
		return g2d.getTransform();
	}
	void setTransform (AffineTransform at)
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

	void drawString(String s, LabelFontAttr lfa, float x, float y)
	{
		setFont(lfa.fontlab);
		setColor(lfa.labelcolour);
		drawString(s, x, y);
	}

	void drawlabel(PathLabelDecode pld, LineStyleAttr linestyleattr, float x, float y)
	{
		// draw the box outline
		if ((pld.bboxpresent) && (pld.rectdef != null))
		{
			drawShape(pld.rectdef, linestyleattr);
		}
		//draw the text
		for (int i = 0; i < pld.ndrawlablns; i++)
			drawString(pld.drawlablns[pld.ndrawlablns - i - 1], pld.labfontattr, x + pld.drawlabxoff, y - pld.fmdescent + pld.drawlabyoff - pld.lnspace * i);
		//Draw arrow
		if (pld.barrowpresent)
			for (int i = 0; i < pld.arrowdef.length; i++)
			{	
				drawShape(pld.arrowdef[i], linestyleattr);
			}
	}
	void drawPath(OnePath op, LineStyleAttr linestyleattr)
	{
		// set the colour
		assert linestyleattr.strokecolour != null;
		assert op != null;
		assert op.zaltcol != null;
		setColor(linestyleattr.strokecolour);
		//setColor(op.zaltcol == null ?linestyleattr.strokecolour : op.zaltcol);
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

	void drawShape(Shape shape, LineStyleAttr linestyleattr) //Just used for odd things like dotted cut out rectangles
	{
		// set the colour
		setColor(linestyleattr.strokecolour);
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

}
