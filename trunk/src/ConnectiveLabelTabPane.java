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
	JComboBox jcbnodestyles = new JComboBox();

	JButton jbcancel = new JButton("Cancel Label");
	JTextArea labtextfield = new JTextArea("how goes\n    there");

	JCheckBox jcbarrowstyle = new JCheckBox("Arrow Present");

	// these codes could be expended later; they are numbered 0-4 clockwise from bottom left
	String[] nodepos = { "Bottom Left", "Top Left", "Bottom Right", "Top Right", "Bottom Middle", "Top Middle", "Left Middle", "Right Middle" };
	float[] nodeposv = { 0.0F, 1.0F, 3.0F, 2.0F, 3.5F, 1.5F, 0.5F, 2.5F };

	/////////////////////////////////////////////
	void AddFontStyles(String[] labstylenames, Font[] fontlabs, int nlabstylenames)
	{
		for (int i = 0; i < nlabstylenames; i++)
			fontstyles.addItem(labstylenames[i]);

		for (int i = 0; i < nodepos.length; i++)
			jcbnodestyles.addItem(nodepos[i]);
	}


	/////////////////////////////////////////////
	int CodeTextNodePos(float fnodepos)
	{
		int ires = 0;
		for (int i = 0; i < nodeposv.length; i++)
		{
			if (Math.abs(fnodepos - nodeposv[i]) <= Math.abs(fnodepos - nodeposv[ires]))
				ires = i;
			if (fnodepos == nodeposv[ires])
				break;
		}
		if (fnodepos != nodeposv[ires])
			TN.emitWarning("Unmatched TextNode Position " + fnodepos);
		return ires;
	}

	/////////////////////////////////////////////
	ConnectiveLabelTabPane()
	{
		super(new BorderLayout());

		JPanel fsp = new JPanel();
		fsp.add(fontstyles);
		fsp.add(jcbnodestyles);

		JPanel fsps = new JPanel();
		fsps.add(jcbarrowstyle);
		fsps.add(jbcancel);

		add("North", fsp);
		add("Center", labtextfield);
		add("South", fsps);
	}
};


