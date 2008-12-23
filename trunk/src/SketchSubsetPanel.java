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
	void SubsetSelectionChanged(boolean bjustframetree)
	{
		sascurrent = (SubsetAttrStyle)jcbsubsetstyles.getSelectedItem();
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
			sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct();
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
		sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
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
			if (!op.vssubsets.isEmpty())
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
		sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}


	/////////////////////////////////////////////
	RefPathO rpcyc = new RefPathO(); 
	RefPathO rpfix = new RefPathO(); 
	boolean ConnectBetweenWalls(OnePath op)
	{	
		rpfix.op = op; 
		rpfix.bFore = true; 
		rpcyc.ccopy(rpfix); 
		boolean bOnwallfront = false; 
		do
		{
			if ((rpcyc.op.linestyle == SketchLineStyle.SLS_WALL) || (rpcyc.op.linestyle == SketchLineStyle.SLS_ESTWALL))
				bOnwallfront = true; 
		}
		while (!rpcyc.AdvanceRoundToNode(rpfix));

		rpfix.bFore = false; 
		rpcyc.ccopy(rpfix); 
		boolean bOnwallback = false; 
		do
		{
			if ((rpcyc.op.linestyle == SketchLineStyle.SLS_WALL) || (rpcyc.op.linestyle == SketchLineStyle.SLS_ESTWALL))
				bOnwallback = true; 
		}
		while (!rpcyc.AdvanceRoundToNode(rpfix));
		
		return bOnwallfront && bOnwallback; 
	}
	
	/////////////////////////////////////////////
	OnePathNode ConnectingCentrelineNode(OnePath op, boolean bFore)
	{
		rpfix.op = op; 
		rpfix.bFore = bFore;
		if (rpfix.ToNode().IsCentrelineNode()) // seen an error where this is 
			return rpfix.ToNode(); // we have the centreline node

		rpcyc.ccopy(rpfix); 
		OnePathNode opncen = null; 
		do
		{
			if (rpcyc.FromNode().IsCentrelineNode())
			{
				if (opncen != null)
					TN.emitWarning("Two centreline nodes connect; which one should be chosen?"); 
				opncen = rpcyc.FromNode(); 
			}
		}
		while (!rpcyc.AdvanceRoundToNode(rpfix));
		return opncen; 
	}
	
	
	/////////////////////////////////////////////
	void ElevationSubset(boolean bXC)
	{
		Set<OnePath> opselset = sketchdisplay.sketchgraphicspanel.MakeTotalSelList(); 
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

		sketchdisplay.sketchgraphicspanel.ClearSelection(true);

		OnePathNode opcfore = ConnectingCentrelineNode(opc, true); 
		OnePathNode opcback = ConnectingCentrelineNode(opc, false); 
	
		String lsselevsubset; 
		if (bXC)
		{
			if (!ConnectBetweenWalls(opc))
			{
				TN.emitWarning("Cross-section must connect between walls"); 
				return; 
			}
			if ((opcfore != null) && (opcback != null) && (opcfore != opcback))
				TN.emitWarning("choice of two stations for naming XC"); 
			lsselevsubset = "XC_" + (opcfore != null ? opcfore.pnstationlabel : (opcback != null ? opcback.pnstationlabel : "d")); 
		}
		else
		{
			if ((opcfore == null) || (opcback == null))
			{
				TN.emitWarning("Elevations must go from nodes connected to centrelines");  			
				return; 
			}
			lsselevsubset = "ELEV_" + opcfore.pnstationlabel + "_" + opcback.pnstationlabel; 
		}			
		lsselevsubset = lsselevsubset.replaceAll("[|^]", "."); 
		
		opc.plabedl.barea_pres_signal = SketchLineStyle.ASE_ELEVATIONPATH; // just now need to find where it is in the list in the combo-box
		opc.plabedl.iarea_pres_signal = SketchLineStyle.iareasigelev; 
		if (SketchLineStyle.iareasigelev == -1)
			TN.emitError("Missing area_signal_def elevationpath in fontcolours");
		assert opc.plabedl.barea_pres_signal == SketchLineStyle.areasigeffect[opc.plabedl.iarea_pres_signal]; 

		// now we're ready to go through with it
		
		//sketchdisplay.sketchlinestyle.SetConnectiveParametersIntoBoxes(opc);

		// cook up a unique name for it.  
		// we are going to need to relay these names out when we come to importing this sketch
		String sselevsubset = lsselevsubset; 
		int ni = 0; 
		while (sascurrent.unattributedss.contains(sselevsubset) || sascurrent.xsectionss.contains(sselevsubset))
			sselevsubset = lsselevsubset + "_n" + (ni++); 

		double opcpathleng = sketchdisplay.selectedsubsetstruct.QCGetPathLength(opc); 

		// make the centreline that will be added
		OnePath opelevaxis; 
        OnePathNode cpnstart; 
        OnePathNode cpnend; 
		if (bXC)
		{
			// find the length
			double xright = Math.max(opc.pnstart.pn.getX(), opc.pnend.pn.getX()) + opcpathleng; 
			double ymid = (opc.pnstart.pn.getY() + opc.pnend.pn.getY()) / 2; 
			double opcpathlengH = (opc.pnstart.pn.getX() < opc.pnend.pn.getX() ? opcpathleng : -opcpathleng) / 2;  

			cpnstart = new OnePathNode((float)(xright - opcpathlengH), (float)ymid, 0.0F); 
			cpnend = new OnePathNode((float)(xright + opcpathlengH), (float)ymid, 0.0F); 
		}

        // elevation case.  try to connect to a node that's already there
		else
		{
            double xright = 50.0; 
        	cpnstart = new OnePathNode((float)(xright + 0.0), (float)opcfore.zalt, 0.0F); 
			cpnend = new OnePathNode((float)(xright + opcpathleng), (float)opcback.zalt, 0.0F); 
			//return; // not done yet
        }
			
		opelevaxis = new OnePath(cpnstart); 
		opelevaxis.EndPath(cpnend); 
		opelevaxis.linestyle = SketchLineStyle.SLS_CENTRELINE; 
		opelevaxis.plabedl = new PathLabelDecode();
		opelevaxis.plabedl.centrelineelev = sselevsubset; 

		List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
		pthstoadd.add(opelevaxis); 
		sketchdisplay.sketchgraphicspanel.CommitPathChanges(null, pthstoadd); 

		// now select this new subset
		sketchdisplay.selectedsubsetstruct.opelevarr.clear(); 
		sketchdisplay.selectedsubsetstruct.selevsubset = sselevsubset; 
		for (OnePath op : opselset)
		{
			PutToSubset(op, sselevsubset, true);
			sketchdisplay.selectedsubsetstruct.opelevarr.add(op); 
		}
		PutToSubset(opelevaxis, sselevsubset, true);
		sketchdisplay.selectedsubsetstruct.opelevarr.add(opelevaxis); 

		sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 
		assert sketchdisplay.selectedsubsetstruct.bIsElevStruct; 
		
		DefaultMutableTreeNode dm = new DefaultMutableTreeNode(sselevsubset); 
		sascurrent.xsectionss.add(0, sselevsubset); 
		sascurrent.dmxsectionss.insert(dm, 0); 
		sascurrent.dmtreemod.reload(sascurrent.dmxsectionss); 

		TreePath tpsel = sascurrent.tpxsection.pathByAddingChild(dm); 
		pansksubsetstree.setSelectionPath(tpsel);

		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
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
		sketchdisplay.selectedsubsetstruct.ReorderAndEstablishXCstruct(); 

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

		List<OnePath> lvpathstodelete = new ArrayList<OnePath>();
		for (OnePath op : sketchdisplay.sketchgraphicspanel.tsketch.vpaths)
		{
			if (op.vssubsets.contains("todelete"))
				lvpathstodelete.add(op);
		}
		for (OnePath op : lvpathstodelete)
			sketchdisplay.sketchgraphicspanel.RemovePath(op);

		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		TN.emitMessage("Deleted " + lvpathstodelete.size() + " paths labelled as 'todelete'"); 
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

