////////////////////////////////////////////////////////////////////////////////
// Tunnel  Julian Todd 2001.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.File; 
import java.io.IOException;  
import java.net.URLEncoder; 

//
//
// TNXML
//
//



/////////////////////////////////////////////
// constants
/////////////////////////////////////////////
class TNXML
{
	static String sHEADER = "<?xml version='1.0' encoding='us-ascii'?>"; 

	static String sTUNNELXML = "tunnelxml"; 

	static String sSET = "set"; 
	static String sFLOAT_VALUE = "fval"; 
	static String sTEXT = "text"; 
	static String sNAME = "name"; 


	static String sEXPORT = "export"; 
		static String sEXPORT_FROM_STATION = "estation"; 
		static String sEXPORT_TO_STATION = "ustation"; 

	static String sMEASUREMENTS = "measurements"; 
		static String sSVX_DATE = "date"; 
		static String sSVX_TITLE = "title"; 
		static String sSVX_TAPE_PERSON = "tapeperson"; 

	static String sLEG = "leg"; // effectively the same as set
		static String sFROM_STATION = "from"; 
		static String sTO_STATION = "to"; 

		static String sTAPE = "tape"; 
		static String sCOMPASS = "compass"; 
		static String sCLINO = "clino"; 

	static String sSKETCH = "sketch"; 

	static String sSKETCH_PATH = "skpath"; 
		static String sFROM_SKNODE = "from"; 
		static String sTO_SKNODE = "to"; 
		static String sSK_LINESTYLE = "linestyle"; 
		// linestyle decode here.  
		static String sSPLINED = "splined"; 

	static String sSYMBOL = "symbol"; 
		static String sSYMBOL_NAME = "name"; 
		static String sSYMBOL_AXIS = "axis"; 
		static String sSYMBOL_AREA_LOC = "arealoc"; 

	static String sPOINT = "pt"; 
		static String sPTX = "X"; 
		static String sPTY = "Y"; 
		static String sPTZ = "Z"; 


	static String[] tabs = { "", "\t", "\t\t", "\t\t\t", "\t\t\t\t" }; 

	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0, String attr1, String val1)  
	{
		return tabs[indent] + "<" + command + " " + attr0 + "=\"" + URLEncoder.encode(val0) + "\" " + attr1 + "=\"" + URLEncoder.encode(val1) + "\"/>"; 
	}

	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0)  
	{
		return tabs[indent] + "<" + command + " " + attr0 + "=\"" + val0 + "\"/>";  
	}

	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0)  
	{
		return tabs[indent] + "<" + command + " " + attr0 + "=\"" + URLEncoder.encode(val0) + "\">"; 
	}

	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1)  
	{
		return tabs[indent] + "<" + command + " " + attr0 + "=\"" + val0 + "\" " + attr1 + "=\"" + val1 + "\">"; 
	}

	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3)  
	{
		return tabs[indent] + "<" + command + " " + attr0 + "=\"" + val0 + "\" " + attr1 + "=\"" + val1 + "\" " + attr2 + "=\"" + val2 + "\" " + attr3 + "=\"" + val3 + "\">"; 
	}

	/////////////////////////////////////////////
	static String xcomopen(int indent, String command)  
	{
		return tabs[indent] + "<" + command + ">"; 
	}

	/////////////////////////////////////////////
	static String xcomclose(int indent, String command)  
	{
		return tabs[indent] + "</" + command + ">"; 
	}
};
