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

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Font;

import java.util.Vector;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import java.awt.geom.Point2D;

/////////////////////////////////////////////
class SketchInfoPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JTextField tfselitempathno = new JTextField();
	JTextField tfselnumpathno = new JTextField();

	JTextField tfmousex = new JTextField();
	JTextField tfmousey = new JTextField();

	JTextArea tapathxml = new JTextArea("");
	LineOutputStream lospathxml = new LineOutputStream();

	/////////////////////////////////////////////
    SketchInfoPanel(SketchDisplay lsketchdisplay)
    {
		//Font[] fs = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		//for (int i = 0; i < fs.length; i++)
		//	System.out.println(fs[i].toString());

    	sketchdisplay = lsketchdisplay;
        tapathxml.setEditable(false);
		tapathxml.setFont(new Font("Courier New", Font.PLAIN, 12));

		setLayout(new BorderLayout());
		add("Center", new JScrollPane(tapathxml));

		// path selection numbering (to give a sense of scale)
		JPanel pan1 = new JPanel(new GridLayout(1, 0));
		tfselitempathno.setEditable(false);
		tfselnumpathno.setEditable(false);
		pan1.add(tfselitempathno);
		pan1.add(new JLabel("paths/"));
		pan1.add(tfselnumpathno);

		tfmousex.setEditable(false);
		tfmousey.setEditable(false);
		JPanel pan2 = new JPanel(new GridLayout(1, 4));
		pan2.add(new JLabel("X:", JLabel.RIGHT));
		pan2.add(tfmousex);
		pan2.add(new JLabel("Y:", JLabel.RIGHT));
		pan2.add(tfmousey);

		JPanel pand = new JPanel(new GridLayout(0, 1));
		pand.add(pan1);
		pand.add(pan2);
		add("South", pand);
    }


	/////////////////////////////////////////////
	void SetPathXML(OnePath op)
	{
		if (op != null)
		{
			try
			{
			op.WriteXML(lospathxml, 0, 0, 0);
			tapathxml.setText(lospathxml.sb.toString().replaceAll("\t", "  "));
			lospathxml.sb.setLength(0);
			}
		 	catch (IOException e) {;}
 		}
		else
			tapathxml.setText("");
	}
}




