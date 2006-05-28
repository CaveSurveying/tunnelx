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
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.geom.AffineTransform;

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
}
