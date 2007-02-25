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

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.io.IOException;

import java.awt.BasicStroke;
import java.awt.Shape;


//
//
// OnePathNode
//
//

// this is going to implement Comparable<OnePathNode> containing the function compareTo()

/////////////////////////////////////////////
class OnePathNode
{
	Point2D.Float pn = null;

	// the altitude of this node (inherited into areas so we can draw them in order).
	// also a value that's saved into the xml file when IsCentrelineNode()
	float zalt = 0.0F;

	private Shape pnell = null; // for drawing.
	float currstrokew = -1.0F;   // used for lazy evaluation to make the shapes
	int nclosenodesbefore = 0; // number of nodes close (within strokewidth distance) of this node when we added it in.

	int pathcount = 0; // number of paths which link to this node.
	    int pathcountch; // spare variable for checking the pathcount

	static String strConnectiveNode = "__CONNECTIVE NODE__";  // used to overload value of pnstationlabel
	String pnstationlabel = null; // lifted from the centreline legs, and used to tell if this is a centreline node
	OnePath opconn = null; // connection to a single path which we can circle around, and will match the pathcount

	int icnodevisiblesubset = 0;

	Vector vproxpathlist = null; // used to get record paths connecting for proximity calcs
	float proxdist = -1.0F;

    // value set by other weighting operations for previewing
    int icolindex = -1;

	/////////////////////////////////////////////
	int compareTo(OnePathNode opn)
	{
		if (pn.x != opn.pn.x)
			return (pn.x < opn.pn.x ? -1 : 1);
		if (pn.y != opn.pn.y)
			return (pn.y < opn.pn.y ? -1 : 1);
		assert false;
		return hashCode() - opn.hashCode();
		// also consider the numbering given when coming in from the XML file.
	}
	// may need also to implement the equals.


	/////////////////////////////////////////////
	boolean IsCentrelineNode()
	{
		return ((pnstationlabel != null) && (pnstationlabel != strConnectiveNode));
	}

	/////////////////////////////////////////////
	boolean IsZSetNode()
	{
		return ((pnstationlabel != null) && (pnstationlabel != null));
	}

	/////////////////////////////////////////////
	void DumpNodeInfo(LineOutputStream los, String sten) throws IOException
	{
		los.WriteLine(sten + ": " +
					  (pnstationlabel == null ? "" : (pnstationlabel == strConnectiveNode ? "RelConnNode" : "Centrelinenode=" + pnstationlabel)) +
					  "  z=" + zalt);
	}

	/////////////////////////////////////////////
	// can be used for running through the array again.
	void SetNodeCloseBefore(Vector vnodes, int n)
	{
		// count how many nodes are within strokewidth of this node
		nclosenodesbefore = 0;
		currstrokew = -1.0F;
		for (int i = 0; i < n; i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			assert opn != this;
			if ((Math.abs(pn.getX() - opn.pn.getX()) < SketchLineStyle.strokew) && (Math.abs(pn.getY() - opn.pn.getY()) < SketchLineStyle.strokew))
				nclosenodesbefore++;
		}
	}

	/////////////////////////////////////////////
	// this is lazy evaluation, since we can change the stroke and then reload an old sketch.
	Shape Getpnell()
	{
		if (currstrokew != SketchLineStyle.strokew)
		{
			currstrokew = SketchLineStyle.strokew;
			if (nclosenodesbefore == 0)
				pnell = new Rectangle2D.Float((float)pn.getX() - 2 * currstrokew, (float)pn.getY() - 2 * currstrokew, 4 * currstrokew, 4 * currstrokew);

			// these are other shapes to discriminate
			else
			{
				GeneralPath gpnell = new GeneralPath();
				float rad = currstrokew * (nclosenodesbefore + 5) / 2.0F;
				int nnodes = (nclosenodesbefore + 8) / 2;
				boolean balternating = ((nclosenodesbefore > 2) && ((nclosenodesbefore % 2) == 1));
				gpnell.moveTo((float)pn.getX(), (float)pn.getY() - (balternating ? -rad : rad));
				for (int i = 1; i < nclosenodesbefore + 3; i++)
				{
					float lrad = (balternating && ((i % 2) == 1) ? rad * 0.2F : rad);
					double thet = Math.PI * 2.0 * i / (nclosenodesbefore + 3);
					gpnell.lineTo((float)(pn.getX() + lrad * Math.sin(thet)), (float)(pn.getY() - (balternating ? -lrad : lrad) * Math.cos(thet)));
				}
				gpnell.closePath();
				pnell = gpnell;
			}
		}
		return pnell;
	}


	/////////////////////////////////////////////
	OnePathNode(float x, float y, float z)
	{
		pn = new Point2D.Float(x, y);
		zalt = z;
		pathcount = 0;
	}


	/////////////////////////////////////////////
	// this is where we track round
	boolean CheckPathCount()
	{
		pathcountch = 0;

		OnePath op = opconn;
		assert ((this == op.pnend) || (this == op.pnstart));
		boolean bFore = (op.pnend == this);
		float ptang = op.GetTangent(!bFore);
		boolean btangcrossx = false;
		do
		{
        	pathcountch++;
			assert pathcountch <= pathcount;
			if (!bFore)
        	{
				bFore = op.baptlfore;
				op = op.aptailleft;
			}
			else
			{
				bFore = op.bapfrfore;
				op = op.apforeright;
        	}
			assert ((!bFore ? op.pnstart : op.pnend) == this);
			float tang = op.GetTangent(!bFore);

			// we're allowed one crossing of the x-axis for wrap-around
			if (tang < ptang)
			{
				assert !btangcrossx;
				btangcrossx = true;
			}
			ptang = tang;
		}
		while (!((op == opconn) && (bFore == (op.pnend == this))));

		assert pathcountch == pathcount;
		return true;
	}


	/////////////////////////////////////////////
	OnePath GetDropDownConnPath()
	{
		OnePath op = opconn;
		boolean bFore = (op.pnend == this);
		do
		{
			if (op.IsDropdownConnective() && (op.pnstart == this))
				return op;
			if (!bFore)
        	{
				bFore = op.baptlfore;
				op = op.aptailleft;
			}
			else
			{
				bFore = op.bapfrfore;
				op = op.apforeright;
        	}
		}
		while (!((op == opconn) && (bFore == (op.pnend == this))));

		// make a new one
		OnePath opddconn = new OnePath(this);
		opddconn.LineTo((float)pn.getX(), (float)pn.getY());
		opddconn.EndPath(null);
		opddconn.linestyle = SketchLineStyle.SLS_CONNECTIVE;
		opddconn.plabedl = new PathLabelDecode();
		opddconn.plabedl.barea_pres_signal = SketchLineStyle.ASE_HCOINCIDE;

		// bit of a useless way of looking up which value indexes it.
		for (opddconn.plabedl.iarea_pres_signal = 0; opddconn.plabedl.iarea_pres_signal < SketchLineStyle.nareasignames; opddconn.plabedl.iarea_pres_signal++)
			if (SketchLineStyle.areasigeffect[opddconn.plabedl.iarea_pres_signal] == opddconn.plabedl.barea_pres_signal)
				break;
System.out.println("AreaPresSig " + opddconn.plabedl.iarea_pres_signal + "  " + opddconn.plabedl.barea_pres_signal); 

		assert opddconn.pnend.pathcount == 0;
		assert opddconn.IsDropdownConnective();
		return opddconn;
	}

	/////////////////////////////////////////////
	// if we have equality, a dropdown connective path may have been fused.
	// it will be happy if we can get the pitch boundary and the dropped invisible boundary the right way round
	void InsertOnNode(OnePath op, boolean bFore)
	{
		assert (bFore ? op.pnend : op.pnstart) == this;

		// single path connecting to empty node here
		if (pathcount == 0)
		{
			assert opconn == null;
			opconn = op;
			if (!bFore)
			{
				assert op.aptailleft == null;
				op.aptailleft = op;
				op.baptlfore = false;
			}
			else
			{
				assert op.apforeright == null;
				op.apforeright = op;
				op.bapfrfore = true;
			}
			pathcount = 1;
			return;
		}

		float tang = op.GetTangent(!bFore);

		// find a place to insert
		boolean lbFore = (opconn.pnend == this);
		if (opconn.pnend == opconn.pnstart) // avoid the null pointer
		{
			if ((!lbFore ? opconn.aptailleft : opconn.apforeright)  == null)
				lbFore = !lbFore;
		}
		boolean pbFore = lbFore;
		OnePath pop = opconn;
		boolean nbFore;
		OnePath nop;
		float ptang = pop.GetTangent(!pbFore);
		boolean bsomech = false; // protect against all edges coinciding
		while (true)
		{
			// find the next point along
			if (!pbFore)
        	{
				nbFore = pop.baptlfore;
				nop = pop.aptailleft;
			}
			else
			{
				nbFore = pop.bapfrfore;
				nop = pop.apforeright;
        	}
			assert ((!nbFore ? nop.pnstart : nop.pnend) == this);
			float ntang = nop.GetTangent(!nbFore);

			// we're allowed one crossing of the x-axis for wrap-around
			if (ptang < ntang)
			{
				bsomech = true;
				if ((ptang <= tang) && (tang <= ntang))
					break;
			}
			else if (ptang > ntang)
			{
				bsomech = true;
				if ((ptang <= tang) || (tang <= ntang))
					break;
			}
			// this detects final pair if it is equal (and all are)
			else
			{
				if (!bsomech && ((nop == opconn) && (nbFore == lbFore)))
					break;
			}
			pbFore = nbFore;
			pop = nop;
			ptang = ntang;

			assert (!((pop == opconn) && (pbFore == lbFore)));
		}


		// link the path we're inserting in
		if (!bFore)
		{
			assert op.aptailleft == null;
			op.aptailleft = nop;
			op.baptlfore = nbFore;
		}
		else
		{
			assert op.apforeright == null;
			op.apforeright = nop;
			op.bapfrfore = nbFore;
		}

		// link the right hand path into this path
		if (!pbFore)
		{
			assert pop.aptailleft == nop;
			assert pop.baptlfore == nbFore;
			pop.aptailleft = op;
			pop.baptlfore = bFore;
		}
		else
		{
			assert pop.apforeright == nop;
			assert pop.bapfrfore == nbFore;
			pop.apforeright = op;
			pop.bapfrfore = bFore;
		}

		pathcount++;
		opconn = op;
	}


	/////////////////////////////////////////////
	boolean RemoveOnNode(OnePath op, boolean bFore)
	{
		assert (bFore ? op.pnend : op.pnstart) == this;

		// single path connecting to single node here
		if (pathcount == 1)
		{
			assert opconn == op;
			opconn = null;
			if (!bFore)
			{
				assert op.aptailleft == op;
				op.aptailleft = null;
			}
			else
			{
				assert op.apforeright == op;
				op.apforeright = null;
			}
			pathcount = 0;
			return true;
		}

		// need to loop arond from opconn to find the one above the path
		// find a place to insert
		boolean lbFore = (opconn.pnend == this);
		if (opconn.pnend == opconn.pnstart) // avoid the null pointer
		{
			if ((!lbFore ? opconn.aptailleft : opconn.apforeright)  == null)
				lbFore = !lbFore;
		}
		boolean pbFore = lbFore;
		OnePath pop = opconn;
		while (true)
		{
			// find the next point along
			boolean nbFore;
			OnePath nop;
			if (!pbFore)
        	{
				nbFore = pop.baptlfore;
				nop = pop.aptailleft;
			}
			else
			{
				nbFore = pop.bapfrfore;
				nop = pop.apforeright;
        	}
			assert ((!nbFore ? nop.pnstart : nop.pnend) == this);
			if ((nop == op) && (nbFore == bFore))
				break;
			pbFore = nbFore;
			pop = nop;

			assert (!((pop == opconn) && (pbFore == lbFore)));
		}

		// next link on from this
		boolean nbFore;
		OnePath nop;
		if (!bFore)
        {
			nbFore = op.baptlfore;
			nop = op.aptailleft;
		}
		else
		{
			nbFore = op.bapfrfore;
			nop = op.apforeright;
       	}

		// delink the path we're inserting in
		if (!bFore)
		{
			assert op.aptailleft == nop;
			op.aptailleft = null;
		}
		else
		{
			assert op.apforeright == nop;
			op.apforeright = null;
		}

		// relink the right hand path into the left path
		if (!pbFore)
		{
			assert pop.aptailleft == op;
			assert pop.baptlfore == bFore;
			pop.aptailleft = nop;
			pop.baptlfore = nbFore;
		}
		else
		{
			assert pop.apforeright == op;
			assert pop.bapfrfore == bFore;
			pop.apforeright = nop;
			pop.bapfrfore = nbFore;
		}

		// decrement and quit
		pathcount--;
		opconn = pop;

		assert (op.pnstart == op.pnend) || (opconn != op);  
		return false;
	}


	/////////////////////////////////////////////
	static boolean CheckAllPathCounts(Vector vnodes, Vector vpaths)
	{
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)vnodes.elementAt(i)).pathcountch = 0;
		for (int j = 0; j < vpaths.size(); j++)
		{
			OnePath op = (OnePath)vpaths.elementAt(j);
			op.pnstart.pathcountch++;
			op.pnend.pathcountch++;
		}

		int tccn = 0;
		for (int i = 0; i < vnodes.size(); i++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i);
			assert opn.pathcountch == opn.pathcount;
			tccn += opn.pathcount;
            assert opn.CheckPathCount();
		}
		assert tccn == 2 * vpaths.size(); // proves all are in the list.
		return true;
	}

}

