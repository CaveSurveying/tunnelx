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



import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.DocFlavor;
import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import javax.print.DocPrintJob;
import javax.print.Doc;
import javax.print.SimpleDoc;
import javax.print.PrintException;
import javax.print.StreamPrintServiceFactory;
import javax.print.StreamPrintService;
import java.io.FileOutputStream;
import java.io.IOException;

//
//
// SketchPrint
//
//
// this should be a full-blown printpreview.
class SketchPrint implements Printable
{
	// objects brought from SketchGraphics
	OneSketch tsketch;
	Dimension csize; // used for print view
	AffineTransform currtrans;

	boolean bHideCentreline;
	boolean bHideMarkers;
	boolean bHideStationNames;
	OneTunnel vgsymbols;

	AffineTransform mdtrans = new AffineTransform();

	/////////////////////////////////////////////
	// printing constants
	double prtxlo;
	double prtxhi;
	double prtylo;
	double prtyhi;

	double prtpagewidth;
	double prtpageheight;
	int nptrpagesx;
	int nptrpagesy;
	boolean bprttoscale;
	boolean bprtfirsttime; // used because we don't see page format in the printthis function

 	double prtimgscale = TN.prtscale / 72.0 * 0.254;
	double prtimageablebordermm = 5.0; // in mm
	double prtimageablewidth;
	double prtimageableheight;
	double prtimageablex;
	double prtimageabley;
	Line2D prtimageablecutrectangle[] = new Line2D[4];

	/////////////////////////////////////////////
	void PrintSetup(PageFormat pf)
	{
		Rectangle2D boundrect = tsketch.getBounds(true);
		prtxlo = boundrect.getX() - boundrect.getWidth() * 0.05;
		prtxhi = boundrect.getX() + boundrect.getWidth() * 1.05;
		prtylo = boundrect.getY() - boundrect.getHeight() * 0.05;
		prtyhi = boundrect.getY() + boundrect.getHeight() * 1.05;
System.out.println("prtxlo " + prtxlo + " prtxhi " + prtxhi + "\nprtylo " + prtylo + " prtyhi " + prtyhi);

		double prtimageableborderpt = prtimageablebordermm / 25.4 * 72.0;
		prtimageablewidth = pf.getImageableWidth() - prtimageableborderpt * 2;
		prtimageableheight = pf.getImageableHeight() - prtimageableborderpt * 2;
		prtimageablex = pf.getImageableX() + prtimageableborderpt;
		prtimageabley = pf.getImageableY() + prtimageableborderpt;

		// do the rectangle with four lines so the dashes line up, and move out by half a linewidth.
		double lnwdisp = SketchLineStyle.linestyleprintcutout.getLineWidth() / 2;
		prtimageablecutrectangle[0] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley - lnwdisp, 	prtimageablex - lnwdisp, prtimageabley + prtimageableheight + lnwdisp);
		prtimageablecutrectangle[1] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley - lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley - lnwdisp);
		prtimageablecutrectangle[2] = new Line2D.Double(prtimageablex + prtimageablewidth + lnwdisp, prtimageabley - lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley + prtimageableheight + lnwdisp);
		prtimageablecutrectangle[3] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley + prtimageableheight + lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley + prtimageableheight + lnwdisp);

		TN.emitMessage("Page dimensions in points inch-width:" + pf.getImageableWidth()/72 + "  inch-height:" + pf.getImageableHeight()/72);
		prtpagewidth = prtimageablewidth * prtimgscale;
		prtpageheight = prtimageableheight * prtimgscale;
System.out.println("prtpagewidth " + prtpagewidth + " prtpageheight " + prtpageheight);

		nptrpagesx = (int)((prtxhi - prtxlo) / prtpagewidth + 1.0);
		nptrpagesy = (int)((prtyhi - prtylo) / prtpageheight + 1.0);
System.out.println("npages w " + nptrpagesx + " h " + nptrpagesy);
		bprtfirsttime = false;
	}


	/////////////////////////////////////////////
	void PrintThisNon()
	{
		PrinterJob printJob = PrinterJob.getPrinterJob();
		if (printJob.printDialog())
		{
			PageFormat pf = new PageFormat();
			pf = printJob.defaultPage();
			pf = printJob.pageDialog(pf);
			printJob.setPrintable(this, pf);
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
boolean bUseDialog = true;
	void PrintThis(boolean lbprttoscale, boolean lbHideCentreline, boolean lbHideMarkers, boolean lbHideStationNames, OneTunnel lvgsymbols, OneSketch ltsketch, Dimension lcsize, AffineTransform lcurrtrans)
	{
		tsketch = ltsketch;
		csize = lcsize;
		currtrans = lcurrtrans;
		bHideCentreline = lbHideCentreline;
		bHideMarkers = lbHideMarkers;
		bHideStationNames = lbHideStationNames;
		vgsymbols = lvgsymbols;
bHideMarkers = true;


		bprttoscale = lbprttoscale;
		bprtfirsttime = true; // because I can't otherwise get the dimesions of the paper.

		if (bUseDialog)
		{
			PrintThisNon();
			return;
		}

		/* Use the pre-defined flavor for a Printable from an InputStream */
		DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;

 		/* Specify the type of the output stream */
		String psMimeType = DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType();
System.out.println(psMimeType);

		/* Locate factory which can export a GIF image stream as Postscript */
		StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor, psMimeType);
		if (factories.length == 0)
		{
			System.err.println("No suitable factories");
			System.exit(0);
		}

		try
		{
			/* Create a file for the exported postscript */
			FileOutputStream fos = new FileOutputStream("c:/gout.ps");

			/* Create a Stream printer for Postscript */
			StreamPrintService sps = factories[0].getPrintService(fos);

			/* Create and call a Print Job */
			DocPrintJob pj = sps.createPrintJob();
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(MediaSizeName.ISO_A3);

			Doc doc = new SimpleDoc(this, flavor, null);

			pj.print(doc, aset);
			fos.close();
		}
		catch (IOException ie) { System.err.println(ie); }
		catch (PrintException e) { System.err.println(e); }
	}




	/////////////////////////////////////////////
	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException
	{
TN.emitMessage("Page dimensions in points inch-width:" + pf.getImageableWidth()/72 + "  inch-height:" + pf.getImageableHeight()/72);
		Graphics2D g2D = (Graphics2D)g;

		if (bprttoscale)
		{
			if (bprtfirsttime)
				PrintSetup(pf);
			TN.emitMessage("Page " + pi);
			if (pi >= nptrpagesx * nptrpagesy)
				return Printable.NO_SUCH_PAGE;
			int ipy = pi / nptrpagesx;
			int ipx = pi - ipy * nptrpagesx;
			double pvx = (prtxlo + prtxhi - nptrpagesx * prtpagewidth) / 2 + ipx * prtpagewidth;
			double pvy = (prtylo + prtyhi - nptrpagesy * prtpageheight) / 2 + ipy * prtpageheight;

			// draw the cutout rectangle in page space
			g2D.setStroke(SketchLineStyle.linestyleprintcutout);
			g2D.setColor(SketchLineStyle.linestylegreyed);
			for (int i = 0; i < prtimageablecutrectangle.length; i++)
				g2D.draw(prtimageablecutrectangle[i]);

			mdtrans.setToTranslation(prtimageablex, prtimageabley);
			mdtrans.scale(1.0F / prtimgscale, 1.0F / prtimgscale);
			mdtrans.translate(-pvx, -pvy);
System.out.println("pvx " + pvx + "pvy " + pvy);

			g2D.transform(mdtrans);
		}

		// fit to screen
		else
		{
			if (pi >= 1)
				return Printable.NO_SUCH_PAGE;

			//TN.emitMessage("Page dimensions in points inch-width:" + pf.getImageableWidth()/72 + "  inch-height:" + pf.getImageableHeight()/72);
			mdtrans.setToTranslation((pf.getImageableX() + pf.getImageableWidth() / 2), (pf.getImageableY() + pf.getImageableHeight() / 2));

			// scale change relative to the size it's on the screen, so that what's on the screen is visible
			double scchange = Math.max(csize.width / (pf.getImageableWidth() * 1.0F), csize.height / (pf.getImageableHeight() * 1.0F));
			if (scchange != 0.0F)
				mdtrans.scale(1.0F / scchange, 1.0F / scchange);
			mdtrans.translate(-csize.width / 2, -csize.height / 2);

			g2D.transform(mdtrans);
			// translation is relative to the screen translation; but if you have a better idea you can hard code it.
			g2D.transform(currtrans);
		}

		tsketch.paintWquality(g2D, bHideCentreline, bHideMarkers, bHideStationNames, vgsymbols);

		return Printable.PAGE_EXISTS;
	}
};

