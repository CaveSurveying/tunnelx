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

import java.util.List;
import java.util.ArrayList;

import java.awt.geom.Rectangle2D;

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
	public void SetUp(List<OneSketch> lvgsymbolstsketches)
	{
		for (SSymbolBase ssb : ssymbolbs)
		{
			// now match each with symbol name to sketch
			for (OneSketch lgsym : lvgsymbolstsketches)
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

