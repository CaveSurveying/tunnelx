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


/////////////////////////////////////////////
class SketchBackgroundPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JComboBox jcbbackground = new JComboBox();
	JCheckBox cbshowbackground;
	JCheckBox cbshowgrid;

	// tells us the grid spacing.
    JTextField tfgridspacing = new JTextField("");
	int gsoffset = 0;

	static String[] imagefiledirectories = new String[10];
	static int nimagefiledirectories = 0;

	static void AddImageFileDirectory(String limagefiledirectory)
	{
		imagefiledirectories[nimagefiledirectories++] = limagefiledirectory;
	}

	// this goes up the directories looking in them for the iname file
	// and for any subdirectories called
	static FileAbstraction GetImageFile(FileAbstraction idir, String iname)
	{
		// recurse up the file structure
		while (idir != null)
		{
			// check if this image file is in the directory
			FileAbstraction res = FileAbstraction.MakeDirectoryAndFileAbstraction(idir, iname);
			if (res.isFile())
				return res;

			// check if it is in one of the image file subdirectories
			for (int i = nimagefiledirectories - 1; i >= 0; i--)
			{
				FileAbstraction lidir = FileAbstraction.MakeDirectoryAndFileAbstraction(idir, imagefiledirectories[i]);
				if (lidir.isDirectory())
				{
					res = FileAbstraction.MakeDirectoryAndFileAbstraction(lidir, iname);
					if (res.isFile())
						return res;
				}
			}

			// recurse up the hierarchy
			idir = idir.getParentFile();
		}
		return null;
	}


	// we have to decode the file to find something that will satisfy the function above
	static String GetImageFileName(FileAbstraction idir, FileAbstraction ifile) throws IOException
	{
		// we need to find a route which takes us here
		String sfiledir = ifile.getParentFile().getCanonicalPath();
		FileAbstraction ridir = FileAbstraction.MakeCanonical(idir);
		while (ridir != null)
		{
			String sdir = ridir.getCanonicalPath();
			if (sfiledir.startsWith(sdir))
			{
				// look through the image file directories to find one that takes us down towards the file
				FileAbstraction lridir = null;
				for (int i = nimagefiledirectories - 1; i >= 0; i--) // in reverse so last ones have highest priority
				{
					FileAbstraction llridir = FileAbstraction.MakeDirectoryAndFileAbstraction(ridir, imagefiledirectories[i]);
					if (llridir.isDirectory())
					{
						String lsdir = llridir.getCanonicalPath();
						if (sfiledir.startsWith(lsdir))
						{
							lridir = llridir;
							break;
						}
					}
				}

				// found an image directory which is part of the stem
				if (lridir != null)
				{
					ridir = lridir;
					break;
				}
			}
			ridir = ridir.getParentFile(); // keep going up.
		}
		if (ridir == null)
		{
			TN.emitWarning("No common stem found");
			System.out.println(idir.getCanonicalPath());
			System.out.println(ifile.getCanonicalPath());
			return null;
		}

		// find the root of which sdir is the stem
		StringBuffer sbres = new StringBuffer();
		FileAbstraction lifile = ifile;
		while (lifile != null)
		{
			if (sbres.length() != 0)
				sbres.insert(0, "/");
			sbres.insert(0, lifile.getName());
			lifile = lifile.getParentFile();
			if ((lifile == null) || lifile.equals(ridir))
				break;
		}


		String sres = sbres.toString();
		TN.emitMessage("Making stem file: " + sres);
		FileAbstraction tifile = GetImageFile(idir, sres);
		if ((tifile != null) && ifile.equals(tifile))
			return sres;

		TN.emitWarning("Stem file failure: " + idir.toString() + "  " + ifile.toString());
		return null;
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
		FileAbstraction lastfile = sketchdisplay.sketchgraphicspanel.tsketch.sketchfile;
		int ib = sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel;
		if (ib == -1)
			ib = sketchdisplay.sketchgraphicspanel.tsketch.backgroundimgnamearr.size() - 1;
		if (ib != -1)
		{
			FileAbstraction llastfile = GetImageFile(lastfile, sketchdisplay.sketchgraphicspanel.tsketch.backgroundimgnamearr.get(ib));
			if (llastfile != null)
				lastfile = llastfile;
		}

		SvxFileDialog sfd = SvxFileDialog.showOpenDialog(lastfile, sketchdisplay, SvxFileDialog.FT_BITMAP, false);
		if ((sfd == null) || (sfd.svxfile == null))
			return;
		String imfilename = null;
		try
		{
			imfilename = GetImageFileName(sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getParentFile(), sfd.svxfile);
		}
		catch (IOException ie)
		{ TN.emitWarning(ie.toString()); };

		if (imfilename != null)
		{
			sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel = sketchdisplay.sketchgraphicspanel.tsketch.AddBackground(imfilename, null);
			jcbbackground.addItem(imfilename);

			jcbbackground.setSelectedIndex(sketchdisplay.sketchgraphicspanel.tsketch.ibackgroundimgnamearrsel);

			if (!sketchdisplay.miShowBackground.isSelected())
				sketchdisplay.miShowBackground.doClick();
		}
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

		// impossible to get checkboxmenu items to reflect at these places (which would have been ideal)
		// maybe it should update the word on the button
		cbshowbackground = new JCheckBox("Show Background");
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


		setLayout(new BorderLayout());
		add("North", jcbbackground);

		JPanel panlower = new JPanel();
		panlower.setLayout(new GridLayout(0, 2));

		panlower.add(cbshowbackground);
		panlower.add(new JButton(sketchdisplay.acaMoveBackground));
		panlower.add(cbshowgrid);
		panlower.add(new JButton(sketchdisplay.acaAddImage));

		panlower.add(new JLabel("Grid spacing"));
		panlower.add(pangridspacingc);

		panlower.add(new JButton(sketchdisplay.acvSetGridOrig));
		panlower.add(new JButton(sketchdisplay.acvResetGridOrig));

		add("Center", panlower);
	}
};


