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

	String selevsubset = null; // is the selected elevation subset

	// the structure of the elevation subset
	List<OnePath> opelevarr = new ArrayList<OnePath>(); // series of connective paths, a centreline path, and then the rest
	int iopelevarrCEN = -1; 
	float vcenX; 
	boolean bIsElevStruct = false; 

	SSymbScratchPath Tsscratchpath = new SSymbScratchPath(); // we'll need one for each connective path we put in line here
	/////////////////////////////////////////////
	boolean ReorderAndEstablishXCstruct() // tends to be called after a batch of puts
	{
		if (selevsubset == null)
			return false; 

		// collect the connective pieces together
		iopelevarrCEN = 0; 
		for (int i = 0; i < opelevarr.size();i++)
		{
			OnePath op = opelevarr.get(i); 
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_ELEVATIONPATH)) 
			{
				if (i != iopelevarrCEN)
				{
					opelevarr.set(i, opelevarr.get(iopelevarrCEN)); 
					opelevarr.set(iopelevarrCEN, op); 
				}
				iopelevarrCEN++; 
			}
		}
		
System.out.println("iopelevarrCEN " + iopelevarrCEN); 
		if (iopelevarrCEN == 0)
			return false; 
		if (iopelevarrCEN > 1)
			return false; 	// fails for now until we get to gluing series of connectives together
				
		// now find the centreline in here
		int iCEN = -1; 
		for (int i = iopelevarrCEN; i < opelevarr.size(); i++)
		{
			if (opelevarr.get(i).linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				if (iCEN != -1)
					return false; // not more than one
				iCEN = i; 
			}
		}
System.out.println(" iCENCEN " + iCEN); 
		if (iCEN == -1)
			return false; 
			
		if (iCEN != iopelevarrCEN)
		{
			OnePath op = opelevarr.get(iCEN); 
			opelevarr.set(iCEN, opelevarr.get(iopelevarrCEN)); 
			opelevarr.set(iopelevarrCEN, op); 
		}

		assert iopelevarrCEN == 1; 
		assert (opelevarr.get(iopelevarrCEN).linestyle == SketchLineStyle.SLS_CENTRELINE); 
		assert (opelevarr.get(0).plabedl.barea_pres_signal == SketchLineStyle.ASE_ELEVATIONPATH); 

System.out.println("WeHAVEelevSubset"); 
		vcenX = (float)(opelevarr.get(iopelevarrCEN).pnend.pn.getX() - opelevarr.get(iopelevarrCEN).pnstart.pn.getX()); 
		Tsscratchpath.SetUpPathLength(opelevarr.get(0)); 

		return true; 
	}
	
	/////////////////////////////////////////////
	double QCGetPathLength(OnePath op)
	{
		Tsscratchpath.SetUpPathLength(op); 
		return Tsscratchpath.GetCumuPathLength(); 
	}
	

	/////////////////////////////////////////////
	static Point2D evalpt = new Point2D.Float(); 
	void AlongCursorMark(Ellipse2D elevpoint, Point2D moupt)
	{
		OnePath cop = opelevarr.get(iopelevarrCEN); 
		double lam = (vcenX != 0.0 ? (moupt.getX() - cop.pnstart.pn.getX()) / vcenX : 0.5); 
	
		OnePath op = opelevarr.get(0); 
		Point2D levalpt; 
		if (lam <= 0.0)
			levalpt = op.pnstart.pn; 
		else if (lam >= 1.0) 
			levalpt = op.pnend.pn; 
		else
		{
			double r = lam * Tsscratchpath.GetCumuPathLength(); 
			double t = Tsscratchpath.ConvertAbstoNodePathLength(r, op);
			op.Eval(evalpt, null, t);
			levalpt = evalpt; 
		}

		double lstrokew = SketchLineStyle.strokew; 
		elevpoint.setFrame(levalpt.getX() - 2 * lstrokew, levalpt.getY() - 2 * lstrokew, 4 * lstrokew, 4 * lstrokew);
	}
	


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
			if ((selevsubset != null) && !opelevarr.contains(op))
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
		bIsElevStruct = ReorderAndEstablishXCstruct();

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
		//TN.emitMessage("Subset paths: " + nsubsetpaths + "  areas: " + nsubsetareas);
	}

	/////////////////////////////////////////////
	void UpdateSingleSubsetSelection(String lsubset)
	{
		vsselectedsubsetsP.clear(); 
		vsselectedsubsets.clear(); 
		vsselectedsubsetsP.add(lsubset); 
		vsselectedsubsets.add(lsubset); 
		selevsubset = null; 
		SetSubsetVisibleCodeStringsT(sketchdisplay.sketchgraphicspanel.tsketch);
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
					bnotelevsubset = (selevsubset != null); 
					selevsubset = ssubset; 
				}
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
			for (SubsetAttr dsa : vssa.get(i).subsetsdownmap.values())
			{
				if (!vssa.contains(dsa))
					vssa.add(dsa); 
			}
			i++; 
		}
	}
}; 
	
	
