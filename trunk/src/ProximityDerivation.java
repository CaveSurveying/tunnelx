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

import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;

import java.util.Arrays;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue; 


/////////////////////////////////////////////
class parainstance implements Comparable<parainstance>
{
	double sdist;
	double zdisp; 
	OnePathNode opn;

	/////////////////////////////////////////////
	parainstance(double lsdist, double lzdisp, OnePathNode lopn)
	{
		sdist = lsdist;
		zdisp = lzdisp; 
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
    boolean bAreasTraversed = true;     // tries to link across areas with a straight line to the nodes in the area
	double fcenlinelengthfactor = 10.0; // factor of length added to centreline connections (to deal with vertical line cases)
	boolean bhcoincideLinesActive; 

	/////////////////////////////////////////////
	RefPathO Dsrefpathconn = new RefPathO();
	void AddNode(OnePathNode opn, double dist, double zdisp)
	{
		opn.proxdist = dist;
		proxdistsetlist.add(opn);
        if (bAreasTraversed)
        {
            Dsrefpathconn.ccopy(opn.ropconn);
            do
            {
                AddNodeAreaCrossings(Dsrefpathconn, opn, dist, zdisp); 
            }
            while (!Dsrefpathconn.AdvanceRoundToNode(opn.ropconn));
        }
        Dsrefpathconn.ccopy(opn.ropconn);
        do
        {
            AddNodePathsConnections(Dsrefpathconn, opn, dist, zdisp); 
        }
        while (!Dsrefpathconn.AdvanceRoundToNode(opn.ropconn));
    }
    
    void AddNodeAreaCrossings(RefPathO srefpathconn, OnePathNode opn, double dist, double zdisp)
    {
        OnePath op = srefpathconn.op;
        if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
            return; 
        if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
            return; 
        
        OneSArea osa = (srefpathconn.bFore ? op.karight : op.kaleft); 
        if (osa == null)
            return; 

		GeneralPath gpconnline = new GeneralPath();
        for (RefPathO rpo : osa.refpaths)
        {
            OnePathNode opo = (rpo.bFore ? rpo.op.pnend : rpo.op.pnstart); 
            if (opo.proxdist != -1.0)
                continue; 
//            gpconnline.clear(); 
//            gpconnline.moveTo(opn.pn.getX(), opn.pn.getY()); 
//            gpconnline.lineTo(opo.pn.getX(), opo.pn.getY()); 
//            gpconnline.subtract(osa.aarea); 
//			System.out.println(" "+connlin.isEmpty()); 
//            if (gpconnline.isEmpty())
            {
                double vx = opo.pn.getX() - opn.pn.getX(); 
                double vy = opo.pn.getY() - opn.pn.getY(); 
                double addd = Math.sqrt(vx*vx + vy*vy); 
                prioqueue.offer(new parainstance(dist + addd, zdisp, opo));
            }
        }
    }
    
    void AddNodePathsConnections(RefPathO srefpathconn, OnePathNode opn, double dist, double zdisp)
    {
        OnePathNode opo = srefpathconn.FromNode();
        OnePath op = srefpathconn.op;
        assert opn == srefpathconn.ToNode();
        if (opo.proxdist != -1.0)
            return;

        double addd;
        double addzdisp;
         
        // line is either zero length or not connected
        if (op.IsDropdownConnective())
        {
            if (!bDropdownConnectiveTraversed)
                return;
            addd = 0.0;
            addzdisp = 0.0; 
        }

            // adjust the value so that centrelines don't get used for connecting in favour
        else if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
        {
            if (!bCentrelineTraversed)
                return;
            addd = op.linelength * fcenlinelengthfactor;
            addzdisp = 0.0; 
        }

        else if (op.IsZSetNodeConnective())
        {
            addd = op.linelength;
            addzdisp = (opo == op.pnstart ? op.plabedl.nodeconnzsetrelative : -op.plabedl.nodeconnzsetrelative); 
        }
        else if (op.IsSketchFrameConnective())
        {
            addd = op.linelength;
            addzdisp = (opo == op.pnstart ? op.plabedl.sketchframedef.sfnodeconnzsetrelative : -op.plabedl.sketchframedef.sfnodeconnzsetrelative); 
        }

        // standard addition
        else
        {
            addd = op.linelength;
            addzdisp = 0.0; 
        }
        prioqueue.offer(new parainstance(dist + addd, zdisp + addzdisp, opo));
	}
}


/////////////////////////////////////////////
// weights from centrelines
/////////////////////////////////////////////
class ProximityDerivation
{
	List<OnePathNode> vnodes;  // OnePathNode
	List<OnePath> vpaths;  // OnePath
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
		for (OnePathNode opn : vnodes)
		{
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
		for (OnePathNode opn : vnodes)
			assert opn.proxdist == -1.0;

		// make the queue and eat through it.
		distmincnode = -1.0;
		distmaxcnode = -1.0;
		distmax = 0.0;

		// start on node or midpoint of path
		assert parainstancequeue.prioqueue.isEmpty();
		if (sopn != null)
			parainstancequeue.AddNode(sopn, distmax, 0.0);
		else
		{
			distmax = sop.linelength / 2;
			parainstancequeue.AddNode(sop.pnstart, distmax, 0.0);
			parainstancequeue.AddNode(sop.pnend, distmax, 0.0);
		}
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	int ShortestPathsToCentrelineNodes(OnePathNode sopn, OnePathNode[] cennodes, double[] zdispcennodes)
	{
		assert (zdispcennodes == null) || (zdispcennodes.length == cennodes.length); 
		ShortestPathsToCentrelineNodesSetup(sopn, null);

		// eat through the queue
		int icennodes = 0;
		OnePathNode substitutecennode = sopn; 
		double substitutecennodezdisp = 0.0; 
		while (!parainstancequeue.prioqueue.isEmpty())
		{
			parainstance pi = parainstancequeue.prioqueue.poll();
			if (pi.opn.proxdist != -1.0)
				continue;
			distmax = pi.sdist;
			parainstancequeue.AddNode(pi.opn, distmax, pi.zdisp);

			if (pi.opn.IsCentrelineNode())
			{
				distmaxcnode = distmax;

				// we're looking for the closest centreline nodes
				// or grab the one with the lowest value in the sort if none emerge in this connected component
				if (cennodes != null)
    			{
					if (zdispcennodes != null)
						zdispcennodes[icennodes] = pi.zdisp; 
					cennodes[icennodes++] = pi.opn;
					if (icennodes == cennodes.length)
						break;  // we've now got enough centreline nodes
				}
			}
			else if ((cennodes != null) && (icennodes == 0) && (substitutecennode.compareTo(pi.opn) > 0))
			{
				substitutecennode = pi.opn; 
				substitutecennodezdisp = pi.zdisp; 
			}
		}

		parainstancequeue.prioqueue.clear();
		if (cennodes != null)
		{
			// use the default topleft node if no centreline nodes were found to tie to
			if (icennodes == 0)
			{
				if (zdispcennodes != null)
					zdispcennodes[0] = substitutecennodezdisp; 
				cennodes[0] = substitutecennode;
				icennodes = 1; 
			}
			for (int i = icennodes; i < cennodes.length; i++)
				cennodes[icennodes++] = null;
		}
		return icennodes; 
	}


	/////////////////////////////////////////////
	OnePath EstSubsetToCen(OnePath op, OnePathNode copn, boolean bdatetype)
	{
    	assert copn.IsCentrelineNode();
		assert bdatetype || op.vssubsets.isEmpty();

		double xmv = (op.pnstart.pn.getX() + op.pnend.pn.getX()) / 2 - copn.pn.getX();
		double ymv = (op.pnstart.pn.getY() + op.pnend.pn.getY()) / 2 - copn.pn.getY();
		double maxdot = 0.0;
		OnePath res = null;

		// pick an edge by closest dot-product
		srefpathconn.ccopy(copn.ropconn); 
		do
		{
			OnePath cop = srefpathconn.op; 
// if bdatetype should select one with a __date__ thing in it for sure
			if ((srefpathconn.op.linestyle == SketchLineStyle.SLS_CENTRELINE) && !srefpathconn.op.vssubsets.isEmpty())
			{
				assert copn == srefpathconn.ToNode();
				OnePathNode ocopn = srefpathconn.FromNode();
				double xcv = (ocopn.pn.getX() - copn.pn.getX());
				double ycv = (ocopn.pn.getY() - copn.pn.getY());
				double ldot = Math.abs(xcv * xmv + ycv * ymv);
				if ((res == null) || (ldot > maxdot))
					res = cop;
			}
		}
		while (!srefpathconn.AdvanceRoundToNode(copn.ropconn));
		return res;
	}

	/////////////////////////////////////////////
	// generates the full shortest path diagram from this node
	OnePath EstClosestCenPath(OnePath op, boolean bdatetype)
	{
		ShortestPathsToCentrelineNodesSetup(null, op);

		// eat through the queue
		OnePath opres = null; 
		while (!parainstancequeue.prioqueue.isEmpty())
		{
			parainstance pi = parainstancequeue.prioqueue.poll();
			if (pi.opn.proxdist == -1.0)
			{
				distmax = pi.sdist;
				parainstancequeue.AddNode(pi.opn, distmax, 0.0);
				if (pi.opn.IsCentrelineNode())
				{
					OnePath cop = EstSubsetToCen(op, pi.opn, bdatetype);
					if (cop != null)
					{
						opres = cop; 
						parainstancequeue.prioqueue.clear();
						break;
					}
				}
			}
		}
		
		// reset for next application
		for (OnePathNode lopn : parainstancequeue.proxdistsetlist)
			lopn.proxdist = -1.0; 
		parainstancequeue.proxdistsetlist.clear(); 

		return opres;
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

		for (OnePathNode opn : vnodes)
			opn.proxdist = -1.0;
			
		// work through each of the nodes and calculate for them.
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = vnodes.get(i);
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
		for (OnePath op : vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.drawlab != null) && !op.plabedl.drawlab.equals(""))
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
	// generates the full shortest path diagram from this node
	void SetZaltsFromCNodesByInverseSquareWeight(OneSketch los)
	{
		parainstancequeue.bDropdownConnectiveTraversed = false;
		parainstancequeue.bCentrelineTraversed = false;

		// reset all the connective nodes ones
		for (OnePathNode opn : vnodes)
		{
			opn.proxdist = -1.0;
			if (opn.pnstationlabel == OnePathNode.strConnectiveNode)
				opn.pnstationlabel = null;
			assert ((opn.pnstationlabel == null) || !opn.pnstationlabel.equals(OnePathNode.strConnectiveNode));
		}

		// just averages over 4 nodes
		assert os == los;
		int ncopn = Math.max(1, Math.min(ncentrelinenodes, 4)); 
		OnePathNode[] copn = new OnePathNode[ncopn];
		double[] zdispcennodes = new double[ncopn]; 

		// set all the unset zalts
		boolean bfirst = true; 
		for (OnePathNode opn : vnodes)
		{
			if (!opn.IsCentrelineNode())
			{
				ShortestPathsToCentrelineNodes(opn, copn, zdispcennodes);
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
						assert zdispcennodes[j] == 0.0; 
						zaltsum = cpn.zalt + zdispcennodes[j]; 
						break;
					}
					double weight = 1.0 / (cpn.proxdist * cpn.proxdist);
					zaltsum += (cpn.zalt + zdispcennodes[j]) * weight;
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

			if ((os.zaltlo > opn.zalt) || bfirst)
				os.zaltlo = opn.zalt;
			if ((os.zalthi < opn.zalt) || bfirst)
				os.zalthi = opn.zalt;
			bfirst = false;
		}
	}
};


