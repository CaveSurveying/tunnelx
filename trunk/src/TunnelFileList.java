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

import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;

import java.awt.Color;

//
//
//
//


/////////////////////////////////////////////
// this class will encapsulate all the mess that is the left hand side of the mainbox
class TunnelFileList extends JScrollPane implements ListSelectionListener, MouseListener
{
	MainBox mainbox;
	OneTunnel activetunnel;

	DefaultListModel tflistmodel;
	JList tflist;
	final static Color[] colNotLoaded = { new Color(1.0F, 1.0F, 1.0F), new Color(0.7F, 0.8F, 0.9F) };
	final static Color[] colLoaded = { new Color(0.6F, 1.0F, 0.6F), new Color(0.2F, 1.0F, 0.3F) };
	final static Color[] colNotSaved = { new Color(1.0F, 0.6F, 0.6F), new Color(1.0F, 0.4F, 0.4F) };
	final static Color[] colNoFile = { new Color(0.6F, 0.5F, 1.0F), new Color(0.2F, 0.3F, 1.0F) };

	// indices into list of special files
	int isvx;
	int ilegs;
	int iexp;
	int ipos;


	// sketch indices
	int isketchb;
	int isketche; // last element in list.

	// what's selected.
	int activesketchindex;
	int activetxt; // 0 svx, 1 legs, 2 exports, 3 pos


	/////////////////////////////////////////////
	OneSketch GetSelectedSketchLoad()
	{
		// load the sketch if necessary.  Then import it
		if (activesketchindex == -1)
			return null; 
		Object obj = activetunnel.tsketches.elementAt(activesketchindex);
		OneSketch lselectedsketch;
		if (obj instanceof OneSketch)
			lselectedsketch = (OneSketch)obj;
		else
		{
			TN.emitWarning("Sketch to be imported not loaded");
			lselectedsketch = mainbox.tunnelloader.LoadSketchFile(activetunnel, activesketchindex);
			assert lselectedsketch == activetunnel.tsketches.elementAt(activesketchindex);
		}
		return lselectedsketch; 				
	}

	/////////////////////////////////////////////
	String GetSelectedSketchPath(OneTunnel ot)
	{
		// first find the path of the tunnel (which we won't) 
		if (activetunnel != ot)
			return null; 
		if (activesketchindex == -1)
			return null; 

		String tunnelpath = ""; 

		Object obj = activetunnel.tsketches.elementAt(activesketchindex);
		FileAbstraction fsselected; 
		if (obj instanceof OneSketch)
			fsselected = ((OneSketch)obj).sketchfile;
		else
			fsselected = (FileAbstraction)obj; 

		return tunnelpath + fsselected.getName(); 
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
				if (index == isvx)
				{
					colsch = (activetunnel.svxfile != null ? (activetunnel.bsvxfilechanged ? colNotSaved : colLoaded) : colNoFile);
					setText("SVX: " + (activetunnel.svxfile != null ? activetunnel.svxfile.getPath() : ""));
				}
				else if (index == ilegs)
				{
					colsch = (activetunnel.bmeasurementsfilechanged ? colNoFile : colLoaded);
					setText("LEGS: " + activetunnel.measurementsfile.getPath());
				}
				else if (index == iexp)
				{
					colsch = (activetunnel.bexportfilechanged ? colNoFile : colLoaded);
					setText("LEGS: " + activetunnel.exportfile.getPath());
				}
				else if (index == ipos)
				{
					colsch = colLoaded;
					setText("POS: " + activetunnel.posfile.getPath());
				}
				// the place holder line
				else
				{
					colsch = colNotLoaded;
					setText((String)value);
				}
			}

			else if (!((index >= isketchb) && (index < isketche)))
			{
				TN.emitWarning("strange index setting " + index);
				TN.emitMessage("isketchbbee " + isketchb + "  " + isketche);  // uncomment this line elsewhere

				colsch = colNotLoaded;
				setText(value.toString());
			}
			// sketch type
			// we have to dereference from the array rather than use the object here since it may have been loaded
			else
			{
				assert (index >= isketchb) && (index < isketche);
				assert (value instanceof FileAbstraction) || (value instanceof OneSketch);
				Object rvalue = activetunnel.tsketches.elementAt(index - isketchb);
				FileAbstraction skfile = (rvalue instanceof FileAbstraction ? (FileAbstraction)rvalue : ((OneSketch)rvalue).sketchfile);

				// just check that at least the file name is the same, even if the object may have been replaced
				assert skfile.getName().equals((value instanceof FileAbstraction ? (FileAbstraction)value : ((OneSketch)value).sketchfile).getName());

				setText((isSelected ? "--" : "") + "SKETCH: " + skfile.getPath());
				colsch = (rvalue instanceof FileAbstraction ? colNotLoaded : (((OneSketch)rvalue).bsketchfilechanged ? colNotSaved : colLoaded));
			}

			setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			setBackground(colsch[isSelected ? 1 : 0]);

			setOpaque(true);
			return this;
		}
	}


	/////////////////////////////////////////////
	TunnelFileList(MainBox lmainbox)
	{
		mainbox = lmainbox;

		tflistmodel = new DefaultListModel();
		tflist = new JList(tflistmodel);
		tflist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tflist.setCellRenderer(new ColourCellRenderer());

		tflist.addListSelectionListener(this);
		tflist.addMouseListener(this);

	        //Create the scroll pane and add the tree to it.
		setViewportView(tflist);
	}


	/////////////////////////////////////////////
	void RemakeList()
	{
		activesketchindex = -1;
		activetxt = -1;

		tflistmodel.clear();
		if (activetunnel == null)
			return;

		if (activetunnel.svxfile != null)
		{
			isvx = tflistmodel.getSize();
			tflistmodel.addElement("junksvxfile"); //activetunnel.svxfile.getTypePlusName(activetunnel.bsvxfilechanged, "SVX"));
		}

		// svx file loaded.  show something there.
		else if (activetunnel.TextData.length() != 0)
		{
			isvx = tflistmodel.getSize();
			tflistmodel.addElement("*SVX");
		}
		else
			isvx = -1;

		if (activetunnel.measurementsfile != null)
		{
			ilegs = tflistmodel.getSize();
			tflistmodel.addElement("junklegsfile"); //activetunnel.measurementsfile.getTypePlusName(activetunnel.bmeasurementsfilechanged, "LEGS"));
		}
		else
			ilegs = -1;

		if (activetunnel.exportfile != null)
		{
			iexp = tflistmodel.getSize();
			tflistmodel.addElement("junkexportfile"); //activetunnel.exportfile.getTypePlusName(activetunnel.bexportfilechanged, "EXPORT"));
		}
		else
			iexp = -1;

		if (activetunnel.posfile != null)
		{
			ipos = tflistmodel.getSize();
			tflistmodel.addElement("junkposfile"); // activetunnel.posfile.getTypePlusName(false, "POS"));
		}
		else
			ipos = -1;

		// list of sketches
		if (!activetunnel.tsketches.isEmpty())
			tflistmodel.addElement(" ---- ");

		isketchb = tflistmodel.getSize();
		for (int i = 0; i < activetunnel.tsketches.size(); i++)
			tflistmodel.addElement(activetunnel.tsketches.elementAt(i));
		isketche = tflistmodel.getSize();
//TN.emitMessage("isketchbbee " + isketchb + "  " + isketche);
	}


	/////////////////////////////////////////////
	void SetActiveTunnel(OneTunnel lactivetunnel)
	{
		activetunnel = lactivetunnel;
		RemakeList();
	}


	/////////////////////////////////////////////
	void AddNewSketch(OneSketch sketch)
	{
		activetunnel.tsketches.addElement(sketch);
		RemakeList();
		tflist.setSelectedIndex(isketche - 1);
		UpdateSelect(true); // doubleclicks it.
	}

	/////////////////////////////////////////////
	public void UpdateSelect(boolean bDoubleClick)
	{
		activesketchindex = -1;
		activetxt = -1;

		int index = tflist.getSelectedIndex();

		if ((index >= isketchb) && (index < isketche))
			activesketchindex = index - isketchb;
		else if (index == isvx)
			activetxt = 0;
		else if (index == ilegs)
			activetxt = 1;
		else if (index == iexp)
			activetxt = 2;
		else if (index == ipos)
			activetxt = 3;

		// spawn off the window.
		if (bDoubleClick)
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

