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

import java.util.Vector;
import java.util.Random;
import java.io.IOException;
import java.lang.StringBuffer;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.Icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;


/////////////////////////////////////////////
class OneSketch
{
	// arrays of sketch components.
	String sketchname; // used sometimes to build up the file name.

	// main sketch.
	Vector vnodes = new Vector();
	Vector vpaths = new Vector();

	boolean bSAreasUpdated = false;
	Vector vsareas = new Vector(); // auto areas

	String backgroundimgname;
	File fbackgimg = null;

	AffineTransform backgimgtrans = new AffineTransform();

	// this gets the clockwise auto-area.
	OneSArea cliparea = null;

	// used for previewing this sketch (when it is a symbol)
	BufferedImage bisymbol = null;
	ImageIcon iicon = null;
	boolean bSymbolType = false; // tells us which functions are allowed.

	File sketchfile = null;
	boolean bsketchfilechanged = false;

	SketchSymbolAreas sksya = new SketchSymbolAreas();

	// range and restrictions in the display.
	float zaltlo = 0.0F;
	float zalthi = 0.0F;
	boolean bRestrictZalt = false;
	float rzaltlo = 0.0F;
	float rzalthi = 0.0F;

	/////////////////////////////////////////////
	void SetVisibleByZ(float sllow, float slupp)
	{
		rzaltlo = zaltlo * (1.0F - sllow) + zalthi * sllow;
		rzalthi = zaltlo * (1.0F - slupp) + zalthi * slupp;

		// set all paths and nodes invisible, except on heights of centreline
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel != null)
				opn.bvisiblebyz = ((rzaltlo <= opn.zalt) && (rzalthi >= opn.zalt));
			else
				opn.bvisiblebyz = false;
		}

// not working for connectives since they aren't bound by an area, but of something else 
// need to update differently
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				op.bvisiblebyz = (op.pnstart.bvisiblebyz || op.pnend.bvisiblebyz);
			else
				op.bvisiblebyz = false;
		}


		// now scan through the areas and set those in range and their components to visible
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			if ((rzaltlo <= osa.zalt) && (rzalthi >= osa.zalt))
				osa.SetVisibleByZ();
			else
				osa.bvisiblebyz = false;
		}
	}


	/////////////////////////////////////////////
	public String toString()
	{
		// might give more of a name shortly
		return sketchname;
	}

	/////////////////////////////////////////////
	Icon GetIcon(Dimension csize, OneTunnel vgsymbols)
	{
		if (!bSymbolType)
			TN.emitWarning("symbol icon got in non-symbol sketch");

		if ((bisymbol == null) || (bisymbol.getWidth() != csize.width) || (bisymbol.getHeight() != csize.height))
		{
			bisymbol = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_ARGB);
			iicon = null;
		}

		if (iicon == null)
		{
			// redraw the buffered image
			Graphics2D g2d = bisymbol.createGraphics();
			g2d.setColor(Color.lightGray);
			g2d.fillRect(0, 0, csize.width, csize.height);

			Rectangle2D boundrect = getBounds(null);

			AffineTransform at = new AffineTransform();
			at.setToTranslation(csize.width / 2, csize.height / 2);
			if (boundrect != null)
			{
				// scale change
				if ((csize.width != 0) && (csize.height != 0))
				{
					double scchange = Math.max(boundrect.getWidth() / (csize.width * 0.9F), boundrect.getHeight() / (csize.height * 0.9F));
					if (scchange != 0.0F)
						at.scale(1.0F / scchange, 1.0F / scchange);
				}

				at.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2));
			}

			g2d.transform(at);
			paintW(g2d, false, false, true, vgsymbols);  // setting to proper symbols render doesn't seem to help.

			// make the new image icon.
			TN.emitMessage("new icon made");
			iicon = new ImageIcon(bisymbol);
		}
		return (Icon)iicon;
	}

	/////////////////////////////////////////////
	int SelPath(Graphics2D g2D, Rectangle selrect, OnePath prevselpath)
	{
		boolean bOvWrite = true;
		OnePath selpath = null;
		int isel = -1;
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i));
			if ((bOvWrite || (path == prevselpath)) && g2D.hit(selrect, path.gp, true))
			{
				boolean lbOvWrite = bOvWrite;
				bOvWrite = (path == prevselpath);
				if (lbOvWrite)
				{
					selpath = path;
					isel = i;
				}
			}
		}
		return isel;
	}


	/////////////////////////////////////////////
	void SetBackground(File backgrounddir, String lbackgroundimgname)
	{
		if ((lbackgroundimgname == null) || lbackgroundimgname.equals(""))
		{
			backgroundimgname = null;
			fbackgimg = null;
		}

		else
		{
			backgroundimgname = lbackgroundimgname;
			fbackgimg = new File(backgrounddir, backgroundimgname);
		}
	}

	/////////////////////////////////////////////
	OnePath GetAxisPath()
	{
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)vpaths.elementAt(i);
			if (path.linestyle == SketchLineStyle.SLS_CENTRELINE)
				return path;
		}
		return null;
	}


	/////////////////////////////////////////////
	void MakeSymbolLayout()
	{
		// use new symbol layout engine
		sksya.MakeSSA(vpaths);

		// go through the symbols and find their positions and take them out.
		OneSSymbol.islmarkl++;
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			for (int j = 0; j < op.vpsymbols.size(); j++)
			{
				OneSSymbol oss = (OneSSymbol)op.vpsymbols.elementAt(j);
				oss.islmark = OneSSymbol.islmarkl; // comparison against itself.

				if (oss.gsym != null)
					oss.RelaySymbolsPosition(sksya, op.iconncompareaindex);
			}
		}
	}




	/////////////////////////////////////////////
	void AddArea(OneSArea osa)
	{
		if (osa.gparea == null)
			return;

		// the clockwise path is the one bounding the outside.
		// it will say how many distinct pieces there are.
		if (OneSArea.FindOrientation(osa.gparea))
		{
			if (bSymbolType && (cliparea != null))
				TN.emitWarning("More than one outerarea for cliparea in symbol " + sketchname);
			cliparea = osa;
		}

		// added this "else" in after thought it had worked, and then it stopped working after a big change
		else
		{
			// insert in order of height
			int i = 0;
			for ( ; i < vsareas.size(); i++)
			{
				OneSArea loa = (OneSArea)vsareas.elementAt(i);
				if (loa.zalt > osa.zalt)
					break;
			}
			vsareas.insertElementAt(osa, i);
		}
	}

	/////////////////////////////////////////////
	// fills in the opforeright values etc.
	// works selectively on a subset of vnodes.
	void MakeAutoAreas()
	{
		// set values to null.  esp the area links.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			op.karight = null;
			op.kaleft = null;
		}
		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);

		// build the main list which we keep in order for rendering
		vsareas.removeAllElements();
		cliparea = null;

		// now collate the areas.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (op.AreaBoundingType())
			{
				if (op.karight == null)
					AddArea(new OneSArea(op, true)); // this constructer makes all the links too.
				if (op.kaleft == null)
					AddArea(new OneSArea(op, false)); // this constructer makes all the links too.
			}
		}


		// make the range set of the areas
		// this is all to do with setting the zaltlam variable
		float zaaltlo = 0.0F;
		float zaalthi = 0.0F;
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			if ((i == 0) || (osa.zalt < zaaltlo))
				zaaltlo = osa.zalt;
			if ((i == 0) || (osa.zalt > zaalthi))
				zaalthi = osa.zalt;
		}

		float zaaltdiff = zaalthi - zaaltlo;
		if (zaaltdiff == 0.0F)
			zaaltdiff = 1.0F;
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			float zaltlam = (osa.zalt - zaaltlo) / zaaltdiff;

			// spread out a bit.
			zaltlam = (zaltlam + (float)i / Math.max(1, vsareas.size() - 1)) / 2.0F;

			// make the shade for the filling in.
			float greyshade = Math.min(1.0F, zaltlam * 0.4F + 0.4F);
			osa.zaltcol = new Color(greyshade, greyshade, greyshade, 0.2F);
		}
	}




	/////////////////////////////////////////////
	OnePathNode SelNode(Graphics2D g2D, Rectangle selrect)
	{
		OnePathNode res = null;
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode pathnode = (OnePathNode)(vnodes.elementAt(i));
			if (g2D.hit(selrect, pathnode.Getpnell(), false))
			{
				res = pathnode;
				break;
			}
		}
		return res;
	}




	/////////////////////////////////////////////
	int AddPath(OnePath path, OneTunnel vgsymbols)
	{
		assert (path.apforeright == null) && (path.aptailleft == null);

		if (path.pnstart.pathcount == 0)
		{
			assert !vnodes.contains(path.pnstart);
			vnodes.addElement(path.pnstart);
		}
		path.pnstart.InsertOnNode(path, false);

		if (path.pnend.pathcount == 0)
		{
			assert !vnodes.contains(path.pnend);
			vnodes.addElement(path.pnend);
		}
		path.pnend.InsertOnNode(path, true);

		vpaths.addElement(path);
		assert path.pnstart.CheckPathCount();
		assert path.pnend.CheckPathCount();

		if (vgsymbols != null)
			path.GenerateSymbolsFromPath(vgsymbols);

		bSAreasUpdated = false;
		return vpaths.size() - 1;
	}


	/////////////////////////////////////////////
	void RemovePath(OnePath path)
	{
		if (path.pnstart.RemoveOnNode(path, false))
			vnodes.removeElement(path.pnstart);
		if (path.pnend.RemoveOnNode(path, true))
			vnodes.removeElement(path.pnend);

		vpaths.removeElement(path);
		assert (path.pnstart.pathcount == 0) || path.pnstart.CheckPathCount();
		assert (path.pnend.pathcount == 0) || path.pnend.CheckPathCount();
		bSAreasUpdated = false; 
	}



	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans)
	{
		Rectangle2D res = null;
		for (int i = 0; i < vpaths.size(); i++)
		{
			Rectangle2D lres = ((OnePath)(vpaths.elementAt(i))).getBounds(currtrans);
			if (res == null)
				res = lres;
			else
				res.add(lres);
		}
		return res;
	}


	/////////////////////////////////////////////
	void SetUniqueSketchname(Vector tsketches, String lsketchname)
	{
		// incomplete.
		if (tsketches == null)
			sketchname = "***";
		else if (lsketchname != null)
			sketchname = lsketchname;
		else
			sketchname = "sketch" + (tsketches.size());

		// ensure no duplicates
		while (true)
		{
			int i = (tsketches == null ? -1 : tsketches.size() - 1);
			while ((i >= 0) && !sketchname.equals(((OneSketch)tsketches.elementAt(i)).sketchname))
				i--;
			if (i == -1)
				return;
			sketchname = sketchname + "x";
		}
	}

	/////////////////////////////////////////////
	OneSketch(Vector tsketches, String lsketchname)
	{
		SetUniqueSketchname(tsketches, lsketchname);
	}


	/////////////////////////////////////////////
	// map in the centreline types.
	void ImportCentreline(OneTunnel ot)
	{
		OnePathNode[] statpathnode = new OnePathNode[ot.vstations.size()];
		for (int i = 0; i < ot.vlegs.size(); i++)
		{
			OneLeg ol = (OneLeg)(ot.vlegs.elementAt(i));
			if (ol.osfrom != null)
			{
				int ipns = ot.vstations.indexOf(ol.osfrom);
				int ipne = ot.vstations.indexOf(ol.osto);

				if ((ipns != -1) || (ipne != -1))
				{
					if (statpathnode[ipns] == null)
						statpathnode[ipns] = new OnePathNode(ol.osfrom.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osfrom.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osfrom.Loc.z * TN.CENTRELINE_MAGNIFICATION, true);
					if (statpathnode[ipne] == null)
						statpathnode[ipne] = new OnePathNode(ol.osto.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osto.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osto.Loc.z * TN.CENTRELINE_MAGNIFICATION, true);

					OnePath path = new OnePath(statpathnode[ipns], statpathnode[ipne], TNXML.xcomtext(TNXML.sTAIL, ol.osfrom.name) + TNXML.xcomtext(TNXML.sHEAD, ol.osto.name));
					AddPath(path, null);
					path.UpdateStationLabel(bSymbolType);
				}
				else
					TN.emitWarning("Can't find station " + ol.osfrom + " or " + ol.osto);
			}
		}
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		// we default set the sketch condition to unsplined for all edges.
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSKETCH, TNXML.sSPLINED, "0"));

		// write out background
		if (backgroundimgname != null)
		{
			// set the matrix
			double[] flatmat = new double[6];
			backgimgtrans.getMatrix(flatmat);
			los.WriteLine(TNXML.xcomopen(1, TNXML.sAFFINE_TRANSFORM, TNXML.sAFTR_M00, String.valueOf(flatmat[0]), TNXML.sAFTR_M10, String.valueOf(flatmat[1]), TNXML.sAFTR_M01, String.valueOf(flatmat[2]), TNXML.sAFTR_M11, String.valueOf(flatmat[3]), TNXML.sAFTR_M20, String.valueOf(flatmat[4]), TNXML.sAFTR_M21, String.valueOf(flatmat[5])));
			los.WriteLine(TNXML.xcom(2, TNXML.sSKETCH_BACK_IMG, TNXML.sSKETCH_BACK_IMG_FILE, backgroundimgname));
			los.WriteLine(TNXML.xcomclose(1, TNXML.sAFFINE_TRANSFORM));
		}

		// write out the paths.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i));
			int ind0 = vnodes.indexOf(path.pnstart);
			int ind1 = vnodes.indexOf(path.pnend);
			if ((ind0 != -1) && (ind1 != -1))
				path.WriteXML(los, ind0, ind1);
			else
				TN.emitProgError("Path_node missing end " + i);
		}

		los.WriteLine(TNXML.xcomclose(0, TNXML.sSKETCH));
	}




	/////////////////////////////////////////////
	static String ExportBetween(OneTunnel tunnsrc, String stat, OneTunnel otdest)
	{
		OneTunnel ot = tunnsrc;
		while (ot != otdest)
		{
			boolean bExported = false;
			if (stat.indexOf(TN.StationDelimeter) == -1)
			{
				// check for exports
				for (int j = 0; j < ot.vexports.size(); j++)
				{
					// this is okay for *fix as long as tunnel non-null (when stotfrom can be).
					OneExport oe = (OneExport)ot.vexports.elementAt(j);
					if (stat.equalsIgnoreCase(oe.estation))
					{
						stat = oe.ustation;
						bExported = true;
						break;
					}
				}

				if (!bExported)
					stat = TN.StationDelimeter + stat;
			}
			else
				stat = TN.PathDelimeter + stat;

			if (!bExported)
				stat = ot.name + stat;

			ot = ot.uptunnel;
		}
		return stat;
	}

	/////////////////////////////////////////////
	boolean ExtractCentrelinePathCorrespondence(OneTunnel thtunnel, Vector clpaths, Vector corrpaths, OneSketch osdest, OneTunnel otdest)
	{
		// clear the result lists.
		clpaths.clear();
		corrpaths.clear();

		if (osdest == this)
		{
			TN.emitWarning("source and destination sketches the same");
			return false;
		}

		// check that the tunnels go up
		OneTunnel ot = thtunnel;
		while (ot != otdest)
		{
			ot = ot.uptunnel;
			if (ot == null)
			{
				TN.emitWarning("source tunnel does not map up to destination tunnel");
				return false;
			}
		}

		// now start matching the centrelines.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)vpaths.elementAt(i);
			if ((path.linestyle == SketchLineStyle.SLS_CENTRELINE) && (path.plabel != null))
			{
				String pnlabtail = TNXML.xrawextracttext(path.plabel, TNXML.sTAIL);
				String pnlabhead = TNXML.xrawextracttext(path.plabel, TNXML.sHEAD);
				if ((pnlabtail != null) && (pnlabhead != null))
				{
					String destpnlabtail = ExportBetween(thtunnel, pnlabtail, otdest);
					String destpnlabhead = ExportBetween(thtunnel, pnlabhead, otdest);

					// search for matching centrelines in destination place.
					OnePath dpath = null;
					for (int j = 0; j < osdest.vpaths.size(); j++)
					{
						OnePath lpath = (OnePath)osdest.vpaths.elementAt(j);
						if ((lpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (lpath.plabel != null))
						{
							String dpnlabtail = TNXML.xrawextracttext(lpath.plabel, TNXML.sTAIL);
							String dpnlabhead = TNXML.xrawextracttext(lpath.plabel, TNXML.sHEAD);

							if (destpnlabtail.equals(dpnlabtail) && destpnlabhead.equals(dpnlabhead))
							{
								if (dpath != null)
									TN.emitWarning("Ambiguous match of centrelines");
								dpath = lpath;
							}
						}
					}

					if (dpath != null)
					{
						clpaths.addElement(path);
						corrpaths.addElement(dpath);
						TN.emitMessage("Corresponding path to " + path.plabel);
					}
					else
						TN.emitWarning("No centreline path corresponding to " + path.plabel + "  " + destpnlabtail + " " + destpnlabhead);
				}
			}
		}

		// false if no correspondence
		if (clpaths.size() == 0)
		{
			TN.emitWarning("no corresponding centrelines found");
			return false;
		}
		return true;
	}


	/////////////////////////////////////////////
	void ImportDistorted(OneSketch isketch, Vector clpaths, Vector corrpaths, OneTunnel vgsymbols)
	{
		// the weights for the paths.
		PtrelLn ptrelln = new PtrelLn(clpaths, corrpaths);
		ptrelln.Extendallnodes(isketch.vnodes);

		// warping over the paths
		for (int i = 0; i < isketch.vpaths.size(); i++)
		{
			OnePath path = (OnePath)isketch.vpaths.elementAt(i);
			if (path.linestyle != SketchLineStyle.SLS_CENTRELINE)
				AddPath(ptrelln.WarpPath(path), vgsymbols);
		}
	}

	/////////////////////////////////////////////
	// no better way of dealing with the problem.
	void UpdateZalts(boolean bFromStationsOnly)
	{
		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i);
			if (!pathnode.bzaltset || (bFromStationsOnly && (pathnode.pnstationlabel == null)))
			{
				// find closest node
				int jc = -1;
				float pndsq = -1.0F;
				for (int j = 0; j < vnodes.size(); j++)
				{
					if (j != i)
					{
						OnePathNode lpathnode = (OnePathNode)vnodes.elementAt(j);
						if (!bFromStationsOnly || (lpathnode.pnstationlabel != null))
						{
							float dx = (float)(pathnode.pn.getX() - lpathnode.pn.getX());
							float dy = (float)(pathnode.pn.getY() - lpathnode.pn.getY());
							float lpndsq = dx * dx + dy * dy;
							if ((jc == -1) || (lpndsq < pndsq))
							{
								jc = j;
								pndsq = lpndsq;
							}
						}
					}
				}

				if (jc != -1)
				{
					OnePathNode lpathnode = (OnePathNode)vnodes.elementAt(jc);
					pathnode.zalt = lpathnode.zalt;
					pathnode.bzaltset = true;
				}
			}
		}
	}


	/////////////////////////////////////////////
	GeneralPath gpgrid = new GeneralPath();
	float ngridspacing = 0.0F;
	Point2D ptsgrid = new Point2D.Float();
	String strgrid = "";

	public void GenerateMetreGrid(Point2D.Float gridscrcorner, float gridscrrad, Point2D.Float gridscrmid)
	{
		gpgrid.reset();

		float pngridspacing = ngridspacing;
		ngridspacing = TN.CENTRELINE_MAGNIFICATION; // the size of one metre.
		for (int s = 0; s < 5; s++)
		{
			if (gridscrrad / ngridspacing < TN.MAX_GRIDLINES)
				break;
			ngridspacing *= ((s % 2) == 0 ? 5 : 2);  //  up by fives and tens.
		}
		if (pngridspacing != ngridspacing)
			strgrid = String.valueOf((int)(ngridspacing / TN.CENTRELINE_MAGNIFICATION + 0.5F));

		int imx = (int)(gridscrcorner.getX() / ngridspacing);
		int imy = (int)(gridscrcorner.getY() / ngridspacing);
		int nglines = Math.min(TN.MAX_GRIDLINES, (int)(gridscrrad / ngridspacing + 1.0F));

		float xs = ngridspacing * imx;
		float ys = ngridspacing * imy;

		for (int i = 0; i <= nglines; i++)
		{
			float yl = ngridspacing * (imy + i);
			gpgrid.moveTo((float)gridscrcorner.getX() - gridscrrad, yl);
			gpgrid.lineTo((float)gridscrcorner.getX() + gridscrrad, yl);

			gpgrid.moveTo((float)gridscrcorner.getX() - gridscrrad, ngridspacing * (imy - i - 1));
			gpgrid.lineTo((float)gridscrcorner.getX() + gridscrrad, ngridspacing * (imy - i - 1));

			float xl = ngridspacing * (imx + i);
			gpgrid.moveTo(xl, (float)gridscrcorner.getY() - gridscrrad);
			gpgrid.lineTo(xl, (float)gridscrcorner.getY() + gridscrrad);

			gpgrid.moveTo(ngridspacing * (imx - i - 1), (float)gridscrcorner.getY() - gridscrrad);
			gpgrid.lineTo(ngridspacing * (imx - i - 1), (float)gridscrcorner.getY() + gridscrrad);

			if (xl < gridscrmid.getX())
				xs = xl;
			if (yl < gridscrmid.getY())
				ys = yl;
		}

		ptsgrid.setLocation(xs - ngridspacing * 0.5F - 2 * TN.strokew * strgrid.length(), ys - TN.strokew * 0.05F);
		//TN.emitMessage("Gridspacing " + ngridspacing + "  " + strgrid + "  nglines " + nglines);
	}


	/////////////////////////////////////////////
	public void DrawMetreGrid(Graphics2D g2D)
	{
		// we will be able to draw this clipped to the outside of all the other geometry drawn, I hope.
		// so it's like the nice background in svx.

		g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_CENTRELINE]); // thin
		g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_FILLED]); // black
		g2D.draw(gpgrid);
		if (strgrid.length() != 0)
			g2D.drawString(strgrid, (float)ptsgrid.getX(), (float)ptsgrid.getY());
	}


	/////////////////////////////////////////////
	public void paintWquality(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols)
	{
		sksya.paintWsymbols(g2D);

		// draw all the paths inactive.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i));
			if (!bHideCentreline || (path.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				if (!bRestrictZalt || path.bvisiblebyz)
					path.paintW(g2D, bHideMarkers, false, true);
			}
		}

		// draw all the station names inactive
		if (!bHideStationNames)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(SketchLineStyle.linestylecolprint);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i);
				if (pathnode.pnstationlabel != null)
				{
					if (!bRestrictZalt || pathnode.bvisiblebyz)
						g2D.drawString(pathnode.pnstationlabel, (float)pathnode.pn.getX() + TN.strokew * 2, (float)pathnode.pn.getY() - TN.strokew);
				}
			}
		}
	}

	/////////////////////////////////////////////
	public void paintW(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols)
	{
		// draw all the paths inactive.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i));
			if (!bHideCentreline || (path.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				if (!bRestrictZalt || path.bvisiblebyz)
					path.paintW(g2D, bHideMarkers, false, false);
			}
		}

		// draw all the nodes inactive
		if (!bHideMarkers)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_DETAIL]);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i);
				if (!bRestrictZalt || pathnode.bvisiblebyz)
				{
					if (pathnode.icolindex != -1)
						g2D.setColor(SketchLineStyle.linestylecolsindex[pathnode.icolindex]);
					g2D.draw(pathnode.Getpnell());
				}
			}
		}

		// draw all the station names inactive
		if (!bHideStationNames)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(TN.fontcol);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i);
				if (pathnode.pnstationlabel != null)
				{
					if (!bRestrictZalt || pathnode.bvisiblebyz)
						g2D.drawString(pathnode.pnstationlabel, (float)pathnode.pn.getX() + TN.strokew * 2, (float)pathnode.pn.getY() - TN.strokew);
				}
			}
		}


		// render all the symbols without clipping.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (!bRestrictZalt || op.bvisiblebyz)
			{
				for (int j = 0; j < op.vpsymbols.size(); j++)
				{
					OneSSymbol msymbol = (OneSSymbol)op.vpsymbols.elementAt(j);
					msymbol.paintW(g2D, false, false);
				}
			}
		}

		// shade in the areas according to depth
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			if (!bRestrictZalt || osa.bvisiblebyz)
			{
				g2D.setColor(osa.zaltcol);
				g2D.fill(osa.gparea);
			}
		}
	}
};


