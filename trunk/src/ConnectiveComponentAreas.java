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

import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.Vector;
import java.awt.Shape;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

//
//
//
//

// manages the area sharing of symbols

/////////////////////////////////////////////
// corresponds to saarea of the ossameva
class ConnectiveComponentAreas
{
	List<OnePath> vconnpaths;
	SortedSet<OneSArea> vconnareas;

	Area saarea = null;

	// index in SketchSymbolAreas of overlapping connective component areas
    List<ConnectiveComponentAreas> overlapcomp = new ArrayList<ConnectiveComponentAreas>();

	boolean bHasrendered = false; // used to help the ordering in the quality rendering
	boolean bccavisiblesubset = false;

	MutualComponentArea pvconncommutual = null;  // used to link into vconncommutual (each area will be in exactly one)

	ConnectiveComponentAreas(List<OnePath> lvconnpaths, SortedSet<OneSArea> lvconnareas)
	{
		vconnpaths = new ArrayList<OnePath>(lvconnpaths);
		vconnareas = new TreeSet<OneSArea>(lvconnareas);

		// now make the combined area here
		saarea = new Area();  // if there's only one area, do we need to duplicate it (will need to if there are two, because we don't want to affect the original area)
		for (OneSArea osa : vconnareas)
		{
			if ((osa.aarea != null) && ((osa.iareapressig == SketchLineStyle.ASE_KEEPAREA) || (osa.iareapressig == SketchLineStyle.ASE_VERYSTEEP)))
				saarea.add(osa.aarea);
			osa.ccalist.add(this);
		}
	}

	// dummy holder
	ConnectiveComponentAreas(boolean bdum)
		{;}

	/////////////////////////////////////////////
	boolean Overlaps(ConnectiveComponentAreas cca)
	{
		for (OneSArea osa : vconnareas)
		{
			if (cca.vconnareas.contains(osa))
				return true;
		}
		return false;
	}

	/////////////////////////////////////////////
	void paintWsymbolsandwords(GraphicsAbstraction ga)
	{
		// the clip has to be reset for printing otherwise it crashes.
		// this is not how it should be according to the spec

		for (OnePath op : vconnpaths)
		{
			for (int k = 0; k < op.vpsymbols.size(); k++)
			{
				OneSSymbol msymbol = (OneSSymbol)op.vpsymbols.elementAt(k);
				if (msymbol.ssb.symbolareafillcolour == null)
				{
					if (msymbol.ssb.bTrimByArea)
						ga.startSymbolClip(this);
					msymbol.paintW(ga, false, true);
					if (msymbol.ssb.bTrimByArea)
						ga.endClip();
				}

				// Should this have a start/end symbols around it?
				else
					ga.fillArea(this, msymbol.ssb.symbolareafillcolour);
			}

			// do the text that's on this line
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
				op.paintLabel(ga, null);
		}
	}
};


