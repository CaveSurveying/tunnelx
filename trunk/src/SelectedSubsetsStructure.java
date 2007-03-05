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

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;

/////////////////////////////////////////////
class SelectedSubsetStructure
{
	SketchDisplay sketchdisplay; 

	Set<String> vsselectedsubsetsP = new HashSet<String>(); 
	Set<String> vsselectedsubsets = new HashSet<String>(); 
	boolean binversubset = false; 
	boolean btransitivesubset = false; 

	String selevsubset = null; // is the selected elevation subset

	// the structure of the elevation subset
	List<OnePath> opelevarr = new ArrayList<OnePath>(); 

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
			if (selevsubset != null)
				opelevarr.add(op); 
			return true;
		}
		op.bpathvisiblesubset = false;
		if (!bAdd && (selevsubset != null))
			opelevarr.remove(op); 
		return false;
	}

	/////////////////////////////////////////////
	void SetSubsetVisibleCodeStringsT(OneSketch sketch)
	{
		opelevarr.clear(); 

		// set node codes down to be set up by the paths
		for (int i = 0; i < sketch.vnodes.size(); i++)
			((OnePathNode)sketch.vnodes.elementAt(i)).icnodevisiblesubset = 0;

		// set paths according to subset code
		sketch.bRestrictSubsetCode = !vsselectedsubsets.isEmpty();
		int nsubsetpaths = 0;
		for (int i = 0; i < sketch.vpaths.size(); i++)
		{
			if (SetSubsetVisibleCodeStrings((OnePath)sketch.vpaths.elementAt(i), true))
				nsubsetpaths++; 
		}

		// now scan through the areas and set those in range and their components to visible
		int nsubsetareas = 0;
		for (OneSArea osa : sketch.vsareas)
			nsubsetareas += osa.SetSubsetAttrs(false, null);

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
		//TN.emitMessage("Subset paths: " + nsubsetpaths + "  areas: " + nsubsetareas);
	}


	/////////////////////////////////////////////
	void UpdateTreeSubsetSelection(JTree pansksubsetstree)
	{
		binversubset = sketchdisplay.miInverseSubset.isSelected(); 
		btransitivesubset = sketchdisplay.miTransitiveSubset.isSelected(); 

		selevsubset = null; 
		boolean bnotelevsubset = false; 
		
		TreePath[] tps = pansksubsetstree.getSelectionPaths();

		vsselectedsubsetsP.clear(); 
		vsselectedsubsets.clear(); 

		for (int i = (tps != null ? tps.length - 1 : -1); i >= 0; i--)
		{
			DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tps[i].getLastPathComponent();
			if (tn.getUserObject() instanceof String)
			{
				// special case which just handles the string-types in the unattributed (relative to fontcolours) subsets list
				String ssubset = (String)tn.getUserObject(); 
				vsselectedsubsetsP.add(ssubset); 
				if (tn != sketchdisplay.subsetpanel.sascurrent.dmunattributess) 
				{
					vsselectedsubsets.add(ssubset); 
					bnotelevsubset = (selevsubset != null); 
					selevsubset = ssubset; 
				}
				else 
				{
					if (btransitivesubset)
						vsselectedsubsets.addAll(sketchdisplay.subsetpanel.sascurrent.unattributedss); 
					bnotelevsubset = true; 
				}	
				continue; 
			}

			SubsetAttr sa = (SubsetAttr)tn.getUserObject(); 
			vsselectedsubsetsP.add(sa.subsetname); 
			if (sketchdisplay.miTransitiveSubset.isSelected()) 
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
		
		if (bnotelevsubset || binversubset)
			selevsubset = null; 
		
		// get going again
		SetSubsetVisibleCodeStringsT(sketchdisplay.sketchgraphicspanel.tsketch);
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
			for (SubsetAttr dsa : vssa.get(i).vsubsetsdown)
			{
				if (!vssa.contains(dsa))
					vssa.add(dsa); 
			}
			i++; 
		}
	}
}; 
	
	