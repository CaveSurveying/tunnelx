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




/////////////////////////////////////////////
class SketchSymbolAreas
{
	Vector vconncom = new Vector(); // ConnectiveComponents

	void InsertConnArea(Vector lvconnareas, OneSArea ka)
	{
		// insert in order please
		ka.iamark = OneSArea.iamarkl;

		int i = 0;
		for ( ; i < lvconnareas.size(); i++)
			if (ka.hashCode() < lvconnareas.elementAt(i).hashCode())
				break;
		lvconnareas.insertElementAt(ka, i);
	}

	/////////////////////////////////////////////
	void SetConnComparIndex(Vector lvconnpaths, int liconncompareaindex)
	{
		for (int i = 0; i < lvconnpaths.size(); i++)
		{
			RefPathO rpo = (RefPathO)lvconnpaths.elementAt(i);
			assert (rpo.op.iconncompareaindex != -1) && (rpo.op.iconncompareaindex >= liconncompareaindex);
			rpo.op.iconncompareaindex = liconncompareaindex;
		}
	}


	/////////////////////////////////////////////
	boolean Checkopinvconnpath(Vector lvconnpaths, OnePath op)
    {
    	for (int i = 0; i < lvconnpaths.size(); i++)
			if (((RefPathO)lvconnpaths.elementAt(i)).op == op)
				return true;
		return false;
    }

	/////////////////////////////////////////////
	// make list of areas, and the joined area.
	// get connective paths to connect to this object
	/////////////////////////////////////////////
	static RefPathO rpot = new RefPathO();
	static RefPathO rpop = new RefPathO();
	void SetConnComp(Vector lvconnpaths, Vector lvconnareas, OnePath op, int liconncompareaindex)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.iconncompareaindex == -1;
		// leave op.iconncomareaindex unset so we track back along it.
		int ivcc = -1;

		// spread through this connected component completely
		// spreading with the sector at each node we meet.
		while (ivcc < lvconnpaths.size())
		{
			// -1 is special case for having just started, and avoiding edge going into array twice
			RefPathO rpo = (ivcc != -1 ? (RefPathO)lvconnpaths.elementAt(ivcc) : new RefPathO(op, true));
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
					if (rpot.op.IsDropdownConnective())
						;
                	if (rpot.op.iconncompareaindex == -1)
					{
						assert !Checkopinvconnpath(lvconnpaths, rpot.op);
	                	rpot.op.iconncompareaindex = liconncompareaindex;
						lvconnpaths.addElement(new RefPathO(rpot.op, !rpot.bFore));
					}

					// if we can connect to it, it should be in this list already
					else
	                {
	                	assert rpot.op.iconncompareaindex == liconncompareaindex;
						assert Checkopinvconnpath(lvconnpaths, rpot.op);
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
		for (int i = 0; i < lvconnpaths.size(); i++)
		{
			OnePath sop = ((RefPathO)lvconnpaths.elementAt(i)).op;
			sop.iconncompareaindex = liconncompareaindex;
			if ((sop.kaleft != null) && (sop.kaleft.iamark != OneSArea.iamarkl))
				InsertConnArea(lvconnareas, sop.kaleft);
			if ((sop.karight != null) && (sop.karight.iamark != OneSArea.iamarkl))
				InsertConnArea(lvconnareas, sop.karight);
		}
	}

	/////////////////////////////////////////////
	// this updates
	void MarkAreasWithConnComp(Vector vareas)
	{
		for (int i = 0; i < vareas.size(); i++)
			((OneSArea)vareas.elementAt(i)).ccalist.removeAllElements();
		for (int j = 0; j < vconncom.size(); j++)
		{
			ConnectiveComponentAreas mcca = (ConnectiveComponentAreas)(vconncom.elementAt(j));
			for (int i = 0; i < mcca.vconnareas.size(); i++)
				((OneSArea)mcca.vconnareas.elementAt(i)).ccalist.addElement(mcca);
		}
	}


	/////////////////////////////////////////////
	// make all the connected symbol areas
	void MakeSSA(Vector vpaths)
	{
		vconncom.removeAllElements();
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).iconncompareaindex = -1;

		Vector lvconnpaths = new Vector();
		Vector lvconnareas = new Vector();
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.iconncompareaindex == -1) && !op.IsDropdownConnective())
			{
				SetConnComp(lvconnpaths, lvconnareas, op, i);

				// find if this is new or not
				ConnectiveComponentAreas mcca = null;
				int i1;
				for (i1 = 0; i1 < vconncom.size(); i1++)
				{
					mcca = (ConnectiveComponentAreas)(vconncom.elementAt(i1));
					if (mcca.CompareConnAreaList(lvconnareas))
						break;
					mcca = null;
				}

				// we have a match by area
				SetConnComparIndex(lvconnpaths, i1); // i1 is a match of vconncom.size()
				if (mcca != null)
					mcca.vconnpaths.addAll(lvconnpaths);
				// no match
				else
					vconncom.addElement(new ConnectiveComponentAreas(lvconnpaths, lvconnareas));

				lvconnpaths.clear();
				lvconnareas.clear();
			}
		}


		// find overlapping components
		// do with a sort in future
		for (int i = 0; i < vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(i);
			cca.overlapcomp[cca.noverlapcomp++] = i; // always overlaps with self
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
		System.out.println("Conn connective areas n = " + vconncom.size());
	}

	/////////////////////////////////////////////
	// this is used only for the drawing of a selected hatched overlay to see what areas the symbol will be restricted to.
	Vector GetCconnAreas(int iconncompareaindex)
	{
		return ((ConnectiveComponentAreas)vconncom.elementAt(iconncompareaindex)).vconnareas;
	}

	/////////////////////////////////////////////
	// this gets its number from the path, and is used to tell what area the symbols should be restricted to.
	Area GetCCArea(int iconncompareaindex)
	{
		if (iconncompareaindex == -1)
			return null;
		return ((ConnectiveComponentAreas)vconncom.elementAt(iconncompareaindex)).saarea;
	}

	/////////////////////////////////////////////
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



	/////////////////////////////////////////////
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



