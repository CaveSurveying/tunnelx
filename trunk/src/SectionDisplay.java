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

import javax.swing.JFrame; 
import javax.swing.JPanel; 
import javax.swing.JButton; 
import javax.swing.JLabel; 
import javax.swing.JOptionPane;
import javax.swing.JTextField; 
import javax.swing.JCheckBox; 

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension; 

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Vector; 

import javax.swing.JSplitPane; 
import javax.swing.JScrollPane; 

import javax.swing.JMenu; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 
import javax.swing.JMenuBar; 

import java.awt.Image; 
import java.awt.image.BufferedImage;

//
//
// SectionDisplay
//
//

// this class defunct

/////////////////////////////////////////////
class SectionDisplay extends JFrame
{
	// the extents panel
	LRUDpanel plrud = new LRUDpanel();

	JTextField tforientfore = new JTextField();
	JTextField tforientback = new JTextField();
	JTextField tfrelorient = new JTextField();
	JTextField tfclino = new JTextField();

	JTextField tfstat0 = new JTextField();
	JTextField tfstat1 = new JTextField();
	JTextField tflambda = new JTextField();

	JCheckBox cbera = new JCheckBox("erase", null, false);
	JCheckBox cbflrud = new JCheckBox("Flrud", null, false);
	JCheckBoxMenuItem miBackDrag = new JCheckBoxMenuItem("Background Drag", false); 

	// the panel which holds the shape of an xsection
	ShapeGraphics shapegraphicspanel = new ShapeGraphics(plrud, cbflrud, cbera); 

	OneSection xsection; 

	Vector vstations; 
	Vector vsections;
	boolean bNewSection; 

	OneTube tube; 
	Vector vtubes;		// used for adding in a split tube

	// place that the redraw message is posted 
	WireframeDisplay wireframedisplay = null; 

	SectionPreviewDisplay sectionpreviewdisplay; 


	/////////////////////////////////////////////
	// local classes
	/////////////////////////////////////////////
	class AlterViewMenuItem extends JMenuItem implements ActionListener
	{
		float diamchange; 
		boolean bMaximize; 

		AlterViewMenuItem(String label, float ldiamchange, boolean lbMaximize)
		{
			super(label); 
			diamchange = ldiamchange; 
			bMaximize = lbMaximize; 
			addActionListener(this); 
		}

		public void actionPerformed(ActionEvent e)
		{
			if (diamchange != 0.0F)
			{
				shapegraphicspanel.diamx = Math.max(shapegraphicspanel.diamx + diamchange, 1.0F); 
				shapegraphicspanel.diamy = Math.max(shapegraphicspanel.diamy + diamchange, 1.0F); 
			}
			else
			{
				shapegraphicspanel.ReformViewCentreDiameter(true);
				if (bMaximize)
					shapegraphicspanel.ReformViewCentreDiameter(false); 
			}

			shapegraphicspanel.ReformPoly();
			shapegraphicspanel.repaint(); 
		}
	}; 


	/////////////////////////////////////////////
	// inactivate case 
	class SectionDisplayHide extends WindowAdapter implements ActionListener
	{
		public void windowClosing(WindowEvent e)
		{
			setVisible(false); 
		}

		public void actionPerformed(ActionEvent e)
		{
			setVisible(false);
		}
	}


	/////////////////////////////////////////////
    SectionDisplay()
	{
		super("Interactive Section Display");
		//super(parent, "Interactive Section Display", false);

		// set up display
		JPanel pfield = new JPanel(new BorderLayout());

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar();

		JMenu menufile = new JMenu("File");

		JMenuItem miLoadBackground = new JMenuItem("Load Background");
		miLoadBackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { LoadBackground(); } } );
		menufile.add(miLoadBackground);

		JMenuItem miLoadPSec = new JMenuItem("Load PrevXS");
		miLoadPSec.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { LoadPrevXS(); } } );
		menufile.add(miLoadPSec);

		JMenuItem miSavePSec = new JMenuItem("Save Preview XS");
		miSavePSec.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SavePrevXS(); } } );
		menufile.add(miSavePSec);

		JMenuItem OkayMenuItem = new JMenuItem("Okay");
		OkayMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
			SectionOkay(); 	} } );
		menufile.add(OkayMenuItem);

		JMenuItem ApplyMenuItem = new JMenuItem("Apply");
		ApplyMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
			SectionApply();	} } );
		menufile.add(ApplyMenuItem);

		JMenuItem CancelMenuItem = new JMenuItem("Cancel"); 
		CancelMenuItem.addActionListener(new SectionDisplayHide()); 
		addWindowListener(new SectionDisplayHide()); 
		menufile.add(CancelMenuItem);  

		JMenuItem DeleteMenuItem = new JMenuItem("Delete"); 
		DeleteMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) 
		{
			wireframedisplay.XSectionDelete(xsection); 
			setVisible(false); 
		} } ); 	
		menufile.add(DeleteMenuItem);  

		menubar.add(menufile);

		miBackDrag.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
		{
			//shapegraphicspanel.SetMouseMotionStuff(miBackDrag.getState());
		} } ); 	

		JMenu menuview = new JMenu("View"); 
		menuview.add(new AlterViewMenuItem("Scale Up", 1.0F, false));  
		menuview.add(new AlterViewMenuItem("Scale Down", -1.0F, false));  
		menuview.add(new AlterViewMenuItem("Centre", 0.0F, false));  
		menuview.add(new AlterViewMenuItem("Max", 0.0F, true));  

		menuview.add(miBackDrag);  
		
		menubar.add(menuview); 


		sectionpreviewdisplay = new SectionPreviewDisplay(shapegraphicspanel); 

		setJMenuBar(menubar); 


		JPanel ppos = new JPanel(new GridLayout(0, 4)); 
		ppos.add(new JLabel("stat.0"));
		ppos.add(new JLabel("")); 
		ppos.add(new JLabel("stat.1"));
		ppos.add(new JLabel("lambda")); 
		ppos.add(tfstat0); 
		ppos.add(new JLabel("")); 
		ppos.add(tfstat1); 
		ppos.add(tflambda); 

		JPanel porient = new JPanel(new GridLayout(0, 4)); 
		porient.add(new JLabel("Fore")); 
		porient.add(new JLabel("Back")); 
		porient.add(new JLabel("A-Disp.")); 
		porient.add(new JLabel("Tilt")); 
		porient.add(tforientfore); 
		porient.add(tforientback); 
		porient.add(tfrelorient); 
		porient.add(tfclino); 

		JPanel pbcontrols = new JPanel(new GridLayout(0, 4)); 
		JButton ApplyButton = new JButton("APPLY"); 
		ApplyButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
			SectionApply();	} } ); 	
		pbcontrols.add(ApplyButton);  

		JButton OkayButton = new JButton("OKAY"); 
		OkayButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
			SectionOkay(); 	} } ); 	
		pbcontrols.add(OkayButton);  
		pbcontrols.add(cbera); 
		pbcontrols.add(cbflrud); 

		JPanel peastnor = new JPanel(new GridLayout(0, 1)); 
		peastnor.add(ppos); 
		peastnor.add(porient); 
		peastnor.add(plrud); 
		peastnor.add(pbcontrols); 


		// one version of this panel 
		JPanel peast = new JPanel(new BorderLayout()); 
		peast.add("North", peastnor); 

		//peast.add("Center", sectionpreviewdisplay); 
		JScrollPane eastso = new JScrollPane(sectionpreviewdisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); 
		peast.add("Center", eastso); 
		

		// build the left hand area
		JPanel toppanel = new JPanel(new BorderLayout()); 
		toppanel.setLayout(new BorderLayout());
		toppanel.add("Center", shapegraphicspanel); 
		toppanel.add("East", peast); 

		// the two centre line type panels
        Dimension minimumSize = new Dimension(600, 500);
        toppanel.setPreferredSize(minimumSize);

        //Add the split pane to this frame
        getContentPane().add(toppanel);

		pack(); 
    }

	/////////////////////////////////////////////
	void LoadBackground()
	{
		SvxFileDialog sfd = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, SvxFileDialog.FT_BITMAP, false); 
		if ((sfd != null) && (sfd.svxfile != null)) 
		{
			TN.currentDirectory = sfd.getCurrentDirectoryA(); 
	        Image img = getToolkit().createImage(sfd.svxfile.toString());
//			shapegraphicspanel.backgroundimg.SetImage(img);
		}
	}


	/////////////////////////////////////////////
	void LoadPrevXS()
	{
		SvxFileDialog sfd = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, SvxFileDialog.FT_XSECTION_PREVIEW, false); 
		if ((sfd != null) && (sfd.svxfile != null)) 
		{
			TN.currentDirectory = sfd.getCurrentDirectoryA(); 
			sectionpreviewdisplay.LoadPrevSections(sfd.tunneldirectory); 
		}
	}

	/////////////////////////////////////////////
	void SavePrevXS()
	{
		SvxFileDialog sfd = SvxFileDialog.showSaveDialog(TN.currentDirectory, this, SvxFileDialog.FT_XSECTION_PREVIEW); 
		if ((sfd != null) && (sfd.svxfile != null)) 
		{
			TN.currentDirectory = sfd.getCurrentDirectoryA(); 
			sectionpreviewdisplay.SavePrevSections(sfd.tunneldirectory); 
		}
	}

	/////////////////////////////////////////////
	void ActivateXSectionDisplay(OneSection lxsection, Vector lvstations, Vector lvsections, boolean lbNewSection, OneTube ltube, Vector lvtubes)
	{
		xsection = lxsection; 
		xsection.LoadIntoGraphics(shapegraphicspanel); 
		vstations = lvstations; 
		vsections = lvsections;	
		bNewSection = lbNewSection; 

		// null if not a split tube 
		tube = ltube; 
		vtubes = lvtubes; 

		// set the values of the textfields
		tforientfore.setText(xsection.orientstationforeS); 
		tforientback.setText(xsection.orientstationbackS); 
		tfrelorient.setText(xsection.relorientcompassS); 
		tfclino.setText(xsection.orientclinoS); 

		tfstat0.setText(xsection.station0S); 
		tfstat1.setText(xsection.station1S); 
		tflambda.setText(String.valueOf(xsection.lambda)); 

		// reform the polygon region
		shapegraphicspanel.ReformViewCentreDiameter(true); 
		shapegraphicspanel.ReformViewCentreDiameter(false); 
		shapegraphicspanel.ReformPoly();
		shapegraphicspanel.repaint(); 
	
setTitle("Section for " + xsection.station0S + " " + String.valueOf(xsection.lambda)); 

		toFront(); 
		setVisible(true); 
	}


	/////////////////////////////////////////////
	boolean SectionApply()
	{
		// first search for errors, returning false without changing anything if there are any.  

		// do the text strings make any sense?  
		String lrelorientcompassS = tfrelorient.getText().trim(); 
		String lorientclinoS = tfclino.getText().trim();
		float lrelorientcompass; 
		float lorientclino; 
		float llambda; 
		try 
		{
			lrelorientcompass = Float.valueOf(lrelorientcompassS).floatValue(); 
			if (!lorientclinoS.equalsIgnoreCase("up") && !lorientclinoS.equalsIgnoreCase("down") && !lorientclinoS.equalsIgnoreCase("-")) 
				lorientclino = Float.valueOf(lorientclinoS).floatValue(); 
			llambda = Float.valueOf(tflambda.getText().trim()).floatValue(); 
		}
		catch (NumberFormatException e) 
		{
			TN.emitWarning("bad lambda/relorient compass/clino"); 
			return(false); 
		}


		// can the fore and back stations be found?  
		String lstation0S = tfstat0.getText().trim(); 
		String lstation1S = tfstat1.getText().trim(); 
		if (lstation1S.equals("")) 
			lstation1S = lstation0S; 
		OneStation lstation0 = null; 
		OneStation lstation1 = null; 

		// can the fore and back stations be found?  
		String lorientstationforeS = tforientfore.getText().trim(); 
		String lorientstationbackS = tforientback.getText().trim(); 
		OneStation lstationfore = null; 
		OneStation lstationback = null; 

		for (int i = 0; i < vstations.size(); i++) 
		{
			OneStation os = (OneStation)(vstations.elementAt(i));  
			if (os.name.equalsIgnoreCase(lorientstationforeS))
				lstationfore = os; 
			if (os.name.equalsIgnoreCase(lorientstationbackS)) 
				lstationback = os; 
			if (os.name.equalsIgnoreCase(lstation0S)) 
				lstation0 = os; 
			if (os.name.equalsIgnoreCase(lstation1S)) 
				lstation1 = os; 
		}


		// this is a returnable offense.  
		if ((lstation0 == null) || (lstation1 == null))  
		{
			TN.emitWarning("station 0 or 1 not found"); 
			return false; 
		}

		// if this is a single station kind, is there a section at it already 
		if (bNewSection && lstation0S.equalsIgnoreCase(lstation1S)) 
		{
			for (int i = 0; i < vsections.size(); i++) 
			{
				OneSection xsection = (OneSection)(vsections.elementAt(i)); 
				if (xsection.station0S.equalsIgnoreCase(xsection.station1S) && lstation0S.equalsIgnoreCase(xsection.station0S)) 
				{
					TN.emitMessage("xsection on station already present"); 
					return false; 
				}
			}
		}


		// doubt if this is a non-returnable offense.  
		if ((lorientstationforeS.length() != 0) && (lstationfore == null))  
			TN.emitWarning("Warning, fore station not found: " + lorientstationforeS);
		if ((lorientstationbackS.length() != 0) && (lstationback == null))  
			TN.emitWarning("Warning, back station not found: " + lorientstationbackS); 

		// now apply all the data.  
		xsection.LoadFromShapeGraphics(shapegraphicspanel); 

		xsection.orientstationforeS = lorientstationforeS; 
		xsection.orientstationbackS = lorientstationbackS; 
		xsection.stationfore = lstationfore; 
		xsection.stationback = lstationback; 

		xsection.relorientcompassS = lrelorientcompassS; 
		xsection.orientclinoS = lorientclinoS; 

		xsection.lambda = llambda; 
		xsection.station0S = lstation0S; 
		xsection.station1S = lstation1S; 
		xsection.station0 = lstation0; 
		xsection.station1 = lstation1; 

		if (bNewSection) 
		{
			vsections.addElement(xsection); 
			bNewSection = false; 
		}

		// remap the tubes if necessary 
		if (tube != null) 
		{
			vtubes.removeElement(tube); 
			vtubes.addElement(new OneTube(tube.xsection0, xsection)); 
			vtubes.addElement(new OneTube(xsection, tube.xsection1)); 

			tube = null; 
			vtubes = null;
		}


		// redoes the tubes to it
		wireframedisplay.ReformXSection(xsection); 

		return(true); 
	}


	/////////////////////////////////////////////
	void SectionOkay()
	{
		if (SectionApply()) 
			setVisible(false); 
	}
}

