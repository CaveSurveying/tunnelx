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

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.StringReader;
import java.net.URLClassLoader; 
import java.net.URL;
import java.util.Vector;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;


//
//
// FileAbstraction
//
//

/////////////////////////////////////////////
// general function which will handle getting files from the local computer 
// and from the internet
// should
public class FileAbstraction
{
	File localfile;
	boolean bIsDirType; 

	// start easy by putting all the constructors
	FileAbstraction()
	{
		localfile = null; 
	}
	String getName() 
	{
		return localfile.getName(); 
	}
	String getPath() 
	{
		return localfile.getPath(); 
	}
	String getAbsolutePath() 
	{
		return localfile.getAbsolutePath(); 
	}
	String getCanonicalPath() throws IOException 
	{
		return localfile.getCanonicalPath(); 
	}
	FileAbstraction getParentFile()
	{
		return MakeDirectoryFileAbstraction(localfile.getParent()); 
	}

	Vector listFilesDir(boolean bFiles) throws IOException
	{
		assert localfile.isDirectory();
		List<File> sfileslist = Arrays.asList(localfile.listFiles());
		Collections.sort(sfileslist);
		File[] sfiles = sfileslist.toArray(new File[0]);
		Vector res = new Vector(); 
		for (int i = 0; i < sfiles.length; i++)
		{
			File tfile = sfiles[i].getCanonicalFile();
			if (bFiles ? tfile.isFile() : tfile.isDirectory())
				res.addElement(FileAbstraction.MakeOpenableFileAbstractionF(tfile));
		}
		return res; 
	}
	boolean mkdirs()
	{
		return localfile.mkdirs(); 
	}
	boolean isDirectory()
	{
		return localfile.isDirectory(); 
	}
	boolean isFile()
	{
		return localfile.isFile(); 
	}
	boolean exists()
	{
		return localfile.exists(); 
	}
	boolean canRead()
	{
		return localfile.canRead(); 
	}
	boolean equals(FileAbstraction fa)
	{
		return localfile.equals(fa.localfile); 
	}

	
	// this is killed
	public String toString()
	{
		assert false; 		
		return localfile.toString(); 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeOpenableFileAbstraction(String fname)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = new File(fname); 
		res.bIsDirType = false; 
		return res; 
	}
	/////////////////////////////////////////////
	static FileAbstraction MakeWritableFileAbstractionF(File file)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = file; 
		res.bIsDirType = false; 
		return res; 
	}
	/////////////////////////////////////////////
	static FileAbstraction MakeWritableFileAbstraction(String fname)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = new File(fname); 
		res.bIsDirType = false; 
		return res; 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeOpenableFileAbstractionF(File file)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = file; 
		res.bIsDirType = false; // unknown
		return res; 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeDirectoryFileAbstraction(String dname)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = new File(dname); 
		res.bIsDirType = true; 
		return res; 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeDirectoryFileAbstractionF(File dfile)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = dfile; 
		res.bIsDirType = true; 
		return res; 
	}

	static FileAbstraction MakeDirectoryAndFileAbstraction(FileAbstraction dfile, String fname)
	{
		FileAbstraction res = new FileAbstraction(); 
		res.localfile = new File(dfile.localfile, fname); 
		res.bIsDirType = false; 
		return res; 
	}



	/////////////////////////////////////////////
	static int TXML_UNKNOWN_FILE = 0;
	static int TXML_SKETCH_FILE = 3;
	static int TXML_EXPORTS_FILE = 2;
	static int TXML_MEASUREMENTS_FILE = 1;
	static int TXML_FONTCOLOURS_FILE = 4;

	/////////////////////////////////////////////
	// looks for the object type listed after the tunnelxml
	static char[] filehead = new char[256];
	int GetFileType()
	{
		String sfilehead = null;
		try
		{
			FileReader fr = new FileReader(localfile);
			int lfilehead = fr.read(filehead, 0, filehead.length);
			fr.close();
			if (lfilehead == -1)
				return TXML_UNKNOWN_FILE;
			sfilehead = new String(filehead, 0, lfilehead);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
		}
		String strtunnxml = "<tunnelxml>";
		int itunnxml = sfilehead.indexOf(strtunnxml);
		if (itunnxml == -1)
			return TXML_UNKNOWN_FILE;

		// this should be quitting when it gets to a space or a closing >
		int bracklo = sfilehead.indexOf('<', itunnxml + strtunnxml.length());
		int brackhic = sfilehead.indexOf('>', bracklo + 1);
		int brackhis = sfilehead.indexOf(' ', bracklo + 1);
		int brackhi = (brackhis != -1 ? Math.min(brackhic, brackhis) : brackhic);
		if ((bracklo == -1) || (brackhi == -1))
			return TXML_UNKNOWN_FILE;

		String sres = sfilehead.substring(bracklo + 1, brackhi);

		if (sres.equals("sketch"))
			return TXML_SKETCH_FILE;
		if (sres.equals("exports"))
			return TXML_EXPORTS_FILE;
		if (sres.equals("measurements"))
			return TXML_MEASUREMENTS_FILE;
		if (sres.equals("fontcolours"))
			return TXML_FONTCOLOURS_FILE;

		return TXML_UNKNOWN_FILE;
	}
}

