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

import java.awt.geom.Rectangle2D;

import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/////////////////////////////////////////////
class LabelFontAttr
{
	SubsetAttr subsetattr; // backpointer
	String labelfontname;

	// defining parameters
	String sfontsize = null;
	String sfontname = null;
	String sfontstyle = null;

	float ffontsize = -1.0F;
	String fontname = null;
	String fontstyle = null;

	Font fontlab = null; // filled automatically

	String slabelcolour = null; // none means we draw nothing for this font.
	Color labelcolour = null; // none means we draw nothing for this font.
	BasicStroke labelstroke = null;

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
		sfontname = lfa.sfontname;
		sfontstyle = lfa.sfontstyle;
		sfontsize = lfa.sfontsize;
		slabelcolour = lfa.slabelcolour;

		subsetattr = lsubsetattr;
	}

	/////////////////////////////////////////////
	LabelFontAttr(Color color, Font font)
	{
		labelcolour = color;
		fontlab = font;
		subsetattr = null;
	}

	/////////////////////////////////////////////
	void FillMissingAttribsLFA(LabelFontAttr lfaupper)
	{
		assert (lfaupper == null) || labelfontname.equals(lfaupper.labelfontname);  // should be copying over from same named style

		if ((lfaupper != null) && (slabelcolour == null))
			slabelcolour = lfaupper.slabelcolour;

		labelcolour = SubsetAttr.ConvertColour(subsetattr.EvalVars(slabelcolour), Color.gray);


		if ((lfaupper != null) && (sfontname == null))
			sfontname = lfaupper.sfontname;
		if ((lfaupper != null) && (sfontsize == null))
			sfontsize = lfaupper.sfontsize;
		if ((lfaupper != null) && (sfontstyle == null))
			sfontstyle = lfaupper.sfontstyle;

		fontname = SubsetAttr.ConvertString(subsetattr.EvalVars(sfontname), "Serif");
		ffontsize = SubsetAttr.ConvertFloat(subsetattr.EvalVars(sfontsize), 10.0F);
		fontstyle = SubsetAttr.ConvertString(subsetattr.EvalVars(sfontstyle), "PLAIN");
		int ifontstyle = (fontstyle.equals("ITALIC") ? Font.ITALIC : (fontstyle.equals("BOLD") ? Font.BOLD : Font.PLAIN));
		fontlab = new Font(fontname, ifontstyle, (int)(ffontsize + 0.5F));

		float labstrokewidth = ffontsize * 0.08F; 
		labelstroke = new BasicStroke(labstrokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, labstrokewidth * 5);
	}
};


/////////////////////////////////////////////
class LineStyleAttr
{
	static int Nlinestyles = 9; // takes in SLS_FILLED

	int linestyle;
	private String sstrokewidth;
	private String sspikegap;
	private String sgapleng;
	private String sspikeheight;
	private String sstrokecolour;

	Color strokecolour;
	float strokewidth;
	float spikegap;
	float gapleng;
	float spikeheight;

	BasicStroke linestroke = null;


	/////////////////////////////////////////////
	LineStyleAttr(LineStyleAttr lls)
	{
		linestyle = lls.linestyle;
    	sstrokewidth = lls.sstrokewidth;
		sspikegap = lls.sspikegap;
		sgapleng = lls.sgapleng;
		sspikeheight = lls.sspikeheight;
		sstrokecolour = lls.sstrokecolour;
//System.out.println("sg3 " + sspikegap + " ls " + linestyle);
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle, String lsstrokewidth, String lsspikegap, String lsgapleng, String lsspikeheight, String lsstrokecolour)
	{
		linestyle = llinestyle;
		sstrokewidth = lsstrokewidth;
		sspikegap = lsspikegap;
		sgapleng = lsgapleng;
		sspikeheight = lsspikeheight;
		sstrokecolour = lsstrokecolour;
//System.out.println("sg2 " + sspikegap + " ls " + linestyle);
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle, float lstrokewidth, float lspikegap, float lgapleng, float lspikeheight, Color lstrokecolour)
	{
		//assert lstrokecolour != null;
		linestyle = llinestyle;
		strokewidth = lstrokewidth;
		spikegap = lspikegap;
		gapleng = lgapleng;
		spikeheight = lspikeheight;
		strokecolour = lstrokecolour;
		SetUpBasicStroke();
	}

	/////////////////////////////////////////////
	void Construct(SubsetAttr lsubsetattr, Color defaultcolor)
	{
		strokewidth = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sstrokewidth), (linestyle != SketchLineStyle.SLS_FILLED ? 2.0F : 0.0F));
		spikegap = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sspikegap), 0.0F);
		gapleng = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sgapleng), 0.0F);
		spikeheight = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sspikeheight), 0.0F);
		gapleng = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sgapleng), 0.0F);
		strokecolour = SubsetAttr.ConvertColour(lsubsetattr.EvalVars(sstrokecolour), defaultcolor);
		SetUpBasicStroke();
	}

	/////////////////////////////////////////////
	void SetUpBasicStroke()
	{
		if (linestyle == SketchLineStyle.SLS_FILLED)
		{
			if (strokewidth != 0.0F)
				TN.emitWarning("nonzero strokewidth " + strokewidth + " on filled line");
		}
		else
		{
			if (strokewidth == 0.0F && strokecolour != null)
				TN.emitWarning("zero strokewidth on line style; use colour=null; colour was " + strokecolour.toString());
		}
		if (spikeheight != 0.0F)
		{
			if ((linestyle != SketchLineStyle.SLS_PITCHBOUND) && (linestyle != SketchLineStyle.SLS_CEILINGBOUND))
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
		linestyle = llinestyle;
	}
	/////////////////////////////////////////////
	float GetStrokeWidth()
	{
		return strokewidth;
	}
	/////////////////////////////////////////////
	void SetColor(Color lcolour)//Used when you want to override the color, eg when colouring by altitude
	{
		strokecolour = lcolour;
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
	void FillMissingAttribsSSA(SymbolStyleAttr ssa)
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
				if (lgsym.sketchsymbolname.equals(ssb.gsymname))
				{
					ssb.gsym = lgsym;
					break;
				}
			}
			if (ssb.gsym == null)
				TN.emitWarning("no match for symbol name " + ssb.gsymname);
			else if ((ssb.gsym.cliparea != null)  && (ssb.gsym.cliparea.aarea != null) && !ssb.bScaleable)
			{
				Rectangle2D sbound = ssb.gsym.cliparea.aarea.getBounds2D();
				ssb.avgsymdim = (sbound.getWidth() + sbound.getHeight()) * ssb.fpicscale / 2;
// far too many of these.  I thought they were reused.
System.out.println("sym dym " + ssb.avgsymdim + " for symbol name " + ssb.gsymname);
			}
		}
	}
}


/////////////////////////////////////////////
class SubsetAttr
{
	// This is used to run variables available in all the parameters
	// so we can change the settings of the colours or all the linestyles and fonts at
	// once, if they refer to a variable rather than an absolute setting.
	// May in future handle expressions.
	Vector vvarsettings = new Vector(); // of strings, pairs of variable->value

	static Color coldefalt = new Color(0);
	String subsetname = null;
	String uppersubset = null;
	SubsetAttr uppersubsetattr = null;
	Vector vsubsetsdown = new Vector();

	String sareamaskcolour = null;
	String sareacolour = null;
		Color areamaskcolour = null;
		Color areacolour = null;

	// list of linestyles
	LineStyleAttr[] linestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];
	// list of shadowlinestyles
	LineStyleAttr[] shadowlinestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];

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
	String EvalVars(String str)
	{
		if ((str == null) || (str.indexOf('$') == -1))
			return str;

		// evaluate in reverse so that settings can refer backwards to earlier settings
		for (int i = vvarsettings.size() - 1; i > 0; i -= 2)
		{
			String svar = (String)vvarsettings.elementAt(i - 1);
			assert svar.charAt(0) == '$';
			int ivar = str.indexOf(svar);
			if ((ivar != -1) && !str.substring(ivar + svar.length()).matches("\\w"))
				str = str.substring(0, ivar) + (String)vvarsettings.elementAt(i) + str.substring(ivar + svar.length());
		}

		// substitute
		if (uppersubsetattr != null)
			str = uppersubsetattr.EvalVars(str);


		// need to evaluate equations here, eg "1.5 * 7"
		str = str.trim();
		//System.out.println(str + " from- " + toString());
		assert str.matches("#[0-9A-Fa-f]{8}|[\\d\\.\\-]*$");
		return str;
	}

	/////////////////////////////////////////////
	static Color ConvertColour(String coldef, Color defalt)
	{
		if (coldef == null)
			return defalt;
		if (coldef.equals("none"))
			return null;

		if (!coldef.startsWith("#"))
			TN.emitError("Colour value should be hex starting with #: " + coldef);

		int col = (int)Long.parseLong(coldef.substring(1), 16);
		return new Color(col, ((col & 0xff000000) != 0));
	}

	/////////////////////////////////////////////
	// this will in future be string tokenizing on * / ( and ) to evaluate equations
	static float ConvertFloat(String fdef, float defalt)
	{
		if (fdef == null)
			return defalt;
		fdef = fdef.trim();
		assert fdef.matches("[\\d\\.\\-]+$");
		return Float.parseFloat(fdef);
	}

	/////////////////////////////////////////////
	// just for consistency
	static String ConvertString(String sdef, String defalt)
	{
		if (sdef == null)
			return defalt;
		return sdef.trim();
	}

	/////////////////////////////////////////////
	// used for copying over SubsetAttrStyles
	SubsetAttr(SubsetAttr lsa)
	{
		subsetname = lsa.subsetname;
		uppersubset = lsa.uppersubset;

		sareamaskcolour = lsa.sareamaskcolour;
		sareacolour = lsa.sareacolour;

		areamaskcolour = lsa.areamaskcolour;
		areacolour = lsa.areacolour;


		// copy defined fonts
		for (int i = 0; i < lsa.labelfonts.size(); i++)
			labelfonts.addElement(new LabelFontAttr((LabelFontAttr)lsa.labelfonts.elementAt(i), this));

		// copy over defined linestyles things
		for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
		{
			linestyleattrs[i] = (lsa.linestyleattrs[i] != null ? new LineStyleAttr(lsa.linestyleattrs[i]) : null);
			shadowlinestyleattrs[i] = (lsa.shadowlinestyleattrs[i] != null ? new LineStyleAttr(lsa.shadowlinestyleattrs[i]) : null);
		}
		// list of symbols.
		for (int i = 0; i < lsa.vsubautsymbols.size(); i++)
			vsubautsymbols.addElement(new SymbolStyleAttr((SymbolStyleAttr)lsa.vsubautsymbols.elementAt(i)));

		// list of variables.
		for (int i = 0; i < lsa.vvarsettings.size(); i++)
			vvarsettings.addElement(lsa.vvarsettings.elementAt(i));
	}


	/////////////////////////////////////////////
	void SetVariable(String svar, String sval)
	{
		if (!svar.matches("\\$\\w+$"))
			TN.emitError("variables must begin with $ and only contain letters and numbers:" + svar + " -> " + sval);
		if (sval.matches(".*\\" + svar + "\\W"))
			TN.emitError("variables must not contain self-references:" + svar + " -> " + sval);
		for (int i = 1; i < vvarsettings.size(); i += 2)
		{
			if (svar.equals((String)vvarsettings.elementAt(i - 1)))
			{
				if (sval.equals(TNXML.sATTR_VARIABLE_VALUE_CLEAR))
				{
					vvarsettings.remove(i - 1);
					vvarsettings.remove(i);
				}
				else
					vvarsettings.setElementAt(sval, i);
				return;
			}
		}
		vvarsettings.addElement(svar);
		vvarsettings.addElement(sval);
		assert (vvarsettings.size() % 2) == 0;
	}

	/////////////////////////////////////////////
	void SetLinestyleAttr(int llinestyle, String lsstrokewidth, String lsspikegap, String lsgapleng, String lsspikeheight, String lsstrokecolour, String lsshadowstrokewidth, String lsshadowstrokecolour)
	{
		if ((llinestyle == SketchLineStyle.SLS_INVISIBLE) || (llinestyle == SketchLineStyle.SLS_CONNECTIVE))
			TN.emitWarning("only renderable linestyles please");
		linestyleattrs[llinestyle] = new LineStyleAttr(llinestyle, lsstrokewidth, lsspikegap, lsgapleng, lsspikeheight, lsstrokecolour);
		shadowlinestyleattrs[llinestyle] = new LineStyleAttr(llinestyle, lsshadowstrokewidth, "0", "0", "0", lsshadowstrokecolour);
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
		//if (res == null)
		//	TN.emitWarning("No font label matches " + llabelfontname + " of subset " + subsetname + " " + labelfonts.size());
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
		//if (res == null)
		//	TN.emitWarning("No symbol name matches " + lsymbolname + " of subset " + subsetname + " " + vsubautsymbols.size());
		return res;
	}

	/////////////////////////////////////////////
	static Color defaltareamaskcolour = new Color(1.0F, 1.0F, 1.0F, 0.55F);
	static Color defaltareacolour = new Color(0.8F, 0.9F, 0.9F, 0.4F);
	void FillMissingAttribs()
	{
//System.out.println("FillMissingAttribsFillMissingAttribs " + subsetname);
		// pull unset defaults down from the upper case
		if ((sareamaskcolour == null) && (uppersubsetattr != null))
			 sareamaskcolour = uppersubsetattr.sareamaskcolour;
		if ((sareacolour == null) && (uppersubsetattr != null))
			 sareacolour = uppersubsetattr.sareacolour;

		areamaskcolour = SubsetAttr.ConvertColour(EvalVars(sareamaskcolour), defaltareamaskcolour);
		areacolour = SubsetAttr.ConvertColour(EvalVars(sareacolour), defaltareacolour);

		// fill in the missing font attributes in each case
		for (int i = 0; i < labelfonts.size(); i++)
		{
			LabelFontAttr lfa = (LabelFontAttr)labelfonts.elementAt(i);
			LabelFontAttr lfaupper = (uppersubsetattr != null ? uppersubsetattr.FindLabelFont(lfa.labelfontname, false) : null);
			lfa.FillMissingAttribsLFA(lfaupper);
		}

		// fill in the missing symbol attributes
		for (int i = 0; i < vsubautsymbols.size(); i++)
		{
			SymbolStyleAttr ssa = (SymbolStyleAttr)vsubautsymbols.elementAt(i);
			SymbolStyleAttr ssaupper = (uppersubsetattr != null ? uppersubsetattr.FindSymbolSpec(ssa.symbolname, 2) : null);
			ssa.FillMissingAttribsSSA(ssaupper);
		}


		// copy over defined linestyles things and fill in gaps
		for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
		{
			if ((i == SketchLineStyle.SLS_INVISIBLE) || (i == SketchLineStyle.SLS_CONNECTIVE))
				continue;

			if (linestyleattrs[i] == null)
				linestyleattrs[i] = (uppersubsetattr != null ? new LineStyleAttr(uppersubsetattr.linestyleattrs[i]) : new LineStyleAttr(i));
			if (shadowlinestyleattrs[i] == null)
				shadowlinestyleattrs[i] = (uppersubsetattr != null ? new LineStyleAttr(uppersubsetattr.shadowlinestyleattrs[i]) : new LineStyleAttr(i));

			linestyleattrs[i].Construct(this, Color.black);
			shadowlinestyleattrs[i].Construct(this, null);
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
	boolean bselectable; // whether we show up in the dropdown list (or is this a partial
	String shortstylename; // used in the dropdown box

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
	SubsetAttrStyle(String lstylename, boolean lbselectable)
	{
		stylename = lstylename;
		if (stylename.length() > 15)
			shortstylename = stylename.substring(0, 9) + "--" + stylename.substring(stylename.length() - 3);
		else
			shortstylename = stylename;
		bselectable = lbselectable;
		System.out.println(" creating " + stylename + (bselectable ? " (selectable)" : "") + " shortname " + shortstylename);
	}

	/////////////////////////////////////////////
	// used for the combobox which needs a short name
	// it would be nice if I could put tooltips
	public String toString()
	{
		return shortstylename;
	}

	/////////////////////////////////////////////
	void ImportSubsetAttrStyle(SubsetAttrStyle lsas)
	{
		for (int i = 0; i < lsas.subsets.size(); i++)
			subsets.addElement(new SubsetAttr((SubsetAttr)lsas.subsets.elementAt(i)));
		if (lsas.sketchgrid != null)
			sketchgrid = lsas.sketchgrid; // copy down from above
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
	// the variables don't work well because the upper subsets don't get copied into the
	// lower subsets and then evaluated.  Only if they are referenced do they get duplicated
	// and then have their variable evaluated in the lower level
    void FillAllMissingAttributes()
    {
		//System.out.println("Updating::" + stylename);
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
		// used to evaluate it in order
		Vector subsetsrevdef = new Vector();
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);
			if (sa.uppersubset == null)
				UnpeelTree(subsetsrevdef, sa);
		}

		// recurse over missing attributes for each subset
		for (int i = 0; i < subsetsrevdef.size();  i++)
			((SubsetAttr)subsetsrevdef.elementAt(i)).FillMissingAttribs();

		// get this part done
		MakeTreeRootNode();
    }
};



