////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1  
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


