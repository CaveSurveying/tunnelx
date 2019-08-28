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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;

import java.io.IOException;
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
import java.awt.Image;

import javax.swing.JProgressBar; 

/////////////////////////////////////////////
class OneSketch
{
	// this must always be set
	FileAbstraction sketchfile = null;
	boolean bsketchfileloaded = false;
    
    String tunnelprojectloaded = ""; 
    String tunneluserloaded = ""; 
    String tunnelversionloaded = ""; 
    String tunneldateloaded = ""; 

	// arrays of sketch components.
	String sketchsymbolname; // not null if it's a symbol type
	boolean bSymbolType = false; // tells us which functions are allowed.

	// this could keep an update of deletes, inserts, and changes in properties (a flag on the path)
	boolean bsketchfilechanged = false;
    int isketchchangecount = 0;   // increments whenever there is a change.  should supercede bsketchfilechanged if used properly

	// main sketch.
	List<OnePathNode> vnodes;
	List<OnePath> vpaths;          // *** this is the only thing saved saved out into XML file

	OnePath opframebackgrounddrag = null;

	Vec3 sketchLocOffset; // sets it to zero by default
	
		// scaledown when we import background sketches into areas on the poster size (so posters don't have to be many kms wide in real space, and instead at least approx on right scale)
	double realposterpaperscale = 1.0;  // gets reset in ImportPaperM to 1000 if no included background images already in the file
	Rectangle2D rbounds = null;

	boolean bZonnodesUpdated = false;
	boolean bSAreasUpdated = false;
	boolean bSymbolLayoutUpdated = false;

	SortedSet<OneSArea> vsareas;

	Set<String> sallsubsets;

	// this gets the clockwise auto-area.
	OneSArea cliparea = null;
	SketchSymbolAreas sksya;  // this is a vector of ConnectiveComponents

	// range and restrictions in the display.
	boolean bRestrictSubsetCode = false;

	float zaltlo;
	float zalthi;

	SubsetAttrStyle sksascurrent = null;
	Map<String, String> submappingcurrent = new TreeMap<String, String>();  // cache this as well so we can tell when it changes (not well organized)

	boolean bWallwhiteoutlines = true;  // some flag that ought to be passed in
	static Color colframebackgroundshow = new Color(0.4F, 0.7F, 0.4F, 0.2F);
	static Color colframebackgroundimageshow = new Color(0.7F, 0.4F, 0.7F, 0.2F);


	// quick and dirty undo feature used by SketchGraphics.CommitPathChanges
    List<OnePath> pthstoremoveSaved = null; 
    List<OnePath> pthstoaddSaved = null; 

	/////////////////////////////////////////////
	OneSketch(FileAbstraction lsketchfile)
	{
		sketchfile = lsketchfile;
		bsketchfileloaded = false;
	}

	/////////////////////////////////////////////
	void UpdateSomething(int scchangetyp, boolean bforce)
	{
		if (((scchangetyp == SketchGraphics.SC_UPDATE_ZNODES) || (scchangetyp == SketchGraphics.SC_UPDATE_ALL) || (scchangetyp == SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS))&& (bforce || !bZonnodesUpdated))
		{
			ProximityDerivation pd = new ProximityDerivation(this);
			pd.SetZaltsFromCNodesByInverseSquareWeight(this); // passed in for the zaltlo/hi values
			bZonnodesUpdated = true;
		}
		if (((scchangetyp == SketchGraphics.SC_UPDATE_AREAS) || (scchangetyp == SketchGraphics.SC_UPDATE_ALL) || (scchangetyp == SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS))&& (bforce || !bSAreasUpdated))
		{
			MakeAutoAreas();  // once it is on always this will be unnecessary.
			assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);
			// used to be part of the Update symbols areas, but brought here
			// so we have a full set of paths associated to each area available
			// for use to pushing into subsets.
			MakeConnectiveComponentsT();
			for (OneSArea osa : vsareas)
				osa.SetSubsetAttrsA(true, sksascurrent);
			bSAreasUpdated = true;
		}
		if (((scchangetyp == SketchGraphics.SC_UPDATE_SYMBOLS) || (scchangetyp == SketchGraphics.SC_UPDATE_ALL)) && (bforce || !bSymbolLayoutUpdated))
		{
			MainBox.symbollayoutprocess.UpdateSymbolLayout(sksya.vconncommutual, null);
			bSymbolLayoutUpdated = true;
		}
	}

	/////////////////////////////////////////////
	void SetupSK()
	{
		assert !bsketchfileloaded;

		// main sketch.
		vnodes = new ArrayList<OnePathNode>();
		vpaths = new ArrayList<OnePath>();   // this is saved out into XML
		sketchLocOffset = new Vec3(0.0F, 0.0F, 0.0F); // sets it to zero by default
		vsareas = new TreeSet<OneSArea>();
		sallsubsets = new HashSet<String>();
		sksya = new SketchSymbolAreas();  // this is a vector of ConnectiveComponents

		bsketchfileloaded = true; 
	}
	
	
	/////////////////////////////////////////////
	void ApplySplineChange()
	{
		for (OnePath op : vpaths)
		{
			if (OnePath.bHideSplines && op.bSplined)
				op.Spline(false, false);
			else if (!OnePath.bHideSplines && !op.bSplined && op.bWantSplined)
				op.Spline(true, false);
		}
	}




	/////////////////////////////////////////////
	// the complexity comes when the opfront is also in the list and must be suppressed.
	OnePathNode SelNode(OnePathNode opfront, boolean bopfrontvalid, Graphics2D g2D, Rectangle selrect, OnePathNode selpathnodecycle, Set<OnePathNode> tsvnodesviz)
	{
		boolean bOvWrite = true;
		OnePathNode selnode = null;

		if (bopfrontvalid && g2D.hit(selrect, opfront.Getpnell(), false))
		{
			bOvWrite = (opfront == selpathnodecycle);
			selnode = opfront;
		}
		for (OnePathNode pathnode : tsvnodesviz)
		{
			if ((bOvWrite || (pathnode == selpathnodecycle)) && g2D.hit(selrect, pathnode.Getpnell(), false))
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
	OnePath SelPath(Graphics2D g2D, Rectangle selrect, OnePath prevselpath, Collection<OnePath> tsvpathsviz)
	{
		boolean bOvWrite = true;
		OnePath selpath = null;
		assert selrect != null;
		for (OnePath path : tsvpathsviz)
		{
			assert path.gp != null;
			if ((bOvWrite || (path == prevselpath)) &&
				(g2D.hit(selrect, path.gp, true) ||
				 ((path.plabedl != null) && (path.plabedl.drawlab != null) && (path.plabedl.rectdef != null) && (path.plabedl.labfontattr != null) && (path.plabedl.labfontattr.labelcolour != null) && g2D.hit(selrect, path.plabedl.rectdef, false))))
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
	OneSArea SelArea(Graphics2D g2D, Rectangle selrect, OneSArea prevselarea, SortedSet<OneSArea> tsvareasviz)
	{
		boolean bOvWrite = true;
		OneSArea selarea = null;
		int isel = -1;
		for (OneSArea oa : tsvareasviz)
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
	OnePath GetAxisPath()
	{
		for (OnePath op : vpaths)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				return op;
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
	class opcenscomp implements Comparator<OnePath>
	{
		public int compare(OnePath op1, OnePath op2)
		{
			float zalt1 = Math.max(op1.pnstart.zalt, op1.pnend.zalt);
			float zalt2 = Math.max(op2.pnstart.zalt, op2.pnend.zalt);
			return (int)Math.signum(zalt1 - zalt2);
		}
	}

	/////////////////////////////////////////////
	void AttachRemainingCentrelines()
	{
		List<OnePath> opcens = new ArrayList<OnePath>();
		for (OnePath op : vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.karight == null) && (op.pnstart != null) && (op.pnend != null))
				opcens.add(op);
		}

		// get the order right and zip it up with the areas
		Collections.sort(opcens, new opcenscomp());
		int iopcens = 0;
		OneSArea osaprev = null;
		for (OneSArea osa : vsareas)
		{
			osa.nconnpathremaining = osa.connpathrootscen.size(); 
			while (iopcens < opcens.size())
			{
				OnePath op = opcens.get(iopcens);
				float pzalt = Math.max(op.pnstart.zalt, op.pnend.zalt);
				if (pzalt > osa.zalt)
					break;
				if (osaprev != null)  // centrelines below the lowest area aren't associated with any of them, so get drawn first.
					osaprev.SetCentrelineThisArea(op, true);
				iopcens++;
			}
			osaprev = osa;
		}
		while (iopcens < opcens.size())  // final piece above the last area
		{
			OnePath op = opcens.get(iopcens);
			if (osaprev != null)
				osaprev.SetCentrelineThisArea(op, true);
			iopcens++;
		}
	}


	/////////////////////////////////////////////
	// fills in the opforeright values etc.
	// works selectively on a subset of vnodes.
	void MakeAutoAreas()
	{
		assert bsketchfileloaded;

		// set values to null.  esp the area links.
		for (OnePath op : vpaths)
		{
			op.karight = null;
			op.kaleft = null;
		}
		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);

		// build the main list which we keep in order for rendering
		vsareas.clear();
		cliparea = null;

		// now collate the areas.
		List<OneSArea> vsareastakeout = new ArrayList<OneSArea>();
		for (OnePath op : vpaths)
		{
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
		double zaaltlo = vsareas.first().zalt;
		double zaalthi = vsareas.last().zalt;
		assert zaaltlo <= zaalthi;

		double zaaltdiff = zaalthi - zaaltlo;
		if (zaaltdiff == 0.0)
			zaaltdiff = 1.0;
		for (OneSArea osa : vsareas)
			osa.icollam = (float)((osa.zalt - zaaltlo) / zaaltdiff);

		if (!bSymbolType)
			AttachRemainingCentrelines();
	}



	/////////////////////////////////////////////
	int TAddPath(OnePath path, Collection<OnePathNode> tsvnodesviz)
	{
		assert (path.apforeright == null) && (path.aptailleft == null);

		if (path.pnstart.pathcount == 0)
		{
			assert !vnodes.contains(path.pnstart);
			path.pnstart.SetNodeCloseBefore(vnodes, vnodes.size());  // makes the start shaped nodes
			vnodes.add(path.pnstart);
			if (tsvnodesviz != null)
				tsvnodesviz.add(path.pnstart);
		}
		path.pnstart.InsertOnNode(path, false);

		if (path.pnend.pathcount == 0)
		{
			assert !vnodes.contains(path.pnend);
			path.pnend.SetNodeCloseBefore(vnodes, vnodes.size());
			vnodes.add(path.pnend);
			if (tsvnodesviz != null)
				tsvnodesviz.add(path.pnend);
		}
		path.pnend.InsertOnNode(path, true);

        if (path.uuid == null)
            path.uuid = "p"+String.valueOf((int)(Math.random()*10000000)); 

		vpaths.add(path);
		assert path.pnstart.CheckPathCount();
		assert path.pnend.CheckPathCount();

		return vpaths.size() - 1;
	}


	/////////////////////////////////////////////
	static RefPathO trefpath = new RefPathO(); 
	boolean TRemovePath(OnePath op, SortedSet<OneSArea> tsvareasviz, Set<OnePathNode> tsvnodesviz)
	{
		// remove any areas automatically
		if (op.AreaBoundingType())
		{
			if (op.kaleft != null)
			{
				// can be falsified if there's been a change from a wall to a connective type
				//assert vsareas.contains(op.kaleft);
				vsareas.remove(op.kaleft);
				tsvareasviz.remove(op.kaleft); 
				op.kaleft.SetkapointersClear();
			}
			if (op.karight != null)
			{
				//assert vsareas.contains(op.karight);
				vsareas.remove(op.karight);
				tsvareasviz.remove(op.karight); 
				op.karight.SetkapointersClear();
			}
		}
		else if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.pthcca != null))
		{
			// assert op.pthcca.vconnpaths.contains(op); // may have already been removed
			op.pthcca.vconnpaths.remove(op); 
		}

		trefpath.op = op; 
		trefpath.bFore = false; 
		if (op.pnstart.RemoveOnNode(trefpath))
		{
			vnodes.remove(op.pnstart);
			tsvnodesviz.remove(op.pnstart); 
		}
		trefpath.bFore = true;
		if (op.pnend.RemoveOnNode(trefpath))
		{
			vnodes.remove(op.pnend);
			tsvnodesviz.remove(op.pnend); 
		}

		assert (op.pnstart.pathcount == 0) || op.pnstart.CheckPathCount();
		assert (op.pnend.pathcount == 0) || op.pnend.CheckPathCount();

		return vpaths.remove(op);
	}



	/////////////////////////////////////////////
	Rectangle2D getBounds(boolean bForce, boolean bOfSubset)
	{
		if (!bForce && (rbounds != null) && !bOfSubset)
			return rbounds;

		Rectangle2D.Float lrbounds = new Rectangle2D.Float();
		boolean bFirst = true;
		for (OnePath op : vpaths)
		{
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
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSKETCH, TNXML.sSPLINED, "0", TNXML.sSKETCH_LOCOFFSETX, String.valueOf(sketchLocOffset.x), TNXML.sSKETCH_LOCOFFSETY, String.valueOf(sketchLocOffset.y), TNXML.sSKETCH_LOCOFFSETZ, String.valueOf(sketchLocOffset.z), TNXML.sSKETCH_REALPAPERSCALE, String.valueOf(realposterpaperscale)));

		// write out the paths.
// IIII this is where we number the path nodes
		for (OnePath op : vpaths)
		{
			int ind0 = vnodes.indexOf(op.pnstart);
			int ind1 = vnodes.indexOf(op.pnend);
			if ((ind0 != -1) && (ind1 != -1))
				op.WriteXMLpath(los, ind0, ind1, 1);
			else
				TN.emitProgError("Path_node missing end " + vpaths.indexOf(op));
		}

		los.WriteLine(TNXML.xcomclose(0, TNXML.sSKETCH));
	}


	/////////////////////////////////////////////
	void SaveSketchLos(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.sHEADER);
		los.WriteLine("");

		los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML, TNXML.sTUNNELVERSION, TN.tunnelversion, TNXML.sTUNNELPROJECT, TN.tunnelproject, TNXML.sTUNNELUSER, TN.tunneluser, TNXML.sTUNNELDATE, TN.tunneldate()));
		WriteXML(los);
		los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML));
	}

	/////////////////////////////////////////////
	boolean SaveSketch()
	{
		if (!bsketchfileloaded)
			return TN.emitWarning("Cannot save a file that's not loaded"); 

        // save when it's a download from seagrass
        if (sketchfile.localurl != null)
        {
            FileAbstraction uploadedimage = NetConnection.uploadFile(sketchfile, "sketch", sketchfile.getSketchName() + ".xml", null, this); 
            if (uploadedimage == null)
                return TN.emitWarning("bum2"); 
 			// needs assert that it's the same
            //sketchgraphicspanel.tsketch.sketchfile = FileAbstraction.GetImageFile(fasketch, TN.setSuffix(uploadedimage.getPath(), TN.SUFF_XML));
    		bsketchfilechanged = false;
            return true; 
        }


		try
		{
		LineOutputStream los = new LineOutputStream(sketchfile);
    	SaveSketchLos(los); 
		los.close();
		bsketchfilechanged = false;
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
    		return false; 
		};
		return true; 
	}



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
	void pwqPathsNonAreaNoLabels(GraphicsAbstraction ga)
	{
		// check any paths if they are now done
		for (OnePath op : vpaths)
		{
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

			// the rest of the drawing of this path with quality
			if (!bRestrictSubsetCode || op.bpathvisiblesubset)
				op.paintWquality(ga);
		}
	}

	/////////////////////////////////////////////
	static RefPathO srefpathconn = new RefPathO();
	void paintWqualityjoiningpaths(GraphicsAbstraction ga, OnePathNode opn, boolean bShadowpaths)
	{
		srefpathconn.ccopy(opn.ropconn);
		do
		{
			OnePath op = srefpathconn.op;
			if (bShadowpaths)
			{
				if (!bRestrictSubsetCode || op.bpathvisiblesubset)
					pwqWallOutlinesPath(ga, op);
   			}
   			else if ((op.ciHasrendered != 3) && (op.pnstart.pathcountch == op.pnstart.pathcount) && (op.pnend.pathcountch == op.pnend.pathcount))
			{
				if (!bRestrictSubsetCode || op.bpathvisiblesubset)
					op.paintWquality(ga);
				op.ciHasrendered = 3;
			}
		}
		while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
	}

	/////////////////////////////////////////////
	void pwqPathsOnAreaNoLabels(GraphicsAbstraction ga, OneSArea osa, Rectangle2D abounds)
	{
		// got to do the associated centrelines first
		for (OnePath op : osa.connpathrootscen)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				assert (op.kaleft == osa) && (op.karight == osa);
				op.pnstart.pathcountch++;
				op.pnend.pathcountch++;
				op.paintWquality(ga);
				op.ciHasrendered = 3;

				// if any of these starts and ends trip over the count then we need to do their connections
				if (bWallwhiteoutlines)
				{
					// now embed drawing all the lines connecting to the two end-nodes
					if (op.pnstart.pathcountch == op.pnstart.pathcount)
						paintWqualityjoiningpaths(ga, op.pnstart, false);
					if (op.pnend.pathcountch == op.pnend.pathcount)
						paintWqualityjoiningpaths(ga, op.pnend, false);
				}
			}
		}

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
				if (!bRestrictSubsetCode || op.bpathvisiblesubset)
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
						mcca.paintWsymbols(ga);
						mcca.bHasrendered = true;
					}
				}
			}
		}
	}


	/////////////////////////////////////////////
	int SetSubsetAttrStyle(SubsetAttrStyle lsksascurrent, SketchFrameDef sketchframedef)
	{
		int res = SketchGraphics.SC_CHANGE_SAS_SYMBOLS_SAME; 

		if (sksascurrent != lsksascurrent)
			res = SketchGraphics.SC_CHANGE_SAS; 
		sksascurrent = lsksascurrent;

		// check if all the submapping has the same symbols
		// which is the real work because we have to find equivalence classes of subsets 
		// with the same symbol layout
		if ((sketchframedef != null) && !submappingcurrent.equals(sketchframedef.submapping))
			res = SketchGraphics.SC_CHANGE_SAS; 
		
		submappingcurrent.clear();
		if (sketchframedef != null)
		{
			submappingcurrent.putAll(sketchframedef.submapping);
			sksascurrent.AssignDefault(sketchframedef);  
		}
		
		// this sets the values on the paths
		for (OnePath op : vpaths)
			op.SetSubsetAttrs(sksascurrent, sketchframedef);

		// this goes again and gets the subsets into the areas from those on the paths
		for (OneSArea osa : vsareas)
			osa.SetSubsetAttrsA(true, sksascurrent);
	
		return res; 
	}

	/////////////////////////////////////////////
	public void paintWqualitySketch(GraphicsAbstraction ga, int irenderingquality, Map<String, SubsetAttrStyle> subsetattrstylesmap) 
	{
		assert OnePathNode.CheckAllPathCounts(vnodes, vpaths);
		
		// if subsetattrstylesmap == null then we assume we're drawing a framed sketch, 
		// so subset selection gets ignored and we don't recurse into subsubrames

		// set up the hasrendered flags to begin with
		for (OneSArea osa : vsareas)
			osa.bHasrendered = false;
		for (ConnectiveComponentAreas cca : sksya.vconncom)
			cca.bHasrendered = false;
		for (OnePathNode opn : vnodes)
			opn.pathcountch = 0;  // count these up as we draw them

		// go through the paths and render those at the bottom here and aren't going to be got later
		pwqPathsNonAreaNoLabels(ga);

		// go through the areas and complete the paths as we tick them off.
		for (OneSArea osa : vsareas)
		{
			// fill the area with a diffuse colour (only if it's a drawing kind)
			if ((subsetattrstylesmap == null) || !bRestrictSubsetCode || osa.bareavisiblesubset)  // setting just for previewing
			{
				// draw the wall type strokes related to this area (the fat white boundaries around the strokes)
				if (bWallwhiteoutlines)
					pwqWallOutlinesArea(ga, osa);

				if (osa.iareapressig == SketchLineStyle.ASE_KEEPAREA)
					ga.pwqFillArea(osa);

				// could have these sorted by group subset style, and remake it for these
				if ((osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME) && (osa.opsketchframedefs != null) && (!bRestrictSubsetCode || osa.bareavisiblesubset))
				{
					// multiple cases are rare, so convenient to sort them on the fly for dynamicness.
					if (osa.opsketchframedefs.size() >= 2)
					{
						Collections.sort(osa.opsketchframedefs, new Comparator<OnePath>() { public int compare(OnePath op1, OnePath op2)
						{
							if (op1.plabedl.sketchframedef.sfnodeconnzsetrelative != op2.plabedl.sketchframedef.sfnodeconnzsetrelative)
								return (op1.plabedl.sketchframedef.sfnodeconnzsetrelative - op2.plabedl.sketchframedef.sfnodeconnzsetrelative < 0.0F ? -1 : 1);
							return op1.plabedl.sketchframedef.distinctid - op2.plabedl.sketchframedef.distinctid;
						}}); 
					}
					for (OnePath op : osa.opsketchframedefs)
					{
						// the plotting of an included image
						SketchFrameDef sketchframedef = op.plabedl.sketchframedef; 
						if (sketchframedef.pframeimage != null)
						{
							if ((irenderingquality == 1) || (irenderingquality == 3))
							{
								ga.startFrame((!sketchframedef.sfstyle.equals("notrim") ? osa : null), sketchframedef.pframesketchtrans);
								Image img = sketchframedef.pframeimage.GetImage(true);
								ga.drawImage(img);
								ga.endFrame();
							}
							else
								ga.fillArea(osa, colframebackgroundimageshow); // signifies that something's there (deliberately overpaints sketches when there's more than one, so it's visible)
							continue;
						}

						// the plotting of the sketch
						if (sketchframedef.pframesketch == null)
							continue;
						if (subsetattrstylesmap == null) // avoids recursion
							continue;

						//assert sketchframedef.pframesketch.sksascurrent != null;
						SubsetAttrStyle sksas = null; 
						if (!sketchframedef.sfstyle.equals(""))
						{
							sksas = subsetattrstylesmap.get(sketchframedef.sfstyle);
							if (sksas == null)
								TN.emitWarning("sfstyle='"+sketchframedef.sfstyle+"' not found"); 
						}
						if (sksas == null)
						{
							TN.emitMessage("failed to get sfstyle "+sketchframedef.sfstyle+" so getting default"); 
							sksas = subsetattrstylesmap.get("default");
							if (sksas == null)
								TN.emitError("groupsubsetattr groupsubsetname='default' not found in fontcolours"); 
						}

// this supresses an assertion error that happens when trying to 
// do the witches in place hack of an sketch on the centreline

						//assert (sksas != null);  // it has to at least be set to something; if it has been loaded in the background
						if (sksas == null)
                        {
                            for (String s : subsetattrstylesmap.keySet())
                                TN.emitMessage("Bad subsetattrstylesmap missing default, key="+s); 
                        }

						if ((sksas != null) && ((sksas != sketchframedef.pframesketch.sksascurrent) || !sketchframedef.pframesketch.submappingcurrent.equals(sketchframedef.submapping) || ((irenderingquality == 3) && !sketchframedef.pframesketch.bSymbolLayoutUpdated)))
						{
							int iProper = (irenderingquality == 3 ? SketchGraphics.SC_UPDATE_ALL : SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS);
							TN.emitMessage("-- Resetting sketchstyle to " + sksas.stylename + " during rendering");
							int scchangetyp = sketchframedef.pframesketch.SetSubsetAttrStyle(sksas, sketchframedef);
							SketchGraphics.SketchChangedStatic(scchangetyp, sketchframedef.pframesketch, null);
							assert (sksas == sketchframedef.pframesketch.sksascurrent);
							assert sketchframedef.pframesketch.submappingcurrent.equals(sketchframedef.submapping);

							// if iproper == SketchGraphics.SC_UPDATE_ALL (not SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS)
							// then it could do it as through a window so that not the whole thing needs redoing.
							sketchframedef.pframesketch.UpdateSomething(iProper, false);
							SketchGraphics.SketchChangedStatic(iProper, sketchframedef.pframesketch, null);
						}

						assert this != sketchframedef.pframesketch; 
						sketchframedef.pframesketch.bRestrictSubsetCode = false; 

						ga.startFrame(osa, sketchframedef.pframesketchtrans);
						TN.emitMessage("Drawing the frame round: " + sketchframedef.sfsketch + " " + sketchframedef.sfelevrotdeg);
						if ((sketchframedef.sfelevrotdeg == 0.0) && !sketchframedef.sfelevvertplane.equals("extunfold"))
                            sketchframedef.pframesketch.paintWqualitySketch(ga, irenderingquality, null);
						else
                            sketchframedef.paintWelevSketch(ga, sksas, true);
						ga.endFrame();
					}
				}
			}
			assert !osa.bHasrendered;
			osa.bHasrendered = true;
			pwqSymbolsOnArea(ga, osa);
			pwqPathsOnAreaNoLabels(ga, osa, null);
		}

		// check for success
		for (OnePath op : vpaths)
		{
			//assert (op.ciHasrendered >= 2;
			if (op.ciHasrendered < 2)
				TN.emitWarning("ciHasrenderedbad on path:" + vpaths.indexOf(op));
		}

		// labels
		// check any paths if they are now done
		for (OnePath op : vpaths)
		{
			if ((op.linestyle != SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
			{
				if ((subsetattrstylesmap == null) || !bRestrictSubsetCode || op.bpathvisiblesubset)  
					op.paintLabel(ga, null);
			}
		}
	}

	/////////////////////////////////////////////
	public void paintWbkgd(GraphicsAbstraction ga, boolean bHideCentreline, boolean bHideMarkers, int stationnamecond, boolean bHideSymbols, Collection<OnePath> tsvpathsviz, Collection<OnePath> tsvpathsvizbound, Collection<OneSArea> tsvareasviz, Collection<OnePathNode> tsvnodesviz, boolean bzthinnedvisible)
	{
		// draw all the paths inactive.
		for (OnePath op : tsvpathsviz)
		{
			if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
			{
				boolean bIsSubsetted = (!bRestrictSubsetCode || op.bpathvisiblesubset); // we draw subsetted kinds as quality for now
                if (!bzthinnedvisible)
                    op.paintW(ga, bIsSubsetted, false);
                else if (op.gpzsliced != null)
                    op.paintWzthinned(ga, bIsSubsetted);
			}
		}

		// tsvpathsvizbound are the paths on the boundary between the areas that are selected by z and those which aren't, so do them in grey
        if ((tsvpathsvizbound != null) && !bzthinnedvisible)
        for (OnePath op : tsvpathsvizbound)
		{
			if (!bHideCentreline || (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
				op.paintW(ga, false, false);
		}
		
		// draw all the nodes inactive
		if (!bHideMarkers)
		{
			for (OnePathNode opn : tsvnodesviz)
			{
				if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
					ga.drawShape(opn.Getpnell(), SketchLineStyle.pnlinestyleattr);
			}
		}

		// draw all the station names inactive
		if (stationnamecond != 0)
		{
			//ga.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
			//ga.setColor(SketchLineStyle.fontcol);
			//ga.setFont(SketchLineStyle.defaultfontlab);
			for (OnePathNode opn : tsvnodesviz)
			{
				if (opn.IsCentrelineNode())
				{
					if (!bRestrictSubsetCode || (opn.icnodevisiblesubset != 0))
					{
						String slab; 
                        if (stationnamecond == 2)
                            slab = String.valueOf((int)(opn.zalt * 0.1)); 
                        else
                            slab = opn.ShortStationLabel();
						ga.drawString(slab, SketchLineStyle.stationPropertyFontAttr, (float)opn.pn.getX() + SketchLineStyle.strokew * 2, (float)opn.pn.getY() - SketchLineStyle.strokew);
					}
				}
			}
		}

		// render all the symbols without clipping.
		if (!bHideSymbols)
        {
            for (OnePath op : tsvpathsviz)
            {
                if (!bRestrictSubsetCode || op.bpathvisiblesubset)
                {
                    for (OneSSymbol oss : op.vpsymbols)
                        oss.paintW(ga, false, false);
                }
            }
        }

		// shade in the areas according to depth
		for (OneSArea osa : tsvareasviz)
		{
			assert osa.subsetattr != null;
			if ((!bRestrictSubsetCode || osa.bareavisiblesubset) && (osa.subsetattr.areacolour != null))
			{
				if (SketchLineStyle.bDepthColours)
					ga.fillArea(osa, SketchLineStyle.GetColourFromCollam(osa.icollam, true)); 
				else
					ga.fillArea(osa, osa.subsetattr.areacolour);
			}
		}
	}
}



