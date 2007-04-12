////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2007  Julian Todd.
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

import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.List;
import java.util.ArrayList;

import java.util.regex.Matcher; 
import java.util.regex.Pattern; 


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

	SortedMap<String, SubsetAttr> subsetsdownmap = new TreeMap<String, SubsetAttr>();

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
