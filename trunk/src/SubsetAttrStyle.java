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
	String labelfontname;

		String fontname = null;
		String fontstyle = null;
		int fontsize = -1;
	Color labelcolour = null;
	Font fontlab = null; // filled automatically


	/////////////////////////////////////////////
	LabelFontAttr(String llabelfontname)
	{
		labelfontname = llabelfontname;
	}

	/////////////////////////////////////////////
	LabelFontAttr(LabelFontAttr lfa)
	{
		labelfontname = lfa.labelfontname;
		fontname = lfa.fontname;
		fontstyle = lfa.fontstyle;
		fontsize = lfa.fontsize;
		labelcolour = lfa.labelcolour;
	}

	/////////////////////////////////////////////
	void MakeFont()
	{
		if ((fontlab == null) && (fontname != null) && (fontsize != -1) && (fontstyle != null))
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

	/////////////////////////////////////////////
	void FillMissingAttribs(LabelFontAttr lfa)
	{
		if (lfa == null)
			return;
		assert labelfontname == lfa.labelfontname;
		if (fontname == null)
			fontname = lfa.fontname;
		if (fontstyle == null)
			fontstyle = lfa.fontstyle;
		if (fontsize == -1)
			fontsize = lfa.fontsize;
		if (labelcolour == null)
			labelcolour = lfa.labelcolour;
	}

	/////////////////////////////////////////////
	void FillMissingAttribsWithDefaults()
	{
		if (fontname == null)
		{
			TN.emitWarning("default fontname defined for " + labelfontname);
			fontname = "Serif";
		}
		if (fontstyle == null)
		{
			TN.emitWarning("default fontstyle defined for " + labelfontname);
			fontstyle = "PLAIN";
		}
		if (fontsize == -1)
		{
			TN.emitWarning("default fontsize defined for " + labelfontname);
			fontsize = 10;
		}
		if (labelcolour == null)
		{
			TN.emitWarning("default labelcolour defined for " + labelfontname);
			labelcolour = Color.gray;
		}

		// set font up if we have enough properties
		MakeFont();
	}
};


/////////////////////////////////////////////
class SubsetAttr
{
	String subsetname = null;
	String uppersubset = null;
	SubsetAttr uppersubsetattr = null;
	Vector vsubsetsdown = new Vector();

	Color areamaskcolour = null;
	Color areacolour = null;

	// this applies to all lines, but could work for the different styles soon.
	Color linecolour = null;

	// list for the styles above.
	Vector labelfonts = new Vector(); // type LabelFontAttr

	/////////////////////////////////////////////
	SubsetAttr(String lsubsetname)
	{
		subsetname = lsubsetname;
	}

	/////////////////////////////////////////////
	SubsetAttr(SubsetAttr lsa)
	{
		subsetname = lsa.subsetname;
		uppersubset = lsa.uppersubset;

		areamaskcolour = lsa.areamaskcolour;
		areacolour = lsa.areacolour;
		linecolour = lsa.linecolour;

		for (int i = 0; i < lsa.labelfonts.size(); i++)
			labelfonts.addElement(new LabelFontAttr((LabelFontAttr)lsa.labelfonts.elementAt(i)));
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
			LabelFontAttr lfa = new LabelFontAttr(llabelfontname);
			labelfonts.addElement(lfa);
			return lfa;
		}
		return null;
	}

	/////////////////////////////////////////////
	void FillMissingAttribs(SubsetAttr sa)
	{
		if (areamaskcolour == null)
			areamaskcolour = sa.areamaskcolour;
		if (areacolour == null)
			areacolour = sa.areacolour;
		if (linecolour == null)
			linecolour = sa.linecolour;

		// fill in the missing fonts
		for (int i = 0; i < labelfonts.size(); i++)
		{
			LabelFontAttr lfa = (LabelFontAttr)labelfonts.elementAt(i);
			lfa.FillMissingAttribs(sa.FindLabelFont(lfa.labelfontname, false));
		}
	}

	/////////////////////////////////////////////
	void FillMissingAttribsWithDefaults()
	{
		if (areamaskcolour == null)
		{
			TN.emitWarning("default areamaskcolour defined for " + subsetname);
			areamaskcolour = new Color(1.0F, 1.0F, 1.0F, 0.55F);
		}
		if (areacolour == null)
		{
			TN.emitWarning("default areacolour defined for " + subsetname);
			areacolour = new Color(0.8F, 0.9F, 0.9F, 0.4F);
		}
		if (linecolour == null)
		{
			TN.emitWarning("default linecolour defined for " + subsetname);
			linecolour = Color.black;
		}

		for (int i = 0; i < labelfonts.size(); i++)
			((LabelFontAttr)labelfonts.elementAt(i)).FillMissingAttribsWithDefaults();
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
			}
		}

		// recurse over missing attributes for each subset
		for (int i = 0; i < subsets.size(); i++)
		{
			SubsetAttr sa = (SubsetAttr)subsets.elementAt(i);

			SubsetAttr saupper = sa.uppersubsetattr;
			if (saupper != null)
			{
				saupper.vsubsetsdown.addElement(sa);
				int sc = 0;
				while (saupper != null)
				{
					sa.FillMissingAttribs(saupper);
					saupper = saupper.uppersubsetattr;
					if (sc++ > subsets.size())
						TN.emitError("recursive subset inheritance " + sa.subsetname);
				}
			}

			// finally get everything into working order.
			sa.FillMissingAttribsWithDefaults();
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



