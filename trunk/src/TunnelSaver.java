////////////////////////////////////////////////////////////////////////////////
// Tunnel  Julian Todd 2001.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.File; 
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
	void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess); 
		throw e; 
	}

	/////////////////////////////////////////////
	void CreateDirectoryRecurse(OneTunnel tunnel, File savedirectory) throws IOException
	{
		if (savedirectory.isFile())  
			emitError("directory name is file " + savedirectory.toString(), new IOException()); 

		// make and save copies of current directory.  
		if (!savedirectory.isDirectory()) 
		{
			if (!savedirectory.mkdirs())  
				emitError("cannot mkdirs on " + savedirectory.toString(), new IOException()); 
			tunnel.tunneldirectory = savedirectory; 
			tunnel.btunneldirectorynew = true; 
			TN.emitMessage("Creating directory " + savedirectory.toString()); 
		}
		else 
		{
			if ((tunnel.tunneldirectory == null) || !tunnel.tunneldirectory.equals(savedirectory))   
			{
				tunnel.tunneldirectory = savedirectory; 
				tunnel.btunneldirectorynew = true; 
				TN.emitMessage("Changing to directory " + savedirectory.toString()); 
			}
			else 
				tunnel.btunneldirectorynew = false; 
		}

		// work with all the downtunnels  
		for (int i = 0; i < tunnel.ndowntunnels; i++)  
		{
			File downdirectory = new File(savedirectory, tunnel.downtunnels[i].name); 
			if (downdirectory.equals(savedirectory))  
				emitError("Not new directory " + downdirectory.toString(), new IOException()); 
			CreateDirectoryRecurse(tunnel.downtunnels[i], downdirectory); 
		}


		// generate the files in this directory.  
		if (tunnel.btunneldirectorynew || (tunnel.svxfile == null))  
		{
			tunnel.svxfilename = tunnel.name + ".svx"; 
			tunnel.svxfile = new File(tunnel.tunneldirectory, tunnel.svxfilename); 
		}

		if (true) // survex file changed.  
		{
			LineOutputStream los = new LineOutputStream(tunnel.svxfile);  
			LineInputStream lis = new LineInputStream(tunnel.getTextData()); 

			// write the includes for the lower tunnels so that this setup still loads into survex.  
			while (lis.FetchNextLine())
				los.WriteLine(lis.GetLine()); 

			lis.close(); 
			los.close(); 
		}


		// generate the files of exports  
		if (tunnel.btunneldirectorynew || (tunnel.exportfilename == null))  
		{
			tunnel.exportfilename = tunnel.name + "-exports" + TN.SUFF_XML; 
			tunnel.exportfile = new File(tunnel.tunneldirectory, tunnel.exportfilename); 
		}

		if (true) // exports need outputting
		{
			LineOutputStream los = new LineOutputStream(tunnel.exportfile);  
			los.WriteLine(TNXML.sHEADER); 
			los.WriteLine(""); 

			los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML)); 
			for (int i = 0; i < tunnel.vexports.size(); i++)  
				((OneExport)tunnel.vexports.elementAt(i)).WriteXML(los); 
			los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML)); 

			los.close(); 
		}

		
		// generate the xml file from the svx  
		if (tunnel.btunneldirectorynew || (tunnel.xmlfilename == null))  
		{
			tunnel.xmlfilename = tunnel.name + TN.SUFF_XML; 
			tunnel.xmlfile = new File(tunnel.tunneldirectory, tunnel.xmlfilename); 
		}

		if (true) // xml interpretation needs outputting
		{
			LineOutputStream los = new LineOutputStream(tunnel.xmlfile);  
			los.WriteLine(TNXML.sHEADER); 
			los.WriteLine(""); 

			los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML)); 
			tunnel.WriteXML(los); 
			los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML)); 

			los.close(); 
		}

		
		// generate sketches xml file 
		if (tunnel.btunneldirectorynew || (tunnel.sketchfilename == null))  
		{
			tunnel.sketchfilename = tunnel.name + "-sketch" + TN.SUFF_XML; 
			tunnel.sketchfile = new File(tunnel.tunneldirectory, tunnel.sketchfilename); 
		}

		if (tunnel.tsketch != null) // sketch need outputting
		{
			LineOutputStream los = new LineOutputStream(tunnel.sketchfile);  
			los.WriteLine(TNXML.sHEADER); 
			los.WriteLine(""); 

			los.WriteLine(TNXML.xcomopen(0, TNXML.sTUNNELXML)); 

			// this will be a list in future.  
			tunnel.tsketch.WriteXML(los); 
			los.WriteLine(TNXML.xcomclose(0, TNXML.sTUNNELXML)); 

			los.close(); 
		}
	}

	/////////////////////////////////////////////
	public TunnelSaver(OneTunnel roottunnel, File savedirectory)  
	{
		// check that saved directory is good.  
		try
		{
			// create the directory tree
			CreateDirectoryRecurse(roottunnel, savedirectory); 

		}
		catch (IOException ie) 
		{
			System.out.println(ie.toString()); 		
		}; 
	}
}; 
