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

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

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

	// the window with the symbols
	SymbolsDisplay symbolsdisplay;

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


	// hold down type buttons
	JButton bupdatesareas = new JButton("Update SAreas");
	JButton bpinkgrouparea = new JButton("V Group Area");
	JButton bpinkdownsketch = new JButton("V Down Sketch");
	JButton bpinkdownsketchU = new JButton("V Down SketchU");



	/////////////////////////////////////////////
	class CChangePathParams implements ActionListener
	{
		// we'd like the default spline case to be reset every time the line type is changed
		// so that it's off when we make a connecting type.
		int maskcpp = 0;
		public void actionPerformed(ActionEvent e) { sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); };
	};

	CChangePathParams ChangePathParams = new CChangePathParams();


	/////////////////////////////////////////////
	// inactivate case
	class SketchHide extends WindowAdapter implements ActionListener
	{
		void CloseWindow()
		{
			//mainbox.symbolsdisplay.hide();
			if (sketchgraphicspanel.tsketch.bSymbolType && sketchgraphicspanel.tsketch.bsketchfilechanged)
			{
				sketchgraphicspanel.tsketch.iicon = null; // assumes a change.
				//symbolsdisplay.UpdateIconPanel();
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
	// View menu actions
	/////////////////////////////////////////////
	public class AcViewac extends AbstractAction
	{
		int viewaction;
		int ks;
        public AcViewac(String name, String shdesc, int lks, int lviewaction)
		{
            super(name);
			ks = lks;
            putValue(SHORT_DESCRIPTION, shdesc);
			viewaction = lviewaction;
        }
        public void actionPerformed(ActionEvent e)
		{
			if (viewaction == 4)
				sketchgraphicspanel.Scale(0.5F);
			else if (viewaction == 5)
				sketchgraphicspanel.Scale(2.0F);
			else if (viewaction == 6)
				sketchgraphicspanel.Translate(0.5F, 0.0F);
			else if (viewaction == 7)
				sketchgraphicspanel.Translate(-0.5F, 0.0F);
			else if (viewaction == 8)
				sketchgraphicspanel.Translate(0.0F, 0.5F);
			else if (viewaction == 9)
				sketchgraphicspanel.Translate(0.0F, -0.5F);
			else if (viewaction == 10)
				sketchgraphicspanel.Translate(0.0F, 0.0F);
			else
				sketchgraphicspanel.iMaxAction = viewaction;
			sketchgraphicspanel.repaint();
        }
	}

	// would like to use VK_RIGHT instead of VK_F12, but is not detected.
	AcViewac acvMax = new AcViewac("Max", "Maximize View", KeyEvent.VK_M, 2);
	AcViewac acvCentre = new AcViewac("Centre", "Centre View", 0, 1);
	AcViewac acvUpright = new AcViewac("Upright", "Upright View", 0, 3);
	AcViewac acvScaledown = new AcViewac("Scale Down", "Zoom out", KeyEvent.VK_MINUS, 4);
	AcViewac acvScaleup = new AcViewac("Scale Up", "Zoom in", KeyEvent.VK_PLUS, 5);
	AcViewac acvRight = new AcViewac("Right", "Translate view right", KeyEvent.VK_F12, 6);
	AcViewac acvLeft = new AcViewac("Left", "Translate view left", KeyEvent.VK_F9, 7);
	AcViewac acvUp = new AcViewac("Up", "Translate view up", KeyEvent.VK_F10, 8);
	AcViewac acvDown = new AcViewac("Down", "Translate view down", KeyEvent.VK_F11, 9);
	AcViewac acvRedraw = new AcViewac("Redraw", "Redraw screen", 0, 10);

	// view menu
	JMenu menuView = new JMenu("View");
	AcViewac[] acViewarr = { acvMax, acvCentre, acvUpright, acvScaledown, acvScaleup, acvRight, acvLeft, acvUp, acvDown, acvRedraw };



	/////////////////////////////////////////////
	// Display menu actions
	/////////////////////////////////////////////
	public class AcDispchbox extends AbstractAction
	{
		boolean backrepaint;
        public AcDispchbox(String name, String shdesc, boolean lbackrepaint)
		{
            super(name);
            putValue(SHORT_DESCRIPTION, shdesc);
			backrepaint = lbackrepaint;
        }
        public void actionPerformed(ActionEvent e)
		{
			if (backrepaint)
				sketchgraphicspanel.backgroundimg.bBackImageDoneGood = false;
			sketchgraphicspanel.bmainImgValid = false;
			sketchgraphicspanel.repaint();
		}
	}

	AcDispchbox acdCentreline = new AcDispchbox("Centreline", "Centreline visible", false);
	AcDispchbox acdStationNames = new AcDispchbox("StaionNames", "Station names visible", false);
	AcDispchbox acdXSections = new AcDispchbox("XSections", "Cross sections visible", false);
	AcDispchbox acdTubes = new AcDispchbox("Tubes", "Tubes visible", false);
	AcDispchbox acdAxes = new AcDispchbox("Axes", "Axes visible", false);
	AcDispchbox acdDepthCols = new AcDispchbox("Depth Colours", "Depth colours visible", false);
	AcDispchbox acdShowNodes = new AcDispchbox("Show Nodes", "Path nodes visible", false);
	AcDispchbox acdShowBackground = new AcDispchbox("Show Background", "Background image visible", true);
	AcDispchbox acdShowGrid = new AcDispchbox("Show Grid", "Background grid visible", true);

	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem(acdCentreline);
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem(acdStationNames);
	JCheckBoxMenuItem miXSections = new JCheckBoxMenuItem(acdXSections);
	JCheckBoxMenuItem miTubes = new JCheckBoxMenuItem(acdTubes);
	JCheckBoxMenuItem miAxes = new JCheckBoxMenuItem(acdAxes);
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem(acdDepthCols);
	JCheckBoxMenuItem miShowNodes = new JCheckBoxMenuItem(acdShowNodes);
	JCheckBoxMenuItem miShowBackground = new JCheckBoxMenuItem(acdShowBackground);
	JCheckBoxMenuItem miShowGrid = new JCheckBoxMenuItem(acdShowGrid);


	// display menu.
	JMenu menuDisplay = new JMenu("Display");
	JCheckBoxMenuItem[] miDisplayarr = { miCentreline, miStationNames, miXSections, miTubes, miAxes, miShowNodes, miDepthCols, miShowBackground, miShowGrid };


	/////////////////////////////////////////////
	// Motion menu
	JCheckBoxMenuItem miTabletMouse = new JCheckBoxMenuItem("Tablet Mouse", false);
	JCheckBoxMenuItem miEnableRotate = new JCheckBoxMenuItem("Enable rotate", false);
	JCheckBoxMenuItem miTrackLines = new JCheckBoxMenuItem("Track Lines", false);
	JCheckBoxMenuItem miShearWarp = new JCheckBoxMenuItem("Shear Warp", false);
	JCheckBoxMenuItem miDefaultSplines = new JCheckBoxMenuItem("Splines Default", true);

	JMenu menuMotion = new JMenu("Motion");
	JCheckBoxMenuItem[] miMotionarr = { miTabletMouse, miEnableRotate, miTrackLines, miShearWarp, miDefaultSplines };

	/////////////////////////////////////////////
	// Action menu actions
	/////////////////////////////////////////////
	public class AcActionac extends AbstractAction
	{
		int acaction;
		int ks;
        public AcActionac(String name, String shdesc, int lks, int lacaction)
		{
            super(name);
			ks = lks;
            putValue(SHORT_DESCRIPTION, shdesc);
			acaction = lacaction;
        }

        public void actionPerformed(ActionEvent e)
		{
			if (acaction == 4)
				sketchgraphicspanel.Deselect(false);
			else if (acaction == 5)
				sketchgraphicspanel.DeleteSel();
			else if (acaction == 6)
				sketchgraphicspanel.FuseCurrent(miShearWarp.isSelected());
			else if (acaction == 7)
				sketchgraphicspanel.BackSel();
			else if (acaction == 8)
				sketchgraphicspanel.ReflectCurrent();
			else if (acaction == 9)
				sketchgraphicspanel.SetAsAxis();
			else if ((acaction == 11) || (acaction == 12))
			{
				TN.SetStrokeWidths(TN.strokew * (acaction == 11 ? 2.0F : 0.5F));
				sketchgraphicspanel.bmainImgValid = false;
			}

			else if (acaction == 20)
				sketchgraphicspanel.SetIColsDefault();
			else if (acaction == 21)
				sketchgraphicspanel.SetIColsByZ();
			else if (acaction == 22)
				sketchgraphicspanel.SetIColsProximity(0);
			else if (acaction == 23)
				sketchgraphicspanel.SetIColsProximity(1);

			// the automatic actions which should be running constantly in a separate thread
			else if (acaction == 51)
			{
				// heavyweight stuff
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
				pd.SetZaltsFromCNodesByInverseSquareWeight(sketchgraphicspanel.tsketch); // passed in for the zaltlo/hi values
			}
			else if (acaction == 52)
				sketchgraphicspanel.UpdateSAreas();
			else if (acaction == 53)
				sketchgraphicspanel.UpdateSymbolLayout();
			else if (acaction == 56) // detail render
			{
				sketchgraphicspanel.bNextRenderSlow = true;
				sketchgraphicspanel.bmainImgValid = false;
			}

			else if (acaction == 57) // printing proximities to the command line
			{
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
				pd.PrintCNodeProximity(3);
			}

			sketchgraphicspanel.repaint();
        }
	}

	// action menu
	// would like to use VK_RIGHT instead of VK_F12, but is not detected.
	AcActionac acaDeselect = new AcActionac("Deselect", "Deselect", 0, 4);
	AcActionac acaDelete = new AcActionac("Delete", "Delete selection", KeyEvent.VK_DELETE, 5);
	AcActionac acaFuse = new AcActionac("Fuse", "Fuse paths", 0, 6);
	AcActionac acaBackNode = new AcActionac("Back", "Remove last hit", KeyEvent.VK_BACK_SPACE, 7);
	AcActionac acaReflect = new AcActionac("Reflect", "Reflect path", 0, 8);
	AcActionac acaSetasaxis = new AcActionac("Set As Axis", "Set As Axis", 0, 9);

	AcActionac acaStrokeThin = new AcActionac("Stroke >>", "Thinner lines", KeyEvent.VK_LESS, 11);
	AcActionac acaStrokeThick = new AcActionac("Stroke <<", "Thicker lines", KeyEvent.VK_GREATER, 12);

	JMenu menuAction = new JMenu("Action");
	AcActionac[] acActionarr = { acaDeselect, acaDelete, acaFuse, acaBackNode, acaReflect, acaStrokeThin, acaStrokeThick, acaSetasaxis };

	// auto menu
	AcActionac acaSetZonnodes = new AcActionac("Set nodeZ", "Set node z from centreline", 0, 51);
	AcActionac acaUpdateSAreas = new AcActionac("Update SAreas", "Update automatic areas", 0, 52);
	AcActionac acaUpdateSymbolLayout = new AcActionac("Update Symbol Lay", "Update symbol layout", 0, 53);
	AcActionac acaDetailRender = new AcActionac("Detail Render", "Detail Render", 0, 56);

	JMenu menuAuto = new JMenu("Auto");
	AcActionac[] acAutoarr = { acaSetZonnodes, acaUpdateSAreas, acaUpdateSymbolLayout, acaDetailRender };

	// colour menu
	AcActionac acaColourDefault = new AcActionac("Default", "Plain colours", 0, 20);
	AcActionac acaColourByZ = new AcActionac("Height", "Depth colours", 0, 21);
	AcActionac acaColourByProx = new AcActionac("Proximity", "Visualize proximity to selection", 0, 22);
	AcActionac acaColourByCnodeWeight = new AcActionac("CNode Weights", "Visualize centreline node weights", 0, 23);
	AcActionac acaPrintProximities = new AcActionac("Print Prox", "Print proximities of nodes to centrelines", 0, 57);

	// action menu
	JMenu menuColour = new JMenu("Colour");

	/////////////////////////////////////////////
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

		// the window with the symbols
		symbolsdisplay = new SymbolsDisplay(vgsymbols, this);

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
		for (int i = 0; i < acViewarr.length; i++)
		{
			JMenuItem mi = new JMenuItem(acViewarr[i]);
			if (acViewarr[i].ks != 0)
				mi.setAccelerator(KeyStroke.getKeyStroke(acViewarr[i].ks, java.awt.event.InputEvent.CTRL_MASK));
			menuView.add(mi);
		}
		menubar.add(menuView);

		// setup the display menu responses
		for (int i = 0; i < miDisplayarr.length; i++)
		{
			miDisplayarr[i].setState(miDisplayarr[i] != miStationNames);
			menuDisplay.add(miDisplayarr[i]);
		}
		menubar.add(menuDisplay);

		// motion menu
		for (int i = 0; i < miMotionarr.length; i++)
			menuMotion.add(miMotionarr[i]);
		menubar.add(menuMotion);

		// action menu stuff.
		for (int i = 0; i < acActionarr.length; i++)
		{
			JMenuItem mi = new JMenuItem(acActionarr[i]);
			if (acActionarr[i].ks != 0)
				mi.setAccelerator(KeyStroke.getKeyStroke(acActionarr[i].ks, java.awt.event.InputEvent.CTRL_MASK));
			menuAction.add(mi);
		}
		menubar.add(menuAction);

		// auto menu
		for (int i = 0; i < acAutoarr.length; i++)
			menuAuto.add(new JMenuItem(acAutoarr[i]));
		menubar.add(menuAuto);

		// colour menu stuff.
		menuColour.add(new JMenuItem(acaColourDefault));
		menuColour.add(new JMenuItem(acaColourByZ));
		menuColour.add(new JMenuItem(acaColourByProx));
		menuColour.add(new JMenuItem(acaColourByCnodeWeight));
		menuColour.add(new JMenuItem(acaPrintProximities)); 
		menubar.add(menuColour);


		// menu bar is complete.
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
		pstr.add(new JButton(acaStrokeThin));
		pstr.add(new JButton(acaStrokeThick));
		pathcoms.add(pstr);

		pathcoms.add(new JButton(acaDeselect));
		pathcoms.add(new JButton(acaDelete));
		pathcoms.add(new JButton(acaBackNode));
		pathcoms.add(new JButton(acaFuse));


		// class used to handle the bupdatesareas button behavoir and state
		// can't do the depression of it through the button interface.
		// could do some fancy changing of its name depending on the value of bSAreasValid
		class BUpdateSAreas extends MouseAdapter implements ActionListener
		{
			public void actionPerformed(ActionEvent event)
			{
				if (!sketchgraphicspanel.tsketch.bSAreasUpdated)
				{
					sketchgraphicspanel.UpdateSAreas();
					sketchgraphicspanel.repaint();
				}
			}

			public void mousePressed(MouseEvent event)
			{
				if (sketchgraphicspanel.tsketch.bSAreasUpdated && !sketchgraphicspanel.bDisplayOverlay[0])
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

		// return key in the label thing (replacing document listener, hopefully)
		sketchlinestyle.pthlabel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.GoSetLabelCurrPath();  } } );

		pathistic.add("North", sketchlinestyle);
		pathistic.add("South", pathcoms);

		JScrollPane scsymbolbutts = new JScrollPane(symbolsdisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel sidepanel = new JPanel(new BorderLayout());
		sidepanel.add("North", sidecontrols);
		sidepanel.add("Center", symbolsdisplay); //scsymbolbutts);
		sidepanel.add("South", pathistic);

		JPanel grpanel = new JPanel(new BorderLayout());
		grpanel.add("Center", sketchgraphicspanel);
		grpanel.add("South", depthslidercontrol);

		// split pane between side panel and graphics area
		JSplitPane splitPaneG = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPaneG.setDividerLocation(300);

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
		//ssobsSymbol.ObserveSelection(-1, sketchgraphicspanel.tsketch.vssymbols.size());
		ssobsArea.ObserveSelection(-1, sketchgraphicspanel.tsketch.vsareas.size());

		// maximize
		sketchgraphicspanel.iMaxAction = 2;

		// load in the background image.
		sketchgraphicspanel.backgroundimg.SetImageF(sketchgraphicspanel.tsketch.fbackgimg, getToolkit());
		sketchgraphicspanel.bmainImgValid = false;
		sketchgraphicspanel.repaint();

		sketchgraphicspanel.DChangeBackNode();

		toFront();
		setVisible(true);
	}
}


