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

import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.util.Vector;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

////////////////////////////////////////////////////////////////////////////////
class MutualComponentAreaScratch
{
	List<OneSSymbol> latticesymbols = new ArrayList<OneSSymbol>();
	List<OneSSymbol> othersymbols = new ArrayList<OneSSymbol>();

	////////////////////////////////////////////////////////////////////////////////
	void SLayoutMutualSymbols(List<OnePath> vmconnpaths)
	{
		assert latticesymbols.isEmpty() && othersymbols.isEmpty();
		for (OnePath op : vmconnpaths)
		{
			for (int j = 0; j < op.vpsymbols.size(); j++)
			{
				OneSSymbol oss = (OneSSymbol)op.vpsymbols.elementAt(j);
				if (oss.ssb.gsym != null)
				{
					if (oss.ssb.bBuildSymbolLatticeAcrossArea)
						latticesymbols.add(oss);
					else
						othersymbols.add(oss);
				}
			}
		}

		//	for (ConnectiveComponentAreas ccal : op.pthcca.overlapcomp)
		//		ssymbinterf.addAll(ccal.vconnpaths);
		//	assert ssymbinterf.size() <= vmconnpaths.size();
		//	assert vmconnpaths.containsAll(ssymbinterf);
   		// ssymbinterf is smaller than vmconnpaths

		// OneSSymbol.islmarkl++; happened outside in this cycle
		for (OneSSymbol oss : othersymbols)
		{
			oss.islmark = OneSSymbol.islmarkl; // comparison against itself.
			oss.RelaySymbolsPosition(vmconnpaths);
		}
		for (OneSSymbol oss : latticesymbols)
		{
			oss.islmark = OneSSymbol.islmarkl; // comparison against itself.
			oss.RelaySymbolsPosition(vmconnpaths);
		}
		latticesymbols.clear();
		othersymbols.clear();
	}
}


////////////////////////////////////////////////////////////////////////////////
// controls the layout in a set of overlapping component areas
class MutualComponentArea
{
	List<ConnectiveComponentAreas> ccamutual = new ArrayList<ConnectiveComponentAreas>();
	SortedSet<OneSArea> osamutual = new TreeSet<OneSArea>();
	List<OnePath> vmconnpaths = new ArrayList<OnePath>();

	static MutualComponentAreaScratch mcascratch = new MutualComponentAreaScratch();

	////////////////////////////////////////////////////////////////////////////////
	boolean hit(GraphicsAbstraction ga, Rectangle windowrect)
	{
		for (OneSArea osa : osamutual)   // could equally loop through ccamutual and look at saarea
		{
			if ((osa.aarea != null) && ga.hit(windowrect, osa.aarea, false))
				return true;
		}
		return false;
	}


	////////////////////////////////////////////////////////////////////////////////
	void LayoutMutualSymbols() // all symbols in this batch
	{
		mcascratch.SLayoutMutualSymbols(vmconnpaths);
	}
};

