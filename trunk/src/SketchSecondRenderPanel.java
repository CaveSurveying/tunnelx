////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2009  Julian Todd.
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
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Graphics2D;
import java.awt.Graphics;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster; 
import java.awt.image.DataBuffer; 
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.AlphaComposite; 
import java.awt.Composite; 
import java.awt.Rectangle;
import java.awt.Color;

import javax.imageio.ImageIO;

import java.util.List; 
import java.util.ArrayList; 



/////////////////////////////////////////////
class SketchSecondRender extends JPanel
{
	SketchDisplay sketchdisplay;
	JButton buttswapview = new JButton("SwapView");
	JButton buttcopyview = new JButton("CopyView");
	JButton buttrefresh = new JButton("Refresh");

	AffineTransform crendtrans = new AffineTransform();
	AffineTransform rendtrans = new AffineTransform();
    AffineTransform satrans = new AffineTransform();
    boolean bredrawbackground = false; 

    /////////////////////////////////////////////
    class SketchSecondRenderPanel extends JPanel // implements MouseListener, MouseMotionListener, MouseWheelListener
    {
        Image mainImg = null;
        Graphics2D g2dimage = null;
        GraphicsAbstraction gaimage = null;  
    	Dimension csize = new Dimension(0, 0);

        public void paintComponent(Graphics g)
        {
            // test if resize has happened because we are rebuffering things
            if ((mainImg == null) || (getSize().height != csize.height) || (getSize().width != csize.width))
            {
                csize.width = getSize().width;
                csize.height = getSize().height;
                mainImg = createImage(csize.width, csize.height);
                g2dimage = (Graphics2D)mainImg.getGraphics();
                gaimage = new GraphicsAbstraction(g2dimage);  
                bredrawbackground = true; 
    			satrans.setTransform(g2dimage.getTransform());
            }

            if (bredrawbackground)
            {   
System.out.println("redrawing22222back"); 
                OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
        
                // simply redraw the back image into the front, transformed for fast dynamic rendering.
                g2dimage.setColor(SketchLineStyle.blankbackimagecol);
                g2dimage.fillRect(0, 0, csize.width, csize.height);
    
                // the frame image types -- which will replace the old style
                OnePath fop = tsketch.opframebackgrounddrag; 
                if ((fop != null) && (fop.plabedl != null) && (fop.plabedl.sketchframedef != null) && ((fop.plabedl.sketchframedef.pframeimage != null) || (fop.plabedl.sketchframedef.pframesketch != null)))
                {
        			SketchFrameDef sketchframedef = fop.plabedl.sketchframedef;
                    gaimage.transform(crendtrans);
        			gaimage.transform(sketchframedef.pframesketchtrans);

                    if (sketchframedef.pframeimage != null)
                        gaimage.drawImage(sketchframedef.pframeimage.GetImage(true));
                    else
                        sketchframedef.pframesketch.paintWqualitySketch(gaimage, Math.max(2, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex()), null);

                    //backimagedoneGraphics.setTransform(ucurrtrans);
                    //ga.drawPath(sketchgraphicspanel.tsketch.opframebackgrounddrag, SketchLineStyle.framebackgrounddragstyleattr); 
                    g2dimage.setTransform(satrans);
                }

                gaimage.transform(crendtrans);
                g2dimage.setFont(sketchdisplay.sketchlinestyle.defaultfontlab);
        
                boolean bHideMarkers = !sketchdisplay.miShowNodes.isSelected(); 
                int stationnamecond = (sketchdisplay.miStationNames.isSelected() ? 1 : 0) + (sketchdisplay.miStationAlts.isSelected() ? 2 : 0); 
                tsketch.paintWbkgd(gaimage, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, stationnamecond, tsketch.vpaths, null, tsketch.vsareas, tsketch.vnodes); 
    
    			g2dimage.setTransform(satrans);
                bredrawbackground = false; 
            }

            Graphics2D g2d = (Graphics2D)g; 
			g.drawImage(mainImg, 0, 0, null);

            // draw the elevation arrow onto here
            if (sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct)
            {
                GraphicsAbstraction ga = new GraphicsAbstraction(g2d); 
                ga.transform(crendtrans);
                ga.drawShape(sketchdisplay.sketchgraphicspanel.elevarrow, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]);  
            }
        }

        SketchSecondRenderPanel()
        {
    		super(false); // not doublebuffered
        }

    }


    /////////////////////////////////////////////
	SketchSecondRender(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay;
		setLayout(new BorderLayout());
        JPanel pan1 = new JPanel(new GridLayout(0, 3)); 
		pan1.add(buttswapview);
		pan1.add(buttcopyview); 
        pan1.add(buttrefresh);

		buttswapview.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ SwapCopyView(true); } });
		buttrefresh.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ bredrawbackground = true;  repaint(); } });

		add(new SketchSecondRenderPanel(), BorderLayout.CENTER); 
		add(pan1, BorderLayout.SOUTH); 
    }


    /////////////////////////////////////////////
    // the window view is clipped (translated) to the centre of the panel
    void SwapCopyView(boolean bswap)
    {
        if (bswap)
        {
            crendtrans.setTransform(rendtrans); 
		    rendtrans.setTransform(sketchdisplay.sketchgraphicspanel.currtrans);
            sketchdisplay.sketchgraphicspanel.currtrans.setTransform(crendtrans); 
            sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
        }
        else
		    rendtrans.setTransform(sketchdisplay.sketchgraphicspanel.currtrans);

        // will translate by the centreof the 
        crendtrans.setToTranslation((getSize().width - sketchdisplay.sketchgraphicspanel.csize.width) / 2, 
                                    (getSize().height - sketchdisplay.sketchgraphicspanel.csize.height) / 2);  

        crendtrans.concatenate(rendtrans);
        bredrawbackground = true; 
        repaint(); 
    }

    /////////////////////////////////////////////
    void Update(boolean btabbingchanged)
    {
        if (btabbingchanged)
        {
            System.out.println("UpdateSecondRender"); 
            bredrawbackground = true; 
            repaint(); 
        }
    }
}

