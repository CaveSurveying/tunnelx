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

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
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
import java.util.Vector;
import java.util.Random;

import java.awt.Color;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.geom.AffineTransform;

import javax.swing.JCheckBoxMenuItem;

//
//
// SketchGraphics
//
//
class SketchGraphics extends JPanel implements MouseListener, MouseMotionListener, Printable
{
	SketchDisplay sketchdisplay;

	static int SELECTWINDOWPIX = 5;
	static int MOVERELEASEPIX = 20;

	// the sketch.
	OneSketch skblank = new OneSketch(null, null);
	OneSketch tsketch = skblank;
	OneTunnel activetunnel = null; // the tunnel to which the above sketch belongs.

	boolean bEditable = false;

	OnePath currgenpath = null;

	// the currently active mouse path information.
	Line2D.Float moulin = new Line2D.Float();
	GeneralPath moupath = new GeneralPath();
	int nmoupathpieces = 1;
	boolean bmoulinactive = false;
	boolean bSketchMode = false;
	float moulinmleng = 0;

	Point2D.Float scrpt = new Point2D.Float();
	Point2D.Float moupt = new Point2D.Float();
	Rectangle selrect = new Rectangle();


	OnePathNode selpathnode = null;
	OnePathNode currpathnode = null;

	// the array of array of paths which are going to define a boundary
	Vector vactivepaths = new Vector();
	OnePathNode vapbegin = null; // endpoints pf active paths list.
	OnePathNode vapend = null;
	boolean bLastAddVActivePathBack = true; // used by the BackSel button to know which side to rollback the active paths.



	Dimension csize = new Dimension(0, 0);

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

	int momotion = M_NONE;

	// the bitmapped background
	ImageWarp backgroundimg = new ImageWarp(csize, this);

	Image mainImg = null;
	Graphics2D mainGraphics = null;
	boolean bmainImgValid = false;
	int bkifrm = 0;

	boolean bNextRenderSlow = false;

	// switches that control overlaying
	boolean[] bDisplayOverlay = new boolean[4]; ;


	boolean bSymbolLayoutUpdated = false;

	AffineTransform orgtrans = new AffineTransform();
	AffineTransform mdtrans = new AffineTransform();
	AffineTransform currtrans = new AffineTransform();
	double[] flatmat = new double[6];

	int iMaxAction = 0;

	// these get set whenever we update the transformation.
	Point2D.Float gridscrmid = new Point2D.Float(0, 0);
	Point2D.Float gridscrcorner = new Point2D.Float(1, 1);

	Point2D.Float scrmid = new Point2D.Float(0, 0);
	Point2D.Float scrcorner = new Point2D.Float(1, 1);
	float gridscrrad = 1;

	// used in correspondence problems
	Vector clpaths = new Vector();
	Vector corrpaths = new Vector();

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

		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}


	AffineTransform id = new AffineTransform();
	OnePath pathaddlastsel = null;
	/////////////////////////////////////////////
	public void paintComponent(Graphics g)
	{
		boolean bHideMarkers = !sketchdisplay.miShowNodes.isSelected();
		boolean bDynBackDraw = ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT));

		// test if resize has happened because we are rebuffering things
		if ((mainImg == null) || (getSize().height != csize.height) || (getSize().width != csize.width))
		{
			csize.width = getSize().width;
			csize.height = getSize().height;
			mainImg = createImage(csize.width, csize.height);
			mainGraphics = (Graphics2D)mainImg.getGraphics();
			backgroundimg.bBackImageDoneGood = false;
			bmainImgValid = false;
			UpdateGridCoords();
		}

		// the Max command
		if (iMaxAction != 0)
		{
			if ((iMaxAction == 1) || (iMaxAction == 2))
			{
				Rectangle2D boundrect = tsketch.getBounds(currtrans);
				if (boundrect != null)
				{
					// set the pre transformation
					mdtrans.setToTranslation(csize.width / 2, csize.height / 2);

					// scale change
					if (iMaxAction == 2)
					{
						if ((csize.width != 0) && (csize.height != 0))
						{
							double scchange = Math.max(boundrect.getWidth() / (csize.width * 0.9F), boundrect.getHeight() / (csize.height * 0.9F));
							if (scchange != 0.0F)
								mdtrans.scale(1.0F / scchange, 1.0F / scchange);
						}
					}

					mdtrans.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2));
				}
				else
					mdtrans.setToIdentity();

				orgtrans.setTransform(currtrans);
				currtrans.setTransform(mdtrans);
				currtrans.concatenate(orgtrans);
			}

			else // iMaxAction == 3, Set Upright
			{
				currtrans.getMatrix(flatmat);
				double sca = Math.sqrt(flatmat[0] * flatmat[0] + flatmat[2] * flatmat[2]);
				double sca1 = Math.sqrt(flatmat[1] * flatmat[1] + flatmat[3] * flatmat[3]);
				TN.emitMessage("Ortho mat " + sca + " " + sca1);
				currtrans.setTransform(sca, 0, 0, sca, flatmat[4], flatmat[5]);
			}


			backgroundimg.bBackImageDoneGood = false;
			bmainImgValid = false;
			UpdateGridCoords();

			iMaxAction = 0;
		}



		//
		// do all the selections of things
		//
		// transform required for selection to work.
		mainGraphics.setTransform(currtrans);


		// do the selection of paths
		if (momotion == M_SEL_PATH)
		{
			int iselpath = tsketch.SelPath(mainGraphics, selrect, currgenpath);
			ClearSelection();
			if (iselpath != -1)
			{
				currgenpath = (OnePath)(tsketch.vpaths.elementAt(iselpath));
				DChangeBackNode();
				currgenpath.SetParametersIntoBoxes(sketchdisplay);
				sketchdisplay.ssobsPath.ObserveSelection(iselpath, tsketch.vpaths.size());
			}
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
			int iselpath = tsketch.SelPath(mainGraphics, selrect, pathaddlastsel);

			// toggle in list.
			if (iselpath != -1)
			{
				OnePath selgenpath = (OnePath)(tsketch.vpaths.elementAt(iselpath));
				pathaddlastsel = selgenpath;
				if (IsActivePath(selgenpath))
				{
					RemoveVActivePath(selgenpath);
					sketchdisplay.ssobsPath.ObserveSelection(-1, tsketch.vpaths.size());
				}
				else
				{
					AddVActivePath(selgenpath);
					selgenpath.SetParametersIntoBoxes(sketchdisplay);
					sketchdisplay.ssobsPath.ObserveSelection(iselpath, tsketch.vpaths.size());
				}
			}
			else
				pathaddlastsel = null;

			momotion = M_NONE;
		}


		// do the selection of pathnodes
		if (momotion == M_SKET_SNAP)
		{
			selpathnode = tsketch.SelNode(mainGraphics, selrect);

			// extra selection of start of currgenpath
			if ((currgenpath != null) && (currgenpath.nlines > 1))
			{
				if (mainGraphics.hit(selrect, currgenpath.pnstart.Getpnell(), false))
					selpathnode = currgenpath.pnstart;
			}

			if (selpathnode == null)
				momotion = M_NONE;
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


		// now we start rendering what is into the mainGraphics.
		// when rendering is complete, we draw it in the front.
		// paint the background in.
		Graphics2D g2D = (Graphics2D)g;
		if (bDynBackDraw)
		{
			// simply redraw the back image into the front, transformed for fast dynamic rendering.
			g.setColor(TN.skeBackground);
			g.fillRect(0, 0, csize.width, csize.height);
			g2D.drawImage(mainImg, mdtrans, null);
		}

		// do the background image thing.
		else
		{
			// the rendering of the background image is already included in the background.
			if (!bmainImgValid)
			{
				mainGraphics.setTransform(id);

				// render the background
				boolean bClearBackground = ((tsketch.fbackgimg == null) || !sketchdisplay.miShowBackground.isSelected());
				if (!bClearBackground && !backgroundimg.bBackImageGood)
				{
					backgroundimg.bBackImageGood = backgroundimg.SketchBufferWholeBackImage();
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


				// render the image on top of the background.
				bmainImgValid = true;
				TN.emitMessage("backimgdraw " + bkifrm++);

				mainGraphics.setTransform(currtrans);
				mainGraphics.setFont(TN.fontlab);
				if (sketchdisplay.miShowGrid.isSelected())
					tsketch.DrawMetreGrid(mainGraphics);

				if (tsketch != null)
					tsketch.paintW(mainGraphics, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, !sketchdisplay.miStationNames.isSelected(), sketchdisplay.vgsymbols, bNextRenderSlow);

				bNextRenderSlow = false;
			}

			g2D.drawImage(mainImg, 0, 0, null);
		}  // end drawing the unselected image.

		//
		// draw the active paths over it in the real window buffer.
		//
		g2D.transform(currtrans);
		g2D.setFont(TN.fontlab);

		for (int i = 0; i < vactivepaths.size(); i++)
		{
			Vector vp = (Vector)(vactivepaths.elementAt(i));
			for (int j = 0; j < vp.size(); j++)
			{
				OnePath op = (OnePath)vp.elementAt(j);
				op.paintW(g2D, false, true, false);

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

			if (!bmoulinactive)
				g2D.draw(moupath); // moulin
		}

//	bDisplayOverlay[2] JButton bpinkdownsketch = new JButton("V Down Sketch");
//	bDisplayOverlay[3] JButton bpinkdownsketchU = new JButton("V Down SketchU");


		// draw the areas in hatched
		if (bDisplayOverlay[0])
		{
			Vector lvsareas = tsketch.vsareas;
			if ((currgenpath != null) && (currgenpath.iconncompareaindex != -1))
				lvsareas = tsketch.sksya.GetCconnAreas(currgenpath.iconncompareaindex);
			for (int i = 0; i < lvsareas.size(); i++)
			{
				OneSArea osa = (OneSArea)lvsareas.elementAt(i);
				osa.paintHatchW(g2D, i, lvsareas.size());
			}
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

			currgenpath.paintW(g2D, false, true, false);

			// draw the startpoint node so we can determin handedness.
			if (currgenpath.pnstart != null)
			{
				g2D.setColor(SketchLineStyle.linestylecolactive);
				g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
				g2D.draw(currgenpath.pnstart.Getpnell());
			}
		}

		// draw the rubber band.
		if (bmoulinactive)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.draw(moupath);  // moulin
		}


		// paint the down sketches that we are going to import (this is a preview).
		// this messes up the g2d.transform.
		if (bDisplayOverlay[3])
		{
			paintSelectedSketches(g2D, sketchdisplay.mainbox.tunnelfilelist.activetunnel, sketchdisplay.mainbox.tunnelfilelist.activesketch);
		}
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
	void ImportSketchCentreline()
	{
		// this otglobal was set when we opened this window.
		// (we unnecessarily evaluate the cave every time).

		// calculate when we import
		sketchdisplay.mainbox.sc.CopyRecurseExportVTunnels(sketchdisplay.mainbox.otglobal, sketchdisplay.mainbox.tunnelfilelist.activetunnel, true);
		sketchdisplay.mainbox.sc.CalcStationPositions(sketchdisplay.mainbox.otglobal, null);
		tsketch.ImportCentreline(sketchdisplay.mainbox.otglobal);
		asketchavglast = null; // change of avg transform cache.

		tsketch.bsketchfilechanged = true;

		bmainImgValid = false;
	}



	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it into the selected sketch in the mainbox.
	void ImportSketch(OneSketch asketch, OneTunnel atunnel)
	{
		// all in one find the centreline paths and the corresponding paths we will export to.
		if ((asketch != null) && asketch.ExtractCentrelinePathCorrespondence(atunnel, clpaths, corrpaths, tsketch, activetunnel))
		{
			tsketch.ImportDistorted(asketch, clpaths, corrpaths, sketchdisplay.vgsymbols);
			tsketch.bsketchfilechanged = true;
			bmainImgValid = false;
		}
	}


	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it into the selected sketch in the mainbox.
	AffineTransform avgtrans = new AffineTransform();
	OneSketch asketchavglast = null; // used for lazy evaluation.

	void paintSelectedSketches(Graphics2D g2D, OneTunnel atunnel, OneSketch asketch)
	{
		// we could make this a lazy evaluation.
		if (asketch != null)
		{
			// find new transform if it's a change.
			if (asketch != asketchavglast)
			{
				if (asketch.ExtractCentrelinePathCorrespondence(atunnel, clpaths, corrpaths, tsketch, activetunnel))
				{
					PtrelLn.CalcAvgTransform(avgtrans, clpaths, corrpaths);
					asketchavglast = asketch;
				}
			}

			// now work from known transform
			if (asketch == asketchavglast)
			{
				g2D.transform(avgtrans);

				// draw all the paths inactive.
				for (int i = 0; i < asketch.vpaths.size(); i++)
				{
					OnePath path = (OnePath)(asketch.vpaths.elementAt(i));
					if (path.linestyle != SketchLineStyle.SLS_CENTRELINE) // of have it unhidden?
						path.paintW(g2D, true, true, false);
				}
			}
		}
	}


	/////////////////////////////////////////////
	void PrintThis()
	{
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);

		//PageFormat pf = printJob.pageDialog(printJob.defaultPage());

        if (printJob.printDialog())
		{
            try
			{
                printJob.print();
            }
			catch (Exception e)
			{
                e.printStackTrace();
            }
        }
	}


	/////////////////////////////////////////////
	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException
	{
		boolean bHideMarkers = sketchdisplay.miShowNodes.isSelected();
		bHideMarkers = true;

		if (pi >= 1)
			return Printable.NO_SUCH_PAGE;

		//g.setColor(TN.skeBackground);
		//g.fillRect(0, 0, (int)pf.getImageableWidth(), (int)pf.getImageableHeight());

		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(TN.fontlab);

		// scale to fit the paper.
		mdtrans.setToTranslation((pf.getImageableX() + pf.getImageableWidth() / 2), (pf.getImageableY() + pf.getImageableHeight() / 2));
		// scale change
		if ((csize.width != 0) && (csize.height != 0))
		{
			double scchange = Math.max(csize.width / (pf.getImageableWidth() * 0.9F), csize.height / (pf.getImageableHeight() * 0.9F));
			if (scchange != 0.0F)
				mdtrans.scale(1.0F / scchange, 1.0F / scchange);
		}
		mdtrans.translate(-csize.width / 2, -csize.height / 2);

		g2D.transform(mdtrans);
		g2D.transform(currtrans);

		if (tsketch != null)
			tsketch.paintW(g2D, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, !sketchdisplay.miStationNames.isSelected(), sketchdisplay.vgsymbols, true);

		return Printable.PAGE_EXISTS;
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
	int nsampsides = 7;

	boolean IsInBlack(int j)
	{
		return IsInBlack(ptlx + perpx * j, ptly + perpy * j);
	}

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
		if (sketchdisplay.miTrackLines.isSelected() && (backgroundimg.backimage != null))
		{
			if ((moulinmleng > 10) && (moulinmleng < 200))
			{
				// both endpoints should be in the black region.
				if (IsInBlack(smpt0.getX(), smpt0.getY()) && IsInBlack(smpt1.getX(), smpt1.getY()))
				{
					nmoupathpieces = Math.max(1, 1 + Math.min(8, (int)(moulinmleng / 10)));
					//TN.emitMessage("npieces:" + String.valueOf(nmoupathpieces));
					// do some precalculations
					if (nmoupathpieces != 1)
					{
						perpy = (float)(smpt1.getY() - smpt0.getY()) / moulinmleng;
						perpx = -(float)(smpt1.getX() - smpt0.getX()) / moulinmleng;
					}
				}
			}
		}



		// work out how many pieces it will split into

		moupath.reset();
		moupath.moveTo((float)moulin.getX1(), (float)moulin.getY1());

		for (int i = 1; i < nmoupathpieces; i++)
		{
			float lam = (float)i / nmoupathpieces;
			ptlx = (float)((1.0F - lam) * smpt0.getX() + lam * smpt1.getX());
			ptly = (float)((1.0F - lam) * smpt0.getY() + lam * smpt1.getY());

			// find the first black sample
			int fb = -1;
			for (int j = 0; j <= nsampsides; j++)
			{
				if (IsInBlack(j))
				{
					fb = j;
					break;
				}
				if ((j != 0) && IsInBlack(-j))
				{
					fb = -j;
					break;
				}
			}
			// skip this one, no black was found.
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

			// now set the point to the mid sample block.
			float fbm = (fblo + fbhi) / 2.0F;
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
		ClearSelection();
	}


	/////////////////////////////////////////////
	void DeletePath(OnePath path)
	{
		// don't delete a centreline type
		if (path.linestyle == SketchLineStyle.SLS_CENTRELINE)
			return;

		tsketch.RemovePath(path);
		tsketch.bsketchfilechanged = true;
		bmainImgValid = false;
	}



	/////////////////////////////////////////////
	void DeleteSel()
	{
		OnePath lcurrgenpath = (bmoulinactive ? null : currgenpath);
		ClearSelection();

		if (bEditable && (lcurrgenpath != null))
		{
			DeletePath(lcurrgenpath);
			tsketch.bsketchfilechanged = true;
		}
		repaint();
	}


	/////////////////////////////////////////////
	void UpdateSAreas()
	{
		tsketch.MakeAutoAreas();  // once it is on always this will be unnecessary.
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		tsketch.bSAreasUpdated = true;
		bSymbolLayoutUpdated = false;
		bmainImgValid = false;

		sketchdisplay.ssobsArea.ObserveSelection(-1, tsketch.vsareas.size());
	}

	/////////////////////////////////////////////
	void UpdateSymbolLayout()
	{
		// should use a random number that's consistent --
		// the seed should be in the file so we make the same diagram.
		// But for now, make it properly random to check.
		tsketch.MakeSymbolLayout();
		bSymbolLayoutUpdated = true;
		tsketch.bsketchfilechanged = true; // we've messed with the zvalues (for now).
		bmainImgValid = false;
	}


	/////////////////////////////////////////////
	void FuseCurrent(boolean bShearWarp)
	{
		// fuse across a node if it's a sequence
		if (vactivepaths.size() > 1)
			return;

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

			ClearSelection();
			OnePath opf = op1.FuseNode(pnconnect, op2);

			// delete this warped path
			tsketch.RemovePath(op1);
			tsketch.RemovePath(op2);
			int iselpath = tsketch.AddPath(opf, sketchdisplay.vgsymbols);
			currgenpath = opf;
			DChangeBackNode();
			currgenpath.SetParametersIntoBoxes(sketchdisplay);
			sketchdisplay.ssobsPath.ObserveSelection(iselpath, tsketch.vpaths.size());
		}

		// fuse along a single edge
		else
		{
			// cases for throwing out the individual edge
			if ((currgenpath == null) || bmoulinactive || (currgenpath.nlines != 1) || (currgenpath.pnstart == currgenpath.pnend) || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
				return;

			// the path to warp along
			OnePath warppath = currgenpath;
			ClearSelection();

			// delete this fused path
			tsketch.RemovePath(warppath);

			// find all paths that link into the first node and warp them to the second.
			// must be done backwards due to messing of the array
			for (int i = tsketch.vpaths.size() - 1; i >= 0; i--)
			{
				OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
				if ((op.pnstart == warppath.pnstart) || (op.pnend == warppath.pnstart))
				{
					tsketch.RemovePath(op);
					tsketch.AddPath(op.WarpPath(warppath.pnstart, warppath.pnend, bShearWarp), sketchdisplay.vgsymbols);
				}
			}
		}
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		// invalidate.
		bmainImgValid = false;
		tsketch.bsketchfilechanged = true;
	}


	/////////////////////////////////////////////
	void ReflectCurrent()
	{
		tsketch.bsketchfilechanged = true;

		// cases for throwing out the individual edge
		if ((currgenpath == null) || bmoulinactive || (currgenpath.linestyle == SketchLineStyle.SLS_CENTRELINE))
			return;

		// must maintain pointers to this the right way 
		tsketch.RemovePath(currgenpath);
		currgenpath.Spline(currgenpath.bSplined, true);
		OnePathNode pnt = currgenpath.pnstart;
		currgenpath.pnstart = currgenpath.pnend;
		currgenpath.pnend = pnt;
		tsketch.AddPath(currgenpath, sketchdisplay.vgsymbols);

		repaint();
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
			bmainImgValid = false;
		}
	}



	/////////////////////////////////////////////
	void SpecSymbol(boolean bOverwrite, String autcode)
	{
		tsketch.bsketchfilechanged = true;
		if ((currgenpath != null) && !bmoulinactive)
		{
			if (currgenpath.linestyle != SketchLineStyle.SLS_CONNECTIVE)
				System.out.println("Symbols can only go on connective types");
			else
			{
				String text;
				if (bOverwrite)
					text = autcode;
				else
					text = sketchdisplay.sketchlinestyle.pthlabel.getText() + autcode;
				sketchdisplay.sketchlinestyle.pthlabel.setText(text);
				GoSetLabelCurrPath();

				tsketch.bSAreasUpdated = false;
				bmainImgValid = false;
				repaint();
			}
		}
	}


	/////////////////////////////////////////////
	void GoSetParametersCurrPath(int maskcpp)
	{
		tsketch.bsketchfilechanged = true;
		if ((currgenpath == null) || !bEditable || (maskcpp != 0))
			return;

		// if the spline changes then the area should change too.
		boolean bPrevSplined = currgenpath.bSplined;
		if (currgenpath.SetParametersFromBoxes(sketchdisplay));
		{
			bmainImgValid = false;
			repaint();
		}
	}

	/////////////////////////////////////////////
	// does the loading of the symbols, etc
	void GoSetLabelCurrPath()
	{
System.out.println("label goset " + sketchdisplay.sketchlinestyle.pthlabel.getText());
		if ((currgenpath != null) && bEditable)
		{
			String lplabel = sketchdisplay.sketchlinestyle.pthlabel.getText().trim();
			if (!lplabel.equals(currgenpath.plabel == null ? "" : currgenpath.plabel))
			{
				if (lplabel.length() == 0)
					lplabel = null;
				currgenpath.plabel = lplabel;
				currgenpath.GenerateSymbolsFromPath(sketchdisplay.vgsymbols);
				bmainImgValid = false;
				repaint();
			}
		}
	}

	/////////////////////////////////////////////
	void ClearSelection()
	{
		sketchdisplay.ssobsPath.ObserveSelection(-1, tsketch.vpaths.size());
		GoSetLabelCurrPath();
		currgenpath = null;
		vactivepaths.clear();
		bmoulinactive = false; // newly added
		DChangeBackNode();

		//sketchdisplay.ssobsSymbol.ObserveSelection(-1, tsketch.vssymbols.size());

		OnePath.ClearSelectionIntoBoxes(sketchdisplay);

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
		tsketch.bsketchfilechanged = true;

		OnePath op = currgenpath;
		OnePathNode pnmid = selpathnode;
		ClearSelection();

		tsketch.RemovePath(op);
		OnePath currgenend = op.SplitNode(pnmid, linesnap_t);
		tsketch.AddPath(op, sketchdisplay.vgsymbols);
		tsketch.AddPath(currgenend, sketchdisplay.vgsymbols);

		sketchdisplay.ssobsPath.ObserveSelection(-1, tsketch.vpaths.size());
		assert OnePathNode.CheckAllPathCounts(tsketch.vnodes, tsketch.vpaths);

		bmainImgValid = false;
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
		UpdateGridCoords();

		bmainImgValid = false;
		backgroundimg.bBackImageDoneGood = false;
	}

	/////////////////////////////////////////////
	public void Translate(float xprop, float yprop)
	{
		// set the pre transformation
		mdtrans.setToTranslation(csize.width * xprop, csize.height * yprop);

		orgtrans.setTransform(currtrans);
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);
		UpdateGridCoords();

		bmainImgValid = false;
		backgroundimg.bBackImageDoneGood = false;
	}

	/////////////////////////////////////////////
	void UpdateGridCoords()
	{
		scrmid.setLocation(csize.width / 2, csize.height / 2);
		scrcorner.setLocation(0.0F, 0.0F);

		try
		{
			currtrans.inverseTransform(scrmid, gridscrmid);
			currtrans.inverseTransform(scrcorner, gridscrcorner);
		}
		catch (NoninvertibleTransformException ex)
		{;}

		gridscrrad = (float)gridscrmid.distance(gridscrcorner);
		tsketch.GenerateMetreGrid(gridscrmid, gridscrrad, scrmid);
	}

	/////////////////////////////////////////////
	void SetIColsDefault()
	{
		for (int i = 0; i < tsketch.vpaths.size(); i++)
			((OnePath)tsketch.vpaths.elementAt(i)).icolindex = -1;
		for (int i = 0; i < tsketch.vnodes.size(); i++)
			((OnePathNode)tsketch.vnodes.elementAt(i)).icolindex = -1; 

		bmainImgValid = false;
	}

	/////////////////////////////////////////////
	void SetIColsByZ()
	{
		// the setting of the zalts is done from a menu auto command 

		// fill in the range
		if (tsketch.zaltlo == tsketch.zalthi)
			return;
		// fill in the colours at the end-nodes
		for (int i = 0; i < tsketch.vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)tsketch.vnodes.elementAt(i); 
			float a = (opn.zalt - tsketch.zaltlo) / (tsketch.zalthi - tsketch.zaltlo); 
			opn.icolindex = Math.max(Math.min((int)(a * SketchLineStyle.linestylecolsindex.length), SketchLineStyle.linestylecolsindex.length - 1), 0);
		}

		for (int i = 0; i < tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)tsketch.vpaths.elementAt(i);
			op.icolindex = (op.pnstart.icolindex + op.pnend.icolindex) / 2; 
		}
		bmainImgValid = false;
	}

	/////////////////////////////////////////////
	void SetIColsProximity(int style)
	{
		OnePathNode ops = (currpathnode != null ? currpathnode : (currgenpath != null ? currgenpath.pnstart : null)); 
		if (ops == null) 
			return; 

		// heavyweight stuff
		ProximityDerivation pd = new ProximityDerivation(tsketch);
		pd.ShortestPathsToCentrelineNodes(ops);

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
			op.icolindex = (op.pnstart.icolindex + op.pnend.icolindex) / 2; 
		}
		bmainImgValid = false;
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////



	/////////////////////////////////////////////
	// dragging out a curve
	/////////////////////////////////////////////
	void StartCurve(OnePathNode pnstart)
	{
		currgenpath = new OnePath(pnstart);
		currgenpath.SetParametersFromBoxes(sketchdisplay);
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
		tsketch.bsketchfilechanged = true;
		if (moulinmleng != 0)
		{	currgenpath.IntermedLines(moupath, nmoupathpieces);
			if (pnend == null)
				currgenpath.LineTo((float)moupt.getX(), (float)moupt.getY());
		}

		if (currgenpath.EndPath(pnend))
		{
			tsketch.AddPath(currgenpath, sketchdisplay.vgsymbols);
			sketchdisplay.ssobsPath.ObserveSelection(tsketch.vpaths.size() - 1, tsketch.vpaths.size());
			bmainImgValid = false;
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
	public void mousePressed(MouseEvent e)
	{
		//TN.emitMessage(e.getModifiers());
		//TN.emitMessage("B1 " + e.BUTTON1_MASK + " B2 " + e.BUTTON2_MASK + " B3 " + e.BUTTON3_MASK + " ALT " + e.ALT_MASK + " META " + e.META_MASK + " MetDown " + e.isMetaDown());


		// are we in the whole picture dragging mode?  (middle mouse button).
		if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
		{
			// if a point is already being dragged, then this second mouse press will delete it.
			if ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT))
			{
				momotion = M_NONE;
				currtrans.setTransform(orgtrans);
				backgroundimg.bBackImageDoneGood = false;
				bmainImgValid = false;
				repaint();
				return;
			}

			orgtrans.setTransform(currtrans);
			backgroundimg.orgparttrans.setTransform(backgroundimg.currparttrans);
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
					ClearSelection();
					StartCurve(new OnePathNode((float)moupt.getX(), (float)moupt.getY(), 0.0F, false));
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

				if (!bmoulinactive)
				{
					// the node splitting one. only on edges if shift is down(over-ride with shift down)
					if ((currgenpath != null) && e.isShiftDown())
					{
						double scale = Math.min(currtrans.getScaleX(), currtrans.getScaleY());
						linesnap_t = currgenpath.ClosestPoint(moupt.getX(), moupt.getY(), 5.0 / scale);
						if ((currgenpath.linestyle != SketchLineStyle.SLS_CENTRELINE) && (linesnap_t != -1.0) && (linesnap_t > 0.0) && (linesnap_t < currgenpath.nlines))
						{
							Point2D clpt = new Point2D.Double();
							currgenpath.Eval(clpt, null, linesnap_t);
							selpathnode = new OnePathNode((float)clpt.getX(), (float)clpt.getY(), 0.0F, false);
							momotion = M_SKET_SNAPPED;
						}

						// failed to split -- get no mode.
						else
							momotion = M_NONE;
					}

					// join on node type
					else
						ClearSelection();
					SetMouseLine(moupt, moupt);
				}
			}

			// M_SKET_END
			else if (e.isShiftDown())
			{
				if (!bmoulinactive)
					ClearSelection();
				else
					EndCurve(null);
			}

			repaint();
			return;
		}

		// selecting a path
		if (e.isMetaDown() && !bmoulinactive)
		{
			momotion = (e.isControlDown() ? M_SEL_PATH_ADD : M_SEL_PATH);
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
		default:
			return;
		}

		// the dynamic drag type things.
		currtrans.setTransform(mdtrans);
		currtrans.concatenate(orgtrans);

		bmainImgValid = false;
		backgroundimg.bBackImageDoneGood = false;
		repaint();
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
					ClearSelection();
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
			UpdateGridCoords();

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
			UpdateGridCoords();

			backgroundimg.bBackImageDoneGood = false;
			DeleteSel();
		}
	}
}



