////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
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
	static File calcIncludeFile(File orgfile, String fname, boolean bImage) 
	{
		if ((orgfile == null) || ((fname.length() > 3) && (fname.charAt(1) == ':')))
			return new File(fname); 

		String cdirectory = orgfile.getParent(); 
		String fnamesuff = TN.getSuffix(fname); 
		
		char lsep; 

		// deal with the up directory situation.  
		if (fname.startsWith("..\\") || fname.startsWith("../")) 
		{
			// System.out.println("**" + cdirectory + " + " + fname); 
			lsep = '\\'; 
			while (fname.startsWith("..\\") || fname.startsWith("../")) 
			{
				fname = fname.substring(3); 
				int ndr = cdirectory.lastIndexOf(lsep); 
				if (ndr != -1) 
					cdirectory = cdirectory.substring(0, ndr); 
				else 
					System.out.println("Failed to go up directory"); 
			}
			// System.out.println(" = " + cdirectory + " + " + fname); 
		}
		else 
		{
			// find local file separator 
			// ".srv" is the walls subfile extension.  
			if (fnamesuff.equalsIgnoreCase(TN.SUFF_SVX) || fnamesuff.equalsIgnoreCase(".srv") || bImage)  
				lsep = '\\'; 
			else if (fname.indexOf('.') != -1) 
				lsep = '.'; 
			else if (fname.indexOf('/') != -1) 
				lsep = '/'; 
			else if (fname.indexOf('\\') != -1) 
				lsep = '\\'; 
			else
				 lsep = '\\'; 
//System.out.println("fname of " + fname + "  suff " + fnamesuff + "  sep " + lsep); 
		}

		int idot; 
		while ((idot = fname.indexOf(lsep)) != -1) 
		{
			cdirectory = cdirectory + File.separator + fname.substring(0, idot); 
			fname = fname.substring(idot + 1); 
		}
		if (lsep == '\\') 
			fname = TN.setSuffix(fname, TN.getSuffix(!bImage && !fnamesuff.equalsIgnoreCase(".srv") ? orgfile.getName() : fname)); 
		else
			fname = fname + TN.getSuffix(orgfile.getName()); 

//System.out.println("include file " + cdirectory + " \\ " + fname); 
		return(new File(cdirectory, fname)); 
	}
}; 

