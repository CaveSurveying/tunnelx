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

import java.awt.event.MouseListener; 
import java.awt.event.MouseEvent; 

import javax.swing.JScrollPane;
import java.io.File; 

//
//
//
//

/////////////////////////////////////////////


/////////////////////////////////////////////
// this class will encapsulate all the mess that is the left hand side of the mainbox
class TunnelFileList extends JScrollPane implements ListSelectionListener, MouseListener
{
	MainBox mainbox; 
	OneTunnel activetunnel; 

	DefaultListModel tflistmodel; 
	JList tflist; 

	// indices into list
	int isvx; 
	int isketchb; 
	int isketche; // last element in list.  

	OneSketch activesketch; 

	/////////////////////////////////////////////
	TunnelFileList(MainBox lmainbox)
	{
		mainbox = lmainbox; 

		tflistmodel = new DefaultListModel(); 
		tflist = new JList(tflistmodel); 
		tflist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 

        tflist.addListSelectionListener(this); 
		tflist.addMouseListener(this); 

        //Create the scroll pane and add the tree to it. 
        setViewportView(tflist); 
	}


	/////////////////////////////////////////////
	void SetActiveTunnel(OneTunnel lactivetunnel) 
	{
		activesketch = null; 
		activetunnel = lactivetunnel; 
		
		tflistmodel.clear(); 
		if (activetunnel != null)
		{
			if (activetunnel.svxfile != null)  
			{
				isvx = tflistmodel.getSize(); 
				tflistmodel.addElement((activetunnel.bsvxfilechanged ? "*SVX   " : " SVX   ") + activetunnel.svxfile.toString()); 
			}

			// svx file loadedd.  show something there.  
			else if (activetunnel.TextData.length() != 0)  
			{
				isvx = tflistmodel.getSize(); 
				tflistmodel.addElement("*SVX"); 
			}
			else
				isvx = -1; 

			if (activetunnel.xmlfile != null)  
				tflistmodel.addElement((activetunnel.bxmlfilechanged ? "*LEGS  " : " LEGS  ") + activetunnel.xmlfile.toString()); 
			if (activetunnel.exportfile != null)  
				tflistmodel.addElement((activetunnel.bexportfilechanged ? "*EXPORT " : " EXPORT ") + activetunnel.exportfile.toString()); 

			// add in list of image files that are available
			for (int i = 0; i < activetunnel.imgfiles.size(); i++) 
				tflistmodel.addElement(" IMG  " + ((File)activetunnel.imgfiles.elementAt(i)).toString());  

			tflistmodel.addElement(" ---- "); 

			isketchb = tflistmodel.getSize(); 
			for (int i = 0; i < activetunnel.tsketches.size(); i++)  
			{
				OneSketch sketch = (OneSketch)activetunnel.tsketches.elementAt(i); 
				tflistmodel.addElement((sketch.bsketchfilechanged ? "*SKETCH" : " SKETCH") + i + " " + sketch.sketchfile.toString()); 
			}
		}
	}


	/////////////////////////////////////////////
	void AddNewSketch(OneSketch sketch) 
	{
		int isketch = tflistmodel.getSize(); 
		activetunnel.tsketches.addElement(sketch); 
		tflistmodel.addElement("*SKETCH" + (activetunnel.tsketches.size() - 1) + " " + sketch.sketchfile.toString()); 
		tflist.setSelectedIndex(isketch); 
		UpdateSelect(true); // doubleclicks it.  
	}

	/////////////////////////////////////////////
	public void UpdateSelect(boolean bDoubleClick) 
	{
		int index = tflist.getSelectedIndex();  
		if (index >= isketchb) 
			activesketch = (OneSketch)activetunnel.tsketches.elementAt(index - isketchb); 
		else 
			activesketch = null; 

		// spawn off the window.  
		if (bDoubleClick && ((index == isvx) || (index >= isketchb))) 
			mainbox.ViewSketch(); 
	}

	/////////////////////////////////////////////
	public void valueChanged(ListSelectionEvent e) 
	{
		UpdateSelect(false); 
	};


 	/////////////////////////////////////////////
	public void mousePressed(MouseEvent e)  {;}; 
	public void mouseReleased(MouseEvent e)  {;}; 
	public void mouseEntered(MouseEvent e)  {;}; 
	public void mouseExited(MouseEvent e)  {;}; 
	public void mouseClicked(MouseEvent e) 
	{
		//int index = tflist.locationToIndex(e.getPoint());
		if (e.getClickCount() == 2) 
			UpdateSelect(true); 
	}
}

