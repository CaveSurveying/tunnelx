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
import java.awt.geom.AffineTransform;



/////////////////////////////////////////////
class ConnectiveAreaSigTabPane extends JPanel
{
	JComboBox areasignals = new JComboBox();
	JButton jbcancel = new JButton("Cancel Area-signal");

	// we can choose to print just one view on one sheet of paper
	JButton tfxtransscale = new JButton("Scale:"); 
	JTextField tfscale = new JTextField();
	JTextField tfrotatedeg = new JTextField();
	JButton tfxtranscenbutt = new JButton("X-translate:"); 
	JTextField tfxtrans = new JTextField();
	JButton tfytranscenbutt = new JButton("Y-translate:"); 
	JTextField tfytrans = new JTextField();
	JButton tfsketchcopybutt = new JButton("Sketch:"); 
	JTextField tfsketch = new JTextField();
	OneSketch tfsketch_store = null; // the pointer to the sketch above, for easier updating
	
	JButton tfsubstylecopybutt = new JButton("Style:"); 
	JTextField tfsubstyle = new JTextField();
	SketchLineStyle sketchlinestyle; 

	JTextField tfzsetrelative = new JTextField();

	/////////////////////////////////////////////
	void SketchCopyButt()
	{
		OneSketch asketch = sketchlinestyle.sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchLoad(); 
		if (asketch == null)
			return; 
			
		OneSketch tsketch = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch; 
		OneTunnel atunnel = asketch.sketchtunnel; 
		String st = asketch.sketchfile.getName(); 
		while (atunnel != tsketch.sketchtunnel)
		{
			st = atunnel.name + TN.PathDelimeter + st; 
			atunnel = atunnel.uptunnel; 
			if (atunnel == null)
			{
				TN.emitWarning("frame sketch must be in tree"); // so we can make this relative map to it
				return; 
			}
		}		

		tfsketch.setText(st); 
		tfsketch_store = asketch; 
		sketchlinestyle.GoSetParametersCurrPath();
	}
	
	/////////////////////////////////////////////
	void StyleCopyButt()
	{
		SubsetAttrStyle sascurrent = sketchlinestyle.sketchdisplay.subsetpanel.sascurrent; 
		if (sascurrent != null)
			tfsubstyle.setText(sascurrent.stylename); 
		sketchlinestyle.GoSetParametersCurrPath();
	}

	
	/////////////////////////////////////////////
	void TransCenButt(int typ)
	{
		// find the area which this line corresponds to.  (have to search the areas to find it).  
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath; 
		if (op == null)
			return; 
		OneSArea osa = op.karight; 
		if ((op.plabedl == null) || (op.plabedl.pframesketch == null))
			return; 

		Rectangle2D areabounds = osa.rboundsarea; 
		Rectangle2D rske = op.plabedl.pframesketch.getBounds(false, false); 

		// generate the tail set of transforms in order
		double lrealpaperscale = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale; 
		AffineTransform aftrans = new AffineTransform(); 
		if ((typ != 0) && (op.plabedl.sfscaledown != 0.0))
			aftrans.scale(lrealpaperscale / op.plabedl.sfscaledown, lrealpaperscale / op.plabedl.sfscaledown);
		if (op.plabedl.sfrotatedeg != 0.0)  
			aftrans.rotate(-op.plabedl.sfrotatedeg * Math.PI / 180);	
		aftrans.translate(op.plabedl.pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -op.plabedl.pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION); 
		rske = aftrans.createTransformedShape(rske).getBounds(); 

		if (typ == 0)
		{
			double lscale = Math.max(rske.getWidth() / areabounds.getWidth(), rske.getHeight() / areabounds.getHeight()) * lrealpaperscale; 
			tfscale.setText(lscale > 100.0 ? String.valueOf((int)lscale) : String.valueOf((float)lscale)); 
		}
		else
		{
			double cx = rske.getX() + rske.getWidth() / 2; 
			double cy = rske.getY() + rske.getHeight() / 2; 

			double dcx = areabounds.getX() + areabounds.getWidth() / 2; 
			double dcy = areabounds.getY() + areabounds.getHeight() / 2; 

			Vec3 lsketchLocOffset = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset; 	
			//dcx = cx + sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION
			double lsfxtrans = ((dcx - cx) / TN.CENTRELINE_MAGNIFICATION + lsketchLocOffset.x) / lrealpaperscale;
			double lsfytrans = ((dcy - cy) / TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.y) / lrealpaperscale;
			if (typ == 1)
				tfxtrans.setText(String.valueOf((float)lsfxtrans)); 
			else
				tfytrans.setText(String.valueOf((float)lsfytrans)); 
		}

		sketchlinestyle.GoSetParametersCurrPath();
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
		pimpfields.add(new JLabel("Rotate:", JLabel.RIGHT));
		pimpfields.add(tfrotatedeg);
		pimpfields.add(tfxtransscale);
		pimpfields.add(tfscale);
		pimpfields.add(tfxtranscenbutt);
		pimpfields.add(tfxtrans);
		pimpfields.add(tfytranscenbutt);
		pimpfields.add(tfytrans);
		pimpfields.add(tfsketchcopybutt);
		pimpfields.add(tfsketch);
		pimpfields.add(tfsubstylecopybutt);
		pimpfields.add(tfsubstyle);
		pimpfields.add(new JLabel("Z Relative:", JLabel.RIGHT)); 
		pimpfields.add(tfzsetrelative); 

		add("Center", pimpfields);

		tfsketchcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SketchCopyButt(); } } );
		tfsubstylecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleCopyButt(); } } );
		tfxtransscale.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(0); } } );
		tfxtranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(1); } } );
		tfytranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(2); } } );
	}

	/////////////////////////////////////////////
	void UpdateAreaSignals(String[] areasignames, int nareasignames)
	{
		areasignals.removeAllItems();
		for (int i = 0; i < nareasignames; i++)
			areasignals.addItem(areasignames[i]);
	}
};


