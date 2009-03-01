////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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
class SketchPrintPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	double truewidth;
	double trueheight;
	double realpaperscale;

	Rectangle2D printrect;

    // one of these two is used for the printrect
    Rectangle2D subsetrect = null; 
    Rectangle2D windowrectreal = null; 


    double dpmetre; 
	int pixelheight; 
	int pixelwidth; 

	int lastpixfield = 0; 
	
	JTextField tftruesize = new JTextField(); 

	JTextField dpifield = new JTextField("200");
	JTextField tfpixelswidth = new JTextField();
	JTextField tfpixelsheight = new JTextField();

	JTextField tfdefaultdirname = new JTextField();
	JTextField tfdefaultsavename = new JTextField();

	JCheckBox chGrayScale = new JCheckBox("Gray Scale");
	JCheckBox chAntialiasing = new JCheckBox("Antialiasing", true);
	JCheckBox chTransparentBackground = new JCheckBox("Transparent");

	JComboBox cbRenderingQuality = new JComboBox();

	JButton buttatlas = new JButton("Atlas");
	JButton buttpng = new JButton("PNG");
	JButton buttjpg = new JButton("JPG");
	JButton buttsvg = new JButton("SVG"); 
	JButton buttnet = new JButton("NET");
    JButton buttoverlay = new JButton("OVERLAY"); 
	JButton buttresetdir = new JButton("ResetDIR");

	AffineTransform aff = new AffineTransform();

	/////////////////////////////////////////////
	SketchPrintPanel(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay;

		setLayout(new BorderLayout());

		JPanel pan1 = new JPanel(new GridLayout(4, 2)); 

		tftruesize.setEditable(false); 
		tfdefaultdirname.setEditable(false); 
		
		pan1.add(new JLabel("Real dimensions:", JLabel.RIGHT));
		pan1.add(tftruesize);

		pan1.add(new JLabel("dots/inch (1:1000):", JLabel.RIGHT));
		pan1.add(dpifield);

		dpifield.addActionListener(new pixfieldlisten(0)); 
		dpifield.getDocument().addDocumentListener(new pixfieldlisten(0)); 
		tfpixelswidth.addActionListener(new pixfieldlisten(1)); 
		tfpixelsheight.addActionListener(new pixfieldlisten(2)); 

		pan1.add(new JLabel("Pixel dimensions:", JLabel.RIGHT));
		JPanel pan4 = new JPanel(new GridLayout(1, 2)); 
		pan4.add(tfpixelswidth); 
		pan4.add(tfpixelsheight); 
		pan1.add(pan4);
		
		pan1.add(tfdefaultdirname); 
		pan1.add(tfdefaultsavename); 
		
		add(pan1, BorderLayout.NORTH); 
		
		JPanel pan2 = new JPanel(new GridLayout(1, 2));

		JPanel panchb = new JPanel(new GridLayout(0, 1));

		JPanel panbutts = new JPanel(new GridLayout(0, 1)); 

		buttatlas.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ AutoOutputPNG(); } });

		buttpng.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ OutputPNG(); } });

		buttjpg.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e)
				{ OutputJPG(); } });

//		buttsvg.addActionListener(new ActionListener()
//			{ public void actionPerformed(ActionEvent e)
//				{ OutputSVG(); } });

		buttnet.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ UploadPNG(false); } });
		buttoverlay.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ UploadPNG(true); } });

		buttresetdir.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ ResetDIR(true); }	});

		panbutts.add(buttpng);
		panbutts.add(buttjpg);
		//panbutts.add(buttsvg);
		panbutts.add(buttnet);
		panbutts.add(buttoverlay);
		panbutts.add(buttresetdir);
		pan2.add(panbutts);

		panchb.add(chGrayScale);
		panchb.add(chAntialiasing);
		panchb.add(chTransparentBackground);

		cbRenderingQuality.addItem("Quick draw");
		cbRenderingQuality.addItem("Show images");
		cbRenderingQuality.addItem("Update styles");
		cbRenderingQuality.addItem("Full draw");
		panchb.add(cbRenderingQuality);

		panbutts.add(buttatlas); 

		pan2.add(panchb);

		add(pan2, BorderLayout.CENTER); 
	}

	/////////////////////////////////////////////
	void ResetDIR(boolean bresetfromcurrent)
	{
		if (bresetfromcurrent)
			TN.currprintdir = FileAbstraction.MakeCanonical(TN.currentDirectory); // alternatively could use FileAbstraction MakeCurrentUserDirectory()

		tfdefaultdirname.setText(TN.currprintdir.getAbsolutePath()); 

		String bname = sketchdisplay.selectedsubsetstruct.GetFirstSubset(); 
		if (bname == null)
			bname = TN.loseSuffix(sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getName());
		tfdefaultsavename.setText(bname); 
        System.out.println("resetting printdir " + bname + "  " + bresetfromcurrent); 
	}


	/////////////////////////////////////////////
	void UpdatePrintingRectangle(Vec3 sketchLocOffset, double lrealpaperscale, boolean bupdatewindowrect) 
	{
		if (bupdatewindowrect)
        {
        	try
				{ sketchdisplay.printingpanel.windowrectreal = sketchdisplay.sketchgraphicspanel.currtrans.createInverse().createTransformedShape(sketchdisplay.sketchgraphicspanel.windowrect).getBounds(); }
			catch (NoninvertibleTransformException ex)
				{;}
            //System.out.println("made new windowrectreal " + sketchdisplay.printingpanel.windowrectreal.toString()); 
        }

        printrect = (sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP.isEmpty() ? windowrectreal : subsetrect); 
        if (printrect == null)
            return; 
		TN.emitMessage("UpdatePrintingRectangle " + printrect.toString()); 

        //ResetDIR((TN.currprintdir == null));  // initialize
			
		// ignore sketchLocOffset
		realpaperscale = lrealpaperscale; 
		trueheight = printrect.getHeight() / TN.CENTRELINE_MAGNIFICATION / realpaperscale; 
		truewidth = printrect.getWidth() / TN.CENTRELINE_MAGNIFICATION / realpaperscale; 
		tftruesize.setText(String.format("%.3fm x %.3fm", truewidth, trueheight));
		Updatefinalsize(lastpixfield);
	}

	/////////////////////////////////////////////
	boolean Updatefinalsize(int lpixfield)
	{
		if (lpixfield == 0)
		{
			dpmetre = -1.0; 
			try 
				{ dpmetre = Float.parseFloat(dpifield.getText()) * 1000.0 / 25.4; }  // there's some compiler problem when using Double.parseDouble
			catch(NumberFormatException e) 
				{;}
			if (dpmetre <= 0.0)
				return false; 

			pixelheight = (int)Math.ceil(trueheight * dpmetre); 
			pixelwidth = (int)Math.ceil(truewidth * dpmetre); 
			tfpixelswidth.setText(String.valueOf(pixelwidth));
			tfpixelsheight.setText(String.valueOf(pixelheight));
		}
		else 
		{
			int dppix = -1; 
			try 
				{ dppix = Integer.parseInt(lpixfield == 1 ? tfpixelswidth.getText() : tfpixelsheight.getText()); }
			catch(NumberFormatException e) 
				{;}
			if (dppix <= 0)
				return false; 

			if (lpixfield == 1)
			{
				pixelwidth = dppix; 
				dpmetre = pixelwidth / truewidth;
				pixelheight = (int)Math.ceil(trueheight * dpmetre); 
				tfpixelsheight.setText(String.valueOf(pixelheight));
			}
			else
			{
				pixelheight = dppix; 
				dpmetre = pixelheight / trueheight; 	
				pixelwidth = (int)Math.ceil(truewidth * dpmetre); 
				tfpixelswidth.setText(String.valueOf(pixelwidth));
			}
			dpifield.setText(String.valueOf((float)(dpmetre * 25.4 / 1000.0))); 
		}	
		lastpixfield = lpixfield; 
		return true; 
	}

	/////////////////////////////////////////////
	class pixfieldlisten implements ActionListener, DocumentListener
	{
		int pixfield; 
		pixfieldlisten(int lpixfield)
		{
			pixfield = lpixfield; 
		}
		public void actionPerformed(ActionEvent e)
		{
			Updatefinalsize(pixfield); 
		}
		
		public void insertUpdate(DocumentEvent e) 
		{ 
			//if (e.source.hasFocus())
                Updatefinalsize(pixfield); 
		}
		public void removeUpdate(DocumentEvent e) 
		{
			//if (e.object.hasFocus())
    			Updatefinalsize(pixfield); 
		}
		public void changedUpdate(DocumentEvent e) 
		{
			//if (e.object.hasFocus())
    			Updatefinalsize(pixfield); 
		}
	};

	/////////////////////////////////////////////
	void OutputSVG()
	{
		System.out.println("NNOOTT  Outting SVG thing"); 
	}


	//for (OneSArea osa : vsareas)
	//pwqFramedSketch(ga, osa, sketchlinestyle);

	// also need to scan through and load all the sketches; and check
	// which ones are built to the style.
	// Need a proper overview, manageing how we select these styles and rebuild all on them.
	// Must have an idea about how much is built on each sketch, so can operate these builds independently.
	//		String sfstyle = "";

	// the stages to be broken out are:
	//ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
	//pd.SetZaltsFromCNodesByInverseSquareWeight(sketchgraphicspanel.tsketch); // passed in for the zaltlo/hi values
	//sketchgraphicspanel.UpdateSAreas();  // this selects the symbol subset style; so we should find those.
	//sketchgraphicspanel.UpdateSymbolLayout(true);



	/////////////////////////////////////////////
	void OutputJPG()
	{
		String[] wfnlist = ImageIO.getWriterFormatNames();
		for (int i = 0; i < wfnlist.length; i++)
			System.out.println("JJJJJ  " + wfnlist[i]);
System.out.println("\nSORRY currently disabled");
	}

	/////////////////////////////////////////////
    BufferedImage RenderBufferedImage()
    {
        BufferedImage bi = new BufferedImage(pixelwidth, pixelheight, (chGrayScale.isSelected() ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_INT_ARGB));
        Graphics2D g2d = bi.createGraphics();
        if (chTransparentBackground.isSelected())
        {
            Composite tcomp = g2d.getComposite();  // preserve the composite in order to clear it
            System.out.println("What is composite: " + tcomp.toString() + "  ");
            //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0F));
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fill(new Rectangle(0, 0, pixelwidth, pixelheight));
            g2d.setComposite(AlphaComposite.SrcOver);
        }
        else
        {
            g2d.setColor(Color.white);  // could make it a different colour
            g2d.fill(new Rectangle(0, 0, pixelwidth, pixelheight));
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (chAntialiasing.isSelected() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF));

// work out the relative translations to the other subsets
        // set the pre transformation
        aff.setToTranslation(pixelwidth / 2, pixelheight / 2);
        double scchange = printrect.getWidth() / (pixelwidth - 2);
        aff.scale(1.0F / scchange, 1.0F / scchange);
        aff.translate(-(printrect.getX() + printrect.getWidth() / 2), -(printrect.getY() + printrect.getHeight() / 2));
        g2d.setTransform(aff);

        GraphicsAbstraction ga = new GraphicsAbstraction(g2d);
        ga.printrect = printrect;

        sketchdisplay.sketchgraphicspanel.tsketch.paintWqualitySketch(ga, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex(), sketchdisplay.sketchlinestyle.subsetattrstylesmap);

        // flatten all the alpha values (can't find any other way to do this than by pixel bashing)
        WritableRaster wr = bi.getAlphaRaster(); 
        if (chTransparentBackground.isSelected() && (wr != null) && (wr.getNumBands() == 1))
        {
            for (int ix = 0; ix < wr.getWidth(); ix++) 
            for (int iy = 0; iy < wr.getHeight(); iy++)
            {
                if  (wr.getSample(ix, iy, 0) != 0)
                    wr.setSample(ix, iy, 0, 255); 
            }
        }
                    
        return bi; 
    }


	/////////////////////////////////////////////
	void AutoOutputPNG()
	{
		int irenderingquality = cbRenderingQuality.getSelectedIndex(); 
		// loops through all selected subsets and creates an image for each one
		FileAbstraction fa = FileAbstraction.MakeDirectoryAndFileAbstraction(TN.currprintdir, tfdefaultsavename.getText());
		fa = fa.SaveAsDialog(false, sketchdisplay); 
		if (fa == null)
			return; 
		ResetDIR(false);
		
		List<String> lsubsets = new ArrayList<String>(); 
		lsubsets.addAll(sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP); 
		OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
		for (String lsubset : lsubsets)		
		{
			sketchdisplay.selectedsubsetstruct.UpdateSingleSubsetSelection(lsubset); 
			// done in update sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realpaperscale); 

			// then build it
			if ((irenderingquality == 2) || (irenderingquality == 3))
				sketchdisplay.mainbox.UpdateSketchFrames(tsketch, (cbRenderingQuality.getSelectedIndex() == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS));

            BufferedImage bi = RenderBufferedImage(); 

			String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
			try
			{
				FileAbstraction lfa = FileAbstraction.MakeDirectoryAndFileAbstraction(TN.currprintdir, lsubset + ".png"); 
				TN.emitMessage("Writing file " + lfa.getAbsolutePath());
				ImageIO.write(bi, ftype, lfa.localfile);
			}
			catch (IOException e)
			{ e.printStackTrace(); }
		}
		
		// finalize (return to normality)
		sketchdisplay.selectedsubsetstruct.UpdateTreeSubsetSelection(sketchdisplay.subsetpanel.pansksubsetstree); 
		sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
	}

	/////////////////////////////////////////////
	void OutputPNG()
	{
		int irenderingquality = cbRenderingQuality.getSelectedIndex(); 

		// dispose of finding the file first
		FileAbstraction fa = FileAbstraction.MakeDirectoryAndFileAbstraction(TN.currprintdir, tfdefaultsavename.getText());
		fa = fa.SaveAsDialog(false, sketchdisplay); 
		if (fa == null)
			return; 
		ResetDIR(false);

		// then build it
		if ((irenderingquality == 2) || (irenderingquality == 3))
			sketchdisplay.mainbox.UpdateSketchFrames(sketchdisplay.sketchgraphicspanel.tsketch, (irenderingquality == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS));

        BufferedImage bi = RenderBufferedImage(); 

		String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
		try
		{
			TN.emitMessage("Writing file " + fa.getAbsolutePath() + " with type " + ftype);
			ImageIO.write(bi, ftype, fa.localfile);
		}
		catch (Exception e)
			{ e.printStackTrace(); }
	}


	/////////////////////////////////////////////
	void UploadPNG(boolean btomjgoverlay)
	{
		int irenderingquality = cbRenderingQuality.getSelectedIndex(); 
        OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
		// then build it
		if ((irenderingquality == 2) || (irenderingquality == 3))
			sketchdisplay.mainbox.UpdateSketchFrames(tsketch, (irenderingquality == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS));

        BufferedImage bi = RenderBufferedImage(); 

//		String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
		try
		{
            //FileAbstraction.postData("http://seagrass.goatchurch.org.uk/~mjg/cgi-bin/uploadtiles.py", bi);
            //String response = FileAbstraction.postData("http://10.0.0.10/expo-cgi-bin/tunserv.py", tfdefaultsavename.getText(), bi);
			//TN.emitMessage("Writing file " + fa.getAbsolutePath() + " with type " + ftype);
            String filename = tfdefaultsavename.getText(); 
            FileAbstraction fimageas; 
            if (btomjgoverlay)
                FileAbstraction.upmjgirebyoverlay(bi, filename, dpmetre / realpaperscale, printrect.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x, -printrect.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y); 
            else
            {
                fimageas = FileAbstraction.uploadImage("tileimage", filename + ".png", bi, null);
                TN.emitMessage("Image was saved as :" + fimageas.getPath() + ":"); 
            }
		}
		catch (Exception e)
			{ e.printStackTrace(); }
	}
}


