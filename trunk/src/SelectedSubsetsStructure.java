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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Enumeration;

import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;


// this controls a set of subsets and the paths which are contained therein
// also sets up the elevation when this selection fits the template.  

/////////////////////////////////////////////
class SelectedSubsetStructure
{
	SketchDisplay sketchdisplay; 

	Set<String> vsselectedsubsetsP = new HashSet<String>();  // actual selected
	Set<String> vsselectedsubsets = new HashSet<String>();   // transitive selected
	boolean binversubset = false; 
	boolean btransitivesubset = false; 

	//String selevsubset = null; // is the selected elevation subset

	// the structure of the elevation subset
	ElevSet elevset = new ElevSet(); 
//float vcenX; 
//SSymbScratchPath Tsscratchpath = new SSymbScratchPath(); // we'll need one for each connective path we put in line here




	/////////////////////////////////////////////
    // (now sets bIsElevStruct itself)
/*	boolean ReorderAndEstablishXCstruct() // tends to be called after a batch of puts
	{
		elevset.SetIsElevStruct(); 
		if (!elevset.bIsElevStruct) 
			return false; 
System.out.println("WeHAVEelevSubset"); 
//		vcenX = (float)(elevset.elevcenpaths.get(0).pnend.pn.getX() - elevset.elevcenpaths.get(0).pnstart.pn.getX()); 
//		Tsscratchpath.SetUpPathLength(elevset.connsequence.get(0)); // for now!
		return true; 
	}
*/
	
	/////////////////////////////////////////////
/*	double QCGetPathLength(OnePath op)
	{
		Tsscratchpath.SetUpPathLength(op); 
		return Tsscratchpath.GetCumuPathLength(); 
	}
*/	

	

	/////////////////////////////////////////////
/*	void FuseNodesElevation(OnePathNode wpnstart, OnePathNode wpnend)
	{
		// this will fuse a whole bunch of pieces
		assert !wpnstart.IsCentrelineNode(); 
		ElevWarpPiece ewp = new ElevWarpPiece(wpnstart, wpnend, opelevarr.get(iopelevarrCEN), ElevWarpPiece.WARP_ZWARP); 

		List<OnePath> nopelevarr = ewp.WarpElevationBatch(opelevarr); 
		assert nopelevarr.size() == opelevarr.size(); 

		for (int i = 0; i < nopelevarr.size(); i++)
		{
			RemovePath(opelevarr.get(i));
			AddPath(nopelevarr.get(i));
			if ((nopelevarr.get(i).linestyle == SketchLineStyle.SLS_CENTRELINE) && (nopelevarr.get(i).plabedl != null))  // prob gratuitous
				nopelevarr.get(i).UpdateStationLabelsFromCentreline();
		}
		assert wpnstart.pathcount == 0; // should have been removed
	}
*/

	/////////////////////////////////////////////
	SelectedSubsetStructure(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay; 
	}
	
	/////////////////////////////////////////////
	String GetFirstSubset()
	{
		if (vsselectedsubsetsP.isEmpty())
			return null; 
		return vsselectedsubsetsP.iterator().next(); 
	}


	/////////////////////////////////////////////
	boolean SetSubsetVisibleCodeStrings(OnePath op, boolean bAdd)
	{
		boolean bpathinsubset = false;
		for (String ssubset : op.vssubsets)
		{
			if (vsselectedsubsets.contains(ssubset))
				bpathinsubset = true;
		}

		if (bpathinsubset != binversubset) // counts the inverse
		{
			op.bpathvisiblesubset = true;
			op.pnstart.icnodevisiblesubset++;
			op.pnend.icnodevisiblesubset++;
			
			assert bAdd != binversubset; 
			if (elevset.selevsubset != null)
				elevset.AddRemovePath(op, true); 
			return true;
		}
		op.bpathvisiblesubset = false;
		if (!bAdd && (elevset.selevsubset != null))  // not sure about the bAdd
			elevset.AddRemovePath(op, false); 
		return false;
	}

	/////////////////////////////////////////////
	void SetSubsetVisibleCodeStringsT(String lselevsubset, OneSketch sketch)
	{
		elevset.Clear(); 
		elevset.selevsubset = lselevsubset; 
		if ((elevset.selevsubset != null) && (elevset.selevsubset.length() > 2))
			elevset.bXC = elevset.selevsubset.substring(0, 2).equals("XC");   // yes this should be more organized into a function

		// set node codes down to be set up by the paths
		for (OnePathNode opn : sketch.vnodes)
			opn.icnodevisiblesubset = 0;

		// set paths according to subset code
		sketch.bRestrictSubsetCode = !vsselectedsubsets.isEmpty();
		int nsubsetpaths = 0;
		for (OnePath op : sketch.vpaths)
		{
			if (SetSubsetVisibleCodeStrings(op, true))
				nsubsetpaths++;
		}

		elevset.SetIsElevStruct(true); 

		// now scan through the areas and set those in range and their components to visible
		int nsubsetareas = 0;
		for (OneSArea osa : sketch.vsareas)
			nsubsetareas += osa.SetSubsetAttrsA(false, null);

		// set subset codes on the symbol areas
		// over-compensate the area; the symbols will spill out.
		int nccaspills = 0;
		for (ConnectiveComponentAreas cca : sketch.sksya.vconncom)
		{
			cca.bccavisiblesubset = false;
			for (OneSArea osa : cca.vconnareas)
			{
				boolean bareavisiblesubset = osa.bareavisiblesubset;
				if (bareavisiblesubset)
					cca.bccavisiblesubset = true;
				else if (cca.bccavisiblesubset)
					nccaspills++;
			}
		}
		if (nccaspills != 0)
			TN.emitMessage("There are " + nccaspills + " symbol area spills beyond subset ");

		assert sketchdisplay.sketchgraphicspanel.tsketch == sketch; // satisfied by all four calls
		//TN.emitMessage("Subset paths: " + nsubsetpaths + "  areas: " + nsubsetareas);
	}

	/////////////////////////////////////////////
	void UpdateSingleSubsetSelection(String lsubset)
	{
		vsselectedsubsetsP.clear(); 
		vsselectedsubsets.clear(); 
		vsselectedsubsetsP.add(lsubset); 
		vsselectedsubsets.add(lsubset); 
		SetSubsetVisibleCodeStringsT(lsubset, sketchdisplay.sketchgraphicspanel.tsketch);
	}
	
	/////////////////////////////////////////////
	void UpdateTreeSubsetSelection(JTree pansksubsetstree)
	{
		binversubset = sketchdisplay.miInverseSubset.isSelected(); 
		btransitivesubset = sketchdisplay.miTransitiveSubset.isSelected(); 

		String selevsubset = null; 
		boolean bnotelevsubset = false; 
		
		TreePath[] tps = pansksubsetstree.getSelectionPaths();

		vsselectedsubsetsP.clear(); 
		vsselectedsubsets.clear(); 

		for (int i = (tps != null ? tps.length - 1 : -1); i >= 0; i--)
		{
			DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tps[i].getLastPathComponent();
                
            boolean bissurvexstruct = false; 
            for (DefaultMutableTreeNode ltn = tn; ltn != null; ltn = (DefaultMutableTreeNode)ltn.getParent())
            {
                if (ltn == sketchdisplay.subsetpanel.sascurrent.dmsurvexstruct)
                    bissurvexstruct = true; 
            }
			if (tn.getUserObject() instanceof String)
			{
				// special case which just handles the string-types in the unattributed (relative to fontcolours) subsets list
				String ssubset = (String)tn.getUserObject(); 
				vsselectedsubsetsP.add(ssubset); 
				if (tn == sketchdisplay.subsetpanel.sascurrent.dmunattributess) 
				{
					if (btransitivesubset)
						vsselectedsubsets.addAll(sketchdisplay.subsetpanel.sascurrent.unattributedss); 
					bnotelevsubset = true; // the set of subsets is not an elevation subset
				}	
				else if (tn == sketchdisplay.subsetpanel.sascurrent.dmxsectionss) 
				{
					if (btransitivesubset)
						vsselectedsubsets.addAll(sketchdisplay.subsetpanel.sascurrent.xsectionss); 
					bnotelevsubset = true; 
				}
				else 
				{
					vsselectedsubsets.add(ssubset); 
					bnotelevsubset = (selevsubset != null); // only one of them
					selevsubset = ssubset; 
				}
			}
			else if (tn.getUserObject() instanceof OneLeg)
			{
				assert bissurvexstruct; 
				OneLeg ol = (OneLeg)tn.getUserObject(); 
                if (btransitivesubset)
				{
					Enumeration<DefaultMutableTreeNode> tnenum = tn.depthFirstEnumeration(); 
					while (tnenum.hasMoreElements())
						vsselectedsubsets.add(((OneLeg)tnenum.nextElement().getUserObject()).stto);  // the actual subset name, not the thing that appears in the treeview
				}
				else
					vsselectedsubsets.add(ol.stto); 
			}
			
			else
			{
				SubsetAttr sa = (SubsetAttr)tn.getUserObject(); 
				vsselectedsubsetsP.add(sa.subsetname); 
				if (btransitivesubset) 
				{
					List<SubsetAttr> vssa = new ArrayList<SubsetAttr>(); 
					SelectedSubsetStructure.VRecurseSubsetsdown(vssa, (SubsetAttr)tn.getUserObject()); 
					for (SubsetAttr dsa : vssa)
						vsselectedsubsets.add(dsa.subsetname); 
				}
				else 
					vsselectedsubsets.add(sa.subsetname); 
				bnotelevsubset = true; 
			}
		}
		
		if (btransitivesubset) 
		{
			for (Map.Entry<String, String> mess : sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied.submapping.entrySet())
			{
				if (!mess.getValue().equals("") && vsselectedsubsets.contains(mess.getValue()))
					vsselectedsubsets.add(mess.getKey()); 
			}
		}
		
		if (bnotelevsubset || binversubset)
			selevsubset = null; 
		
		// get going again
		SetSubsetVisibleCodeStringsT(selevsubset, sketchdisplay.sketchgraphicspanel.tsketch);
		sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
	}


	/////////////////////////////////////////////
	static void VRecurseSubsetsdown(List<SubsetAttr> vssa, SubsetAttr sa)
	{
		if (vssa.contains(sa))
			return; 
		int i = vssa.size(); 
		vssa.add(sa); 
		while (i < vssa.size())
		{
			SubsetAttr lsa = vssa.get(i); 
			for (SubsetAttr dsa : vssa.get(i).subsetsdownmap.values())
			{
				if (!vssa.contains(dsa))
					vssa.add(dsa); 
			}
			i++; 
		}
	}
}; 
	
	
