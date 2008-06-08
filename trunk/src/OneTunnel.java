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

	// the sketches (should be a sorted map from getSketchName() to sketch, but for the problem with the tunnelfilelist
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

boolean bWFtunnactive = false;	// set true if this tunnel is to be highlighted (is a descendent of activetunnel).


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
		//if (sfsketch.startsWith("../"))  // should be PathDelimeterChar
		//	return uptunnel.FindSketchFrame(sfsketch.substring(3), mainbox);
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
			if (sfsketch.equals(ltsketch.sketchfile.getSketchName()))
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
	void DumpDetails()
	{
		System.out.println("Dump Fullname: " + fullname + "  downtunnels: " + vdowntunnels.size());
	}

	/////////////////////////////////////////////
	void UpdateSketchFrames(OneSketch tsketch, int iProper, MainBox mainbox)
	{
		List<OneSketch> framesketchesseen = (iProper != SketchGraphics.SC_UPDATE_NONE ? new ArrayList<OneSketch>() : null);
		for (OneSArea osa : tsketch.vsareas)
		{
			if ((osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME) && (osa.sketchframedefs != null))
			{
				for (SketchFrameDef sketchframedef : osa.sketchframedefs)
				{
					sketchframedef.SetSketchFrameFiller(this, mainbox, tsketch.realpaperscale, tsketch.sketchLocOffset);
					OneSketch lpframesketch = sketchframedef.pframesketch;
					if ((iProper != SketchGraphics.SC_UPDATE_NONE) && (lpframesketch != null))
					{
						// got to consider setting the sket
						SubsetAttrStyle sksas = mainbox.sketchdisplay.sketchlinestyle.subsetattrstylesmap.get(sketchframedef.sfstyle);
						if ((sksas == null) && (sketchframedef.pframesketch.sksascurrent == null))
							sksas = mainbox.sketchdisplay.subsetpanel.sascurrent;
						if ((sksas != null) && (sksas != sketchframedef.pframesketch.sksascurrent) && !framesketchesseen.contains(lpframesketch))
						{
							TN.emitMessage("Setting sketchstyle to " + sksas.stylename + " (maybe should relay the symbols)");
							sketchframedef.pframesketch.SetSubsetAttrStyle(sksas, mainbox.sketchdisplay.vgsymbols, mainbox.sketchdisplay.sketchlinestyle.pthstyleareasigtab.sketchframedefCopied);
							SketchGraphics.SketchChangedStatic(SketchGraphics.SC_CHANGE_SAS, lpframesketch, null);
						}
						// SketchGraphics.SC_UPDATE_ALL_BUT_SYMBOLS or SketchGraphics.SC_UPDATE_ALL
						lpframesketch.UpdateSomething(iProper, false);
                    	SketchGraphics.SketchChangedStatic(iProper, lpframesketch, null);
					}
					if ((framesketchesseen != null) && !framesketchesseen.contains(lpframesketch))
						framesketchesseen.add(lpframesketch);
				}
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
			res.xfiletype = FileAbstraction.FA_FILE_XML_SKETCH; 
			sknum++;
			boolean bexists = res.exists();
			if (res.equals(svxfile))
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
	class sortdate implements Comparator<OneTunnel>
	{
		public int compare(OneTunnel ot1, OneTunnel ot2)
			{	return ot1.CurrentLegLineFormat.bb_svxdate.compareTo(ot2.CurrentLegLineFormat.bb_svxdate);  }
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
}



