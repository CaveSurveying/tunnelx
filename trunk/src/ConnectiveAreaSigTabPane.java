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
	JButton tfxrotatedeg = new JButton("Rotate:"); 
	JTextField tfrotatedeg = new JTextField();
	JButton tfxtransscale = new JButton("Scale:"); 
	JTextField tfscale = new JTextField();
 	JButton tfxtranscenbutt = new JButton("X-translate:"); 
	JTextField tfxtrans = new JTextField();
	JButton tfytranscenbutt = new JButton("Y-translate:"); 
	JTextField tfytrans = new JTextField();
	JButton tfsketchcopybutt = new JButton("Sketch:"); 
	JTextField tfsketch = new JTextField();
	
	JButton tfsubstylecopybutt = new JButton("Style:"); 
	JTextField tfsubstyle = new JTextField();
	SketchLineStyle sketchlinestyle; 

	JTextField tfzsetrelative = new JTextField();

	// it might be necessary to back-up the initial value as well, so we wind up cycling through three values
	String saverotdeg = "0.0"; 
	String savescale = "1000.0"; 
	String savextrans = "0.0"; 
	String saveytrans = "0.0"; 
	String savesketch = ""; 
	String savesubstyle = ""; 

	/////////////////////////////////////////////
	void SketchCopyButt()
	{
		OneSketch asketch = sketchlinestyle.sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchLoad(); 
		String st = ""; 
		if (asketch != null)
		{
			OneSketch tsketch = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch; 
			OneTunnel atunnel = asketch.sketchtunnel; 
			st = asketch.sketchfile.getSketchName(); 
			while (atunnel != tsketch.sketchtunnel)
			{
				st = atunnel.name + "/" + st; 
				atunnel = atunnel.uptunnel; 
				if (atunnel == null)
				{
					TN.emitWarning("selected frame sketch must be in tree"); // so we can make this relative map to it
					st = "";
					break;  
				}
			}		
		}
		if (st.equals("") || (tfsketch.getText().equals(st) && !savesketch.equals("")))
			st = savesketch; 
		else if (savesketch.equals(""))
			savesketch = st; 
		tfsketch.setText(st); 
		sketchlinestyle.GoSetParametersCurrPath();
	}
	
	/////////////////////////////////////////////
	void StyleCopyButt()
	{
		SubsetAttrStyle sascurrent = sketchlinestyle.sketchdisplay.subsetpanel.sascurrent; 
		if ((sascurrent != null) && tfsubstyle.getText().equals(savesubstyle))
			tfsubstyle.setText(sascurrent.stylename); 
		else
			tfsubstyle.setText(savesubstyle); 
		sketchlinestyle.GoSetParametersCurrPath();
	}

	
	/////////////////////////////////////////////
	void TransCenButt(int typ)
	{
		// find the area which this line corresponds to.  (have to search the areas to find it).  
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath; 
		if ((op == null) || (op.plabedl == null))
			return; 
		OneSArea osa = op.karight; 
		String sval = ""; 
		if ((op.plabedl.pframesketch != null) && (osa != null))
		{
			Rectangle2D areabounds = osa.rboundsarea; 
			Rectangle2D rske = op.plabedl.pframesketch.getBounds(false, false); 

			// generate the tail set of transforms in order
			double lrealpaperscale = sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale; 
			AffineTransform aftrans = new AffineTransform(); 
			if ((typ != 0) && (op.plabedl.sfscaledown != 0.0))
				aftrans.scale(lrealpaperscale / op.plabedl.sfscaledown, lrealpaperscale / op.plabedl.sfscaledown);
			if (op.plabedl.sfrotatedeg != 0.0)  
				aftrans.rotate(-Math.toRadians(op.plabedl.sfrotatedeg));	
			aftrans.translate(op.plabedl.pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -op.plabedl.pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION); 
			rske = aftrans.createTransformedShape(rske).getBounds(); 

			if (typ == 0)
			{
				double lscale = Math.max(rske.getWidth() / areabounds.getWidth(), rske.getHeight() / areabounds.getHeight()) * lrealpaperscale; 
				if (lscale > 100.0)
					lscale = Math.ceil(lscale); 
				sval = Float.toString((float)lscale); 
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
					sval = String.valueOf((float)lsfxtrans); 
				else
					sval = String.valueOf((float)lsfytrans); 
			}
		}
		else
			TN.emitWarning("Need to make areas in this sketch first for this button to work"); 

		if (typ == 0)
		{
			if (sval.equals("") || !tfscale.getText().equals(savescale))
				sval = savescale; 
			tfscale.setText(sval); 
		}
		else if (typ == 1)
		{
			if (sval.equals("") || !tfxtrans.getText().equals(savextrans))
				sval = savextrans; 
			tfxtrans.setText(sval); 
		}
		else
		{
			if (sval.equals("") || !tfytrans.getText().equals(saveytrans))
				sval = saveytrans; 
			tfytrans.setText(sval); 
		}
		sketchlinestyle.GoSetParametersCurrPath();
	}
	
	
	/////////////////////////////////////////////
	ConnectiveAreaSigTabPane(SketchLineStyle lsketchlinestyle)
	{
		super(new BorderLayout());
		sketchlinestyle = lsketchlinestyle; 
		
		JPanel ntop = new JPanel(new BorderLayout());
		ntop.add(new JLabel("Area Signals", JLabel.CENTER), BorderLayout.NORTH);

		JPanel pie = new JPanel();
		pie.add(areasignals);
		pie.add(jbcancel);
		ntop.add(pie, BorderLayout.CENTER);

		add(ntop, BorderLayout.NORTH);

		JPanel pimpfields = new JPanel(new GridLayout(0, 2));
		pimpfields.add(tfxrotatedeg);
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

		add(pimpfields, BorderLayout.CENTER);

		tfxrotatedeg.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { tfrotatedeg.setText(saverotdeg);  sketchlinestyle.GoSetParametersCurrPath(); } } );
		tfxtransscale.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(0); } } );
		tfxtranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(1); } } );
		tfytranscenbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(2); } } );
		tfsketchcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SketchCopyButt(); } } );
		tfsubstylecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleCopyButt(); } } );
	}

	/////////////////////////////////////////////
	void UpdateAreaSignals(String[] areasignames, int nareasignames)
	{
		areasignals.removeAllItems();
		for (int i = 0; i < nareasignames; i++)
			areasignals.addItem(areasignames[i]);
	}

	/////////////////////////////////////////////
	void SetFrameSketchInfoText(OnePath op)
	{ 
		boolean bsketchframe = ((op != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME)); 
		boolean bnodeconnzrelative = ((op != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE)); 
		
		if (bsketchframe)
		{
			tfscale.setText(Float.toString(op.plabedl.sfscaledown)); 
			tfrotatedeg.setText(String.valueOf(op.plabedl.sfrotatedeg)); 
			tfxtrans.setText(Float.toString(op.plabedl.sfxtrans)); 
			tfytrans.setText(Float.toString(op.plabedl.sfytrans)); 
			tfsketch.setText(op.plabedl.sfsketch);
			tfsubstyle.setText(op.plabedl.sfstyle);
		}
		else
		{
			if (!tfrotatedeg.getText().trim().equals(""))
				saverotdeg = tfrotatedeg.getText(); 
			tfrotatedeg.setText(""); 
			if (!tfscale.getText().trim().equals(""))
				savescale = tfscale.getText(); 
			tfscale.setText(""); 
			if (!tfxtrans.getText().trim().equals(""))
				savextrans = tfxtrans.getText(); 
			tfxtrans.setText(""); 
			if (!tfytrans.getText().trim().equals(""))
				saveytrans = tfytrans.getText(); 
			tfytrans.setText(""); 
			if (!tfsketch.getText().trim().equals(""))
				savesketch = tfsketch.getText(); 
			tfsketch.setText(""); 
			if (!tfsubstyle.getText().trim().equals(""))
				savesubstyle = tfsubstyle.getText(); 
			tfsubstyle.setText(""); 
		}
		
		if (bnodeconnzrelative)
			tfzsetrelative.setText(String.valueOf(op.plabedl.nodeconnzsetrelative)); 
		else
			tfzsetrelative.setText(""); 

		tfscale.setEditable(bsketchframe); 
		tfrotatedeg.setEditable(bsketchframe); 
		tfxtranscenbutt.setEnabled(bsketchframe); 
		tfxtrans.setEditable(bsketchframe); 
		tfytranscenbutt.setEnabled(bsketchframe); 
		tfytrans.setEditable(bsketchframe); 
		tfsketchcopybutt.setEnabled(bsketchframe); 
		tfsketch.setEditable(bsketchframe); 
		tfsubstylecopybutt.setEnabled(bsketchframe); 
		tfsubstyle.setEditable(bsketchframe); 
		tfzsetrelative.setEditable(bnodeconnzrelative); 
	}
};


