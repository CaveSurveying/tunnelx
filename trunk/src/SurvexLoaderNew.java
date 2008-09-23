////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2008  Julian Todd.
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

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Collections;
import java.util.Collection;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

//
//
// SurvexLoaderNew
//
//

/////////////////////////////////////////////
/////////////////////////////////////////////
class SVXline
{
	String cmd;
	String sline;
	String comment;

	/////////////////////////////////////////////
	SVXline(String lcmd, String lsline, String lcomment)
	{
		cmd = lcmd;
		sline = lsline;
		comment = lcomment;
	}

	/////////////////////////////////////////////
	boolean IsCalibration()
	{
		if (cmd == null)
			return false;
		return ("*calibrate".equals(cmd) || "*units".equals(cmd) || "*date".equals(cmd) || "*team".equals(cmd));

	}

	/////////////////////////////////////////////
	public String toString()
	{
		if (cmd == null)
		{
			if (comment == null)
				return sline;
			return sline + comment;
		}
		if (comment == null)
			return cmd + " " + sline;
		return cmd + " " + sline + comment;
	}

	/////////////////////////////////////////////
	SVXline(String lsline)
	{
		sline = lsline.trim();

		// comment should chew up previous whitespace
		int ic = sline.indexOf(";");
		if (ic != -1)
		{
			while ((ic > 0) && (sline.charAt(ic - 1) <= '\u0020'))
				ic--;
			comment = sline.substring(ic);
			sline = sline.substring(0, ic);
		}
		else
			comment = null;

		if ((sline.length() != 0) && (sline.charAt(0) == '*'))
		{
			int ie = 0;
			while (ie < sline.length())
			{
				if (sline.charAt(ie) <= '\u0020')
					break;
				ie++;
			}

			cmd = sline.substring(0, ie).toLowerCase();
			sline = sline.substring(ie).trim();
			sline = sline.replace("\"", "");
		}
		else
			cmd = null;
	}
}



/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexLoaderNew
{
	Map<String, Set<String> > eqmap = null;
	Map<String, Set<String> > eqlmap = null;  

	// used for interpreting svx text
	List<OneLeg> vlegs = null;
	List<OneLeg> vfixes = null;
	Vec3 avgfix;
	Map<String, OneStation> osmap = null;
	Vec3d sketchLocOffset;

	Stack<OneStation> statrec = null;
	int npieces = 0;
	int nstationsdone = 0;

	// allow for loading the centreline as an elevation
	boolean belevation = false; 
	float elevationvalue = 0.0F; 
	// ;IMPORT_AS_ELEVATION 90


	/////////////////////////////////////////////
	public SurvexLoaderNew()
	{
	}

	/////////////////////////////////////////////
	void DumpEQs()
	{
		List< Set<String> > Dss = new ArrayList< Set<String> >();
		for (Set<String> lss : eqmap.values())
		{
			if (!Dss.contains(lss))
				System.out.println("PPP: " + lss);
			Dss.add(lss);
		}
	}



	/////////////////////////////////////////////
	void AddEquate(String prefixd, String sline)
	{
		//System.out.println("EQQ: " + svxbatch.prefix + "  " + svxline.sline);
		Set<String> v0 = null;
		for (String eqel : sline.split("\\s+"))
		{
			String peqel = prefixd + eqel.toLowerCase();
			if (v0 != null)
			{
            	Set<String> v = eqmap.get(peqel);
            	if (v != null)
            	{
					v0.addAll(v);
					for (String p : v)
						eqmap.put(p, v0);
            	}
				else
				{
					v0.add(peqel);
					eqmap.put(peqel, v0);
				}
			}
			else
			{
				v0 = eqmap.get(peqel);
				if (v0 == null)
				{
					v0 = new HashSet<String>();
					v0.add(peqel);
					eqmap.put(peqel, v0);
				}
			}

			// put the mapping into the SVXbatch prefix
			if (eqlmap != null)
			{
				int lld = peqel.lastIndexOf('.');
				String lprefixd = peqel.substring(0, lld + 1).toLowerCase();
				Set<String> vel = eqlmap.get(lprefixd);
				if (vel == null)
				{
					vel = new HashSet<String>();
					eqlmap.put(lprefixd, vel);
				}
				vel.add(peqel);
			}
		}
	}



	/////////////////////////////////////////////
	void ReadSurvexRecurseIncludeOnly(StringBuilder sb, FileAbstraction loadfile) throws IOException
	{
		LineInputStream lis = new LineInputStream(loadfile, null, null);
		while (lis.FetchNextLineNoSplit())
        {
			String sline = lis.GetLine();
            if (sline.matches("(?i)\\s*\\*include\\s.*"))
			{
                SVXline svxline = new SVXline(sline);
                assert "*include".equals(svxline.cmd);
				FileAbstraction includefile = FileAbstraction.calcIncludeFile(loadfile, svxline.sline, false);
                sb.append(TN.nl);
                sb.append(" ;;;;;;;;;;;;;;;;;;;;;;;;;;;");
                sb.append(TN.nl);
                sb.append(" ; included file \"");
                sb.append(svxline.sline);
                sb.append("\"");
                if (svxline.comment != null)
                    sb.append(svxline.comment);
                sb.append(TN.nl);
                sb.append(" ;;;;;;;;;;;;;;;;;;;;;;;;;;;");
                sb.append(TN.nl);
				ReadSurvexRecurseIncludeOnly(sb, includefile);
                sb.append(" ; end-included file \"");
                sb.append(svxline.sline);
                sb.append("\"");
                sb.append(TN.nl);
                sb.append(TN.nl);
            }
            else
            {
                sb.append(sline);
                sb.append(TN.nl);
            }
        }
    }


	/////////////////////////////////////////////
	public String LoadSVX(FileAbstraction loadfile)
	{
        StringBuilder sb = new StringBuilder();
		try
		{ ReadSurvexRecurseIncludeOnly(sb, loadfile); }
		catch (IOException e)
		{ TN.emitError(e.toString()); };
        return sb.toString();
    }


	/////////////////////////////////////////////
	// pulls stuff into vlegs
	void InterpretSvxTextRecurse(String prefixd, LineInputStream lis, LegLineFormat initLLF, int depth)
	{
		// make working copy (will be from new once the header is right).
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(initLLF);
		while (lis.FetchNextLine())
		{
			if (lis.w[0].equals(""))
			{
				// magic code which we can stick at the start to cause the centreline to be converted to an elevation
				if (lis.comment.startsWith("IMPORT_AS_ELEVATION"))
				{
					elevationvalue = Float.valueOf(lis.comment.substring(20).trim()); 
					belevation = true; 
				}
			}
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*units"))
			{
				int ist = 2; 
				while (lis.w[ist].equals("dx") || lis.w[ist].equals("dy") || lis.w[ist].equals("dz"))
					ist++; 
				for (int iist = 1; iist < ist; iist++)
					CurrentLegLineFormat.StarUnits(lis.w[iist], lis.w[ist], lis.w[ist + 1], lis);
			}
			else if (lis.w[0].equalsIgnoreCase("*set"))
				CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2], lis);
			else if (lis.w[0].equalsIgnoreCase("*data"))
			{
				if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc))
					TN.emitWarning("Bad *data line:  " + lis.GetLine());
			}

			else if (lis.w[0].equalsIgnoreCase("*fix"))
			{
				OneLeg oleg = CurrentLegLineFormat.ReadFix(lis.w, lis);
				if (oleg != null)
				{
					oleg.stto = prefixd + oleg.stto.toLowerCase();
					vfixes.add(oleg);
				}
			}

			else if (lis.w[0].equalsIgnoreCase("*date"))
				CurrentLegLineFormat.bb_svxdate = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*title"))
				CurrentLegLineFormat.bb_svxtitle = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*flags"))
				CurrentLegLineFormat.StarFlags(lis.w, lis.iwc);
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
				CurrentLegLineFormat.UpdateTotalTeam(); 
			}

			else if (lis.w[0].equalsIgnoreCase("*begin"))
			{
				String nprefixd; 
				if (lis.w[1].toLowerCase().equals(""))
				{
					// no longer an issue due to ignoring structure now
					// all we need to maintain track of really are flags surface
					//TN.emitMessage("empty *begin in " + prefixd); 
					nprefixd = prefixd;
				}
				else
					nprefixd = prefixd + lis.w[1].toLowerCase() + "."; 	
				InterpretSvxTextRecurse(nprefixd, lis, CurrentLegLineFormat, depth + 1); // recurse down
			}
			else if (lis.w[0].equalsIgnoreCase("*end"))
			{
				if (depth == 0)
					TN.emitWarning("Too many *ends for the *begin blocks");
				return;  // out of recursion
			}
			else if (lis.w[0].equalsIgnoreCase("*include"))
				TN.emitWarning("word should have been stripped");

			else if (lis.w[0].equalsIgnoreCase("*entrance"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*instrument"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*export"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*equate"))
			{
				SVXline svxline = new SVXline(lis.GetLine());
				AddEquate(prefixd, svxline.sline);
			}
			else if (lis.w[0].equalsIgnoreCase("*sd"))
				; // ignore.

			else if (lis.w[0].startsWith("*"))
				TN.emitWarning("Unknown command: " + lis.w[0]);

			// used to be ==2.  want to use the ignoreall term in the *data normal...
			else if ((lis.iwc >= 2) || ((CurrentLegLineFormat.newlineindex != -1) && (lis.iwc >= 1)))
			{
				OneLeg oleg = CurrentLegLineFormat.ReadLeg(lis.w, lis);
				if (oleg != null)
				{
					if (oleg.stfrom != null)
						oleg.stfrom = prefixd + oleg.stfrom.toLowerCase();
					oleg.stto = prefixd + oleg.stto.toLowerCase();
					vlegs.add(oleg);
				}
			}

			else
				TN.emitWarning("Too few arguments: " + lis.GetLine());
		}
		if (depth != 0)
			TN.emitWarning("Data ended with *begin blocks still open");
	}

	/////////////////////////////////////////////
	void InterpretSvxText(String svxtext)
	{
 		vlegs = new ArrayList<OneLeg>();
		vfixes = new ArrayList<OneLeg>();
		osmap = new HashMap<String, OneStation>();
		eqmap = new HashMap<String, Set<String> >();

 		LineInputStream lis = new LineInputStream(svxtext, null);
		InterpretSvxTextRecurse("", lis, new LegLineFormat(), 0);

		avgfix = new Vec3(0.0F, 0.0F, 0.0F);
		for (OneLeg ol : vfixes)
        	avgfix.PlusEquals(ol.m);
		if (vfixes.size() != 0)
			avgfix.TimesEquals(1.0F / vfixes.size());

		// group the equates into the map
 		for (Set<String> vs : eqmap.values())
		{
			String osn = Collections.min(vs);
			OneStation osc = new OneStation(osn);
			for (String s : vs)
				osmap.put(s, osc);
		}


		// put the station objects into the legs
		for (OneLeg ol : vlegs)
		{
			if (ol.stfrom != null)
			{
				String lstfrom = ol.stfrom.toLowerCase();
				ol.osfrom = osmap.get(lstfrom);
				if (ol.osfrom == null)
				{
					ol.osfrom = new OneStation(ol.stfrom);
					osmap.put(lstfrom, ol.osfrom);
				}
			}
			String lstto = ol.stto.toLowerCase();
			ol.osto = osmap.get(lstto);
			if (ol.osto == null)
			{
				ol.osto = new OneStation(ol.stto);
				osmap.put(lstto, ol.osto);
			}
		}

		// put station object in the fixes
		for (OneLeg olf : vfixes)
		{
			assert (olf.stfrom == null);
			String lstto = olf.stto.toLowerCase();
			olf.osto = osmap.get(lstto);
			if (olf.osto == null)
			{
				olf.osto = new OneStation(olf.stto);
				osmap.put(lstto, olf.osto);
			}
		}

 		System.out.println("Num Legs: " + vlegs.size() + "  EQQ: " + eqmap.size() + "  SS: " + osmap.values().size() + "  " + avgfix.toString());
	}


	/////////////////////////////////////////////
	class posentry extends Vec3d
	{
		String sname;

		posentry(String[] w)
		{
			assert (w.length == 5) && w[0].equals("");
			sname = w[4];
			x = Double.parseDouble(w[1]);
			y = Double.parseDouble(w[2]);
			z = Double.parseDouble(w[3]);
		}
	}

	/////////////////////////////////////////////
	boolean LoadPosFile(LineInputStream lis, Vec3 appsketchLocOffset) throws IOException
	{
		lis.FetchNextLineNoSplit();
		System.out.println("POSLINE0:  " + lis.GetLine());

		// (  -19.97,    -0.88,   -64.00 ) 204.110_bidet.1

		// load all the stations first so we can average them
		sketchLocOffset = (appsketchLocOffset == null ? new Vec3d(0.0, 0.0, 0.0) : new Vec3d(appsketchLocOffset.x, appsketchLocOffset.y, appsketchLocOffset.z));
		List<posentry> posentries = new ArrayList<posentry>();
		while (lis.FetchNextLineNoSplit())
		{
			String[] w = lis.GetLine().split("[\\s,()]+");
			posentry pe = new posentry(w);
			posentries.add(pe);
			if (appsketchLocOffset == null)
				sketchLocOffset.PlusEquals(pe);
		}
		if ((posentries.size() != 0) && (appsketchLocOffset == null))
			sketchLocOffset.TimesEquals(1.0 / posentries.size());

		for (posentry pe : posentries)
		{
			OneStation os = osmap.get(pe.sname);
			if (os != null)
			{
				if (os.Loc == null)
					os.Loc = new Vec3((float)(pe.x - sketchLocOffset.x), (float)(pe.y - sketchLocOffset.y), (float)(pe.z - sketchLocOffset.z));
				else
					//System.out.println("DUP:  " + pe.sname + ": " + Math.abs(os.Loc.x + sketchLocOffset.x - pe.x) + " " + Math.abs(os.Loc.y + sketchLocOffset.y - pe.y) + " " + Math.abs(os.Loc.z + sketchLocOffset.z - pe.z));
					assert (Math.abs(os.Loc.x + sketchLocOffset.x - pe.x) <= 0.01) && (Math.abs(os.Loc.y + sketchLocOffset.y - pe.y) <= 0.01) && (Math.abs(os.Loc.z + sketchLocOffset.z - pe.z) <= 0.01);
			}
			else
				TN.emitWarning("unable to match pos station: " + pe.sname);  // might be a naked fix
		}

		for (OneStation os : osmap.values())
			if (os.Loc == null)
				TN.emitWarning("** Station not POS applied: " + os.name);

		for (OneStation os : osmap.values())
		{
			if (os.Loc == null)
				return !TN.emitWarning("Station not POS applied: " + os.name);
		}
		return true;
	}

	// range values of the box (used for perspective projections
	float volxlo, volxhi;
	float volylo, volyhi;
	float volzlo, volzhi;

	/////////////////////////////////////////////
	boolean MergeVol(float x, float y, float z, boolean bFirst)
	{
		//System.out.println("  x=" + x + " y=" + y + " z=" + z + " " + bFirst);
		if (bFirst || (x < volxlo))
			volxlo = x;
		if (bFirst || (x > volxhi))
			volxhi = x;
		if (bFirst || (y < volylo))
			volylo = y;
		if (bFirst || (y > volyhi))
			volyhi = y;
		if (bFirst || (z < volzlo))
			volzlo = z;
		if (bFirst || (z > volzhi))
			volzhi = z;
		return false;
	}

	/////////////////////////////////////////////
	void CalcPosStack()
	{
		npieces++;
		while (!statrec.isEmpty())
		{
			OneStation os = statrec.pop();
			for (OneLeg ol : os.olconn)
			{
				if (ol.bnosurvey)
					continue;
				if ((os == ol.osfrom) && (ol.osto.Loc == null))
				{
					nstationsdone++;
					OneStation osn = ol.osto;
					osn.Loc = Vec3.GoLeg(os.Loc, ol.m, +1);
					statrec.push(osn);
				}
				if ((os == ol.osto) && (ol.osfrom.Loc == null))
				{
					nstationsdone++;
					OneStation osn = ol.osfrom;
					osn.Loc = Vec3.GoLeg(os.Loc, ol.m, -1);
					statrec.push(osn);
				}
			}
		}
	}

	/////////////////////////////////////////////
	int CalcStationPositions()
	{
		// build up the network of stations
		int npieces = 0;
		int nfixpieces = 0;
		statrec = new Stack<OneStation>();

		for (OneLeg ol : vlegs)
		{
			ol.osfrom.olconn.add(ol);
			ol.osto.olconn.add(ol);
		}

		for (OneLeg olf : vfixes)
		{
			if ((olf.osto != null) && (olf.osto.Loc == null))
			{
				olf.osto.Loc = new Vec3((float)(olf.m.x - sketchLocOffset.x), (float)(olf.m.y - sketchLocOffset.y), (float)(olf.m.z - sketchLocOffset.z));
				statrec.push(olf.osto);
				nstationsdone++;
			}
		}
		if (!statrec.isEmpty())
			CalcPosStack();
		for (OneLeg ol : vlegs)
		{
			if ((ol.osto != null) && (ol.osto.Loc == null))
			{
				ol.osto.Loc = new Vec3((float)npieces * 1000.0F, 0.0F, 0.0F);
				statrec.push(ol.osto);
				nstationsdone++;
				CalcPosStack();
			}
		}
		return npieces;
	}

	/////////////////////////////////////////////
	void ConstructWireframe(List<OneLeg> lvlegs, List<OneStation> lvstations)
	{
		lvlegs.addAll(vlegs);
		Set<OneStation> sstations = new HashSet<OneStation>();
		sstations.addAll(osmap.values());
		lvstations.addAll(sstations);

		// make the bounding box values, just containing the real legs
		boolean bFirst = true;
		for (OneStation os : sstations)
		{
			if (os.Loc != null)
				bFirst = MergeVol(os.Loc.x, os.Loc.y, os.Loc.z, bFirst);
			else
				System.out.println("SST missing: " + os.name);
		}
		System.out.println("Volume range [" + volxlo + ", " + volxhi + "]  [" + volylo + ", " + volyhi + "]  [" + (volzlo) + ", " + volzhi + "]");
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
// this is where we match the positions and discard vlegs already accounted for
	boolean ThinDuplicateLegs(List<OnePathNode> vnodes, List<OnePath> vpaths)
	{
		Map<String, OnePathNode> cnodemaps = new HashMap<String, OnePathNode>(); 
		for (OnePathNode opn : vnodes)
		{
			if (opn.IsCentrelineNode())
				cnodemaps.put(opn.pnstationlabel, opn); 
		}

		if (cnodemaps.isEmpty())
			return true; 
		
		float tol = 0.01F; 
		List<OneLeg> lvlegs = vlegs; 
		vlegs = new ArrayList<OneLeg>(); 
		for (OneLeg ol : lvlegs)
		{
			if (ol.osfrom == null)
				continue; 
			OnePathNode eopnfrom = cnodemaps.get(ol.osfrom.name); 
			OnePathNode eopnto = cnodemaps.get(ol.osto.name); 
			if (eopnfrom != null)
			{
				System.out.println("  " + Math.abs(ol.osfrom.station_opn.pn.getX() - eopnfrom.pn.getX()) + "  " + Math.abs(ol.osfrom.station_opn.pn.getY() - eopnfrom.pn.getY()) + "  " + Math.abs(ol.osfrom.station_opn.zalt - eopnfrom.zalt)); 
				if ((Math.abs(ol.osfrom.station_opn.pn.getX() - eopnfrom.pn.getX()) > tol) || (Math.abs(ol.osfrom.station_opn.pn.getY() - eopnfrom.pn.getY()) > tol) || (Math.abs(ol.osfrom.station_opn.zalt - eopnfrom.zalt) > tol))
					return false; 
				ol.osfrom.station_opn = eopnfrom; 
			}
			if (eopnto != null)
			{
				System.out.println(" t " + Math.abs(ol.osto.station_opn.pn.getX() - eopnto.pn.getX()) + "  " + Math.abs(ol.osto.station_opn.pn.getY() - eopnto.pn.getY()) + "  " + Math.abs(ol.osto.station_opn.zalt - eopnto.zalt)); 
				if ((Math.abs(ol.osto.station_opn.pn.getX() - eopnto.pn.getX()) > tol) || (Math.abs(ol.osto.station_opn.pn.getY() - eopnto.pn.getY()) > tol) || (Math.abs(ol.osto.station_opn.zalt - eopnto.zalt) > tol))
					return false; 
				ol.osto.station_opn = eopnto; 
			}
			if ((eopnfrom == null) || (eopnto == null)) // || the nodes exist, but there's no corresponding leg in the list
				vlegs.add(ol); 
		}
		return true; 
	}		
}

