////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.Graphics; 
import java.util.Vector; 
import java.io.IOException;


//
//
// PossibleSection
//
//

/////////////////////////////////////////////
// this is lifted from the *data LRUD stuff
class PossibleXSection 
{
	String basestationS; 
	float L, R, U, D; 
	String orientclinoS; 

	
	PossibleXSection(LegLineFormat llf, String lbasestationS, String sL, String sR, String sU, String sD, String lorientclinoS) 
	{
		basestationS = lbasestationS; 
		try 
		{
			L = (Float.valueOf(sL).floatValue() + llf.tapeoffset) * llf.tapefac; 
			R = (Float.valueOf(sR).floatValue() + llf.tapeoffset) * llf.tapefac; 
			U = (Float.valueOf(sU).floatValue() + llf.tapeoffset) * llf.tapefac; 
			D = (Float.valueOf(sD).floatValue() + llf.tapeoffset) * llf.tapefac; 
		}
		catch (NumberFormatException e)
		{
			basestationS = null;
		}; 

		// no zero or negative sized cross sections.  
		if ((L <= -R) || (U <= -D)) 
			basestationS = null;  

		// limits on what is sensible data.  (possobly put in the constructor argument.)  
//		if (!((L >= -1.0F) && (L <= 10.0F) && (R >= -1.0F) && (R <= 10.0F) && (U >= -1.F) && (U <= 10.0F) && (D >= -1.0F) && (D <= 10.0F)))  
//			basestationS = null;

		try 
		{
			orientclinoS = lorientclinoS; 
			if ((orientclinoS == null) || (orientclinoS.equals(""))) 
				orientclinoS = "-"; 
			float orientclino; 
			if (!orientclinoS.equalsIgnoreCase("up") && !orientclinoS.equalsIgnoreCase("down") && !orientclinoS.equalsIgnoreCase("-")) 
				orientclino = (Float.valueOf(orientclinoS).floatValue()); 
		}
		catch (NumberFormatException e)
		{
System.out.println(orientclinoS); 
			orientclinoS = "0.0"; 
		}; 
	}
}


