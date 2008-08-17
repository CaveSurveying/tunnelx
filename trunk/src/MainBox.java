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


	List<FileAbstraction> allfontcolours = new ArrayList<FileAbstraction>(); 
	
	List<OneSketch> ftsketches = new ArrayList<OneSketch>(); 

// this could be a map
	List<OneSketch> vgsymbolstsketches = new ArrayList<OneSketch>(); 
	

	
	// the default treeroot with list of symbols.
	FileAbstraction vgsymbolsdirectory; 

	// single xsection window and wireframe display
	WireframeDisplay wireframedisplay = new WireframeDisplay();

	// sketch display window
	SketchDisplay sketchdisplay = new SketchDisplay(this);


	/////////////////////////////////////////////
	void MainRefresh()
	{
		tunnelfilelist.RemakeTFList(); 
	}


	/////////////////////////////////////////////
	void MainOpen(boolean bAuto, int ftype)
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, ftype, bAuto);
		if ((sfiledialog == null) || ((sfiledialog.svxfile == null) && (sfiledialog.tunneldirectory == null)))
			return;

		TN.currentDirectory = sfiledialog.getSelectedFileA();
		String soname = (sfiledialog.tunneldirectory == null ? sfiledialog.svxfile.getName() : sfiledialog.tunneldirectory.getName());
		int il = soname.indexOf('.');
		if (il != -1)
			soname = soname.substring(0, il);

		// put the tunnel in
		String filetunnname = soname.replace(' ', '_').replace('\t', '_'); // can't cope with spaces.

		// loading directly from a tunnel directory tree
		if (sfiledialog.tunneldirectory != null)
		{
			try
			{
				int nfl = allfontcolours.size(); 
				sfiledialog.tunneldirectory.FindFilesOfDirectory(ftsketches, allfontcolours); 
				System.out.println("nnnnnn " + nfl); 
				for (int i = nfl; i < allfontcolours.size(); i++)
					tunnelloader.LoadFontcolour(allfontcolours.get(i));  
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
		
		else if ((sfiledialog.svxfile != null) && (ftype == SvxFileDialog.FT_XMLSKETCH))
		{
			sfiledialog.svxfile.xfiletype = sfiledialog.svxfile.GetFileType();  // part of the constructor?
			OneSketch tsketch = new OneSketch(sfiledialog.svxfile); 
			if (GetActiveTunnelSketches() == vgsymbolstsketches)
			{
				tsketch.sketchsymbolname = tsketch.sketchfile.getName();
				tsketch.bSymbolType = true;
			}
			GetActiveTunnelSketches().add(tsketch);
			tunnelfilelist.RemakeTFList();
			tunnelfilelist.tflist.setSelectedIndex(tunnelfilelist.isketche - 1);
			tunnelfilelist.UpdateSelect(true); // doubleclicks it.
			System.out.println(" -EEE- " + GetActiveTunnelSketches().size());
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
		for (OneSketch lsketch : ftsketches)
			lsketch.SaveSketch();
		for (OneSketch lsketch : vgsymbolstsketches)
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
	/////////////////////////////////////////////
	// build a sketch window.
	void ViewSketch()
	{
		// now make the sketch
		if (tunnelfilelist.activetxt == FileAbstraction.FA_FILE_XML_SKETCH)
		{
			// load the sketch if necessary.  Then view it
			OneSketch activesketch = GetActiveTunnelSketches().get(tunnelfilelist.activesketchindex);
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
		// if new symbols type we should be able to edit the name before creating.

		// find a unique new name.  (this can go wrong, but tired of it).
		int nsknum = GetActiveTunnelSketches().size() - 1;

		// determin if this is the sketch type (needs refining)
		OneSketch tsketch = new OneSketch(FileAbstraction.GetUniqueSketchFileName(TN.currentDirectory, GetActiveTunnelSketches()));
		if (GetActiveTunnelSketches() == vgsymbolstsketches)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tsketch.SetupSK();
		tsketch.bsketchfilechanged = true;

		// load into the structure and view it.
		assert tsketch.bsketchfileloaded;
		GetActiveTunnelSketches().add(tsketch);
		tunnelfilelist.RemakeTFList();
		tunnelfilelist.tflist.setSelectedIndex(tunnelfilelist.isketche - 1);
		tunnelfilelist.UpdateSelect(true); // doubleclicks it.
	}




	/////////////////////////////////////////////
	OneSketch FindSketchFrame(List<OneSketch> tsketches, FileAbstraction fasketch)
	{
System.out.println("finding sketchframes " + tsketches.size() + "  " + fasketch.getPath()); 
		// account for which sketches have actually been loaded
		for (OneSketch ltsketch : tsketches)
		{
			if (fasketch.equals(ltsketch.sketchfile))
			{
				if (ltsketch.bsketchfileloaded)
					return ltsketch;
				else 
				{
					tunnelloader.LoadSketchFile(ltsketch, true);
					tunnelfilelist.tflist.repaint();
					return ltsketch;
				}
			}
		}

		fasketch.xfiletype = fasketch.GetFileType();  // part of the constructor?
		OneSketch tsketch = new OneSketch(fasketch); 
		if (GetActiveTunnelSketches() == vgsymbolstsketches)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tunnelloader.LoadSketchFile(tsketch, true);
// if fails return null

		GetActiveTunnelSketches().add(tsketch);
		tunnelfilelist.tflist.repaint();
		tunnelfilelist.RemakeTFList();
		return tsketch;
	}

	/////////////////////////////////////////////
	void UpdateSketchFrames(OneSketch tsketch, int iProper)
	{
		List<OneSketch> framesketchesseen = (iProper != SketchGraphics.SC_UPDATE_NONE ? new ArrayList<OneSketch>() : null);
		for (OneSArea osa : tsketch.vsareas)
		{
			if ((osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME) && (osa.sketchframedefs != null))
			{
				for (SketchFrameDef sketchframedef : osa.sketchframedefs)
				{
					sketchframedef.SetSketchFrameFiller(this, tsketch.realpaperscale, tsketch.sketchLocOffset, tsketch.sketchfile);
					OneSketch lpframesketch = sketchframedef.pframesketch;
					if ((iProper != SketchGraphics.SC_UPDATE_NONE) && (lpframesketch != null))
					{
						// got to consider setting the sket
						SubsetAttrStyle sksas = sketchdisplay.sketchlinestyle.subsetattrstylesmap.get(sketchframedef.sfstyle);
						if ((sksas == null) && (sketchframedef.pframesketch.sksascurrent == null))
							sksas = sketchdisplay.subsetpanel.sascurrent;
						if ((sksas != null) && (sksas != sketchframedef.pframesketch.sksascurrent) && !framesketchesseen.contains(lpframesketch))
						{
							TN.emitMessage("Setting sketchstyle to " + sksas.stylename + " (maybe should relay the symbols)");
							int scchangetyp = sketchframedef.pframesketch.SetSubsetAttrStyle(sksas, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
							SketchGraphics.SketchChangedStatic(scchangetyp, lpframesketch, null);
						}
						// SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS or SketchGraphics.SC_UPDATE_ALL
						lpframesketch.UpdateSomething(iProper, false);
                    	SketchGraphics.SketchChangedStatic(iProper, lpframesketch, null);
					}
					if ((framesketchesseen != null) && !framesketchesseen.contains(lpframesketch))
						framesketchesseen.add(lpframesketch);
				}
			}
		}
	}

	/////////////////////////////////////////////

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public MainBox()
	{
		tunnelloader = new TunnelLoader(false, sketchdisplay.sketchlinestyle);

// hide for AppletConversion
		setTitle("TunnelX - Cave Drawing Program");
		setLocation(new Point(100, 100));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
			{ public void windowClosing(WindowEvent event) { MainExit(); } } );
	}

// need to load the fontcolours.xml which will then call in a bunch of symbols that need to be loaded
// into vgsymbolss.
// each of these resources comes in as a name
//
// LoadSymbols

    public void init()
	{
		FileAbstraction.InitFA(); 
		
		JMenuItem miOpenXMLDir = new JMenuItem("Open Sketches Directory...");
		miOpenXMLDir.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(false, SvxFileDialog.FT_DIRECTORY); } } );

		JMenuItem miOpen = new JMenuItem("Open Sketch...");
		miOpen.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(false, SvxFileDialog.FT_XMLSKETCH); } } );

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
		LoadSymbols(FileAbstraction.currentSymbols);
		sketchdisplay.miUseSurvex.setSelected(FileAbstraction.SurvexExists()); 

		assert sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating; 
		sketchdisplay.sketchlinestyle.UpdateSymbols(true);
		if (SketchLineStyle.strokew == -1.0F)
			SketchLineStyle.SetStrokeWidths(0.625F);
	}

	/////////////////////////////////////////////
	// we should soon be loading these files from the same place as the svx as well as this general directory
	void LoadSymbols(FileAbstraction fasymbols)
	{
		TN.emitMessage("Loading symbols " + fasymbols.getName());

		// do the tunnel loading thing
		TunnelLoader symbtunnelloader = new TunnelLoader(true, sketchdisplay.sketchlinestyle);
		try
		{
			vgsymbolsdirectory = fasymbols;
			int nfl = allfontcolours.size(); 
			fasymbols.FindFilesOfDirectory(vgsymbolstsketches, allfontcolours); 
			for (int i = nfl; i < allfontcolours.size(); i++)
				symbtunnelloader.LoadFontcolour(allfontcolours.get(i));  

			// load up sketches
			for (OneSketch tsketch : vgsymbolstsketches)
				symbtunnelloader.LoadSketchFile(tsketch, false);
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


	/////////////////////////////////////////////
	List<OneSketch> GetActiveTunnelSketches()
	{
		return (miViewSymbolsList.isSelected() ? vgsymbolstsketches : ftsketches); 	
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
			mainbox.MainOpen(true, (TN.currentDirectory.isDirectory() ? SvxFileDialog.FT_DIRECTORY : SvxFileDialog.FT_XMLSKETCH));
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
//		TN.currentDirectory.localurl = cl.getResource(getParameter("cavedir"));
//		TN.currentDirectory.bIsDirType = true;
//		System.out.println("inputdir: " + getParameter("cavedir"));
//		System.out.println("currentdir: " + TN.currentDirectory.localurl);
//		MainOpen(true, SvxFileDialog.FT_DIRECTORY);
//LoadTunnelDirectoryTree("cavecave", TN.currentDirectory);
		MainRefresh();
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

