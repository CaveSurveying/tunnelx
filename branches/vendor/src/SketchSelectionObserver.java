////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
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

