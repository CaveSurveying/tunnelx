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

	String pnstationlabel = null; // lifted from the centreline legs.

	boolean bvisiblebyz = true;

	Vector vproxpathlist = null; // used to get record paths connecting for proximity calcs
	float proxdist = -1.0F;

    // value set by other weighting operations for previewing
    int icolindex = -1;

	/////////////////////////////////////////////
	// notes and sorts the paths coming into this node.  Then adds the links.
	void SetPathAreaLinks(Vector vpaths)
	{
		// class  associated to a path.
		class RefPath
		{
			OnePath op;
			boolean bForward;
			float tangent;

			RefPath(OnePath lop, boolean lbForward)
			{
				op = lop;
				bForward = lbForward;
				tangent = op.GetTangent(!bForward);
			}

			void InsertInto(Vector lrpa)
			{
				int i;
				for (i = 0; i < lrpa.size(); i++)
					if (((RefPath)lrpa.elementAt(i)).tangent > tangent)
						break;
				lrpa.insertElementAt(this, i);
			}
		}

		// make the vector of references attached to this point in order.
		Vector rpa = new Vector();
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
			if (op.AreaBoundingType())
			{
				if (op.pnstart.pn.equals(pn))  // this used to be by ref (op.pnstart == this), now by value to cope with zero length segs
					(new RefPath(op, false)).InsertInto(rpa);
				if (op.pnend.pn.equals(pn))
					(new RefPath(op, true)).InsertInto(rpa);
			}
		}

		// set the link paths circulating around this node.
		for (int i = 0; i < rpa.size(); i++)
		{
			RefPath rp = (RefPath)rpa.elementAt(i);
			RefPath rpnext = (RefPath)rpa.elementAt(i == rpa.size() - 1 ? 0 : i + 1);

			if (rp.bForward)
			{
				// facing into the point
				rp.op.apforeright = rpnext.op;
				rp.op.bapfrfore = rpnext.bForward;
			}
			else
			{
				// do the left back case.
				rp.op.aptailleft = rpnext.op;
				rp.op.baptlfore = rpnext.bForward;
			}
		}
	}

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
}

