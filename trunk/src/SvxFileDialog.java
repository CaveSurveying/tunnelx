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
	static final int FT_DIRECTORY = 6; 

	static String[] ftnames = { "Any",  
								"SVX/TOP/PRJ", 
								"XSection Preview", 
								"Symbols", 
								"VRML", 
								"Bitmap", 
								"Directory" }; 

	static String[][] ftexts = { { "*" }, 
								 { "svx", "top", "prj" }, 
								 { "??" }, 
								 { "??" }, 
								 { "wrl" }, 
								 { "bmp", "gif", "jpg" }, 
								 { "??" } }; 

	File svxfile = null; 
	File tunneldirectory = null; 

	boolean bReadCommentedXSections; 

	

	/////////////////////////////////////////////
	SvxFileDialog(File currentDirectory) 
	{
		super(currentDirectory); 
		if (!currentDirectory.getName().equals("")) 
			setSelectedFile(currentDirectory); 
	}

	/////////////////////////////////////////////
	void SetFileFil(int ftype)  
	{
		SvxFileFilter sff = (ftype != FT_DIRECTORY ? new SvxFileFilter(ftnames[ftype], ftexts[ftype]) : new SvxFileFilter(ftnames[ftype])); 
		TN.emitMessage(sff.getDescription()); 
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
	static SvxFileDialog showOpenDialog(File currentDirectory, JFrame frame, int ftype, boolean bAuto) 
	{
		String lsuff = TN.getSuffix(currentDirectory.getName()); 
		boolean bBlankFile = (!lsuff.equalsIgnoreCase(TN.SUFF_SVX) && !currentDirectory.getName().equals("")); 

		SvxFileDialog sfd = new SvxFileDialog((bBlankFile ? currentDirectory.getParentFile() : currentDirectory));  
		sfd.SetFileFil(ftype); 

		sfd.svxfile = null; 
		sfd.tunneldirectory	= null; 

		sfd.setDialogTitle("Open " + ftnames[ftype] + "File"); 
		sfd.setFileSelectionMode(ftype != FT_DIRECTORY ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY); 

	    File file = sfd.getSelectedFile(); 
		if (!bAuto) 
		{
			if (sfd.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) 
				return null; 
		    file = sfd.getSelectedFile(); 
		}
		else 
			file = currentDirectory; 


		// directory type  
		TN.emitMessage("ft " + ftype + " " + FT_DIRECTORY); 
		if (ftype == FT_DIRECTORY)  
		{
			if (!file.isDirectory()) 
				return null; 
			sfd.tunneldirectory = file; 
			return sfd; 
		}

		// get rid of directories  
		if (file.isDirectory()) 
			return null; 

		String suff = TN.getSuffix(file.getName()); 
		sfd.bReadCommentedXSections = (suff.equalsIgnoreCase(TN.SUFF_SVX) || suff.equalsIgnoreCase(TN.SUFF_TOP)); 

		if (ftype == FT_BITMAP) 
		{
			sfd.svxfile = file; 
			return sfd; 
		}
		if (suff.equalsIgnoreCase(TN.SUFF_SVX) || suff.equalsIgnoreCase(TN.SUFF_TOP) || suff.equalsIgnoreCase(TN.SUFF_WALLS))  
		{
			sfd.svxfile = file; 
			return sfd; 
		}
		else 
			JOptionPane.showMessageDialog(frame, "Unknown File Type");  
		return null; 
	}

	/////////////////////////////////////////////
	static SvxFileDialog showSaveDialog(File currentDirectory, JFrame frame, int ftype) 
	{ 
		File savetype = (currentDirectory.getName().equals("") ? currentDirectory : new File(currentDirectory.getParent(), TN.setSuffix(currentDirectory.getName(), "." + ftexts[ftype][0]))); 

		SvxFileDialog sfd = new SvxFileDialog(savetype);  
		sfd.SetFileFil(ftype); 

		sfd.svxfile = null; 
		sfd.tunneldirectory	= null; 

		sfd.setDialogTitle("Save " + ftnames[ftype] + "File"); 
		sfd.setFileSelectionMode(ftype != FT_DIRECTORY ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY); 
 
		if (sfd.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) 
			return null; 

	    File file = sfd.getSelectedFile();
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

		case FT_XSECTION_PREVIEW: 
		case FT_DIRECTORY: 
		case FT_SYMBOLS: 
			if (file.isFile()) 
				return null; 
			sfd.tunneldirectory = file; 
			break; 

		default: 
			TN.emitProgError("Unrecognized file type"); 
			break; 
		}
		return sfd; 
	}
}
