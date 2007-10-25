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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue; 
import java.util.Deque;
import java.util.ArrayDeque;


/////////////////////////////////////////////
class parainstance implements Comparable<parainstance>
{
	double sdist;
	OnePathNode opn;

	/////////////////////////////////////////////
	parainstance(double lsdist, OnePathNode lopn)
	{
		sdist = lsdist;
		opn = lopn;
	}

	/////////////////////////////////////////////
	public int compareTo(parainstance pi)
	{
		double ll = sdist - pi.sdist;
		return (ll < 0.0 ? -1 : (ll > 0.0 ? 1 : 0));
	}
}



/////////////////////////////////////////////
class Parainstancequeue
{
	PriorityQueue<parainstance> prioqueue = new PriorityQueue<parainstance>();
	List<OnePathNode> proxdistsetlist = new ArrayList<OnePathNode>(); // list of nodes that have been visited so their proxdists can be reset
            // may be able to replace proxdistsetlist with a map from OnePathNode to Double, so can get rid of the proxdist

	// these settings determine which types of the path are traversed
	// the requirements differ when we are morphing or setting the z values
	boolean bDropdownConnectiveTraversed = true;
	boolean bCentrelineTraversed = true; // when false, would we also want to abolish linking through centreline nodes as well?
	double fcenlinelengthfactor = 10.0; // factor of length added to centreline connections (to deal with vertical line cases)
	boolean bnodeconnZSetrelativeTraversed = true;
	boolean bhcoincideLinesActive;

	/////////////////////////////////////////////
	RefPathO srefpathconn = new RefPathO();
	void AddNode(OnePathNode opn, double dist)
	{
		opn.proxdist = dist;
		proxdistsetlist.add(opn);

		srefpathconn.ccopy(opn.ropconn);
		do
		{
			OnePathNode opo = srefpathconn.FromNode();
			OnePath op = srefpathconn.op;
			assert opn == srefpathconn.ToNode();
			if (opo.proxdist != -1.0)
				continue;

			double addd;

			// line is either zero length or not connected
			if (op.IsDropdownConnective())
			{
				if (!bDropdownConnectiveTraversed)
					continue;
				addd = 0.0;
			}

				// adjust the value so that centrelines don't get used for connecting in favour
			else if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
			{
				if (!bCentrelineTraversed)
					continue;
				addd = op.linelength * fcenlinelengthfactor;
			}

			else if (op.IsZSetNodeConnective())
			{
				if (!bnodeconnZSetrelativeTraversed)
					continue;
				addd = op.linelength;
			}

				// standard addition
			else
				addd = op.linelength;

			prioqueue.offer(new parainstance(dist + addd, opo));
		}
		while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
	}
}


/////////////////////////////////////////////
// weights from centrelines
/////////////////////////////////////////////
class ProximityDerivation
{
	Vector vnodes;  // OnePathNode
	Vector vpaths;  // OnePath
	OneSketch os;

	int ncentrelinenodes = 0;
	Parainstancequeue parainstancequeue;

	double distmincnode = 0.0;
	double distmaxcnode = 0.0;
	double distmax = 0.0;

	RefPathO srefpathconn = new RefPathO(); // reused object
	/////////////////////////////////////////////
	ProximityDerivation(OneSketch los)
	{
		// make the array parallel to the nodes
		os = los;
		vnodes = os.vnodes;
		vpaths = os.vpaths;
		parainstancequeue = new Parainstancequeue();

        // find the centreline nodes; reset the proxdists
		ncentrelinenodes = 0;
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			opn.proxdist = -1.0;
			if (opn.IsCentrelineNode())
				ncentrelinenodes++; 
		}
	}


	/////////////////////////////////////////////
	void ShortestPathsToCentrelineNodesSetup(OnePathNode sopn, OnePath sop)
	{
		assert ((sopn == null) != (sop == null));
		assert parainstancequeue.proxdistsetlist.isEmpty();
		for (int i = 0; i < vnodes.size(); i++)
			assert ((OnePathNode)vnodes.elementAt(i)).proxdist == -1.0;

		// make the queue and eat through it.
		distmincnode = -1.0;
		distmaxcnode = -1.0;
		distmax = 0.0;

		// start on node or midpoint of path
		assert parainstancequeue.prioqueue.isEmpty();
		if (sopn != null)
			parainstancequeue.AddNode(sopn, distmax);
		else
		{
			distmax = sop.linelength / 2;
			parainstancequeue.AddNode(sop.pnstart, distmax);
			parainstancequeue.AddNode(sop.pnend, distmax);
		}
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	int ShortestPathsToCentrelineNodes(OnePathNode sopn, OnePath sop, OnePathNode[] cennodes, boolean bzsetkind)
	{
		ShortestPathsToCentrelineNodesSetup(sopn, sop);

		// eat through the queue
		int icennodes = 0;
		while (!parainstancequeue.prioqueue.isEmpty())
		{
			parainstance pi = parainstancequeue.prioqueue.poll();
			if (pi.opn.proxdist != -1.0)
				continue;
			distmax = pi.sdist;
			parainstancequeue.AddNode(pi.opn, distmax);

			if (pi.opn.IsCentrelineNode())
				distmaxcnode = distmax;

			// we're looking for the closest centreline nodes
			if ((cennodes != null) && (bzsetkind ? pi.opn.IsZSetNode() : pi.opn.IsCentrelineNode()))
    		{
				cennodes[icennodes++] = pi.opn;
				if (icennodes == cennodes.length)
					break;  // we've now got enough centreline nodes
			}
		}

		parainstancequeue.prioqueue.clear();
		if (cennodes != null)
		{
			for (int i = icennodes; i < cennodes.length; i++)
				cennodes[icennodes++] = null;
		}
		return icennodes; 
	}


	/////////////////////////////////////////////
	OnePath EstSubsetToCen(OnePath op, OnePathNode copn)
	{
    	assert(copn.IsCentrelineNode());
		assert(op.vssubsets.isEmpty());

		double xmv = (op.pnstart.pn.getX() + op.pnend.pn.getX()) / 2 - copn.pn.getX();
		double ymv = (op.pnstart.pn.getY() + op.pnend.pn.getY()) / 2 - copn.pn.getY();
		double maxdot = 0.0;
		OnePath res = null;

		// pick an edge by closest dot-product
		srefpathconn.ccopy(copn.ropconn); 
		do
		{
			OnePath cop = srefpathconn.op; 
			if ((srefpathconn.op.linestyle == SketchLineStyle.SLS_CENTRELINE) && !srefpathconn.op.vssubsets.isEmpty())
			{
				assert copn == srefpathconn.ToNode();
				OnePathNode ocopn = srefpathconn.FromNode();
				double xcv = (ocopn.pn.getX() - copn.pn.getX());
				double ycv = (ocopn.pn.getY() - copn.pn.getY());
				double ldot = xcv * xmv + ycv * ymv;
				if ((res == null) || (ldot > maxdot))
					res = cop;
			}
		}
		while (!srefpathconn.AdvanceRoundToNode(copn.ropconn));
		return res;
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	OnePath EstClosestCenPath(OnePath op)
	{
		ShortestPathsToCentrelineNodesSetup(null, op);

		// eat through the queue
		while (!parainstancequeue.prioqueue.isEmpty())
		{
			parainstance pi = parainstancequeue.prioqueue.poll();
			if (pi.opn.proxdist == -1.0)
			{
				distmax = pi.sdist;
				parainstancequeue.AddNode(pi.opn, distmax);
				if (pi.opn.IsCentrelineNode())
				{
					OnePath cop = EstSubsetToCen(op, pi.opn);
					if (cop != null)
					{
						parainstancequeue.prioqueue.clear();
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

		// reset for next application
		for (OnePathNode lopn : parainstancequeue.proxdistsetlist)
			lopn.proxdist = -1.0; 
		parainstancequeue.proxdistsetlist.clear(); 
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void PrintCNodeProximity(int nnodes)  // number of nodes we print
	{
		System.out.println("******   BEGIN PRINT PROXIMITIES   ******");
		// centrelinenodes by dist
		OnePathNode[] copn = new OnePathNode[Math.min(ncentrelinenodes, nnodes)];

		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).proxdist = -1.0;
			
		// work through each of the nodes and calculate for them.
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.IsCentrelineNode())
			{
				System.out.print("station, ");
				System.out.print(i);
				System.out.print(", ");
				System.out.print(opn.pnstationlabel);
			}
			else
			{
				ShortestPathsToCentrelineNodes(opn, null, copn, true);
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
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.drawlab != null) && !op.plabedl.drawlab.equals(""))
			{
				ShortestPathsToCentrelineNodes(op.pnstart, null, copn, true);

				System.out.print("label, ");
				System.out.print(op.plabedl.sfontcode);
				System.out.print(", ");
				System.out.print(op.plabedl.drawlab.replace('\n', ' '));
				PrintProxOneNode(copn);
				System.out.println(""); // newline
			}
		}

		System.out.println("******   END PRINT PROXIMITIES   ******");
	}

	/////////////////////////////////////////////
	void SetZRelativeConn()
	{
		// reset all the connective nodes ones
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			opn.proxdist = -1.0;
			if (opn.pnstationlabel == OnePathNode.strConnectiveNode)
				opn.pnstationlabel = null;
			assert ((opn.pnstationlabel == null) || !opn.pnstationlabel.equals(OnePathNode.strConnectiveNode));
		}

		// should we create a warning if a path which is IsZSetNodeConnective doesn't connect to a centreline node?

		// label all the connective nodes
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (!opn.IsCentrelineNode())
				continue;
			srefpathconn.ccopy(opn.ropconn);
			do
			{
				OnePath op = srefpathconn.op;
				if (op.IsZSetNodeConnective())
				{
					assert opn == srefpathconn.ToNode();
					OnePathNode opo = srefpathconn.FromNode();
					if (opo.pnstationlabel == OnePathNode.strConnectiveNode)
						TN.emitWarning("Two Zrelative connectives to the same node");
					else if (opo.pnstationlabel != null)
						TN.emitError("Setting centrelinenode to Zrelative connective");
					opo.pnstationlabel = OnePathNode.strConnectiveNode;
					opo.zalt = opn.zalt + op.plabedl.nodeconnzsetrelative;
System.out.println("ZaltConn " + opn.pnstationlabel + "  " + opn.zalt + " : " + opo.zalt);
				}
			}
			while (!srefpathconn.AdvanceRoundToNode(opn.ropconn));
		}

		// set the frame sketch nodes
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (op.IsSketchFrameConnective())
			{
				op.pnstart.pnstationlabel = OnePathNode.strConnectiveNode;
				op.pnstart.zalt = op.plabedl.sketchframedef.sfnodeconnzsetrelative;
System.out.println("Framesketch setting zalt " + "  " + op.pnstart.zalt);
			}
		}
	}



	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void SetZaltsFromCNodesByInverseSquareWeight(OneSketch los)
	{
		parainstancequeue.bDropdownConnectiveTraversed = false;
		parainstancequeue.bCentrelineTraversed = false;
		parainstancequeue.bnodeconnZSetrelativeTraversed = false;

		// should we create a warning if a path which is IsZSetNodeConnective doesn't connect to a centreline node?

		SetZRelativeConn();  // sets the height of the non-centreline nodes

		// just averages over 4 nodes
		assert os == los;
		OnePathNode[] copn = new OnePathNode[Math.min(ncentrelinenodes, 4)];

		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel == null)
			{
				ShortestPathsToCentrelineNodes(opn, null, copn, false);
				double tweight = 0.0;
				double zaltsum = 0.0;
				for (int j = 0; j < copn.length; j++)
				{
					OnePathNode cpn = copn[j];
					if (cpn == null)
					{
						if (j == 0)
						{
							tweight = 1.0;
							zaltsum = 0.0;
						}
						break;
					}

					assert cpn.proxdist != -1.0;
					if (cpn.proxdist == 0.0) // station node case
					{
						tweight = 1.0;
						zaltsum = cpn.zalt;
						break;
					}
					double weight = 1.0 / (cpn.proxdist * cpn.proxdist);
					zaltsum += cpn.zalt * weight;
					tweight += weight;
				}
				if (tweight == 0.0)
					tweight = 1.0;
				opn.zalt = (float)(zaltsum / tweight);

				// reset for next application
				for (OnePathNode lopn : parainstancequeue.proxdistsetlist)
				{
					assert lopn.proxdist >= 0.0;
					lopn.proxdist = -1.0;
				}
				parainstancequeue.proxdistsetlist.clear();
			}

			if ((os.zaltlo > opn.zalt) || (i == 0))
				os.zaltlo = opn.zalt;
			if ((os.zalthi < opn.zalt) || (i == 0))
				os.zalthi = opn.zalt;
		}
	}
};


