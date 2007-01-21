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
// controls the layout in a set of overlapping component areas
class MutualComponentArea
{
	List<ConnectiveComponentAreas> ccamutual = new ArrayList<ConnectiveComponentAreas>();
	SortedSet<OneSArea> osamutual = new TreeSet<OneSArea>();
	List<OnePath> vmconnpaths = new ArrayList<OnePath>();

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
		List<OnePath> ssymbinterf = new ArrayList<OnePath>();
		for (OnePath op : vmconnpaths)
		{
			assert ssymbinterf.isEmpty();
			for (ConnectiveComponentAreas ccal : op.pthcca.overlapcomp)
				ssymbinterf.addAll(ccal.vconnpaths);

			assert ssymbinterf.size() <= vmconnpaths.size();
			assert vmconnpaths.containsAll(ssymbinterf);

			for (int j = 0; j < op.vpsymbols.size(); j++)
			{
				OneSSymbol oss = (OneSSymbol)op.vpsymbols.elementAt(j);
				oss.islmark = OneSSymbol.islmarkl; // comparison against itself.

				if (oss.ssb.gsym != null)
					oss.RelaySymbolsPosition(ssymbinterf, op.pthcca);
			}
			ssymbinterf.clear();
		}
	}
};

