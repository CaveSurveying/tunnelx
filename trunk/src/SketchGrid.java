////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2005  Julian Todd.
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
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.Graphics2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Dimension;




/////////////////////////////////////////////
class SketchGrid
{
	float xorig = 0.0F;
	float yorig = 0.0F;
	float[] gridspacing = new float[20];
	int[] gridlineslimit = new int[20];
	int ngridspacing = 0; // the sizes of the above dynamic arrays

	int igridspacing = 0; // the chosen index of the grid spacing
    float gridspace = 1.0F;

	// a temporary copy of the grid origin which can be set and reset
	float txorig = 0.0F;
	float tyorig = 0.0F;

	// the region the lines are drawn in
	float xlo, xhi, ylo, yhi;
	int ixlo, ixhi, iylo, iyhi;  // the index positions of the lines

	GeneralPath gpgrid = new GeneralPath();


	/////////////////////////////////////////////
	SketchGrid(float lxorig, float lyorig)
	{
		xorig = lxorig;
		txorig = lxorig;
		yorig = lyorig;
		tyorig = lyorig;
	}

	/////////////////////////////////////////////
	public void SetGridSpacing(int gsoffset)
	{
		float mwid = Math.max(xhi - xlo, yhi - ylo);
		int ligridspacing;
		for (ligridspacing = 0; ligridspacing < ngridspacing; ligridspacing++)
		{
			float lgridspace = gridspacing[ligridspacing] * TN.CENTRELINE_MAGNIFICATION;
			if (mwid / lgridspace < gridlineslimit[ligridspacing])
				break;
		}

		// set the values incl the offset
		igridspacing = Math.max(0, Math.min(ngridspacing - 1, ligridspacing + gsoffset));
		gridspace = gridspacing[igridspacing] * TN.CENTRELINE_MAGNIFICATION;
		assert gridspace != 0.0;

		// set the index positions
		// xlo < ixlo * gridspace + xorig
		ixlo = (int)Math.floor((xlo - txorig) / gridspace) + 1;
		ixhi = (int)Math.floor((xhi - txorig) / gridspace);
		iylo = (int)Math.floor((ylo - tyorig) / gridspace) + 1;
		iyhi = (int)Math.floor((yhi - tyorig) / gridspace);

		if (igridspacing == gridlineslimit.length - 1)
		{
			int gridlinelimit = gridlineslimit[igridspacing];
			int xdi = ((ixhi - ixlo) - gridlinelimit) / 2;
			if (xdi > 0)
			{
				ixlo += xdi;
				ixhi -= xdi;
			}
			int ydi = ((iyhi - iylo) - gridlinelimit) / 2;
			if (ydi > 0)
			{
				iylo += ydi;
				iyhi -= ydi;
			}
		}
	}


	/////////////////////////////////////////////
	public void GenerateMetreGrid()
	{
		gpgrid.reset();

		for (int i = ixlo; i <= ixhi; i++)
		{
			gpgrid.moveTo(i * gridspace + txorig, ylo);
			gpgrid.lineTo(i * gridspace + txorig, yhi);
		}
		for (int i = iylo; i <= iyhi; i++)
		{
			gpgrid.moveTo(xlo, i * gridspace + tyorig);
			gpgrid.lineTo(xhi, i * gridspace + tyorig);
		}
	}


	/////////////////////////////////////////////
	boolean ClosestGridPoint(Point2D res, double ptx, double pty, double scale)
	{
		int ix = (int)Math.floor((ptx - txorig) / gridspace + 0.5F);
		int iy = (int)Math.floor((pty - tyorig) / gridspace + 0.5F);
		if ((ix < ixlo) || (ix > ixhi) || (iy < iylo) || (iy > iyhi))
			return false;

		res.setLocation(ix * gridspace + txorig, iy * gridspace + tyorig);
		double md = Math.max(Math.abs(res.getX() - ptx), Math.abs(res.getY() - pty));
		return ((scale == -1.0) || (md < scale));
	}



	/////////////////////////////////////////////
	Point2D.Float gridscrcorner = new Point2D.Float();
	Point2D.Float scrcorner = new Point2D.Float();
	void SetUntransRanges(float tx, float ty, AffineTransform currtrans, boolean bfirst) throws NoninvertibleTransformException
	{
		scrcorner.setLocation(tx, ty);
		currtrans.inverseTransform(scrcorner, gridscrcorner);

		float x = (float)gridscrcorner.getX();
		float y = (float)gridscrcorner.getY();

		if (bfirst || (x < xlo))
			xlo = x;
		if (bfirst || (x > xhi))
			xhi = x;
		if (bfirst || (y < ylo))
			ylo = y;
		if (bfirst || (y > yhi))
			yhi = y;
	}

	/////////////////////////////////////////////
	void UpdateGridCoords(Dimension csize, AffineTransform currtrans, boolean bhasrotation, SketchBackgroundPanel backgroundpanel)
	{
		try
		{
			SetUntransRanges(0.0F, 0.0F, currtrans, true);
			SetUntransRanges(csize.width, csize.height, currtrans, false);
			if (bhasrotation)
			{
				SetUntransRanges(csize.width, 0.0F, currtrans, false);
				SetUntransRanges(0.0F, csize.height, currtrans, false);
			}
		}
		catch (NoninvertibleTransformException ex)
		{;}

		int pigridspacing = igridspacing;
		SetGridSpacing(backgroundpanel.gsoffset);
		if (pigridspacing != igridspacing)
			backgroundpanel.tfgridspacing.setText(String.valueOf(gridspacing[igridspacing]));
		GenerateMetreGrid();
	}
};




