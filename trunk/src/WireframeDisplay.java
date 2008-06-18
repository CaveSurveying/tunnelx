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

import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 


import java.awt.Graphics; 

import java.awt.FileDialog;
import java.awt.BorderLayout;


import java.io.IOException; 

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 

import javax.swing.JSlider; 
import javax.swing.event.ChangeEvent; 
import javax.swing.event.ChangeListener; 


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

	DateSliderControl dateslidercontrol; 

	boolean[] bmiStationNamesState = new boolean[2]; 

	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem("Centreline", true); 
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem("StationNames", true); 
	JCheckBoxMenuItem miAxes = new JCheckBoxMenuItem("Axes", true); 
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem("Depth Colours", true); 
	JCheckBoxMenuItem miZFixed = new JCheckBoxMenuItem("Z Fixed", true); 

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
	// slider on bottom for the amount of cave up to a date
	class DateSliderControl extends JSlider implements ChangeListener
	{
		WireframeGraphics wiregraphicspanel; 
		DateSliderControl(WireframeGraphics lwiregraphicspanel)
		{
			super(0, 100, 100); 
			wiregraphicspanel = lwiregraphicspanel; 
			addChangeListener(this); 

		}

		public void stateChanged(ChangeEvent e)
		{
			int slv = getValue(); 
//			wiregraphicspanel.depthcol.datelimit = (slv != 100 ? slv * wiregraphicspanel.ot.mdatepos / 100 : -1); 
			wiregraphicspanel.repaint(); 
		}
	}

	/////////////////////////////////////////////
	// set up the arrays
	WireframeDisplay() 
	{
		super("Wireframe Display"); 

		wiregraphicspanel = new WireframeGraphics(this); 
		dateslidercontrol = new DateSliderControl(wiregraphicspanel); 

		// set up display
		getContentPane().add(wiregraphicspanel, BorderLayout.CENTER); 
		getContentPane().add(dateslidercontrol, BorderLayout.SOUTH); 
		

		// setup the display menu responses
		miCentreline.addItemListener(WireframeRepaint); 
		miStationNames.addItemListener(WireframeRepaint); 
		miAxes.addItemListener(WireframeRepaint); 
		miDepthCols.addItemListener(WireframeRepaint); 
		miZFixed.addItemListener(WireframeRepaint); 

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar(); 

		JMenu menufile = new JMenu("File"); 

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
		menudisplay.add(miAxes); 
		menudisplay.add(miDepthCols); 
		menubar.add(menudisplay); 

		setJMenuBar(menubar); 

		bmiStationNamesState[0] = true; 
		bmiStationNamesState[1] = false; 

		pack(); 
		setSize(400, 400);
	}


	/////////////////////////////////////////////
	void ActivateWireframeDisplay(String lname)
	{
		miStationNames.setSelected(bmiStationNamesState[wiregraphicspanel.bEditable ? 0 : 1]); 

		setTitle(lname); 
		toFront(); 

				
		wiregraphicspanel.ReformMatrix(); 
		wiregraphicspanel.MaximizeView(); 
		wiregraphicspanel.ReformView(); 
		wiregraphicspanel.UpdateDepthCol(); 
		setVisible(true); 
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
}


