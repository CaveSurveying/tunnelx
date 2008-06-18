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
	/////////////////////////////////////////////
	void LoadSketchFile(OneSketch tsketch, boolean bwritemessage)
	{
		assert !tsketch.bsketchfileloaded;

		tsketch.SetupSK();
		FileAbstraction tfile = tsketch.sketchfile;
		String fnamess = TN.loseSuffix(tfile.getName());
		txp.SetUp(fnamess, FileAbstraction.FA_FILE_XML_SKETCH);
		tsketch.bsketchfilechanged = false;
		if (txp.bSymbolType)
		{
			tsketch.bSymbolType = true;
			tsketch.sketchsymbolname = TN.loseSuffix(tfile.getName());
		}

		txp.tunnelsketch = tsketch;
		if (bwritemessage)
			TN.emitMessage("loading sketch file: " + tsketch.sketchfile.getName());
		tunnXML.ParseFile(txp, tfile);
	}


	/////////////////////////////////////////////
	void ReloadFontcolours(OneTunnel tunnel, int activesketchindex)
	{
		try
		{
			FileAbstraction tfile = tunnel.tfontcolours.get(activesketchindex);
			System.out.println("RE-Loading font colours:" + tfile.getName());
			txp.SetUp(TN.loseSuffix(tfile.getName()), FileAbstraction.FA_FILE_XML_FONTCOLOURS);
			tunnXML.ParseFile(txp, tfile);
		}
		catch (NullPointerException e)
		{
			TN.emitWarning(e.toString());
			e.printStackTrace();
		};
	}


	/////////////////////////////////////////////
	void LoadFontcolours(List<FileAbstraction> tfontcolours) throws IOException
	{
		// load up the font colours found
		for (FileAbstraction tfile : tfontcolours)
		{
			System.out.println("Loading font colours:" + tfile.getName());
			txp.SetUp(TN.loseSuffix(tfile.getName()), FileAbstraction.FA_FILE_XML_FONTCOLOURS);
			tunnXML.ParseFile(txp, tfile);
		}
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


