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

import java.awt.Graphics; 
import java.io.IOException; 

//
//
// OneLeg
//
//
class OneLeg 
{
	// the station names and their pointers
	String stfrom; 
	boolean bPosFix = false; // true only when station from is null
	OneTunnel stotfrom; 
	String stto; 
	OneTunnel stotto; 

	OneStation osfrom = null; 
	OneStation osto = null; 

	// the measured vector (in polar and cartesian)
	float tape; 
	float compass; 

	boolean bUseClino; 
	float clino; 
	float fromdepth; 
	float todepth; 

	OneTunnel gtunnel;	// used to identify which leg belongs to which tunnel

	// the calculated vector
	Vec3 m = new Vec3(); 


	/////////////////////////////////////////////
	OneLeg(OneLeg ol)
	{
		stfrom = ol.stfrom; 
		stotfrom = ol.stotfrom; 
		bPosFix = ol.bPosFix; 
		stto = ol.stto; 
		stotto = ol.stotto; 
		osfrom = ol.osfrom; 
		osto = ol.osto; 
		tape = ol.tape; 
		compass = ol.compass; 

		bUseClino = ol.bUseClino; 
		clino = ol.clino; 
		fromdepth = ol.fromdepth; 
		todepth = ol.todepth; 

		gtunnel = ol.gtunnel; 
		m = ol.m; 
	}


	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, float ltape, float lcompass, float lclino, OneTunnel lgtunnel)
	{
		gtunnel = lgtunnel; 

		stfrom = lstfrom; 
		stotfrom = gtunnel; 
		bPosFix = false; 
		stto = lstto; 
		stotto = gtunnel; 

		tape = ltape; 
		compass = lcompass; 
		bUseClino = true; 
		clino = lclino; 

		// update from measurments
		m.z = tape * (float)TN.degsin(clino); 
		float cc = tape * (float)TN.degcos(clino); 
		m.x = cc * (float)TN.degsin(compass); 
		m.y = cc * (float)TN.degcos(compass); 
	}

	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, float ltape, float lcompass, float lfromdepth, float ltodepth, OneTunnel lgtunnel)
	{
		gtunnel = lgtunnel; 

		stfrom = lstfrom; 
		stotfrom = gtunnel; 
		bPosFix = false; 
		stto = lstto; 
		stotto = gtunnel; 

		tape = ltape; 
		compass = lcompass; 
		bUseClino = false; 
		fromdepth = lfromdepth; 
		todepth = ltodepth; 

		// update from measurments
		m.z = todepth - fromdepth; 
		float ccsq = tape * tape - m.z * m.z; 
		float cc = (float)Math.sqrt(ccsq >= 0.0F ? ccsq : 0.0F); 
		m.x = cc * (float)TN.degsin(compass); 
		m.y = cc * (float)TN.degcos(compass); 
	}

	/////////////////////////////////////////////
	OneLeg(String lstto, float fx, float fy, float fz, OneTunnel lgtunnel, boolean lbPosFix) 
	{
		gtunnel = lgtunnel; 
		bPosFix = lbPosFix; 

		stfrom = null; 
		stotfrom = null; 
		stto = lstto; 
		stotto = gtunnel; 

		// maybe calculate measure 
		tape = -1.0F; 
		compass = -1.0F; 
		clino = -1.0F; 

		// update from measurments
		m.SetXYZ(fx, fy, fz); 
	}



	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		if (stfrom != null)  
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sLEG, TNXML.sFROM_STATION, stfrom, TNXML.sTO_STATION, stto)); 
			los.WriteLine(TNXML.xcom(1, TNXML.sTAPE, TNXML.sFLOAT_VALUE, String.valueOf(tape))); 
			los.WriteLine(TNXML.xcom(1, TNXML.sCOMPASS, TNXML.sFLOAT_VALUE, String.valueOf(compass))); 
			los.WriteLine(TNXML.xcom(1, TNXML.sCLINO, TNXML.sFLOAT_VALUE, String.valueOf(clino))); 
			los.WriteLine(TNXML.xcomclose(0, TNXML.sLEG)); 
		}

		// *fix type.  
		else 
		{
			los.WriteLine(TNXML.xcomopen(0, (bPosFix ? TNXML.sPOS_FIX : TNXML.sFIX), TNXML.sTO_STATION, stto)); 
			los.WriteLine(TNXML.xcom(1, TNXML.sPOINT, TNXML.sPTX, String.valueOf(m.x), TNXML.sPTY, String.valueOf(m.y), TNXML.sPTZ, String.valueOf(m.z))); 
			los.WriteLine(TNXML.xcomclose(0, (bPosFix ? TNXML.sPOS_FIX : TNXML.sFIX))); 
		}
	}


	/////////////////////////////////////////////
	void paintW(Graphics g, boolean bHighLightActive, DepthCol depthcol)
	{
		// get rid of fixed point vectors
		if (stfrom == null) 
			return; 

		// get rid of date restrictions
		if ((depthcol != null) && depthcol.bdatelimit)
		{
			if (gtunnel.dateorder > depthcol.datelimit)
				return;  
		}

		boolean bHighlight = (bHighLightActive && gtunnel.bWFtunnactive); 
		if ((depthcol == null) || bHighlight) 
		{
			g.setColor(bHighlight ? TN.wfmpointActive : TN.wfmLeg); 
			g.drawLine(osfrom.TLocX, osfrom.TLocY, osto.TLocX, osto.TLocY);
		}
			
		// funny colors
		else 
		{
			// for now do from lowest range.  
			// TN.xsgLines : 
			float zfrom = osfrom.Loc.z; 
			float zto = osto.Loc.z; 

			int izfrom = (int)((zfrom - depthcol.zlo) / (depthcol.zhi - depthcol.zlo) * depthcol.znslices); 
			if (izfrom < 0) 
				izfrom = 0; 
			if (izfrom >= depthcol.znslices) 
				izfrom = depthcol.znslices - 1; 

			g.setColor(depthcol.col[izfrom]); 
			g.drawLine(osfrom.TLocX, osfrom.TLocY, osto.TLocX, osto.TLocY);
		}
	}
}

