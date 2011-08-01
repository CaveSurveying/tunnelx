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
import javax.swing.ScrollPaneConstants; 
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
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
import java.util.Map;
import java.util.HashMap;

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

	// says what lists the current selection is in
	List<String> vsubsetsinarea = new ArrayList<String>();
	List<String> vsubsetspartinarea = new ArrayList<String>();
	JComboBox subsetlistsel = new JComboBox(); 

	/////////////////////////////////////////////
	SketchSubsetPanel(SketchDisplay lsketchdisplay)
	{
		super(new BorderLayout());
		sketchdisplay = lsketchdisplay;

		JPanel jpbuts = new JPanel(new GridLayout(0, 2));
		jpbuts.add(new JButton(sketchdisplay.acaReflect));
		JButton butacaAddToSubset = new JButton(sketchdisplay.acaAddToSubset); 
		butacaAddToSubset.setMargin(new Insets(2, 3, 2, 3));
		jpbuts.add(butacaAddToSubset);
		JButton butacaRemoveFromSubset = new JButton(sketchdisplay.acaRemoveFromSubset);
		butacaRemoveFromSubset.setMargin(new Insets(2, 3, 2, 3));
		jpbuts.add(new JButton(sketchdisplay.acaCleartreeSelection));
		jpbuts.add(butacaRemoveFromSubset);

		jpbuts.add(new JLabel("subset style:", JLabel.RIGHT));
		jcbsubsetstyles = new JComboBox();
        jcbsubsetstyles.setRenderer(new SubsetStyleComboBoxRenderer());

		jcbsubsetstyles.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ SubsetSelectionChanged(false); } } );
		jpbuts.add(jcbsubsetstyles);

		subsetlistsel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ int i = subsetlistsel.getSelectedIndex();
				  //System.out.println("Selind " + i + " " + vsubsetsinarea.size() + " " + vsubsetspartinarea.size());  
				  String ssub = (i >= vsubsetsinarea.size() + 1 ? vsubsetspartinarea.get(i - 1 - vsubsetsinarea.size()) : (i >= 1 ? vsubsetsinarea.get(i - 1) : "")); 
				  SelectSubset(ssub); 
				}
			} ); 

		pansksubsetstree.setRootVisible(false);
		pansksubsetstree.setShowsRootHandles(true);
		pansksubsetstree.setEditable(true); 
		pansksubsetstree.setExpandsSelectedPaths(true); 
		pansksubsetstree.addTreeSelectionListener(new TreeSelectionListener()
			{ public void valueChanged(TreeSelectionEvent e)
				{ sketchdisplay.selectedsubsetstruct.UpdateTreeSubsetSelection(pansksubsetstree);  } } );


		add(jpbuts, BorderLayout.NORTH);
		JScrollPane jsp = new JScrollPane(pansksubsetstree);
		jsp.setPreferredSize(new Dimension(150, 150));
		add(jsp, BorderLayout.CENTER);
		add(subsetlistsel, BorderLayout.SOUTH);
	}

	/////////////////////////////////////////////
	// this is all pretty annoying and explains why trees are not right here anymore
	// just does the tree to a depth of two
	void SelectSubset(String ssub)
	{
		if ((sascurrent == null) || ssub.equals(""))
			return; 
		for (int i = 0; i < sascurrent.dmroot.getChildCount(); i++)
		{
			TreeNode tn1 = sascurrent.dmroot.getChildAt(i); 
			for (int j = 0; j < tn1.getChildCount(); j++)
			{
				DefaultMutableTreeNode tn2 = (DefaultMutableTreeNode)tn1.getChildAt(j);
				if (tn2.getUserObject() instanceof String) 
				{
					String tn2v = (String)tn2.getUserObject(); 
					if (tn2v.equals(ssub))
					{
						TreePath tpres = new TreePath(sascurrent.dmroot).pathByAddingChild(tn1).pathByAddingChild(tn2); 
						pansksubsetstree.setSelectionPath(tpres);
						return; 
					}
				}
			}
		}
	}

	/////////////////////////////////////////////
	int Getcbsubsetstyleindex(String sfstyle)
	{
		for (int i = 0; i < jcbsubsetstyles.getItemCount(); i++)
			if (((SubsetAttrStyle)jcbsubsetstyles.getItemAt(i)).stylename.equals(sfstyle))
				return i;
		return -1; 
	}

	/////////////////////////////////////////////
	void SubsetSelectionChanged(boolean bjustframetree)
	{
		sascurrent = (SubsetAttrStyle)jcbsubsetstyles.getSelectedItem();
System.out.println(" SubsetSelectionChanged " + sascurrent);
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
		if (sascurrent != null)
		{
			sascurrent.TreeListUnattributedSubsets(sketchdisplay.sketchgraphicspanel.tsketch.vpaths);
			pansksubsetstree.setModel(sascurrent.dmtreemod);
			sketchdisplay.sketchgraphicspanel.tsketch.SetSubsetAttrStyle(sascurrent, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
			sketchdisplay.sketchgraphicspanel.sketchgrid = sascurrent.sketchgrid;

			sketchdisplay.selectedsubsetstruct.UpdateTreeSubsetSelection(pansksubsetstree);
			if (!bjustframetree)
			{
				sketchdisplay.sketchlinestyle.symbolsdisplay.ReloadSymbolsButtons(sascurrent); 
				sketchdisplay.sketchlinestyle.pthstylelabeltab.ReloadLabelsCombo(sascurrent); 
			}
			sascurrent.TreeListFrameDefCopiedSubsets(sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied); 
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SAS);
	}

	
	

	
	/////////////////////////////////////////////
	void AddSelCentreToCurrentSubset()
	{
		if (sketchdisplay.mainbox.tunnelfilelist.activesketchindex == -1)
		{
			TN.emitMessage("Should have a sketch selected");
			return;
		}
		OneSketch asketch = sketchdisplay.mainbox.GetActiveTunnelSketches().get(sketchdisplay.mainbox.tunnelfilelist.activesketchindex);
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;

		PtrelLn ptrelln = new PtrelLn();
		if (ptrelln.ExtractCentrelinePathCorrespondence(asketch, sketchdisplay.sketchgraphicspanel.tsketch))
		{
			// assign the subset to each path that has correspondence.
			for (PtrelPLn wptreli : ptrelln.wptrel)
				PutToSubset(wptreli.crp, sactive, true);
			sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
	}

	/////////////////////////////////////////////
	void AddRemainingCentreToCurrentSubset()
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;

		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && op.vssubsets.isEmpty())
				PutToSubset(op, sactive, true);
		}
		sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
	}


	/////////////////////////////////////////////
	// this is the proximity graph one
	void PartitionRemainsByClosestSubset()
	{
		ProximityDerivation pd = new ProximityDerivation(sketchdisplay.sketchgraphicspanel.tsketch);
		pd.parainstancequeue.bDropdownConnectiveTraversed = true;
		pd.parainstancequeue.bCentrelineTraversed = true;
		pd.parainstancequeue.fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections (to deal with vertical line cases)

		OnePathNode[] cennodes = new OnePathNode[pd.ncentrelinenodes];
		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
		{
			if (op.vssubsets.isEmpty())
			{
				OnePath cop = pd.EstClosestCenPath(op, false);
				if ((cop != null) && !cop.vssubsets.isEmpty())
					op.vssubsets.add(cop.vssubsets.get(0));
			}
		}

		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
	}


	/////////////////////////////////////////////
	// this is the proximity graph one
	void PartitionRemainsByClosestSubsetDatetype()
	{
		ProximityDerivation pd = new ProximityDerivation(sketchdisplay.sketchgraphicspanel.tsketch);
		pd.parainstancequeue.bDropdownConnectiveTraversed = true;
		pd.parainstancequeue.bCentrelineTraversed = true;
		pd.parainstancequeue.fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections (to deal with vertical line cases)

		Map<OnePath, String> datetypemap = new HashMap<OnePath, String>(); 

		OnePathNode[] cennodes = new OnePathNode[pd.ncentrelinenodes];
		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				continue; 
			OnePath cop = pd.EstClosestCenPath(op, true);
			if (cop == null)
				continue; 
			for (String ssubset : cop.vssubsets)
			{
				if (ssubset.startsWith("__date__ "))
					datetypemap.put(op, ssubset); 
			}
		}

		// now go through the areas and set them whole ones to subsets
		for (OneSArea osa : sketchdisplay.sketchgraphicspanel.tsketch.vsareas)
		{
			// first get the list of dates we need to select from
			Set<String> areadates = new HashSet<String>(); 
			for (RefPathO rpo : osa.refpathsub)
			{
				String ldate = datetypemap.get(rpo.op); 
				if (ldate != null)
					areadates.add(ldate); 
			}
			
			// now find the one date we will set things to by measuring their total lengths of each (a Map<String, Double> attempt went bad)
			String bdate = null; 
			double bdateleng = 0.0; 
			for (String ldate : areadates)
			{
				double ldateleng = 0.0; 
				for (RefPathO rpo : osa.refpathsub)
				{
					String lldate = datetypemap.get(rpo.op); 
					if ((lldate != null) && lldate.equals(ldate))
						ldateleng += rpo.op.linelength; 
				}
				if ((bdate == null) || (ldateleng > bdateleng))
				{
					bdate = ldate; 
					bdateleng = ldateleng; 
				}
			}
System.out.println("zzzzz  " + bdate + "  " + bdateleng); 
			
			if (bdate != null)
			{
				for (RefPathO rpo : osa.refpaths)
					rpo.op.vssubsets.add(bdate); 
			}
		}

		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
	}


	/////////////////////////////////////////////
	String GetSubOfTypeD(List<String> vssubsets, boolean bdatetype) 
	{
		if (!bdatetype)
			return (vssubsets.isEmpty() ? null : vssubsets.get(0)); 
		for (String ssubset : vssubsets)
		{
			if (ssubset.startsWith("__date__ "))
				return ssubset;
		}
		return null; 
	}
	


	/////////////////////////////////////////////
	void PutToSubset(OnePath op, String sactive, boolean bAdd)
	{
		// present
		if (op.vssubsets.contains(sactive))
		{
			if (!bAdd)
			{
				op.vssubsets.remove(sactive);
				assert !op.vssubsets.contains(sactive);
				op.pnstart.icnodevisiblesubset--; // take off node counters
				op.pnend.icnodevisiblesubset--;
			}
		}

		// absent
		else if (bAdd)
			op.vssubsets.add(sactive);  // node counters added with setvisiblecodestrings

		op.SetSubsetAttrs(sascurrent, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
		sketchdisplay.selectedsubsetstruct.SetSubsetVisibleCodeStrings(op, bAdd);
		if (op.karight != null)
			op.karight.SetSubsetAttrsA(true, sascurrent);
		if (op.kaleft != null)
			op.kaleft.SetSubsetAttrsA(true, sascurrent);

		//System.out.println("  vv-icnodevisible subset " + op.pnstart.icnodevisiblesubset + " " + op.pnend.icnodevisiblesubset); 
	}


	/////////////////////////////////////////////
	// adds and removes from subset
	void PutSelToSubset(boolean bAdd)
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null)
			return;
		Set<OnePath> opselset = sketchdisplay.sketchgraphicspanel.MakeTotalSelList(); 
		for (OnePath op : opselset)
			PutToSubset(op, sactive, bAdd);
		sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}


	/////////////////////////////////////////////
	static RefPathO srefpathconnspare = new RefPathO();
	static boolean IsWallNode(OnePathNode opn)
	{
		// find the subsets this node could be in
		srefpathconnspare.ccopy(opn.ropconn);
		do
		{
			if ((srefpathconnspare.op.linestyle == SketchLineStyle.SLS_WALL) || (srefpathconnspare.op.linestyle == SketchLineStyle.SLS_ESTWALL))
				return true; 
		}
		while (!srefpathconnspare.AdvanceRoundToNode(opn.ropconn));
		return false; 
	}

	
	
	/////////////////////////////////////////////
	boolean ElevationSubset(boolean bXC)
	{
		Set<OnePath> opselset = sketchdisplay.sketchgraphicspanel.MakeTotalSelList(); 
		if (opselset.isEmpty())
			return TN.emitWarning("Must have a path selected"); 

		List<OnePath> opclist = new ArrayList<OnePath>(); 
		for (OnePath opc : opselset)
		{
			if (opc.linestyle != SketchLineStyle.SLS_CONNECTIVE)
				return TN.emitWarning("Must be connective paths"); 
			if (opc.plabedl == null)
				opc.plabedl = new PathLabelDecode();
			if (opc.plabedl.barea_pres_signal != SketchLineStyle.ASE_KEEPAREA)
				return TN.emitWarning("Must be simple connective paths"); 
			opclist.add(opc); 
		}

		if (!ElevSet.ReorderElevPaths(opclist))
			return TN.emitWarning("Selected paths not in sequence (will try to reflect to fit later)"); 

		sketchdisplay.sketchgraphicspanel.ClearSelection(true);

		OnePathNode opcfore = opclist.get(0).pnstart.ConnectingCentrelineNode(); 
		OnePathNode opcback = opclist.get(opclist.size() - 1).pnend.ConnectingCentrelineNode(); 
	
		// this delimits by spaces (could use the head and tail settings, though, and try harder in the XC case to get to a station name)
		String lsselevsubset; 
		if (bXC)
		{
			if (!IsWallNode(opclist.get(0).pnstart))
				return TN.emitWarning("Cross-section must start at wall node"); 
			if (!IsWallNode(opclist.get(opclist.size() - 1).pnend))
				return TN.emitWarning("Cross-section must end at wall node"); 
			if ((opcfore != null) && (opcback != null) && (opcfore != opcback))
				TN.emitWarning("choice of two stations for naming XC"); 
			lsselevsubset = "XC " + (opcfore != null ? opcfore.pnstationlabel : (opcback != null ? opcback.pnstationlabel : "d")); 
		}
		else
		{
			if ((opcfore == null) || (opcback == null))
				return TN.emitWarning("Elevations must go from nodes connected to centrelines");  			
			lsselevsubset = "ELEV " + opcfore.pnstationlabel + " " + opcback.pnstationlabel; 
		}			
		lsselevsubset = lsselevsubset.replaceAll("[|^]", "."); 
		
		// cook up a unique name for it.  
		// we are going to need to relay these names out when we come to importing this sketch
		String sselevsubset = lsselevsubset; 
		int ni = 0; 
		while (sascurrent.unattributedss.contains(sselevsubset) || sascurrent.xsectionss.contains(sselevsubset))
			sselevsubset = lsselevsubset + " n" + (ni++); 


		// now add these paths into the elevset
		sketchdisplay.selectedsubsetstruct.elevset.Clear(); 
		//sketchdisplay.selectedsubsetstruct.elevset.selevsubset = sselevsubset; 
		for (OnePath opc : opclist)
		{
			PutToSubset(opc, sselevsubset, true);
			sketchdisplay.selectedsubsetstruct.elevset.connsequence.add(opc); 
		}
		
		OnePath opelevaxis = sketchdisplay.selectedsubsetstruct.elevset.MakeElevAxisPath(sselevsubset, bXC, opcfore, opcback); 

		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoadd.add(opelevaxis); 
		sketchdisplay.sketchgraphicspanel.CommitPathChanges(null, pthstoadd); 


		PutToSubset(opelevaxis, sselevsubset, true);
		//sketchdisplay.selectedsubsetstruct.elevset.elevcenpaths.add(opelevaxis); 
		assert (sketchdisplay.selectedsubsetstruct.elevset.elevcenpaths.size() == 1) && sketchdisplay.selectedsubsetstruct.elevset.elevcenpaths.get(0) == opelevaxis; 

		sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 
		assert sketchdisplay.selectedsubsetstruct.elevset.bIsElevStruct; 
		
		// put this new subset into the tree structure
		DefaultMutableTreeNode dm = new DefaultMutableTreeNode(sselevsubset); 
		sascurrent.xsectionss.add(0, sselevsubset); 
		sascurrent.dmxsectionss.insert(dm, 0); 
		sascurrent.dmtreemod.reload(sascurrent.dmxsectionss); 

		TreePath tpsel = sascurrent.tpxsection.pathByAddingChild(dm); 
		pansksubsetstree.setSelectionPath(tpsel);

		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
		return true; 	
	}
	
	/////////////////////////////////////////////
	String GetNewPaperSubset(String papersize) 
	{
		int ni = 1; 
		String sspapersubset; 
		do
			sspapersubset = "paper_" + papersize + "_page_" + (ni++); 
		while (sascurrent.unattributedss.contains(sspapersubset));

		DefaultMutableTreeNode dm = new DefaultMutableTreeNode(sspapersubset); 
		sascurrent.unattributedss.add(0, sspapersubset); 
		sascurrent.dmunattributess.insert(dm, 0); 
		sascurrent.dmtreemod.reload(sascurrent.dmunattributess); 
		return sspapersubset; 
	}
	
	/////////////////////////////////////////////
	void RemoveAllFromSubset()
	{
		String sactive = sketchdisplay.selectedsubsetstruct.GetFirstSubset();
		if (sactive == null) 
			return;

		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
			PutToSubset(op, sactive, false);
		sketchdisplay.selectedsubsetstruct.elevset.SetIsElevStruct(true); 

		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}

	/////////////////////////////////////////////
	void DeleteTodeleteSubset()
	{
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
		if (!sketchdisplay.sketchgraphicspanel.bEditable)
			return; 

		List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
		{
			if (op.vssubsets.contains("todelete"))
				pthstoremove.add(op);
		}
		sketchdisplay.sketchgraphicspanel.CommitPathChanges(pthstoremove, null); 

		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		TN.emitMessage("Deleted " + pthstoremove.size() + " paths labelled as 'todelete'"); 
	}
	

	/////////////////////////////////////////////
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
	void UpdateSubsetsOfPath(OnePath op)
	{
		// a single path is selected
		vsubsetsinarea.clear();
		vsubsetspartinarea.clear();

		// an area set of paths is selected (feature frigged in)
		if (sketchdisplay.sketchgraphicspanel.currselarea != null)
		{
			boolean bfirst = true; 
			for (RefPathO rpo : sketchdisplay.sketchgraphicspanel.currselarea.refpaths)
				bfirst = Updateviewvpartialsubsets(rpo.op.vssubsets, bfirst);
			for (ConnectiveComponentAreas cca : sketchdisplay.sketchgraphicspanel.currselarea.ccalist)
			{
				for (OnePath sop : cca.vconnpaths)
					bfirst = Updateviewvpartialsubsets(sop.vssubsets, bfirst);
			}
		}
		else if (op != null)
			vsubsetsinarea.addAll(op.vssubsets); 

		// update the subset buttons from this
		subsetlistsel.removeAllItems(); 

		String subsetlistf = (!vsubsetsinarea.isEmpty() ? vsubsetsinarea.get(0) : (!vsubsetspartinarea.isEmpty() ? "(" + vsubsetspartinarea.get(0) + ")" : "")); 
		String subsetlistsum = "List: " + vsubsetsinarea.size() + " " + (!vsubsetspartinarea.isEmpty() ? "(" + vsubsetspartinarea.size() + ")" : "") + " " + subsetlistf; 
		subsetlistsel.addItem(subsetlistsum); 
		for (String ssub : vsubsetsinarea)
			subsetlistsel.addItem(ssub); 
		for (String ssub : vsubsetspartinarea)
			subsetlistsel.addItem("Part: " + ssub); 
		subsetlistsel.validate(); 
	}
}

