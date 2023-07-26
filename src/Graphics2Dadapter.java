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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.FontMetrics;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.awt.geom.AffineTransform;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;
import java.awt.image.renderable.RenderableImageOp;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.Composite;
import java.awt.Rectangle;
import java.awt.Paint;
import java.awt.GraphicsConfiguration;
import java.awt.font.FontRenderContext;
import java.util.Map;


/////////////////////////////////////////////
public class Graphics2Dadapter extends Graphics2D
{
	private boolean bnotimplemented()
	{
		assert false;
		return false;
	}
	private Object notimplemented()
	{
		assert false;
		return null;
	}

    public Graphics2Dadapter()
	{
        this("Untitled");
    }


    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
		{ notimplemented(); }

    public Graphics2Dadapter(String title)
	{
    }


    public void draw3DRect(int x, int y, int width, int height, boolean raised)
		{ notimplemented(); }
    public void fill3DRect(int x, int y, int width, int height, boolean raised)
		{ notimplemented(); }
    public void draw(Shape s)
		{ notimplemented(); }

    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs)
		{ return bnotimplemented(); }
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y)
		{ notimplemented(); }
    public void drawRenderedImage(RenderedImage img, AffineTransform xform)
		{ notimplemented(); }
    public void drawRenderableImage(RenderableImage img, AffineTransform xform)
		{ notimplemented(); }
    public void drawString(String str, int x, int y)
		{ notimplemented(); }
    public void drawString(String s, float x, float y)
		{ notimplemented(); }
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
		{ notimplemented(); }
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
		{ notimplemented(); }
    public void drawGlyphVector(GlyphVector g, float x, float y)
		{ notimplemented(); }
    public void fill(Shape s)
		{ notimplemented(); }
    public boolean hit(Rectangle rect, Shape s, boolean onStroke)
		{ return bnotimplemented(); }
    public GraphicsConfiguration getDeviceConfiguration()
		{ return (GraphicsConfiguration)notimplemented(); }
    public void setComposite(Composite comp)
		{ notimplemented(); }
    public void setPaint(Paint paint)
		{ notimplemented(); }
    public void setStroke(Stroke s)
		{ notimplemented(); }
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue)
		{ notimplemented(); }
    public Object getRenderingHint(RenderingHints.Key hintKey)
		{ return notimplemented(); }
// Uncomment the <?,?> stuff for Java 1.5
    public void addRenderingHints(Map/*<?,?>*/ hints)
		{ notimplemented(); }
    public void setRenderingHints(Map/*<?,?>*/ hints)
		{ notimplemented(); }
    public RenderingHints getRenderingHints()
		{ return (RenderingHints)notimplemented(); }
    public void translate(int x, int y)
		{ notimplemented(); }
    public void translate(double tx, double ty)
		{ notimplemented(); }
    public void rotate(double theta)
		{ notimplemented(); }
    public void rotate(double theta, double x, double y)
		{ notimplemented(); }
    public void scale(double sx, double sy)
		{ notimplemented(); }
    public void shear(double shx, double shy)
		{ notimplemented(); }
    public void transform(AffineTransform Tx)
		{ notimplemented(); }
    public void setTransform(AffineTransform Tx)
		{ notimplemented(); }
    public AffineTransform getTransform()
		{ return (AffineTransform)notimplemented(); }
    public Paint getPaint()
		{ return (Paint)notimplemented(); }
    public Composite getComposite()
		{ return (Composite)notimplemented(); }
    public void setBackground(Color color)
		{ notimplemented(); }
    public Color getBackground()
		{ return (Color)notimplemented(); }
    public Stroke getStroke()
		{ return (Stroke)notimplemented(); }
    public void clip(Shape s)
		{ notimplemented(); }
    public FontRenderContext getFontRenderContext()
		{ return (FontRenderContext)notimplemented(); }
    public Graphics create()
		{ return (Graphics)notimplemented(); }
    public Graphics create(int x, int y, int width, int height)
		{ return (Graphics)notimplemented(); }
    public Color getColor()
		{ return (Color)notimplemented(); }
    public void setColor(Color c)
		{ notimplemented(); }
    public void setPaintMode()
		{ notimplemented(); }
    public void setXORMode(Color c1)
		{ notimplemented(); }
    public Font getFont()
		{ return (Font)notimplemented(); }
    public void setFont(Font font)
		{ notimplemented(); }
    public FontMetrics getFontMetrics()
		{ return (FontMetrics)notimplemented(); }
    public FontMetrics getFontMetrics(Font f)
		{ return (FontMetrics)notimplemented(); }
    public Rectangle getClipBounds()
		{ return (Rectangle)notimplemented(); }
    public void clipRect(int x, int y, int width, int height)
		{ notimplemented(); }
    public void setClip(int x, int y, int width, int height)
		{ notimplemented(); }
    public Shape getClip()
		{ return (Shape)notimplemented(); }
    public void setClip(Shape clip)
		{ notimplemented(); }
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
		{ notimplemented(); }
    public void drawLine(int x1, int y1, int x2, int y2)
		{ notimplemented(); }
    public void fillRect(int x, int y, int width, int height)
		{ notimplemented(); }
    public void drawRect(int x, int y, int width, int height)
		{ notimplemented(); }
    public void clearRect(int x, int y, int width, int height)
		{ notimplemented(); }
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
		{ notimplemented(); }
    public void drawOval(int x, int y, int width, int height)
		{ notimplemented(); }
    public void fillOval(int x, int y, int width, int height)
		{ notimplemented(); }
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
		{ notimplemented(); }
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
		{ notimplemented(); }
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
		{ notimplemented(); }
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
		{ notimplemented(); }
    public void drawPolygon(Polygon p)
		{ notimplemented(); }
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
		{ notimplemented(); }
    public void fillPolygon(Polygon p)
		{ notimplemented(); }
    public void drawChars(char[] data, int offset, int length, int x, int y)
		{ notimplemented(); }
    public void drawBytes(byte[] data, int offset, int length, int x, int y)
		{ notimplemented(); }
    public boolean drawImage(Image img, int x, int y, ImageObserver observer)
		{ return bnotimplemented(); }
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer)
		{ return bnotimplemented(); }
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer)
		{ return bnotimplemented(); }
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer)
		{ return bnotimplemented(); }
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
		{ return bnotimplemented(); }
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer)
		{ return bnotimplemented(); }
    public void dispose()
		{ notimplemented(); }
    public String toString()
		{ return (String)notimplemented(); }
    public boolean hitClip(int x, int y, int width, int height)
		{ return bnotimplemented(); }
    public Rectangle getClipBounds(Rectangle r)
		{ return (Rectangle)notimplemented(); }
}
