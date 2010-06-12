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
import java.io.InputStream; 
import java.io.FileInputStream; 
import java.io.FileReader;
import java.io.File;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Arrays;
//import java.util.regexp.Pattern; // does not exist

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import java.util.regex.Matcher; 
import java.util.regex.Pattern; 

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
	static int FA_FILE_POCKET_TOPO = 11;

	// default type, because starting in the static main of MainBox allows us to set to false
	static boolean bIsApplet = true; 

	// the actual
	File localfile;
	URL localurl;
	boolean bIsDirType = false;
	int xfiletype;

	static FileAbstraction currentSymbols = new FileAbstraction();
	static FileAbstraction helpFile = new FileAbstraction();
	static FileAbstraction tutorialSketches = new FileAbstraction();
	static File tmpdir = null;

	static void InitFA()
	{
System.out.println(TN.tunneldate()); 

// this is where we set up the symbols directory
		ClassLoader cl = MainBox.class.getClassLoader();

        currentSymbols = new FileAbstraction();
        currentSymbols.bIsDirType = true; 
        tmpdir = null;

        tutorialSketches = new FileAbstraction();
        tutorialSketches.bIsDirType = true; 

        // unix type
        if (System.getProperty("file.separator").equals("/") && !bIsApplet) 
		{
            currentSymbols.localfile = new File(System.getProperty("user.home"), ".tunnelx/symbols/"); 
            if (!currentSymbols.localfile.isDirectory())
                currentSymbols.localfile = new File("/usr/share/tunnelx/symbols/"); 
            if (!currentSymbols.localfile.isDirectory())
                currentSymbols.localfile = null; 

            tmpdir = new File(System.getProperty("user.dir"), ".tunnelx/tmp/"); 
            if (!tmpdir.isDirectory())
                tmpdir = new File("/tmp/"); 
            if (!tmpdir.isDirectory())
                tmpdir = null; 
        }
        if (currentSymbols.localfile == null)
        {
    		currentSymbols.localurl = cl.getResource("symbols/");
            if (currentSymbols.localurl == null) 
                currentSymbols.localurl = cl.getResource("symbols/listdir.txt");   // this gets it from the jar file
        }
        if (tutorialSketches.localfile == null)
        {
    		tutorialSketches.localurl = cl.getResource("tutorials/");
            if (tutorialSketches.localurl == null) 
                tutorialSketches.localurl = cl.getResource("tutorials/listdir.txt");   // this gets it from the jar file
        }


        // the useful help file (hope this can pull from the jar file)
        helpFile.localurl = cl.getResource("symbols/helpfile.html"); 
        if (helpFile.localurl == null)
            TN.emitWarning("Missing symbols/helpfile.html"); 
System.out.println("sysysysysy " + FileAbstraction.helpFile.getAbsolutePath()); 

		if (!bIsApplet && (tmpdir == null))
    	    tmpdir = new File(System.getProperty("user.dir"), "tmp"); 

        if ((TN.tunneluser == null) || TN.tunneluser.equals(""))
            TN.tunneluser = System.getProperty("user.name"); 

		TN.emitMessage("currentSymbols: " + FileAbstraction.currentSymbols.getAbsolutePath()); 
		TN.emitMessage("tutorials: " + FileAbstraction.tutorialSketches.getAbsolutePath()); 
		TN.emitMessage("tmpdir: " + FileAbstraction.tmpdir.getAbsolutePath()); 
		TN.emitMessage("tunneluser: " + TN.tunneluser); 
	}

	// start easy by putting all the constructors
	FileAbstraction()
	{
		localfile = null;
		localurl = null;
	}

	String getName()
	{
		if (localurl != null)
		{
			// applet case; strip off slashes and trailing slashes for dirs
			String res = localurl.getPath();
			int lch = (res.charAt(res.length() - 1) == '/' ? res.length() - 1 : res.length());
			int i = res.lastIndexOf("/", lch - 1);
			if (i == -1)
				return res.substring(0, lch);
			return res.substring(i + 1, lch);
		}
		assert !bIsApplet; 
		return localfile.getName();
	}

	String getSketchName()
	{
        String sname = getName();
		if (xfiletype == FA_FILE_XML_SKETCH)
        {
            assert sname.substring(sname.length() - 4).equalsIgnoreCase(".xml");
            return sname.substring(0, sname.length() - 4);
		}
        else if (xfiletype == FA_FILE_SVX)
        {
            assert sname.substring(sname.length() - 4).equalsIgnoreCase(".svx");
            return sname.substring(0, sname.length() - 4);
		}
        TN.emitError("file " + sname + " has wrong type: " + xfiletype);
		return sname;
	}

	String getPath()
	{
		return (localurl != null ? localurl.toString() : localfile.getPath());
	}
	String getAbsolutePath()
	{
		assert !bIsApplet;
		if (localfile != null)
			return localfile.getAbsolutePath();
		if (localurl != null)
			return localurl.toString(); 
		return null; 
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
        assert localfile != null; 
		String parent = localfile.getParent();
		if (parent == null)
			return null;
		return MakeDirectoryFileAbstraction(parent);
	}
	
	boolean isDirectory()
	{
		assert !bIsApplet;
		if (localurl == null)
            return localfile.isDirectory();
        String urlname = localurl.getPath(); 
        return ((urlname.indexOf("/tunneldata") != -1) && (urlname.indexOf(".xml") == -1)); 
	}
	boolean isFile()
	{
		assert !bIsApplet;
		return localfile.isFile();
	}
	boolean equals(FileAbstraction fa)
	{
		if ((localurl != null) && (fa.localurl != null)) 
			return localurl.equals(fa.localurl); 
		
		if ((localfile == null) || (fa.localfile == null))
			return false; 
			
		assert !bIsApplet;
		return localfile.equals(fa.localfile); 
	}


	// this is killed (except for debug)
	public String toString()
	{
		//assert false;
		if (localfile != null)
			return localfile.toString();
		if (localurl != null)
			return localurl.toString();
		return "null"; 
	}

	/////////////////////////////////////////////
	static FileAbstraction MakeOpenableFileAbstraction(String fname)
	{
		assert !bIsApplet;

		FileAbstraction res = new FileAbstraction();
		res.bIsDirType = false;

		int ihttp = fname.indexOf("http:"); 
		if (ihttp != -1)
		{
			ihttp += 5; 
			while ((ihttp < fname.length()) && ((fname.charAt(ihttp) == '\\') || (fname.charAt(ihttp) == '/')))
				ihttp++; 
			String utail = fname.substring(ihttp).replace('\\', '/'); 
            System.out.println("UUUUUUU   " + utail); 
			try
			{
				res.localurl = new URL("http://" + utail);
			}
			catch (MalformedURLException e)
				{ TN.emitWarning("yyy");}
			if (res.localurl == null)
				return null; 
		}
		else
			res.localfile = new File(fname);

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

    // case insensitive (often applies to survex files made under windows)
	static FileAbstraction MakeDirectoryAndFileAbstractionCI(FileAbstraction dfile, String fname)
	{
		assert !bIsApplet;
		FileAbstraction res = new FileAbstraction();
		res.localfile = (dfile != null ? new File(dfile.localfile, fname) : new File(fname));
		res.bIsDirType = false;  // will be true for last call of function
        if (!res.localfile.exists() && (dfile.localfile != null) && dfile.localfile.isDirectory())
        {
    		for (File f : dfile.localfile.listFiles())
            {
                if (fname.equalsIgnoreCase(f.getName()))
                {
                    res.localfile = new File(dfile.localfile, f.getName()); 
                    System.out.println("MakeDirectoryAndFileAbstractionCI matches-- " + fname + " " + f.getName()); 
                }
            }
        }
    	return res;
	}


	/////////////////////////////////////////////
	static FileAbstraction MakeCanonical(FileAbstraction fa)
	{
		if (fa.localurl != null)
			return fa; 
			
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
	// replaces GetBufferedReader which doesn't allow for closing the original stream!
    InputStream GetInputStream() throws IOException
	{
		if (localurl != null)
			return localurl.openStream(); 
		assert !bIsApplet; 
		return new FileInputStream(localfile);
	}

	/////////////////////////////////////////////
    String ReadFileHead()
    {
		// the XML file types require loading the header to determin what's in them
		// look for the xml tag that follows <tunnelxml>
		String sfilehead = "";
		try
		{
			InputStream inputstream = GetInputStream(); 
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream); 
			int lfilehead = inputstreamreader.read(filehead, 0, filehead.length);
			inputstream.close();
			if (lfilehead == -1)
            {			
                TN.emitWarning("****  left file unknown on " + getName()); 
                return "";
            }
			sfilehead = new String(filehead, 0, lfilehead);
		}
		catch (IOException e)
		{
			TN.emitWarning(e.toString());
		}
        return sfilehead; 
    }

	/////////////////////////////////////////////
    // read characters until count nLB of < brackets
    String ReadFileHeadLB(int nLB)
    {
		// the XML file types require loading the header to determin what's in them
		// look for the xml tag that follows <tunnelxml>
		StringBuffer sb = new StringBuffer();
		try
		{
			InputStream inputstream = GetInputStream(); 
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream); 
			for (int i = 0; ((nLB == -1) || (i < 1024)); i++)
            {
                int ch = inputstreamreader.read(); 
                if (ch == -1)
                    break; 
                sb.append((char)ch); 
                if ((nLB != -1) && (ch == '<'))
                {
                    nLB--; 
                    if (nLB == 0)
                        break; 
                }
            }
            inputstream.close(); 
        }
		catch (IOException e)
		{
			TN.emitWarning(e.toString());
		}
        return sb.toString(); 
    }

	/////////////////////////////////////////////
// we could use this opportunity to detect the version, project and user for this file
// also should record dates (changed in upload)
	// looks for the object type listed after the tunnelxml
	static char[] filehead = new char[1024];
	int GetFileType()
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
		if (suff.equalsIgnoreCase(TN.SUFF_PNG) || suff.equalsIgnoreCase(TN.SUFF_GIF) || suff.equalsIgnoreCase(TN.SUFF_JPG))
			return FA_FILE_IMAGE;
		if (suff.equalsIgnoreCase(TN.SUFF_TXT))
        {
            String sfilehead = ReadFileHead();
            if (sfilehead.indexOf("FIX") == 0)
                return FA_FILE_POCKET_TOPO; 
			return FA_FILE_IGNORE;
        }

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
        String sfilehead = ReadFileHeadLB(4); 
        TN.emitMessage("READ " + sfilehead.length() + " chars of " + getName()); 


		String strtunnxml = "<tunnelxml";
		int itunnxml = sfilehead.indexOf(strtunnxml);
		if (itunnxml == -1)
        {			
            TN.emitWarning("****  missing <tunnelxml on " + getName()); 
System.out.println(sfilehead); 
			return FA_FILE_UNKNOWN;
        }
		// this should be quitting when it gets to a space or a closing >
		int bracklo = sfilehead.indexOf('<', itunnxml + strtunnxml.length());
		int brackhic = sfilehead.indexOf('>', bracklo + 1);
		int brackhis = sfilehead.indexOf(' ', bracklo + 1);
		int brackhi = (brackhis != -1 ? Math.min(brackhic, brackhis) : brackhic);
		if ((bracklo == -1) || (brackhi == -1))
        {			
            TN.emitWarning("****  missing bracket on " + getName() + " " + bracklo + " " + brackhic + " " + brackhis + " " + brackhi); 
			System.out.println("FILEHEAD: " + sfilehead); 
            return FA_FILE_UNKNOWN;
        }
		String sres = sfilehead.substring(bracklo + 1, brackhi);

		if (sres.equals("sketch"))
			return FA_FILE_XML_SKETCH;
		if (sres.equals("exports"))
			return FA_FILE_XML_EXPORTS;
		if (sres.equals("measurements"))
			return FA_FILE_XML_MEASUREMENTS;
		if (sres.equals("fontcolours"))
			return FA_FILE_XML_FONTCOLOURS;

        TN.emitWarning("****  what's sres " + sres + " " + getName()); 
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
	List<FileAbstraction> listFilesDir() throws IOException
	{
		List<FileAbstraction> res = new ArrayList<FileAbstraction>();
		if (localurl != null)
		{
			URL urllistdir = localurl;
			//System.out.println("eee " + urllistdir);
			//DumpURL(urllistdir); 
			BufferedReader br = new BufferedReader(new InputStreamReader(urllistdir.openStream()));
			List<String> sres = new ArrayList<String>(); 
			String lfile;
			while ((lfile = br.readLine()) != null)
				sres.add(lfile); 
			Collections.sort(sres);
			for (String llfile : sres)
			{
				FileAbstraction faf = new FileAbstraction(); 
				faf.localurl = new URL(urllistdir, llfile);
				//System.out.println(faf.localurl + " = " + urllistdir + " " + llfile); 
				faf.xfiletype = faf.GetFileType();  // part of the constructor?
				//System.out.println(" Nnnnn " + faf.getName() + " " + faf.xfiletype); 
				res.add(faf);
			}
			br.close();
			return res;
		}

		assert !bIsApplet; 
		assert localfile.isDirectory();
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
		}
		return res;
	}

	static boolean isDirectory(String ldir)
	{
		assert !bIsApplet;
		return new File(ldir).isDirectory();
	}


	/////////////////////////////////////////////
	List<FileAbstraction> GetDirContents() throws IOException
	{
		List<FileAbstraction> res = new ArrayList<FileAbstraction>();
		if (localurl != null)
		{
            Pattern fildir = Pattern.compile("<a class=\"(.*?)\" href=\"(.*?)\">");
			String urlpath = localurl.getPath(); 
            boolean bjgtfile = (urlpath.indexOf("/jgtfile/") != -1); 
            //<li class="file"><a href="/troggle/jgtfile/tunneldata2007/204area.3d">tunneldata2007/204area.3d</a> (148607 bytes)</li>

            // http://troggle.cavingexpedition.com/troggle/jgtfile/tunneldata2007
			URL urllistdir = localurl;
			//System.out.println("eee " + urllistdir);
			//DumpURL(urllistdir); 
            TN.emitMessage("Reading... " + urlpath); 
			BufferedReader br = new BufferedReader(new InputStreamReader(localurl.openStream()));
			List<String> sres = new ArrayList<String>(); 
			String lfile;
			while ((lfile = br.readLine()) != null)
            {
                Matcher mdir = fildir.matcher(lfile); 
                if (mdir.find())
                {
                    System.out.println("jsdfsdf " + mdir.group(1) + "  " + mdir.group(2)); 
                    if (mdir.group(1).equals("subdir"))
                    {
                        FileAbstraction faf = new FileAbstraction(); 
                        faf.localurl = new URL(localurl, mdir.group(2));
                        faf.bIsDirType = true; 
                        faf.xfiletype = FA_DIRECTORY; 
                        res.add(faf); 
                    }
                    if (mdir.group(1).equals("filesketch"))
                    {
                        FileAbstraction faf = new FileAbstraction(); 
                        faf.localurl = new URL(localurl, mdir.group(2));
                        faf.xfiletype = FA_FILE_XML_SKETCH; 
                        res.add(faf); 
                    }
                    if (mdir.group(1).equals("filesvx"))
                    {
                        FileAbstraction faf = new FileAbstraction(); 
                        faf.localurl = new URL(localurl, mdir.group(2));
                        faf.xfiletype = FA_FILE_SVX; 
                        res.add(faf); 
                    }
                    if (mdir.group(1).equals("filefontcolours"))
                    {
                        FileAbstraction faf = new FileAbstraction(); 
                        faf.localurl = new URL(localurl, mdir.group(2));
                        faf.xfiletype = FA_FILE_XML_FONTCOLOURS; 
                        res.add(faf); 
                    }
                    if (mdir.group(1).equals("fileimage"))
                    {
                        FileAbstraction faf = new FileAbstraction(); 
                        faf.localurl = new URL(localurl, mdir.group(2));
                        faf.xfiletype = FA_FILE_IMAGE; 
                        res.add(faf); 
                    }
                }
            }
		}

        else
		{
            assert localfile.isDirectory();
            List<File> sfileslist = Arrays.asList(localfile.listFiles());
            Collections.sort(sfileslist);
            for (File sfile : sfileslist)
            {
                File tfile = sfile.getCanonicalFile();
                if (tfile.isFile())
                {
                    FileAbstraction faf = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
                    faf.xfiletype = faf.GetFileType();  // part of the constructor?
                    if ((faf.xfiletype == FA_FILE_XML_SKETCH) || (faf.xfiletype == FA_FILE_XML_FONTCOLOURS) || 
                        (faf.xfiletype == FA_FILE_IMAGE) || (faf.xfiletype == FA_FILE_SVX))
                        res.add(faf);
                }
                else if (tfile.isDirectory())
                {
                    FileAbstraction faf = FileAbstraction.MakeOpenableFileAbstractionF(tfile);
                    faf.bIsDirType = true; 
                    faf.xfiletype = FA_DIRECTORY; 
                    if (!tfile.getName().startsWith(".") && !tfile.getName().equals("CVS"))  // skip .svn files
                        res.add(faf);
                }
            }
        }
		return res;
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	void FindFilesOfDirectory(List<OneSketch> tsketches, List<FileAbstraction> tfontcolours) throws IOException
	{
		List<FileAbstraction> fod = listFilesDir();
		
		// here we begin to open XML readers and such like, filling in the different slots.
		boolean bsomethinghere = false;
		for (FileAbstraction tfile : fod)
		{
			int iftype = tfile.xfiletype;

			// fill in the file positions according to what was in this file.
			if (iftype == FileAbstraction.FA_FILE_XML_SKETCH)
			{
				tsketches.add(new OneSketch(tfile));
				bsomethinghere = true;
			}
			else if (iftype == FileAbstraction.FA_FILE_XML_FONTCOLOURS)
				tfontcolours.add(tfile);
		}
	}



	/////////////////////////////////////////////
	static void DumpURL(URL url)
	{
		try
		{
		System.out.println("Printing contents of: " + url);
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		String sline;
		while ((sline = br.readLine()) != null)
			System.out.println("JJJ: " + sline);
		br.close();
		System.out.println("ENDS");
		}
		catch (IOException ioe)
		{ System.out.println("Exception:" + ioe); }
	}


	/////////////////////////////////////////////
	static BufferedImage cachedframedimage = null;  // some very limited caching for use by UI when reselecting the windows
	static String cachedframedimageabspath = "";
	BufferedImage GetImage(boolean bFramed)
	{
		if ((cachedframedimage != null) && getAbsolutePath().equals(cachedframedimageabspath))
		{
			//TN.emitMessage("Reusing cached image: " + getAbsolutePath());
			return cachedframedimage;
		}
		BufferedImage res = null;
		try
		{
			TN.emitMessage("Loading image: " + getAbsolutePath());
			if (localfile != null)
				res = ImageIO.read(localfile); 
			else if (localurl != null)
				res = ImageIO.read(localurl); 
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
	FileAbstraction SaveAsDialog(boolean bsketchprint, JFrame frame)  // sketch/print=false/true
	{
		// this == sketchgraphicspanel.sketchdisplay, but for the fact we're in an anonymous event listner
        int ftype = (bsketchprint ? SvxFileDialog.FT_XMLSKETCH : SvxFileDialog.FT_BITMAP); 
        SvxFileDialog sfd = SvxFileDialog.showSaveDialog(this, frame, ftype);
        if (sfd == null)
            return null; 
        FileAbstraction res = sfd.getSelectedFileA(ftype, true); 
        if (res.localurl == null)
        {
            if (bsketchprint)
                TN.currentDirectory = res; 
            else
                TN.currprintdir = res; 
        }
        return res;
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////

	static String boundry = "-----xxxxxxxBOUNDERxxxxdsx";   // something that is unlikely to appear in the text
	public static void writeField(DataOutputStream out, String name, String value) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
		out.writeBytes(value);
		out.writeBytes("\r\n");
		out.flush();
        TN.emitMessage("WriteField: " + name + "=" + value); 
	}

	/////////////////////////////////////////////
    static void writeTileImageFile(DataOutputStream out, String fieldname, String filename, BufferedImage bi) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + fieldname + "\"; filename=\"" + filename + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Type: image/png");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
		ImageIO.write(bi, "png", out);
		out.writeBytes("\r\n");
	}

	/////////////////////////////////////////////
    static void writeSketchFile(DataOutputStream out, String name, String fileName, OneSketch tsketch) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Type: text/plain");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
    	LineOutputStream los = new LineOutputStream(out);
        tsketch.SaveSketchLos(los); 
		out.writeBytes("\r\n");
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
    // returns the URL of the image that has been uploaded
    // should be a member function
	public static FileAbstraction uploadFile(FileAbstraction target, String fieldname, String filename, BufferedImage bi, OneSketch tsketch)
	{
        String fres = ""; 
		try
		{
        assert target.localurl != null; 
		String response = "";
		URL url = target.localurl;

        // append a command onto the end of the url to say what we're doing
        if (url.toString().endsWith(".xml"))
            url = new URL(url.toString() + "/upload"); 

        TN.emitMessage("About to post\nURL: " + url.toString());

		URLConnection conn = url.openConnection();

		// Set connection parameters.
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);

		// Make server believe we are form data
		conn.setRequestProperty("Content-Type",
                                "multipart/related; boundary=" + boundry);
        //		connection.setRequestProperty("MIME-version", "1.0");

		DataOutputStream out = new DataOutputStream (conn.getOutputStream());
        //		out.write(("--" + boundry + " ").getBytes());

        // write some fields
		writeField(out, "tunneluser", TN.tunneluser);
		writeField(out, "tunnelpassword", TN.tunnelpassword);
		writeField(out, "tunnelproject", TN.tunnelproject);
		writeField(out, "tunnelversion", TN.tunnelversion);

		// Write out the bytes of the content string to the stream.
        assert ((bi == null) != (tsketch == null)); 
        if (bi != null)
	       writeTileImageFile(out, fieldname, filename, bi);
	    else
           writeSketchFile(out, fieldname, filename, tsketch);

		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("--");
		out.writeBytes("\r\n");
		out.flush();
		out.close();

		// Read response from the input stream (should detect the file name).
        // directly from fileupload.html in the django template
        String suploadedfile = "<li>UPLOADEDFILE: "; 
        String smessage = "<p>MESSAGE: "; 
        //Pattern pattuploadedfile = Pattern.compile("<li>UPLOADEDFILE: \"(.*?)\"</li>"); 
        //Pattern pattmessage = Pattern.compile("<p>MESSAGE: (.*?)</li>"); 

		BufferedReader fin = new BufferedReader(new InputStreamReader(conn.getInputStream ()));
		String fline;
		System.out.println("Server response:");
		while ((fline = fin.readLine()) != null)
        {
            System.out.println(":" + fline);
            int iuploadedfile = fline.indexOf(suploadedfile); 
            if (iuploadedfile != -1)
            {
                fres = fline.substring(iuploadedfile + suploadedfile.length()); 
                System.out.println("FIFIF:" + fres + ":::"); 
      		}
        }
		fin.close();
		}

		catch (MalformedURLException e)
			{ TN.emitWarning("yyy"); return null;}
		catch (IOException e)
			{ TN.emitWarning("eee " + e.toString()); return null;}

		return FileAbstraction.MakeOpenableFileAbstraction(fres);
	}


	/////////////////////////////////////////////
    // see: http://seagrass.goatchurch.org.uk/~mjg/cgi-bin/addsurvey.py
    //<placement copyright="left" image="irebyoverlay2.png"
    //spatialreference="WGS84-UTM30">
    //<point grideast="532601" gridnorth="6004830" imx="2550" imy="1734"/>
    //<ewpoint grideast="532651" gridnorth="6004830" imx="2943" imy="1734"/>
    //<nspoint grideast="532601" gridnorth="6004880" imx="2550" imy="1341"/>
	public static void upmjgirebyoverlay(BufferedImage bi, String name, double dpmetrereal, double cornercoordX, double cornercoordY, String spatial_reference_system)
	{
		try
		{
		String target = "http://seagrass.goatchurch.org.uk/caves/image/upload_and_locate_image"; 
        TN.emitMessage("About to post\nURL: " + target);
        System.out.println(" fname=" + name + " ----dots per metre " + dpmetrereal + "  XX " + cornercoordX + "  YY " + cornercoordY + "  spatial_system " + spatial_reference_system); 

		String response = "";
		URL url = new URL(target);
		URLConnection conn = url.openConnection();

		// Set connection parameters.
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);

		// Make server believe we are form data
		conn.setRequestProperty("Content-Type",
                                "multipart/related; boundary=" + boundry);
        //		connection.setRequestProperty("MIME-version", "1.0");

		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        //		out.write(("--" + boundry + " ").getBytes());
        // write some fields
    	//	name=surveyname, acknowledgment=tunnelupload, copyright=left

		writeField(out, "name-name", name);
		writeField(out, "acknowledgment", "tunnelupload");
		writeField(out, "copyright", "left");
		writeField(out, "tunnelversion", TN.tunnelversion);
		//writeField(out, "spatial_reference_system", "WGS84-UTM30");

        int srsid = 31285; // MGI / M31
        //if (spatial_reference_system.equals("OS Grid SD"))
        //   srsid = 13000; 

        double cavealtitude = 1800.0; 

		writeField(out, "at-srsid", String.valueOf(srsid)); 

        
// we want to get these all coming in from the change things
// the GPS signals are 50 apart.  
// GPS from the Ireby is *fix	001	66668.00	78303.00	319.00	;GPS Added    09/01/05 N.P


        double gpx = cornercoordX; 
        double gpy = cornercoordY; 
        //double gpx = 532601 + (cornercoordX - 66668.00); 
        //double gpy = 6004830 + (cornercoordY - 78303.00); 
        double d100 = dpmetrereal * 100;
        String sd100 = String.valueOf(d100);  

        String sdYbot = String.valueOf(bi.getHeight()); 
        String sdYbotup100 = String.valueOf(bi.getHeight() - d100); 
        
		writeField(out, "p1-worldX", String.valueOf(gpx));
		writeField(out, "p1-worldY", String.valueOf(gpy - 100));
		writeField(out, "p1-imageX", "0");
		writeField(out, "p1-imageY", sdYbotup100);
		writeField(out, "p2-worldX", String.valueOf(gpx + 100));
		writeField(out, "p2-worldY", String.valueOf(gpy - 100));
		writeField(out, "p2-imageX", sd100);
		writeField(out, "p2-imageY", sdYbotup100);
		writeField(out, "p3-worldX", String.valueOf(gpx));
		writeField(out, "p3-worldY", String.valueOf(gpy));
		writeField(out, "p3-imageX", "0");
		writeField(out, "p3-imageY", sdYbot);
		writeField(out, "at-average_image_altitude", String.valueOf(cavealtitude));

		// Write out the bytes of the content string to the stream.
        writeTileImageFile(out, "name-image", name + ".png", bi);

		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("--");
		out.writeBytes("\r\n");
		out.flush();
		out.close();


		BufferedReader fin = new BufferedReader(new InputStreamReader(conn.getInputStream ()));
		String fline;
		System.out.println("Server response:");
		while ((fline = fin.readLine()) != null)
            System.out.println(":" + fline);
		fin.close();
		}
		catch (MalformedURLException e)
			{ TN.emitWarning("yyy");}
		catch (IOException e)
			{ TN.emitWarning("eee " + e.toString());};
	}


	/////////////////////////////////////////////
	static boolean SurvexExists()
	{
		if (bIsApplet)
			return false; 
		return new File(TN.survexexecutabledir).isDirectory();
	}


	/////////////////////////////////////////////
    

	/////////////////////////////////////////////
	// this will need to be runable through the web
	static boolean RunSurvex(SurvexLoaderNew sln, String drawlab, Vec3 appsketchLocOffset) 
	{	
		File lposfile = null; 
		if (!tmpdir.isDirectory())
		{
			TN.emitWarning("Must create tunnelx/tmp directory to use this feature");
			return false; 
		}
		String tmpfilename = "tunnelx_tmp_all";
		File lsvxfile = new File(tmpdir, TN.setSuffix(tmpfilename, TN.SUFF_SVX));
		try
		{
			LineOutputStream los = new LineOutputStream(MakeWritableFileAbstractionF(lsvxfile));
			los.WriteLine(drawlab);
			los.close();
		}
		catch (IOException e) { TN.emitWarning(e.toString()); }

		File l3dfile = new File(tmpdir, TN.setSuffix(tmpfilename, TN.SUFF_3D));
		if (l3dfile.isFile())
			l3dfile.delete();
		lposfile = new File(tmpdir, TN.setSuffix(tmpfilename, TN.SUFF_POS));
		if (lposfile.isFile())
			lposfile.delete();

		if (sln == null)  // preview aven
			lposfile = null;

		if (!RunCavern(tmpdir, lsvxfile, l3dfile, lposfile))
		{
			TN.emitWarning("Failed to generate the 3D file");
			return false; 
		}

		// preview aven
        if (sln == null)  
		{
			RunAven(tmpdir, l3dfile);
			return true; 
		}
			
		try
		{
            FileAbstraction fapos = MakeWritableFileAbstractionF(lposfile); 
			LineInputStream lis = new LineInputStream(fapos.GetInputStream(), fapos, null, null);
			boolean bres = sln.LoadPosFile(lis, appsketchLocOffset);
			lis.close();
            lis.inputstream.close(); 
			if (!bres)
				return false;
		}
		catch (IOException e) { TN.emitWarning(e.toString()); }
		return true; 
	}


	/////////////////////////////////////////////
	static boolean OperateProcess(ProcessBuilder pb, String pname)
	{
		try
		{
		pb.redirectErrorStream(true); 
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) 
			TN.emitMessage(" " + pname + ":: " + line);
		int ires = p.waitFor(); 
		if (ires == 0)
			return true; 
		}
		catch (IOException ie)
		{
			TN.emitWarning("@@ caught exception"); 
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		catch (InterruptedException ie)
		{
			TN.emitWarning("@@ caught Interrupted exception"); 
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		return false; 
	}


	/////////////////////////////////////////////
	static boolean RunCavern(File ldirectory, File lsvxfile, File l3dfile, File lposfile)
	{
		List<String> cmds = new ArrayList<String>();
		cmds.add(TN.survexexecutabledir + "cavern");
		cmds.add("--no-auxiliary-files");
		cmds.add("--quiet"); // or -qq for properly quiet
		cmds.add("-o");
		cmds.add(l3dfile.getPath());
		cmds.add(lsvxfile.getPath());

		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(ldirectory);
		if (!OperateProcess(pb, "cavern"))
			return false;
		if (!l3dfile.exists())
			return false;
		if (lposfile == null)
			return true;

		cmds.clear();
		cmds.add(TN.survexexecutabledir + "3dtopos");
		cmds.add(l3dfile.getPath());
		cmds.add(lposfile.getPath());

		//System.out.println("SVX path: " + tunnelfilelist.activetunnel.svxfile.getPath());
		ProcessBuilder pb3 = new ProcessBuilder(cmds);
		pb3.directory(ldirectory);
		if (!OperateProcess(pb3, "3dtopos"))
			return false;
		if (!lposfile.exists())
			return false;

		return true;
	}

	/////////////////////////////////////////////
	static boolean RunAven(File ldirectory, File l3dfile)
	{
		assert l3dfile != null;
		ProcessBuilder pb = new ProcessBuilder(TN.survexexecutabledir + "aven", l3dfile.getPath());
		pb.directory(ldirectory);
		OperateProcess(pb, "aven");
		return true;
	}



	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static List<String> imagefiledirectories = new ArrayList<String>();
	static void AddImageFileDirectory(String newimagefiledirectory)
	{
		for (String limagefiledirectory : imagefiledirectories)
		{
			if (limagefiledirectory.equals(newimagefiledirectory))
			{
				TN.emitMessage("Already have imagefiledirectory: " + limagefiledirectory); 
				return; 
			}
		}
		imagefiledirectories.add(newimagefiledirectory); 
	}

	// this goes up the directories looking in them for the iname file
	// and for any subdirectories of these matching an element in imagefiledirectories
	static FileAbstraction GetImageFile(FileAbstraction idir, String iname)
	{
		if (iname.startsWith("http:"))
		{
            FileAbstraction res = MakeOpenableFileAbstraction(iname); 
            if (res.localurl != null)
                return res; 
		}
        if (idir == null)
            return null; 

        // get the path from troggle if we can
        if (idir.localurl != null)
        {
            try
            {

            String sname = iname.replace("#", "%23"); 
            sname = sname.replace("http://", ""); 

        	FileAbstraction rread = MakeOpenableFileAbstraction(idir.localurl.toString() + "/backgroundscan/" + sname); 
            TN.emitMessage("---- " + rread.localurl.toString()); 
        	LineInputStream lis = new LineInputStream(rread.GetInputStream(), rread, null, null); 
            lis.FetchNextLine(); 
            if (lis.w[0].equals("imageforward="))
            {
                FileAbstraction res = MakeOpenableFileAbstraction(lis.w[1]); 
                lis.close(); 
                lis.inputstream.close(); 
                return res; 
            }

            System.out.println("::" + lis.GetLine()); 
            while (lis.FetchNextLine())
                System.out.println("::" + lis.GetLine()); 
            lis.inputstream.close(); 
			}

            catch (IOException e) { TN.emitWarning("bbad url " + iname + " " + e.toString());  return null;  }
        }

		// recurse up the file structure
		while (idir != null)
		{
			// check if this image file is in the directory
			FileAbstraction res = FileAbstraction.MakeDirectoryAndFileAbstraction(idir, iname);
			if (res.isFile())
				return res;

			// check if it is in one of the image file subdirectories
			for (int i = imagefiledirectories.size() - 1; i >= 0; i--)
			{
				FileAbstraction lidir = FileAbstraction.MakeDirectoryAndFileAbstraction(idir, imagefiledirectories.get(i));
				if (lidir.isDirectory())
				{
					res = FileAbstraction.MakeDirectoryAndFileAbstraction(lidir, iname);
					if (res.isFile())
						return res;
				}
			}

			// recurse up the hierarchy
			if (idir.localfile == null)
                break; 
            idir = idir.getParentFile();
		}
		return null;
	}


	// we have to decode the file to find something that will satisfy the function above
	static String GetImageFileName(FileAbstraction idir, FileAbstraction ifile) throws IOException
	{
        if (ifile.localurl != null)
        {
            if (idir.localurl == null)
                return ifile.localurl.toString(); 
            System.out.println(ifile.localurl.getHost() + " " + idir.localurl.getHost()); 
            System.out.println("FFF: " + ifile.localurl.getFile()); 
            if (ifile.localurl.getHost().equals(idir.localurl.getHost()))
                return ifile.localurl.getFile();   // just the string part after the host
            return ifile.localurl.toString(); 
        }

		// we need to find a route which takes us here
		String sfiledir = ifile.getParentFile().getCanonicalPath();
		FileAbstraction ridir = FileAbstraction.MakeCanonical(idir);
		while (ridir != null)
		{
			String sdir = ridir.getCanonicalPath();
			if (sfiledir.startsWith(sdir))
			{
				// look through the image file directories to find one that takes us down towards the file
				FileAbstraction lridir = null;
				for (int i = imagefiledirectories.size() - 1; i >= 0; i--) // in reverse so last ones have highest priority
				{
					FileAbstraction llridir = FileAbstraction.MakeDirectoryAndFileAbstraction(ridir, imagefiledirectories.get(i));
					if (llridir.isDirectory())
					{
						String lsdir = llridir.getCanonicalPath();
						if (sfiledir.startsWith(lsdir))
						{
							lridir = llridir;
							break;
						}
					}
				}

				// found an image directory which is part of the stem
				if (lridir != null)
				{
					ridir = lridir;
					break;
				}
			}
			ridir = ridir.getParentFile(); // keep going up.
		}
		if (ridir == null)
		{
			TN.emitWarning("No common stem found");
			System.out.println(idir.getCanonicalPath());
			System.out.println(ifile.getCanonicalPath());
			return null;
		}

		// find the root of which sdir is the stem
		StringBuffer sbres = new StringBuffer();
		FileAbstraction lifile = ifile;
		while (lifile != null)
		{
			if (sbres.length() != 0)
				sbres.insert(0, "/");
			sbres.insert(0, lifile.getName());
			lifile = lifile.getParentFile();
			if ((lifile == null) || lifile.equals(ridir))
				break;
		}


		String sres = sbres.toString();
		TN.emitMessage("Making stem file: " + sres);
		FileAbstraction tifile = GetImageFile(idir, sres);
		if ((tifile != null) && ifile.equals(tifile))
			return sres;

		TN.emitWarning("Stem file failure: " + idir.toString() + "  " + ifile.toString());
		return null;
	}


	/////////////////////////////////////////////
	// goes through files that exist and those that are intended to be saved
	static FileAbstraction GetUniqueSketchFileName(FileAbstraction tundirectory, List<OneSketch> tsketches, String lname)
	{
        if (tundirectory.localurl != null)
        {
            System.out.println(tundirectory.bIsDirType + " " + tundirectory.xfiletype + " " + tundirectory.getPath()); 
// should be save to disk type
return tundirectory; // want to ask the server to give a new one
        }

		if (!tundirectory.bIsDirType)
        	tundirectory = tundirectory.getParentFile(); 

		int sknum = -1; 
		FileAbstraction res;
		while (true)
		{
            res = FileAbstraction.MakeDirectoryAndFileAbstraction(tundirectory, lname + (sknum == -1 ? "" : String.valueOf(sknum)) + ".xml");
			res.xfiletype = FileAbstraction.FA_FILE_XML_SKETCH; 
			sknum = (sknum == -1 ? tsketches.size() + 1 : 1); 
			boolean bexists = res.localfile.exists();
			for (OneSketch tsketch : tsketches)
			{
				if (res.equals(tsketch.sketchfile))
                	bexists = true;
			}
			if (!bexists)
				break;
		}
		return res;
	}

	static FileAbstraction calcIncludeFile(FileAbstraction orgfile, String fname, boolean bPosType) 
	{
		if ((orgfile == null) || ((fname.length() > 3) && (fname.charAt(1) == ':')))
			return FileAbstraction.MakeOpenableFileAbstraction(fname); 

		FileAbstraction cdirectory = orgfile.getParentFile(); 
		String fnamesuff = TN.getSuffix(fname); 
		
		char lsep; 

		// deal with the up directory situation.  
		if (fname.startsWith("..\\") || fname.startsWith("../")) 
		{
			// TN.emitMessage("**" + cdirectory + " + " + fname); 
			lsep = '\\'; 
			while (fname.startsWith("..\\") || fname.startsWith("../")) 
			{
	// this is all a bit junk and should be redone using basic join libraries
				fname = fname.substring(3); 
				int ndr = cdirectory.getAbsolutePath().lastIndexOf(lsep); 
				if (ndr == -1) 
					ndr = cdirectory.getAbsolutePath().lastIndexOf('/'); 

				if (ndr != -1) 
					cdirectory = cdirectory.getParentFile(); 
				else 
				{
					TN.emitWarning("Failed to go up directory:" + lsep + ":" + cdirectory.getName().substring(cdirectory.getName().length() - 4)); 
					char atatat = cdirectory.getName().charAt(cdirectory.getName().length() - 4); 
					TN.emitWarning("hh  " + atatat + " " + (lsep == atatat)); 
				}
			}
TN.emitMessage(" = " + cdirectory + " + " + fname); 
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
			cdirectory = FileAbstraction.MakeDirectoryAndFileAbstractionCI(cdirectory, fname.substring(0, idot)); 
			fname = fname.substring(idot + 1); 
		}

		if (bPosType) 
			fname = fname + TN.SUFF_POS; 
		else if (lsep == '\\') 
			fname = TN.setSuffix(fname, TN.getSuffix(!fnamesuff.equalsIgnoreCase(TN.SUFF_SRV) ? orgfile.getName() : fname)); 
		else
			fname = fname + TN.getSuffix(orgfile.getName()); 

		//TN.emitMessage("include file " + cdirectory + " \\ " + fname); 
		return(FileAbstraction.MakeDirectoryAndFileAbstractionCI(cdirectory, fname)); 
	}
}

