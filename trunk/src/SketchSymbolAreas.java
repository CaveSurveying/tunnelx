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

import java.util.SortedSet;
import java.util.TreeSet;



/////////////////////////////////////////////
class SketchSymbolAreas
{
	Vector vconncom = new Vector(); // ConnectiveComponents




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
	static ConnectiveComponentAreas ccaplaceholder = new ConnectiveComponentAreas(false);
	void GetConnComp(Vector lvconnpaths, SortedSet<OneSArea> lvconnareas, OnePath op)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.pthcca == null;
		assert lvconnpaths.isEmpty() && lvconnareas.isEmpty();


		RefPathO rpot = new RefPathO();

		// spread through this connected component completely
		// We used to spread within the sector at each node we meet,
		// but now we only spread round nodes that only have connective pieces on them.

		lvconnpaths.addElement(new RefPathO(op, false));
		op.pthcca = ccaplaceholder;
		int ivcc = -1;
		while (ivcc < lvconnpaths.size())
		{
			// -1 is special case for having just started, and avoiding edge going into array twice
			RefPathO rpo = (ivcc != -1 ? (RefPathO)lvconnpaths.elementAt(ivcc) : new RefPathO(op, true));

			// scan round and check if this is a fully connective type node
			rpot.ccopy(rpo);
			do
			{
				if (rpot.op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
					break;
			} while (!rpot.AdvanceRoundToNode(rpo));

			// all connective; advance round again and add all these connective edges
			if (rpot.cequals(rpo))
			{
				do
				{
					if (rpot.op.IsDropdownConnective())
						;
					else if (rpot.op.pthcca == null)
					{
						assert !Checkopinvconnpath(lvconnpaths, rpot.op);
						rpot.op.pthcca = ccaplaceholder;
						lvconnpaths.addElement(new RefPathO(rpot.op, !rpot.bFore));
					}

					// if we can connect to it, it should be in this list already
					else
					{
						assert rpot.op.pthcca == ccaplaceholder;
						assert Checkopinvconnpath(lvconnpaths, rpot.op);
					}
				} while (!rpot.AdvanceRoundToNode(rpo));
			}
			ivcc++;
		}
		assert op.pthcca == ccaplaceholder;

		// now we have all the components, we make the set of areas for this component.
		OneSArea.iamarkl++;
		for (int i = 0; i < lvconnpaths.size(); i++)
		{
			OnePath sop = ((RefPathO)lvconnpaths.elementAt(i)).op;
			assert sop.pthcca == ccaplaceholder;  // was an assignment
			if ((sop.kaleft != null) && (sop.kaleft.iamark != OneSArea.iamarkl))
			{
				sop.kaleft.iamark = OneSArea.iamarkl;
				if ((sop.kaleft.iareapressig == SketchLineStyle.ASE_KEEPAREA) || (sop.kaleft.iareapressig == SketchLineStyle.ASE_VERYSTEEP))
					lvconnareas.add(sop.kaleft);
			}

			// (both sides should be the same, so this should be unnecessary)
			if ((sop.karight != null) && (sop.karight.iamark != OneSArea.iamarkl))
			{
				sop.karight.iamark = OneSArea.iamarkl;
				if ((sop.karight.iareapressig == SketchLineStyle.ASE_KEEPAREA) || (sop.karight.iareapressig == SketchLineStyle.ASE_VERYSTEEP))
					lvconnareas.add(sop.karight);
			}
		}
	}

	/////////////////////////////////////////////
	// this updates
	void MarkAreasWithConnComp(Vector vareas)
	{
		for (int i = 0; i < vareas.size(); i++)
			((OneSArea)vareas.elementAt(i)).ccalist.clear();
		for (int j = 0; j < vconncom.size(); j++)
		{
			ConnectiveComponentAreas mcca = (ConnectiveComponentAreas)(vconncom.elementAt(j));
			for (OneSArea osa : mcca.vconnareas)
				osa.ccalist.add(mcca);
		}
	}


	/////////////////////////////////////////////
	// make all the connected symbol areas
	void MakeSSA(Vector vpaths)
	{
		vconncom.removeAllElements();
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).pthcca = null;

		Vector lvconnpaths = new Vector();
		SortedSet<OneSArea> lvconnareas = new TreeSet<OneSArea>();

		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.pthcca == null) && !op.IsDropdownConnective())
			{
				GetConnComp(lvconnpaths, lvconnareas, op);

				// find if this is new or not
				ConnectiveComponentAreas mcca = null;
				int i1;
				for (i1 = 0; i1 < vconncom.size(); i1++)
				{
					ConnectiveComponentAreas lmcca = (ConnectiveComponentAreas)(vconncom.elementAt(i1));
					if ((lmcca.vconnareas.size() == lvconnareas.size()) && lmcca.vconnareas.containsAll(lvconnareas))
					{
						mcca = lmcca;
						break;
					}
				}

				if (mcca != null)
					mcca.vconnpaths.addAll(lvconnpaths);
				else // no match
				{
					mcca = new ConnectiveComponentAreas(lvconnpaths, lvconnareas);
					vconncom.add(mcca);
				}

				// copy in all the pthcca values
				for (int k = 0; k < lvconnpaths.size(); k++)
				{
					RefPathO rpo = (RefPathO)lvconnpaths.elementAt(k);
					//assert (rpo.op.iconncompareaindex != -1) && (rpo.op.iconncompareaindex >= liconncompareaindex);
					assert rpo.op.pthcca == ccaplaceholder;
					rpo.op.pthcca = mcca;
				}

				lvconnpaths.clear();
				lvconnareas.clear();
			}
		}


		// find overlapping components
		// do with a sort in future
		for (int i = 0; i < vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(i);
			cca.overlapcomp.add(cca); // always overlaps with self
			for (int j = 0; j < i; j++)
			{
				ConnectiveComponentAreas ccap = (ConnectiveComponentAreas)vconncom.elementAt(j);
				for (OneSArea osa : cca.vconnareas)
				{
					if (ccap.vconnareas.contains(osa))
					{
					    cca.overlapcomp.add(ccap);
					    ccap.overlapcomp.add(cca);
						break;
					}
				}
			}
		}
		TN.emitMessage("connective compnents: " + vconncom.size());
	}



	/////////////////////////////////////////////
	void GetInterferingSymbols(Vector ssymbinterf, ConnectiveComponentAreas cca)
	{
		// the set of paths in each area is unique
		for (ConnectiveComponentAreas ccal : cca.overlapcomp)
		{
			// not an add-all, because we are separating down to the op level
			for (int j = 0; j < ccal.vconnpaths.size(); j++)
				ssymbinterf.addElement(((RefPathO)ccal.vconnpaths.elementAt(j)).op);
		}
	}



	/////////////////////////////////////////////
	void paintWsymbols(GraphicsAbstraction ga)
	{
		// the clip has to be reset for printing otherwise it crashes.
		// this is not how it should be according to the spec
		for (int i = 0; i < vconncom.size(); i++)
		{
			ConnectiveComponentAreas cca = (ConnectiveComponentAreas)vconncom.elementAt(i);
			//ga.setClip(cca.saarea);
			for (int j = 0; j < cca.vconnpaths.size(); j++)
			{
				OnePath op = ((RefPathO)cca.vconnpaths.elementAt(j)).op;
				for (int k = 0; k < op.vpsymbols.size(); k++)
				{
					OneSSymbol msymbol = (OneSSymbol)op.vpsymbols.elementAt(k);
					if (msymbol.ssb.bTrimByArea)
						ga.startSymbolClip(cca);
					msymbol.paintW(ga, false, true);
					if (msymbol.ssb.bTrimByArea)
						ga.endClip();
				}
			}
		}
	}
};



