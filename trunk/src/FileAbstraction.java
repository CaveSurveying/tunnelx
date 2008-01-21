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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Arrays;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

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
	static int FA_FILE_3D = 5;

	// multiple files possible
	static int FA_FILE_XML_SKETCH = 6;
	static int FA_FILE_XML_FONTCOLOURS = 7;

	static int FA_FILE_IMAGE = 8;
	static int FA_FILE_IGNORE = 9;

	static int FA_DIRECTORY = 10;

	static boolean bIsApplet = true; // default type, because starting in the static main of MainBox allows us to set to false
	static URL documentbase = null;

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
	String getSketchName()
	{
		if (xfiletype != FA_FILE_XML_SKETCH)
			TN.emitError("file " + getName() + " has wrong type: " + xfiletype);
		assert xfiletype == FA_FILE_XML_SKETCH;
		String sname = getName();
		assert sname.substring(sname.length() - 4).equalsIgnoreCase(".xml");
		return sname.substring(0, sname.length() - 4);
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
	List<FileAbstraction> listFilesDir(List<FileAbstraction> dod) throws IOException
	{
		List<FileAbstraction> res = new ArrayList<FileAbstraction>();
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
					dod.add(fad);
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
				res.add(faf);
			}
			br.close();
			return res;
		}

		assert localfile.isDirectory();

		// 1.5 version
		List<File> sfileslist = Arrays.asList(localfile.listFiles());
		Collections.sort(sfileslist);
		for (File sfile : sfileslist)
		{
			File tfile = sfile.getCanonicalFile();
			if (tfile.isFile())
			{
				FileAbstraction faf = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
				faf.xfiletype = faf.GetFileType();  // part of the constructor?
				res.add(faf);
			}
			else if (tfile.isDirectory() && (dod != null))
			{
				FileAbstraction fad = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
				fad.xfiletype = FA_DIRECTORY;
				dod.add(fad);
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

	public BufferedReader GetBufferedReader()
	{
		try
		{
 		BufferedReader br = new BufferedReader(FileAbstraction.bIsApplet ? new InputStreamReader(localurl.openStream())
		 																	 : new FileReader(localfile));
		return br; 
		}
		catch (IOException e)
			{;};
		return null; 
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
		//assert !bIsApplet;
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
		assert dfile.bIsDirType; 
		FileAbstraction res = new FileAbstraction();
		res.localfile = new File(dfile.localfile, dname);
		res.bIsDirType = true;
		return res;
	}

	static FileAbstraction MakeCanonical(FileAbstraction fa) 
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		try
			{ res.localfile = new File(fa.localfile.getCanonicalPath()); }
		catch (IOException e) 
			{;};
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
		if (suff.equals(TN.SUFF_3D))
			return FA_FILE_3D;
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
	static boolean FindFilesOfDirectory(OneTunnel tunnel, List<FileAbstraction> dod) throws IOException
	{
		List<FileAbstraction> fod = tunnel.tundirectory.listFilesDir(dod);

		// here we begin to open XML readers and such like, filling in the different slots.
		boolean bsomethinghere = false;
		for (FileAbstraction tfile : fod)
		{
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
				tunnel.tsketches.add(new OneSketch(tfile, tunnel));
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_XML_FONTCOLOURS)
				tunnel.tfontcolours.add(tfile);

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
				assert tunnel.vposlegs == null; 
			}
			else if (iftype == FileAbstraction.FA_FILE_3D)
			{
				assert tunnel.t3dfile == null;
				tunnel.t3dfile = tfile;
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

		List<FileAbstraction> dod = new ArrayList<FileAbstraction>();
		if (!FileAbstraction.FindFilesOfDirectory(tunnel, dod))  // nothing here
			return false;

		// get the subdirectories and recurse.
		for (FileAbstraction sdir : dod)
		{
			assert sdir.isDirectory();
			String dtname = sdir.getName();
			OneTunnel dtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(dtname, null));
			if (!FileDirectoryRecurse(dtunnel, sdir))
				tunnel.vdowntunnels.remove(tunnel.vdowntunnels.size() - 1); // if there's nothing interesting, take this introduced tunnel back out!
		}
		return true;
	}




	/////////////////////////////////////////////
	static void ApplyFilenamesRecurse(OneTunnel tunnel, FileAbstraction savedirectory)
	{
		// move the sketches that may already be there (if we foolishly made some)
		for (OneSketch lsketch : tunnel.tsketches)
		{
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
		for (OneTunnel downtunnel : tunnel.vdowntunnels)
		{
			FileAbstraction downdirectory = FileAbstraction.MakeDirectoryAndFileAbstraction(savedirectory, downtunnel.name);
			ApplyFilenamesRecurse(downtunnel, downdirectory);
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


	/////////////////////////////////////////////
	static BufferedImage cachedframedimage = null;  // some very limited caching for use by UI when reselecting the windows
	static String cachedframedimageabspath = "";
	BufferedImage GetImage(boolean bFramed)
	{
		if ((cachedframedimage != null) && getAbsolutePath().equals(cachedframedimageabspath))
		{
			TN.emitMessage("Reusing cached image: " + getAbsolutePath());
			return cachedframedimage;
		}
		BufferedImage res = null;
		try
		{
			TN.emitMessage("Loading image: " + getAbsolutePath());
			res = ImageIO.read(localfile);
			if (res == null)
			{
				String[] imnames = ImageIO.getReaderFormatNames();
				System.out.println("Image reader format names: ");
				for (int i = 0; i < imnames.length; i++)
					System.out.println(imnames[i]);
			}
		}
		catch (IOException e)
			{  TN.emitWarning("getimageIO " + e.toString()); };

		if (bFramed)
		{
			cachedframedimage = res;
			cachedframedimageabspath = getAbsolutePath();
		}
		return res;
	}

	/////////////////////////////////////////////
	void SaveImage(BufferedImage bi, String ftype)
	{
		try
		{
			TN.emitMessage("Writing file " + getAbsolutePath() + " with type " + ftype);
			if (bIsApplet)
			{
				URL ldocumentbase = new URL("http://www.freesteel.co.uk/cgi-bin/tunnelapplet.py?file=");
				TN.emitMessage("to upload this to:" + ldocumentbase);
//write(RenderedImage im, String formatName, OutputStream output)
			}
			else
				ImageIO.write(bi, ftype, localfile);
		}
		catch (Exception e)
			{ e.printStackTrace(); }
	}
}





