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
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;

class ConnectiveGenTabPane extends JPanel
{
	JButton jbsymbols = new JButton("Add symbols");
	JButton jblabel = new JButton("Write text");
	JButton jbarea = new JButton("Area signal");

	ConnectiveGenTabPane()
	{
		super(new BorderLayout());

		setBackground(TN.sketchlinestyle_col); 
		JPanel pie = new JPanel(new GridLayout(3, 1));
		pie.add(jbsymbols);
		pie.add(jblabel);
		pie.add(jbarea);

		add("North", new JLabel("Connective path subtypes", JLabel.CENTER));
		add("Center", pie);
	}
}


