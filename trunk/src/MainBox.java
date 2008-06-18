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
import javax.swing.JCheckBoxMenuItem; 

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
	TunnelFileList tunnelfilelist = new TunnelFileList(this);
	TunnelLoader tunnelloader;
	JCheckBoxMenuItem miViewSymbolsList; 

	OneTunnel filetunnel = new OneTunnel("clean");

	// single xsection window and wireframe display
	WireframeDisplay wireframedisplay = new WireframeDisplay();

	// the default treeroot with list of symbols.
	OneTunnel vgsymbols = new OneTunnel("gsymbols");

	// sketch display window
	SketchDisplay sketchdisplay = new SketchDisplay(this, vgsymbols);


	/////////////////////////////////////////////
	void MainRefresh()
	{
		tunnelfilelist.RemakeTFList(); 
	}

	/////////////////////////////////////////////
	void LoadTunnelDirectoryTree(String filetunnname, FileAbstraction tunneldirectory)
	{
		filetunnel = new OneTunnel(filetunnname);
		tunnelloader = new TunnelLoader(vgsymbols, sketchdisplay.sketchlinestyle);

		try
		{
			filetunnel.tundirectory = tunneldirectory;
			FileAbstraction.FindFilesOfDirectory(filetunnel);   
			tunnelloader.LoadFontcolours(filetunnel.tfontcolours);
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
			filetunnel = new OneTunnel("clean2");

		String soname = (sfiledialog.tunneldirectory == null ? sfiledialog.svxfile.getName() : sfiledialog.tunneldirectory.getName());
		int il = soname.indexOf('.');
		if (il != -1)
			soname = soname.substring(0, il);

		// put the tunnel in
		String filetunnname = soname.replace(' ', '_').replace('\t', '_'); // can't cope with spaces.

		// loading directly from a tunnel directory tree
		if (sfiledialog.tunneldirectory != null)
			LoadTunnelDirectoryTree(filetunnname, sfiledialog.tunneldirectory);

		else if ((sfiledialog.svxfile != null) && (ftype == SvxFileDialog.FT_XMLSKETCH))
		{
			if (GetActiveTunnel() != null)
{
				OneSketch tsketch = new OneSketch(sfiledialog.svxfile, GetActiveTunnel()); 
				if (GetActiveTunnel() == vgsymbols)
				{
					tsketch.sketchsymbolname = tsketch.sketchfile.getName();
					tsketch.bSymbolType = true;
				}
				GetActiveTunnel().tsketches.add(tsketch);
				tunnelfilelist.RemakeTFList();
				tunnelfilelist.tflist.setSelectedIndex(tunnelfilelist.isketche - 1);
				tunnelfilelist.UpdateSelect(true); // doubleclicks it.
System.out.println(GetActiveTunnel().name + " -EEE- " + GetActiveTunnel().tsketches.size());
}
		}
		
		// loading a survex file
		else
		{
TN.emitWarning("no more"); 
		}

		MainRefresh();
	}


	/////////////////////////////////////////////
	void MainSaveAll()
	{
		for (OneSketch lsketch : filetunnel.tsketches)
			lsketch.SaveSketch();
		for (OneSketch lsketch : vgsymbols.tsketches)
			lsketch.SaveSketch();
		tunnelfilelist.tflist.repaint(); 
	}


	/////////////////////////////////////////////
	void MainExit()
	{
		if (JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Quit?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			System.exit(0);
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
		if (GetActiveTunnel() == null)
			TN.emitWarning("No tunnel selected");

		// now make the sketch
		if (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_SKETCH)
		{
			// load the sketch if necessary.  Then view it
			OneSketch activesketch = GetActiveTunnel().tsketches.get(tunnelfilelist.activesketchindex);
			if (!activesketch.bsketchfileloaded)
				tunnelloader.LoadSketchFile(activesketch, true);
			sketchdisplay.ActivateSketchDisplay(activesketch, true);
		}
		tunnelfilelist.tflist.repaint(); 
	}

	/////////////////////////////////////////////
	// make a new sketch
	void NewSketch()
	{
		if (GetActiveTunnel() == null)
			return;

		// if new symbols type we should be able to edit the name before creating.

		// find a unique new name.  (this can go wrong, but tired of it).
		int nsknum = GetActiveTunnel().tsketches.size() - 1;

		// determin if this is the sketch type (needs refining)
		OneSketch tsketch = new OneSketch(GetActiveTunnel().GetUniqueSketchFileName(), GetActiveTunnel());
		if (GetActiveTunnel() == vgsymbols)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tsketch.SetupSK();
		tsketch.bsketchfilechanged = true;

		// load into the structure and view it.
		assert tsketch.bsketchfileloaded;
		GetActiveTunnel().tsketches.add(tsketch);
		tunnelfilelist.RemakeTFList();
		tunnelfilelist.tflist.setSelectedIndex(tunnelfilelist.isketche - 1);
		tunnelfilelist.UpdateSelect(true); // doubleclicks it.
	}


	/////////////////////////////////////////////
	boolean RunCavern(FileAbstraction ldirectory, FileAbstraction lsvxfile, FileAbstraction l3dfile, FileAbstraction lposfile)
	{
		List<String> cmds = new ArrayList<String>();
		cmds.add(TN.survexexecutabledir + "cavern");
		cmds.add("--no-auxiliary-files");
		cmds.add("--quiet"); // or -qq for properly quiet
		cmds.add("-o");
		cmds.add(l3dfile.getPath());
		cmds.add(lsvxfile.getPath());

		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(ldirectory.localfile);
		if (!OperateProcess(pb, "cavern.exe"))
			return false;
		if (!l3dfile.exists())
			return false;
		if (lposfile == null)
			return true;

		cmds.clear();
		cmds.add(TN.survexexecutabledir + "3dtopos");
		cmds.add(l3dfile.getPath());
		cmds.add(lposfile.getPath());

		//System.out.println("SVX path: " + tunnelfilelist.activetunnel.svxfile.getPath());
		ProcessBuilder pb3 = new ProcessBuilder(cmds);
		pb3.directory(ldirectory.localfile);
		if (!OperateProcess(pb3, "3dtopos.exe"))
			return false;
		if (!lposfile.exists())
			return false;

		return true;
	}

	/////////////////////////////////////////////
	boolean RunAven(FileAbstraction ldirectory, FileAbstraction l3dfile)
	{
		assert l3dfile != null;
		ProcessBuilder pb = new ProcessBuilder(TN.survexexecutabledir + "aven", l3dfile.getPath());
		pb.directory(ldirectory.localfile);
		OperateProcess(pb, "aven.exe");
		return true;
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
		//System.out.println(getDocumentBase());
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
			{ public void actionPerformed(ActionEvent event) { filetunnel = null;  MainRefresh(); } } );

		JMenuItem miOpenXMLDir = new JMenuItem("Open Sketches Directory...");
		miOpenXMLDir.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(true, false, SvxFileDialog.FT_DIRECTORY); } } );

		JMenuItem miOpen = new JMenuItem("Open Sketch...");
		miOpen.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(false, false, SvxFileDialog.FT_XMLSKETCH); } } );

		JMenuItem miSaveAll = new JMenuItem("Save All");
		miSaveAll.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainSaveAll(); } } );

		JMenuItem miRefresh = new JMenuItem("Refreshhh");
		miRefresh.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainRefresh(); } } );

		JMenuItem miExit = new JMenuItem("Exit");
		miExit.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainExit(); } } );

		JMenuItem miSketch = new JMenuItem("View Sketch");
		miSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewSketch(); } } );

		miViewSymbolsList = new JCheckBoxMenuItem("Symbols List", false);
		miViewSymbolsList.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { tunnelfilelist.RemakeTFList(); } } ); 

		JMenuItem miNewEmptySketch = new JMenuItem("New Empty Sketch");
		miNewEmptySketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { NewSketch(); } } );

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
			menufile.add(miSaveAll);
			menufile.add(miExit);
		}
		menubar.add(menufile);

		JMenu menutunnel = new JMenu("Tunnel");
		menutunnel.add(miViewSymbolsList);
		menutunnel.add(miSketch);
		menutunnel.add(miNewEmptySketch);
		menubar.add(menutunnel);

		setJMenuBar(menubar);

        tunnelfilelist.setPreferredSize(new Dimension(500, 300));
        getContentPane().add(tunnelfilelist);

		pack();  //hide for AppletConversion
		setVisible(true);

		// load the symbols from the current working directory.
		// byproduct is it will load the stoke colours too
		sketchdisplay.sketchlinestyle.LoadSymbols(FileAbstraction.currentSymbols);
		sketchdisplay.miUseSurvex.setSelected(FileAbstraction.SurvexExists()); 

		assert sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating; 
		sketchdisplay.sketchlinestyle.UpdateSymbols(true);
		if (SketchLineStyle.strokew == -1.0F)
			SketchLineStyle.SetStrokeWidths(0.625F);
	}

	/////////////////////////////////////////////
	OneTunnel GetActiveTunnel()
	{
		return (miViewSymbolsList.isSelected() ? vgsymbols : filetunnel); 	
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
		TN.currentDirectoryIMG = FileAbstraction.MakeCurrentUserDirectory();

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
		TN.currentDirectoryIMG = new FileAbstraction(); 
// uncomment for AppletConversion
/*		TN.currentDirectory.localurl = cl.getResource(getParameter("cavedir") + "/");
TN.emitWarning("localurl:" + TN.currentDirectory.localurl);
String fullcavedir = getParameter("fullcavedir");
if (fullcavedir != null)
{
try { TN.currentDirectory.localurl = new URL(fullcavedir); }
catch (MalformedURLException e) {  TN.emitWarning("malformed:" + fullcavedir);}
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

