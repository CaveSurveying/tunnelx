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
////////////////////////////////////////////////////////////////////////////////
class SketchGraphics extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener
{
	SketchDisplay sketchdisplay;

	static int SELECTWINDOWPIX = 5;
	static int MOVERELEASEPIX = 20;

	// the sketch.
	OneSketch skblank = new OneSketch(null);
	OneSketch tsketch = skblank;

	// cached paths of those on screen (used for speeding up of drawing during editing).
		Set<OnePath> tsvpathsvizbound   = new HashSet<OnePath>();  // subset which has one area outside of selection
	Set<OnePath> tsvpathsviz            = new HashSet<OnePath>();
	SortedSet<OneSArea> tsvareasviz     = new TreeSet<OneSArea>();
	Set<OnePathNode> tsvnodesviz        = new HashSet<OnePathNode>();
	List<OnePath> tsvpathsframesimages  = new ArrayList<OnePath>(); 
	List<OnePath> tsvpathsframessubsets = new ArrayList<OnePath>(); 
    List<OnePath> tspathssurvexlabel    = new ArrayList<OnePath>(); 
    List<OnePath> tsvpathsframesall     = new ArrayList<OnePath>(); // merging of the above three lists

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
	int[] vactivepathcomponentpairs = new int[40]; // this is a sequence of pairs that subselects vactivepaths
	int nvactivepathcomponents = -1; 
	int ivactivepathcomponents = -1; 
    int ivactivepathcomponents_wholeselection = -1; 
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
	int ibackimageredo = 0; // 0 redo everything, 1 except bitmap background,
							// 2 except partial sketch caching, 3 except redrawing the background sketch (just the overlay),
	int bkifrm = 0;

	boolean bNextRenderDetailed = false;
	boolean bNextRenderPinkDownSketch = false;
	boolean bNextRenderAreaStripes = false;

	AffineTransform orgtrans = new AffineTransform();
	AffineTransform mdtrans = new AffineTransform();
	double mdtransrotate = 0.0F; 
	AffineTransform currtrans = new AffineTransform();
	double currtransrotate = 0.0F;   // poss not used to show the rotation.  try to take it out if poss

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
	boolean ElevBackImageWarp()
	{
		if (currgenpath.nlines != 1)
			return TN.emitWarning("not a line segment");
		float[] pco = currgenpath.GetCoords();
		float vx = pco[2] - pco[0]; 
		float vy = pco[3] - pco[1];
		mdtrans.setToTranslation(pco[0], pco[1]);
		mdtrans.shear(0.0, vy/vx);
		mdtrans.translate(-pco[0], -pco[1]);
		currtrans.concatenate(mdtrans);
		backgroundimg.PreConcatBusiness(mdtrans); 
		DeleteSel();
		//currtrans.concatenate(mdtrans);
		RedoBackgroundView(); 
		return true; 
	}

	/////////////////////////////////////////////
	// 2 Maximize View
	// 1 Centre View
	// 12 Maximize Subset View
	// 121 Maximize Select View
	// 11 Centre Subset View
	// 122 Centre Select View
	void MaxAction(int imaxaction)
	{
		if (imaxaction != 3)
		{
			// imaxaction == 1, 2, 11, 12, 121, 122
            Rectangle2D boundrect; 
			if ((imaxaction == 121) || (imaxaction == 122))
                boundrect = GetSelectedRange(); 
            else
                boundrect = tsketch.getBounds(true, (imaxaction >= 11));

			if ((boundrect.getWidth() != 0.0F) && (boundrect.getHeight() != 0.0F))
			{
				// set the pre transformation
				mdtrans.setToTranslation(getSize().width / 2, getSize().height / 2);

				// scale change
				if ((imaxaction == 2) || (imaxaction == 12) || (imaxaction == 121))
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
					double scaX = Math.sqrt(currtrans.getScaleX()*currtrans.getScaleX() + currtrans.getShearX()*currtrans.getShearX()); 
					mdtrans.scale(scaX, scaX);
				}

				mdtrans.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2));
			}
			//else
			//	mdtrans.setToIdentity();

			orgtrans.setTransform(currtrans);
			currtrans.setTransform(mdtrans);
		}

		else // Set Upright (undoing the rotate and tilt)
		{
			double scaX = Math.sqrt(currtrans.getScaleX()*currtrans.getScaleX() + currtrans.getShearX()*currtrans.getShearX()); 
			double scaY = Math.sqrt(currtrans.getScaleY()*currtrans.getScaleY() + currtrans.getShearY()*currtrans.getShearY()); 
			TN.emitMessage("Ortho mat " + scaX + " " + scaY);
			currtrans.setTransform(scaX, 0, 0, scaX, currtrans.getTranslateX(), currtrans.getTranslateY());
			UpdateTilt(false); 
			assert scaTilt == 1.0; 
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
				sketchdisplay.infopanel.SetPathXML(op, tsketch.sketchLocOffset);
			else if (osa != null)
				sketchdisplay.infopanel.SetAreaInfo(osa, tsketch);
			else if (btabbingchanged)
				sketchdisplay.infopanel.SetSketchInfo(tsketch);
			else
				sketchdisplay.infopanel.SetCleared(); 
		}

		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 3)  // use windowrect when no subsets selected
		{
	        if (btabbingchanged)
            {
                sketchdisplay.printingpanel.subsetrect = tsketch.getBounds(true, true); 
                RedrawBackgroundView();
            }
            sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realposterpaperscale, btabbingchanged); 
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

		boolean bsurvexlabel = (((op != null) && op.IsSurvexLabel()) || !tspathssurvexlabel.isEmpty()); 
		sketchdisplay.acaPreviewLabelWireframe.setEnabled(bsurvexlabel); 
		sketchdisplay.acaImportLabelCentreline.setEnabled(bsurvexlabel); 

		sketchdisplay.menuImportPaper.setEnabled((op == null) || ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) && op.vssubsets.isEmpty())); 
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
			repaint();
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
			repaint();
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
	float GetMidZsel()
	{
		return (sketchdisplay.ztiltpanel.bzthinnedvisible ? (float)(sketchdisplay.ztiltpanel.zlothinnedvisible + sketchdisplay.ztiltpanel.zhithinnedvisible) / 2 : 0.0F); 
	}

	/////////////////////////////////////////////
    float zloselected = 0.0F; 
    float zhiselected = 0.0F; 
    boolean bzrselected = false; 
    Rectangle2D GetSelectedRange()
    {
        zloselected = 0.0F; 
        zhiselected = 0.0F; 
        bzrselected = false; 
        Rectangle2D.Float selbounds = new Rectangle2D.Float();
        if ((currgenpath != null) && (currgenpath.pnend != null))
        {
            zloselected = Math.min(currgenpath.pnstart.zalt, currgenpath.pnend.zalt); 
            zhiselected = Math.max(currgenpath.pnstart.zalt, currgenpath.pnend.zalt); 
            bzrselected = true; 
            selbounds.setRect(currgenpath.getBounds(null));

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
                if (bzrselected)
                    selbounds.add(rpo.op.getBounds(null));
                else
                    selbounds.setRect(rpo.op.getBounds(null));
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
            if (bzrselected)
                selbounds.add(op.getBounds(null));
            else
                selbounds.setRect(op.getBounds(null));
            bzrselected = true; 
        }
        return selbounds; 
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

		
	/////////////////////////////////////////////
	void RenderBackground()
	{
		mainGraphics.setTransform(id);

		// this is due to the background moving
		if ((ibackimageredo == 0) && sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
			sketchgrid.UpdateGridCoords(csize, currtrans, sketchdisplay.miEnableRotate.isSelected(), sketchdisplay.backgroundpanel);

        if ((ibackimageredo == 0) && (sketchdisplay.bottabbedpane.getSelectedIndex() == 3))  // use windowrect when no subsets selected
        	sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.sketchLocOffset, tsketch.realposterpaperscale, true); 

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
	    //double tsca = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
		double scaX = Math.sqrt(currtrans.getScaleX()*currtrans.getScaleX() + currtrans.getShearX()*currtrans.getShearX()); 

        // preview of jigsaw contours
		if (sketchdisplay.miJigsawContour.isSelected())
        {
            mainGraphics.setColor(new Color(1.0F, 0.9F, 0.9F));
            mainGraphics.fill(sketchdisplay.ztiltpanel.jigsawareaoffset);
        }

		// caching the paths which are in view
		if (ibackimageredo == 1)
		{
			tsvpathsviz.clear();
			tsvpathsvizbound.clear(); 
			tsvareasviz.clear(); 
			tsvnodesviz.clear(); 
			tsvpathsframesimages.clear(); 
			tsvpathsframessubsets.clear(); 
			tspathssurvexlabel.clear(); 

			// accelerate this caching if we are zoomed out a lot (using the max calculation)
			Rectangle2D boundrect = tsketch.getBounds(false, false);
			double scchange = Math.max(boundrect.getWidth() / (getSize().width * 0.9F), boundrect.getHeight() / (getSize().height * 0.9F));
			if ((scchange * scaX > 1.9) || sketchdisplay.ztiltpanel.bzthinnedvisible)
			{
                // do the zslicing of the paths and areas here
				Collection<OnePath> lvpathsviz; 
				Collection<OneSArea> lvsareasviz; 
				if (sketchdisplay.ztiltpanel.bzthinnedvisible)
				{
					lvpathsviz = new HashSet<OnePath>(); 
					lvsareasviz = new HashSet<OneSArea>(); 
					for (OnePath op : tsketch.vpaths)
					{
                        op.MakeZsliced(sketchdisplay.ztiltpanel.zlothinnedvisible, sketchdisplay.ztiltpanel.zhithinnedvisible); 
                        if (op.gpzsliced != null)
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
							tsvpathsframesimages.add(op); 

						// do the visibility of the nodes around it (it's a set so doesn't mind duplicates)
                        if (!sketchdisplay.ztiltpanel.bzthinnedvisible || ((sketchdisplay.ztiltpanel.zlothinnedvisible <= op.pnstart.zalt) && (op.pnstart.zalt <= sketchdisplay.ztiltpanel.zhithinnedvisible)))
                            tsvnodesviz.add(op.pnstart); 
                        if (!sketchdisplay.ztiltpanel.bzthinnedvisible || ((sketchdisplay.ztiltpanel.zlothinnedvisible <= op.pnend.zalt) && (op.pnend.zalt <= sketchdisplay.ztiltpanel.zhithinnedvisible)))
                            tsvnodesviz.add(op.pnend); 
					}

                    // survex label scans across all places
                    if (op.IsSurvexLabel())
                        tspathssurvexlabel.add(op); 
                    if (op.IsSketchFrameConnective() && op.plabedl.sketchframedef.sfsketch.equals("") && !op.plabedl.sketchframedef.submapping.isEmpty())
                        tsvpathsframessubsets.add(op); 
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
						tsvpathsframesimages.add(op); 
                    if (op.IsSurvexLabel())
                        tspathssurvexlabel.add(op); 
                    if (op.IsSketchFrameConnective() && op.plabedl.sketchframedef.sfsketch.equals("") && !op.plabedl.sketchframedef.submapping.isEmpty())
                        tsvpathsframessubsets.add(op); 
				}
			}

            tsvpathsframesall.clear(); 
            tsvpathsframesall.addAll(tspathssurvexlabel); 
            tsvpathsframesall.addAll(tsvpathsframessubsets); 
            tsvpathsframesall.addAll(tsvpathsframesimages); 


            // account for unreliable setting
            sketchdisplay.acaPreviewLabelWireframe.setEnabled(!tspathssurvexlabel.isEmpty()); 
            sketchdisplay.acaImportLabelCentreline.setEnabled(!tspathssurvexlabel.isEmpty()); 

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
        boolean bHideSymbols = (scaX < 0.2); 
        if (bHideSymbols)
            TN.emitMessage("hiding symbols because scale is " + scaX); 
		GraphicsAbstraction ga = new GraphicsAbstraction(mainGraphics); 
		if (bNextRenderDetailed)
        {
            if (SketchLineStyle.bDepthColours)
            {
                ga.depthcolourswindowrect = windowrect; 
                ga.depthcolourswidthstep = 2.0 / scaX;  // an area every 10 pixels on the screen
            }
			tsketch.paintWqualitySketch(ga, sketchdisplay.printingpanel.cbRenderingQuality.getSelectedIndex(), sketchdisplay.sketchlinestyle.subsetattrstylesmap);
        }
		else
			tsketch.paintWbkgd(ga, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, stationnamecond, bHideSymbols, tsvpathsviz, tsvpathsvizbound, tsvareasviz, tsvnodesviz, sketchdisplay.ztiltpanel.bzthinnedvisible);

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
    
		// draw the tilted view
		if (sketchdisplay.miShowTilt.isSelected())
        {
			AffineTransform satrans = g2D.getTransform();
            if (!bDynBackDraw)
                UpdateTilt(false); 
            
            // need to premultiply the scale transform
            // Very difficult to avoid the stroke drawn with the proper width and not be scaled
            // Need to draw this entirely without a transform 
            g2D.setTransform(orgtrans);
            //ga.g2d.scale(1.0, 1.0/scaTilt);
            //ga.transform(currtrans);

			boolean bHideCentreline = !sketchdisplay.miCentreline.isSelected(); 

				// Does the selection component subset system get drawn by same function too?
			//List<OnePath> jvpaths = (nvactivepathcomponents == -1 ? tsketch.vpaths : vactivepaths); 
			//int a = (ivactivepathcomponents == -1 ? 0 : vactivepathcomponentpairs[ivactivepathcomponents*2]); 
			//int b = (ivactivepathcomponents == -1 ? tsketch.vpaths.size() : vactivepathcomponentpairs[ivactivepathcomponents*2+1]); 
			for (OnePath op : tsketch.vpaths)
			{
				//OnePath op = jvpaths.get(i); 
				if ((op.linestyle == SketchLineStyle.SLS_INVISIBLE) || (op.linestyle == SketchLineStyle.SLS_CONNECTIVE) || (bHideCentreline && (op.linestyle == SketchLineStyle.SLS_CENTRELINE)))
					continue; 
				boolean bIsSubsetted = (!tsketch.bRestrictSubsetCode || op.bpathvisiblesubset); 
				if (!bIsSubsetted)
					continue; 
				if (op.gptiltin != null)
				{
					LineStyleAttr linestyleattr = SketchLineStyle.ActiveLineStyleAttrs[op.linestyle]; 
					g2D.setColor(linestyleattr.strokecolour);
					g2D.setStroke(linestyleattr.linestroke);
					g2D.draw(op.gptiltin);
				}
				if (op.gptiltout != null)
				{
					LineStyleAttr linestyleattr = SketchLineStyle.notInSelSubsetLineStyleAttrs[op.linestyle]; 
					g2D.setColor(linestyleattr.strokecolour);
					g2D.setStroke(linestyleattr.linestroke);
					g2D.draw(op.gptiltout);
				}
			}

			g2D.setTransform(satrans);
		}
		
		//if (tsketch.opframebackgrounddrag != null)
		//	ga.drawPath(tsketch.opframebackgrounddrag, SketchLineStyle.framebackgrounddragstyleattr); 

		// draw all the active paths, or just a selected component
		if (nvactivepathcomponents == -1)
		{
			for (OnePath op : vactivepaths)
				op.paintW(ga, false, true);
		}
		else
		{
			int a = vactivepathcomponentpairs[ivactivepathcomponents*2]; 
			int b = vactivepathcomponentpairs[ivactivepathcomponents*2+1]; 
            System.out.println("a="+a+" b="+b+"  vactivepaths,size()="+vactivepaths.size()); 
			for (int i = a; i < b; i++)
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
                    // normal plan projection of sketch
				    if (currgenpath.plabedl.sketchframedef.sfelevrotdeg == 0.0)
					{
                        OneSketch asketch = currgenpath.plabedl.sketchframedef.pframesketch;
                        //System.out.println("Plotting frame sketch " + asketch.vpaths.size() + "  " + satrans.toString());
                        for (OnePath op : asketch.vpaths)
                        {
                            if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) && (op.linestyle != SketchLineStyle.SLS_CONNECTIVE))
                                op.paintW(ga, true, true);
                        }
                    }

                    // elevation projection of sketch
                    else
                    {
                        currgenpath.plabedl.sketchframedef.MakeElevClines(false); 
                        for (ElevCLine ecl : currgenpath.plabedl.sketchframedef.elevclines)
                            ga.drawShape(ecl.gp, SketchLineStyle.ActiveLineStyleAttrs[ecl.linestyle]); 
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

        // new todenode overlay
        if (TN.bTodeNode)
            sketchdisplay.todenodepanel.painttodenode(ga); 

        // for the purpose of animations
        if (sketchdisplay.ztiltpanel.cbaAnimateTour.isSelected())
			sketchdisplay.ztiltpanel.buttanimatestep.doClick(0);
    }


	/////////////////////////////////////////////
	// dimensions of the paper are given in metres (then multiplied up by 1000 so that the font stuff actually works)
	// An entirely new set of fonts and linewidths will be required on this paper level (all the title stuff I guess)
	boolean ImportPaperM(String papersize, float lwidth, float lheight)
	{
		// set the poster scale to 1000 if there are no imported sketches here already
		if (tsketch.realposterpaperscale == 1.0)
		{
			int nimportedsketches = 0; 
			for (OnePath op : tsketch.vpaths)
			{
				if (op.IsSketchFrameConnective() && !op.plabedl.sketchframedef.IsImageType())
					nimportedsketches++; 
			}
			if (nimportedsketches == 0)
				tsketch.realposterpaperscale = TN.defaultrealposterpaperscale; 
			else
				return TN.emitWarning("Cannot import paper (and reset realposterpaperscale) because there are "+nimportedsketches+" imported sketches"); 
		}
	
		float pwidth = (float)(lwidth * tsketch.realposterpaperscale * TN.CENTRELINE_MAGNIFICATION);
		float pheight = (float)(lheight * tsketch.realposterpaperscale * TN.CENTRELINE_MAGNIFICATION);

		List<OnePath> pthstoremove = new ArrayList<OnePath>();
		List<OnePath> pthstoadd = new ArrayList<OnePath>();

		OnePath opC;
		if (currgenpath != null)
		{
			if ((currgenpath.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (currgenpath.plabedl == null) || (currgenpath.plabedl.barea_pres_signal != SketchLineStyle.ASE_SKETCHFRAME) || !currgenpath.vssubsets.isEmpty())
				TN.emitError("Connective path, with frame area signal, not in any subset, must selected");
			opC = currgenpath;
		}
		else
		{
			opC = MakeConnectiveLineForData(2, pwidth);
			pthstoadd.add(opC);
		}

		String sspapersubset = sketchdisplay.subsetpanel.GetNewPaperSubset(papersize); 

		sketchdisplay.subsetpanel.PutToSubset(opC, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(opC, sspapersubset, true);

		OnePathNode opn00 = opC.pnstart;
		float x = (float)opn00.pn.getX();
		float y = (float)opn00.pn.getY();
		OnePathNode opn01 = new OnePathNode(x + pwidth, y, GetMidZsel());
		OnePathNode opn10 = new OnePathNode(x, y + pheight, GetMidZsel());
		OnePathNode opn11 = new OnePathNode(x + pwidth, y + pheight, GetMidZsel());

		OnePath op0X = new OnePath(opn00);
		op0X.EndPath(opn01);
		op0X.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(op0X, sspapersubset, true);

		OnePath opX1 = new OnePath(opn01);
		opX1.EndPath(opn11);
		opX1.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(opX1, sspapersubset, true);

		OnePath op1X = new OnePath(opn11);
		op1X.EndPath(opn10);
		op1X.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(op1X, sspapersubset, true);

		OnePath opX0 = new OnePath(opn10);
		opX0.EndPath(opn00);
		opX0.linestyle = SketchLineStyle.SLS_INVISIBLE;
		sketchdisplay.subsetpanel.PutToSubset(opX0, sspapersubset, true);

		pthstoadd.add(opX0); 
		pthstoadd.add(opX1); 
		pthstoadd.add(op0X); 
		pthstoadd.add(op1X); 

		CommitPathChanges(pthstoremove, pthstoadd); 

		vactivepaths.addAll(pthstoadd);
		if (currgenpath != null)
			vactivepaths.add(opC); 
		MaxAction(121); 
		return true; 
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

		ptrelln.realposterpaperscale = asketch.realposterpaperscale;
		//assert ptrelln.realposterpaperscale == tsketch.realposterpaperscale;  // not a useful assert when we transition
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
					for (String subset : wptreli.cp.vssubsets) // avoid duplicates
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
		if (bcorrespsucc)
			ptrelln.CalcAvgTransform(ptrelln.ucavgtrans, null, null, null);
        else
			ptrelln.ucavgtrans.setToIdentity();
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

// this bit could be multithreaded
		for (OnePath op : asketch.vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (bImportNoCentrelines || cplist.contains(op)))
				continue;
			boolean bsurvexlabel = ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && op.plabedl.sfontcode.equals("survey")); 
			if (bsurvexlabel)
				continue; 

			pthstoadd.add(ptrelln.WarpPathD(op, importfromname));
			int progress = (20*i) / asketch.vpaths.size();
			i++;
			if (progress == lastprogress)
				continue;
			lastprogress = progress;
			TN.emitMessage("" + (5*progress) + "% complete at " + (new Date()).toString());
		}

		CommitPathChanges(pthstoremove, pthstoadd); 
		RedrawBackgroundView(); 
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
				ptrelln.CalcAvgTransform(avgtrans, null, null, null);
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
        double rescalew = Math.pow(0.66F, e.getWheelRotation());  // almost always +1 or -1

		// protect zooming too far in relation to the width of the line.  
		// it freezes if zoom out too far with thin lines.
		double plinewidth = currtrans.getScaleX() * rescalew * sketchdisplay.sketchlinestyle.strokew;
		if ((rescalew < 1.0) && (rescalew * plinewidth < 0.001))
			return;
		if ((rescalew > 1.0) && (rescalew * plinewidth > 100.0))
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
        //TN.emitMessage("strokew " + sketchdisplay.sketchlinestyle.strokew + "   scale " + currtrans.getScaleX());
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
            if (bmoulinactive) 
            {
                float x = (((float)moupt.getX() - (float)moulin.getX1()) / TN.CENTRELINE_MAGNIFICATION);
                float y = (((float)moupt.getY() - (float)moulin.getY1()) / TN.CENTRELINE_MAGNIFICATION);
                double distance = java.lang.Math.sqrt(x * x + y * y);
                double bearing = java.lang.Math.toDegrees(java.lang.Math.atan2(x, -y));
                sketchdisplay.infopanel.tfdistance.setText(String.format("%.2f%n", distance));
                if (bearing > 0) 
                    sketchdisplay.infopanel.tfbearing.setText(String.format("%.1f%n", bearing));
                else 
                    sketchdisplay.infopanel.tfbearing.setText(String.format("%.1f%n", 360 + bearing));
            }
            else 
            {
                sketchdisplay.infopanel.tfbearing.setText("-");
                sketchdisplay.infopanel.tfdistance.setText("-");
            }
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
            if (currgenpath.nlines >= 1)
            {
                Point2D bpt = currgenpath.BackOne();
                SetMouseLine(bpt, null);
            }
            else
			{
                ClearSelection(true);  // drop the line entirely
				repaint();
			}
		}

		else if (!vactivepaths.isEmpty() && (nvactivepathcomponents == -1))
			RemoveVActivePath(vactivepaths.get(vactivepaths.size() - 1));

        // very crude undo of one change -- just swaps it in.  Later we can add in an undo stack and a position in it.  
        else
		{
			CommitPathChanges(tsketch.pthstoaddSaved, tsketch.pthstoremoveSaved); 
			RedrawBackgroundView(); 
		}
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

		if (nvactivepathcomponents != -1)
		{
			int a = vactivepathcomponentpairs[ivactivepathcomponents*2]; 
            int b = vactivepathcomponentpairs[ivactivepathcomponents*2+1]; 
            //System.out.println("a="+a+" b="+b+"  vactivepaths,size()="+vactivepaths.size()); 
            for (int i = a; i < b; i++)
				opselset.add(vactivepaths.get(i)); 
		}
		else
			opselset.addAll(vactivepaths);

		return opselset; 
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
			if (tsvpathsframesimages.remove(path))
				bupdatebicox = true; 
			if (tsvpathsframessubsets.remove(path))
				bupdatebicox = true; 

            if (tspathssurvexlabel.remove(path))
            {
                sketchdisplay.acaPreviewLabelWireframe.setEnabled(!tspathssurvexlabel.isEmpty()); 
                sketchdisplay.acaImportLabelCentreline.setEnabled(!tspathssurvexlabel.isEmpty()); 
            }

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
		for (OnePath op : opselset)
		{
			if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) || sketchdisplay.miDeleteCentrelines.isSelected())
				pthstoremove.add(op);
		}
		if (!pthstoremove.isEmpty())
        {
			CommitPathChanges(pthstoremove, null); 
			RedrawBackgroundView(); 
		}
        else
		{
    		ClearSelection(true);
			repaint();
		}
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
            tsketch.isketchchangecount++; 
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


	/////////////////////////////////////////////
	void GUpdateSymbolLayout(boolean bAllSymbols, JProgressBar visiprogressbar)
	{
        visiprogressbar.setString("symbols");
        visiprogressbar.setStringPainted(true);

        List<MutualComponentArea> lvconncommutual = new ArrayList<MutualComponentArea>(); 
        GraphicsAbstraction ga = new GraphicsAbstraction(mainGraphics); 
        for (MutualComponentArea mca : tsketch.sksya.vconncommutual)
        {
            if (bAllSymbols || (!mca.bsymbollaidout && ((windowrect == null) || mca.hit(ga, windowrect))))
                lvconncommutual.add(mca); 
        }

        sketchdisplay.mainbox.symbollayoutprocess.UpdateSymbolLayout(lvconncommutual, visiprogressbar); 
	}

	/////////////////////////////////////////////
	void FuseNodesS(List<OnePath> pthstoremove, List<OnePath> pthstoadd, OnePathNode wpnstart, OnePathNode wpnend, OnePath opexcl1, OnePath opexcl2, boolean bShearWarp)
	{
		// find all paths that link into the first node and warp them to the second.
		// must be done backwards due to messing of the array
        // could have done this with one invocation of WarpPiece that is reused
		for (int i = tsketch.vpaths.size() - 1; i >= 0; i--)
		{
			OnePath op = tsketch.vpaths.get(i);
			if (((op.pnstart == wpnstart) || (op.pnend == wpnstart)) && ((op != opexcl1) && (op != opexcl2))) 
			{
				pthstoremove.add(op);
				WarpPiece ewp = new WarpPiece(wpnstart, wpnend, op, (bShearWarp ? WarpPiece.WARP_SHEARWARP : WarpPiece.WARP_NORMALWARP)); 
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

		ClearSelection(true);  // causes a repaint (could be a problem)
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
				// assert (pthstoadd == null) || !pthstoadd.contains(op); // violated in the pitch undercut case 
                sketchdisplay.mainbox.netconnection.netcommitpathchange(op, "remove", tsketch); 
				DRemovePath(op);
			}
		}
		if (pthstoadd != null)
		{
			for (OnePath op : pthstoadd)
			{
				assert !tsketch.vpaths.contains(op);
				DAddPath(op);
                sketchdisplay.mainbox.netconnection.netcommitpathchange(op, "add", tsketch); 
				if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null))  
					op.UpdateStationLabelsFromCentreline();
			}
		}

		SketchChanged(SC_CHANGE_STRUCTURE);
		// RedrawBackgroundView();  // move to outer calls so it can be deferred

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
	boolean FuseTranslate(OnePath lcurrgenpath, int a, int b)
	{
		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
        for (int i = a; i < b; i++)
            pthstoremove.add(vactivepaths.get(i)); 
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
		RedrawBackgroundView(); 
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
		CommitPathChanges(pthstoremove, pthstoadd); 
		RedrawBackgroundView(); 
		return true; 
	}

	/////////////////////////////////////////////
	boolean FuseCurrent(boolean bShearWarp)
	{
		// FuseTranslate situation
		if ((nvactivepathcomponents != -1) && (ivactivepathcomponents == ivactivepathcomponents_wholeselection) && (icurrgenvactivepath != -1) && !bmoulinactive)
		{
			OnePath lcurrgenpath = vactivepaths.get(icurrgenvactivepath); 
			if ((lcurrgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE) || (lcurrgenpath.nlines != 1) || 
				(lcurrgenpath.pnend.pathcount != 1) || (lcurrgenpath.pnstart.pathcount == 1))
				return TN.emitWarning("Can only fuse-translate single path with simple connections"); 
			int a = vactivepathcomponentpairs[ivactivepathcomponents*2]; 
			int b = vactivepathcomponentpairs[ivactivepathcomponents*2+1]; 
			return FuseTranslate(lcurrgenpath, a, b); 
		}
	
		CollapseVActivePathComponent(); 
		if (vactivepaths.size() >= 3)
			return TN.emitWarning("Fuse works on single or pair of paths");

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

            if (!bEditable)
                return TN.emitWarning("Sketch not editable"); 
            if (warppath.pnstart.pathcount == 1)
                return TN.emitWarning("Can't fuse nothing across"); 

            // now fuse the nodes, and behave differently if it's an elevation node
			// the default fusing is forced on an elevation node by making the warppath a ceiling boundary
			List<OnePath> elevcenconn = (warppath.linestyle != SketchLineStyle.SLS_CEILINGBOUND ? ElevSet.IsElevationNode(warppath.pnstart) : null); 

            List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
            List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

			if (elevcenconn != null)
			{
                System.out.println("asdasda " + elevcenconn.size()); 
                ElevWarp elevwarp = new ElevWarp(); 
                if (!elevwarp.MakeElevWarp(elevcenconn, tsketch.vpaths))
                    return TN.emitWarning("Failed to account for all elevation structures connecting to this node"); 
                elevwarp.MakeWarpPathPieceMap(warppath); 
                elevwarp.MakeWarpPathNodeslists(); 
        
                elevwarp.WarpAllPaths(pthstoremove, pthstoadd, warppath); 
        
                assert !warppath.pnstart.IsCentrelineNode(); 
        
                assert pthstoadd.size() == pthstoremove.size(); 
			}
			else
				FuseNodesS(pthstoremove, pthstoadd, warppath.pnstart, warppath.pnend, warppath, null, bShearWarp);

            // separate out the warp path first so when we do an undo, we don't get it back
            List<OnePath> pthstoremovewarppath = new ArrayList<OnePath>(); 
            pthstoremovewarppath.add(warppath); 
            CommitPathChanges(pthstoremovewarppath, null); 

            //pthstoremove.add(warppath); 
            CommitPathChanges(pthstoremove, pthstoadd); 

            if (warppath.pnstart.pathcount != 0)
                TN.emitError("Warp path failed to carry all connections from its node"); 
            
			RedrawBackgroundView(); 
            return true; 
		}
	}


	/////////////////////////////////////////////
	boolean Makesquare()
	{
		Set<OnePath> opselset = MakeTotalSelList(); 
		int nxalign = 0; 
		int nyalign = 0;
		List<OnePath> pthssquare = new ArrayList<OnePath>();
		List<OnePath> pthsnotsquare = new ArrayList<OnePath>();
		for (OnePath op : opselset)
		{
			double xdiff = Math.abs(op.pnend.pn.getX() - op.pnstart.pn.getX()); 
			double ydiff = Math.abs(op.pnend.pn.getY() - op.pnstart.pn.getY()); 
			if ((xdiff == 0.0) || (ydiff == 0.0))
				pthssquare.add(op);
			else 
				pthsnotsquare.add(op); 
			if (xdiff > ydiff)
				nxalign++; 
			else
				nyalign++; 
		}
System.out.println("sel="+opselset.size()+" xyalign "+nxalign+" "+nyalign+"  ss "+pthssquare.size()+" "+pthsnotsquare.size()); 
		if ((nxalign != 0) && (nyalign != 0))
			return TN.emitWarning("Selected paths not consistently aligned");
		boolean bxfixed = (nyalign != 0);
		if ((bxfixed ? nyalign : nxalign) == 0)
			return TN.emitWarning("No paths selected subject to make square");
		if (pthsnotsquare.size() == 0)
			return TN.emitWarning("All paths already square"); 
		
		double wval = -999.0; 
		if (pthssquare.size() != 0)
		{
			int i = 0; 
			for (OnePath op : pthssquare)
			{
				double lwval = (bxfixed ? op.pnend.pn.getX() : op.pnend.pn.getY()); 
				if (i == 0)
					wval = lwval; 
				else if (wval != lwval)
					return TN.emitWarning("Square paths not aligned (move the one you want to move out of line first)"); 
				i++; 
			}
		}
		else
		{
			double sumwval = 0.0; 
			for (OnePath op : pthsnotsquare)
				sumwval += (bxfixed ? op.pnstart.pn.getX() : op.pnstart.pn.getY()) + (bxfixed ? op.pnend.pn.getX() : op.pnend.pn.getY()); 
			wval = sumwval / (2 * pthsnotsquare.size()); 
		}

		// now produce the aligned array of nodes to warp
        List<OnePathNode> pthnodesfrom = new ArrayList<OnePathNode>(); 
		for (OnePath op : pthsnotsquare)
		{
			if (!pthnodesfrom.contains(op.pnstart))
				pthnodesfrom.add(op.pnstart); 
			if (!pthnodesfrom.contains(op.pnend))
				pthnodesfrom.add(op.pnend); 
		}

		// now fuse each node in turn (may attempt to do a batch job in future, but tricky)
		ClearSelection(true); 
		for (OnePathNode opn : pthnodesfrom)
		{
			OnePathNode opnsquare = new OnePathNode((float)(bxfixed ? wval : opn.pn.getX()), (float)(!bxfixed ? wval : opn.pn.getY()), opn.zalt); 
            List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
            List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
			FuseNodesS(pthstoremove, pthstoadd, opn, opnsquare, null, null, false);
            CommitPathChanges(pthstoremove, pthstoadd); 
			RedrawBackgroundView(); 
		}
		return true; 
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
		Set<OnePath> vpathscomponentsiremains = new HashSet<OnePath>(tsketch.vpaths); 
		vpathscomponentsiremains.removeAll(vpathssel); 
		List<OnePathNode> vpathnodesstack = new ArrayList<OnePathNode>();
		Set<OnePathNode> vpathnodeschecked = new HashSet<OnePathNode>();
		for (OnePath op : vpathsoffsel)
		{
			if (!vpathscomponentsiremains.contains(op))
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
			vpathscomponentsiremains.removeAll(vpathscomponent); 
		}

		ClearSelection(true); 
		assert vactivepaths.isEmpty();

		if (vpathscomponents.size()*2 + 4 > vactivepathcomponentpairs.length) 
			vactivepathcomponentpairs = new int[vpathscomponents.size()*2 + 10]; 

		nvactivepathcomponents = 0; 
		for (Set<OnePath> vpathscomponent : vpathscomponents)
		{
			vactivepathcomponentpairs[nvactivepathcomponents*2] = vactivepaths.size(); 
			vactivepaths.addAll(vpathscomponent); 
			vactivepathcomponentpairs[nvactivepathcomponents*2+1] = vactivepaths.size(); 
            nvactivepathcomponents++; 
		}

        // original selection group
        vactivepathcomponentpairs[nvactivepathcomponents*2] = vactivepaths.size(); 
		vactivepaths.addAll(vpathssel); 
        vactivepathcomponentpairs[nvactivepathcomponents*2+1] = vactivepaths.size(); 
    	nvactivepathcomponents++; 

        // whole selection
        vactivepathcomponentpairs[nvactivepathcomponents*2] = 0; 
        vactivepathcomponentpairs[nvactivepathcomponents*2+1] = vactivepaths.size(); 
		ivactivepathcomponents = nvactivepathcomponents;   // starting point
        ivactivepathcomponents_wholeselection = nvactivepathcomponents; 
    	nvactivepathcomponents++; 

        // complement of selected set
        if (!vpathscomponentsiremains.isEmpty())
        {
            vactivepathcomponentpairs[nvactivepathcomponents*2] = vactivepaths.size();  
            vactivepaths.addAll(vpathscomponentsiremains); 
            vactivepathcomponentpairs[nvactivepathcomponents*2+1] = vactivepaths.size(); 
        	nvactivepathcomponents++; 
        }

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
		CommitPathChanges(pthstoremove, pthstoadd); 
		RedrawBackgroundView(); 
		return true; 
	}

	/////////////////////////////////////////////
    void SelectSingle(OnePath op)
    {
        ClearSelection(true); 
		repaint();
	    currgenpath = op;
	    ObserveSelection(currgenpath, null, 8);
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
    		ClearSelection(true);
			RedrawBackgroundView();
		}
	}



	/////////////////////////////////////////////
	boolean MakePitchUndercut()
	{
		if (bmoulinactive || (currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_PITCHBOUND))
			return TN.emitWarning("Pitch undercut must have pitch boundary selected.");

        OnePath oppitch = currgenpath; 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 

		OnePath opddconnstart = oppitch.pnstart.GetDropDownConnPath();
		if (opddconnstart.pnend.pathcount == 0)  // always true when adding a new one (not in the unlikely event of having found a dropdown connpath already, if we are working in segments)
		{
			opddconnstart.vssubsets.addAll(oppitch.vssubsets);
			pthstoadd.add(opddconnstart);
		}

		OnePath opddconnend = oppitch.pnend.GetDropDownConnPath();
		if (opddconnend.pnend.pathcount == 0)
		{
			opddconnend.vssubsets.addAll(oppitch.vssubsets);
			pthstoadd.add(opddconnend);
		}

		// now make the invisible line
		OnePath opinv = new OnePath();
		opinv.pnstart = opddconnstart.pnend;
		opinv.pnend = opddconnend.pnend;
		opinv.linestyle = SketchLineStyle.SLS_INVISIBLE;
		opinv.vssubsets.addAll(oppitch.vssubsets);
		opinv.gp = (GeneralPath)oppitch.gp.clone();
		opinv.nlines = oppitch.nlines;
		opinv.linelength = oppitch.linelength;
		opinv.bSplined = oppitch.bSplined;
		opinv.bWantSplined = oppitch.bWantSplined;

		pthstoadd.add(opinv);

        // remove and add the current path to be removed and added so it appears on top
        pthstoremove.add(oppitch); 
		pthstoadd.add(oppitch);

		CommitPathChanges(pthstoremove, pthstoadd); 
		RedrawBackgroundView(); 
		return true; 
	}




	/////////////////////////////////////////////
	void ClearSelection(boolean bupdatepathparameters)
	{
		if (bupdatepathparameters)
        {
            if (sketchdisplay.subsetpanel.sascurrent != null)
                sketchdisplay.sketchlinestyle.GoSetParametersCurrPath(); // this copies over anything that was missed
            else
                TN.emitWarning("sascurrent is null when updating parameters"); 
        }
		currgenpath = null;
		currselarea = null;
		vactivepaths.clear();
		vactivepathsnodecounts.clear(); 
		nvactivepathcomponents = -1; 
		ivactivepathcomponents = -1; 
		bmoulinactive = false; // newly added
		ObserveSelection(null, null, 7);
		//repaint(); 
	}

	/////////////////////////////////////////////
	void CollapseVActivePathComponent()
	{
		if (nvactivepathcomponents != -1)
		{
			// would like to do this with two remove range functions, but don't have the docs on this machine
			int a = vactivepathcomponentpairs[ivactivepathcomponents*2]; 
			int b = vactivepathcomponentpairs[ivactivepathcomponents*2+1]; 
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
		CommitPathChanges(pthstoremove, pthstoadd); 
		RedrawBackgroundView(); 
		return true; 
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
// new stuff for tilting and producing a drawing plane
	public void TiltView(double ltiltdeg)
	{
		// set the pre transformation
		mdtrans.setToTranslation(csize.width / 2, csize.height / 2);
		mdtrans.scale(1.0F, (ltiltdeg > 0.0 ? 0.5F : 2.0F));
		mdtrans.translate(-csize.width / 2, -csize.height / 2);

		orgtrans.setTransform(currtrans);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		UpdateTilt(false); 
		RedoBackgroundView();
	}

	
	/////////////////////////////////////////////
    double scaTilt = 1.0; // of currtrans
	AffineTransform currtilttrans = new AffineTransform();
	void UpdateTilt(boolean bforce)
	{
		// derive the scatilt from currtrans (not ideal, but keeps it consistent)
		double scaX = Math.sqrt(currtrans.getScaleX()*currtrans.getScaleX() + currtrans.getShearX()*currtrans.getShearX()); 
		double scaY = Math.sqrt(currtrans.getScaleY()*currtrans.getScaleY() + currtrans.getShearY()*currtrans.getShearY()); 
		scaTilt = scaY / scaX;
		assert scaTilt <= 1.001; 
		if (scaTilt > 0.999)
			scaTilt = 1.0; 

		if (!bforce && currtilttrans.equals(currtrans))
            return;
		if (!sketchdisplay.miShowTilt.isSelected())
            return; // save time
        currtilttrans.setTransform(currtrans); 
			
		// apply to the tilted lifted paths
		System.out.println("scscT "+scaTilt+" "+currtrans.getScaleY() / currtrans.getScaleX());
			// tilt and undo the scale in x axis (the real scale) Don't know how the rotating is working without doing this
		double scaTiltZ = scaX * Math.sqrt(1.0 - scaTilt*scaTilt); //(scaTilt != 1.0 ? Math.sin(Math.acos(scaTilt)) : 0.0); 
System.out.println("TIIILT  " +scaX+"  "+ scaTilt + " "+scaTiltZ+ " "); 
		for (OnePath op : tsketch.vpaths)
        {
            if ((op.linestyle != SketchLineStyle.SLS_INVISIBLE) && (op.linestyle != SketchLineStyle.SLS_CONNECTIVE))
                op.MakeTilted(sketchdisplay.ztiltpanel.zlothinnedvisible, sketchdisplay.ztiltpanel.zhithinnedvisible, scaTiltZ, currtrans); 
        }
	}
	
	/////////////////////////////////////////////
	public void Translate(double xprop, double yprop)
	{
		// set the pre transformation
		mdtrans.setToTranslation(csize.width * xprop, csize.height * yprop);

		orgtrans.setTransform(currtrans);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	public void Rotate(float degrees)
	{
		//double scaTilt = currtrans.getScaleY() / currtrans.getScaleX();
		mdtrans.setToScale(1.0, scaTilt);
		mdtrans.rotate(Math.toRadians(degrees), csize.width / 2, csize.height / (scaTilt * 2));
		mdtrans.scale(1.0, 1.0 / scaTilt);

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
			RedrawBackgroundView(); 
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
	void mousePressedDragview(MouseEvent e)
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
		//backgroundimg.orgparttrans.setTransform(backgroundimg.currparttrans);
		mdtrans.setToIdentity();
		prevx = e.getX();
		prevy = e.getY();

		if (!e.isMetaDown())
		{
			if (e.isShiftDown())
				momotion = M_DYN_DRAG;
			else if (e.isControlDown())
				momotion = M_DYN_SCALE;
			else if (sketchdisplay.miEnableRotate.isSelected() || sketchdisplay.miShowTilt.isSelected())
				momotion = M_DYN_ROT;
			else
				momotion = M_DYN_DRAG; // was M_NONE
		}
	}

	/////////////////////////////////////////////
	void mousePressedCtrlUp(MouseEvent e)
	{
		SetMPoint(e);

		// ending a path
		if (bmoulinactive)
        {
			LineToCurve();
            if (e.isShiftDown() || ((e.getClickCount() == 2) && sketchdisplay.miEnableDoubleClick.isSelected()))
                EndCurve(null);
		}

		// here is where we can toggle the requirement that the shift key is held down to start a path
		else 
        {
            if (!e.isShiftDown())
            {
                ClearSelection(true);
                OnePathNode opns = new OnePathNode((float)moupt.getX(), (float)moupt.getY(), GetMidZsel());
                opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
                StartCurve(opns);
                momotion = M_SKET;
                LineToCurve();
				repaint();
            }
            // nothing happens if shift is down when you start clicking
        }
		repaint();
	}

	/////////////////////////////////////////////
	void mousePressedEndAndStartPath(MouseEvent e)
	{
		LineToCurve();
		EndCurve(null);
		OnePathNode opns = currgenpath.pnend;
		ClearSelection(true);
		StartCurve(opns);
		repaint();
	}

	/////////////////////////////////////////////
	void mousePressedSnapToNode(MouseEvent e)
	{
		SetMPoint(e);
		momotion = M_SKET_SNAP;
		linesnap_t = -1.0;
		selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);
		repaint();
	}

	/////////////////////////////////////////////
	void mousePressedSplitLine(MouseEvent e)
	{
		SetMPoint(e);
		// the node splitting one. only on edges if shift is down(over-ride with shift down)
		//double scale = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
		double scaX = Math.sqrt(currtrans.getScaleX()*currtrans.getScaleX() + currtrans.getShearX()*currtrans.getShearX()); 
		linesnap_t = currgenpath.ClosestPoint(moupt.getX(), moupt.getY(), 5.0 / scaX);
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
        sketchdisplay.ztiltpanel.cbaAnimateTour.setSelected(false); 

        //System.out.println("mouclickcount " + e.getClickCount()); 
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
// an effective rotation system will be difficult to do
// kind of want to by making circles round the centre, 
// but then it's not by start point that matters.  must update prevx as we move
			int vx = e.getX() - prevx;
			//double scaTilt = currtrans.getScaleY() / currtrans.getScaleX();
			mdtrans.setToScale(1.0, scaTilt); 
			mdtransrotate = (float)vx / csize.width; 
			mdtrans.rotate(mdtransrotate, csize.width / 2, csize.height / (scaTilt * 2));
			mdtrans.scale(1.0, 1.0 / scaTilt);
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
			if (momotion == M_DYN_ROT)
				currtransrotate += mdtransrotate;

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

		RedrawBackgroundView(); 
	}


	/////////////////////////////////////////////
	Point2D.Float scrddpt = new Point2D.Float();
	Point2D.Float ddpt = new Point2D.Float();
	OnePath MakeConnectiveLineForData(int cldtype, float sdist)
	{
		double x0, y0; 
		double x1, y1; 
		double x2, y2; 
		double x3, y3; 
		assert currgenpath == null; 
		assert (cldtype == 0) || (cldtype == 1) || (cldtype == 2);  // 0 is image loop;  1 is survex data;  2 is frame specifier
		OnePath gop; 
		try
		{
			if (cldtype == 0)
			{
				scrddpt.setLocation(30, 20);
				currtrans.inverseTransform(scrddpt, ddpt);
				x0 = ddpt.getX();  y0 = ddpt.getY(); 
				scrddpt.setLocation(80, 20);
				currtrans.inverseTransform(scrddpt, ddpt);
				x1 = ddpt.getX();  y1 = ddpt.getY(); 
				scrddpt.setLocation(80, 40);
				currtrans.inverseTransform(scrddpt, ddpt);
				x2 = ddpt.getX();  y2 = ddpt.getY(); 
				scrddpt.setLocation(30, 40);
				currtrans.inverseTransform(scrddpt, ddpt);
				x3 = ddpt.getX();  y3 = ddpt.getY(); 

				OnePathNode opns = new OnePathNode((float)x0, (float)y0, GetMidZsel());
				opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
				gop = new OnePath(opns); 
				gop.LineTo((float)x1, (float)y1);
				gop.LineTo((float)x2, (float)y2);
				gop.LineTo((float)x3, (float)y3);
				gop.EndPath(opns);
			}
			else if (cldtype == 2)
			{
				ddpt.setLocation(csize.width / 3, csize.height / 3);
				currtrans.inverseTransform(scrddpt, ddpt);
				OnePathNode opn00 = new OnePathNode((float)ddpt.getX(), (float)ddpt.getY(), GetMidZsel());
				OnePathNode opn00t = new OnePathNode((float)(ddpt.getX() + sdist * 0.3), (float)(ddpt.getY() + sdist * 0.2), GetMidZsel()); 
				gop = new OnePath(opn00);
				gop.EndPath(opn00t);
			}
			else 
			{
				assert cldtype == 1; 
				float sxoffset = 0.0F; 
				for (OnePath op : tsketch.vpaths)
				{
					if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && op.plabedl.sfontcode.equals("survey"))
						sxoffset = Math.max(sxoffset, (int)(op.pnstart.pn.getX() / 200 + 1) * 200); 
				}
System.out.println("  sXXX " + sxoffset); 				
				int nfacets = 12; 
                float rad = sdist; 
				
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

		if ((cldtype == 0) || (cldtype == 2))
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
			// prob originally worked from currgenpath, but then generalized to more odd ways of selecting
		Set<OnePath> opselset = MakeTotalSelList(); 
		if ((opselset.size() != 1) || bmoulinactive)
			return TN.emitWarning("must have one path selected"); 
		OnePath op = opselset.iterator().next(); 
		float[] pco = op.GetCoords();
		if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
		{
			if ((tsketch.opframebackgrounddrag == null) || !tsketch.opframebackgrounddrag.IsSketchFrameConnective())
				return TN.emitWarning("no background frame image to drag");
			SketchFrameDef sketchframedef = tsketch.opframebackgrounddrag.plabedl.sketchframedef; 
			if (sketchframedef.IsImageType() || (sketchframedef.pframesketch == null) || !bBackgroundOnly || (op.nlines != 1))
				return TN.emitWarning("must have non-centreline path selected (unless positioning plan sketch in elevation)"); 
			
			// may want to outsource this to MatchSketchCentrelines class
            // final parameter would allow taking a subset value that would subselect centrelines and connective lines (whose ends connect between stations) that have been subsetted
            // The import for the elevation centreline would require a designated subset to define this, 
            // which could be specified by the include of the plan used for shifting to position along the centrelines
            // possible to a series of subsets, specifying which ones to fold left or right
            // in which case, this set would need to go as the final parameter here
			OnePath opcorresp = MatchSketchCentrelines.FindBestStationpairMatch(sketchframedef.pframesketch.vpaths, op.pnstart.pnstationlabel, op.pnend.pnstationlabel, null); 
			if (opcorresp == null)
				return TN.emitWarning("no corresponding centreline found for this elevation leg"); 
				
			sketchframedef.ConvertSketchTransformTCLINE(pco, tsketch.realposterpaperscale, tsketch.sketchLocOffset, currtrans, opcorresp);
			sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(tsketch.opframebackgrounddrag, true);
			RedoBackgroundView();  
			return true;
		}

		// implementation on the screen 
		if (op.nlines == 1)
			mdtrans.setToTranslation(pco[2] - pco[0], pco[3] - pco[1]);
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

		// implementation in the settings for the background
		if (!bBackgroundOnly)
		{
			currtrans.concatenate(mdtrans);
			DeleteSel();
			return true; 
		}
		
		// we apply the transform to the matrix *and* to the underlying positioning values (in ConvertSketchTransformT) 
		// to check the values come out the same, because it was a hard computation to get right.
		backgroundimg.PreConcatBusiness(mdtrans);
		if ((tsketch.opframebackgrounddrag == null) || !tsketch.opframebackgrounddrag.IsSketchFrameConnective())
			return TN.emitWarning("no background frame image to drag");
			
		SketchFrameDef sketchframedef = tsketch.opframebackgrounddrag.plabedl.sketchframedef; 
		System.out.println("nilllll " + sketchframedef.pframesketchtrans);
		AffineTransform lpframetrans = new AffineTransform(sketchframedef.pframesketchtrans);
		if (!sketchframedef.sfelevvertplane.equals("") && (scaTilt == 1.0))
			return TN.emitWarning("cannot shift vertical image when view not tilted");
		sketchframedef.ConvertSketchTransformT(pco, op.nlines, tsketch.realposterpaperscale, tsketch.sketchLocOffset, currtrans, tsketch.opframebackgrounddrag);
		sketchdisplay.sketchlinestyle.pthstyleareasigtab.UpdateSFView(tsketch.opframebackgrounddrag, true);
		DeleteSel();
		FrameBackgroundOutline(); 
		RedoBackgroundView();  
		return true; 
	}
}



