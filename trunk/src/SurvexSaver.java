////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.IOException; 
import java.util.Vector; 
import java.io.File; 

//
//
// SurvexSaver
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexSaver extends SurvexCommon
{
	boolean bCentreLine; 
	boolean bAsEquates; 
	boolean bTubes; 

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	String SaveTunnelRecurse(LineInputStream lis, OneTunnel tunnel, LineOutputStream los) throws IOException 
	{
		StringBuffer sbequates = (bAsEquates ? new StringBuffer() : null); 

		// first expand the text data if required
		while (lis.FetchNextLine())
		{
			if ((lis.iwc == 2) && lis.w[0].equalsIgnoreCase("*subtunnel")) 
			{
				// find the sub tunnel and output that. 
				OneTunnel downtunnel = null; 
				for (int i = 0; i < tunnel.ndowntunnels; i++)
				{
					downtunnel = tunnel.downtunnels[i]; 
					if (lis.w[1].equalsIgnoreCase(downtunnel.name)) 
						break; 
					downtunnel = null; 
				}
				if (downtunnel != null)
				{
					los.WriteLine("*begin " + downtunnel.name + (downtunnel.bTunnelTreeExpanded ? " -" : " +")); 
					LineInputStream lls = new LineInputStream(downtunnel.getTextData()); 
					String strequates = SaveTunnelRecurse(lls, downtunnel, los); 
					los.WriteLine("*end " + downtunnel.name); 
					los.WriteLine(strequates); 
				}
			}	

			else if (bCentreLine)
			{
				los.WriteLine(lis.GetLine()); 
			}
		}

		// then output cross sections and tubes. 
		if (bTubes)
		{
			// write out the sections. 
			for (int i = 0; i < tunnel.vsections.size(); i++)
				((OneSection)(tunnel.vsections.elementAt(i))).WriteSection(los); 
			
			// write out the tubes 
			for (int i = 0; i < tunnel.vtubes.size(); i++)
			{
				OneTube tube = (OneTube)(tunnel.vtubes.elementAt(i)); 
				int ind0 = tunnel.vsections.indexOf(tube.xsection0); 
				int ind1 = tunnel.vsections.indexOf(tube.xsection1); 
				if ((ind0 != -1) && (ind1 != -1) && (ind0 != ind1)) 
					los.WriteLine("*Linear_Tube " + String.valueOf(ind0) + " " + String.valueOf(ind1)); 
				else
					System.out.println("Tube missing end"); 
			}

			if (tunnel.tsketch != null) 
				tunnel.tsketch.WriteSketch(los); 
		}
		return(sbequates != null ? sbequates.toString() : ""); 
	}


	/////////////////////////////////////////////
	public SurvexSaver(OneTunnel filetunnel, File savefile, boolean lbCentreLine, boolean lbAsEquates, boolean lbTubes)  
	{
		bCentreLine = lbCentreLine; 
		bAsEquates = lbAsEquates; 
		bTubes = lbTubes; 

		try
		{
			LineOutputStream los = new LineOutputStream(savefile); 
			LineInputStream ls = new LineInputStream(filetunnel.getTextData()); 
			SaveTunnelRecurse(ls, filetunnel, los); 
			los.close(); 
		}
		catch (IOException ie) 
		{
			System.out.println(ie.toString()); 		
		}; 
	}
}; 


