////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel; 

import java.util.Vector;
import java.io.File; 

import java.awt.event.WindowAdapter; 
import java.awt.event.WindowEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ActionEvent; 

import java.awt.Dimension; 

import javax.swing.JFrame; 
import javax.swing.JSplitPane; 
import javax.swing.JTextArea; 
import javax.swing.JTextField; 
import javax.swing.JScrollPane; 

import javax.swing.JMenu; 
import javax.swing.JMenuItem; 
import javax.swing.JMenuBar; 

import javax.swing.JOptionPane; 

/////////////////////////////////////////////
/////////////////////////////////////////////
// the main frame
public class MainBox extends JFrame 
{
// the parameters used in this main box

	// the survey tree
	TunnelTree treeview; 
	OneTunnel roottunnel; 
	OneTunnel filetunnel; 
	OneTunnel activetunnel; 

	// the CentreLine panel
	JTextArea rhstarea = new JTextArea("JGT"); 

	// this will keep the global sections, tubes, and sketch in it 
	// which a station calculation is lifted into and then operated on.  
	OneTunnel otglobal = new OneTunnel("Global", null); 

	// the class that loads and calculates the positions of everything from the data in the tunnels. 
	StationCalculation sc = new StationCalculation(); 

	// single xsection window
	SectionDisplay sectiondisplay = new SectionDisplay(this); 

	// wireframe display window
	WireframeDisplay wireframedisplay = new WireframeDisplay(sectiondisplay); 

	// the list of symbols.  
	OneTunnel vgsymbols = new OneTunnel("gsymbols", null); 

	// sketch display window
	SketchDisplay sketchdisplay = new SketchDisplay(vgsymbols, null); 

	// sketch display window for symbols (must be constructed after the above).  
	SketchDisplay sketchdisplaysymbols = new SketchDisplay(vgsymbols, sketchdisplay.symbolspanel); 

	/////////////////////////////////////////////
	void SaveActiveData()
	{
		if (activetunnel != null)
			activetunnel.setTextData(rhstarea.getText()); 
	}

	/////////////////////////////////////////////
	void MainRefresh()
	{
		OneTunnel otoldactive = activetunnel; 
		SelectTunnel(null); 
		roottunnel.RefreshTunnel(); // redoes all the *subtunnels

		// find the active tunnel in this list??  
		treeview.RefreshListBox(roottunnel, activetunnel); // or load filetunnel. 
	}


	/////////////////////////////////////////////
	void MainClear()
	{
		roottunnel = new OneTunnel("root", null); 
		activetunnel = null; 
		SelectTunnel(null); 
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
			System.out.println("Loading survey file " + sfiledialog.svxfile.getName()); 
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

System.out.println("Hi there " + filetunnname); 
		filetunnel = roottunnel.IntroduceSubTunnel(filetunnname, null, false); 
		if (sfiledialog.tunneldirectory != null)  
			new TunnelLoader(filetunnel, sfiledialog.tunneldirectory); 
		else 
			new SurvexLoader(sfiledialog.svxfile, filetunnel, sfiledialog.bReadCommentedXSections, false, sfiledialog.bIsTunFile); 

		// load the tube file
		if (sfiledialog.tubefile != null)
		{
			System.out.println("Loading tube file " + sfiledialog.tubefile.getAbsolutePath()); 
			if (sfiledialog.tubefile.canRead())
 				new SurvexLoader(sfiledialog.tubefile, filetunnel, false, true, true); 
			else
				JOptionPane.showMessageDialog(this, "Cannot open tube file: " + sfiledialog.tubefile.getName());  
		}

		MainRefresh(); 
	}


	/////////////////////////////////////////////
	void MainSave(int ftype)
	{
		SaveActiveData(); 
		SvxFileDialog sfiledialog = SvxFileDialog.showSaveDialog(TN.currentDirectory, this, ftype); 
		if (sfiledialog == null) 
			return; 

		TN.currentDirectory = sfiledialog.getSelectedFile(); 

		if (sfiledialog.tunneldirectory != null)  
		{
			System.out.println("Saving tunnel directory " + sfiledialog.tunneldirectory.getName()); 
			if (filetunnel.ndowntunnels != 1) 
				TN.emitProgError("File Tunnel wrong number"); 
			new TunnelSaver(filetunnel.downtunnels[0], sfiledialog.tunneldirectory); 
		}

		if (sfiledialog.svxfile != null)
		{
			System.out.println("Saving survey file " + sfiledialog.svxfile.getName()); 
			new SurvexSaver(filetunnel, sfiledialog.svxfile, true, false, (ftype == SvxFileDialog.FT_TUN)); 
		}
		if (sfiledialog.tubefile != null)
		{
			System.out.println("Saving tube file " + sfiledialog.tubefile.getName()); 
			new SurvexSaver(filetunnel, sfiledialog.tubefile, false, false, true); 
		}
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
		SaveActiveData(); 
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
			sc.CopyRecurseExportVTunnels(otglobal, disptunnel); 
			if (sc.CalcStationPositions(otglobal, null) == 0) 
				return; 
			wireframedisplay.ActivateWireframeDisplay(otglobal, false); 
		}
	}

	/////////////////////////////////////////////
	// build a sketch window. 
	void ViewSketch(boolean bSingleTunnel, OneTunnel[] disptunnels)
	{
		SaveActiveData(); 
		if (disptunnels == null) 
			return; 

		if (bSingleTunnel)
		{
			OneTunnel disptunnel = disptunnels[0]; // for now take the first 
			System.out.println(disptunnels.length); 

			boolean bSomethingPresent = false; 
			for (int i = 0; i < disptunnels.length; i++) 
			{
				disptunnels[i].ResetUniqueBaseStationTunnels(); 
				bSomethingPresent |= (sc.CalcStationPositions(disptunnels[i], otglobal.vstations) != 0); 
			}
			if (!bSomethingPresent) 
				return; 
			sketchdisplay.ActivateSketchDisplay(disptunnels, activetunnel, true); 
		}

		else
		{
			// doesn't work.  
			/*
			
			//lift all the stuff into otglobal

			sc.LoadVTunnels(disptunnel, true, vsectionsglobal, vtubesglobal); 
			if (sc.CalcStationPositions() == 0) 
				return; 
			sketchdisplay.ActivateSketchDisplay(vlegs, vstations, otglobal, false); 
			*/
		}
	}




	/////////////////////////////////////////////
	void SelectTunnel(OneTunnel ot)
	{
		SaveActiveData(); 
		if (activetunnel != null) 
			activetunnel.SetWFactiveRecurse(false);  

		// set all subtunnels to inactive  
		activetunnel = ot; 
		rhstarea.setText(activetunnel != null ? activetunnel.getTextData() : ""); 
		rhstarea.setCaretPosition(0); 
		if ((activetunnel != null) && (activetunnel != roottunnel)) 
			activetunnel.SetWFactiveRecurse(true);  
		wireframedisplay.RefreshWireDisplay(); 

		if (sketchdisplay.isVisible()) 
			sketchdisplay.SelectTunnel(ot); 
	}


	/////////////////////////////////////////////

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public MainBox()
	{
		TN.SetStrokeWidths(0.5F);  

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

		JMenuItem miOpen = new JMenuItem("Open..."); 
		miOpen.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainOpen(true, false, SvxFileDialog.FT_SVXTUBETUN); } } ); 

		JMenuItem miOpenInto = new JMenuItem("Open Into..."); 
		miOpenInto.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainOpen(false, false, SvxFileDialog.FT_SVXTUBETUN); } } ); 

		JMenuItem miSaveSVX = new JMenuItem("Save SVX"); 
		miSaveSVX.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainSave(SvxFileDialog.FT_SVX); } } ); 

		JMenuItem miSaveTUBE = new JMenuItem("Save TUBE"); 
		miSaveTUBE.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainSave(SvxFileDialog.FT_TUBE); } } ); 

		JMenuItem miSaveTUN = new JMenuItem("Save TUN"); 
		miSaveTUN.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainSave(SvxFileDialog.FT_TUN); } } ); 

		JMenuItem miSaveTUNDIR = new JMenuItem("Save TUNDIR"); 
		miSaveTUNDIR.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainSave(SvxFileDialog.FT_DIRECTORY); } } ); 


		JMenuItem miRefresh = new JMenuItem("Refresh"); 
		miRefresh.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainRefresh(); } } ); 

		JMenuItem miExit = new JMenuItem("Exit"); 
		miExit.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { MainExit(); } } ); 


		JMenuItem miWireframe = new JMenuItem("Wireframe"); 
		miWireframe.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(true, activetunnel); } } ); 

		JMenuItem miSketch = new JMenuItem("Sketch"); 
		miSketch.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { ViewSketch(true, treeview.GetActiveTunnels()); } } ); 

		JMenuItem miCaveBelow = new JMenuItem("Cave Below"); 
		miCaveBelow.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(false, activetunnel); } } ); 

		JMenuItem miWholeCave = new JMenuItem("Whole Cave"); 
		miWholeCave.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { ViewWireframe(false, roottunnel); } } ); 

		JMenuItem miSketchSymbols = new JMenuItem("Sketch Symbols"); 
		miSketchSymbols.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchdisplaysymbols.ActivateSketchSymbols(); } } ); 

		// build the layout of the menu bar
		JMenuBar menubar = new JMenuBar(); 

		JMenu menufile = new JMenu("File"); 
		menufile.add(miSketchSymbols); 
		menufile.add(miClear); 
		menufile.add(miOpenXMLDir); 
		menufile.add(miOpen); 
		menufile.add(miOpenInto); 
		menufile.add(miRefresh); 
		menufile.add(miSaveSVX); 
		menufile.add(miSaveTUBE); 
		menufile.add(miSaveTUN); 
		menufile.add(miSaveTUNDIR); 
		menufile.add(miExit); 
		menubar.add(menufile); 

		JMenu menutunnel = new JMenu("Tunnel"); 
		menutunnel.add(miWireframe); 
		menutunnel.add(miSketch); 
		menubar.add(menutunnel); 

		JMenu menuview = new JMenu("View"); 
		menuview.add(miCaveBelow); 
		menuview.add(miWholeCave); 
		menubar.add(menuview); 

		setJMenuBar(menubar); 
		MainClear(); 


        //Add the scroll panes to a split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(100); 

		// build the left hand area 
		treeview = new TunnelTree(this); 
		JScrollPane rhsview = new JScrollPane(rhstarea); 

		// the two centre line type panels
        Dimension minimumSize = new Dimension(300, 200);
        treeview.setPreferredSize(minimumSize);
        rhsview.setPreferredSize(minimumSize);

		splitPane.setLeftComponent(treeview); 
		splitPane.setRightComponent(rhsview); 

        //Add the split pane to this frame
        getContentPane().add(splitPane);

		sketchdisplaysymbols.symbolspanel.LoadSymbols(); // load the symbols from the current working directory.  

		pack();
		show();
	}



	/////////////////////////////////////////////
	// startup the program
    public static void main(String args[]) 
	{
		MainBox mainbox = new MainBox();
		if (args.length == 1)
		{
			TN.currentDirectory = new File(args[0]); 
System.out.println(args[0]); 
System.out.println(TN.currentDirectory.toString()); 
			mainbox.MainOpen(true, true, (TN.currentDirectory.isDirectory() ? SvxFileDialog.FT_DIRECTORY : SvxFileDialog.FT_SVXTUBETUN)); 
		}
	}
}
