////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
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
	static final int FT_TUBE = 2; 
	static final int FT_SVXTUBETUN = 3; 
	static final int FT_TUN = 4; 
	static final int FT_XSECTION_PREVIEW = 5; 
	static final int FT_SYMBOLS = 6; 
	static final int FT_VRML = 7; 
	static final int FT_BITMAP = 8; 
	static final int FT_DIRECTORY = 9; 

	static String[] ftnames = { "Any",  
								"SVX", 
								"TUBE", 
								"SVX/TOP/PRJ/TUBE/TUN", 
								"TUN", 
								"XSection Preview", 
								"Symbols", 
								"VRML", 
								"Bitmap", 
								"Directory" }; 

	static String[][] ftexts = { { "*" }, 
								 { "svx" }, 
								 { "tube" }, 
								 { "svx", "top", "prj", "tube", "tun" }, 
								 { "tun" }, 
								 { "tun" }, 
								 { "tun" }, 
								 { "wrl" }, 
								 { "bmp", "gif", "jpg" }, 
								 { "??" } }; 

	File svxfile = null; 
	File tubefile = null;  
	File tunneldirectory = null; 

	boolean bReadCommentedXSections; 
	boolean bIsTunFile; 

	

	/////////////////////////////////////////////
	SvxFileDialog(File currentDirectory, int ftype) 
	{
		super(currentDirectory); 
		if (!currentDirectory.getName().equals("")) 
			setSelectedFile(currentDirectory); 
		SvxFileFilter sff = (ftype != FT_DIRECTORY ? new SvxFileFilter(ftnames[ftype], ftexts[ftype]) : new SvxFileFilter(ftnames[ftype])); 

		addChoosableFileFilter(sff); 
		setFileFilter(sff); 
	}

	/////////////////////////////////////////////
	static SvxFileDialog showOpenDialog(File currentDirectory, JFrame frame, int ftype, boolean bAuto) 
	{
		String lsuff = TN.getSuffix(currentDirectory.getName()); 
		boolean bBlankFile = (!lsuff.equalsIgnoreCase(TN.SUFF_SVX) && !lsuff.equalsIgnoreCase(TN.SUFF_TUN)  && !lsuff.equalsIgnoreCase(TN.SUFF_TUBE) && !currentDirectory.getName().equals("")); 
		SvxFileDialog sfd = new SvxFileDialog((bBlankFile ? currentDirectory.getParentFile() : currentDirectory), ftype);  

		sfd.svxfile = null; 
		sfd.tubefile = null; 
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
		sfd.bIsTunFile = suff.equalsIgnoreCase(TN.SUFF_TUN); 

		if (ftype == FT_BITMAP) 
		{
			sfd.svxfile = file; 
			return sfd; 
		}


		if (suff.equalsIgnoreCase(TN.SUFF_SVX) || suff.equalsIgnoreCase(TN.SUFF_TUN) || suff.equalsIgnoreCase(TN.SUFF_TOP) || suff.equalsIgnoreCase(TN.SUFF_WALLS)) 
		{
			sfd.svxfile = file; 
			sfd.tubefile = null; 
			return sfd; 
		}
		else if (suff.equalsIgnoreCase(TN.SUFF_TUBE)) 
		{
			sfd.svxfile = new File(file.getParent(), TN.setSuffix(file.getName(), TN.SUFF_SVX)); 
			sfd.tubefile = file; 

			// alternatively try the toporobot style.  
			if (!sfd.svxfile.isFile()) 
			{
				sfd.svxfile = new File(file.getParent(), TN.setSuffix(file.getName(), TN.SUFF_TOP)); 

				// alternatively try the walls style.  
				if (!sfd.svxfile.isFile()) 
				{
					sfd.svxfile = new File(file.getParent(), TN.setSuffix(file.getName(), TN.SUFF_WALLS)); 
					if (!sfd.svxfile.isFile()) 
						return null; 
				}
			}

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

		SvxFileDialog sfd = new SvxFileDialog(savetype, ftype);  
		sfd.svxfile = null; 
		sfd.tubefile = null; 
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
				System.out.println("wrong suffix for SVX file"); 
			else
				sfd.svxfile = file; 
			break; 

		case FT_TUBE: 
			if (!suff.equalsIgnoreCase(TN.SUFF_TUBE)) 
				System.out.println("wrong suffix for TUBE file"); 
			else
				sfd.tubefile = file; 
			break; 

		case FT_TUN: 
		case FT_XSECTION_PREVIEW: 
		case FT_SYMBOLS: 
			if (!suff.equalsIgnoreCase(TN.SUFF_TUN)) 
				System.out.println("wrong suffix for TUN file"); 
			else
				sfd.svxfile = file; 
			break; 


		case FT_VRML: 
			if (!suff.equalsIgnoreCase(TN.SUFF_VRML)) 
				System.out.println("wrong suffix for WRML file"); 
			else
				sfd.svxfile = file; 
			break; 

		case FT_DIRECTORY: 
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
