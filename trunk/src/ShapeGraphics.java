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

import javax.swing.JPanel;
import javax.swing.JButton; 
import javax.swing.JCheckBox; 
import javax.swing.JTextField; 

import java.awt.Dimension; 
import java.awt.Graphics; 

import java.util.Vector; 


import java.awt.event.MouseListener; 
import java.awt.event.MouseMotionListener; 
import java.awt.event.MouseEvent; 
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Image; 

//
//
// ShapeGraphics
//
//


// keys don't work.  use second button for delete.  
/////////////////////////////////////////////
class ShapeGraphics extends JPanel implements MouseListener, MouseMotionListener, ActionListener
{
	// origin offset
	float ox = 0.0F; 
	float oy = 0.0F; 

	float diamx = 1.0F; 
	float diamy = 1.0F; 

	// current size things are drawn to
	Dimension csize = new Dimension(0, 0); 
	float scale = 0.0F; 

	// the integral representation of the xsibase
	Vector vsgp = new Vector(); // ShapeGraphicsPoint
	Vector vsgl = new Vector(); // ShapeGraphicsLine

	// the extents panel (a member of the SectionDisplay window)
	LRUDpanel plrud; 
	JCheckBox cbflrud; 
	JCheckBox cbera; 

	// the integral origin position
	int iox = 0; 
	int ioy = 0; 

	// centre of screen
	int xoc = 0; 
	int yoc = 0; 

	ShapeGraphicsPoint sgpactive = null; 
	boolean bInsertPoint = false; 
	boolean bHighlightedDel = false; 

	// grid line stuff
	float glleft = 0.0F; 
	int glvlines = 0; 
	float glup = 0.0F; 
	int glhlines = 0; 
	 
	// used in dynamic scaling
	int prevx = 0;
	int prevy = 0;

	// mouse motion state
	final static int M_NONE = 0; 
	final static int M_DRAG_POINT = 1; 
	final static int M_DRAG_NEW_POINT = 2; 
	final static int M_SEL_DEL_POINT = 3; 
	final static int M_DYN_SCALE = 4; 
	final static int M_DYN_DRAG = 5; 
	int momotion = M_NONE; 


	// the bitmapped background 
    ImageWarp backgroundimg = new ImageWarp(csize, this); 


	/////////////////////////////////////////////
    ShapeGraphics(LRUDpanel lplrud, JCheckBox lcbflrud, JCheckBox lcbera) 
	{
		// super(true);	// doublebuffered.  

		plrud = lplrud; 
		cbflrud = lcbflrud; 
		cbera = lcbera; 

		SetMouseMotionStuff(false);  

		// perhaps want to make a local class for this too.  (or make the listener ShapeGraphicsPanel)
		plrud.tfL.addActionListener(this); 
		plrud.tfR.addActionListener(this); 
		plrud.tfU.addActionListener(this); 
		plrud.tfD.addActionListener(this); 
	}

	/////////////////////////////////////////////
	void SetMouseMotionStuff(boolean btobackground) 
	{
		if (btobackground)
		{
			removeMouseListener(this); 
			removeMouseMotionListener(this); 
			addMouseListener(backgroundimg); 
			addMouseMotionListener(backgroundimg); 
		}
		else 
		{
			removeMouseListener(backgroundimg); 
			removeMouseMotionListener(backgroundimg); 
			addMouseListener(this); 
			addMouseMotionListener(this); 
		}
	}

	/////////////////////////////////////////////
	void ReformViewCentreDiameter(boolean bCentre)
	{
		// first get the limits
		plrud.DerivePrimaryLRUD(vsgp);  

		// extend the ranges to include the origin.  
		float Lo = Math.min(plrud.L1, 0.0F); 
		float Ro = Math.max(plrud.R1, 0.0F); 
		float Uo = Math.max(plrud.U1, 0.0F); 
		float Do = Math.min(plrud.D1, 0.0F); 


		if (bCentre)
		{
			ox = (Lo + Ro) / 2;  
			oy = -(Uo + Do) / 2;  
		}

		else
		{
			diamx = Math.max(Ro - ox, 0.0F) - Math.min(Lo - ox, 0.0F) + 0.1F; 
			diamy = Math.max(Uo - oy, 0.0F) - Math.min(Do - oy, 0.0F) + 0.1F; 

			diamx = (float)Math.ceil(diamx); 
			diamy = (float)Math.ceil(diamy); 
		}
	}


	/////////////////////////////////////////////
	void ReformPoly()
	{
		// copy over the size
		csize.width = getSize().width; 
		csize.height = getSize().height; 

		sgpactive = null; 

		// first calculate the scale
		scale = Math.min(csize.width / diamx, csize.height / diamy); 

		// centre of screen
		xoc = csize.width / 2; 
		yoc = csize.height / 2; 

		// loop and fill in the values for the points
		for (int i = 0; i < vsgp.size(); i++)
		{
			ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
			sgp.ix = (int)((sgp.x - ox) * scale + xoc + 0.5F);
			sgp.iy = (int)((-sgp.y - oy) * scale + yoc + 0.5F);
		}

		// integral origin position
		iox = (int)((-ox) * scale + xoc + 0.5F); 
		ioy = (int)((-oy) * scale + yoc + 0.5F); 

		// set up the counters for the grid lines
		int jglleft = (int)(xoc / scale - ox); 
		glleft = (-ox - jglleft) * scale + xoc; 
		glvlines = (int)(csize.width / scale + 0.5F); 
		int jglup = (int)(yoc / scale - oy); 
		glup = (-oy - jglup) * scale + yoc; 
		glhlines = (int)(csize.height / scale + 0.5F); 

		// fill in the extents
		plrud.DerivePrimaryLRUD(vsgp);  

	}

	/////////////////////////////////////////////
	// void DistortFromLRUDvalues() or load to of from one of the preview buttons.  
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JButton) 
		{
			JButton jb = (JButton)e.getSource(); 
			SPSIcon spi = (SPSIcon)jb.getIcon(); 

			// erase type behavoir.  
			if (cbera.isSelected()) 
			{
				spi.SetSection(null); 
				jb.repaint(); 
				return; 
			}

			if (spi.pxsection == null)// or some other thing set.  
			{
				spi.SetSection(new OneSection(this)); 
				jb.repaint(); 
				return; 
			}

			// copy from.  
			if (spi.pxsection == null) 
				return; 

			spi.pxsection.LoadIntoGraphics(this); 
			
			// apply the lrud if required.  
			if (cbflrud.isSelected()) // or some other thing set.  
			{
				plrud.DeriveSecondaryLRUD(vsgp, null); 
				plrud.DistortShapeToLRUD(vsgp, true); 
			}
			else 
				plrud.DerivePrimaryLRUD(vsgp);  

			// reform the polygon region
			ReformViewCentreDiameter(true); 
			ReformViewCentreDiameter(false); 
		}
		else if (e.getSource() instanceof JTextField) // from the LRUD panel.  
			plrud.DistortShapeToLRUD(vsgp, false); 
		else 
			TN.emitWarning("Bad Action Performed"); 

		ReformPoly(); 
		repaint(); 
	}

	/////////////////////////////////////////////
	public void paintComponent(Graphics g)
	{
		// test if resize has happened
		if ((getSize().height != csize.height) || (getSize().width != csize.width))
		{
			ReformPoly(); 
			// need to make new backgroundimg.  
		}

		backgroundimg.DoBackground(g, true, ox, oy, scale); 

		// draw the origin lines and origin
		g.setColor(TN.xsgGridline); 
		for (int lix = 0; lix < glvlines; lix++)
		{
			int sx = (int)(glleft + lix * scale + 0.5F); 
			g.drawLine(sx, 0, sx, csize.height); 
		}
		for (int liy = 0; liy < glhlines; liy++)
		{
			int sy = (int)(glup + liy * scale + 0.5F); 
			g.drawLine(0, sy, csize.width, sy); 
		}

		g.setColor(TN.xsgOrigin);
		g.drawLine(iox, ioy - TN.xsgOriginSize, iox, ioy + TN.xsgOriginSize);
		g.drawLine(iox - TN.xsgOriginSize, ioy, iox + TN.xsgOriginSize, ioy);

		// draw all the points
		ShapeGraphicsPoint lsgpactive = (((momotion == M_SEL_DEL_POINT) && !bHighlightedDel) ? null : sgpactive); 
		for (int i = 0; i < vsgp.size(); i++)
		{
			ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
			g.setColor(sgp.SugColour(lsgpactive)); 
			g.drawRect(sgp.ix - TN.xsgPointSize, sgp.iy - TN.xsgPointSize, 2 * TN.xsgPointSize, 2 * TN.xsgPointSize);  
		}

		// draw all the lines
		for (int i = 0; i < vsgl.size(); i++)
		{
			ShapeGraphicsLine sgl = (ShapeGraphicsLine)(vsgl.elementAt(i));  
			g.setColor(sgl.SugColour(lsgpactive)); 
			g.drawLine(sgl.sgp1.ix, sgl.sgp1.iy, sgl.sgp2.ix, sgl.sgp2.iy);  
		}
	}


	/////////////////////////////////////////////
	public void mouseMoved(MouseEvent e) {;}
	public void mouseClicked(MouseEvent e) {;}
	public void mouseEntered(MouseEvent e) {;}; 
	public void mouseExited(MouseEvent e) {;}; 


	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)  
	{
		// if a point is already being dragged, then this second mouse press will delete it.  
		if (momotion != M_NONE)
		{
			if (momotion == M_DRAG_NEW_POINT)
			{
				vsgp.removeElement(sgpactive); 
				vsgl.removeElement(sgpactive.sgl2); 
				sgpactive.sgl1.sgp2 = sgpactive.sgl2.sgp2; 
				sgpactive.sgl2.sgp2.sgl1 = sgpactive.sgl1; 
			}

			sgpactive = null; 
			momotion = M_NONE; 
			ReformPoly(); 
		    repaint();
			return; 
		}

		int x = e.getX(); 
		int y = e.getY(); 

		if (!e.isMetaDown() && (e.isShiftDown() || e.isControlDown())) 
		{
			momotion = (e.isShiftDown() ? M_DYN_DRAG : M_DYN_SCALE); 
			prevx = x; 
			prevy = y; 
			sgpactive = null; 
			return; 
		}

		momotion = (e.isMetaDown() ? (e.isShiftDown() ? M_SEL_DEL_POINT : M_DRAG_NEW_POINT) : M_DRAG_POINT); 

		// insert a node on nearest line
		if (momotion == M_DRAG_NEW_POINT)  
		{
			// find closest line
			ShapeGraphicsLine sglactive = null; 
			float rdistsq = TN.XSinteractiveSensitivitySQ;
			for (int i = 0; i < vsgl.size(); i++)
			{
				ShapeGraphicsLine sgl = (ShapeGraphicsLine)(vsgl.elementAt(i)); 

				float vx = sgl.sgp2.ix - sgl.sgp1.ix;
				float vy = sgl.sgp2.iy - sgl.sgp1.iy;

				float dx = x - sgl.sgp1.ix;
				float dy = y - sgl.sgp1.iy;

				float lambda = (dx * vx + dy * vy) / (vx * vx + vy * vy);
				lambda = Math.min(1.0F, Math.max(0.0F, lambda)); 

				float rx = lambda * vx - dx;
				float ry = lambda * vy - dy;

				float idistsq = rx * rx + ry * ry;
				if (idistsq < rdistsq)
				{
					sglactive = sgl; 
					rdistsq = idistsq;
				}
			}

			// insert a point now we have chosen the line
			if (sglactive != null)
			{
				// make the new points
				sgpactive = new ShapeGraphicsPoint(); 
				ShapeGraphicsLine sglnew = new ShapeGraphicsLine(); 
				
				// add into the vectors
				vsgp.addElement(sgpactive); 
				vsgl.addElement(sglnew); 

				// link up the forward and backward pointers 
				sgpactive.sgl1 = sglactive; 
				sgpactive.sgl2 = sglnew; 
				sglnew.sgp1 = sgpactive;
 				sglnew.sgp2 = sglactive.sgp2; 
				sglnew.sgp2.sgl1 = sglnew; 
				sglactive.sgp2 = sgpactive; 
			}
		}

		// otherwise find closest node 
		else 
		{
			sgpactive = null; 
			int rdistsq = TN.XSinteractiveSensitivitySQ;
			for (int i = 0; i < vsgp.size(); i++)
			{
				ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
				int dx = sgp.ix - x;
				int dy = sgp.iy - y;
				int idistsq = dx * dx + dy * dy;
				if (idistsq < rdistsq) 
				{
					rdistsq = idistsq;
					sgpactive = sgp;
				}
			}
		}


		// set the location
		if (sgpactive != null)
		{
			if (momotion != M_SEL_DEL_POINT)  
			{
				sgpactive.ix = x;
				sgpactive.iy = y;

				// the extents
				plrud.DeriveSecondaryLRUD(vsgp, sgpactive);  
				plrud.DeriveDynamicPrimaryLRUD(sgpactive.x, sgpactive.y); 
			} 
			else
				bHighlightedDel = true; 
		    repaint();
		}
		else
			momotion = M_NONE; 
	}

	/////////////////////////////////////////////
    public void mouseDragged(MouseEvent e) 
	{
		switch (momotion)
		{
		case M_DYN_SCALE: 
		{
			int x = e.getX(); 
			int y = e.getY(); 
			float rescalex = 1.0F + ((float)Math.abs(x - prevx) / csize.width) * 2.0F; 
			if (x < prevx)
				rescalex = 1.0F / rescalex; 
			float rescaley = 1.0F + ((float)Math.abs(y - prevy) / csize.height) * 2.0F; 
			if (y < prevy)
				rescaley = 1.0F / rescaley; 
			for (int i = 0; i < vsgp.size(); i++)
			{
				ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
				sgp.ix = (int)((sgp.x * rescalex - ox) * scale + xoc + 0.5F); 
				sgp.iy = (int)((-sgp.y * rescaley - oy) * scale + yoc + 0.5F); 
			}
			plrud.DeriveDynamicPrimaryLRUDrescale(rescalex, rescaley); 
			repaint();
			break; 
		}

		case M_DYN_DRAG: 
		{
			int xv = e.getX() - prevx; 
			int yv = e.getY() - prevy; 
			for (int i = 0; i < vsgp.size(); i++)
			{
				ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
				sgp.ix = (int)((sgp.x - ox) * scale + xoc + 0.5F) + xv; 
				sgp.iy = (int)((-sgp.y - oy) * scale + yoc + 0.5F) + yv; 
			}
			plrud.DeriveDynamicPrimaryLRUDtranslate(xv / scale, -yv / scale); 
			repaint();
			break; 
		}

		case M_DRAG_POINT: 
		case M_DRAG_NEW_POINT: 
		{
			sgpactive.ix = e.getX();
			sgpactive.iy = e.getY();
			plrud.DeriveDynamicPrimaryLRUD((float)(sgpactive.ix - xoc) / scale + ox, -((float)(sgpactive.iy - yoc) / scale + oy)); 
			repaint();
			break; 
		}


		case M_SEL_DEL_POINT: 
		{
			int dx = sgpactive.ix - e.getX();
			int dy = sgpactive.iy - e.getY(); 
			int idistsq = dx * dx + dy * dy; 
			boolean pbHighlightedDel = bHighlightedDel; 
			bHighlightedDel = (idistsq < TN.XSinteractiveSensitivitySQ); 
			if (pbHighlightedDel != bHighlightedDel) 
				repaint();
		}

		case M_NONE: 
		default: 
			break; 
		}
	}

	/////////////////////////////////////////////
    public void mouseReleased(MouseEvent e)
	{
		switch (momotion)
		{
		case M_DYN_SCALE: 
		{
			int x = e.getX(); 
			int y = e.getY(); 
			float rescalex = 1.0F + ((float)Math.abs(x - prevx) / csize.width) * 2.0F; 
			if (x < prevx)
				rescalex = 1.0F / rescalex; 
			float rescaley = 1.0F + ((float)Math.abs(y - prevy) / csize.height) * 2.0F; 
			if (y < prevy)
				rescaley = 1.0F / rescaley; 
			for (int i = 0; i < vsgp.size(); i++)
			{
				ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
				sgp.x *= rescalex; 
				sgp.y *= rescaley; 
			}
			break; 
		}

		case M_DYN_DRAG: 
		{
			float xv = (e.getX() - prevx) / scale; 
			float yv = -(e.getY() - prevy) / scale; 
			for (int i = 0; i < vsgp.size(); i++)
			{
				ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.elementAt(i)); 
				sgp.x += xv; 
				sgp.y += yv; 
			}
			break; 
		}

		case M_DRAG_POINT: 
		case M_DRAG_NEW_POINT: 
		{
			sgpactive.ix = e.getX();
			sgpactive.iy = e.getY();


			// calculate the real point
			//xp[i] = (int)((xsibase.xc[i] - ox) * scale + xoc + 0.5F);
			sgpactive.x = (float)(sgpactive.ix - xoc) / scale + ox; 
			sgpactive.y = -((float)(sgpactive.iy - yoc) / scale + oy); 
			sgpactive = null;

			break; 
		}

		case M_SEL_DEL_POINT: 
		{
			int dx = sgpactive.ix - e.getX();
			int dy = sgpactive.iy - e.getY(); 
			int idistsq = dx * dx + dy * dy; 
			if ((idistsq < TN.XSinteractiveSensitivitySQ) && (vsgp.size() > 3))
			{
				vsgp.removeElement(sgpactive); 
				vsgl.removeElement(sgpactive.sgl2); 
				sgpactive.sgl1.sgp2 = sgpactive.sgl2.sgp2; 
				sgpactive.sgl2.sgp2.sgl1 = sgpactive.sgl1; 
			}
		}

		case M_NONE: 
		default: 
			break; 
		}

		sgpactive = null;
		momotion = M_NONE; 
		ReformPoly();	
		repaint();
	}

}


