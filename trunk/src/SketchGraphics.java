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
import java.awt.geom.Rectangle2D.Double;

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
import java.util.Vector;
import java.util.Random;
import java.util.Date;

import java.awt.Color;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
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
class SketchGraphics extends JPanel implements MouseListener, MouseMotionListener
{
	SketchDisplay sketchdisplay;

	SketchPrint sketchprint = new SketchPrint();

	static int SELECTWINDOWPIX = 5;
	static int MOVERELEASEPIX = 20;

	// the sketch.
	OneSketch skblank = new OneSketch(null);
	OneSketch tsketch = skblank;
	OneTunnel activetunnel = null; // the tunnel to which the above sketch belongs.

	// cached paths of those on screen (used for speeding up of drawing during editing).
	Vector tsvpathsviz = new Vector();

	boolean bEditable = false;

	OnePath currgenpath = null;
	OneSArea currselarea = null;

	// the currently active mouse path information.
	Line2D.Float moulin = new Line2D.Float();
	GeneralPath moupath = new GeneralPath();

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

	// these are set from SketchSubsetPanel (match by object pointer)
	Vector vsselectedsubsets = new Vector();  // of Strings
	Vector vsaselected = new Vector(); // of SubsetAttr

	// the array of array of paths which are going to define a boundary
	Vector vactivepaths = new Vector();
	OnePathNode vapbegin = null; // endpoints pf active paths list.
	OnePathNode vapend = null;
	boolean bLastAddVActivePathBack = true; // used by the BackSel button to know which side to rollback the active paths.



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
	boolean bNextRenderPFrame = false;
	boolean bNextRenderAreaStripes = false;


	AffineTransform orgtrans = new AffineTransform();
	AffineTransform mdtrans = new AffineTransform();
	AffineTransform currtrans = new AffineTransform();
	double[] flatmat = new double[6];


	// used in correspondence problems
	List<OnePath> clpaths = new ArrayList();
	List<OnePath> corrpaths = new ArrayList();

	/////////////////////////////////////////////
	// trial to see if we can do good greying out of buttons.
	void DChangeBackNode()
	{
		sketchdisplay.acaBackNode.setEnabled(((currgenpath != null) && bmoulinactive) || (vactivepaths.size() != 0));
	}

	/////////////////////////////////////////////
	SketchGraphics(SketchDisplay lsketchdisplay)
	{
		super(false); // not doublebuffered

		setBackground(TN.wfmBackground);
		setForeground(TN.wfmLeg);

		addMouseListener(this);
		addMouseMotionListener(this);

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
			else
				mdtrans.setToIdentity();

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
	void ObserveSelection(int iselpath)
	{
		OnePath op = (iselpath == -1 ? null : (OnePath)tsketch.vpaths.elementAt(iselpath));
		sketchdisplay.sketchlinestyle.SetParametersIntoBoxes(op);
		if (sketchdisplay.bottabbedpane.getSelectedIndex() == 2)
		{
			sketchdisplay.infopanel.tfselitempathno.setText(iselpath == -1 ? "" : String.valueOf(iselpath + 1));
			sketchdisplay.infopanel.tfselnumpathno.setText(String.valueOf(tsketch.vpaths.size()));
			sketchdisplay.infopanel.SetPathXML(op);
		}
		sketchdisplay.subsetpanel.UpdateSubsetsOfPath();
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
			int iselpath = tsketch.SelPath(mainGraphics, selrect, currgenpath, tsvpathsviz);
			ClearSelection(true);
			if (iselpath != -1)
			{
				currgenpath = (OnePath)(tsketch.vpaths.elementAt(iselpath));
				DChangeBackNode();
				ObserveSelection(iselpath);
			}
			momotion = M_NONE;
		}

		// do the selection of areas
		if (momotion == M_SEL_AREA)
		{
			OneSArea lcurrselarea = tsketch.SelArea(mainGraphics, selrect, currselarea);
			ClearSelection(true);
			currselarea = lcurrselarea;
			ObserveSelection(-1);
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
			int iselpath = tsketch.SelPath(mainGraphics, selrect, pathaddlastsel, tsvpathsviz);

			// toggle in list.
			if (iselpath != -1)
			{
				OnePath selgenpath = (OnePath)(tsketch.vpaths.elementAt(iselpath));
				pathaddlastsel = selgenpath;
				if (IsActivePath(selgenpath))
				{
					RemoveVActivePath(selgenpath);
					ObserveSelection(-1);
				}
				else
				{
					AddVActivePath(selgenpath);
					ObserveSelection(iselpath);
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
			selpathnode = tsketch.SelNode(opfront, bopfrontvalid, mainGraphics, selrect, selpathnodecycle);

			if (selpathnode == null)
				momotion = M_NONE;
			else
				selpathnodecycle = selpathnode;
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
		sketchprint.PrintThis(prtscalecode, !sketchdisplay.miCentreline.isSelected(), sketchdisplay.miShowNodes.isSelected(), !sketchdisplay.miStationNames.isSelected(), sketchdisplay.vgsymbols, tsketch, csize, currtrans, sketchdisplay);
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
		boolean bClearBackground = ((tsketch.ibackgroundimgnamearrsel == -1) || !sketchdisplay.miShowBackground.isSelected());
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
			mainGraphics.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]);
			mainGraphics.fillRect(0, 0, csize.width, csize.height);
		}
		else
			mainGraphics.drawImage(backgroundimg.backimagedone, 0, 0, null);

		// as if the above was using this flag correctly
		if (ibackimageredo == 0)
			ibackimageredo = 1;

		// render the image on top of the background.
		TN.emitMessage("backimgdraw " + bkifrm++);

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
				for (int i = 0; i < tsketch.vpaths.size(); i++)
				{
					OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
					if (mainGraphics.hit(windowrect, op.gp, (op.linestyle != SketchLineStyle.SLS_FILLED)))
						tsvpathsviz.add(op);
				}
System.out.println("vizpaths " + tsvpathsviz.size() + " of " + tsketch.vpaths.size());
			}

			ibackimageredo = 2;
		}
		else
			assert(tsvpathsviz.isEmpty() || tsketch.vpaths.contains(tsvpathsviz.elementAt(0)));

		// the grid thing
		if (sketchdisplay.miShowGrid.isSelected() && (sketchgrid != null))
		{
			mainGraphics.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_CENTRELINE]); // thin
			mainGraphics.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_FILLED]); // black
			mainGraphics.draw(sketchgrid.gpgrid);
		}

		// draw the sketch according to what view we want (incl single frame of print quality)
		int stationnamecond = (sketchdisplay.miStationNames.isSelected() ? 1 : 0) + (sketchdisplay.miStationAlts.isSelected() ? 2 : 0);
		if (bNextRenderDetailed)
			tsketch.paintWquality(mainGraphics, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, !sketchdisplay.miStationNames.isSelected(), sketchdisplay.vgsymbols);
		else
			tsketch.paintWbkgd(mainGraphics, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, stationnamecond, sketchdisplay.vgsymbols, tsvpathsviz);

		// all back image stuff done.  Now just the overlays.
		ibackimageredo = 3;
	}



	AffineTransform id = new AffineTransform();
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
		g2D.transform(currtrans);
		g2D.setFont(sketchdisplay.sketchlinestyle.defaultfontlab);

		for (int i = 0; i < vactivepaths.size(); i++)
		{
			Vector vp = (Vector)(vactivepaths.elementAt(i));
			for (int j = 0; j < vp.size(); j++)
			{
				OnePath op = (OnePath)vp.elementAt(j);
				op.paintW(g2D, false, true);

				// find out if the node between this and the previous should be coloured.
				OnePath opp = (OnePath)vp.elementAt(j == 0 ? vp.size() - 1 : j - 1);
				g2D.setColor(SketchLineStyle.linestylecolactive);

				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);

				if ((op.pnstart == opp.pnend) || (op.pnstart == opp.pnstart))
					g2D.draw(op.pnstart.Getpnell());
				else if ((op.pnend == opp.pnend) || (op.pnend == opp.pnstart))
					g2D.draw(op.pnend.Getpnell());
				else if (j != 0)
					TN.emitProgError("active lath loop non-connecting nodes");
			}
		}

		// the current node
		if ((momotion == M_SKET_SNAPPED) && (currpathnode != null))
		{
			g2D.setColor(SketchLineStyle.linestylecolactive);
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.draw(currpathnode.Getpnell());
			if ((currpathnode.pnstationlabel != null) && sketchdisplay.miStationNames.isSelected())
			{
				g2D.setFont(SketchLineStyle.defaultfontlab);
				g2D.drawString(currpathnode.pnstationlabel, (float)currpathnode.pn.getX() + SketchLineStyle.strokew * 2, (float)currpathnode.pn.getY() - SketchLineStyle.strokew);
			}

			if (!bmoulinactive)
				g2D.draw(moupath); // moulin
		}


		// draw the selected/active paths.
		g2D.setColor(TN.wfmnameActive);
		if (currgenpath != null)
		{
			// draw the symbols on this path
			for (int j = 0; j < currgenpath.vpsymbols.size(); j++)
			{
				OneSSymbol msymbol = (OneSSymbol)currgenpath.vpsymbols.elementAt(j);
				msymbol.paintW(g2D, true, false);
			}

			// draw the endpoints different colours so we can determin handedness.
			if (currgenpath.pnstart != null)
			{
				g2D.setColor(SketchLineStyle.linestylecolactivefnode);
				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
				g2D.draw(currgenpath.pnstart.Getpnell());
			}
			if (currgenpath.pnend != null)
			{
				g2D.setColor(SketchLineStyle.linestylecolactivemoulin);
				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
				g2D.draw(currgenpath.pnend.Getpnell());
			}

			currgenpath.paintW(g2D, false, true);
		}

		// draw in the selected area outline (what will be put into the subset).
		if (currselarea != null)
		{
			for (int i = 0; i < (int)currselarea.refpaths.size(); i++)
				((RefPathO)currselarea.refpaths.elementAt(i)).op.paintW(g2D, false, true);
			for (int i = 0; i < currselarea.ccalist.size(); i++)
			{
				ConnectiveComponentAreas cca = (ConnectiveComponentAreas)currselarea.ccalist.elementAt(i);
				for (int j = 0; j < cca.vconnpaths.size(); j++)
					((RefPathO)cca.vconnpaths.elementAt(j)).op.paintW(g2D, false, true);
			}
		}

		// draw the rubber band.
		if (bmoulinactive)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(SketchLineStyle.linestylecolactive);
			g2D.draw(moupath);  // moulin
		}


		// deal with the overlays
		bNextRenderDetailed = false;
		// draw the areas in hatched
		if (bNextRenderAreaStripes)
		{
			Vector lvsareas = tsketch.vsareas;
			if ((currgenpath != null) && (currgenpath.iconncompareaindex != -1))
				lvsareas = tsketch.sksya.GetCconnAreas(currgenpath.iconncompareaindex);
			for (int i = 0; i < lvsareas.size(); i++)
			{
				OneSArea osa = (OneSArea)lvsareas.elementAt(i);
				osa.paintHatchW(g2D, i, lvsareas.size());
			}
			bNextRenderAreaStripes = false;
		}

		// paint the down sketches that we are going to import (this is a preview).
		// this messes up the g2d.transform.
		if (bNextRenderPinkDownSketch)
		{
			OneSketch lselectedsketch = sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchLoad(); 
			if (lselectedsketch != null)
				paintSelectedSketches(g2D, sketchdisplay.mainbox.tunnelfilelist.activetunnel, lselectedsketch);
			else
				TN.emitWarning("No sketch selected");
			bNextRenderPinkDownSketch = false;
		}

		if (bNextRenderPFrame && (currgenpath != null))
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_WALL]);
			g2D.setColor(SketchLineStyle.linestylecolactive);
			sketchdisplay.sketchmakeframe.previewFrame(g2D, currgenpath);
		}
		bNextRenderPFrame = false;
	}


	/////////////////////////////////////////////
	void WriteHPGL(boolean bThicklines)
	{
		// draw all the paths inactive.
		Rectangle2D bounds = new Rectangle2D.Float();
		for (int i = 0; i < tsketch.vpaths.size(); i++)
		{
			OnePath path = (OnePath)(tsketch.vpaths.elementAt(i));
			if (bThicklines ? ((path.linestyle == SketchLineStyle.SLS_WALL) || (path.linestyle == SketchLineStyle.SLS_ESTWALL)) :
							  ((path.linestyle == SketchLineStyle.SLS_PITCHBOUND) || (path.linestyle == SketchLineStyle.SLS_CEILINGBOUND) || (path.linestyle == SketchLineStyle.SLS_DETAIL)))
				System.out.println(path.writeHPGL());

			if (i == 0)
				bounds = path.gp.getBounds2D();
			else
				bounds.add(path.gp.getBounds2D());
		}

		System.out.println("");
		System.out.println(" Real bounds " + bounds.toString());
	}

	/////////////////////////////////////////////
	void ImportFrameSketch()
	{
		sketchdisplay.sketchmakeframe.addpreviewFrame(this, currgenpath);
		SketchChanged(0, true);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// xsectioncode = 0 for none, 1 for plan, 2 for elev
	void ImportSketchCentreline(boolean bcopytitles)
	{
		// protect there being centrelines in this sketch already
		// (should always make new and warp over)
		boolean bnoimport = tsketch.bSymbolType;
		for (int i = 0; i < tsketch.vpaths.size(); i++)
			bnoimport |= (((OnePath)tsketch.vpaths.elementAt(i)).linestyle == SketchLineStyle.SLS_CENTRELINE);

		// although if they are compatible, it would be fair to bring in just extra centrelines
		if (bnoimport)
		{
			TN.emitWarning("no centreline import where there are centrelines or symbol type");
			return;
		}

		if (bcopytitles)
			TN.emitMessage("Importing with *titles setting the subsets");

		// this otglobal was set when we opened this window.
		// this is the standard upper tunnel that station calculations are sucked into.
		OneTunnel otfrom = sketchdisplay.mainbox.otglobal;

		// calculate when we import
		sketchdisplay.mainbox.sc.CopyRecurseExportVTunnels(otfrom, sketchdisplay.mainbox.tunnelfilelist.activetunnel, true);
		sketchdisplay.mainbox.sc.CalcStationPositions(otfrom, null); // calculate

		// parallel array of new nodes
		OnePathNode[] statpathnode = new OnePathNode[otfrom.vstations.size()];
		for (int i = 0; i < otfrom.vlegs.size(); i++)
		{
			OneLeg ol = (OneLeg)(otfrom.vlegs.elementAt(i));
			if (ol.osfrom != null)
			{
				int ipns = otfrom.vstations.indexOf(ol.osfrom);
				int ipne = otfrom.vstations.indexOf(ol.osto);

				if ((ipns != -1) || (ipne != -1))
				{
					if (statpathnode[ipns] == null)
						statpathnode[ipns] = new OnePathNode(ol.osfrom.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osfrom.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osfrom.Loc.z * TN.CENTRELINE_MAGNIFICATION, true);
					if (statpathnode[ipne] == null)
						statpathnode[ipne] = new OnePathNode(ol.osto.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osto.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osto.Loc.z * TN.CENTRELINE_MAGNIFICATION, true);

					OnePath op = new OnePath(statpathnode[ipns], ol.osfrom.name, statpathnode[ipne], ol.osto.name);
					if (bcopytitles && (ol.svxtitle != null) && !ol.svxtitle.equals(""))
						op.vssubsets.addElement(ol.svxtitle);
					AddPath(op);
					op.UpdateStationLabelsFromCentreline();
				}
				else
					TN.emitWarning("Can't find station " + ol.osfrom + " or " + ol.osto);
			}
		}

		asketchavglast = null; // change of avg transform cache.
		SketchChanged(0, true);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// this builds a little miniature version of the centreline in elevation
	void CopySketchCentreline(float angdeg, float scalefac)
	{
		float cosa = (float)Math.cos(angdeg * Math.PI / 180);
		float sina = (float)Math.sin(angdeg * Math.PI / 180);

		// use the pathcountch flag to mark down the nodes as we meet them
		for (int i = 0; i < tsketch.vnodes.size(); i++)
			((OnePathNode)tsketch.vnodes.elementAt(i)).pathcountch = -1;

		int nvpaths = tsketch.vpaths.size();
		for (int i = 0; i < nvpaths; i++)
		{
			OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				OnePathNode pnstart;
				if (op.pnstart.pathcountch == -1)
					pnstart = new OnePathNode((float)(op.pnstart.pn.getX() * cosa - op.pnstart.pn.getY() * sina) * scalefac, -op.pnstart.zalt * scalefac, (float)(-op.pnstart.pn.getX() * sina - op.pnstart.pn.getY() * cosa) * scalefac, true);
				else
					pnstart = (OnePathNode)tsketch.vnodes.elementAt(op.pnstart.pathcountch);

				OnePathNode pnend;
				if (op.pnend.pathcountch == -1)
					pnend = new OnePathNode((float)(op.pnend.pn.getX() * cosa - op.pnend.pn.getY() * sina) * scalefac, -op.pnend.zalt * scalefac, (float)(-op.pnend.pn.getX() * sina - op.pnend.pn.getY() * cosa) * scalefac, true);
				else
					pnend = (OnePathNode)tsketch.vnodes.elementAt(op.pnend.pathcountch);

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

		SketchChanged(4, true);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it into the selected sketch in the mainbox.
	// hang on, that's the wrong way round, isn't it? -- DL
	void ImportSketch(OneSketch asketch, OneTunnel atunnel)
	{
		if ((asketch == null) || (tsketch == asketch))
		{
			TN.emitWarning(asketch == null ? "Sketch not selected" : "Can't import sketch onto itself");
			return;
		}

		int dresponse = JOptionPane.showConfirmDialog(sketchdisplay, "Import centerline subset information?", "Import Sketch", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if(dresponse == JOptionPane.CANCEL_OPTION) return;
		boolean bRetrofitSubsets = (dresponse == JOptionPane.YES_OPTION);


		// all in one find the centreline paths and the corresponding paths we will export to.
		boolean bcorrespsucc = asketch.ExtractCentrelinePathCorrespondence(atunnel, clpaths, corrpaths, tsketch, activetunnel);

		// clpaths is the list of paths in the imported sketch. corrpaths is the corresponding paths in the new sketch.

		TN.emitWarning("Finished finding centerline correspondence");

		if (bcorrespsucc && bRetrofitSubsets)
		{
			for(int i = 0; i < clpaths.size(); i++) corrpaths.get(i).vssubsets.addAll(clpaths.get(i).vssubsets);
			TN.emitWarning("Finished copying centerline subsets");
		}


		if (!bcorrespsucc)
			TN.emitWarning("no centreline correspondence here");

		TN.emitWarning("Extending all nodes");
		PtrelLn ptrelln = new PtrelLn((bcorrespsucc ? clpaths : null), (bcorrespsucc ? corrpaths : null), asketch);
		ptrelln.Extendallnodes(asketch.vnodes);

		TN.emitWarning("Warping all paths");

		// warp over all the paths from the sketch
		int lastprogress = -1;
		Date keeptime;
		for (int i = 0; i < asketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)asketch.vpaths.elementAt(i);
			if (op.linestyle != SketchLineStyle.SLS_CENTRELINE)
				AddPath(ptrelln.WarpPath(op, atunnel.name));

			int progress = (20*i) / asketch.vpaths.size();
			if ( progress > lastprogress )
			{
				lastprogress = progress;
				keeptime = new Date();
				TN.emitMessage(Integer.toString(5*progress) + "% complete at " + keeptime.toString());
			}

		}

		SketchChanged(0, true);
		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it into the selected sketch in the mainbox.
	AffineTransform avgtrans = new AffineTransform();
	OneSketch asketchavglast = null; // used for lazy evaluation.

	void paintSelectedSketches(Graphics2D g2D, OneTunnel atunnel, OneSketch asketch)
	{
		// find new transform if it's a change.
		if (asketch != asketchavglast)
		{
			// lets us see import from sketches with no correspondence
			boolean bcorrespsucc = asketch.ExtractCentrelinePathCorrespondence(atunnel, clpaths, corrpaths, tsketch, activetunnel);
			PtrelLn.CalcAvgTransform(avgtrans, (bcorrespsucc ? clpaths : null), (bcorrespsucc ? corrpaths : null));
			asketchavglast = asketch;
		}

		// now work from known transform
		g2D.transform(avgtrans);

		// draw all the paths inactive.
		for (int i = 0; i < asketch.vpaths.size(); i++)
		{
			OnePath path = (OnePath)(asketch.vpaths.elementAt(i));
			if (path.linestyle != SketchLineStyle.SLS_CENTRELINE) // of have it unhidden?
				path.paintW(g2D, true, true);
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
	int nsampsides = 20;
	int nsampsidesmid = 30;

	boolean IsInBlack(int j)
	{
		return IsInBlack(ptlx + perpx * j, ptly + perpy * j);
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
					if (nmoupathpieces != 1)
					{
						perpy = 2.5F * (float)(smpt1.getY() - smpt0.getY()) / moulinmleng;
						perpx = -2.5F * (float)(smpt1.getX() - smpt0.getX()) / moulinmleng;
					}
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


			smidpt.setLocation(ptlx + perpx * fbm, ptly + perpy * fbm);

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
	}


	/////////////////////////////////////////////
	public void mouseMoved(MouseEvent e)
	{
		boolean bwritecoords = (sketchdisplay.bottabbedpane.getSelectedIndex() == 2);
		if (bmoulinactive || (momotion == M_SKET_SNAPPED))
		{
			SetMPoint(e);
			if (bmoulinactive)
				SetMouseLine(null, moupt);
			if (momotion == M_SKET_SNAPPED)
				selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);

			// movement not in a drag.
			else if ((momotion != M_SKET) && sketchdisplay.miTabletMouse.isSelected() && (moulinmleng > MOVERELEASEPIX))
			{
				moulinmleng = 0;
				EndCurve(null);
			}

			repaint();
		}
		else if (bwritecoords)
			SetMPoint(e);

		if (bwritecoords)
		{
			sketchdisplay.infopanel.tfmousex.setText(String.valueOf((float)moupt.getX() / TN.CENTRELINE_MAGNIFICATION));
			sketchdisplay.infopanel.tfmousey.setText(String.valueOf(-(float)moupt.getY() / TN.CENTRELINE_MAGNIFICATION));
		}
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

		else if (vactivepaths.size() != 0)
		{
			Vector vp = (Vector)(vactivepaths.lastElement());
			OnePath path = (OnePath)(bLastAddVActivePathBack ? vp.lastElement() : vp.firstElement());
			RemoveVActivePath(path);
		}
	}


	/////////////////////////////////////////////
	void Deselect(boolean bStrong)
	{
		if (bmoulinactive)
		{
			moulinmleng = 0;
			EndCurve(null);
		}
		ClearSelection(true);
	}


	/////////////////////////////////////////////
	int AddPath(OnePath op)
	{
		op.SetSubsetAttrs(sketchdisplay.subsetpanel.sascurrent, sketchdisplay.vgsymbols);
		tsvpathsviz.add(op);
		tsketch.rbounds.add(op.getBounds(null));
		int res = tsketch.TAddPath(op, sketchdisplay.vgsymbols);
		SketchChanged(0, true);
		return res;
	}
	/////////////////////////////////////////////
	void RemovePath(OnePath path)
	{
		tsvpathsviz.remove(path);
		if (tsketch.TRemovePath(path))
			SketchChanged(0, true);
	}


	/////////////////////////////////////////////
	void DeletePath(OnePath path)
	{
		// don't delete a centreline type
		if (path.linestyle == SketchLineStyle.SLS_CENTRELINE)
			return;

		RemovePath(path);
		RedrawBackgroundView();
	}



	/////////////////////////////////////////////
	void DeleteSel()
	{
		OnePath lcurrgenpath = (bmoulinactive ? null : currgenpath);
		ClearSelection(false);
		if (bEditable && (lcurrgenpath != null))
			DeletePath(lcurrgenpath);
		else
			repaint();
	}



	/////////////////////////////////////////////
	// typ  0  everything, 1 nodez, 2 areasupdated, 3 symbolsupdated
	void SketchChanged(int typ, boolean lbsketchfilechanged)
	{
		// case of changing the actual file which needs to be saved
		if (lbsketchfilechanged && !tsketch.bsketchfilechanged)
		{
			sketchdisplay.mainbox.tunnelfilelist.repaint();
			tsketch.bsketchfilechanged = true;
		}

		// case of how much of the file needs rebuilding
		if (typ == 0)
		{
			tsketch.bSAreasUpdated = false;
			tsketch.bSymbolLayoutUpdated = false;

			sketchdisplay.acaSetZonnodes.setEnabled(true);
			sketchdisplay.acaUpdateSAreas.setEnabled(true);
			sketchdisplay.acaUpdateSymbolLayout.setEnabled(true);
		}
		else if (typ == 1)
		{
			tsketch.bSAreasUpdated = false;
			tsketch.bSymbolLayoutUpdated = false;

			sketchdisplay.acaSetZonnodes.setEnabled(false);
			sketchdisplay.acaUpdateSAreas.setEnabled(true);
			sketchdisplay.acaUpdateSymbolLayout.setEnabled(true);
		}
		else if (typ == 2)
		{
			tsketch.bSAreasUpdated = true;
			tsketch.bSymbolLayoutUpdated = false;
			sketchdisplay.acaUpdateSAreas.setEnabled(false);
			sketchdisplay.acaUpdateSymbolLayout.setEnabled(true);
		}
		else if (typ == 3)
		{
			tsketch.bSymbolLayoutUpdated = true;
			sketchdisplay.acaUpdateSymbolLayout.setEnabled(false);
		}
		else
			assert typ == 4;
	}

	/////////////////////////////////////////////
	void UpdateSAreas()
	{
		tsketch.MakeAutoAreas();  // once it is on always this will be unnecessary.
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);
		activetunnel.UpdateSketchFrames(tsketch); 

		// used to be part of the Update symbols areas, but brought here
		// so we have a full set of paths associated to each area available
		// for use to pushing into subsets.
		tsketch.MakeConnectiveComponents();

		SketchChanged(2, false);

		for (int i = 0; i < tsketch.vsareas.size(); i++)
			((OneSArea)tsketch.vsareas.elementAt(i)).SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);

		tsketch.SetSubsetVisibleCodeStrings(vsselectedsubsets, sketchdisplay.miInverseSubset.isSelected());

		RedoBackgroundView();
	}

	/////////////////////////////////////////////
	void UpdateSymbolLayout(boolean bAllSymbols)
	{
		boolean ballsymbolslayed = (bAllSymbols ? tsketch.MakeSymbolLayout(null, null) : tsketch.MakeSymbolLayout(mainGraphics, windowrect));
		tsketch.SetSubsetVisibleCodeStrings(vsselectedsubsets, sketchdisplay.miInverseSubset.isSelected());
		if (ballsymbolslayed)
			SketchChanged(3, true);
		RedoBackgroundView();
	}


	/////////////////////////////////////////////
	void FuseCurrent(boolean bShearWarp)
	{
		// fuse across a node if it's a sequence
		if (vactivepaths.size() > 1)
			return;

		// fuse two edges (in a single selected chain)
		else if (vactivepaths.size() == 1)
		{
			Vector vp = (Vector)(vactivepaths.elementAt(0));
			if (vp.size() != 2)
				return;

			OnePath op1 = (OnePath)vp.elementAt(0);
			OnePath op2 = (OnePath)vp.elementAt(1);

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
			for (int i = 0; i < op2.vssubsets.size(); i++)
			{
				String ssub = (String)op2.vssubsets.elementAt(i);
				int j = 0;
				for ( ; j < opf.vssubsets.size(); j++)
					if (ssub.equals((String)opf.vssubsets.elementAt(j)))
						break;
				if (j == opf.vssubsets.size())
					opf.vssubsets.addElement(ssub);
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
			ObserveSelection(iselpath);
		}

		// fuse along a single edge
		else
		{
			// cases for throwing out the individual edge
			if ((currgenpath == null) || bmoulinactive || (currgenpath.nlines != 1) ||
				(currgenpath.pnstart == currgenpath.pnend) ||
				(currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE) ||
				(currgenpath.pnstart.bzaltset && currgenpath.pnend.bzaltset)) // fusing two stations
				return;

			// the path to warp along
			OnePath warppath = currgenpath;
			ClearSelection(true);

			// delete this fused path
			RemovePath(warppath);

			// find all paths that link into the first node and warp them to the second.
			// must be done backwards due to messing of the array
			for (int i = tsketch.vpaths.size() - 1; i >= 0; i--)
			{
				OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
				if ((op.pnstart == warppath.pnstart) || (op.pnend == warppath.pnstart))
				{
					RemovePath(op);
					OnePath opw = op.WarpPath(warppath.pnstart, warppath.pnend, bShearWarp);
					AddPath(opw);

					// relabel the node if necessary
					if (!tsketch.bSymbolType && (opw.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opw.plabedl != null))
						opw.UpdateStationLabelsFromCentreline();
				}
			}

			// copy any z-settings on this node
			assert warppath.pnstart.pathcount == 0; // should have been removed
			if (warppath.pnstart.bzaltset)
			{
				assert !warppath.pnend.bzaltset;
				warppath.pnend.bzaltset = true;
				warppath.pnend.zalt = warppath.pnstart.zalt;
System.out.println("copying fuzed z " + warppath.pnend.zalt);
			}
		}
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		// invalidate.
		RedrawBackgroundView();
	}


	/////////////////////////////////////////////
	void TranslateConnectedSet() // fusetranslate
	{
		if (!vactivepaths.isEmpty() || (currgenpath == null) || (currgenpath.pnend.pathcount != 1) || (currgenpath.pnstart.pathcount == 1) || bmoulinactive || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return;
		RemovePath(currgenpath);

		// make a stack of the connected component
		for (int i = 0; i < tsketch.vnodes.size(); i++)
			((OnePathNode)tsketch.vnodes.elementAt(i)).pathcountch = -1;

		Vector vstack = new Vector();
		vstack.addElement(currgenpath.pnstart);
		int nvs = 0;
		while (!vstack.isEmpty())
		{
			nvs++;
			// pop back
			OnePathNode opn = (OnePathNode)vstack.lastElement();
			vstack.setSize(vstack.size() - 1);
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
						vstack.addElement(op.pnend);
					bFore = op.baptlfore;
					op = op.aptailleft;
				}
				else
				{
					if (op.pnstart.pathcountch == -1)
						vstack.addElement(op.pnstart);
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
		for (int i = 0; i < tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
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
				op.Spline(op.bWantSplined, false);
			}
		}

		// translate all the nodes
		for (int i = 0; i < tsketch.vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)tsketch.vnodes.elementAt(i);
			if (opn.pathcountch == 0)
			{
				opn.pn = new Point2D.Float((float)(opn.pn.getX() + vx), (float)(opn.pn.getY() + vy));
				opn.currstrokew = 0.0F; // so the rectangle gets rebuilt
			}
		}

		SketchChanged(0, true);
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
		currgenpath.Spline(currgenpath.bSplined, true);
		OnePathNode pnt = currgenpath.pnstart;
		currgenpath.pnstart = currgenpath.pnend;
		currgenpath.pnend = pnt;
		AddPath(currgenpath);

		SketchChanged(4, true);
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
		bmoulinactive = false; // newly added
		DChangeBackNode();
		ObserveSelection(-1);
		repaint();
	}

	/////////////////////////////////////////////
	boolean IsActivePath(OnePath path)
	{
		for (int i = 0; i < vactivepaths.size(); i++)
		{
			Vector vp = (Vector)(vactivepaths.elementAt(i));
			if (vp.contains(path))
				return true;
		}
		return false;
	}


	/////////////////////////////////////////////
	boolean RemoveVActivePath(OnePath path)
	{
		if (vactivepaths.size() == 0)
			return false;

		// break into a loop -- remove the entire loop.
		if (vapbegin == null)
		{
			for (int i = 0; i < vactivepaths.size(); i++)
			{
				Vector vp = (Vector)(vactivepaths.elementAt(i));
				if (vp.contains(path))
				{
					vactivepaths.removeElementAt(i);
					DChangeBackNode();
					return true;
				}
			}
			return false;
		}


		Vector vp = (Vector)(vactivepaths.lastElement());

		// last element of a loop.
		if (vp.size() == 1)
		{
			if (vp.firstElement() == path)
			{
				vactivepaths.removeElementAt(vactivepaths.size() - 1);
				DChangeBackNode();
				vapbegin = null;
				return true;
			}
			return false;
		}

		// knock off one of the ends.
		if (vp.lastElement() == path)
		{
			vp.removeElementAt(vp.size() - 1);
			vapend = (vapend == path.pnstart ? path.pnend : path.pnstart);
			bLastAddVActivePathBack = true;
			return true;
		}

		if (vp.firstElement() == path)
		{
			vp.removeElementAt(0);
			vapbegin = (vapbegin == path.pnstart ? path.pnend : path.pnstart);
			bLastAddVActivePathBack = false;
			return true;
		}

		return false;
	}


	/////////////////////////////////////////////
	boolean AddVActivePath(OnePath path)
	{
		// insert start of new cycle.
		if ((vactivepaths.size() == 0) || (vapbegin == null))
		{
			Vector vp = new Vector();
			vactivepaths.addElement(vp);
			DChangeBackNode();
			vp.addElement(path);
			if (path.pnstart != path.pnend)
			{
				vapbegin = path.pnstart;
				vapend = path.pnend;
			}
			else
				vapbegin = null;
			return true;
		}

		// join into path.
		boolean bJoinFront = ((vapbegin == path.pnstart) || (vapbegin == path.pnend));
		boolean bJoinBack = ((vapend == path.pnstart) || (vapend == path.pnend));
		Vector vp = (Vector)(vactivepaths.lastElement());

		// loop
		if (bJoinFront && bJoinBack)
		{
			vp.addElement(path);
			vapbegin = null;
			return true;
		}

		if (bJoinBack)
		{
			vp.addElement(path);
			vapend = (vapend == path.pnstart ? path.pnend : path.pnstart);
			bLastAddVActivePathBack = true;
			return true;
		}

		if (bJoinFront)
		{
			vp.insertElementAt(path, 0);
			vapbegin = (vapbegin == path.pnstart ? path.pnend : path.pnstart);
			bLastAddVActivePathBack = false;
			return true;
		}

		return false;
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

		ObserveSelection(-1);
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
		for (int i = 0; i < tsketch.vpaths.size(); i++)
			((OnePath)tsketch.vpaths.elementAt(i)).zaltcol = null;
		for (int i = 0; i < tsketch.vnodes.size(); i++)
			((OnePathNode)tsketch.vnodes.elementAt(i)).icolindex = -1;

		RedrawBackgroundView();
	}

	/////////////////////////////////////////////
	void SetIColsByZ(boolean bFromVisiblePaths)
	{
		// extract the zrange from what we see
		Vector vpa = (bFromVisiblePaths ? tsvpathsviz : tsketch.vpaths);
		float zlo = 0.0F;
		float zhi = 0.0F;

		// scan through using the half-points of each vector
		for (int i = 0; i < vpa.size(); i++)
		{
			OnePath op = (OnePath)vpa.elementAt(i);
			float z = (op.pnstart.zalt + op.pnend.zalt) / 2;
			if ((i == 0) || (z < zlo))
				zlo = z;
			if ((i == 0) || (z > zlo))
				zhi = z;
		}

		// the setting of the zalts is done from a menu auto command
		TN.emitMessage("zrange in view zlo " + zlo + "  zhi " + zhi);

		// now set the zalts on all the paths
		for (int i = 0; i < tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
			float z = (op.pnstart.zalt + op.pnend.zalt) / 2;
			float a = (z - zlo) / (zhi - zlo);
			int icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
			op.zaltcol = SketchLineStyle.linestylecolsindex[icolindex];
		}

		// now set the zalts on all the paths
		for (int i = 0; i < tsketch.vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)tsketch.vsareas.elementAt(i);
			float a = (osa.zalt - zlo) / (zhi - zlo);
			int icolindex = Math.max(Math.min((int)(a * SketchLineStyle.areastylecolsindex.length), SketchLineStyle.areastylecolsindex.length - 1), 0);
			osa.zaltcol = SketchLineStyle.areastylecolsindex[icolindex]; // this doesn't get set back by the default -- remake the areas instead
		}

		// fill in the colours at the end-nodes
/*		for (int i = 0; i < tsketch.vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)tsketch.vnodes.elementAt(i);
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
		ProximityDerivation pd = new ProximityDerivation(tsketch, true);
		pd.ShortestPathsToCentrelineNodes(ops, null);

		float dlo = 0.0F;
		float dhi = pd.distmax;

		if (style == 1)
		{
			dlo = pd.distmincnode;
			dhi = pd.distmaxcnode;
		}

		// separate out case
		if (dlo == dhi)
			dhi += dlo * 0.00001F;

		// fill in the colours at the end-nodes
		for (int i = 0; i < tsketch.vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)tsketch.vnodes.elementAt(i);
			float dp = opn.proxdist;
			float a = (dp - dlo) / (dhi - dlo);
			if (style == 0)
				a = 1.0F - a; // make red 0.0
			else if (style == 1)
			{
				if (dp <= dlo)
					a = 1.0F;
				else
					a = (dlo * dlo) / (dp * dp);
			}
			opn.icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
		}

		// fill in the colours by averaging the distance at the end-nodes
		for (int i = 0; i < tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
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
		if (moulinmleng != 0)
		{	currgenpath.IntermedLines(moupath, nmoupathpieces);
			if (pnend == null)
				currgenpath.LineTo((float)moupt.getX(), (float)moupt.getY());
		}

		if (currgenpath.EndPath(pnend))
		{
			AddPath(currgenpath);
			ObserveSelection(tsketch.vpaths.size() - 1);
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
	public void mousePressed(MouseEvent e)
	{
		//TN.emitMessage(e.getModifiers());
		//TN.emitMessage("B1 " + e.BUTTON1_MASK + " B2 " + e.BUTTON2_MASK + " B3 " + e.BUTTON3_MASK + " ALT " + e.ALT_MASK + " META " + e.META_MASK + " MetDown " + e.isMetaDown());


		// are we in the whole picture dragging mode?  (middle mouse button).
		if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
		{
			// if a point is already being dragged, then this second mouse press will delete it.
// this doesn't ever get called!
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
				momotion = (e.isShiftDown() ? M_DYN_DRAG : (e.isControlDown() ? M_DYN_SCALE : (sketchdisplay.miEnableRotate.isSelected() ? M_DYN_ROT : M_NONE)));
			return;
		}

		// non-dragging mode.  what kind of motion?
		if (!e.isMetaDown() && bEditable)
		{
			// there's going to be a very special case with sket_snap.
			SetMPoint(e);

			// M_SKET
			if (!e.isShiftDown() && !e.isControlDown())
			{
				momotion = M_SKET;
				if (!bmoulinactive)
				{
					ClearSelection(true);
					OnePathNode opns = new OnePathNode((float)moupt.getX(), (float)moupt.getY(), 0.0F, false);
					opns.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
					StartCurve(opns);
				}
				else
					LineToCurve();
			}


			// M_SKET_SNAP
			else if (e.isControlDown())
			{
				momotion = M_SKET_SNAP;
				linesnap_t = -1.0;
				selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);

				if (e.isShiftDown())
				{
					double scale = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
					if ((currgenpath != null) && !bmoulinactive)
					{
						// the node splitting one. only on edges if shift is down(over-ride with shift down)
						linesnap_t = currgenpath.ClosestPoint(moupt.getX(), moupt.getY(), 5.0 / scale);
						if ((currgenpath.linestyle != SketchLineStyle.SLS_CENTRELINE) && (linesnap_t != -1.0) && (linesnap_t > 0.0) && (linesnap_t < currgenpath.nlines))
						{
							currgenpath.Eval(clpt, null, linesnap_t);
							selpathnode = new OnePathNode((float)clpt.getX(), (float)clpt.getY(), 0.0F, false);
							selpathnode.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
							momotion = M_SKET_SNAPPED;
						}

						// failed to split -- get no mode.
						else
							momotion = M_NONE;
					}

					// selecting on a gridnode (either at start or end of a path)
					else
					{
						if (sketchgrid.ClosestGridPoint(clpt, moupt.getX(), moupt.getY(), 5.0 / scale))
						{
							selpathnode = new OnePathNode((float)clpt.getX(), (float)clpt.getY(), 0.0F, false);
							selpathnode.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
							momotion = M_SKET_SNAPPED;
						}
						else
							momotion = M_NONE;
					}
				}
				if (!bmoulinactive)
					SetMouseLine(moupt, moupt);
			}

			// M_SKET_END
			else if (e.isShiftDown())
			{
				if (!bmoulinactive)
					ClearSelection(true);
				else
					EndCurve(null);
			}

			repaint();
			return;
		}

		// selecting a path
		if (e.isMetaDown() && !bmoulinactive)
		{
			momotion = (e.isShiftDown() ? M_SEL_AREA : (e.isControlDown() ? M_SEL_PATH_ADD : M_SEL_PATH));
			selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2);
			repaint(); // to activate the hit command.
		}
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
			float rescalex = 1.0F + ((float)Math.abs(x - prevx) / csize.width) * 2.0F;
			if (x < prevx)
				rescalex = 1.0F / rescalex;
			float rescaley = 1.0F + ((float)Math.abs(y - prevy) / csize.height) * 2.0F;
			if (y < prevy)
				rescaley = 1.0F / rescaley;

			// uniform scale when not doing background.
			//if (!sketchdisplay.tbmovebackg.isSelected())
				rescaley = rescalex;

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
				backgroundimg.currparttrans.preConcatenate(mdtrans);
			else
				currtrans.concatenate(mdtrans);

//			mdtrans.setToIdentity();
			RedoBackgroundView();
			DeleteSel();
		}
	}
}



