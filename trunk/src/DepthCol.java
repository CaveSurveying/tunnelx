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

import java.awt.Color; 


// separates the different colours for depth drawing.  
class DepthCol 
{
	boolean bByAbssolute; 
	
	// Range in depth.  
	float zlo; 
	float zhi; 

	// number of slices 
	int znslices; 
	Color[] col; 

	boolean bdatelimit = false; 
	int datelimit = 0; 

	/////////////////////////////////////////////
	DepthCol()
	{
		znslices = 10; 
		col = new Color[znslices]; 
		for (int i = 0; i < znslices; i++) 
		{
			float scz = (float)i / znslices;
			col[i] = new Color(Color.HSBtoRGB(scz, 1.0F, 1.0F)); 
		}
	}

	/////////////////////////////////////////////
	// find the range of stations 
	void AbsorbRange(OneStation os, boolean bFirst) 
	{
		float zval = os.Loc.z; 
		if (bFirst) 
		{
			zlo = zval; 
			zhi = zlo; 
		}
		else if (zlo > zval) 
			zlo = zval; 
		else if (zhi < zval) 
			zhi = zval; 
	}
}


