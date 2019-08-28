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
import java.awt.Graphics; 
import java.awt.Graphics2D; 

import java.awt.Dimension; 
import java.awt.Image; 

import java.awt.Color; 

import java.awt.event.MouseListener; 
import java.awt.event.MouseMotionListener; 
import java.awt.event.MouseEvent; 
import java.awt.event.ActionEvent; 

import java.util.List; 
import java.util.ArrayList; 
import java.util.SortedSet; 
import java.util.TreeSet; 


//
//
// WireframeGraphics
//
//
class WireframeGraphics extends JPanel implements MouseListener, MouseMotionListener
{
	WireframeDisplay wireframedisplay; 

	DepthCol depthcol = new DepthCol(); 

	List<OneStation> vstations = new ArrayList<OneStation>(); 
	List<OneLeg> vlegs = new ArrayList<OneLeg>(); 
	
	boolean bEditable = false; 

	// recordings of active objects
	OneStation vstationactive = null; 
	OneStation vstationactivesel = null; 

	// current rotation
	Matrix3D mat = new Matrix3D();
	Matrix3D invmat = new Matrix3D();

	// main centre offset
	Vec3 coff = new Vec3(); 

	// quaternion rotations
	Quaternion qnow = new Quaternion(); 
	Quaternion qmdown = new Quaternion(); 
	Quaternion qmdrag = new Quaternion(); 
	Vec3 vmdown = new Vec3(); 
	Vec3 vmdrag = new Vec3(); 
	
	// Zfixed rotations
	float zfRotZ = (float)Math.PI / 2; 
	float zfRotX = (float)Math.PI / 2; 
	float zfRotZdown = 0.0F; 
	float zfRotXdown = 0.0F; 

	int tcmx; // mouse coordinates used for dragging the tube cone
	int tcmy; 

	Matrix3D rotmat = new Matrix3D(); 
	Matrix3D invrotmat = new Matrix3D(); 

	float diameter = 20.0F; 
	Dimension csize = new Dimension(0, 0); 

	int xoc = 0; 
	int yoc = 0; 

	
	// values used by the dynamic rotate and scale

	// old and new offsets
	Vec3 ocoff = new Vec3(); 
	Vec3 ncoff = new Vec3(); 
	float xfac; 

	int prevx = 0;
	int prevy = 0;
	float prevdiameter = 0.0F; 

	// screen vectors
	Vec3 sxvec = new Vec3(); 
	Vec3 syvec = new Vec3(); 
	Vec3 szvec = new Vec3(); 

	// the axes
	WireAxes wireaxes = new WireAxes(); 

	// mouse motion state
	final static int M_NONE = 0; 
	final static int M_DYN_ROTATE = 1; 
	final static int M_DYN_TRANSLATE = 2; 
	final static int M_DYN_SCALE = 3; 
	final static int M_SEL_STATIONS = 4; 
	
	int momotion = M_NONE; 

	// types of repainting 
	boolean bNeedsClearing = false; 
	boolean bNeedsRedrawing = false; //(maybe only redrawing a little piece of the pic)  


	/////////////////////////////////////////////
	WireframeGraphics(WireframeDisplay lwireframedisplay) 
	{
		super(true); // doublebuffered  

		setBackground(TN.skeBackground); 
		setForeground(TN.wfmLeg); 

		addMouseListener(this); 
		addMouseMotionListener(this); 

		wireframedisplay = lwireframedisplay; 
	}

	/////////////////////////////////////////////
	void UpdateDepthCol() 
	{
		for (int i = 0; i < vstations.size(); i++)
		{
			OneStation os = vstations.get(i); 
			depthcol.AbsorbRange(os, (i == 0)); 
		} 
		
		SortedSet<String> legdates = new TreeSet<String>(); 
		for (OneLeg ol : vlegs)
			legdates.add(ol.svxdate); 
		depthcol.svxdates.clear(); 
		depthcol.svxdates.addAll(legdates); 
		depthcol.datelimit = (depthcol.svxdates.isEmpty() ? "" : depthcol.svxdates.get(depthcol.svxdates.size() - 1)); 
	}


	/////////////////////////////////////////////
	public void paintComponent(Graphics g) 
	{
		// test if resize has happened
		if ((getSize().height != csize.height) || (getSize().width != csize.width))
			ReformView(); 

		setBackground(TN.wfmBackground); 
/*		if (bNeedsClearing) 
		{
			g.clearRect(0, 0, csize.width, csize.height); 
			bNeedsClearing = false; 
		}
		else if (!bNeedsRedrawing) 
			return; 
		bNeedsRedrawing = false; 
*/
		//g.clearRect(0, 0, csize.width, csize.height);
		g.setColor(TN.wfmBackground);
		g.fillRect(0, 0, csize.width, csize.height);

		// draw the legs
		if (wireframedisplay.miCentreline.isSelected())
		{
			for (OneLeg ol : vlegs)
			{
				if (ol.osfrom != null)
				{
					ol.paintW(g, !bEditable, (wireframedisplay.miDepthCols.isSelected() ? depthcol : null));
				}
			}
		}
		// draw the stations
		if (wireframedisplay.miStationNames.isSelected() && !((momotion == M_DYN_ROTATE) || (momotion == M_DYN_TRANSLATE) || (momotion == M_DYN_SCALE)))
		{
			for (OneStation os : vstations)
				if (os != vstationactive)
					os.paintW(g, false, false);
		}
		if (vstationactive != null)
			vstationactive.paintW(g, true, !bEditable);



		// draw the axes
		if (wireframedisplay.miAxes.isSelected())
			wireaxes.paintW(g); 
    }



	/////////////////////////////////////////////
	void ReformMatrix()
	{
		// first reform the matrix
		csize.width = getSize().width; 
		csize.height = getSize().height; 

		// first calculate the scale
		int minwidth = Math.min(csize.width, csize.height); 
		if (minwidth == 0)
			minwidth = 1; 
		xfac = minwidth / diameter; 

		// centre of screen
		xoc = csize.width / 2; 
		yoc = csize.height / 2; 

		mat.unit();
		mat.translate(coff.x, coff.y, coff.z);

		rotmat.SetQuat(qnow.x, qnow.y, qnow.z, qnow.w); 
		mat.mult(rotmat); 

		mat.scale(xfac, -xfac, xfac);
		mat.translate(xoc, yoc, 0);

		sxvec.SetXYZ(rotmat.xx, rotmat.xy, rotmat.xz); 
		syvec.SetXYZ(rotmat.yx, rotmat.yy, rotmat.yz); 
		szvec.SetXYZ(rotmat.zx, rotmat.zy, rotmat.zz); 

		// inverse matrix
		invmat.unit(); 
		invmat.translate(-xoc, -yoc, 0);
		invmat.scale(1.0F / xfac, 1.0F / (-xfac), 1.0F / xfac);

		invrotmat.SetQuat(qnow.x, qnow.y, qnow.z, -qnow.w); 
		invmat.mult(invrotmat); 

		invmat.translate(-coff.x, -coff.y, -coff.z);
	}


	/////////////////////////////////////////////
	void ReformView()
	{
		bNeedsClearing = true; 

		ReformMatrix(); 

		// axes position
		wireaxes.ReformAxes(rotmat, csize, xoc, yoc, xfac);   

		// now transform the stations and points of the XSections
		for (OneStation os : vstations)
			os.SetTLoc(mat); 
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////

	/////////////////////////////////////////////
	void ReCentre(int mx, int my)
	{
		// find centre depth
		int izlo = 0; 
		int izhi = -1; 

		for (OneStation os : vstations)
		{
			// scan only the stations that are on the screen 
			// (ignores legs, unfortunately).  Could make this a menu switch.  
			if ((os.TLocX > -10) && (os.TLocX < csize.width + 10) && (os.TLocY > -10) && (os.TLocY < csize.height + 10))  
			{
				int iz = os.TLocZ; 
				
				if ((izhi == -1) && (izlo == 0)) 
				{
					izlo = iz; 
					izhi = iz; 
				}
				else
				{
					if (iz < izlo)
						izlo = iz; 
					if (iz > izhi) 
						izhi = iz; 
				}
			}
		}

		int mz = (izlo + izhi) / 2; 

		ncoff.x = -(mx * invmat.xx + my * invmat.xy + mz * invmat.xz + invmat.xo);
		ncoff.y = -(mx * invmat.yx + my * invmat.yy + mz * invmat.yz + invmat.yo);
		ncoff.z = -(mx * invmat.zx + my * invmat.zy + mz * invmat.zz + invmat.zo);

		ReformView(); 
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	void MaximizeView()
	{
		// probably this ought to be calculating directly from the point 
		// positions without using the ReformView information. 

		if (vstations.isEmpty())
			return; 
					
		float fxlo = 0.0F; 
		float fxhi = 0.0F; 
		float fylo = 0.0F; 
		float fyhi = 0.0F; 

		boolean bfirst = true; 
		for (OneStation os : vstations)
		{
			float fx = sxvec.Dot(os.Loc); 
			float fy = syvec.Dot(os.Loc); 
			
			if (bfirst || (fx < fxlo))
				fxlo = fx; 
			if (bfirst || (fx > fxhi))
				fxhi = fx; 
			if (bfirst || (fy < fylo))
				fylo = fy; 
			if (bfirst || (fy > fyhi))
				fyhi = fy; 
			bfirst = false; 
		}

		// do the centring
		float fcx = -(fxhi + fxlo) / 2; 
		float fcy = -(fyhi + fylo) / 2; 

		coff.SetXYZ(fcx * sxvec.x + fcy * syvec.x, fcx * sxvec.y + fcy * syvec.y, fcx * sxvec.z + fcy * syvec.z); 

		// do the scaling
		float fxd = Math.max(fxhi - fxlo, 0.1F); 
		float fyd = Math.max(fyhi - fylo, 0.1F);  
		diameter = ((csize.width / fxd < csize.height / fyd) ? fxd : fyd) * 1.2F; 
	}


	/////////////////////////////////////////////
	void CalcQView()
	{
		float LzfRotX = zfRotX - ((int)(zfRotX / (float)Math.PI) + (zfRotX < 0.0F ? -1 : 0)) * (float)Math.PI; 
		float LzfRotZ = zfRotZ - ((int)(zfRotZ / (float)Math.PI) + (zfRotZ < 0.0F ? -1 : 0)) * (float)Math.PI; 
		qmdown.SetXYZ(0.0F, 0.0F, (float)Math.cos(LzfRotX)); 
		qmdrag.SetXYZ(-(float)Math.cos(LzfRotZ), 0.0F, 0.0F); 
		qnow.Mult(qmdown, qmdrag); 
	}


	/////////////////////////////////////////////
	public void SetAutomaticView(float lzfRotX, float lzfRotZ)	
	{
		if (lzfRotX != -1.0)
		{
			zfRotX = lzfRotX; 
			zfRotZ = lzfRotZ; 
			CalcQView(); 
		}
		else
			MaximizeView(); 

		ReformView(); 
		repaint(); 
	}





	/////////////////////////////////////////////
	// mouse events
	/////////////////////////////////////////////

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
			vstationactive = null; 
			momotion = M_NONE; 
		    repaint();
			return; 
		}


		int x = e.getX(); 
		int y = e.getY(); 

		// selection types (right mouse) 
		if (e.isMetaDown())  
		{
			momotion = M_SEL_STATIONS; 
			if (!bEditable && (momotion != M_SEL_STATIONS)) 
				momotion = M_NONE; 
		}

		// select the motion type (left mouse) 
		else
		{
			if (e.isShiftDown())
				momotion = M_DYN_TRANSLATE; 
			else if (e.isControlDown())
				momotion = M_DYN_SCALE; 
			else 
				momotion = M_DYN_ROTATE; 
		}

		
		// act on the motion type
		switch(momotion)
		{
		case M_DYN_SCALE: 
		case M_DYN_TRANSLATE: 
			ReCentre(x, y); 
			ocoff.SetXYZ(coff.x, coff.y, coff.z); 

			prevx = x;
			prevy = y;
			
			prevdiameter = diameter; 
			break; 

		case M_DYN_ROTATE: 
			ReCentre(xoc, yoc); 
			coff.SetXYZ(ncoff.x, ncoff.y, ncoff.z); 

			prevx = x;
			prevy = y;

			if (!wireframedisplay.miZFixed.isSelected())
			{
				vmdown.SetOnSphere((float)(x - xoc) * 2 / csize.width, -(float)(y - yoc) * 2 / csize.height); 
				qmdown.SetFrom(qnow); 
			}
			else
			{
				zfRotXdown = zfRotX; 
				zfRotZdown = zfRotZ; 
			}

			break; 

		case M_SEL_STATIONS: 
			{
				vstationactivesel = null; 
				int rdistsq = TN.XSinteractiveSensitivitySQ; 

				for (OneStation vstation : vstations)
				{
					int idistsq = vstation.sqDist(x, y);
					if (idistsq < rdistsq) 
					{
						rdistsq = idistsq;
						vstationactivesel = vstation;
					}
				}
				vstationactive = vstationactivesel;
				if (vstationactive != null)
					repaint(); 
				else
					momotion = M_NONE; 
			}
			break; 

		default: 
			break; 
		}
	}

	/////////////////////////////////////////////
    public void mouseDragged(MouseEvent e) 
	{
		int x = e.getX(); 
		int y = e.getY(); 

		// act on the motion type
		float lambda; 
		switch(momotion)
		{
		case M_DYN_ROTATE: 
			if (!wireframedisplay.miZFixed.isSelected())
			{
				vmdrag.SetOnSphere((float)(x - xoc) * 2 / csize.width, -(float)(y - yoc) * 2 / csize.height); 
				qmdrag.VecRot(vmdown, vmdrag); 
				qnow.Mult(qmdown, qmdrag); 
			}
			else
			{
				zfRotX = zfRotXdown + (float)Math.PI * (x - prevx) / csize.width; 
				zfRotZ = zfRotZdown + (float)Math.PI * (y - prevy) / csize.height; 
				CalcQView(); 
			}


			ReformView(); 
			repaint(); 
			break; 

		case M_DYN_SCALE: 
			lambda = 1.0F + ((float)Math.abs(x - prevx) / csize.width) * 2.0F; 
			if (x > prevx)
				lambda = 1.0F / lambda; 

			diameter = prevdiameter * lambda; 
			coff.SetAlong(lambda, ncoff, ocoff); 

			ReformView(); 
			repaint(); 
			break; 

		case M_DYN_TRANSLATE: 
			{
				float lx = (x - prevx) / xfac; 
				float ly = -(y - prevy) / xfac; 
				coff.SetXYZ(ocoff.x + lx * sxvec.x + ly * syvec.x, 
							ocoff.y + lx * sxvec.y + ly * syvec.y, 
							ocoff.z + lx * sxvec.z + ly * syvec.z); 

				ReformView(); 
				repaint(); 
			}
			break; 

		case M_SEL_STATIONS: 
			{
				OneStation vsaOld = vstationactive; 
				vstationactive = ((vstationactivesel.sqDist(x, y) < TN.XSinteractiveSensitivitySQ) ? vstationactivesel : null);
				if (vstationactive != vsaOld)
				{
					bNeedsRedrawing = true; 
					repaint(); 
				}
			}
			break; 

		case M_NONE: 
			break; 

		default: 
			break; 
		}
	}

	/////////////////////////////////////////////
    public void mouseReleased(MouseEvent e)
	{
		int x = e.getX(); 
		int y = e.getY(); 

		switch(momotion)
		{
		case M_SEL_STATIONS: 
			vstationactivesel = null; 
			vstationactive = null; 
			break; 


		case M_DYN_ROTATE: 
		case M_DYN_SCALE: 
		case M_DYN_TRANSLATE: 
			break; 

		case M_NONE: 
			return; 

		default: 
			break; 
		}

		momotion = M_NONE; 

		bNeedsRedrawing = true; 
		repaint();	// get the labels back. 
	}
}


