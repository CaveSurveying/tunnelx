////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Collections; 
import java.util.Set;


import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;





/////////////////////////////////////////////
class SubsetAttrStyle implements Comparable<SubsetAttrStyle>
{
	String stylename;
	boolean bselectable; // whether we show up in the dropdown list (or is this a partial
	String shortstylename; // used in the dropdown box
	int iloadorder; // used for sorting the order of loading, so we can put them in the drop-down box correctly; since using a map loses this ordering

	Map<String, SubsetAttr> msubsets = new HashMap<String, SubsetAttr>();

	//for (Map.Entry<String, SubsetAttr> e : m.entrySet())
	//System.out.println(e.getKey() + ": " + e.getValue());
	
	DefaultMutableTreeNode dmroot = new DefaultMutableTreeNode("root");
	DefaultTreeModel dmtreemod = new DefaultTreeModel(dmroot);

	List<String> unattributedss = new ArrayList<String>(); // contains the same, but as SubsetAttrs
	DefaultMutableTreeNode dmunattributess = new DefaultMutableTreeNode("_Unattributed_");
	List<String> xsectionss = new ArrayList<String>(); // those that appear superficially to act as subsets (they contain a centreline of elevation type)
	DefaultMutableTreeNode dmxsectionss = new DefaultMutableTreeNode("_XSections_");
	TreePath tpxsection = (new TreePath(dmroot)).pathByAddingChild(dmxsectionss); 

	SketchGrid sketchgrid = null;

	

	/////////////////////////////////////////////
	public int compareTo(SubsetAttrStyle sas)
	{
		return iloadorder - sas.iloadorder; 
	}
	
	
	/////////////////////////////////////////////
	void MakeTreeRootNode()
	{
		dmroot.removeAllChildren();
		Deque<DefaultMutableTreeNode> dmtnarr = new ArrayDeque<DefaultMutableTreeNode>(); 
		
		// build the tree downwards from each primary root node
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubsetattr != null)
				continue; 

			DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(sa);
			dmroot.add(cnode);
			dmtnarr.addFirst(cnode); 
			while (!dmtnarr.isEmpty())
			{
				DefaultMutableTreeNode lcnode = dmtnarr.removeFirst();
				SubsetAttr lsa = (SubsetAttr)lcnode.getUserObject(); 
				for (SubsetAttr dsa : lsa.subsetsdownmap.values())
				{
					DefaultMutableTreeNode ncnode = new DefaultMutableTreeNode(dsa);
					lcnode.add(ncnode);
					dmtnarr.addFirst(ncnode); 
				}
			}
		}

		// this is a separate dynamic folder with the subsets that don't have any subset attributes on them
		dmroot.add(dmunattributess); 
		dmroot.add(dmxsectionss); 
		dmtreemod.reload(dmroot); 
	}

	
	/////////////////////////////////////////////
	void TreeListUnattributedSubsets(List<OnePath> vpaths)
	{
		unattributedss.clear(); 
		xsectionss.clear(); 
		for (OnePath op : vpaths)
		{
			for (String ssubset : op.vssubsets)
			{
				if (msubsets.containsKey(ssubset))
					continue; 
				if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null) && (op.plabedl.centrelineelev != null) && op.plabedl.centrelineelev.equals(ssubset) && !xsectionss.contains(ssubset)) 
					xsectionss.add(ssubset); 									
				if (!unattributedss.contains(ssubset))
					unattributedss.add(ssubset); 
			}
		}		

		dmunattributess.removeAllChildren(); 
		dmxsectionss.removeAllChildren(); 
		Collections.reverse(xsectionss); 
		for (String ssubset : xsectionss)
			dmxsectionss.add(new DefaultMutableTreeNode(ssubset)); 
		for (String ssubset : unattributedss)
		{
			if (!xsectionss.contains(ssubset))
				dmunattributess.add(new DefaultMutableTreeNode(ssubset)); 
		}
		dmtreemod.reload(dmunattributess); 
		dmtreemod.reload(dmxsectionss); 
	}
	
	/////////////////////////////////////////////
	SubsetAttrStyle(String lstylename, int liloadorder, boolean lbselectable)
	{
		stylename = lstylename;
		iloadorder = liloadorder; 
		if (stylename.length() > 15)
			shortstylename = stylename.substring(0, 9) + "--" + stylename.substring(stylename.length() - 3);
		else
			shortstylename = stylename;
		bselectable = lbselectable;
		System.out.println(" creating " + stylename + (bselectable ? " (selectable)" : "") + " shortname " + shortstylename);
	}

	/////////////////////////////////////////////
	// these settings will be used to set a second layer of invisibility (entirely hide -- not just grey out -- from the list anything that is in any of these bViewhidden subsets.  
	void ToggleViewHidden(Set<String> vsselectedsubsets, boolean btransitive)
	{
		Deque<SubsetAttr> sarecurse = new ArrayDeque<SubsetAttr>(); 
		for (String ssubsetname : vsselectedsubsets)
			sarecurse.addFirst(msubsets.get(ssubsetname)); 
		while (!sarecurse.isEmpty())
		{
			SubsetAttr sa = sarecurse.removeFirst(); 
			sa.bViewhidden = !sa.bViewhidden; 
			if (!btransitive)
				continue; 
			for (SubsetAttr dsa : sa.subsetsdownmap.values())
				sarecurse.addFirst(dsa); 
		}
		dmtreemod.reload(dmroot); // should call nodesChanged on the individual ones (tricky because of no pointers to TreeNodes), but keep it simple for now
	}
	
	/////////////////////////////////////////////
	// used for the combobox which needs a short name
	// it would be nice if I could put tooltips
	public String toString()
	{
		return shortstylename;
	}

	/////////////////////////////////////////////
	void ImportSubsetAttrStyle(SubsetAttrStyle lsas)  // does a huge copy of a batch of subsetattributestyles
	{
		for (SubsetAttr lsa : lsas.msubsets.values())
		{
			SubsetAttr nsa = new SubsetAttr(lsa); 
			//subsets.add(nsa);
			msubsets.put(lsa.subsetname, nsa); 
		}
		if (lsas.sketchgrid != null)
			sketchgrid = lsas.sketchgrid; // copy down from above
	}

	/////////////////////////////////////////////
	// the variables don't work well because the upper subsets don't get copied into the
	// lower subsets and then evaluated.  Only if they are referenced do they get duplicated
	// and then have their variable evaluated in the lower level
    void FillAllMissingAttributes()
    {
		for (SubsetAttr sa : msubsets.values())
			sa.subsetsdownmap.clear(); 

		//System.out.println("Updating::" + stylename);
		// set pointers up
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubset != null)
			{
				sa.uppersubsetattr = msubsets.get(sa.uppersubset);
				if (sa.uppersubsetattr == null)
					TN.emitWarning("Upper subset " + sa.uppersubset + " not found of " + sa.subsetname);
				else
				{
					assert !sa.uppersubsetattr.subsetsdownmap.containsKey(sa.subsetname); 
					sa.uppersubsetattr.subsetsdownmap.put(sa.subsetname, sa);
				}
			}
		}

		// make the tree in reverse order of definition (or could have set up a partial sort)
		// used to evaluate it in order
		List<SubsetAttr> subsetsrevdef = new ArrayList<SubsetAttr>();
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubset == null)
				SelectedSubsetStructure.VRecurseSubsetsdown(subsetsrevdef, sa);
		}

		// recurse over missing attributes for each subset
		for (SubsetAttr sa : subsetsrevdef)
			sa.FillMissingAttribs();

		// get this part done
		MakeTreeRootNode();
    }
};



