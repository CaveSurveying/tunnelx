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

import javax.swing.JFrame;
import javax.swing.filechooser.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import javax.swing.JOptionPane;
import javax.swing.JApplet;


/////////////////////////////////////////////
class SvxFileFilter extends FileFilter
{
	StringBuffer desc = new StringBuffer();
	String[] ftext;

	/////////////////////////////////////////////
	SvxFileFilter(String ftname, String[] lftext)
	{
		ftext = lftext;

		// make the description
		desc.append(ftname);
		desc.append(" Files (");
		for (int i = 0; i < ftext.length; i++)
		{
			if (i != 0)
				desc.append(", ");
			desc.append("*.");
			desc.append(ftext[i]);
		}
		desc.append(")");
	}

	/////////////////////////////////////////////
	SvxFileFilter(String namedirectory)
	{
		desc.append(namedirectory);
		ftext = null;
	}

	/////////////////////////////////////////////
	public boolean accept(File file)
	{
		if (file.isDirectory())
			return true;
		if (ftext == null)
			return false;

		String suff = TN.getSuffix(file.getName()); // need a coherent interface for this function.
		if ((suff == null) || suff.equals("") || (suff.charAt(0) != '.'))
			return false;
		suff = suff.substring(1);

		for (int i = 0; i < ftext.length; i++)
			if (suff.equalsIgnoreCase(ftext[i]))
				return true;
		return false;
	}

	/////////////////////////////////////////////
	public String getDescription()
	{
		return desc.toString();
	}
};



/////////////////////////////////////////////
/////////////////////////////////////////////
// the loading dialog
public class SvxFileDialog extends JFileChooser
{
	static final int FT_ANY = 0;
	static final int FT_SVX = 1;
	static final int FT_XSECTION_PREVIEW = 2;
	static final int FT_SYMBOLS = 3;
	static final int FT_VRML = 4;
	static final int FT_BITMAP = 5;
	static final int FT_TH2 = 6;
	static final int FT_XMLSKETCH = 7;
	static final int FT_DIRECTORY = 8;

	static String[] ftnames = {	"Any",
								"SVX/TOP/PRJ",
								"XSection Preview",
								"Symbols",
								"VRML",
								"Bitmap",
								"Therion sketch",
								"Tunnel sketch",
								"Directory" };

	static String[][] ftexts = { { "*" },
								 { "svx", "top", "prj" },
								 { "??" },
								 { "??" },
								 { "wrl" },
								 { "png", "jpg", "bmp", "gif" },
								 { "th2" },
								 { "xml" },
								 { "??" } };

	FileAbstraction svxfile = null;
	FileAbstraction tunneldirectory = null;

	boolean bReadCommentedXSections;



	/////////////////////////////////////////////
	SvxFileDialog(FileAbstraction currentDirectory)
	{
		super(currentDirectory.localfile);
		if (!currentDirectory.getName().equals(""))
			setSelectedFile(currentDirectory.localfile);  // filechooser function
	}
	/////////////////////////////////////////////
	FileAbstraction getCurrentDirectoryA()
	{
		return FileAbstraction.MakeDirectoryFileAbstractionF(getCurrentDirectory()); 
	}
	
	/////////////////////////////////////////////
	FileAbstraction getSelectedFileA(int ftype)
	{
        String fsel = getSelectedFile().toString();

        // the dialog box removes necessary trailing slashes when we abuse it to enter in URLs
        if ((ftype == FT_DIRECTORY) && !fsel.endsWith("/"))
            fsel = fsel + "/"; 
        return FileAbstraction.MakeOpenableFileAbstraction(fsel); // doesn't set the xfiletype
	}
	FileAbstraction getSelectedFileA()
	   { return getSelectedFileA(FT_ANY); }
	

	/////////////////////////////////////////////
	void SetFileFil(int ftype)
	{
		SvxFileFilter sff = (ftype != FT_DIRECTORY ? new SvxFileFilter(ftnames[ftype], ftexts[ftype]) : new SvxFileFilter(ftnames[ftype]));
		//TN.emitMessage(sff.getDescription());
		try
		{
			addChoosableFileFilter(sff);
			setFileFilter(sff);
		}
		catch (NullPointerException e)
		{
			TN.emitWarning(e.toString());
		}
	}

	/////////////////////////////////////////////
	static SvxFileDialog showOpenDialog(FileAbstraction currentDirectory, JApplet frame, int ftype, boolean bAuto)
	{
		System.out.println("help");
		return null;
	}

	/////////////////////////////////////////////
	static SvxFileDialog showOpenDialog(FileAbstraction currentDirectory, JFrame frame, int ftype, boolean bAuto)
	{
		// weird getting the suffix off a directory?
		// maybe something's bee posted into it
		String lsuff = TN.getSuffix(currentDirectory.getName());
		boolean bBlankFile = (!lsuff.equalsIgnoreCase(TN.SUFF_SVX) && !currentDirectory.getName().equals(""));

		//SvxFileDialog sfd = new SvxFileDialog((bBlankFile ? currentDirectory.getParentFile() : currentDirectory));
		SvxFileDialog sfd = new SvxFileDialog(currentDirectory);
		sfd.SetFileFil(ftype);

		sfd.svxfile = null;
		sfd.tunneldirectory	= null;

		sfd.setDialogTitle("Open " + ftnames[ftype] + "File");
		sfd.setFileSelectionMode(ftype != FT_DIRECTORY ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);

	    FileAbstraction file; 
		if (!bAuto)
		{
			if (sfd.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
				return null;
		    file = sfd.getSelectedFileA(ftype);
		}
		else
			file = currentDirectory;


		// directory type
		//TN.emitMessage("ft " + ftype + " " + FT_DIRECTORY);
		if (ftype == FT_DIRECTORY)
		{
			if (!file.isDirectory())
				return null;
			sfd.tunneldirectory = file;
			sfd.tunneldirectory.xfiletype = FileAbstraction.FA_DIRECTORY; 
            sfd.tunneldirectory.bIsDirType = true; 
            return sfd;
		}

		// get rid of directories
		if ((file.localurl == null) && file.isDirectory())
			return null;

		String suff = TN.getSuffix(file.getName());
		sfd.bReadCommentedXSections = (suff.equalsIgnoreCase(TN.SUFF_SVX) || suff.equalsIgnoreCase(TN.SUFF_TOP));

		if ((ftype == FT_TH2) || suff.equalsIgnoreCase(TN.SUFF_TOP) || suff.equalsIgnoreCase(TN.SUFF_WALLS))
		{
			sfd.svxfile = file;
            return sfd;
		}
		if (ftype == FT_BITMAP)
		{
			sfd.svxfile = file;
			sfd.svxfile.xfiletype = FileAbstraction.FA_FILE_IMAGE; 
            return sfd;
		}
		if (suff.equalsIgnoreCase(TN.SUFF_SVX))
		{
			sfd.svxfile = file;
			sfd.svxfile.xfiletype = FileAbstraction.FA_FILE_SVX; 
			return sfd;
		}
		if (suff.equalsIgnoreCase(TN.SUFF_XML) && (ftype == FT_XMLSKETCH))
		{
			sfd.svxfile = file;
            //sfd.svxfile.xfiletype = FileAbstraction.FA_FILE_XML_SKETCH;  (look it up?)
			return sfd;
		}
		else
		{
			TN.emitWarning("Unknown File Type on:" + file.getName());
			JOptionPane.showMessageDialog(frame, "Unknown File Type on:" + file.getName());
		}
		return null;
	}

	/////////////////////////////////////////////
	static SvxFileDialog showSaveDialog(FileAbstraction currentDirectory, JApplet frame, int ftype)
	{
		return null;
	}

	/////////////////////////////////////////////
	static SvxFileDialog showSaveDialog(FileAbstraction currentDirectory, JFrame frame, int ftype)
	{
		FileAbstraction savetype = null; 
        if (currentDirectory.localurl != null)
            currentDirectory = TN.currentDirectory; 
        else if (currentDirectory.getName().equals(""))
            currentDirectory = currentDirectory; 
		else 
			currentDirectory = FileAbstraction.MakeDirectoryAndFileAbstraction(currentDirectory.getParentFile(), TN.setSuffix(currentDirectory.getName(), "." + ftexts[ftype][0])); 

		SvxFileDialog sfd = new SvxFileDialog(currentDirectory);
		sfd.SetFileFil(ftype);

		sfd.svxfile = null;
		sfd.tunneldirectory	= null;

		sfd.setDialogTitle("Save " + ftnames[ftype] + "File");
		sfd.setFileSelectionMode(ftype != FT_DIRECTORY ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);

		if (sfd.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
			return null;

	    FileAbstraction file = sfd.getSelectedFileA(ftype);
		String suff = TN.getSuffix(file.getName());
		switch (ftype)
		{
		case FT_SVX:
			if (!suff.equalsIgnoreCase(TN.SUFF_SVX))
				TN.emitWarning("wrong suffix for SVX file");
			else
				sfd.svxfile = file;
			break;

		case FT_VRML:
			if (!suff.equalsIgnoreCase(TN.SUFF_VRML))
				TN.emitWarning("wrong suffix for WRML file");
			else
				sfd.svxfile = file;
			break;

		case FT_XMLSKETCH:
			if (!suff.equalsIgnoreCase(TN.SUFF_XML))
				TN.emitWarning("wrong suffix for XML file");
			else
				sfd.svxfile = file;
			break;

		case FT_XSECTION_PREVIEW:
		case FT_DIRECTORY:
		case FT_SYMBOLS:
			if (file.isFile())
				return null;
			sfd.tunneldirectory = file;
			break;

		case FT_BITMAP:
			sfd.svxfile = file;
			break;

		default:
			TN.emitProgError("Unrecognized file type");
			break;
		}
		return sfd;
	}
}
