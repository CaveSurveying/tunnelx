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

import java.util.List;
import java.util.ArrayList;

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
		tunnel.posfileLocOffset.SetXYZ(0.0F, 0.0F, 0.0F);
		tunnel.vposlegs = new ArrayList<OneLeg>();
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
					OneLeg ol = new OneLeg(lis.w[isecnum + 3], px, py, pz, tunnel, true); 
					tunnel.posfileLocOffset.PlusEquals(ol.m);
					tunnel.vposlegs.add(ol);
				}
				else if (lis.iwc != 0)
				{
					tunnel.AppendLine(";Unknown pos-line: " + lis.GetLine());
					System.out.println("Unknown pos-line missing(: " + lis.GetLine());
				}
			}

			lis.close();
			tunnel.posfileLocOffset.TimesEquals(1.0F / tunnel.vposlegs.size()); 
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}




	/////////////////////////////////////////////
	/////////////////////////////////////////////
	OneSketch LoadSketchFile(OneTunnel tunnel, int isketchfileindex, boolean bwritemessage)
	{
		if (tunnel.tsketches.elementAt(isketchfileindex) instanceof FileAbstraction)
		{
			FileAbstraction tfile = (FileAbstraction)tunnel.tsketches.elementAt(isketchfileindex);
			String fnamess = TN.loseSuffix(tfile.getName());
			txp.SetUp(tunnel, fnamess, FileAbstraction.FA_FILE_XML_SKETCH);
			OneSketch tsketch = new OneSketch(tfile);
			tsketch.bsketchfilechanged = false;

			if (txp.bSymbolType)
			{
				tsketch.bSymbolType = true;
				tsketch.sketchsymbolname = TN.loseSuffix(tfile.getName());
			}

			tunnel.tsketches.setElementAt(tsketch, isketchfileindex);
			txp.tunnelsketch = tsketch;

			if (bwritemessage)
				TN.emitMessage("loading sketch file: " + tsketch.sketchfile.getName());
			tunnXML.ParseFile(txp, tfile);
		}
		return (OneSketch)tunnel.tsketches.elementAt(isketchfileindex);
	}


	/////////////////////////////////////////////
	void LoadFilesRecurse(OneTunnel tunnel) throws IOException
	{
		if (tunnel.svxfile != null)
			LoadSVXdata(tunnel);
		if (tunnel.posfile != null)
			LoadPOSdata(tunnel);
		if (tunnel.exportfile != null)
		{
			txp.SetUp(tunnel, TN.loseSuffix(tunnel.exportfile.getName()), FileAbstraction.FA_FILE_XML_EXPORTS);
			tunnXML.ParseFile(txp, tunnel.exportfile);
		}
		if (tunnel.measurementsfile != null)
		{
			txp.SetUp(tunnel, TN.loseSuffix(tunnel.measurementsfile.getName()), FileAbstraction.FA_FILE_XML_MEASUREMENTS);
			tunnXML.ParseFile(txp, tunnel.measurementsfile);
		}

		// load up the font colours found
		for (FileAbstraction tfile : tunnel.tfontcolours)
		{
			System.out.println("Loading font colours:" + tfile.getName());
			txp.SetUp(tunnel, TN.loseSuffix(tfile.getName()), FileAbstraction.FA_FILE_XML_FONTCOLOURS);
			tunnXML.ParseFile(txp, tfile);
		}

		// do all the subtunnels
		for (int i = 0; i < tunnel.ndowntunnels; i++)
			LoadFilesRecurse(tunnel.downtunnels[i]);
		if (!tunnel.posfileLocOffset.isZero())
			System.out.println(tunnel.posfileLocOffset + "LocOffset " + tunnel.name);  
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public TunnelLoader(OneTunnel vgsymbols, SketchLineStyle sketchlinestyle)
	{
		txp = new TunnelXMLparse(vgsymbols);
		txp.bSymbolType = (vgsymbols == null);
		txp.sketchlinestyle = sketchlinestyle; // for loading up the fontcolours
		tunnXML = new TunnelXML();
	}

};
