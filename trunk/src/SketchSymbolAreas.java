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
import java.awt.Graphics2D;
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
	Area saarea;
    int[] overlapcomp = new int[50];
    int noverlapcomp = 0;


	/////////////////////////////////////////////
	boolean Checkopinvconnpath(OnePath op)
    {
    	for (int i = 0; i < vconnpaths.size(); i++)
			if (((RefPathO)vconnpaths.elementAt(i)).op == op)
				return true;
		return false;
    }

	/////////////////////////////////////////////
	// make list of areas, and the joined area.
	// get connective paths to connect to this object
	/////////////////////////////////////////////
	static RefPathO rpot = new RefPathO();
	static RefPathO rpop = new RefPathO();
	ConnectiveComponentAreas(OnePath op, int liconncompareaindex)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.iconncompareaindex == -1;
		// leave op.iconncomareaindex unset so we track back along it.
		int ivcc = -1;

		// spread through this connected component completely
		// spreading with the sector at each node we meet.
		while (ivcc < vconnpaths.size())
		{
			// -1 is special case for having just started, and avoiding edge going into array twice
			RefPathO rpo = (ivcc != -1 ? (RefPathO)vconnpaths.elementAt(ivcc) : new RefPathO(op, true));
			rpot.ccopy(rpo);
            rpop.ccopy(rpo);

			// scan round to find the preceding edge
			do
			{
				if ((rpot.op.linestyle != SketchLineStyle.SLS_CONNECTIVE) && (rpot.op.linestyle != SketchLineStyle.SLS_CENTRELINE))
		            rpop.ccopy(rpot);
			} while (!rpot.AdvanceRoundToNode(rpo));

			// scan from this edge to next, or to this edge
			rpot.ccopy(rpop);
			while (true)
			{
				boolean brepback = rpot.AdvanceRoundToNode(rpop);
				if (rpot.op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
				{
                	if (rpot.op.iconncompareaindex == -1)
					{
						assert !Checkopinvconnpath(rpot.op);
	                	rpot.op.iconncompareaindex = liconncompareaindex;
						vconnpaths.addElement(new RefPathO(rpot.op, !rpot.bFore));
					}

					// if we can connect to it, it should be in this list already
					else
	                {
	                	assert rpot.op.iconncompareaindex == liconncompareaindex;
						assert Checkopinvconnpath(rpot.op);
					}
				}
				else if (rpot.op.linestyle == SketchLineStyle.SLS_CENTRELINE)
					; // this type doesn't bound the sector.
				else
					break;
				if (brepback)
					break;
			}
			ivcc++;
		}

		// now we have all the components, we make the set of areas for this component.
		OneSArea.iamarkl++;
		for (int i = 0; i < vconnpaths.size(); i++)
		{
			OnePath sop = ((RefPathO)vconnpaths.elementAt(i)).op;
			sop.iconncompareaindex = liconncompareaindex;
			if ((sop.kaleft != null) && (sop.kaleft.iamark != OneSArea.iamarkl))
			{
				vconnareas.addElement(sop.kaleft);
				sop.kaleft.iamark = OneSArea.iamarkl;
   			}
			if ((sop.karight != null) && (sop.karight.iamark != OneSArea.iamarkl))
			{
				vconnareas.addElement(sop.karight);
				sop.karight.iamark = OneSArea.iamarkl;
   			}
		}

		// now make the combined area here
		saarea = null;
		for (int i = 0; i < vconnareas.size(); i++)
		{
			Area aarea = ((OneSArea)vconnareas.elementAt(i)).aarea;
			if (saarea == null)
				saarea = new Area(aarea);
			else
				saarea.add(aarea);
		}
	}
};




/////////////////////////////////////////////
class SketchSymbolAreas
{
	Vector vconncom = new Vector(); // ConnectiveComponents

	// make all the connected symbol areas
	void MakeSSA(Vector vpaths)
	{
		vconncom.removeAllElements();
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).iconncompareaindex = -1;

		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.iconncompareaindex == -1))
				vconncom.addElement(new ConnectiveComponentAreas(op, vconncom.size()));
		}

		// find overlapping components
		// do with a sort in future
		for (int i = 1; i < vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(i);
			cca.overlapcomp[cca.noverlapcomp++] = i;
			for (int j = 0; j < i; j++)
			{
				ConnectiveComponentAreas ccap = (ConnectiveComponentAreas)vconncom.elementAt(j);
				for (int k = 0; k < cca.vconnareas.size(); k++)
				{
					if (ccap.vconnareas.contains(cca.vconnareas.elementAt(k)))
					{
					    cca.overlapcomp[cca.noverlapcomp++] = j;
					    ccap.overlapcomp[ccap.noverlapcomp++] = i;
						break;
					}
				}
			}
		}

		TN.emitMessage("Unique group areas: " + vconncom.size());
	}

	// this is used only for the drawing of a selected hatched overlay to see what areas the symbol will be restricted to.
	Vector GetCconnAreas(int iconncompareaindex)
	{
		return ((ConnectiveComponentAreas)vconncom.elementAt(iconncompareaindex)).vconnareas;
	}

	// this gets its number from the path, and is used to tell what area the symbols should be restricted to.
	Area GetCCArea(int iconncompareaindex)
	{
		if (iconncompareaindex == -1)
			return null;
		return ((ConnectiveComponentAreas)vconncom.elementAt(iconncompareaindex)).saarea;
	}

	void GetInterferingSymbols(Vector res, int iconncompareaindex)
	{
		res.removeAllElements();
		ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(iconncompareaindex);

		// the set of paths in each area is unique
		for (int i = 0; i < cca.noverlapcomp; i++)
		{
			ConnectiveComponentAreas ccal = (ConnectiveComponentAreas)vconncom.elementAt(cca.overlapcomp[i]);
			for (int j = 0; j < ccal.vconnpaths.size(); j++)
				res.addElement(((RefPathO)ccal.vconnpaths.elementAt(j)).op);
		}
	}

	Color colfacarea
	void paintWsymbols(Graphics2D g2D)
	{
		// the clip has to be reset for printing otherwise it crashes.
		// this is not how it should be according to the spec
		Shape sclip = g2D.getClip();
		for (int i = 0; i < vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(i);
			//g2D.setClip(cca.saarea);
			for (int j = 0; j < cca.vconnpaths.size(); j++)
			{
				OnePath op = ((RefPathO)cca.vconnpaths.elementAt(j)).op;
				for (int k = 0; k < op.vpsymbols.size(); k++)
				{
					OneSSymbol msymbol = (OneSSymbol)op.vpsymbols.elementAt(k);
					if (msymbol.bTrimByArea)
						g2D.setClip(cca.saarea);
					else
						g2D.setClip(sclip);
					msymbol.paintW(g2D, false, true);
				}
			}
			g2D.setClip(sclip);
		}
	}
};



