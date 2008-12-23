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

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

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

	// default type, because starting in the static main of MainBox allows us to set to false
	static boolean bIsApplet = true; 

	// the actual
	File localfile;
	URL localurl;
	boolean bIsDirType = false;
	int xfiletype;


	static void InitFA()
	{
		ClassLoader cl = MainBox.class.getClassLoader();
		currentSymbols.localurl = cl.getResource("symbols/");
		if (currentSymbols.localurl == null) 
			currentSymbols.localurl = cl.getResource("symbols/listdir.txt");
		System.out.println("cllll " + FileAbstraction.currentSymbols.localurl); 
		if (!bIsApplet)
			tmpdir = new File(System.getProperty("user.dir"), "tmp"); 
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
		if (xfiletype != FA_FILE_XML_SKETCH)
			TN.emitError("file " + getName() + " has wrong type: " + xfiletype);
		assert xfiletype == FA_FILE_XML_SKETCH;
		String sname = getName();
		assert sname.substring(sname.length() - 4).equalsIgnoreCase(".xml");
		return sname.substring(0, sname.length() - 4);
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
		String parent = localfile.getParent();
		if (parent == null)
			return null;
		return MakeDirectoryFileAbstraction(parent);
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
	BufferedReader GetBufferedReader() throws IOException
	{
		if (localurl != null)
			return new BufferedReader(new InputStreamReader(localurl.openStream())); 
		assert !bIsApplet; 
		return new BufferedReader(new FileReader(localfile));
	}
	
	/////////////////////////////////////////////
	// looks for the object type listed after the tunnelxml
	static char[] filehead = new char[256];
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
			BufferedReader br = GetBufferedReader(); 
			int lfilehead = br.read(filehead, 0, filehead.length);
			br.close();
			if (lfilehead == -1)
{			
System.out.println("****  left file unknown on " + getName()); 
				return FA_FILE_UNKNOWN;
}
			sfilehead = new String(filehead, 0, lfilehead);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
		}
		String strtunnxml = "<tunnelxml>";
		int itunnxml = sfilehead.indexOf(strtunnxml);
		if (itunnxml == -1)
{			
System.out.println("****  missing <tunnelxml> on " + getName()); 
			return FA_FILE_UNKNOWN;
}
		// this should be quitting when it gets to a space or a closing >
		int bracklo = sfilehead.indexOf('<', itunnxml + strtunnxml.length());
		int brackhic = sfilehead.indexOf('>', bracklo + 1);
		int brackhis = sfilehead.indexOf(' ', bracklo + 1);
		int brackhi = (brackhis != -1 ? Math.min(brackhic, brackhis) : brackhic);
		if ((bracklo == -1) || (brackhi == -1))
{			
System.out.println("****  missing bracket on " + getName()); 
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

System.out.println("****  what's sres " + sres + " " + getName()); 
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
System.out.println(" nnnn " + faf.getName() + " " + faf.xfiletype); 
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

	static FileAbstraction currentSymbols = new FileAbstraction();
	static File tmpdir = null;

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
	FileAbstraction SaveAsDialog(boolean bsketchprint, JFrame frame)
	{
		// this == sketchgraphicspanel.sketchdisplay, but for the fact we're in an anonymous event listner
		if (bsketchprint)
		{
			SvxFileDialog sfd = SvxFileDialog.showSaveDialog(this, frame, SvxFileDialog.FT_XMLSKETCH);
			if (sfd == null)
				return null; 
			TN.currentDirectory = sfd.getSelectedFileA(); 
			return sfd.getSelectedFileA();
		}
		else
		{
			SvxFileDialog sfd = SvxFileDialog.showSaveDialog(this, frame, SvxFileDialog.FT_BITMAP);
			if (sfd == null)
				return null;
			TN.currprintdir = sfd.getCurrentDirectoryA();
			return sfd.getSelectedFileA();
		}
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////

	static String boundry = "-----xxxxxxxBOUNDERxxxxdsx";
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
	}

	/////////////////////////////////////////////
    static void writeFile(DataOutputStream out, String name, String fileName, BufferedImage bi) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Type: image/png");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
		ImageIO.write(bi, "png", out);
		out.writeBytes("\r\n");
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public static String postData(String target, String imname, BufferedImage bi)
	{
		try
		{
		System.out.println("About to post\nURL: " + target);
		String response = "";
		URL url = new URL(target);
		URLConnection conn = url.openConnection();

		// Set connection parameters.
		conn.setDoInput (true);
		conn.setDoOutput (true);
		conn.setUseCaches (false);
//		conn.addRequestProperty("well", "shshshsh");
//		System.out.println("jjj\n" + conn.getFileNameMap().getContentTypeFor("hi there.png") + "::::");

		// Make server believe we are form data
//		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Type",
                                "multipart/related; boundary=" + boundry);
//		connection.setRequestProperty("MIME-version", "1.0");

		DataOutputStream out = new DataOutputStream (conn.getOutputStream ());
//		out.write(("--" + boundry + " ").getBytes());
		// Write out the bytes of the content string to the stream.
		writeField(out, "kkj", "eeee");
		writeField(out, "jgt", "sss");
//out.writeBytes("kkk=9&");
	    writeFile(out, "imfile", imname, bi);

//		ImageIO.write(bi, "image/png", out);


//		out.writeBytes(content);
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("--");
		out.writeBytes("\r\n");
		out.flush ();
		out.close ();

		// Read response from the input stream.
		BufferedReader in = new BufferedReader (new InputStreamReader(conn.getInputStream ()));
		String temp;
		while ((temp = in.readLine()) != null)
			response += temp + "\n";
		temp = null;
		in.close ();
		System.out.println("Server response:\n'" + response + "'");
		return response;
		}
		catch (MalformedURLException e)
			{ TN.emitWarning("yyy");}
		catch (IOException e)
			{ TN.emitWarning("eee " + e.toString());};
		return "";
	}

	/////////////////////////////////////////////
	static boolean SurvexExists()
	{
		if (bIsApplet)
			return false; 
		return new File(TN.survexexecutabledir).isDirectory();
	}


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
		String tmpfilename = "tmp_all";
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

		if (sln == null)  // preview aven
		{
			RunAven(tmpdir, l3dfile);
			return true; 
		}
			
		try
		{
			LineInputStream lis = new LineInputStream(MakeWritableFileAbstractionF(lposfile), null, null);
			boolean bres = sln.LoadPosFile(lis, appsketchLocOffset);
			lis.close();
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
	// and for any subdirectories called
	static FileAbstraction GetImageFile(FileAbstraction idir, String iname)
	{
		if (iname.startsWith("http:"))
		{
			FileAbstraction res = new FileAbstraction(); 
			try { res.localurl = new URL(iname); }
				catch (MalformedURLException e) { TN.emitWarning("bad url " + iname);  return null;  }
			return res; 				
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
			idir = idir.getParentFile();
		}
		return null;
	}


	// we have to decode the file to find something that will satisfy the function above
	static String GetImageFileName(FileAbstraction idir, FileAbstraction ifile) throws IOException
	{
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
	static FileAbstraction GetUniqueSketchFileName(FileAbstraction tundirectory, List<OneSketch> tsketches)
	{
		if (!tundirectory.bIsDirType)
			tundirectory = tundirectory.getParentFile(); 
		
		int sknum = tsketches.size();
		FileAbstraction res;
		while (true)
		{
			res = FileAbstraction.MakeDirectoryAndFileAbstraction(tundirectory, "sketch" + sknum + ".xml");
			res.xfiletype = FileAbstraction.FA_FILE_XML_SKETCH; 
			sknum++;
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
			cdirectory = FileAbstraction.MakeDirectoryAndFileAbstraction(cdirectory, fname.substring(0, idot)); 
			fname = fname.substring(idot + 1); 
		}

		if (bPosType) 
			fname = fname + TN.SUFF_POS; 
		else if (lsep == '\\') 
			fname = TN.setSuffix(fname, TN.getSuffix(!fnamesuff.equalsIgnoreCase(TN.SUFF_SRV) ? orgfile.getName() : fname)); 
		else
			fname = fname + TN.getSuffix(orgfile.getName()); 

		//TN.emitMessage("include file " + cdirectory + " \\ " + fname); 
		return(FileAbstraction.MakeDirectoryAndFileAbstraction(cdirectory, fname)); 
	}
}

