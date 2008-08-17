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
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import java.awt.geom.Point2D;

import java.util.List;
import java.util.ArrayList;

/////////////////////////////////////////////
class SketchBackgroundPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JCheckBox cbshowbackground;
	JCheckBox cbshowgrid;
	JCheckBox cbsnaptogrid; 

	// tells us the grid spacing.
    JTextField tfgridspacing = new JTextField("");
	int gsoffset = 0;

	JComboBox cbbackimage = new JComboBox(); 
	List<OnePath> tsvpathsframescbelements = new ArrayList<OnePath>(); // parallel to the list
	List<String> tsvpathsframescbelementsS = new ArrayList<String>(); // parallel to the list; used to prevent multiple equal strings getting into the combobox, which prevents it working;
																	  // the correct implementation would have been to add OnePaths into the combobox and apply the toString function to get the names
	ActionListener cbbackimageAL = new ActionListener()
	{ 
		public void actionPerformed(ActionEvent event)
		{ 
			System.out.println("cbbackimageSEL " + cbbackimage.getSelectedIndex() + " " + event.getActionCommand()); 
			int i = cbbackimage.getSelectedIndex(); 
			if (i != -1)
			{
				OnePath op = tsvpathsframescbelements.get(i); 
				sketchdisplay.sketchlinestyle.pthstyleareasigtab.LoadSketchFrameDef(op.plabedl.sketchframedef);
				sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag = op;
				sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(op, false); 
			}
		}
	}; 


	/////////////////////////////////////////////
	class radbuttpress implements ActionListener
	{
		int lgsoffset;
		radbuttpress(int llgsoffset)
		{
			lgsoffset = llgsoffset;
		}
		public void actionPerformed(ActionEvent event)
		{
			gsoffset = lgsoffset;
			sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
		}
	};

	/////////////////////////////////////////////
	void SetGridOrigin(boolean btocurrent)
	{
		if (btocurrent)
		{
			if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
			{
				Point2D pn = sketchdisplay.sketchgraphicspanel.currgenpath.pnstart.pn;
				sketchdisplay.sketchgraphicspanel.sketchgrid.txorig = (float)pn.getX();
				sketchdisplay.sketchgraphicspanel.sketchgrid.tyorig = (float)pn.getY();
			}
		}
		else
		{
			sketchdisplay.sketchgraphicspanel.sketchgrid.txorig = sketchdisplay.sketchgraphicspanel.sketchgrid.xorig;
			sketchdisplay.sketchgraphicspanel.sketchgrid.tyorig = sketchdisplay.sketchgraphicspanel.sketchgrid.yorig;
		}

		sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
	}


	/////////////////////////////////////////////
	void NewBackgroundFile()
	{
System.out.println("calling NewBackgroundFile " + sketchdisplay.sketchgraphicspanel.tsketch.sketchfile); 
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectoryIMG, sketchdisplay, SvxFileDialog.FT_BITMAP, false);
		if ((sfiledialog == null) || (sfiledialog.svxfile == null))
			return;
		TN.currentDirectoryIMG = sfiledialog.getSelectedFileA();
		String imfilename = null;
		try
		{
			imfilename = FileAbstraction.GetImageFileName(sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getParentFile(), sfiledialog.svxfile);
		}
		catch (IOException ie)
		{ TN.emitWarning(ie.toString()); };

		if (imfilename == null)
			return;

		OnePath prevcurrpath = sketchdisplay.sketchgraphicspanel.currgenpath;
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);

System.out.println("YYYYY " + imfilename);
		OnePath gop  = sketchdisplay.sketchgraphicspanel.MakeConnectiveLineForData(0);
		sketchdisplay.sketchgraphicspanel.AddPath(gop); 
		//sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		gop.plabedl.sketchframedef.sfsketch = imfilename;

		gop.plabedl.sketchframedef.sfscaledown = 1.0F;
		gop.plabedl.sketchframedef.sfrotatedeg = 0.0F;
		gop.plabedl.sketchframedef.sfxtrans = (float)(sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x / TN.CENTRELINE_MAGNIFICATION);
		gop.plabedl.sketchframedef.sfytrans = -(float)(sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y / TN.CENTRELINE_MAGNIFICATION);
		gop.plabedl.sketchframedef.SetSketchFrameFiller(sketchdisplay.mainbox, sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale, sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchdisplay.sketchgraphicspanel.tsketch.sketchfile);

		sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(gop, true);
		sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag = gop;

		assert gop.plabedl.sketchframedef.IsImageType();
		gop.plabedl.sketchframedef.MaxCentreOnScreenButt(sketchdisplay.sketchgraphicspanel.getSize(), true, 1.0, sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchdisplay.sketchgraphicspanel.currtrans);
		sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(gop, true);
		sketchdisplay.sketchgraphicspanel.FrameBackgroundOutline(); 

		if (!sketchdisplay.miShowBackground.isSelected())
			sketchdisplay.miShowBackground.doClick();
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	// with this case we're removing the action listener to avoid any events firing that are not from mouse clicks
	synchronized void UpdateBackimageCombobox(int iy)
	{
		//System.out.println("yyyy yyy " + iy); 
		OnePath tsvpathsframescbelementssel = sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag; 
		List<OnePath> ltsvpathsframescbelements = sketchdisplay.sketchgraphicspanel.tsvpathsframes; 
		boolean baddselelement = ((tsvpathsframescbelementssel != null) && !ltsvpathsframescbelements.contains(tsvpathsframescbelementssel)); 

		// check if the values have changed
		assert tsvpathsframescbelements.size() == cbbackimage.getItemCount(); 

		boolean btoupdate = (tsvpathsframescbelements.size() != ltsvpathsframescbelements.size() + (baddselelement ? 1 : 0)); 
		int isel = -1; 
		if (!btoupdate)
		{
			for (int i = 0; i < ltsvpathsframescbelements.size(); i++)
			{
				if (tsvpathsframescbelements.get(i) != ltsvpathsframescbelements.get(i))
				{
					btoupdate = true; 
					break; 
				}
				if (tsvpathsframescbelements.get(i) == tsvpathsframescbelementssel)
					isel = i; 
			}
			if (baddselelement && !btoupdate)
			{
				if (tsvpathsframescbelements.get(ltsvpathsframescbelements.size()) != tsvpathsframescbelementssel)
					btoupdate = true; 
				else
					isel = ltsvpathsframescbelements.size(); 
			}
		}
		
		if (btoupdate)
		{
System.out.println("Updating cbbackimage"); 
			tsvpathsframescbelements.clear(); 
			tsvpathsframescbelements.addAll(ltsvpathsframescbelements); 
			if (baddselelement)
				tsvpathsframescbelements.add(tsvpathsframescbelementssel); 

			// create distinct strings
			tsvpathsframescbelementsS.clear(); 
			for (OnePath op : tsvpathsframescbelements)
			{
				String ssval = op.plabedl.sketchframedef.sfsketch; 
				int i = 1; 
				String lssval = ssval; 
				while (tsvpathsframescbelementsS.contains(lssval))
					lssval = ssval + " (" + (++i) + ")"; 
				tsvpathsframescbelementsS.add(lssval); 
			}
			assert tsvpathsframescbelements.size() == tsvpathsframescbelementsS.size(); 
			
			// suppress the action listener
			cbbackimage.removeActionListener(cbbackimageAL); 
			cbbackimage.removeAllItems();
			for (int i = 0; i < tsvpathsframescbelements.size(); i++)
			{
// must give all entries different names because the implementation of setSelectedIndex is setSelectedItem
				cbbackimage.addItem(tsvpathsframescbelementsS.get(i)); 
				if (tsvpathsframescbelements.get(i) == tsvpathsframescbelementssel)
					isel = i; 
			}
			if (isel != cbbackimage.getSelectedIndex())
				cbbackimage.setSelectedIndex(isel); 
			cbbackimage.addActionListener(cbbackimageAL); 
		}
		else if (isel != cbbackimage.getSelectedIndex())
		{
			// suppress the action listener
			cbbackimage.removeActionListener(cbbackimageAL); 
			cbbackimage.setSelectedIndex(isel); 
			cbbackimage.addActionListener(cbbackimageAL); 
		}
		
		assert (isel == -1 || (tsvpathsframescbelements.get(isel) == tsvpathsframescbelementssel)); 
		assert isel == cbbackimage.getSelectedIndex(); 
	}
	

	/////////////////////////////////////////////
	SketchBackgroundPanel(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay;

		// grid spacing controls
		JPanel pangridspacingc = new JPanel(new GridLayout(1, 0));
		ButtonGroup buttgp = new ButtonGroup();
		for (int i = -1; i <= 1; i++)
		{
			JRadioButton radbutt = new JRadioButton("", (i == 0));
			radbutt.addActionListener(new radbuttpress(i));
			buttgp.add(radbutt);
			pangridspacingc.add(radbutt);
		}
		tfgridspacing.setEditable(false);
		pangridspacingc.add(tfgridspacing);

		// impossible to get checkboxmenu items to reflect at these places (which would have been ideal)
		// maybe it should update the word on the button
		cbshowbackground = new JCheckBox("Show Background", true);
		cbshowbackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ if (sketchdisplay.miShowBackground.isSelected() != cbshowbackground.isSelected())
				  { sketchdisplay.miShowBackground.doClick();
				  }
				} } );


		cbshowgrid = new JCheckBox("Show Grid", true);
		cbshowgrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ if (sketchdisplay.miShowGrid.isSelected() != cbshowgrid.isSelected())
				  { sketchdisplay.miShowGrid.doClick();
				  }
				} } );

		cbsnaptogrid = new JCheckBox("Snap to Grid", false);
		cbsnaptogrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ if (sketchdisplay.miSnapToGrid.isSelected() != cbsnaptogrid.isSelected())
				  { sketchdisplay.miSnapToGrid.doClick();
				  }
				} } );

		cbbackimage.addActionListener(cbbackimageAL); 

		setLayout(new BorderLayout());

		JPanel panupper = new JPanel(new BorderLayout());
		
		JPanel panuppersec = new JPanel(new GridLayout(0, 2));
		panuppersec.add(cbshowbackground);
		panuppersec.add(new JButton(sketchdisplay.acaAddImage));
		panuppersec.add(new JButton(sketchdisplay.acaMoveBackground));
		panuppersec.add(new JButton(sketchdisplay.acaReloadImage)); 

		panupper.add(panuppersec, BorderLayout.NORTH); 
		panupper.add(cbbackimage, BorderLayout.SOUTH); 

		JPanel panlower = new JPanel(new GridLayout(0, 2));
		panlower.add(cbsnaptogrid); 
		panlower.add(cbshowgrid);
		panlower.add(new JLabel("Grid spacing (lo-hi)")); 
		panlower.add(pangridspacingc);
		panlower.add(new JButton(sketchdisplay.acvSetGridOrig));
		panlower.add(new JButton(sketchdisplay.acvResetGridOrig));

		add(panupper, BorderLayout.NORTH);
		add(panlower, BorderLayout.SOUTH);
	}
};


