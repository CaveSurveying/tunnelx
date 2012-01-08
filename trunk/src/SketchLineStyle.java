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
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection; 
import java.util.Comparator;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import javax.swing.text.BadLocationException;

//
//
// SketchLineStyle
//
//

/////////////////////////////////////////////

// to finish make sort all edges so the colours are in order when they're plotted by height
class pathcollamzcomp implements Comparator<OnePath>
{
    public int compare(OnePath op1, OnePath op2)
    {
        float op1z = (op1.pnstart.icollam + op1.pnend.icollam) / 2; 
        float op2z = (op2.pnstart.icollam + op2.pnend.icollam) / 2; 
        if (op1z < op2z)
            return -1; 
        if (op1z > op2z)
            return 1; 
        return 0; 
    }
}

/////////////////////////////////////////////
class SketchLineStyle extends JPanel
{
	// parallel arrays of wall style info.
	static String[] linestylenames = { "Centreline", "Wall", "Est. Wall", "Pitch Bound", "Ceiling Bound", "Detail", "Invisible", "Connective", "Filled" };
	static String[] shortlinestylenames = { "Cent", "Wall", "EstW", "Pitc", "CeilB", "Detl", "Invs", "Conn", "Fill" };
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
	static LabelFontAttr stationPropertyFontAttr = null;
	static Font defaultfontlab = null;

	// area-connective type signals which get loaded and their numeric values (should really be a map between ints)
	static final int ASE_KEEPAREA = 0;		// default state
	static final int ASE_VERYSTEEP = 0;		// not used yet, but will define an area that's a foreshortened pitch wall
	static final int ASE_HCOINCIDE = 1;		// pitch dropdown connection (on paths, not areas)
	static final int ASE_OUTLINEAREA = 2;	// pitch hole
	static final int ASE_KILLAREA = 3;		// column
	static final int ASE_ZSETRELATIVE = 5;	// setting relative z displacement between the nodes (on paths, not areas)
	static final int ASE_ELEVATIONPATH = 6;	// defines the connective as forming the path of an elevation diagramette
	static final int ASE_OUTERAREA = 7;		// assigned to an outer area of the diagram (not selectable)
	static final int ASE_NOAREA = 8;		// assigned to the object when path is part of a tree (not selectable)
	static final int ASE_SKETCHFRAME = 55;	// defining the interior of a frame

	static String[] areasignames = new String[12];
	static int[] areasigeffect = new int[12];
	static int iareasigelev = -1; 
	static int iareasigframe = -1; 
	static int nareasignames = 0;

	//Colours for drawing symbols
	private static Color linestylesymbcol = new Color(0.0F, 0.1F, 0.8F);
	private static Color linestylesymbcolinvalid = new Color(0.3F, 0.3F, 0.6F, 0.77F);

	//Line style used as a border for printing to help it to be cut out
	static LineStyleAttr printcutoutlinestyleattr = null;

	//Line styles for drawing paths when not in detail mode
	static Color linestylecolactive = Color.magenta;
	static Color linestylecolactiveCen = Color.magenta.brighter();
	static LineStyleAttr[] ActiveLineStyleAttrs = new LineStyleAttr[10];
    static LineStyleAttr[] ActiveLineStyleAttrsConnective = new LineStyleAttr[10]; 
	static float mouperplinlength; 
	static LineStyleAttr[] inSelSubsetLineStyleAttrs = new LineStyleAttr[10];
	static LineStyleAttr[] inSelSubsetLineStyleAttrsConnective = new LineStyleAttr[10];
	static Color notInSelSubsetCol = new Color(0.6F, 0.6F, 0.9F);
	static Color blankbackimagecol = new Color(0.9F, 0.9F, 0.6F);
	static LineStyleAttr[] notInSelSubsetLineStyleAttrs = new LineStyleAttr[10];
	static LineStyleAttr framebackgrounddragstyleattr = null; 
	
	//Line styles for drawing nodes
	static LineStyleAttr pnlinestyleattr = null;
	static LineStyleAttr activepnlinestyleattr = null;
	static LineStyleAttr firstselpnlinestyleattr = null;
	static LineStyleAttr lastselpnlinestyleattr = null;
	static LineStyleAttr middleselpnlinestyleattr = null;
	
	//Lines for drawing symbols to screen
	static LineStyleAttr linestylesymb = null;
	static LineStyleAttr linestylefirstsymb = null;
	static LineStyleAttr linestylesymbinvalid = null;
	static LineStyleAttr linestylefirstsymbinvalid = null;
	static LineStyleAttr lineactivestylesymb = null;
	static LineStyleAttr fillstylesymb = null;
	static LineStyleAttr fillstylefirstsymb = null;
	static LineStyleAttr fillstylesymbinvalid = null;
	static LineStyleAttr fillstylefirstsymbinvalid = null;
	static LineStyleAttr fillactivestylesymb = null;

	//Lines for hatching areas
	static LineStyleAttr linestylehatch1 = null;
	static LineStyleAttr linestylehatch2 = null;

	static String[] linestylebuttonnames = { "", "W", "E", "P", "C", "D", "I", "N", "F" };
	static int[] linestylekeystrokes = { 0, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_P, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_I, KeyEvent.VK_N, KeyEvent.VK_F };

	//These should be removed eventually...
	static BasicStroke gridStroke = null;
	static Color gridColor = null;

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
	ConnectiveAreaSigTabPane pthstyleareasigtab;

	SymbolsDisplay symbolsdisplay; // a tabbed pane


	// secondary sets of colours which over-ride using the icolindex attribute in lines
	// static for convenient access from OnePath.paintW
	static boolean bDepthColours = false; 
	static Color[] linestylecolsindex = new Color[100];
	static Color[] areastylecolsindex = new Color[200];
	static float zlo; 
	static float zhi; 

	/////////////////////////////////////////////
	static Color GetColourFromCollam(float icollam, boolean bAreas)
	{
		Color[] stylecolsindex = (bAreas ? areastylecolsindex : linestylecolsindex); 
		int i = (int)(icollam * stylecolsindex.length); 
		int i0 = Math.max(0, Math.min(stylecolsindex.length - 1, i)); 
		return stylecolsindex[i0]; 
	}


	/////////////////////////////////////////////
	// this will be a list
	Map<String, SubsetAttrStyle> subsetattrstylesmap = new TreeMap<String, SubsetAttrStyle>(); 
	boolean bsubsetattributesneedupdating = false;
	SubsetAttrStyle GetSubsetAttrStyle(String sasname) // dead func
	{
		// find the upper default we inherit from
		if (sasname == null)
			return null;
		return subsetattrstylesmap.get(sasname); 
	}



	/////////////////////////////////////////////
	static void SetIColsByZ(List<OnePath> vpaths, Collection<OnePath> tsvpathsviz, List<OnePathNode> vnodes, Collection<OneSArea> vsareas)
	{
		// extract the zrange from what we see
		zlo = 0.0F;
		zhi = 0.0F;

		// scan through using the half-points of each vector
		boolean bfirst = true; 
		for (OnePath op : tsvpathsviz)
		{
			float z = (op.pnstart.zalt + op.pnend.zalt) / 2;
			if (bfirst || (z < zlo))
				zlo = z;
			if (bfirst || (z > zlo))
				zhi = z;
			bfirst = false; 
		}

		// the setting of the zalts is done from a menu auto command
		TN.emitMessage("zrange in view zlo " + zlo + "  zhi " + zhi);

		// now set the zalts on all the paths
		for (OnePathNode opn : vnodes)
			opn.icollam = (opn.zalt - zlo) / (zhi - zlo);

        // sort the edges by height so lower ones don't over-write upper ones
        Collections.sort(vpaths, new pathcollamzcomp()); 

		// now set the zalts on all the areas
		for (OneSArea osa : vsareas)
			osa.icollam = (osa.zalt - zlo) / (zhi - zlo);
		bDepthColours = true; 
	}

	/////////////////////////////////////////////
	static void SetIColsProximity(int style, OneSketch tsketch, OnePathNode ops)
	{
		if (ops == null)
			return;

		// heavyweight stuff
		ProximityDerivation pd = new ProximityDerivation(tsketch);
		pd.ShortestPathsToCentrelineNodes(ops, null, null);

		double dlo = 0.0;
		double dhi = pd.distmax;

		if (style == 1)
		{
			dlo = pd.distmincnode;
			dhi = pd.distmaxcnode;
		}

		// separate out case
		if (dlo == dhi)
			dhi += dlo * 0.00001;

		// fill in the colours at the end-nodes
		for (OnePathNode opn : tsketch.vnodes)
		{
			double dp = opn.proxdist;
			opn.icollam = (float)((dp - dlo) / (dhi - dlo));
			if (style == 0)
				opn.icollam = 1.0F - opn.icollam; // make red 0.0
			else if (style == 1)
			{
				if (dp <= dlo)
					opn.icollam = 1.0F;
				else
					opn.icollam = (float)((dlo * dlo) / (dp * dp));
			}
		}
		bDepthColours = true; 
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
			setMargin(new Insets(1, 1, 1, 1));
		}
	};


	/////////////////////////////////////////////
    static Color ColorBlueMagenta = new Color(0.7F, 0.0F, 1.0F);  
    static Color ColorDarkGreen = new Color(0.0F, 0.9F, 0.0F); 
    static Color ColorGreenYellow = new Color(0.5F, 0.8F, 0.0F); 
    static Color ColorYellowGreen = new Color(0.7F, 0.7F, 0.0F); 

	/////////////////////////////////////////////
	static void SetStrokeWidths(float lstrokew, boolean bnotdotted)
	{
		strokew = lstrokew;
		//TN.emitMessage("New stroke width: " + strokew);

		//Set Grid stroke and colour
		gridStroke = new BasicStroke(1.0F * strokew);
		gridColor = Color.black;
		//Set Line style attributes for the cut out line when printing
		printcutoutlinestyleattr = new LineStyleAttr(SLS_SYMBOLOUTLINE, 3.0F * strokew, 6 * 2, 4 * 2, 0, Color.lightGray);
		//Set Line style attributes for non active path nodes
		pnlinestyleattr = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, Color.blue);
		//Set Line style attributes for active path nodes
		activepnlinestyleattr = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, Color.magenta);
		//Set Line style attributes for the first selected path nodes
		firstselpnlinestyleattr = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, new Color(1.0F, 0.5F, 1.0F));
		//Set Line style attributes for the last selected path nodes
		lastselpnlinestyleattr = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, new Color(0.8F, 0.0F, 0.8F));
		//Set Line style attributes for the middle selected path nodes
		middleselpnlinestyleattr = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, Color.magenta);

		//Set Line style attributes for hatching areas
		linestylehatch1 = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, Color.blue);
		linestylehatch2 = new LineStyleAttr(SLS_DETAIL, 1.0F * strokew, 0, 0, 0, Color.cyan);

		//Set default font
		stationPropertyFontAttr = new LabelFontAttr(fontcol, new Font("Serif", 0, Math.max(4, (int)(strokew * 15))));

		//Lines for drawing symbols to screen
		linestylesymb =        new LineStyleAttr(SLS_DETAIL, 1.0F*strokew, 0, 0, 0, linestylesymbcol);
		linestylesymbinvalid = new LineStyleAttr(SLS_DETAIL, 1.0F*strokew, 0, 0, 0, linestylesymbcolinvalid);
		lineactivestylesymb =  new LineStyleAttr(SLS_DETAIL, 1.0F*strokew, 0, 0, 0, linestylecolactive);
		fillstylesymb =        new LineStyleAttr(SLS_FILLED, 0.0F*strokew, 0, 0, 0, linestylesymbcol);
		fillstylesymbinvalid = new LineStyleAttr(SLS_FILLED, 0.0F*strokew, 0, 0, 0, linestylesymbcolinvalid);
		fillactivestylesymb =  new LineStyleAttr(SLS_FILLED, 0.0F*strokew, 0, 0, 0, linestylecolactive);

		// set 'in selected subsets' line style attributes
    	//LineStyleAttr(int llinestyle, float lstrokewidth, float lspikegap, float lgapleng, float lspikeheight, Color lstrokecolour)
        float strokewd = (bnotdotted ? 0.0F : strokew); 

		inSelSubsetLineStyleAttrs[SLS_CENTRELINE] =   new LineStyleAttr(SLS_CENTRELINE,   0.5F*strokew, 0,           0,          0, Color.red);
		inSelSubsetLineStyleAttrs[SLS_WALL] =         new LineStyleAttr(SLS_WALL,         2.0F*strokew, 0,           0,          0, Color.blue);
		inSelSubsetLineStyleAttrs[SLS_ESTWALL] =      new LineStyleAttr(SLS_ESTWALL,      2.0F*strokew, 12*strokewd, 6*strokewd, 0, Color.blue);
		inSelSubsetLineStyleAttrs[SLS_PITCHBOUND] =   new LineStyleAttr(SLS_PITCHBOUND,   1.0F*strokew, 16*strokewd, 6*strokewd, 0, ColorBlueMagenta);
		inSelSubsetLineStyleAttrs[SLS_CEILINGBOUND] = new LineStyleAttr(SLS_CEILINGBOUND, 1.0F*strokew, 16*strokewd, 6*strokewd, 0, Color.cyan);
		inSelSubsetLineStyleAttrs[SLS_DETAIL] =       new LineStyleAttr(SLS_DETAIL,       1.0F*strokew, 0,           0,          0, Color.blue);
		inSelSubsetLineStyleAttrs[SLS_INVISIBLE] =    new LineStyleAttr(SLS_INVISIBLE,    1.0F*strokew, 0,           0,          0, ColorDarkGreen);
		inSelSubsetLineStyleAttrs[SLS_CONNECTIVE] =   new LineStyleAttr(SLS_CONNECTIVE,   1.0F*strokew, 6*strokewd,  3*strokewd, 0, ColorGreenYellow);
		inSelSubsetLineStyleAttrs[SLS_FILLED] =       new LineStyleAttr(SLS_FILLED,       0.0F*strokew, 0,           0,          0, Color.black);
		// symbol paint background.
		inSelSubsetLineStyleAttrs[SLS_SYMBOLOUTLINE] =new LineStyleAttr(SLS_SYMBOLOUTLINE,3.0F*strokew, 0,           0,          0, Color.black);// for printing.

        // connective line variations
        inSelSubsetLineStyleAttrsConnective[SketchLineStyle.ASE_ELEVATIONPATH] = 
                                                      new LineStyleAttr(SLS_CONNECTIVE,   0.75F*strokew,4*strokewd,  3*strokewd, 0, ColorYellowGreen);

		// set 'active (highlighted)' line style attributes
		ActiveLineStyleAttrs[SLS_CENTRELINE] =        new LineStyleAttr(SLS_CENTRELINE,   0.5F*strokew, 0,           0,          0, linestylecolactiveCen);
		ActiveLineStyleAttrs[SLS_WALL] =              new LineStyleAttr(SLS_WALL,         2.0F*strokew, 0,           0,          0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_ESTWALL] =           new LineStyleAttr(SLS_ESTWALL,      2.0F*strokew, 12*strokewd, 6*strokewd, 0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_PITCHBOUND] =        new LineStyleAttr(SLS_PITCHBOUND,   1.0F*strokew, 16*strokewd, 6*strokewd, 0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_CEILINGBOUND] =      new LineStyleAttr(SLS_CEILINGBOUND, 1.0F*strokew, 16*strokewd, 6*strokewd, 0, linestylecolactive);  // experimental to see if pitch bounds can draw automatically on the selected edge
		ActiveLineStyleAttrs[SLS_DETAIL] =            new LineStyleAttr(SLS_DETAIL,       1.0F*strokew, 0,           0,          0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_INVISIBLE] =         new LineStyleAttr(SLS_INVISIBLE,    1.0F*strokew, 0,           0,          0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_CONNECTIVE] =        new LineStyleAttr(SLS_CONNECTIVE,   1.0F*strokew, 6*strokewd,  3*strokewd, 0, linestylecolactive);
		ActiveLineStyleAttrs[SLS_FILLED] =            new LineStyleAttr(SLS_FILLED,       0.0F*strokew, 0,           0,          0, linestylecolactive);
		// symbol paint background.
		ActiveLineStyleAttrs[SLS_SYMBOLOUTLINE] =     new LineStyleAttr(SLS_SYMBOLOUTLINE, 3.0F * strokew, 0, 0, 0, Color.white);// for printing.

        // connective line variations
        ActiveLineStyleAttrsConnective[SketchLineStyle.ASE_ELEVATIONPATH] = 
                                                      new LineStyleAttr(SLS_CONNECTIVE,   1.0F*strokew, 4*strokewd,  3*strokewd, 0, linestylecolactive);

		mouperplinlength = 8*strokew; 

		// set 'not in selected subsets' line style attributes
		notInSelSubsetLineStyleAttrs[SLS_CENTRELINE] = new LineStyleAttr(SLS_CENTRELINE,  0.5F*strokew, 0,           0,          0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_WALL] =       new LineStyleAttr(SLS_WALL,        2.0F*strokew, 0,           0,          0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_ESTWALL] =    new LineStyleAttr(SLS_ESTWALL,     2.0F*strokew, 12*strokewd, 6*strokewd, 0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_PITCHBOUND] = new LineStyleAttr(SLS_PITCHBOUND,  1.0F*strokew, 16*strokewd, 6*strokewd, 0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_CEILINGBOUND]=new LineStyleAttr(SLS_CEILINGBOUND,1.0F*strokew, 16*strokewd, 6*strokewd, 0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_DETAIL] =     new LineStyleAttr(SLS_DETAIL,      1.0F*strokew, 0,           0,          0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_INVISIBLE] =  new LineStyleAttr(SLS_INVISIBLE,   1.0F*strokew, 0,           0,          0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_CONNECTIVE] = new LineStyleAttr(SLS_CONNECTIVE,  1.0F*strokew, 6*strokewd,  3*strokewd, 0, notInSelSubsetCol);
		notInSelSubsetLineStyleAttrs[SLS_FILLED] =     new LineStyleAttr(SLS_FILLED,      0.0F*strokew, 0,           0,          0, notInSelSubsetCol);
		// symbol paint background.
		notInSelSubsetLineStyleAttrs[SLS_SYMBOLOUTLINE] = 
                                                       new LineStyleAttr(SLS_SYMBOLOUTLINE,3.0F*strokew,0,          0,         0, notInSelSubsetCol);// for printing.

		//Set Line style attributes for selected image carrying connective path
		framebackgrounddragstyleattr = new LineStyleAttr(SLS_DETAIL, 2.0F * strokew, 0, 0, 0, new Color(0.87F, 0.4F, 0.1F));
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
		//?? if (!FileAbstraction.bIsApplet)  // can't handle this
		{
			if (pthstyleareasigtab.areasignals.getItemCount() != 0)
                pthstyleareasigtab.areasignals.setSelectedIndex(0);
			pthstyleareasigtab.SetFrameSketchInfoText(null);
		}
	}


	/////////////////////////////////////////////
	boolean SetFrameZSetRelative(OnePath op)
	{
		float pnodeconnzsetrelative = op.plabedl.nodeconnzsetrelative;
		try
		{
		op.plabedl.nodeconnzsetrelative = Float.parseFloat(pthstyleareasigtab.tfsubmapping.getText().trim());
		}
		catch (NumberFormatException e)  { System.out.println(pthstyleareasigtab.tfsubmapping.getText()); };
		return (pnodeconnzsetrelative != op.plabedl.nodeconnzsetrelative);
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
				symbolsdisplay.SelEnableButtons(op.subsetattr);
				symbolsdisplay.UpdateSymbList(op.plabedl.vlabsymb, op.subsetattr);
			}

			// label type at this one
			else if ((op.plabedl != null) && (op.plabedl.sfontcode != null))
			{
				pthstylelabeltab.fontstyles.setSelectedIndex(pthstylelabeltab.lfontstyles.indexOf(op.plabedl.sfontcode));
				pthstylelabeltab.setTextPosCoords(op.plabedl.fnodeposxrel, op.plabedl.fnodeposyrel);
				pthstylelabeltab.jcbarrowpresent.setSelected(op.plabedl.barrowpresent);
				pthstylelabeltab.jcbboxpresent.setSelected(op.plabedl.bboxpresent);
				String ldrawlab = op.plabedl.drawlab == null ? "" : op.plabedl.drawlab; 
				if (!ldrawlab.equals(pthstylelabeltab.labtextfield.getText()))
					pthstylelabeltab.labtextfield.setText(ldrawlab); 
				Showpthstylecard("Label");
				pthstylelabeltab.labtextfield.requestFocus();
			}

			// area-signal present at this one
			else if ((op.plabedl != null) && (op.plabedl.iarea_pres_signal != 0))
			{
				pthstyleareasigtab.areasignals.setSelectedIndex(op.plabedl.iarea_pres_signal);
				pthstyleareasigtab.SetFrameSketchInfoText(op);
				Showpthstylecard("Area-Sig");
			}

			// none specified; free choice
			else
				SetClearedTabs("Conn", true);
		}
		else if (op.linestyle == SLS_CENTRELINE)
		{
			pthstylecentreline.tfhead.setText(((op.plabedl != null) && (op.plabedl.centrelinehead != null)) ? op.plabedl.centrelinehead : "--nothing--");
			pthstylecentreline.tftail.setText(((op.plabedl != null) && (op.plabedl.centrelinetail != null)) ? op.plabedl.centrelinetail : "--nothing--");
			pthstylecentreline.tfelev.setText(((op.plabedl != null) && (op.plabedl.centrelineelev != null)) ? op.plabedl.centrelineelev : "");
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
		if (SetParametersFromBoxes(op))
		{
			sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
			sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_STRUCTURE);
		}
	}


	/////////////////////////////////////////////
	// returns true if anything actually changed.
	boolean SetParametersFromBoxes(OnePath op)
	{
		boolean bRes = false;

		int llinestyle = linestylesel.getSelectedIndex();
		if (op.linestyle != llinestyle)
			bRes = true; 
		op.linestyle = llinestyle;

		if (op.bWantSplined != pthsplined.isSelected())
			bRes = true; 
		op.bWantSplined = pthsplined.isSelected();

		// go and spline it if required
		// (should do this in the redraw actually).
		if ((op.pnend != null) && (op.bWantSplined != op.bSplined))
			op.Spline(op.bWantSplined && !OnePath.bHideSplines, false);

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

				String lsfontcode = (lifontcode == -1 ? "default" : pthstylelabeltab.lfontstyles.get(lifontcode));
				if ((op.plabedl.drawlab == null) || !op.plabedl.drawlab.equals(ldrawlab) || (op.plabedl.sfontcode == null) || (!op.plabedl.sfontcode.equals(lsfontcode)))
				{
					op.plabedl.drawlab = ldrawlab;
					op.plabedl.sfontcode = lsfontcode;
					bRes = true;
				}

				float pfnodeposxrel = op.plabedl.fnodeposxrel;
				float pfnodeposyrel = op.plabedl.fnodeposyrel;
				boolean pbarrowpresent = op.plabedl.barrowpresent;
				boolean pbboxpresent = op.plabedl.bboxpresent;
				try
				{
				op.plabedl.fnodeposxrel = Float.parseFloat(pthstylelabeltab.tfxrel.getText());
				op.plabedl.fnodeposyrel = Float.parseFloat(pthstylelabeltab.tfyrel.getText());
				} catch (NumberFormatException e)  { System.out.println(pthstylelabeltab.tfxrel.getText() + "/" + pthstylelabeltab.tfyrel.getText()); };
				op.plabedl.barrowpresent = pthstylelabeltab.jcbarrowpresent.isSelected();
				op.plabedl.bboxpresent = pthstylelabeltab.jcbboxpresent.isSelected();

				if ((pfnodeposxrel != op.plabedl.fnodeposxrel) || (pfnodeposyrel != op.plabedl.fnodeposyrel) || (pbarrowpresent != op.plabedl.barrowpresent) || (pbboxpresent != op.plabedl.bboxpresent))
					bRes = true;
			}
			else
			{
				if ((op.plabedl.drawlab != null) || (op.plabedl.sfontcode != null))
					bRes = true; 
				op.plabedl.drawlab = null;
				op.plabedl.sfontcode = null;
			}


			// area-signal present at this one (no need to specialize because default is number 0)
			// if (pthstylecardlayoutshown.equals("Area-sig"))
			int liarea_pres_signal = pthstyleareasigtab.areasignals.getSelectedIndex();
			if ((liarea_pres_signal != -1) && (op.plabedl.iarea_pres_signal != liarea_pres_signal))
			{
				op.plabedl.iarea_pres_signal = liarea_pres_signal;  // look up in combobox
				int bareapre = op.plabedl.barea_pres_signal;
				op.plabedl.barea_pres_signal = areasigeffect[op.plabedl.iarea_pres_signal];

				// change in state.  update
				pthstyleareasigtab.SetFrameSketchInfoText(op);
				bRes = true;
			}

			if (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE)
			{
				if (SetFrameZSetRelative(op))
					bRes = true; 
			}
		}

		op.SetSubsetAttrs(sketchdisplay.subsetpanel.sascurrent, sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied); // font change
		return bRes;
	}


	/////////////////////////////////////////////
	void SetConnTabPane(String tstring)
	{
		OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath;
		if ((op == null) || !sketchdisplay.sketchgraphicspanel.bEditable)
		{
			TN.emitWarning("Must have connective path selected"); // maybe use disabled buttons
			return;
		}
		Showpthstylecard(tstring);
		if (op.plabedl == null)
			op.plabedl = new PathLabelDecode();

		if (tstring.equals("Label"))
		{
			// new paths are not survey type
			int lifontcode = pthstylelabeltab.fontstyles.getSelectedIndex();
			String lsfontcode = (lifontcode == -1 ? "default" : pthstylelabeltab.lfontstyles.get(lifontcode));
			if (lsfontcode.equals("survey"))
				pthstylelabeltab.fontstyles.setSelectedIndex(pthstylelabeltab.lfontstyles.indexOf("default"));
			pthstylelabeltab.labtextfield.requestFocus();
		}
		else if (tstring.equals("Symbol"))
			symbolsdisplay.SelEnableButtons(op.subsetattr);
	}



	/////////////////////////////////////////////
	class DocAUpdate implements DocumentListener, ActionListener
	{
		public void changedUpdate(DocumentEvent e) 
		{
			//System.out.println("EEECU: " + e.toString());
		}
		public void removeUpdate(DocumentEvent e)
		{
			//System.out.println("EEEd: " + e.getOffset() + " " + e.getLength());
			if (!bsettingaction)
			{
				if (e.getOffset() == 0)  // update when entire thing disappears
					GoSetParametersCurrPath();
			}
		}
		public void insertUpdate(DocumentEvent e)
		{
			//System.out.println("EEEi: " + e.getOffset() + " " + e.getLength());
			if (!bsettingaction)
			{
				// update when space is pressed
				try {
					String istr = e.getDocument().getText(e.getOffset(), e.getLength());
					if ((istr.indexOf(' ') != -1) || (istr.indexOf('\n') != -1) || (istr.indexOf('%') != -1))
						GoSetParametersCurrPath();
				} catch (BadLocationException ex) {;};
			}
		}

		public void actionPerformed(ActionEvent e)
		{
			if (!bsettingaction)
			{
				GoSetParametersCurrPath();
				sketchdisplay.sketchgraphicspanel.ObserveSelection(sketchdisplay.sketchgraphicspanel.currgenpath, null, 10); 
			}
		}
	};

	/////////////////////////////////////////////
	class DocActionUpdate implements ActionListener
	{
		int iy;  // to help track where the events are coming from
		DocActionUpdate(int liy)
		{
			iy = liy; 
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (!bsettingaction)
			{
				GoSetParametersCurrPath();
				sketchdisplay.sketchgraphicspanel.ObserveSelection(sketchdisplay.sketchgraphicspanel.currgenpath, null, 11); 
				//System.out.println("EEEAP: " + iy + " " + bsettingaction);
			}
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
			op.plabedl.vlabsymb.clear();
		if (name != null)
			op.plabedl.vlabsymb.add(name);
		symbolsdisplay.UpdateSymbList(op.plabedl.vlabsymb, op.subsetattr);


		sketchdisplay.sketchgraphicspanel.SketchChanged(SketchGraphics.SC_CHANGE_SYMBOLS);
		op.GenerateSymbolsFromPath();
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView();
		return true;
	}

	/////////////////////////////////////////////
	void SetupSymbolStyleAttr()
	{
		// apply a setup on all the symbols in the attribute styles
		for (SubsetAttrStyle sas : subsetattrstylesmap.values())
		{
			for (SubsetAttr sa : sas.msubsets.values())
			{
				for (SymbolStyleAttr ssa : sa.subautsymbolsmap.values())
					ssa.SetUp(sketchdisplay.mainbox.vgsymbolstsketches);
			}
		}
	}

	/////////////////////////////////////////////
	SketchLineStyle(SymbolsDisplay lsymbolsdisplay, SketchDisplay lsketchdisplay)
	{
		symbolsdisplay = lsymbolsdisplay;
		sketchdisplay = lsketchdisplay;
		pthstyleareasigtab = new ConnectiveAreaSigTabPane(this);

		setBackground(TN.sketchlinestyle_col);

		Border bord_loweredbevel = BorderFactory.createLoweredBevelBorder();
		Border bord_redline = BorderFactory.createLineBorder(Color.blue);
		Border bord_compound = BorderFactory.createCompoundBorder(bord_redline, bord_loweredbevel);
		pthstylecards.setBorder(bord_compound);


		// do the button panel
		JPanel buttpanel = new JPanel();
		buttpanel.setLayout(new GridLayout(1, 0));
		for (int i = 0; i < linestylebuttonnames.length; i++)
		{
			if (!linestylebuttonnames[i].equals(""))
				buttpanel.add(new LineStyleButton(i));
		}
		pthsplined.setMargin(new Insets(1, 1, 1, 1));
		buttpanel.add(pthsplined);
		linestylesel.setSelectedIndex(SLS_DETAIL);

		// the listener for all events among the linestyles
		DocAUpdate docaupdate = new DocAUpdate();

		// action listeners on the linestyles
		pthsplined.addActionListener(new DocActionUpdate(1));

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
		pthstyleareasigtab.areasignals.addActionListener(new DocActionUpdate(2));
		pthstylelabeltab.fontstyles.addActionListener(new DocActionUpdate(3333));
		pthstylelabeltab.tfxrel.addActionListener(new DocActionUpdate(4));
		pthstylelabeltab.tfyrel.addActionListener(new DocActionUpdate(5));
		pthstylelabeltab.jcbarrowpresent.addActionListener(new DocActionUpdate(6));
		pthstylelabeltab.jcbboxpresent.addActionListener(new DocActionUpdate(7));
		pthstylelabeltab.labtextfield.getDocument().addDocumentListener(docaupdate);
		pthstyleareasigtab.tfsubmapping.getDocument().addDocumentListener(docaupdate);

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
		add(partpanel, BorderLayout.NORTH);
		add(pthstylecards, BorderLayout.CENTER);


		// fill in the colour rainbow for showing weighting and depth
		for (int i = 0; i < linestylecolsindex.length; i++)
		{
			float a = (float)i / linestylecolsindex.length ;
			linestylecolsindex[i] = new Color(Color.HSBtoRGB(0.5F + 1.2F * a, 1.0F, 0.9F));
			//linestylecolsindex[i] = new Color(a, (1.0F - a) * 0.2F, 1.0F - a);
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
	Color GetDepthColourPoint(float x, float y, float z)
	{
		float a = (z - zlo) / (zhi - zlo);
		int icolindex = Math.max(Math.min((int)(a * areastylecolsindex.length), areastylecolsindex.length - 1), 0);
		return areastylecolsindex[icolindex]; 
	}


	/////////////////////////////////////////////
	SubsetAttrStyle GetSubsetSelection(String lstylename) // dead func
	{
		for (SubsetAttrStyle sas : subsetattrstylesmap.values())
		{
			if (lstylename.equals(sas.stylename))
				return sas; 
		}
		TN.emitWarning("Not found subsetstylename " + lstylename); 
		return null; 
	}

	/////////////////////////////////////////////
	// this gets called on opening, and whenever a set of sketches which contains some fontcolours gets loaded
	void UpdateSymbols(boolean bfirsttime)
	{
		assert bsubsetattributesneedupdating;
		// update the underlying symbols
		for (OneSketch tsketch : sketchdisplay.mainbox.vgsymbolstsketches)
		{
			assert tsketch.bsketchfileloaded;
			tsketch.MakeAutoAreas();
		}

		// fill in all the attributes
		for (SubsetAttrStyle sas : subsetattrstylesmap.values())
			sas.FillAllMissingAttributes();

		// push the newly loaded stuff into the panels
		SetupSymbolStyleAttr();
		pthstyleareasigtab.UpdateAreaSignals(areasignames, nareasignames);

		// extract out and sort
		List<SubsetAttrStyle> lsaslist = new ArrayList<SubsetAttrStyle>();
		for (SubsetAttrStyle lsas : subsetattrstylesmap.values())
		{
			if (lsas.bselectable)
		        lsaslist.add(lsas);
		}
		Collections.sort(lsaslist);

		int iprevselindex = sketchdisplay.subsetpanel.jcbsubsetstyles.getSelectedIndex();
  		sketchdisplay.subsetpanel.jcbsubsetstyles.removeAllItems();
		for (SubsetAttrStyle lsas : lsaslist)
			sketchdisplay.subsetpanel.jcbsubsetstyles.addItem(lsas);

		bsubsetattributesneedupdating = false;

		if ((iprevselindex != -1) && (iprevselindex < sketchdisplay.subsetpanel.jcbsubsetstyles.getItemCount()))
			sketchdisplay.subsetpanel.jcbsubsetstyles.setSelectedIndex(iprevselindex);
	}
};


