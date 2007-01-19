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
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;



/////////////////////////////////////////////
class SketchSymbolAreas
{
	List<ConnectiveComponentAreas> vconncom = new ArrayList<ConnectiveComponentAreas>();

// this is not done yet -- need to get these together, and these will be the basis for 
// big batched symbol layouts
	List< List<ConnectiveComponentAreas> > vconncommutual = new ArrayList< List<ConnectiveComponentAreas> >();

	/////////////////////////////////////////////
	// make list of areas, and the joined area.
	// get connective paths to connect to this object
	/////////////////////////////////////////////
	static ConnectiveComponentAreas ccaplaceholder = new ConnectiveComponentAreas(false);
	static void GetConnComp(List<OnePath> lvconnpaths, SortedSet<OneSArea> lvconnareas, OnePath op)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.pthcca == null;
		assert lvconnpaths.isEmpty() && lvconnareas.isEmpty();

		// spread through this connected component completely
		// We used to spread within the sector at each node we meet,
		// but now we only spread round nodes that only have connective pieces on them.

		Deque<RefPathO> lconnpathsrpstack = new ArrayDeque<RefPathO>();
		lconnpathsrpstack.addFirst(new RefPathO(op, false)); // going both directions
		lconnpathsrpstack.addFirst(new RefPathO(op, true));
		lvconnpaths.add(op);
		RefPathO rpocopy = new RefPathO();
		op.pthcca = SketchSymbolAreas.ccaplaceholder;
		while (!lconnpathsrpstack.isEmpty())
		{
			// scan round and check if this is a fully connective type node
			RefPathO rpolast = lconnpathsrpstack.removeFirst();
			rpocopy.ccopy(rpolast);
			do
			{
				if (rpocopy.op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
					break;
			}
			while (!rpocopy.AdvanceRoundToNode(rpolast));

			if (!rpocopy.cequals(rpolast))
				continue;

			// all connective; advance round again and add all these connective edges
			do
			{
				if (rpocopy.op.IsDropdownConnective())
					;
				else if (rpocopy.op.pthcca == null)
				{
					assert !lvconnpaths.contains(rpocopy.op);
					rpocopy.op.pthcca = ccaplaceholder;
					lconnpathsrpstack.addFirst(new RefPathO(rpocopy.op, !rpocopy.bFore));
					lvconnpaths.add(rpocopy.op);
				}

				// if we can connect to it, it should be in this list already
				else
				{
					assert rpocopy.op.pthcca == ccaplaceholder;
					assert lvconnpaths.contains(rpocopy.op);
				}
			} while (!rpocopy.AdvanceRoundToNode(rpolast));
		}
		assert op.pthcca == ccaplaceholder;

		// now we have all the components, we make the set of areas for this component.
		OneSArea.iamarkl++;
		for (OnePath sop : lvconnpaths)
		{
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
	void MarkAreasWithConnComp(Vector vareas)
	{
		for (int i = 0; i < vareas.size(); i++)
			((OneSArea)vareas.elementAt(i)).ccalist.clear();
		for (ConnectiveComponentAreas mcca : vconncom)
		{
			for (OneSArea osa : mcca.vconnareas)
				osa.ccalist.add(mcca);
		}
	}

	/////////////////////////////////////////////
	void MakeConnectiveComponents(Vector vpaths)
	{
		List<OnePath> lvconnpaths = new ArrayList<OnePath>();
		SortedSet<OneSArea> lvconnareas = new TreeSet<OneSArea>();
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (!((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.pthcca == null) && !op.IsDropdownConnective() && !op.vpsymbols.isEmpty()))
				continue;

			GetConnComp(lvconnpaths, lvconnareas, op);

			ConnectiveComponentAreas mcca = null;
			for (ConnectiveComponentAreas lmcca : vconncom)
			{
				if ((lmcca.vconnareas.size() == lvconnareas.size()) && lmcca.vconnareas.containsAll(lvconnareas))
				{
					mcca = lmcca;
					break;
				}
			}

			if (mcca == null)
			{
				mcca = new ConnectiveComponentAreas(lvconnpaths, lvconnareas);
				vconncom.add(mcca);
			}
			else
				mcca.vconnpaths.addAll(lvconnpaths);

			// copy in all the pthcca values
			for (OnePath sop : lvconnpaths)
			{
				//assert (rpo.op.iconncompareaindex != -1) && (rpo.op.iconncompareaindex >= liconncompareaindex);
				assert sop.pthcca == ccaplaceholder;
				sop.pthcca = mcca;
			}

			lvconnpaths.clear();
			lvconnareas.clear();
		}
	}

	/////////////////////////////////////////////
	void CollectMutuallyOverlappingComponents()
	{
		
	}

	/////////////////////////////////////////////
	void CollectOverlappingComponents()
	{
		// find overlapping components
		for (int i = 0; i < vconncom.size(); i++)  // had used iterators, but they're not copyable
		{
			ConnectiveComponentAreas cca = vconncom.get(i);
			cca.overlapcomp.add(cca); // always overlaps with self

			for (int j = i + 1; j < vconncom.size(); j++)
			{
				ConnectiveComponentAreas cca1 = vconncom.get(j);
				if (cca.Overlaps(cca1))
				{
				    cca.overlapcomp.add(cca1);
				    cca1.overlapcomp.add(cca);
				}
			}
		}
	}


	/////////////////////////////////////////////
	// (re)make all the connected symbol areas
	void MakeSSA(Vector vpaths, Vector vareas)
	{
		vconncom.clear();
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).pthcca = null;

		MakeConnectiveComponents(vpaths);
		MarkAreasWithConnComp(vareas);
		CollectOverlappingComponents();
		CollectMutuallyOverlappingComponents();

		TN.emitMessage("connective compnents: " + vconncom.size());
		//for (ConnectiveComponentAreas cca : vconncom)
		//	TN.emitMessage("compnents overlap: " + cca.overlapcomp.size());
	}


	/////////////////////////////////////////////
	void GetInterferingSymbols(Vector ssymbinterf, ConnectiveComponentAreas cca)
	{
		// the set of paths in each area is unique
		for (ConnectiveComponentAreas ccal : cca.overlapcomp)
		{
			// not an add-all, because we are separating down to the op level
			for (OnePath op : ccal.vconnpaths)
				ssymbinterf.addElement(op);
		}
	}

	/////////////////////////////////////////////
	void paintWsymbols(GraphicsAbstraction ga)
	{
		// the clip has to be reset for printing otherwise it crashes.
		// this is not how it should be according to the spec
		for (ConnectiveComponentAreas cca : vconncom)
		{
			//ga.setClip(cca.saarea);
			for (OnePath op : cca.vconnpaths)
			{
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



