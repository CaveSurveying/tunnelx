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
import java.io.IOException;


/////////////////////////////////////////////
class ConnectiveAreaSigTabPane extends JPanel
{
	JComboBox areasignals = new JComboBox();
	SketchLineStyle sketchlinestyle;
	JButton jbcancel = new JButton("Cancel Area-signal");

	// we can choose to print just one view on one sheet of paper
	JButton tfmaxbutt = new JButton("Max");
	JButton tfcentrebutt = new JButton("Centr");

	JButton tfsketchcopybutt = new JButton("Sketch");
	JButton tfimagecopybutt = new JButton("Image");

	JButton tfsubstylecopybutt = new JButton("Style");

	JButton tfsubmappingcopybutt = new JButton("Copy");
	JButton tfsubmappingpastebutt = new JButton("Paste");

	JButton tfsetsubsetlower = new JButton("S-lo");
	JButton tfsetsubsetupper = new JButton("S-up");

	JButton tfshiftground = new JButton("Shift");

	JTextArea tfsubmapping = new JTextArea();
	JScrollPane jsp = new JScrollPane(tfsubmapping);

	// this is used to modify the treeview which we see
	// it's a bad hidden modal thing, but for now till we think of something better.
	SketchFrameDef sketchframedefCopied = new SketchFrameDef();

	// use these for parsing the text in the submapping textarea
	TunnelXMLparse txp = new TunnelXMLparse();
	TunnelXML tunnXML = new TunnelXML();

	String copiedsubmapping = "";

	/////////////////////////////////////////////
	void ShiftGround()
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op != null) && !sketchlinestyle.sketchdisplay.sketchgraphicspanel.bmoulinactive && (op.linestyle != SketchLineStyle.SLS_CENTRELINE))
		{
			float[] pco = op.GetCoords();
			sketchlinestyle.sketchdisplay.sketchgraphicspanel.backgroundimg.PreConcatBusinessF(pco, op.nlines);
		}
	}

	/////////////////////////////////////////////
	void SketchCopyButt()
	{
// ultimately this should use the same algorithm for finding the sketch name as we do for raw images
		OneSketch asketch = sketchlinestyle.sketchdisplay.mainbox.tunnelfilelist.GetSelectedSketchLoad();
		String st = "";
		if (asketch != null)
		{
			try
			{
				st = FileAbstraction.GetImageFileName(sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getParentFile(), asketch.sketchfile); 
				if (st != null)
					st = TN.loseSuffix(st); 
			}
			catch (IOException ie)
			{ TN.emitWarning(ie.toString()); };
		}

		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		op.plabedl.sketchframedef.sfsketch = st;
		sketchlinestyle.pthstyleareasigtab.LoadSketchFrameDef(op.plabedl.sketchframedef);
		sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag = op;
		UpdateSFView(op, true);
	}

	/////////////////////////////////////////////
	void CopyBackgroundSketchTransform(String st, AffineTransform lat, Vec3 lsketchLocOffset)
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		op.plabedl.sketchframedef.sfsketch = st;
		op.plabedl.sketchframedef.ConvertSketchTransform(lat, 1.0F, lsketchLocOffset);
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

		op.SetSubsetAttrs(sketchlinestyle.sketchdisplay.subsetpanel.sascurrent, null); // font changes
		op.plabedl.sketchframedef.SetSketchFrameFiller(sketchlinestyle.sketchdisplay.mainbox, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchfile);

		sketchlinestyle.sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
		if (bsketchchanged)
			sketchlinestyle.sketchdisplay.sketchgraphicspanel.SketchChanged(bsketchchanged ? SketchGraphics.SC_CHANGE_STRUCTURE : SketchGraphics.SC_CHANGE_SAS);
	}

	/////////////////////////////////////////////
	void LoadSketchFrameDef(SketchFrameDef lsketchframedefCopied)
	{
		sketchframedefCopied.copy(lsketchframedefCopied);
		sketchlinestyle.sketchdisplay.subsetpanel.SubsetSelectionChanged(true);
		//sketchlinestyle.sketchdisplay.subsetpanel.sascurrent.TreeListFrameDefCopiedSubsets(sketchframedefCopied);
		//sketchlinestyle.sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SAS);
	}

	/////////////////////////////////////////////
	void StyleMappingCopyButt(boolean bcopypaste)
	{
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || (op.linestyle != SketchLineStyle.SLS_CONNECTIVE) || (op.plabedl == null) || (op.plabedl.barea_pres_signal != SketchLineStyle.ASE_SKETCHFRAME) || (op.plabedl.sketchframedef == null))
		{
			TN.emitWarning("Can't execute StyleMappingCopyButt"); 
			return; 
		}
		
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

		sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag = op;

		if (!bcopypaste)
		{
			if (op.plabedl.sketchframedef.IsImageType())
			{
				sketchlinestyle.sketchdisplay.sketchgraphicspanel.FrameBackgroundOutline(); 
				sketchlinestyle.sketchdisplay.sketchgraphicspanel.ClearSelection(false);
			}
		}

		if (sketchlinestyle.sketchdisplay.bottabbedpane.getSelectedIndex() == 1)
			sketchlinestyle.sketchdisplay.backgroundpanel.UpdateBackimageCombobox(3); 
	}



	/////////////////////////////////////////////
	void MaxCentreOnScreenButt(boolean bmaxcen)
	{
		// find the area which this line corresponds to.  (have to search the areas to find it).
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || (op.plabedl == null) || (op.plabedl.sketchframedef == null))
			return;
		sketchlinestyle.sketchdisplay.sketchgraphicspanel.ClearSelection(false);
		
		op.plabedl.sketchframedef.MaxCentreOnScreenButt(sketchlinestyle.sketchdisplay.sketchgraphicspanel.getSize(), bmaxcen, (op.plabedl.sketchframedef.IsImageType() ? 1.0 : sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.realpaperscale), sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchlinestyle.sketchdisplay.sketchgraphicspanel.currtrans);
		sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.opframebackgrounddrag = op;

		if (op.plabedl.sketchframedef.IsImageType())
			sketchlinestyle.sketchdisplay.sketchgraphicspanel.FrameBackgroundOutline(); 

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

		JPanel pimpfields = new JPanel(new GridLayout(0, 5));
		pimpfields.add(tfmaxbutt);
		pimpfields.add(tfcentrebutt);
		pimpfields.add(tfshiftground);
									
		pimpfields.add(tfsketchcopybutt);
		pimpfields.add(tfimagecopybutt);
		pimpfields.add(tfsubstylecopybutt);

		pimpfields.add(tfsubmappingcopybutt);
		pimpfields.add(tfsubmappingpastebutt);

		pimpfields.add(tfsetsubsetlower);
		pimpfields.add(tfsetsubsetupper);
		
		ntop.add(pimpfields, BorderLayout.SOUTH);
		add(ntop, BorderLayout.NORTH);

		add(jsp, BorderLayout.CENTER);

		tfmaxbutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { MaxCentreOnScreenButt(true); } } );
		tfcentrebutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { MaxCentreOnScreenButt(false); } } );
		tfshiftground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { ShiftGround(); } } );
		tfsketchcopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { SketchCopyButt(); } } );
		tfsubstylecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { StyleCopyButt(); } } );
		tfimagecopybutt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { AddImage(); } } );
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
		tfimagecopybutt.setToolTipText("Load image as framed sketch or background");
		tfimagecopybutt.setMargin(smallbuttinsets);
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
		if ((op.plabedl == null) || (op.plabedl.sketchframedef == null))
			return; 
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
	        if (sketchlinestyle.sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP.isEmpty())
			{
				if (submapping.containsKey("default"))
					submapping.put("Your subset here", ""); 
				else
					submapping.put("default", ""); 
			}	
	        else
	        {
				for (String ssubset : sketchlinestyle.sketchdisplay.selectedsubsetstruct.vsselectedsubsetsP)
					submapping.put(ssubset, "");
			}
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
		tfimagecopybutt.setEnabled(bbuttenabled);
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


	/////////////////////////////////////////////
// This function replecates NewBackgroundFile, which creates the path in the first place
	// in the future this will be adding a sketch too--
	void AddImage()
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentDirectory, sketchlinestyle.sketchdisplay, SvxFileDialog.FT_BITMAP, false);
		if ((sfiledialog == null) || (sfiledialog.svxfile == null))
			return;
		TN.currentDirectory = sfiledialog.getSelectedFileA();
		OnePath op = sketchlinestyle.sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op.plabedl == null) || (op.plabedl.sketchframedef == null))
			return;
		try
		{
			op.plabedl.sketchframedef.sfsketch = FileAbstraction.GetImageFileName(sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchfile.getParentFile(), sfiledialog.svxfile);
		}
		catch (IOException ie)
		{ TN.emitWarning(ie.toString()); };

		op.plabedl.sketchframedef.sfscaledown = 1.0F;
		op.plabedl.sketchframedef.sfrotatedeg = 0.0F;

		op.plabedl.sketchframedef.sfxtrans = (float)(sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x / TN.CENTRELINE_MAGNIFICATION);
		op.plabedl.sketchframedef.sfytrans = -(float)(sketchlinestyle.sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y / TN.CENTRELINE_MAGNIFICATION);

		UpdateSFView(op, true);
		MaxCentreOnScreenButt(true);
		if (!sketchlinestyle.sketchdisplay.miShowBackground.isSelected())
			sketchlinestyle.sketchdisplay.miShowBackground.doClick();
	}
};


