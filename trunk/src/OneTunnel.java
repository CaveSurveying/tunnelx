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

	// this is the directory structure (should all be in the same directory).
	FileAbstraction tundirectory = null;

	// the sketches (should be a sorted map from getSketchName() to sketch, but for the problem with the tunnelfilelist
	List<OneSketch> tsketches = new ArrayList<OneSketch>(); 

	// the fontcolours files
	List<FileAbstraction> tfontcolours = new ArrayList<FileAbstraction>();

	/////////////////////////////////////////////
	public String toString()
	{
		return name;
	}

	/////////////////////////////////////////////
	public OneTunnel(String lname)
	{
		name = lname.toLowerCase();
	};

	/////////////////////////////////////////////
	OneSketch FindSketchFrame(String sfsketch, MainBox mainbox)
	{
		if (sfsketch.equals(""))
			return null;

		// account for which sketches have actually been loaded
		for (OneSketch ltsketch : tsketches)
		{
			if (sfsketch.equals(ltsketch.sketchfile.getSketchName()))
			{
				if (ltsketch.bsketchfileloaded)
					return ltsketch;
				else if (mainbox != null)
				{
					mainbox.tunnelloader.LoadSketchFile(ltsketch, true);
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
		TN.emitWarning("Failed to find sketch " + sfsketch + " from " + name);
		return null;
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
					sketchframedef.SetSketchFrameFiller(mainbox, tsketch.realpaperscale, tsketch.sketchLocOffset);
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
}



