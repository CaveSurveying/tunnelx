////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;



// classes to be calculated from the equate array.  
/////////////////////////////////////////////
class Eq 
{
	String eqstationname = null; 
	OneTunnel eqtunnel; 
	Eq eqlink = this; 

	Eq(OneTunnel leqtunnel, String leqstationname)
	{
		eqtunnel = leqtunnel; 
		eqstationname = leqstationname; 
	}

	public String toString() 
	{
		return eqtunnel.fullname + "  " + eqstationname;  
	}
}
