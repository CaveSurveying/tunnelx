////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2007  Julian Todd.
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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

// the tree thing in the left crashes when I click on some of the elements

// very often the main file list isn't showing up.  (some sort of rendering is too early)
// Make A4 should not need frame to be selected.  Just put in corner +100x100

// the XC Subset doesn't update the subsets in the List

// the mini-window has to do dragging and zooming, or taking itself to the selected places automatically 
// using some kind of max on a chosen path.

// make the text update when we hit % as well as /, or after a delay.  Maybe only text (not numbers) should not update
// mode to draw only the station name (use lastIndexOf("."))

// pitch undercut to put the invisible path below; (take out and put in the pitch boundary)

// remember to update the version of tunnel in TN.java each time

// frame-sketch do all should update all of current image first (so one click operation)

// multiple threads on the symbols layout for speed!

// 1) Change of type to connective/centreline to be implemented by deleting and adding the line.  
// (maybe this should apply to all line changes, including splined -- just like the reflect type)

// 2) Make area update no longer happen, because it's updated each time we add in a line

// 3) Then areas are constant, and we can build symbols as they happen in an on-going thread (when the update symbols button is down)

// 4) The znodes updates merely re-orders the lists of areas

// 5) major update would happen when we change the subset style and bring in different symbols

// 6) create new lists rather than clearing them as this will be thread safe

// make area updates make areas rebuild automatically -- not just taking them out.  Count them for verification against current version 



// make the Java3d Y for Z conversion
// update node z to 

// what about the z height things


// repaint the miniview when we get a new background
// import new downsketch doesn't update the subsets see FillAllMissingAttributes and maketreerootnode to debug this
// build the more sophisticated elevdev to handle multiple sequences.  get working on this.  
// Ability to move individual nodes (not the ends) later.  Prob do them separately.  

// other errors: 
//  when you change a line from centreline to wall type it doesn't update the left-right kaareas! Must be implemented by deleting and making new
//  null pointer problems if you select Sketch from frame view and nothing is selected

// move the SketchGraphics.elevpoint and other stuff into here
// XC values should just work by direct line of sight
// and have a linear over-lay along the XC.  
// closest direct centreline functions
// extract the height values in the centrelines
// zlev connective lines should be respected (does this come in with the update z?)

// check the loading and saving of these files
// check we can import sketches with elevations and XC sections without ruining them 
//   (must account for subset duplicates)

// max on sub view for a cross section or something, so we can go there quickly.  

// what do the setnodes in the info panel do?  
// do the map overlay upload.  


/////////////////////////////////////////////
/////////////////////////////////////////////
// this is a member of SelectedSubsetStructure and exists in partial (invalid) form, having to account for when 
// (a) more paths are added to the subset, or (b) when the selected subset changes
class ElevSet
{
	String selevsubset = null; 
	List<OnePath> connsequence = new ArrayList<OnePath>(); 
	List<OnePath> elevcenpaths = new ArrayList<OnePath>(); // will be one path when true.  should have the number of line segments equal to the size of connsequence
	List<OnePath> elevpaths = new ArrayList<OnePath>(); 
	boolean bIsElevStruct = false; 
	boolean bXC; 

	// for drawing the cursor mark
	List<SSymbScratchPath> sscratchpaths = null; // will be parallel to connsequence
	double totalpathlength; 

	/////////////////////////////////////////////
	ElevSet()
	{;}

	ElevSet(String lselevsubset)
	{
		selevsubset = lselevsubset; 
		bXC = selevsubset.substring(0, 2).equals("XC"); 
	}

	/////////////////////////////////////////////
	void Clear()
	{
		connsequence.clear(); 
		elevcenpaths.clear(); 
		elevpaths.clear(); 
		bIsElevStruct = false; 
	}

	/////////////////////////////////////////////
	void AddRemovePath(OnePath op, boolean bAdd)
	{
		List<OnePath> lcat; 
		if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && 
			(op.plabedl.barea_pres_signal == SketchLineStyle.ASE_ELEVATIONPATH)) 
			lcat = connsequence; 
		else if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			lcat = elevcenpaths; 
		else
			lcat = elevpaths; 
		
		if (bAdd)
		{
			if (!lcat.contains(op))
				lcat.add(op); 
		}
		else
			lcat.remove(op); 
	}

	/////////////////////////////////////////////
	boolean SetIsElevStruct(boolean bsetpathlengths)
	{
		bIsElevStruct = ((selevsubset != null) && ReorderElevPaths(connsequence) && (elevcenpaths.size() == 1)); 
		if (!bIsElevStruct)
			return false; 

		if (bsetpathlengths)
			SetupPathLength(); 
		bXC = selevsubset.substring(0, 2).equals("XC"); 
		return true; 
	}

	/////////////////////////////////////////////
	void SetupPathLength()
	{
		if (sscratchpaths == null)
			sscratchpaths = new ArrayList<SSymbScratchPath>(); 
		totalpathlength = 0.0; 
		for (int i = 0; i < connsequence.size(); i++)
		{
			if (i >= sscratchpaths.size())
				sscratchpaths.add(new SSymbScratchPath()); 
			sscratchpaths.get(i).SetUpPathLength(connsequence.get(i)); 
			totalpathlength += sscratchpaths.get(i).GetCumuPathLength(); 
		}
	}

	/////////////////////////////////////////////
	static Point2D evalpt = new Point2D.Float(); 
	static Point2D evalpttan = new Point2D.Float(); 
// this wants to set a triangle/arrow
	void AlongCursorMark(GeneralPath elevarrow, Ellipse2D elevpoint, Point2D moupt)
	{
		assert bIsElevStruct; 
		OnePath cop = elevcenpaths.get(0); 
		double x0 = elevcenpaths.get(0).pnstart.pn.getX(); 
		double x1 = elevcenpaths.get(elevcenpaths.size() - 1).pnend.pn.getX(); 
		Point2D c0 = connsequence.get(0).pnstart.pn; 
		Point2D c1 = connsequence.get(connsequence.size() - 1).pnend.pn; 
		double vcenX = x1 - x0; 
		double lam = (vcenX != 0.0 ? (moupt.getX() - x0) / vcenX : 0.5); 
		lam = (lam >= 0.0 ? (lam <= 1.0 ? lam : 1.0) : 0.0); 
		Point2D levalpt =  null; 
		if (bXC)
		{
			evalpttan.setLocation(c1.getX() - c0.getX(), c1.getY() - c0.getY()); 
			evalpt.setLocation(c0.getX() * (1.0 - lam) + c1.getX() * lam, c0.getY() * (1.0 - lam) + c1.getY() * lam); 
			levalpt = evalpt; 
		}
		else
		{
			double r = totalpathlength * lam; 
			for (int i = 0; i < connsequence.size(); i++)
			{
				OnePath opc = connsequence.get(i); 
				if ((r < sscratchpaths.get(i).GetCumuPathLength()) || (i == connsequence.size() - 1))
				{
					double t = sscratchpaths.get(i).ConvertAbstoNodePathLength(r, opc);
					opc.Eval(evalpt, evalpttan, t);
					levalpt = evalpt; 
					break; 
				}
				r -= sscratchpaths.get(i).GetCumuPathLength(); 
			}
		}

		double lstrokew = SketchLineStyle.strokew * 1.5; 
		elevpoint.setFrame(levalpt.getX() - 2 * lstrokew, levalpt.getY() - 2 * lstrokew, 4 * lstrokew, 4 * lstrokew);

		double tanlen = Math.sqrt(evalpttan.getX() * evalpttan.getX() + evalpttan.getY() * evalpttan.getY()); // no document to refer to to find the leng function
		elevarrow.reset();
		double xp = levalpt.getX(); 
		double yp = levalpt.getY(); 
		double xv = evalpttan.getX() / tanlen * lstrokew * 4; 
		double yv = evalpttan.getY() / tanlen * lstrokew * 4; 
		elevarrow.moveTo((float)xp, (float)yp); 
		elevarrow.lineTo((float)(xp - xv - yv), (float)(yp - yv + xv));
		elevarrow.lineTo((float)(xp - xv + yv), (float)(yp - yv - xv));
		elevarrow.lineTo((float)xp, (float)yp); 

		// for now add in the XC cut line
		if (bXC)
		{
			elevarrow.moveTo((float)c0.getX(), (float)c0.getY()); 
			elevarrow.lineTo((float)c1.getX(), (float)c1.getY()); 
		}
	}


	/////////////////////////////////////////////
	OnePath MakeElevAxisPath(String sselevsubset, boolean lbXC, OnePathNode opcfore, OnePathNode opcback)
	{
		selevsubset = sselevsubset; 
		bXC = lbXC; 
		assert bXC == selevsubset.substring(0, 2).equals("XC"); 

		SetupPathLength(); 

		double opcpathleng = 0.0; 
		double xmax = connsequence.get(0).pnstart.pn.getX(); 
		for (OnePath opc : connsequence)
		{
			opc.plabedl.barea_pres_signal = SketchLineStyle.ASE_ELEVATIONPATH; // just now need to find where it is in the list in the combo-box
			opc.plabedl.iarea_pres_signal = SketchLineStyle.iareasigelev; 
			if (SketchLineStyle.iareasigelev == -1)
				TN.emitError("Missing area_signal_def elevationpath in fontcolours");
			assert opc.plabedl.barea_pres_signal == SketchLineStyle.areasigeffect[opc.plabedl.iarea_pres_signal]; 
			xmax = Math.max(xmax, opc.pnend.pn.getX()); 
		}

		double slx = connsequence.get(connsequence.size() - 1).pnend.pn.getX() - connsequence.get(0).pnstart.pn.getX(); 
		double sly = connsequence.get(connsequence.size() - 1).pnend.pn.getY() - connsequence.get(0).pnstart.pn.getY(); 
		double dstraightlensq = slx * slx + sly * sly; 
		double dstraightlen = Math.sqrt(dstraightlensq); 

		// now we're ready to go through with it
		
		//sketchdisplay.sketchlinestyle.SetConnectiveParametersIntoBoxes(opc);

		// make the centreline that will be added
		// this has nodes along it.  
		// the nodes and endpoints could be displaced in y according to the z-values of the ends and mid-nodes 
		// to enable complex contours of the sides of vertical pitches which may have multiple horizontal cross-sections all the way down.

		OnePath opelevaxis; 
        OnePathNode cpnstart; 
        OnePathNode cpnend; 
		if (bXC)
		{
			// find the length
			double xright = xmax + totalpathlength / 2; 
			double ylast = connsequence.get(connsequence.size() - 1).pnend.pn.getY(); 
			//double opcpathlengH = (opc.pnstart.pn.getX() < opc.pnend.pn.getX() ? opcpathleng : -opcpathleng) / 2;  

			cpnstart = new OnePathNode((float)(xright), (float)ylast, 0.0F); 
			cpnend = new OnePathNode((float)(xright + dstraightlen), (float)ylast, 0.0F); 
			//double opcpathlengH = (opc.pnstart.pn.getX() < opc.pnend.pn.getX() ? opcpathleng : -opcpathleng) / 2;  
		}

        // elevation case.  try to connect to a node that's already there
		else
		{
			//OnePathNode opcfore = connsequence.get(0).pnstart.ConnectingCentrelineNode(); 
			//OnePathNode opcback = connsequence.get(connsequence.size() - 1).pnend.ConnectingCentrelineNode(); 

            double xright = 50.0; 
        	cpnstart = new OnePathNode((float)(xright + 0.0), -(float)opcfore.zalt, 0.0F); 
			cpnend = new OnePathNode((float)(xright + totalpathlength), -(float)opcback.zalt, 0.0F); 
			//return; // not done yet
        }
			
		// now make the axis (inserting nodes along the way that can be flagged)
		opelevaxis = new OnePath(cpnstart); 
		
		double lopcpathleng = sscratchpaths.get(0).GetCumuPathLength(); 
		double z0 = (opcfore != null ? opcfore.zalt : connsequence.get(0).pnstart.zalt); 
		for (int i = 1; i < connsequence.size(); i++)
		{
			OnePath opc = connsequence.get(i); 

			OnePathNode opcc = opc.pnstart.ConnectingCentrelineNode(); // will have to account for zdisp too
			if (opcc == null)
				opcc = opc.pnstart; 

			double lam; 
			if (bXC)
			{
				double dslx = opc.pnstart.pn.getX() - connsequence.get(0).pnstart.pn.getX(); 
				double dsly = opc.pnstart.pn.getY() - connsequence.get(0).pnstart.pn.getY(); 
				double dd = dslx * slx + dsly * sly; 
				lam = dd / dstraightlensq; 
			}
			else
			{
				lam = lopcpathleng / totalpathlength; 
			}
			double lx = cpnstart.pn.getX() * (1.0 - lam) + cpnend.pn.getX() * lam; 
			double ly = cpnstart.pn.getY() * (1.0 - lam) + cpnend.pn.getY() * lam; 

			// resetting ly so we get jagged path which may corresp to horizontally displaced centreline
			// requires update znodes to be done first
			ly = cpnstart.pn.getY() - (opcc.zalt - z0); // remember the ys go down

			opelevaxis.LineTo((float)lx, (float)ly); 

			lopcpathleng += sscratchpaths.get(i).GetCumuPathLength(); 
		}

		opelevaxis.EndPath(cpnend); 
		opelevaxis.linestyle = SketchLineStyle.SLS_CENTRELINE; 
		opelevaxis.plabedl = new PathLabelDecode();
		opelevaxis.plabedl.centrelineelev = selevsubset; 

		return opelevaxis; 
	}

	/////////////////////////////////////////////
	static void ExchangePaths(List<OnePath> oplist, int i, int j)
	{
		if (i == j)
			return; 
		OnePath op = oplist.get(i); 
		oplist.set(i, oplist.get(j)); 
		oplist.set(j, op); 
	}

	/////////////////////////////////////////////
	static boolean ReorderElevPaths(List<OnePath> lopelevarr)
	{
		int nelevs = lopelevarr.size(); 
		// first find the front node
		int ifront = -1; 
		for (int i = 0; i < nelevs; i++)
		{
			boolean bfrontunique = true; 
			for (int j = 0; j < nelevs; j++)
			{
				if (lopelevarr.get(i).pnstart == lopelevarr.get(j).pnend)
					bfrontunique = false; 
			}
			if (bfrontunique)
			{
				if (ifront != -1)
					return false; 
				ifront = i; 
			}
		}
		if (ifront == -1)
			return false; 

		ExchangePaths(lopelevarr, 0, ifront); 

		// now find the joining pieces
		for (int i = 1; i < nelevs; i++)
		{
			int Lj = -1; 
			for (int j = i; j < nelevs; j++)
			{
				if (lopelevarr.get(j).pnstart == lopelevarr.get(i - 1).pnend)
				{
					if (Lj != -1)
						return false; 
					Lj = j; 
				}
			}
			if (Lj == -1)
				return false; 
			ExchangePaths(lopelevarr, i, Lj); 
		}

for (int i = 0; i < nelevs; i++)
	System.out.println(" elevccccc " + lopelevarr.get(i).pnstart + "  " + lopelevarr.get(i).pnend); 
		return true; 
	}


	/////////////////////////////////////////////
	// this will be more wide-ranging or test with 
	static List<OnePath> IsElevationNode(OnePathNode wopn)
	{
		boolean belevnode = false; 
		RefPathO srefpathconn = new RefPathO();
		srefpathconn.ccopy(wopn.ropconn);
		do
		{
			if (srefpathconn.op.IsElevationCentreline())
				belevnode = true; 
		}
		while (!srefpathconn.AdvanceRoundToNode(wopn.ropconn));

		//boolean bres = (bIsElevStruct && ((wopn == opelevarr.get(iopelevarrCEN).pnstart) || (wopn == opelevarr.get(iopelevarrCEN).pnend))); 
		//System.out.println("Elevnodedetector " + belevnode  + " " +  bres); 
		if (!belevnode)
			return null; 

        // this looks for the entire connected component associated with this elevation node
		List<OnePath> elevcenconn = new ArrayList<OnePath>(); 
		List<OnePathNode> vpnstack = new ArrayList<OnePathNode>(); 
		List<OnePathNode> vpnused = new ArrayList<OnePathNode>(); 
		vpnstack.add(wopn); 
		vpnused.add(wopn); 
		while (!vpnstack.isEmpty())
		{
			OnePathNode opn = vpnstack.remove(vpnstack.size() - 1); 
			srefpathconn.ccopy(opn.ropconn);
			do
			{
				if (srefpathconn.op.IsElevationCentreline())
				{
					if (!elevcenconn.contains(srefpathconn.op)) 
						elevcenconn.add(srefpathconn.op); 
					OnePathNode oopn = (srefpathconn.op.pnstart == opn ? srefpathconn.op.pnend : srefpathconn.op.pnstart); 
					if (!vpnused.contains(oopn))
					{
						vpnstack.add(oopn); 
						vpnused.add(oopn); 
					}
				}
			}
			while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
		}
		return elevcenconn; 
	}
};


/////////////////////////////////////////////
class ElevWarp
{
// has to account for liopelevarrCEN not always being 1

// make this run
// reorder for each of these lists
// make sure it covers
// make a node mapping for every path that connects to it that's in the movable subsets
// move the elevations transitively.
	Map<String, ElevSet> elevconnmap = new HashMap<String, ElevSet>(); 

	Map<String, WarpPiece> elevwarppiecemap = new HashMap<String, WarpPiece>(); 

	List<OnePathNode> warpfromcnodes = new ArrayList<OnePathNode>(); 
	List<OnePathNode> warptocnodes = new ArrayList<OnePathNode>(); 

	// collect all the components which make up the elevation
	boolean MakeElevWarp(List<OnePath> elevcenconn, List<OnePath> vpaths)
	{
		// not as effectively coded as poss, but I'm lacking the docs for the classes
		Set<String> clsubsets = new HashSet<String>(); 

		// find the subsets represented by the centrelines that connect to the chosen node
		for (OnePath opc : elevcenconn)
		{
			assert opc.IsElevationCentreline(); 
			for (String ssubset : opc.vssubsets)	
				clsubsets.add(ssubset); 
		}

        // allocate an elevation subset for each one
		for (String ssubset : clsubsets)
			elevconnmap.put(ssubset, new ElevSet(ssubset)); 

		// allocate the paths into the subsets
		for (OnePath op : vpaths)
		{
			for (String ssubset : op.vssubsets)
			{
				if (clsubsets.contains(ssubset))
					elevconnmap.get(ssubset).AddRemovePath(op, true); 
			}
		}

		// go through and lose the connective lines parts from these sets
		for (String ssubset : clsubsets)
		{
			if (!elevconnmap.get(ssubset).SetIsElevStruct(false))
				elevconnmap.remove(ssubset); 
		}
System.out.println("ggggggggg  " + elevconnmap.size() + "  " + clsubsets.size() + "  " + elevconnmap.keySet().size()); 

        boolean bres = true; 
		for (OnePath opc : elevcenconn)
        {
    		boolean bcpathaccounted = false; 
            for (ElevSet elevset : elevconnmap.values())
            {
                if (elevset.elevcenpaths.contains(opc))
                    bcpathaccounted = true; 
            }
            if (!bcpathaccounted)
                bres = false; 
        }
        return bres; 
	}

	/////////////////////////////////////////////
	// makes warpfromcnodes.  another function could make a more global change
	void MakeWarpPathPieceMap(OnePath warppath)
	{
		for (String ssubset : elevconnmap.keySet())
		{
			ElevSet elevset = elevconnmap.get(ssubset); 
			assert elevset.bIsElevStruct; 

			OnePath opc = elevset.elevcenpaths.get(0); 
			assert opc.IsElevationCentreline(); 

			// only include the nodes one a line that will be moved
			if (!((opc.pnstart == warppath.pnstart) || (opc.pnend == warppath.pnstart)))
				continue; 

			OnePathNode npnstart = (opc.pnstart == warppath.pnstart ? warppath.pnend : opc.pnstart); 
			OnePathNode npnend = (opc.pnend == warppath.pnstart ? warppath.pnend : opc.pnend); 

			elevwarppiecemap.put(ssubset, new WarpPiece(opc.pnstart, opc.pnend, npnstart, npnend)); 
		}

		assert warpfromcnodes.size() == warptocnodes.size(); 
		//elevwarppiecemap
	}

			
	/////////////////////////////////////////////
	Point2D ptspare = new Point2D.Float();
	Set<String> ssubsetsspare = new HashSet<String>(); 
	RefPathO srefpathconnspare = new RefPathO();
	Set<OnePath> pthstowarp = new HashSet<OnePath>(); 

	/////////////////////////////////////////////
	OnePathNode WarpElevationNode(OnePathNode opn)
	{
		int ind = warpfromcnodes.indexOf(opn); 
		if (ind != -1)
			return warptocnodes.get(ind); 

		// find the subsets this node could be in
		srefpathconnspare.ccopy(opn.ropconn);
		do
		{
			ssubsetsspare.addAll(srefpathconnspare.op.vssubsets); 
		}
		while (!srefpathconnspare.AdvanceRoundToNode(opn.ropconn));

		// find the average displacement for the sums
		double xsum = 0.0; 
		double ysum = 0.0; 
		int nsum = 0; 
		for (String ssubset : ssubsetsspare)
		{
			WarpPiece ewp = elevwarppiecemap.get(ssubset); 
			if (ewp == null)
				continue; 
			ewp.WarpPoint(ptspare, opn.pn.getX(), opn.pn.getY()); 
			xsum += ptspare.getX(); 
			ysum += ptspare.getY(); 
			nsum++; 
		}
		ssubsetsspare.clear(); 
	
		OnePathNode opnto; 
		if (nsum != 0)
			opnto = new OnePathNode((float)(xsum / nsum), (float)(ysum / nsum), opn.zalt); 
		else
			opnto = opn; 
		warpfromcnodes.add(opn); 
		warptocnodes.add(opnto); 
System.out.println("  nnnn  " + nsum); 
		return opnto; 
	}

	/////////////////////////////////////////////
	void MakeWarpPathNodeslists()
	{
		// central warping of pieces
		for (WarpPiece ewp : elevwarppiecemap.values())
		{
			if (!warpfromcnodes.contains(ewp.pnstart))
			{
				warpfromcnodes.add(ewp.pnstart); 
				warptocnodes.add(ewp.npnstart); 
			}
			if (!warpfromcnodes.contains(ewp.pnend))
			{
				warpfromcnodes.add(ewp.pnend); 
				warptocnodes.add(ewp.npnend); 
			}
		}
	}


	/////////////////////////////////////////////
	void WarpAllPaths(List<OnePath> pthstoremove, List<OnePath> pthstoadd, OnePath warppath) 
	{
		for (String ssubset : elevconnmap.keySet())
		{
			ElevSet elevset = elevconnmap.get(ssubset); 
			pthstowarp.addAll(elevset.elevcenpaths);  // one entry
			pthstowarp.addAll(elevset.elevpaths); 
		}
		pthstowarp.remove(warppath); 
		for (OnePath op : pthstowarp)
		{
			OnePathNode nopnstart = WarpElevationNode(op.pnstart); 
			OnePathNode nopnend = WarpElevationNode(op.pnend); 
			if ((nopnstart == op.pnstart) && (nopnend == op.pnend))
				continue; 
			float[] pco = op.GetCoords();
			OnePath nop = new OnePath(nopnstart); 
			WarpPiece lewp = new WarpPiece(op.pnstart, op.pnend, nopnstart, nopnend); 
			for (int i = 1; i < op.nlines; i++)
			{
				lewp.WarpPoint(ptspare, pco[i * 2], pco[i * 2 + 1]); 
				nop.LineTo((float)ptspare.getX(), (float)ptspare.getY());
			}
			nop.EndPath(nopnend);
			nop.CopyPathAttributes(op);

			pthstoremove.add(op); 
			pthstoadd.add(nop); 
		}
	}


};

