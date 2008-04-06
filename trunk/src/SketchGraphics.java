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

import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;

import java.awt.Dimension;
import java.awt.Image;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

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

	SketchPrint sketchprint = new SketchPrint();

	static int SELECTWINDOWPIX = 5;
	static int MOVERELEASEPIX = 20;

	// the sketch.
	OneSketch skblank = new OneSketch(null, null);
	OneSketch tsketch = skblank;
	OneTunnel activetunnel = null; // the tunnel to which the above sketch belongs.

	// cached paths of those on screen (used for speeding up of drawing during editing).
	List<OnePath> tsvpathsviz = new ArrayList<OnePath>();

	boolean bEditable = false;

	OnePath currgenpath = null;
	OneSArea currselarea = null;

	// the currently active mouse path information.
	Line2D.Float moulin = new Line2D.Float();
	GeneralPath moupath = new GeneralPath();
	Ellipse2D elevpoint = new Ellipse2D.Float();
	
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
	// trial to see if we can do good greying out of buttons.
	void DChangeBackNode()
	{
		sketchdisplay.acaBackNode.setEnabled(((currgenpath != null) && bmoulinactive) || !vactivepaths.isEmpty());
	}

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
	void UpdateBottTabbedPane(OnePath op, OneSArea osa)
	{
		if (sketchdisplay.bottabbedpane.getSelectedIndex() == 0)  
			sketchdisplay.subsetpanel.UpdateSubsetsOfPath(op);
		else if (sketchdisplay.bottabbedpane.getSelectedIndex() == 2)
		{
			if (op != null)
			{
				int iselpath = tsketch.vpaths.indexOf(op); // slow; (maybe not necessary)
				sketchdisplay.infopanel.tfselitempathno.setText(String.valueOf(iselpath + 1));
				sketchdisplay.infopanel.tfselnumpathno.setText(String.valueOf(tsketch.vpaths.size())); 
				sketchdisplay.infopanel.SetPathXML(op);
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
			sketchdisplay.printingpanel.UpdatePrintingRectangle(tsketch.getBounds(true, true), tsketch.sketchLocOffset, tsketch.realpaperscale); 
	}
	
	/////////////////////////////////////////////
	void ObserveSelection(OnePath op, OneSArea osa)
	{
		assert (op == null) || (osa == null); 
		sketchdisplay.sketchlinestyle.SetParametersIntoBoxes(op);
		UpdateBottTabbedPane(op, osa); 
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
				DChangeBackNode();
				ObserveSelection(currgenpath, null);
			}
			momotion = M_NONE;
		}

		// do the selection of areas
		if (momotion == M_SEL_AREA)
		{
			OneSArea lcurrselarea = tsketch.SelArea(mainGraphics, selrect, currselarea);
			ClearSelection(true);
			currselarea = lcurrselarea;
			ObserveSelection(null, currselarea);
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
				DChangeBackNode();
			}

			// find a path and invert toggle it in the list.
			OnePath selgenpath = tsketch.SelPath(mainGraphics, selrect, pathaddlastsel, tsvpathsviz);

			// toggle in list.
			if (selgenpath != null)
			{
				pathaddlastsel = selgenpath;
				if (vactivepaths.contains(selgenpath))
				{
					RemoveVActivePath(selgenpath);
					ObserveSelection(null, null);
				}
				else
				{
					AddVActivePath(selgenpath);
					ObserveSelection(selgenpath, null);
				}
				DChangeBackNode();
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
			selpathnode = tsketch.SelNode(opfront, bopfrontvalid, mainGraphics, selrect, selpathnodecycle);

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
	void PrintThis(int prtscalecode)
	{
		sketchprint.PrintThis(prtscalecode, !sketchdisplay.miCentreline.isSelected(), sketchdisplay.miShowNodes.isSelected(), !sketchdisplay.miStationNames.isSelected(), sketchdisplay.vgsymbols, sketchdisplay.sketchlinestyle, tsketch, csize, currtrans, sketchdisplay);
	}

	/////////////////////////////////////////////
	void ExportSVG()
	{
		tsketch.ExportSVG(sketchdisplay.vgsymbols);
	}

	/////////////////////////////////////////////
	void RenderBackground()
	{
		boolean bHideMarkers = !sketchdisplay.miShowNodes.isSelected();
		mainGraphics.setTransform(id);

		// this is due to the background moving
		if ((ibackimageredo == 0) && sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
			sketchgrid.UpdateGridCoords(csize, currtrans, sketchdisplay.miEnableRotate.isSelected(), sketchdisplay.backgroundpanel);

		// render the background
// this is working independently of ibackimageredo for now
		boolean bNewBackgroundExists = ((tsketch.opframebackgrounddrag != null) && (tsketch.opframebackgrounddrag.plabedl != null) && (tsketch.opframebackgrounddrag.plabedl.sketchframedef != null));
		boolean bClearBackground = (((tsketch.ibackgroundimgnamearrsel == -1) && !bNewBackgroundExists) || !sketchdisplay.miShowBackground.isSelected());
		if (!bClearBackground && !backgroundimg.bBackImageGood)
		{
			backgroundimg.bBackImageGood = true;
			bClearBackground = !backgroundimg.bBackImageGood;
		}

		// gawe have a background.
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
		if	(ibackimageredo == 1)
		{
			tsvpathsviz.clear();

			// accelerate this caching if we are zoomed out a lot (using the max calculation)
			Rectangle2D boundrect = tsketch.getBounds(false, false);
			double scchange = Math.max(boundrect.getWidth() / (getSize().width * 0.9F), boundrect.getHeight() / (getSize().height * 0.9F));
			double tsca = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
			if (scchange * tsca < 1.9)
				tsvpathsviz.addAll(tsketch.vpaths);
			else
			{
				for (OnePath op : tsketch.vpaths)
				{
					if (mainGraphics.hit(windowrect, op.gp, (op.linestyle != SketchLineStyle.SLS_FILLED)))
						tsvpathsviz.add(op);
				}
				//TN.emitMessage("vizpaths " + tsvpathsviz.size() + " of " + tsketch.vpaths.size());
			}
			//Collections.reverse(tsvpathsviz); 
			
			ibackimageredo = 2;
		}
		else
			assert(tsvpathsviz.isEmpty() || tsketch.vpaths.contains(tsvpathsviz.get(0)));

		// the grid thing
		if (sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
		{
			mainGraphics.setStroke(SketchLineStyle.gridStroke); // thin
			mainGraphics.setColor(SketchLineStyle.gridColor); // black
			mainGraphics.draw(sketchgrid.gpgrid);
		}

		// draw the sketch according to what view we want (incl single frame of print quality)
		int stationnamecond = (sketchdisplay.miStationNames.isSelected() ? 1 : 0) + (sketchdisplay.miStationAlts.isSelected() ? 2 : 0);
		if (bNextRenderDetailed)
			tsketch.paintWqualitySketch(new GraphicsAbstraction(mainGraphics), false, sketchdisplay.vgsymbols, sketchdisplay.sketchlinestyle);
		else
			tsketch.paintWbkgd(new GraphicsAbstraction(mainGraphics), !sketchdisplay.miCentreline.isSelected(), bHideMarkers, stationnamecond, sketchdisplay.vgsymbols, tsvpathsviz);

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

		for (OnePath op : vactivepaths)
			op.paintW(ga, false, true);
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
			for (OnePath sop : currselarea.connpathrootscen)
				sop.paintW(ga, false, true);
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

		if (sketchdisplay.selectedsubsetstruct.bIsElevStruct)
			ga.drawShape(elevpoint, SketchLineStyle.ActiveLineStyleAttrs[SketchLineStyle.SLS_DETAIL]);  
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// maybe whole of this could be moved into station calculation
	class TransformSpaceToSketch
	{
		boolean bAnaglyphPerpective = false;
		float anaglyphX;
		float anaglyphD;

		float xcen, ycen, zcen;
		float rothx, rothd;

		/////////////////////////////////////////////
		TransformSpaceToSketch(OnePath currgenpath, StationCalculation sc)
		{
			if ((currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_CENTRELINE))
				return;
			bAnaglyphPerpective = true;
			xcen = (sc.volxlo + sc.volxhi) / 2;
			ycen = (sc.volylo + sc.volyhi) / 2;
			zcen = (sc.volzlo + sc.volzhi) / 2;

			// factored down by a hundred metres times the dimensions of the box
			float afac = (sc.volzhi - sc.volzlo) / 100.0F;
			anaglyphX = (float)((currgenpath.pnend.pn.getX() - currgenpath.pnstart.pn.getX()) * afac);
			anaglyphD = 5.0F * (float)(Math.abs(currgenpath.pnend.pn.getY() - currgenpath.pnstart.pn.getY()) * afac);
			System.out.println("Anaglyph X " + anaglyphX + "  D " + anaglyphD);

			float adleng = (float)Math.sqrt(anaglyphX * anaglyphX + anaglyphD * anaglyphD);
			rothx = anaglyphX / adleng;
			rothd = anaglyphD / adleng;
		}
				

		/////////////////////////////////////////////
		OnePathNode TransPoint(Vec3 Loc)  // used only on ImportCentreline, nodes
		{
			if (!bAnaglyphPerpective)
				return new OnePathNode(Loc.x * TN.CENTRELINE_MAGNIFICATION, -Loc.y * TN.CENTRELINE_MAGNIFICATION, Loc.z * TN.CENTRELINE_MAGNIFICATION); 

			// first translate to centre 
			float x0 = Loc.x - xcen; 
			float y0 = Loc.y - ycen; 
			float z0 = Loc.z - zcen; 
			
			// apply rotation about the y-axis
			float xr0 = rothd * x0 - rothx * z0; 
			float yr0 = y0; 
			float zr0 = rothd * z0 + rothx * x0; 
			
			// apply perspective shortening 
			float dfac = (anaglyphD - zr0) / anaglyphD; 
			
			// reapply centre
			//dfac = 1.0F; 			
			float lx = xr0 * dfac + xcen; 
			float ly = yr0 * dfac + ycen; 
			float lz = zr0 + zcen; 

			System.out.println("PT: " + Loc.x + "," + Loc.y + "," + Loc.z + "\n   " + lx + "," + ly + "," + lz + "  dfac=" + dfac); 

			return new OnePathNode(lx * TN.CENTRELINE_MAGNIFICATION, -ly * TN.CENTRELINE_MAGNIFICATION, lz * TN.CENTRELINE_MAGNIFICATION); 
		}
	}; 
	


	/////////////////////////////////////////////
	// xsectioncode = 0 for none, 1 for plan, 2 for elev
	void ImportSketchCentreline(boolean bcopytitles)
	{
		// protect there being centrelines in this sketch already
		// (should always make new and warp over)
		boolean bnoimport = tsketch.bSymbolType;
		for (OnePath op : tsketch.vpaths)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				if (op != currgenpath)
					bnoimport = true; 
			}
		} 
				
				
		// although if they are compatible, it would be fair to bring in just extra centrelines
		if (bnoimport)
		{
			TN.emitWarning("no centreline import where there are centrelines or symbol type");
			return;
		}

		if (bcopytitles)
			TN.emitMessage("Importing with *titles setting the subsets");


		// set the value of the LocOffset of this sketch (once and for all)
		tsketch.sketchLocOffset = activetunnel.posfileLocOffset;

		// this should be projecting perspecively
		// onto the XY plane, but for now we frig it with a translation in z
		

		// this otglobal was set when we opened this window.
		// this is the standard upper tunnel that station calculations are sucked into.
		OneTunnel otfrom = sketchdisplay.mainbox.otglobal;

		// calculate when we import
		if ((sketchdisplay.mainbox.tunnelfilelist.activetunnel.posfile != null) && (sketchdisplay.mainbox.tunnelfilelist.activetunnel.vposlegs == null))
			TunnelLoader.LoadPOSdata(sketchdisplay.mainbox.tunnelfilelist.activetunnel);
		sketchdisplay.mainbox.tunnelfilelist.tflist.repaint();
		sketchdisplay.mainbox.sc.CopyRecurseExportVTunnels(otfrom, sketchdisplay.mainbox.tunnelfilelist.activetunnel, true);
		if (sketchdisplay.mainbox.sc.CalcStationPositions(otfrom, null, otfrom.name) <= 0)
			return;

		// extract the anaglyph distance from selected line
		TransformSpaceToSketch tsts = new TransformSpaceToSketch(currgenpath, sketchdisplay.mainbox.sc);

		// parallel array of new nodes
		OnePathNode[] statpathnode = new OnePathNode[otfrom.vstations.size()];
		for (OneLeg ol : otfrom.vlegs)
		{
			if (ol.osfrom != null)
			{
				int ipns = otfrom.vstations.indexOf(ol.osfrom);
				int ipne = otfrom.vstations.indexOf(ol.osto);

				if ((ipns != -1) || (ipne != -1))
				{
					if (statpathnode[ipns] == null)
						statpathnode[ipns] = tsts.TransPoint(ol.osfrom.Loc);
					if (statpathnode[ipne] == null)
						statpathnode[ipne] = tsts.TransPoint(ol.osto.Loc);

					OnePath op = new OnePath(statpathnode[ipns], ol.osfrom.name, statpathnode[ipne], ol.osto.name);
					if (bcopytitles && (ol.svxtitle != null) && !ol.svxtitle.equals(""))
						op.vssubsets.add(ol.svxtitle);
					AddPath(op);
					op.UpdateStationLabelsFromCentreline();
					assert (statpathnode[ipns].IsCentrelineNode() && statpathnode[ipne].IsCentrelineNode());
				}
				else
					TN.emitWarning("Can't find station " + ol.osfrom + " or " + ol.osto);
			}
		}

		asketchavglast = null; // change of avg transform cache.
		SketchChanged(SC_CHANGE_STRUCTURE);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// this builds a little miniature version of the centreline in elevation
	void CopySketchCentreline(float angdeg, float scalefac, float xorig, float yorig)
	{
		float cosa = (float)TN.degcos(angdeg);
		float sina = (float)TN.degsin(angdeg);

		// use the pathcountch flag to mark down the nodes as we meet them
		for (OnePathNode opn : tsketch.vnodes)
			opn.pathcountch = -1;

		
		// cache the centrelines, so we then can change vpaths without worrying about the iterators
		List<OnePath> lvpathscentrelines = new ArrayList<OnePath>();
		for (OnePath op : tsketch.vpaths)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				lvpathscentrelines.add(op); 
		}

		for (OnePath op : lvpathscentrelines)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				OnePathNode pnstart;
				if (op.pnstart.pathcountch == -1)
					pnstart = new OnePathNode((float)(op.pnstart.pn.getX() * cosa - op.pnstart.pn.getY() * sina) * scalefac - xorig, -(op.pnstart.zalt + 10*tsketch.sketchLocOffset.z) * scalefac + yorig, (float)(-op.pnstart.pn.getX() * sina - op.pnstart.pn.getY() * cosa) * scalefac);
				else
					pnstart = tsketch.vnodes.get(op.pnstart.pathcountch);

				OnePathNode pnend;
				if (op.pnend.pathcountch == -1)
					pnend = new OnePathNode((float)(op.pnend.pn.getX() * cosa - op.pnend.pn.getY() * sina) * scalefac - xorig, -(op.pnend.zalt + 10*tsketch.sketchLocOffset.z) * scalefac + yorig, (float)(-op.pnend.pn.getX() * sina - op.pnend.pn.getY() * cosa) * scalefac); // we use sketchLocOffset.z here so we can use the sketch grid to draw height gridlines onto the elevation 
				else
					pnend = tsketch.vnodes.get(op.pnend.pathcountch);

				OnePath nop = new OnePath(pnstart);
				nop.linestyle = op.linestyle;
				nop.EndPath(pnend);
				nop.vssubsets.addAll(op.vssubsets);
				nop.importfromname = "elevcopy";
				AddPath(nop);

				// the add path adds in the nodes, and we have to get their cross indexes
				if (op.pnstart.pathcountch == -1)
					op.pnstart.pathcountch = tsketch.vnodes.indexOf(pnstart);
				if (op.pnend.pathcountch == -1)
					op.pnend.pathcountch = tsketch.vnodes.indexOf(pnend);
			}
		}

		SketchChanged(SC_CHANGE_PATHS);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// dimensions of the paper are given in metres (then multiplied up by 1000 so that the font stuff actually works)
	// An entirely new set of fonts and linewidths will be required on this paper level (all the title stuff I guess)
	void ImportPaperM(String papersize, float lwidth, float lheight)
	{
		if ((currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (currgenpath.plabedl == null) || (currgenpath.plabedl.barea_pres_signal != SketchLineStyle.ASE_SKETCHFRAME) || !currgenpath.vssubsets.isEmpty())
		{
			TN.emitWarning("Connective path, with frame area signal, not in any subset, must selected");
			return;
		}

		String sspapersubset = sketchdisplay.subsetpanel.GetNewPaperSubset(papersize); 

		sketchdisplay.subsetpanel.PutToSubset(currgenpath, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(currgenpath, sspapersubset, true); 

		float pwidth = (float)(lwidth * tsketch.realpaperscale * TN.CENTRELINE_MAGNIFICATION);
		float pheight = (float)(lheight * tsketch.realpaperscale * TN.CENTRELINE_MAGNIFICATION);

		OnePathNode opn00 = currgenpath.pnstart;
		float x = (float)opn00.pn.getX();
		float y = (float)opn00.pn.getY();
		OnePathNode opn01 = new OnePathNode(x + pwidth, y, 0.0F);
		OnePathNode opn10 = new OnePathNode(x, y + pheight, 0.0F);
		OnePathNode opn11 = new OnePathNode(x + pwidth, y + pheight, 0.0F);

		OnePath op;

		op = new OnePath(opn00);
		op.EndPath(opn01);
		op.linestyle = SketchLineStyle.SLS_INVISIBLE;
		AddPath(op);
		sketchdisplay.subsetpanel.PutToSubset(op, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op, sspapersubset, true); 

		op = new OnePath(opn01);
		op.EndPath(opn11);
		op.linestyle = SketchLineStyle.SLS_INVISIBLE;
		AddPath(op);
		sketchdisplay.subsetpanel.PutToSubset(op, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op, sspapersubset, true); 

		op = new OnePath(opn11);
		op.EndPath(opn10);
		op.linestyle = SketchLineStyle.SLS_INVISIBLE;
		AddPath(op);
		sketchdisplay.subsetpanel.PutToSubset(op, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op, sspapersubset, true); 

		op = new OnePath(opn10);
		op.EndPath(opn00);
		op.linestyle = SketchLineStyle.SLS_INVISIBLE;
		AddPath(op);
		sketchdisplay.subsetpanel.PutToSubset(op, TN.framestylesubset, true); 
		sketchdisplay.subsetpanel.PutToSubset(op, sspapersubset, true); 

		RedrawBackgroundView();
	}


	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it from the selected sketch in the mainbox.
	void ImportSketch(OneSketch asketch, OneTunnel atunnel, boolean bOverwriteSubsetsOnCentreline, boolean bImportNoCentrelines)
	{
		if ((asketch == null) || (tsketch == asketch))
		{
			TN.emitWarning(asketch == null ? "Sketch not selected" : "Can't import sketch onto itself");
			return;
		}
		TN.emitMessage((bOverwriteSubsetsOnCentreline ? "" : "Not ") + "Overwriting subsets info on centrelines");

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

		if (bcorrespsucc && bOverwriteSubsetsOnCentreline)
		{
			for (PtrelPLn wptreli : ptrelln.wptrel)
			{
				wptreli.crp.vssubsets.clear();
				wptreli.crp.vssubsets.addAll(wptreli.cp.vssubsets);
			}
			TN.emitMessage("Finished copying centerline subsets");
		}

		if (!bcorrespsucc)
			TN.emitWarning("no centreline correspondence here");

		TN.emitWarning("Extending all nodes");
		ptrelln.PrepareProximity(asketch);
		ptrelln.PrepareForUnconnectedNodes(asketch.vnodes);
		ptrelln.Extendallnodes(asketch.vnodes);
		TN.emitWarning("Warping all paths");

		List<OnePath> cplist = new ArrayList<OnePath>();
		for (PtrelPLn wptreli : ptrelln.wptrel)
		{
			wptreli.crp.vssubsets.clear();
			wptreli.crp.vssubsets.addAll(wptreli.cp.vssubsets);
			cplist.add(wptreli.cp);
		}

		// warp over all the paths from the sketch
		int lastprogress = -1;
		int i = 0;
		for (OnePath op : asketch.vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (bImportNoCentrelines || cplist.contains(op)))
				continue;
			AddPath(ptrelln.WarpPath(op, atunnel.name));
			int progress = (20*i) / asketch.vpaths.size();
			i++;
			if (progress == lastprogress)
				continue;
			lastprogress = progress;
			TN.emitMessage("" + (5*progress) + "% complete at " + (new Date()).toString());
		}

		SketchChanged(SC_CHANGE_STRUCTURE);
		RedoBackgroundView();
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
		else if (bwritecoords || sketchdisplay.selectedsubsetstruct.bIsElevStruct)
			SetMPoint(e);

		if (bwritecoords)
		{
			sketchdisplay.infopanel.tfmousex.setText(String.valueOf(((float)moupt.getX() / TN.CENTRELINE_MAGNIFICATION) + tsketch.sketchLocOffset.x));
			sketchdisplay.infopanel.tfmousey.setText(String.valueOf((-(float)moupt.getY() / TN.CENTRELINE_MAGNIFICATION) + tsketch.sketchLocOffset.y));
		}

		if (sketchdisplay.selectedsubsetstruct.bIsElevStruct)
		{
			sketchdisplay.selectedsubsetstruct.AlongCursorMark(elevpoint, moupt); 
			btorepaint = true; 
		}
		
		if (btorepaint)
			repaint();
	}
		

	public void mouseClicked(MouseEvent e) {;}
	public void mouseEntered(MouseEvent e) {;};
	public void mouseExited(MouseEvent e) {;};


	/////////////////////////////////////////////
	void BackSel()
	{
		if ((currgenpath != null) && bmoulinactive)
		{
			Point2D bpt = currgenpath.BackOne();
			SetMouseLine(bpt, null);
		}

		else if (!vactivepaths.isEmpty())
			RemoveVActivePath(vactivepaths.get(vactivepaths.size() - 1));
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
			opselset.addAll(currselarea.connpathrootscen);
		}
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
	int AddPath(OnePath op)
	{
		op.SetSubsetAttrs(sketchdisplay.subsetpanel.sascurrent, sketchdisplay.vgsymbols, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
		tsvpathsviz.add(op);
		tsketch.rbounds.add(op.getBounds(null));
		int res = tsketch.TAddPath(op, sketchdisplay.vgsymbols);
		if ((sketchdisplay.selectedsubsetstruct.selevsubset != null) && op.bpathvisiblesubset)
		{
			sketchdisplay.selectedsubsetstruct.opelevarr.add(op); 
			sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		}	
		SketchChanged(SC_CHANGE_STRUCTURE);
		return res;
	}

	/////////////////////////////////////////////
	void RemovePath(OnePath path)
	{
		tsvpathsviz.remove(path);
		if (sketchdisplay.selectedsubsetstruct.selevsubset != null)
		{
			sketchdisplay.selectedsubsetstruct.opelevarr.remove(path); 
			sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		}	
		if (tsketch.TRemovePath(path))
			SketchChanged(SC_CHANGE_STRUCTURE);
	}


	/////////////////////////////////////////////
	void DeleteSel()
	{
		Set<OnePath> opselset = MakeTotalSelList(); 
		ClearSelection(false);
		for (OnePath op : opselset)
		{
			if (op.linestyle != SketchLineStyle.SLS_CENTRELINE)
				RemovePath(op);
		}
		RedrawBackgroundView();
	}



	/////////////////////////////////////////////
	static int SC_CHANGE_STRUCTURE = 100; 
	static int SC_CHANGE_AREAS = 101;
	static int SC_CHANGE_SYMBOLS = 102; 
	static int SC_CHANGE_PATHS = 103; 
	static int SC_CHANGE_SAS = 104; 
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
		if (!tsketch.bsketchfilechanged && ((scchangetyp == SC_CHANGE_STRUCTURE) || (scchangetyp == SC_CHANGE_AREAS) || (scchangetyp == SC_CHANGE_SYMBOLS) || (scchangetyp == SC_CHANGE_PATHS) || (scchangetyp == SC_CHANGE_BACKGROUNDIMAGE)))
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

		if ((scchangetyp == SC_CHANGE_STRUCTURE) || (scchangetyp == SC_CHANGE_AREAS) || (scchangetyp == SC_CHANGE_SYMBOLS) || (scchangetyp == SC_UPDATE_AREAS))
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
		activetunnel.UpdateSketchFrames(tsketch, SketchGraphics.SC_UPDATE_NONE, sketchdisplay.mainbox); 
		SketchChanged(SC_UPDATE_AREAS);
		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStringsT(tsketch);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	void UpdateSymbolLayout(boolean bAllSymbols)
	{
		boolean ballsymbolslayed; 
		if (bAllSymbols)
			ballsymbolslayed = tsketch.MakeSymbolLayout(null, null); 
		else
			ballsymbolslayed = tsketch.MakeSymbolLayout(new GraphicsAbstraction(mainGraphics), windowrect);
		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStringsT(tsketch);
		if (ballsymbolslayed)
			SketchChanged(SC_UPDATE_SYMBOLS);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	void FuseNodes(OnePathNode wpnstart, OnePathNode wpnend, boolean bShearWarp)
	{
		// find all paths that link into the first node and warp them to the second.
		// must be done backwards due to messing of the array
		for (int i = tsketch.vpaths.size() - 1; i >= 0; i--)
		{
			OnePath op = tsketch.vpaths.get(i);
			if ((op.pnstart == wpnstart) || (op.pnend == wpnstart))
			{
				RemovePath(op);
				OnePath opw = op.WarpPath(wpnstart, wpnend, bShearWarp);
				AddPath(opw);

				// relabel the node if necessary
				if (!tsketch.bSymbolType && (opw.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opw.plabedl != null))
					opw.UpdateStationLabelsFromCentreline();
			}
		}

		if (wpnstart.IsCentrelineNode())
		{
			assert wpnend.IsCentrelineNode();
			assert wpnend.pnstationlabel.equals(wpnstart.pnstationlabel);
			wpnend.zalt = wpnstart.zalt;
		}

		// copy any z-settings on this node
		assert wpnstart.pathcount == 0; // should have been removed
	}

	/////////////////////////////////////////////
	void FuseCurrent(boolean bShearWarp)
	{
		// fuse across a node if it's a sequence
		if (vactivepaths.size() >= 3)
			return;

		// fuse two edges (in a single selected chain)
		if (vactivepaths.size() == 2)
		{
			OnePath op1 = vactivepaths.get(0);
			OnePath op2 = vactivepaths.get(1);

			// work out node connection.
			OnePathNode pnconnect = null;
			if ((op1.pnend == op2.pnstart) || (op1.pnend == op2.pnend))
				pnconnect = op1.pnend;
			else if ((op1.pnstart == op2.pnstart) || (op1.pnstart == op2.pnend))
				pnconnect = op1.pnstart;
			else
				TN.emitProgError("Non connecting two paths");

			// decide whether to fuse if properties agree
			if ((pnconnect == null) || (pnconnect.pathcount != 2) || (op1.linestyle != op2.linestyle) || (op1.linestyle == SketchLineStyle.SLS_CENTRELINE))
				return;

			ClearSelection(true);
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

			// delete this warped path
			RemovePath(op1);
			RemovePath(op2);
			int iselpath = AddPath(opf);
			currgenpath = opf;
			DChangeBackNode();
			ObserveSelection(opf, null);
		}

		// fuse along a single edge
		else
		{
			// cases for throwing out the individual edge
			if ((currgenpath == null) || bmoulinactive || (currgenpath.nlines != 1) || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE) || (currgenpath.pnstart == currgenpath.pnend))
			{
				TN.emitWarning("Must have straight non-centreline ine selected");
				return;
			}
			if (currgenpath.pnstart.IsCentrelineNode() && currgenpath.pnend.IsCentrelineNode())
			{
				TN.emitWarning("Can't fuse two centreline nodes");
				return;
			}

			// the path to warp along
			OnePath warppath = currgenpath;
			ClearSelection(true);

			// delete this fused path
			RemovePath(warppath);
			FuseNodes(warppath.pnstart, warppath.pnend, bShearWarp);
		}
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		// invalidate.
		RedrawBackgroundView();
	}


	/////////////////////////////////////////////
// this could be used for finding other connectivity components
	void TranslateConnectedSet() // fusetranslate
	{
		if (!vactivepaths.isEmpty() || (currgenpath == null) || (currgenpath.pnend.pathcount != 1) || (currgenpath.pnstart.pathcount == 1) || bmoulinactive || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return;
		RemovePath(currgenpath);

		// make a stack of the connected component
		for (OnePathNode opn : tsketch.vnodes)
			opn.pathcountch = -1;

		Deque<OnePathNode> vstack = new ArrayDeque<OnePathNode>();
		vstack.addFirst(currgenpath.pnstart);

		int nvs = 0;
		while (!vstack.isEmpty())
		{
			nvs++;
			OnePathNode opn = vstack.removeFirst();
			opn.pathcountch = 0;

			// loop round the node
			OnePath op = opn.opconn;
			assert ((opn == op.pnend) || (opn == op.pnstart));
			boolean bFore = (op.pnend == opn);
			do
			{
				if (!bFore)
	        	{
					if (op.pnend.pathcountch == -1)
						vstack.addFirst(op.pnend);
					bFore = op.baptlfore;
					op = op.aptailleft;
				}
				else
				{
					if (op.pnstart.pathcountch == -1)
						vstack.addFirst(op.pnstart);
					bFore = op.bapfrfore;
					op = op.apforeright;
	        	}
				assert ((!bFore ? op.pnstart : op.pnend) == opn);
			}
			while (!((op == opn.opconn) && (bFore == (op.pnend == opn))));
		}

		// all nodes in this component are marked.  We now do the translation to everything (leaving them all in place)
		// hopefully this is not too dangerous
		System.out.println("Stacked nodes " + nvs);
		float vx = (float)(currgenpath.pnend.pn.getX() - currgenpath.pnstart.pn.getX());
		float vy = (float)(currgenpath.pnend.pn.getY() - currgenpath.pnstart.pn.getY());
		ClearSelection(true);

		// translate all the paths
		for (OnePath op : tsketch.vpaths)
		{
			if (op.pnstart.pathcountch == 0)
			{
				assert(op.pnend.pathcountch == 0);
				float[] pco = op.GetCoords();
				for (int j = 0; j <= op.nlines; j++)
				{
					pco[j * 2] += vx;
					pco[j * 2 + 1] += vy;
				}
				op.lpccon = null; // force rebuild of splice control points
				op.Spline(op.bWantSplined && !OnePath.bHideSplines, false);
			}
		}

		// translate all the nodes
		for (OnePathNode opn : tsketch.vnodes)
		{
			if (opn.pathcountch == 0)
			{
				opn.pn = new Point2D.Float((float)(opn.pn.getX() + vx), (float)(opn.pn.getY() + vy));
				opn.currstrokew = 0.0F; // so the rectangle gets rebuilt
			}
		}

		SketchChanged(SC_CHANGE_STRUCTURE);
		RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	void ReflectCurrent()
	{
		// cases for throwing out the individual edge
		if (!vactivepaths.isEmpty() || (currgenpath == null) || bmoulinactive || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return;

		// must maintain pointers to this the right way
		RemovePath(currgenpath);
		currgenpath.Spline(currgenpath.bSplined && !OnePath.bHideSplines, true);
		OnePathNode pnt = currgenpath.pnstart;
		currgenpath.pnstart = currgenpath.pnend;
		currgenpath.pnend = pnt;
		AddPath(currgenpath);

		SketchChanged(SC_CHANGE_PATHS);
		RedrawBackgroundView();
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

	/////////////////////////////////////////////
	void MakePitchUndercut()
	{
		if (bmoulinactive || (currgenpath == null) || (currgenpath.linestyle != SketchLineStyle.SLS_PITCHBOUND))
		{
			TN.emitWarning("Pitch undercut must have pitch boundary selected.");
			return;
		}

		OnePath opddconnstart = currgenpath.pnstart.GetDropDownConnPath();
		if (opddconnstart.pnend.pathcount == 0)
		{
			opddconnstart.vssubsets.addAll(currgenpath.vssubsets);
			AddPath(opddconnstart);
		}

		OnePath opddconnend = currgenpath.pnend.GetDropDownConnPath();
		if (opddconnend.pnend.pathcount == 0)
		{
			opddconnend.vssubsets.addAll(currgenpath.vssubsets);
			AddPath(opddconnend);
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
		AddPath(opinv);

		ClearSelection(true);
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
		bmoulinactive = false; // newly added
		DChangeBackNode();
		ObserveSelection(null, null);
		repaint();
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
	void SplitCurrpathNode()
	{
		OnePath op = currgenpath;
		OnePathNode pnmid = selpathnode;
		ClearSelection(true);

		RemovePath(op);
		OnePath currgenend = op.SplitNode(pnmid, linesnap_t);
		AddPath(op);
		AddPath(currgenend);
		currgenend.vssubsets.addAll(op.vssubsets);
		currgenend.vssubsetattrs.addAll(op.vssubsetattrs);
		currgenend.bpathvisiblesubset = op.bpathvisiblesubset;

		ObserveSelection(null, null);
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		RedrawBackgroundView();
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
	void SetIColsDefault()
	{
		for (OnePath op : tsketch.vpaths)
			op.zaltcol = null;
		for (OnePathNode opn : tsketch.vnodes)
			opn.icolindex = -1;

		RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	void SetIColsByZ()
	{
		// extract the zrange from what we see
		float zlo = 0.0F;
		float zhi = 0.0F;

		// scan through using the half-points of each vector
		boolean bfirst = true; 
		for (OnePath op : tsvpathsviz)
		{
			float z = (op.pnstart.zalt + op.pnend.zalt) / 2;
			if (bfirst || (z < zlo))
				zlo = z;
			if (bfirst || (z > zlo))
				zhi = z;
			bfirst = false; 
		}

		// the setting of the zalts is done from a menu auto command
		TN.emitMessage("zrange in view zlo " + zlo + "  zhi " + zhi);

		// now set the zalts on all the paths
		for (OnePath op : tsketch.vpaths)
		{
			float z = (op.pnstart.zalt + op.pnend.zalt) / 2;
			float a = (z - zlo) / (zhi - zlo);
			int icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
			op.zaltcol = SketchLineStyle.linestylecolsindex[icolindex];
		}

		// now set the zalts on all the paths
		for (OneSArea osa : tsketch.vsareas)
		{
			float a = (osa.zalt - zlo) / (zhi - zlo);
			int icolindex = Math.max(Math.min((int)(a * SketchLineStyle.areastylecolsindex.length), SketchLineStyle.areastylecolsindex.length - 1), 0);
			osa.zaltcol = SketchLineStyle.areastylecolsindex[icolindex]; // this doesn't get set back by the default -- remake the areas instead
		}

		// fill in the colours at the end-nodes
/*		for (OnePathNode opn : tsketch.vnodes)
		{
			float a = (opn.zalt - tsketch.zaltlo) / (tsketch.zalthi - tsketch.zaltlo);
			opn.icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
		}
*/
		RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	void SetIColsProximity(int style)
	{
		OnePathNode ops = (currpathnode != null ? currpathnode : (currgenpath != null ? currgenpath.pnstart : null));
		if (ops == null)
			return;

		// heavyweight stuff
		ProximityDerivation pd = new ProximityDerivation(tsketch);
		pd.ShortestPathsToCentrelineNodes(ops, null, null, false);

		double dlo = 0.0;
		double dhi = pd.distmax;

		if (style == 1)
		{
			dlo = pd.distmincnode;
			dhi = pd.distmaxcnode;
		}

		// separate out case
		if (dlo == dhi)
			dhi += dlo * 0.00001;

		// fill in the colours at the end-nodes
		for (OnePathNode opn : tsketch.vnodes)
		{
			double dp = opn.proxdist;
			double a = (dp - dlo) / (dhi - dlo);
			if (style == 0)
				a = 1.0 - a; // make red 0.0
			else if (style == 1)
			{
				if (dp <= dlo)
					a = 1.0;
				else
					a = (dlo * dlo) / (dp * dp);
			}
			opn.icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
		}

		// fill in the colours by averaging the distance at the end-nodes
		for (OnePath op : tsketch.vpaths)
		{
			int icolindex = (op.pnstart.icolindex + op.pnend.icolindex) / 2;
			op.zaltcol = SketchLineStyle.linestylecolsindex[icolindex];
		}
		RedrawBackgroundView();
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
		DChangeBackNode();
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
		if (currgenpath.EndPath(pnend))
		{
			AddPath(currgenpath);
			ObserveSelection(currgenpath, null);
			RedrawBackgroundView();
		}
		else
			currgenpath = null;

		bmoulinactive = false;
		DChangeBackNode();
		momotion = M_NONE;
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
			OnePathNode opns = new OnePathNode((float)moupt.getX(), (float)moupt.getY(), 0.0F);
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
			selpathnode = new OnePathNode((float)clpt.getX(), (float)clpt.getY(), 0.0F);
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
	void MoveGround(boolean bBackgroundOnly)
	{
		if ((currgenpath != null) && !bmoulinactive && (currgenpath.linestyle != SketchLineStyle.SLS_CENTRELINE))
			// && ((currgenpath.pnstart.pathcount == 1) && (currgenpath.pnend.pathcount == 1))
		{
			float[] pco = currgenpath.GetCoords();
			if (currgenpath.nlines == 1)
			{
				mdtrans.setToTranslation(pco[2] - pco[0], pco[3] - pco[1]);
			}
			else if (currgenpath.nlines == 2)
			{
				float x2 = pco[4] - pco[0];
				float y2 = pco[5] - pco[1];
				float x1 = pco[2] - pco[0];
				float y1 = pco[3] - pco[1];
				double len2 = Math.sqrt(x2 * x2 + y2 * y2);
				double len1 = Math.sqrt(x1 * x1 + y1 * y1);
				double len12 = len1 * len2;
				if (len12 == 0.0F)
					return;

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
				return;

			// this is the application.
			if (bBackgroundOnly)
			{
				backgroundimg.PreConcatBusiness(mdtrans);
				backgroundimg.PreConcatBusinessF(pco, currgenpath.nlines);
			}
			else
				currtrans.concatenate(mdtrans);

//			mdtrans.setToIdentity();
			RedoBackgroundView();
			DeleteSel();
		}
	}
}



