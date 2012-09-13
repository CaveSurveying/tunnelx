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

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import javax.swing.JSplitPane;
import java.awt.Dimension;

import java.awt.Color;
import java.util.List;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import javax.swing.JTextArea;
//
//
//
//

/////////////////////////////////////////////
class DefaultMutableTreeNodeFile extends DefaultMutableTreeNode
{
	FileAbstraction fa = new FileAbstraction(); 
    boolean bdirnodeloaded = false; 

	DefaultMutableTreeNodeFile(FileAbstraction lfa)
	{
		super(lfa.getAbsolutePath());
        fa = lfa; 
	}
	public String toString()
	{
        return fa.getName() + ((fa.xfiletype == FileAbstraction.FA_DIRECTORY) || (fa.xfiletype == FileAbstraction.FA_FILE_XML_SKETCH) ? "" : " (" + fa.xfiletype + ")");
	}
    public boolean isLeaf()
    {
        return (fa.xfiletype != FileAbstraction.FA_DIRECTORY); 
    }
}


/////////////////////////////////////////////
// this class will encapsulate all the mess that is the left hand side of the mainbox
class TunnelFileList extends JPanel implements TreeSelectionListener
{
	MainBox mainbox;
    JSplitPane jsp; 

	DefaultListModel tflistmodel;
	JList tflist;
	final static Color[] colNotLoaded = { new Color(1.0F, 1.0F, 1.0F), new Color(0.7F, 0.8F, 0.9F) };
	final static Color[] colLoaded = { new Color(0.8F, 1.0F, 0.8F), new Color(0.2F, 1.0F, 0.3F) };
	final static Color[] colNotSaved = { new Color(1.0F, 0.6F, 0.6F), new Color(1.0F, 0.4F, 0.4F) };
	final static Color[] colNoFile = { new Color(0.6F, 0.5F, 1.0F), new Color(0.2F, 0.3F, 1.0F) };

	// sketch indices
	int isketche; // last element in list.

	// what's selected.
	int activesketchindex;

    JTree tftree = new JTree(); 
	DefaultMutableTreeNode dmroot = new DefaultMutableTreeNode("root");
	DefaultTreeModel dmtreemod = new DefaultTreeModel(dmroot);

	DefaultMutableTreeNodeFile dmsymbols = new DefaultMutableTreeNodeFile(FileAbstraction.currentSymbols);
	DefaultMutableTreeNodeFile dmtutorials = new DefaultMutableTreeNodeFile(FileAbstraction.tutorialSketches);

	/////////////////////////////////////////////
	void AddTreeDirectory(FileAbstraction td)
	{
    	DefaultMutableTreeNodeFile dmtd = new DefaultMutableTreeNodeFile(td);
		dmroot.add(dmtd); 
		System.out.println("Addtreedirectory " + dmtd.getPath()); 
        dmtreemod.reload(dmroot); 
        LoadDirNode(dmtd); 
    }


	/////////////////////////////////////////////
    synchronized void LoadDirNode(DefaultMutableTreeNodeFile dmtf)
    {
        dmtf.bdirnodeloaded = true; // block second loading which can be done in different thread at startup
		//tunneldirectory.FindFilesOfDirectory(ftsketches, allfontcolours); 
        try
        {
        List<FileAbstraction> fod = dmtf.fa.GetDirContents();
        for (FileAbstraction tfile : fod)
        {
        	DefaultMutableTreeNodeFile dmf = new DefaultMutableTreeNodeFile(tfile);
    		dmtf.add(dmf); 
        }
        dmtreemod.reload(dmtf); 
        }
        catch (IOException e)
        { TN.emitWarning(e.toString()); }
    }



	/////////////////////////////////////////////
	TunnelFileList(MainBox lmainbox)
	{
        super(new BorderLayout());
   		mainbox = lmainbox;

		tftree.setRootVisible(false);
		tftree.setShowsRootHandles(true);
		tftree.setEditable(false); 
		tftree.setExpandsSelectedPaths(true); 
		tftree.addTreeSelectionListener(this);
		//tftree.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dmroot.add(dmsymbols); 
		dmroot.add(dmtutorials); 

		tftree.setModel(dmtreemod);
        tftree.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            public void treeWillExpand(TreeExpansionEvent e)
            {
                DefaultMutableTreeNodeFile dmtf = (DefaultMutableTreeNodeFile)e.getPath().getLastPathComponent(); 
                if (!dmtf.bdirnodeloaded && (dmtf.fa.xfiletype == FileAbstraction.FA_DIRECTORY))
                    LoadDirNode(dmtf);
            }
            public void treeWillCollapse(TreeExpansionEvent e)  {;}
        }); 
        tftree.addMouseListener(new MouseAdapter() 
        {
            public void mousePressed(MouseEvent e) 
            {
                int selRow = tftree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tftree.getPathForLocation(e.getX(), e.getY());
                if ((selRow == -1) || (e.getClickCount() != 2))
                    return; 
                DefaultMutableTreeNodeFile dmtf = (DefaultMutableTreeNodeFile)selPath.getLastPathComponent(); 

                //System.out.println(dmtf.fa.getAbsolutePath() + "  " + e.getClickCount()); 
                if (mainbox.GetActiveTunnelSketches() == mainbox.vgsymbolstsketches)
                    TN.emitWarning("Cannot use on symbols list"); 
                else if (dmtf.fa.xfiletype == FileAbstraction.FA_FILE_XML_SKETCH)
                {
                    List<OneSketch> ftsketches = mainbox.GetActiveTunnelSketches(); // shouldn't ever work on symbols
                    int iselindex = -1; 
                    for (int i = 0; i < ftsketches.size(); i++)
                    {
                        if (dmtf.fa.equals(ftsketches.get(i).sketchfile))
                            iselindex = i; 
                    }

                    // either select it or load it new
                    if (iselindex != -1)
                    {
                        tflist.setSelectedIndex(iselindex); 
                        mainbox.ViewSketch(ftsketches.get(iselindex));
                    }
                    else
                        mainbox.MainOpen(dmtf.fa, SvxFileDialog.FT_XMLSKETCH); 
                }
                else if (dmtf.fa.xfiletype == FileAbstraction.FA_FILE_SVX)
                    mainbox.MainOpen(dmtf.fa, SvxFileDialog.FT_SVX); 
                else if (dmtf.fa.xfiletype == FileAbstraction.FA_FILE_POCKET_BINTOP)
                    mainbox.MainOpen(dmtf.fa, SvxFileDialog.FT_SVX); 
                else
                    TN.emitWarning("Nothing to do on type " + dmtf.fa.xfiletype + " which is at "+dmtf.fa.getAbsolutePath()); 
            }
        }); 

		tflistmodel = new DefaultListModel();
		tflist = new JList(tflistmodel);
		tflist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tflist.setCellRenderer(new ColourCellRenderer());

		tflist.addListSelectionListener(new ListSelectionListener()
    	{
            public void valueChanged(ListSelectionEvent e)
        	{
                UpdateSelect(false);
	        };
        });

		tflist.addMouseListener(new MouseAdapter() 
        {
            public void mouseClicked(MouseEvent e) 
            {
                if (e.getClickCount() == 2) 
                    UpdateSelect(true);
            }
        });

        jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
		jsp.setRightComponent(new JScrollPane(tflist));
		jsp.setLeftComponent(new JScrollPane(tftree));
	    add(jsp, BorderLayout.CENTER); 

        //Create the scroll pane and add the tree to it.
		//setViewportView(tflist);
	}

	/////////////////////////////////////////////
	OneSketch GetSelectedSketchLoad()
	{
		// load the sketch if necessary.  Then import it
		if (activesketchindex == -1)
			return null; 
		OneSketch lselectedsketch = mainbox.GetActiveTunnelSketches().get(activesketchindex); 
		if (!lselectedsketch.bsketchfileloaded)
		{
			mainbox.tunnelloader.LoadSketchFile(lselectedsketch, true);
			tflist.repaint();
		}
		return lselectedsketch;
	}


	/////////////////////////////////////////////
	class ColourCellRenderer extends JLabel implements ListCellRenderer
	{
		// This is the only method defined by ListCellRenderer.
		// We just reconfigure the JLabel each time we're called.
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			assert index != -1;  // this is the null setting for the listed indices
			// one of the strings in the list (the other type files)
			Color[] colsch;
			if (value instanceof String)
			{
				colsch = colNotLoaded;
				setText((String)value);
			}
			else if (!(index < isketche))
			{
				TN.emitWarning("strange index setting " + index + "<" + isketche + " " + tflistmodel.getSize());

				colsch = colNotLoaded;
				setText(value.toString());
			}

			// sketch type
			// we have to dereference from the array rather than use the object here since it may have been loaded
			else
			{
				assert (index < isketche);
				OneSketch rsketch = mainbox.GetActiveTunnelSketches().get(index);
				FileAbstraction skfile = rsketch.sketchfile;

				setText((isSelected ? "--" : "") + skfile.getSketchName() + "  |  " + skfile.getPath());
				colsch = (!rsketch.bsketchfileloaded ? colNotLoaded : (rsketch.bsketchfilechanged ? colNotSaved : colLoaded));
			}

			setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			setBackground(colsch[isSelected ? 1 : 0]);

			setOpaque(true);
			return this;
		}
	}

	/////////////////////////////////////////////
    public void valueChanged(TreeSelectionEvent e)
    {
        //System.out.println("hi there" + e); 
    }

        


	/////////////////////////////////////////////
	void RemakeTFList()
	{
        System.out.println("RemakeTFList with " + mainbox.GetActiveTunnelSketches().size() + " entries"); 
		activesketchindex = -1;

        // clearing and adding the elements into the list model in a tight loop sometimes failed to give any list at all 
        // on startup of this window.  Okay after startup if it changed and this function was called.  
        // Unresolved problem.  Not sure how this list fits in with the tree view as well
        //tflistmodel.clear();
		tflistmodel = new DefaultListModel(); 

		for (OneSketch tsketch : mainbox.GetActiveTunnelSketches())
			tflistmodel.addElement(tsketch);
		isketche = tflistmodel.getSize();
        System.out.println("isketche " + isketche); 
        tflist.setModel(tflistmodel); 
	}


	/////////////////////////////////////////////
	public void UpdateSelect(boolean bDoubleClick)
	{
		// work out what it is that's selected.
		int index = tflist.getSelectedIndex();

		activesketchindex = -1;
		if (index < isketche)
			activesketchindex = index;

		// spawn off the window.
		if (bDoubleClick)
			mainbox.ViewSketch((activesketchindex != -1 ? mainbox.GetActiveTunnelSketches().get(activesketchindex) : null));
	}
}

