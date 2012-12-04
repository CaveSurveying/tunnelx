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
import java.awt.Color;

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
import java.lang.ClassLoader;

import java.util.List; 
import java.util.ArrayList; 

import javax.swing.text.BadLocationException; 


// to do:

// see ElevWarp.java for more todos

// save to url makes it green
// do whole list of tunnel files available at same time
// sort out backgrounds

// the survex file open to do its own removing of http: crud from the front
// bring in background images through this
// open with http on command line should work
// new sketch is made relative to the disk??

// the tree in the tunnel file list can be used to import images
// and it should refresh
// and it should open sketches directly from it.
// then we can lose the list view.


// save as from http file should work
// upload and upload as... should post up to directory 
// upload rendered image should do something in troggle (some kind of workspace)
// upload background images for sure
// cached background images that have been pulled down
// when the browser thing is working, we can emulate it in tunnel (mainbox?)
// OneSketch save sketch


// fix lookup for symbols location
// comments in symbols directory
// reloadfontcolours


/////////////////////////////////////////////
/////////////////////////////////////////////
// the main frame
public class MainBox
	extends JFrame
//	extends JApplet // AppletConversion
{
// the parameters used in this main box

	// the survey tree
	TunnelFileList tunnelfilelist; 
	TunnelLoader tunnelloader;
	JCheckBoxMenuItem miViewSymbolsList; 
	JCheckBoxMenuItem miNetConnection; 

    JTextArea textareaerrors = new JTextArea("Errors and warnings here\n========================\n"); 

	List<FileAbstraction> allfontcolours = new ArrayList<FileAbstraction>(); 
	
// this could be moved into TunnelFileList
	List<OneSketch> ftsketches = new ArrayList<OneSketch>(); 

// this could be a map
	List<OneSketch> vgsymbolstsketches = new ArrayList<OneSketch>(); 
	
	InstantHelp instanthelp = null; 

	// the default treeroot with list of symbols.
	FileAbstraction vgsymbolsdirectory; 

	// single xsection window and wireframe display
	WireframeDisplay wireframedisplay = new WireframeDisplay();

	// sketch display window
	SketchDisplay sketchdisplay; 

    // for handling multi-threaded layouts of symbols
    static SymbolLayoutProcess symbollayoutprocess; 

    NetConnection netconnection = new NetConnection(this); 

	/////////////////////////////////////////////
	void MainRefresh()
	{
		tunnelfilelist.RemakeTFList(); 
	}


	/////////////////////////////////////////////
	void MainOpen(FileAbstraction fileauto, int ftype)
	{
        if (fileauto != null)
            TN.emitMessage("Auto load: " + fileauto + " of type: " + ftype); 

        //if (bAuto)
        //    TN.emitMessage("Auto load: " + TN.currentDirectory + " of type: " + ftype); 
        // Everything is svxfile
		SvxFileDialog sfiledialog = (fileauto == null ? SvxFileDialog.showOpenDialog(TN.currentDirectory, this, ftype, false) : SvxFileDialog.showOpenDialog(fileauto, this, ftype, true));
		if ((sfiledialog == null) || ((sfiledialog.svxfile == null) && (sfiledialog.tunneldirectory == null)))
			return;

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
                if ((sfiledialog.tunneldirectory.localfile != null) && sfiledialog.tunneldirectory.isDirectory())
                    TN.currentDirectory = sfiledialog.tunneldirectory; 
                tunnelfilelist.AddTreeDirectory(sfiledialog.tunneldirectory); 
				int nfl = allfontcolours.size(); 
				sfiledialog.tunneldirectory.FindFilesOfDirectory(ftsketches, allfontcolours); 
    			tunnelfilelist.RemakeTFList();  // do it here so the list entries get sorted out quickly before they get caught out
                        // this whole preview function needs dealing with; poss to remove the situation that it handles multiple lists
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
		
		// loading a survex file
		else if (sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_SVX)
		{
            if (sfiledialog.svxfile.localfile != null)
                TN.currentDirectory = sfiledialog.svxfile.getParentFile(); 
			TN.emitMessage("Do the SVX loading: " + ftype); 
            NewSketch(sfiledialog.svxfile, sfiledialog.svxfile.getSketchName() + "-sketch", (sketchdisplay.subsetpanel.jcbsubsetstyles.getItemCount() != 0 ? 0 : -1)); 
			TN.emitMessage("import centerline: "); 
        	if (sketchdisplay.ImportSketchCentrelineFile(sfiledialog))
			{
                TN.emitMessage("import survex centrline: "); 
                sketchdisplay.ImportCentrelineLabel(false); 
                TN.emitMessage("Done"); 
            }
        }

        // (unintentionally different from the xfiletype thing above)  It doesn't know if the XML actually contains a sketch
		else if (ftype == SvxFileDialog.FT_XMLSKETCH)
		{
			sfiledialog.svxfile.xfiletype = sfiledialog.svxfile.GetFileType();  // part of the constructor?
            if (sfiledialog.svxfile.localfile != null)
                TN.currentDirectory = sfiledialog.svxfile.getParentFile(); 
			if ((sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_XML_SKETCH))
            {
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
                TN.emitMessage(" -EEE- " + GetActiveTunnelSketches().size());
            }
            else
                TN.emitError("Skipping file of unrecognized type "+sfiledialog.svxfile.xfiletype); 
		}
		
		// loading a survex file
		else if ((sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_POCKET_TOPO) || (sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_POCKET_BINTOP))
		{
            if (sfiledialog.svxfile.localfile != null)
                TN.currentDirectory = sfiledialog.svxfile.getParentFile(); 
			TN.emitMessage("Do the POCKETTOPO loading: " + ftype); 
            NewSketch(sfiledialog.svxfile, sfiledialog.svxfile.getSketchName() + "-sketch", (sketchdisplay.subsetpanel.jcbsubsetstyles.getItemCount() != 0 ? 0 : -1)); 
			TN.emitMessage("import centreline: "); 
        	if (sketchdisplay.ImportSketchCentrelineFile(sfiledialog))
			{
                TN.emitMessage("worked: Now importing actual centreline");   // this could be a menu option?
				sketchdisplay.ImportCentrelineLabel(false); 
			}
        }

		else
			TN.emitError("can't do this type any more no more: " + ftype); 

		//MainRefresh();
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
	public void emitErrorMessageLine(String mess, boolean btofront)
	{
        textareaerrors.append(mess); 
        if (btofront)
            toFront(); 

        int lc = textareaerrors.getLineCount() - 1; 
        try
        {
        textareaerrors.setSelectionStart(textareaerrors.getLineStartOffset(lc)); 
        textareaerrors.setSelectionEnd(textareaerrors.getLineEndOffset(lc)); 
        }
        catch (BadLocationException e)
        {;}
    }

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// build a sketch window.
	void ViewSketch(OneSketch activesketch)
	{
  		if (activesketch != null)
		{
			boolean bloaded = true; ; 
            if (!activesketch.bsketchfileloaded)
				bloaded = tunnelloader.LoadSketchFile(activesketch, true);
			if (bloaded)
                sketchdisplay.ActivateSketchDisplay(activesketch, true);
		}
		tunnelfilelist.tflist.repaint(); 
	}

	/////////////////////////////////////////////
	// make a new sketch
	void NewSketch(FileAbstraction fanewsketchdir, String lname, int subsetstyleindex)
	{
		// if new symbols type we should be able to edit the name before creating.

		// determin if this is the sketch type (needs refining)
		OneSketch tsketch = new OneSketch(FileAbstraction.GetUniqueSketchFileName(fanewsketchdir, GetActiveTunnelSketches(), lname));
		if (GetActiveTunnelSketches() == vgsymbolstsketches)
		{
			tsketch.sketchsymbolname = tsketch.sketchfile.getName();
			tsketch.bSymbolType = true;
		}
		tsketch.SetupSK();

		// default to first value
		if (subsetstyleindex != -1)
			tsketch.SetSubsetAttrStyle(((SubsetAttrStyle)sketchdisplay.subsetpanel.jcbsubsetstyles.getItemAt(subsetstyleindex)), null); 
		
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
			if ((osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME) && (osa.opsketchframedefs != null))
			{
				for (OnePath op : osa.opsketchframedefs)
				{
					SketchFrameDef sketchframedef = op.plabedl.sketchframedef; 
					if (!op.bpathvisiblesubset)
					{
						System.out.println("skipping outofsubset sketchframe"); 
						continue;
					}

					sketchframedef.SetSketchFrameFiller(this, tsketch.realposterpaperscale, tsketch.sketchLocOffset, tsketch.sketchfile);
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
	public MainBox()  // the main construction is done in init()
	{
        TN.mainbox = this; 

        textareaerrors.setBackground(new Color(1.0F, 0.8F, 0.8F)); 
        textareaerrors.setRows(4); 

// hide for AppletConversion
		setTitle("TunnelX - " + TN.tunnelversion);
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
        if (FileAbstraction.helpFile != null)
            instanthelp = new InstantHelp(this); 
    	sketchdisplay = new SketchDisplay(this);
		tunnelloader = new TunnelLoader(false, sketchdisplay.sketchlinestyle);
		tunnelfilelist = new TunnelFileList(this);
        symbollayoutprocess = new SymbolLayoutProcess(this); 

		JMenuItem miOpenXMLDir = new JMenuItem("Open Sketches Directory...");
		miOpenXMLDir.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(null, SvxFileDialog.FT_DIRECTORY); } } );

		JMenuItem miOpen = new JMenuItem("Open Sketch...");
		miOpen.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(null, SvxFileDialog.FT_XMLSKETCH); } } );

		JMenuItem miOpenSVX = new JMenuItem("Open Survex...");
		miOpenSVX.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainOpen(null, SvxFileDialog.FT_SVX); } } );

		JMenuItem miSaveAll = new JMenuItem("Save All");
		miSaveAll.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainSaveAll(); } } );

		JMenuItem miRefresh = new JMenuItem("Refreshhh");
		miRefresh.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainRefresh(); } } );

		miNetConnection = new JCheckBoxMenuItem("Net Connection", false);
        miNetConnection.setEnabled(false); 

		JMenuItem miExit = new JMenuItem("Exit");
		miExit.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { MainExit(); } } );

		JMenuItem miSketch = new JMenuItem("View Sketch");
		miSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ViewSketch((tunnelfilelist.activesketchindex != -1 ? GetActiveTunnelSketches().get(tunnelfilelist.activesketchindex) : null)); } } );

		miViewSymbolsList = new JCheckBoxMenuItem("Symbols List", false);
		miViewSymbolsList.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { tunnelfilelist.RemakeTFList(); } } ); 

		JMenuItem miNewEmptySketch = new JMenuItem("New Empty Sketch");
		miNewEmptySketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { NewSketch(TN.currentDirectory, "sketch", -1); } } );

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar();

		JMenu menufile = new JMenu("File");
		if (!FileAbstraction.bIsApplet)  // or use disable function on them to grey them out.
		{
			menufile.add(miOpen);
            menufile.add(miOpenSVX); 
			menufile.add(miOpenXMLDir);
		}
		menufile.add(miNewEmptySketch);
		menufile.add(miRefresh);
		if (!FileAbstraction.bIsApplet)
		{
			menufile.add(miSaveAll);
            menufile.add(miNetConnection); 
			menufile.add(miExit);
		}
		menubar.add(menufile);

		JMenu menutunnel = new JMenu("Tunnel");
		menutunnel.add(miViewSymbolsList);
		menutunnel.add(miSketch);
		menubar.add(menutunnel);

        if (instanthelp != null)
        {
        	JMenu menuHelp = new JMenu("Help");
            for (JMenuItem mihelp : instanthelp.mihelpsmain)
                menuHelp.add(mihelp); 
            menubar.add(menuHelp); 
        }

		setJMenuBar(menubar);

        tunnelfilelist.setPreferredSize(new Dimension(600, 300));

		JSplitPane vsplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		vsplitpane.setLeftComponent(tunnelfilelist);
		vsplitpane.setRightComponent(new JScrollPane(textareaerrors));
        getContentPane().add(vsplitpane);

		pack();  //hide for AppletConversion
        tunnelfilelist.jsp.setDividerLocation(0.3); 
        vsplitpane.setDividerLocation(0.8); 
		setVisible(true);

		// load the symbols from the current working directory.
		// byproduct is it will load the stoke colours too
		LoadSymbols(FileAbstraction.currentSymbols);
		sketchdisplay.miUseSurvex.setSelected(FileAbstraction.SurvexExists()); 

		if (sketchdisplay.sketchlinestyle.bsubsetattributesneedupdating)  // false is no subsetattributes ever got loaded (ie wasn't such a file in symbols directory)
    		sketchdisplay.sketchlinestyle.UpdateSymbols(true);
		if (SketchLineStyle.strokew == -1.0F)
		{
        	SketchLineStyle.SetStrokeWidths(0.625F, sketchdisplay.miNotDotted.isSelected());
            if (sketchdisplay.todenodepanel != null)
                sketchdisplay.todenodepanel.BuildSpirals(); 
        }
	}

	/////////////////////////////////////////////
	// we should soon be loading these files from the same place as the svx as well as this general directory
	void LoadSymbols(FileAbstraction fasymbols)
	{
		TN.emitWarning("Loading symbols dir: " + fasymbols.getAbsolutePath());

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
//System.out.println("Content-Type: text/plain\n");
//System.out.println("Hi there"); 
//System.exit(0); 

        String fstart = null; 

		// produce a default
		//fstart = "http://seagrass.goatchurch.org.uk/~expo/tunneldata/";

        String snetconnection = null;
		boolean bmakeimages = false; 
		boolean btwotone = false; 
        for (int i = 0; i < args.length; i++)
        {
			if (!args[i].substring(0, 2).equals("--"))
				fstart = args[i];
			else if (args[i].equals("--verbose"))
				TN.bVerbose = true;
			else if (args[i].equals("--quiet"))
				TN.bVerbose = false;
			else if (args[i].equals("--todenode"))
				TN.bTodeNode = true;
			else if (args[i].equals("--netconnection"))
				snetconnection = "http://localhost:8000/run/tunnelx_receiver/";
			else if (args[i].equals("--makeimages"))
				bmakeimages = true;
			else if (args[i].startsWith("--printdir="))
				TN.currprintdir = FileAbstraction.MakeDirectoryFileAbstraction(args[i].substring(11));
			else if (args[i].equals("--twotone"))
				btwotone = true; 
			else
				TN.emitWarning("Unknown arg: " + args[i]); 
        }
    
        // start-up
        FileAbstraction.bIsApplet = false;
        TN.currentDirectory = FileAbstraction.MakeCurrentUserDirectory();
        TN.currentDirectoryIMG = FileAbstraction.MakeCurrentUserDirectory();
    
        MainBox mainbox = new MainBox();
        mainbox.init();  // the init gets called

		if (btwotone)
			mainbox.sketchdisplay.printingpanel.cbBitmaptype.setSelectedIndex(2); 

        // do the filename
        if (fstart != null)
        {
            FileAbstraction fastart = FileAbstraction.MakeOpenableFileAbstraction(fstart);
            fastart = FileAbstraction.MakeCanonical(fastart); 
            if (fastart.localurl == null)
                TN.currentDirectory = fastart; 
            fastart.xfiletype = fastart.GetFileType(); 
            int ftype = (fastart.isDirectory() ? SvxFileDialog.FT_DIRECTORY : (fastart.xfiletype == FileAbstraction.FA_FILE_XML_SKETCH ? SvxFileDialog.FT_XMLSKETCH : SvxFileDialog.FT_SVX)); 
            mainbox.MainOpen(fastart, ftype);
		}

		// the command line to generate bitmaps directly from all the frame sketches
		if (bmakeimages)
		{
			if ((fstart != null) && (mainbox.sketchdisplay.sketchgraphicspanel.tsketch != null))
				mainbox.sketchdisplay.MakeImages(); 
			else
				TN.emitError("Must specify a sketch on the command line to do makeimages");
        }
        if (snetconnection != null)
            mainbox.netconnection.ncstart(snetconnection); 
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

