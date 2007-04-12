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

