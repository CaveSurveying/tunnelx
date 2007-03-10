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

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;

import javax.swing.plaf.basic.BasicComboBoxRenderer;


/////////////////////////////////////////////
class SubsetStyleComboBoxRenderer extends BasicComboBoxRenderer
{
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
//System.out.println("yipyip" + index);
		SubsetAttrStyle sas = (SubsetAttrStyle)value;
		if (isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
			if (sas != null)
				list.setToolTipText(sas.stylename);
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setFont(list.getFont());
		setText((sas == null) ? "" : sas.shortstylename);
		return this;
	}
};


/////////////////////////////////////////////
class SketchSubsetPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JComboBox jcbsubsetstyles;

	JTree pansksubsetstree = new JTree();
	SubsetAttrStyle sascurrent = null;

	JTextField tfsubsetlist = new JTextField();


	/////////////////////////////////////////////
	SketchSubsetPanel(SketchDisplay lsketchdisplay)
	{
		super(new BorderLayout());
		sketchdisplay = lsketchdisplay;

		JPanel jpbuts = new JPanel(new GridLayout(0, 2));
		jpbuts.add(new JButton(sketchdisplay.acaReflect));
		jpbuts.add(new JButton(sketchdisplay.acaAddToSubset));
		JButton butacaRemoveFromSubset = new JButton(sketchdisplay.acaRemoveFromSubset);
		butacaRemoveFromSubset.setMargin(new Insets(2, 3, 2, 3));
		jpbuts.add(new JButton(sketchdisplay.acaCleartreeSelection));
		jpbuts.add(butacaRemoveFromSubset);

		jpbuts.add(new JLabel("subset style:", JLabel.RIGHT));
		//jcbsubsetstyles = new JComboBox(sketchdisplay.sketchlinestyle.subsetattrstylesselectable);  // this updates dynamically from the vector
		jcbsubsetstyles = new JComboBox();
        jcbsubsetstyles.setRenderer(new SubsetStyleComboBoxRenderer());

		jcbsubsetstyles.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ SubsetSelectionChanged(); } } );
		jpbuts.add(jcbsubsetstyles);

		// says what lists the current selection is in
		tfsubsetlist.setEditable(false);

		pansksubsetstree.setRootVisible(false);
		pansksubsetstree.setShowsRootHandles(true);
		pansksubsetstree.setEditable(true); 
		pansksubsetstree.setExpandsSelectedPaths(true); 
		pansksubsetstree.addTreeSelectionListener(new TreeSelectionListener()
			{ public void valueChanged(TreeSelectionEvent e)
				{ sketchdisplay.selectedsubsetstruct.UpdateTreeSubsetSelection(pansksubsetstree);  } } );


		add("North", jpbuts);
		JScrollPane jsp = new JScrollPane(pansksubsetstree);
		jsp.setPreferredSize(new Dimension(150, 150));
		add("Center", jsp);
		add("South", tfsubsetlist);
	}


	/////////////////////////////////////////////
	void SubsetSelectionChanged()
	{
		sascurrent = (SubsetAttrStyle)jcbsubsetstyles.getSelectedItem();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true); 
		if (sascurrent != null)
		{
			ReloadTreeSubsets(); 
			sketchdisplay.selectedsubsetstruct.UpdateTreeSubsetSelection(pansksubsetstree);
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(0, false);
	}

	
	/////////////////////////////////////////////
	void ReloadTreeSubsets()
	{
		sascurrent.TreeListUnattributedSubsets(sketchdisplay.sketchgraphicspanel.tsketch.vpaths); 
		pansksubsetstree.setModel(sascurrent.dmtreemod);
		sketchdisplay.sketchgraphicspanel.tsketch.SetSubsetAttrStyle(sascurrent, sketchdisplay.vgsymbols); 
		sketchdisplay.sketchgraphicspanel.sketchgrid = sascurrent.sketchgrid;
	}
	

	
	/////////////////////////////////////////////
	void AddSelCentreToCurrentSubset()
	{
		OneTunnel atunnel = sketchdisplay.mainbox.tunnelfilelist.activetunnel;
		Object obj = (sketchdisplay.mainbox.tunnelfilelist.activesketchindex != -1 ? atunnel.tsketches.elementAt(sketchdisplay.mainbox.tunnelfilelist.activesketchindex) : null);
		if (!(obj instanceof OneSketch))
		{
			TN.emitMessage("Should have a sketch selected");
			return;
		}
		OneSketch asketch = (OneSketch)obj;

		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;

		if (asketch.ExtractCentrelinePathCorrespondence(atunnel, sketchdisplay.sketchgraphicspanel.clpaths, sketchdisplay.sketchgraphicspanel.corrpaths, sketchdisplay.sketchgraphicspanel.tsketch, sketchdisplay.sketchgraphicspanel.activetunnel))
		{
			// assign the subset to each path that has correspondence.
			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.corrpaths.size(); i++)
				PutToSubset(sketchdisplay.sketchgraphicspanel.corrpaths.get(i), sactive, true);
			sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}

	/////////////////////////////////////////////
	void AddRemainingCentreToCurrentSubset()
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;

		for (int i = 0; i < sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && op.vssubsets.isEmpty())
				PutToSubset(op, sactive, true);
		}
		sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}


	/////////////////////////////////////////////
	// this is the proximity graph one
	void PartitionRemainsByClosestSubset()
	{
		ProximityDerivation pd = new ProximityDerivation(sketchdisplay.sketchgraphicspanel.tsketch);
		pd.parainstancequeue.bDropdownConnectiveTraversed = true;
		pd.parainstancequeue.bCentrelineTraversed = true;
		pd.parainstancequeue.fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections (to deal with vertical line cases)
		pd.parainstancequeue.bnodeconnZSetrelativeTraversed = true;

		OnePathNode[] cennodes = new OnePathNode[pd.vcentrelinenodes.size()];
		for (int i = 0; i < sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if (op.vssubsets.isEmpty())
			{
				// this could be done a lot more efficiently with a specialized version
				// that stops when it finds the first node it can use for deciding.
				OnePath cop = pd.EstClosestCenPath(op);
				if ((cop != null) && !cop.vssubsets.isEmpty())
					op.vssubsets.add(cop.vssubsets.get(0));
			}
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}

	/////////////////////////////////////////////
	void PutToSubset(OnePath op, String sactive, boolean bAdd)
	{
		// present
		if (op.IsPathInSubset(sactive))
		{
			if (!bAdd)
			{
				op.RemoveFromSubset(sactive);
				op.pnstart.icnodevisiblesubset--; // take off node counters
				op.pnend.icnodevisiblesubset--;
			}
		}

		// absent
		else if (bAdd)
			op.vssubsets.add(sactive);  // node counters added with setvisiblecodestrings

		op.SetSubsetAttrs(sascurrent, sketchdisplay.vgsymbols);
		if (op.karight != null)
			op.karight.SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);
		if (op.kaleft != null)
			op.kaleft.SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);
		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStrings(op, bAdd);
	}



	/////////////////////////////////////////////
	// go through all the different means of selection available and push them in.
	void MakeTotalSelList(Set<OnePath> opselset)
	{
		if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
			opselset.add(sketchdisplay.sketchgraphicspanel.currgenpath);
		if (sketchdisplay.sketchgraphicspanel.currselarea != null)
		{
			for (RefPathO rpo : sketchdisplay.sketchgraphicspanel.currselarea.refpaths)
				opselset.add(rpo.op);
			for (ConnectiveComponentAreas cca : sketchdisplay.sketchgraphicspanel.currselarea.ccalist)
			{
				for (OnePath sop : cca.vconnpaths)
					opselset.add(sop);
			}
		}
		for (List<OnePath> vp : sketchdisplay.sketchgraphicspanel.vactivepaths)
		{
			for (OnePath op : vp)
				opselset.add(op);
		}
	}
	
	/////////////////////////////////////////////
	// adds and removes from subset
	void PutSelToSubset(boolean bAdd)
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;

		Set<OnePath> opselset = new HashSet<OnePath>(); 
		MakeTotalSelList(opselset); 
		for (OnePath op : opselset)
			PutToSubset(op, sactive, bAdd);
		sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}

	/////////////////////////////////////////////
	void ElevationSubset(String Sprefix)
	{
		Set<OnePath> opselset = new HashSet<OnePath>(); 
		MakeTotalSelList(opselset); 
		if (opselset.isEmpty())
			return; 

		// check if this set is just connective lines, and mark them up if we can
		// do for single connective line for now.  
		if (opselset.size() != 1)
			return; 
		OnePath opc = opselset.iterator().next(); 
		if (opc.linestyle != SketchLineStyle.SLS_CONNECTIVE)
			return; 
		if (opc.plabedl == null)
			opc.plabedl = new PathLabelDecode();
		if (opc.plabedl.barea_pres_signal != SketchLineStyle.ASE_KEEPAREA)
			return; 
		
 		opc.plabedl.barea_pres_signal = SketchLineStyle.ASE_ELEVATIONPATH; // just now need to find where it is in the list in the combo-box
		opc.plabedl.iarea_pres_signal = SketchLineStyle.iareasigelev; 
		if (SketchLineStyle.iareasigelev == -1)
			TN.emitError("Missing area_signal_def elevationpath in fontcolours"); 
		assert opc.plabedl.barea_pres_signal == SketchLineStyle.areasigeffect[opc.plabedl.iarea_pres_signal]; 

// make the new Centreline bit
		sketchdisplay.sketchlinestyle.SetConnectiveParametersIntoBoxes(opc);

		// heavy calculation setup to get the closest centreline nodes
		ProximityDerivation pd = new ProximityDerivation(sketchdisplay.sketchgraphicspanel.tsketch);
		OnePathNode[] cennodes = new OnePathNode[1]; 
		int icennodes = pd.ShortestPathsToCentrelineNodes(opc, cennodes, null);
		
		// cook up a unique name for it.  
		// we are going to need to relay these names out when we come to importing this sketch
		String sselevsubset = null; 
		for (int i = 0; i < icennodes + 10000; i++)
		{
			assert cennodes[i].pnstationlabel != null; 
			sselevsubset = Sprefix + (i < icennodes ? cennodes[i].pnstationlabel : (icennodes != 0 ? cennodes[i].pnstationlabel : "n") + "n" + (i - icennodes)); 
			if (!sascurrent.unattributedss.contains(sselevsubset))
				break; 
			sselevsubset = null; 
		}
		assert !sascurrent.unattributedss.contains(sselevsubset); 

		// now select it
		for (OnePath op : opselset)
			PutToSubset(op, sselevsubset, true);
		sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 

		DefaultMutableTreeNode dm = new DefaultMutableTreeNode(sselevsubset); 
		sascurrent.unattributedss.add(0, sselevsubset); 
		sascurrent.dmunattributess.insert(dm, 0); 
		sascurrent.dmtreemod.reload(sascurrent.dmunattributess); 

		TreePath tpsel = sascurrent.tpunattributed.pathByAddingChild(dm); 
		pansksubsetstree.setSelectionPath(tpsel);

		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}
	
	
	/////////////////////////////////////////////
	void RemoveAllFromSubset()
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null) 
			return;

		for (int i = sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size() - 1; i >= 0; i--)
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			PutToSubset(op, sactive, false);
		}
		sketchdisplay.selectedsubsetstruct.bIsElevStruct = sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 

		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}

	/////////////////////////////////////////////
	void DeleteTodeleteSubset()
	{
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
		if (!sketchdisplay.sketchgraphicspanel.bEditable)
			return; 
		int ndeletions = 0; 
		for (int i = sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size() - 1; i >= 0; i--) 
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if (op.IsPathInSubset("todelete"))
			{
				sketchdisplay.sketchgraphicspanel.RemovePath(op);
				ndeletions++; 
			}
		}
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		TN.emitMessage("Deleted " + ndeletions + " paths labelled as 'todelete'"); 
	}
	

	/////////////////////////////////////////////
	List<String> vsubsetsinarea = new ArrayList<String>();
	List<String> vsubsetspartinarea = new ArrayList<String>();
	boolean Updateviewvpartialsubsets(List<String> opvss, boolean bfirst)
	{
		if (bfirst)
		{
			vsubsetsinarea.addAll(opvss);
			return false;
		}

		// we can only move elements from the left to the right
		for (int i = vsubsetsinarea.size() - 1; i >= 0; i--)
		{
			if (!opvss.contains(vsubsetsinarea.get(i)))
				vsubsetspartinarea.add(vsubsetsinarea.remove(i));
		}

		// file strings we have in the correct place
		for (int i = 0; i < opvss.size(); i++)
		{
			if (!vsubsetsinarea.contains(opvss.get(i)))
			{
				if (!vsubsetspartinarea.contains(opvss.get(i)))
					vsubsetspartinarea.add(opvss.get(i));
			}
		}
		return false; 
	}

	/////////////////////////////////////////////
	void UpdateSubsetsOfPath()
	{
		// a single path is selected
		if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
		{
			OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
			if (op.vssubsets.size() == 0)
				tfsubsetlist.setText("  -- no subset -- ");
			else if (op.vssubsets.size() == 1)
				tfsubsetlist.setText(op.vssubsets.get(0));
			else
			{
				StringBuffer sb = new StringBuffer();
				for (String ssubset : op.vssubsets)
				{
					if (sb.length() != 0)
						sb.append("+");
					sb.append(ssubset);
				}
				tfsubsetlist.setText(sb.toString());
			}
		}

		// an area set of paths is selected
		else if (sketchdisplay.sketchgraphicspanel.currselarea != null)
		{
			vsubsetsinarea.clear();
			vsubsetspartinarea.clear();
			boolean bfirst = true; 
			for (RefPathO rpo : sketchdisplay.sketchgraphicspanel.currselarea.refpaths)
				bfirst = Updateviewvpartialsubsets(rpo.op.vssubsets, bfirst);
			for (ConnectiveComponentAreas cca : sketchdisplay.sketchgraphicspanel.currselarea.ccalist)
			{
				for (OnePath sop : cca.vconnpaths)
					bfirst = Updateviewvpartialsubsets(sop.vssubsets, bfirst);
			}

			StringBuffer sb = new StringBuffer();
			for (String ssub : vsubsetsinarea)
			{
				if (sb.length() != 0)
					sb.append("+");
				sb.append(ssub);
			}
			if (!vsubsetspartinarea.isEmpty())
			{
				for (String ssub : vsubsetspartinarea)
				{
					if (sb.length() != 0)
						sb.append("+");
					sb.append("(");
					sb.append(ssub);
					sb.append(")");
				}
			}
			tfsubsetlist.setText(sb.toString());
		}

		// nothing selected
		else
			tfsubsetlist.setText("");
	}
}

