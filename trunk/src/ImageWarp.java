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
import java.awt.Color;

import java.util.Vector;
import java.io.IOException;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

////////////////////////////////////////////////////////////////////////////////
class ImageWarp
{
	// this is the background image
	FileAbstraction lbackimageF = null; // if these differ then we need to update
	FileAbstraction backimageF = null;
	BufferedImage backimage = null;
	boolean bBackImageGood = true;

	// this is the image that it renders into in the panel
	Dimension csize;
	BufferedImage backimagedone;
	Graphics2D backimagedoneGraphics;
	boolean bBackImageDoneGood = false; // needs update.
	JPanel foreground;

	SketchGraphics sketchgraphicspanel;
	boolean bMaxBackImage = false;

	AffineTransform currtrans = new AffineTransform();
	AffineTransform currparttrans = new AffineTransform();


	/////////////////////////////////////////////
	ImageWarp(Dimension lcsize, JPanel lforeground)
	{
		csize = lcsize;
		foreground = lforeground;
	}



	/////////////////////////////////////////////
	void SetImageF(FileAbstraction llbackimageF)
	{
		lbackimageF = llbackimageF;

		currtrans.setToIdentity();
		bBackImageGood = false;
		bBackImageDoneGood = false;

		if (sketchgraphicspanel != null)
			sketchgraphicspanel.RedrawBackgroundView();
	}



	/////////////////////////////////////////////
	void SketchBackground(AffineTransform ucurrtrans)
	{
		if (backimageF == null ? (lbackimageF != null) : !backimageF.equals(lbackimageF))
		{
			backimageF = lbackimageF;
			backimage = null;
			if (backimageF != null)
			{
				try
				{
System.out.println(lbackimageF);
					if (lbackimageF != null)
						backimage = ImageIO.read(lbackimageF.localfile);
					if (backimage == null)
					{
						String[] imnames = ImageIO.getReaderFormatNames();
						System.out.println("Image reader format names: ");
						for (int i = 0; i < imnames.length; i++)
                        	System.out.println(imnames[i]);
					}
System.out.println(backimage);
				}
				catch (IOException e)
				{;};
			}
		}

		// make the transformed image
		Dimension lcsize = foreground.getSize();
		if ((backimagedone == null) || (backimagedone.getWidth() != lcsize.width) || (backimagedone.getHeight() != lcsize.height))
		{
			backimagedone = new BufferedImage(lcsize.width, lcsize.height, BufferedImage.TYPE_INT_RGB);
			backimagedoneGraphics = (Graphics2D)backimagedone.getGraphics();
			TN.emitMessage("new backimagedone");
		}

		backimagedoneGraphics.setColor(Color.lightGray);
		backimagedoneGraphics.fillRect(0, 0, backimagedone.getWidth(), backimagedone.getHeight());
		if (backimage == null)
			return;

		// this is where we implement the Max command on background image.
		if (bMaxBackImage)
		{
			TN.emitMessage("making max backimage");
			currparttrans.setToIdentity();
			if (ucurrtrans != null)
			{
				try
				{
					currparttrans.setTransform(ucurrtrans.createInverse());
				}
				catch(NoninvertibleTransformException e)
				{;};
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

		if (ucurrtrans != null)
			currtrans.setTransform(ucurrtrans);
		else
			currtrans.setToIdentity();
		currtrans.concatenate(currparttrans);

		if (backimage != null)
			backimagedoneGraphics.drawRenderedImage(backimage, currtrans);
	}
}



