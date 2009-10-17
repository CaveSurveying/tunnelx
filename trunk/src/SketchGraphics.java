/////////////////////////////////////////////////////////////////////////////////
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

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Rectangle;
import java.awt.Cursor;

import java.awt.Dimension;
import java.awt.Image;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import java.util.Random;
import java.util.Date;

import java.awt.Color;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.geom.AffineTransform;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar; 

import java.io.FileOutputStream;
import java.io.IOException;

//
//
// SketchGraphics
//
//
class SketchGraphics extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener
{
	SketchDisplay sketchdisplay;

	static int SELECTWINDOWPIX = 5;
	static int MOVERELEASEPIX = 20;

	// the sketch.
	OneSketch skblank = new OneSketch(null);
	OneSketch tsketch = skblank;

	// cached paths of those on screen (used for speeding up of drawing during editing).
		Set<OnePath> tsvpathsvizbound = new HashSet<OnePath>();  // subset which has one area outside of selection
	Set<OnePath> tsvpathsviz = new HashSet<OnePath>();
	SortedSet<OneSArea> tsvareasviz = new TreeSet<OneSArea>();
	Set<OnePathNode> tsvnodesviz = new HashSet<OnePathNode>();
	List<OnePath> tsvpathsframes = new ArrayList<OnePath>(); 
	
	// z range thinning
	boolean bzthinnedvisible = false; 
	float zlothinnedvisible; 
	float zhithinnedvisible; 
	float zlovisible; 
	float zhivisible; 

	boolean bEditable = false;

	OnePath currgenpath = null;
	OneSArea currselarea = null;

	// the currently active mouse path information.
	Line2D.Float moulin = new Line2D.Float();
	GeneralPath moupath = new GeneralPath();
	Ellipse2D elevpoint = new Ellipse2D.Float();
	GeneralPath elevarrow = new GeneralPath(); 
	
	int nmoupathpieces = 1;
	int nmaxmoupathpieces = 30;
	int[] moupiecesfblo = new int[nmaxmoupathpieces];
	int[] moupiecesfbhi = new int[nmaxmoupathpieces];

	boolean bmoulinactive = false;
	boolean bSketchMode = false;
	float moulinmleng = 0;

	Point2D.Float scrpt = new Point2D.Float();
	Point2D.Float moupt = new Point2D.Float();
	Rectangle selrect = new Rectangle();
	Rectangle windowrect = new Rectangle();

	OnePathNode selpathnode = null;
	OnePathNode currpathnode = null;
	OnePathNode selpathnodecycle = null; // used for cycling the selection

	// the array of array of paths which are going to define a boundary
	List<OnePath> vactivepaths = new ArrayList<OnePath>();
	List<OnePathNode> vactivepathsnodecounts = new ArrayList<OnePathNode>(); // sort this.  size = 2 * vactivepaths.size()

	// makes subselections from vactive paths for the components (set up by SelectConnectedSetsFromSelection)
	int[] vactivepathcomponents = new int[20]; // this is a series of pairs
	int nvactivepathcomponents = -1; 
	int ivactivepathcomponents = -1; 
	int icurrgenvactivepath = -1; // indexes currgenpath when it was incoming (useful for the FuseTranslate)

	Dimension csize = new Dimension(0, 0);
	SketchGrid sketchgrid = null;

	int xoc = 0;
	int yoc = 0;

	float ox = 0;
	float oy = 0;

	// values used by the dynamic rotate and scale
	int prevx = 0;
	int prevy = 0;


	// mouse motion state
	final static int M_NONE = 0;
	final static int M_DYN_ROT = 1;
	final static int M_DYN_DRAG = 2;
	final static int M_DYN_SCALE = 3;

	final static int M_SEL_STATIONS = 4;
	final static int M_SEL_XSECTIONS = 5;
	final static int M_SEL_TUBES = 6;
	final static int M_SEL_TUBE_CONE = 7;

	final static int M_SKET = 10;
	final static int M_SKET_SNAP = 11;
	final static int M_SKET_SNAPPED = 12;
	final static int M_SKET_END = 13;

	final static int M_SEL_PATH = 20;
	final static int M_SEL_PATH_ADD = 21;
	final static int M_SEL_PATH_NODE = 22;

    final static int M_SEL_AREA = 23;

	int momotion = M_NONE;

	// the bitmapped background
	ImageWarp backgroundimg = new ImageWarp(csize, this);

	Image mainImg = null;
	Graphics2D mainGraphics = null;
	int ibackimageredo = 0; // 0 redo everything, 1 except bitmat background,
							// 2 except partial sketch caching, 3 except redrawing the background sketch (just the overlay),
	int bkifrm = 0;


	boolean bNextRenderDetailed = false;
	boolean bNextRenderPinkDownSketch = false;
	boolean bNextRenderAreaStripes = false;


	AffineTransform orgtrans = new AffineTransform();
	AffineTransform mdtrans = new AffineTransform();
	AffineTransform currtrans = new AffineTransform();
	double[] flatmat = new double[6];

	/////////////////////////////////////////////
	SketchGraphics(SketchDisplay lsketchdisplay)
	{
		super(false); // not doublebuffered

		skblank.SetupSK(); 
		
		setBackground(TN.wfmBackground);
		setForeground(TN.wfmLeg);

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		sketchdisplay = lsketchdisplay;
		backgroundimg.sketchgraphicspanel = this;

		//Some cursor sets have thick crosses which mean that a cross cursor type is rubbish for drawing
		//A thin cross cursor as I believe was originally intended would be reasonable.  Martin
		//setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}


	/////////////////////////////////////////////
	void RedoBackgroundView()
	{
		ibackimageredo = 0;
		backgroundimg.bBackImageDoneGood = false;
		repaint();
	}
	
	// this keeps all caching
	void RedrawBackgroundView()
	{
		ibackimageredo = 2;
		repaint();
	}

	/////////////////////////////////////////////
	void MaxAction(int imaxaction)
	{
		if (imaxaction != 3)
		{
			// imaxaction == 1, 2, 11, 12
			Rectangle2D boundrect = tsketch.getBounds(true, (imaxaction >= 11));
			if ((boundrect.getWidth() != 0.0F) && (boundrect.getHeight() != 0.0F))
			{
				// set the pre transformation
				mdtrans.setToTranslation(getSize().width / 2, getSize().height / 2);

				// scale change
				if ((imaxaction == 2) || (imaxaction == 12))
				{
					if ((getSize().width != 0) && (getSize().height != 0))
					{
						double scchange = Math.max(boundrect.getWidth() / (getSize().width * 0.9F), boundrect.getHeight() / (getSize().height * 0.9F));
						if (scchange != 0.0F)
							mdtrans.scale(1.0F / scchange, 1.0F / scchange);
					}
				}
				else
				{
					currtrans.getMatrix(flatmat);
					double sca = Math.sqrt(flatmat[0] * flatmat[0] + flatmat[2] * flatmat[2]);
					mdtrans.scale(sca, sca);
				}

				mdtrans.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2));
			}
			//else
			//	mdtrans.setToIdentity();

			orgtrans.setTransform(currtrans);
			currtrans.setTransform(mdtrans);
		}

		else // Set Upright
		{
			currtrans.getMatrix(flatmat);
			double sca = Math.sqrt(flatmat[0] * flatmat[0] + flatmat[2] * flatmat[2]);
			double sca1 = Math.sqrt(flatmat[1] * flatmat[1] + flatmat[3] * flatmat[3]);
			TN.emitMessage("Ortho mat " + sca + " " + sca1);
			currtrans.setTransform(sca, 0, 0, sca, flatmat[4], flatmat[5]);
		}
		RedoBackgroundView();
	}



	/////////////////////////////////////////////
	void UpdateBottTabbedPane(OnePath op, OneSArea osa, boolean btabbingchanged)
	{
		if (sketchdisplay.bottabbedpane.getSelectedIndex() == 0)  
			sketchdisplay.subsetpanel.UpdateSubsetsOfPath(op);
		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 1)  
			sketchdisplay.backgroundpanel.UpdateBackimageCombobox(1); 
		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 2)
		{
			if (op != null)
			{
				int iselpath = tsketch.vpaths.indexOf(op); // slow; (maybe not necessary)
				sketchdisplay.infopanel.tfselitempathno.setText(String.valueOf(iselpath + 1));
				sketchdisplay.infopanel.tfselnumpathno.setText(String.valueOf(tsketch.vpaths.size())); 
				sketchdisplay.infopanel.SetPathXML(op, tsketch.sketchLocOffset);
				assert osa == null; 
			}
			else if (osa != null)
			{
				int iselarea = 0; // tsketch.vsareas.indexOf(osa) doesn't exist
				for (OneSArea losa : tsketch.vsareas)
				{
					if (losa == osa)
						break; 
					iselarea++; 
				}
				sketchdisplay.infopanel.tfselitempathno.setText(String.valueOf(iselarea + 1));
				sketchdisplay.infopanel.tfselnumpathno.setText(String.valueOf(tsketch.vsareas.size()));
				sketchdisplay.infopanel.SetAreaInfo(osa, tsketch);
			}
			else
			{
				sketchdisplay.infopanel.tfselitempathno.setText("");
				sketchdisplay.infopanel.tfselnumpathno.setText("");
				sketchdisplay.infopanel.SetCleared(); 
			}
		}

		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 3)  // use windowrect when no subsets selected
		{
	        if (btabbingchanged)
            {
                sketchdisplay.printingpanel.subsetrect = tsketch.getBounds(true, true); 
                RedrawBackgroundView();
            }
            sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realpaperscale, btabbingchanged); 
        }

		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 4)  
        {
            sketchdisplay.secondrender.Update(btabbingchanged); 
        }
	}

// separate out the observed selection for this so we update the rectangle when there is a change of seleciton
// find out how to make the background alpha channel binary
// check the splitter window on left works
// draw the centreline heavy
// emboss and fill in around the words


	/////////////////////////////////////////////
	void ObserveSelection(OnePath op, OneSArea osa, int yi)
	{
		assert (op == null) || (osa == null); 
		sketchdisplay.sketchlinestyle.SetParametersIntoBoxes(op);
		//System.out.println("oooooo ooo " + yi); 
		UpdateBottTabbedPane(op, osa, false); 

		sketchdisplay.acaAddImage.setEnabled(op == null); 
		boolean btwothreepointpath = (op != null) && ((op.nlines == 1) || (op.nlines == 2)); 
		sketchdisplay.acaMoveBackground.setEnabled(btwothreepointpath && (tsketch.opframebackgrounddrag != null)); 
		sketchdisplay.acaMovePicture.setEnabled(btwothreepointpath); 
		sketchdisplay.acaReloadImage.setEnabled((op != null) && op.IsSketchFrameConnective());  
		sketchdisplay.acvSetGridOrig.setEnabled(op != null); 
		sketchdisplay.acaReflect.setEnabled((op != null) && (op.linestyle != SketchLineStyle.SLS_CENTRELINE)); 
		sketchdisplay.acaImportCentrelineFile.setEnabled(op == null); 
		boolean bsurvexlabel = ((op != null) && (op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.sfontcode != null) && op.plabedl.sfontcode.equals("survey")); 
		sketchdisplay.acaPreviewLabelWireframe.setEnabled(bsurvexlabel); 
		sketchdisplay.acaImportLabelCentreline.setEnabled(bsurvexlabel); 
		sketchdisplay.menuImportPaper.setEnabled((op != null) && (op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) && op.vssubsets.isEmpty()); 
//SSSS

	}


	OnePath pathaddlastsel = null;
	/////////////////////////////////////////////
	// do all the selections of things
	void SelectAction()
	{
		// transform required for selection to work.
		mainGraphics.setTransform(currtrans);


		// do the selection of paths
		if (momotion == M_SEL_PATH)
		{
			OnePath selgenpath = tsketch.SelPath(mainGraphics, selrect, currgenpath, tsvpathsviz);
			ClearSelection(true);
			if (selgenpath != null)
			{
				currgenpath = selgenpath;
				ObserveSelection(currgenpath, null, 2);
			}
			momotion = M_NONE;
		}

		// do the selection of areas
		if (momotion == M_SEL_AREA)
		{
			OneSArea lcurrselarea = tsketch.SelArea(mainGraphics, selrect, currselarea, tsvareasviz);
			ClearSelection(true);
			currselarea = lcurrselarea;
			ObserveSelection(null, currselarea, 3);
			momotion = M_NONE;
		}


		// do the selection of paths to add
		if (momotion == M_SEL_PATH_ADD)
		{
			// push current selected path in the list.
			if (currgenpath != null)
			{
				AddVActivePath(currgenpath);
				currgenpath = null;
			}

			// find a path and invert toggle it in the list.
			OnePath selgenpath = tsketch.SelPath(mainGraphics, selrect, pathaddlastsel, tsvpathsviz);

			// toggle in list.
			if (selgenpath != null)
			{
				pathaddlastsel = selgenpath;
				CollapseVActivePathComponent(); 
				if (vactivepaths.contains(selgenpath))
				{
					RemoveVActivePath(selgenpath);
					ObserveSelection(null, null, 4);
				}
				else
				{
					AddVActivePath(selgenpath);
					ObserveSelection(selgenpath, null, 5);
				}
			}
			else
				pathaddlastsel = null;

			momotion = M_NONE;
		}


		// do the selection of pathnodes
		if (momotion == M_SKET_SNAP)
		{
			OnePathNode opfront =(bmoulinactive && (currgenpath != null) ? currgenpath.pnstart : null);
			boolean bopfrontvalid = ((opfront != null) && (currgenpath.nlines >= 2));
			selpathnode = tsketch.SelNode(opfront, bopfrontvalid, mainGraphics, selrect, selpathnodecycle, tsvnodesviz);

			if (selpathnode == null)
				momotion = M_NONE;
			else
			{
				selpathnodecycle = selpathnode;
				if (!bmoulinactive)
					SetMouseLine(selpathnode.pn, selpathnode.pn);
			}
		}

		// the drop through into snapped mode
		if ((momotion == M_SKET_SNAPPED) || (momotion == M_SKET_SNAP))
		{
			if (momotion == M_SKET_SNAPPED)
			{
				if (mainGraphics.hit(selrect, selpathnode.Getpnell(), false))
					currpathnode = selpathnode;
				else
					currpathnode = null;
			}
			else
			{
				momotion = M_SKET_SNAPPED;
				currpathnode = selpathnode;
			}

			// snap the mouse line to it.
			if (bmoulinactive && (currpathnode != null))
			{
				if (currgenpath == null)
					SetMouseLine(currpathnode.pn, null);
				else
					SetMouseLine(null, currpathnode.pn);
			}
		}
	}

	/////////////////////////////////////////////
	void ApplyZheightSelected(boolean bthinbyheight, int widencode)
	{
        // on resizing
		if (bthinbyheight && bzthinnedvisible && (widencode != 0))
		{
			double zwidgap = zhithinnedvisible - zlothinnedvisible; 
			double zwidgapfac = (widencode == 1 ? zwidgap / 2 : -zwidgap / 4); 
			zlothinnedvisible -= zwidgapfac; 
			zhithinnedvisible += zwidgapfac; 
			assert zlothinnedvisible <= zhithinnedvisible; 
		}

        // on selection
		else if (bthinbyheight)
		{
			// very crudely do it by selection list
			CollapseVActivePathComponent(); 
			Set<OnePath> opselset = MakeTotalSelList(); 
			if (opselset.isEmpty())
			{
				TN.emitWarning("No selection set for thinning by z"); 
				sketchdisplay.miThinZheightsel.setSelected(false); 
				return; 
			}

			boolean bfirst = true; 
			for (OnePath op : opselset)
			{
				if (bfirst)
				{
					zlothinnedvisible = op.pnstart.zalt; 
					zhithinnedvisible = op.pnstart.zalt; 
					bfirst = false; 
				}
				else
				{
					if (op.pnstart.zalt < zlothinnedvisible)
						zlothinnedvisible = op.pnstart.zalt; 
					else if (op.pnstart.zalt > zhithinnedvisible)
						zhithinnedvisible = op.pnstart.zalt; 
				}
				if (op.pnend.zalt < zlothinnedvisible)
					zlothinnedvisible = op.pnend.zalt; 
				else if (op.pnend.zalt > zhithinnedvisible)
					zhithinnedvisible = op.pnend.zalt; 
			}

			bzthinnedvisible = true; 
			TN.emitMessage("Thinning on z " + zlothinnedvisible + " < " + zhithinnedvisible); 
		}

        // on deselection
		else
			bzthinnedvisible = false; 
		RedoBackgroundView(); 
	}
	
	/////////////////////////////////////////////
	float GetMidZsel()
	{
		return (bzthinnedvisible ? (float)(zlothinnedvisible + zhithinnedvisible) / 2 : 0.0F); 
	}

	/////////////////////////////////////////////
// todo
//
// get the overlay drawn properly
// move the thinzlevels selection into the background/gridlines tab
// overlay could also draw up where the mouse is on the edge
// allow for middle mouse button click to extend/shorten this range
// make sure new edges are added to mid-point of selection or the midpoint of visible
// implement the *title "  "; "  "; "  "
// implement the *uppertitle "  "...

    void paintThinZBar(Graphics2D g2D, int cheight)
    {
		g2D.setColor(Color.blue);
        g2D.fillRect(1, 0, 4, cheight);

        g2D.drawString(String.valueOf(zhivisible), 5, 0);
        g2D.drawString(String.valueOf(zlovisible), 5, cheight - 5);
g2D.drawString("mmmm", 100, 100);

// draw a blue box representing the Z-range from bottom to top
// 
        float zvisiblediff = zhivisible - zlovisible; 
        if (zvisiblediff != 0.0)
        {
            g2D.setColor(Color.red); 
            float lamzlo = (zlothinnedvisible - zlovisible) / zvisiblediff; 
            float lamzhi = (zhithinnedvisible - zlovisible) / zvisiblediff; 
            int zbtop = (int)((1.0 - lamzhi) * csize.height); 
            int zbbot = (int)((1.0 - lamzlo) * csize.height + 1.0); 
            g2D.fillRect(0, zbtop, 4, zbbot - zbtop); 
        }

        // find the z-range of what is selected
        float zloselected = 0.0F; 
        float zhiselected = 0.0F; 
        boolean bzrselected = false; 
        if ((currgenpath != null) && (currgenpath.pnend != null))
        {
            zloselected = Math.min(currgenpath.pnstart.zalt, currgenpath.pnend.zalt); 
            zhiselected = Math.max(currgenpath.pnstart.zalt, currgenpath.pnend.zalt); 
            bzrselected = true; 
        }
        if (currselarea != null)
        {
			for (RefPathO rpo : currselarea.refpaths)
            {
                float zalt = rpo.ToNode().zalt; 
                if (!bzrselected || (zalt < zloselected))
                     zloselected = zalt; 
                if (!bzrselected || (zalt > zhiselected))
                     zhiselected = zalt; 
                bzrselected = true; 
            }
        }
        for (OnePath op : vactivepaths)
        {
            float zlo = Math.min(op.pnstart.zalt, op.pnend.zalt); 
            float zhi = Math.max(op.pnstart.zalt, op.pnend.zalt); 
            if (!bzrselected || (zlo < zloselected))
                zloselected = zlo; 
            if (!bzrselected || (zhi > zhiselected))
                    zhiselected = zhi; 
            bzrselected = true; 
        }

        if (bzrselected)
        {
            g2D.setColor(SketchLineStyle.activepnlinestyleattr.strokecolour); 
            float lamzlo = (zloselected - zlovisible) / zvisiblediff; 
            float lamzhi = (zhiselected - zlovisible) / zvisiblediff; 
            int zbtop = (int)((1.0 - lamzhi) * csize.height); 
            int zbbot = (int)((1.0 - lamzlo) * csize.height + 1.0); 
            g2D.fillRect(0, zbtop, 6, zbbot - zbtop); 
        }
    }

		
	/////////////////////////////////////////////
	void RenderBackground()
	{
		mainGraphics.setTransform(id);

		// this is due to the background moving
		if ((ibackimageredo == 0) && sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
			sketchgrid.UpdateGridCoords(csize, currtrans, sketchdisplay.miEnableRotate.isSelected(), sketchdisplay.backgroundpanel);

        if ((ibackimageredo == 0) && (sketchdisplay.bottabbedpane.getSelectedIndex() == 3))  // use windowrect when no subsets selected
        	sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realpaperscale, true); 

		// render the background
// this is working independently of ibackimageredo for now
		boolean bNewBackgroundExists = ((tsketch.opframebackgrounddrag != null) && (tsketch.opframebackgrounddrag.plabedl != null) && (tsketch.opframebackgrounddrag.plabedl.sketchframedef != null));
		boolean bClearBackground = (!bNewBackgroundExists || !sketchdisplay.miShowBackground.isSelected());
		if (!bClearBackground && !backgroundimg.bBackImageGood)
		{
			backgroundimg.bBackImageGood = true;
			bClearBackground = !backgroundimg.bBackImageGood;
		}
		
		// we have a background.
		if (!bClearBackground && !backgroundimg.bBackImageDoneGood)
		{
			backgroundimg.SketchBackground(currtrans);
			backgroundimg.bBackImageDoneGood = true;
		}

		// now draw our cached background or fill it with empty.
		if (bClearBackground)
		{
			mainGraphics.setColor(Color.white);
			mainGraphics.fillRect(0, 0, csize.width, csize.height);
		}
		else
			mainGraphics.drawImage(backgroundimg.backimagedone, 0, 0, null);

		// as if the above was using this flag correctly
		if (ibackimageredo == 0)
			ibackimageredo = 1;

		// render the image on top of the background.
		TN.emitMessage("backimgdraw " + bkifrm++ + "  " + ibackimageredo + "  " + bClearBackground);

		// drawing stuff on top
		mainGraphics.setTransform(currtrans);

		// caching the paths which are in view
		if (ibackimageredo == 1)
		{
			tsvpathsviz.clear();
			tsvpathsvizbound.clear(); 
			tsvareasviz.clear(); 
			tsvnodesviz.clear(); 
			tsvpathsframes.clear(); 

			// accelerate this caching if we are zoomed out a lot (using the max calculation)
			Rectangle2D boundrect = tsketch.getBounds(false, false);
			double scchange = Math.max(boundrect.getWidth() / (getSize().width * 0.9F), boundrect.getHeight() / (getSize().height * 0.9F));
			double tsca = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
			if ((scchange * tsca > 1.9) || bzthinnedvisible)
			{
				Collection<OnePath> lvpathsviz; 
				Collection<OneSArea> lvsareasviz; 
				if (bzthinnedvisible)
				{
					lvpathsviz = new HashSet<OnePath>(); 
					lvsareasviz = new HashSet<OneSArea>(); 
					for (OnePath op : tsketch.vpaths)
					{
						// select paths by z and grab areas on either side
						if ((zlothinnedvisible <= Math.max(op.pnstart.zalt, op.pnend.zalt)) && (Math.min(op.pnstart.zalt, op.pnend.zalt) <= zhithinnedvisible)) 
						{
							lvpathsviz.add(op);
							if (op.kaleft != null)
								lvsareasviz.add(op.kaleft); 
							if (op.karight != null)
								lvsareasviz.add(op.karight); 
						}
					}
					for (OneSArea osa : lvsareasviz)
					{
						// get paths in each area, have the ones on the boundary greyed by putting into tsvpathsvizbound instead
						for (RefPathO rpo : osa.refpaths)
						{
							OneSArea osacross = rpo.GetCrossArea(); 
							if ((osacross == null) || lvsareasviz.contains(osacross))
								lvpathsviz.add(rpo.op); 
							else if (mainGraphics.hit(windowrect, rpo.op.gp, (rpo.op.linestyle != SketchLineStyle.SLS_FILLED)))
								tsvpathsvizbound.add(rpo.op); 
						}
						for (ConnectiveComponentAreas cca : osa.ccalist)
						{
							lvpathsviz.addAll(cca.vconnpaths);
							lvpathsviz.addAll(cca.vconnpathsrem); 
						}
					}	
				}
				else
				{
					// use the originals before hit thinning
					lvpathsviz = tsketch.vpaths; 
					lvsareasviz = tsketch.vsareas; 
				}						

				// now thin by visibility
				for (OnePath op : lvpathsviz)
				{
					boolean bcountasfilled = ((op.linestyle == SketchLineStyle.SLS_FILLED) || op.IsSketchFrameConnective()); 
					if ((mainGraphics.hit(windowrect, op.gp, !bcountasfilled) || 
						 ((op.plabedl != null) && (op.plabedl.drawlab != null) && (op.plabedl.rectdef != null) && mainGraphics.hit(selrect, op.plabedl.rectdef, false))))
					{
						tsvpathsviz.add(op);
						if (op.IsSketchFrameConnective() && !op.plabedl.sketchframedef.sfsketch.equals(""))
							tsvpathsframes.add(op); 

						// do the visibility of the nodes around it (it's a set so doesn't mind duplicates)
						tsvnodesviz.add(op.pnstart); 
						tsvnodesviz.add(op.pnend); 
					}
				}
				for (OneSArea osa : lvsareasviz)
				{
					if (mainGraphics.hit(windowrect, osa.gparea, false))
						tsvareasviz.add(osa); 
				}				
			}
			else 
			{
				tsvpathsviz.addAll(tsketch.vpaths);
				tsvareasviz.addAll(tsketch.vsareas); 
				tsvnodesviz.addAll(tsketch.vnodes); 
				for (OnePath op : tsvpathsviz)
				{
					if (op.IsSketchFrameConnective() && !op.plabedl.sketchframedef.sfsketch.equals(""))
						tsvpathsframes.add(op); 
				}
			}

            // set the height range that's visible
            zlovisible = (tsvnodesviz.isEmpty() ? 0.0F : tsvnodesviz.iterator().next().zalt); 
            zhivisible = zlovisible; 
            for (OnePathNode opn : tsvnodesviz)
            {
                if (opn.zalt < zlovisible)
                    zlovisible = opn.zalt; 
                else if (opn.zalt > zhivisible)
                    zhivisible = opn.zalt; 
            }
            TN.emitMessage("Setting zvisible " + zlovisible + "  " + zhivisible); 

			ibackimageredo = 2;
			
			if (sketchdisplay.bottabbedpane.getSelectedIndex() == 1)  
				sketchdisplay.backgroundpanel.UpdateBackimageCombobox(0); 
		}

		

		// the grid thing
		if (sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
		{
			mainGraphics.setStroke(SketchLineStyle.gridStroke); // thin
			mainGraphics.setColor(SketchLineStyle.gridColor); // black
			mainGraphics.draw(sketchgrid.gpgrid);
		}

		// draw the sketch according to what view we want (incl single frame of print quality)
		boolean bHideMarkers = !sketchdisplay.miShowNodes.isSelected();
		int stationnamecond = (sketchdisplay.miStationNames.isSelected() ? 1 : 0) + (sketchdisplay.miStationAlts.isSelected() ? 2 : 0);
		GraphicsAbstraction ga = new GraphicsAbstraction(mainGraphics); 
		if (bNextRenderDetailed)
			tsketch.paintWqualitySketch(ga, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex(), sketchdisplay.sketchlinestyle.subsetattrstylesmap);
		else
			tsketch.paintWbkgd(new GraphicsAbstraction(mainGraphics), !sketchdisplay.miCentreline.isSelected(), bHideMarkers, stationnamecond, tsvpathsviz, tsvpathsvizbound, tsvareasviz, tsvnodesviz);

		// all back image stuff done.  Now just the overlays.
		ibackimageredo = 3;
	}



	AffineTransform id = new AffineTransform(); // identity
	/////////////////////////////////////////////
	public void paintComponent(Graphics g)
	{
		boolean bDynBackDraw = ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT));
		if (bNextRenderDetailed)
			ibackimageredo = 2;

		// test if resize has happened because we are rebuffering things
		// then go in again.
		if ((mainImg == null) || (getSize().height != csize.height) || (getSize().width != csize.width))
		{
			csize.width = getSize().width;
			csize.height = getSize().height;
			windowrect.setRect(0, 0, csize.width, csize.height);
			mainImg = createImage(csize.width, csize.height);
			mainGraphics = (Graphics2D)mainImg.getGraphics();
			RedoBackgroundView();
			return;
		}

		// do all the selection stuff, all based on momotion
		SelectAction();

		// now we start rendering what is into the mainGraphics.
		// when rendering is complete, we draw it in the front.
		// paint the background in.
		Graphics2D g2D = (Graphics2D)g;
            // this contains a reference to g2D and doesn't quite encapsulate all the functions yet.  should it?  Was for production of SVG and other such outputs

        AffineTransform orgtrans = g2D.getTransform();

		GraphicsAbstraction ga = new GraphicsAbstraction(g2D);  
		if (!bDynBackDraw)
		{
			// the rendering of the background image is already included in the background.
			if (ibackimageredo <= 2)
				RenderBackground();
            assert(ibackimageredo == 3);

			g2D.drawImage(mainImg, 0, 0, null);
		}

		// drawing of bitmap for quick dynamic dragging
		else
		{
			// simply redraw the back image into the front, transformed for fast dynamic rendering.
			g2D.setColor(TN.skeBackground);
			g2D.fillRect(0, 0, csize.width, csize.height);
			g2D.drawImage(mainImg, mdtrans, null);
		}


		//
		// draw the active paths over it in the real window buffer.
        //

		ga.transform(currtrans);
		ga.SetMainClip();
		g2D.setFont(sketchdisplay.sketchlinestyle.defaultfontlab);

		//if (tsketch.opframebackgrounddrag != null)
		//	ga.drawPath(tsketch.opframebackgrounddrag, SketchLineStyle.framebackgrounddragstyleattr); 

		// draw all the active paths, or just a selected component
		if ((nvactivepathcomponents == -1) || (ivactivepathcomponents == 0)) 
		{
			for (OnePath op : vactivepaths)
				op.paintW(ga, false, true);
		}
		else
		{
			for (int i = vactivepathcomponents[ivactivepathcomponents - 1]; i < vactivepathcomponents[ivactivepathcomponents]; i++)
				vactivepaths.get(i).paintW(ga, false, true);
		}

		int ipn = 0;
		while (ipn < vactivepathsnodecounts.size())
		{
			int pipn = ipn++;
			while ((ipn < vactivepathsnodecounts.size()) && (vactivepathsnodecounts.get(ipn) == vactivepathsnodecounts.get(pipn)))
				ipn++;
			if (((ipn - pipn) % 2) == 1)
				ga.drawShape(vactivepathsnodecounts.get(pipn).Getpnell(), SketchLineStyle.activepnlinestyleattr);
		}

		// the current node
		if ((momotion == M_SKET_SNAPPED) && (currpathnode != null))
		{
			ga.drawShape(currpathnode.Getpnell(), SketchLineStyle.activepnlinestyleattr);
			if ((currpathnode.IsCentrelineNode()) && sketchdisplay.miStationNames.isSelected())
				ga.drawString(currpathnode.pnstationlabel, SketchLineStyle.stationPropertyFontAttr, (float)currpathnode.pn.getX() + SketchLineStyle.strokew * 2, (float)currpathnode.pn.getY() - SketchLineStyle.strokew);

			if (!bmoulinactive)
				ga.drawShape(moupath, SketchLineStyle.activepnlinestyleattr); // moulin
		}


		// draw the selected/active paths.
		if (currgenpath != null)
		{
			// do we have a Frame sketch
			if ((currgenpath.plabedl != null) && (currgenpath.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) && ((currgenpath.karight != null) || (currgenpath.kaleft != null)) && ((currgenpath.plabedl.sketchframedef.pframesketch != null) || (currgenpath.plabedl.sketchframedef.pframeimage != null)))
			{
				AffineTransform satrans = g2D.getTransform();
				ga.transform(currgenpath.plabedl.sketchframedef.pframesketchtrans);

				if (currgenpath.plabedl.sketchframedef.pframeimage != null)
					ga.drawImage(currgenpath.plabedl.sketchframedef.pframeimage.GetImage(true));

				if (currgenpath.plabedl.sketchframedef.pframesketch != null)
				{
					OneSketch asketch = currgenpath.plabedl.sketchframedef.pframesketch;
					//System.out.println("Plotting frame sketch " + asketch.vpaths.size() + "  " + satrans.toString());
					for (OnePath op : asketch.vpaths)
					{
						if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) && (op.linestyle != SketchLineStyle.SLS_CONNECTIVE))
							op.paintW(ga, true, true);
					}
				}
				g2D.setTransform(satrans);
			}

			// draw the symbols on this path
			for (OneSSymbol oss : currgenpath.vpsymbols)
				oss.paintW(ga, true, false);

			// draw the endpoints different colours so we can determin handedness.
			if (currgenpath.pnstart != null)
				ga.drawShape(currgenpath.pnstart.Getpnell(), SketchLineStyle.firstselpnlinestyleattr);
			if (currgenpath.pnend != null)
				ga.drawShape(currgenpath.pnend.Getpnell(), SketchLineStyle.lastselpnlinestyleattr);

			currgenpath.paintW(ga, false, true);
		}

		// draw in the selected area outline (what will be put into the subset).
		if (currselarea != null)
		{
			for (RefPathO rpo : currselarea.refpaths)
				rpo.op.paintW(ga, false, true);
			for (ConnectiveComponentAreas cca : currselarea.ccalist)
			{
				for (OnePath sop : cca.vconnpaths)
					sop.paintW(ga, false, true);
				for (OnePath sop : cca.vconnpathsrem)
					sop.paintW(ga, false, true);
			}
			for (int i = 0; i < currselarea.nconnpathremaining; i++)
				currselarea.connpathrootscen.get(i).paintW(ga, false, true);
		}

		// draw the rubber band.
		if (bmoulinactive)
			ga.drawShape(moupath, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]);  // moulin/


		// deal with the overlays
		bNextRenderDetailed = false;
		// draw the areas in hatched
		if (bNextRenderAreaStripes)
		{
			if ((currgenpath != null) && (currgenpath.pthcca != null))
			{
				int i = 0; 
				for (OneSArea osa : currgenpath.pthcca.vconnareas)
					osa.paintHatchW(ga, i++);
			}
			else
			{
				int i = 0; 
				for (OneSArea osa : tsketch.vsareas)
					osa.paintHatchW(ga, i++);
			}
			bNextRenderAreaStripes = false;
		}

		// paint the down sketches that we are going to import (this is a preview).
		// this messes up the ga.transform.
		if (bNextRenderPinkDownSketch)
		{
			OneSketch lselectedsketch = sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchLoad(); 
			if (lselectedsketch != null)
				paintSelectedSketch(ga, lselectedsketch);
			else
				TN.emitWarning("No sketch selected");
			bNextRenderPinkDownSketch = false;
		}

		// this is where that elevation blob is drawn
		if (sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct)
			//ga.drawShape(elevpoint, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]);  
			ga.drawShape(elevarrow, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]);  

        // this is where the Z-range scale is drawn
        if (sketchdisplay.miThinZheightsel.isSelected())
    	{
        	g2D.setTransform(orgtrans); 
    		g2D.setFont(sketchdisplay.sketchlinestyle.defaultfontlab);
            paintThinZBar(g2D, csize.height); 
        }
	}
	


	/////////////////////////////////////////////
	// dimensions of the paper are given in metres (then multiplied up by 1000 so that the font stuff actually works)
	// An entirely new set of fonts and linewidths will be required on this paper level (all the title stuff I guess)
	boolean ImportPaperM(String papersize, float lwidth, float lheight)
	{
		if ((currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (currgenpath.plabedl == null) || (currgenpath.plabedl.barea_pres_signal != SketchLineStyle.ASE_SKETCHFRAME) || !currgenpath.vssubsets.isEmpty())
			return TN.emitWarning("Connective path, with frame area signal, not in any subset, must selected");

		String sspapersubset = sketchdisplay.subsetpanel.GetNewPaperSubset(papersize); 

		sketchdisplay.subsetpanel.PutToSubset(currgenpath, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(currgenpath, sspapersubset, true); 

		float pwidth = (float)(lwidth * tsketch.realpaperscale * TN.CENTRELINE_MAGNIFICATION);
		float pheight = (float)(lheight * tsketch.realpaperscale * TN.CENTRELINE_MAGNIFICATION);

		OnePathNode opn00 = currgenpath.pnstart;
		float x = (float)opn00.pn.getX();
		float y = (float)opn00.pn.getY();
		OnePathNode opn01 = new OnePathNode(x + pwidth, y, GetMidZsel());
		OnePathNode opn10 = new OnePathNode(x, y + pheight, GetMidZsel());
		OnePathNode opn11 = new OnePathNode(x + pwidth, y + pheight, GetMidZsel());

		OnePath op0X = new OnePath(opn00);
		op0X.EndPath(opn01);
		op0X.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(op0X, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op0X, sspapersubset, true); 

		OnePath opX1 = new OnePath(opn01);
		opX1.EndPath(opn11);
		opX1.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(opX1, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(opX1, sspapersubset, true); 

		OnePath op1X = new OnePath(opn11);
		op1X.EndPath(opn10);
		op1X.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(op1X, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op1X, sspapersubset, true); 

		OnePath opX0 = new OnePath(opn10);
		opX0.EndPath(opn00);
		opX0.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(opX1, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(opX1, sspapersubset, true); 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoadd.add(opX0); 
		pthstoadd.add(opX1); 
		pthstoadd.add(op0X); 
		pthstoadd.add(op1X); 

		return CommitPathChanges(pthstoremove, pthstoadd); 
	}



	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it from the selected sketch in the mainbox.
	void ImportSketch(OneSketch asketch, boolean bImportSubsetsOnCentreline, boolean bClearSubsetsOnCentreline, boolean bImportNoCentrelines)
	{
		if ((asketch == null) || (tsketch == asketch))
		{
			TN.emitWarning(asketch == null ? "Sketch not selected" : "Can't import sketch onto itself");
			return;
		}
		TN.emitMessage((bClearSubsetsOnCentreline ? "" : "Not ") + "Overwriting subsets info on centrelines");

		PtrelLn ptrelln = new PtrelLn();

		// all in one find the centreline paths and the corresponding paths we will export to.
		boolean bcorrespsucc = ptrelln.ExtractCentrelinePathCorrespondence(asketch, tsketch);

		ptrelln.realpaperscale = asketch.realpaperscale;
		assert ptrelln.realpaperscale == tsketch.realpaperscale;
		ptrelln.sketchLocOffsetFrom = asketch.sketchLocOffset;
		ptrelln.sketchLocOffsetTo = tsketch.sketchLocOffset;

// do some connected components
		// clpaths is the list of paths in the imported sketch. corrpaths is the corresponding paths in the new sketch.

		TN.emitMessage("Finished finding centerline correspondence");

		if (bcorrespsucc && (bImportSubsetsOnCentreline || bClearSubsetsOnCentreline))
		{
			for (PtrelPLn wptreli : ptrelln.wptrel)
			{
				if (bClearSubsetsOnCentreline)
					wptreli.crp.vssubsets.clear();
				if (bImportSubsetsOnCentreline)
				{
					for (String subset : wptreli.cp.vssubsets) // avoid dublicates
						if (!wptreli.crp.vssubsets.contains(subset))
							wptreli.crp.vssubsets.add(subset); 
					//wptreli.crp.vssubsets.addAll(wptreli.cp.vssubsets);
				}
			}
			TN.emitMessage("Finished copying centerline subsets");
		}

		if (!bcorrespsucc)
			TN.emitMessage("no centreline correspondence here");

		TN.emitMessage("Extending all nodes");
		ptrelln.PrepareProximity(asketch);
		ptrelln.PrepareForUnconnectedNodes(asketch.vnodes);
		ptrelln.Extendallnodes(asketch.vnodes);
		TN.emitMessage("Warping all paths");

		List<OnePath> cplist = new ArrayList<OnePath>();
		for (PtrelPLn wptreli : ptrelln.wptrel)
			cplist.add(wptreli.cp);

		// warp over all the paths from the sketch
		int lastprogress = -1;
		int i = 0;
		
		String importfromname = asketch.sketchfile.getSketchName(); 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		for (OnePath op : asketch.vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (bImportNoCentrelines || cplist.contains(op)))
				continue;
			boolean bsurvexlabel = ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.sfontcode != null) && (op.plabedl.sfontcode != null) && op.plabedl.sfontcode.equals("survey")); 
			if (bsurvexlabel)
				continue; 
//pld.sfontcode = "default";

			pthstoadd.add(ptrelln.WarpPathD(op, importfromname));
			int progress = (20*i) / asketch.vpaths.size();
			i++;
			if (progress == lastprogress)
				continue;
			lastprogress = progress;
			TN.emitMessage("" + (5*progress) + "% complete at " + (new Date()).toString());
		}

		CommitPathChanges(pthstoremove, pthstoadd); 
	}

	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it into the selected sketch in the mainbox.
	AffineTransform avgtrans = new AffineTransform();
	OneSketch asketchavglast = null; // used for lazy evaluation.
	void paintSelectedSketch(GraphicsAbstraction ga, OneSketch asketch)
	{
		// find new transform if it's a change.
		if (asketch != asketchavglast)
		{
			PtrelLn ptrelln = new PtrelLn();
			boolean bcorrespsucc = ptrelln.ExtractCentrelinePathCorrespondence(asketch, tsketch);
			if (bcorrespsucc)
				ptrelln.CalcAvgTransform(avgtrans);
            else
				avgtrans.setToIdentity();
			asketchavglast = asketch;
		}

		// now work from known transform
		ga.transform(avgtrans);

		// draw all the paths inactive.
		for (OnePath op : asketch.vpaths)
		{
			if (op.linestyle != SketchLineStyle.SLS_CENTRELINE) // of have it unhidden?
				op.paintW(ga, true, true);
		}
	}



	/////////////////////////////////////////////
	boolean IsInBlack(double fx, double fy)
	{
		int rgb = backgroundimg.backimagedone.getRGB((int)(fx + 0.5F), (int)(fy + 0.5F));
		// find intensity.
		int intens = ((rgb & 0xff) + (rgb & 0xff00) / 0x100 + (rgb & 0xff0000) / 0x10000);

		return (intens < (3 * 0x80));
	}

	/////////////////////////////////////////////
	Point2D.Float smpt0 = new Point2D.Float();
	Point2D.Float smpt1 = new Point2D.Float();

	Point2D.Float smidpt = new Point2D.Float();
	Point2D.Float midptt = new Point2D.Float();
	/////////////////////////////////////////////
	float ptlx;
	float ptly;
	float perpx;
	float perpy;
	float tracklinesidefac = 2.5F; 
	int nsampsides = 20;
	int nsampsidesmid = 30;

	boolean IsInBlack(int j)
	{
		return IsInBlack(ptlx + perpx * tracklinesidefac * j, ptly + perpy * tracklinesidefac * j);
	}

	int nmoupathpiecesleng = 15;
	/////////////////////////////////////////////
	void SetMouseLine(Point2D pt0, Point2D pt1)
	{
		moulin.setLine((pt0 == null ? moulin.getX1() : pt0.getX()), (pt0 == null ? moulin.getY1() : pt0.getY()),
					   (pt1 == null ? moulin.getX2() : pt1.getX()), (pt1 == null ? moulin.getY2() : pt1.getY()));

		// put the line into screen space.
		if (pt0 != null)
			currtrans.transform(pt0, smpt0);
		if (pt1 != null)
			currtrans.transform(pt1, smpt1);
		moulinmleng = (float)smpt0.distance(smpt1);

		nmoupathpieces = 1;
		if (sketchdisplay.miTrackLines.isSelected() && (backgroundimg.backimage != null) && ((currgenpath.linestyle != SketchLineStyle.SLS_CONNECTIVE)))
		{
			if (moulinmleng > nmoupathpiecesleng * 2)
			{
				// both endpoints should be in the black region.
				if (IsInBlack(smpt0.getX(), smpt0.getY()) && IsInBlack(smpt1.getX(), smpt1.getY()))
				{
					nmoupathpieces = Math.min(nmaxmoupathpieces, 1 + (int)(moulinmleng / nmoupathpiecesleng));
					//TN.emitMessage("npieces:" + String.valueOf(nmoupathpieces));
					// do some precalculations
					perpy = (float)(smpt1.getY() - smpt0.getY()) / moulinmleng;
					perpx = -(float)(smpt1.getX() - smpt0.getX()) / moulinmleng;
				}
			}
		}


		// work out how many pieces it will split into

		moupath.reset();
		moupath.moveTo((float)moulin.getX1(), (float)moulin.getY1());

		// loop through and find the scans on each side at all the points along the line
		float fbgapsum = 0.0F;  // for working out the average width
		int fbgapn = 0;
		int fb0 = 0; // the end ones
		int fb1 = 0;
		for (int ia = 1; ia < nmoupathpieces; ia++)
		{
			int i = (((ia % 2) == 0) ? (ia / 2) : (nmoupathpieces - (ia + 1) / 2));
			float lam = (float)i / nmoupathpieces;
			ptlx = (float)((1.0F - lam) * smpt0.getX() + lam * smpt1.getX());
			ptly = (float)((1.0F - lam) * smpt0.getY() + lam * smpt1.getY());

			// find the first black sample
			int fb = -1;
			int lnsampsides = (Math.abs(lam - 0.5F) < 0.3F ? nsampsidesmid : nsampsides);
			int fbmid = (fb0 + fb1) / 2;

			// scan outwards for the closest blackness to the centre
			for (int j = 0; j <= nsampsides; j++)
			{
				if (IsInBlack(j + fbmid))
				{
					fb = j + fbmid;
					break;
				}
				if ((j != 0) && IsInBlack(-j + fbmid))
				{
					fb = -j + fbmid;
					break;
				}
			}
			// skip this one, no black was found.
			moupiecesfblo[i] = -1;
			if (fb == -1)
				continue;

			// scan for highest and lowest black.
			int fblo = fb;
			if (fb <= 0)
			{
				while((fblo > -nsampsides) && IsInBlack(fblo - 1))
					fblo--;
			}

			int fbhi = fb;
			if (fb >= 0)
			{
				while((fbhi < nsampsides) && IsInBlack(fbhi + 1))
					fbhi++;
			}

			moupiecesfblo[i] = fblo;
			moupiecesfblo[i] = fbhi;
			fbgapsum += (fbhi - fblo);
			fbgapn++;

			if ((ia % 2) == 0)
				fb0 = fb;
			else
				fb1 = fb;
		}

		// width limit to avoid going up any perpendicular side segments
		float fbgapmax = (fbgapn != 0 ? fbgapsum / fbgapn : 0.0F) * 1.1F;

		// now rerun the array and discount sections that are too wide
		for (int i = 1; i < nmoupathpieces; i++)
		{
			if ((moupiecesfblo[i] == -1) || (moupiecesfbhi[i] - moupiecesfblo[i] > fbgapmax))
				continue;
			float lam = (float)i / nmoupathpieces;
			ptlx = (float)((1.0F - lam) * smpt0.getX() + lam * smpt1.getX());
			ptly = (float)((1.0F - lam) * smpt0.getY() + lam * smpt1.getY());

			// now set the point to the mid sample block.
			float fbm = (moupiecesfblo[i] + moupiecesfbhi[i]) / 2.0F;

//fbm = (i % nsampsides) * ((i % 2) == 0 ? 1 : -1);


			smidpt.setLocation(ptlx + perpx * tracklinesidefac * fbm, ptly + perpy * tracklinesidefac * fbm);

			try
			{
			currtrans.inverseTransform(smidpt, midptt);
			}
			catch (NoninvertibleTransformException ex)
			{;}

			moupath.lineTo((float)midptt.getX(), (float)midptt.getY());
		}


		moupath.lineTo((float)moulin.getX2(), (float)moulin.getY2());


//if (backgroundimg.backimage != null)
//	TN.emitMessage(backgroundimg.backimage.getRGB(e.getX(), e.getY()));

	}






	/////////////////////////////////////////////
	// mouse events
	/////////////////////////////////////////////

	/////////////////////////////////////////////
	void SetMPoint(MouseEvent e)
	{
		try
		{
			scrpt.setLocation(e.getX(), e.getY());
			currtrans.inverseTransform(scrpt, moupt);
		}
		catch (NoninvertibleTransformException ex)
		{
			moupt.setLocation(0, 0);
		}

		if (sketchdisplay.miSnapToGrid.isSelected() && sketchdisplay.miShowGrid.isSelected())
			sketchgrid.ClosestGridPoint(moupt, moupt.getX(), moupt.getY(), -1.0);
	}


	/////////////////////////////////////////////
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT))
			return;
		float rescalew = 0.66F;
		if (e.getWheelRotation() == -1)
			rescalew = 1.0F / rescalew;
		else if (e.getWheelRotation() != 1)
		{
			System.out.println("Unrecognized wheel rotation");
			System.out.println("   scrollamount " + e.getScrollAmount() +
							   "   getScrollType " + e.getScrollType() +
							   "   getUnitsToScroll " + e.getUnitsToScroll() +
							   "   getWheelRotation " + e.getWheelRotation());
			return;
		}


		// protect zooming too far in relation to the width of the line.  
		// it freezes if zoom out too far with thin lines.
		double plinewidth = currtrans.getScaleX() * rescalew * sketchdisplay.sketchlinestyle.strokew;
		if ((rescalew < 1.0) && (plinewidth < 0.001))
			return;
		if ((rescalew > 1.0) && (plinewidth > 100.0))
			return;

		orgtrans.setTransform(currtrans);
		mdtrans.setToIdentity();
		prevx = e.getX();
		prevy = e.getY();
		mdtrans.setToTranslation(prevx * (1.0F - rescalew), prevy * (1.0F - rescalew));
		mdtrans.scale(rescalew, rescalew);


		//System.out.println("prod " + pscale * sketchdisplay.sketchlinestyle.strokew);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);
		RedoBackgroundView();
		//System.out.println("strokew " + sketchdisplay.sketchlinestyle.strokew + "   scale " + currtrans.getScaleX() + "  " + pscale);
		repaint();
	}


	/////////////////////////////////////////////
	public void mouseMoved(MouseEvent e)
	{
		boolean bwritecoords = (sketchdisplay.bottabbedpane.getSelectedIndex() == 2);
		boolean btorepaint = false;
		if (bmoulinactive || (momotion == M_SKET_SNAPPED))
		{
			SetMPoint(e);
			if (bmoulinactive)
				SetMouseLine(null, moupt);
			if (momotion == M_SKET_SNAPPED)
				selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);

			// movement not in a drag.
			else if ((momotion != M_SKET) && sketchdisplay.miTabletMouse.isSelected() && (moulinmleng > MOVERELEASEPIX))
				EndCurve(null);

			btorepaint = true; 
		}
		else if (bwritecoords || sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct)
			SetMPoint(e);

		if (bwritecoords)
		{
			sketchdisplay.infopanel.tfmousex.setText(String.valueOf(((float)moupt.getX() / TN.CENTRELINE_MAGNIFICATION) + tsketch.sketchLocOffset.x));
			sketchdisplay.infopanel.tfmousey.setText(String.valueOf((-(float)moupt.getY() / TN.CENTRELINE_MAGNIFICATION) + tsketch.sketchLocOffset.y));
		}

		if (sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct)
		{
			sketchdisplay.selectedsubsetstruct.elevset.AlongCursorMark(elevarrow, elevpoint, moupt); 
			btorepaint = true; 
    		if (sketchdisplay.bottabbedpane.getSelectedIndex() == 4)  
                sketchdisplay.secondrender.repaint(); 
		}
		
		if (btorepaint)
			repaint();
	}
		

	public void mouseClicked(MouseEvent e) {;}
	public void mouseEntered(MouseEvent e) {;};
	public void mouseExited(MouseEvent e) {;};


	/////////////////////////////////////////////
	void BackSelUndo()
	{
		if ((currgenpath != null) && bmoulinactive)
		{
			Point2D bpt = currgenpath.BackOne();
			SetMouseLine(bpt, null);
		}

		else if (!vactivepaths.isEmpty() && (nvactivepathcomponents == -1))
			RemoveVActivePath(vactivepaths.get(vactivepaths.size() - 1));

        // very crude undo of one change -- just swaps it in.
        else
			CommitPathChanges(tsketch.pthstoaddSaved, tsketch.pthstoremoveSaved); 
	}

	/////////////////////////////////////////////
	Set<OnePath> MakeTotalSelList()
	{
		Set<OnePath> opselset = new HashSet<OnePath>(); 
		if ((currgenpath != null) && (currgenpath.pnend != null))
			opselset.add(currgenpath);
		if (currselarea != null)
		{
			for (RefPathO rpo : currselarea.refpaths)
				opselset.add(rpo.op);
			for (ConnectiveComponentAreas cca : currselarea.ccalist)
			{
				opselset.addAll(cca.vconnpaths);
				opselset.addAll(cca.vconnpathsrem); 
			}

			// not select bits of centreline that were only later allocated by z because they didn't fit match any of the areas so far
			for (int i = 0; i < currselarea.nconnpathremaining; i++)
				opselset.add(currselarea.connpathrootscen.get(i));
		}

		if ((nvactivepathcomponents != -1) && (ivactivepathcomponents != 0))
		{
			for (int i = vactivepathcomponents[ivactivepathcomponents - 1]; i < vactivepathcomponents[ivactivepathcomponents]; i++)
				opselset.add(vactivepaths.get(i)); 
		}
		else
			opselset.addAll(vactivepaths);

		return opselset; 
	}

	/////////////////////////////////////////////
	void Deselect(boolean bStrong)
	{
		if (bmoulinactive)
			EndCurve(null);
		ClearSelection(true);
	}


	/////////////////////////////////////////////
	int DAddPath(OnePath op)
	{
		op.SetSubsetAttrs(sketchdisplay.subsetpanel.sascurrent, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
		tsvpathsviz.add(op);
		tsketch.rbounds.add(op.getBounds(null));
		int res = tsketch.TAddPath(op, tsvnodesviz);
		if (sketchdisplay.selectedsubsetstruct.elevset.selevsubset != null)
		{
			sketchdisplay.selectedsubsetstruct.elevset.AddRemovePath(op, true); 
			sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		}	
		SketchChanged(SC_CHANGE_STRUCTURE);
		return res;
	}

	/////////////////////////////////////////////
	void DRemovePath(OnePath path)
	{
		tsvpathsviz.remove(path);

		if (sketchdisplay.selectedsubsetstruct.elevset.selevsubset != null)
		{
			sketchdisplay.selectedsubsetstruct.elevset.AddRemovePath(path, false); // this doesn't work if we change the linetype (eg from the centreline) before deleting 
			sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		}	

		if (tsketch.TRemovePath(path, tsvareasviz, tsvnodesviz))
		{
			SketchChanged(SC_CHANGE_STRUCTURE);
	
			boolean bupdatebicox = false; 
			if (tsketch.opframebackgrounddrag == path)
			{
				tsketch.opframebackgrounddrag = null;
				bupdatebicox = true; 
			}
			if (tsvpathsframes.remove(path))
				bupdatebicox = true; 

			if (bupdatebicox && (sketchdisplay.bottabbedpane.getSelectedIndex() == 1))
				sketchdisplay.backgroundpanel.UpdateBackimageCombobox(2); 
		}
	}


	/////////////////////////////////////////////
	void DeleteSel()
	{
		CollapseVActivePathComponent(); 
		Set<OnePath> opselset = MakeTotalSelList(); 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		for (OnePath op : opselset)
		{
			if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) || sketchdisplay.miDeleteCentrelines.isSelected())
				pthstoremove.add(op);
		}
		CommitPathChanges(pthstoremove, pthstoadd); 
	}



	/////////////////////////////////////////////
	static int SC_CHANGE_STRUCTURE = 100; 
	static int SC_CHANGE_AREAS = 101;
	static int SC_CHANGE_SYMBOLS = 102; 
	static int SC_CHANGE_PATHS = 103; 
	static int SC_CHANGE_SAS = 104; 
	static int SC_CHANGE_SAS_SYMBOLS_SAME = 106; 
	static int SC_CHANGE_BACKGROUNDIMAGE = 105; 
	static int SC_UPDATE_ZNODES = 110; 
	static int SC_UPDATE_AREAS = 111; 
	static int SC_UPDATE_SYMBOLS = 112; 
	static int SC_UPDATE_NONE = 113; 
	static int SC_UPDATE_ALL = 115;
	static int SC_UPDATE_ALL_BUT_SYMBOLS = 116; 

	// allows for calling during frame sketch updates
	// ought to be moved to close to OneSketch.UpdateSomething
	static void SketchChangedStatic(int scchangetyp, OneSketch tsketch, SketchDisplay sketchdisplay)
	{
		// case of changing the actual file which needs to be saved
		if (!tsketch.bsketchfilechanged && 
            ((scchangetyp == SC_CHANGE_STRUCTURE) || (scchangetyp == SC_CHANGE_AREAS) || 
             (scchangetyp == SC_CHANGE_SYMBOLS) || (scchangetyp == SC_CHANGE_PATHS) || (scchangetyp == SC_CHANGE_BACKGROUNDIMAGE)))
		{
			sketchdisplay.mainbox.tunnelfilelist.repaint();
			tsketch.bsketchfilechanged = true;
		}
		if (scchangetyp == SC_CHANGE_SAS)
			scchangetyp = SC_CHANGE_SYMBOLS; 

		if (scchangetyp == SC_CHANGE_STRUCTURE)
			tsketch.bZonnodesUpdated = false; 
		else if (scchangetyp == SC_UPDATE_ZNODES)
			tsketch.bZonnodesUpdated = true; 
			
		if ((scchangetyp == SC_CHANGE_STRUCTURE) || (scchangetyp == SC_CHANGE_AREAS) || (scchangetyp == SC_UPDATE_ZNODES))
			tsketch.bSAreasUpdated = false;
		else if (scchangetyp == SC_UPDATE_AREAS)
			tsketch.bSAreasUpdated = true;

		if ((scchangetyp == SC_CHANGE_STRUCTURE) || (scchangetyp == SC_CHANGE_AREAS) || 
            (scchangetyp == SC_CHANGE_SYMBOLS) || (scchangetyp == SC_UPDATE_AREAS))
			tsketch.bSymbolLayoutUpdated = false;
		else if (scchangetyp == SC_UPDATE_SYMBOLS)
			tsketch.bSymbolLayoutUpdated = true;

		if (sketchdisplay != null)
		{
			sketchdisplay.acaSetZonnodes.setEnabled(!tsketch.bZonnodesUpdated);
			sketchdisplay.acaUpdateSAreas.setEnabled(!tsketch.bSAreasUpdated);
			sketchdisplay.acaUpdateSymbolLayout.setEnabled(!tsketch.bSymbolLayoutUpdated);
		}
	}

	/////////////////////////////////////////////
	void SketchChanged(int scchangetyp)
	{
		SketchChangedStatic(scchangetyp, tsketch, sketchdisplay); 
	}

	/////////////////////////////////////////////
	void UpdateZNodes()
	{
		tsketch.UpdateSomething(SC_UPDATE_ZNODES, true); 
		SketchChanged(SC_UPDATE_ZNODES);
	}
	
	/////////////////////////////////////////////
	void UpdateSAreas()
	{
		tsketch.UpdateSomething(SC_UPDATE_AREAS, true); 
		sketchdisplay.mainbox.UpdateSketchFrames(tsketch, SketchGraphics.SC_UPDATE_NONE); 
		SketchChanged(SC_UPDATE_AREAS);
		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStringsT(sketchdisplay.selectedsubsetstruct.elevset.selevsubset, tsketch);  // put back the same subset (hack!)
		RedoBackgroundView();
	}

    class MakeSymbLayout implements Runnable
    {
        OneSketch tsketch; 
        JProgressBar visiprogressbar; 
        MakeSymbLayout(OneSketch ltsketch, JProgressBar lvisiprogressbar)
        {
            tsketch = ltsketch; 
            visiprogressbar = lvisiprogressbar; 
        }
        public void run() 
        {
			boolean ballsymbolslayed = tsketch.MakeSymbolLayout(null, null, visiprogressbar); 
        }
    }

	/////////////////////////////////////////////
	void UpdateSymbolLayout(boolean bAllSymbols, JProgressBar visiprogressbar)
	{
        visiprogressbar.setString("symbols");
        visiprogressbar.setStringPainted(true);
		boolean ballsymbolslayed = false; 
//		if (bAllSymbols)
//			ballsymbolslayed = tsketch.MakeSymbolLayout(null, null, visiprogressbar); 
//		else
//			ballsymbolslayed = tsketch.MakeSymbolLayout(new GraphicsAbstraction(mainGraphics), windowrect, visiprogressbar);
        Thread t = new Thread(new MakeSymbLayout(tsketch, visiprogressbar));
        t.start();

		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStringsT(sketchdisplay.selectedsubsetstruct.elevset.selevsubset, tsketch);
		if (ballsymbolslayed)
			SketchChanged(SC_UPDATE_SYMBOLS);
		RedoBackgroundView();
    //    visiprogressbar.setValue(0);
    //    visiprogressbar.setStringPainted(false);
	}

	/////////////////////////////////////////////
	void FuseNodesS(List<OnePath> pthstoremove, List<OnePath> pthstoadd, OnePathNode wpnstart, OnePathNode wpnend, OnePath opexcl1, OnePath opexcl2, boolean bShearWarp)
	{
		// find all paths that link into the first node and warp them to the second.
		// must be done backwards due to messing of the array
		for (int i = tsketch.vpaths.size() - 1; i >= 0; i--)
		{
			OnePath op = tsketch.vpaths.get(i);
			if (((op.pnstart == wpnstart) || (op.pnend == wpnstart)) && ((op != opexcl1) && (op != opexcl2))) 
			{
				pthstoremove.add(op);
				ElevWarpPiece ewp = new ElevWarpPiece(wpnstart, wpnend, op, (bShearWarp ? ElevWarpPiece.WARP_SHEARWARP : ElevWarpPiece.WARP_NORMALWARP)); 
				OnePath opw = ewp.WarpPathS(op);
				pthstoadd.add(opw);
			}
		}

		// copy over the altitude of this node
		if (wpnstart.IsCentrelineNode())
		{
			//assert wpnend.IsCentrelineNode();
			//assert wpnend.pnstationlabel.equals(wpnstart.pnstationlabel);
			wpnend.zalt = wpnstart.zalt;
		}
	}


	/////////////////////////////////////////////
    // should be able to in-line the DAddPath and DRemovePaths to this function
    // then commit onto an undo stack in the sketch
	boolean CommitPathChanges(List<OnePath> pthstoremove, List<OnePath> pthstoadd)
	{
        TN.emitMessage("Committing to delete " + (pthstoremove == null ? 0 : pthstoremove.size()) + " paths and add " + (pthstoadd == null ? 0 : pthstoadd.size()) + " paths"); 

		ClearSelection(true);
		if (!bEditable)
			return false; 
        
        // the single undo element (which cycles automatically when we reverse the two in the BackSelUndo function)
        tsketch.pthstoremoveSaved = pthstoremove; 
        tsketch.pthstoaddSaved = pthstoadd; 

		if (pthstoremove != null)
		{
			for (OnePath op : pthstoremove)
			{
				assert tsketch.vpaths.contains(op);
				assert (pthstoadd == null) || !pthstoadd.contains(op); 
				DRemovePath(op);
			}
		}
		if (pthstoadd != null)
		{
			for (OnePath op : pthstoadd)
			{
				assert !tsketch.vpaths.contains(op);
				DAddPath(op);
				if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null))  
					op.UpdateStationLabelsFromCentreline();
			}
		}

		SketchChanged(SC_CHANGE_STRUCTURE);
		RedrawBackgroundView();

		if ((pthstoadd != null) && (pthstoadd.size() == 1))
		{
			currgenpath = pthstoadd.get(0);
			ObserveSelection(currgenpath, null, 6);
		}
		// else select everything into the vactivepaths 

		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);
		return true; 
	}

	/////////////////////////////////////////////
	OnePathNode TranslatedNode(OnePathNode opn, List<OnePathNode> pthnodestomove, List<OnePathNode> pthnodesmoved, double vx, double vy)
	{
		assert pthnodestomove.size() == pthnodesmoved.size(); 
		int i = pthnodestomove.indexOf(opn); 
		if (i != -1)
			return pthnodesmoved.get(i); 
		OnePathNode res = new OnePathNode((float)(opn.pn.getX() + vx), (float)(opn.pn.getY() + vy), opn.zalt); 
		pthnodestomove.add(opn); 
		pthnodesmoved.add(res); 
		return res; 
	}


	/////////////////////////////////////////////
	boolean FuseTranslate(OnePath lcurrgenpath)
	{
		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		pthstoremove.addAll(vactivepaths); 
		List<OnePathNode> pthnodestomove = new ArrayList<OnePathNode>(); 
		List<OnePathNode> pthnodesmoved = new ArrayList<OnePathNode>(); // parallel array
		double vx = lcurrgenpath.pnend.pn.getX() - lcurrgenpath.pnstart.pn.getX();
		double vy = lcurrgenpath.pnend.pn.getY() - lcurrgenpath.pnstart.pn.getY();
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		for (OnePath op : pthstoremove)
		{
			if (op == lcurrgenpath)
				continue; 
			OnePathNode nopnstart = TranslatedNode(op.pnstart, pthnodestomove, pthnodesmoved, vx, vy); 
			OnePathNode nopnend = TranslatedNode(op.pnend, pthnodestomove, pthnodesmoved, vx, vy); 
			float[] pco = op.GetCoords();
			OnePath nop = new OnePath(nopnstart); 
			for (int i = 1; i < op.nlines; i++)
				nop.LineTo((float)(pco[i * 2] + vx), (float)(pco[i * 2 + 1] + vy)); 
			nop.EndPath(nopnend); 
			nop.CopyPathAttributes(op); 
			pthstoadd.add(nop); 
		}
		CommitPathChanges(pthstoremove, pthstoadd); 
System.out.println("Do fuse translate"); 
		return true; 
	}

	/////////////////////////////////////////////
	boolean FuseTwoEdges(OnePath op1, OnePath op2)
	{
		// work out node connection.
		OnePathNode pnconnect = null;
		if ((op1.pnend == op2.pnstart) || (op1.pnend == op2.pnend))
			pnconnect = op1.pnend;
		else if ((op1.pnstart == op2.pnstart) || (op1.pnstart == op2.pnend))
			pnconnect = op1.pnstart;
		else
			return TN.emitWarning("Must have connecting paths");

		// decide whether to fuse if properties agree
		if ((pnconnect == null) || (pnconnect.pathcount != 2) || (op1.linestyle != op2.linestyle) || (op1.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return TN.emitWarning("Fusing paths must agree");

		OnePath opf = op1.FuseNode(pnconnect, op2);

		opf.vssubsets.addAll(op1.vssubsets);

		// add without duplicates
		for (String ssub : op2.vssubsets)
		{
			if (!opf.vssubsets.contains(ssub))
				opf.vssubsets.add(ssub); 
		}

		// just runs in parallel, duplicates don't matter
		opf.vssubsetattrs.addAll(op1.vssubsetattrs);
		opf.vssubsetattrs.addAll(op2.vssubsetattrs);
		opf.bpathvisiblesubset = (op1.bpathvisiblesubset || op2.bpathvisiblesubset);

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		// delete this warped path
		pthstoremove.add(op1);
		pthstoremove.add(op2);
		pthstoadd.add(opf);
		return CommitPathChanges(pthstoremove, pthstoadd); 
	}

	/////////////////////////////////////////////
	boolean FuseAlongSingleEdgeElevation(List<OnePath> elevcenconn, OnePath warppath)
	{
		System.out.println("asdasda " + elevcenconn.size()); 
		ElevWarp elevwarp = new ElevWarp(elevcenconn, tsketch.vpaths); 
		elevwarp.MakeWarpPathPieceMap(warppath); 
		elevwarp.MakeWarpPathNodeslists(); 

		// delete this fused path
		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		elevwarp.WarpAllPaths(pthstoremove, pthstoadd, warppath); 

		//sketchdisplay.selectedsubsetstruct.FuseNodesElevation(warppath.pnstart, warppath.pnend); 
		// this will fuse a whole bunch of pieces
		assert !warppath.pnstart.IsCentrelineNode(); 

		assert pthstoadd.size() == pthstoremove.size(); 
		pthstoremove.add(warppath); 
		CommitPathChanges(pthstoremove, pthstoadd); 
		assert warppath.pnstart.pathcount == 0; // should have been removed

		return true; 
	}

	/////////////////////////////////////////////
	boolean FuseCurrent(boolean bShearWarp)
	{
		// FuseTranslate situation
		if ((nvactivepathcomponents != -1) && (ivactivepathcomponents == 0) && (icurrgenvactivepath != -1) && !bmoulinactive)
		{
			OnePath lcurrgenpath = vactivepaths.get(icurrgenvactivepath); 
			if ((lcurrgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE) || (lcurrgenpath.nlines != 1) || 
				(lcurrgenpath.pnend.pathcount != 1) || (lcurrgenpath.pnstart.pathcount == 1))
				return TN.emitWarning("Can only fuse-translate single path with simple connections"); 
			return 	FuseTranslate(lcurrgenpath); 
		}
	
		CollapseVActivePathComponent(); 
		if (vactivepaths.size() >= 3)
			return TN.emitWarning("Can't fuse three paths");

		// fuse two edges (in a single selected chain)
		if (vactivepaths.size() == 2)
			return FuseTwoEdges(vactivepaths.get(0), vactivepaths.get(1)); 

		// fuse along a single edge
		else
		{
			if (bmoulinactive)
				return TN.emitWarning("Can't fuse active path");

			// the path to warp along
			OnePath warppath; 
			if ((vactivepaths.size() == 1) && (currgenpath == null))
				warppath = vactivepaths.get(0); 
			else if (currgenpath != null)
				warppath = currgenpath; 
			else
				return TN.emitWarning("Must have single path selected for fuse"); 
			
			if ((warppath.nlines > 1) || (warppath.linestyle == SketchLineStyle.SLS_CENTRELINE) || (warppath.pnstart == currgenpath.pnend))
				return TN.emitWarning("Must have straight non-centreline line selected");
			if (warppath.pnstart.IsCentrelineNode() && warppath.pnend.IsCentrelineNode())
				return TN.emitWarning("Can't fuse two centreline nodes");

			// the default fusing is engaged by making it a ceiling boundary
			List<OnePath> elevcenconn = sketchdisplay.selectedsubsetstruct.IsElevationNode(warppath.pnstart); 
			if ((elevcenconn != null) && (warppath.linestyle != SketchLineStyle.SLS_CEILINGBOUND))
			{
				return FuseAlongSingleEdgeElevation(elevcenconn, warppath); 
			}
			else
			{
				List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
				List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
				FuseNodesS(pthstoremove, pthstoadd, warppath.pnstart, warppath.pnend, warppath, null, bShearWarp);
				pthstoremove.add(warppath); 
				return CommitPathChanges(pthstoremove, pthstoadd); 
			}
		}
	}


	/////////////////////////////////////////////
	// and finds the components which the selection separates
	boolean SelectConnectedSetsFromSelection()
	{
		// cycle through the components
		if (nvactivepathcomponents != -1)
		{
			ivactivepathcomponents = (ivactivepathcomponents + 1 == nvactivepathcomponents ? 0 : ivactivepathcomponents + 1); 
System.out.println("ivactivepathcomponents " + ivactivepathcomponents); 
			return true; 
		}
		OnePath lcurrgenpath = (((currgenpath != null) && (currgenpath.pnend != null)) ? currgenpath : null);

		CollapseVActivePathComponent(); 
		Set<OnePath> vpathssel = MakeTotalSelList(); 
		if (vpathssel.isEmpty())
			return TN.emitWarning("Must have something selected"); 

		Set<OnePathNode> vpathnodessel = new HashSet<OnePathNode>(); 
		for (OnePath op : vpathssel)
		{
			vpathnodessel.add(op.pnstart); 
			vpathnodessel.add(op.pnend); 
		}

		Set<OnePath> vpathsoffsel = new HashSet<OnePath>(); 
		RefPathO srefpathconn = new RefPathO();
		for (OnePathNode opn : vpathnodessel)
		{
			srefpathconn.ccopy(opn.ropconn);
			do
			{
				if (!vpathssel.contains(srefpathconn.op))
					vpathsoffsel.add(srefpathconn.op); 
			}
			while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
		}

		List< Set<OnePath> > vpathscomponents = new ArrayList< Set<OnePath> >(); 
		Set<OnePath> vpathscomponentsiunion = new HashSet<OnePath>(); 
		List<OnePathNode> vpathnodesstack = new ArrayList<OnePathNode>();
		Set<OnePathNode> vpathnodeschecked = new HashSet<OnePathNode>();
		for (OnePath op : vpathsoffsel)
		{
			if (vpathscomponentsiunion.contains(op))
				continue; 

			assert vpathnodesstack.isEmpty() && vpathnodeschecked.isEmpty(); 
			Set<OnePath> vpathscomponent = new HashSet<OnePath>(); 
			vpathscomponent.add(op); 
			if (!vpathnodessel.contains(op.pnstart))
				vpathnodesstack.add(op.pnstart); 
			if (!vpathnodessel.contains(op.pnend))
				vpathnodesstack.add(op.pnend); 
			
			while (!vpathnodesstack.isEmpty())
			{
				OnePathNode opn = vpathnodesstack.remove(vpathnodesstack.size() - 1);
				srefpathconn.ccopy(opn.ropconn);
				vpathnodeschecked.add(opn); 
				do
				{
					assert !vpathscomponents.contains(srefpathconn.op); 
					vpathscomponent.add(srefpathconn.op); 
					OnePathNode fopn = srefpathconn.FromNode(); 
					if (!vpathnodessel.contains(fopn) && !vpathnodeschecked.contains(fopn))
						vpathnodesstack.add(fopn); 
				}
				while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
			}
			vpathnodeschecked.clear(); 

			vpathscomponents.add(vpathscomponent); 
			vpathscomponentsiunion.addAll(vpathscomponent); 
		}

		ClearSelection(true); 
		if (vpathscomponents.size() + 2 > vactivepathcomponents.length) 
			vactivepathcomponents = new int[vpathscomponents.size() + 10]; 
		assert vactivepaths.isEmpty();

		vactivepathcomponents[0] = 0; 
		nvactivepathcomponents = 1; 
		for (Set<OnePath> vpathscomponent : vpathscomponents)
		{
			vactivepaths.addAll(vpathscomponent); 
			vactivepathcomponents[nvactivepathcomponents] = vactivepaths.size(); 
			nvactivepathcomponents++; 
		}
		vactivepaths.addAll(vpathssel); 
		vactivepathcomponents[nvactivepathcomponents] = vactivepaths.size(); 
		nvactivepathcomponents++; 
		ivactivepathcomponents = 0; // signifies everything selected
		vactivepathsnodecounts.clear(); // not useful here
		icurrgenvactivepath = (lcurrgenpath != null ? vactivepaths.indexOf(lcurrgenpath) : -1); 
System.out.println("nvactivepathcomponentsnvactivepathcomponents " + nvactivepathcomponents); 

		//vactivepathsnodecounts.add(path.pnstart); 
		//vactivepathsnodecounts.add(path.pnend); 
		//Collections.sort(vactivepathsnodecounts); 
		return true; 
	}


	/////////////////////////////////////////////
	boolean ReflectCurrent()
	{
		// cases for throwing out the individual edge
		if (!vactivepaths.isEmpty() || (currgenpath == null) || bmoulinactive || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return TN.emitWarning("Can only reflect single selective path that's not a centreline"); 

		OnePath nop = new OnePath(currgenpath.pnend); 
		float[] pco = currgenpath.GetCoords();
		for (int i = currgenpath.nlines - 1; i >= 1; i--)
			nop.LineTo(pco[i * 2], pco[i * 2 + 1]); 
		nop.EndPath(currgenpath.pnstart); 
		nop.CopyPathAttributes(currgenpath); 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoremove.add(currgenpath); 
		pthstoadd.add(nop); 
		return CommitPathChanges(pthstoremove, pthstoadd); 
	}


	/////////////////////////////////////////////
	// if the current selection is a line segment then we make it a centreline type.
	void SetAsAxis()
	{
		if (!tsketch.bSymbolType)
			TN.emitWarning("Set axis only valid for symbol type.");

		if (!bmoulinactive && (currgenpath != null) && (currgenpath.nlines == 1))
		{
			OnePath apath = tsketch.GetAxisPath();
			if (apath != null)
				apath.linestyle = SketchLineStyle.SLS_DETAIL;
			currgenpath.linestyle = SketchLineStyle.SLS_CENTRELINE;
			TN.emitMessage("Axis Set");
			Deselect(true);
			RedrawBackgroundView();
		}
	}



	/////////////////////////////////////////////
	boolean MakePitchUndercut()
	{
		if (bmoulinactive || (currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_PITCHBOUND))
			return TN.emitWarning("Pitch undercut must have pitch boundary selected.");

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		OnePath opddconnstart = currgenpath.pnstart.GetDropDownConnPath();
		if (opddconnstart.pnend.pathcount == 0)
		{
			opddconnstart.vssubsets.addAll(currgenpath.vssubsets);
			pthstoadd.add(opddconnstart);
		}

		OnePath opddconnend = currgenpath.pnend.GetDropDownConnPath();
		if (opddconnend.pnend.pathcount == 0)
		{
			opddconnend.vssubsets.addAll(currgenpath.vssubsets);
			pthstoadd.add(opddconnend);
		}

		// now make the invisible line
		OnePath opinv = new OnePath();
		opinv.pnstart = opddconnstart.pnend;
		opinv.pnend = opddconnend.pnend;
		opinv.linestyle = SketchLineStyle.SLS_INVISIBLE;
		opinv.vssubsets.addAll(currgenpath.vssubsets);
		opinv.gp = (GeneralPath)currgenpath.gp.clone();
		opinv.nlines = currgenpath.nlines;
		opinv.linelength = currgenpath.linelength;
		opinv.bSplined = currgenpath.bSplined;
		opinv.bWantSplined = currgenpath.bWantSplined;
		pthstoadd.add(opinv);

		return CommitPathChanges(pthstoremove, pthstoadd); 
	}




	/////////////////////////////////////////////
	void ClearSelection(boolean bupdatepathparameters)
	{
		if (bupdatepathparameters)
			sketchdisplay.sketchlinestyle.GoSetParametersCurrPath(); // this copies over anything that was missed

		currgenpath = null;
		currselarea = null;
		vactivepaths.clear();
		vactivepathsnodecounts.clear(); 
		nvactivepathcomponents = -1; 
		ivactivepathcomponents = -1; 
		bmoulinactive = false; // newly added
		ObserveSelection(null, null, 7);
		repaint();
	}

	/////////////////////////////////////////////
	void CollapseVActivePathComponent()
	{
		if ((nvactivepathcomponents != -1) && (ivactivepathcomponents != 0))
		{
			// would like to do this with two remove range functions, but don't have the docs on this machine
			int a = vactivepathcomponents[ivactivepathcomponents - 1]; 
			int b = vactivepathcomponents[ivactivepathcomponents]; 
			for (int i = a; i < b; i++)
				vactivepaths.set(i - a, vactivepaths.get(i)); 
			while (vactivepaths.size() > b - a)
				vactivepaths.remove(vactivepaths.size() - 1); 
		}
		if (nvactivepathcomponents != -1)
		{
			for (OnePath op : vactivepaths)
			{
				vactivepathsnodecounts.add(op.pnstart); 
				vactivepathsnodecounts.add(op.pnend); 
			}
			Collections.sort(vactivepathsnodecounts); 
			nvactivepathcomponents = -1; 
		}
	}

	/////////////////////////////////////////////
	void RemoveVActivePath(OnePath path)
	{
		assert vactivepaths.contains(path); 
		vactivepaths.remove(path); 
		vactivepathsnodecounts.remove(path.pnstart); 
		vactivepathsnodecounts.remove(path.pnend); 
	}

	/////////////////////////////////////////////
	void AddVActivePath(OnePath path)
	{
		vactivepaths.add(path);
		vactivepathsnodecounts.add(path.pnstart); 
		vactivepathsnodecounts.add(path.pnend); 
		Collections.sort(vactivepathsnodecounts); 
	}

	/////////////////////////////////////////////
	// works from the known values in the class to break the current path
	boolean SplitCurrpathNode()
	{
		OnePath nop1 = new OnePath(currgenpath.pnstart); 
		float[] pco = currgenpath.GetCoords();
		for (int i = 1; i < linesnap_t; i++)  // < on the float simulates ceil
			nop1.LineTo(pco[i * 2], pco[i * 2 + 1]); 
		nop1.EndPath(currpathnode); 
		nop1.CopyPathAttributes(currgenpath); 

		OnePath nop2 = new OnePath(currpathnode); 
		for (int i = (int)(linesnap_t + 1.0); i < currgenpath.nlines; i++)
			nop2.LineTo(pco[i * 2], pco[i * 2 + 1]); 
		nop2.EndPath(currgenpath.pnend); 
		nop2.CopyPathAttributes(currgenpath); 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoremove.add(currgenpath); 
		pthstoadd.add(nop1); 
		pthstoadd.add(nop2); 
		return CommitPathChanges(pthstoremove, pthstoadd); 
	}

	/////////////////////////////////////////////
	public void Scale(float sca)
	{
		// set the pre transformation
		mdtrans.setToTranslation(csize.width / 2, csize.height / 2);
		mdtrans.scale(sca, sca);
		mdtrans.translate(-csize.width / 2, -csize.height / 2);

		orgtrans.setTransform(currtrans);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	public void Translate(float xprop, float yprop)
	{
		// set the pre transformation
		mdtrans.setToTranslation(csize.width * xprop, csize.height * yprop);

		orgtrans.setTransform(currtrans);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		RedoBackgroundView();
	}



	/////////////////////////////////////////////
	/////////////////////////////////////////////



	/////////////////////////////////////////////
	// dragging out a curve
	/////////////////////////////////////////////
	void StartCurve(OnePathNode pnstart)
	{
		currgenpath = new OnePath(pnstart);
		sketchdisplay.sketchlinestyle.SetParametersFromBoxes(currgenpath);
		SetMouseLine(pnstart.pn, moupt);
		bmoulinactive = true;
	}

	/////////////////////////////////////////////
	void LineToCurve()
	{
		if (moulinmleng != 0)
		{
			currgenpath.IntermedLines(moupath, nmoupathpieces);
			currgenpath.LineTo((float)moupt.getX(), (float)moupt.getY());
			SetMouseLine(moupt, moupt);
		}
	}


	/////////////////////////////////////////////
	void EndCurve(OnePathNode pnend)
	{
		bmoulinactive = false;
		momotion = M_NONE;
		if (currgenpath.EndPath(pnend))
		{
			List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
			pthstoadd.add(currgenpath);

            // automatically add to subsets
            if (sketchdisplay.miAutoAddToSubset.isSelected() || sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct)
	        {
		        for (String ssub : sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP)
			        sketchdisplay.subsetpanel.PutToSubset(currgenpath, ssub, true);
        	}
			CommitPathChanges(null, pthstoadd); 
		}
		else
		{
			currgenpath = null;
		}
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	double linesnap_t = -1.0; // records the location of splitting.
	Point2D clpt = new Point2D.Double();

	/////////////////////////////////////////////
	public void mousePressedDragview(MouseEvent e)
	{
		// if a point is already being dragged, then this second mouse press will delete it.
		if ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT))
		{
			momotion = M_NONE;
			currtrans.setTransform(orgtrans);
			RedoBackgroundView();
			return;
		}
		orgtrans.setTransform(currtrans);
//			backgroundimg.orgparttrans.setTransform(backgroundimg.currparttrans);
		mdtrans.setToIdentity();
		prevx = e.getX();
		prevy = e.getY();

		if (!e.isMetaDown())
		{
			if (e.isShiftDown())
				momotion = M_DYN_DRAG;
			else if (e.isControlDown())
				momotion = M_DYN_SCALE;
			else  if (sketchdisplay.miEnableRotate.isSelected())
				momotion = M_DYN_ROT;
			else
				momotion = M_DYN_DRAG; // was M_NONE
		}
	}

	/////////////////////////////////////////////
	public void mousePressedCtrlUp(MouseEvent e)
	{
		SetMPoint(e);

		// ending a path
		if (e.isShiftDown() && bmoulinactive)
		{
			LineToCurve();
			EndCurve(null);
		}

		// here is where we can toggle the requirement that the shift key is held down to start a path
		if (!e.isShiftDown() && !bmoulinactive)
		{
			ClearSelection(true);
			OnePathNode opns = new OnePathNode((float)moupt.getX(), (float)moupt.getY(), GetMidZsel());
			opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
			StartCurve(opns);
		}
		if (e.isShiftDown() && bmoulinactive)
		{
			LineToCurve();
			EndCurve(null);
		}
		if (!e.isShiftDown() && bmoulinactive)
		{
			momotion = M_SKET;
			LineToCurve();
		}

		repaint();
	}

	/////////////////////////////////////////////
	public void mousePressedEndAndStartPath(MouseEvent e)
	{
		LineToCurve();
		EndCurve(null);
		OnePathNode opns = currgenpath.pnend;
		ClearSelection(true);
		StartCurve(opns);
		repaint();
	}

	/////////////////////////////////////////////
	public void mousePressedSnapToNode(MouseEvent e)
	{
		SetMPoint(e);
		momotion = M_SKET_SNAP;
		linesnap_t = -1.0;
		selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);
		repaint();
	}

	/////////////////////////////////////////////
	public void mousePressedSplitLine(MouseEvent e)
	{
		SetMPoint(e);
		// the node splitting one. only on edges if shift is down(over-ride with shift down)
		double scale = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
		linesnap_t = currgenpath.ClosestPoint(moupt.getX(), moupt.getY(), 5.0 / scale);
		if ((currgenpath.linestyle != SketchLineStyle.SLS_CENTRELINE) && (linesnap_t != -1.0) && (linesnap_t > 0.0) && (linesnap_t < currgenpath.nlines))
		{
			currgenpath.Eval(clpt, null, linesnap_t);
			selpathnode = new OnePathNode((float)clpt.getX(), (float)clpt.getY(), GetMidZsel());
			selpathnode.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
			SetMouseLine(clpt, clpt);
			momotion = M_SKET_SNAPPED;
			repaint();
		}

		// failed to split -- get no mode.
		else
			momotion = M_NONE;
	}


	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)
	{
		//TN.emitMessage("  " + e.getModifiers() + " " + e.getModifiersEx() + "-" + (e.getModifiersEx() & MouseEvent.BUTTON2_MASK) + " " + MouseEvent.BUTTON2_MASK);
		//TN.emitMessage("B1 " + e.BUTTON1_MASK + " B2 " + e.BUTTON2_MASK + " B3 " + e.BUTTON3_MASK + " ALT " + e.ALT_MASK + " META " + e.META_MASK + " MetDown " + e.isMetaDown());

		// are we in the whole picture dragging mode?  (middle mouse button).
		// (if we click another mouse button while holding the middle mouse button down, we will still get in here)
		if (((e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) || e.isAltDown())  // altdown means alt-key gets you there too.
			mousePressedDragview(e);

		// right mouse button
		else if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0)
		{
			// selecting a path
			if (!bmoulinactive)
			{
				momotion = (e.isShiftDown() ? M_SEL_AREA : (e.isControlDown() ? M_SEL_PATH_ADD : M_SEL_PATH));
				selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);
				repaint(); // to activate the hit command.
			}
		}

		// bail out non-editable cases
		else if (!bEditable)
			;

		// left mouse button
		else if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			; // impossible

		// there's going to be a very special case with sket_snap.
		else if (!e.isControlDown())
			mousePressedCtrlUp(e);

		else if (!e.isShiftDown())
			mousePressedSnapToNode(e);

		else if (currgenpath == null)
			;

		// shift and control held down
		else if (!bmoulinactive)
			mousePressedSplitLine(e);

		// end and continue node at same time
		else if (bmoulinactive)
			mousePressedEndAndStartPath(e);
	}


	/////////////////////////////////////////////
    public void mouseDragged(MouseEvent e)
	{
		switch (momotion)
		{
		case M_DYN_DRAG:
		{
			int xv = e.getX() - prevx;
			int yv = e.getY() - prevy;
			mdtrans.setToTranslation(xv, yv);
			break;
		}
		case M_DYN_SCALE:
		{
			int x = e.getX();
			int y = e.getY();
			float rescalex, rescaley;
			if (false)  // non-uniform scaling
			{
				rescalex = 1.0F + ((float)Math.abs(x - prevx) / csize.width) * 2.0F;
				if (x < prevx)
					rescalex = 1.0F / rescalex;
				rescaley = 1.0F + ((float)Math.abs(y - prevy) / csize.height) * 2.0F;
				if (y < prevy)
					rescaley = 1.0F / rescaley;
			}

			// uniform scaling in both axes when not doing background.
			else
			{
				int w = (x - prevx) + (prevy - y); // allow drawing in either direction
				float wfac = Math.max(csize.width, csize.height);
				float rescalew = 1.0F + ((float)Math.abs(w) / wfac) * 2.0F;
				if (w < 0)
					rescalew = 1.0F / rescalew;
				rescalex = rescalew;
				rescaley = rescalew;
			}


			mdtrans.setToTranslation(prevx * (1.0F - rescalex), prevy * (1.0F - rescaley));
			mdtrans.scale(rescalex, rescaley);

			break;
		}

		case M_DYN_ROT:
		{
			int vy = e.getY() - prevy;
			mdtrans.setToRotation((float)vy / csize.height, csize.width / 2, csize.height / 2);

			break;
		}

		case M_SKET:
			mouseMoved(e);
			// simulates a lot of clicking
			if (sketchdisplay.miTabletMouse.isSelected() && bmoulinactive)
				LineToCurve();
			return;

		case M_SKET_SNAP:
		case M_SKET_SNAPPED:
			mouseMoved(e);
			return;

		case M_NONE:
			mouseMoved(e);
			return;
		default:
			return;
		}

		// the dynamic drag type things.
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		RedoBackgroundView();
	}


	/////////////////////////////////////////////
    public void mouseReleased(MouseEvent e)
	{
		mouseDragged(e);

		// in this mode things happen on release.
		if ((momotion == M_SKET_SNAP) || (momotion == M_SKET_SNAPPED))
		{
			// start of path
			if (!bmoulinactive)
			{
				if (currpathnode != null)
				{
					// splitnode
					if ((currgenpath != null) && (linesnap_t != -1.0))
						SplitCurrpathNode();
					ClearSelection(true);
					StartCurve(currpathnode);
					repaint();
				}
			}
			else
			{
				// end of path
				if (currpathnode != null)
				{
					if (moulinmleng != 0)
						currgenpath.IntermedLines(moupath, nmoupathpieces);  // handle any tracking of the drawing
					EndCurve(currpathnode);
					repaint();
				}
			}
		}

		else if ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT))
		{
			RedoBackgroundView();
		}

		momotion = M_NONE;
		repaint();
	}

	/////////////////////////////////////////////
	void FrameBackgroundOutline()
	{
System.out.println("   WWWWWW  setting FrameBackgroundOutline"); 
		// only to closed loops
		if (tsketch.opframebackgrounddrag.pnstart != tsketch.opframebackgrounddrag.pnend)
			return; 
		if (!tsketch.opframebackgrounddrag.plabedl.sketchframedef.IsImageType())
			return;
		OnePath fop = tsketch.opframebackgrounddrag; 
		OnePathNode fopn = fop.pnstart; 

		// check if all paths are non-connective
		RefPathO srefpathconn = new RefPathO();
		int nvpaths = 0; 
		srefpathconn.ccopy(fopn.ropconn);
		do
		{
			OnePath lop = srefpathconn.op;
			assert lop != null; 
			if (lop == fop)
				;
			else if ((lop.linestyle != SketchLineStyle.SLS_CONNECTIVE) || ((lop.plabedl != null) && (lop.plabedl.barea_pres_signal != SketchLineStyle.ASE_ZSETRELATIVE) && (lop.plabedl.barea_pres_signal != SketchLineStyle.ASE_KEEPAREA)))  
				nvpaths = -1; 
			else if (nvpaths != -1)
				nvpaths++; 
		}
		while (!srefpathconn.AdvanceRoundToNode(fopn.ropconn));

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		OnePath gop = fop.plabedl.sketchframedef.MakeBackgroundOutline(1.0, tsketch.sketchLocOffset); 
		gop.CopyPathAttributes(fop);

System.out.println("NNNN  " + nvpaths + "  " + gop.nlines); 

		// bring over any paths connecting to this rectangle where we have moved it
		if (nvpaths >= 1)
			FuseNodesS(pthstoremove, pthstoadd, fopn, gop.pnstart, fop, null, sketchdisplay.miShearWarp.isSelected()); 
		
		pthstoremove.add(fop); 
		pthstoadd.add(gop); 

		CommitPathChanges(pthstoremove, pthstoadd); 

		tsketch.opframebackgrounddrag = gop; 
		sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(gop, true);
		if (sketchdisplay.bottabbedpane.getSelectedIndex() == 1)
			sketchdisplay.backgroundpanel.UpdateBackimageCombobox(4); 
	}


	/////////////////////////////////////////////
	OnePath MakeConnectiveLineForData(int cldtype)
	{
		double x0, y0; 
		double x1, y1; 
		double x2, y2; 
		double x3, y3; 
		assert currgenpath == null; 
		assert (cldtype == 0) || (cldtype == 1);  // 0 is image loop;  1 is survex data
		OnePath gop; 
		try
		{
			if (cldtype == 0)
			{
				scrpt.setLocation(30, 20);
				currtrans.inverseTransform(scrpt, moupt);
				x0 = moupt.getX();  y0 = moupt.getY(); 
				scrpt.setLocation(80, 20);
				currtrans.inverseTransform(scrpt, moupt);
				x1 = moupt.getX();  y1 = moupt.getY(); 
				scrpt.setLocation(80, 40);
				currtrans.inverseTransform(scrpt, moupt);
				x2 = moupt.getX();  y2 = moupt.getY(); 
				scrpt.setLocation(30, 40);
				currtrans.inverseTransform(scrpt, moupt);
				x3 = moupt.getX();  y3 = moupt.getY(); 

				OnePathNode opns = new OnePathNode((float)x0, (float)y0, GetMidZsel());
				opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
				gop = new OnePath(opns); 
				gop.LineTo((float)x1, (float)y1);
				gop.LineTo((float)x2, (float)y2);
				gop.LineTo((float)x3, (float)y3);
				gop.EndPath(opns);
			}
			else
			{
				float sxoffset = 0.0F; 
				for (OnePath op : tsketch.vpaths)
				{
					if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.sfontcode != null) && op.plabedl.sfontcode.equals("survey"))
						sxoffset = Math.max(sxoffset, (int)(op.pnstart.pn.getX() / 200 + 1) * 200); 
				}
System.out.println("  sXXX " + sxoffset); 				
				int nfacets = 12; 
                float rad = TN.radiusofsurveylabel_S; 
				
				OnePathNode opns = new OnePathNode(sxoffset + rad, 0.0F, GetMidZsel());
				opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
				gop = new OnePath(opns); 
				for (int i = 1; i <= nfacets; i++)
				{
					double ang = i * Math.PI * 3 / 2 / nfacets; 
					gop.LineTo(sxoffset + (float)Math.cos(ang) * rad, -(float)Math.sin(ang) * rad);
				}
				for (int i = 1; i < nfacets; i++)
				{
					double ang = i * Math.PI * 3 / 2 / nfacets; 
					gop.LineTo(sxoffset + (float)Math.sin(ang) * rad, 2 * rad - (float)Math.cos(ang) * rad);
				}
				OnePathNode opne = new OnePathNode(sxoffset - rad, rad * 2, GetMidZsel());
				gop.EndPath(opne);
			}
		}

		catch (NoninvertibleTransformException ex)
		{
			TN.emitError("Bad transform");  return null; 
		}
				
		gop.linestyle = SketchLineStyle.SLS_CONNECTIVE;
		gop.bWantSplined = false; 
		gop.plabedl = new PathLabelDecode();

		if (cldtype == 0)
		{
			gop.plabedl.barea_pres_signal = SketchLineStyle.ASE_SKETCHFRAME; // just now need to find where it is in the list in the combo-box
			gop.plabedl.iarea_pres_signal = SketchLineStyle.iareasigframe; 
			gop.plabedl.sketchframedef = new SketchFrameDef();
		}
		else		
			gop.plabedl.sfontcode = "survey"; 

		return gop; 
	}
	
	/////////////////////////////////////////////
	boolean MoveGround(boolean bBackgroundOnly)
	{
		Set<OnePath> opselset = MakeTotalSelList(); 
		if ((opselset.size() != 1) || bmoulinactive)
			return TN.emitWarning("must have one path selected"); 
		OnePath op = opselset.iterator().next(); 
		if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			return TN.emitWarning("must have non-centreline path selected"); 

		float[] pco = op.GetCoords();
		if (op.nlines == 1)
		{
			mdtrans.setToTranslation(pco[2] - pco[0], pco[3] - pco[1]);
		}
		else if (op.nlines == 2)
		{
			float x2 = pco[4] - pco[0];
			float y2 = pco[5] - pco[1];
			float x1 = pco[2] - pco[0];
			float y1 = pco[3] - pco[1];
			double len2 = Math.sqrt(x2 * x2 + y2 * y2);
			double len1 = Math.sqrt(x1 * x1 + y1 * y1);
			double len12 = len1 * len2;
			if (len12 == 0.0F)
				return false;

			double dot12 = (x1 * x2 + y1 * y2) / len12;
			double dot1p2 = (x1 * y2 - y1 * x2) / len12;
			double sca = len2 / len1;

			mdtrans.setToTranslation(pco[0], pco[1]);
			mdtrans.scale(sca, sca);
			orgtrans.setTransform(dot12, dot1p2, -dot1p2, dot12, 0.0F, 0.0F);
			mdtrans.concatenate(orgtrans);
			mdtrans.translate(-pco[0], -pco[1]);
		}
		else
			return TN.emitWarning("must have a two or three point path selected");

		// this is the application.
		if (bBackgroundOnly)
		{
			backgroundimg.PreConcatBusiness(mdtrans);
			backgroundimg.PreConcatBusinessF(pco, currgenpath.nlines);
			if (tsketch.opframebackgrounddrag != null)
    		{
            	DeleteSel();
				FrameBackgroundOutline(); 
            }
		}
		else
		{
			currtrans.concatenate(mdtrans);
			DeleteSel();
		}

//		RedoBackgroundView();
		return true; 
	}
}



