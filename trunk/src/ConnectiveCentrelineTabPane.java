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
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/////////////////////////////////////////////
class ConnectiveCentrelineTabPane extends JPanel
{
	JTextField tfhead = new JTextField("junk1");
	JTextField tftail = new JTextField("junk2");

	/////////////////////////////////////////////
	ConnectiveCentrelineTabPane()
	{
		super(new BorderLayout());

		tfhead.setEditable(false);
		tftail.setEditable(false);

		JPanel pfie = new JPanel(new GridLayout(2, 2));
		pfie.add(new JLabel("head:"));
		pfie.add(tfhead);
		pfie.add(new JLabel("tail:"));
		pfie.add(tftail);

		add("North", new JLabel("Centreline leg", JLabel.CENTER));
		add("South", pfie);
	}
}


