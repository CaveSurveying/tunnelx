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
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.Insets;

import java.util.Map;


/////////////////////////////////////////////
class ConnectiveAreaSigTabPane extends JPanel
{
	JComboBox areasignals = new JComboBox();
	SketchLineStyle sketchlinestyle;
	JButton jbcancel = new JButton("Cancel Area-signal");

	// we can choose to print just one view on one sheet of paper
	JButton tfmaxbutt = new JButton("Max");
	JButton tfcentrebutt = new JButton("Centre");

	JButton tfsketchcopybutt = new JButton("Sketch");

	JButton tfsubstylecopybutt = new JButton("Style");

	JButton tfsubmappingcopybutt = new JButton("Copy");
	JButton tfsubmappingpastebutt = new JButton("Paste");

	JButton tfsetsubsetlower = new JButton("S-lower");
	JButton tfsetsubsetupper = new JButton("S-upper");

	JTextArea tfsubmapping = new JTextArea();
	JScrollPane jsp = new JScrollPane(tfsubmapping);

	// this is used to modify the treeview which we see
	// it's a bad hidden modal thing, but for now till we think of something better.
	SketchFrameDef sketchframedefCopied = new SketchFrameDef(); 

	// use these for parsing the text in the submapping textarea
	TunnelXMLparse txp = new TunnelXMLparse(null);
	TunnelXML tunnXML = new TunnelXML();

	String copiedsubmapping = "";

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

		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		op.plabedl.sketchframedef.sfsketch = st;
		UpdateSFView(op, true);
	}

	/////////////////////////////////////////////
	void CopyBackgroundSketchTransform(String st, AffineTransform lat, Vec3 lsketchLocOffset)
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		op.plabedl.sketchframedef.sfsketch = st;
		op.plabedl.sketchframedef.ConvertSketchTransform(lat, lsketchLocOffset);
		UpdateSFView(op, true);
	}

	/////////////////////////////////////////////
	void StyleCopyButt()
	{
		SubsetAttrStyle sascurrent = sketchlinestyle.sketchdisplay.subsetpanel.sascurrent;
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		op.plabedl.sketchframedef.sfstyle = sascurrent.stylename;
		UpdateSFView(op, true);
	}

	/////////////////////////////////////////////
	void UpdateSFView(OnePath op, boolean bsketchchanged)
	{
		tfsubmapping.setText(op.plabedl.sketchframedef.GetToTextV());

		op.SetSubsetAttrs(sketchlinestyle.sketchdisplay.subsetpanel.sascurrent, sketchlinestyle.sketchdisplay.vgsymbols, null); // font changes
		op.plabedl.sketchframedef.SetSketchFrameFiller(sketchlinestyle.sketchdisplay.sketchgraphicspanel.activetunnel, sketchlinestyle.sketchdisplay.mainbox, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset);

		sketchlinestyle.sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		if (bsketchchanged)
			sketchlinestyle.sketchdisplay.sketchgraphicspanel.SketchChanged(bsketchchanged ? SketchGraphics.SC_CHANGE_STRUCTURE : SketchGraphics.SC_CHANGE_SAS);
	}

	/////////////////////////////////////////////
	void LoadSketchFrameDef(SketchFrameDef lsketchframedefCopied)
	{
		sketchframedefCopied.copy(lsketchframedefCopied); 
		sketchlinestyle.sketchdisplay.subsetpanel.SubsetSelectionChanged(); 
//		sketchlinestyle.sketchdisplay.subsetpanel.sascurrent.TreeListFrameDefCopiedSubsets(sketchframedefCopied); 
//		sketchlinestyle.sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SAS);
	}
	
	/////////////////////////////////////////////
	void StyleMappingCopyButt(boolean bcopypaste)
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		txp.sketchframedef.submapping.clear();
		if (bcopypaste)
		{
			copiedsubmapping = tfsubmapping.getText();
			tfsubmappingpastebutt.setToolTipText(copiedsubmapping);
		}

		String erm = tunnXML.ParseString(txp, (bcopypaste ? tfsubmapping.getText() : copiedsubmapping));

		// if successful, copy it into the path and then back
		if (erm == null)
		{
			op.plabedl.sketchframedef.copy(txp.sketchframedef);
			UpdateSFView(op, !bcopypaste);

			if (bcopypaste)
				LoadSketchFrameDef(op.plabedl.sketchframedef); 
		}
		else
			TN.emitWarning("Failed to parse: " + erm);
	}


	/////////////////////////////////////////////
	void TransCenButt(boolean bmaxcen)
	{
		// find the area which this line corresponds to.  (have to search the areas to find it).
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || (op.plabedl == null) || (op.plabedl.sketchframedef == null))
			return;
		OneSArea osa = (op.karight != null ? op.karight : op.kaleft);
		if ((op.plabedl.sketchframedef.pframesketch == null) || (osa == null))
		{
			TN.emitWarning("Need to make areas in this sketch first for this button to work");
			return;
		}
		op.plabedl.sketchframedef.TransCenButtF(bmaxcen, osa, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset);
		UpdateSFView(op, true);
	}


	/////////////////////////////////////////////
	ConnectiveAreaSigTabPane(SketchLineStyle lsketchlinestyle)
	{
		super(new BorderLayout());
		sketchlinestyle = lsketchlinestyle;

		txp.sketchframedef = new SketchFrameDef(); 
		
		JPanel ntop = new JPanel(new BorderLayout());
		ntop.add(new JLabel("Area Signals", JLabel.CENTER), BorderLayout.NORTH);

		JPanel pie = new JPanel();
		pie.add(areasignals);
		pie.add(jbcancel);
		ntop.add(pie, BorderLayout.CENTER);

		JPanel pimpfields = new JPanel(new GridLayout(0, 4));
		pimpfields.add(tfmaxbutt);
		pimpfields.add(tfcentrebutt);

		pimpfields.add(tfsketchcopybutt);
		pimpfields.add(tfsubstylecopybutt);

		pimpfields.add(tfsubmappingcopybutt);
		pimpfields.add(tfsubmappingpastebutt);

		pimpfields.add(tfsetsubsetlower);
		pimpfields.add(tfsetsubsetupper);
		
		ntop.add(pimpfields, BorderLayout.SOUTH);
		add(ntop, BorderLayout.NORTH);

		add(jsp, BorderLayout.CENTER);

		tfmaxbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(true); } } );
		tfcentrebutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { TransCenButt(false); } } );

		tfsketchcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SketchCopyButt(); } } );
		tfsubstylecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleCopyButt(); } } );
		tfsubmappingcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleMappingCopyButt(true); } } );
		tfsubmappingpastebutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleMappingCopyButt(false); } } );
		
		tfsetsubsetlower.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SetSubsetLoHi(true); } } );
		tfsetsubsetupper.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SetSubsetLoHi(false); } } );

		Insets smallbuttinsets = new Insets(2, 3, 2, 3);
		tfmaxbutt.setToolTipText("Maximize viewed sketch in framed area");
		tfmaxbutt.setMargin(smallbuttinsets);
		tfcentrebutt.setToolTipText("Centre viewed sketch in framed area");
		tfcentrebutt.setMargin(smallbuttinsets);
		tfsketchcopybutt.setToolTipText("Copy selected sketch from mainbox to framed area");
		tfsketchcopybutt.setMargin(smallbuttinsets);
		tfsubstylecopybutt.setToolTipText("Copy selected style to apply to framed sketch");
		tfsubstylecopybutt.setMargin(smallbuttinsets);
		tfsubmappingcopybutt.setToolTipText("Copy selected style to apply to framed sketch");
		tfsubmappingcopybutt.setMargin(smallbuttinsets);
		tfsubmappingpastebutt.setToolTipText("Paste copied parameters into framed sketch");
		tfsubmappingpastebutt.setMargin(smallbuttinsets);
		tfsetsubsetlower.setToolTipText("Select subset style for assignment");
		tfsetsubsetlower.setMargin(smallbuttinsets);
		tfsetsubsetupper.setToolTipText("Assign upper subset for style");
		tfsetsubsetupper.setMargin(smallbuttinsets);
	}


	/////////////////////////////////////////////
	void SetSubmappingSettings()
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		Map<String, String> submapping = op.plabedl.sketchframedef.submapping;
		for (String ssubset : submapping.keySet())
		{
			if (submapping.get(ssubset).equals(""))
			{
				tfsetsubsetupper.setEnabled(true);
				return;
			}
		}
		tfsetsubsetupper.setEnabled(false);
	}

	/////////////////////////////////////////////
	void SetSubsetLoHi(boolean bsetsubsetlohi)
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		Map<String, String> submapping = op.plabedl.sketchframedef.submapping;
		if (bsetsubsetlohi)
		{
	        for (String ssubset : sketchlinestyle.sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP)
				submapping.put(ssubset, "");
		}
		else
		{
			String ssubsetupper = sketchlinestyle.sketchdisplay.selectedsubsetstruct.GetFirstSubset();
			if (ssubsetupper != null)
			{
				for (String ssubset : submapping.keySet())
				{
					if (submapping.get(ssubset).equals(""))
						submapping.put(ssubset, ssubsetupper);
				}
			}
		}
		SetSubmappingSettings();
		UpdateSFView(op, true);
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
		boolean bbuttenabled = false;
		boolean bareaenabled = false;
		if ((op != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME))
		{
			if (op.plabedl.sketchframedef == null)
				op.plabedl.sketchframedef = new SketchFrameDef();
			tfsubmapping.setText(op.plabedl.sketchframedef.GetToTextV());
			bbuttenabled = true;
			bareaenabled = true;
		}
		if ((op != null) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE))
		{
			tfsubmapping.setText(String.valueOf(op.plabedl.nodeconnzsetrelative));
			bareaenabled = true;
		}
		tfmaxbutt.setEnabled(bbuttenabled);
		tfcentrebutt.setEnabled(bbuttenabled);
		tfsketchcopybutt.setEnabled(bbuttenabled);
		tfsubstylecopybutt.setEnabled(bbuttenabled);
		tfsubmappingcopybutt.setEnabled(bbuttenabled);
		tfsubmappingpastebutt.setEnabled(bbuttenabled);

		tfsetsubsetlower.setEnabled(bbuttenabled);
		if (bbuttenabled)
			SetSubmappingSettings(); 
		else
			tfsetsubsetupper.setEnabled(false);

		if (!bareaenabled)
			tfsubmapping.setText("");
		tfsubmapping.setEnabled(bareaenabled);
	}
};


