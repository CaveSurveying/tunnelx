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

import javax.swing.JComboBox; 
import javax.swing.JPanel; 
import javax.swing.JButton; 
import javax.swing.BoxLayout; 
import javax.swing.JLabel; 
import javax.swing.JTextField; 

import java.awt.Insets; 
import java.awt.Component; 
import java.awt.GridLayout; 

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 

import java.awt.BasicStroke; 
import java.awt.Color; 

//
//
// SketchSelectionObserver  
//
//

/////////////////////////////////////////////
class SketchSelectionObserver extends JPanel 
{
	JTextField tfselitem = new JTextField(); 
	JLabel lab; 
	JTextField tfselnum = new JTextField(); 

	int item = -1; 
	int num = -1; 

	/////////////////////////////////////////////
	SketchSelectionObserver(String slab) 
	{
		// set up the view 
		tfselitem.setEditable(false); 
		lab = new JLabel(slab); 
		tfselnum.setEditable(false); 

		//setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
		setLayout(new GridLayout(1, 0)); 
		add("West", tfselitem); 
		add("Center", lab); 
		add("East", tfselnum); 
	}

	/////////////////////////////////////////////
	void ObserveSelection(int litem, int lnum) 
	{
		if (item != litem) 
		{
			item = litem; 
			tfselitem.setText(item == -1 ? "" : String.valueOf(item + 1)); 
		}
		if (num != lnum) 
		{
			num = lnum; 
			tfselnum.setText(num == -1 ? "" : String.valueOf(num)); 
		}
	}
}; 

