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

import javax.swing.JSlider; 
import javax.swing.event.ChangeEvent; 
import javax.swing.event.ChangeListener; 

//
//
// SketchDisplay
//
//


// this class contains the whole outer set of options and buttons
class SketchDisplay extends JFrame
{
	MainBox mainbox; 

	// the panel which holds the sketch graphics
	SketchGraphics sketchgraphicspanel;  
	DepthSliderControl depthslidercontrol; 

	OneTunnel vgsymbols; 

	// the menu bar 
	JMenuBar menubar = new JMenuBar(); 

	// file menu
	JMenu menufile = new JMenu("File"); 

	JMenuItem miImportSketchCentreline = new JMenuItem("Import Centreline"); 
	JMenuItem miImportSketch = new JMenuItem("Import Sketch"); 

	JMenuItem miPrint = new JMenuItem("Print"); 

	JMenuItem miWriteHPGLthick = new JMenuItem("HPGL thick"); 
	JMenuItem miWriteHPGLthin = new JMenuItem("HPGL thin"); 

	JMenuItem doneitem = new JMenuItem("Close"); 

	// view menu
	JMenu menuview = new JMenu("View"); 
	JMenuItem mimax = new JMenuItem("Max"); 
	JMenuItem micentre = new JMenuItem("Centre"); 
	JMenuItem miupright = new JMenuItem("Upright"); 
	JMenuItem miscad = new JMenuItem("Scale Down"); 
	JMenuItem miscau = new JMenuItem("Scale Up"); 
	JMenuItem miredraw = new JMenuItem("Redraw"); 

	// display menu.  
	JMenu menudisplay = new JMenu("Display"); 
	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem("Centreline", true); 
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem("StationNames", false); 
	JCheckBoxMenuItem miXSections = new JCheckBoxMenuItem("XSections", true); 
	JCheckBoxMenuItem miTubes = new JCheckBoxMenuItem("Tubes", true); 
	JCheckBoxMenuItem miAxes = new JCheckBoxMenuItem("Axes", true); 
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem("Depth Colours", true); 
	JCheckBoxMenuItem miShowNodes = new JCheckBoxMenuItem("Show Nodes", true); 
	JCheckBoxMenuItem miShowBackground = new JCheckBoxMenuItem("Show Background", true); 
	JCheckBoxMenuItem miShowGrid = new JCheckBoxMenuItem("Show Grid", true); 

	// motion menu
	JMenu menumotion = new JMenu("Motion"); 
	JCheckBoxMenuItem miTabletMouse = new JCheckBoxMenuItem("Tablet Mouse", false); 
	JCheckBoxMenuItem miEnableRotate = new JCheckBoxMenuItem("Enable rotate", false); 
	JCheckBoxMenuItem miTrackLines = new JCheckBoxMenuItem("Track Lines", false); 
	JCheckBoxMenuItem miShearWarp = new JCheckBoxMenuItem("Shear Warp", false); 

	// action menu
	JMenu menuaction = new JMenu("Action"); 
	JMenuItem miDetailRender = new JMenuItem("Detail Render"); 
	JMenuItem miUpdateSAreas = new JMenuItem ("Update SAreas"); 
	JMenuItem miUpdateSymbolLayout = new JMenuItem("Update Symbol Lay"); 
	JMenuItem miDeselect = new JMenuItem("Deselect"); 
	JMenuItem miDelete = new JMenuItem("Delete"); 
	JMenuItem miFuse = new JMenuItem("Fuse"); 
	JMenuItem miBackNode = new JMenuItem("Back"); 
	JMenuItem miReflect = new JMenuItem("Reflect"); 
	JMenuItem miSetasaxis = new JMenuItem("Set As Axis"); 
	JMenuItem miDeleteAllSymbols = new JMenuItem("Delete All Symbols"); 

	// top right buttons.  
	JButton bmoveground = new JButton("Shift Picture"); 
	JButton bmovebackground = new JButton("Shift Background"); 
	JTextField sfbackground = new JTextField(""); 
	File backgrounddir = null; 

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

	// hold down type buttons
	JButton bupdatesareas = new JButton("Update SAreas"); 
	JButton bpinkgrouparea = new JButton("V Group Area"); 
	JButton bpinkdownsketch = new JButton("V Down Sketch"); 
	JButton bpinkdownsketchU = new JButton("V Down SketchU"); 

	/////////////////////////////////////////////
	ItemListener SketchRepaint = new ItemListener()
	{ 
		public void itemStateChanged(ItemEvent e) { sketchgraphicspanel.bmainImgValid = false;  sketchgraphicspanel.repaint(); }  
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
		void CloseWindow()  
		{
			mainbox.symbolsdisplay.hide(); 
			if (sketchgraphicspanel.tsketch.bSymbolType && sketchgraphicspanel.tsketch.bsketchfilechanged) 
			{
				sketchgraphicspanel.tsketch.iicon = null; // assumes a change.  
				mainbox.symbolsdisplay.UpdateIconPanel(); 
			}
			sketchgraphicspanel.Deselect(true); 
			setVisible(false); 
		}

		public void windowClosing(WindowEvent e)
		{
			CloseWindow(); 
		}

		public void actionPerformed(ActionEvent e)
		{
			CloseWindow(); 
		}
	}

	/////////////////////////////////////////////
	class DepthSliderControl extends JPanel implements ChangeListener
	{
		JSlider sllower = new JSlider(0, 100, 0); 
		JSlider slupper = new JSlider(0, 100, 100); 
		SketchGraphics sketchgraphicspanel; 

		DepthSliderControl(SketchGraphics lsketchgraphicspanel)
		{
			super(new BorderLayout()); 
			sketchgraphicspanel = lsketchgraphicspanel; 
			add("North", sllower); 
			add("South", slupper); 
			sllower.addChangeListener(this); 
			slupper.addChangeListener(this); 
		}

		public void stateChanged(ChangeEvent e)
		{
			float sllow = Math.min(sllower.getValue(), slupper.getValue()) / 100.0F; 
			float slupp = Math.max(sllower.getValue(), slupper.getValue()) / 100.0F; 
			sketchgraphicspanel.tsketch.bRestrictZalt = !((sllow == 0.0F) && (slupp == 1.0F));  
			sketchgraphicspanel.tsketch.SetVisibleByZ(sllow, slupp); 
			sketchgraphicspanel.bmainImgValid = false;  
			sketchgraphicspanel.repaint();  
		}
	}


	/////////////////////////////////////////////
	// set up the arrays
	SketchDisplay(MainBox lmainbox, OneTunnel lvgsymbols) 
	{
		super("Sketch Display"); 

		// symbols communication. 
		mainbox = lmainbox;  
		vgsymbols = lvgsymbols; 

		// it's important that the two panels are constructed in order.  
		sketchgraphicspanel = new SketchGraphics(this); 

		// the depth viewing controls
		depthslidercontrol = new DepthSliderControl(sketchgraphicspanel); 


		// file menu stuff.  
		miImportSketchCentreline.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ImportSketchCentreline();  sketchgraphicspanel.repaint(); } } ); 
		menufile.add(miImportSketchCentreline); 

		miImportSketch.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event)  { sketchgraphicspanel.ImportSketch(mainbox.tunnelfilelist.activesketch, mainbox.tunnelfilelist.activetunnel);  sketchgraphicspanel.repaint(); } } ); 
		menufile.add(miImportSketch); 

		miPrint.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(); } } ); 
		menufile.add(miPrint); 

		miWriteHPGLthick.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.WriteHPGL(true); } } ); 
		menufile.add(miWriteHPGLthick); 

		miWriteHPGLthin.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.WriteHPGL(false); } } ); 
		menufile.add(miWriteHPGLthin); 

		doneitem.addActionListener(new SketchHide()); 
		menufile.add(doneitem); 

		menubar.add(menufile); 

		// view menu stuff.  
		mimax.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.iMaxAction = 2; sketchgraphicspanel.repaint(); } } ); 
		menuview.add(mimax); 

		micentre.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.iMaxAction = 1; sketchgraphicspanel.repaint(); } } ); 
		menuview.add(micentre); 

		miupright.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.iMaxAction = 3; sketchgraphicspanel.repaint(); } } ); 
		menuview.add(miupright); 

		miscad.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Scale(0.5F);  sketchgraphicspanel.repaint(); } } ); 
		menuview.add(miscad); 

		miscau.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.Scale(2.0F);  sketchgraphicspanel.repaint(); } } ); 
		menuview.add(miscau); 

		miredraw.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.backgroundimg.bBackImageDoneGood = false;  sketchgraphicspanel.bmainImgValid = false;  sketchgraphicspanel.repaint(); } } ); 
		menuview.add(miredraw); 

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

		miShowNodes.addItemListener(SketchRepaint); 
		menudisplay.add(miShowNodes); 

		miShowBackground.addItemListener(new ItemListener() 
			{ public void itemStateChanged(ItemEvent e) { sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.backgroundimg.bBackImageDoneGood = false; sketchgraphicspanel.repaint(); } } ); 
		menudisplay.add(miShowBackground); 

		miShowGrid.addItemListener(new ItemListener() 
			{ public void itemStateChanged(ItemEvent e) { sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.backgroundimg.bBackImageDoneGood = false; sketchgraphicspanel.repaint(); } } ); 
		menudisplay.add(miShowGrid); 

		menubar.add(menudisplay); 

		// motion menu  
		menumotion.add(miTrackLines); 
		menumotion.add(miTabletMouse); 
		menumotion.add(miEnableRotate); 
		menumotion.add(miShearWarp); 

		menubar.add(menumotion); 


		// action menu  
		miDetailRender.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.bNextRenderSlow = true; sketchgraphicspanel.bmainImgValid = false; sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miDetailRender);  

		miUpdateSAreas.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.UpdateSAreas();  sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miUpdateSAreas);  

		miUpdateSymbolLayout.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { sketchgraphicspanel.UpdateSymbolLayout();  sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miUpdateSymbolLayout);  

		miDeselect.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.Deselect(false); sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miDeselect); 

		miDelete.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.DeleteSel(); } } ); 
		menuaction.add(miDelete); 

		miFuse.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.FuseCurrent(miShearWarp.isSelected()); } } ); 
		menuaction.add(miFuse); 

		miBackNode.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.BackSel();  sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miBackNode); 

		miReflect.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ReflectCurrent(); } } ); 
		menuaction.add(miReflect); 

		miSetasaxis.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.SetAsAxis(); } } ); 
		menuaction.add(miSetasaxis); 

		miDeleteAllSymbols.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ClearSelection();  sketchgraphicspanel.DeleteSymbols(null);  sketchgraphicspanel.repaint(); } } ); 
		menuaction.add(miDeleteAllSymbols); 

		menubar.add(menuaction); 


		// done with menu bar.  
		setJMenuBar(menubar); 


		// do the buttons and fields stuff for the right hand side panel.  
		JPanel sidecontrols = new JPanel(new GridLayout(0, 1)); 

		bmoveground.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.MoveGround(false); } } ); 
		sidecontrols.add(bmoveground); 

		bmovebackground.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.MoveGround(true); } } ); 
		sidecontrols.add(bmovebackground); 

		sfbackground.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.tsketch.SetBackground(backgrounddir, sfbackground.getText());  sketchgraphicspanel.backgroundimg.bMaxBackImage = true;  sketchgraphicspanel.backgroundimg.SetImageF(sketchgraphicspanel.tsketch.fbackgimg, getToolkit());  sketchgraphicspanel.repaint(); } } ); 
		sidecontrols.add(sfbackground); 

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
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.BackSel();  sketchgraphicspanel.repaint(); } } ); 
		pathcoms.add(pthback); 

		pthfuse.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.FuseCurrent(miShearWarp.isSelected()); } } ); 
		pathcoms.add(pthfuse); 


		// class used to handle the bupdatesareas button behavoir and state 
		// can't do the depression of it through the button interface.  
		// could do some fancy changing of its name depending on the value of bSAreasValid
		class BUpdateSAreas extends MouseAdapter implements ActionListener 
		{
			public void actionPerformed(ActionEvent event) 
			{
				if (!sketchgraphicspanel.bSAreasUpdated) 
				{
					sketchgraphicspanel.UpdateSAreas(); 
					sketchgraphicspanel.repaint(); 
				}
			}

			public void mousePressed(MouseEvent event) 
			{
				if (sketchgraphicspanel.bSAreasUpdated && !sketchgraphicspanel.bDisplayOverlay[0]) 
				{
					sketchgraphicspanel.bDisplayOverlay[0] = true; 
					sketchgraphicspanel.repaint(); 
				}
			}

			public void mouseReleased(MouseEvent event) 
			{
				if (sketchgraphicspanel.bDisplayOverlay[0]) 
				{
					sketchgraphicspanel.bDisplayOverlay[0] = false; 
					sketchgraphicspanel.repaint(); 
				}
			}
		}
		BUpdateSAreas bupdatesareasLis = new BUpdateSAreas(); 
		bupdatesareas.addActionListener(bupdatesareasLis); 
		bupdatesareas.addMouseListener(bupdatesareasLis); 
		pathcoms.add(bupdatesareas); 

		// we need the button pressed cases.  Shame there's no function in jbutton for this.  
		class BPinkDisplay extends MouseAdapter
		{
			int ob; 
			
			BPinkDisplay(int lob) 
			{
				ob = lob; 
			}

			public void mousePressed(MouseEvent event) 
			{
				if (!sketchgraphicspanel.bDisplayOverlay[ob]) // necessary?  
				{
					sketchgraphicspanel.bDisplayOverlay[ob] = true; 
					sketchgraphicspanel.repaint(); 
				}
			}

			public void mouseReleased(MouseEvent event) 
			{
				if (sketchgraphicspanel.bDisplayOverlay[ob]) // necessary?  
				{
					sketchgraphicspanel.bDisplayOverlay[ob] = false; 
					sketchgraphicspanel.repaint(); 
				}
			}
		}

		BPinkDisplay bpinkgroupareaLis = new BPinkDisplay(1); 
		bpinkgrouparea.addMouseListener(bpinkgroupareaLis); 
		pathcoms.add(bpinkgrouparea); 

		BPinkDisplay bpinkdownsketchLis = new BPinkDisplay(2); 
		bpinkdownsketch.addMouseListener(bpinkdownsketchLis); 
		pathcoms.add(bpinkdownsketch); 

		BPinkDisplay bpinkdownsketchULis = new BPinkDisplay(3); 
		bpinkdownsketchU.addMouseListener(bpinkdownsketchULis); 
		pathcoms.add(bpinkdownsketchU); 


		JPanel pathistic = new JPanel(new BorderLayout()); 
		sketchlinestyle.linestylesel.addActionListener(ChangePathParams); 
		sketchlinestyle.pthsplined.addActionListener(ChangePathParams); 
		sketchlinestyle.pthlabel.getDocument().addDocumentListener(ChangePathParams); 

		pathistic.add("North", sketchlinestyle); 
		pathistic.add("South", pathcoms); 

		JPanel sidepanel = new JPanel(new BorderLayout()); 
		sidepanel.add("North", sidecontrols); 
		sidepanel.add("South", pathistic); 

		JPanel grpanel = new JPanel(new BorderLayout()); 
		grpanel.add("Center", sketchgraphicspanel); 
		grpanel.add("South", depthslidercontrol); 

		// split pane between side panel and graphics area 
		JSplitPane splitPaneG = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPaneG.setDividerLocation(200); 

		splitPaneG.setLeftComponent(sidepanel); 
		splitPaneG.setRightComponent(grpanel); 

		
		// final set up of display
		getContentPane().setLayout(new BorderLayout()); 
		getContentPane().add("Center", splitPaneG); 

		addWindowListener(new SketchHide()); 

		pack(); 
        setSize(800, 600);
		setLocation(300, 100); 
    }




	/////////////////////////////////////////////
	void ActivateSketchDisplay(OneTunnel activetunnel, OneSketch activesketch, boolean lbEditable)  
	{
		backgrounddir = activetunnel.tundirectory; 
		sfbackground.setText(activesketch.backgroundimgname == null ? "" : activesketch.backgroundimgname); 
		sketchgraphicspanel.bEditable = lbEditable; 
		
		sketchgraphicspanel.tsketch = activesketch; 
		sketchgraphicspanel.activetunnel = activetunnel; 
		sketchgraphicspanel.asketchavglast = null; // used for lazy evaluation of the average transform.  

		// set the transform pointers to same object
		sketchgraphicspanel.backgroundimg.currparttrans = sketchgraphicspanel.tsketch.backgimgtrans; 
		setTitle(activesketch.sketchname); 

		// set the observed values 
		ssobsPath.ObserveSelection(-1, sketchgraphicspanel.tsketch.vpaths.size()); 
		ssobsSymbol.ObserveSelection(-1, sketchgraphicspanel.tsketch.vssymbols.size()); 
		ssobsArea.ObserveSelection(-1, sketchgraphicspanel.tsketch.vsareas.size()); 

		// maximize
		sketchgraphicspanel.iMaxAction = 2; 

		// load in the background image.  
		sketchgraphicspanel.backgroundimg.SetImageF(sketchgraphicspanel.tsketch.fbackgimg, getToolkit()); 
		sketchgraphicspanel.bmainImgValid = false;  
		sketchgraphicspanel.repaint(); 

		mainbox.symbolsdisplay.show(); 

		toFront(); 
		show(); 
	}
}


