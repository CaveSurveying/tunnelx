////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2002  Julian Todd.
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
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;

import java.awt.Insets;
//import java.lang.NumberFormatException;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Component;

import javax.swing.border.Border;
import javax.swing.BorderFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color; 

import java.io.IOException;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.JCheckBoxMenuItem;

import java.util.Vector;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import javax.swing.text.BadLocationException;
//
//
// SketchLineStyle
//
//

/////////////////////////////////////////////
class SketchLineStyle extends JPanel
{
	// parallel arrays of wall style info.
	static String[] linestylenames = { "Centreline", "Wall", "Est. Wall", "Pitch Bound", "Ceiling Bound", "Detail", "Invisible", "Connective", "Filled" };
	static final int SLS_CENTRELINE = 0;

	static final int SLS_WALL = 1;
	static final int SLS_ESTWALL = 2;

	static final int SLS_PITCHBOUND = 3;
	static final int SLS_CEILINGBOUND = 4;

	static final int SLS_DETAIL = 5;
	static final int SLS_INVISIBLE = 6;
	static final int SLS_CONNECTIVE = 7;
	static final int SLS_FILLED = 8;

	static final int SLS_SYMBOLOUTLINE = 9; // not a selected style.

	static float strokew = -1.0F;

	static Color fontcol = new Color(0.7F, 0.3F, 1.0F);

// should be non-static (problems with printing in the OneSketch)
	static LabelFontAttr[] labstylenames = new LabelFontAttr[40];
	int nlabstylenames = 0;
	static Font defaultfontlab = null;

	// area-connective type signals which get loaded and their numeric values
	static String[] areasignames = new String[10];
	static int[] areasigeffect = new int[10];
	static int nareasignames = 0;



	static Color[] linestylecols = new Color[10];

	static Color linestylesymbcol = new Color(0.5F, 0.2F, 1.0F);
	static Color linestylefirstsymbcol = new Color(0.0F, 0.1F, 0.8F);
	static Color linestylesymbcolinvalid = new Color(0.6F, 0.6F, 0.6F, 0.77F);
	static Color linestylefirstsymbcolinvalid = new Color(0.3F, 0.3F, 0.6F, 0.77F);

	static BasicStroke[] linestylestrokes = new BasicStroke[10];
	static BasicStroke doublewallstroke; // for drawing the mini elevation survey in big

	static Color linestylecolactive = Color.magenta;
	static Color linestylecolactivemoulin = new Color(1.0F, 0.5F, 1.0F); //linestylecolactive.brighter();
	static Color linestylecolactivefnode = new Color(0.8F, 0.0F, 0.8F); //linestylecolactive.darker();
	static Color linestylecolprint= Color.black;
	static Color linestylegreyed = Color.lightGray;

	static BasicStroke linestylegreystrokes = null;
	static BasicStroke linestyleprintcutout = null;
	static Color linestyleprintgreyed = Color.darkGray;

	static String[] linestylebuttonnames = { "", "W", "E", "P", "C", "D", "I", "N", "F" };
	static int[] linestylekeystrokes = { 0, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_P, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_I, KeyEvent.VK_N, KeyEvent.VK_F };

	static float pitchbound_flatness;
	static float pitchbound_spikegap;
	static float pitchbound_spikeheight;
	static float ceilingbound_gapleng;

	// used to get back for defaults and to the active path.
	SketchDisplay sketchdisplay;

	// (we must prevent the centreline style from being selected --  it's special).
	JComboBox linestylesel = new JComboBox(linestylenames);
	JToggleButton pthsplined = new JToggleButton("s");

	// tabbing panes that are put in the bottom part
	CardLayout pthstylecardlayout = new CardLayout();
	JPanel pthstylecards = new JPanel(pthstylecardlayout);
	String pthstylecardlayoutshown = null;

	// a panel displayed when no path is selected (useful for holding a few spare buttons)
	JPanel pthstylenonconn = new JPanel();

	// panel of deselect and delete buttons
	JPanel pathcoms = new JPanel(new GridLayout(1, 0));

	// the other panel types
	ConnectiveCentrelineTabPane pthstylecentreline = new ConnectiveCentrelineTabPane();
	ConnectiveLabelTabPane pthstylelabeltab = new ConnectiveLabelTabPane();
	ConnectiveAreaSigTabPane pthstyleareasigtab = new ConnectiveAreaSigTabPane();
	SymbolsDisplay symbolsdisplay; // a tabbed pane


	// secondary sets of colours which over-ride using the icolindex attribute in lines
	static Color[] linestylecolsindex = new Color[100];
	static Color[] areastylecolsindex = new Color[200];

// this will be a list
	/////////////////////////////////////////////
	Vector subsetattrstyles = new Vector(); // SubsetAttrStyle
//	Vector subsetattrstylesselectable = new Vector(); // jcbsubsetstyles combobox derives from this one
	boolean bsubsetattributestoupdate = false;
	SubsetAttrStyle GetSubsetAttrStyle(String sasname)
	{
		// find the upper default we inherit from
		if (sasname == null)
			return null;
		for (int i = subsetattrstyles.size() - 1; i >= 0; i--)
		{
			SubsetAttrStyle lsas = (SubsetAttrStyle)subsetattrstyles.elementAt(i);
			if (sasname.equals(lsas.stylename))
				return lsas;
		}
		return null;
	}


	/////////////////////////////////////////////
	void AddToFontList(LabelFontAttr lfa)
	{
		for (int i = 0; i < nlabstylenames; i++)
			if (lfa.labelfontname.equals(labstylenames[i].labelfontname))
				return;
		labstylenames[nlabstylenames++] = lfa;
	}




	/////////////////////////////////////////////
	public class AclsButt extends AbstractAction
	{
		int index;
	    public AclsButt(int lindex)
		{
			super(linestylebuttonnames[lindex]);
			index = lindex;
            putValue(SHORT_DESCRIPTION, linestylenames[index]);
            putValue(MNEMONIC_KEY, new Integer(linestylekeystrokes[index]));
		}

	    public void actionPerformed(ActionEvent e)
		{
			linestylesel.setSelectedIndex(index);
		}
	}

	/////////////////////////////////////////////
	class LineStyleButton extends JButton
	{
		int index;

		LineStyleButton(int lindex)
		{
			super(new AclsButt(lindex));
			index = lindex;
			setMargin(new Insets(2, 2, 2, 2));
		}
	};




	/////////////////////////////////////////////
	static void SetStrokeWidths(float lstrokew)
	{
		strokew = lstrokew;
		TN.emitMessage("New stroke width: " + strokew);

		float[] dash = new float[2];

		pitchbound_flatness = strokew / 2;
		ceilingbound_gapleng = strokew * 4;
		pitchbound_spikegap = strokew * 12;
		pitchbound_spikeheight = strokew * 4;

		// centreline
		linestylestrokes[0] = new BasicStroke(0.5F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[0] = Color.red;

		// wall
		linestylestrokes[1] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[1] = Color.blue;

		// wall
		doublewallstroke = new BasicStroke(5.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);

		// estimated wall
		dash[0] = 6 * strokew;
		dash[1] = 6 * strokew;
		linestylestrokes[2] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 3.0F * strokew);
		linestylecols[2] = Color.blue;

		// pitch boundary
		dash[0] = 10 * strokew;
		dash[1] = 6 * strokew;
		linestylestrokes[3] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 5.0F * strokew);
		linestylecols[3] = new Color(0.7F, 0.0F, 1.0F);

		// ceiling boundary
		linestylestrokes[4] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 1.7F * strokew);
		linestylecols[4] = Color.cyan;

		// detail
		linestylestrokes[5] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[5] = Color.blue;

		// invisible
		linestylestrokes[6] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[6] = new Color(0.0F, 0.9F, 0.0F);

		// connective
		dash[0] = 3 * strokew;
		dash[1] = 3 * strokew;
		linestylestrokes[7] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 1.5F * strokew);
		linestylecols[7] = new Color(0.5F, 0.8F, 0.0F);

		// filled
		linestylestrokes[8] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[8] = Color.black;

		// symbol paint background.
		linestylestrokes[9] = new BasicStroke(3.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[9] = Color.white; // for printing.


		// greyed out stuff
		dash[0] = 4 * strokew;
		dash[1] = 6 * strokew;
		linestylegreystrokes = new BasicStroke(1.2F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 2.4F * strokew);

		// the cutting out when printing in tiles
		// this should be in points, not in the local size
		dash[0] = 6 * 2;
		dash[1] = 4 * 2;
		linestyleprintcutout = new BasicStroke(1.2F * 1.1F, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 5.0F * 1.1F, dash, 2.4F * 1.1F);

		defaultfontlab = new Font("Serif", 0, Math.max(4, (int)(strokew * 15)));
	}


	// this is dangerous but seems to work.
	boolean bsettingaction = false;

	/////////////////////////////////////////////
	// we don't otherwise have a way to recover which card is visible
	void Showpthstylecard(String lpthstylecardlayoutshown)
	{
		pthstylecardlayoutshown = lpthstylecardlayoutshown;
		pthstylecardlayout.show(pthstylecards, pthstylecardlayoutshown);
	}

	/////////////////////////////////////////////
	void SetClearedTabs(String tstring, boolean benableconnbuttons)
	{
		sketchdisplay.SetEnabledConnectiveSubtype(benableconnbuttons);
		Showpthstylecard(tstring); 

		// zero the other visual areas
		pthstylelabeltab.labtextfield.setText("");
		pthstylelabeltab.setTextPosCoords(-1, -1);
		pthstylelabeltab.jcbarrowpresent.setSelected(false);
		pthstylelabeltab.jcbboxpresent.setSelected(false);
		LSpecSymbol(true, null);
		pthstyleareasigtab.areasignals.setSelectedIndex(0);
	}


	/////////////////////////////////////////////
	// this has got two uses; when we select a new path,
	// or we change the linestyle of a path
	boolean SetConnectiveParametersIntoBoxes(OnePath op)
	{
		if (op == null)
		{
			bsettingaction = true;
			SetClearedTabs("Nonconn", false);
			bsettingaction = false;
			return false;
		}

		bsettingaction = true;
		op.linestyle = linestylesel.getSelectedIndex(); // this would recopy it just after it had been copied over, I guess

		if (op.linestyle == SLS_CONNECTIVE)
		{
			// symbols present in this one
			if ((op.plabedl != null) && !op.plabedl.vlabsymb.isEmpty())
			{
				Showpthstylecard("Symbol");
				symbolsdisplay.UpdateSymbList(op.plabedl.vlabsymb);
			}

			// label type at this one
			else if ((op.plabedl != null) && (op.plabedl.sfontcode != null))
			{
				// set the font if there is one recorded
				int lifontcode = -1;
				for (int i = 0; i < nlabstylenames; i++)
					if (op.plabedl.sfontcode.equals(labstylenames[i].labelfontname))
						lifontcode = i;
				if (lifontcode == -1)
					TN.emitWarning("Unrecognized sfontcode " + op.plabedl.sfontcode);
				pthstylelabeltab.fontstyles.setSelectedIndex(lifontcode);

				pthstylelabeltab.setTextPosCoords(op.plabedl.fnodeposxrel, op.plabedl.fnodeposyrel);
				pthstylelabeltab.jcbarrowpresent.setSelected(op.plabedl.barrowpresent);
				pthstylelabeltab.jcbboxpresent.setSelected(op.plabedl.bboxpresent);
				pthstylelabeltab.labtextfield.setText(op.plabedl.drawlab == null ? "" : op.plabedl.drawlab);
				Showpthstylecard("Label");
				pthstylelabeltab.labtextfield.requestFocus();
			}

			// area-signal present at this one
			else if ((op.plabedl != null) && (op.plabedl.iarea_pres_signal != 0))
			{
				pthstyleareasigtab.areasignals.setSelectedIndex(op.plabedl.iarea_pres_signal);
				Showpthstylecard("Area-Sig");
			}

			// none specified; free choice
			else
				SetClearedTabs("Conn", true);
		}
		else if (op.linestyle == SLS_CENTRELINE)
		{
			pthstylecentreline.tfhead.setText(op.plabedl != null ? op.plabedl.head : "--nothing--");
			pthstylecentreline.tftail.setText(op.plabedl != null ? op.plabedl.tail : "--nothing--");
			Showpthstylecard("Centreline");
		}
		else
		{
			sketchdisplay.SetEnabledConnectiveSubtype(false);
			Showpthstylecard("Nonconn");
		}

		bsettingaction = false;
		return true;
	}

	/////////////////////////////////////////////
	// we have some confounding situations of what to show when there is no path shown
	void SetParametersIntoBoxes(OnePath op)
	{
		bsettingaction = true;
		if (op != null)
		{
			bsettingaction = true;
			pthsplined.setSelected(op.bWantSplined);
			linestylesel.setSelectedIndex(op.linestyle);
		}

		// null case
		else
		{
			if (linestylesel.getSelectedIndex() == SLS_CENTRELINE)
				linestylesel.setSelectedIndex(SLS_DETAIL);

			// set the splining by default.
			// except make the splining off if the type is connective, which we don't really want splined since it's distracting.
//			pthsplined.setSelected(sketchdisplay.miDefaultSplines.isSelected() && (linestylesel.getSelectedIndex() != SLS_CONNECTIVE));
			bsettingaction = false;

			sketchdisplay.SetEnabledConnectiveSubtype(false);
			Showpthstylecard("Nonconn");
		}
		bsettingaction = false;

		// we have a connective type, so should load the contents here
		SetConnectiveParametersIntoBoxes(op);
	}



	/////////////////////////////////////////////
	void GoSetParametersCurrPath()  // this calls function below
	{
		OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || !sketchdisplay.sketchgraphicspanel.bEditable)
			return;

		// if the spline changes then the area should change too.
		if (SetParametersFromBoxes(op));
		{
			sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
			sketchdisplay.sketchgraphicspanel.SketchChanged(0, true);
		}
	}


	/////////////////////////////////////////////
	// returns true if anything actually changed.
	boolean SetParametersFromBoxes(OnePath op)
	{
		boolean bRes = false;

		int llinestyle = linestylesel.getSelectedIndex();
		bRes |= (op.linestyle != llinestyle);
		op.linestyle = llinestyle;

		bRes |= (op.bWantSplined != pthsplined.isSelected());
		op.bWantSplined = pthsplined.isSelected();

		// go and spline it if required
		// (should do this in the redraw actually).
		if ((op.pnend != null) && (op.bWantSplined != op.bSplined))
			op.Spline(op.bWantSplined, false);

		// we have a connective type, so should load the contents here
		if (op.plabedl != null)
		{
			// symbols are loaded as they are pressed.
			//if ((op.plabedl != null) && !op.plabedl.vlabsymb.isEmpty())
			//	pthstylecardlayout.show(pthstylecards, "Symbol");

			// label type at this one
			if (pthstylecardlayoutshown.equals("Label"))
			{
				String ldrawlab = pthstylelabeltab.labtextfield.getText().trim();
				int lifontcode = pthstylelabeltab.fontstyles.getSelectedIndex();
				String lsfontcode = (lifontcode == -1 ? "default" : labstylenames[lifontcode].labelfontname);
				if ((op.plabedl.drawlab == null) || !op.plabedl.drawlab.equals(ldrawlab) || (op.plabedl.sfontcode == null) || (!op.plabedl.sfontcode.equals(lsfontcode)))
				{
					op.plabedl.drawlab = ldrawlab;
					op.plabedl.sfontcode = lsfontcode;
					bRes = true;
				}

				try {
				op.plabedl.fnodeposxrel = Float.parseFloat(pthstylelabeltab.tfxrel.getText());
				op.plabedl.fnodeposyrel = Float.parseFloat(pthstylelabeltab.tfyrel.getText());
				} catch (NumberFormatException e)  { System.out.println(pthstylelabeltab.tfxrel.getText() + "/" + pthstylelabeltab.tfyrel.getText()); };
				op.plabedl.barrowpresent = pthstylelabeltab.jcbarrowpresent.isSelected();
				op.plabedl.bboxpresent = pthstylelabeltab.jcbboxpresent.isSelected();
			}
			else
			{
				bRes = ((op.plabedl.drawlab != null) || (op.plabedl.sfontcode != null));
				op.plabedl.drawlab = null;
				op.plabedl.sfontcode = null;
			}


			// area-signal present at this one (no need to specialize because default is number 0)
			// if (pthstylecardlayoutshown.equals("Area-sig"))
			int liarea_pres_signal = pthstyleareasigtab.areasignals.getSelectedIndex();
			if (op.plabedl.iarea_pres_signal != liarea_pres_signal)
			{
				op.plabedl.iarea_pres_signal = liarea_pres_signal;  // look up in combobox
				op.plabedl.barea_pres_signal = areasigeffect[op.plabedl.iarea_pres_signal];
				bRes = true;
			}
		}

		op.SetSubsetAttrs(sketchdisplay.subsetpanel.sascurrent, sketchdisplay.vgsymbols); // font change
		return bRes;
	}


	/////////////////////////////////////////////
	void SetConnTabPane(String tstring)
	{
		OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || !sketchdisplay.sketchgraphicspanel.bEditable)
		{
			System.out.println("Must have connective path selected"); // maybe use disabled buttons
			return;
		}
		Showpthstylecard(tstring);
		if (op.plabedl == null)
			op.plabedl = new PathLabelDecode("", null);

		if (tstring == "Label")
			pthstylelabeltab.labtextfield.requestFocus();
	}


	/////////////////////////////////////////////
	class DocAUpdate implements DocumentListener, ActionListener
	{
		public void changedUpdate(DocumentEvent e) {;}
		public void removeUpdate(DocumentEvent e)
		{
			if (!bsettingaction)
			{
				if (e.getOffset() == 0)  // update when entire thing disappears
					GoSetParametersCurrPath();
			}
		}
		public void insertUpdate(DocumentEvent e)
		{
			if (!bsettingaction)
			{
				// update when space is pressed
				try {
					String istr = e.getDocument().getText(e.getOffset(), e.getLength());
					if ((istr.indexOf(' ') != -1) || (istr.indexOf('\n') != -1))
						GoSetParametersCurrPath();
				} catch (BadLocationException ex) {;};
			}
		}

		public void actionPerformed(ActionEvent event)
		{
			if (!bsettingaction)
				GoSetParametersCurrPath();
		}
	};



	/////////////////////////////////////////////
	// from when the symbol buttons are pressed
	boolean LSpecSymbol(boolean bOverwrite, String name)
	{
		// shares much code from GoSetParametersCurrPath
		OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || !sketchdisplay.sketchgraphicspanel.bEditable)
			return false;

		if ((op.linestyle != SLS_CONNECTIVE) || (op.plabedl == null))
			return false;

    		assert ((name != null) || bOverwrite);
		if ((name == null) && op.plabedl.vlabsymb.isEmpty())
			return false; // no change

   		if (bOverwrite)
			op.plabedl.vlabsymb.removeAllElements();
		if (name != null)
			op.plabedl.vlabsymb.addElement(name);
		symbolsdisplay.UpdateSymbList(op.plabedl.vlabsymb);


		sketchdisplay.sketchgraphicspanel.SketchChanged(1, true);
		op.GenerateSymbolsFromPath(sketchdisplay.vgsymbols);
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		return true;
	}

	/////////////////////////////////////////////
	void SetupSymbolStyleAttr()
	{
		// apply a setup on all the symbols in the attribute styles
		for (int i = 0; i < subsetattrstyles.size(); i++)
		{
			SubsetAttrStyle sas = (SubsetAttrStyle)subsetattrstyles.elementAt(i);
			for (int j = 0; j < sas.subsets.size(); j++)
			{
				SubsetAttr sa = (SubsetAttr)sas.subsets.elementAt(j);
				for (int k = 0; k < sa.vsubautsymbols.size(); k++)
				{
					SymbolStyleAttr ssa = (SymbolStyleAttr)sa.vsubautsymbols.elementAt(k);
					ssa.SetUp(sketchdisplay.vgsymbols);
				}
			}
		}
	}

	/////////////////////////////////////////////
	SketchLineStyle(SymbolsDisplay lsymbolsdisplay, SketchDisplay lsketchdisplay)
	{
		symbolsdisplay = lsymbolsdisplay;
		sketchdisplay = lsketchdisplay;
		setBackground(TN.sketchlinestyle_col);

		Border bord_loweredbevel = BorderFactory.createLoweredBevelBorder();
		Border bord_redline = BorderFactory.createLineBorder(Color.blue);
		Border bord_compound = BorderFactory.createCompoundBorder(bord_redline, bord_loweredbevel);
		pthstylecards.setBorder(bord_compound);


		// do the button panel
		JPanel buttpanel = new JPanel();
		buttpanel.setLayout(new GridLayout(1, 0));
		Insets inset = new Insets(1, 1, 1, 1);
		for (int i = 0; i < linestylebuttonnames.length; i++)
		{
			if (!linestylebuttonnames[i].equals(""))
			{
				buttpanel.add(new LineStyleButton(i));
				pthsplined.setMargin(inset);
  			}
		}
		pthsplined.setMargin(new Insets(5, 5, 5, 5));
		buttpanel.add(pthsplined);
		linestylesel.setSelectedIndex(SLS_DETAIL);

		// the listener for all events among the linestyles
		DocAUpdate docaupdate = new DocAUpdate();

		// action listeners on the linestyles
		pthsplined.addActionListener(docaupdate);

		// change of linestyle
		linestylesel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)
				{
				  if (!bsettingaction)
				  { if (sketchdisplay.miDefaultSplines.isSelected())
						pthsplined.setSelected((linestylesel.getSelectedIndex() != SLS_CONNECTIVE) && (linestylesel.getSelectedIndex() != SLS_CENTRELINE));
					GoSetParametersCurrPath();
					SetConnectiveParametersIntoBoxes(sketchdisplay.sketchgraphicspanel.currgenpath);
				  }
				}
			} );

		// LSpecSymbol calls added with the symbolsdisplay


		// put in the tabbing panes updates
		pthstyleareasigtab.areasignals.addActionListener(docaupdate);
		pthstylelabeltab.fontstyles.addActionListener(docaupdate);
		pthstylelabeltab.tfxrel.addActionListener(docaupdate);
		pthstylelabeltab.tfyrel.addActionListener(docaupdate);
		pthstylelabeltab.jcbarrowpresent.addActionListener(docaupdate);
		pthstylelabeltab.jcbboxpresent.addActionListener(docaupdate);

		pthstylelabeltab.labtextfield.getDocument().addDocumentListener(docaupdate);

		// cancel buttons
		pthstyleareasigtab.jbcancel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SetClearedTabs("Nonconn", true);  GoSetParametersCurrPath();  } } );
		pthstylelabeltab.jbcancel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SetClearedTabs("Nonconn", true);  GoSetParametersCurrPath();  } } );
		symbolsdisplay.jbcancel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SetClearedTabs("Nonconn", true);  GoSetParametersCurrPath();  } } );

		// the clear symbols button
		symbolsdisplay.jbclear.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { LSpecSymbol(true, null);  } } );


		pthstylecards.add(pthstylenonconn, "Nonconn"); // when no connected path is selected
		pthstylecards.add(pthstylecentreline, "Centreline"); // this should have buttons that take you to the other four types
		pthstylecards.add(pthstylelabeltab, "Label");
		pthstylecards.add(symbolsdisplay, "Symbol");
		pthstylecards.add(pthstyleareasigtab, "Area-Sig");


		// do the layout of the main thing.
		JPanel partpanel = new JPanel(new GridLayout(3, 1));
		partpanel.add(linestylesel);
		partpanel.add(buttpanel);
		partpanel.add(pathcoms);  // delete and deselect


		setLayout(new BorderLayout());
		add("North", partpanel);
		add("Center", pthstylecards);


		// fill in the colour rainbow for showing weighting and depth
		for (int i = 0; i < linestylecolsindex.length; i++)
		{
			float a = (float)i / linestylecolsindex.length ;
			//linestylecolsindex[i] = new Color(Color.HSBtoRGB(0.9F * a, 1.0F, 0.9F));
			linestylecolsindex[i] = new Color(a, (1.0F - a) * 0.2F, 1.0F - a);
		}

		for (int i = 0; i < areastylecolsindex.length; i++)
		{
			float a = (float)i / areastylecolsindex.length ;
			//linestylecolsindex[i] = new Color();
			// fcolw = new Color(0.8F, 1.0F, 1.0F, 0.6F);
			//areastylecolsindex[i] = new Color(0.7F + a * 0.3F, 1.0F - a * 0.3F, 1.0F, 0.6F);
			int col = Color.HSBtoRGB(0.6F * (1.0F - a) + 0.06F, 1.0F, 1.0F) + 0x61000000;
			areastylecolsindex[i] = new Color(col, true);
			//linestylecolsindex[i] = new Color(Color.HSBtoRGB(0.9F * a, 1.0F, 0.9F));
		}
	}

	/////////////////////////////////////////////
// we should soon be loading these files from the same place as the svx as well as this general directory
	void LoadSymbols(boolean bAuto)
	{
		if (TN.currentSymbols == null)
		{
			FileAbstraction fauserdir = FileAbstraction.MakeDirectoryFileAbstraction(System.getProperty("user.dir"));
			TN.currentSymbols = FileAbstraction.MakeDirectoryAndFileAbstraction(fauserdir, "symbols");
		}

		SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentSymbols, sketchdisplay, SvxFileDialog.FT_DIRECTORY, bAuto);
		if ((sfiledialog == null) || (sfiledialog.tunneldirectory == null))
			return;

		if (!bAuto)
			TN.currentSymbols = sfiledialog.getSelectedFileA();

		TN.emitMessage("Loading symbols " + TN.currentSymbols.getName());

		// do the tunnel loading thing
		TunnelLoader symbtunnelloader = new TunnelLoader(null, this);
		try
		{
			FileAbstraction.FileDirectoryRecurse(symbolsdisplay.vgsymbols, sfiledialog.tunneldirectory);
			symbtunnelloader.LoadFilesRecurse(symbolsdisplay.vgsymbols, true);
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		catch (NullPointerException e)
		{
			TN.emitWarning(e.toString());
			e.printStackTrace();
		};
	}

	/////////////////////////////////////////////
	void UpdateSymbols()
	{
		assert bsubsetattributestoupdate;
		// update the underlying symbols
		for (int i = 0; i < symbolsdisplay.vgsymbols.tsketches.size(); i++)
		{
			OneSketch tsketch = (OneSketch)(symbolsdisplay.vgsymbols.tsketches.elementAt(i));
			tsketch.MakeAutoAreas();
		}

		// fill in all the attributes
		for (int i = 0; i < subsetattrstyles.size(); i++)
			((SubsetAttrStyle)subsetattrstyles.elementAt(i)).FillAllMissingAttributes();

		// push the newly loaded stuff into the panels
		SetupSymbolStyleAttr();
		pthstylelabeltab.AddFontStyles(labstylenames, nlabstylenames);
		pthstyleareasigtab.AddAreaSignals(areasignames, nareasignames);


		sketchdisplay.subsetpanel.jcbsubsetstyles.removeAllItems();
		for (int i = 0; i < subsetattrstyles.size(); i++)
		{
			SubsetAttrStyle lsas = (SubsetAttrStyle)subsetattrstyles.elementAt(i);
			if (lsas.bselectable)
		        sketchdisplay.subsetpanel.jcbsubsetstyles.addItem(lsas);
		}

		bsubsetattributestoupdate = false;
	}
};


