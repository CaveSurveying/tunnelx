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

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/////////////////////////////////////////////
class LabelFontAttr
{
	SubsetAttr subsetattr; // backpointer
	String labelfontname;

		String fontname = null;
		String fontstyle = null;
		int fontsize = -1;
	Font fontlab = null; // filled automatically
	Color labelcolour = SubsetAttr.coldefalt; // none means we draw nothing for this font.


	/////////////////////////////////////////////
	LabelFontAttr(String llabelfontname, SubsetAttr lsubsetattr)
	{
		labelfontname = llabelfontname;
		subsetattr = lsubsetattr;
	}

	/////////////////////////////////////////////
	// copy of whole style
	LabelFontAttr(LabelFontAttr lfa, SubsetAttr lsubsetattr)
	{
		labelfontname = lfa.labelfontname;
		fontname = lfa.fontname;
		fontstyle = lfa.fontstyle;
		fontsize = lfa.fontsize;
		labelcolour = lfa.labelcolour;
		subsetattr = lsubsetattr;
	}


	/////////////////////////////////////////////
	void FillMissingAttribs(LabelFontAttr lfa)
	{
		assert (lfa == null) || labelfontname.equals(lfa.labelfontname);
		if (fontname == null)
			fontname = (lfa != null ? lfa.fontname : "Serif");
		if (fontstyle == null)
			fontstyle = (lfa != null ? lfa.fontstyle : "PLAIN");
		if (fontsize == -1)
			fontsize = (lfa != null ? lfa.fontsize : 10);
		if (labelcolour == SubsetAttr.coldefalt)
			labelcolour = (lfa != null ? lfa.labelcolour : Color.gray);

		// set font up if we have enough properties
		if (lfa == null)	// final default round
		{
			int ifontstyle = Font.PLAIN;
			if (fontstyle.equals("ITALIC"))
				ifontstyle = Font.ITALIC;
			else if (fontstyle.equals("BOLD"))
				ifontstyle = Font.BOLD;
			else if (!fontstyle.equals("PLAIN"))
				TN.emitWarning("Unrecognized font style " + fontstyle);
			fontlab = new Font(fontname, ifontstyle, fontsize);
		}
	}
};


/////////////////////////////////////////////
class LineStyleAttr
{
	static int Nlinestyles = 9; // takes in SLS_FILLED

	float strokewidth;
	float spikegap;
	float gapleng;
	float spikeheight;
	BasicStroke linestroke = null;

	Color strokecolour;


	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle, float lstrokewidth, float lspikegap, float lgapleng, float lspikeheight, Color lstrokecolour)
	{
    	strokewidth = lstrokewidth;
		spikegap = lspikegap;
		gapleng = lgapleng;
		spikeheight = lspikeheight;
		strokecolour = lstrokecolour; 
		Construct(llinestyle);
	}

	/////////////////////////////////////////////
	void Construct(int llinestyle)
	{
		if (llinestyle == SketchLineStyle.SLS_FILLED)
		{
			if (strokewidth != 0.0F)
				TN.emitWarning("nonzero strokewidth on filled line");
		}
		else
		{
			if (strokewidth == 0.0F)
				TN.emitWarning("zero strokewidth on line style; use colour=null");
		}
		if (spikeheight != 0.0F)
		{
			if ((llinestyle != SketchLineStyle.SLS_PITCHBOUND) && (llinestyle != SketchLineStyle.SLS_CEILINGBOUND))
				TN.emitWarning("spikes only on pitch and ceiling bounds please");
		}

		// setup the basicstroke
		if (strokewidth != 0.0F)
		{
			// dotted
			float mitrelimit = strokewidth * 5.0F;
			if ((gapleng != 0.0F) && (spikeheight == 0.0F))
			{
				float[] dash = new float[2];
				dash[0] = spikegap - gapleng;
				dash[1] = gapleng;
				linestroke = new BasicStroke(strokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, mitrelimit, dash, dash[0] / 2);
			}
			else
				linestroke = new BasicStroke(strokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, mitrelimit);
		}
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle)
	{
    	strokewidth = 1.0F;
		spikegap = 0.0F;
		gapleng = 0.0F;
		spikeheight = 0.0F;
		strokecolour = Color.black;
		Construct(llinestyle);
	}
}

/////////////////////////////////////////////
class SymbolStyleAttr
{
	String symbolname;
	float symbolstrokewidth = -1.0F;
	Color symbolcolour = SubsetAttr.coldefalt;
	Vector ssymbolbs = null; // SSymbolBase
	BasicStroke symbolstroke = null;

	/////////////////////////////////////////////
	SymbolStyleAttr(String lsymbolname)
	{
		symbolname = lsymbolname;
	}

	/////////////////////////////////////////////
	// copy of whole style
	SymbolStyleAttr(SymbolStyleAttr ssa)
	{
		symbolname = ssa.symbolname;
		symbolstrokewidth = ssa.symbolstrokewidth;
		symbolcolour = ssa.symbolcolour;
		ssymbolbs = new Vector();
		ssymbolbs.addAll(ssa.ssymbolbs); // or should copy?
	}


	/////////////////////////////////////////////
	void FillMissingAttribs(SymbolStyleAttr ssa)
	{
		assert (ssa == null) || symbolname.equals(ssa.symbolname);
		if (symbolstrokewidth == -1.0F)
			symbolstrokewidth = (ssa != null ? ssa.symbolstrokewidth : 1);
		if (symbolcolour == SubsetAttr.coldefalt)
			symbolcolour = (ssa != null ? ssa.symbolcolour : Color.blue);
		if ((ssymbolbs == null) && ((ssa == null) || (ssa.ssymbolbs != null)))
		{
			ssymbolbs = new Vector();
			if (ssa != null)
				ssymbolbs.addAll(ssa.ssymbolbs);
		}

		// set font up if we have enough properties
		if (ssa == null)
			symbolstroke = new BasicStroke(symbolstrokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, symbolstrokewidth * 5.0F);
	}


	/////////////////////////////////////////////
	public void SetUp(OneTunnel lvgsymbols)
	{
		//vgsymbols = lvgsymbols;
		for (int k = 0; k < ssymbolbs.size(); k++)
		{
			// now match each with symbol name to sketch
			SSymbolBase ssb = (SSymbolBase)ssymbolbs.elementAt(k);
			for (int j = 0; j < lvgsymbols.tsketches.size(); j++)
			{
				OneSketch lgsym = (OneSketch)lvgsymbols.tsketches.elementAt(j);
				if (lgsym.sketchname.equals(ssb.gsymname))
				{
					ssb.gsym = lgsym;
					break;
				}
			}
			if (ssb.gsym == null)
				TN.emitWarning("no match for symbol name " + ssb.gsymname);
		}
	}
}


/////////////////////////////////////////////
class SubsetAttr
{
	static Color coldefalt = new Color(0);
	String subsetname = null;
	String uppersubset = null;
	SubsetAttr uppersubsetattr = null;
	Vector vsubsetsdown = new Vector();


	Color areamaskcolour = coldefalt;
	Color areacolour = coldefalt;

	// list of linestyles
	LineStyleAttr[] linestyleattrs = null;

	// list of fonts
	Vector labelfonts = new Vector(); // type LabelFontAttr

	// list of symbols.
    Vector vsubautsymbols = new Vector(); // type SymbolStyleAttr

	/////////////////////////////////////////////
	SubsetAttr(String lsubsetname)
	{
		subsetname = lsubsetname;
	}

	/////////////////////////////////////////////
	// used for copying over SubsetAttrStyles
	SubsetAttr(SubsetAttr lsa)
	{
		subsetname = lsa.subsetname;
		uppersubset = lsa.uppersubset;

		areamaskcolour = lsa.areamaskcolour;
		areacolour = lsa.areacolour;



		// copy defined fonts
		for (int i = 0; i < lsa.labelfonts.size(); i++)
			labelfonts.addElement(new LabelFontAttr((LabelFontAttr)lsa.labelfonts.elementAt(i), this));
		//TN.emitMessage("Copying subset attr " + subsetname + " " + labelfonts.size());

		// copy over defined linestyles things
		if (lsa.linestyleattrs != null)
		{
			linestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];
			for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
				linestyleattrs[i] = lsa.linestyleattrs[i];
		}

		// list of symbols.
		for (int i = 0; i < lsa.vsubautsymbols.size(); i++)
			vsubautsymbols.addElement(new SymbolStyleAttr((SymbolStyleAttr)lsa.vsubautsymbols.elementAt(i)));
	}

	/////////////////////////////////////////////
	LabelFontAttr FindLabelFont(String llabelfontname, boolean bcreate)
	{
		for (int i = 0; i < labelfonts.size(); i++)
		{
			LabelFontAttr lfa = (LabelFontAttr)labelfonts.elementAt(i);
			if (llabelfontname.equals(lfa.labelfontname))
				return lfa;
		}
		if (bcreate)
		{
			LabelFontAttr lfa = new LabelFontAttr(llabelfontname, this);
			labelfonts.addElement(lfa);
			return lfa;
		}
		LabelFontAttr res = (uppersubsetattr != null ? uppersubsetattr.FindLabelFont(llabelfontname, false) : null);
		if (res == null)
			TN.emitWarning("No font label matches " + llabelfontname + " of subset " + subsetname + " " + labelfonts.size());
		return res;
	}

	/////////////////////////////////////////////
	SymbolStyleAttr FindSymbolSpec(String lsymbolname, int icreate)
	{
		for (int i = 0; i < vsubautsymbols.size(); i++)
		{
			SymbolStyleAttr ssa = (SymbolStyleAttr)vsubautsymbols.elementAt(i);
			if (lsymbolname.equals(ssa.symbolname))
				return ssa;
		}
		if (icreate == 1)
		{
			SymbolStyleAttr ssa = new SymbolStyleAttr(lsymbolname);
			vsubautsymbols.addElement(ssa);
			return ssa;
		}
		if (icreate == 2)
			return null;
		SymbolStyleAttr res = (uppersubsetattr != null ? uppersubsetattr.FindSymbolSpec(lsymbolname, 0) : null);
		if (res == null)
			TN.emitWarning("No symbol name matches " + lsymbolname + " of subset " + subsetname + " " + vsubautsymbols.size());
		return res;
	}

	/////////////////////////////////////////////
	void FillMissingAttribs(SubsetAttr sa)
	{
		if (areamaskcolour == coldefalt)
			areamaskcolour = (sa != null ? sa.areamaskcolour : new Color(1.0F, 1.0F, 1.0F, 0.55F));
		if (areacolour == coldefalt)
			areacolour = (sa != null ? sa.areacolour : new Color(0.8F, 0.9F, 0.9F, 0.4F));

		// fill in the missing font attributes
		for (int i = 0; i < labelfonts.size(); i++)
		{
			LabelFontAttr lfa = (LabelFontAttr)labelfonts.elementAt(i);
			if (sa != null)
			{
				LabelFontAttr llfa = sa.FindLabelFont(lfa.labelfontname, false);
				if (llfa != null)
					lfa.FillMissingAttribs(llfa);
			}
			else
				lfa.FillMissingAttribs(null);
		}

		// fill in the missing symbol attributes
		for (int i = 0; i < vsubautsymbols.size(); i++)
		{
			SymbolStyleAttr ssa = (SymbolStyleAttr)vsubautsymbols.elementAt(i);
			if (sa != null)
			{
				SymbolStyleAttr lssa = sa.FindSymbolSpec(ssa.symbolname, 2);
				if (lssa != null)
					ssa.FillMissingAttribs(lssa);
			}
			else
				ssa.FillMissingAttribs(null);
		}


		// copy over defined linestyles things and fill in gaps
		if (linestyleattrs == null)
		{
			if (sa != null)
				linestyleattrs = sa.linestyleattrs;
			else
				linestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];
		}
		if ((linestyleattrs != null) && (sa != null))
		{
			for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
			{
				if (linestyleattrs[i] == null)
					linestyleattrs[i] = sa.linestyleattrs[i];
			}
		}
		for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
		{
			if ((linestyleattrs[i] == null) && !((i == SketchLineStyle.SLS_INVISIBLE) || (i == SketchLineStyle.SLS_CONNECTIVE)))
			{
				TN.emitWarning("Building default linestyle for " + TNXML.EncodeLinestyle(i));
				linestyleattrs[i] = new LineStyleAttr(i);
			}
		}
	}


	/////////////////////////////////////////////
	public String toString()
	{
		return subsetname;
	}
};


/////////////////////////////////////////////
class SubsetAttrStyle
{
	String stylename;
	String stylenamedefaults;
	Vector subsets = new Vector(); // of SubsetAttr

	DefaultMutableTreeNode dmroot;
	DefaultTreeModel dmtreemod;

	SketchGrid sketchgrid = null;

	/////////////////////////////////////////////
	void RecurseFillTree(DefaultMutableTreeNode dmr, SubsetAttr sa)
	{
		DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(sa);
		dmr.add(cnode);
		for (int i = 0; i < sa.vsubsetsdown.size(); i++)
			RecurseFillTree(cnode, (SubsetAttr)sa.vsubsetsdown.elementAt(i));
	}
	void MakeTreeRootNode()
	{
		dmroot = new DefaultMutableTreeNode("root");
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);
			if (sa.uppersubsetattr == null)
				RecurseFillTree(dmroot, sa);
		}
		dmtreemod = new DefaultTreeModel(dmroot);
	}

	/////////////////////////////////////////////
	SubsetAttrStyle(String lstylename, SubsetAttrStyle lsas)
	{
		stylename = lstylename;
		// copy in the defaults which we seed this with
		if (lsas != null)
		{
			for (int i = 0; i < lsas.subsets.size(); i++)
				subsets.addElement(new SubsetAttr((SubsetAttr)lsas.subsets.elementAt(i)));

			sketchgrid = lsas.sketchgrid; // copy down from above
		}
	}

	/////////////////////////////////////////////
	SubsetAttr FindSubsetAttr(String lsubsetname, boolean bcreate)
	{
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);
			if (lsubsetname.equals(sa.subsetname))
				return sa;
		}
		if (bcreate)
		{
			SubsetAttr sa = new SubsetAttr(lsubsetname);
			subsets.addElement(sa);
			return sa;
		}
		return null;
	}

	/////////////////////////////////////////////
	void UnpeelTree(Vector subsetsrevdef, SubsetAttr sa)
	{
		assert sa != null;
		subsetsrevdef.addElement(sa);
		for (int i = 0; i < sa.vsubsetsdown.size(); i++)
			UnpeelTree(subsetsrevdef, (SubsetAttr)sa.vsubsetsdown.elementAt(i));
	}

	/////////////////////////////////////////////
    void FillAllMissingAttributes()
    {
		// set pointers up
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);
			if (sa.uppersubset != null)
			{
				sa.uppersubsetattr = FindSubsetAttr(sa.uppersubset, false);
				if (sa.uppersubsetattr == null)
					TN.emitWarning("Upper subset " + sa.uppersubset + " not found of " + sa.subsetname);
				else
					sa.uppersubsetattr.vsubsetsdown.addElement(sa);
			}
		}

		// make the tree in reverse order of definition (or could have set up a partial sort)
		Vector subsetsrevdef = new Vector();
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);
			if (sa.uppersubset == null)
				UnpeelTree(subsetsrevdef, sa);
		}

		// recurse over missing attributes for each subset
		for (int i = 0; i < subsetsrevdef.size();  i++)
		{
			SubsetAttr sa = (SubsetAttr)subsetsrevdef.elementAt(i);
			SubsetAttr saupper = sa.uppersubsetattr;
			if (saupper != null)
			{
				int sc = 0;
				while (saupper != null)
				{
					sa.FillMissingAttribs(saupper);
					saupper = saupper.uppersubsetattr;
					if (sc++ > subsets.size())
						TN.emitError("recursive subset inheritance " + sa.subsetname);
				}
			}
			sa.FillMissingAttribs(null); // the default round
		}

		// get this part done
		MakeTreeRootNode();
    }

	/////////////////////////////////////////////
	public String toString()
	{
		return stylename + (stylenamedefaults == null ? "" : "(" + stylenamedefaults + ")");
	}
};



