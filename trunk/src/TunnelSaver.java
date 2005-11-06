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

//
//
// TunnelSaver
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
class TunnelSaver
{
	/////////////////////////////////////////////
	static void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess); 
		throw e; 
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static void Savesvxfile(OneTunnel tunnel) throws IOException
	{
		LineOutputStream los = new LineOutputStream(tunnel.svxfile);  
		LineInputStream lis = new LineInputStream(tunnel.getTextData(), null); 

		// bracket with *begin and end
		los.WriteLine("*begin " + tunnel.name); 

		while (lis.FetchNextLine())
			los.WriteLine(lis.GetLine()); 

		// put out the *includes 
		for (int i = 0; i < tunnel.ndowntunnels; i++) 
			los.WriteLine("*include " + tunnel.downtunnels[i].name + "/" + tunnel.downtunnels[i].name + ".svx"); 
		los.WriteLine(""); 

		los.WriteLine("*end " + tunnel.name); 

		lis.close(); 
		los.close(); 
	}

	/////////////////////////////////////////////
	static void SaveExportsFile(OneTunnel tunnel) throws IOException  
	{
		LineOutputStream los = new LineOutputStream(tunnel.exportfile);  
		los.WriteLine(TNXML.sHEADER); 
		los.WriteLine(""); 

		los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML)); 
	
		los.WriteLine(TNXML.xcomopen(0, TNXML.sEXPORTS)); 
		for (int i = 0; i < tunnel.vexports.size(); i++)  
			((OneExport)tunnel.vexports.elementAt(i)).WriteXML(los); 
		los.WriteLine(TNXML.xcomclose(0, TNXML.sEXPORTS)); 

		los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML)); 

		los.close(); 
	}

	/////////////////////////////////////////////
	static void Savexmllegs(OneTunnel tunnel) throws IOException
	{
		LineOutputStream los = new LineOutputStream(tunnel.measurementsfile);  
		los.WriteLine(TNXML.sHEADER); 
		los.WriteLine(""); 

		los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML)); 
		tunnel.WriteXML(los); 
		los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML)); 

		los.close();
	}

	/////////////////////////////////////////////
	static void SaveSketches(OneTunnel tunnel) throws IOException
	{
		for (int i = 0; i < tunnel.tsketches.size(); i++)
		{
			if (tunnel.tsketches.elementAt(i) instanceof FileAbstraction)
				continue;
			OneSketch lsketch = (OneSketch)tunnel.tsketches.elementAt(i);
			if (lsketch.bsketchfilechanged)
			{
				LineOutputStream los = new LineOutputStream(lsketch.sketchfile);
				los.WriteLine(TNXML.sHEADER);
				los.WriteLine("");

				los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML));
				lsketch.WriteXML(los);
				los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML));

				los.close();

				lsketch.bsketchfilechanged = false;
			}
		}
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
				TunnelLoader.FindFilesOfDirectory(tunnel);
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


	/////////////////////////////////////////////
	static void SaveFilesRecurse(OneTunnel tunnel) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length; // hack this to make sure the &space; will get in.

		if (tunnel.tundirectory.isFile())
			emitError("directory name is file " + tunnel.tundirectory.toString(), new IOException());
		if (!tunnel.tundirectory.isDirectory())
		{
			if (!tunnel.tundirectory.mkdirs())
				emitError("cannot mkdirs on " + tunnel.tundirectory.toString(), new IOException());
			TN.emitMessage("Creating directory " + tunnel.tundirectory.toString());
		}


		if (tunnel.bsvxfilechanged)
		{
			Savesvxfile(tunnel);
			tunnel.bsvxfilechanged = false;
		}
		if (tunnel.bmeasurementsfilechanged)
		{
			Savexmllegs(tunnel);
			tunnel.bmeasurementsfilechanged = false;
		}
		if (tunnel.bexportfilechanged)
		{
			SaveExportsFile(tunnel);
			tunnel.bexportfilechanged = false;
		}
		SaveSketches(tunnel);

		// work with all the downtunnels
		for (int i = 0; i < tunnel.ndowntunnels; i++)
			SaveFilesRecurse(tunnel.downtunnels[i]);
	}

	/////////////////////////////////////////////
	static void SaveFilesRoot(OneTunnel tunnel, boolean bSketchesOnly)
	{
		// check that saved directory is good.
		try
		{
			if (bSketchesOnly)
				SaveSketches(tunnel);
			else
				SaveFilesRecurse(tunnel);
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}
};
