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
import javax.swing.JCheckBox;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import java.awt.geom.Point2D;
import java.awt.Color;


/////////////////////////////////////////////
class SketchZTiltPanel extends JPanel
{
	SketchDisplay sketchdisplay;
	JButton buttsomething = new JButton("Something"); 
	JCheckBox cbaShowTilt;
    JCheckBox cbaThinZheightsel; 

	/////////////////////////////////////////////
    SketchZTiltPanel(SketchDisplay lsketchdisplay)
    {
    	sketchdisplay = lsketchdisplay;

        cbaShowTilt = new JCheckBox("Show Tilted in z", false);
		cbaShowTilt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miShowTilt.isSelected() != cbaShowTilt.isSelected())
                    sketchdisplay.miShowTilt.doClick();
			} } );
        cbaThinZheightsel = new JCheckBox("Thin Z Selection", false);
		cbaThinZheightsel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miThinZheightsel.isSelected() != cbaThinZheightsel.isSelected())
                    sketchdisplay.miThinZheightsel.doClick();
			} } );

		JPanel panuppersec = new JPanel(new GridLayout(0, 2));
        panuppersec.add(cbaThinZheightsel); 
        panuppersec.add(cbaShowTilt);
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselWiden)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltOver));
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselNarrow)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltBack));
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvUpright)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneDown)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneUp)); 

    
		setLayout(new BorderLayout());
		add(panuppersec, BorderLayout.SOUTH);
	}
}


