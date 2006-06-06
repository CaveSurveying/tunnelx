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
import java.util.Arrays;
import java.util.Comparator;
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
	LegLineFormat InitialLegLineFormat = TN.defaultleglineformat;
	LegLineFormat CurrentLegLineFormat = InitialLegLineFormat; // this encapsulates the end value of all the constants like svxdate

	// the down connections
	OneTunnel[] downtunnels = null;
	int ndowntunnels = 0; // number of down vectors (the rest are there for the delete to be "undone").


	// this is the directory structure (should all be in the same directory).
	FileAbstraction tundirectory = null;
	boolean bsvxfilechanged = false;
	FileAbstraction svxfile = null;
	boolean bexportfilechanged = false;
	FileAbstraction exportfile = null;

	// name should change to measurementsfile
	boolean bmeasurementsfilechanged = false;
	FileAbstraction measurementsfile = null;

        // output file from survex, retro-fitted for reliable loading.
        FileAbstraction posfile = null;
        Vector vposlegs = null;

	// the sketches
	Vector tsketches = new Vector(); // of type OneSketch or type FileAbstraction if not loaded.

	// the fontcolours files
	Vector tfontcolours = new Vector(); // type FileAbstraction

// used to list those on the directory for handy access.
// this probably should go
//Vector imgfiles = new Vector();

// this is the compiled data from the TextData
	Vector vlegs = new Vector();		// of type OneLeg

	// attributes
	int dateorder = 0; // index into list of dates

	// the station names present in the survey leg data.
	Vector stationnames = new Vector();

	// values read from the TextData
	Vector vstations = new Vector();	// of type OneStation.

	Vec3 LocOffset = new Vec3(); // location offset of the stations (to avoid getting too far from the origin and losing float precision).

	// from the exports file.
	Vector vexports = new Vector(); // of type OneExport.

	// from the exports file.
	Vector vposfixes = new Vector(); // of type OnePosfix.

	boolean bWFtunnactive = false;	// set true if this tunnel is to be highlighted (is a descendent of activetunnel).

	// the cross sections
	Vector vsections = new Vector();
	Vector vtubes = new Vector();


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
		starendindex = -1;
	}

	public String toString()
	{
		return name;
	}

	/////////////////////////////////////////////
	OneSketch FindSketchFrame(String sfsketch)
	{
		// this will separate out the delimeters and look up and down through the chain.
		if (sfsketch.startsWith("../"))
			return uptunnel.FindSketchFrame(sfsketch.substring(3));
		int islash = sfsketch.indexOf('/');
		if (islash != -1)
		{
			String sftunnel = sfsketch.substring(0, islash);
			String sfnsketch = sfsketch.substring(islash + 1);

			for (int i = 0; i <	ndowntunnels; i++)
			{
				if (sftunnel.equals(downtunnels[i].name))
					return downtunnels[i].FindSketchFrame(sfnsketch);
			}
		}

		// account for which sketches have actually been loaded
		for (int i = 0; i < tsketches.size(); i++)
		{
			Object obj = tsketches.elementAt(i);
			if (obj instanceof OneSketch)
			{
				OneSketch ltsketch = (OneSketch)obj;
				if (sfsketch.equals(ltsketch.sketchfile.getName()))
					return ltsketch;
			}
			else
			{
				FileAbstraction lfasketch = (FileAbstraction)obj;
				if (sfsketch.equals(lfasketch.getName()))
				{
					TN.emitWarning("Sketch for frame not loaded: " + sfsketch);
					//lselectedsketch = mainbox.tunnelloader.LoadSketchFile(activetunnel, activesketchindex);
					//assert lselectedsketch == activetunnel.tsketches.elementAt(activesketchindex);
					return null;
				}
			}
		}
		TN.emitWarning("Failed to find sketch " + sfsketch + " from " + fullname);
		return null;
	}

	/////////////////////////////////////////////
	void UpdateSketchFrames(OneSketch tsketch)
	{
		for (int i = 0; i < tsketch.vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)tsketch.vsareas.elementAt(i);
	
			// make the framesketch for the area if there is one
			if ((osa.iareapressig == 55) && (osa.pldframesketch != null))
				osa.UpdateSketchFrame(osa.pldframesketch.sfsketch.equals("") ? tsketch : FindSketchFrame(osa.pldframesketch.sfsketch)); 
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

			for (int i = 0; i < tsketches.size(); i++)
			{
				if (tsketches.elementAt(i) instanceof FileAbstraction)
                {
                	if (res.equals(tsketches.elementAt(i)))
                		bexists = true;
                }
                else if (res.equals(((OneSketch)tsketches.elementAt(i)).sketchfile))
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


		for (int i = 0; i < vlegs.size(); i++)
			((OneLeg)vlegs.elementAt(i)).WriteXML(los);

		// unroll the sets.
		for (int i = 0; i < nsets; i++)
			los.WriteLine(TNXML.xcomclose(0, TNXML.sSET));


		// write the xsections and tubes
		for (int i = 0; i < vsections.size(); i++)
			((OneSection)vsections.elementAt(i)).WriteXML(los, i);
		for (int i = 0; i < vtubes.size(); i++)
			((OneTube)vtubes.elementAt(i)).WriteXML(los, vsections);

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
	public OneTunnel(String lname, LegLineFormat NewLegLineFormat)
	{
		name = lname.toLowerCase();
		uptunnel = null;
		fullname = name;

		// the eq name tree.
		fulleqname = name;
		depth = 0;

		if (NewLegLineFormat != null)
			InitialLegLineFormat = new LegLineFormat(NewLegLineFormat);
	};

	/////////////////////////////////////////////
	public OneTunnel IntroduceSubTunnel(OneTunnel subtunnel)
	{
		// extend the array.
		if (downtunnels == null)
			downtunnels = new OneTunnel[1];
		else if (ndowntunnels == downtunnels.length)
		{
			OneTunnel[] ldowntunnels = downtunnels;
			downtunnels = new OneTunnel[ndowntunnels * 2];
			for (int j = 0; j < ndowntunnels; j++)
			{
				downtunnels[j] = ldowntunnels[j];
				downtunnels[j + ndowntunnels] = null;
			}
		}

// should check this subtunnel is actually new.


		subtunnel.uptunnel = this;
		subtunnel.fullname = fullname + TN.PathDelimeter + subtunnel.name;

		// the eq name tree.
		subtunnel.fulleqname = fulleqname + TN.PathDelimeter + subtunnel.name;
		if (!subtunnel.fulleqname.equals(subtunnel.fullname))
			TN.emitMessage("eq name is: " + subtunnel.fulleqname + "  for: " + subtunnel.fullname);

		subtunnel.depth = depth + 1;

		downtunnels[ndowntunnels] = subtunnel;
		ndowntunnels++;

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
		CurrentLegLineFormat = new LegLineFormat(InitialLegLineFormat);

		while (lis.FetchNextLine())
		{
			if (lis.w[0].equals(""))
				;
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*units"))
				CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2], lis);
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
					vlegs.addElement(NewTunnelLeg);
			}

			else if (lis.w[0].equalsIgnoreCase("*date"))
				CurrentLegLineFormat.bb_svxtitle = lis.w[1];
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
					vlegs.addElement(NewTunnelLeg);
			}

			else
			{
				TN.emitWarning("Too few arguments: " + lis.GetLine());
			}
		}
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.
	private void RefreshTunnelRecurse(OneTunnel vgsymbols, Vector vtunnels)
	{
		vtunnels.addElement(this);

		// now scan the data
		LineInputStream lis = new LineInputStream(getTextData(), svxfile);

		vlegs.removeAllElements();

		InterpretSvxText(lis);


		// apply export names to the stations listed in the legs,
		// and to the stations listed in the xsections

		// the recurse bit
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].RefreshTunnelRecurse(vgsymbols, vtunnels);
	}


	/////////////////////////////////////////////
	class sortdate implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			OneTunnel ot1 = (OneTunnel)o1;
			OneTunnel ot2 = (OneTunnel)o2;
			return ot1.CurrentLegLineFormat.bb_svxdate.compareTo(ot2.CurrentLegLineFormat.bb_svxdate);
		}
	}

	/////////////////////////////////////////////
	int SetOrderdateorder(Vector vtunnels)
	{
		Object[] vts = vtunnels.toArray();
		Arrays.sort(vts, new sortdate());
		for (int i = 0; i < vts.length; i++)
 			((OneTunnel)vts[i]).dateorder = i;
		return vts.length;
	}


	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.
	void RefreshTunnel(OneTunnel vgsymbols)
	{
		Vector vtunnels = new Vector();
		RefreshTunnelRecurse(vgsymbols, vtunnels);
		dateorder = SetOrderdateorder(vtunnels);
System.out.println("dateorder " + dateorder);
	}


	/////////////////////////////////////////////
	void SetWFactiveRecurse(boolean lbWFtunnactive)
	{
		bWFtunnactive = lbWFtunnactive;
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].SetWFactiveRecurse(lbWFtunnactive);
	}

	/////////////////////////////////////////////
	void ApplySplineChangeRecurse()
	{
		for (int j = 0; j < tsketches.size(); j++)
		{
			if (tsketches.elementAt(j) instanceof OneSketch)
				((OneSketch)tsketches.elementAt(j)).ApplySplineChange();
		}
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].ApplySplineChangeRecurse();
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



