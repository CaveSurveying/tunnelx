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

