////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2007  Julian Todd.
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

import java.awt.geom.Point2D; 

/////////////////////////////////////////////
class SSymbScratchPath
{
	double[] cumpathleng = new double[256]; // (nodelength, reallength) records the distance to each node along the path, as pairs
	int lencumpathleng = -1;
	Point2D pathevalpoint = new Point2D.Double();
	Point2D pathevaltang = new Point2D.Double();

	/////////////////////////////////////////////
	void SetUpPathLength(OnePath lpath)
	{
		lpath.GetCoords();
		lencumpathleng = 0;
		int nsegs = (lpath.bSplined ? 5 : 1);
		double clen = 0.0;
		double prevx = 0.0;
		double prevy = 0.0;
		for (int i = 0; i < lpath.nlines; i++)
		{
			for (int j = (i == 0 ? 0 : 1); j <= nsegs; j++)
			{
				double tr = (double)j / nsegs;
				lpath.EvalSeg(pathevalpoint, null, i, tr);
				if ((i != 0) || (j != 0))
				{
					double vx = pathevalpoint.getX() - prevx;
					double vy = pathevalpoint.getY() - prevy;
					clen += Math.sqrt(vx * vx + vy * vy);
				}
				cumpathleng[lencumpathleng * 2] = i + tr;
				cumpathleng[lencumpathleng * 2 + 1] = clen;
				prevx = pathevalpoint.getX();
				prevy = pathevalpoint.getY(); 
				lencumpathleng++;
			}
		}
	}

	/////////////////////////////////////////////
	double ConvertAbstoNodePathLength(double r, OnePath lpath)
	{
		int i; 
		for (i = 1; i < lencumpathleng; i++)
		{
			if (r <= cumpathleng[i * 2 + 1])
			{
				double lam = (r - cumpathleng[i * 2 - 1]) / (cumpathleng[i * 2 + 1] - cumpathleng[i * 2 - 1]);
				return cumpathleng[i * 2 - 2] * (1.0 - lam) + cumpathleng[i * 2] * lam;
			}
		}
		return 0.0;
	}

	/////////////////////////////////////////////
	double GetCumuPathLength()
	{
		return cumpathleng[lencumpathleng * 2 - 1]; 
	}
};
