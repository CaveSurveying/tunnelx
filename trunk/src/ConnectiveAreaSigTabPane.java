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
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;



/////////////////////////////////////////////
class ConnectiveAreaSigTabPane extends JPanel
{
	JComboBox areasignals = new JComboBox();
	JButton jbcancel = new JButton("Cancel Area-signal");

	// we can choose to print just one view on one sheet of paper
	JTextField tfscale = new JTextField();
	JTextField tfrotatedeg = new JTextField();
	JButton tfxtranscenbutt = new JButton("X-translate:"); 
	JTextField tfxtrans = new JTextField();
	JButton tfytranscenbutt = new JButton("Y-translate:"); 
	JTextField tfytrans = new JTextField();
	JButton tfsketchcopybutt = new JButton("Sketch:"); 
	JTextField tfsketch = new JTextField();
	JButton tfsubstylecopybutt = new JButton("Style:"); 
	JTextField tfsubstyle = new JTextField();

	SketchLineStyle sketchlinestyle; 

	/////////////////////////////////////////////
	void SketchCopyButt()
	{
		String st = sketchlinestyle.sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchPath(sketchlinestyle.sketchdisplay.sketchgraphicspanel.activetunnel); 
		if (st != null)
			tfsketch.setText(st); 
	}
	
	/////////////////////////////////////////////
	void StyleCopyButt()
	{
		SubsetAttrStyle sascurrent = sketchlinestyle.sketchdisplay.subsetpanel.sascurrent; 
		if (sascurrent != null)
			tfsubstyle.setText(sascurrent.stylename); 
	}

	/////////////////////////////////////////////
	void TransCenButt(boolean bX)
	{
		// find the area which this line corresponds to.  (have to search the areas to find it).  
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath; 
		OneSketch os = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch; 
		OneSArea osa = null; 
		for (int i = 0; i < os.vsareas.size(); i++)
		{
			OneSArea losa = (OneSArea)os.vsareas.elementAt(i); 
			if (losa.pldframesketch == op.plabedl)
				osa = losa; 
		}
		System.out.println("ososososos    " + osa); 
		if ((osa == null) || (osa.pframesketch == null)) 
			return; 

		Rectangle2D rske = osa.pframesketch.getBounds(false, false); 
		// (consider the rotation)
		double smid = ((bX ? rske.getX() : rske.getY()) + (bX ? rske.getWidth() : rske.getHeight()) * 0.5) / (osa.pldframesketch.sfscaledown != 0.0 ? osa.pldframesketch.sfscaledown : 1.0); 
		double amid = (bX ? osa.rboundsarea.getX() : osa.rboundsarea.getY()) + (bX ? osa.rboundsarea.getWidth() : osa.rboundsarea.getHeight()) * 0.5; 
		(bX ? tfxtrans : tfytrans).setText(String.valueOf(amid - smid - (bX ? osa.rboundsarea.getX() : osa.rboundsarea.getY()))); 
//System.out.println(amid); 
//System.out.println(); 
	}
	
	
	/////////////////////////////////////////////
	ConnectiveAreaSigTabPane(SketchLineStyle lsketchlinestyle)
	{
		super(new BorderLayout());
		sketchlinestyle = lsketchlinestyle; 
		
		JPanel ntop = new JPanel(new BorderLayout());
		ntop.add("North", new JLabel("Area Signals", JLabel.CENTER));

		JPanel pie = new JPanel();
		pie.add(areasignals);
		pie.add(jbcancel);
		ntop.add("Center", pie);

		add("North", ntop);

		JPanel pimpfields = new JPanel(new GridLayout(0, 2));
		pimpfields.add(new JLabel("Scale down:", JLabel.RIGHT));
		pimpfields.add(tfscale);
		pimpfields.add(new JLabel("Rotate:", JLabel.RIGHT));
		pimpfields.add(tfrotatedeg);
		pimpfields.add(tfxtranscenbutt);
		pimpfields.add(tfxtrans);
		pimpfields.add(tfytranscenbutt);
		pimpfields.add(tfytrans);
		pimpfields.add(tfsketchcopybutt);
		pimpfields.add(tfsketch);
		pimpfields.add(tfsubstylecopybutt);
		pimpfields.add(tfsubstyle);

		add("Center", pimpfields);


		tfsketchcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SketchCopyButt(); } } );
		tfsubstylecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleCopyButt(); } } );
		tfxtranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(true); } } );
		tfytranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(false); } } );
	}

	/////////////////////////////////////////////
	void UpdateAreaSignals(String[] areasignames, int nareasignames)
	{
		areasignals.removeAllItems();
		for (int i = 0; i < nareasignames; i++)
			areasignals.addItem(areasignames[i]);
	}
};


