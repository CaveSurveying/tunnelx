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
import java.awt.Color; 
import java.awt.Component; 

import java.util.Vector; 

import javax.swing.JPanel; 
import javax.swing.JButton; 
import javax.swing.Icon; 




/////////////////////////////////////////////
class SPSIcon implements Icon 
{
	OneSection pxsection; 
	float transx; 
	float transy; 
	float scale;  

	/////////////////////////////////////////////
	void SetSection(OneSection lpxsection) 
	{
		pxsection = lpxsection; 
		if (pxsection == null) 
			return; 

		float L = 0.0F; 
		float R = 0.0F; 
		float U = 0.0F; 
		float D = 0.0F; 

		for (int i = 0; i < pxsection.nnodes; i++)
		{
			Vec3 pt = pxsection.nodes[i]; 
			if (i == 0)
			{
				L = pt.x; 
				R = L; 
				U = pt.y; 
				D = U; 
			}
			else
			{
				L = Math.min(L, pt.x); 
				R = Math.max(R, pt.x); 
				U = Math.max(U, pt.y); 
				D = Math.min(D, pt.y); 
			}
		}

		float scalex = TN.XprevWidth / (R - L); 
		float scaley = TN.XprevHeight / (U - D); 
		scale = Math.min(scalex, scaley); 

		transx = -(R + L) / 2 + TN.XprevWidth / scale / 2; 
		transy = -(U + D) / 2 + TN.XprevHeight / scale / 2; 

	}


	public int getIconHeight() 
	{
		return TN.XprevHeight; 
	}

	public int getIconWidth() 
	{
		return TN.XprevWidth; 
	}


	public void paintIcon(Component c, Graphics g, int x, int y) 
	{
		//g.clearRect(0, 0, c.getSize().width, c.getSize().height); 
		if (pxsection != null) 
		{
			g.setColor(TN.wfmpointInactive); 
			for (int i = 0; i < pxsection.nnodes; i++)
			{
				Vec3 pt0 = pxsection.nodes[i == 0 ? pxsection.nnodes - 1 : i - 1]; 
				Vec3 pt1 = pxsection.nodes[i]; 
				g.drawLine((int)((pt0.x + transx) * scale) + x, y + TN.XprevHeight - (int)((pt0.y + transy) * scale), 
						   (int)((pt1.x + transx) * scale) + x, y + TN.XprevHeight - (int)((pt1.y + transy) * scale)); 
			}

			// do an x to scale.  
			g.setColor(TN.xsgGridline); 
			g.drawLine((int)((1.0F + transx) * scale) + x, y + TN.XprevHeight - (int)((transy) * scale), 
					   (int)((-1.0F + transx) * scale) + x, y + TN.XprevHeight - (int)((transy) * scale)); 
			g.drawLine((int)((transx) * scale) + x, y + TN.XprevHeight - (int)((1.0F + transy) * scale), 
					   (int)((transx) * scale) + x, y + TN.XprevHeight - (int)((-1.0F + transy) * scale)); 

		}
	}
}; 
