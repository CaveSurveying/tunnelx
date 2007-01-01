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
import java.io.InputStreamReader;
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
// we can gradually move all the file name generating and handling code into this class,
// and then arrange to share the work with what's going on on the server.
// Only return linein/outputstreams to these.
/////////////////////////////////////////////
// general function which will handle getting files from the local computer
// and from the internet
public class FileAbstraction
{
	// single type files (per OneTunnel)
	static int FA_FILE_UNKNOWN = 0;
	static int FA_FILE_XML_MEASUREMENTS = 1;
	static int FA_FILE_XML_EXPORTS = 2;
	static int FA_FILE_SVX = 3;
	static int FA_FILE_POS = 4;

	// multiple files possible
	static int FA_FILE_XML_SKETCH = 5;
	static int FA_FILE_XML_FONTCOLOURS = 6;

	static int FA_FILE_IMAGE = 7;
	static int FA_FILE_IGNORE = 8;

	static int FA_DIRECTORY = 10;

	static boolean bIsApplet = true; // default type, because starting in the static main of MainBox allows us to set to false

	// the actual
	File localfile;
	URL localurl;
	boolean bIsDirType;
	int xfiletype;

	// start easy by putting all the constructors
	FileAbstraction()
	{
		localfile = null;
		localurl = null;
	}
	String getName()
	{
		if (!bIsApplet)
			return localfile.getName();

		// applet case; strip off slashes and trailing slashes for dirs
		String res = localurl.getPath();
		int lch = (res.charAt(res.length() - 1) == '/' ? res.length() - 1 : res.length());
		int i = res.lastIndexOf("/", lch - 1);
		if (i == -1)
			return res.substring(0, lch);
		return res.substring(i + 1, lch);
	}
	String getPath()
	{
		return (bIsApplet ? localurl.toString() : localfile.getPath());
	}
	String getAbsolutePath()
	{
		assert !bIsApplet;
		return localfile.getAbsolutePath();
	}
	String getCanonicalPath() throws IOException
	{
		assert !bIsApplet;
		return localfile.getCanonicalPath();
	}
	FileAbstraction getParentFile()
	{
		if (bIsApplet)
			return null;
		String parent = localfile.getParent();
		if (parent == null)
			return null;
		return MakeDirectoryFileAbstraction(parent);
	}



	/////////////////////////////////////////////
	Vector listFilesDir(Vector dod) throws IOException
	{
		Vector res = new Vector();

		if (bIsApplet)
		{
			URL urllistdir = new URL(localurl, "listdir.txt");
System.out.println(urllistdir);

			//LineInputStream lis = new LineInputStream(FileAbstraction lloadfile, "", "");
			BufferedReader br = new BufferedReader(new InputStreamReader(urllistdir.openStream()));
			String lfile;
			while ((lfile = br.readLine()) != null)
			{
				int ib = lfile.indexOf(' ');
				assert ib != -1;
				String stype = lfile.substring(0, ib);
				String sfil = lfile.substring(ib + 1);

				if (stype.equals("DIR"))
				{
					FileAbstraction fad = new FileAbstraction();
					fad.xfiletype = FA_DIRECTORY;
					fad.localurl = new URL(localurl, sfil + "/");
					dod.addElement(fad);
System.out.println("DIR  " + fad.getName());
					continue;
				}

				FileAbstraction faf = new FileAbstraction();
				faf.localurl = new URL(localurl, sfil);
				if (stype.equals("SVX"))
					faf.xfiletype = FA_FILE_SVX;
				else if (stype.equals("POS"))
					faf.xfiletype = FA_FILE_POS;
				else if (stype.equals("MEASUREMENTS"))
					faf.xfiletype = FA_FILE_XML_MEASUREMENTS;
				else if (stype.equals("EXPORTS"))
					faf.xfiletype = FA_FILE_XML_EXPORTS;
				else if (stype.equals("FONTCOLOURS"))
					faf.xfiletype = FA_FILE_XML_FONTCOLOURS;
				else if (stype.equals("SKETCH"))
					faf.xfiletype = FA_FILE_XML_SKETCH;
				else if (stype.equals("IMG"))
					faf.xfiletype = FA_FILE_IMAGE;
				else
					assert false;
				res.addElement(faf);
			}
			br.close();
			return res;
		}

		assert localfile.isDirectory();
		
		// 1.5 version
		//List<File> sfileslist = Arrays.asList(localfile.listFiles());
		//Collections.sort(sfileslist);
		//File[] sfiles = sfileslist.toArray(new File[0]);  // argument passes in the type

		// 1.4 version
		File[] sfiles = localfile.listFiles();  

		for (int i = 0; i < sfiles.length; i++)
		{
			File tfile = sfiles[i].getCanonicalFile();
			if (tfile.isFile())
			{
				FileAbstraction faf = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
				faf.xfiletype = faf.GetFileType();  // part of the constructor?
				res.addElement(faf);
			}
			else if (tfile.isDirectory() && (dod != null))
			{
				FileAbstraction fad = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
				fad.xfiletype = FA_DIRECTORY;
				dod.addElement(fad);
			}
		}
		return res;
	}

	boolean mkdirs()
	{
		assert !bIsApplet;
		return localfile.mkdirs();
	}
	boolean isDirectory()
	{
		assert !bIsApplet;
		return localfile.isDirectory();
	}
	boolean isFile()
	{
		assert !bIsApplet; 
		return localfile.isFile();
	}
	boolean exists()
	{
		assert !bIsApplet;
		return localfile.exists();
	}
	boolean canRead()
	{
		assert !bIsApplet;
		return localfile.canRead();
	}
	boolean equals(FileAbstraction fa)
	{
		assert !bIsApplet;
		return ((fa != null) && localfile.equals(fa.localfile));
	}


	// this is killed
	public String toString()
	{
//		assert false;
		return localfile.toString();
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeOpenableFileAbstraction(String fname)
	{
		assert !bIsApplet; 
		FileAbstraction res = new FileAbstraction();
		res.localfile = new File(fname);
		res.bIsDirType = false;
		return res;
	}
	/////////////////////////////////////////////
	static FileAbstraction MakeWritableFileAbstractionF(File file)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = file;
		res.bIsDirType = false;
		return res;
	}
	/////////////////////////////////////////////
	static FileAbstraction MakeWritableFileAbstraction(String fname)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = new File(fname);
		res.bIsDirType = false;
		return res;
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeOpenableFileAbstractionF(File file)
	{
		assert !bIsApplet; 
		FileAbstraction res = new FileAbstraction();
		res.localfile = file;
		res.bIsDirType = false; // unknown
		return res;
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeCurrentUserDirectory()
	{
		// this is used to start the file dialog off.  To get it to land in the current 
		// directory, rather than the directory above with the current directory selected, 
		// it looks like we'd have to find a file/directory in this directory and select it.  
		// this seems to be the limitations of JFileChooser.setSelectedFile
		File Linitialuserdir = new File("").getAbsoluteFile(); 
		FileAbstraction fa = FileAbstraction.MakeDirectoryFileAbstractionF(Linitialuserdir);
		FileAbstraction fac = FileAbstraction.MakeCanonical(fa); 
		return fac; 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeDirectoryFileAbstraction(String dname)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = new File(dname);
		res.bIsDirType = true;
		return res;
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeDirectoryFileAbstractionF(File dfile)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = dfile;
		res.bIsDirType = true;
		return res;
	}

	static FileAbstraction MakeDirectoryAndFileAbstraction(FileAbstraction dfile, String fname)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = (dfile != null ? new File(dfile.localfile, fname) : new File(fname));
		res.bIsDirType = false;
		return res;
	}

	static FileAbstraction MakeDirectoryDirectoryAbstraction(FileAbstraction dfile, String dname)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = new File(dfile.localfile, dname);
		res.bIsDirType = true;
		return res;
	}

	static FileAbstraction MakeCanonical(FileAbstraction fa) 
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		try { res.localfile = new File(fa.localfile.getCanonicalPath()); }
		catch (IOException e) {;};
		res.bIsDirType = fa.bIsDirType;
		return res;
	}

	/////////////////////////////////////////////

	/////////////////////////////////////////////
	// looks for the object type listed after the tunnelxml
	static char[] filehead = new char[256];
	private int GetFileType()
	{
		if (getName().startsWith(".#"))
			return FileAbstraction.FA_FILE_IGNORE;

		String suff = TN.getSuffix(getName());

		// work some out from just the suffix
		if (suff.equals(TN.SUFF_SVX))
			return FA_FILE_SVX;
		if (suff.equals(TN.SUFF_POS))
			return FA_FILE_POS;
		if (suff.equals(TN.SUFF_PNG) || suff.equalsIgnoreCase(TN.SUFF_GIF) || suff.equalsIgnoreCase(TN.SUFF_JPG))
			return FA_FILE_IMAGE;
		if (suff.equalsIgnoreCase(TN.SUFF_TXT))
			return FA_FILE_IGNORE;

		// remaining non-xml types
		if (!suff.equalsIgnoreCase(TN.SUFF_XML))
		{
			for (int i = 0; i < TN.SUFF_IGNORE.length; i++)
				if (suff.equalsIgnoreCase(TN.SUFF_IGNORE[i]))
					return FA_FILE_IGNORE;
			TN.emitMessage("Unknown file type " + getName());
			return FA_FILE_UNKNOWN;
		}


		// the XML file types require loading the header to determin what's in them
		// look for the xml tag that follows <tunnelxml>
		String sfilehead = null;
		try
		{
			FileReader fr = new FileReader(localfile);
			int lfilehead = fr.read(filehead, 0, filehead.length);
			fr.close();
			if (lfilehead == -1)
				return FA_FILE_UNKNOWN;
			sfilehead = new String(filehead, 0, lfilehead);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
		}
		String strtunnxml = "<tunnelxml>";
		int itunnxml = sfilehead.indexOf(strtunnxml);
		if (itunnxml == -1)
			return FA_FILE_UNKNOWN;

		// this should be quitting when it gets to a space or a closing >
		int bracklo = sfilehead.indexOf('<', itunnxml + strtunnxml.length());
		int brackhic = sfilehead.indexOf('>', bracklo + 1);
		int brackhis = sfilehead.indexOf(' ', bracklo + 1);
		int brackhi = (brackhis != -1 ? Math.min(brackhic, brackhis) : brackhic);
		if ((bracklo == -1) || (brackhi == -1))
			return FA_FILE_UNKNOWN;

		String sres = sfilehead.substring(bracklo + 1, brackhi);

		if (sres.equals("sketch"))
			return FA_FILE_XML_SKETCH;
		if (sres.equals("exports"))
			return FA_FILE_XML_EXPORTS;
		if (sres.equals("measurements"))
			return FA_FILE_XML_MEASUREMENTS;
		if (sres.equals("fontcolours"))
			return FA_FILE_XML_FONTCOLOURS;

		return FA_FILE_UNKNOWN;
	}

	/////////////////////////////////////////////
	// we could construct a miniclass or structure of vectors with
	// indexes from the xfiletype values, that recurses and gives us the entire tree of
	// FileAbstractions, which may be URLs or Files.

	// need to build up the tree structure separately, and then import into all the tunnels.
	// so that the tree/file information can be transmitted at once from the server.
	// and then later the different FileAbstractions can pull the data from URLs rather than
	// the File.

	// Or at the very least, make listFilesDir(dod) the secret of what can be got from the
	// server, and this forms the basis for pulling anything in.

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static boolean FindFilesOfDirectory(OneTunnel tunnel, Vector dod) throws IOException
	{
		Vector fod = tunnel.tundirectory.listFilesDir(dod);

		// here we begin to open XML readers and such like, filling in the different slots.
		boolean bsomethinghere = false;
		for (int i = 0; i < fod.size(); i++)
		{
			FileAbstraction tfile = (FileAbstraction)fod.elementAt(i);
			assert tfile.isFile();

			int iftype = tfile.xfiletype;

			// fill in the file positions according to what was in this file.
			if (iftype == FileAbstraction.FA_FILE_XML_EXPORTS)
			{
				assert tunnel.exportfile == null;
				tunnel.exportfile = tfile;
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_XML_MEASUREMENTS)
			{
				assert tunnel.measurementsfile == null;
				tunnel.measurementsfile = tfile;
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_XML_SKETCH)
			{
				tunnel.tsketches.addElement(tfile);
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_XML_FONTCOLOURS)
			{
				tunnel.tfontcolours.addElement(tfile);
			}

			else if (iftype == FileAbstraction.FA_FILE_SVX)
			{
				assert tunnel.svxfile == null;
				tunnel.svxfile = tfile;
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_POS)
			{
				assert tunnel.posfile == null;
				tunnel.posfile = tfile;
			}
			else if (iftype == FileAbstraction.FA_FILE_IMAGE)
				;
			else if (iftype == FileAbstraction.FA_FILE_IGNORE)
				;
			else
			{
				TN.emitWarning("Unknown file type: " + tfile.getName());
				assert (iftype == FileAbstraction.FA_FILE_UNKNOWN);
			}
		}
		return bsomethinghere;
	}


	/////////////////////////////////////////////
	static boolean FileDirectoryRecurse(OneTunnel tunnel, FileAbstraction loaddirectory) throws IOException
	{
		tunnel.tundirectory = loaddirectory;

		Vector dod = new Vector();
		if (!FileAbstraction.FindFilesOfDirectory(tunnel, dod))  // nothing here
			return false;

		// get the subdirectories and recurse.
		for (int i = 0; i < dod.size(); i++)
		{
			FileAbstraction sdir = (FileAbstraction)dod.elementAt(i);
			assert sdir.isDirectory();
			String dtname = sdir.getName();
			OneTunnel dtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(dtname, null));
			if (!FileDirectoryRecurse(dtunnel, sdir))
				tunnel.ndowntunnels--; // if there's nothing interesting, take this introduced tunnel back out!
		}
		return true;
	}




	/////////////////////////////////////////////
	static void ApplyFilenamesRecurse(OneTunnel tunnel, FileAbstraction savedirectory)
	{
		// move the sketches that may already be there (if we foolishly made some)
		for (int i = 0; i < tunnel.tsketches.size(); i++)
		{
			assert tunnel.tsketches.elementAt(i) instanceof OneSketch; // no file types here, everything must be loaded
			OneSketch lsketch = (OneSketch)tunnel.tsketches.elementAt(i);
			lsketch.sketchfile = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, lsketch.sketchfile.getName());
			lsketch.bsketchfilechanged = true;
		}

		// generate the files in this directory.
		tunnel.tundirectory = savedirectory;
		try
		{
			if (tunnel.tundirectory.isDirectory())
				FileAbstraction.FindFilesOfDirectory(tunnel, null);
		}
		catch (IOException ie)
		{
			TN.emitWarning("IOexception " + ie.toString());
		}
		// This seems to be the only function that sets the file names, but only if they are not null.
		// So file names never get set in the first place.
		// If the XML directory is being reset, then again the file names need to change, so I edited out the if statements.
		// Martin
		//if (tunnel.svxfile != null)
		tunnel.svxfile = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, tunnel.name + TN.SUFF_SVX);
		tunnel.bsvxfilechanged = true;

		// generate the xml file from the svx
		//if (tunnel.measurementsfile != null)
		tunnel.measurementsfile = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, tunnel.name + TN.SUFF_XML);
		tunnel.bmeasurementsfilechanged = true;

		// generate the files of exports
		//if (tunnel.exportfile != null)
		tunnel.exportfile = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, tunnel.name + "-exports" + TN.SUFF_XML);
		tunnel.bexportfilechanged = true;


		// work with all the downtunnels
		for (int i = 0; i < tunnel.ndowntunnels; i++)
		{
			FileAbstraction downdirectory = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, tunnel.downtunnels[i].name);
			ApplyFilenamesRecurse(tunnel.downtunnels[i], downdirectory);
		}
	}


	static FileAbstraction currentSymbols = null;
	/////////////////////////////////////////////
	static void InitFA() // enables the applet type
	{
		if (!bIsApplet)
		{
			FileAbstraction fauserdir = FileAbstraction.MakeDirectoryFileAbstraction(System.getProperty("user.dir"));
			FileAbstraction.currentSymbols = FileAbstraction.MakeDirectoryDirectoryAbstraction(fauserdir, "symbols");
		}
		else
		{
			ClassLoader cl = MainBox.class.getClassLoader();
			FileAbstraction.currentSymbols = new FileAbstraction();
			FileAbstraction.currentSymbols.localurl = cl.getResource("symbols/");
			FileAbstraction.currentSymbols.bIsDirType = true;
System.out.println("currentsymb: " + FileAbstraction.currentSymbols.localurl);
		}
	}
}





