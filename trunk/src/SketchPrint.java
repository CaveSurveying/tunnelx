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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import javax.imageio.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import org.jibble.epsgraphics.*;

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
	JFrame frame;
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
	int prtscalecode;

	// used because we don't see page format in the printthis function
	int nprintcalls;

 	double prtimgscale;

	double prtimageablebordermm = 5.0; // in mm
	double prtimageablewidth;
	double prtimageableheight;
	double prtimageablex;
	double prtimageabley;

	Line2D prtimageablecutrectangle[] = new Line2D[4];
	boolean bdrawcutoutrectangle;
	boolean brefilloverlaps;

	boolean bsinglepageenabled;
	int singlepagenx;
	int singlepageny;


	// page format information
	double pfimageablewidth;
	double pfimageableheight;
	double pfimageableX;
	double pfimageableY;

	/////////////////////////////////////////////
	boolean PrintScaleSetup()
	{
		double pcentmargin = 0.005;
		Rectangle2D boundrect = tsketch.getBounds(true);
		prtxlo = boundrect.getX() - boundrect.getWidth() * pcentmargin;
		prtxhi = boundrect.getX() + boundrect.getWidth() * (1 + pcentmargin);
		prtylo = boundrect.getY() - boundrect.getHeight() * pcentmargin;
		prtyhi = boundrect.getY() + boundrect.getHeight() * (1 + pcentmargin);
		System.out.println("Image dimensions");
		System.out.println("prtxlo " + prtxlo + " prtxhi " + prtxhi + "\nprtylo " + prtylo + " prtyhi " + prtyhi);

		double prtimageableborderpt = prtimageablebordermm / 25.4 * 72.0;
		prtimageablewidth = pfimageablewidth - prtimageableborderpt * 2;
		prtimageableheight = pfimageableheight - prtimageableborderpt * 2;
		prtimageablex = pfimageableX + prtimageableborderpt;
		prtimageabley = pfimageableY + prtimageableborderpt;

		System.out.println(prtimageablewidth);
		System.out.println(prtimageableheight);
		System.out.println(prtimageablex);
		System.out.println(prtimageabley);

		// do the rectangle with four lines so the dashes line up, and move out by half a linewidth.
		double lnwdisp = SketchLineStyle.linestyleprintcutout.getLineWidth() / 2;
		prtimageablecutrectangle[0] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley - lnwdisp, 	prtimageablex - lnwdisp, prtimageabley + prtimageableheight + lnwdisp);
		prtimageablecutrectangle[1] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley - lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley - lnwdisp);
		prtimageablecutrectangle[2] = new Line2D.Double(prtimageablex + prtimageablewidth + lnwdisp, prtimageabley - lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley + prtimageableheight + lnwdisp);
		prtimageablecutrectangle[3] = new Line2D.Double(prtimageablex - lnwdisp, prtimageabley + prtimageableheight + lnwdisp, 	prtimageablex + prtimageablewidth + lnwdisp, prtimageabley + prtimageableheight + lnwdisp);

		TN.emitMessage("Page dimensions in points inch-width:" + pfimageablewidth / 72.0 + "  inch-height:" + pfimageableheight / 72.0);

		// crazy doing it this far down the line, but seems necessary.
		PrintScaleDialog psd = new PrintScaleDialog(frame, prtxhi - prtxlo, prtyhi - prtylo, prtimageablewidth, prtimageableheight);
		int n = psd.getScale();
		if (n == -1) // returns -1 if user cancelled operation
			return false;
		bdrawcutoutrectangle = psd.cutoutrectangle.isSelected();
		bsinglepageenabled = psd.singlepageenabled.isSelected();
		if (bsinglepageenabled)
		{
			singlepagenx = Integer.parseInt(psd.pagenx.getText());
			singlepageny = Integer.parseInt(psd.pageny.getText());
		}

		prtimgscale = n / 72.0 * 0.254;

		System.out.println("Printing to scale: " + prtimgscale);

		prtpagewidth = prtimageablewidth * prtimgscale;
		prtpageheight = prtimageableheight * prtimgscale;
		System.out.println("prtpagewidth " + prtpagewidth + " prtpageheight " + prtpageheight);

		nptrpagesx = (int)((prtxhi - prtxlo) / prtpagewidth + 1.0);
		nptrpagesy = (int)((prtyhi - prtylo) / prtpageheight + 1.0);
		System.out.println("npages w " + nptrpagesx + " h " + nptrpagesy);

		// force down to one page
		if (psd.forceonepage.isSelected())
		{
			nptrpagesx = 1;
			nptrpagesy = 1;
		}

		nprintcalls = 0;
		return true;
	}


	/////////////////////////////////////////////
	void PrintThisNon() throws Exception
	{
		PrinterJob printJob = PrinterJob.getPrinterJob();

		if(printJob.printDialog())
		{
			PageFormat pf = new PageFormat();
			pf = printJob.defaultPage();
			pf = printJob.pageDialog(pf);
			printJob.setPrintable(this, pf);
			printJob.print();
		}
	}

	/////////////////////////////////////////////
	void PrintThisEPS() throws Exception
	{
		// ghostscript view EPS only with default of A3
		pfimageablewidth = (2 * 297.0 / 25.4 - 1.0) * 72;
		pfimageableheight = (2 * 420.0 / 25.4 - 1.0) * 72;
		pfimageableX = 0.5 * 72;
		pfimageableY = 0.5 * 72;
		brefilloverlaps = true;


		if (!PrintScaleSetup())
			return;

		// Save this document to example.eps
		FileOutputStream outputStream = new FileOutputStream("example.eps");

//		EpsGraphics2D epsg = new EpsGraphics2D("Example", outputStream, (int)prtxlo, (int)prtylo, (int)prtxhi, (int)prtyhi, 1.0);
		EpsGraphics2D epsg = new EpsGraphics2D("Example", outputStream, (int)pfimageableX, (int)pfimageableY, (int)(pfimageableX + pfimageablewidth + 100), (int)(pfimageableY + pfimageableheight + 100), 1.0);

//tsketch.paintWquality(epsg, bHideCentreline, bHideMarkers, bHideStationNames, vgsymbols);
		lprint(epsg, 0);


		// Flush and close the document (don't forget to do this!)
		epsg.flush();
		epsg.close();
	}

	/////////////////////////////////////////////
	void PrintThis(int lprtscalecode, boolean lbHideCentreline, boolean lbHideMarkers, boolean lbHideStationNames, OneTunnel lvgsymbols, OneSketch ltsketch, Dimension lcsize, AffineTransform lcurrtrans, JFrame inframe)
	{
		frame = inframe;

		tsketch = ltsketch;
		csize = lcsize;
		currtrans = lcurrtrans;
		bHideCentreline = lbHideCentreline;
		bHideMarkers = lbHideMarkers;
		bHideStationNames = lbHideStationNames;
		vgsymbols = lvgsymbols;
		bHideMarkers = true;
		prtscalecode = lprtscalecode;
		brefilloverlaps = false;

		// counts the times the print function gets called
		// we know the paper size on the first call and can deal with it.
		nprintcalls = 0;

		try
		{
		if (lprtscalecode == 3)
			PrintThisEPS();
		else if (lprtscalecode == 5)
			PrintThisBitmap();
		else
			PrintThisNon();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void PrintThisBitmap()
	{
		// Output as a bitmap using ImageIO class.

		// Create a bounding rectangle
		Rectangle2D boundrect = tsketch.getBounds(true);

		// set up as scaled image at 72dpi

		double pxperdecimetre = showBitmapSizeDialog(boundrect);

		if(pxperdecimetre == -1.0) return;

		int widthpx = (int) Math.ceil(boundrect.getWidth() * pxperdecimetre);
		int heightpx = (int) Math.ceil(boundrect.getHeight() * pxperdecimetre);

		System.out.println("Using size " + java.lang.Double.toString(widthpx) + "x" + java.lang.Double.toString(heightpx));

		BufferedImage bi = new BufferedImage(widthpx, heightpx, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();

		AffineTransform aff = new AffineTransform();

		if (boundrect != null)
		{
			// set the pre transformation
			aff.setToTranslation(widthpx / 2, heightpx / 2);

			double scchange = boundrect.getWidth() / (widthpx - 2);
			aff.scale(1.0F / scchange, 1.0F / scchange);

			aff.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2));
		}

		g2d.setTransform(aff);

		tsketch.paintWquality(g2d, bHideCentreline, bHideMarkers, bHideStationNames, vgsymbols, brefilloverlaps);

		g2d.dispose();

		Object[] foo = ImageIO.getWriterFormatNames();
		for(int i = 0; ; i++)
		{
			try
			{
				System.out.println("Found image format " + foo[i]);
			}
			catch (Exception e) { break; }
		}


		SvxFileDialog sfd = SvxFileDialog.showSaveDialog(TN.currentDirectory, frame, SvxFileDialog.FT_BITMAP);
		if (sfd == null)
			return;

		File file = sfd.getSelectedFile();

		String ftype = TN.getSuffix(file.getName()).substring(1).toLowerCase();

		try{
			ImageIO.write(bi, ftype, file);
		} catch (Exception e) { e.printStackTrace(); }
	}


	double showBitmapSizeDialog(Rectangle2D boundrect)
	{
		// Show a dialog to allow the user to choose the size of the output bitmap.
		// Returns a value in output pixels per decimetre.

		final JDialog sizeDialog = new JDialog(frame, "Export bitmap", true);
		sizeDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		JPanel sizePanel = new JPanel();
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.Y_AXIS));

		final double truewidth = boundrect.getWidth();
		final double trueheight = boundrect.getHeight();

		JLabel trueSize = new JLabel("True size: " + java.lang.Double.toString(Math.rint(truewidth/10))
			+ " x " + java.lang.Double.toString(Math.rint(trueheight/10)) + "m", SwingConstants.CENTER);

		// tediously, definition of Rectangle2D.Double shadows java.lang.Double!

		sizePanel.add(trueSize);

		JPanel subpanel1 = new JPanel();
		subpanel1.setLayout(new BoxLayout(subpanel1, BoxLayout.X_AXIS));
		JLabel scaleLabel = new JLabel("Scale (at 72dpi): 1 :", SwingConstants.RIGHT);
		final JTextField scaleField = new JTextField();
		subpanel1.add(scaleLabel);
		subpanel1.add(scaleField);
		sizePanel.add(subpanel1);

		final JLabel finalSize = new JLabel();
		sizePanel.add(finalSize);

		scaleField.getDocument().addDocumentListener(
			new DocumentListener() {
				public void handleDocumentEvent(DocumentEvent event) {
					try {
						double scale = java.lang.Double.parseDouble(scaleField.getText());
						finalSize.setText( java.lang.Double.toString(Math.ceil(truewidth / scale * 72/0.254)) + "x" +
							 java.lang.Double.toString(Math.ceil(trueheight / scale * 72/0.254)) + " px");
					} catch(Exception e) {}
				}
				public void insertUpdate(DocumentEvent event) { handleDocumentEvent(event); }
				public void removeUpdate(DocumentEvent event) { handleDocumentEvent(event); }
				public void changedUpdate(DocumentEvent event) { handleDocumentEvent(event); }

			});

		scaleField.setText(java.lang.Double.toString(TN.prtscale));
		// we do this now after the DocumentListener is set up
		// so as to initialize the next field

		JPanel subpanel2 = new JPanel();
		subpanel2.setLayout(new BoxLayout(subpanel2, BoxLayout.X_AXIS));

		JButton doOKButton = new JButton("OK");
		JButton doCancelButton = new JButton("Cancel");
		subpanel2.add(doOKButton);
		subpanel2.add(doCancelButton);
		sizePanel.add(subpanel2);

		doOKButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					sizeDialog.setVisible(false);
				}
			});

		doCancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					scaleField.setText("-1");
					sizeDialog.setVisible(false);
				}
			});

		sizeDialog.addWindowListener( new WindowListener() {
			public void windowClosing(WindowEvent e) {
				scaleField.setText("-1");
				sizeDialog.setVisible(false);
			}

			public void windowOpened(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
		});



		sizeDialog.getContentPane().add(sizePanel, BorderLayout.CENTER);
		sizeDialog.pack();
		sizeDialog.setVisible(true);

		// return a value in px per decimeter

		double scalefactor = java.lang.Double.parseDouble(scaleField.getText());
		if(scalefactor == -1.0) return -1.0; // cancelled
		System.out.println("Read scale factor of " + java.lang.Double.toString(scalefactor));
		double pxperdecimetre = 72/0.254/scalefactor;
		System.out.println("corresponding to " + java.lang.Double.toString(pxperdecimetre) + " px per decimetre");
		return pxperdecimetre;

	}


	/////////////////////////////////////////////
	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException
	{
		// copy in the values
		if (nprintcalls == 0)
		{
			pfimageablewidth = pf.getImageableWidth();
			pfimageableheight = pf.getImageableHeight();
			pfimageableX = pf.getImageableX();
			pfimageableY = pf.getImageableY();
		}

		// call the dialog and set the scaling up (except when it's fit to screen).
		if ((prtscalecode != 0) && (nprintcalls == 0))
		{
			if (!PrintScaleSetup())
				return Printable.NO_SUCH_PAGE; // wonder if this will work?
		}

		Graphics2D g2D = (Graphics2D)g;
		try
		{
			return lprint(g2D, pi);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return Printable.NO_SUCH_PAGE;
 	}

	/////////////////////////////////////////////
	// local version with the pageformat removed
	public int lprint(Graphics2D g2D, int pi) throws Exception
	{
		if (prtscalecode != 0)
		{
			TN.emitMessage("Page " + pi + "  : calls " + nprintcalls);
			if (pi >= nptrpagesx * nptrpagesy)
				return Printable.NO_SUCH_PAGE;
			int ipy = pi / nptrpagesx;
			int ipx = pi - ipy * nptrpagesx;
			if (bsinglepageenabled)
			{
				if (pi >= 1)
					return Printable.NO_SUCH_PAGE;
				ipy = singlepageny;
				ipx = singlepagenx;
			}

			double pvx = (prtxlo + prtxhi - nptrpagesx * prtpagewidth) / 2 + ipx * prtpagewidth;
			double pvy = (prtylo + prtyhi - nptrpagesy * prtpageheight) / 2 + ipy * prtpageheight;

			// draw the cutout rectangle in page space
			if(bdrawcutoutrectangle)
			{
				g2D.setStroke(SketchLineStyle.linestyleprintcutout);
				g2D.setColor(SketchLineStyle.linestylegreyed);
				for (int i = 0; i < prtimageablecutrectangle.length; i++)
					g2D.draw(prtimageablecutrectangle[i]);
			}

			// translate to scale space
			mdtrans.setToTranslation(prtimageablex, prtimageabley);
			mdtrans.scale(1.0F / prtimgscale, 1.0F / prtimgscale);
			mdtrans.translate(-pvx, -pvy);

			g2D.transform(mdtrans);
		}

		// fit to screen
		else
		{
			if (pi >= 1)
				return Printable.NO_SUCH_PAGE;

			if (nprintcalls == 0)
				TN.emitMessage("Page dimensions in points inch-width:" + pfimageablewidth/72 + "  inch-height:" + pfimageableheight/72);
			//TN.emitMessage("Page dimensions in points inch-width:" + pf.getImageableWidth()/72 + "  inch-height:" + pf.getImageableHeight()/72);
			mdtrans.setToTranslation((pfimageableX + pfimageablewidth / 2), (pfimageableY + pfimageableheight / 2));

			// scale change relative to the size it's on the screen, so that what's on the screen is visible
			double scchange = Math.min(csize.width / (pfimageablewidth * 1.0F), csize.height / (pfimageableheight * 1.0F));
			mdtrans.scale(scchange, scchange);
			mdtrans.translate(-csize.width / 2, -csize.height / 2);

			// translation is relative to the screen translation; but if you have a better idea you can hard code it.
			g2D.transform(mdtrans);
			g2D.transform(currtrans);
		}

		// do the drawing of it
		tsketch.paintWquality(g2D, bHideCentreline, bHideMarkers, bHideStationNames, vgsymbols, brefilloverlaps);
		nprintcalls++;
		return Printable.PAGE_EXISTS;
	}
};



