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

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

import java.io.IOException;
import java.lang.StringBuffer;
import java.awt.geom.AffineTransform; 

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
	OneTunnel uptunnel = null;

	// the actual data in this tunnel
	StringBuffer TextData = new StringBuffer();
	int starendindex = -1;

	// the leg line format at the start of this text
	LegLineFormat initLLF = TN.defaultleglineformat;
	LegLineFormat CurrentLegLineFormat = initLLF; // this encapsulates the end value of all the constants like svxdate

	// the down connections
	List<OneTunnel> vdowntunnels = new ArrayList<OneTunnel>(); 

	// this is the directory structure (should all be in the same directory).
	FileAbstraction tundirectory = null;
	boolean bsvxfilechanged = false;
	FileAbstraction svxfile = null;
	boolean bexportfilechanged = false;
	FileAbstraction exportfile = null;

	// name should change to measurementsfile
	boolean bmeasurementsfilechanged = false;
	FileAbstraction measurementsfile = null;

		// intermediate survex file as part of the integration
		FileAbstraction t3dfile = null; 

        // output file from survex, retro-fitted for reliable loading.
        FileAbstraction posfile = null;
        List<OneLeg> vposlegs = null;

	// the sketches
	List<OneSketch> tsketches = new ArrayList<OneSketch>(); 

	// the fontcolours files
	List<FileAbstraction> tfontcolours = new ArrayList<FileAbstraction>(); 

	// this is the compiled data from the TextData
	List<OneLeg> vlegs = new ArrayList<OneLeg>();

	// attributes
	int datepos = 0; // index into list of dates
	int mdatepos = 0; // max index in all descending tunnels

	// the station names present in the survey leg data.
	List<String> stationnames = new ArrayList<String>();

	// values read from the TextData
	List<OneStation> vstations = new ArrayList<OneStation>();

	Vec3 posfileLocOffset = new Vec3(); // location offset of the stations (to avoid getting too far from the origin and losing float precision).
	// only nonzero when a .pos file is imported.

	// from the exports file.
	List<OneExport> vexports = new ArrayList<OneExport>(); 

	boolean bWFtunnactive = false;	// set true if this tunnel is to be highlighted (is a descendent of activetunnel).

	// the cross sections
	List<OneSection> vsections = new ArrayList<OneSection>();
	List<OneTube> vtubes = new ArrayList<OneTube>();


	// the possible sections
	List<PossibleXSection> vposssections = new ArrayList<PossibleXSection>();

	// the text getting and setting
	String getTextData()
	{
		return(TextData.toString());
	}

	void setTextData(String text)
	{
		TextData.setLength(0);
		TextData.append(text);
		starendindex = -1;
	}

	public String toString()
	{
		return name;
	}

	/////////////////////////////////////////////
	OneSketch FindSketchFrame(String sfsketch, MainBox mainbox)
	{
		if (sfsketch.equals(""))
			return null; 
			
		// this will separate out the delimeters and look up and down through the chain.
		if (sfsketch.startsWith("../"))
			return uptunnel.FindSketchFrame(sfsketch.substring(3), mainbox);
		int islash = sfsketch.indexOf('/');
		if (islash != -1)
		{
			String sftunnel = sfsketch.substring(0, islash);
			String sfnsketch = sfsketch.substring(islash + 1);

			for (OneTunnel downtunnel : vdowntunnels)
			{
				if (sftunnel.equals(downtunnel.name))
					return downtunnel.FindSketchFrame(sfnsketch, mainbox);
			}
		}

		// account for which sketches have actually been loaded
		for (OneSketch ltsketch : tsketches)
		{
			if (sfsketch.equals(ltsketch.sketchfile.getName()))
			{
				if (ltsketch.bsketchfileloaded)
					return ltsketch;
				else if (mainbox != null)
				{
					mainbox.tunnelloader.LoadSketchFile(this, ltsketch, true);
					mainbox.tunnelfilelist.tflist.repaint(); 
					return ltsketch; 
				}
				else
				{
					TN.emitWarning("Sketch for frame not loaded: " + sfsketch);
					return null; 
				}
			}
		}
		TN.emitWarning("Failed to find sketch " + sfsketch + " from " + fullname);
		return null;
	}

	/////////////////////////////////////////////
	void UpdateSketchFrames(OneSketch tsketch, int iProper, MainBox mainbox)
	{
		List<OneSketch> framesketchesseen = (iProper != SketchGraphics.SC_UPDATE_NONE ? new ArrayList<OneSketch>() : null); 			
		for (OneSArea osa : tsketch.vsareas)
		{
			if (osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME)
			{
				// osa.pldframesketch.
				OneSketch lpframesketch = FindSketchFrame(osa.pldframesketch.sfsketch, mainbox);  // loads if necessary
				osa.UpdateSketchFrame(lpframesketch, tsketch.realpaperscale); 
				
				if ((iProper != SketchGraphics.SC_UPDATE_NONE) && (lpframesketch != null))
				{
					// got to consider setting the sket
					SubsetAttrStyle sksas = mainbox.sketchdisplay.sketchlinestyle.subsetattrstylesmap.get(osa.pldframesketch.sfstyle); 
					if ((sksas == null) && (osa.pframesketch.sksascurrent == null))
						sksas = mainbox.sketchdisplay.subsetpanel.sascurrent;  
					if ((sksas != null) && (sksas != osa.pframesketch.sksascurrent) && !framesketchesseen.contains(lpframesketch))
					{
						TN.emitMessage("Setting sketchstyle to " + sksas.stylename + " (maybe should relay the symbols)"); 
						osa.pframesketch.SetSubsetAttrStyle(sksas, mainbox.sketchdisplay.vgsymbols); 
					}
					// SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS or SketchGraphics.SC_UPDATE_ALL
					lpframesketch.UpdateSomething(iProper, false); 
				}
				if ((framesketchesseen != null) && !framesketchesseen.contains(lpframesketch))
					framesketchesseen.add(lpframesketch); 
			}
		}
	}


	/////////////////////////////////////////////
	// goes through files that exist and those that are intended to be saved
	FileAbstraction GetUniqueSketchFileName()
	{
		int sknum = tsketches.size();
		FileAbstraction res;
		while (true)
		{
			res = FileAbstraction.MakeDirectoryAndFileAbstraction(tundirectory, name + "-sketch" + sknum + ".xml");
			sknum++;
			boolean bexists = res.exists();
			if (res.equals(svxfile) || res.equals(exportfile) || res.equals(measurementsfile))
				bexists = true;

			for (OneSketch tsketch : tsketches)
			{
				if (res.equals(tsketch.sketchfile))
                	bexists = true;
			}
			if (!bexists)
				break;
		}
		return res;
	}

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(0, TNXML.sMEASUREMENTS, TNXML.sNAME, name));

		int nsets = 0;
		if (CurrentLegLineFormat.bb_svxdate != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_DATE, CurrentLegLineFormat.bb_svxdate));
			nsets++;
		}

		if (CurrentLegLineFormat.bb_svxtitle != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TITLE, CurrentLegLineFormat.bb_svxtitle));
			nsets++;
		}

		if (CurrentLegLineFormat.bb_teamtape != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TAPE_PERSON, CurrentLegLineFormat.bb_teamtape));
			nsets++;
		}

//	String teampics;
//	String teaminsts;
//	String teamnotes;


		for (OneLeg ol : vlegs)
			ol.WriteXML(los);

		// unroll the sets.
		for (int i = 0; i < nsets; i++)
			los.WriteLine(TNXML.xcomclose(0, TNXML.sSET));


		// write the xsections and tubes
		for (int i = 0; i < vsections.size(); i++)
			((OneSection)vsections.get(i)).WriteXML(los, i);
		for (int i = 0; i < vtubes.size(); i++)
			((OneTube)vtubes.get(i)).WriteXML(los, vsections);

		los.WriteLine(TNXML.xcomclose(0, TNXML.sMEASUREMENTS));
	}


	// extra text
	/////////////////////////////////////////////
	public void Append(String textline)
	{
		if (textline.startsWith("*end"))
			starendindex = TextData.length();
		else
			starendindex = -1;
		TextData.append(textline);
	}

	/////////////////////////////////////////////
	public void AppendLine(String textline)
	{
		Append(textline);
		TextData.append(TN.nl);
	}

	/////////////////////////////////////////////
	// for the survex crap exports that have to come straight after a *begin
	public void PrependLine(String textline)
	{
		TextData.insert(0, textline + TN.nl);
	}


	/////////////////////////////////////////////
	// used to put in the *pos_fix,  what a hack.
	public void AppendLineBeforeStarEnd(String textline)
	{
		if (starendindex != -1)
		{
			TextData.insert(starendindex, textline);
			starendindex += textline.length();
			TextData.insert(starendindex, TN.nl);
			starendindex += TN.nl.length();
		}
		else
			AppendLine(textline);
	}


	/////////////////////////////////////////////
	public OneTunnel(String lname, LegLineFormat newLLF)
	{
		name = lname.toLowerCase();
		uptunnel = null;
		fullname = name;

		// the eq name tree.
		fulleqname = name;
		depth = 0;

		if (newLLF != null)
			initLLF = new LegLineFormat(newLLF);
	};

	/////////////////////////////////////////////
	public OneTunnel IntroduceSubTunnel(OneTunnel subtunnel)
	{
// should check this subtunnel is actually new.
		subtunnel.uptunnel = this;
		subtunnel.fullname = fullname + TN.PathDelimeter + subtunnel.name;

		// the eq name tree.
		subtunnel.fulleqname = fulleqname + TN.PathDelimeter + subtunnel.name;
		if (!subtunnel.fulleqname.equals(subtunnel.fullname))
			TN.emitMessage("eq name is: " + subtunnel.fulleqname + "  for: " + subtunnel.fullname);

		subtunnel.depth = depth + 1;

		vdowntunnels.add(subtunnel);

		return subtunnel;
	}



	/////////////////////////////////////////////
	void emitMalformedSvxWarning(String mess)
	{
		TN.emitWarning("Malformed svx warning: " + mess);
	}

	/////////////////////////////////////////////
	// pulls stuff into vlegs and vstations.
	private void InterpretSvxText(LineInputStream lis)
	{
		// make working copy (will be from new once the header is right).
		CurrentLegLineFormat = new LegLineFormat(initLLF);

		while (lis.FetchNextLine())
		{
			if (lis.w[0].equals(""))
				;
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*units"))
				CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*set"))
				CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2], lis);
			else if (lis.w[0].equalsIgnoreCase("*data"))
			{
				if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc))
					TN.emitWarning("Bad *data line:  " + lis.GetLine() + ": " + fullname);
			}

			else if (lis.w[0].equalsIgnoreCase("*fix") || lis.w[0].equalsIgnoreCase("*pos_fix"))
			{
				OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadFix(lis.w, this, lis.w[0].equalsIgnoreCase("*pos_fix"), lis);
				if (NewTunnelLeg != null)
					vlegs.add(NewTunnelLeg);
			}

			else if (lis.w[0].equalsIgnoreCase("*date"))
				CurrentLegLineFormat.bb_svxdate = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*title"))
				CurrentLegLineFormat.bb_svxtitle = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*flags"))
				; // ignore for now
			else if (lis.w[0].equalsIgnoreCase("*team"))
			{
				if (lis.w[1].equalsIgnoreCase("notes"))
					CurrentLegLineFormat.bb_teamnotes = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("tape"))
					CurrentLegLineFormat.bb_teamtape = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("insts"))
					CurrentLegLineFormat.bb_teaminsts = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("pics"))
					CurrentLegLineFormat.bb_teampics = lis.remainder2.trim();
				else
					; // TN.emitMessage("Unknown *team " + lis.remainder1);
			}

			else if (lis.w[0].equalsIgnoreCase("*begin"))
				TN.emitWarning("word should have been stripped");
			else if (lis.w[0].equalsIgnoreCase("*end"))
				TN.emitWarning("word should have been stripped");
			else if (lis.w[0].equalsIgnoreCase("*include"))
				TN.emitWarning("word should have been stripped");

			else if (lis.w[0].equalsIgnoreCase("*entrance"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*instrument"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*export"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*equate"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*sd"))
				; // ignore.

			else if (lis.w[0].startsWith("*"))
				TN.emitWarning("Unknown command: " + lis.w[0]);

			else if (lis.iwc >= 2) // used to be ==.  want to use the ignoreall term in the *data normal...
			{
				OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadLeg(lis.w, this, lis);
				if (NewTunnelLeg != null)
					vlegs.add(NewTunnelLeg);
			}

			else
			{
				TN.emitWarning("Too few arguments: " + lis.GetLine());
			}
		}
	}


	/////////////////////////////////////////////
	class sortdate implements Comparator<OneTunnel>
	{
		public int compare(OneTunnel ot1, OneTunnel ot2)
			{	return ot1.CurrentLegLineFormat.bb_svxdate.compareTo(ot2.CurrentLegLineFormat.bb_svxdate);  }
	}


	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.
	void RefreshTunnelFromSVX()
	{
		List<OneTunnel> alltunnels = new ArrayList<OneTunnel>(); 
		alltunnels.add(this); 
		for (int ia = 0; ia < alltunnels.size(); ia++)
		{
			OneTunnel tunnel = alltunnels.get(ia); 
			LineInputStream lis = new LineInputStream(tunnel.getTextData(), tunnel.svxfile);
			tunnel.vlegs.clear();
			tunnel.InterpretSvxText(lis);
			alltunnels.addAll(tunnel.vdowntunnels); 
		}

		List<OneTunnel> salltunnels = new ArrayList<OneTunnel>(); 
		salltunnels.addAll(alltunnels); 
		Collections.sort(salltunnels, new sortdate()); 
		for (int i = 0; i < salltunnels.size(); i++)
		{
			OneTunnel tunnel = salltunnels.get(i); 
			tunnel.datepos = i; 
			tunnel.mdatepos = i; 
		}
		
		for (int i = alltunnels.size() - 1; i >= 0; i--)
		{
			OneTunnel tunnel = alltunnels.get(i); 
			if ((tunnel.uptunnel != null) && (tunnel.uptunnel.mdatepos < tunnel.mdatepos))
				tunnel.uptunnel.mdatepos = tunnel.mdatepos; 
		}
	}


	/////////////////////////////////////////////
	void SetWFactiveRecurse(boolean lbWFtunnactive)
	{
		bWFtunnactive = lbWFtunnactive;
		for (OneTunnel downtunnel : vdowntunnels)
			downtunnel.SetWFactiveRecurse(lbWFtunnactive);
	}

	/////////////////////////////////////////////
	void ApplySplineChangeRecurse()
	{
		for (OneSketch tsketch : tsketches)
		{
			if (tsketch.bsketchfileloaded)
				tsketch.ApplySplineChange();
		}
		for (OneTunnel downtunnel : vdowntunnels)
			downtunnel.ApplySplineChangeRecurse();
	}

	/////////////////////////////////////////////
	void ResetUniqueBaseStationTunnels()
	{
		// the xsections
		for (int i = 0; i < vsections.size(); i++)
		{
			OneSection oxs = (OneSection)(vsections.get(i));
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



