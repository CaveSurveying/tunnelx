////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.util.Vector; 
import java.io.IOException; 
import java.io.File; 
import java.lang.StringBuffer; 


//
//
// OneTunnel
//
//
// the class which stores the data and connections for one tunnel of data
// this is 
class OneTunnel
{
	// tunnel starting point
	String name; 
	String fullname; 
	String fulleqname;	// same as fullname but with the blank begin names removed.  
	int depth; 

	// to tell whether its expanded in the tunneltree
	boolean bTunnelTreeExpanded = true; 

	// connections up (if present)
	OneTunnel uptunnel; 

	// the actual data in this tunnel
	StringBuffer TextData = new StringBuffer(); 

	// the leg line format at the start of this text
	LegLineFormat InitialLegLineFormat = new LegLineFormat(); 

	// the down connections
	OneTunnel[] downtunnels = null;   
	int ndowntunnels = 0; // number of down vectors (the rest are there for the delete to be "undone"). 



// this is the directory structure.  
	File tunneldirectory; 
	boolean btunneldirectorynew; 
	String svxfilename; 
	File svxfile; 
	String exportfilename; 
	File exportfile; 
	String xmlfilename; 
	File xmlfile; 
	String sketchfilename; 
	File sketchfile; 

// this is the compiled data from the TextData
	Vector vlegs = new Vector();		// of type OneLeg 

	// attributes 
	String svxdate; 
	String svxtitle; 
	String teamtape; 
	String teampics; 
	String teaminsts; 
	String teamnotes; 

	// the station names present in the survey leg data.  
	Vector stationnames = new Vector(); 

	// values read from the TextData
	Vector vstations = new Vector();	// of type OneStation. 


	// from the exports file.  
	Vector vexports = new Vector(); // of type OneExport.  

	boolean bWFtunnactive = false;	// set true if this tunnel is to be highlighted (is a descendent of activetunnel).  

	// the cross sections
	Vector vsections = new Vector(); 
	Vector vtubes = new Vector(); 

	// the sketch
	OneSketch tsketch = null; 

	// the back image (recursively taken from the uptunnel).  
	File fbackgimg = null; 

	// the possible sections 
	Vector vposssections = new Vector(); 

	// the text getting and setting
	String getTextData()
	{
		return(TextData.toString()); 
	}

	void setTextData(String text)
	{
		TextData.setLength(0); 
		TextData.append(text); 
	}

	public String toString() 
	{
		return name; 
	}


	// constructor
	public OneTunnel(String lname, OneTunnel luptunnel)
	{
		name = lname.toLowerCase(); 
		uptunnel = luptunnel; 
		fullname = (uptunnel != null ? (uptunnel.fullname + TN.PathDelimeter + name) : name); 

		// the eq name tree.  
		if ((uptunnel != null) && name.startsWith("d-blank-begin")) 
			fulleqname = uptunnel.fulleqname; 
		else 
			fulleqname = (uptunnel != null ? (uptunnel.fulleqname + TN.PathDelimeter + name) : name); 

		if (!fulleqname.equals(fullname)) 
			System.out.println("eq name is: " + fulleqname + "  for: " + fullname); 

		depth = (uptunnel == null ? 0 : uptunnel.depth + 1); 
	}; 

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(0, TNXML.sMEASUREMENTS, TNXML.sNAME, name)); 

		int nsets = 0; 
		if (svxdate != null) 
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_DATE, svxdate)); 
			nsets++; 
		}
		
		if (svxtitle != null) 
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TITLE, svxtitle)); 
			nsets++; 
		}

		if (teamtape != null) 
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TAPE_PERSON, teamtape)); 
			nsets++; 
		}

//	String teampics; 
//	String teaminsts; 
//	String teamnotes; 


		for (int i = 0; i < vlegs.size(); i++)  
			((OneLeg)vlegs.elementAt(i)).WriteXML(los); 

		// unroll the sets.  
		for (int i = 0; i < nsets; i++) 
			los.WriteLine(TNXML.xcomclose(0, TNXML.sSET)); 

		los.WriteLine(TNXML.xcomclose(0, TNXML.sMEASUREMENTS)); 
	}

	// extra text
	public void AppendLine(String textline)
	{
		TextData.append(textline); 
		TextData.append(TN.nl); 
	}

	/////////////////////////////////////////////
	public void RebuildSymbolText(int delval) 
	{
		// delete given value first 
		if (delval != -1) 
		{
			for (int i = delval; i < downtunnels.length - 1; i++) 
				downtunnels[i] = downtunnels[i + 1]; 
			downtunnels[downtunnels.length - 1] = null; 
			ndowntunnels--; 
		}

		TextData.setLength(0); 
		for (int i = 0; i < ndowntunnels; i++) 
			AppendLine("*subtunnel " + downtunnels[i].name); 
	}

	/////////////////////////////////////////////
	public OneTunnel IntroduceSubTunnel(String subtunnelname, LegLineFormat NewLegLineFormat, boolean bAppendingOnly)
	{
		// the null case.  
		OneTunnel subtunnel; 
		if (downtunnels == null) 
		{
			if (bAppendingOnly)  
				System.out.println("Warning:: Loading XSections in non-existent tunnel: " + subtunnelname); 

			downtunnels = new OneTunnel[1]; 
			subtunnel = new OneTunnel(subtunnelname, this); 
			downtunnels[0] = subtunnel; 
			ndowntunnels = 1; 
		}

		else 
		{
			// first search it out in the list of downtunnels 
			int i = 0; 
			for (i = 0; i < downtunnels.length; i++)
				if ((downtunnels[i] != null) && subtunnelname.equalsIgnoreCase(downtunnels[i].name))  
					break;  

			if ((i >= ndowntunnels) && bAppendingOnly) 
				System.out.println("Warning:: Loading XSections in non-existent tunnel: " + subtunnelname); 

			// totally new tunnel case 
			if (i == downtunnels.length)
			{	
				for (int j = downtunnels.length - 2; j >= ndowntunnels; j--) 
					downtunnels[j + 1] = downtunnels[j]; 

				if (ndowntunnels == downtunnels.length) 
				{
					OneTunnel[] ldowntunnels = downtunnels; 
					downtunnels = new OneTunnel[ndowntunnels * 2]; 
					for (int j = 0; j < ndowntunnels; j++) 
					{
						downtunnels[j] = ldowntunnels[j]; 
						downtunnels[j + ndowntunnels] = null; 
					}
				}
				subtunnel = new OneTunnel(subtunnelname, this); 
				downtunnels[ndowntunnels] = subtunnel; 
				ndowntunnels++; 
			}

			else
			{
				// tunnel needs swapping in (bring back one that is still there at the end of the array).  
				subtunnel = downtunnels[i]; 
				if (i >= ndowntunnels)
				{
					subtunnel = downtunnels[i]; 
					downtunnels[i] = downtunnels[ndowntunnels]; 
					downtunnels[ndowntunnels] = subtunnel; 
					ndowntunnels++; 
				}
			}
		}

		subtunnel.InitialLegLineFormat = new LegLineFormat(NewLegLineFormat); 
		return subtunnel; 
	}



	/////////////////////////////////////////////
	void emitMalformedSvxWarning(String mess) 
	{
		System.out.println("Malformed svx warning: " + mess); 
	}

	/////////////////////////////////////////////
	// pulls stuff into vlegs and vstations.  
	private void InterpretSvxText(LineInputStream lis)  
	{
		// make working copy (will be from new once the header is right).  
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(InitialLegLineFormat); 

		while (lis.FetchNextLine())
		{
			if (lis.w[0].equals("")) 
				; 
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2]); 
			else if (lis.w[0].equalsIgnoreCase("*units"))
				CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2]); 
			else if (lis.w[0].equalsIgnoreCase("*set"))
				CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2]); 
			else if (lis.w[0].equalsIgnoreCase("*data")) 
			{
				if (lis.w[1].equalsIgnoreCase("normal") || lis.w[1].equalsIgnoreCase("normal-")) 
				{
					if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc)) 
						System.out.println("Bad *data normal line:  " + lis.GetLine()); 
				}
				else
					System.out.println("What:  " + lis.GetLine()); 
			}

			else if (lis.w[0].equalsIgnoreCase("*fix")) 
			{
				try
				{
					OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadFix(lis.w, this); 
					vlegs.addElement(NewTunnelLeg); 
				}

				catch(NumberFormatException nfe)
				{
					System.out.println("Number Format Exception: " + lis.GetLine()); 
				}
			}

			else if (lis.w[0].equalsIgnoreCase("*date")) 
				svxdate = lis.w[1]; 
			else if (lis.w[0].equalsIgnoreCase("*title")) 
				svxtitle = lis.w[1]; 
			else if (lis.w[0].equalsIgnoreCase("*team")) 
			{
				if (lis.w[1].equalsIgnoreCase("notes")) 
					teamnotes = lis.remainder2.trim(); 
				else if (lis.w[1].equalsIgnoreCase("tape")) 
					teamtape = lis.remainder2.trim(); 
				else if (lis.w[1].equalsIgnoreCase("insts")) 
					teaminsts = lis.remainder2.trim(); 
				else if (lis.w[1].equalsIgnoreCase("pics")) 
					teampics = lis.remainder2.trim(); 
				else 
					System.out.println("Unknown *team " + lis.remainder1); 
			}

			// these two should check themselves  
			else if (lis.w[0].equalsIgnoreCase("*begin")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*end")) 
				; // ignore.  

			else if (lis.w[0].equalsIgnoreCase("*team")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*entrance")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*instrument")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*export")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*equate")) 
				; // ignore.  
			else if (lis.w[0].equalsIgnoreCase("*include")) 
				; // ignore.  

			else if (lis.w[0].startsWith("*")) 
				System.out.println("Unknown command: " + lis.w[0]);

			else if (lis.iwc >= 4) // used to be ==.  want to use the ignoreall term in the *data normal...
			{
				try
				{
					OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadLeg(lis.w, this); 
					vlegs.addElement(NewTunnelLeg); 
				}

				catch(NumberFormatException nfe)
				{
					System.out.println("Number Format Exception: " + lis.GetLine()); 
					System.out.println(" with  " + CurrentLegLineFormat.toString()); 
				}
			}

			else
			{
				System.out.println("Too few arguments: " + lis.GetLine()); 
			}
		}
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.  
	private void RefreshTunnelRecurse()
	{
		// now scan the data
		LineInputStream lis = new LineInputStream(getTextData()); 

		vlegs.removeAllElements();

		InterpretSvxText(lis); 

		// apply export names to the stations listed in the legs, 
		// and to the stations listed in the xsections

		// the recurse bit
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].RefreshTunnelRecurse(); 
	}


	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.  
	void RefreshTunnel()
	{
		RefreshTunnelRecurse(); 
	}


	/////////////////////////////////////////////
	void SetWFactiveRecurse(boolean lbWFtunnactive) 
	{
		bWFtunnactive = lbWFtunnactive; 
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].SetWFactiveRecurse(lbWFtunnactive); 
	}


	/////////////////////////////////////////////
	void ResetUniqueBaseStationTunnels()
	{
		// the xsections
		for (int i = 0; i < vsections.size(); i++)
		{
			OneSection oxs = (OneSection)(vsections.elementAt(i)); 
			oxs.station0ot = this; 
			oxs.station0EXS = oxs.station0S; 
			oxs.station1ot = this; 
			oxs.station1EXS = oxs.station1S; 

			oxs.stationforeot = this; 
			oxs.stationforeEXS = oxs.orientstationforeS; 
			oxs.stationbackot = this; 
			oxs.stationbackEXS = oxs.orientstationbackS; 
		}
	}
}



