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

import java.awt.Graphics; 
import java.awt.Dimension; 


/////////////////////////////////////////////
class WireAxes 
{
    Matrix3D axesmat = new Matrix3D();
	int ax0x = 0, ax0y = 0; 
	int axXx = 0, axXy = 0; 
	int axYx = 0, axYy = 0; 
	int axZx = 0, axZy = 0; 
	boolean bXbY = true, bXbZ = true, bYbZ = true; 

	int axScaLy = 0, axScaLyD = 0, axScaLxlo = 0, axScaLxN = 0; 
	float xfac; // records for the paintw case

	float[] scaleRG = { 0.5F, 1.0F, 5.0F, 10.0F, 50.0F, 100.0F, 500.0F, 1000.0F }; 

	/////////////////////////////////////////////
	void ReformAxes(Matrix3D rotmat, Dimension csize, float xoc, float yoc, float lxfac)  
	{
		xfac = lxfac; 
		axesmat.SetFrom(rotmat); 
		float afac = Math.min(csize.width, csize.height) / 10.0F; 
		axesmat.scale(afac, -afac, afac * 1.3F); 
		float fax0x = xoc / 4.0F; 
		float fax0y = yoc * (7.0F / 4.0F); 

		ax0x = (int)(fax0x + 0.5F); 
		ax0y = (int)(fax0y + 0.5F); 
		axXx = (int)(fax0x + axesmat.xx + 0.5F); 
		axXy = (int)(fax0y + axesmat.yx + 0.5F); 
		axYx = (int)(fax0x + axesmat.xy + 0.5F); 
		axYy = (int)(fax0y + axesmat.yy + 0.5F); 
		axZx = (int)(fax0x + axesmat.xz + 0.5F); 
		axZy = (int)(fax0y + axesmat.yz + 0.5F); 
		bXbY = (axesmat.zx < axesmat.zy); 
		bXbZ = (axesmat.zx < axesmat.zz); 
		bYbZ = (axesmat.zy < axesmat.zz); 

		axScaLy = (int)(yoc * (7.5F / 4.0F) + 0.5F); 
		axScaLyD = (int)(yoc * (0.1F / 4.0F) + 0.5F); 
		axScaLxN = Math.max((int)((2 * afac + 0.5F) / xfac), 1); 
		axScaLxlo = (int)(fax0x - afac + 0.5F); 
	}


	/////////////////////////////////////////////
	void paintW(Graphics g) 
	{
		if (axesmat.zz < 0.0F) 
		{ 
			g.setColor(TN.wfdaxesZ); 
			g.drawLine(ax0x, ax0y, axZx, axZy); 
		}
		g.setColor(TN.wfdaxesXY); 
		g.drawLine(ax0x, ax0y, axXx, axXy); 
		g.drawLine(ax0x, ax0y, axYx, axYy); 
		if (axesmat.zz >= 0.0F) 
		{ 
			g.setColor(TN.wfdaxesZ); 
			g.drawLine(ax0x, ax0y, axZx, axZy); 
		}

		// draw the scale bars 

		// step up rate (by fixed if too many bits).  
		boolean bTooWide = (axScaLxN > 120); 
		int sbstep = (axScaLxN < 20 ? 1 : (!bTooWide ? 5 : axScaLxN)); 
		int axScaLxNL = (int)(axScaLxN / sbstep) * sbstep; 

		g.setColor(!bTooWide ? TN.wfdaxesZ : TN.wfdaxesXY); 
		g.drawLine(axScaLxlo, axScaLy, axScaLxlo + (int)(axScaLxNL * xfac + 0.5F), axScaLy); 

		// draw the double size ones 
		if ((sbstep <= 5) && (axScaLxNL >= 5)) 
		{
			g.setColor(TN.wfdaxesXY); 
			for (int sb = 0; sb <= axScaLxNL; sb += 5) 
			{
				int xi = axScaLxlo + (int)(sb * xfac + 0.5F); 
				g.drawLine(xi, axScaLy - axScaLyD * 2, xi, axScaLy + axScaLyD * 2); 
			}
		}

		g.setColor(!bTooWide ? TN.wfdaxesZ : TN.wfdaxesXY); 
		for (int sb = 0; sb <= axScaLxNL; sb += sbstep) 
		{
			int xi = axScaLxlo + (int)(sb * xfac + 0.5F); 
			g.drawLine(xi, axScaLy - axScaLyD, xi, axScaLy + axScaLyD); 
		}
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////


}; 


