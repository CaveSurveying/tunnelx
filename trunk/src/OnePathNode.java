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


//
//
// OnePathNode
//
//



/////////////////////////////////////////////
class OnePathNode
{
	Point2D.Float pn = null;
	boolean bzaltset = false;
	float zalt = 0.0F; // the altitude of this node (inherited into areas so we can draw them in order).

	private Rectangle2D.Float pnell = null; // for drawing.
	float currstrokew = 0.0F;
	int pathcount; // number of paths which link to this node.
	    int pathcountch; // spare variable for checking the pathcount

	String pnstationlabel = null; // lifted from the centreline legs.
	OnePath opconn = null; // connection to a single path which we can circle around, and will match the pathcount

	int isubsetcode = 0;

	Vector vproxpathlist = null; // used to get record paths connecting for proximity calcs
	float proxdist = -1.0F;

    // value set by other weighting operations for previewing
    int icolindex = -1;


	/////////////////////////////////////////////
	Rectangle2D.Float Getpnell()
	{
		if (currstrokew != TN.strokew)
		{
			currstrokew = TN.strokew;
			pnell = new Rectangle2D.Float((float)pn.getX() - 2 * currstrokew, (float)pn.getY() - 2 * currstrokew, 4 * currstrokew, 4 * currstrokew);
		}
		return pnell;
	}


	/////////////////////////////////////////////
	OnePathNode(float x, float y, float z, boolean lbzaltset)
	{
		pn = new Point2D.Float(x, y);
		zalt = z;
		bzaltset = lbzaltset;
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

