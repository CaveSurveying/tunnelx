////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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

import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

/////////////////////////////////////////////
class parainstance
{
	float sdist;
	OnePathNode opn;

	parainstance(float lsdist, OnePathNode lopn)
	{
		sdist = lsdist;
		opn = lopn;
	}
}

/////////////////////////////////////////////
class Ccmpparainstance implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		float ll = ((parainstance)o1).sdist - ((parainstance)o2).sdist;
		return (ll < 0.0 ? -1 : (ll > 0.0 ? 1 : 0));
	}
}

/////////////////////////////////////////////
class Ccmpopnprox implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		float ll = ((OnePathNode)o1).proxdist - ((OnePathNode)o2).proxdist;
		return (ll < 0.0 ? -1 : (ll > 0.0 ? 1 : 0));
	}
}

/////////////////////////////////////////////
class Parainstancequeue extends TreeSet
{
	static Ccmpparainstance cmpparainstance = new Ccmpparainstance();
	static float fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections to deal with vertical line cases

	Parainstancequeue()
	{
		super(cmpparainstance);
	}

	void AddNode(OnePathNode opn, float dist)
	{
		opn.proxdist = dist;
		for (int i = 0; i < opn.vproxpathlist.size(); i++)
		{
			OnePath op = (OnePath)opn.vproxpathlist.elementAt(i);
			OnePathNode opo = (op.pnstart == opn ? op.pnend : op.pnstart);
			if (opo.proxdist == -1.0F)
			{
				float addd = op.linelength * (op.linestyle != SketchLineStyle.SLS_CENTRELINE ? 1.0F : fcenlinelengthfactor);
				add(new parainstance(dist + addd, opo));
			}
		}
	}
}


/////////////////////////////////////////////
// weights from centrelines
/////////////////////////////////////////////
class ProximityDerivation
{
	Vector vnodes;
	Vector vpaths;
	OneSketch os;

	Vector vcentrelinenodes = new Vector();
	Parainstancequeue parainstancequeue = new Parainstancequeue();

	float distmincnode = 0.0F;
	float distmaxcnode = 0.0F;
	float distmax = 0.0F;

	/////////////////////////////////////////////
	ProximityDerivation(OneSketch los)
	{
		// make the array parallel to the nodes
		os = los;
		vnodes = os.vnodes;
		vpaths = os.vpaths;

		// make the proxpathlists
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			opn.proxdist = -1.0F;
			if (opn.vproxpathlist == null)
				opn.vproxpathlist = new Vector();
			else
				opn.vproxpathlist.removeAllElements();

			if (opn.pnstationlabel != null)
				vcentrelinenodes.add(opn);
		}

		// make the edges coming from the each node
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			op.GetCoords();
			op.pnstart.vproxpathlist.addElement(op);
			if (op.pnend != op.pnstart)
				op.pnend.vproxpathlist.addElement(op);
		}
	}



	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void ShortestPathsToCentrelineNodes(OnePathNode opn)
	{
		// reset the prox-distances
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).proxdist = -1.0F;

		// make the queue and eat through it.
		distmincnode = -1.0F;
		distmaxcnode = -1.0F;
		distmax = 0.0F;

		parainstancequeue.AddNode(opn, distmax);
		while (!parainstancequeue.isEmpty())
		{
			parainstance pi = (parainstance)parainstancequeue.first();
			parainstancequeue.remove(pi);
			if (pi.opn.proxdist == -1.0F)
			{
				distmax = pi.sdist;
				parainstancequeue.AddNode(pi.opn, distmax);
				if (pi.opn.pnstationlabel != null)
				{
					if (distmincnode == -1.0)
						distmincnode = distmax;
					distmaxcnode = distmax;
				}
			}
		}
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void PrintCNodeProximity(int nnodes)  // number of nodes we print
	{
		System.out.println("******   BEGIN PRINT PROXIMITIES   ******"); 
		// centrelinenodes by dist
		OnePathNode[] cnbydist = new OnePathNode[vcentrelinenodes.size()];
		Ccmpopnprox cmpopnprox = new Ccmpopnprox();

		// work through each of the nodes and calculate for them.
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel == null)
			{
				ShortestPathsToCentrelineNodes(opn);
				for (int j = 0; j < vcentrelinenodes.size(); j++)
					cnbydist[j] = (OnePathNode)vcentrelinenodes.elementAt(j);
				Arrays.sort(cnbydist, cmpopnprox);

				System.out.print(i);
				for (int j = 0; j < nnodes; j++)
				{
					System.out.print("  ");
					System.out.print(vnodes.indexOf(cnbydist[j]));
					System.out.print("  ");
					System.out.print(cnbydist[j].proxdist);
				}
			}

			// centreline node type -- make it easy for Martin's parser to load
			else
			{
				System.out.print(i);
				for (int j = 0; j < nnodes; j++)
					System.out.print("  -1  -1");
			}
			System.out.println(""); // newline
		}
		System.out.println("******   END PRINT PROXIMITIES   ******"); 
	}


	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void SetZaltsFromCNodesByInverseSquareWeight(OneSketch los)
	{
		assert os == los;
		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel == null)
			{
				ShortestPathsToCentrelineNodes(opn);
				float tweight = 0.0F;
				float zaltsum = 0.0F;
				for (int j = 0; j < vcentrelinenodes.size(); j++)
				{
					OnePathNode cpn = (OnePathNode)vcentrelinenodes.elementAt(j);
					if (cpn.proxdist == 0.0F)
					{
						tweight = 1.0F;
						zaltsum = cpn.zalt;
						break;
					}
					float weight = 1.0F / (cpn.proxdist * cpn.proxdist);
					zaltsum += cpn.zalt * weight;
					tweight += weight;
				}
				opn.zalt = zaltsum / tweight;
			}
			if ((os.zaltlo > opn.zalt) || (i == 0))
				os.zaltlo = opn.zalt;
			if ((os.zalthi < opn.zalt) || (i == 0))
				os.zalthi = opn.zalt;
		}

		// make the proxpathlists
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			opn.proxdist = -1.0F;
			if (opn.vproxpathlist == null)
				opn.vproxpathlist = new Vector();
			else
				opn.vproxpathlist.removeAllElements();

			if (opn.pnstationlabel != null)
				vcentrelinenodes.add(opn);
		}
	}
};


