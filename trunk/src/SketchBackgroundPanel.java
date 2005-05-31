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
import javax.swing.JComboBox;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import java.util.Vector;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import java.awt.geom.Point2D;


/////////////////////////////////////////////
class SketchBackgroundPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JComboBox jcbbackground = new JComboBox();


	// tells us the grid spacing.
    JTextField tfgridspacing = new JTextField("");
	int gsoffset = 0;

	static String[] imagefiledirectories = new String[10];
	static int nimagefiledirectories = 0;


	// this goes up the directories looking in them for the iname file
	// and for any subdirectories called
	static File GetImageFile(File idir, String iname)
	{
		// recurse up the file structure
		while (true)
		{
			// check if this image file is in the directory
			File res = new File(idir, iname);
			if (res.isFile())
				return res;

			// check if it is in one of the image file subdirectories
			for (int i = 0; i < nimagefiledirectories; i++)
			{
				File lidir = new File(idir, imagefiledirectories[i]);
				if (lidir.isDirectory())
				{
					res = new File(lidir, iname);
					if (res.isFile())
						return res;
				}
			}

			// recurse up the hierarchy
			if (idir == null)
				break;
			idir = idir.getParentFile();
		}
		return null;
	}

//	static String[] imagefiledirectories = new String[10];

	// we have to decode the file to find something that will satisfy the function above
	static String GetImageFileName(File ifile)
	{
String iname = ifile.getName();
while (ifile != null)
{
	System.out.println("File:: " + ifile.toString());
	ifile = ifile.getParentFile();
}
return iname; 
/*
		iname = ifile.getName();

		File imfile = GetImageFile(idir, iname);
		if (ifile.equals(imfile))
			return iname;

		// work way up the chain looking for occurances of imagedirname
		File difile = ifile.getParentFile();
		File nfile = new File(ifile.getName());
		while (difile != null)
		{
			if (imagedirname.equals(difile.getName()))
			{
				File imfile = GetImageFile(idir, iname);
				if (ifile.equals(imfile))
					return iname;
			}
			nfile = new File(difile.getName(), nfile);
			difile = difile.getParentFile();
System.out.println("file splitting :  " + difile.toString() + ",,,," + nfile.toString());
		}
		return null;

//called by ShowBackgroundImage in SketchDisplay, and when the file dialog box is made
*/
	}


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
	void RemoveBackgroundFile()
	{
		sketchdisplay.miShowBackground.setSelected(false);
		if (sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel != -1)
		{
			jcbbackground.removeItemAt(sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel);
			sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel = -1;
			jcbbackground.setSelectedIndex(-1);
		}
	}

	/////////////////////////////////////////////
	void NewBackgroundFile()
	{
		// try and find a good starting point for the file
		File lastfile = sketchdisplay.sketchgraphicspanel.tsketch.sketchfile;
		int ib = sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel;
		if (ib == -1)
			ib = sketchdisplay.sketchgraphicspanel.tsketch.backgroundimgnamearr.size() - 1;
		if (ib != -1)
		{
			File llastfile = GetImageFile(lastfile, (String)sketchdisplay.sketchgraphicspanel.tsketch.backgroundimgnamearr.elementAt(ib));
			if (llastfile != null)
				lastfile = llastfile;
		}

		SvxFileDialog sfd = SvxFileDialog.showOpenDialog(lastfile, sketchdisplay, SvxFileDialog.FT_BITMAP, false);
		if ((sfd == null) || (sfd.svxfile == null))
			return;
		String imfilename = GetImageFileName(sfd.svxfile);
		sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel = sketchdisplay.sketchgraphicspanel.tsketch.AddBackground(imfilename, null);
		jcbbackground.addItem(imfilename);

		jcbbackground.setSelectedIndex(sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel);
		sketchdisplay.miShowBackground.setSelected(true);
	}


	/////////////////////////////////////////////
	SketchBackgroundPanel(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay;

		// background panel
		jcbbackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel = jcbbackground.getSelectedIndex();
				  if (sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel != -1)
					  sketchdisplay.ShowBackgroundImage(sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel);
// would like this to be set, but it comes on when we are updating the combobox too
//				  sketchdisplay.miShowBackground.setSelected(true);
			} } );


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

		JButton buttnewbackgroundfile = new JButton("Add Image");
		buttnewbackgroundfile.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { NewBackgroundFile(); } } );

		JButton buttremovebackgroundfile = new JButton("Remove Image");
		buttremovebackgroundfile.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { RemoveBackgroundFile(); } } );

		// impossible to get checkboxmenu items to reflect at these places (which would have been ideal)
		// maybe it should update the word on the button
		JButton butttogglebackground = new JButton("Toggle Background");
		butttogglebackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowBackground.setState(!sketchdisplay.miShowBackground.getState()); sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );


		JButton butthidegrid = new JButton("Hide Grid");
		butthidegrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowGrid.setSelected(false);  sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );
		JButton buttshowgrid = new JButton("Show Grid");
		buttshowgrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowGrid.setSelected(true);  sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );



		setLayout(new BorderLayout());
		add("North", jcbbackground);

		JPanel panlower = new JPanel();
		panlower.setLayout(new GridLayout(0, 2));

		JPanel bggrdpan = new JPanel(new GridLayout(2, 0));
		bggrdpan.add(butthidegrid);
		bggrdpan.add(buttshowgrid);
		panlower.add(bggrdpan);

		JPanel bgfilepan = new JPanel(new GridLayout(2, 0));
		bgfilepan.add(buttnewbackgroundfile);
		bgfilepan.add(buttremovebackgroundfile);
		panlower.add(bgfilepan);

		panlower.add(butttogglebackground);
		panlower.add(new JButton(sketchdisplay.acaMoveBackground));

		panlower.add(new JLabel("Grid spacing"));
		panlower.add(pangridspacingc);

		panlower.add(new JButton(sketchdisplay.acvSetGridOrig));
		panlower.add(new JButton(sketchdisplay.acvResetGridOrig));

		add("Center", panlower);
	}
};


