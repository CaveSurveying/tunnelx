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

import java.util.List;
import java.util.ArrayList;

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

	// these settings determine which types of the path are traversed
	// the requirements differ when we are morphing or setting the z values
	boolean bDropdownConnectiveTraversed = true; 
	boolean bCentrelineTraversed = true; // when false, would we also want to abolish linking through centreline nodes as well?  
	float fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections (to deal with vertical line cases)
	boolean bnodeconnZSetrelativeTraversed = true; 
	
	boolean bhcoincideLinesActive;


	/////////////////////////////////////////////
	Parainstancequeue()
	{
		super(cmpparainstance);
	}

	/////////////////////////////////////////////
	void AddNode(OnePathNode opn, float dist, Vector proxdistsetlist)
	{
		opn.proxdist = dist;
		if (proxdistsetlist != null)
			proxdistsetlist.addElement(opn); 
			
		for (OnePath op : opn.vproxpathlist)
		{
			OnePathNode opo = (op.pnstart == opn ? op.pnend : op.pnstart);
			if (opo.proxdist == -1.0F)
			{
				float addd;

				// line is either zero length or not connected
				if (op.IsDropdownConnective())
				{
					if (!bDropdownConnectiveTraversed)
						continue;
					addd = 0.0F;
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
	Parainstancequeue parainstancequeue;

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
		parainstancequeue = new Parainstancequeue();

		// make the proxpathlists
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			opn.proxdist = -1.0F;
			if (opn.vproxpathlist == null)
				opn.vproxpathlist = new ArrayList<OnePath>();
			else
				opn.vproxpathlist.clear();

			if (opn.IsCentrelineNode())
				vcentrelinenodes.add(opn);
		}

		// make the edges coming from the each node
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			op.GetCoords();
			op.pnstart.vproxpathlist.add(op);
			if (op.pnend != op.pnstart)
				op.pnend.vproxpathlist.add(op);
		}
	}




	/////////////////////////////////////////////
	void ShortestPathsToCentrelineNodesSetup(Object o, Vector proxdistsetlist)
	{
		// reset the prox-distances
		if (proxdistsetlist == null)
		{
			for (int i = 0; i < vnodes.size(); i++)
				((OnePathNode)vnodes.elementAt(i)).proxdist = -1.0F;
		}
		
		// for now check so we can get rid of the strict nsquared measure
		else
		{
			assert proxdistsetlist.isEmpty(); 
			for (int i = 0; i < vnodes.size(); i++)
				assert ((OnePathNode)vnodes.elementAt(i)).proxdist == -1.0F; 
		}
		
		// make the queue and eat through it.
		distmincnode = -1.0F;
		distmaxcnode = -1.0F;
		distmax = 0.0F;

		// start on node or midpoint of path
		assert(parainstancequeue.isEmpty());
		if (o instanceof OnePathNode)
			parainstancequeue.AddNode((OnePathNode)o, distmax, proxdistsetlist);
		else if (o instanceof OnePath)
		{
			OnePath op = (OnePath)o;
			distmax = op.linelength / 2;
			parainstancequeue.AddNode(op.pnstart, distmax, proxdistsetlist);
			parainstancequeue.AddNode(op.pnend, distmax, proxdistsetlist);
		}
		else
			assert(false);
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	void ShortestPathsToCentrelineNodes(Object o, OnePathNode[] cennodes, Vector proxdistsetlist)
	{
		ShortestPathsToCentrelineNodesSetup(o, proxdistsetlist);

		// eat through the queue
		int icennodes = 0;
		while (!parainstancequeue.isEmpty())
		{
			parainstance pi = (parainstance)parainstancequeue.first();
			parainstancequeue.remove(pi);
			if (pi.opn.proxdist == -1.0F)
			{
				distmax = pi.sdist;
				parainstancequeue.AddNode(pi.opn, distmax, proxdistsetlist);

				if (pi.opn.IsCentrelineNode())
					distmaxcnode = distmax;

				// we're looking for the closest centreline nodes
				if ((cennodes != null) && (proxdistsetlist == null ? pi.opn.IsCentrelineNode() : pi.opn.IsZSetNode())) 
    			{
					cennodes[icennodes++] = pi.opn;
					if (icennodes == cennodes.length)
						break;  // we've now got enough centreline nodes
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
    	assert(copn.IsCentrelineNode());
		assert(op.vssubsets.isEmpty());

		float xmv = (float)((op.pnstart.pn.getX() + op.pnend.pn.getX()) / 2 - copn.pn.getX());
		float ymv = (float)((op.pnstart.pn.getY() + op.pnend.pn.getY()) / 2 - copn.pn.getY());
		float maxdot = 0.0F;
		OnePath res = null;

		// pick an edge by closest dot-product
		for (OnePath cop : copn.vproxpathlist)
		{
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
		ShortestPathsToCentrelineNodesSetup(op, null);

		// eat through the queue
		while (!parainstancequeue.isEmpty())
		{
			parainstance pi = (parainstance)parainstancequeue.first();
			parainstancequeue.remove(pi);
			if (pi.opn.proxdist == -1.0F)
			{
				distmax = pi.sdist;
				parainstancequeue.AddNode(pi.opn, distmax, null);
				if (pi.opn.IsCentrelineNode())
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

		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).proxdist = -1.0F;
			
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
				ShortestPathsToCentrelineNodes(opn, copn, null);
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
				ShortestPathsToCentrelineNodes(op.pnstart, copn, null);

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
			opn.proxdist = -1.0F;
			if (opn.pnstationlabel == OnePathNode.strConnectiveNode) 
				opn.pnstationlabel = null; 
			assert ((opn.pnstationlabel == null) || !opn.pnstationlabel.equals(OnePathNode.strConnectiveNode)); 
		}
			
		// should we create a warning if a path which is IsZSetNodeConnective doesn't connect to a centreline node?

		// label all the connective nodes
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i); 
			if (opn.IsCentrelineNode())
			{
				for (OnePath op : opn.vproxpathlist)
				{
					if (op.IsZSetNodeConnective()) 
					{
						OnePathNode opo = (op.pnstart == opn ? op.pnend : op.pnstart); 
						if (opo.pnstationlabel == OnePathNode.strConnectiveNode)
							TN.emitWarning("Two Zrelative connectives to the same node"); 
						else if (opo.pnstationlabel != null)
							TN.emitError("Setting centrelinenode to Zrelative connective"); 
						opo.pnstationlabel = OnePathNode.strConnectiveNode;  
						opo.zalt = opn.zalt + op.plabedl.nodeconnzsetrelative; 
System.out.println("ZaltConn " + opn.pnstationlabel + "  " + opn.zalt + " : " + opo.zalt); 
					}
				}
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
		Vector proxdistsetlist = new Vector(); 
			
		// just averages over 4 nodes
		assert os == los;
		OnePathNode[] copn = new OnePathNode[Math.min(vcentrelinenodes.size(), 4)];

		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			if (opn.pnstationlabel == null)
			{
				ShortestPathsToCentrelineNodes(opn, copn, proxdistsetlist);
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

					assert cpn.proxdist != -1.0; 
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
				if (tweight == 0.0)
					tweight = 1.0F; 
				opn.zalt = zaltsum / tweight;
				
				// reset for next application
				for (int j = 0; j < proxdistsetlist.size(); j++)
					((OnePathNode)proxdistsetlist.elementAt(j)).proxdist = -1.0F; 
				proxdistsetlist.removeAllElements(); 
			}
			if ((os.zaltlo > opn.zalt) || (i == 0))
				os.zaltlo = opn.zalt;
			if ((os.zalthi < opn.zalt) || (i == 0))
				os.zalthi = opn.zalt;
		}
	}
};


