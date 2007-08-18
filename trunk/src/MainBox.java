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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

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

import javax.swing.JOptionPane;

import javax.swing.JApplet;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.ClassLoader;

import java.util.List; 
import java.util.ArrayList; 

/////////////////////////////////////////////
/////////////////////////////////////////////
// the main frame
public class MainBox
	extends JFrame
//	extends JApplet // AppletConversion
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


	// single xsection window and wireframe display
	SectionDisplay sectiondisplay = new SectionDisplay();
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
		roottunnel.RefreshTunnelFromSVX();
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
	void LoadTunnelDirectoryTree(String filetunnname, FileAbstraction tunneldirectory)
	{
		filetunnel = roottunnel.IntroduceSubTunnel(new OneTunnel(filetunnname, null));
		tunnelloader = new TunnelLoader(vgsymbols, sketchdisplay.sketchlinestyle);

		try
		{
			FileAbstraction.FileDirectoryRecurse(filetunnel, tunneldirectory);
			tunnelloader.LoadFilesRecurse(filetunnel);
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

		// update any symbols information that may have showed up in the process
		if (sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating)
			sketchdisplay.sketchlinestyle.UpdateSymbols(false);
	}

	/////////////////////////////////////////////
	void MainOpen(boolean bClearFirst, boolean bAuto, int ftype)
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, ftype, bAuto);

		if ((sfiledialog == null) || ((sfiledialog.svxfile == null) && (sfiledialog.tunneldirectory == null)))
			return;

		TN.currentDirectory = sfiledialog.getSelectedFileA();

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
			LoadTunnelDirectoryTree(filetunnname, sfiledialog.tunneldirectory);

		// loading a survex file
		else
		{
			int lndowntunnels = roottunnel.vdowntunnels.size(); // allows for more than one SVX to be loaded in
			new SurvexLoader(sfiledialog.svxfile, roottunnel, sfiledialog.bReadCommentedXSections);
			if (roottunnel.vdowntunnels.size() == lndowntunnels + 1)
			{
				filetunnel = roottunnel.vdowntunnels.get(lndowntunnels);

				// case where the tunnel directory is automatically set
				if (filetunnel.tundirectory != null)
					MainSetXMLdir(filetunnel.tundirectory);
   			}
			else
				TN.emitWarning("svx root contains " + (roottunnel.vdowntunnels.size() - lndowntunnels) + " primary *begin blocks instead of one");
			if (!roottunnel.vexports.isEmpty() || !roottunnel.vlegs.isEmpty())
				TN.emitWarning("Cave data outside *begin, missing data possible");
		}

		MainRefresh();
	}

	/////////////////////////////////////////////
	void LoadAllSketchesRecurse(OneTunnel tunnel)
	{
		for (OneSketch tsketch : tunnel.tsketches)
		{
			if (!tsketch.bsketchfileloaded)
				tunnelloader.LoadSketchFile(tunnel, tsketch, true);
		}
		for (OneTunnel downtunnel : tunnel.vdowntunnels)
			LoadAllSketchesRecurse(downtunnel);
	}


	/////////////////////////////////////////////
	void MainSetXMLdir(FileAbstraction ltundirectory)
	{
		if (ltundirectory == null)
		{
			SvxFileDialog sfiledialog = SvxFileDialog.showSaveDialog(TN.currentDirectory, this, SvxFileDialog.FT_DIRECTORY);
			if (sfiledialog == null)
				return;
			TN.currentDirectory = sfiledialog.getSelectedFileA();
			ltundirectory = sfiledialog.tunneldirectory;
		}
		if ((ltundirectory != null) && (filetunnel != null))
		{
			TN.emitMessage("Loading all sketches");
			LoadAllSketchesRecurse(filetunnel);
			TN.emitMessage("Setting tunnel directory tree" + ltundirectory.getName());
			FileAbstraction.ApplyFilenamesRecurse(filetunnel, ltundirectory);
			tunnelfilelist.tflist.repaint();
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
		tunnelfilelist.tflist.repaint(); 
	}

	/////////////////////////////////////////////
	void ApplySplineChange()
	{
		
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
			//if ((tunnelfilelist.activetunnel.posfile != null) && (tunnelfilelist.activetunnel.vposlegs == null))
			//	TunnelLoader.LoadPOSdata(tunnelfilelist.activetunnel); 
			if (sc.CalcStationPositions(disptunnel, otglobal.vstations, disptunnel.name) <= 0)
				return;
			wireframedisplay.ActivateWireframeDisplay(disptunnel, true);
		}

		else
		{
			sc.CopyRecurseExportVTunnels(otglobal, disptunnel, true);
			if ((tunnelfilelist.activetunnel.posfile != null) && (tunnelfilelist.activetunnel.vposlegs == null))
				TunnelLoader.LoadPOSdata(tunnelfilelist.activetunnel); 
			tunnelfilelist.tflist.repaint(); 
			if (sc.CalcStationPositions(otglobal, null, disptunnel.name) <= 0)
				return;
			otglobal.mdatepos = disptunnel.mdatepos;
			wireframedisplay.ActivateWireframeDisplay(otglobal, false);
		}
	}

	/////////////////////////////////////////////
	boolean OperateProcess(ProcessBuilder pb, String pname)
	{
		try
		{
		pb.redirectErrorStream(true); 
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) 
			TN.emitMessage(" " + pname + ":: " + line);
		int ires = p.waitFor(); 
		if (ires == 0)
			return true; 
		}
		catch (IOException ie)
		{
			TN.emitWarning("@@ caught exception"); 
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		catch (InterruptedException ie)
		{
			TN.emitWarning("@@ caught Interrupted exception"); 
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		return false; 
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// build a sketch window.
	void ViewSketch()
	{
		if (tunnelfilelist.activetunnel == null)
			TN.emitWarning("No tunnel selected");

		else if (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_3D)
		{
			assert tunnelfilelist.activetunnel.t3dfile != null;
			ProcessBuilder pb = new ProcessBuilder(TN.survexexecutabledir + "aven", tunnelfilelist.activetunnel.t3dfile.getPath());
			pb.directory(tunnelfilelist.activetunnel.tundirectory.localfile);
			OperateProcess(pb, "aven.exe");
		}

		else if (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_FONTCOLOURS)
		{
			textdisplay.ActivateTextDisplay(tunnelfilelist.activetunnel, tunnelfilelist.activetxt, tunnelfilelist.activesketchindex);  // for seeing in text window

			// used to do the reload
			/*sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating = true;
			tunnelfilelist.tflist.repaint(); // used to make it at least blink
			tunnelloader.ReloadFontcolours(tunnelfilelist.activetunnel, tunnelfilelist.activesketchindex);
			if (sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating)
				sketchdisplay.sketchlinestyle.UpdateSymbols(false);
			if (sketchdisplay.sketchgraphicspanel.tsketch != null)
				SketchGraphics.SketchChangedStatic(SketchGraphics.SC_CHANGE_SAS, sketchdisplay.sketchgraphicspanel.tsketch, sketchdisplay);
			tunnelfilelist.tflist.repaint();*/
		}

		// now make the sketch
		else if (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_SKETCH)
		{
			// load the sketch if necessary.  Then view it
			OneSketch activesketch = tunnelfilelist.activetunnel.tsketches.get(tunnelfilelist.activesketchindex);
			if (!activesketch.bsketchfileloaded)
				tunnelloader.LoadSketchFile(tunnelfilelist.activetunnel, activesketch, true);
			sketchdisplay.ActivateSketchDisplay(tunnelfilelist.activetunnel, activesketch, true);
		}

		// rest are text types
		else if ((tunnelfilelist.activetxt == FileAbstraction.FA_FILE_SVX) || 
				 (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_MEASUREMENTS) ||
				 (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_EXPORTS) ||
				 (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_POS))
			textdisplay.ActivateTextDisplay(tunnelfilelist.activetunnel, tunnelfilelist.activetxt, -1);

		tunnelfilelist.tflist.repaint(); 
	}

	/////////////////////////////////////////////
	// make a new sketch
	void NewSketch()
	{
		if (tunnelfilelist.activetunnel == null)
			return;

		// if new symbols type we should be able to edit the name before creating.

		// find a unique new name.  (this can go wrong, but tired of it).
		int nsknum = tunnelfilelist.activetunnel.tsketches.size() - 1;

		// determin if this is the sketch type (needs refining)
		OneSketch tsketch = new OneSketch(tunnelfilelist.activetunnel.GetUniqueSketchFileName(), tunnelfilelist.activetunnel);
		if (tunnelfilelist.activetunnel == vgsymbols)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tsketch.SetupSK(); 
		tsketch.bsketchfilechanged = true;

		// load into the structure and view it.
		assert tsketch.bsketchfileloaded; 
		tunnelfilelist.activetunnel.tsketches.add(tsketch);
		tunnelfilelist.RemakeTFList();
		tunnelfilelist.tflist.setSelectedIndex(tunnelfilelist.isketche - 1);
		tunnelfilelist.UpdateSelect(true); // doubleclicks it.
	}

	/////////////////////////////////////////////
	void SvxGenPosfile(OneTunnel ot)
	{
		if ((ot == null) || (ot == vgsymbols) || (ot.tundirectory == null) || (ot.svxfile == null))
			return; 
		
		if (TN.survexexecutabledir.equals(""))
			TN.emitError("Missing <survex_executable_directory> from fontcolours");  
			
		// overwrite those intermediate files if they exist, because there can only be one of each per directory.  
		// 
		FileAbstraction l3dfile = (ot.t3dfile != null ? ot.t3dfile : FileAbstraction.MakeDirectoryAndFileAbstraction(ot.tundirectory, TN.setSuffix(ot.svxfile.getName(), TN.SUFF_3D))); 
		FileAbstraction lposfile = (ot.posfile != null ? ot.posfile : FileAbstraction.MakeDirectoryAndFileAbstraction(ot.tundirectory, TN.setSuffix(ot.svxfile.getName(), TN.SUFF_POS))); 

		List<String> cmds = new ArrayList<String>(); 
		cmds.add(TN.survexexecutabledir + "cavern"); 
		cmds.add("--no-auxiliary-files"); 
		cmds.add("--quiet"); // or -qq for properly quiet
		cmds.add("-o"); 
		cmds.add(l3dfile.getPath()); 
		cmds.add(ot.svxfile.getPath()); 

		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(ot.tundirectory.localfile);
		if (OperateProcess(pb, "cavern.exe"))
		{
			cmds.clear(); 
			cmds.add(TN.survexexecutabledir + "3dtopos"); 
			cmds.add(l3dfile.getPath()); 
			cmds.add(lposfile.getPath()); 

			//System.out.println("SVX path: " + tunnelfilelist.activetunnel.svxfile.getPath()); 
			ProcessBuilder pb3 = new ProcessBuilder(cmds);
			pb3.directory(ot.tundirectory.localfile);
			if (OperateProcess(pb3, "cavern.exe"))
			{
				ot.t3dfile = l3dfile;
				ot.posfile = lposfile;
				ot.vposlegs = null; 
				//LoadPOSdata(tunnel);
				tunnelfilelist.RemakeTFList();
			}
		}
		tunnelfilelist.RemakeTFList();
	}

	/////////////////////////////////////////////

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public MainBox()
	{
		// hide for AppletConversion
		setTitle("TunnelX - Cave Drawing Program");
		setLocation(new Point(100, 100));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
			{ public void windowClosing(WindowEvent event) { MainExit(); } } );
		
		// applet type
//		docbaseurl = getDocumentBase();
//		System.out.println(getDocumentBase());
	}

// need to load the fontcolours.xml which will then call in a bunch of symbols that need to be loaded
// into vgsymbols.
// each of these resources comes in as a name
//
// LoadSymbols

    public void init()
	{
		FileAbstraction.InitFA();  // at this point we know if it's applet or not

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

		JMenuItem miRefresh = new JMenuItem("Refreshhh");
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

		JMenuItem miSVXPOSfile = new JMenuItem("Survex gen Posfile");
		miSVXPOSfile.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SvxGenPosfile(tunnelfilelist.activetunnel); } } );

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
		if (!FileAbstraction.bIsApplet)  // or use disable function on them to grey them out.
		{
			menufile.add(miOpenXMLDir);
			menufile.add(miOpen);
		}
		menufile.add(miRefresh);
		if (!FileAbstraction.bIsApplet)
		{
			menufile.add(miSetXMLDIR);
			menufile.add(miSaveXMLDIR);
			menufile.add(miExit);
		}
		menubar.add(menufile);

		JMenu menutunnel = new JMenu("Tunnel");
		menutunnel.add(miSVXPOSfile);
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

		pack();  //hide for AppletConversion
		setVisible(true);

		// load the symbols from the current working directory.
		// byproduct is it will load the stoke colours too
		sketchdisplay.sketchlinestyle.LoadSymbols(FileAbstraction.currentSymbols);

		assert sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating; 
		sketchdisplay.sketchlinestyle.UpdateSymbols(true);
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
		FileAbstraction.bIsApplet = false;
		TN.currentDirectory = FileAbstraction.MakeCurrentUserDirectory();
		MainBox mainbox = new MainBox();
		mainbox.init();  // the init gets called

		// do the filename
		if (args.length == i + 1)
		{
			TN.currentDirectory = FileAbstraction.MakeWritableFileAbstraction(args[i]);
			TN.currentDirectory = FileAbstraction.MakeCanonical(TN.currentDirectory); 
			mainbox.MainOpen(true, true, (TN.currentDirectory.isDirectory() ? SvxFileDialog.FT_DIRECTORY : SvxFileDialog.FT_SVX));
		}
	}

	/////////////////////////////////////////////
	boolean bFileLoaded = false;
    public void start()
	{
		assert FileAbstraction.bIsApplet;
		if (bFileLoaded)
			return;

		ClassLoader cl = MainBox.class.getClassLoader();
		TN.currentDirectory = new FileAbstraction();
// uncomment for AppletConversion
/*		TN.currentDirectory.localurl = cl.getResource(getParameter("cavedir") + "/");
String fullcavedir = getParameter("fullcavedir");
if (fullcavedir != null)
{
try { TN.currentDirectory.localurl = new URL(fullcavedir); }
catch (MalformedURLException e) {;}
}
		TN.currentDirectory.bIsDirType = true;
System.out.println("inputdir: " + getParameter("cavedir"));
System.out.println("currentdir: " + TN.currentDirectory.localurl);
//		MainOpen(true, true, SvxFileDialog.FT_DIRECTORY);
LoadTunnelDirectoryTree("cavecave", TN.currentDirectory);
*/		MainRefresh();
		bFileLoaded = true;
	}

	/////////////////////////////////////////////
    public void stop()
	{
    }

	/////////////////////////////////////////////
    public void destroy()
	{
    }

	/////////////////////////////////////////////
    public String getAppletInfo()
	{
        return "tunnelx applet.  Protected by the GPL";
    }

}

