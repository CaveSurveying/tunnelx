////////////////////////////////////////////////////////////////////////////////
// Tunnel  Julian Todd 2001.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.IOException;


//
//
// OneExport
//
//
class OneExport 
{
	// the station names and their pointers
	String estation; 
	String ustation; 

	/////////////////////////////////////////////
	OneExport(String lestation, String lustation)
	{
		estation = lestation; 
		ustation = lustation; 
	}

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcom(0, TNXML.sEXPORT, TNXML.sEXPORT_FROM_STATION, estation, TNXML.sEXPORT_TO_STATION, ustation)); 
	}
}; 

