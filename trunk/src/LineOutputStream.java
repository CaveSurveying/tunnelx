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
		TN.emitWarning("Saving file " + lsavefile.getPath()); 
		savefile = lsavefile; 
	}

	/////////////////////////////////////////////
	public void WriteLine(String sline) throws IOException
	{
		writeBytes(sline); 
		writeBytes(TN.nl); 
	}
}

