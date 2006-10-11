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

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import javax.swing.tree.*;
import javax.swing.JScrollPane;

//import javax.swing.JFrame;import java.awt.*;import java.awt.event.*;

//
//
// TunnelTree
//
//

/////////////////////////////////////////////
//class TunnelTreeModel implements DefaultTreeModel


/////////////////////////////////////////////
// this class will encapsulate all the mess that is the left hand side of the mainbox
class TunnelTree extends JScrollPane implements TreeSelectionListener
{
	// array of one tunnels corresponding to the rows
	MainBox mainbox;
	JTree tree;
	DefaultTreeModel ttmod;
	DefaultMutableTreeNode dmroot;

	OneTunnel activetunnel;

	/////////////////////////////////////////////
	TunnelTree(MainBox lmainbox)
	{
		mainbox = lmainbox;
		dmroot = new DefaultMutableTreeNode(mainbox.roottunnel);
		ttmod = new DefaultTreeModel(dmroot);

        //Create a tree that allows one selection at a time.
        tree = new JTree(ttmod);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);
        //Create the scroll pane and add the tree to it.
        setViewportView(tree);
	}



	/////////////////////////////////////////////
	public void valueChanged(TreeSelectionEvent e)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.getPath().getLastPathComponent());
		Object nodeInfo = node.getUserObject();
		//TN.emitMessage(" valueChanged " + nodeInfo.toString());

		OneTunnel lactivetunnel = (OneTunnel)(nodeInfo);
		if (mainbox.wireframedisplay.isVisible() && (activetunnel != lactivetunnel))
		{
			if (activetunnel != null)
				activetunnel.SetWFactiveRecurse(false);
			if (lactivetunnel != null)
				lactivetunnel.SetWFactiveRecurse(true);
			mainbox.wireframedisplay.RefreshWireDisplay();
		}

		activetunnel = lactivetunnel;
		mainbox.tunnelfilelist.SetActiveTunnel(activetunnel);
	};

	/////////////////////////////////////////////
	/////////////////////////////////////////////
    public void addObjectRecurse(DefaultMutableTreeNode parent)
	{
		OneTunnel tunnel = (OneTunnel)(parent.getUserObject());
		for (int i = 0; i < tunnel.ndowntunnels; i++)
		{
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(tunnel.downtunnels[i]);
			ttmod.insertNodeInto(childNode, parent, parent.getChildCount());
		    addObjectRecurse(childNode);
		}
	}

	/////////////////////////////////////////////
	void RefreshListBox(OneTunnel root)
	{
       	dmroot.removeAllChildren();
		dmroot.setUserObject(root);
		addObjectRecurse(dmroot);
		ttmod.reload();
		activetunnel = null;
	}
}

