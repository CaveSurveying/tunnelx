////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.util.Vector; 

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
	Vector tunnels = new Vector(); 
	MainBox mainbox; 
	JTree tree; 
	DefaultTreeModel ttmod; 
	DefaultMutableTreeNode dmroot; 

	/////////////////////////////////////////////
	TunnelTree(MainBox lmainbox)
	{
		mainbox = lmainbox; 
		dmroot = new DefaultMutableTreeNode(mainbox.roottunnel); 
		ttmod = new DefaultTreeModel(dmroot); 

        //Create a tree that allows one selection at a time.
        tree = new JTree(ttmod); 
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        //tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

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
		System.out.println(nodeInfo.toString());
		mainbox.SelectTunnel((OneTunnel)(nodeInfo)); 
	};

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
	// used for multiactive selections used in the sketch and wireframes.  
	OneTunnel[] GetActiveTunnels() 
	{
		TreePath[] tpaths = tree.getSelectionPaths(); 
		if (tpaths == null) 
			return null; 

		OneTunnel[] res = new OneTunnel[tpaths.length]; 
		for (int i = 0; i < tpaths.length; i++) 
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)(tpaths[i].getLastPathComponent()); 
			Object nodeInfo = node.getUserObject();
			res[i] = (OneTunnel)(nodeInfo); 
		}

		return res; 
	}

	/////////////////////////////////////////////
	void RefreshListBox(OneTunnel root, OneTunnel activetunnel)
	{
        dmroot.removeAllChildren();
		dmroot.setUserObject(root); 
		addObjectRecurse(dmroot); 
		ttmod.reload(); 

		// then we should reselect the node
        //tree.expandPath(new TreePath(parent.getPath()));
        //tree.scrollPathToVisible(new TreePath(childNode.getPath()));
	}
}

