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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.AlphaComposite; 
import java.awt.Composite; 
import java.awt.Rectangle;
import java.awt.Color;

import javax.imageio.ImageIO;


/////////////////////////////////////////////
class SketchPrintPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	double truewidth;
	double trueheight;
	double realpaperscale; 

	Rectangle2D printrect; 

	int pixelheight; 
	int pixelwidth; 

	int lastpixfield = 0; 
	
	JTextField tftruesize = new JTextField(); 

	JTextField dpifield = new JTextField("72.0");
	JTextField tfpixelswidth = new JTextField(); 
	JTextField tfpixelsheight = new JTextField(); 

	JTextField tfdefaultdirname = new JTextField(); 
	JTextField tfdefaultsavename = new JTextField(); 
	
	JCheckBox chGrayScale = new JCheckBox("Gray Scale");
	JCheckBox chAntialiasing = new JCheckBox("Antialiasing", true);
	JCheckBox chTransparentBackground = new JCheckBox("Transparent"); 
	JCheckBox chProperFramesketches = new JCheckBox("Proper sketches"); 

	JButton buttpng = new JButton("PNG"); 
	JButton buttsvg = new JButton("SVG"); 
	JButton buttresetdir = new JButton("ResetDIR"); 
	
	AffineTransform aff = new AffineTransform();
	FileAbstraction currprintdir = null; 

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

		pan1.add(new JLabel("dots/inch:", JLabel.RIGHT));
		pan1.add(dpifield);

		dpifield.addActionListener(new pixfieldlisten(0)); 
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
		
		JPanel pan2 = new JPanel(new BorderLayout());

		JPanel panchb = new JPanel(new GridLayout(0, 1));
		panchb.add(chGrayScale);
		panchb.add(chAntialiasing);
		panchb.add(chTransparentBackground); 
		panchb.add(chProperFramesketches);
		add(panchb, BorderLayout.EAST); 

		JPanel panbutts = new JPanel(new GridLayout(0, 1)); 

		buttpng.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
				{ OutputPNG(); }
		});

		buttsvg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
				{ OutputSVG(); }
		});

		buttresetdir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
				{ ResetDIR(true); }
		});

		panbutts.add(buttpng); 
		panbutts.add(buttsvg); 
		panbutts.add(buttresetdir); 
		
		add(panbutts, BorderLayout.WEST); 
	}

	/////////////////////////////////////////////
	void ResetDIR(boolean bresetfromcurrent)
	{
		if (bresetfromcurrent)	
			currprintdir = FileAbstraction.MakeCanonical(TN.currentDirectory); // alternatively could use FileAbstraction MakeCurrentUserDirectory()

		tfdefaultdirname.setText(currprintdir.getAbsolutePath()); 

		String bname = sketchdisplay.selectedsubsetstruct.GetFirstSubset(); 
		if ((bname == null) && (sketchdisplay.sketchgraphicspanel.tsketch.sketchtunnel != null))
			bname = sketchdisplay.sketchgraphicspanel.tsketch.sketchtunnel.name; 
		if (bname == null)
			bname = TN.loseSuffix(sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getName());
		tfdefaultsavename.setText(bname); 
	}

	/////////////////////////////////////////////
	void UpdatePrintingRectangle(Rectangle2D lprintrect, Vec3 sketchLocOffset, double lrealpaperscale) 
	{
		if (sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP.isEmpty())
		{
			try
				{ lprintrect = sketchdisplay.sketchgraphicspanel.currtrans.createInverse().createTransformedShape(sketchdisplay.sketchgraphicspanel.windowrect).getBounds(); }
			catch (NoninvertibleTransformException ex)
				{;}
		}
		ResetDIR((currprintdir == null));  // initialize
			
		printrect = lprintrect; 
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
			double dpmetre = -1.0; 
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

			double dpmetre; 
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
	class pixfieldlisten implements ActionListener
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
			Updatefinalsize(pixfield); 
		}
		public void removeUpdate(DocumentEvent e) 
		{
			Updatefinalsize(pixfield); 
		}
		public void changedUpdate(DocumentEvent e) 
		{
			Updatefinalsize(pixfield); 
		}
	};

	/////////////////////////////////////////////
	void OutputSVG()
	{
		System.out.println("NNOOTT  Outting SVG thing"); 
	}
	

	//for (OneSArea osa : vsareas)
	//pwqFramedSketch(ga, osa, vgsymbols, sketchlinestyle);

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
	void OutputPNG()
	{
		// dispose of finding the file first
		FileAbstraction fa = FileAbstraction.MakeDirectoryAndFileAbstraction(currprintdir, tfdefaultsavename.getText()); 
		SvxFileDialog sfd = SvxFileDialog.showSaveDialog(fa, sketchdisplay, SvxFileDialog.FT_BITMAP); 
		if (sfd == null)
			return;
		fa = sfd.getSelectedFileA();
		currprintdir = sfd.getCurrentDirectoryA(); 
		ResetDIR(false); 

		// then build it
		if (chProperFramesketches.isSelected())
			sketchdisplay.sketchgraphicspanel.activetunnel.UpdateSketchFrames(sketchdisplay.sketchgraphicspanel.tsketch, true, sketchdisplay.mainbox); 

		BufferedImage bi = new BufferedImage(pixelwidth, pixelheight, (chGrayScale.isSelected() ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_INT_ARGB));
		Graphics2D g2d = bi.createGraphics();
		if (chTransparentBackground.isSelected())
		{
			Composite tcomp = g2d.getComposite(); 
			System.out.println("What is composite:" + tcomp.toString()); 
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0F));
			g2d.fill(new Rectangle(0, 0, pixelwidth, pixelheight));
			g2d.setComposite(tcomp);
		}
		else
		{
			g2d.setColor(Color.white);  // could make it a different colour
			g2d.fill(new Rectangle(0, 0, pixelwidth, pixelheight));
		}
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (chAntialiasing.isSelected() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF));

		// set the pre transformation
		aff.setToTranslation(pixelwidth / 2, pixelheight / 2);
		double scchange = printrect.getWidth() / (pixelwidth - 2);
		aff.scale(1.0F / scchange, 1.0F / scchange);
		aff.translate(-(printrect.getX() + printrect.getWidth() / 2), -(printrect.getY() + printrect.getHeight() / 2));
		g2d.setTransform(aff);
		
		GraphicsAbstraction ga = new GraphicsAbstraction(g2d); 
		ga.printrect = printrect; 
		
		sketchdisplay.sketchgraphicspanel.tsketch.paintWqualitySketch(ga, true, sketchdisplay.vgsymbols, sketchdisplay.sketchlinestyle);

		String ftype = TN.getSuffix(fa.getName()).substring(1).toLowerCase();
		try
		{
			TN.emitMessage("Writing file " + fa.getAbsolutePath() + " with type " + ftype);
			ImageIO.write(bi, ftype, fa.localfile);
		}
		catch (Exception e) 
			{ e.printStackTrace(); }
	}
}


