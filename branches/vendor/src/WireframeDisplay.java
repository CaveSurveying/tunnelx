////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JFrame; 

import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 

import java.awt.Graphics; 

import java.util.Vector; 
import java.awt.FileDialog;


import java.io.IOException; 
import java.io.File;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 


//
//
// WireframeDisplay
//
//


// this class contains the whole outer set of options and buttons
class WireframeDisplay extends JFrame
{
	// the panel which holds the wireframe 3D graphics
	WireframeGraphics wiregraphicspanel;  

	SectionDisplay sectiondisplay; 

	boolean[] bmiStationNamesState = new boolean[2]; 

	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem("Centreline", true); 
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem("StationNames", true); 
	JCheckBoxMenuItem miXSections = new JCheckBoxMenuItem("XSections", true); 
	JCheckBoxMenuItem miTubes = new JCheckBoxMenuItem("Tubes", true); 
	JCheckBoxMenuItem miAxes = new JCheckBoxMenuItem("Axes", true); 
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem("Depth Colours", true); 
	JCheckBoxMenuItem miZFixed = new JCheckBoxMenuItem("Z Fixed", true); 
	JCheckBoxMenuItem miGlassLores = new JCheckBoxMenuItem("Glass LoRes", true); 

	// glassview dialog box 
	GlassDialog glassdialog; 


	/////////////////////////////////////////////
	// local classes
	/////////////////////////////////////////////
	class AutoViewMenuItem extends JMenuItem implements ActionListener
	{
		float zfRotX, zfRotZ; 
		AutoViewMenuItem(String label, float lzfRotX, float lzfRotZ) 
		{
			super(label); 
			zfRotX = lzfRotX; 
			zfRotZ = lzfRotZ; 
			addActionListener(this); 
		}

		public void actionPerformed(ActionEvent e)
		{
			wiregraphicspanel.SetAutomaticView(zfRotX, zfRotZ); 
		} 
	}; 
			
	/////////////////////////////////////////////
	ItemListener WireframeRepaint = new ItemListener()
	{ 
		public void itemStateChanged(ItemEvent e) 
		{
			wiregraphicspanel.repaint(); 
		}
	}; 


	/////////////////////////////////////////////
	// inactivate case 
	class WireframeHide extends WindowAdapter implements ActionListener	
	{
		public void windowClosing(WindowEvent e)
		{
			bmiStationNamesState[wiregraphicspanel.bEditable ? 0 : 1] = miStationNames.isSelected(); 
			setVisible(false); 
		}

		public void actionPerformed(ActionEvent e)
		{
			bmiStationNamesState[wiregraphicspanel.bEditable ? 0 : 1] = miStationNames.isSelected(); 
			setVisible(false); 
		}
	}


	/////////////////////////////////////////////
	// set up the arrays
    WireframeDisplay(SectionDisplay lsectiondisplay) 
	{
		super("Wireframe Display"); 

		sectiondisplay = lsectiondisplay; 
		wiregraphicspanel = new WireframeGraphics(sectiondisplay, this); 
		sectiondisplay.wireframedisplay = this; 

		// glass dialog class
		glassdialog = new GlassDialog(this, wiregraphicspanel.glassview); 

		// set up display
		getContentPane().add("Center", wiregraphicspanel); 

		// setup the display menu responses
		miCentreline.addItemListener(WireframeRepaint); 
		miStationNames.addItemListener(WireframeRepaint); 
		miXSections.addItemListener(WireframeRepaint); 
		miTubes.addItemListener(WireframeRepaint); 
		miAxes.addItemListener(WireframeRepaint); 
		miDepthCols.addItemListener(WireframeRepaint); 
		miZFixed.addItemListener(WireframeRepaint); 

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar(); 

		JMenu menufile = new JMenu("File"); 
		JMenuItem OutVRML = new JMenuItem("Write VRML"); 
		OutVRML.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { SaveVRML(); wiregraphicspanel.repaint(); } } ); // sometimes java doesn't clear the screen properly.  
		menufile.add(OutVRML); 

		JMenuItem miglass = new JMenuItem("Glass"); 
		miglass.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { MakeGlassView(); } } ); 
		menufile.add(miglass); 

		JMenuItem doneitem = new JMenuItem("Close"); 
		doneitem.addActionListener(new WireframeHide()); 
		addWindowListener(new WireframeHide()); 
		menufile.add(doneitem); 

		menubar.add(menufile); 

		JMenu menuview = new JMenu("View"); 
		menuview.add(miZFixed); 
		menuview.add(new AutoViewMenuItem("DownZ", (float)Math.PI / 2, (float)Math.PI / 2)); 
		menuview.add(new AutoViewMenuItem("DownX", (float)Math.PI / 4, (float)Math.PI / 4)); 
		menuview.add(new AutoViewMenuItem("DownY", (float)Math.PI * 3 / 4, (float)Math.PI / 4)); 
		menuview.add(new AutoViewMenuItem("Max", -1.0F, -1.0F)); 
		menubar.add(menuview); 

		JMenu menudisplay = new JMenu("Display"); 
		menudisplay.add(miCentreline); 
		menudisplay.add(miStationNames); 
		menudisplay.add(miXSections); 
		menudisplay.add(miTubes); 
		menudisplay.add(miAxes); 
		menudisplay.add(miDepthCols); 
		menudisplay.add(miGlassLores); 
		menubar.add(menudisplay); 

		setJMenuBar(menubar); 

		bmiStationNamesState[0] = true; 
		bmiStationNamesState[1] = false; 

		pack(); 
        setSize(400, 400);
    }

	/////////////////////////////////////////////
	void MakeGlassView()
	{
		if (!glassdialog.calcsolid(miGlassLores.isSelected())) 
			return; 

		glassdialog.preshowImParms(); 
		//wiregraphicspanel.repaint(); 
	}


	/////////////////////////////////////////////
	void SaveVRML()
	{
		boolean bSwapYZ = true; 
		float creaseAngle = 1.1F; 

		SvxFileDialog sfd = SvxFileDialog.showSaveDialog(TN.currentDirectory, this, SvxFileDialog.FT_VRML); 
		if ((sfd != null) && (sfd.svxfile != null)) 
		{
			TN.currentDirectory = sfd.getCurrentDirectory(); 
			try
			{
				System.out.println("Writing VRML file"); 
				VRMLOutputStream vos = new VRMLOutputStream(sfd.svxfile, bSwapYZ); 
				if (miTubes.isSelected())
					vos.WriteTubes(wiregraphicspanel.ot.vsections, wiregraphicspanel.ot.vtubes, creaseAngle); 
				if (miCentreline.isSelected())
					vos.WriteCentreline(wiregraphicspanel.ot.vstations, wiregraphicspanel.ot.vlegs); 
				vos.close(); 
			}
			catch (IOException ioe)
			{;}; 
		}
	}

	/////////////////////////////////////////////
	void ActivateWireframeDisplay(OneTunnel lot, boolean lbEditable)
	{
		wiregraphicspanel.ot = lot; 
		wiregraphicspanel.bEditable = lbEditable; 

		miStationNames.setSelected(bmiStationNamesState[wiregraphicspanel.bEditable ? 0 : 1]); 

		setTitle(lot.name); 
		toFront(); 

		// reform and remove xsections that should not be here 
		for (int i = wiregraphicspanel.ot.vsections.size() - 1; i >= 0; i--)
		{
			OneSection xsection = (OneSection)(wiregraphicspanel.ot.vsections.elementAt(i)); 
			if (!xsection.ReformRspace()) 
			{
				System.out.println("removing xsection " + xsection.station0.name); 
				wiregraphicspanel.ot.vsections.removeElementAt(i); 
			}
		}
				
		for (int i = wiregraphicspanel.ot.vtubes.size() - 1; i >= 0; i--)
		{
			OneTube tube = (OneTube)(wiregraphicspanel.ot.vtubes.elementAt(i)); 
			if ((wiregraphicspanel.ot.vsections.indexOf(tube.xsection0) == -1) || (wiregraphicspanel.ot.vsections.indexOf(tube.xsection1) == -1)) 
			{
				wiregraphicspanel.ot.vtubes.removeElementAt(i); 
				System.out.println("removing tube"); 
			}
			else
				tube.ReformTubespace(); 
		}


		wiregraphicspanel.ReformMatrix(); 
		wiregraphicspanel.MaximizeView(); 
		wiregraphicspanel.ReformView(); 
		wiregraphicspanel.UpdateDepthCol(); 
		show(); 
	}


	/////////////////////////////////////////////
	void ReformXSection(OneSection xsection) 
	{
		xsection.ReformRspace(); 
		for (int i = 0; i < wiregraphicspanel.ot.vtubes.size(); i++) 
		{
			OneTube tube = (OneTube)(wiregraphicspanel.ot.vtubes.elementAt(i)); 
			if ((tube.xsection0 == xsection) || (tube.xsection1 == xsection)) 
				tube.ReformTubespace(); 
		}
		RefreshWireDisplay(); 
	}

	/////////////////////////////////////////////
	void RefreshWireDisplay()
	{
		if (isVisible())
		{
			wiregraphicspanel.ReformView(); 
			wiregraphicspanel.repaint(); 
		}
	}

	/////////////////////////////////////////////
	void XSectionDelete(OneSection xsection)
	{
		if (!wiregraphicspanel.ot.vsections.removeElement(xsection)) 
			return; 

		System.out.println("delete xc"); 

		wiregraphicspanel.ReformView(); 
		wiregraphicspanel.repaint(); 

		// kill all tubes stuck to it?  
		// if two tubes connect to it, connect across them 
		OneSection lxsection0 = null; 
		OneSection lxsection1 = null; 

		for (int i = wiregraphicspanel.ot.vtubes.size() - 1; i >= 0; i--) 
		{
			OneTube tube = (OneTube)(wiregraphicspanel.ot.vtubes.elementAt(i)); 
			if (tube.xsection0 == xsection) 
			{
				lxsection1 = tube.xsection1; 
				wiregraphicspanel.ot.vtubes.removeElementAt(i); 
			}
			if (tube.xsection1 == xsection) 
			{
				lxsection0 = tube.xsection0; 
				wiregraphicspanel.ot.vtubes.removeElementAt(i); 
			}
		}

		// add in tube if it looks natural and isn't already present 
		if ((lxsection0 != null) && (lxsection1 != null)) 
		{
			boolean bTubePresent = false; 
			for (int i = 0; ((i < wiregraphicspanel.ot.vtubes.size()) && !bTubePresent); i++) 
			{
				OneTube tube = (OneTube)(wiregraphicspanel.ot.vtubes.elementAt(i)); 
				bTubePresent |= (((tube.xsection0 == lxsection0) && (tube.xsection1 == lxsection1)) || ((tube.xsection0 == lxsection1) && (tube.xsection1 == lxsection0)));  
			} 
			if (!bTubePresent) 
			{
				OneTube tube = new OneTube(lxsection0, lxsection1); 
				tube.ReformTubespace(); 
				wiregraphicspanel.ot.vtubes.addElement(tube); 
				System.out.println("adding extra tube"); 
			}
		}
	}


	/////////////////////////////////////////////
	void TubeDelete(OneTube ot)
	{
		if (wiregraphicspanel.ot.vtubes.removeElement(ot))
			System.out.println("delete tube"); 

		wiregraphicspanel.ReformView(); 
		wiregraphicspanel.repaint(); 
	}
}


