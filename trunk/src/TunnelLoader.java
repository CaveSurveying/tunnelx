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
import java.io.IOException;

import java.util.Vector;

//
//
// TunnelLoader
//
//


/////////////////////////////////////////////
/////////////////////////////////////////////
class TunnelLoader
{
	TunnelXMLparse txp;
	TunnelXML tunnXML;
	OneTunnel vgsymbols;

	/////////////////////////////////////////////
	void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess);
		throw e;
	}


	/////////////////////////////////////////////
	void LoadSVXdata(OneTunnel tunnel)
	{
		try
		{
			LineInputStream lis = new LineInputStream(tunnel.svxfile, null, null);

			// strip the *begins and *includes
			while (lis.FetchNextLine())
			{
				if (lis.w[0].equalsIgnoreCase("*begin"))
					;
				else if (lis.w[0].equalsIgnoreCase("*end"))
					;
				else if (lis.w[0].equalsIgnoreCase("*include"))
					;
				else
					tunnel.AppendLine(lis.GetLine());
			}

			lis.close();
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}


	/////////////////////////////////////////////
	// this rearranges the line into a svx command.
	void LoadPOSdata(OneTunnel tunnel)
	{
		tunnel.vposlegs = new Vector();
		try
		{
			LineInputStream lis = new LineInputStream(tunnel.posfile, null, null);
			while (lis.FetchNextLine())
			{
				// this is a rather poor attempt at dealing with the 
				// cases of long numbers not leaving a space between 
				// the parenthesis and the first number.  
				if (lis.w[0].startsWith("(")) 
				{
					if ((lis.iwc == 5) && lis.w[1].equals("Easting") && lis.w[2].equals("Northing") && lis.w[3].equals("Altitude")) 
						continue; 
					int isecnum = 2; 
					String sfirstnum = lis.w[1]; 
					if ((lis.iwc == 5) && !lis.w[0].equals("(")) 
					{
						sfirstnum = lis.w[0].substring(1); 
						isecnum = 1; 
					}
					if (isecnum + 4 != lis.iwc) 
					{
						System.out.println("Unknown pos-line: " + lis.GetLine()); 
						continue; 
					}
					float px =  Float.valueOf(sfirstnum).floatValue();
					float py =  Float.valueOf(lis.w[isecnum]).floatValue();
					float pz =  Float.valueOf(lis.w[isecnum + 1]).floatValue();
					tunnel.vposlegs.addElement(new OneLeg(lis.w[isecnum + 3], px, py, pz, tunnel, true)); 
				}
				else if (lis.iwc != 0)
				{
					tunnel.AppendLine(";Unknown pos-line: " + lis.GetLine());
					System.out.println("Unknown pos-line missing(: " + lis.GetLine());
				}
			}

			lis.close();
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	boolean LoadDirectoryRecurse(OneTunnel tunnel, File loaddirectory) throws IOException
	{
		tunnel.tundirectory = loaddirectory;

		//TN.emitMessage("Dir " + loaddirectory.getName());
		if (!loaddirectory.isDirectory())
			emitError("file not a directory " + loaddirectory.toString(), new IOException());

		boolean bsomethinghere = false;
		File[] sfiles = loaddirectory.listFiles();

		// here we begin to open XML readers and such like, filling in the different slots.
		for (int i = 0; i < sfiles.length; i++)
		{
			if (sfiles[i].isFile())
			{
				String suff = TN.getSuffix(sfiles[i].getName());
				if (suff.equals(TN.SUFF_XML))
				{
					//TN.emitMessage("parsing " + sfiles[i].getName());
					txp.SetUp(tunnel, TN.loseSuffix(sfiles[i].getName()));
					tunnXML.ParseFile(txp, sfiles[i]);

					// fill in the file positions according to what was in this file.
					if (txp.bContainsExports)
					{
						tunnel.exportfile = sfiles[i];
						tunnel.bexportfilechanged = false;
					}
					else if (txp.bContainsMeasurements)
					{
						tunnel.xmlfile = sfiles[i];
						tunnel.bxmlfilechanged = false;
					}
					else if (txp.nsketches == 1)
					{
						OneSketch sketch = (OneSketch)tunnel.tsketches.lastElement();
						sketch.sketchfile = sfiles[i];
						sketch.bsketchfilechanged = false;
					}

					bsomethinghere = true;
				}

				else if (suff.equals(TN.SUFF_SVX))
				{
					if (tunnel.svxfile != null)
						TN.emitError("two svx files in same directory");
					tunnel.svxfile = sfiles[i];
					tunnel.bsvxfilechanged = false;
					LoadSVXdata(tunnel);

					bsomethinghere = true;
				}
				else if (suff.equals(TN.SUFF_POS))
				{
					if (tunnel.posfile != null)
						TN.emitError("two svx files in same directory");
					tunnel.posfile = sfiles[i];
					LoadPOSdata(tunnel);
				}

				else if (suff.equals(TN.SUFF_PNG) || suff.equalsIgnoreCase(TN.SUFF_GIF) || suff.equalsIgnoreCase(TN.SUFF_JPG))
					tunnel.imgfiles.addElement(sfiles[i]);
				else if (suff.equalsIgnoreCase(TN.SUFF_TXT))
					;
				else
				{
					int j = TN.SUFF_IGNORE.length;
					while (--j >= 0)
						if (suff.equalsIgnoreCase(TN.SUFF_IGNORE[j]))
							break;
					if (j == -1)
						TN.emitMessage("Unknown file type " + sfiles[i].getName());
				}
			}
		}


		// get the subdirectories and recurse.
		for (int i = 0; i < sfiles.length; i++)
		{
			if (sfiles[i].isDirectory())
			{
				String dtname = sfiles[i].getName();
				OneTunnel dtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(dtname, null));

				if (!LoadDirectoryRecurse(dtunnel, sfiles[i]))
					tunnel.ndowntunnels--; // if there's nothing interesting, take this introducedd tunnel back out!
				else
					bsomethinghere = true;
			}
		}
		return bsomethinghere;
	}

	/////////////////////////////////////////////
	public TunnelLoader(OneTunnel filetunnel, File loaddirectory, OneTunnel vgsymbols)
	{
		txp = new TunnelXMLparse(vgsymbols);
		txp.bSymbolType = (vgsymbols == null);
		tunnXML = new TunnelXML();

		// check that saved directory is good.
		try
		{
			// create the directory tree
			LoadDirectoryRecurse(filetunnel, loaddirectory);
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
			ie.printStackTrace();
		}
		catch (NullPointerException e)
		{
			TN.emitWarning(e.toString());
			e.printStackTrace();
		};
	}
};
