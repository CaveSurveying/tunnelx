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
import javax.swing.JLabel;

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

	OneTunnel vgsymbols;

	// the window with the symbols
	SymbolsDisplay symbolsdisplay;

	// the menu bar
	JMenuBar menubar = new JMenuBar();

	// file menu
	JMenu menufile = new JMenu("File");

	JMenuItem miImportSketchCentreline = new JMenuItem("Import Centreline");
	JMenuItem miImportSketch = new JMenuItem("Import Sketch");
	JMenuItem miCopyCentrelineElev = new JMenuItem("Copy Centreline Elev");

	JMenuItem miPrintView = new JMenuItem("Print view");
	JMenuItem miPrintToScale = new JMenuItem("Print Scale " + TN.prtscale);

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
	PathSelectionObserver pathselobs = new PathSelectionObserver();

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
		public void actionPerformed(ActionEvent e)
			{ sketchgraphicspanel.GoSetParametersCurrPath(maskcpp); };
	};

	CChangePathParams ChangePathParams = new CChangePathParams();


	/////////////////////////////////////////////
	// inactivate case
	class SketchHide extends WindowAdapter implements ActionListener
	{
		void CloseWindow()
		{
			//mainbox.symbolsdisplay.hide();
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


	static boolean bPerformSkSubsetActions = true;

	/////////////////////////////////////////////
	class SkSubset
	{
		String name;
		JCheckBoxMenuItem miSubsetViz;
		int npaths;

		SkSubset(String lname)
		{
			name = lname;
			miSubsetViz = new JCheckBoxMenuItem(name);
			miSubsetViz.addActionListener(new ActionListener()
				{ public void actionPerformed(ActionEvent event)
					{ if (bPerformSkSubsetActions) { Updatecbmsub();  sketchgraphicspanel.repaint(); } } } );
			npaths = 0;
		}
	};

	Vector vsksubsets = new Vector();
	int isksubfirstactive = -1;

	/////////////////////////////////////////////
	class PathSelectionObserver extends JPanel
	{
		JTextField tfselitem = new JTextField();
		JLabel lab = new JLabel("paths/");
		JTextField tfselnum = new JTextField();
		int item = -1;
		int num = -1;

		PathSelectionObserver()
		{
			super(new GridLayout(1, 0));
			tfselitem.setEditable(false);
			tfselnum.setEditable(false);
			add(tfselitem);
			add(lab);
			add(tfselnum);
		}

		void ObserveSelection(int litem, int lnum)
		{
			if (item != litem)
			{
				item = litem;
				String res = "";
				if (item != -1)
				{
					// find the index of the endpoints of the path for martin's debugging
					res = String.valueOf(item + 1);

					// this bit is slow and should be enabled by a mode
					OnePath op = (OnePath)sketchgraphicspanel.tsketch.vpaths.elementAt(item);
					/*
					int n0 = sketchgraphicspanel.tsketch.vnodes.indexOf(op.pnstart);
					int n1 = sketchgraphicspanel.tsketch.vnodes.indexOf(op.pnend);
					res = res + " (" + n0 + ", " + n1 + ")";
					*/

					if (!op.vssubsets.isEmpty())
					{
						if (op.vssubsets.size() == 1)
							res = res + " [" + (String)op.vssubsets.elementAt(0) + "]";
						else
						{
							for (int i = 0; i < op.vssubsets.size(); i++) 
								res = res + (i == 0 ? " [" : ", ") + (String)op.vssubsets.elementAt(i);
							res = res + "]";
						}
					}
				}
				tfselitem.setText(res);
			}
			if (num != lnum)
			{
				num = lnum;
				tfselnum.setText(num == -1 ? "" : String.valueOf(num));
			}
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
				sketchgraphicspanel.MaxAction(viewaction);
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
				sketchgraphicspanel.RedoBackgroundView();
			else
				sketchgraphicspanel.RedrawBackgroundView();
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
				SketchLineStyle.SetStrokeWidths(SketchLineStyle.strokew * (acaction == 11 ? 2.0F : 0.5F));
				sketchgraphicspanel.RedrawBackgroundView();
			}
			else if (acaction == 13)
				sketchgraphicspanel.TranslateConnectedSet();


			else if (acaction == 20)
				sketchgraphicspanel.SetIColsDefault();
			else if (acaction == 21)
				sketchgraphicspanel.SetIColsByZ(true);
			else if (acaction == 22)
				sketchgraphicspanel.SetIColsProximity(0);
			else if (acaction == 23)
				sketchgraphicspanel.SetIColsProximity(1);
			else if (acaction == 24)
				sketchgraphicspanel.SetIColsBySubset();

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
				sketchgraphicspanel.RedrawBackgroundView();
			}

			else if (acaction == 57) // printing proximities to the command line
			{
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
				pd.PrintCNodeProximity(3);
			}

			// subsets
			else if (acaction == 70)
				NewSubset(sketchlinestyle.pthlabel.getText(), true); // just use from a label we have somewhere
			else if (acaction == 71)
				RemoveSubset();
			else if (acaction == 72)
				AddSelCentreToCurrentSubset();
			else if (acaction == 77)
				AddRemainingCentreToCurrentSubset();
			else if (acaction == 73)
				PartitionRemainsByClosestSubset();
			else if (acaction == 74)
				PutSelToSubset(true);
			else if (acaction == 75)
				PutSelToSubset(false);
			else if (acaction == 76)
				Updatecbmsub();

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

	AcActionac acaFuseTranslateComponent = new AcActionac("Fuse Translate", "Translates Connected Component", 0, 13);

	JMenu menuAction = new JMenu("Action");
	AcActionac[] acActionarr = { acaDeselect, acaDelete, acaFuse, acaBackNode, acaReflect, acaStrokeThin, acaStrokeThick, acaSetasaxis, acaFuseTranslateComponent };

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
	AcActionac acaColourBySubset = new AcActionac("Colour by Subset", "Subset Colours", 0, 24);
	AcActionac acaPrintProximities = new AcActionac("Print Prox", "Print proximities of nodes to centrelines", 0, 57);

	JMenu menuColour = new JMenu("Colour");

	// subset menu
	JMenu menuSubset = new JMenu("Subset");
	AcActionac acaNewSubset = new AcActionac("New Subset", "Default name from label", 0, 70);
	AcActionac acaRemoveSubset = new AcActionac("Remove Subset", "Deletes all references of first selected", 0, 71);
	AcActionac acaAddCentreSubset = new AcActionac("Add Centrelines", "Add all centrelines from selected survey to subset", 0, 72);
	AcActionac acaAddRestCentreSubset = new AcActionac("Add Rest Centrelines", "Add all centrelines not already in a subset", 0, 77);
	AcActionac acaPartitionSubset = new AcActionac("Partition Remains", "Put paths into nearest subset", 0, 73);
	AcActionac acaAddToSubset = new AcActionac("Add to Subset", "Add selected paths to subset", 0, 74);
	AcActionac acaRemoveFromSubset = new AcActionac("Remove from Subset", "Remove selected paths to subset", 0, 75);
	AcActionac acaRefreshSubsets = new AcActionac("Refresh Subset", "Reallocates areas and nodes to subsets", 0, 76);
	AcActionac[] acSubsetarr = { acaNewSubset, acaRemoveSubset, acaAddCentreSubset, acaAddRestCentreSubset, acaPartitionSubset, acaAddToSubset, acaRemoveFromSubset, acaRefreshSubsets };

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


		// file menu stuff.
		miImportSketchCentreline.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ImportSketchCentreline(); } } );
		menufile.add(miImportSketchCentreline);

		miImportSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { sketchgraphicspanel.ImportSketch(mainbox.tunnelfilelist.activesketch, mainbox.tunnelfilelist.activetunnel); } } );
		menufile.add(miImportSketch);

		miCopyCentrelineElev.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { sketchgraphicspanel.CopySketchCentreline(240.0F, 0.25F); } } );
		menufile.add(miCopyCentrelineElev);


		miPrintView.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(false); } } );
		menufile.add(miPrintView);

		miPrintToScale.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(true); } } );
		menufile.add(miPrintToScale);

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
		menuColour.add(new JMenuItem(acaColourBySubset));
		menuColour.add(new JMenuItem(acaPrintProximities));
		menubar.add(menuColour);

		// subset menu stuff.
		for (int i = 0; i < acSubsetarr.length; i++)
			menuSubset.add(new JMenuItem(acSubsetarr[i]));
		menuSubset.addSeparator();
		menubar.add(menuSubset);

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
		pathcoms.add(pathselobs);

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
					sketchgraphicspanel.UpdateSAreas();
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
		//grpanel.add("South", depthslidercontrol);

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
	SkSubset NewSubset(String newname, boolean bvalidate)
	{
		if (newname.equals(""))
			return null;
		for (int i = 0; i < vsksubsets.size(); i++)
			if (newname.equals(((SkSubset)vsksubsets.elementAt(i)).name))
				return null;
		SkSubset sks = new SkSubset(newname);
		vsksubsets.addElement(sks);
		menuSubset.add(sks.miSubsetViz);
		if (bvalidate)
		{
			menuSubset.invalidate();
			System.out.println("validate");

			// make this subset active and all the rest inactive
			bPerformSkSubsetActions = false; // just to protect matters
			for (int i = 0; i < vsksubsets.size() - 1; i++)
				((SkSubset)vsksubsets.elementAt(i)).miSubsetViz.setState(false);
			sks.miSubsetViz.setState(true);
			bPerformSkSubsetActions = true;
			Updatecbmsub();
			sketchgraphicspanel.repaint();
		}
		return sks;
	}

	/////////////////////////////////////////////
	void Updatecbmsub()
	{
		sketchgraphicspanel.vssubsets.clear();

		isksubfirstactive = -1;
		for (int i = 0; i < vsksubsets.size(); i++)
		{
			SkSubset sks = (SkSubset)vsksubsets.elementAt(i);
			if (sks.miSubsetViz.getState())
			{
				TN.emitMessage("Active subset " + sks.name);
				if (isksubfirstactive == -1)
					isksubfirstactive = i;
				sketchgraphicspanel.vssubsets.addElement(sks.name);
			}
		}
		sketchgraphicspanel.tsketch.SetSubsetCode(sketchgraphicspanel.vssubsets);
		sketchgraphicspanel.RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	void RemoveSubset()
	{
		if (isksubfirstactive == -1)
			return;
		SkSubset sks = (SkSubset)vsksubsets.elementAt(isksubfirstactive);
		TN.emitMessage("Removing subset " + sks.name);
		// vsksubsets.removeElementAt(isksubfirstactive); // (done in the update)
		for (int j = 0; j < sketchgraphicspanel.tsketch.vpaths.size(); j++)
			((OnePath)sketchgraphicspanel.tsketch.vpaths.elementAt(j)).vssubsets.remove(sks.name);
		UpdateSubsets();
		sketchgraphicspanel.tsketch.bsketchfilechanged = true;
	}


	/////////////////////////////////////////////
	void AddSelCentreToCurrentSubset()
	{
		OneSketch asketch = mainbox.tunnelfilelist.activesketch;
		OneTunnel atunnel = mainbox.tunnelfilelist.activetunnel;
		if (asketch == null)
		{
			TN.emitMessage("Should have a sketch selected");
			return;
		}

		if (asketch.ExtractCentrelinePathCorrespondence(atunnel, sketchgraphicspanel.clpaths, sketchgraphicspanel.corrpaths, sketchgraphicspanel.tsketch, sketchgraphicspanel.activetunnel))
		{
			// assign the subset to each path that has correspondence.
			for (int i = 0; i < sketchgraphicspanel.corrpaths.size(); i++)
				PutSelToSubset((OnePath)sketchgraphicspanel.corrpaths.elementAt(i), true);
		}
		Updatecbmsub();
		sketchgraphicspanel.tsketch.bsketchfilechanged = true;
	}

	/////////////////////////////////////////////
	void AddRemainingCentreToCurrentSubset()
	{
		for (int i = 0; i < sketchgraphicspanel.tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && op.vssubsets.isEmpty())
				PutSelToSubset(op, true);
		}
		Updatecbmsub();
		sketchgraphicspanel.tsketch.bsketchfilechanged = true;
	}


	/////////////////////////////////////////////
	// this is the proximity graph one
	void PartitionRemainsByClosestSubset()
	{
		ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
		OnePathNode[] cennodes = new OnePathNode[pd.vcentrelinenodes.size()];
		for (int i = 0; i < sketchgraphicspanel.tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if (op.vssubsets.isEmpty())
			{
				// this could be done a lot more efficiently with a specialized version
				// that stops when it finds the first node it can use for deciding.
				OnePath cop = pd.EstClosestCenPath(op);
				if ((cop != null) && !cop.vssubsets.isEmpty())
					op.vssubsets.addElement(cop.vssubsets.elementAt(0));
			}
		}
		Updatecbmsub();
		sketchgraphicspanel.tsketch.bsketchfilechanged = true;
	}

	/////////////////////////////////////////////
	void PutSelToSubset(OnePath op, boolean bAdd)
	{
		if (isksubfirstactive == -1)
			return;
		SkSubset sks = (SkSubset)vsksubsets.elementAt(isksubfirstactive);

		// find if this path is in the subset
		int i = 0;
		for ( ; i < op.vssubsets.size(); i++)
		{
			if (sks.name == op.vssubsets.elementAt(i))
				break;
			else
				assert (!sks.name.equals((String)op.vssubsets.elementAt(i)));
		}

		// present
		if (i != op.vssubsets.size())
		{
			if (!bAdd)
				op.vssubsets.removeElementAt(i);
		}
		// absent
		else
		{
			if (bAdd)
				op.vssubsets.add(sks.name);
		}
		sketchgraphicspanel.tsketch.SetSubsetCode(op, sketchgraphicspanel.vssubsets);
	}


	/////////////////////////////////////////////
	// adds and removes from subset
	void PutSelToSubset(boolean bAdd)
	{
		// go through all the different means of selection available and push them in.
		if (sketchgraphicspanel.currgenpath != null)
			PutSelToSubset(sketchgraphicspanel.currgenpath, bAdd);
		if (sketchgraphicspanel.currselarea != null)
		{
			for (int i = 0; i < (int)sketchgraphicspanel.currselarea.refpaths.size(); i++)
				PutSelToSubset(((RefPathO)sketchgraphicspanel.currselarea.refpaths.elementAt(i)).op, bAdd);
			for (int i = 0; i < sketchgraphicspanel.currselarea.ccalist.size(); i++)
			{
				ConnectiveComponentAreas cca = (ConnectiveComponentAreas)sketchgraphicspanel.currselarea.ccalist.elementAt(i);
				for (int j = 0; j < cca.vconnpaths.size(); j++)
					PutSelToSubset(((RefPathO)cca.vconnpaths.elementAt(j)).op, bAdd);
			}
		}
		for (int i = 0; i < sketchgraphicspanel.vactivepaths.size(); i++)
		{
			Vector vp = (Vector)(sketchgraphicspanel.vactivepaths.elementAt(i));
			for (int j = 0; j < vp.size(); j++)
				PutSelToSubset((OnePath)vp.elementAt(j), bAdd);
		}


		sketchgraphicspanel.tsketch.bsketchfilechanged = true;
		sketchgraphicspanel.RedrawBackgroundView();
		sketchgraphicspanel.ClearSelection();
	}



	/////////////////////////////////////////////
	void UpdateSubsets()
	{
		// reset counters to zero
		for (int i = 0; i < vsksubsets.size(); i++)
			((SkSubset)vsksubsets.elementAt(i)).npaths = 0;

		// run twice in case of deletions
		for (int trun = 0; trun < 2; trun++)
		{
			// go through the paths, create new subsets; reallocate old ones
			for (int j = 0; j < sketchgraphicspanel.tsketch.vpaths.size(); j++)
			{
				OnePath op = (OnePath)sketchgraphicspanel.tsketch.vpaths.elementAt(j);

				// subsets path is in (backwards list so sksubcode starts right
				for (int k = 0; k < op.vssubsets.size(); k++)
				{
					// match to a known subset
					String name = (String)op.vssubsets.elementAt(k);
					SkSubset sks = null;
					for (int i = 0; i < vsksubsets.size(); i++)
					{
						SkSubset lsks = (SkSubset)vsksubsets.elementAt(i);
						if (name.equals(lsks.name))
						{
							// make all strings point to the same objects in the string list so == works as well as .equals
							sks = lsks;
							if (name != sks.name)
								op.vssubsets.setElementAt(sks.name, k);
							break;
						}
					}

					// no match.  new entry
					if (sks == null)
						sks = NewSubset(name, false);
					if (trun == 0)
						sks.npaths++;
				}
			}

			// check if any need deleting.
			if (trun == 0)
			{
				int ivsk = vsksubsets.size();
				for (int i = vsksubsets.size() - 1; i >= 0; i--)
				{
					SkSubset sks = (SkSubset)vsksubsets.elementAt(i);
					if (sks.npaths == 0)
					{
						menuSubset.remove(sks.miSubsetViz);
						vsksubsets.removeElementAt(i);
						TN.emitMessage("Removing subset checkbox " + sks.name);
					}
				}
				// no deletions; nothing to rerun.
				if (ivsk == vsksubsets.size())
					break;
			}
		}
		Updatecbmsub();
		menuSubset.invalidate();
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
		UpdateSubsets();

		// set the transform pointers to same object
		sketchgraphicspanel.backgroundimg.currparttrans = sketchgraphicspanel.tsketch.backgimgtrans;
		setTitle(activesketch.sketchname);

		// set the observed values
		pathselobs.ObserveSelection(-1, sketchgraphicspanel.tsketch.vpaths.size());



		// load in the background image.
		sketchgraphicspanel.backgroundimg.SetImageF(sketchgraphicspanel.tsketch.fbackgimg, getToolkit());
		sketchgraphicspanel.MaxAction(2); // maximize

		sketchgraphicspanel.DChangeBackNode();

		toFront();
		setVisible(true);
	}
}



