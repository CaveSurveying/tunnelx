////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JPanel;
import javax.swing.JButton; 
import javax.swing.JCheckBox; 
import javax.swing.JTextField; 

import java.awt.Dimension; 
import java.awt.Graphics; 
import java.awt.Graphics2D; 

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


////////////////////////////////////////////////////////////////////////////////
class ImageWarp implements MouseListener, MouseMotionListener
{
	final static int M_NONE = 0; 
	final static int M_DYN_SCALE = 4; 
	final static int M_DYN_DRAG = 5; 
	final static int M_DYN_ROT = 6; 

	Dimension csize; 
	float ox = 0.0F; 
	float oy = 0.0F; 
	float scale = 0.0F; 

	Image backimageS = null; 
	int backimageW = -1; 
	int backimageH = -1; 
	BufferedImage backimage = null; 
	boolean bBackImageGood = true; 

	BufferedImage backimagedone; 
	Graphics2D backimagedoneGraphics;
	boolean bBackImageDoneGood = false; // needs update.  

	JPanel foreground; 
	int momotion = M_NONE; 

	AffineTransform orgtrans = new AffineTransform(); 
	AffineTransform mdtrans = new AffineTransform(); 
	AffineTransform currtrans = new AffineTransform(); 
	
	AffineTransform orgparttrans = new AffineTransform(); 
	AffineTransform currparttrans = new AffineTransform(); 

	int prevx; 
	int prevy; 

	ImageWarp(Dimension lcsize, JPanel lforeground)
	{
		csize = lcsize; 
		foreground = lforeground; 
	}

	void SetImage(Image img) 
	{
		currtrans.setToIdentity(); 

		if (img != null) 
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
		// work through the chain of things as they are drawn.  
		if (!bBackImageGood) 
		{
			if (backimageS == null) 
				return; 

			backimageW = backimageS.getWidth(foreground);
			backimageH = backimageS.getHeight(foreground); 
			if ((backimageW == -1) || (backimageH == -1))  
			{
				System.out.println("height width not ready"); 
				g.setColor(TN.skeBackground); 
				g.fillRect(0, 0, csize.width, csize.height); 
				return; 
			}
			if ((backimage == null) || (backimage.getWidth() != backimageW) || (backimage.getHeight() != backimageH))  
			{
				System.out.println("making new backimage"); 
				backimage = new BufferedImage(backimageW, backimageH, BufferedImage.TYPE_INT_RGB); 
			}
			
			Graphics backimageG = backimage.getGraphics(); 
			backimageG.setColor(TN.skeBackground);
			backimageG.fillRect(0, 0, backimageW, backimageH); 
			bBackImageGood = backimageG.drawImage(backimageS, 0, 0, foreground); 
			if (!bBackImageGood) 
			{
				System.out.println("image not yet good"); 
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
	void DoBackground(Graphics g, AffineTransform ucurrtrans, boolean bDisplayBackground) 
	{
		currtrans.setTransform(ucurrtrans); 
		currtrans.concatenate(currparttrans); 
		DoBackground(g, bDisplayBackground, 0.0F, 0.0F, 1.0F);  
	}

	/////////////////////////////////////////////
	public void mouseMoved(MouseEvent e) {;}
	public void mouseClicked(MouseEvent e) {;}
	public void mouseEntered(MouseEvent e) {;}; 
	public void mouseExited(MouseEvent e) {;}; 


	AffineTransform ident = new AffineTransform(); 

	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)  
	{
System.out.println("eep"); 
		// if a point is already being dragged, then this second mouse press will delete it.  
		if (momotion != M_NONE)
		{
			momotion = M_NONE; 
			currtrans.setTransform(orgtrans); 
			bBackImageDoneGood = false; 
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
