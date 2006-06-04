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
import java.awt.geom.Area;


//
//
//
//

// manages the area sharing of symbols

/////////////////////////////////////////////
// corresponds to saarea of the ossameva
class ConnectiveComponentAreas
{
	Vector vconnpaths = new Vector();
	Vector vconnareas = new Vector();
	//Area saarea = null;
	Vector osas = new Vector();
    int[] overlapcomp = new int[55];
    int noverlapcomp = 0;

	boolean bHasrendered = false; // used to help the ordering in the quality rendering
	boolean bccavisiblesubset = false; 

	boolean CompareConnAreaList(Vector lvconn)
	{
		if (lvconn.size() != vconnareas.size())
			return false;
		for (int i = 0; i < lvconn.size(); i++)
			if (vconnareas.elementAt(i) != lvconn.elementAt(i))
				return false;
		return true;
	}

	ConnectiveComponentAreas(Vector lvconnpaths, Vector lvconnareas)
	{
		vconnpaths.addAll(lvconnpaths);
		vconnareas.addAll(lvconnareas);

		// now make the combined area here
		for (int i = 0; i < vconnareas.size(); i++)
		{
			osas.add((OneSArea)vconnareas.elementAt(i));
/*			if (aarea != null)
			{
				if (saarea == null)
					saarea = new Area(aarea);
				else
					saarea.add(aarea);
			}
*/
		}
	}


	/////////////////////////////////////////////
	void paintWsymbolsandwords(GraphicsAbstraction ga)
	{
		// the clip has to be reset for printing otherwise it crashes.
		// this is not how it should be according to the spec

		for (int j = 0; j < vconnpaths.size(); j++)
		{
			OnePath op = ((RefPathO)vconnpaths.elementAt(j)).op;
			for (int k = 0; k < op.vpsymbols.size(); k++)
			{
				OneSSymbol msymbol = (OneSSymbol)op.vpsymbols.elementAt(k);
				if (msymbol.ssb.symbolareafillcolour == null)
				{
					if (msymbol.ssb.bTrimByArea)
						ga.startSymbols(osas, true);
					else
						ga.startSymbols(osas, false);
					msymbol.paintWquality(ga);
					ga.endSymbols();
				}
				else
				{
					ga.fillArea(osas, msymbol.ssb.symbolareafillcolour);//Should this have a start/end symbols around it?
				}
			}

			// do the text that's on this line
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.labfontattr != null))
				op.paintLabel(ga, false);
		}
	}
};


