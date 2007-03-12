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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.Vector;
import java.util.List;

import java.util.Map;
import java.util.HashMap;



/////////////////////////////////////////////
/////////////////////////////////////////////
/////////////////////////////////////////////
class CorrespondSketchs
{
	static String ExportBetweenUpOne(OneTunnel ot, String stat)
	{
		boolean bExported = false;
		if (stat.indexOf(TN.StationDelimeter) == -1)
		{
			// check for exports
			for (OneExport oe : ot.vexports)
			{
				// this is okay for *fix as long as tunnel non-null (when stotfrom can be).
				if (stat.equalsIgnoreCase(oe.estation))
				{
					stat = oe.ustation;
					bExported = true;
					break;
				}
			}

			if (!bExported)
				stat = TN.StationDelimeter + stat;
		}
		else
			stat = TN.PathDelimeter + stat;
		if (!bExported)
			stat = ot.name + stat;
		return stat;
	}

	/////////////////////////////////////////////
	static String ReExportNameRecurse(OneTunnel thtunnel, String lname)
	{
		for (int i = 0; i < thtunnel.ndowntunnels; i++)
		{
			OneTunnel dtunnel = thtunnel.downtunnels[i];
			if (!lname.startsWith(dtunnel.name))
				continue;
			String llname = lname.substring(dtunnel.name.length());
			if (llname.startsWith(TN.PathDelimeter))
				lname = llname.substring(TN.PathDelimeter.length());
			else if (llname.startsWith(TN.StationDelimeter))
				lname = llname.substring(TN.StationDelimeter.length());
			else if (llname.startsWith(TN.ExportDelimeter))
				lname = llname.substring(TN.ExportDelimeter.length());
			else
				continue;
			lname = ReExportNameRecurse(dtunnel, lname);
			lname = ExportBetweenUpOne(dtunnel, lname);
		}
		return lname;
	}


	/////////////////////////////////////////////
	static String ExportBetween(OneTunnel tunnsrc, String stat, OneTunnel otdest)
	{
		OneTunnel ot = tunnsrc;
		while (ot != otdest)
		{
			stat = ExportBetweenUpOne(ot, stat);
			ot = ot.uptunnel;
		}
		return stat;
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static OnePath FindMatchingCentrelinePath(String destpnlabtail, String destpnlabhead, OneSketch osdest)
	{
		String ldestpnlabtail = destpnlabtail.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');
		String ldestpnlabhead = destpnlabhead.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');

		// search for matching centrelines in destination place.
		OnePath dpath = null;
		for (int j = 0; j < osdest.vpaths.size(); j++)
		{
			OnePath lpath = (OnePath)osdest.vpaths.elementAt(j);
			if ((lpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (lpath.plabedl != null))
			{
				String dpnlabtail = lpath.plabedl.tail.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');
				String dpnlabhead = lpath.plabedl.head.replace(TN.PathDelimeterChar, '.').replace(TN.StationDelimeterChar, '.');

				if (ldestpnlabtail.equals(dpnlabtail) && ldestpnlabhead.equals(dpnlabhead))
				{
					if (dpath != null)
						TN.emitWarning("Ambiguous match of centrelines: " + dpnlabtail + " -> " + dpnlabhead);
					dpath = lpath;
				}
			}
		}
		return dpath;
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	Map<OnePath, OnePath> centlinecorresp = new HashMap<OnePath, OnePath>(); 
	boolean ExtractCentrelinePathCorrespondence(OneSketch asketch, OneTunnel thtunnel, OneSketch osdest, OneTunnel otdest)
	{
		// clear the result lists.
		centlinecorresp.clear();
		if (osdest == asketch)
		{
			TN.emitWarning("source and destination sketches the same");
			return false;
		}

		// check that the tunnels go up
		OneTunnel ot = thtunnel;
		while (ot != otdest)
		{
			ot = ot.uptunnel;
			if (ot == null)
			{
				TN.emitWarning("source tunnel does not map up to destination tunnel");
				return false;
			}
		}
	
		// now start matching the centrelines.
		for (int i = 0; i < asketch.vpaths.size(); i++)
		{
			OnePath path = (OnePath)asketch.vpaths.elementAt(i);
			if ((path.linestyle == SketchLineStyle.SLS_CENTRELINE) && (path.plabedl != null))
			{
				String pnlabtail = path.plabedl.tail;
				String pnlabhead = path.plabedl.head;
				if ((pnlabtail != null) && (pnlabhead != null))
				{
					// try to find a matching path, running a re-export if necessary
					OnePath dpath = FindMatchingCentrelinePath(ExportBetween(thtunnel, pnlabtail, otdest), ExportBetween(thtunnel, pnlabhead, otdest), osdest);
					if (dpath == null)
						dpath = FindMatchingCentrelinePath(ExportBetween(thtunnel, ReExportNameRecurse(thtunnel, pnlabtail), otdest), ExportBetween(thtunnel, ReExportNameRecurse(thtunnel, pnlabhead), otdest), osdest);
					if (dpath != null)
						centlinecorresp.put(path, dpath);
					else
						TN.emitWarning("No centreline path corresponding to " + path.plabedl.toString()/* + "  " + destpnlabtail + " " + destpnlabhead*/);
				}
			}
		}

		// false if no correspondence
		if (centlinecorresp.isEmpty())
		{
			TN.emitWarning("no corresponding centrelines found");
			return false;
		}
		return true;
	}
}

