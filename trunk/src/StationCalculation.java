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

import java.util.Vector; 

//
//
// StationCalculation
//
//
class StationCalculation 
{
	// local helper classes
	Vector statrec = new Vector(); 

	// values to show how much is done.  
	public int nstations = 0; 
	public int nstationsdone = 0; 



	/////////////////////////////////////////////
	static OneStation FFindOneStation(Vector vstations, OneTunnel lutunnel, String lname)
	{
		if (!lname.equals("")) 
		{	
			// search for new station
			for (int i = 0; i < vstations.size(); i++)
			{
				OneStation os = (OneStation)(vstations.elementAt(i));  
				if ((os.utunnel == lutunnel) && (os.name.equalsIgnoreCase(lname)))
					return os; 
			}
			TN.emitMessage("Failed to fetch station |" + lname + "|" + " in " + lutunnel.name); 
		} 
		return null; 
	}

	/////////////////////////////////////////////
	static OneStation FindOneStation(Vector vstations, OneTunnel lutunnel, String lname) 
	{
		// search for new station
		for (int i = 0; i < vstations.size(); i++)
		{
			OneStation os = (OneStation)(vstations.elementAt(i));  
			if ((os.utunnel == lutunnel) && (os.name.equalsIgnoreCase(lname)))
				return os; 
		}
		return null; 
	}


	/////////////////////////////////////////////
	static OneStation FetchOneStation(Vector vstations, OneTunnel lutunnel, String lname)
	{
		OneStation os = FindOneStation(vstations, lutunnel, lname);  
		// otherwise make new station
		if (os == null)
		{
			os = new OneStation(lutunnel, lname); 
			vstations.addElement(os); 
		}
		return os; 
	}


	/////////////////////////////////////////////
	static void LoadVTunnelsRecurse(OneTunnel otglobal, OneTunnel tunnel, boolean bFullNameMangle, boolean bApplyExports)
	{
		int sl = otglobal.vlegs.size(); 
		int sxs = otglobal.vsections.size(); 
		int stb = otglobal.vtubes.size(); 
		// load all the subtunnels
		for (int i = 0; i < tunnel.ndowntunnels; i++)
			LoadVTunnelsRecurse(otglobal, tunnel.downtunnels[i], bFullNameMangle, true); 
		
		// load this tunnel's information
		tunnel.ResetUniqueBaseStationTunnels(); 
		for (int i = 0; i < tunnel.vlegs.size(); i++)
			otglobal.vlegs.addElement(new OneLeg((OneLeg)(tunnel.vlegs.elementAt(i)))); 

		if (!bApplyExports)  
			return; 


		// exports to the legs
		for (int i = sl; i < otglobal.vlegs.size(); i++)
		{
			OneLeg ol = (OneLeg)(otglobal.vlegs.elementAt(i)); 
			boolean bfexp = false; 
			boolean btexp = false; 
			for (int j = 0; j < tunnel.vexports.size(); j++)
			{
				// this is okay for *fix as long as tunnel non-null (when stotfrom can be).  
				OneExport oe = (OneExport)tunnel.vexports.elementAt(j); 
				if ((ol.stfrom != null) && (ol.stotfrom == tunnel) && (ol.stfrom.equalsIgnoreCase(oe.estation))) 
				{
					ol.stfrom = oe.ustation; 
					ol.stotfrom = tunnel.uptunnel; 
					bfexp = true; 
				}

				if ((ol.stotto == tunnel) && (ol.stto.equalsIgnoreCase(oe.estation))) 
				{
					ol.stto = oe.ustation; 
					ol.stotto = tunnel.uptunnel; 
					btexp = true; 
				}
			}

			if (bFullNameMangle) 
			{
				if (!bfexp)  
					ol.stfrom = tunnel.name + (ol.stfrom.indexOf(TN.StationDelimeter) != -1 ? TN.PathDelimeter : TN.StationDelimeter) + ol.stfrom; 
				if (!btexp)  
					ol.stto = tunnel.name + (ol.stto.indexOf(TN.StationDelimeter) != -1 ? TN.PathDelimeter : TN.StationDelimeter) + ol.stto; 
			}
		}


		// leave out xsections when we are doing name mangling necessary to avoid confusion in the case of the sketches.  
		if (!bFullNameMangle)  
		{
			for (int i = 0; i < tunnel.vsections.size(); i++)
				otglobal.vsections.addElement(tunnel.vsections.elementAt(i)); 

			for (int i = 0; i < tunnel.vtubes.size(); i++)
				otglobal.vtubes.addElement(tunnel.vtubes.elementAt(i)); 
		}

		// exports to the sections
		for (int i = sxs; i < otglobal.vsections.size(); i++)
		{
			OneSection oxs = (OneSection)(otglobal.vsections.elementAt(i)); 

			for (int j = 0; j < tunnel.vexports.size(); j++)
			{
				OneExport oe = (OneExport)tunnel.vexports.elementAt(j); 
				if ((oxs.station0ot == tunnel) && oxs.station0EXS.equalsIgnoreCase(oe.estation)) 
				{
					oxs.station0EXS = oe.ustation; 
					oxs.station0ot = tunnel.uptunnel; 
				}
				if ((oxs.station1ot == tunnel) && oxs.station1EXS.equalsIgnoreCase(oe.estation)) 
				{
					oxs.station1EXS = oe.ustation; 
					oxs.station1ot = tunnel.uptunnel; 
				}

				if ((oxs.stationforeot == tunnel) && oxs.stationforeEXS.equalsIgnoreCase(oe.estation)) 
				{
					oxs.stationforeEXS = oe.ustation; 
					oxs.stationforeot = tunnel.uptunnel; 
				}

				if ((oxs.stationbackot == tunnel) && oxs.stationbackEXS.equalsIgnoreCase(oe.estation)) 
				{
					oxs.stationbackEXS = oe.ustation; 
					oxs.stationbackot = tunnel.uptunnel; 
				}
			}
		}
	}

	/////////////////////////////////////////////
	static void CopyRecurseExportVTunnels(OneTunnel otglobal, OneTunnel tunnel, boolean bFullNameMangle)  
	{
		otglobal.vlegs.removeAllElements(); 
		otglobal.vsections.removeAllElements(); 
		otglobal.vtubes.removeAllElements(); 

		LoadVTunnelsRecurse(otglobal, tunnel, bFullNameMangle, false); 
	}
			
	
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// calculates a connected component.  
	void CalcPosFrom(OneStation los, Vec3 lLoc)
	{
		los.Loc = lLoc; 
		statrec.addElement(los); 
		nstationsdone++; 

		while (!statrec.isEmpty())  
		{
			int le = statrec.size() - 1; 
			OneStation os = (OneStation)(statrec.elementAt(le)); 
			statrec.removeElementAt(le); 

			for (int i = 0; i < os.njl; i++)
			{
				OneLeg ol = os.olconn[i]; 
				if ((os == ol.osfrom) && (ol.osto.Loc == null))
				{
					nstationsdone++; 
					OneStation osn = ol.osto; 
					osn.Loc = Vec3.GoLeg(os.Loc, ol.m, +1); 
					statrec.addElement(osn); 
				}
				if ((os == ol.osto) && (ol.osfrom.Loc == null))
				{
					nstationsdone++; 
					OneStation osn = ol.osfrom; 
					osn.Loc = Vec3.GoLeg(os.Loc, ol.m, -1); 
					statrec.addElement(osn); 
				}
			}
		}
	}

	/////////////////////////////////////////////
	void FabricatePosition(Vec3 fixloc, Vector vstationsglobal, OneTunnel ot, String sname, int j) 
	{
		// extract a position from the global list if there is one.  
		if (vstationsglobal != null) 
		{
			// export this name up a bit.  
			boolean bNoExport;  
			do 
			{
				bNoExport = true; 
				for (int i = 0; i < ot.vexports.size(); i++) 
				{
					OneExport oe = (OneExport)ot.vexports.elementAt(i); 
					if ((sname.equalsIgnoreCase(oe.estation))) 
					{
						sname = oe.ustation; 
						ot = ot.uptunnel; 
						bNoExport = false; 
						break; 
					}
				}
			}
			while (!bNoExport); 

			// find the station in this global list 
			OneStation os = FindOneStation(vstationsglobal, ot, sname); 
			if ((os != null) && (os.Loc != null))  
			{
				TN.emitMessage(os.Loc.toString()); 
				fixloc.SetXYZ(os.Loc); 
				return; 
			}
		}

		// put this new station out of the way a bit.  
		fixloc.SetXYZ(j * 10.0F, 0.0F, 0.0F); 
	}


	/////////////////////////////////////////////
	int CalcStationPositions(OneTunnel ot, Vector vstationsglobal)
	{
		// load all the stations from the legs.  
		ot.vstations.removeAllElements(); 
		for (int i = 0; i < ot.vlegs.size(); i++)
		{
			OneLeg ol = (OneLeg)ot.vlegs.elementAt(i); 

			ol.osto = FetchOneStation(ot.vstations, ol.stotto, ol.stto);  

			if (ol.stfrom != null) 
			{
				ol.osfrom = FetchOneStation(ot.vstations, ol.stotfrom, ol.stfrom);  

				ol.osfrom.MergeLeg(ol); 
				ol.osto.MergeLeg(ol); 
			}
			else 
				ol.osfrom = null;	// *fix type.  (no point in merging the leg).  
		}

		// build the links for the sections
		for (int i = ot.vsections.size() - 1; i >= 0 ; i--)
		{
			OneSection xsection = (OneSection)(ot.vsections.elementAt(i)); 
			xsection.station0 = FFindOneStation(ot.vstations, xsection.station0ot, xsection.station0EXS); 
			xsection.station1 = FFindOneStation(ot.vstations, xsection.station1ot, xsection.station1EXS); 

			xsection.stationfore = FFindOneStation(ot.vstations, xsection.stationforeot, xsection.stationforeEXS); 
			xsection.stationback = FFindOneStation(ot.vstations, xsection.stationbackot, xsection.stationbackEXS); 

			// build mappings to equated xsections 
			if (xsection.station0S.equalsIgnoreCase(xsection.station1S)) 
			{
				if (xsection.station0.vsig == -1) 
				{
					xsection.xsectionE = null; 
					xsection.station0.vsig = i; 
				}
				else
				{
					xsection.xsectionE = (OneSection)(ot.vsections.elementAt(xsection.station0.vsig)); 
					// TN.emitMessage("Error: two xsections on same station"); 
				}
			}
		}


		// decide what our offset is going to be by averaging across the *fixes
		ot.LocOffset.SetXYZ(0.0F, 0.0F, 0.0F); 
		int nfixes = 0; 
		for (int i = 0; i < ot.vlegs.size(); i++) 
		{
			OneLeg ol = (OneLeg)ot.vlegs.elementAt(i); 
			if (ol.stfrom == null) 
			{
				ot.LocOffset.PlusEquals(ol.m); 
				nfixes++; 
			}
		}
		if (nfixes != 0) 
		{
			ot.LocOffset.TimesEquals(1.0F / nfixes); 
			TN.emitMessage("Undo station offset of " + ot.LocOffset.toString()); 
		}

		// build up the network of stations
		int npieces = 0; 
		int nfixpieces = 0; 

		nstations = ot.vstations.size(); 
		nstationsdone = 0; 

		// do all the *fixed places 
		Vec3 fixloc = new Vec3(); 
		for (int i = 0; i < ot.vlegs.size(); i++) 
		{
			OneLeg ol = (OneLeg)ot.vlegs.elementAt(i); 
			if ((ol.stfrom == null) && (ol.osto.Loc == null))  
			{
				fixloc.Diff(ol.m, ot.LocOffset); 
				CalcPosFrom(ol.osto, fixloc); 
				npieces = 1; 
				nfixpieces++; 
			}
		}

		// loop through and do each connected component
		for (int j = 0; j < ot.vstations.size(); j++)
		{
			OneStation os = (OneStation)ot.vstations.elementAt(j); 
			if (os.Loc == null)
			{
				npieces++; 
				nfixpieces++; 
				FabricatePosition(fixloc, vstationsglobal, ot, os.name, j); 
				CalcPosFrom(os, fixloc);  
			}
		}

		TN.emitMessage("  Number of pieces: " + npieces + " fixpieces: " + nfixpieces); 
		return(npieces); 
	}
}

