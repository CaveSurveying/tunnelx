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
	void ShortestPathsToCentrelineNodesSetup(Object o)
	{
		// reset the prox-distances
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).proxdist = -1.0F;

		// make the queue and eat through it.
		distmincnode = -1.0F;
		distmaxcnode = -1.0F;
		distmax = 0.0F;

		// start on node or midpoint of path
		assert(parainstancequeue.isEmpty());
		if (o instanceof OnePathNode)
			parainstancequeue.AddNode((OnePathNode)o, distmax);
		else if (o instanceof OnePath)
		{
			OnePath op = (OnePath)o;
			distmax = op.linelength / 2;
			parainstancequeue.AddNode(op.pnstart, distmax);
			parainstancequeue.AddNode(op.pnend, distmax);
		}
		else
			assert(false);
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void ShortestPathsToCentrelineNodes(Object o, OnePathNode[] cennodes)
	{
		ShortestPathsToCentrelineNodesSetup(o);

		// eat through the queue
		int icennodes = 0;
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
					if (cennodes != null)
					{
						cennodes[icennodes++] = pi.opn;
						if (icennodes == cennodes.length)
							break;
					}
				}
			}
		}
		parainstancequeue.clear();
		if (cennodes != null)
			while (icennodes < cennodes.length)
				cennodes[icennodes++] = null;
	}


	/////////////////////////////////////////////
	OnePath EstSubsetToCen(OnePath op, OnePathNode copn)
	{
    	assert(copn.pnstationlabel != null);
		assert(op.vssubsets.isEmpty());

		float xmv = (float)((op.pnstart.pn.getX() + op.pnend.pn.getX()) / 2 - copn.pn.getX());
		float ymv = (float)((op.pnstart.pn.getY() + op.pnend.pn.getY()) / 2 - copn.pn.getY());
		float maxdot = 0.0F;
		OnePath res = null;

		// pick an edge by closest dot-product
		for (int i = 0; i < copn.vproxpathlist.size(); i++)
		{
			OnePath cop = (OnePath)copn.vproxpathlist.elementAt(i);
			if ((cop.linestyle == SketchLineStyle.SLS_CENTRELINE) && !cop.vssubsets.isEmpty())
			{
				assert((copn == cop.pnstart) || (copn == cop.pnend));
				OnePathNode ocopn = (copn == cop.pnstart ? cop.pnend : cop.pnstart);
				float xcv = (float)(ocopn.pn.getX() - copn.pn.getX());
				float ycv = (float)(ocopn.pn.getY() - copn.pn.getY());
				float ldot = xcv * xmv + ycv * ymv;
				if ((res == null) || (ldot > maxdot))
					res = cop;
			}
		}
		return res;
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	OnePath EstClosestCenPath(OnePath op)
	{
		ShortestPathsToCentrelineNodesSetup(op);

		// eat through the queue
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
					OnePath cop = EstSubsetToCen(op, pi.opn);
					if (cop != null)
					{
						parainstancequeue.clear();
						return cop;
					}
				}
			}
		}
		return null;
	}

	/////////////////////////////////////////////
	void PrintProxOneNode(OnePathNode[] copn)
	{
		for (int j = 0; j < copn.length; j++)
		{
			OnePathNode cpn = copn[j];
			if (cpn != null)
			{
				System.out.print(", ");
				System.out.print(vnodes.indexOf(cpn));
				System.out.print(", ");
				System.out.print(cpn.pnstationlabel);
				System.out.print(", ");
				System.out.print(cpn.proxdist);
			}
			else
				System.out.print(", -1, , -1");
		}
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void PrintCNodeProximity(int nnodes)  // number of nodes we print
	{
		System.out.println("******   BEGIN PRINT PROXIMITIES   ******");
		// centrelinenodes by dist
		OnePathNode[] copn = new OnePathNode[Math.min(vcentrelinenodes.size(), nnodes)];

		// work through each of the nodes and calculate for them.
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel != null)
			{
				System.out.print("station, ");
				System.out.print(i);
				System.out.print(", ");
				System.out.print(opn.pnstationlabel);
			}
			else
			{
				ShortestPathsToCentrelineNodes(opn, copn);
				System.out.print("node, ");
				System.out.print(i);
				PrintProxOneNode(copn);
			}
			System.out.println(""); // newline
		}

		// do the labels
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && !op.plabedl.drawlab.equals(""))
			{
				ShortestPathsToCentrelineNodes(op.pnstart, copn);

				System.out.print("label, ");
				System.out.print(SketchLineStyle.labstylenames[op.plabedl.ifontcode]);
				System.out.print(", ");
				System.out.print(op.plabedl.drawlab.replace('\n', ' '));
				PrintProxOneNode(copn);
				System.out.println(""); // newline
			}
		}

		System.out.println("******   END PRINT PROXIMITIES   ******");
	}


	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void SetZaltsFromCNodesByInverseSquareWeight(OneSketch los)
	{
		// just averages over 4 nodes
		assert os == los;
		OnePathNode[] copn = new OnePathNode[Math.min(vcentrelinenodes.size(), 4)];

		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel == null)
			{
				ShortestPathsToCentrelineNodes(opn, copn);
				float tweight = 0.0F;
				float zaltsum = 0.0F;
				for (int j = 0; j < copn.length; j++)
				{
					OnePathNode cpn = copn[j];
					if (cpn == null)
					{
						if (j == 0)
						{
							tweight = 1.0F;
							zaltsum = 0.0F;
						}
						break;
					}
					if (cpn.proxdist == 0.0F) // station node case
					{
						tweight = 1.0F;
						zaltsum = cpn.zalt;
						break;
					}
					float weight = 1.0F / (cpn.proxdist * cpn.proxdist);
					zaltsum += cpn.zalt * weight;
					tweight += weight;
				}
				assert(tweight != 0.0);
				opn.zalt = zaltsum / tweight;
			}
			if ((os.zaltlo > opn.zalt) || (i == 0))
				os.zaltlo = opn.zalt;
			if ((os.zalthi < opn.zalt) || (i == 0))
				os.zalthi = opn.zalt;
		}
	}
};


