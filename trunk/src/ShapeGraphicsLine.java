////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.Color; 

//
//
// ShapeGraphicsLine
//
//
/////////////////////////////////////////////
class ShapeGraphicsLine 
{
	ShapeGraphicsPoint sgp1 = null; 
	ShapeGraphicsPoint sgp2 = null; 

	Color SugColour(ShapeGraphicsPoint sgpactive)
	{
		return (((sgp1 == sgpactive) || (sgp2 == sgpactive)) ? TN.xsgSelected : TN.xsgLines); 
	}
}

