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

	JButton buttaddfix = new JButton("Add Path Nodes"); 
	
	/////////////////////////////////////////////
    SketchInfoPanel(SketchDisplay lsketchdisplay)
    {
		//Font[] fs = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		//for (int i = 0; i < fs.length; i++)
		//	System.out.println(fs[i].toString());

    	sketchdisplay = lsketchdisplay;
		tapathxml.setFont(new Font("Courier New", Font.PLAIN, 12));

		setLayout(new BorderLayout());
		add(new JScrollPane(tapathxml), BorderLayout.CENTER);

		// path selection numbering (to give a sense of scale)
		JPanel pan1 = new JPanel(new GridLayout(1, 0));
		tfselitempathno.setEditable(false);
		tfselnumpathno.setEditable(false);
		pan1.add(tfselitempathno);
		pan1.add(new JLabel("out of"));
		pan1.add(tfselnumpathno);

		tfmousex.setEditable(false);
		tfmousey.setEditable(false);
		JPanel pan2 = new JPanel(new GridLayout(1, 4));
		pan2.add(new JLabel("X:", JLabel.RIGHT));
		pan2.add(tfmousex);
		pan2.add(new JLabel("Y:", JLabel.RIGHT));
		pan2.add(tfmousey);

		JPanel pand = new JPanel(new GridLayout(0, 1));
		pand.add(buttaddfix); 
		pand.add(pan1);
		pand.add(pan2);
		add(pand, BorderLayout.SOUTH);
		
		buttaddfix.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { AddFixPath();	} } ); 	
	}


	/////////////////////////////////////////////
	void AddFixPath()
	{
		System.out.println("Hi there:" + tapathxml.getText()); 
		String[] nums = tapathxml.getText().split("[\\s,]+"); 
		try
		{
			for (int i = 1; i < nums.length; i += 2)
			{
				float lfixedx = Float.parseFloat(nums[i - 1]);
				float fixedx = TN.CENTRELINE_MAGNIFICATION * (lfixedx - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x); 
				float lfixedy = Float.parseFloat(nums[i]);
				float fixedy = -TN.CENTRELINE_MAGNIFICATION * (lfixedy - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y); 

				if (sketchdisplay.sketchgraphicspanel.bmoulinactive)
				{
					sketchdisplay.sketchgraphicspanel.currgenpath.LineTo(fixedx, fixedy);
					sketchdisplay.sketchgraphicspanel.SetMouseLine(new Point2D.Float(fixedx, fixedy), sketchdisplay.sketchgraphicspanel.moupt);
				}
				else
				{
					OnePathNode fixedpt = new OnePathNode(fixedx, fixedy, 0); 
					fixedpt.SetNodeCloseBefore(sketchdisplay.sketchgraphicspanel.tsketch.vnodes, sketchdisplay.sketchgraphicspanel.tsketch.vnodes.size()); 
					sketchdisplay.sketchgraphicspanel.StartCurve(fixedpt);
				}
			}
		}
		catch (NumberFormatException e)
		{;}

		sketchdisplay.sketchgraphicspanel.repaint();
		
		/*
		String[] bits = coords.split(" ");
		Float fixedx = new Float(bits[0]);
		Float fixedy = new Float(bits[1]);
		System.out.println("Fixing endpath at " + coords);
		OnePathNode fixedpt = new OnePathNode(10*fixedx-10*tsketch.sketchLocOffset.x,-10*fixedy+10*tsketch.sketchLocOffset.y,0); // sic! the mixed signs are confusing, and I only got that by trial and error :-)
		
		if(!bmoulinactive)
		{
			ClearSelection(true);
			fixedpt.SetNodeCloseBefore(tsketch.vnodes, tsketch.vnodes.size());
			StartCurve(fixedpt);
		}
		else
		{
			EndCurve(fixedpt);
		}
*/
	}
	
	/////////////////////////////////////////////
	void SetPathXML(OnePath op)
	{
		try
		{
		op.WriteXML(lospathxml, 0, 0, 0);
		lospathxml.WriteLine(""); 
		if (op.pnstart != null)
			op.pnstart.DumpNodeInfo(lospathxml, "start"); 
		if (op.pnend != null)
			op.pnend.DumpNodeInfo(lospathxml, "end"); 
lospathxml.WriteLine("ciHasrendered=" + op.ciHasrendered); 
if (op.plabedl != null)
	lospathxml.WriteLine("symbc " + op.plabedl.vlabsymb.size() + "<" + op.vpsymbols.size()); 

		lospathxml.WriteLine("kaleft:  " + (op.kaleft != null ? op.kaleft.zalt : "null")); 
		lospathxml.WriteLine("karight: " + (op.karight != null ? op.karight.zalt : "null")); 
		
		tapathxml.setEditable(false);
		buttaddfix.setEnabled(false); 
		tapathxml.setText(lospathxml.sb.toString().replaceAll("\t", "  "));
		lospathxml.sb.setLength(0);
		}
	 	catch (IOException e) {;}
	}

	/////////////////////////////////////////////
	void SetAreaInfo(OneSArea osa, OneSketch tsketch)
	{
		tapathxml.setText("");
		tapathxml.append("Area zalt = ");
		tapathxml.append(String.valueOf(osa.zalt)); 
		tapathxml.append("\n\n");

		for (OnePath op : osa.connpathrootscen)
		{
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				tapathxml.append("connpathrootscen " + op.toStringCentreline() + "\n");
		}

		tapathxml.append("\n"); 
		for (ConnectiveComponentAreas cca : osa.ccalist)
			tapathxml.append("cca vconncomindex=" + tsketch.sksya.vconncom.indexOf(cca) + "  vconnpaths=" + cca.vconnpaths.size() + "\n"); 

		tapathxml.setEditable(false);
		buttaddfix.setEnabled(false); 
	}

	/////////////////////////////////////////////
	void SetCleared()
	{
		tapathxml.setText("");
		tapathxml.setEditable(true);
		buttaddfix.setEnabled(true); 
	}
}




