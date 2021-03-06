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
import java.io.BufferedWriter; 
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
//

//
// LineOutputStream
//
//

/////////////////////////////////////////////
class LineOutputStream 
{
	FileAbstraction savefile;
	DataOutputStream dos = null;   // why was this one ever used?  does not handle string encoding
    BufferedWriter bos = null; 
	StringBuffer sb = null;

    
	/////////////////////////////////////////////
	public LineOutputStream()
	{
		sb = new StringBuffer();
	}

	/////////////////////////////////////////////
	public LineOutputStream(FileAbstraction lsavefile) throws IOException
	{
		savefile = lsavefile;
		if (savefile != null)
		{
			dos = new DataOutputStream(new FileOutputStream(lsavefile.getPath()));
			TN.emitMessage("Saving file " + savefile.getPath());
		}
		else
		{
			sb = new StringBuffer();
			//TN.emitWarning("File to save to not specified ");
		}
	}

	/////////////////////////////////////////////
    // charsetName="UTF-8"
	public LineOutputStream(FileAbstraction lsavefile, String charsetName) throws IOException
	{
		savefile = lsavefile;
		if (savefile != null)
		{
			bos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lsavefile.getPath()), charsetName));
			TN.emitMessage("Saving file " + savefile.getPath()+" with charSet: "+charsetName);
		}
		else
			sb = new StringBuffer();
	}

	/////////////////////////////////////////////
	public LineOutputStream(DataOutputStream ldos) throws IOException
	{
        dos = ldos;
        TN.emitMessage("Saving on Data output stream");
	}

	/////////////////////////////////////////////
	public void WriteLine(String sline) throws IOException
	{
		if (dos != null)
		{
			dos.writeBytes(sline); 
			dos.writeBytes(TN.nl); 
		}
        else if (bos != null)
            bos.append(sline).append(TN.nl); 
		else
		{
			sb.append(sline); 
			sb.append(TN.nl); 
		}
	}

	/////////////////////////////////////////////
	public void Write(String sline) throws IOException
	{
		if (dos != null)
			dos.writeBytes(sline);
        else if (bos != null)
            bos.append(sline); 
		else
			sb.append(sline);
	}

	
	/////////////////////////////////////////////
	public void Write(float x, float y) throws IOException
	{
		Write(" ");
		Write(String.valueOf(x));
		Write(" ");
		Write(String.valueOf(y));
	}

	/////////////////////////////////////////////
	public void close() throws IOException
	{
		if (dos != null)
			dos.close();
        else if (bos != null)
            bos.close(); 
	}
}

