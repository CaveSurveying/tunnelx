////////////////////////////////////////////////////////////////////////////////
// Tunnel  Julian Todd 2001.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.File; 
import java.io.IOException;  

//
//
// TunnelLoader
//
//

import org.xml.sax.*;

import javax.xml.parsers.SAXParserFactory;  
import javax.xml.parsers.ParserConfigurationException;  
import javax.xml.parsers.SAXParser;  


/////////////////////////////////////////////
// local class of the parser.  
class TunnelXMLparse extends HandlerBase // DefaultHandler
{
	OneTunnel tunnel; 

	String[] attnamestack = new String[50]; 
	String[] attvalstack = new String[50]; 
	int[] iposstack = new int[20]; 
	int istack = 0; 


	/////////////////////////////////////////////
	String SeStack(String name)  
	{
		for (int i = (istack != 0 ? iposstack[istack - 1] : 0) - 1; i >= 0; i--)  
		{
			if (attnamestack[i].equals(name)) 
				return attvalstack[i]; 
		}
		return null; 
	}

	/////////////////////////////////////////////
	void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess); 
		throw e; 
	}

	/////////////////////////////////////////////
	TunnelXMLparse(OneTunnel ltunnel)  
	{
		tunnel = ltunnel; 
	}

    /////////////////////////////////////////////
	public void startElement (String name, AttributeList attrs) throws SAXException
    {
		// roll forward the stack and introduce the attributes 
		iposstack[istack] = (istack == 0 ? 0 : iposstack[istack - 1]); 
		istack++; 
		for (int i = 0; i < attrs.getLength(); i++)  
		{
			attnamestack[iposstack[istack - 1]] = attrs.getName(i); 
			attvalstack[iposstack[istack - 1]] = attrs.getValue(i); 
			iposstack[istack - 1]++; 
		}

		// go through the possible commands 
		if (name.equals(TNXML.sEXPORT))  
			System.out.println("export " + SeStack(TNXML.sEXPORT_FROM_STATION) + "  " + SeStack(TNXML.sEXPORT_TO_STATION)); 

		// as a batch
		//static String sTAPE = "tape"; 
		//static String sCOMPASS = "compass"; 
		//static String sCLINO = "clino"; 

		// sketch things  
    }

    public void endElement (String name) throws SAXException  
    {
		// roll back the stack 
		istack--; 
    }
}; 



/////////////////////////////////////////////
/////////////////////////////////////////////
class TunnelLoader
{
	SAXParserFactory factory;  

	/////////////////////////////////////////////
	void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess); 
		throw e; 
	}


	/////////////////////////////////////////////
	void LoadSVXdata(OneTunnel tunnel, File svxfile)  
	{
		try 
		{
			LineInputStream lis = new LineInputStream(svxfile, null, null); 
			while (lis.FetchNextLine())
				tunnel.AppendLine(lis.GetLine()); 
			lis.close(); 
		}
		catch (IOException ie) 
		{
			System.out.println(ie.toString()); 		
		}; 
	}

	/////////////////////////////////////////////


	/////////////////////////////////////////////
	void LoadDirectoryRecurse(OneTunnel tunnel, File loaddirectory) throws IOException, SAXException, ParserConfigurationException
	{
		System.out.println("Dir " + loaddirectory.getName()); 
		if (!loaddirectory.isDirectory())  
			emitError("file not a directory " + loaddirectory.toString(), new IOException()); 

		File[] sfiles = loaddirectory.listFiles(); 

// here we begin to open XML readers and such like, filling in the different slots.  
		for (int i = 0; i < sfiles.length; i++) 
		{
			if (sfiles[i].isFile())  
			{
				String suff = TN.getSuffix(sfiles[i].getName()); 
				if (suff.equals(TN.SUFF_XML))  
				{
					SAXParser saxParser = factory.newSAXParser();
			        saxParser.parse(sfiles[i], new TunnelXMLparse(tunnel));
				}
				else if (suff.equals(TN.SUFF_SVX))  
					LoadSVXdata(tunnel, sfiles[i]); 
				else 
					TN.emitWarning("Unknown file type " + sfiles[i].getName()); 
			}
		}


		// get the subdirectories and recurse.  
		for (int i = 0; i < sfiles.length; i++) 
		{
			if (sfiles[i].isDirectory())  
			{
				String dtname = sfiles[i].getName(); 
				OneTunnel dtunnel = tunnel.IntroduceSubTunnel(dtname, null, false); 
				LoadDirectoryRecurse(dtunnel, sfiles[i]); 
			}
		}
	}

	/////////////////////////////////////////////
	public TunnelLoader(OneTunnel roottunnel, File loaddirectory)  
	{
		// check that saved directory is good.  
		try
		{
			// create the directory tree
			factory = SAXParserFactory.newInstance();  
			LoadDirectoryRecurse(roottunnel, loaddirectory); 
		}
		catch (IOException ie) 
		{
			System.out.println(ie.toString()); 		
		}  
		catch (ParserConfigurationException e) 
		{
			System.out.println(e.toString()); 		
		} 
		catch (SAXException e) 
		{
			System.out.println(e.toString()); 		
		}; 
	}
}; 
