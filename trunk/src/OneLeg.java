////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
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
	OneTunnel stotfrom; 
	String stto; 
	OneTunnel stotto; 

	OneStation osfrom = null; 
	OneStation osto = null; 

	// the measured vector (in polar and cartesian)
	float tape; 
	float compass; 
	float clino; 

	OneTunnel gtunnel;	// used to identify which leg belongs to which tunnel

	// the calculated vector
	Vec3 m = new Vec3(); 


	/////////////////////////////////////////////
	OneLeg(OneLeg ol)
	{
		stfrom = ol.stfrom; 
		stotfrom = ol.stotfrom; 
		stto = ol.stto; 
		stotto = ol.stotto; 
		osfrom = ol.osfrom; 
		osto = ol.osto; 
		tape = ol.tape; 
		compass = ol.compass; 
		clino = ol.clino; 
		gtunnel = ol.gtunnel; 
		m = ol.m; 
	}


	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, float ltape, float lcompass, float lclino, OneTunnel lgtunnel)
	{
		gtunnel = lgtunnel; 

		stfrom = lstfrom; 
		stotfrom = gtunnel; 
		stto = lstto; 
		stotto = gtunnel; 

		tape = ltape; 
		compass = lcompass; 
		clino = lclino; 

		// update from measurments
		m.z = tape * (float)Math.sin(clino * TN.angfac); 
		float cc = tape * (float)Math.cos(clino * TN.angfac); 
		m.x = cc * (float)Math.sin(compass * TN.angfac); 
		m.y = cc * (float)Math.cos(compass * TN.angfac); 
	}

	/////////////////////////////////////////////
	OneLeg(String lstto, float fx, float fy, float fz, OneTunnel lgtunnel) 
	{
		gtunnel = lgtunnel; 

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
		los.WriteLine(TNXML.xcomopen(0, TNXML.sLEG, TNXML.sFROM_STATION, stfrom, TNXML.sTO_STATION, stto)); 
		los.WriteLine(TNXML.xcom(1, TNXML.sTAPE, TNXML.sFLOAT_VALUE, String.valueOf(tape))); 
		los.WriteLine(TNXML.xcom(1, TNXML.sCOMPASS, TNXML.sFLOAT_VALUE, String.valueOf(compass))); 
		los.WriteLine(TNXML.xcom(1, TNXML.sCLINO, TNXML.sFLOAT_VALUE, String.valueOf(clino))); 
		los.WriteLine(TNXML.xcomclose(0, TNXML.sLEG)); 
	}


	/////////////////////////////////////////////
	void paintW(Graphics g, boolean bHighLightActive, DepthCol depthcol)
	{
		if (stfrom != null) 
		{
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
}

