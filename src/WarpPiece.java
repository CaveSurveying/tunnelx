////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2009  Julian Todd.
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

import java.util.List;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;

// class used for warping a path when FuseNodes is used, either in plan or elevation case
/////////////////////////////////////////////
class WarpPiece
{
	static final int WARP_NORMALWARP = 0; 
	static final int WARP_SHEARWARP = 1; 
	static final int WARP_ZWARP = 2; 

	int iwarp; 

	// the line from and line to
	OnePathNode pnstart; 
	OnePathNode pnend; 
	OnePathNode npnstart; 
	OnePathNode npnend; 
	
	// initial vector
	double xv;
	double yv;
	double vsq;

	// final vector
	double nxv;
	double nyv;
	double nvsq;

	// translation vector for shear warp
	double xt = 0.0;
	double yt = 0.0;


	/////////////////////////////////////////////
	void SetUpVectors()
	{
		// initial vector
		xv = pnend.pn.getX() - pnstart.pn.getX();
		yv = pnend.pn.getY() - pnstart.pn.getY();
		vsq = xv * xv + yv * yv;

		// final vector
		nxv = npnend.pn.getX() - npnstart.pn.getX();
		nyv = npnend.pn.getY() - npnstart.pn.getY();
		nvsq = nxv * nxv + nyv * nyv;
	}

	/////////////////////////////////////////////
	// construct from one end path drag (bug could do total move)
	WarpPiece(OnePathNode pnfrom, OnePathNode pnto, OnePath axop, int liwarp)
	{
		iwarp = liwarp; 

		pnstart = axop.pnstart; 
		pnend = axop.pnend; 
		npnstart = (axop.pnstart == pnfrom ? pnto : axop.pnstart);
		npnend = (axop.pnend == pnfrom ? pnto : axop.pnend);
		xt = pnto.pn.getX() - pnfrom.pn.getX();
		yt = pnto.pn.getY() - pnfrom.pn.getY();
		SetUpVectors(); 
	}

	/////////////////////////////////////////////
	WarpPiece(OnePathNode lpnstart, OnePathNode lpnend, OnePathNode lnpnstart, OnePathNode lnpnend)
	{
		iwarp = WARP_ZWARP; 

		pnstart = lpnstart; 
		pnend = lpnend; 
		npnstart = lnpnstart;
		npnend = lnpnend;
		SetUpVectors(); 
	}

	/////////////////////////////////////////////
	void WarpPoint(Point2D res, double x, double y)
	{
		// translation case (if endpoints match).
		if ((vsq == 0.0) || (nvsq == 0.0))
		{
			if ((vsq != 0.0) || (nvsq != 0.0))
				TN.emitWarning("Bad warp: only one axis vector is zero length");
			res.setLocation(x + xt, y + yt);
		}

		// by shearing
		else if (iwarp == WARP_SHEARWARP)
		{
			double vix = x - pnstart.pn.getX();
			double viy = y - pnstart.pn.getY();
			double lam = (vix * xv + viy * yv) / vsq;
			res.setLocation(x + lam * xt, y + lam * yt);
		}

		else if (iwarp == WARP_ZWARP) 
		{
			if ((xv == 0.0) || (nxv == 0.0))
				TN.emitWarning("zwarp on zero x axis"); 
			double vix = x - pnstart.pn.getX();
			double viy = y - pnstart.pn.getY();
			double lam = (xv != 0.0 ? vix / xv : (vix * xv + viy * yv) / vsq);  // goes to other warp when zero
			//float ay = (float)pnstart.pn.getY() + lam * yv;
			double dy = viy - lam * yv; 
			res.setLocation(npnstart.pn.getX() + lam * nxv, npnstart.pn.getY() + lam * nyv + dy);
		}

		// rotation case (one endpoint matches)
		else 
		{
			assert (iwarp == WARP_NORMALWARP); 
			double vix = x - pnstart.pn.getX();
			double viy = y - pnstart.pn.getY();

			double lam = (vix * xv + viy * yv) / vsq;
			double plam = (vix * (-yv) + viy * (xv)) / vsq;

			res.setLocation(npnstart.pn.getX() + lam * nxv + plam * (-nyv), npnstart.pn.getY() + lam * nyv + plam * (nxv));
		}
	}

	
	/////////////////////////////////////////////
	OnePath WarpPathS(OnePath op)
	{
		assert pnstart == op.pnstart; 
		assert pnend == op.pnend; 
		float[] pco = op.GetCoords();
		OnePath res = new OnePath(npnstart);

		Point2D pt = new Point2D.Float();

		for (int i = 1; i < op.nlines; i++)
		{
			WarpPoint(pt, pco[i * 2], pco[i * 2 + 1]); 
			res.LineTo((float)pt.getX(), (float)pt.getY());
		}
		res.EndPath(npnend);
		res.CopyPathAttributes(op);
		return res;
	}


	/////////////////////////////////////////////
	OnePathNode WarpElevationNode(OnePathNode opn, List<OnePath> nopelevarr, List<OnePath> opelevarr, Point2D ptspare)
	{
System.out.println("node " + opn.pn.getX() + "," + opn.pn.getY()); 
		if (opn == pnstart)
			return npnstart; 
		if (opn == pnend)
			return npnend; 
		for (int i = 0; i < nopelevarr.size(); i++)
		{
			if (opn == opelevarr.get(i).pnstart)
				return nopelevarr.get(i).pnstart; 
			if (opn == opelevarr.get(i).pnend)
				return nopelevarr.get(i).pnend; 
		}
		WarpPoint(ptspare, opn.pn.getX(), opn.pn.getY()); 
System.out.println("   tonode " + ptspare.getX() + "," + ptspare.getY()); 
		return new OnePathNode((float)ptspare.getX(), (float)ptspare.getY(), 0.0F); 
	}

	/////////////////////////////////////////////
	void WarpElevationBatch(List<OnePath> res, List<OnePath> opelevarr)
	{
		Point2D ptspare = new Point2D.Float();
		assert res.isEmpty(); 
		for (int j = 0; j < opelevarr.size(); j++)
		{
			OnePath op = opelevarr.get(j); 
			float[] pco = op.GetCoords();
System.out.println("web " + j + "  " + op.nlines + "  x " + pco[0]); 
			OnePath nop = new OnePath(WarpElevationNode(op.pnstart, res, opelevarr, ptspare)); 
			for (int i = 1; i < op.nlines; i++)
			{
				WarpPoint(ptspare, pco[i * 2], pco[i * 2 + 1]); 
				nop.LineTo((float)ptspare.getX(), (float)ptspare.getY());
			}
			nop.EndPath(WarpElevationNode(op.pnend, res, opelevarr, ptspare));
			nop.CopyPathAttributes(op);
			res.add(nop); 
		}
	}
};
