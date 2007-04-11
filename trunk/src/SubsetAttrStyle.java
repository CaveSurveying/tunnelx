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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Collections; 
import java.util.Set;

import java.util.regex.Matcher; 
import java.util.regex.Pattern; 

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;

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
	int iautsymboverwrite; // style of button (filled in after construction)
	String autsymbdesc; // filled in after construction
	float symbolstrokewidth = -1.0F;
	Color symbolcolour = SubsetAttr.coldefalt;
	List<SSymbolBase> ssymbolbs = null; 
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
		iautsymboverwrite = ssa.iautsymboverwrite; 
		autsymbdesc = ssa.autsymbdesc; 
		symbolstrokewidth = ssa.symbolstrokewidth;
		symbolcolour = ssa.symbolcolour;
		ssymbolbs = new ArrayList<SSymbolBase>();
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
			ssymbolbs = new ArrayList<SSymbolBase>();
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
		for (SSymbolBase ssb : ssymbolbs)
		{
			// now match each with symbol name to sketch
			for (OneSketch lgsym : lvgsymbols.tsketches)
			{
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
				ssb.avgsymdim = (sbound.getWidth() + sbound.getHeight()) * Math.abs(ssb.fpicscale) / 2;
// far too many of these.  I thought they were reused.
//System.out.println("sym dym " + ssb.avgsymdim + " for symbol name " + ssb.gsymname);
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
	Map<String, String> vvarsettings = new HashMap<String, String>(); // variable->value

	static Color coldefalt = new Color(0);
	String subsetname = null;
	String uppersubset = null;
	SubsetAttr uppersubsetattr = null;
	List<SubsetAttr> vsubsetsdown = new ArrayList<SubsetAttr>();
	boolean bViewhidden = false; // tsvpathsviz - would mean it doesn't get into tsvpathsviz
	
	String sareamaskcolour = null;
	String sareacolour = null;
		Color areamaskcolour = null;
		Color areacolour = null;

	LineStyleAttr[] linestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];
	LineStyleAttr[] shadowlinestyleattrs = new LineStyleAttr[LineStyleAttr.Nlinestyles];

	Map<String, LabelFontAttr> labelfontsmap = new HashMap<String, LabelFontAttr>(); 
    Map<String, SymbolStyleAttr> subautsymbolsmap = new HashMap<String, SymbolStyleAttr>(); 

	/////////////////////////////////////////////
	SubsetAttr(String lsubsetname)
	{
		subsetname = lsubsetname;
	}

	/////////////////////////////////////////////
	static List<String> alreadyusedeval = new ArrayList<String>(); 
	String EvalVars(String str)
	{
		alreadyusedeval.clear(); 
		if (str == null)
			return str; 

		while (str.indexOf('$') != -1)
		{
			//String Dstr = str; 
			int naue = alreadyusedeval.size(); 
			Matcher mdvar = Pattern.compile("(\\$\\w+);?").matcher(str); 
			while (mdvar.find())
			{
				if (!alreadyusedeval.contains(mdvar.group(1)) && vvarsettings.containsKey(mdvar.group(1)))
					alreadyusedeval.add(mdvar.group(1)); 
			}
			if (naue == alreadyusedeval.size())
				 break; 
			while (naue < alreadyusedeval.size())
			{
				// escape the leading $
				str = str.replaceAll("\\" + alreadyusedeval.get(naue) + ";?", vvarsettings.get(alreadyusedeval.get(naue))); 
				naue++; 
			}
			//System.out.println("Variable substitution: " + Dstr + " => " + str); 
		}
		
		// substitute
		if (uppersubsetattr != null)
			return uppersubsetattr.EvalVars(str);

		// need to evaluate equations here, eg "1.5 * 7"
		str = str.trim();
		//System.out.println(str + " from- " + toString());
		//assert str.matches("#[0-9A-Fa-f]{8}|[\\d\\.\\-]*$");
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
		for (LabelFontAttr lfa : lsa.labelfontsmap.values())
			labelfontsmap.put(lfa.labelfontname, new LabelFontAttr(lfa, this));

		// copy over defined linestyles things
		for (int i = 0; i < LineStyleAttr.Nlinestyles; i++)
		{
			linestyleattrs[i] = (lsa.linestyleattrs[i] != null ? new LineStyleAttr(lsa.linestyleattrs[i]) : null);
			shadowlinestyleattrs[i] = (lsa.shadowlinestyleattrs[i] != null ? new LineStyleAttr(lsa.shadowlinestyleattrs[i]) : null);
		}
		// list of symbols.
		for (SymbolStyleAttr ssa : lsa.subautsymbolsmap.values())
			subautsymbolsmap.put(ssa.symbolname, new SymbolStyleAttr(ssa)); 

		// list of variables.
		vvarsettings.putAll(lsa.vvarsettings);
	}


	/////////////////////////////////////////////
	void SetVariable(String svar, String sval)
	{
		if (!svar.matches("\\$\\w+$"))
			TN.emitError("variables must begin with $ and only contain letters and numbers:" + svar + " -> " + sval);
		if (sval.matches(".*\\" + svar + "\\W"))
			TN.emitError("variables must not contain self-references:" + svar + " -> " + sval);

		if (sval.equals(TNXML.sATTR_VARIABLE_VALUE_CLEAR))
			vvarsettings.remove(svar); 
		else
			vvarsettings.put(svar, sval); 
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
	static Color defaltareamaskcolour = new Color(1.0F, 1.0F, 1.0F, 0.55F);
	static Color defaltareacolour = new Color(0.8F, 0.9F, 0.9F, 0.4F);
	void FillMissingAttribs() // this function is called recursing down the tree in order
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
		for (LabelFontAttr lfa : labelfontsmap.values())
			lfa.FillMissingAttribsLFA(uppersubsetattr != null ? uppersubsetattr.labelfontsmap.get(lfa.labelfontname) : null);

		// fill in the missing symbol attributes
		for (SymbolStyleAttr ssa : subautsymbolsmap.values())
			ssa.FillMissingAttribsSSA(uppersubsetattr != null ? uppersubsetattr.subautsymbolsmap.get(ssa.symbolname) : null);

		// copy in any symbols that aren't there already
		if (uppersubsetattr != null)
		{
			for (SymbolStyleAttr ussa : uppersubsetattr.subautsymbolsmap.values())
			{
				if (!subautsymbolsmap.containsKey(ussa.symbolname))
					subautsymbolsmap.put(ussa.symbolname, ussa);  
			}
			for (LabelFontAttr ulfa : uppersubsetattr.labelfontsmap.values())
			{
				if (!labelfontsmap.containsKey(ulfa.labelfontname))
					labelfontsmap.put(ulfa.labelfontname, ulfa);  
			}
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
		return (bViewhidden ? ("[X] " + subsetname) : subsetname);
	}
};


/////////////////////////////////////////////
class SubsetAttrStyle implements Comparable<SubsetAttrStyle>
{
	String stylename;
	boolean bselectable; // whether we show up in the dropdown list (or is this a partial
	String shortstylename; // used in the dropdown box
	int iloadorder; // used for sorting the order of loading, so we can put them in the drop-down box correctly; since using a map loses this ordering

	Map<String, SubsetAttr> msubsets = new HashMap<String, SubsetAttr>(); 
	//for (Map.Entry<String, SubsetAttr> e : m.entrySet())
	//System.out.println(e.getKey() + ": " + e.getValue());
	
	DefaultMutableTreeNode dmroot = new DefaultMutableTreeNode("root");
	DefaultTreeModel dmtreemod = new DefaultTreeModel(dmroot);

	List<String> unattributedss = new ArrayList<String>(); // contains the same, but as SubsetAttrs
	DefaultMutableTreeNode dmunattributess = new DefaultMutableTreeNode("_Unattributed_");
	List<String> xsectionss = new ArrayList<String>(); // those that appear superficially to act as subsets (they contain a centreline of elevation type)
	DefaultMutableTreeNode dmxsectionss = new DefaultMutableTreeNode("_XSections_");
	TreePath tpxsection = (new TreePath(dmroot)).pathByAddingChild(dmxsectionss); 

	SketchGrid sketchgrid = null;

	

	/////////////////////////////////////////////
	public int compareTo(SubsetAttrStyle sas)
	{
		return iloadorder - sas.iloadorder; 
	}
	
	
	/////////////////////////////////////////////
	void MakeTreeRootNode()
	{
		dmroot.removeAllChildren();
		Deque<DefaultMutableTreeNode> dmtnarr = new ArrayDeque<DefaultMutableTreeNode>(); 
		
		// build the tree downwards from each primary root node
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubsetattr != null)
				continue; 

			DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(sa);
			dmroot.add(cnode);
			dmtnarr.addFirst(cnode); 
			while (!dmtnarr.isEmpty())
			{
				DefaultMutableTreeNode lcnode = dmtnarr.removeFirst();
				SubsetAttr lsa = (SubsetAttr)lcnode.getUserObject(); 
				for (SubsetAttr dsa : lsa.vsubsetsdown)
				{
					DefaultMutableTreeNode ncnode = new DefaultMutableTreeNode(dsa);
					lcnode.add(ncnode);
					dmtnarr.addFirst(ncnode); 
				}
			}
		}

		// this is a separate dynamic folder with the subsets that don't have any subset attributes on them
		dmroot.add(dmunattributess); 
		dmroot.add(dmxsectionss); 
		dmtreemod.reload(dmroot); 
	}

	
	/////////////////////////////////////////////
	void TreeListUnattributedSubsets(Vector vpaths)
	{
		unattributedss.clear(); 
		xsectionss.clear(); 
		for (int j = 0; j < vpaths.size(); j++)
		{
			OnePath op = (OnePath)vpaths.elementAt(j);
			for (String ssubset : op.vssubsets)
			{
				if (msubsets.containsKey(ssubset))
					continue; 
				if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.plabedl != null) && (op.plabedl.centrelineelev != null) && op.plabedl.centrelineelev.equals(ssubset) && !xsectionss.contains(ssubset)) 
					xsectionss.add(ssubset); 									
				if (!unattributedss.contains(ssubset))
					unattributedss.add(ssubset); 
			}
		}		

		dmunattributess.removeAllChildren(); 
		dmxsectionss.removeAllChildren(); 
		Collections.reverse(xsectionss); 
		for (String ssubset : xsectionss)
			dmxsectionss.add(new DefaultMutableTreeNode(ssubset)); 
		for (String ssubset : unattributedss)
		{
			if (!xsectionss.contains(ssubset))
				dmunattributess.add(new DefaultMutableTreeNode(ssubset)); 
		}
		dmtreemod.reload(dmunattributess); 
		dmtreemod.reload(dmxsectionss); 
	}
	
	/////////////////////////////////////////////
	SubsetAttrStyle(String lstylename, int liloadorder, boolean lbselectable)
	{
		stylename = lstylename;
		iloadorder = liloadorder; 
		if (stylename.length() > 15)
			shortstylename = stylename.substring(0, 9) + "--" + stylename.substring(stylename.length() - 3);
		else
			shortstylename = stylename;
		bselectable = lbselectable;
		System.out.println(" creating " + stylename + (bselectable ? " (selectable)" : "") + " shortname " + shortstylename);
	}

	/////////////////////////////////////////////
	// these settings will be used to set a second layer of invisibility (entirely hide -- not just grey out -- from the list anything that is in any of these bViewhidden subsets.  
	void ToggleViewHidden(Set<String> vsselectedsubsets, boolean btransitive)
	{
		Deque<SubsetAttr> sarecurse = new ArrayDeque<SubsetAttr>(); 
		for (String ssubsetname : vsselectedsubsets)
			sarecurse.addFirst(msubsets.get(ssubsetname)); 
		while (!sarecurse.isEmpty())
		{
			SubsetAttr sa = sarecurse.removeFirst(); 
			sa.bViewhidden = !sa.bViewhidden; 
			if (!btransitive)
				continue; 
			for (SubsetAttr dsa : sa.vsubsetsdown)
				sarecurse.addFirst(dsa); 
		}
		dmtreemod.reload(dmroot); // should call nodesChanged on the individual ones (tricky because of no pointers to TreeNodes), but keep it simple for now
	}
	
	/////////////////////////////////////////////
	// used for the combobox which needs a short name
	// it would be nice if I could put tooltips
	public String toString()
	{
		return shortstylename;
	}

	/////////////////////////////////////////////
	void ImportSubsetAttrStyle(SubsetAttrStyle lsas)  // does a huge copy of a batch of subsetattributestyles
	{
		for (SubsetAttr lsa : lsas.msubsets.values())
		{
			SubsetAttr nsa = new SubsetAttr(lsa); 
			//subsets.add(nsa);
			msubsets.put(lsa.subsetname, nsa); 
		}
		if (lsas.sketchgrid != null)
			sketchgrid = lsas.sketchgrid; // copy down from above
	}

	/////////////////////////////////////////////
	SubsetAttr FindSubsetAttr(String lsubsetname, boolean bcreate)
	{
		SubsetAttr sa = msubsets.get(lsubsetname); 
		if (bcreate && (sa == null))
		{			
			sa = new SubsetAttr(lsubsetname);
			//subsets.add(sa);
			msubsets.put(sa.subsetname, sa); 
		}
		return sa;
	}



	/////////////////////////////////////////////
	// the variables don't work well because the upper subsets don't get copied into the
	// lower subsets and then evaluated.  Only if they are referenced do they get duplicated
	// and then have their variable evaluated in the lower level
    void FillAllMissingAttributes()
    {
		//System.out.println("Updating::" + stylename);
		// set pointers up
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubset != null)
			{
				sa.uppersubsetattr = FindSubsetAttr(sa.uppersubset, false);
				if (sa.uppersubsetattr == null)
					TN.emitWarning("Upper subset " + sa.uppersubset + " not found of " + sa.subsetname);
				else
					sa.uppersubsetattr.vsubsetsdown.add(sa);
			}
		}

		// make the tree in reverse order of definition (or could have set up a partial sort)
		// used to evaluate it in order
		List<SubsetAttr> subsetsrevdef = new ArrayList<SubsetAttr>();
		for (SubsetAttr sa : msubsets.values())
		{
			if (sa.uppersubset == null)
				SelectedSubsetStructure.VRecurseSubsetsdown(subsetsrevdef, sa);
		}

		// recurse over missing attributes for each subset
		for (SubsetAttr sa : subsetsrevdef)
			sa.FillMissingAttribs();

		// get this part done
		MakeTreeRootNode();
    }
};



