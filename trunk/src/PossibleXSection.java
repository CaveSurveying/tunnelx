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

		// limits on what is sensible data.  (possibly put in the constructor argument.)  
		if (!((L >= -1.0F) && (L <= 10.0F) && (R >= -1.0F) && (R <= 10.0F) && (U >= -1.F) && (U <= 10.0F) && (D >= -1.0F) && (D <= 10.0F)))  
			basestationS = null;

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
			TN.emitMessage(orientclinoS); 
			orientclinoS = "0.0"; 
		}; 
	}
}


