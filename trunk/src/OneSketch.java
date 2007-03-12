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

import java.awt.Graphics2D;

import java.util.Vector;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set; 
import java.util.HashSet; 

import java.io.IOException;
import java.lang.StringBuffer;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.IOException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/////////////////////////////////////////////
class OneSketch
{
	// this must always be set
	FileAbstraction sketchfile = null;
	boolean bsketchfileloaded = false; 

	// arrays of sketch components.
	String sketchsymbolname; // not null if it's a symbol type
	boolean bSymbolType = false; // tells us which functions are allowed.

	// this could keep an update of deletes, inserts, and changes in properties (a flag on the path)
	boolean bsketchfilechanged = false;

	// the scale import A4 magnifies to.  in future this might be a member value that's saved to the file
	// there's lots of problems with making fonts too small and magnifying them back up.  
	// value could be useful with the printing on the sheet.  
	// this really only applies when we're dealing with framed sketches.  
	float realpaperscale = 1000.0F; 
	String papersizename = ""; 
	
	// main sketch.
	Vector vnodes;
	Vector vpaths;   // this is saved out into XML

	Vec3 sketchLocOffset; // sets it to zero by default

	Rectangle2D rbounds = null;

	boolean bSAreasUpdated = false;
	SortedSet<OneSArea> vsareas; 

	Set<String> sallsubsets; 

	List<String> backgroundimgnamearr; 
	List<AffineTransform> backgimgtransarr; 
	int ibackgroundimgnamearrsel = -1;

	// this gets the clockwise auto-area.
	OneSArea cliparea = null;

	SketchSymbolAreas sksya;  // this is a vector of ConnectiveComponents

	// range and restrictions in the display.
	boolean bRestrictSubsetCode = false;

	float zaltlo;
	float zalthi;

	boolean bSymbolLayoutUpdated = false;

	SubsetAttrStyle sksascurrent = null;


	/////////////////////////////////////////////
	OneSketch(FileAbstraction lsketchfile)
	{
		sketchfile = lsketchfile;
		bsketchfileloaded = false; 
	}

	/////////////////////////////////////////////
	void SetupSK()
	{
		assert !bsketchfileloaded; 

		// main sketch.
		vnodes = new Vector();
		vpaths = new Vector();   // this is saved out into XML
		sketchLocOffset = new Vec3(); // sets it to zero by default
		vsareas = new TreeSet<OneSArea>(); 
		sallsubsets = new HashSet<String>(); 
		backgroundimgnamearr = new ArrayList<String>(); 
		backgimgtransarr = new ArrayList<AffineTransform>(); 
		sksya = new SketchSymbolAreas();  // this is a vector of ConnectiveComponents

		bsketchfileloaded = true; 
	}
	
	
	/////////////////////////////////////////////
	void ApplySplineChange()
	{
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (OnePath.bHideSplines && op.bSplined)
				op.Spline(false, false);
			else if (!OnePath.bHideSplines && !op.bSplined && op.bWantSplined)
				op.Spline(true, false);
		}
	}




	/////////////////////////////////////////////
	// the complexity comes when the opfront is also in the list and must be suppressed.
	OnePathNode SelNode(OnePathNode opfront, boolean bopfrontvalid, Graphics2D g2D, Rectangle selrect, OnePathNode selpathnodecycle)
	{
		boolean bOvWrite = true;
		OnePathNode selnode = null;
		for (int i = 0; i <= vnodes.size(); i++)
		{
			OnePathNode pathnode = (i < vnodes.size() ? (OnePathNode)(vnodes.elementAt(i)) : opfront);
			if ((pathnode != null) && (bopfrontvalid || (pathnode != opfront)) && (bOvWrite || (pathnode == selpathnodecycle)) && g2D.hit(selrect, pathnode.Getpnell(), false))
			{
				boolean lbOvWrite = bOvWrite;
				bOvWrite = (pathnode == selpathnodecycle);
				if (lbOvWrite)
					selnode = pathnode;
			}
		}
		return selnode;
	}


	/////////////////////////////////////////////
	OnePath SelPath(Graphics2D g2D, Rectangle selrect, OnePath prevselpath, List<OnePath> tsvpathsviz)
	{
		boolean bOvWrite = true;
		OnePath selpath = null;
		assert selrect != null;
		int isel = -1;
		for (int i = tsvpathsviz.size() - 1; i >= 0; i--)
		{
			OnePath path = tsvpathsviz.get(i); 
			assert path.gp != null;
			if ((bOvWrite || (path == prevselpath)) &&
				(g2D.hit(selrect, path.gp, true) ||
				 ((path.plabedl != null) && (path.plabedl.drawlab != null) && (path.plabedl.rectdef != null) && g2D.hit(selrect, path.plabedl.rectdef, false))))
			{
				boolean lbOvWrite = bOvWrite;
				bOvWrite = (path == prevselpath);
				if (lbOvWrite)
					selpath = path;
			}
		}
		return selpath;
	}


	/////////////////////////////////////////////
	OneSArea SelArea(Graphics2D g2D, Rectangle selrect, OneSArea prevselarea)
	{
		boolean bOvWrite = true;
		OneSArea selarea = null;
		int isel = -1;
		for (OneSArea oa : vsareas)
		{
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
	int AddBackground(String lbackgroundimgname, AffineTransform lbackgimgtrans)
	{
		assert backgimgtransarr.size() == backgimgtransarr.size();
		backgroundimgnamearr.add(lbackgroundimgname);
		backgimgtransarr.add(lbackgimgtrans);
//System.out.println("Adding background " + lbackgroundimgname);
		return backgimgtransarr.size() - 1;
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
	void MakeConnectiveComponentsT()
	{
		// use new symbol layout engine
		sksya.MakeSSA(vpaths, vsareas);
	}

	/////////////////////////////////////////////
	boolean MakeSymbolLayout(GraphicsAbstraction ga, Rectangle windowrect)
	{
		// go through the symbols and find their positions and take them out.
		boolean bres = true;
		for (MutualComponentArea mca : sksya.vconncommutual)
		{
			if ((windowrect == null) || mca.hit(ga, windowrect))
				mca.LayoutMutualSymbols(); // all symbols in this batch
			else
			{
				//TN.emitMessage("skipping mutualcomponentarea");
				bres = false;
			}
		}
		return bres;
	}




	/////////////////////////////////////////////
	void AddArea(OnePath lop, boolean lbFore, List<OneSArea> vsareastakeout)
	{
		OneSArea osa = new OneSArea(lop, lbFore);
		if (osa.gparea == null) // no area (just a tree)
		{
			vsareastakeout.add(osa);
			osa.iareapressig = SketchLineStyle.ASE_NOAREA; 
			return;  // no linking created
		}
        // iareapressig gets picked up by the iteration around the contour the paths which make up this area

		// the clockwise path is the one bounding the outside.
		// it will say how many distinct pieces there are.

		int aread = OneSArea.FindOrientationReliable(osa.gparea);

		// can't determin orientation (should set the karight to null)
		if (aread != 1) // good areas are always clockwise
		{
			if (aread == -1)
			{
				if (bSymbolType && (cliparea != null))
					TN.emitWarning("More than one outerarea for cliparea in symbol " + sketchsymbolname);
				cliparea = osa; // the outer area thing if not a
			}
			osa.iareapressig = SketchLineStyle.ASE_OUTERAREA;
			vsareastakeout.add(osa);
			return;
		}

		// take out the areas that have been knocked out by area_signals
		if (osa.iareapressig == SketchLineStyle.ASE_KILLAREA) // rock/tree type (not pitchhole)
		{
			vsareastakeout.add(osa);
			return;
		}

		vsareas.add(osa);
	}

	/////////////////////////////////////////////
	// fills in the opforeright values etc.
	// works selectively on a subset of vnodes.
	void MakeAutoAreas()
	{
		assert bsketchfileloaded; 

		// set values to null.  esp the area links.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			op.karight = null;
			op.kaleft = null;
		}
		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);

		// build the main list which we keep in order for rendering
		vsareas.clear();
		cliparea = null;

		// now collate the areas.
		List<OneSArea> vsareastakeout = new ArrayList<OneSArea>();  
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (op.AreaBoundingType())
			{
				if (op.karight == null)
					AddArea(op, true, vsareastakeout); // this constructer makes all the links too.
				if (op.kaleft == null)
					AddArea(op, false, vsareastakeout); // this constructer makes all the links too.
			}
		}

		// Now clear out the links in the altareas
		for (OneSArea osa : vsareastakeout)
			osa.SetkapointersClear();

		if (vsareas.isEmpty())
			return; 

		// make the range set of the areas
		// this is all to do with setting the zaltlam variable
		float zaaltlo = vsareas.first().zalt; 
		float zaalthi = vsareas.last().zalt; 
		assert zaaltlo <= zaalthi; 

		float zaaltdiff = zaalthi - zaaltlo;
		if (zaaltdiff == 0.0F)
			zaaltdiff = 1.0F;
		for (OneSArea osa : vsareas)
		{
			//float zaltlam = (osa.zalt - zaaltlo) / zaaltdiff;

			// spread out a bit.
			//zaltlam = (zaltlam + (float)i / Math.max(1, vsareas.size() - 1)) / 2.0F;

			// set the shade for the filling in.
			osa.zaltcol = null;
		}
	}



	/////////////////////////////////////////////
	int TAddPath(OnePath path, OneTunnel vgsymbols)
	{
		assert (path.apforeright == null) && (path.aptailleft == null);

		if (path.pnstart.pathcount == 0)
		{
			assert !vnodes.contains(path.pnstart);
			path.pnstart.SetNodeCloseBefore(vnodes, vnodes.size());
			vnodes.addElement(path.pnstart);
		}
		path.pnstart.InsertOnNode(path, false);

		if (path.pnend.pathcount == 0)
		{
			assert !vnodes.contains(path.pnend);
			path.pnend.SetNodeCloseBefore(vnodes, vnodes.size());
			vnodes.addElement(path.pnend);
		}
		path.pnend.InsertOnNode(path, true);

		vpaths.addElement(path);
		assert path.pnstart.CheckPathCount();
		assert path.pnend.CheckPathCount();

		return vpaths.size() - 1;
	}


	/////////////////////////////////////////////
	boolean TRemovePath(OnePath op)
	{
		// remove any areas automatically
		if (op.AreaBoundingType())
		{
			if (op.kaleft != null)
			{
				// can be falsified if there's been a change from a wall to a connective type
				//assert vsareas.contains(op.kaleft);
				vsareas.remove(op.kaleft);
				op.kaleft.SetkapointersClear();
			}
			if (op.karight != null)
			{
				//assert vsareas.contains(op.karight);
				vsareas.remove(op.karight);
				op.karight.SetkapointersClear();
			}
		}
		else if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.pthcca != null))
		{
System.out.println("removingPathfrom CCA"); 
			assert op.pthcca.vconnpaths.contains(op); 
			op.pthcca.vconnpaths.remove(op); 
		}

		if (op.pnstart.RemoveOnNode(op, false))
			vnodes.removeElement(op.pnstart);
		if (op.pnend.RemoveOnNode(op, true))
			vnodes.removeElement(op.pnend);

		assert (op.pnstart.pathcount == 0) || op.pnstart.CheckPathCount();
		assert (op.pnend.pathcount == 0) || op.pnend.CheckPathCount();
		return vpaths.removeElement(op);
	}



	/////////////////////////////////////////////
	Rectangle2D getBounds(boolean bForce, boolean bOfSubset)
	{
		if (!bForce && (rbounds != null) && !bOfSubset)
			return rbounds;

		Rectangle2D.Float lrbounds = new Rectangle2D.Float();
		boolean bFirst = true;
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (!bOfSubset || !bRestrictSubsetCode || op.bpathvisiblesubset)
			{
				if (bFirst)
				{
					lrbounds.setRect(op.getBounds(null));
                    bFirst = false;
			    }
				else
					lrbounds.add(op.getBounds(null));
			}
		}

		// cache the result
		if (!bOfSubset)
			rbounds = lrbounds;

		return lrbounds;
	}

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		// we default set the sketch condition to unsplined for all edges.
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSKETCH, TNXML.sSPLINED, "0", TNXML.sLOCOFFSETX, java.lang.Float.toString(sketchLocOffset.x), TNXML.sLOCOFFSETY, java.lang.Float.toString(sketchLocOffset.y), TNXML.sLOCOFFSETZ, java.lang.Float.toString(sketchLocOffset.z)));

		for (int i = 0; i < backgroundimgnamearr.size(); i++)
		{
			// set the matrix (if it exists)
			AffineTransform backgimgtrans = backgimgtransarr.get(i);
			if (backgimgtrans != null)
			{
				double[] flatmat = new double[6];
				backgimgtrans.getMatrix(flatmat);
				los.WriteLine(TNXML.xcomopen(1, TNXML.sAFFINE_TRANSFORM, TNXML.sAFTR_M00, String.valueOf(flatmat[0]), TNXML.sAFTR_M10, String.valueOf(flatmat[1]), TNXML.sAFTR_M01, String.valueOf(flatmat[2]), TNXML.sAFTR_M11, String.valueOf(flatmat[3]), TNXML.sAFTR_M20, String.valueOf(flatmat[4]), TNXML.sAFTR_M21, String.valueOf(flatmat[5])));
			}

			// write the name of the file
			los.WriteLine(TNXML.xcom(2, TNXML.sSKETCH_BACK_IMG, TNXML.sSKETCH_BACK_IMG_FILE, backgroundimgnamearr.get(i), TNXML.sSKETCH_BACK_IMG_FILE_SELECTED, (i == ibackgroundimgnamearrsel ? "1" : "0")));

			if (backgimgtrans != null)
				los.WriteLine(TNXML.xcomclose(1, TNXML.sAFFINE_TRANSFORM));
		}

		// write out the paths.
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i));
			int ind0 = vnodes.indexOf(path.pnstart);
			int ind1 = vnodes.indexOf(path.pnend);
			if ((ind0 != -1) && (ind1 != -1))
				path.WriteXML(los, ind0, ind1, 1);
			else
				TN.emitProgError("Path_node missing end " + i);
		}

		los.WriteLine(TNXML.xcomclose(0, TNXML.sSKETCH));
	}




	/////////////////////////////////////////////
	OnePath FindMatchingCentrelinePath(String destpnlabtail, String destpnlabhead, OneSketch osdest)
	{
		String ldestpnlabtail = destpnlabtail.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');
		String ldestpnlabhead = destpnlabhead.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');

		// search for matching centrelines in destination place.
		OnePath dpath = null;
		for (int j = 0; j < osdest.vpaths.size(); j++)
		{
			OnePath lpath = (OnePath)osdest.vpaths.elementAt(j);
			if ((lpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (lpath.plabedl != null))
			{
				String dpnlabtail = lpath.plabedl.tail.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');
				String dpnlabhead = lpath.plabedl.head.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');

				if (ldestpnlabtail.equals(dpnlabtail) && ldestpnlabhead.equals(dpnlabhead))
				{
					if (dpath != null)
						TN.emitWarning("Ambiguous match of centrelines: " + dpnlabtail + " -> " + dpnlabhead);
					dpath = lpath;
				}
			}
		}
		return dpath;
	}


	
	/////////////////////////////////////////////
	boolean ExtractCentrelinePathCorrespondence(OneTunnel thtunnel, List<OnePath> clpaths, List<OnePath> corrpaths, OneSketch osdest, OneTunnel otdest)
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
					// try to find a matching path, running a re-export if necessary
					OnePath dpath = FindMatchingCentrelinePath(CorrespondSketchs.ExportBetween(thtunnel, pnlabtail, otdest), CorrespondSketchs.ExportBetween(thtunnel, pnlabhead, otdest), osdest);
					if (dpath == null)
						dpath = FindMatchingCentrelinePath(CorrespondSketchs.ExportBetween(thtunnel, CorrespondSketchs.ReExportNameRecurse(thtunnel, pnlabtail), otdest), CorrespondSketchs.ExportBetween(thtunnel, CorrespondSketchs.ReExportNameRecurse(thtunnel, pnlabhead), otdest), osdest);
					if (dpath != null)
					{
						clpaths.add(path);
						corrpaths.add(dpath);
						//TN.emitMessage("Corresponding path to " + path.plabedl.toString());
					}
					else
						TN.emitWarning("No centreline path corresponding to " + path.plabedl.toString()/* + "  " + destpnlabtail + " " + destpnlabhead*/);
				}
			}
		}

		// false if no correspondence
		if (clpaths.isEmpty())
		{
			TN.emitWarning("no corresponding centrelines found");
			return false;
		}
		return true;
	}





	/////////////////////////////////////////////
boolean bWallwhiteoutlines = true;

	/////////////////////////////////////////////
	void pwqWallOutlinesPath(GraphicsAbstraction ga, OnePath op)
	{
		if (op.ciHasrendered != 0)
			return;
		op.ciHasrendered = 1;
		if (bRestrictSubsetCode && op.bpathvisiblesubset)
			return;
		if ((op.linestyle == SketchLineStyle.SLS_INVISIBLE) || (op.linestyle == SketchLineStyle.SLS_CONNECTIVE))
			return;
		if (op.subsetattr.linestyleattrs[op.linestyle] == null)
			return;
		if (op.subsetattr.shadowlinestyleattrs[op.linestyle].linestroke == null)
			return;

		ga.drawPath(op, op.subsetattr.shadowlinestyleattrs[op.linestyle]);
	}

	/////////////////////////////////////////////
	void pwqWallOutlinesArea(GraphicsAbstraction ga, OneSArea osa)
	{
		for (RefPathO rpo : osa.refpathsub)
		{
			pwqWallOutlinesPath(ga, rpo.op);
			paintWqualityjoiningpaths(ga, rpo.ToNode(), true);
		}
	}

	/////////////////////////////////////////////
	void pwqPathsNonAreaNoLabels(GraphicsAbstraction ga, boolean bHideCentreline, Rectangle2D abounds)
	{
		// check any paths if they are now done
		for (int j = 0; j < vpaths.size(); j++)
		{
			OnePath op = (OnePath)vpaths.elementAt(j);
			op.ciHasrendered = 0;
			if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
			{
				op.pnstart.pathcountch++;
				op.pnend.pathcountch++;
				op.ciHasrendered = 2;
				continue;
			}

			// path belongs to an area
			if ((op.karight != null) || (op.kaleft != null))
				continue;

			// no shadows are painted on unarea types
			op.pnstart.pathcountch++;
			op.pnend.pathcountch++;
			op.ciHasrendered = 3;

			if (bHideCentreline && (op.linestyle == SketchLineStyle.SLS_CENTRELINE))
				continue;
			if ((abounds != null) && !op.gp.intersects(abounds))
				continue;

			// the rest of the drawing of this path with quality
			op.paintWquality(ga);
		}
	}

	/////////////////////////////////////////////
	void paintWqualityjoiningpaths(GraphicsAbstraction ga, OnePathNode opn, boolean bShadowpaths)
	{
		OnePath op = opn.opconn;
		boolean bFore = (op.pnend == opn);
		do
		{
			if (bShadowpaths)
				pwqWallOutlinesPath(ga, op);

   			else if ((op.ciHasrendered != 3) && (op.pnstart.pathcountch == op.pnstart.pathcount) && (op.pnend.pathcountch == op.pnend.pathcount))
			{
				op.paintWquality(ga);
				op.ciHasrendered = 3;
			}

			if (!bFore)
        	{
				bFore = op.baptlfore;
				op = op.aptailleft;
			}
			else
			{
				bFore = op.bapfrfore;
				op = op.apforeright;
        	}
		}
		while (!((op == opn.opconn) && (bFore == (op.pnend == opn))));
	}

	/////////////////////////////////////////////
	void pwqPathsOnAreaNoLabels(GraphicsAbstraction ga, OneSArea osa, Rectangle2D abounds)
	{
		// there are duplicates in the refpaths list, so we cannot inline this check
		for (RefPathO rpo : osa.refpaths)
			assert (rpo.op.ciHasrendered <= 1);

		// check any paths if they are now done
		for (RefPathO rpo : osa.refpaths)
		{
			OnePath op = rpo.op;
			assert ((op.karight == osa) || (op.kaleft == osa));
			if (op.ciHasrendered >= 2)
				continue;
			if (((op.karight != null) && !op.karight.bHasrendered) || ((op.kaleft != null) && !op.kaleft.bHasrendered))
				continue;
			op.ciHasrendered = 2;
			op.pnstart.pathcountch++;
			op.pnend.pathcountch++;
			assert op.pnstart.pathcountch <= op.pnstart.pathcount;
			assert op.pnend.pathcountch <= op.pnend.pathcount;
			if ((abounds != null) && !op.gp.intersects(abounds))
				continue;

			// the rest of the drawing of this path with quality
			if (bWallwhiteoutlines)
			{
				// now embed drawing all the lines connecting to the two end-nodes
				if (op.pnstart.pathcountch == op.pnstart.pathcount)
					paintWqualityjoiningpaths(ga, op.pnstart, false);
				if (op.pnend.pathcountch == op.pnend.pathcount)
					paintWqualityjoiningpaths(ga, op.pnend, false);
			}
			else
			{
				op.paintWquality(ga);
				op.ciHasrendered = 3;
			}
		}
	}



	/////////////////////////////////////////////
	void pwqSymbolsOnArea(GraphicsAbstraction ga, OneSArea osa)
	{
		//ga.setColor(SketchLineStyle.linestylecolprint);
		// check any symbols that are now done
		// (there will be only one last area to come through).

		// once all areas in the connective component have been rendered, the symbols get rendered.
		// in practice, this is equivalent to the connective component being rendered when the last area in its list gets rendered
		// after we render an area, the only changes could happen with the connective components that had that area
		for (ConnectiveComponentAreas mcca : osa.ccalist)
		{
			if (!bRestrictSubsetCode || mcca.bccavisiblesubset)
			{
				if (!mcca.bHasrendered)
				{
					boolean bHasr = false;  // basically does an and across values in this list -- might be better with a count
					for (OneSArea cosa : mcca.vconnareas)
					{
						if (!cosa.bHasrendered)
						{
							bHasr = true;
							break;
						}
					}
					if (!bHasr)
					{
						mcca.paintWsymbolsandwords(ga);
						mcca.bHasrendered = true;
					}
				}
			}
		}
	}

	/////////////////////////////////////////////
	void pwqFillArea(GraphicsAbstraction ga, OneSArea osa)
	{
		assert osa.subsetattr != null;
		if (osa.subsetattr.areamaskcolour != null) //This shadow lightens the background, I think this should be combined with drawing the colour
			ga.fillArea(osa, osa.subsetattr.areamaskcolour);

		if (osa.subsetattr.areacolour != null)
			ga.fillArea(osa, osa.zaltcol == null ? osa.subsetattr.areacolour : osa.zaltcol);
	}

	/////////////////////////////////////////////
	boolean binpaintWquality = false;
 	void pwqFramedSketch(GraphicsAbstraction ga, OneSArea osa, OneTunnel vgsymbols, SketchLineStyle sketchlinestyle)
	{
		// the frame sketch
		if (osa.pframesketch == null)
		{
			ga.fillArea(osa, Color.lightGray);
			return;
		}

		// can't simultaneously render (prevents a recursion)
		if (osa.pframesketch.binpaintWquality)
			return;

		ga.startFrame(osa, osa.pframesketchtrans);
		System.out.println("stylename " + osa.pldframesketch.sfstyle + " ()()"); 
		SubsetAttrStyle sksas = sketchlinestyle.GetSubsetSelection(osa.pldframesketch.sfstyle); 
		if ((sksas != null) && (sksas != osa.pframesketch.sksascurrent))
		{
			TN.emitMessage("Setting sketchstyle to " + sksas.stylename + " (maybe should relay the symbols)"); 
			osa.pframesketch.SetSubsetAttrStyle(sksas, vgsymbols); 
			//osa.pframesketch.SetSubsetVisibleCodeStringsT(null, false);
			TN.emitMessage("SHould be updateing all here"); 
		}
		else
			System.out.println("Notsetting sketchstyle " + sksas); 	

		osa.pframesketch.paintWqualitySketch(ga, false, true, true, vgsymbols, null);
		ga.endFrame();
	}


	/////////////////////////////////////////////
	void SetSubsetAttrStyle(SubsetAttrStyle lsksascurrent, OneTunnel vgsymbols)
	{
		sksascurrent = lsksascurrent; 
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).SetSubsetAttrs(sksascurrent, vgsymbols);
		for (OneSArea osa : vsareas)
			osa.SetSubsetAttrs(true, sksascurrent);
	}
	
	/////////////////////////////////////////////
	public void paintWqualitySketch(GraphicsAbstraction ga, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols, SketchLineStyle sketchlinestyle)
	{
		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);
		binpaintWquality = true;

		// set up the hasrendered flags to begin with
		for (OneSArea osa : vsareas)
			osa.bHasrendered = false;
		for (ConnectiveComponentAreas cca : sksya.vconncom)
			cca.bHasrendered = false;
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).pathcountch = 0;  // count these up as we draw them

		// go through the paths and render those at the bottom here and aren't going to be got later
		pwqPathsNonAreaNoLabels(ga, bHideCentreline, null);

		// go through the areas and complete the paths as we tick them off.
		for (OneSArea osa : vsareas)
		{
			// draw the wall type strokes related to this area
			// this makes the white boundaries around the strokes !!!
			if (bWallwhiteoutlines)
				pwqWallOutlinesArea(ga, osa);

			// fill the area with a diffuse colour (only if it's a drawing kind)
			if (!bRestrictSubsetCode || osa.bareavisiblesubset)  // setting just for previewing
			{
				if (osa.iareapressig == SketchLineStyle.ASE_KEEPAREA)
					pwqFillArea(ga, osa);

				// could have these sorted by group subset style, and remake it for these
				if (osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME)
					pwqFramedSketch(ga, osa, vgsymbols, sketchlinestyle);
			}
			assert !osa.bHasrendered;
			osa.bHasrendered = true;
			pwqSymbolsOnArea(ga, osa);
			pwqPathsOnAreaNoLabels(ga, osa, null);
		}

		// check for success
		for (int i = 0; i < vpaths.size(); i++)
		{
			//assert ((OnePath)vpaths.elementAt(i)).ciHasrendered >= 2;
			OnePath op = (OnePath)vpaths.elementAt(i); 
			if (op.ciHasrendered < 2)
			{
				TN.emitWarning("ciHasrenderedbad on path:" + i); 
			}
		}

		// draw all the station names inactive
		if (!bHideStationNames)
		{
//			ga.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
//			ga.setColor(SketchLineStyle.linestylecolprint);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (opn.IsCentrelineNode())
				{
					if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
						ga.drawString(opn.pnstationlabel, SketchLineStyle.stationPropertyFontAttr, (float)opn.pn.getX() + SketchLineStyle.strokew * 2, (float)opn.pn.getY() - SketchLineStyle.strokew);
				}
			}
		}

		// labels
		// check any paths if they are now done
		for (int j = 0; j < vpaths.size(); j++)
		{
			OnePath op = (OnePath)vpaths.elementAt(j);
			if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
				op.paintLabel(ga, null);
		}
		binpaintWquality = false; 
	}
	
	/////////////////////////////////////////////
	public void ExportSVG(OneTunnel vgsymbols)
	{
		try
		{
			SVGWriter svgwriter = new SVGWriter();//Initilisation should set offsets and scale
			FileAbstraction fpaths = FileAbstraction.MakeWritableFileAbstraction("paths.svg");
			LineOutputStream losp = new LineOutputStream(fpaths);
     	 	svgwriter.SVGPaths(losp, vpaths);
			FileAbstraction fareas = FileAbstraction.MakeWritableFileAbstraction("areas.svg");
			LineOutputStream losa = new LineOutputStream(fareas);
     	 	svgwriter.SVGAreas(losa, vsareas);
			FileAbstraction fsymbols = FileAbstraction.MakeWritableFileAbstraction("symbols.svg");
			LineOutputStream loss = new LineOutputStream(fsymbols);
     	 	svgwriter.SVGSymbols(loss, vgsymbols);	
			FileAbstraction fview = FileAbstraction.MakeWritableFileAbstraction("view.svg");
			LineOutputStream losv = new LineOutputStream(fview);
     	 	svgwriter.SVGView(losv, vgsymbols, vpaths, vsareas, true, true);				
		}
		catch(Exception e)
		{
			TN.emitMessage("Writing Failed! ");
		}
   }


	/////////////////////////////////////////////
//	public void paintSVG(LineOutputStream los, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols)
//	{
//		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);
//		binpaintWquality = true; 
//
//		svg = new Svg(los);

		// set up the has rendered flags to begin with
//		for (int i = 0; i < vsareas.size(); i++)
//			((OneSArea)vsareas.elementAt(i)).bHasrendered = false;
//		for (int i = 0; i < sksya.vconncom.size(); i++)
//			((ConnectiveComponentAreas)sksya.vconncom.elementAt(i)).bHasrendered = false;
//		for (int i = 0; i < vnodes.size(); i++)
//			((OnePathNode)vnodes.elementAt(i)).pathcountch = 0;  // count these up as we draw them
//
//		//Initiate SVG file
//		svg.initialise();
//
//		// go through the paths and render those at the bottom here and aren't going to be got later
//		svg.WritePathsNonAreaNoLabels(vpaLineOutputStreamths, bHideCentreline);
//
//		// go through the areas and complete the paths as we tick them off.
//		svg.WriteAreas(vsareas);
//
//		// check for success
//		for (int i = 0; i < vpaths.size(); i++)
//			assert ((OnePath)vpaths.elementAt(i)).ciHasrendered >= 2;
//
//		// draw all the station names inactive
//		if (!bHideStationNames)
//		{
//			ga.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
//			ga.setColor(SketchLineStyle.linestylecolprint);
//			for (int i = 0; i < vnodes.size(); i++)
//			{
//				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
//				if (opn.IsCentrelineNode())
//				{
//					if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
//						ga.drawString(opn.pnstationlabel, (float)opn.pn.getX() + SketchLineStyle.strokew * 2, (float)opn.pn.getY() - SketchLineStyle.strokew);
//				}
//			}
//		}
//
//		// labels
//		// check any paths if they are now done
//		for (int j = 0; j < vpaths.size(); j++)
//		{
//			OnePath op = (OnePath)vpaths.elementAt(j);
//			if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
//				op.paintLabel(ga, null);
//		}
//		binpaintWquality = false;
//	}
	/////////////////////////////////////////////
	public void paintWbkgd(GraphicsAbstraction ga, boolean bHideCentreline, boolean bHideMarkers, int stationnamecond, OneTunnel vgsymbols, List<OnePath> tsvpathsviz)
	{
		// draw all the paths inactive.
		for (OnePath op : tsvpathsviz)
		{
			if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				boolean bIsSubsetted = (!bRestrictSubsetCode || op.bpathvisiblesubset); // we draw subsetted kinds as quality for now
				op.paintW(ga, bIsSubsetted, false);
			}
		}

		// draw all the nodes inactive
		if (!bHideMarkers)
		{
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
				{
					ga.drawShape(opn.Getpnell(), SketchLineStyle.pnlinestyleattr);
				}
			}
		}

		// draw all the station names inactive
		if (stationnamecond != 0)
		{
			//ga.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			//ga.setColor(SketchLineStyle.fontcol);
			//ga.setFont(SketchLineStyle.defaultfontlab);
			for (int i = 0; i < vnodes.size(); i++)
			{
				OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
				if (opn.IsCentrelineNode())
				{
					if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
					{
						String slab = (stationnamecond == 2 ? String.valueOf((int)(opn.zalt * 0.1)) : opn.pnstationlabel);
						ga.drawString(slab, SketchLineStyle.stationPropertyFontAttr, (float)opn.pn.getX() + SketchLineStyle.strokew * 2, (float)opn.pn.getY() - SketchLineStyle.strokew);
					}
				}
			}
		}

		// render all the symbols without clipping.
		for (OnePath op : tsvpathsviz)
		{
			if (!bRestrictSubsetCode || op.bpathvisiblesubset)
			{
				for (OneSSymbol oss : op.vpsymbols)
					oss.paintW(ga, false, false);
			}
		}

		// shade in the areas according to depth
		for (OneSArea osa : vsareas)
		{
			assert osa.subsetattr != null;
			if ((!bRestrictSubsetCode || osa.bareavisiblesubset) && (osa.subsetattr.areacolour != null))
				ga.fillArea(osa, osa.zaltcol == null ? osa.subsetattr.areacolour : osa.zaltcol);
		}
	}
};


