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
// to have this being automatically updating, we'd need to be able to kill off areas
// as soon as something changed to a connective line associated with it.
class SketchSymbolAreas
{
	// there will be another thread working through this, and more than just these lists
	List<ConnectiveComponentAreas> vconncom = new ArrayList<ConnectiveComponentAreas>();

	// these are overlapping groups of componentareas that need to have their symbols laid out all at once
	List<MutualComponentArea> vconncommutual = new ArrayList<MutualComponentArea>();

	/////////////////////////////////////////////
	// make list of areas, and the joined area.
	// get connective paths to connect to this object
	/////////////////////////////////////////////
	static ConnectiveComponentAreas ccaplaceholder = new ConnectiveComponentAreas(false);
	static void GetConnCompPath(List<OnePath> lvconnpaths, OnePath op)
	{
		// spread through this connected component completely
		// We used to spread within the sector at each node we meet,
		// but now we only spread round nodes that only have connective pieces on them.

		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.pthcca == null;
		assert lvconnpaths.isEmpty();

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
				if ((rpocopy.op.linestyle != SketchLineStyle.SLS_CONNECTIVE) && (rpocopy.op.linestyle != SketchLineStyle.SLS_CENTRELINE))
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
	}
	

	/////////////////////////////////////////////
	static void GetConnComp(List<OnePath> lvconnpaths, SortedSet<OneSArea> lvconnareas, OnePath op, SortedSet<OneSArea> Dvsareas)
	{
		assert op.linestyle == SketchLineStyle.SLS_CONNECTIVE;
		assert op.pthcca == null;
		assert lvconnpaths.isEmpty() && lvconnareas.isEmpty();

		GetConnCompPath(lvconnpaths, op);

		// now we have all the components, we make the set of areas for this component.
		for (OnePath sop : lvconnpaths)
		{
			assert sop.pthcca == ccaplaceholder;  // was an assignment
			if ((sop.kaleft != null) && !lvconnareas.contains(sop.kaleft))  // usually such a small set, this should work
{
//assert Dvsareas.contains(sop.kaleft);  // these shouldn't matter as it's just a connective 
				lvconnareas.add(sop.kaleft);
}
			// (both sides should be the same, so this should be unnecessary)
			if ((sop.karight != null) && !lvconnareas.contains(sop.karight))
{
//assert Dvsareas.contains(sop.karight); 
				lvconnareas.add(sop.karight);
}
		}
for (OneSArea Dosa : lvconnareas)
	assert Dvsareas.contains(Dosa); // check
		}



	/////////////////////////////////////////////
	void MakeConnectiveComponents(Vector vpaths, SortedSet<OneSArea> Dvsareas)
	{
		List<OnePath> lvconnpaths = new ArrayList<OnePath>();
		SortedSet<OneSArea> lvconnareas = new TreeSet<OneSArea>();
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			assert op.pthcca != SketchSymbolAreas.ccaplaceholder;

			if (op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
				continue;
			if (op.pthcca != null)
				continue;
			if (op.IsDropdownConnective())
				continue;
			if (op.vpsymbols.isEmpty())
				continue;

			// vpsymbols is actual symbols from fontcolours; plabedl.vlabsymb would be about symbols requested
//			if (!((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.pthcca == null) && !op.IsDropdownConnective() && !op.vpsymbols.isEmpty()))
//				continue;

			GetConnComp(lvconnpaths, lvconnareas, op, Dvsareas);
			/*for (OnePath Dop : lvconnpaths)
				assert vpaths.contains(Dop); // check
			for (OneSArea Dosa : lvconnareas)
				assert Dvsareas.contains(Dosa); // check
			*/

			// remove connected paths that don't have any symbols on them
			for (int j = lvconnpaths.size() - 1; j >= 0; j--)
			{
				OnePath opj = lvconnpaths.get(j);
				if (opj.vpsymbols.isEmpty())
				{
					assert opj.pthcca == ccaplaceholder;
					opj.pthcca = null;
					OnePath lop = lvconnpaths.remove(lvconnpaths.size() - 1);  // copy last element into deleted element slot
					if (j != lvconnpaths.size())
						lvconnpaths.set(j, lop);
				}
			}

			// find or make component with this set of areas
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
		assert vconncommutual.isEmpty();

		Deque<ConnectiveComponentAreas> ccastack = new ArrayDeque<ConnectiveComponentAreas>();
		int Dconmtotal = 0;
		int Damtotal = 0;
		for (ConnectiveComponentAreas cca : vconncom)
		{
			if (cca.pvconncommutual != null)
				continue;
			assert ccastack.isEmpty();
			ccastack.addFirst(cca);

			MutualComponentArea conncommutual = new MutualComponentArea();
			while (!ccastack.isEmpty())
			{
				ConnectiveComponentAreas scca = ccastack.removeFirst();
				if (scca.pvconncommutual == null)
				{
					conncommutual.MergeIn(scca);
				    for (ConnectiveComponentAreas occa : scca.overlapcomp)
					{
						if (occa.pvconncommutual == null)
							ccastack.addFirst(occa);
						else
							assert (occa.pvconncommutual == conncommutual);
					}
				}
				else
					assert (scca.pvconncommutual == conncommutual);
			}
			Dconmtotal += conncommutual.ccamutual.size();
			Damtotal += conncommutual.osamutual.size(); // won't exceed number of areas since each is in one mutual only.
			vconncommutual.add(conncommutual);
		}
		assert Dconmtotal == vconncom.size();
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
	void MakeSSA(Vector vpaths, SortedSet<OneSArea> vsareas)
	{
		// reset everything
		for (int i = 0; i < vpaths.size(); i++)
			((OnePath)vpaths.elementAt(i)).pthcca = null;
		for (OneSArea osa : vsareas)
			osa.ccalist.clear();
		vconncom.clear();
		vconncommutual.clear();

		MakeConnectiveComponents(vpaths, vsareas);
		CollectOverlappingComponents();
		CollectMutuallyOverlappingComponents();

		TN.emitMessage("connective compnents: " + vconncom.size() + "  mutuals: " + vconncommutual.size());
		//for (ConnectiveComponentAreas cca : vconncom)
		//	TN.emitMessage("compnents overlap: " + cca.overlapcomp.size());
	}
};



