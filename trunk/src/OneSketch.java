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
import java.awt.geom.Rectangle2D.Float;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.File;

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
	Rectangle2D rbounds = null;

	boolean bSAreasUpdated = false;
	Vector vsareas = new Vector(); // auto areas
	Vector vsareasalt = new Vector(); // auto areas not in above list

	String backgroundimgname;
	File fbackgimg = null;

	AffineTransform backgimgtrans = new AffineTransform();

	// this gets the clockwise auto-area.
	OneSArea cliparea = null;

	// used for previewing this sketch (when it is a symbol)
	BufferedImage bisymbol = null;
	boolean bSymbolType = false; // tells us which functions are allowed.

	File sketchfile = null;
	boolean bsketchfilechanged = false;

	SketchSymbolAreas sksya = new SketchSymbolAreas();

	// range and restrictions in the display.
	boolean bRestrictSubsetCode = false;

	float zaltlo;
	float zalthi;


	/////////////////////////////////////////////
	int SetSubsetCode(OnePath op, Vector vssubsets)
	{
		op.isubsetcode = 0;
		for (int j = 0; j < op.vssubsets.size(); j++)
		{
			if (vssubsets.contains(op.vssubsets.elementAt(j)))
			{
				op.isubsetcode++;
				if (op.pnstart.isubsetcode < op.isubsetcode)
					op.pnstart.isubsetcode = op.isubsetcode;
				if (op.pnend.isubsetcode < op.isubsetcode)
					op.pnend.isubsetcode = op.isubsetcode;
			}
		}
		return (op.isubsetcode == 0 ? 0 : 1);
	}

	/////////////////////////////////////////////
	void SetSubsetCode(Vector vssubsets)
	{
		// set node codes down to be set up by the paths
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).isubsetcode = 0;

		// set paths according to subset code
		bRestrictSubsetCode = !vssubsets.isEmpty();
		int nsubsetpaths = 0;
		for (int i = 0; i < vpaths.size(); i++)
			nsubsetpaths += SetSubsetCode((OnePath)vpaths.elementAt(i), vssubsets);

		// now scan through the areas and set those in range and their components to visible
		int nsubsetareas = 0;
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			osa.isubsetcode = 0;
			if (bRestrictSubsetCode)
			{
				for (int j = 0; j < osa.refpaths.size(); j++)
				{
					OnePath op = ((RefPathO)osa.refpaths.elementAt(j)).op;
					if ((j == 0) || (osa.isubsetcode > op.isubsetcode))
						osa.isubsetcode = op.isubsetcode;
					if (osa.isubsetcode == 0)
						break;
				}
				if (osa.isubsetcode != 0)
					nsubsetareas++;
			}
		}

		// set subset codes on the symbol areas
		// over-compensate the area; the symbols will spill out.
		int nccaspills = 0;
		for (int i = 0; i < sksya.vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)sksya.vconncom.elementAt(i);
			cca.isubsetcode = 0;
			for (int j = 0; j < cca.vconnareas.size(); j++)
			{
				int assc = ((OneSArea)cca.vconnareas.elementAt(j)).isubsetcode;
				nccaspills = ((j != 0) && (assc != cca.isubsetcode) ? 1 : 0);
				if ((j == 0) || (assc < cca.isubsetcode))
					cca.isubsetcode = assc;
			}
		}
		if (nccaspills != 0)
			TN.emitMessage("There are " + nccaspills + " symbol area spills beyond subset ");

		TN.emitMessage("Subset paths: " + nsubsetpaths + "  areas: " + nsubsetareas);
	}


	/////////////////////////////////////////////
	public String toString()
	{
		// might give more of a name shortly
		return sketchname;
	}


	/////////////////////////////////////////////
	int SelPath(Graphics2D g2D, Rectangle selrect, OnePath prevselpath, Vector tsvpathsviz)
	{
		boolean bOvWrite = true;
		OnePath selpath = null;
		int isel = -1;
		for (int i = 0; i < tsvpathsviz.size(); i++)
		{
			OnePath path = (OnePath)(tsvpathsviz.elementAt(i));
			if ((bOvWrite || (path == prevselpath)) && g2D.hit(selrect, path.gp, true))
			{
				boolean lbOvWrite = bOvWrite;
				bOvWrite = (path == prevselpath);
				if (lbOvWrite)
				{
					selpath = path;
					isel = vpaths.indexOf(selpath);
				}
			}
		}
		return isel;
	}


	/////////////////////////////////////////////
	OneSArea SelArea(Graphics2D g2D, Rectangle selrect, OneSArea prevselarea)
	{
		boolean bOvWrite = true;
		OneSArea selarea = null;
		int isel = -1;
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea oa = (OneSArea)(vsareas.elementAt(i));
			if ((bOvWrite || (oa == prevselarea)) && g2D.hit(selrect, oa.gparea, false))
			{
				boolean lbOvWrite = bOvWrite;
				bOvWrite = (oa == prevselarea);
				if (lbOvWrite)
					selarea = oa;
			}
		}
		return selarea;
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
	void MakeConnectiveComponents()
	{
		// use new symbol layout engine
		sksya.MakeSSA(vpaths);
		sksya.MarkAreasWithConnComp(vsareas);
	}

	/////////////////////////////////////////////
	void MakeSymbolLayout()
	{
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
		if (OneSArea.FindOrientationG(osa.gparea))
		{
			if (bSymbolType && (cliparea != null))
				TN.emitWarning("More than one outerarea for cliparea in symbol " + sketchname);
			cliparea = osa;
			vsareasalt.addElement(osa);
		}

		// take out the areas that have been knocked out by area_signals
		else if (!osa.bShouldrender)
			vsareasalt.addElement(osa);

		// areas that get into the system, put them in sorted.
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
		vsareasalt.removeAllElements();
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
	int TAddPath(OnePath path, OneTunnel vgsymbols)
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
	void TRemovePath(OnePath path)
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
	Rectangle2D getBounds(boolean bForce)
	{
		if ((rbounds == null) || bForce)
		{
			if (!vpaths.isEmpty())
			{
				rbounds = ((OnePath)(vpaths.elementAt(0))).getBounds(null);
				for (int i = 1; i < vpaths.size(); i++)
					rbounds.add(((OnePath)(vpaths.elementAt(i))).getBounds(null));
			}
			else
				rbounds = new Rectangle2D.Float();
		}
		return rbounds;
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
					TAddPath(path, null);
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
			if ((path.linestyle == SketchLineStyle.SLS_CENTRELINE) && (path.plabedl != null))
			{
				String pnlabtail = path.plabedl.tail;
				String pnlabhead = path.plabedl.head;
				if ((pnlabtail != null) && (pnlabhead != null))
				{
					String destpnlabtail = ExportBetween(thtunnel, pnlabtail, otdest);
					String destpnlabhead = ExportBetween(thtunnel, pnlabhead, otdest);

					// search for matching centrelines in destination place.
					OnePath dpath = null;
					for (int j = 0; j < osdest.vpaths.size(); j++)
					{
						OnePath lpath = (OnePath)osdest.vpaths.elementAt(j);
						if ((lpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (lpath.plabedl != null))
						{
							String dpnlabtail = lpath.plabedl.tail;
							String dpnlabhead = lpath.plabedl.head;

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
						TN.emitMessage("Corresponding path to " + path.plabedl.lab);
					}
					else
						TN.emitWarning("No centreline path corresponding to " + path.plabedl.lab + "  " + destpnlabtail + " " + destpnlabhead);
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
				TAddPath(ptrelln.WarpPath(path), vgsymbols);
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
	//static Color fcolw = new Color(1.0F, 1.0F, 1.0F, 0.5F);
	static Color fcolw = new Color(0.8F, 1.0F, 1.0F, 0.6F);
	static Color fcol = new Color(0.1F, 0.2F, 0.4F, 0.6F);
	public void paintWquality(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols)
	{
		// set up the hasrendered flags to begin with
		for (int i = 0; i < vsareas.size(); i++)
			((OneSArea)vsareas.elementAt(i)).bHasrendered = false;
		for (int i = 0; i < vsareasalt.size(); i++)	// areas that are not to be drawn
			((OneSArea)vsareasalt.elementAt(i)).bHasrendered = true;
		for (int i = 0; i < sksya.vconncom.size(); i++)
			((ConnectiveComponentAreas)sksya.vconncom.elementAt(i)).bHasrendered = false;

		// go through the paths and render those at the bottom here and aren't going to be got later
		g2D.setColor(SketchLineStyle.linestylecolprint);
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)(vpaths.elementAt(i));
			assert((op.linestyle != SketchLineStyle.SLS_CENTRELINE) || ((op.karight == null) && (op.kaleft == null)));
			if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				if (((op.karight == null) || op.karight.bHasrendered) && ((op.kaleft == null) || op.kaleft.bHasrendered))
					op.paintWquality(g2D, (!bRestrictSubsetCode || (op.isubsetcode != 0)));
			}
		}


		// go through the areas and complete the paths as we tick them off.
		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			//System.out.println("area zalt " + osa.zalt);

			// draw the wall type strokes related to this area
			// this makes the white boundaries around the strokes !!!
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_SYMBOLOUTLINE]); // thicker than walls
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]);
			for (int j = 0; j < osa.refpathsub.size(); j++)
			{
				OnePath op = ((RefPathO)osa.refpathsub.elementAt(j)).op;
				if (!bRestrictSubsetCode || (op.isubsetcode != 0))
					if ((op.linestyle == SketchLineStyle.SLS_WALL) || (op.linestyle == SketchLineStyle.SLS_ESTWALL))
						g2D.draw(op.gp);
			}

			// fill the area with a diffuse colour
			if (!bRestrictSubsetCode || (osa.isubsetcode != 0))
			{
				g2D.setColor(fcolw);
				g2D.fill(osa.gparea);
			}
			osa.bHasrendered = true;

			// check any paths if they are now done
			for (int j = 0; j < osa.refpaths.size(); j++)
			{
				OnePath op = ((RefPathO)osa.refpaths.elementAt(j)).op;
				if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
				{
					if (((op.karight == null) || op.karight.bHasrendered) && ((op.kaleft == null) || op.kaleft.bHasrendered))
						op.paintWquality(g2D, (!bRestrictSubsetCode || (op.isubsetcode != 0)));
				}
			}

            // check any symbols that are now done
            // (there will be only one last area to come through).
			for (int k = 0; k < osa.ccalist.size(); k++)
			{
				ConnectiveComponentAreas mcca = (ConnectiveComponentAreas)osa.ccalist.elementAt(k);
				if (!bRestrictSubsetCode || (mcca.isubsetcode != 0))
				{
					if (!mcca.bHasrendered)
					{
						int l = 0;
						for ( ; l < mcca.vconnareas.size(); l++)
							if (!((OneSArea)mcca.vconnareas.elementAt(l)).bHasrendered)
								break;
						if (l == mcca.vconnareas.size())
						{
							mcca.paintWsymbols(g2D);
							mcca.bHasrendered = true;
						}
					}
				}
            }
		}

		// old code which rendered all the symbols
		//sksya.paintWsymbols(g2D);

		// draw all the station names inactive
		if (!bHideStationNames)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(SketchLineStyle.linestylecolprint);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (opn.pnstationlabel != null)
				{
					if (!bRestrictSubsetCode || (opn.isubsetcode != 0))
						g2D.drawString(opn.pnstationlabel, (float)opn.pn.getX() + TN.strokew * 2, (float)opn.pn.getY() - TN.strokew);
				}
			}
		}
	}

	/////////////////////////////////////////////
	public void paintWbkgd(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols, Vector tsvpathsviz)
	{
		// draw all the paths inactive.
		//for (int i = 0; i < vpaths.size(); i++)
		for (int i = 0; i < tsvpathsviz.size(); i++)
		{
			OnePath op = (OnePath)(tsvpathsviz.elementAt(i));
			if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				boolean bIsSubsetted = (!bRestrictSubsetCode || (op.isubsetcode != 0)); // we draw subsetted kinds as quality for now
				if (!bIsSubsetted)
				{
					g2D.setColor(SketchLineStyle.linestylegreyed);
					op.paintWnosetcol(g2D, bHideMarkers, false);
				}
				else
					op.paintW(g2D, bHideMarkers, false);
			}
		}

		// draw all the nodes inactive
		if (!bHideMarkers)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_DETAIL]);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (!bRestrictSubsetCode || (opn.isubsetcode != 0))
				{
					if (opn.icolindex != -1)
						g2D.setColor(SketchLineStyle.linestylecolsindex[opn.icolindex]);
					g2D.draw(opn.Getpnell());
				}
			}
		}

		// draw all the station names inactive
		if (!bHideStationNames)
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			g2D.setColor(TN.fontcol);
			g2D.setFont(TN.fontlabs[0]);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (opn.pnstationlabel != null)
				{
					if (!bRestrictSubsetCode || (opn.isubsetcode != 0))
						g2D.drawString(opn.pnstationlabel, (float)opn.pn.getX() + TN.strokew * 2, (float)opn.pn.getY() - TN.strokew);
				}
			}
		}


		// render all the symbols without clipping.
		for (int i = 0; i < tsvpathsviz.size(); i++)
		{
			OnePath op = (OnePath)tsvpathsviz.elementAt(i);
			if (!bRestrictSubsetCode || (op.isubsetcode != 0))
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
			if (!bRestrictSubsetCode || (osa.isubsetcode != 0))
			{
				g2D.setColor(osa.zaltcol);
				g2D.fill(osa.gparea);
			}
		}
	}
};


