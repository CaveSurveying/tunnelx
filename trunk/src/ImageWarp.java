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

import java.io.IOException;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
	void PreConcatBusiness(AffineTransform mdtrans)
	{
		currparttrans.preConcatenate(mdtrans);
	}



	/////////////////////////////////////////////
	ImageWarp(Dimension lcsize, JPanel lforeground)
	{
		csize = lcsize;
		foreground = lforeground;
	}


	/////////////////////////////////////////////
	void SketchBackground(AffineTransform ucurrtrans)
	{
		if (backimageF == null ? (lbackimageF != null) : !backimageF.equals(lbackimageF))
		{
			backimageF = lbackimageF;
			backimage = null;
			if (backimageF != null)
				backimage = backimageF.GetImage(false);
		}

		// make the transformed image for the view window
		Dimension lcsize = foreground.getSize();
		if ((backimagedone == null) || (backimagedone.getWidth() != lcsize.width) || (backimagedone.getHeight() != lcsize.height))
		{
			backimagedone = new BufferedImage(lcsize.width, lcsize.height, BufferedImage.TYPE_INT_RGB);
			backimagedoneGraphics = (Graphics2D)backimagedone.getGraphics();
			TN.emitMessage("new backimagedone");
		}

		backimagedoneGraphics.setColor(SketchLineStyle.blankbackimagecol);
		backimagedoneGraphics.fillRect(0, 0, backimagedone.getWidth(), backimagedone.getHeight());

		// the frame image types -- which will replace the old style
		OnePath fop = sketchgraphicspanel.tsketch.opframebackgrounddrag; 
		if ((fop != null) && (fop.plabedl != null) && (fop.plabedl.sketchframedef != null) && ((fop.plabedl.sketchframedef.pframeimage != null) || (fop.plabedl.sketchframedef.pframesketch != null)))
		{
			// could potentially trim it
			SketchFrameDef sketchframedef = fop.plabedl.sketchframedef;
			SubsetAttrStyle sksas = null; 
            if (sketchframedef.pframesketch != null)
			{
				// calculate the state of mapping we should have in the frame sketch and compare it			
				sksas = sketchgraphicspanel.sketchdisplay.sketchlinestyle.subsetattrstylesmap.get(sketchframedef.sfstyle);
				if (sksas == null)
					sksas = sketchgraphicspanel.sketchdisplay.sketchlinestyle.subsetattrstylesmap.get("default");
				if ((sksas != null) && ((sksas != sketchframedef.pframesketch.sksascurrent) || !sketchframedef.pframesketch.submappingcurrent.equals(sketchframedef.submapping)))
				{
					TN.emitMessage("-- Resetting sketchstyle to Frame thing " + sksas.stylename + " during ImageWarp");
					int scchangetyp = sketchframedef.pframesketch.SetSubsetAttrStyle(sksas, sketchframedef);
					SketchGraphics.SketchChangedStatic(scchangetyp, sketchframedef.pframesketch, null);
					assert (sksas == sketchframedef.pframesketch.sksascurrent);

					// if iproper == SketchGraphics.SC_UPDATE_ALL (not SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS)
					// then it could do it as through a window so that not the whole thing needs redoing.
					sketchframedef.pframesketch.UpdateSomething(SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS, false);
					SketchGraphics.SketchChangedStatic(SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS, sketchframedef.pframesketch, null);
				}
			}

			AffineTransform satrans = backimagedoneGraphics.getTransform();
			
			GraphicsAbstraction ga = new GraphicsAbstraction(backimagedoneGraphics); 
			currtrans = sketchframedef.MakeVertplaneTransform(ucurrtrans, fop); // identity if normal plane
			if (currtrans != null)   // if not an edge on case
			{
				currtrans.concatenate(sketchframedef.pframesketchtrans);
				ga.transform(currtrans); 
				
				if (sketchframedef.pframeimage != null)
					ga.drawImage(sketchframedef.SetImageWidthHeight());
				else if (sketchframedef.sfelevrotdeg == 0.0)
					sketchframedef.pframesketch.paintWqualitySketch(ga, Math.max(2, sketchgraphicspanel.sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex()), null);
				else
					sketchframedef.paintWelevSketch(ga, sksas);
			}
			
			// draw the controlling path in orange
			backimagedoneGraphics.setTransform(ucurrtrans);
			ga.drawPath(sketchgraphicspanel.tsketch.opframebackgrounddrag, SketchLineStyle.framebackgrounddragstyleattr); 

			backimagedoneGraphics.setTransform(satrans);
			return; // bail out now we've done the new back image
		}
		if (backimage == null)
			return;
		TN.emitWarning("Shouldn't get here - dead backgroundimage code"); 
	}
}



