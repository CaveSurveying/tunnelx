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

import javax.swing.AbstractAction;
import java.util.Vector;
import java.awt.event.ActionEvent;

/////////////////////////////////////////////
// this thing should be disentangleable.
class AutSymbolAc extends AbstractAction
{
	SketchLineStyle sketchlinestyle;
	String name;
	String shdesc;
	boolean bOverwrite;
	OneTunnel vgsymbols;

	// this is the set of symbols that get generated when this autsymbol is found
	// (could be in a base class without the abstract action)
	SSymbolBase[] ssba;

	/////////////////////////////////////////////
	public AutSymbolAc(String lname, String lshdesc, boolean lbOverwrite, Vector lssba)
	{
		super(lname);
		shdesc = lshdesc;
		bOverwrite = lbOverwrite;
		name = lname;

		putValue(SHORT_DESCRIPTION, shdesc);

		ssba = new SSymbolBase[lssba.size()];
		for (int k = 0; k < lssba.size(); k++)
			ssba[k] = (SSymbolBase)lssba.elementAt(k);
	}

	/////////////////////////////////////////////
	public void SetUp(OneTunnel lvgsymbols, SketchLineStyle lsketchlinestyle)
	{
		vgsymbols = lvgsymbols;
		sketchlinestyle = lsketchlinestyle;
		for (int k = 0; k < ssba.length; k++)
		{
			// now match each with symbol name to sketch
			SSymbolBase ssb = ssba[k];
			for (int j = 0; j < vgsymbols.tsketches.size(); j++)
			{
				OneSketch lgsym = (OneSketch)vgsymbols.tsketches.elementAt(j);
				if (lgsym.sketchname.equals(ssb.gsymname))
				{
					ssb.gsym = lgsym;
					break;
				}
			}
			if (ssb.gsym == null)
				System.out.println("no match for symbol name " + ssb.gsymname);
		}
	}

	/////////////////////////////////////////////
	public void actionPerformed(ActionEvent e)
	{
		sketchlinestyle.LSpecSymbol(bOverwrite, name);
	}
};


