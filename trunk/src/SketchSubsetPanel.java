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

import java.util.Vector;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;




/////////////////////////////////////////////
class SketchSubsetPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JComboBox jcbsubsetstyles;

	JTree pansksubsetstree = new JTree();
	SubsetAttrStyle sascurrent = null;

	Vector vsaselected = new Vector();

	JTextField tfsubsetlist = new JTextField();


	/////////////////////////////////////////////
	class SkSubset
	{
		String name;
		int npaths;

		SkSubset(String lname)
		{
			name = lname;
			npaths = 0;
		}

		public String toString()
		{
			return name;//"<html><b>" + name + "</b>" + (isSelected() ? "<font color=#ff00dd> ***" : "") + "</html>";
		}
	};


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
		jcbsubsetstyles = new JComboBox(sketchdisplay.sketchlinestyle.subsetattrstylesselectable);  // this updates dynamically from the vector

		jcbsubsetstyles.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{ sascurrent = (SubsetAttrStyle)jcbsubsetstyles.getSelectedItem();
				  UpdateTreeSubsetSelection(true);
				  sketchdisplay.sketchgraphicspanel.SketchChanged(0, false);
				} } );


		jpbuts.add(jcbsubsetstyles);

		// says what lists the current selection is in
		tfsubsetlist.setEditable(false);

		pansksubsetstree.setRootVisible(false);
        pansksubsetstree.addTreeSelectionListener(new TreeSelectionListener()
			{ public void valueChanged(TreeSelectionEvent e)
				{ UpdateTreeSubsetSelection(false);  } } );


		add("North", jpbuts);
		JScrollPane jsp = new JScrollPane(pansksubsetstree);
		jsp.setPreferredSize(new Dimension(150, 150));
		add("Center", jsp);
		add("South", tfsubsetlist);
	}




	/////////////////////////////////////////////
	void UpdateTreeSubsetSelection(boolean brefactortree)
	{
		// reloads a tree and sets all the attributes
		if (brefactortree)
		{
			pansksubsetstree.setModel(sascurrent.dmtreemod);

			// find the active subsets
			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size(); i++)
			{
				OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
				op.SetSubsetAttrs(sascurrent, sketchdisplay.vgsymbols);
			}

			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.tsketch.vsareas.size(); i++)
				((OneSArea)sketchdisplay.sketchgraphicspanel.tsketch.vsareas.elementAt(i)).SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);

			// reset the grid
			sketchdisplay.sketchgraphicspanel.sketchgrid = sascurrent.sketchgrid;
		}

		// sets the list of the visible components
		Vector vsaselected = sketchdisplay.sketchgraphicspanel.vsaselected;
		vsaselected.clear();
		TreePath[] tps = pansksubsetstree.getSelectionPaths();
		if (tps != null)
		{
			for (int i = 0; i < tps.length; i++)
			{
				DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tps[i].getLastPathComponent();
				SubsetAttr sa = (SubsetAttr)tn.getUserObject();
				if (vsaselected.contains(sa))
					continue;
				vsaselected.addElement(sa);
				if (!sketchdisplay.miTransitiveSubset.isSelected())
					continue;
				// do dependents (simulated recursion)
				for (int j = vsaselected.size() - 1; j < vsaselected.size(); j++)
				{
					SubsetAttr dsa = (SubsetAttr)vsaselected.elementAt(j);
					for (int k = 0; k < dsa.vsubsetsdown.size(); k++)
						if (!vsaselected.contains(dsa.vsubsetsdown.elementAt(k)))
							vsaselected.addElement(dsa.vsubsetsdown.elementAt(k));
				}
			}
		}

		// make visible codestrings
		sketchdisplay.sketchgraphicspanel.vsselectedsubsets.clear();
		for (int i = 0; i < vsaselected.size(); i++)
			sketchdisplay.sketchgraphicspanel.vsselectedsubsets.addElement(((SubsetAttr)vsaselected.elementAt(i)).subsetname);

//		saactive = null;

		// get going again
		sketchdisplay.sketchgraphicspanel.tsketch.SetSubsetVisibleCodeStrings(sketchdisplay.sketchgraphicspanel.vsselectedsubsets, sketchdisplay.miInverseSubset.isSelected());
		sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
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

		if (asketch.ExtractCentrelinePathCorrespondence(atunnel, sketchdisplay.sketchgraphicspanel.clpaths, sketchdisplay.sketchgraphicspanel.corrpaths, sketchdisplay.sketchgraphicspanel.tsketch, sketchdisplay.sketchgraphicspanel.activetunnel))
		{
			// assign the subset to each path that has correspondence.
			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.corrpaths.size(); i++)
				PutSelToSubset((OnePath)sketchdisplay.sketchgraphicspanel.corrpaths.elementAt(i), true);
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}

	/////////////////////////////////////////////
	void AddRemainingCentreToCurrentSubset()
	{
		for (int i = 0; i < sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size(); i++)
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && op.vssubsets.isEmpty())
				PutSelToSubset(op, true);
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}


	/////////////////////////////////////////////
	// this is the proximity graph one
	void PartitionRemainsByClosestSubset()
	{
		ProximityDerivation pd = new ProximityDerivation(sketchdisplay.sketchgraphicspanel.tsketch, false);
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
					op.vssubsets.addElement(cop.vssubsets.elementAt(0));
			}
		}
		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
	}

	/////////////////////////////////////////////
	void PutSelToSubset(OnePath op, boolean bAdd)
	{
		if (sketchdisplay.sketchgraphicspanel.vsselectedsubsets.isEmpty())
			return;
		String sactive = (String)sketchdisplay.sketchgraphicspanel.vsselectedsubsets.elementAt(0);

		// find if this path is in the subset
		int i = 0;
		for ( ; i < op.vssubsets.size(); i++)
		{
			if (sactive.equals((String)op.vssubsets.elementAt(i)))
				break;
		}

		// present
		if (i != op.vssubsets.size())
		{
			if (!bAdd)
			{
				op.vssubsets.removeElementAt(i);
				op.pnstart.icnodevisiblesubset--; // take off node counters
				op.pnend.icnodevisiblesubset--;
			}
		}
		// absent
		else
		{
			if (bAdd)
				op.vssubsets.add(sactive);  // node counters added with setvisiblecodestrings
		}

		op.SetSubsetAttrs(sascurrent, sketchdisplay.vgsymbols);
		if (op.karight != null)
			op.karight.SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);
		if (op.kaleft != null)
			op.kaleft.SetSubsetAttrs(true, sketchdisplay.subsetpanel.sascurrent);
		op.SetSubsetVisibleCodeStrings(sketchdisplay.sketchgraphicspanel.vsselectedsubsets, sketchdisplay.miInverseSubset.isSelected());
	}


	/////////////////////////////////////////////
	// adds and removes from subset
	void PutSelToSubset(boolean bAdd)
	{
		// go through all the different means of selection available and push them in.
		if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
			PutSelToSubset(sketchdisplay.sketchgraphicspanel.currgenpath, bAdd);
		if (sketchdisplay.sketchgraphicspanel.currselarea != null)
		{
			for (int i = 0; i < (int)sketchdisplay.sketchgraphicspanel.currselarea.refpaths.size(); i++)
				PutSelToSubset(((RefPathO)sketchdisplay.sketchgraphicspanel.currselarea.refpaths.elementAt(i)).op, bAdd);
			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.currselarea.ccalist.size(); i++)
			{
				ConnectiveComponentAreas cca = (ConnectiveComponentAreas)sketchdisplay.sketchgraphicspanel.currselarea.ccalist.elementAt(i);
				for (int j = 0; j < cca.vconnpaths.size(); j++)
					PutSelToSubset(((RefPathO)cca.vconnpaths.elementAt(j)).op, bAdd);
			}
		}
		for (int i = 0; i < sketchdisplay.sketchgraphicspanel.vactivepaths.size(); i++)
		{
			Vector vp = (Vector)(sketchdisplay.sketchgraphicspanel.vactivepaths.elementAt(i));
			for (int j = 0; j < vp.size(); j++)
				PutSelToSubset((OnePath)vp.elementAt(j), bAdd);
		}


		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		sketchdisplay.sketchgraphicspanel.ClearSelection(true);
	}


	/////////////////////////////////////////////
	Vector vsubsetsinarea = new Vector();
	Vector vsubsetspartinarea = new Vector();
	boolean IsStringInVS(Vector vs, String s)
	{
		for (int i = 0; i < vs.size(); i++)
			if (s.equals(vs.elementAt(i)))
				return true;
		return false;
	}
	void Updateviewvpartialsubsets(Vector opvss, boolean bfirst)
	{
		if (bfirst)
		{
			vsubsetsinarea.addAll(opvss);
			return;
		}

		// we can only move elements from the left to the right
		for (int i = vsubsetsinarea.size() - 1; i >= 0; i--)
		{
			String vss = (String)vsubsetsinarea.elementAt(i);
			if (!IsStringInVS(opvss, vss))
			{
				vsubsetsinarea.removeElementAt(i);
				vsubsetspartinarea.addElement(vss);
				break;
			}
		}

		// file strings we have in the correct place
		for (int i = 0; i < opvss.size(); i++)
		{
			String ops = (String)opvss.elementAt(i);
			if (!IsStringInVS(vsubsetsinarea, ops))
			{
				if (!IsStringInVS(vsubsetspartinarea, ops))
					vsubsetspartinarea.addElement(ops);
			}
		}
	}
	/////////////////////////////////////////////
	void UpdateSubsetsOfPath()
	{
		// a single path is selected
		if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
		{
			OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
			if (op.vssubsets.size() == 1)
				tfsubsetlist.setText((String)op.vssubsets.elementAt(0));
			else
			{
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < op.vssubsets.size(); i++)
				{
					if (i != 0)
						sb.append("+");
					sb.append((String)op.vssubsets.elementAt(i));
				}
				tfsubsetlist.setText(sb.toString());
			}
		}

		// an area set of paths is selected
		else if (sketchdisplay.sketchgraphicspanel.currselarea != null)
		{
			vsubsetsinarea.clear();
			vsubsetspartinarea.clear();
			for (int i = 0; i < (int)sketchdisplay.sketchgraphicspanel.currselarea.refpaths.size(); i++)
				Updateviewvpartialsubsets(((RefPathO)sketchdisplay.sketchgraphicspanel.currselarea.refpaths.elementAt(i)).op.vssubsets, (i == 0));
			for (int i = 0; i < sketchdisplay.sketchgraphicspanel.currselarea.ccalist.size(); i++)
			{
				ConnectiveComponentAreas cca = (ConnectiveComponentAreas)sketchdisplay.sketchgraphicspanel.currselarea.ccalist.elementAt(i);
				for (int j = 0; j < cca.vconnpaths.size(); j++)
					Updateviewvpartialsubsets(((RefPathO)cca.vconnpaths.elementAt(j)).op.vssubsets, false);
			}

			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < vsubsetsinarea.size(); i++)
			{
				if (i != 0)
					sb.append("+");
				sb.append((String)vsubsetsinarea.elementAt(i));
			}
			if (!vsubsetspartinarea.isEmpty())
			{
				for (int i = 0; i < vsubsetspartinarea.size(); i++)
				{
					if (i != 0)
						sb.append("+");
					else
						sb.append(vsubsetsinarea.isEmpty() ? "(" : "+(");
					sb.append((String)vsubsetspartinarea.elementAt(i));
				}
				sb.append(")");
			}
			tfsubsetlist.setText(sb.toString());
		}

		// nothing selected
		else
			tfsubsetlist.setText("");
	}

	/////////////////////////////////////////////
	void ListMissingSubsets()
	{
		if (sascurrent == null)
			return;
		Vector subsetlist = new Vector();
		for (int i = 0; i < sascurrent.subsets.size(); i++)
			subsetlist.addElement(((SubsetAttr)sascurrent.subsets.elementAt(i)).subsetname);
		int isknown = subsetlist.size();

		// go through the paths, create new subsets; reallocate old ones
		for (int j = 0; j < sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size(); j++)
		{
			OnePath op = (OnePath)sketchdisplay.sketchgraphicspanel.tsketch.vpaths.elementAt(j);

			// subsets path is in (backwards list so sksubcode starts right
			for (int k = 0; k < op.vssubsets.size(); k++)
			{
				// match to a known subset
				String name = (String)op.vssubsets.elementAt(k);
				int i = 0;
				for ( ; i < subsetlist.size(); i++)
				{
					if (name.equals((String)subsetlist.elementAt(i)))
						break;
				}
				if (i == subsetlist.size())
					subsetlist.addElement(name);
			}
		}

		// list the missing subsets
		if (isknown == subsetlist.size())
			return;

		StringBuffer sb = new StringBuffer();
		for (int i = isknown; i < subsetlist.size(); i++)
		{
			sb.append(i == isknown ? "Unknown subsets in sketch: " : ", ");
			sb.append("\"" + (String)subsetlist.elementAt(i) + "\"");
		}
		TN.emitMessage(sb.toString());
	}
}

