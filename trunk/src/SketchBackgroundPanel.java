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

	static List<String> imagefiledirectories = new ArrayList<String>();
	static void AddImageFileDirectory(String newimagefiledirectory)
	{
		for (String limagefiledirectory : imagefiledirectories)
		{
			if (limagefiledirectory.equals(newimagefiledirectory))
			{
				TN.emitMessage("Already have imagefiledirectory: " + limagefiledirectory); 
				return; 
			}
		}
		imagefiledirectories.add(newimagefiledirectory); 
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
			for (int i = imagefiledirectories.size() - 1; i >= 0; i--)
			{
				FileAbstraction lidir = FileAbstraction.MakeDirectoryAndFileAbstraction(idir, imagefiledirectories.get(i));
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
				for (int i = imagefiledirectories.size() - 1; i >= 0; i--) // in reverse so last ones have highest priority
				{
					FileAbstraction llridir = FileAbstraction.MakeDirectoryAndFileAbstraction(ridir, imagefiledirectories.get(i));
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
	void NewBackgroundFile()
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectoryIMG, sketchdisplay, SvxFileDialog.FT_BITMAP, false);
		if ((sfiledialog == null) || (sfiledialog.svxfile == null))
			return;
		TN.currentDirectoryIMG = sfiledialog.getSelectedFileA();
		String imfilename = null;
		try
		{
			imfilename = GetImageFileName(sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getParentFile(), sfiledialog.svxfile);
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
		gop.plabedl.sketchframedef.SetSketchFrameFiller(sketchdisplay.mainbox, sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale, sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset);

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


		setLayout(new BorderLayout());

		JPanel panupper = new JPanel(new GridLayout(0, 2));
		panupper.add(cbshowbackground);
		panupper.add(new JButton(sketchdisplay.acaAddImage));
		panupper.add(new JButton(sketchdisplay.acaMoveBackground));
		panupper.add(new JButton(sketchdisplay.acaReloadImage)); 

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

	/////////////////////////////////////////////
	void UpdateBackgroundControls(OnePath op) 
	{
		System.out.println("--background controls update: " + op); 
	}
	
};


