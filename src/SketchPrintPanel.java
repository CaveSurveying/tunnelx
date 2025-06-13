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

	JTextField dpifield = new JTextField("400");
	JTextField tfpixelswidth = new JTextField();
	JTextField tfpixelsheight = new JTextField();

	JTextField tfdefaultdirname = new JTextField();
	JTextField tfdefaultsavename = new JTextField();

	JCheckBox chGrayScale = new JCheckBox("Gray Scale");
	JComboBox cbBitmaptype = new JComboBox();
	JCheckBox chAntialiasing = new JCheckBox("Antialiasing", true);   // default true as useful for geotiffs
	JCheckBox chTransparentBackground = new JCheckBox("Transparent", false);

	JComboBox cbRenderingQuality = new JComboBox();
	//JTextField tfespgstring = new JTextField("EPSG:32630"); // UK UTM30N
	JTextField tfespgstring = new JTextField("EPSG:32633"); // austria UTM33N

	JButton buttatlas = new JButton("Atlas");
	JButton buttpng = new JButton("PNG");
	JButton buttjpg = new JButton("JPG");
	JButton buttsvg = new JButton("SVG"); 
	JButton buttsvgnew = new JButton("SVGnew"); 
	JButton buttnet = new JButton("NET");
	JButton buttbgs = new JButton("BGS");  // British Geological Survey output
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
		//dpifield.getDocument().addDocumentListener(new pixfieldlisten(0)); 
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
				{ OutputIMG("png", cbRenderingQuality.getSelectedIndex(), false); } });

		buttsvg.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ OutputIMG("svg", cbRenderingQuality.getSelectedIndex(), false); } });
		buttsvgnew.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ OutputIMG("svgnew", cbRenderingQuality.getSelectedIndex(), false); } });
        buttbgs.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e)
				{ OutputIMG("bgs", cbRenderingQuality.getSelectedIndex(), false); } });

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
		//panbutts.add(buttjpg);
		panbutts.add(buttsvg);
		panbutts.add(buttsvgnew);
		panbutts.add(buttnet);
        panbutts.add(buttbgs);
		panbutts.add(buttoverlay);
		panbutts.add(buttresetdir);
		//panbutts.add(buttatlas); 
		pan2.add(panbutts);

		cbBitmaptype.addItem("RGB colours");
		cbBitmaptype.addItem("Grey scale");
		cbBitmaptype.addItem("Two tone");
		panchb.add(cbBitmaptype);
		panchb.add(chAntialiasing);
		panchb.add(chTransparentBackground);

        tfespgstring.setToolTipText("ESPG code 32630=UTM30N(uk), 32633=URM33N(austria)"); 
        panchb.add(tfespgstring); 
        
		cbRenderingQuality.addItem("Quick draw");
		cbRenderingQuality.addItem("Show images");
		cbRenderingQuality.addItem("Update styles");
		cbRenderingQuality.addItem("Full draw");
		panchb.add(cbRenderingQuality);

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
	void UpdatePrintingRectangle(Vec3 sketchLocOffset, double lrealposterpaperscale, boolean bupdatewindowrect) 
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
		realpaperscale = TN.defaultrealposterpaperscale; // was lrealposterpaperscale which actually applies to the included edges and we're used to the 1000 fold scale in this field
		trueheight = printrect.getHeight() / TN.CENTRELINE_MAGNIFICATION / realpaperscale; 
		truewidth = printrect.getWidth() / TN.CENTRELINE_MAGNIFICATION / realpaperscale; 
		tftruesize.setText(String.format("%.3fm x %.3fm", truewidth, trueheight));
		Updatefinalsize(lastpixfield);
	}
	/////////////////////////////////////////////
	static int aaa = 0;
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
	boolean OutputSVG(FileAbstraction fa) throws IOException
	{
        LineOutputStream los = new LineOutputStream(fa, "UTF-8"); 
        
        //new SVGPaths(los, sketchdisplay.sketchgraphicspanel.tsketch.vpaths); 

        SvgGraphics2D svgg2d = new SvgGraphics2D(los, (chTransparentBackground.isSelected() ? "#dddddd" : null), sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getName());
        if (sketchdisplay.miJigsawContour.isSelected())
            svgg2d.jigsawareaoffset = sketchdisplay.ztiltpanel.jigsawareaoffset;
        else
            svgg2d.jigsawareaoffset = null;
        GraphicsAbstraction ga = new GraphicsAbstraction(svgg2d); 
        
        float scalefactor = Float.parseFloat(dpifield.getText()); 

double rx0 = printrect.getX()/TN.CENTRELINE_MAGNIFICATION + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x; 
double ry0 = -printrect.getY()/TN.CENTRELINE_MAGNIFICATION + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y; 
double rwidth = printrect.getWidth()/TN.CENTRELINE_MAGNIFICATION; 
double rheight = printrect.getHeight()/TN.CENTRELINE_MAGNIFICATION; 
List<String> cmds = FileAbstraction.GdalTranslateCommand(fa, rx0, ry0, rwidth, rheight, tfespgstring.getText()); 

        svgg2d.writeheader((float)printrect.getX(), (float)printrect.getY(), (float)printrect.getWidth(), (float)printrect.getHeight(), scalefactor, String.join(" ", cmds)); 

        
        sketchdisplay.sketchgraphicspanel.tsketch.paintWqualitySketch(ga, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex(), sketchdisplay.sketchlinestyle.subsetattrstylesmap);
        svgg2d.writefooter(); 

        los.close(); 
        return true; 
	}

	/////////////////////////////////////////////
	boolean OutputSVGnew(FileAbstraction fa) throws IOException
	{
        try
        {
        float scalefactor = Float.parseFloat(dpifield.getText()); 
        SVGnew svgnew = new SVGnew(fa, chTransparentBackground.isSelected(), scalefactor, printrect, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex(), sketchdisplay.sketchlinestyle.subsetattrstylesmap); 
        svgnew.DrawSketch(sketchdisplay.sketchgraphicspanel.tsketch); 
        svgnew.los.close(); 
        return true; 
        }
        catch (IOException io) {;}
        return false; 
	}

	/////////////////////////////////////////////
	BufferedImage RenderBufferedImage(int irenderingquality)
    {
		int ibitmaptype = cbBitmaptype.getSelectedIndex();
		int imageType = (ibitmaptype == 0 ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_USHORT_GRAY);
		// use of IndexColorModel and TYPE_BYTE_BINARY doesn't work.  pixel bash the grey scales into a two tone later
		BufferedImage bi = new BufferedImage(pixelwidth, pixelheight, imageType);
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

		sketchdisplay.sketchgraphicspanel.tsketch.paintWqualitySketch(ga, irenderingquality, sketchdisplay.sketchlinestyle.subsetattrstylesmap);

        // flatten all the alpha values (can't find any other way to do this than by pixel bashing)
		WritableRaster awr = bi.getAlphaRaster();
		//srcRaster.getNumBands()
		if (ibitmaptype == 2)
		{
			WritableRaster wr = bi.getRaster();
			for (int ix = 0; ix < wr.getWidth(); ix++)
			for (int iy = 0; iy < wr.getHeight(); iy++)
			{
				if (wr.getSample(ix, iy, 0) < 65000)  // two bytes
					wr.setSample(ix, iy, 0, 0);
			}
		}
		else if (chTransparentBackground.isSelected() && (awr != null) && (awr.getNumBands() == 1))
        {
			for (int ix = 0; ix < awr.getWidth(); ix++) 
            for (int iy = 0; iy < awr.getHeight(); iy++)
            {
                if (awr.getSample(ix, iy, 0) != 0)
                    awr.setSample(ix, iy, 0, 255); 
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
		fa = fa.SaveAsDialog(SvxFileDialog.FT_BITMAP, sketchdisplay, false); 
		if (fa == null)
			return; 
		ResetDIR(false);
		
		List<String> lsubsets = new ArrayList<String>(); 
		lsubsets.addAll(sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP); 
		OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
		for (String lsubset : lsubsets)		
		{
			sketchdisplay.selectedsubsetstruct.UpdateSingleSubsetSelection(lsubset); 
			// done in update sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realposterpaperscale); 

			// then build it

            BufferedImage bi = RenderBufferedImage(irenderingquality); 

			String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
			try
			{
				FileAbstraction lfa = FileAbstraction.MakeDirectoryAndFileAbstraction(TN.currprintdir, lsubset + TN.SUFF_PNG); 
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
    boolean OutputBGS(FileAbstraction fa) throws IOException
    {
        System.out.println("BGS here"); 
        LineOutputStream los = new LineOutputStream(fa, "UTF-8"); 
        
        boolean bxml = false; 
        if (bxml)
        {
            los.WriteLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            los.WriteLine(TNXML.xcomopen(0, "survexlines"));
            los.WriteLine(TNXML.xcomtext(1, "title", "Example"));
            
            OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
            for (OnePath op : tsketch.vpaths)
            {
                if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
                {
                    double x1 = op.pnstart.pn.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x; 
                    double y1 = -op.pnstart.pn.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y;
                    double z1 = op.pnstart.zalt / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.z;
                    double x2 = op.pnend.pn.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x; 
                    double y2 = -op.pnend.pn.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y;
                    double z2 = op.pnend.zalt / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.z;
                    los.WriteLine(TNXML.xcom(1, "line", "pos1", String.format("SD %.0f %.0f", x1, y1), "alt1", String.format("%.0f", z1), 
                                                        "pos2", String.format("SD %.0f %.0f", x2, y2), "alt2", String.format("%.0f", z2), 
                                                        "lab1", op.pnstart.pnstationlabel, "lab2", op.pnend.pnstationlabel));
                }
            }
            los.WriteLine(TNXML.xcomclose(0, "survexlines"));
        }
        
        // csv type (yes, we have to rewrite the file name to get this done, and the SD offset is hard-coded for three counties)
        else
        {
            los.WriteLine("easting1,northing1,alt1,lab1,easting2,northing2,alt2,lab2");
            OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
            for (OnePath op : tsketch.vpaths)
            {
                if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
                {
                    double x1 = op.pnstart.pn.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x; 
                    double y1 = -op.pnstart.pn.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y;
                    double z1 = op.pnstart.zalt / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.z;
                    double x2 = op.pnend.pn.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x; 
                    double y2 = -op.pnend.pn.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y;
                    double z2 = op.pnend.zalt / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.z;
                    los.Write(String.format("%.0f,%.0f,%.0f,\"%s\",", x1+300000, y1+400000, z1, op.pnstart.pnstationlabel));  
                    los.WriteLine(String.format("%.0f,%.0f,%.0f,\"%s\"", x2+300000, y2+400000, z2, op.pnend.pnstationlabel));  
                }
            }
        }
        los.close(); 
        return true; 
    }

	/////////////////////////////////////////////
	// irenderingquality = 0 Quick draw, 1 Show images, 2 Update styles, 3 Full draw
    // stype = "png", "svg", "svgnew", "bgs"
	boolean OutputIMG(String stype, int irenderingquality, boolean bAuto)
	{
System.out.println("eeeee " + stype); 
		// dispose of finding the file first
		FileAbstraction fa = FileAbstraction.MakeDirectoryAndFileAbstraction(TN.currprintdir, tfdefaultsavename.getText());
TN.emitMessage("DSN: " + tfdefaultsavename.getText() + "  " + irenderingquality);
		fa = fa.SaveAsDialog((!stype.equals("png") ? SvxFileDialog.FT_VECTOR : SvxFileDialog.FT_BITMAP), sketchdisplay, bAuto);   // this is where the default .png setting is done
		if (fa == null) {
			System.out.println("nooo " + stype); 
			return false; 
		}
		ResetDIR(false);

		// we have to make it as far as the areas so that we can filter by their subsets and render the symbols only in those which apply
		sketchdisplay.mainbox.UpdateSketchFrames(sketchdisplay.sketchgraphicspanel.tsketch, SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS);
		String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
TN.emitMessage("ftype: " + ftype);

		try
		{
			if (ftype.equals("svg"))
			{
				// then build it
				if ((irenderingquality == 2) || (irenderingquality == 3))
					sketchdisplay.mainbox.UpdateSketchFrames(sketchdisplay.sketchgraphicspanel.tsketch, (irenderingquality == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS));
                if (stype.equals("svgnew"))
                    return OutputSVGnew(fa);
                return OutputSVG(fa);
			}
			if (stype.equals("bgs") && ftype.equals("xml"))
                return OutputBGS(fa);

			BufferedImage bi = RenderBufferedImage(irenderingquality); 

			TN.emitMessage("Writing file " + fa.getAbsolutePath() + " with type " + ftype);
			ImageIO.write(bi, ftype, fa.localfile);

// convert to geotiff file            
double rx0 = printrect.getX()/TN.CENTRELINE_MAGNIFICATION + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x; 
double ry0 = -printrect.getY()/TN.CENTRELINE_MAGNIFICATION + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y; 
double rwidth = printrect.getWidth()/TN.CENTRELINE_MAGNIFICATION; 
double rheight = printrect.getHeight()/TN.CENTRELINE_MAGNIFICATION; 
String espgstring = tfespgstring.getText(); 
//FileAbstraction.RunGdalTranslate(fa, rx0, ry0, rwidth, rheight, espgstring); 
		}
		catch (Exception e)
        { 
            TN.emitWarning(e.toString()); 
            e.printStackTrace(); 
            return false; 
        }
        return true; 
	}

	/////////////////////////////////////////////
	void ListImageFormats()  // for some reason I can't do JPG output
	{
		String[] wfnlist = ImageIO.getWriterFormatNames();
		for (int i = 0; i < wfnlist.length; i++)
			System.out.println("JJJJJ  " + wfnlist[i]);
	}

	/////////////////////////////////////////////
    // this tech is probably suprerceded by the gdal_translate tech
	void UploadPNG(boolean btomjgoverlay)
	{
		int irenderingquality = cbRenderingQuality.getSelectedIndex(); 
        OneSketch tsketch = sketchdisplay.sketchgraphicspanel.tsketch; 
		// then build it
		if ((irenderingquality == 2) || (irenderingquality == 3))
			sketchdisplay.mainbox.UpdateSketchFrames(tsketch, (irenderingquality == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS));

        BufferedImage bi = RenderBufferedImage(irenderingquality); 

//		String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
		try
		{
            //FileAbstraction.postData("http://seagrass.goatchurch.org.uk/~mjg/cgi-bin/uploadtiles.py", bi);
            //String response = FileAbstraction.postData("http://10.0.0.10/expo-cgi-bin/tunserv.py", tfdefaultsavename.getText(), bi);
			//TN.emitMessage("Writing file " + fa.getAbsolutePath() + " with type " + ftype);
            String filename = tfdefaultsavename.getText(); 
            FileAbstraction fimageas; 
            if (btomjgoverlay)
            {
                String lspatial_reference_system = "OS Grid SD"; // for Ireby (Yorkshire)
                double lspatial_reference_systemXoffset = 0; 
                double lspatial_reference_systemYoffset = 0; 

                for (OnePath op : tsketch.vpaths)
        		{
			        if (!((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && op.plabedl.sfontcode.equals("survey") && !op.plabedl.drawlab.equals("")))
                        continue; 

                    int isrs = op.plabedl.drawlab.indexOf("spatial_reference_system"); 
                    //System.out.println(isrs + " drawlab: " + op.plabedl.drawlab); 
                    if (isrs == -1)
                        continue; 

                    isrs += "spatial_reference_system".length(); 
                    while ((isrs < op.plabedl.drawlab.length()) && ((op.plabedl.drawlab.charAt(isrs) == ' ') || (op.plabedl.drawlab.charAt(isrs) == '=')))
                        isrs++; 

                    //System.out.println(isrs); 
                    if (isrs >= op.plabedl.drawlab.length())
                        continue; 
                    if (op.plabedl.drawlab.charAt(isrs) != '"')
                        continue; 

                    int isrse = op.plabedl.drawlab.indexOf('"', isrs + 1); 
                    //System.out.println(isrse); 
                    if ((isrse != -1) && (isrse - isrs < 200))
                        lspatial_reference_system = op.plabedl.drawlab.substring(isrs + 1, isrse); 
                    else
                        continue; 

                    // now extract the two following values
                    lspatial_reference_systemXoffset = 0.0; 
                    lspatial_reference_systemYoffset = 0.0; 
                    while ((isrse < op.plabedl.drawlab.length()) && ((op.plabedl.drawlab.charAt(isrs) == ' ') || (op.plabedl.drawlab.charAt(isrs) == ',')))
                        isrse++; 

                    int isrse1 = isrse; 
                    while ((isrse1 < op.plabedl.drawlab.length()) && (((op.plabedl.drawlab.charAt(isrse1) >= '0') && (op.plabedl.drawlab.charAt(isrse1) <= '9')) || (op.plabedl.drawlab.charAt(isrse1) == '-') || (op.plabedl.drawlab.charAt(isrse1) == '+') || (op.plabedl.drawlab.charAt(isrse1) == '.')))
                        isrse1++; 
                    String sXoffset = op.plabedl.drawlab.substring(isrse, isrse1); 

                    isrse = isrse1; 
                    while ((isrse < op.plabedl.drawlab.length()) && ((op.plabedl.drawlab.charAt(isrs) == ' ') || (op.plabedl.drawlab.charAt(isrs) == ',')))
                        isrse++; 

                    isrse1 = isrse; 
                    while ((isrse1 < op.plabedl.drawlab.length()) && (((op.plabedl.drawlab.charAt(isrse1) >= '0') && (op.plabedl.drawlab.charAt(isrse1) <= '9')) || (op.plabedl.drawlab.charAt(isrse1) == '-') || (op.plabedl.drawlab.charAt(isrse1) == '+') || (op.plabedl.drawlab.charAt(isrse1) == '.')))
                        isrse1++; 
                    String sYoffset = op.plabedl.drawlab.substring(isrse, isrse1); 

                    if (!sXoffset.equals("") || !sYoffset.equals(""))
                    {
                        try
                        {
                            lspatial_reference_systemXoffset = Double.valueOf(sXoffset); 
                            lspatial_reference_systemYoffset = Double.valueOf(sYoffset); 
                        }
                        catch(NumberFormatException e)
                        { TN.emitWarning(e.toString()); }
                    }
                }
TN.emitMessage(lspatial_reference_system + "  " + lspatial_reference_systemXoffset + "  " + lspatial_reference_systemYoffset); 
                NetConnection.upmjgirebyoverlay(bi, filename, dpmetre / realpaperscale, printrect.getX() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.x, -printrect.getY() / TN.CENTRELINE_MAGNIFICATION + tsketch.sketchLocOffset.y, lspatial_reference_system); 
            }
            else
            {
        		String target = TN.troggleurl + "jgtuploadfile"; 
                fimageas = NetConnection.uploadFile(FileAbstraction.MakeOpenableFileAbstraction(target), "tileimage", filename + ".png", bi, null);
                TN.emitMessage("Image was saved as :" + fimageas.getPath() + ":"); 
            }
		}
		catch (Exception e)
			{ e.printStackTrace(); }
	}
}


