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
import java.awt.Graphics2D; 
import java.awt.Toolkit; 

import java.util.Vector; 

import java.awt.geom.AffineTransform; 
import java.awt.geom.NoninvertibleTransformException; 

import java.awt.event.MouseListener; 
import java.awt.event.MouseMotionListener; 
import java.awt.event.MouseEvent; 
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Image; 
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import java.io.File;


////////////////////////////////////////////////////////////////////////////////
class ImageWarp implements MouseListener, MouseMotionListener, ImageObserver
{
	final static int M_NONE = 0; 
	final static int M_DYN_SCALE = 4; 
	final static int M_DYN_DRAG = 5; 
	final static int M_DYN_ROT = 6; 

	Dimension csize; 
	float ox = 0.0F; 
	float oy = 0.0F; 
	float scale = 0.0F; 

	File backimageF = null; 

	Image backimageS = null; 
	int backimageW = -1; 
	int backimageH = -1; 
	BufferedImage backimage = null; 
	boolean bBackImageGood = true; 

	BufferedImage backimagedone; 
	Graphics2D backimagedoneGraphics;
	boolean bBackImageDoneGood = false; // needs update.  

	JPanel foreground; 
	SketchGraphics sketchgraphicspanel; 
	boolean bMaxBackImage = false; 

	int momotion = M_NONE; 

	AffineTransform orgtrans = new AffineTransform(); 
	AffineTransform mdtrans = new AffineTransform(); 
	AffineTransform currtrans = new AffineTransform(); 
	
	AffineTransform orgparttrans = new AffineTransform(); 
	AffineTransform currparttrans = new AffineTransform(); 

	int prevx; 
	int prevy; 


	/////////////////////////////////////////////
	ImageWarp(Dimension lcsize, JPanel lforeground)
	{
		csize = lcsize; 
		foreground = lforeground; 
	}

	/////////////////////////////////////////////
	void SetImage(Image img) 
	{
		currtrans.setToIdentity(); 
		TN.emitMessage("Image set to " + (img == null ? "null" : img.toString())); 
		backimageS = img; 
		backimageW = -1; 
		backimageH = -1; 
		bBackImageGood = false; 
		bBackImageDoneGood = false; 
		foreground.repaint(); 
	}


	/////////////////////////////////////////////
	void DoBackground(Graphics g, boolean bDisplayBackground, float lox, float loy, float lscale) 
	{
		if (!bDisplayBackground)  
		{
			g.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
			g.fillRect(0, 0, csize.width, csize.height); 
			return; 
		}


		// work through the chain of things as they are drawn.  
		if (!bBackImageGood) 
		{
			if (backimageS == null) 
				return; 

			backimageW = backimageS.getWidth(foreground);
			backimageH = backimageS.getHeight(foreground); 
			if ((backimageW == -1) || (backimageH == -1))  
			{
				TN.emitMessage("height width not ready"); 
				g.setColor(TN.skeBackground); 
				g.fillRect(0, 0, csize.width, csize.height); 
				return; 
			}
			if ((backimage == null) || (backimage.getWidth() != backimageW) || (backimage.getHeight() != backimageH))  
			{
				TN.emitMessage("making new backimage"); 
				backimage = new BufferedImage(backimageW, backimageH, BufferedImage.TYPE_INT_RGB); 
			}
			
			Graphics backimageG = backimage.getGraphics(); 
			backimageG.setColor(TN.skeBackground);
			backimageG.fillRect(0, 0, backimageW, backimageH); 
			bBackImageGood = backimageG.drawImage(backimageS, 0, 0, foreground); 
			if (!bBackImageGood) 
			{
				TN.emitMessage("image not yet good"); 
				g.setColor(TN.skeBackground); 
				g.fillRect(0, 0, csize.width, csize.height); 
				return; 
			} 
		}

		// make the transformed image
		if ((backimagedone == null) || (backimagedone.getWidth() != csize.width) || (backimagedone.getHeight() != csize.height)) 
		{
			backimagedone = (BufferedImage)foreground.createImage(csize.width, csize.height); 
			backimagedoneGraphics = (Graphics2D)backimagedone.getGraphics();
			bBackImageDoneGood = false; 
		}

		if (!bBackImageDoneGood || !((ox == lox) && (oy == loy) && (scale == lscale)))
		{
			ox = lox; 
			oy = loy; 
			scale = lscale; 

		// put in white before returning. (not good, but need white to see the symbols well.)   
		backimagedoneGraphics.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
//			backimagedoneGraphics.setColor(TN.skeBackground); 
			backimagedoneGraphics.fillRect(0, 0, csize.width, csize.height); 



			//backimagedoneGraphics.drawImage(backimage, offx, offy, offx + (int)(csize.width * simwidth), offy + (int)(csize.height * simheight), 0, 0, backimageW, backimageH, null); 
			if ((backimage != null) && bDisplayBackground) 
				backimagedoneGraphics.drawRenderedImage(backimage, currtrans); 
			bBackImageDoneGood = true; 
		}			

		g.drawImage(backimagedone, 0, 0, null); 
	}


	/////////////////////////////////////////////
	File[] Sfimages = new File[3]; 
	Image[] Simages = new Image[3]; 
	int SimagesNE = 0; 

	/////////////////////////////////////////////
	void SetImageF(File lbackimageF, Toolkit toolkit)  
	{
		backimageF = lbackimageF; 
		if (backimageF != null) 
		{
			// first look for images that match.  
			backimageS = null;  
			for (int i = 0; i < Sfimages.length; i++)  
			{
				if ((Sfimages[i] != null) && Sfimages[i].equals(backimageF))  
				{
					backimageS = Simages[i]; 
					break; 
				}
			}

			// image not cached on stack.  make new one and cache.  
			if (backimageS == null)  
			{
				TN.emitMessage("Loading new backimage " + backimageF.toString()); 
				backimageS = toolkit.createImage(backimageF.toString()); 
				if (Simages[SimagesNE] != null) 
					Simages[SimagesNE].flush(); 
				Sfimages[SimagesNE] = backimageF; 
				Simages[SimagesNE] = backimageS; 
				SimagesNE++; 
				if (SimagesNE == Sfimages.length)  
					SimagesNE = 0; 
			}
		}
		else 
			backimageS = null; 

		currtrans.setToIdentity(); 
		bBackImageGood = false; 
		bBackImageDoneGood = false; 
		sketchgraphicspanel.bmainImgValid = false; 
		sketchgraphicspanel.repaint(); 
	}

	/////////////////////////////////////////////
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) 
	{
		if ((infoflags & ImageObserver.ALLBITS) == ImageObserver.ALLBITS)  
		{
			TN.emitMessage("image allbits there"); 
			bBackImageDoneGood = false; 
			sketchgraphicspanel.bmainImgValid = false; 
			sketchgraphicspanel.repaint(); 
			return false; 
		}
		else if (infoflags != ImageObserver.SOMEBITS)  
			TN.emitMessage("image bits " + infoflags + " " + ImageObserver.SOMEBITS + " " + ImageObserver.PROPERTIES + " " + ImageObserver.ERROR); 

		return true; 
	}

	/////////////////////////////////////////////
	// this kicks out an image observer that will update when everything is here.  
	boolean SketchBufferWholeBackImage() 
	{
		// work through the chain of things as they are drawn.  
		if (backimageS == null) 
			return false; 

		int backimageW = backimageS.getWidth(this);
		int backimageH = backimageS.getHeight(this); 
		if ((backimageW == -1) || (backimageH == -1))  
			return false; 

		if ((backimage == null) || (backimage.getWidth() != backimageW) || (backimage.getHeight() != backimageH))  
		{
			// these buffered images should be cached too.  
			TN.emitMessage("making new backimage"); 
			backimage = new BufferedImage(backimageW, backimageH, BufferedImage.TYPE_INT_RGB); 
		}

		Graphics backimageG = backimage.getGraphics(); 
		backimageG.setColor(TN.skeBackground);
		backimageG.fillRect(0, 0, backimageW, backimageH); 
		bBackImageGood = backimageG.drawImage(backimageS, 0, 0, this); 
		return bBackImageGood; 
	}


	/////////////////////////////////////////////
	void SketchBackground(AffineTransform ucurrtrans) 
	{
		// make the transformed image
		if ((backimagedone == null) || (backimagedone.getWidth() != csize.width) || (backimagedone.getHeight() != csize.height)) 
		{
			//backimagedone = (BufferedImage)foreground.createImage(csize.width, csize.height); 
			backimagedone = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_RGB); 
			backimagedoneGraphics = (Graphics2D)backimagedone.getGraphics();
			TN.emitMessage("new backimagedone"); 
		}

		backimagedoneGraphics.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
		backimagedoneGraphics.fillRect(0, 0, csize.width, csize.height); 

		// this is where we implement the Max command on background image.  
		if (bMaxBackImage) 
		{
			TN.emitMessage("making max backimage");  

			try 
			{
			currparttrans.setTransform(ucurrtrans.createInverse()); 
			}
			catch(NoninvertibleTransformException e)
			{
			currparttrans.setToIdentity(); 
			}

			// scale and translate
			float scaw = (float)csize.width / backimage.getWidth(); 
			float scah = (float)csize.height / backimage.getHeight(); 

			if (scaw < scah) 
			{
				currparttrans.scale(scaw, scaw); 
				float tv = (csize.height - scaw * backimage.getHeight()) / (2 * scaw); 
				currparttrans.translate(0.0F, tv); 
			}
			else 
			{
				currparttrans.scale(scah, scah); 
				float th = (csize.width - scah * backimage.getWidth()) / (2 * scah); 
				currparttrans.translate(th, 0.0F); 
			}

			bMaxBackImage = false; 
		}

		currtrans.setTransform(ucurrtrans); 
		currtrans.concatenate(currparttrans); 

		if (backimage != null) 
			backimagedoneGraphics.drawRenderedImage(backimage, currtrans); 
	}



	/////////////////////////////////////////////
	public void mouseMoved(MouseEvent e) {;}
	public void mouseClicked(MouseEvent e) {;}
	public void mouseEntered(MouseEvent e) {;}; 
	public void mouseExited(MouseEvent e) {;}; 


	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)  
	{
		TN.emitMessage("eep"); 
		// if a point is already being dragged, then this second mouse press will delete it.  
		if (momotion != M_NONE)
		{
			momotion = M_NONE; 
			currtrans.setTransform(orgtrans); 
			bBackImageGood = false; 
		    foreground.repaint();
			return; 
		}

		orgtrans.setTransform(currtrans); 
		mdtrans.setToIdentity(); 
		prevx = e.getX(); 
		prevy = e.getY(); 

		if (!e.isMetaDown()) 
		{
			momotion = (e.isShiftDown() ? M_DYN_DRAG : (e.isControlDown() ? M_DYN_SCALE : M_DYN_ROT)); 
			return; 
		}
	}


	/////////////////////////////////////////////
    public void mouseDragged(MouseEvent e) 
	{
		switch (momotion)
		{
		case M_DYN_DRAG: 
		{
			int xv = e.getX() - prevx; 
			int yv = e.getY() - prevy; 
			mdtrans.setToTranslation(xv, yv); 

			bBackImageDoneGood = false; 
			break; 
		}
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

			mdtrans.setToScale(rescalex, rescaley); 
			mdtrans.translate(csize.width * (1.0F - rescalex) / 2, csize.height * (1.0F - rescaley) / 2); 

			bBackImageDoneGood = false; 
			break; 
		}

		case M_DYN_ROT: 
		{
			int vy = e.getY() - prevy; 
			mdtrans.setToRotation((float)vy / csize.height, csize.width / 2, csize.height / 2); 

			bBackImageDoneGood = false; 
			break; 
		}

		case M_NONE: 
		default: 
			return; 
		}
		currtrans.setTransform(mdtrans); 
		currtrans.concatenate(orgtrans); 
		foreground.repaint();  
	}


	/////////////////////////////////////////////
    public void mouseReleased(MouseEvent e)
	{
		mouseDragged(e); 
		momotion = M_NONE; 
	}
}



////////////////////////////////////////////////////////////////////////////////
