////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JFrame; 

import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 
import javax.swing.JToggleButton; 
import javax.swing.JPanel; 
import javax.swing.JCheckBox; 
import javax.swing.JButton; 
import javax.swing.JTextField; 
import javax.swing.JComboBox; 

import javax.swing.JSplitPane; 
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 

import java.awt.Graphics; 
import java.awt.BorderLayout; 
import java.awt.GridLayout; 
import javax.swing.BoxLayout; 

import java.util.Vector; 
import java.awt.FileDialog;

import java.awt.Image; 

import java.io.IOException; 
import java.io.File;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 

import java.awt.event.MouseEvent; 
import java.awt.event.MouseAdapter; 

import javax.swing.event.DocumentListener; 
import javax.swing.event.DocumentEvent; 

//
//
// SketchDisplay
//
//


// this class contains the whole outer set of options and buttons
class SketchDisplay extends JFrame
{
	// the panel which holds the sketch graphics
	SketchGraphics sketchgraphicspanel;  

	OneTunnel vgsymbols; 
	SymbolsPanel symbolspanel; 

	SymbolsPanel usesymbolspanel; // nonnull only if symbol type window and edits have to be transfered.  


	// the menu bar 
	JMenuBar menubar = new JMenuBar(); 

	// file menu
	JMenu menufile = new JMenu("File"); 
	JMenuItem doneitem = new JMenuItem("Close"); 
		JMenuItem miSaveitem = new JMenuItem("Save Symbols"); 
		JMenuItem miLoaditem = new JMenuItem("Load Symbols"); 
	JMenuItem miPrint = new JMenuItem("Print"); 

	// display menu.  
	JMenu menudisplay = new JMenu("Display"); 
	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem("Centreline", true); 
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem("StationNames", true); 
	JCheckBoxMenuItem miXSections = new JCheckBoxMenuItem("XSections", true); 
	JCheckBoxMenuItem miTubes = new JCheckBoxMenuItem("Tubes", true); 
	JCheckBoxMenuItem miAxes = new JCheckBoxMenuItem("Axes", true); 
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem("Depth Colours", true); 
	JCheckBoxMenuItem miHideMarkers = new JCheckBoxMenuItem("Hide Markers", false); 
	JCheckBoxMenuItem miTrackLines = new JCheckBoxMenuItem("Track Lines", false); 
	JCheckBoxMenuItem miTabletMouse = new JCheckBoxMenuItem("Tablet Mouse", true); 
	JCheckBoxMenuItem mibackhide = new JCheckBoxMenuItem("Hide Background", false); 

	// view menu
	JMenu menuview = new JMenu("View"); 
	JMenuItem mimax = new JMenuItem("Max"); 
	JMenuItem micentre = new JMenuItem("Centre"); 
	JMenuItem miscad = new JMenuItem("Scale Down"); 
	JMenuItem miscau = new JMenuItem("Scale Up"); 


	// top right buttons.  
	JToggleButton tbpicmove = new JToggleButton("Move Picture"); 
	JToggleButton tbmovebackg = new JToggleButton("Move Background"); 
	JButton bmoveground = new JButton("Shift Ground"); 

	// sketch line style selection
	SketchLineStyle sketchlinestyle = new SketchLineStyle(); 
	
	// selection observers 
	SketchSelectionObserver ssobsPath = new SketchSelectionObserver("paths/"); 
	SketchSelectionObserver ssobsArea = new SketchSelectionObserver("areas/"); 
	SketchSelectionObserver ssobsSymbol = new SketchSelectionObserver("symbols/"); 

	// the lower right hand cluster of buttons and fields.  
	JButton bstrokethick = new JButton("Stroke >>"); 
	JButton bstrokethin = new JButton("Stroke <<"); 

	JButton pthdesel = new JButton("Deselect"); 
	JButton pthdel = new JButton("Delete"); 
	JButton pthback = new JButton("Back"); 
	JButton pthfuse = new JButton("Fuse"); 
	JButton bupdatesareas = new JButton("Update SAreas"); 
	JButton bdetrender = new JButton("Detail Render"); 


	/////////////////////////////////////////////
	ItemListener SketchRepaint = new ItemListener()
	{ 
		public void itemStateChanged(ItemEvent e) { sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.repaint(); }  
	}; 

	
	/////////////////////////////////////////////
	class CChangePathParams implements ActionListener, DocumentListener 
	{
		int maskcpp = 0; 
		public void actionPerformed(ActionEvent e) { sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); }; 
		public void changedUpdate(DocumentEvent e) { sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); }; 
		public void insertUpdate(DocumentEvent e) { sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); }; 
		public void removeUpdate(DocumentEvent e) { sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); }; 
	};

	CChangePathParams ChangePathParams = new CChangePathParams(); 


	/////////////////////////////////////////////
	// inactivate case 
	class SketchHide extends WindowAdapter implements ActionListener	
	{
		public void windowClosing(WindowEvent e)
		{
			sketchgraphicspanel.Deselect(true); 
			setVisible(false); 
		}

		public void actionPerformed(ActionEvent e)
		{
			sketchgraphicspanel.Deselect(true); 
			setVisible(false); 
		}
	}


	/////////////////////////////////////////////
	// set up the arrays
    SketchDisplay(OneTunnel lvgsymbols, SymbolsPanel lusesymbolspanel) // usesymbolspanel is null if not symbol type.  
	{
		super("Sketch Display"); 

		// symbols communication.  
	    vgsymbols = lvgsymbols; 
		usesymbolspanel = lusesymbolspanel;  

		// it's important that the two panels are constructed in order.  
		sketchgraphicspanel = new SketchGraphics(this, (usesymbolspanel != null)); 
		symbolspanel = new SymbolsPanel(vgsymbols, this, usesymbolspanel); 


		// file menu stuff.  

		doneitem.addActionListener(new SketchHide()); 
		menufile.add(doneitem); 

		if (usesymbolspanel != null) 
		{
			miSaveitem.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { symbolspanel.SaveSymbols(); } } ); 
			menufile.add(miSaveitem); 

			miLoaditem.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { symbolspanel.LoadSymbols(); } } ); 
			menufile.add(miLoaditem); 
		}

		miPrint.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(); } } ); 
		menufile.add(miPrint); 
		menubar.add(menufile); 


		// view menu stuff.  
		mimax.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Max(true); } } ); 
		menuview.add(mimax); 

		micentre.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Max(false); } } ); 
		menuview.add(micentre); 

		miscad.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Scale(0.5F); } } ); 
		menuview.add(miscad); 

		miscau.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Scale(2.0F); } } ); 
		menuview.add(miscau); 

		mibackhide.addItemListener(new ItemListener() 
			{ public void itemStateChanged(ItemEvent e) { sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.backgroundimg.bBackImageDoneGood = false; sketchgraphicspanel.repaint(); } } ); 
		menuview.add(mibackhide); 

		menubar.add(menuview); 


		// setup the display menu responses
		miCentreline.addItemListener(SketchRepaint); 
		menudisplay.add(miCentreline); 

		miStationNames.addItemListener(SketchRepaint); 
		menudisplay.add(miStationNames); 

		miXSections.addItemListener(SketchRepaint); 
		menudisplay.add(miXSections); 

		miTubes.addItemListener(SketchRepaint); 
		menudisplay.add(miTubes); 

		miAxes.addItemListener(SketchRepaint); 
		menudisplay.add(miAxes); 

		miDepthCols.addItemListener(SketchRepaint); 
		menudisplay.add(miDepthCols); 

		miHideMarkers.addItemListener(SketchRepaint); 
		menudisplay.add(miHideMarkers); 

		menudisplay.add(miTrackLines); 
		menudisplay.add(miTabletMouse); 
		menubar.add(menudisplay); 

		// done with menu bar.  
		setJMenuBar(menubar); 


		// do the buttons and fields stuff for the right hand side panel.  
		JPanel sidecontrols = new JPanel(new GridLayout(0, 1)); 
		sidecontrols.add(tbpicmove); 
		sidecontrols.add(tbmovebackg); 
		bmoveground.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.MoveGround(); } } ); 
		sidecontrols.add(bmoveground); 

		JPanel pathcoms = new JPanel(new GridLayout(0, 1)); 

		pathcoms.add(ssobsPath); 
		pathcoms.add(ssobsArea); 
		pathcoms.add(ssobsSymbol); 

		JPanel pstr = new JPanel(new GridLayout(1, 2)); 
		bstrokethin.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { TN.SetStrokeWidths(TN.strokew / 2.0F); sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.repaint(); } } ); 
		pstr.add(bstrokethin); 

		bstrokethick.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { TN.SetStrokeWidths(TN.strokew * 2.0F); sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.repaint(); } } ); 
		pstr.add(bstrokethick); 
		pathcoms.add(pstr); 

		pthdesel.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.Deselect(false); sketchgraphicspanel.repaint(); } } ); 
		pathcoms.add(pthdesel); 

		pthdel.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.DeleteSel(); } } ); 
		pathcoms.add(pthdel); 

		pthback.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.BackSel(); } } ); 
		pathcoms.add(pthback); 

		pthfuse.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.FuseCurrent(); } } ); 
		pathcoms.add(pthfuse); 

		// class used to handle the bupdatesareas button behavoir and state 
		// can't do the depression of it through the button interface.  
		// could do some fancy changing of its name depending on the value of bSAreasValid
		class BUpdateSAreas extends MouseAdapter implements ActionListener 
		{
			public void actionPerformed(ActionEvent event) 
			{
				if (!sketchgraphicspanel.bSAreasValid) 
				{
					sketchgraphicspanel.UpdateSAreas(); 
					sketchgraphicspanel.repaint(); 
				}
			}

			public void mousePressed(MouseEvent event) 
			{
				if (sketchgraphicspanel.bSAreasValid && !sketchgraphicspanel.bDisplaySAreas) 
				{
					sketchgraphicspanel.bDisplaySAreas = true; 
					sketchgraphicspanel.repaint(); 
				}
			}

			public void mouseReleased(MouseEvent event) 
			{
				if (sketchgraphicspanel.bDisplaySAreas) 
				{
					sketchgraphicspanel.bDisplaySAreas = false; 
					sketchgraphicspanel.repaint(); 
				}
			}
		}
		BUpdateSAreas bupdatesareasLis = new BUpdateSAreas(); 
		bupdatesareas.addActionListener(bupdatesareasLis); 
		bupdatesareas.addMouseListener(bupdatesareasLis); 
		pathcoms.add(bupdatesareas); 

		bdetrender.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.bNextRenderSlow = true; sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.repaint(); } } ); 
		pathcoms.add(bdetrender); 


		JPanel pathistic = new JPanel(new BorderLayout()); 
		sketchlinestyle.linestylesel.addActionListener(ChangePathParams); 
		sketchlinestyle.pthsplined.addActionListener(ChangePathParams); 
		sketchlinestyle.pthlabel.getDocument().addDocumentListener(ChangePathParams); 

		pathistic.add("North", sketchlinestyle); 
		pathistic.add("South", pathcoms); 

		JPanel sidepanel = new JPanel(new BorderLayout()); 
		sidepanel.add("North", sidecontrols); 
		sidepanel.add("South", pathistic); 

		
		// split panel between the symbols and the graphics area.  
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setDividerLocation(200); 

		JScrollPane lhview = new JScrollPane(symbolspanel); 

		splitPane.setLeftComponent(lhview); 
		splitPane.setRightComponent(sketchgraphicspanel); 


		// final set up of display
		getContentPane().setLayout(new BorderLayout()); 
		getContentPane().add("Center", splitPane); 

		getContentPane().add("East", sidepanel); 
		addWindowListener(new SketchHide()); 

		pack(); 
        setSize(800, 600);
    }


	/////////////////////////////////////////////
	void SelectTunnel(OneTunnel lot)  
	{
		sketchgraphicspanel.ClearSelection(); 

		if ((lot != null) && sketchgraphicspanel.dtsketches.contains(lot.tsketch))  
		{
			sketchgraphicspanel.tsketch = lot.tsketch; 
			sketchgraphicspanel.backgroundimg.currparttrans = sketchgraphicspanel.tsketch.backgimgtrans; 
			setTitle(lot.name); 
		}
		else 
		{
			sketchgraphicspanel.tsketch = sketchgraphicspanel.skblank; 
			setTitle("none"); 
		}

		sketchgraphicspanel.bmainImgValid = false; 
		sketchgraphicspanel.repaint(); 
	}





	/////////////////////////////////////////////
// to add in Vector of points, edge types and boundary types.  
	void ActivateSketchDisplay(OneTunnel[] disptunnels, OneTunnel activetunnel, boolean lbEditable)  
	{
		// ASSERT(usesymbolspanel == null); 

		// make an array of sketches. 
		sketchgraphicspanel.dtsketches.clear(); 

		// detect first times.  
		for (int i = 0; i < disptunnels.length; i++) 
		{
			if (disptunnels[i].tsketch == null) 
				disptunnels[i].tsketch = new OneSketch(disptunnels[i]); 
			else 
				System.out.println("Leaving legs the same, perhaps should do a translation");  
				// this is a problem if more than one sketch is displayed.  

			disptunnels[i].tsketch.fbackgimg = disptunnels[i].fbackgimg; 
			sketchgraphicspanel.dtsketches.addElement(disptunnels[i].tsketch); 
		}
			
		sketchgraphicspanel.bEditable = lbEditable; 
		
		SelectTunnel(activetunnel); 
		sketchgraphicspanel.Max(true); 

		toFront(); 
		show(); 
	}

	/////////////////////////////////////////////
	void ActivateSketchSymbols()  
	{
		sketchgraphicspanel.ClearSelection(); 
		toFront(); 
		show(); 
	}
}


