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

import java.awt.FileDialog;

import java.awt.Image;
import java.awt.Insets;
import java.awt.Dimension;

import java.io.IOException;

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

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.geom.AffineTransform;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JProgressBar;

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
    JSlider zdepthzlider = new JSlider(0, 100, 100); 

	// the window with the symbols
	SymbolsDisplay symbolsdisplay;

	// the menu bar
	JMenuBar menubar = new JMenuBar();

	// file menu
	JMenu menufile = new JMenu("File");
	JMenuItem miCopyCentrelineElev = new JMenuItem("Copy Centreline Elev");

	JMenuItem miPrintToPYVTK = new JMenuItem("Export PYVTK");
	JMenuItem mi3Dpassageview = new JMenuItem("3D Passage View");

	JMenuItem miSaveSketch = new JMenuItem("Save");
	JMenuItem miSaveSketchAs = new JMenuItem("Save As...");
    JMenuItem miUploadSketch = new JMenuItem("Upload"); 

	JMenuItem doneitem = new JMenuItem("Close");

	SketchLineStyle sketchlinestyle;

	SketchSubsetPanel subsetpanel;
	SelectedSubsetStructure selectedsubsetstruct; 

	SketchBackgroundPanel backgroundpanel;
	SketchInfoPanel infopanel;
	SketchPrintPanel printingpanel;
	SketchSecondRender secondrender;
	
	JTabbedPane bottabbedpane;

    PassageFloor3D passagefloor3D = null; 

	/////////////////////////////////////////////
	// inactivate case
	class SketchHide extends WindowAdapter implements ActionListener
	{
		void CloseWindow()
		{
			//mainbox.symbolsdisplay.hide();
			sketchgraphicspanel.ClearSelection(true);
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
	AcViewac acvMax =          new AcViewac("Max",             "Maximize View", 0, 2);
	AcViewac acvCentre =       new AcViewac("Centre",          "Centre View", 0, 1);
	AcViewac acvMaxSubset =    new AcViewac("Max Subset",      "Maximize Subset View", KeyEvent.VK_M, 12);
	AcViewac acvCentreSubset = new AcViewac("Centre Subset",   "Centre Subset View", 0, 11);
	AcViewac acvUpright =      new AcViewac("Upright",         "Upright View", 0, 3);
	AcViewac acvScaledown =    new AcViewac("Scale Down",      "Zoom out", KeyEvent.VK_MINUS, 4);
	AcViewac acvScaleup =      new AcViewac("Scale Up",        "Zoom in", KeyEvent.VK_PLUS, 5);
	AcViewac acvRight =        new AcViewac("Right",           "Translate view right", KeyEvent.VK_F12, 6);
	AcViewac acvLeft =         new AcViewac("Left",            "Translate view left", KeyEvent.VK_F9, 7);
	AcViewac acvUp =           new AcViewac("Up",              "Translate view up", KeyEvent.VK_F10, 8);
	AcViewac acvDown =         new AcViewac("Down",            "Translate view down", KeyEvent.VK_F11, 9);
	AcViewac acvSetGridOrig =  new AcViewac("Set Grid Orig",   "Move the grid origin to the start node of selected line", 0, 21);
	AcViewac acvResetGridOrig =new AcViewac("Reset Grid Orig", "Move the grid origin to original place", 0, 22);
	AcViewac acvRedraw =       new AcViewac("Redraw",          "Redraw screen", 0, 10);

	// view menu
	JMenu menuView = new JMenu("View");
	AcViewac[] acViewarr = { acvMaxSubset, acvMax, acvCentre, acvCentreSubset, acvUpright, acvScaledown, acvScaleup, acvRight, acvLeft, acvUp, acvDown, acvSetGridOrig, acvResetGridOrig, acvRedraw };



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
					selectedsubsetstruct.UpdateTreeSubsetSelection(subsetpanel.pansksubsetstree); 
				sketchgraphicspanel.RedoBackgroundView();
			}
		}
	}

	AcDispchbox acdCentreline =        new AcDispchbox("Centreline", "Centreline visible", 0);
	AcDispchbox acdStationNames =      new AcDispchbox("Station Names", "Station names visible", 0);
	AcDispchbox acdStationAlts =       new AcDispchbox("Station Altitudes", "Station altitudes visible", 0);
	AcDispchbox acdXSections =         new AcDispchbox("XSections", "Cross sections visible", 0);
	AcDispchbox acdTubes =             new AcDispchbox("Tubes", "Tubes visible", 0);
	AcDispchbox acdAxes =              new AcDispchbox("Axes", "Axes visible", 0);
	AcDispchbox acdDepthCols =         new AcDispchbox("Depth Colours", "Depth colours visible", 0);
	AcDispchbox acdShowNodes =         new AcDispchbox("Show Nodes", "Path nodes visible", 0);
	AcDispchbox acdShowBackground =    new AcDispchbox("Show Background", "Background image visible", 1);
	AcDispchbox acdShowGrid =          new AcDispchbox("Show Grid", "Background grid visible", 1);
	AcDispchbox acdTransitiveSubset =  new AcDispchbox("Transitive Subset", "View selected subsets and branches", 2);
	AcDispchbox acdInverseSubset =     new AcDispchbox("Inverse Subset", "Grey out the selected subsets", 2);
	AcDispchbox acdHideSplines =       new AcDispchbox("Hide Splines", "Show all paths as non-splined", 1);

	JCheckBoxMenuItem miCentreline =       new JCheckBoxMenuItem(acdCentreline);
	JCheckBoxMenuItem miStationNames =     new JCheckBoxMenuItem(acdStationNames);
	JCheckBoxMenuItem miStationAlts =      new JCheckBoxMenuItem(acdStationAlts);
	JCheckBoxMenuItem miDepthCols =        new JCheckBoxMenuItem(acdDepthCols);
	JCheckBoxMenuItem miShowNodes =        new JCheckBoxMenuItem(acdShowNodes);
	JCheckBoxMenuItem miShowBackground =   new JCheckBoxMenuItem(acdShowBackground);
	JCheckBoxMenuItem miShowGrid =         new JCheckBoxMenuItem(acdShowGrid);
	JCheckBoxMenuItem miTransitiveSubset = new JCheckBoxMenuItem(acdTransitiveSubset);
	JCheckBoxMenuItem miInverseSubset =    new JCheckBoxMenuItem(acdInverseSubset);
	JCheckBoxMenuItem miHideSplines =      new JCheckBoxMenuItem(acdHideSplines);
	JCheckBoxMenuItem miThinZheightsel =   new JCheckBoxMenuItem("Thin Z Selection", false);
	JMenuItem miThinZheightselWiden =      new JMenuItem("Widen Z Selection");
	JMenuItem miThinZheightselNarrow =     new JMenuItem("Narrow Z Selection");

	// display menu.
	JMenu menuDisplay = new JMenu("Display");
	JCheckBoxMenuItem[] miDisplayarr = { miCentreline, miStationNames, miStationAlts, miShowNodes, miDepthCols, miShowBackground, miShowGrid, miTransitiveSubset, miInverseSubset, miHideSplines };


	/////////////////////////////////////////////
	// Motion menu
	JCheckBoxMenuItem miTabletMouse =      new JCheckBoxMenuItem("Tablet Mouse",       false);
	JCheckBoxMenuItem miEnableRotate =     new JCheckBoxMenuItem("Enable rotate",      false);
	JCheckBoxMenuItem miTrackLines =       new JCheckBoxMenuItem("Track Lines",        false);
	JCheckBoxMenuItem miShearWarp =        new JCheckBoxMenuItem("Shear Warp",         false);
	JCheckBoxMenuItem miDefaultSplines =   new JCheckBoxMenuItem("Splines Default",    true);
	JCheckBoxMenuItem miSnapToGrid =       new JCheckBoxMenuItem("Snap to Grid",       false);
	JCheckBoxMenuItem miEnableDoubleClick =new JCheckBoxMenuItem("Enable double-click",true);

	JMenu menuMotion = new JMenu("Motion");
	JCheckBoxMenuItem[] miMotionarr = { miTabletMouse, miEnableRotate, miTrackLines, miShearWarp, miDefaultSplines, miSnapToGrid, miEnableDoubleClick };

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
				sketchgraphicspanel.ClearSelection(false);
			else if (acaction == 5)
				sketchgraphicspanel.DeleteSel();
			else if (acaction == 6)
				sketchgraphicspanel.FuseCurrent(miShearWarp.isSelected());
			else if (acaction == 7)
				sketchgraphicspanel.BackSelUndo();
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
			else if (acaction == 18)
				sketchgraphicspanel.SelectConnectedSetsFromSelection(); 
			else if (acaction == 14)
				sketchgraphicspanel.MoveGround(false);
			else if (acaction == 15)
				sketchgraphicspanel.MoveGround(true);
			else if (acaction == 16)
				backgroundpanel.NewBackgroundFile();
			else if (acaction == 177)
				backgroundpanel.UploadBackgroundFile();
			else if (acaction == 17)
				sketchlinestyle.pthstyleareasigtab.StyleMappingCopyButt(true); 

			else if (acaction == 20)
				{ SketchLineStyle.bDepthColours = false;  sketchgraphicspanel.RedrawBackgroundView();  }
			else if (acaction == 21)
				{ SketchLineStyle.SetIColsByZ(sketchgraphicspanel.tsvpathsviz, sketchgraphicspanel.tsketch.vnodes, sketchgraphicspanel.tsketch.vsareas);  sketchgraphicspanel.RedrawBackgroundView();  }
			else if (acaction == 22)
				{ OnePathNode ops = (sketchgraphicspanel.currpathnode != null ? sketchgraphicspanel.currpathnode : (sketchgraphicspanel.currgenpath != null ? sketchgraphicspanel.currgenpath.pnstart : null)); 
				  SketchLineStyle.SetIColsProximity(0, sketchgraphicspanel.tsketch, ops);  sketchgraphicspanel.RedrawBackgroundView();  }
			else if (acaction == 23)
				{ OnePathNode ops = (sketchgraphicspanel.currpathnode != null ? sketchgraphicspanel.currpathnode : (sketchgraphicspanel.currgenpath != null ? sketchgraphicspanel.currgenpath.pnstart : null)); 
				  sketchlinestyle.SetIColsProximity(1, sketchgraphicspanel.tsketch, ops);  sketchgraphicspanel.RedrawBackgroundView();  }

			// the automatic actions which should be running constantly in a separate thread
			else if ((acaction == 51) || (acaction == 58))
			{
				sketchgraphicspanel.UpdateZNodes();

				// do everything
				if (acaction == 58)
				{
					sketchgraphicspanel.UpdateSAreas();
					sketchgraphicspanel.UpdateSymbolLayout(true, visiprogressbar);
					sketchgraphicspanel.bNextRenderDetailed = true;
				}
			}
			else if (acaction == 52)
				sketchgraphicspanel.UpdateSAreas();
			else if ((acaction == 53) || (acaction == 54))
				sketchgraphicspanel.UpdateSymbolLayout(acaction == 54, visiprogressbar);
			else if (acaction == 56) // detail render
				sketchgraphicspanel.bNextRenderDetailed = true;

			else if (acaction == 57) // printing proximities to the command line
			{
				ProximityDerivation pd = new ProximityDerivation(sketchgraphicspanel.tsketch);
				pd.PrintCNodeProximity(3); // uses default settings in the pd.parainstancequeue
			}
			else if (acaction == 59)
				ReloadFontcolours();

			// subsets
			else if (acaction == 72)
				subsetpanel.AddSelCentreToCurrentSubset();
			else if (acaction == 77)
				subsetpanel.AddRemainingCentreToCurrentSubset();
			else if (acaction == 73)
				subsetpanel.PartitionRemainsByClosestSubset();
			else if (acaction == 733)
				subsetpanel.PartitionRemainsByClosestSubsetDatetype();
			else if (acaction == 74)
				subsetpanel.PutSelToSubset(true);
			else if (acaction == 75)
				subsetpanel.PutSelToSubset(false);
			else if (acaction == 76)
				subsetpanel.pansksubsetstree.clearSelection();
			else if (acaction == 78)
				subsetpanel.DeleteTodeleteSubset();
			else if (acaction == 79)
				subsetpanel.RemoveAllFromSubset();


			else if (acaction == 71)
				subsetpanel.ElevationSubset(true);
			else if (acaction == 711)
				subsetpanel.ElevationSubset(false);
			else if (acaction == 70)
				subsetpanel.sascurrent.ToggleViewHidden(selectedsubsetstruct.vsselectedsubsets, miTransitiveSubset.isSelected()); 

			// these ones don't actually need the repaint
			else if (acaction == 80)
				sketchlinestyle.SetConnTabPane("Symbol");
			else if (acaction == 81)
				sketchlinestyle.SetConnTabPane("Label");
			else if (acaction == 82)
				sketchlinestyle.SetConnTabPane("Area-Sig");

			else if (acaction == 91)
    			sketchgraphicspanel.bNextRenderPinkDownSketch = true;
			else if (acaction == 93)
    			sketchgraphicspanel.bNextRenderAreaStripes = true;

			else if (acaction == 95)
				sketchgraphicspanel.ImportSketch(mainbox.tunnelfilelist.GetSelectedSketchLoad(), miImportCentreSubsetsU.isSelected(), miClearCentreSubsets.isSelected(), miImportNoCentrelines.isSelected());

			// paper sizes
			else if (acaction == 404)
				sketchgraphicspanel.ImportPaperM("A4", 0.210F, 0.297F);
			else if (acaction == 414)
				sketchgraphicspanel.ImportPaperM("A4_land", 0.297F, 0.210F);
			else if (acaction == 403)
				sketchgraphicspanel.ImportPaperM("A3", 0.297F, 0.420F);
			else if (acaction == 413)
				sketchgraphicspanel.ImportPaperM("A3_land", 0.420F, 0.297F);
			else if (acaction == 402)
				sketchgraphicspanel.ImportPaperM("A2", 0.420F, 0.594F);
			else if (acaction == 401)
				sketchgraphicspanel.ImportPaperM("A1", 0.594F, 0.840F);
			else if (acaction == 411)
				sketchgraphicspanel.ImportPaperM("A1_land", 0.840F, 0.594F);
			else if (acaction == 400)
				sketchgraphicspanel.ImportPaperM("A0", 0.840F, 1.188F);
			// new survex label controls interface
			else if (acaction == 501)
				ImportSketchCentrelineFile(null); 
			else if (acaction == 502)
				ImportAtlasTemplate(); 
			else if (acaction == 510)
				ImportCentrelineLabel(true, miUseSurvex.isSelected()); 
			else if (acaction == 511)
				ImportCentrelineLabel(false, miUseSurvex.isSelected()); 

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

	AcActionac acaAddImage = new AcActionac("Add Image", "Adds a new background image to the sketch", 0, 16);
	AcActionac acaReloadImage = new AcActionac("Select Image", "Copies this background image to background of the sketch", 0, 17);
	
    // could grey this one too
    AcActionac acaUploadImage = new AcActionac("Upload Back Image", "Uploads this background image to the server", 0, 177);

	AcActionac acaSelectComponent = new AcActionac("Component", "Selects Connected Component for selected edge", 0, 18);
	JCheckBoxMenuItem miDeleteCentrelines = new JCheckBoxMenuItem("Allow Delete Centrelines", false);

	// connective type specifiers
	AcActionac acaConntypesymbols = new AcActionac("Add symbols", "Put symbols on connective path", 0, 80);
	AcActionac acaConntypelabel = new AcActionac("Write Text", "Put label on connective path", 0, 81);
	AcActionac acaConntypearea = new AcActionac("Area signal", "Put area signal on connective path", 0, 82);

	JMenu menuAction = new JMenu("Action");
	AcActionac[] acActionarr = { acaDeselect, acaDelete, acaFuse, acaBackNode, acaReflect, acaPitchUndercut, acaStrokeThin, acaStrokeThick, acaSetasaxis, acaMovePicture, acaMoveBackground, acaAddImage, acaUploadImage, acaSelectComponent, acaConntypesymbols, acaConntypelabel, acaConntypearea };
	AcActionac[] acPathcomarr = { acaReflect, acaFuse, acaSelectComponent, acaBackNode, acaDelete };

	// auto menu
	AcActionac acaSetZonnodes = new AcActionac("Update Node Z", "Set node heights from centreline", 0, 51);
	AcActionac acaUpdateSAreas = new AcActionac("Update Areas", "Update automatic areas", 0, 52);
	AcActionac acaUpdateSymbolLayout = new AcActionac("Update Symbol Lay", "Update symbol layout in view", 0, 53);
	AcActionac acaUpdateSymbolLayoutAll = new AcActionac("Update Symbol Lay All", "Update symbol layout Everywhere", 0, 54);
	AcActionac acaDetailRender = new AcActionac("Detail Render", "Detail Render", 0, 56);
	AcActionac acaUpdateEverything = new AcActionac("Update Everything", "All updates in a row", 0, 58);
	AcActionac acaReloadFontcolours = new AcActionac("Reload Fontcolours", "Makes all the subsets again", 0, 59);

	JMenu menuAuto = new JMenu("Update");
	AcActionac[] acAutoarr = { acaSetZonnodes, acaUpdateSAreas, acaUpdateSymbolLayout, acaUpdateSymbolLayoutAll, acaDetailRender, acaUpdateEverything, acaReloadFontcolours };

	// import menu
	AcActionac acaPrevDownsketch = new AcActionac("Preview Down Sketch", "See the sketch that will be distorted", 0, 91);

	AcActionac acaImportDownSketch = new AcActionac("Import Down Sketch", "Bring in the distorted sketch", 0, 95);
	JCheckBoxMenuItem miImportTitleSubsets = new JCheckBoxMenuItem("*title Subsets", true);
	JCheckBoxMenuItem miImportDateSubsets = new JCheckBoxMenuItem("*date Subsets", false);
	JCheckBoxMenuItem miImportCentreSubsetsU = new JCheckBoxMenuItem("Import Cen-Subsets", true);
	JCheckBoxMenuItem miClearCentreSubsets = new JCheckBoxMenuItem("Clear Cen-Subsets", true);
	JCheckBoxMenuItem miImportNoCentrelines = new JCheckBoxMenuItem("Exclude Centrelines", false);
	JCheckBoxMenuItem miUseSurvex = new JCheckBoxMenuItem("Use Survex", false);

	AcActionac acaStripeAreas = new AcActionac("Stripe Areas", "See the areas filled with stripes", 0, 93);

	AcActionac acaImportA4 = new AcActionac("Make A4", "Make A4 rectangle", 0, 404);
	AcActionac acaImportA4landscape = new AcActionac("Make A4 landscape", "Make A4 rectangle landscape", 0, 414);
	AcActionac acaImportA3 = new AcActionac("Make A3", "Make A3 rectangle", 0, 403);
	AcActionac acaImportA3landscape = new AcActionac("Make A3 landscape", "Make A3 rectangle landscape", 0, 413);
	AcActionac acaImportA2 = new AcActionac("Make A2", "Make A2 rectangle", 0, 402);
	AcActionac acaImportA1 = new AcActionac("Make A1", "Make A1 rectangle", 0, 401);
	AcActionac acaImportA1landscape = new AcActionac("Make A1 landscape", "Make A1 rectangle landscape", 0, 411);
	AcActionac acaImportA0 = new AcActionac("Make A0", "Make A0 rectangle", 0, 400);
	AcActionac[] acmenuPaper = { acaImportA4, acaImportA4landscape, acaImportA3, acaImportA3landscape, acaImportA2, acaImportA1, acaImportA1landscape, acaImportA0 };

	AcActionac acaImportCentrelineFile = new AcActionac("Import Survex File", "Loads a survex file into a Label", 0, 501);
	AcActionac acaPreviewLabelWireframe = new AcActionac("Wireframe view", "Previews selected SVX data as Wireframe in Aven if available", 0, 510);
	AcActionac acaImportLabelCentreline = new AcActionac("Import Centreline", "Imports selected SVX data from label", 0, 511);

	AcActionac acaImportAtlasTemplate = new AcActionac("Import Atlas", "Makes atlas from template", 0, 502);

	JMenu menuImport = new JMenu("Import");

	JMenu menuImportPaper = new JMenu("Import Paper");

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
	AcActionac acaPartitionSubsetDates = new AcActionac("Partition Date Subsets", "Put paths into nearest date subset", 0, 733);
	AcActionac acaAddToSubset = new AcActionac("Add to Subset", "Add selected paths to subset", 0, 74);
	AcActionac acaRemoveFromSubset = new AcActionac("Remove from Subset", "Remove selected paths to subset", 0, 75);
	AcActionac acaDeleteTodeleteSubset = new AcActionac("Delete 'todelete' Subset", "Delete all paths in the 'todelete' subset", 0, 78);
	AcActionac acaClearSubsetContents = new AcActionac("Clear subset contents", "Remove all paths from subset", 0, 79);
	AcActionac acaCleartreeSelection = new AcActionac("Clear subset selection", "Clear selections on subset tree", 0, 76);
	AcActionac acaToggleViewHidden = new AcActionac("Toggle Hidden", "Change hidden subset settings", 0, 70);
	AcActionac[] acSubsetarr = { acaToggleViewHidden, acaAddCentreSubset, acaAddRestCentreSubset, acaPartitionSubset, acaPartitionSubsetDates, acaAddToSubset, acaRemoveFromSubset, acaClearSubsetContents, acaDeleteTodeleteSubset, acaCleartreeSelection };

	JCheckBoxMenuItem miAutoAddToSubset = new JCheckBoxMenuItem("Add new paths subset", false);

	JMenu menuElevation = new JMenu("Elevation");
	AcActionac acaXCSubset = new AcActionac("XC subset", "Make new cross-section subset", 0, 71);
	AcActionac acaElevationSubset = new AcActionac("Elevation subset", "Make new elevation subset", 0, 711);
	AcActionac[] acElevarr = { acaXCSubset, acaElevationSubset, };

    JProgressBar visiprogressbar = new JProgressBar(0, 100); 
    
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// set up the arrays
	SketchDisplay(MainBox lmainbox)
	{
		super("Sketch Display");

		// symbols communication.
		mainbox = lmainbox;
		
		// it's important that the two panels are constructed in order.
		sketchgraphicspanel = new SketchGraphics(this);

		// the window with the symbols
		symbolsdisplay = new SymbolsDisplay(this);

		// sketch line style selection
		sketchlinestyle = new SketchLineStyle(symbolsdisplay, this);

		// file menu stuff.
		mi3Dpassageview.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { Make3Dpassageview(); } } );
		menufile.add(mi3Dpassageview);

		miPrintToPYVTK.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { pyvtkGraphics2D.PrintThisPYVTK(sketchgraphicspanel.tsketch); } } );
		menufile.add(miPrintToPYVTK);

		miSaveSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SaveSketch(0); } } );
		menufile.add(miSaveSketch);
		miSaveSketchAs.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SaveSketch(1); } } );
		menufile.add(miSaveSketchAs);
		miUploadSketch.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SaveSketch(2); } } );
		menufile.add(miUploadSketch);

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
			boolean binitialstate = !(/*(miDisplayarr[i] == miShowBackground) ||*/
									  (miDisplayarr[i] == miStationNames) ||
									  (miDisplayarr[i] == miStationAlts) ||
									  (miDisplayarr[i] == miTransitiveSubset) ||
									  (miDisplayarr[i] == miInverseSubset) ||
									  ((miDisplayarr[i] == miHideSplines) && !OnePath.bHideSplines));
			miDisplayarr[i].setState(binitialstate);
			menuDisplay.add(miDisplayarr[i]);
		}
		menuDisplay.add(new JMenuItem(acaStripeAreas));
		menuDisplay.add(miThinZheightsel); 
		menuDisplay.add(miThinZheightselWiden); 
		menuDisplay.add(miThinZheightselNarrow); 
		
		menubar.add(menuDisplay);

		// yoke this checkboxes to ones in the background menu
		miShowBackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) {
				if (backgroundpanel.cbshowbackground.isSelected() != miShowBackground.isSelected())
				  backgroundpanel.cbshowbackground.setSelected(miShowBackground.isSelected());
			} } );
		miShowGrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) {
				if (backgroundpanel.cbshowgrid.isSelected() != miShowGrid.isSelected())
				  backgroundpanel.cbshowgrid.setSelected(miShowGrid.isSelected());
			} } );

		miHideSplines.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { ApplySplineChange(miHideSplines.isSelected()); } } ); 
		
		miSnapToGrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) {
				if (backgroundpanel.cbsnaptogrid.isSelected() != miSnapToGrid.isSelected())
				  backgroundpanel.cbsnaptogrid.setSelected(miSnapToGrid.isSelected());

			} } );
		
		miThinZheightsel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ApplyZheightSelected(miThinZheightsel.isSelected(), 0); } } ); 
		miThinZheightselWiden.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ApplyZheightSelected(miThinZheightsel.isSelected(), 1); } } ); 
		miThinZheightselNarrow.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchgraphicspanel.ApplyZheightSelected(miThinZheightsel.isSelected(), -1); } } ); 


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
		menuAction.add(miDeleteCentrelines); 
		menubar.add(menuAction);

		// auto menu
		for (int i = 0; i < acAutoarr.length; i++)
			menuAuto.add(new JMenuItem(acAutoarr[i]));
		menubar.add(menuAuto);

		// import menu
		menuImport.add(acaImportCentrelineFile); 

		miUseSurvex.setSelected(FileAbstraction.SurvexExists()); 
		miImportNoCentrelines.setToolTipText("Applies to Import Down Sketch only");
		miDeleteCentrelines.setToolTipText("Enable deletion of centrelines as well as other types"); 

		menuImport.add(miUseSurvex); 
		menuImport.add(acaPreviewLabelWireframe); 
		menuImport.add(miImportTitleSubsets);
		menuImport.add(miImportDateSubsets); 
		menuImport.add(acaImportLabelCentreline); 
		menuImport.add(new JMenuItem(acaPrevDownsketch));
		menuImport.add(miImportNoCentrelines);
		menuImport.add(miImportCentreSubsetsU);
		menuImport.add(miClearCentreSubsets);
		menuImport.add(new JMenuItem(acaImportDownSketch));

		for (int i = 0; i < acmenuPaper.length; i++)
			menuImportPaper.add(new JMenuItem(acmenuPaper[i]));
		menuImport.add(menuImportPaper);
		menuImportPaper.setToolTipText("Used to define the paper outline in a poster view"); 

		menuImport.add(new JMenuItem(acaImportAtlasTemplate)); 

		menubar.add(menuImport);

		// colour menu stuff.
		menuColour.add(new JMenuItem(acaColourDefault));
		menuColour.add(new JMenuItem(acaColourByZ));
		menuColour.add(new JMenuItem(acaColourByProx));
		menuColour.add(new JMenuItem(acaColourByCnodeWeight));
		menuColour.add(new JMenuItem(acaPrintProximities));
		menubar.add(menuColour);

		for (int i = 0; i < acElevarr.length; i++)
			menuElevation.add(new JMenuItem(acElevarr[i]));
		menubar.add(menuElevation);

		// subset menu stuff.
		menuSubset.add(new JMenuItem(acSubsetarr[0]));
		menuSubset.add(new JMenuItem(acSubsetarr[1]));
        menuSubset.add(miAutoAddToSubset); 
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
		sketchlinestyle.pthstylenonconn.add(pnonconn, BorderLayout.CENTER);
		sketchlinestyle.pthstylenonconn.add(visiprogressbar, BorderLayout.SOUTH);;

		// put in the deselect and delete below the row of style buttons
		Insets inset = new Insets(1, 1, 1, 1);
		for (int i = 0; i < acPathcomarr.length; i++)
		{
			JButton butt = new JButton(acPathcomarr[i]); 
			butt.setMargin(inset);
			sketchlinestyle.pathcoms.add(butt);
		}

		subsetpanel = new SketchSubsetPanel(this);
	    selectedsubsetstruct = new SelectedSubsetStructure(this); 
		backgroundpanel = new SketchBackgroundPanel(this);
        infopanel = new SketchInfoPanel(this);
		printingpanel = new SketchPrintPanel(this); 
        secondrender = new SketchSecondRender(this); 

		// do the tabbed pane of extra buttons and fields in the side panel.
		bottabbedpane = new JTabbedPane();
		bottabbedpane.addTab("subs", null, subsetpanel, "Subsets used in sketch and on the selected paths");
		bottabbedpane.addTab("img", null, backgroundpanel, "Manage the background scanned image used for tracing");
		bottabbedpane.addTab("info", null, infopanel, "Inspect the raw information relating to a selected path");          // (sketchdisplay.bottabbedpane.getSelectedIndex() == 2)
		bottabbedpane.addTab("out", null, printingpanel, "Set resolution for the rendered survey either to a file or to the internet");
        bottabbedpane.addTab("view", null, secondrender, "Secondary preview of sketch in a mini-window"); 
		bottabbedpane.setSelectedIndex(1); 

		bottabbedpane.addChangeListener(new ChangeListener()
			{ public void stateChanged(ChangeEvent event) { sketchgraphicspanel.UpdateBottTabbedPane(sketchgraphicspanel.currgenpath, sketchgraphicspanel.currselarea, true); } } );

		// the full side panel
		JSplitPane sidepanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sidepanel.setLeftComponent(sketchlinestyle);
		sidepanel.setRightComponent(bottabbedpane);
		sidepanel.setDividerLocation(300);
        sketchlinestyle.setMinimumSize(new Dimension(10, 10)); 
		bottabbedpane.setMinimumSize(new Dimension(10, 10)); 
        //JPanel sidepanel = new JPanel(new BorderLayout());
		//sidepanel.add(sketchlinestyle, BorderLayout.CENTER);
		//sidepanel.add(bottabbedpane, BorderLayout.SOUTH);
        sidepanel.setMinimumSize(new Dimension(10, 10)); 

		JPanel grpanel = new JPanel(new BorderLayout());
		grpanel.add(sketchgraphicspanel, BorderLayout.CENTER);
        //grpanel.add(zdepthzlider, BorderLayout.WEST);   // hmmmm.

		// split pane between side panel and graphics area
		JSplitPane splitPaneG = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPaneG.setDividerLocation(300);

		splitPaneG.setLeftComponent(sidepanel);
		splitPaneG.setRightComponent(grpanel);


		// final set up of display
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(splitPaneG, BorderLayout.CENTER);

		addWindowListener(new SketchHide());

		pack();
        setSize(800, 600);
		setLocation(300, 100);
    }

	/////////////////////////////////////////////
	void Make3Dpassageview()
    {
        try
        {
            if (passagefloor3D == null)
                passagefloor3D = new PassageFloor3D(); 
            passagefloor3D.Make3Dview(sketchgraphicspanel.tsketch); 
        }
		catch (NoClassDefFoundError e)
        {
            TN.emitWarning("*** You need Java3D installed to run this feature"); 
        }
    }


	/////////////////////////////////////////////
	boolean SaveSketch(int savetype)  // 0 save, 1 saveas, 2 upload
    {
        if (savetype == 1)
        {
            FileAbstraction lsketchfile = sketchgraphicspanel.tsketch.sketchfile.SaveAsDialog(true, sketchgraphicspanel.sketchdisplay); 
            if (lsketchfile == null)
                return false; 
            sketchgraphicspanel.tsketch.sketchfile = lsketchfile; 
            setTitle("TunnelX - " + sketchgraphicspanel.tsketch.sketchfile.getPath());
        }
        
        if (savetype == 2)
        {
            String target = TN.troggleurl + "jgtuploadfile";  // for now
            FileAbstraction uploadedimage = FileAbstraction.uploadFile(FileAbstraction.MakeOpenableFileAbstraction(target), "sketch", sketchgraphicspanel.tsketch.sketchfile.getSketchName() + ".xml", null, sketchgraphicspanel.tsketch); 
            if (uploadedimage == null)
                return TN.emitWarning("bum"); 
            TN.emitMessage("jjj   " + uploadedimage.getPath());
			sketchgraphicspanel.tsketch.sketchfile = FileAbstraction.GetImageFile(null, TN.setSuffix(uploadedimage.getPath(), TN.SUFF_XML));

            setTitle("TunnelX - " + sketchgraphicspanel.tsketch.sketchfile.getPath());
    		mainbox.tunnelfilelist.tflist.repaint(); 

            return true; 
        }
        
        visiprogressbar.setString("saving");
        visiprogressbar.setStringPainted(true);
        visiprogressbar.setValue(80); 
        sketchgraphicspanel.tsketch.SaveSketch(); 
        visiprogressbar.setStringPainted(false);
        visiprogressbar.setValue(0); 

		mainbox.tunnelfilelist.tflist.repaint(); 
        return true;     
    }


	/////////////////////////////////////////////
	// switched on and off if we have a connective line selected
	void SetEnabledConnectiveSubtype(boolean benabled)
	{
		acaConntypesymbols.setEnabled(benabled);
		acaConntypelabel.setEnabled(benabled);
		acaConntypearea.setEnabled(benabled);
	}


	/////////////////////////////////////////////
	void ApplySplineChange(boolean lbHideSplines)
	{
		OnePath.bHideSplines = lbHideSplines;
		for (OneSketch tsketch : mainbox.ftsketches)
		{
			if (tsketch.bsketchfileloaded)
				tsketch.ApplySplineChange();
		}
		for (OneSketch tsketch : mainbox.vgsymbolstsketches)
		{
			if (tsketch.bsketchfileloaded)
				tsketch.ApplySplineChange();
		}
	}


	/////////////////////////////////////////////
	void ActivateSketchDisplay(OneSketch activesketch, boolean lbEditable)
	{
		sketchgraphicspanel.bEditable = lbEditable;
		sketchgraphicspanel.ClearSelection(true);

		sketchgraphicspanel.tsketch = activesketch;
		sketchgraphicspanel.asketchavglast = null; // used for lazy evaluation of the average transform.

		// set greyness
		acaUpdateSAreas.setEnabled(!sketchgraphicspanel.tsketch.bSAreasUpdated);
		acaUpdateSymbolLayout.setEnabled(!sketchgraphicspanel.tsketch.bSymbolLayoutUpdated);

		// set the transform pointers to same object
		setTitle(activesketch.sketchfile.getPath());
		
		// it's confusing if this applies to different views
		if (!miThinZheightsel.isSelected())
			miThinZheightsel.setSelected(false);

// could record the last viewing position of the sketch; saved in the sketch as an affine transform
		sketchgraphicspanel.MaxAction(2); // maximize

		//TN.emitMessage("getselindex " + subsetpanel.jcbsubsetstyles.getSelectedIndex());
		sketchgraphicspanel.UpdateBottTabbedPane(null, null, true); 

		if ((subsetpanel.jcbsubsetstyles.getSelectedIndex() == -1) && (subsetpanel.jcbsubsetstyles.getItemCount() != 0))
			subsetpanel.jcbsubsetstyles.setSelectedIndex(0);  // this will cause SubsetSelectionChanged to be called
		else
			subsetpanel.SubsetSelectionChanged(false);

        printingpanel.ResetDIR((TN.currprintdir == null));  // catch it here

		toFront();
		setVisible(true);
	}


	/////////////////////////////////////////////
	void ReloadFontcolours()
	{
		sketchlinestyle.bsubsetattributesneedupdating = true;
		for (FileAbstraction tfile : mainbox.allfontcolours)
			mainbox.tunnelloader.LoadFontcolour(tfile);

		if (sketchlinestyle.bsubsetattributesneedupdating)
			sketchlinestyle.UpdateSymbols(false);
		if (sketchgraphicspanel.tsketch != null)
			SketchGraphics.SketchChangedStatic(SketchGraphics.SC_CHANGE_SAS, sketchgraphicspanel.tsketch, this);
		mainbox.tunnelfilelist.tflist.repaint();
		miUseSurvex.setSelected(FileAbstraction.SurvexExists()); 
	}
	

	/////////////////////////////////////////////
	void ImportAtlasTemplate()
	{
		AtlasGenerator ag = new AtlasGenerator(); 
		OneSketch asketch = mainbox.tunnelfilelist.GetSelectedSketchLoad(); 
		if (asketch.sksascurrent == null)
			asketch.SetSubsetAttrStyle(sketchgraphicspanel.tsketch.sksascurrent, null);
		
		asketch.UpdateSomething(SketchGraphics.SC_UPDATE_ZNODES, true); 
		asketch.UpdateSomething(SketchGraphics.SC_UPDATE_AREAS, true); 
		mainbox.UpdateSketchFrames(asketch, SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS); 

		ag.ImportAtlasTemplate(asketch);

		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoadd.addAll(ag.vpathsatlas); 
		sketchgraphicspanel.CommitPathChanges(null, pthstoadd); 

		sketchgraphicspanel.UpdateBottTabbedPane(null, null, true); 
		subsetpanel.SubsetSelectionChanged(true);
		sketchgraphicspanel.MaxAction(2); // maximize
	}

	/////////////////////////////////////////////
	boolean ImportSketchCentrelineFile(SvxFileDialog sfiledialog)
	{
		OnePath op; 
		if (sketchgraphicspanel.currgenpath == null)
		{
			List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
			op = sketchgraphicspanel.MakeConnectiveLineForData(1); 
			pthstoadd.add(op); 
			sketchgraphicspanel.CommitPathChanges(null, pthstoadd); 
		}
		else
		{
			sketchlinestyle.GoSetParametersCurrPath();
			op = sketchgraphicspanel.currgenpath;
		}


		if (!sketchgraphicspanel.bEditable || (op == null) || (op.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (op.plabedl == null) || (op.plabedl.sfontcode == null))
			return TN.emitWarning("Connective Path with label must be created or selected");

		if (sfiledialog == null)
        {
            sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectory, this, SvxFileDialog.FT_SVX, false);
            if ((sfiledialog == null) || ((sfiledialog.svxfile == null) && (sfiledialog.tunneldirectory == null)))
                return false;
            FileAbstraction fa = sfiledialog.getSelectedFileA(SvxFileDialog.FT_SVX);
            if (fa.localurl == null)
                TN.currentDirectory = fa; 
    		TN.emitMessage(sfiledialog.svxfile.toString() + "  CD " + TN.currentDirectory.getAbsolutePath() + "  " + (fa.localurl == null));
        }

		String survextext = null; 
        if (sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_SVX) 
            survextext = (new SurvexLoaderNew()).LoadSVX(sfiledialog.svxfile);
        else if (sfiledialog.svxfile.xfiletype == FileAbstraction.FA_FILE_POCKET_TOPO) 
            survextext = (new PocketTopoLoader()).LoadPockettopo(sfiledialog.svxfile);
        else
            TN.emitError("unknown file type loader"); 

		sketchlinestyle.pthstylelabeltab.labtextfield.setText(survextext); // the document events
		sketchgraphicspanel.MaxAction(2); // maximize
		return true; 
	}

	/////////////////////////////////////////////
	boolean ImportCentrelineLabel(boolean bpreview, boolean busesurvex)
	{
		OnePath opcll = sketchgraphicspanel.currgenpath;
		if ((opcll == null) || (opcll.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (opcll.plabedl == null) || (opcll.plabedl.sfontcode == null))
			return !TN.emitWarning("Connective Path with label containing the survex data must be selected");

		sketchgraphicspanel.ClearSelection(true);

		// load in the centreline we have into the sketch
		// could even check with centreline existing
// this is how we do the extending of centrelines
//		if (!bpreview && !sketchgraphicspanel.tsketch.sketchLocOffset.isZero())
//			return !TN.emitWarning("Sketch Loc Offset already set; poss already have loaded in a centreline");
		Vec3 appsketchLocOffset = (sketchgraphicspanel.tsketch.sketchLocOffset.isZero() ? null : sketchgraphicspanel.tsketch.sketchLocOffset); 

		// run survex cases
		SurvexLoaderNew sln = null; 
		if (!bpreview || !busesurvex)
		{
			sln = new SurvexLoaderNew();
			sln.InterpretSvxText(opcll.plabedl.drawlab);
		}

		if (busesurvex) // copy in the POS files
		{
			if (!FileAbstraction.RunSurvex(sln, opcll.plabedl.drawlab, appsketchLocOffset))
				return false; 
			if (bpreview)
				return true; 
		}
		else
		{
			sln.sketchLocOffset = (appsketchLocOffset == null ? new Vec3d((float)sln.avgfix.x, (float)sln.avgfix.y, (float)sln.avgfix.z) : new Vec3d((float)appsketchLocOffset.x, (float)appsketchLocOffset.y, (float)appsketchLocOffset.z)); 
			sln.CalcStationPositions();
		}

		if (bpreview) // show preview
		{
			mainbox.wireframedisplay.wiregraphicspanel.vlegs.clear(); 
			mainbox.wireframedisplay.wiregraphicspanel.vstations.clear(); 
			sln.ConstructWireframe(mainbox.wireframedisplay.wiregraphicspanel.vlegs, mainbox.wireframedisplay.wiregraphicspanel.vstations);
			mainbox.wireframedisplay.ActivateWireframeDisplay("Name of sketch");
			return true;
		}
		
		// set the Locoffset
		if (appsketchLocOffset == null)
			sketchgraphicspanel.tsketch.sketchLocOffset = new Vec3((float)sln.sketchLocOffset.x, (float)sln.sketchLocOffset.y, (float)sln.sketchLocOffset.z);
		else
			assert Math.abs(appsketchLocOffset.x - sketchgraphicspanel.tsketch.sketchLocOffset.x) < 0.000001; 
			
		// do anaglyph rotation here
		// TransformSpaceToSketch tsts = new TransformSpaceToSketch(currgenpath, sketchdisplay.mainbox.sc);
		// statpathnode[ipns] = tsts.TransPoint(ol.osfrom.Loc);

		Vec3 xrot, yrot, zrot; 
		double rotanaglyph = 0.0; // hard code this and recompile before reimporting
		//double rotanaglyph = -5.0 * Math.PI / 180; // hard code this and recompile before reimporting
        if (sln.belevation)
		{
			double th = sln.elevationvalue * Math.PI / 180; 
			
			xrot = new Vec3((float)Math.cos(th), (float)Math.sin(th), 0.0F); 
			yrot = new Vec3(0.0F, 0.0F, 1.0F); 
			zrot = new Vec3(-(float)Math.sin(th), (float)Math.cos(th), 0.0F); 
		}
		else if (rotanaglyph != 0.0)
        {
            double cs = Math.cos(rotanaglyph); 
            double sn = Math.sin(rotanaglyph); 
            xrot = new Vec3((float)cs, 0.0F, (float)sn); 
			yrot = new Vec3(0.0F, 1.0F, 0.0F); 
			zrot = new Vec3(-(float)sn, 0.0F, (float)cs); 
            TN.emitWarning("\n\n\n*****\n*****anaglyph rot sn " + sn + "\n*****\n\n\n"); 
        }
        else
		{
            xrot = new Vec3(1.0F, 0.0F, 0.0F); 
			yrot = new Vec3(0.0F, 1.0F, 0.0F); 
			zrot = new Vec3(0.0F, 0.0F, 1.0F); 
		}
        

		Vec3 fsketchLocOffset = new Vec3((float)sln.sketchLocOffset.x, (float)sln.sketchLocOffset.y, (float)sln.sketchLocOffset.z); 
		sketchgraphicspanel.tsketch.sketchLocOffset = new Vec3(fsketchLocOffset.Dot(xrot), fsketchLocOffset.Dot(yrot), fsketchLocOffset.Dot(zrot)); 

        double xsmin = 0.0; 
        double ysmin = 0.0; 
        boolean bfirstsmin = true; 
        for (OneStation os : sln.osmap.values())
        {
        	if (os.station_opn == null)
        	{
            	os.station_opn = new OnePathNode(os.Loc.Dot(xrot) * TN.CENTRELINE_MAGNIFICATION, -os.Loc.Dot(yrot) * TN.CENTRELINE_MAGNIFICATION, os.Loc.Dot(zrot) * TN.CENTRELINE_MAGNIFICATION);
                xsmin = (bfirstsmin ? os.station_opn.pn.getX() : Math.min(os.station_opn.pn.getX(), xsmin)); 
                ysmin = (bfirstsmin ? os.station_opn.pn.getY() : Math.min(os.station_opn.pn.getY(), ysmin)); 
                bfirstsmin = false; 
            }
		}

		if (!sln.ThinDuplicateLegs(sketchgraphicspanel.tsketch.vnodes, sketchgraphicspanel.tsketch.vpaths))
			return TN.emitWarning("Cannot copy over extended legs"); 
		sketchgraphicspanel.ClearSelection(true);
		
		boolean bcopytitles = miImportTitleSubsets.isSelected();
		boolean bcopydates = miImportDateSubsets.isSelected(); 
		int Dnsurfacelegs = 0; 
		int Dnfixlegs = 0; 

		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 

        // perform the translation of the "S"
        {
            double vx = xsmin - opcll.pnstart.pn.getX(); 
            double vy = ysmin - opcll.pnend.pn.getY(); 

    		OnePathNode nopnstart = new OnePathNode((float)(opcll.pnstart.pn.getX() + vx), (float)(opcll.pnstart.pn.getY() + vy), opcll.pnstart.zalt); 
    		OnePathNode nopnend = new OnePathNode((float)(opcll.pnend.pn.getX() + vx), (float)(opcll.pnend.pn.getY() + vy), opcll.pnend.zalt); 
			float[] pco = opcll.GetCoords();
			OnePath nopcll = new OnePath(nopnstart); 
			for (int i = 1; i < opcll.nlines; i++)
				nopcll.LineTo((float)(pco[i * 2] + vx), (float)(pco[i * 2 + 1] + vy)); 
			nopcll.EndPath(nopnend); 
			nopcll.CopyPathAttributes(opcll); 

            pthstoremove.add(opcll); 
			pthstoadd.add(nopcll); 
        }

        // add in all the legs to the adding section
		for (OneLeg ol : sln.vlegs)
		{
			if (ol.osfrom == null)
				Dnfixlegs++; 
			else if (ol.bsurfaceleg)
				Dnsurfacelegs++; 
			else
			{
				OnePath lop = new OnePath(ol.osfrom.station_opn, ol.osfrom.name, ol.osto.station_opn, ol.osto.name);
				if (bcopytitles && !ol.svxtitle.equals(""))
					lop.vssubsets.add(ol.svxtitle);
				if (bcopydates && !ol.svxdate.equals(""))
					lop.vssubsets.add("__date__ " + ol.svxdate); 
				pthstoadd.add(lop); 
			}
		}
		TN.emitMessage("Ignoring " + Dnfixlegs + " fixlegs and " + Dnsurfacelegs + " surfacelegs"); 

		sketchgraphicspanel.asketchavglast = null; // change of avg transform cache.
		sketchgraphicspanel.CommitPathChanges(pthstoremove, pthstoadd); 

        // check everything has been designated centreline nodes after the above commit
        for (OneLeg ol : sln.vlegs)
		{
			if (!ol.bsurfaceleg)
            {
                assert ((ol.osfrom == null) || ol.osfrom.station_opn.IsCentrelineNode()); 
                assert (ol.osto.station_opn.IsCentrelineNode()); 
            }
        }

		sketchgraphicspanel.MaxAction(2);
		subsetpanel.SubsetSelectionChanged(true);

		return true;
	}
}



