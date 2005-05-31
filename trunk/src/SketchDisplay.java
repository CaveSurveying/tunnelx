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
import javax.swing.JTabbedPane;

import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import java.awt.geom.AffineTransform;

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
	JMenuItem miCopyCentrelineElev = new JMenuItem("Copy Centreline Elev");

	JMenuItem miPrintView = new JMenuItem("Print view");
	JMenuItem miPrintDialog = new JMenuItem("Print...");
	JMenuItem miExportBitmap = new JMenuItem("Export bitmap");
	JMenuItem miPrintToJSVG = new JMenuItem("Export SVG");
	JMenuItem miPrintToPYVTK = new JMenuItem("Export PYVTK");


	JMenuItem miWriteHPGLthick = new JMenuItem("HPGL thick");
	JMenuItem miWriteHPGLthin = new JMenuItem("HPGL thin");

	JMenuItem doneitem = new JMenuItem("Close");


	// sketch line style selection
	SketchLineStyle sketchlinestyle;


	// subset info
	SketchSubsetPanel subsetpanel;
	SketchBackgroundPanel backgroundpanel;
	SketchInfoPanel infopanel;
	JTabbedPane bottabbedpane;

    SketchMakeFrame sketchmakeframe = new SketchMakeFrame();


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

			else if (viewaction == 21)
				backgroundpanel.SetGridOrigin(true);
			else if (viewaction == 22)
				backgroundpanel.SetGridOrigin(false);

			// 1, 2, 3, 11, 12
			else
				sketchgraphicspanel.MaxAction(viewaction);
        }
	}

	// would like to use VK_RIGHT instead of VK_F12, but is not detected.
	AcViewac acvMax = new AcViewac("Max", "Maximize View", KeyEvent.VK_M, 2);
	AcViewac acvCentre = new AcViewac("Centre", "Centre View", 0, 1);
	AcViewac acvMaxSubset = new AcViewac("Max Subset", "Maximize Subset View", 0, 12);
	AcViewac acvCentreSubset = new AcViewac("Centre Subset", "Centre Subset View", 0, 11);
	AcViewac acvUpright = new AcViewac("Upright", "Upright View", 0, 3);
	AcViewac acvScaledown = new AcViewac("Scale Down", "Zoom out", KeyEvent.VK_MINUS, 4);
	AcViewac acvScaleup = new AcViewac("Scale Up", "Zoom in", KeyEvent.VK_PLUS, 5);
	AcViewac acvRight = new AcViewac("Right", "Translate view right", KeyEvent.VK_F12, 6);
	AcViewac acvLeft = new AcViewac("Left", "Translate view left", KeyEvent.VK_F9, 7);
	AcViewac acvUp = new AcViewac("Up", "Translate view up", KeyEvent.VK_F10, 8);
	AcViewac acvDown = new AcViewac("Down", "Translate view down", KeyEvent.VK_F11, 9);
	AcViewac acvSetGridOrig = new AcViewac("Set Grid Orig", "Move the grid origin to the start node of selected line", 0, 21);
	AcViewac acvResetGridOrig = new AcViewac("Reset Grid Orig", "Move the grid origin to original place", 0, 22);
	AcViewac acvRedraw = new AcViewac("Redraw", "Redraw screen", 0, 10);

	// view menu
	JMenu menuView = new JMenu("View");
	AcViewac[] acViewarr = { acvMax, acvMaxSubset, acvCentre, acvCentreSubset, acvUpright, acvScaledown, acvScaleup, acvRight, acvLeft, acvUp, acvDown, acvSetGridOrig, acvResetGridOrig, acvRedraw };



	/////////////////////////////////////////////
	// Display menu actions
	/////////////////////////////////////////////
	public class AcDispchbox extends AbstractAction
	{
		int backrepaint;
        public AcDispchbox(String name, String shdesc, int lbackrepaint)
		{
            super(name);
            putValue(SHORT_DESCRIPTION, shdesc);
			backrepaint = lbackrepaint;
        }
        public void actionPerformed(ActionEvent e)
		{
			if (backrepaint == 0)
				sketchgraphicspanel.RedrawBackgroundView();
			else
			{
				if (backrepaint == 2)
					sketchgraphicspanel.tsketch.SetSubsetVisibleCodeStrings(sketchgraphicspanel.vsselectedsubsets, miInverseSubset.isSelected());
				sketchgraphicspanel.RedoBackgroundView();
			}
		}
	}

	AcDispchbox acdCentreline = new AcDispchbox("Centreline", "Centreline visible", 0);
	AcDispchbox acdStationNames = new AcDispchbox("Station Names", "Station names visible", 0);
	AcDispchbox acdStationAlts = new AcDispchbox("Station Altitudes", "Station altitudes visible", 0);
	AcDispchbox acdXSections = new AcDispchbox("XSections", "Cross sections visible", 0);
	AcDispchbox acdTubes = new AcDispchbox("Tubes", "Tubes visible", 0);
	AcDispchbox acdAxes = new AcDispchbox("Axes", "Axes visible", 0);
	AcDispchbox acdDepthCols = new AcDispchbox("Depth Colours", "Depth colours visible", 0);
	AcDispchbox acdShowNodes = new AcDispchbox("Show Nodes", "Path nodes visible", 0);
	AcDispchbox acdShowBackground = new AcDispchbox("Show Background", "Background image visible", 1);
	AcDispchbox acdShowGrid = new AcDispchbox("Show Grid", "Background grid visible", 1);
	AcDispchbox acdTransitiveSubset = new AcDispchbox("Transitive Subset", "View selected subsets and branches", 2);
	AcDispchbox acdInverseSubset = new AcDispchbox("Inverse Subset", "Grey out the selected subsets", 2);

	JCheckBoxMenuItem miCentreline = new JCheckBoxMenuItem(acdCentreline);
	JCheckBoxMenuItem miStationNames = new JCheckBoxMenuItem(acdStationNames);
	JCheckBoxMenuItem miStationAlts = new JCheckBoxMenuItem(acdStationAlts);
	JCheckBoxMenuItem miDepthCols = new JCheckBoxMenuItem(acdDepthCols);
	JCheckBoxMenuItem miShowNodes = new JCheckBoxMenuItem(acdShowNodes);
	JCheckBoxMenuItem miShowBackground = new JCheckBoxMenuItem(acdShowBackground);
	JCheckBoxMenuItem miShowGrid = new JCheckBoxMenuItem(acdShowGrid);
	JCheckBoxMenuItem miTransitiveSubset = new JCheckBoxMenuItem(acdTransitiveSubset);
	JCheckBoxMenuItem miInverseSubset = new JCheckBoxMenuItem(acdInverseSubset);


	// display menu.
	JMenu menuDisplay = new JMenu("Display");
	JCheckBoxMenuItem[] miDisplayarr = { miCentreline, miStationNames, miStationAlts, miShowNodes, miDepthCols, miShowBackground, miShowGrid, miTransitiveSubset, miInverseSubset };


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

		/////////////////////////////////////////////
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
			else if (acaction == 10)
				sketchgraphicspanel.MakePitchUndercut();
			else if ((acaction == 11) || (acaction == 12))
			{
				SketchLineStyle.SetStrokeWidths(SketchLineStyle.strokew * (acaction == 11 ? 2.0F : 0.5F));
				sketchgraphicspanel.RedrawBackgroundView();
			}
			else if (acaction == 13)
				sketchgraphicspanel.TranslateConnectedSet();

			else if (acaction == 14)
				sketchgraphicspanel.MoveGround(false);
			else if (acaction == 15)
				sketchgraphicspanel.MoveGround(true);

			else if (acaction == 20)
				sketchgraphicspanel.SetIColsDefault();
			else if (acaction == 21)
				sketchgraphicspanel.SetIColsByZ(true);
			else if (acaction == 22)
				sketchgraphicspanel.SetIColsProximity(0);
			else if (acaction == 23)
				sketchgraphicspanel.SetIColsProximity(1);

			// the automatic actions which should be running constantly in a separate thread
			else if ((acaction == 51) || (acaction == 58))
			{
				// heavyweight stuff
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch, false);
				pd.SetZaltsFromCNodesByInverseSquareWeight(sketchgraphicspanel.tsketch); // passed in for the zaltlo/hi values
				sketchgraphicspanel.SketchChanged(1);

				// do everything
				if (acaction == 58)
				{
					sketchgraphicspanel.UpdateSAreas();
					sketchgraphicspanel.UpdateSymbolLayout(true);
					sketchgraphicspanel.bNextRenderDetailed = true;
				}
			}
			else if (acaction == 52)
				sketchgraphicspanel.UpdateSAreas();
			else if ((acaction == 53) || (acaction == 54))
				sketchgraphicspanel.UpdateSymbolLayout(acaction == 54);
			else if (acaction == 56) // detail render
				sketchgraphicspanel.bNextRenderDetailed = true;

			else if (acaction == 57) // printing proximities to the command line
			{
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch, true);
				pd.PrintCNodeProximity(3);
			}

			// subsets
			else if (acaction == 72)
				subsetpanel.AddSelCentreToCurrentSubset();
			else if (acaction == 77)
				subsetpanel.AddRemainingCentreToCurrentSubset();
			else if (acaction == 73)
				subsetpanel.PartitionRemainsByClosestSubset();
			else if (acaction == 74)
				subsetpanel.PutSelToSubset(true);
			else if (acaction == 75)
				subsetpanel.PutSelToSubset(false);
			else if (acaction == 76)
				subsetpanel.pansksubsetstree.clearSelection();

			// these ones don't actually need the repaint
			else if (acaction == 80)
				sketchlinestyle.SetConnTabPane("Symbol");
			else if (acaction == 81)
				sketchlinestyle.SetConnTabPane("Label");
			else if (acaction == 82)
				sketchlinestyle.SetConnTabPane("Area-Sig");


			else if (acaction == 91)
    			sketchgraphicspanel.bNextRenderPinkDownSketch = true;
			else if (acaction == 92)
    			sketchgraphicspanel.bNextRenderPFrame = true;
			else if (acaction == 93)
    			sketchgraphicspanel.bNextRenderAreaStripes = true;

			else if (acaction == 95)
				sketchgraphicspanel.ImportSketch(mainbox.tunnelfilelist.activesketch, mainbox.tunnelfilelist.activetunnel);
			else if (acaction == 96)
				sketchgraphicspanel.ImportFrameSketch();
			else if ((acaction == 97) || (acaction == 89))
				{ sketchgraphicspanel.ImportSketchCentreline(acaction == 89 ? 1 : 0);  sketchgraphicspanel.MaxAction(2); }
			else if (acaction == 98)
				sketchgraphicspanel.CopySketchCentreline(32.0F, 0.25F);



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
	AcActionac acaPitchUndercut = new AcActionac("Pitch Undercut", "Drop-down an invisible copy of a pitch boundary", 0, 10);

	AcActionac acaStrokeThin = new AcActionac("Stroke >>", "Thicker lines", KeyEvent.VK_GREATER, 11);
	AcActionac acaStrokeThick = new AcActionac("Stroke <<", "Thinner lines", KeyEvent.VK_LESS, 12);

	AcActionac acaMovePicture = new AcActionac("Shift View", "Moves view by according to path", 0, 14);
	AcActionac acaMoveBackground = new AcActionac("Shift Ground", "Moves background image by according to path", 0, 15);

	AcActionac acaFuseTranslateComponent = new AcActionac("Fuse Translate", "Translates Connected Component", 0, 13);

	// connective type specifiers
	AcActionac acaConntypesymbols = new AcActionac("Add symbols", "Put symbols on connective path", 0, 80);
	AcActionac acaConntypelabel = new AcActionac("Write Text", "Put label on connective path", 0, 81);
	AcActionac acaConntypearea = new AcActionac("Area signal", "Put area signal on connective path", 0, 82);

	JMenu menuAction = new JMenu("Action");
	AcActionac[] acActionarr = { acaDeselect, acaDelete, acaFuse, acaBackNode, acaReflect, acaPitchUndercut, acaStrokeThin, acaStrokeThick, acaSetasaxis, acaMovePicture, acaMoveBackground, acaFuseTranslateComponent, acaConntypesymbols, acaConntypelabel, acaConntypearea };

	// auto menu
	AcActionac acaSetZonnodes = new AcActionac("Update Node Z", "Set node heights from centreline", 0, 51);
	AcActionac acaUpdateSAreas = new AcActionac("Update Areas", "Update automatic areas", 0, 52);
	AcActionac acaUpdateSymbolLayout = new AcActionac("Update Symbol Lay", "Update symbol layout in view", 0, 53);
	AcActionac acaUpdateSymbolLayoutAll = new AcActionac("Update Symbol Lay All", "Update symbol layout Everywhere", 0, 54);
	AcActionac acaDetailRender = new AcActionac("Detail Render", "Detail Render", 0, 56);
	AcActionac acaUpdateEverything = new AcActionac("Update Everything", "All updates in a row", 0, 58);

	JMenu menuAuto = new JMenu("Update");
	AcActionac[] acAutoarr = { acaSetZonnodes, acaUpdateSAreas, acaUpdateSymbolLayout, acaUpdateSymbolLayoutAll, acaDetailRender, acaUpdateEverything };

	// import menu
	AcActionac acaPrevDownsketch = new AcActionac("Preview Down Sketch", "See the sketch that will be distorted", 0, 91);
	AcActionac acaPrevFrame = new AcActionac("Preview Frame", "See the printable frame based on the selected path", 0, 92);
	AcActionac acaStripeAreas = new AcActionac("Stripe Areas", "See the areas filled with stripes", 0, 93);
	AcActionac acaImportCentreline = new AcActionac("Import Centreline", "Bring in the centreline for this survey", 0, 97);
	AcActionac acaImportCentrelineXC = new AcActionac("Import Centreline XC", "Bring in the centreline for this survey with crosssections", 0, 89);
	AcActionac acaImportDownSketch = new AcActionac("Import Down Sketch", "Bring in the distorted sketch", 0, 95);
	AcActionac acaImportFrame = new AcActionac("Import Frame", "Bring in the printable frame", 0, 96);
	AcActionac acaCopyCentrelineElev = new AcActionac("Copy Centreline Elev", "The little elevation thing", 0, 98);

	JMenu menuImport = new JMenu("Import");
	AcActionac[] acImportarr = { acaPrevDownsketch, acaPrevFrame, acaStripeAreas, acaImportCentreline, acaImportCentrelineXC, acaImportDownSketch, acaImportFrame, acaCopyCentrelineElev };

	// colour menu
	AcActionac acaColourDefault = new AcActionac("Default", "Plain colours", 0, 20);
	AcActionac acaColourByZ = new AcActionac("Height", "Depth colours", 0, 21);
	AcActionac acaColourByProx = new AcActionac("Proximity", "Visualize proximity to selection", 0, 22);
	AcActionac acaColourByCnodeWeight = new AcActionac("CNode Weights", "Visualize centreline node weights", 0, 23);
	AcActionac acaPrintProximities = new AcActionac("Print Prox", "Print proximities of nodes to centrelines", 0, 57);

	JMenu menuColour = new JMenu("Colour");

	// subset menu
	JMenu menuSubset = new JMenu("Subset");
	AcActionac acaAddCentreSubset = new AcActionac("Add Centrelines", "Add all centrelines from selected survey to subset", 0, 72);
	AcActionac acaAddRestCentreSubset = new AcActionac("Add Rest Centrelines", "Add all centrelines not already in a subset", 0, 77);
	AcActionac acaPartitionSubset = new AcActionac("Partition Remains", "Put paths into nearest subset", 0, 73);
	AcActionac acaAddToSubset = new AcActionac("Add to Subset", "Add selected paths to subset", 0, 74);
	AcActionac acaRemoveFromSubset = new AcActionac("Remove from Subset", "Remove selected paths to subset", 0, 75);
	AcActionac acaCleartreeSelection = new AcActionac("Clear subset selection", "Clear selections on subset tree", 0, 76);
	AcActionac[] acSubsetarr = { acaAddCentreSubset, acaAddRestCentreSubset, acaPartitionSubset, acaAddToSubset, acaRemoveFromSubset, acaCleartreeSelection };



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

		// sketch line style selection
		sketchlinestyle = new SketchLineStyle(symbolsdisplay, this);

		// file menu stuff.
		miPrintView.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(0); } } );
		menufile.add(miPrintView);

		miPrintToPYVTK.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(2);; } } );
		menufile.add(miPrintToPYVTK);

		miPrintToJSVG.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(3);; } } );
		menufile.add(miPrintToJSVG);

		miPrintDialog.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(4); } } );
		menufile.add(miPrintDialog);

		miExportBitmap.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.PrintThis(5); } } );
		menufile.add(miExportBitmap);

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
			boolean binitialstate = !((miDisplayarr[i] == miShowBackground) || (miDisplayarr[i] == miStationNames) || (miDisplayarr[i] == miStationAlts) || (miDisplayarr[i] == miTransitiveSubset) || (miDisplayarr[i] == miInverseSubset));
			miDisplayarr[i].setState(binitialstate);
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

		// import menu
		for (int i = 0; i < acImportarr.length; i++)
			menuImport.add(new JMenuItem(acImportarr[i]));
		menubar.add(menuImport);

		// colour menu stuff.
		menuColour.add(new JMenuItem(acaColourDefault));
		menuColour.add(new JMenuItem(acaColourByZ));
		menuColour.add(new JMenuItem(acaColourByProx));
		menuColour.add(new JMenuItem(acaColourByCnodeWeight));
		menuColour.add(new JMenuItem(acaPrintProximities));
		menubar.add(menuColour);

		// subset menu stuff.
		for (int i = 0; i < acSubsetarr.length; i++)
			menuSubset.add(new JMenuItem(acSubsetarr[i]));
		menubar.add(menuSubset);

		// menu bar is complete.
		setJMenuBar(menubar);


		// the panel of useful buttons that're part of the non-connective type display
		JPanel pnonconn = new JPanel(new GridLayout(0, 2));
		pnonconn.add(new JButton(acaStrokeThin));
		pnonconn.add(new JButton(acaStrokeThick));
		pnonconn.add(new JLabel());
		pnonconn.add(new JLabel());
		pnonconn.add(new JButton(acaSetZonnodes));
		pnonconn.add(new JButton(acaUpdateSAreas));
		pnonconn.add(new JButton(acaUpdateSymbolLayout));
		pnonconn.add(new JButton(acaDetailRender));
		pnonconn.add(new JLabel());
		pnonconn.add(new JLabel());

		pnonconn.add(new JLabel());
		pnonconn.add(new JLabel());
		pnonconn.add(new JLabel("Connective subtypes"));
		pnonconn.add(new JButton(acaConntypesymbols));
		pnonconn.add(new JButton(acaConntypelabel));
		pnonconn.add(new JButton(acaConntypearea));
		SetEnabledConnectiveSubtype(false);

		// we build one of the old tabbing panes into the bottom and have it
		sketchlinestyle.pthstylenonconn.setLayout(new BorderLayout());
		sketchlinestyle.pthstylenonconn.add("Center", pnonconn);

		// put in the deselect and delete below the row of style buttons
		sketchlinestyle.pathcoms.add(new JButton(acaReflect));
		sketchlinestyle.pathcoms.add(new JButton(acaFuse));
		sketchlinestyle.pathcoms.add(new JButton(acaBackNode));
		sketchlinestyle.pathcoms.add(new JButton(acaDelete));


		subsetpanel = new SketchSubsetPanel(this);
        backgroundpanel = new SketchBackgroundPanel(this);
        infopanel = new SketchInfoPanel(this);

		// do the tabbed pane of extra buttons and fields in the side panel.
		bottabbedpane = new JTabbedPane();
		bottabbedpane.add("subsets", subsetpanel);
		bottabbedpane.add("background", backgroundpanel);
		bottabbedpane.add("info", infopanel);



		// the full side panel
		JPanel sidepanel = new JPanel(new BorderLayout());
		sidepanel.add("Center", sketchlinestyle);
		sidepanel.add("South", bottabbedpane);

		JPanel grpanel = new JPanel(new BorderLayout());
		grpanel.add("Center", sketchgraphicspanel);

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


	// switched on and off if we have a connective line selected
	void SetEnabledConnectiveSubtype(boolean benabled)
	{
		acaConntypesymbols.setEnabled(benabled);
		acaConntypelabel.setEnabled(benabled);
		acaConntypearea.setEnabled(benabled);
	}



	/////////////////////////////////////////////
	void ShowBackgroundImage(int libackgroundimgnamearrsel)
	{
System.out.println("showback image " + libackgroundimgnamearrsel + "  " + sketchgraphicspanel.tsketch.backgroundimgnamearr.size());
		File idir = sketchgraphicspanel.tsketch.sketchfile.getParentFile();
		String iname = (String)sketchgraphicspanel.tsketch.backgroundimgnamearr.elementAt(libackgroundimgnamearrsel);

		if (sketchgraphicspanel.tsketch.backgimgtransarr.elementAt(libackgroundimgnamearrsel) == null)
		{
			sketchgraphicspanel.tsketch.backgimgtransarr.setElementAt(new AffineTransform(), libackgroundimgnamearrsel);
			sketchgraphicspanel.backgroundimg.bMaxBackImage = true;
		}

		// set object pointer over
		sketchgraphicspanel.backgroundimg.currparttrans = (AffineTransform)sketchgraphicspanel.tsketch.backgimgtransarr.elementAt(libackgroundimgnamearrsel);

		sketchgraphicspanel.backgroundimg.SetImageF(SketchBackgroundPanel.GetImageFile(idir, iname));
	}


	/////////////////////////////////////////////
	void ActivateSketchDisplay(OneTunnel activetunnel, OneSketch activesketch, boolean lbEditable)
	{
		sketchgraphicspanel.bEditable = lbEditable;
		sketchgraphicspanel.Deselect(true);

		sketchgraphicspanel.tsketch = activesketch;
		sketchgraphicspanel.activetunnel = activetunnel;
		sketchgraphicspanel.asketchavglast = null; // used for lazy evaluation of the average transform.
		subsetpanel.ListMissingSubsets();

		// set up the background image dropdown box
		backgroundpanel.jcbbackground.removeAllItems();
		for (int i = 0; i < activesketch.backgroundimgnamearr.size(); i++)
			backgroundpanel.jcbbackground.addItem(activesketch.backgroundimgnamearr.elementAt(i));
		backgroundpanel.jcbbackground.setSelectedIndex(activesketch.ibackgroundimgnamearrsel);
System.out.println("Selecting background image " + activesketch.ibackgroundimgnamearrsel + " from " + activesketch.backgroundimgnamearr.size());

		// set greyness
		acaUpdateSAreas.setEnabled(!sketchgraphicspanel.tsketch.bSAreasUpdated);
		acaUpdateSymbolLayout.setEnabled(!sketchgraphicspanel.tsketch.bSymbolLayoutUpdated);


		// set the transform pointers to same object
		setTitle(activesketch.sketchname);
		sketchgraphicspanel.MaxAction(2); // maximize
		sketchgraphicspanel.DChangeBackNode();

		if ((subsetpanel.jcbsubsetstyles.getSelectedIndex() == -1) && (subsetpanel.jcbsubsetstyles.getItemCount() != 0))
			subsetpanel.jcbsubsetstyles.setSelectedIndex(0);
		else
			subsetpanel.UpdateTreeSubsetSelection(true);

		toFront();
		setVisible(true);
	}
}



