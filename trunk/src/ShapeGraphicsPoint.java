////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
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
