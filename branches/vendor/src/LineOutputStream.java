////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;

//
//
// LineOutputStream
//
//

/////////////////////////////////////////////
class LineOutputStream extends DataOutputStream
{
	File savefile; 

	/////////////////////////////////////////////
	public LineOutputStream(File lsavefile) throws IOException
	{
		super(new FileOutputStream(lsavefile.getPath())); 
		System.out.println("Saving file " + lsavefile.getPath()); 
		savefile = lsavefile; 
	}

	/////////////////////////////////////////////
	public void WriteLine(String sline) throws IOException
	{
		writeBytes(sline); 
		writeBytes(TN.nl); 
	}
}

