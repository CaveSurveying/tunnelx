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
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import java.awt.Font;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/////////////////////////////////////////////
class ConnectiveLabelTabPane extends JPanel
{
	JComboBox fontstyles = new JComboBox();
//JComboBox jcbnodestyles = new JComboBox();

	JButton jbcancel = new JButton("Cancel Label");
	JTextArea labtextfield = new JTextArea("how goes\n    there");

	JCheckBox jcbarrowpresent = new JCheckBox("Arrow");
	JCheckBox jcbboxpresent = new JCheckBox("Box");

	// the positioning of the text
	JTextField tfxrel = new JTextField();
	JTextField tfyrel = new JTextField();

	// these codes could be expended later; they are numbered 0-4 clockwise from bottom left
	static String[] rppos = { "-1", "0", "1" };

	/////////////////////////////////////////////
	class radbut implements ActionListener
	{
		String xrel;
		String yrel;

		radbut(int ipos)
		{
			xrel = rppos[(ipos % 3)];
			yrel = rppos[2 - (ipos / 3)];
		}
		public void actionPerformed(ActionEvent e)
		{
			tfxrel.setText(xrel);
			tfyrel.setText(yrel);
			tfxrel.postActionEvent();
		}
	}

	ButtonGroup buttgroup = new ButtonGroup();
	JRadioButton[] rbposes = new JRadioButton[10];
	radbut[] radbuts = new radbut[9];


	/////////////////////////////////////////////
	void AddFontStyles(LabelFontAttr[] labstylenames, int nlabstylenames)
	{
		for (int i = 0; i < nlabstylenames; i++)
			fontstyles.addItem(labstylenames[i].labelfontname + " (" + labstylenames[i].subsetattr.subsetname + ")");
	}

	/////////////////////////////////////////////
	void setTextPosCoords(float fxrel, float fyrel)
	{
		int ix = (fxrel == -1.0F ? 0 : (fxrel == 0.0F ? 1 : (fxrel == 1.0F ? 2 : -1)));
		int iy = (fyrel == -1.0F ? 2 : (fyrel == 0.0F ? 1 : (fyrel == 1.0F ? 0 : -1)));
		if ((ix != -1) && (iy != -1))
		{
			rbposes[ix + iy * 3].setSelected(true);
			tfxrel.setText(rppos[ix]);
			tfyrel.setText(rppos[2 - iy]);
		}
		else
		{
			rbposes[9].setSelected(true);
			tfxrel.setText(String.valueOf(fxrel));
			tfyrel.setText(String.valueOf(fyrel));
		}
	}




	/////////////////////////////////////////////
	ConnectiveLabelTabPane()
	{
		super(new BorderLayout());

		JPanel fsp = new JPanel(new GridLayout(1, 3));

		JPanel fsp1 = new JPanel(new GridLayout(2, 1));
		fsp1.add(jcbarrowpresent);
		fsp1.add(jcbboxpresent);
		fsp.add(fsp1);

		JPanel fsp3 = new JPanel(new GridLayout(3, 3));
		for (int i = 0; i < 10; i++)
		{
			rbposes[i] = new JRadioButton("");
			buttgroup.add(rbposes[i]);
			if (i != 9)
			{
				radbuts[i] = new radbut(i);
				rbposes[i].addActionListener(radbuts[i]);
				fsp3.add(rbposes[i]);
			}
		}
		fsp.add(fsp3);

		JPanel fsp2 = new JPanel(new GridLayout(2, 1));
		fsp2.add(tfxrel);
		fsp2.add(tfyrel);
		fsp.add(fsp2);


		JPanel fsps = new JPanel();
		fsps.add(fontstyles);
		fsps.add(jbcancel);

		add("North", fsp);
		add("Center", labtextfield);
		add("South", fsps);
	}
};


