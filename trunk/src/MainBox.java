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

import java.util.Vector;
import java.io.File;
import java.io.IOException;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;

import javax.swing.JOptionPane;

/////////////////////////////////////////////
/////////////////////////////////////////////
// the main frame
public class MainBox extends JFrame
{
// the parameters used in this main box

	// the survey tree
	TunnelTree treeview;
	TunnelFileList tunnelfilelist;
	TunnelLoader tunnelloader;

	OneTunnel roottunnel;
	OneTunnel filetunnel;


	// this will keep the global sections, tubes, and sketch in it
	// which a station calculation is lifted into and then operated on.
	OneTunnel otglobal = new OneTunnel("Global", null); // maybe should be moved into stationcalculation.

	// the class that loads and calculates the positions of everything from the data in the tunnels.
	StationCalculation sc = new StationCalculation();

	// single xsection window
	SectionDisplay sectiondisplay = new SectionDisplay(this);

	// wireframe display window
	WireframeDisplay wireframedisplay = new WireframeDisplay(sectiondisplay);

	// the default treeroot with list of symbols.
	OneTunnel vgsymbols = new OneTunnel("gsymbols", null);

	// sketch display window
	SketchDisplay sketchdisplay = new SketchDisplay(this, vgsymbols);

	// text display of the other files.
	TextDisplay textdisplay = new TextDisplay();

	// for previewing images in the directory.
	ImgDisplay imgdisplay = new ImgDisplay();

	/////////////////////////////////////////////
	void MainRefresh()
	{
		roottunnel.RefreshTunnel(vgsymbols);

		// find the active tunnel in this list??
		treeview.RefreshListBox(roottunnel); // or load filetunnel.
	}


	/////////////////////////////////////////////
	void MainClear()
	{
		roottunnel = new OneTunnel("root", null);
		roottunnel.IntroduceSubTunnel(vgsymbols);
		treeview.RefreshListBox(roottunnel);
	}


	/////////////////////////////////////////////
	void MainOpen(boolean bClearFirst, boolean bAuto, int ftype)
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, ftype, bAuto);
		if ((sfiledialog == null) || ((sfiledialog.svxfile == null) && (sfiledialog.tunneldirectory == null)))
			return;

		TN.currentDirectory = sfiledialog.getSelectedFile();

		if (sfiledialog.tunneldirectory == null)
		{
			if (!sfiledialog.svxfile.canRead())
			{
				JOptionPane.showMessageDialog(this, "Cannot open svx file: " + sfiledialog.svxfile.getName());
				return;
			}
			TN.emitMessage("Loading survey file " + sfiledialog.svxfile.getName());
		}
		else if (!sfiledialog.tunneldirectory.isDirectory())
		{
			JOptionPane.showMessageDialog(this, "Cannot open tunnel directory: " + sfiledialog.tunneldirectory.getName());
			return;
		}

		if (bClearFirst)
			MainClear();

		String soname = (sfiledialog.tunneldirectory == null ? sfiledialog.svxfile.getName() : sfiledialog.tunneldirectory.getName());
		int il = soname.indexOf('.');
		if (il != -1)
			soname = soname.substring(0, il);

		// put the tunnel in
		String filetunnname = soname.replace(' ', '_').replace('\t', '_'); // can't cope with spaces.

		// loading directly from a tunnel directory tree
		if (sfiledialog.tunneldirectory != null)
		{
			filetunnel = roottunnel.IntroduceSubTunnel(new OneTunnel(filetunnname, null));
			tunnelloader = new TunnelLoader(vgsymbols, sketchdisplay.sketchlinestyle);

			try
			{
				tunnelloader.FileDirectoryRecurse(filetunnel, sfiledialog.tunneldirectory);
				tunnelloader.LoadFilesRecurse(filetunnel, false);
			}
			catch (IOException ie)
			{
				TN.emitWarning(ie.toString());
				ie.printStackTrace();
			}
			catch (NullPointerException e)
			{
				TN.emitWarning(e.toString());
				e.printStackTrace();
			};
		}

		// loading a survex file
		else
		{
			int lndowntunnels = roottunnel.ndowntunnels;
			new SurvexLoader(sfiledialog.svxfile, roottunnel, sfiledialog.bReadCommentedXSections);
			if (roottunnel.ndowntunnels == lndowntunnels + 1)
			{
				filetunnel = roottunnel.downtunnels[lndowntunnels];

				// case where the tunnel directory is automatically set
				if (filetunnel.tundirectory != null)
					MainSetXMLdir(filetunnel.tundirectory);
   			}
			else
				TN.emitWarning("svx root contains " + roottunnel.ndowntunnels + " primary *begin blocks instead of one");
			if (!roottunnel.vexports.isEmpty() || !roottunnel.vlegs.isEmpty())
				TN.emitWarning("Cave data outside *begin, missing data possible");
		}

		MainRefresh();
	}


	/////////////////////////////////////////////
	void MainSetXMLdir(File ltundirectory)
	{
		if (ltundirectory == null)
		{
			SvxFileDialog sfiledialog = SvxFileDialog.showSaveDialog(TN.currentDirectory, this, SvxFileDialog.FT_DIRECTORY);
			if (sfiledialog == null)
				return;
			TN.currentDirectory = sfiledialog.getSelectedFile();
			ltundirectory = sfiledialog.tunneldirectory;
		}
		if ((ltundirectory != null) && (filetunnel != null))
		{
			TN.emitMessage("Setting tunnel directory tree" + ltundirectory.getName());
			TunnelSaver.ApplyFilenamesRecurse(filetunnel, ltundirectory);
		}
	}

	/////////////////////////////////////////////
	void MainSaveXMLdir()
	{
		// we could save just from selected place on down.
		if ((filetunnel != null) && (filetunnel.tundirectory != null))
			TunnelSaver.SaveFilesRoot(filetunnel, false);
		else
			TN.emitWarning("Need to set the XML dir first");

		// save any edited symbols
		TunnelSaver.SaveFilesRoot(vgsymbols, true);
	}

	/////////////////////////////////////////////
	void MainExit()
	{
		if (JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Quit?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			System.exit(0);
	}


	/////////////////////////////////////////////
	// build a wireframe window.
	void ViewWireframe(boolean bSingleTunnel, OneTunnel disptunnel)
	{
		if (disptunnel == null)
			return;

		if (bSingleTunnel)
		{
			disptunnel.ResetUniqueBaseStationTunnels();
			if (sc.CalcStationPositions(disptunnel, otglobal.vstations) == 0)
				return;
			wireframedisplay.ActivateWireframeDisplay(disptunnel, true);
		}

		else
		{
			sc.CopyRecurseExportVTunnels(otglobal, disptunnel, false);
			if (sc.CalcStationPositions(otglobal, null) == 0)
				return;
			otglobal.dateorder = disptunnel.dateorder;
			wireframedisplay.ActivateWireframeDisplay(otglobal, false);
		}
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// build a sketch window.
	void ViewSketch()
	{
		if (tunnelfilelist.activetunnel == null)
			return;
		if (tunnelfilelist.activetxt != -1)
		{
			textdisplay.ActivateTextDisplay(tunnelfilelist.activetunnel, tunnelfilelist.activetxt);
			return;
		}

		// now make the sketch
		if (tunnelfilelist.activesketchindex != -1)
		{
			// load the sketch if necessary.  Then view it
			Object obj = tunnelfilelist.activetunnel.tsketches.elementAt(tunnelfilelist.activesketchindex); 
			OneSketch activesketch; 
			if (obj instanceof OneSketch)
				activesketch = (OneSketch)obj;
			else
				activesketch = tunnelloader.LoadSketchFile(tunnelfilelist.activetunnel, tunnelfilelist.activesketchindex);
			sketchdisplay.ActivateSketchDisplay(tunnelfilelist.activetunnel, activesketch, true);
		}
	}

	/////////////////////////////////////////////
	// make a new sketch
	void NewSketch()
	{
		if (tunnelfilelist.activetunnel == null)
			return;

		// if new symbols type we should be able to edit the name before creating.

		// find a unique new name.  (this can go wrong, but tire of it).
		int nsknum = tunnelfilelist.activetunnel.tsketches.size() - 1;
		String skname;
		File skfile;
		do
		{
			nsknum++;
			skname = tunnelfilelist.activetunnel.name + "-sketch" + nsknum;
			skfile = new File(tunnelfilelist.activetunnel.tundirectory, skname + TN.SUFF_XML);
		}
		while (skfile.exists());

		// determin if this is the sketch type (needs refining)
		OneSketch tsketch = new OneSketch(tunnelfilelist.activetunnel.GetUniqueSketchFileName());
		if (tunnelfilelist.activetunnel == vgsymbols)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tsketch.bsketchfilechanged = true;

		// load into the structure and view it.
		tunnelfilelist.AddNewSketch(tsketch);
	}




	/////////////////////////////////////////////

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public MainBox()
	{
		super("TunnelX - Cave Drawing Program");
		setLocation(new Point(100, 100));

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
			{ public void windowClosing(WindowEvent event) { MainExit(); } } );

		// setup the menu items
		JMenuItem miClear = new JMenuItem("New");
		miClear.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainClear(); MainRefresh(); } } );

		JMenuItem miOpenXMLDir = new JMenuItem("Open XML dir...");
		miOpenXMLDir.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(true, false, SvxFileDialog.FT_DIRECTORY); } } );

		JMenuItem miOpen = new JMenuItem("Open svx...");
		miOpen.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(true, false, SvxFileDialog.FT_SVX); } } );

		JMenuItem miSetXMLDIR = new JMenuItem("Set XMLDIR");
		miSetXMLDIR.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainSetXMLdir(null); } } );

		JMenuItem miSaveXMLDIR = new JMenuItem("Save XMLDIR");
		miSaveXMLDIR.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainSaveXMLdir(); } } );

		JMenuItem miRefresh = new JMenuItem("Refresh");
		miRefresh.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainRefresh(); } } );

		JMenuItem miExit = new JMenuItem("Exit");
		miExit.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainExit(); } } );


		JMenuItem miWireframe = new JMenuItem("Wireframe");
		miWireframe.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(true, tunnelfilelist.activetunnel); } } );

		JMenuItem miSketch = new JMenuItem("View Sketch");
		miSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewSketch(); } } );

		JMenuItem miNewEmptySketch = new JMenuItem("New Empty Sketch");
		miNewEmptySketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { NewSketch(); } } );

		JMenuItem miCaveBelow = new JMenuItem("Cave Below");
		miCaveBelow.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(false, tunnelfilelist.activetunnel); } } );

		JMenuItem miWholeCave = new JMenuItem("Whole Cave");
		miWholeCave.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(false, roottunnel); } } );

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar();

		JMenu menufile = new JMenu("File");
		menufile.add(miClear);
		menufile.add(miOpenXMLDir);
		menufile.add(miOpen);
		menufile.add(miRefresh);
		menufile.add(miSetXMLDIR);
		menufile.add(miSaveXMLDIR);
		menufile.add(miExit);
		menubar.add(menufile);

		JMenu menutunnel = new JMenu("Tunnel");
		menutunnel.add(miWireframe);
		menutunnel.add(miSketch);
		menutunnel.add(miNewEmptySketch);
		menubar.add(menutunnel);

		JMenu menuview = new JMenu("View");
		menuview.add(miCaveBelow);
		menuview.add(miWholeCave);
		menubar.add(menuview);

		setJMenuBar(menubar);

		// set the listener on the list
		//rhslist.


        //Add the scroll panes to a split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(100);

		// build the left hand area
		treeview = new TunnelTree(this);
		tunnelfilelist = new TunnelFileList(this);


		//JScrollPane rhsview = new JScrollPane(rhslist);

		// the two centre line type panels
        Dimension minimumSize = new Dimension(300, 200);
        treeview.setPreferredSize(minimumSize);
        tunnelfilelist.setPreferredSize(minimumSize);

		splitPane.setLeftComponent(treeview);
		splitPane.setRightComponent(tunnelfilelist);

        //Add the split pane to this frame
        getContentPane().add(splitPane);

		pack();
		setVisible(true);

		// load the symbols from the current working directory.
		// byproduct is it will load the stoke colours too
		sketchdisplay.sketchlinestyle.LoadSymbols(true);
		if (SketchLineStyle.strokew == -1.0F)
			SketchLineStyle.SetStrokeWidths(0.625F);
		MainClear();
	}



	/////////////////////////////////////////////
	// startup the program
    public static void main(String args[])
	{
		// set the verbose flag
		int i = 0;
		while (args.length > i)
		{
			if (args[i].equals("--verbose"))
			{
				TN.bVerbose = true;
				i++;
			}

			else if (args[i].equals("--quiet"))
			{
				TN.bVerbose = false;
				i++;
			}
			break;
		}

		// start-up
		MainBox mainbox = new MainBox();

		// do the filename
		if (args.length == i + 1)
		{
			TN.currentDirectory = new File(args[i]);
			mainbox.MainOpen(true, true, (TN.currentDirectory.isDirectory() ? SvxFileDialog.FT_DIRECTORY : SvxFileDialog.FT_SVX));
		}
	}
}

