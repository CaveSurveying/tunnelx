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

//
//
// ShapeGraphicsPoint
//
//
/////////////////////////////////////////////
class ShapeGraphicsPoint extends Vec3
{
	int ix, iy; 

	// these links are just for the active types to help recreate the contour when we are reading it
	ShapeGraphicsLine sgl1 = null; 
	ShapeGraphicsLine sgl2 = null; 

	

	Color SugColour(ShapeGraphicsPoint sgpactive)
	{
		return (this == sgpactive ? TN.xsgSelected : TN.xsgLines); 
	}
}
