////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JPanel; 
import java.awt.Graphics; 
import java.awt.Graphics2D; 
import java.awt.geom.Line2D; 
import java.awt.geom.Point2D; 
import java.awt.geom.Rectangle2D; 
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

import java.awt.Color; 

import java.awt.event.MouseListener; 
import java.awt.event.MouseMotionListener; 
import java.awt.event.MouseEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ActionEvent; 

import java.awt.geom.AffineTransform; 


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
	OneSketch skblank = new OneSketch(); 
	OneSketch tsketch = skblank; 

	Vector dtsketches = null; 
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



	OnePathNode currpathnode = null; 

	// the array of array of paths which are going to define a boundary 
	Vector vactivepaths = new Vector(); 
	OnePathNode vapbegin = null; // endpoints pf active paths list.  
	OnePathNode vapend = null; 
	boolean bLastAddVActivePathBack = true; // used by the BackSel button to know which side to rollback the active paths.  


	OneSSymbol currssymbol = null; 

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
	final static int M_SKET_END = 12; 

	final static int M_SEL_PATH = 20; 
	final static int M_SEL_PATH_ADD = 21; 
	final static int M_SEL_PATH_NODE = 22; 
	final static int M_SEL_SYMBOL = 23; 

	int momotion = M_NONE; 

	// the bitmapped background 
    ImageWarp backgroundimg = new ImageWarp(csize, this); 

	Image mainImg = null; 
	Graphics2D mainGraphics = null; 
	boolean bmainImgValid = false; 
	int bkifrm = 0; 

	boolean bNextRenderSlow = false; 

	boolean bDisplaySAreas = false; 
	boolean bSAreasValid = false; 

	AffineTransform orgtrans = new AffineTransform(); 
	AffineTransform mdtrans = new AffineTransform(); 
	AffineTransform currtrans = new AffineTransform(); 
	

	/////////////////////////////////////////////
	SketchGraphics(SketchDisplay lsketchdisplay, boolean bSymbolsType) 
	{
		super(false); // not doublebuffered  

		setBackground(TN.wfmBackground); 
		setForeground(TN.wfmLeg); 

		addMouseListener(this); 
		addMouseMotionListener(this); 

		sketchdisplay = lsketchdisplay; 

		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); 

		if (!bSymbolsType) 
			dtsketches = new Vector(); 
	}

	


	AffineTransform id = new AffineTransform(); 
	OnePath pathaddlastsel = null; 
	/////////////////////////////////////////////
    public void paintComponent(Graphics g) 
	{
		boolean bHideMarkers = sketchdisplay.miHideMarkers.isSelected(); 
		boolean bDynBackDraw = ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT)); 

		// test if resize has happened
		if ((mainImg == null) || (getSize().height != csize.height) || (getSize().width != csize.width))
		{
			csize.width = getSize().width; 
			csize.height = getSize().height; 
			mainImg = createImage(csize.width, csize.height); 
			mainGraphics = (Graphics2D)mainImg.getGraphics(); 
			bmainImgValid = false; 
		}

		// do the background
		if ((!backgroundimg.bBackImageGood || !bmainImgValid) && !bDynBackDraw)  
		{
			// test if new image is specified.  
			if (tsketch.fibackgimg != tsketch.fbackgimg) 
			{
				if (tsketch.fbackgimg != null) 
			    {
				    System.out.println("Getting back-image " + tsketch.fbackgimg.toString()); 
					tsketch.ibackgimg = getToolkit().createImage(tsketch.fbackgimg.toString()); 
				}
				else 
					tsketch.ibackgimg = null; 
				tsketch.fibackgimg = tsketch.fbackgimg;  

				if (backgroundimg.backimageS != tsketch.ibackgimg) 
					backgroundimg.SetImage(tsketch.ibackgimg); 
			}

			// do the background.  
			mainGraphics.setTransform(id); 
			backgroundimg.DoBackground(mainGraphics, currtrans, !sketchdisplay.mibackhide.isSelected()); 
			bmainImgValid = false; 
		}
			

		// set the transform
		mainGraphics.setTransform(currtrans); 

		//
		// do all the selections of things
		//


		// do the selection of paths
		if (momotion == M_SEL_PATH) 
		{
			int iselpath = tsketch.SelPath(mainGraphics, selrect, currgenpath); 
			ClearSelection(); 
			if (iselpath != -1) 
			{
				currgenpath = (OnePath)(tsketch.vpaths.elementAt(iselpath)); 
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
			currpathnode = tsketch.SelNode(mainGraphics, selrect); 

			// snap to start of current path if poss.  
			if ((currpathnode == null) && (currgenpath != null) && (currgenpath.nlines > 1)) 
			{
				if (mainGraphics.hit(selrect, currgenpath.pnstart.Getpnell(), false))
					currpathnode = currgenpath.pnstart; 
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

		// do the selection of areas.  
		if (momotion == M_SEL_SYMBOL) 
		{
			int iselsymb = tsketch.SelSymbol(mainGraphics, selrect, currssymbol); 
			ClearSelection(); 
			if (iselsymb != -1) 
			{
				currssymbol = (OneSSymbol)(tsketch.vssymbols.elementAt(iselsymb)); 
				sketchdisplay.ssobsSymbol.ObserveSelection(iselsymb, tsketch.vssymbols.size()); 
			}

			momotion = M_NONE; 
		}


		//
		// do the drawing of things into the background.  
		//

		if (!bmainImgValid && !bDynBackDraw) 
		{
			bmainImgValid = true; 
			System.out.println("backimgdraw " + bkifrm++); 
			mainGraphics.setFont(TN.fontlab); 

			if (dtsketches != null) 
			{
				// loop through the display tunnels
				for (int t = 0; t < dtsketches.size(); t++) 
				{
					OneSketch dtsketch = (OneSketch)(dtsketches.elementAt(t)); 
					dtsketch.paintW(mainGraphics, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, sketchdisplay.vgsymbols, bNextRenderSlow); 
				}
			}

			else 
			{
				if (tsketch != null) 
					tsketch.paintW(mainGraphics, false, bHideMarkers, sketchdisplay.vgsymbols, bNextRenderSlow); 
			}
			bNextRenderSlow = false; 
		}

		// paint the background in.  
		Graphics2D g2D = (Graphics2D)g; 
		if (bDynBackDraw) 
		{
			g.setColor(TN.skeBackground);
			g.fillRect(0, 0, csize.width, csize.height); 
			g2D.drawImage(mainImg, mdtrans, null);
		}
		else 
			g2D.drawImage(mainImg, 0, 0, null);

		// draw the active paths over it.  
		g2D.transform(currtrans); 
		g2D.setFont(TN.fontlab); 

		for (int i = 0; i < vactivepaths.size(); i++) 
		{
			Vector vp = (Vector)(vactivepaths.elementAt(i)); 
			for (int j = 0; j < vp.size(); j++) 
				((OnePath)(vp.elementAt(j))).paintW(g2D, false, true, false); 
		}

		// the current node 
		if ((momotion == M_SKET_SNAP) && (currpathnode != null)) 
		{
			g2D.setColor(SketchLineStyle.linestylecolactive); 
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]); 
			g2D.draw(currpathnode.Getpnell()); 

			if (!bmoulinactive) 
				g2D.draw(moupath); // moulin
		}

		// draw the active symbol
		if (currssymbol != null) 
			currssymbol.paintW(g2D, sketchdisplay.vgsymbols, !bHideMarkers, true, false);  


		// draw the areas in hatched 
		if (bDisplaySAreas)  
		{
			boolean bSymbolEdit = (sketchdisplay.usesymbolspanel != null); 
			for (int i = 0; i < tsketch.vsareas.size(); i++) 
			{
				OneSArea osa = (OneSArea)tsketch.vsareas.elementAt(i); 
				if (osa.bPathClockwise == bSymbolEdit) 
					osa.paintHatchW(g2D, i, tsketch.vsareas.size()); 
			}
		}

		// draw the selected/active paths.  
		g2D.setColor(TN.wfmnameActive); 
		if (currgenpath != null) 
			currgenpath.paintW(g2D, false, true, false); 

		// draw the rubber band.  
		if (bmoulinactive) 
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]); 
			g2D.draw(moupath);  // moulin
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
		boolean bHideMarkers = sketchdisplay.miHideMarkers.isSelected(); 
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

		if (dtsketches != null) 
		{
				// loop through the display tunnels
			for (int t = 0; t < dtsketches.size(); t++) 
			{
				OneSketch dtsketch = (OneSketch)(dtsketches.elementAt(t)); 
				dtsketch.paintW(g2D, !sketchdisplay.miCentreline.isSelected(), bHideMarkers, sketchdisplay.vgsymbols, true); 
			}
		}

		else 
		{
			if (tsketch != null) 
				tsketch.paintW(g2D, false, bHideMarkers, sketchdisplay.vgsymbols, true); 
		}

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
					//System.out.println("npieces:" + String.valueOf(nmoupathpieces)); 
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
//	System.out.println(backgroundimg.backimage.getRGB(e.getX(), e.getY())); 

	}



	/////////////////////////////////////////////
	static float PIb2 = (float)(Math.PI / 2); 


	

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
		if (bmoulinactive || (momotion == M_SKET_SNAP)) 
		{
			SetMPoint(e); 
			SetMouseLine(null, moupt); 
			if (momotion == M_SKET_SNAP)  
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
		repaint(); 
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
		if (path.linestyle == 0) 
			return; 

		tsketch.RemovePath(path); 
		bmainImgValid = false; 
		bSAreasValid = false; 
	}

	/////////////////////////////////////////////
	void DeleteSel() 
	{
		if (!bEditable) 
			return; 

		if (currssymbol != null) 
		{
			tsketch.vssymbols.removeElement(currssymbol); 
			ClearSelection(); 
			bmainImgValid = false; 
			bSAreasValid = false; 
		}

		else if ((currgenpath != null) && !bmoulinactive) 
		{
			DeletePath(currgenpath); 
			ClearSelection(); 
			bSAreasValid = false; 
		}

		bmoulinactive = false; 
		repaint(); 
	}


	/////////////////////////////////////////////
	void UpdateSAreas() 
	{
		tsketch.MakeAutoAreas(); 
		tsketch.PutSymbolsToAutoAreas(sketchdisplay.usesymbolspanel != null); 
		bSAreasValid = true; 
	}

	/////////////////////////////////////////////
	void FuseCurrent()  
	{
		// fuse across a node 
		if (vactivepaths.size() == 1) 
		{
			Vector vp = (Vector)(vactivepaths.elementAt(0)); 
			if (vp.size() == 2) 
			{
				OnePath op1 = (OnePath)vp.elementAt(0); 
				OnePath op2 = (OnePath)vp.elementAt(1); 

				// make share the same node and then merge.  and do it through all the shared areas.  
				ClearSelection(); 

				System.out.println("Should join across node"); 
			}
		}

		// warp along the given edge.  
		if ((currgenpath != null) && !bmoulinactive && (currgenpath.pnstart != currgenpath.pnend))  
		{
			OnePath warppath = currgenpath; 
			// Delete this active path.  

			// find all paths that link into the first node and warp them to the second.  
			for (int i = 0; i < tsketch.vpaths.size(); i++) 
			{
				OnePath op = (OnePath)tsketch.vpaths.elementAt(i); 
				if (op != warppath) 
					System.out.println("warp path by this"); 
			}
			System.out.println("Should join across node"); 
		}
	}

	/////////////////////////////////////////////
	// if the current selection is a line segment then we make it a centreline type.  
	void SetAsAxis()
	{
		if (!bmoulinactive && (currgenpath != null) && (currgenpath.nlines == 1))  
		{
			OnePath apath = tsketch.GetAxisPath(); 
			if (apath != null) 
				apath.linestyle = SketchLineStyle.SLS_DETAIL; 
			currgenpath.linestyle = SketchLineStyle.SLS_CENTRELINE; 
			System.out.println("Axis Set"); 
			Deselect(true); 
			bmainImgValid = false; 
			repaint(); 
		}
	}



	/////////////////////////////////////////////
	void SpecSymbol(int selitem)
	{
		if ((currgenpath != null) && !bmoulinactive && (currgenpath.pnstart.pathcount == 1) && (currgenpath.pnend.pathcount == 1)) 
		{
			// make the axis out of the currline.  
			float[] pco = currgenpath.ToCoords(); 
			int nlines = currgenpath.nlines; 
			DeleteSel(); 

			// now build the symbol.  
			currssymbol = new OneSSymbol(pco, nlines); 
			currssymbol.SpecSymbol(sketchdisplay.vgsymbols.downtunnels[selitem].name, sketchdisplay.vgsymbols.downtunnels[selitem].tsketch); 
			tsketch.vssymbols.addElement(currssymbol); 
			sketchdisplay.ssobsSymbol.ObserveSelection(tsketch.vssymbols.size() - 1, tsketch.vssymbols.size()); 
			bSAreasValid = false; // really only want to flag that this path new symbol needs reallocating. 
		}

		// set the symbol 
		else if (currssymbol != null) 
		{
			currssymbol.SpecSymbol(sketchdisplay.vgsymbols.downtunnels[selitem].name, sketchdisplay.vgsymbols.downtunnels[selitem].tsketch); 
			currssymbol.changeincvzp--; 
			bmainImgValid = false; 
		}

		repaint(); 
	}


	/////////////////////////////////////////////
	void GoSetParametersCurrPath(int maskcpp) 
	{
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
	void ClearSelection() 
	{
		sketchdisplay.ssobsPath.ObserveSelection(-1, tsketch.vpaths.size()); 
		currgenpath = null; 
		vactivepaths.clear(); 

		currssymbol = null; 
		sketchdisplay.ssobsSymbol.ObserveSelection(-1, tsketch.vssymbols.size()); 

		sketchdisplay.sketchlinestyle.pthlabel.setText(""); 
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
			vactivepaths.addElement(new Vector()); 
			((Vector)vactivepaths.lastElement()).addElement(path); 
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
		
		// loop 
		if (bJoinFront && bJoinBack) 
		{
			((Vector)vactivepaths.lastElement()).addElement(path); 
			vapbegin = null; 
			return true; 
		}

		if (bJoinBack) 
		{
			((Vector)vactivepaths.lastElement()).addElement(path); 
			vapend = (vapend == path.pnstart ? path.pnend : path.pnstart); 
			bLastAddVActivePathBack = true; 
			return true; 
		}

		if (bJoinFront) 
		{
			((Vector)vactivepaths.lastElement()).insertElementAt(path, 0); 
			vapbegin = (vapbegin == path.pnstart ? path.pnend : path.pnstart); 
			bLastAddVActivePathBack = false; 
			return true; 
		}

		return false; 
	}


	/////////////////////////////////////////////
	boolean SplitCurrpathNode(Point2D.Float pt)
	{
		float scale = (float)Math.min(currtrans.getScaleX(), currtrans.getScaleY()); 
		System.out.println(5 / scale); 
		OnePath currgenend = currgenpath.SplitNode((float)pt.getX(), (float)pt.getY(), 5.0F / scale); 
		if (currgenend == null) 
			return false; 

		tsketch.AddPath(currgenend); 

		// adjust the counters on the nodes 
		currgenend.pnstart.pathcount++; 
		currgenend.pnend.pathcount--; 

		sketchdisplay.ssobsPath.ObserveSelection(-1, tsketch.vpaths.size()); 


		bmainImgValid = false; 
		bSAreasValid = false; 
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

		bmainImgValid = false; 
		backgroundimg.bBackImageDoneGood = false; 
		repaint(); 
	}

	/////////////////////////////////////////////
	public void Max(boolean bRescale) 
	{
		// (do for the centreline too?).  

		Rectangle2D boundrect = tsketch.getBounds(currtrans); 
		if (boundrect != null) 
		{
			// set the pre transformation  
			mdtrans.setToTranslation(csize.width / 2, csize.height / 2); 

			// scale change
			if (bRescale) 
			{
				if ((csize.width != 0) && (csize.height != 0))
				{
					double scchange = Math.max(boundrect.getWidth() / (csize.width * 0.9F), boundrect.getHeight() / (csize.height * 0.9F)); 
					if (scchange != 0.0F)
						mdtrans.scale(1.0F / scchange, 1.0F / scchange); 
				}
			}

			mdtrans.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2)); 

			orgtrans.setTransform(currtrans); 
			currtrans.setTransform(mdtrans); 
			currtrans.concatenate(orgtrans); 

			bmainImgValid = false; 
			backgroundimg.bBackImageDoneGood = false; 
			repaint(); 
		}
	}


	/////////////////////////////////////////////
	// dragging out a curve
	/////////////////////////////////////////////
	void StartCurve(OnePathNode pnstart)
	{
		currgenpath = new OnePath(pnstart); 
		currgenpath.SetParametersFromBoxes(sketchdisplay); 
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
		if (moulinmleng != 0)  
		{	currgenpath.IntermedLines(moupath, nmoupathpieces); 
			if (pnend == null)  
				currgenpath.LineTo((float)moupt.getX(), (float)moupt.getY()); 
		}

		if (currgenpath.EndPath(pnend)) 
		{
			tsketch.AddPath(currgenpath); 
			sketchdisplay.ssobsPath.ObserveSelection(tsketch.vpaths.size() - 1, tsketch.vpaths.size()); 
			bmainImgValid = false; 
			bSAreasValid = false; 
		}
		else 
			currgenpath = null; 

		bmoulinactive = false; 
		momotion = M_NONE; 
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)  
	{
		//System.out.println(e.getModifiers()); 
		//System.out.println("B1 " + e.BUTTON1_MASK + " B2 " + e.BUTTON2_MASK + " B3 " + e.BUTTON3_MASK + " ALT " + e.ALT_MASK + " META " + e.META_MASK + " MetDown " + e.isMetaDown()); 


		// are we in the whole picture dragging mode?  
		if (sketchdisplay.tbpicmove.isSelected() || ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)) 
		{
			// if a point is already being dragged, then this second mouse press will delete it.  
			if ((momotion == M_DYN_DRAG) || (momotion == M_DYN_SCALE) || (momotion == M_DYN_ROT)) 
			{
				momotion = M_NONE; 
				if (sketchdisplay.tbmovebackg.isSelected()) 
					backgroundimg.currparttrans.setTransform(backgroundimg.orgparttrans); 
				else
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
				momotion = (e.isShiftDown() ? M_DYN_DRAG : (e.isControlDown() ? M_DYN_SCALE : M_DYN_ROT)); 
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
					StartCurve(new OnePathNode((float)moupt.getX(), (float)moupt.getY(), null)); 
				}
				else 
					LineToCurve(); 
			}
			
			// M_SKET_END
			else if (e.isShiftDown())
			{
				if (!bmoulinactive) 
					ClearSelection(); 
				else 
					EndCurve(null); 
			}

			// M_SKET_SNAP
			else 
			{
				momotion = M_SKET_SNAP; 
				selrect.setRect(e.getX() - SELECTWINDOWPIX, e.getY() - SELECTWINDOWPIX, SELECTWINDOWPIX * 2, SELECTWINDOWPIX * 2); 
				if (!bmoulinactive) 
				{
					// the node splitting one.  
					if (currgenpath != null)
						SplitCurrpathNode(moupt);  
					ClearSelection();  
					SetMouseLine(moupt, moupt); 
				}
			}

			repaint();
		}

		// selecting a path 
		if (e.isMetaDown() && !bmoulinactive)  
		{
			momotion = (e.isShiftDown() ? M_SEL_SYMBOL : (e.isControlDown() ? M_SEL_PATH_ADD : M_SEL_PATH)); 
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
			mouseMoved(e);  
			return; 

		case M_NONE: 
		default: 
			return; 
		}

		// the dynamic drag type things.  
		if (sketchdisplay.tbmovebackg.isSelected())
		{
			try
			{
				AffineTransform currinv = currtrans.createInverse(); 
				backgroundimg.currparttrans.setTransform(currinv); 
				backgroundimg.currparttrans.concatenate(mdtrans); 
				backgroundimg.currparttrans.concatenate(currtrans); 
				backgroundimg.currparttrans.concatenate(backgroundimg.orgparttrans); 
			}
			catch (NoninvertibleTransformException ex) 
			{;} 

		}
		else 
		{
			currtrans.setTransform(mdtrans); 
			currtrans.concatenate(orgtrans); 
		}

		bmainImgValid = false; 
		backgroundimg.bBackImageDoneGood = false; 
		repaint();  
	}


	/////////////////////////////////////////////
    public void mouseReleased(MouseEvent e)
	{
		mouseDragged(e); 

		// in this mode things happen on release.  
		if (momotion == M_SKET_SNAP) 
		{
			// start of path 
			if (!bmoulinactive) 
			{
				if (currpathnode != null) 
				{
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


		momotion = M_NONE; 
		repaint(); 
	}

	/////////////////////////////////////////////
	void MoveGround()
	{
		if ((currgenpath != null) && !bmoulinactive && (currgenpath.pnstart.pathcount == 1) && (currgenpath.pnend.pathcount == 1))  
		{
			float[] pco = currgenpath.ToCoords();  
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
			if (sketchdisplay.tbmovebackg.isSelected())
				backgroundimg.currparttrans.preConcatenate(mdtrans); 
			else
				currtrans.concatenate(mdtrans); 

//			mdtrans.setToIdentity(); 

			backgroundimg.bBackImageDoneGood = false; 
			DeleteSel(); 
		}
	}
}



