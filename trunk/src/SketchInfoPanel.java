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
import java.awt.CardLayout;
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

	JTextField tfmousex = new JTextField();
	JTextField tfmousey = new JTextField();
	JTextField tfdistance = new JTextField();
	JTextField tfbearing = new JTextField();

	JTextArea tapathxml = new JTextArea("");
	LineOutputStream lospathxml = new LineOutputStream();

	JButton buttaddfix = new JButton("New nodes"); 
	JButton buttsearch = new JButton("Search"); 
	JTextField tfenterfield = new JTextField();

    CardLayout vcardlayout = new CardLayout(); 
    JPanel pancards = new JPanel(vcardlayout); 
	
	/////////////////////////////////////////////
    SketchInfoPanel(SketchDisplay lsketchdisplay)
    {
		//Font[] fs = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		//for (int i = 0; i < fs.length; i++)
		//	System.out.println(fs[i].toString());

    	sketchdisplay = lsketchdisplay;

		buttaddfix.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { AddFixPath(); } } ); 	
        buttaddfix.setToolTipText("Convert a comma separated list of coordinates to a path"); 

		buttsearch.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { SearchLabels(); } } ); 	
        buttsearch.setToolTipText("Search for labels in sketch"); 

		tapathxml.setFont(new Font("Courier New", Font.PLAIN, 12));
        tapathxml.setEditable(false); 

		tfmousex.setEditable(false);
		tfmousey.setEditable(false);
		tfdistance.setEditable(false);
		tfbearing.setEditable(false);

		Insets inset = new Insets(1, 1, 1, 1);
		buttaddfix.setMargin(inset);
		buttsearch.setMargin(inset);

        // selpathxml card
        pancards.add(new JScrollPane(tapathxml), "selpathxml"); 

        // searchopt card
        JPanel pan1 = new JPanel(new GridLayout(1, 2)); 
		pan1.add(buttaddfix); 
        pan1.add(buttsearch); 
        JPanel pan3 = new JPanel(new GridLayout(2, 1)); 
        pan3.add(tfenterfield); 
        pan3.add(pan1); 

        JPanel pansearch = new JPanel(new BorderLayout()); 
        pansearch.add(pan3, BorderLayout.SOUTH); 
        pancards.add(pansearch, "searchopt"); 

        // bottom part
        JPanel pan2 = new JPanel(new GridLayout(2, 4)); 
		pan2.add(new JLabel("X:", JLabel.RIGHT));
		pan2.add(tfmousex);
		pan2.add(new JLabel("Y:", JLabel.RIGHT));
		pan2.add(tfmousey);

		pan2.add(new JLabel("Dist:", JLabel.RIGHT));
		pan2.add(tfdistance);
		pan2.add(new JLabel("Bearing:", JLabel.RIGHT));
		pan2.add(tfbearing);

        // main pane layout
		setLayout(new BorderLayout());
        add(pancards, BorderLayout.CENTER); 
		add(pan2, BorderLayout.SOUTH);
	}


	/////////////////////////////////////////////
    void SearchLabels()
    {
        TN.emitWarning("Searching: " + tfenterfield.getText()); 
    }

	/////////////////////////////////////////////
	void AddFixPath()
	{
		System.out.println("Hi there:" + tfenterfield.getText()); 
		String[] nums = tfenterfield.getText().split("[\\s,]+"); 
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
	void SetPathXML(OnePath op, Vec3 sketchLocOffset)
	{
        vcardlayout.show(pancards, "selpathxml"); 
		try
		{
		op.WriteXMLpath(lospathxml, 0, 0, 0);
		lospathxml.WriteLine(""); 
		if (op.pnstart != null)
			op.pnstart.DumpNodeInfo(lospathxml, "start", sketchLocOffset); 
		if (op.pnend != null)
			op.pnend.DumpNodeInfo(lospathxml, "end", sketchLocOffset); 

        lospathxml.WriteLine("ciHasrendered=" + op.ciHasrendered); 
        if (op.plabedl != null)
	       lospathxml.WriteLine("symbc " + op.plabedl.vlabsymb.size() + "<" + op.vpsymbols.size()); 

		lospathxml.WriteLine("kaleft:  " + (op.kaleft != null ? op.kaleft.zalt + sketchLocOffset.z : "null")); 
		lospathxml.WriteLine("karight: " + (op.karight != null ? op.karight.zalt + sketchLocOffset.z : "null")); 

        int iselpath = sketchdisplay.sketchgraphicspanel.tsketch.vpaths.indexOf(op); // slow; (maybe not necessary)
		lospathxml.WriteLine("Path " + (iselpath+1) + " out of " + String.valueOf(sketchdisplay.sketchgraphicspanel.tsketch.vpaths.size())); 
		
		tapathxml.setText(lospathxml.sb.toString().replaceAll("\t", "  "));
		lospathxml.sb.setLength(0);
		}
	 	catch (IOException e) {;}
	}

	/////////////////////////////////////////////
	void SetAreaInfo(OneSArea osa, OneSketch tsketch)
	{
        vcardlayout.show(pancards, "selpathxml"); 
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

		tapathxml.append("\n"); 

        int iselarea = 0; // tsketch.vsareas.indexOf(osa) doesn't exist
        for (OneSArea losa : sketchdisplay.sketchgraphicspanel.tsketch.vsareas)
        {
            if (losa == osa)
                break; 
            iselarea++; 
        }
		tapathxml.append("Area " + (iselarea+1) + " out of " + String.valueOf(sketchdisplay.sketchgraphicspanel.tsketch.vsareas.size())); 
	}

	/////////////////////////////////////////////
	void SetCleared()
	{
        vcardlayout.show(pancards, "searchopt"); 
		tapathxml.setText("");
	}
}




