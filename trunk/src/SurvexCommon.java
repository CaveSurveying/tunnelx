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

import java.io.File; 

//
//
// SurvexCommon
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexCommon 
{
	/////////////////////////////////////////////
	static File calcIncludeFile(File orgfile, String fname, boolean bPosType) 
	{
		if ((orgfile == null) || ((fname.length() > 3) && (fname.charAt(1) == ':')))
			return new File(fname); 

		String cdirectory = orgfile.getParent(); 
		String fnamesuff = TN.getSuffix(fname); 
		
		char lsep; 

		// deal with the up directory situation.  
		if (fname.startsWith("..\\") || fname.startsWith("../")) 
		{
			// TN.emitMessage("**" + cdirectory + " + " + fname); 
			lsep = '\\'; 
			while (fname.startsWith("..\\") || fname.startsWith("../")) 
			{
				fname = fname.substring(3); 
				int ndr = cdirectory.lastIndexOf(lsep); 
				if (ndr != -1) 
					cdirectory = cdirectory.substring(0, ndr); 
				else 
					TN.emitWarning("Failed to go up directory"); 
			}
			// TN.emitMessage(" = " + cdirectory + " + " + fname); 
		}

		else 
		{
			// find local file separator 
			if (fnamesuff.equalsIgnoreCase(TN.SUFF_SVX) || fnamesuff.equalsIgnoreCase(TN.SUFF_SRV))  
				lsep = '\\'; 
			else if (fname.indexOf('.') != -1) 
				lsep = '.'; 
			else if (fname.indexOf('/') != -1) 
				lsep = '/'; 
			else if (fname.indexOf('\\') != -1) 
				lsep = '\\'; 
			else
				lsep = '\\'; 
			//TN.emitMessage("fname of " + fname + "  suff " + fnamesuff + "  sep " + lsep); 
		}

		// replace delimeter characters with the separator character.  
		int idot; 
		while ((idot = fname.indexOf(lsep)) != -1) 
		{
			cdirectory = cdirectory + File.separator + fname.substring(0, idot); 
			fname = fname.substring(idot + 1); 
		}

		if (bPosType) 
			fname = fname + TN.SUFF_POS; 
		else if (lsep == '\\') 
			fname = TN.setSuffix(fname, TN.getSuffix(!fnamesuff.equalsIgnoreCase(TN.SUFF_SRV) ? orgfile.getName() : fname)); 
		else
			fname = fname + TN.getSuffix(orgfile.getName()); 

		//TN.emitMessage("include file " + cdirectory + " \\ " + fname); 
		return(new File(cdirectory, fname)); 
	}
}; 

