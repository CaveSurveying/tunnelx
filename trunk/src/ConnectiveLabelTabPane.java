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
import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import java.awt.Font;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;


/////////////////////////////////////////////
class ConnectiveLabelTabPane extends JPanel
{
	JComboBox fontstyles = new JComboBox();
	JComboBox arrowstyles = new JComboBox();

	JButton jbcancel = new JButton("Cancel Label");
	JTextArea labtextfield = new JTextArea("how goes\n    there");

	/////////////////////////////////////////////
	void AddFontStyles(String[] labstylenames, Font[] fontlabs, int nlabstylenames)
	{
		for (int i = 0; i < nlabstylenames; i++)
			fontstyles.addItem(labstylenames[i]);
			
		arrowstyles.addItem("no arrow");
		arrowstyles.addItem("top left");
		arrowstyles.addItem("this doesn't");
		arrowstyles.addItem("really work");
	}


	/////////////////////////////////////////////
	ConnectiveLabelTabPane()
	{
		super(new BorderLayout());

		JPanel fsp = new JPanel();
		fsp.add(fontstyles);
		fsp.add(arrowstyles);
		add("North", fsp);
		add("Center", labtextfield);
		add("South", jbcancel);
	}
};

