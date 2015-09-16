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
	List<OneLeg> vanonymouses = null;
	Vec3 avgfix;
	Map<String, OneStation> osmap = null;
    List<OneLeg> vfilebeginblocklegs = null; 
	Map<String, OneStation> osfileblockmap = null;
	Vec3d sketchLocOffset;

	Stack<OneStation> statrec = null;
	int npieces = 0;
	int nstationsdone = 0;

	// allow for loading the centreline as an elevation
	// ;IMPORT_AS_ELEVATION 90
	boolean bprojectedelevation = false; 
	float projectedelevationvalue = 0.0F; 
	
	boolean btopextendedelevation = false; 

    OneLeg filebeginblockrootleg = null; 
	List<OneLeg> filebeginblocklegstack = null;  // starts out as null and initialized at first entry

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
    // lsline is the actual line of the *include to which calcIncludeFile was applied
	void ReadSurvexRecurseIncludeOnly(StringBuilder sb, FileAbstraction loadfile, String lsline, String lcomment, FileAbstraction upperloadfile) throws IOException
	{
		LineInputStream lis = new LineInputStream(loadfile.GetInputStream(), loadfile, null, null);
        sb.append("*file_begin \""); 
        sb.append(loadfile.getAbsolutePath()); 
        sb.append("\" \""); 
        sb.append(lsline); 
        sb.append("\"");
        if (lcomment != null)
            sb.append(lcomment);
        sb.append(TN.nl);
		while (lis.FetchNextLineNoSplit())
        {
			String sline = lis.GetLine();
            if (sline.matches("(?i)\\s*\\*include\\s.*"))
			{
                SVXline svxline = new SVXline(sline);
                assert "*include".equals(svxline.cmd);
				FileAbstraction includefile = FileAbstraction.calcIncludeFile(loadfile, svxline.sline, false);
                ReadSurvexRecurseIncludeOnly(sb, includefile, svxline.sline, svxline.comment, loadfile);
            }
            else
            {
                sb.append(sline);
                sb.append(TN.nl);
            }
        }
        sb.append("*file_end"); 
        sb.append(" \""); 
        sb.append(loadfile.getAbsolutePath()); 
        sb.append("\" \""); 
        sb.append(upperloadfile != null ? upperloadfile.getAbsolutePath() : ""); // this is included to make it easier to scan through and find what file we are now in.  
        sb.append("\""); 
        sb.append(TN.nl);
        lis.inputstream.close(); 
    }


	/////////////////////////////////////////////
	public String LoadSVX(FileAbstraction loadfile)
	{
        StringBuilder sb = new StringBuilder();
		try
		{ ReadSurvexRecurseIncludeOnly(sb, loadfile, "", null, null); }
		catch (IOException e)
		{ TN.emitError(e.toString()); };
        return sb.toString();
    }


	/////////////////////////////////////////////
	// pulls stuff into vlegs (the begin-end recursion).  The recursion is over the *begin/*end, not the *file_begin, *file_end
	void InterpretSvxTextRecurse(String prefixd, LineInputStream lis, LegLineFormat initLLF, int depth)
	{
        if (filebeginblocklegstack == null)
        {
            filebeginblocklegstack = new ArrayList<OneLeg>(); 
            filebeginblockrootleg = new OneLeg("__ROOT__", "__ROOT__", 0, "--root--"); 
            filebeginblocklegstack.add(filebeginblockrootleg); 
            vfilebeginblocklegs.add(filebeginblockrootleg); 
        }
        OneLeg currentfilebeginblockleg = filebeginblocklegstack.get(filebeginblocklegstack.size() - 1); 
        
		// make working copy (will be from new once the header is right).
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(initLLF);
		while (lis.FetchNextLine())
		{
			// concatennate case when there is a space after the *
			if (lis.w[0].equals("*") && (lis.iwc >= 2))
			{
				lis.w[0] = lis.w[0] + lis.w[1]; 
				for (int i = 2; i < lis.iwc; i++)
					lis.w[i - 1] = lis.w[i]; 
				lis.w[lis.iwc - 1] = ""; 
				lis.iwc--; 
			}

			if (lis.w[0].equals(""))
			{
				// magic code which we can stick at the start to cause the centreline to be converted to a projected elevation
				if (lis.comment.trim().startsWith("IMPORT_AS_ELEVATION"))
				{
					projectedelevationvalue = Float.valueOf(lis.comment.trim().substring(19).trim()); 
					bprojectedelevation = true; 
				}
			}
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*units"))
			{
				int ist = 2; 
				while (lis.w[ist].equalsIgnoreCase("dx") || lis.w[ist].equalsIgnoreCase("dy") || lis.w[ist].equalsIgnoreCase("dz") || 
                       lis.w[ist].equalsIgnoreCase("compass") || lis.w[ist].equalsIgnoreCase("backcompass") || lis.w[ist].equalsIgnoreCase("clino") || lis.w[ist].equalsIgnoreCase("backclino"))
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

			else if (lis.w[0].equalsIgnoreCase("*alias"))
			{
				if (!(lis.w[1].equalsIgnoreCase("station") && lis.w[2].equals("-") && lis.w[3].equals("..")))
					TN.emitWarning("Unrecognized *alias command"); 
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
				if (lis.w[1].toLowerCase().equals(""))  // blank begin case
				{
					// no longer an issue due to ignoring structure now
					// all we need to maintain track of really are flags surface
					//TN.emitMessage("empty *begin in " + prefixd); 
					nprefixd = prefixd;
                    filebeginblocklegstack.add(filebeginblocklegstack.get(filebeginblocklegstack.size() - 1)); // pointer to same leg that gets rolled back with the next *end
				}
				else
				{
                    nprefixd = prefixd + lis.w[1].toLowerCase() + "."; 	
                    OneLeg lcurrentfilebeginblockleg = new OneLeg(currentfilebeginblockleg.stto, nprefixd, vfilebeginblocklegs.size(), "--begincase--"); 
                    vfilebeginblocklegs.add(lcurrentfilebeginblockleg); 
                    filebeginblocklegstack.add(lcurrentfilebeginblockleg); 
                    currentfilebeginblockleg.lowerfilebegins.add(lcurrentfilebeginblockleg); 
                }
				InterpretSvxTextRecurse(nprefixd, lis, CurrentLegLineFormat, depth + 1); // recurse down (first step in recursion is to clone CurrentLegLineFormat)
			}
            
            // second exit point here
			else if (lis.w[0].equalsIgnoreCase("*end"))
			{
				if (depth == 0)
					TN.emitWarning("Too many *ends for the *begin blocks");
                    
                String currentfileended = filebeginblocklegstack.get(filebeginblocklegstack.size() - 1).stto; 
                if (!currentfileended.equals(currentfilebeginblockleg.stto))
                    TN.emitError("disagreement between include and begin trees!!! "+currentfilebeginblockleg.stto); 
                filebeginblocklegstack.remove(filebeginblocklegstack.size() - 1); 
				return;  // out of recursion
			}
            
			else if (lis.w[0].equalsIgnoreCase("*include"))
				TN.emitWarning("word should have been stripped");
            else if (lis.w[0].equalsIgnoreCase("*file_begin"))
            {
                OneLeg lcurrentfilebeginblockleg = new OneLeg(currentfilebeginblockleg.stto, lis.w[1], vfilebeginblocklegs.size()+1, lis.w[2]); 
                vfilebeginblocklegs.add(lcurrentfilebeginblockleg); 
                filebeginblocklegstack.add(lcurrentfilebeginblockleg); 
                currentfilebeginblockleg.lowerfilebegins.add(lcurrentfilebeginblockleg); 
                currentfilebeginblockleg = lcurrentfilebeginblockleg; 
            }
            else if (lis.w[0].equalsIgnoreCase("*file_end"))
            {
                assert currentfilebeginblockleg.stto.equals(lis.w[1]); 
                filebeginblocklegstack.remove(filebeginblocklegstack.size() - 1); 
                currentfilebeginblockleg = filebeginblocklegstack.get(filebeginblocklegstack.size() - 1); 
            }
            
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
			else if (lis.w[0].equalsIgnoreCase("*require"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*infer"))   // can be infer exports on to stop the errors being generated from survex
				; // ignore.

			else if (lis.w[0].startsWith("*"))
				TN.emitWarning("Unknown command: " + lis.w[0]);

			// used to be ==2.  want to use the ignoreall term in the *data normal...
			else if ((lis.iwc >= 2) || ((CurrentLegLineFormat.newlineindex != -1) && (lis.iwc >= 1)) || (CurrentLegLineFormat.bnosurvey && (lis.iwc >= 1)))
			{
				OneLeg oleg = CurrentLegLineFormat.ReadLeg(lis.w, lis);
				if (oleg != null)
				{
					if (oleg.stfrom != null)
						oleg.stfrom = prefixd + oleg.stfrom.toLowerCase();
					if (oleg.stto != null)
                        oleg.stto = prefixd + oleg.stto.toLowerCase();
                    if (oleg.stto != null)
                        vlegs.add(oleg);
                    else
                        vanonymouses.add(oleg); 
                    oleg.llcurrentfilebeginblockleg = currentfilebeginblockleg; 
                    currentfilebeginblockleg.lowerfilebegins.add(oleg); // mix in the legs we have here so we can find the C of G for each of these stations corresponding to a section of the cave
				}
			}

			else
				lis.emitWarning("Too few argumentss: " + lis.GetLine());
		}
        
        // exit point when run out of text
		if (depth != 0)
			TN.emitWarning("Data ended with *begin blocks still open");
	}

	/////////////////////////////////////////////
	void InterpretSvxText(String svxtext)
	{
 		vlegs = new ArrayList<OneLeg>();
		vfixes = new ArrayList<OneLeg>();
        vanonymouses = new ArrayList<OneLeg>(); 
		osmap = new HashMap<String, OneStation>();
		eqmap = new HashMap<String, Set<String> >();
        
        vfilebeginblocklegs = new ArrayList<OneLeg>(); 
		osfileblockmap = new HashMap<String, OneStation>();

 		LineInputStream lis = new LineInputStream(svxtext, null);
		LegLineFormat llf = new LegLineFormat(); 
		llf.btopextendedelevation = btopextendedelevation; 
		InterpretSvxTextRecurse("", lis, llf, 0);

		avgfix = new Vec3(0.0F, 0.0F, 0.0F);
        
        for (OneLeg ol : vfixes)
            avgfix.PlusEquals(ol.mlegvec);
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
            if (ol.stfrom != null)   // superfluous
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
        // put the station objects into the legs
        for (OneLeg ol : vanonymouses)
        {
            String lstfrom = ol.stfrom.toLowerCase();
            ol.osfrom = osmap.get(lstfrom);
            if (ol.osfrom == null)
            {
                ol.osfrom = new OneStation(ol.stfrom);
                osmap.put(lstfrom, ol.osfrom);
            }
            ol.osto = new OneStation("anonmymous_station");
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

        TN.emitMessage("Num Legs: " + vlegs.size() + "  EQQ: " + eqmap.size() + "  SS: " + osmap.values().size() + "  " + avgfix.toString());
        
        // bfilebeginmode tree of legs
        for (OneLeg ol : vfilebeginblocklegs)
        {
            ol.osfrom = osfileblockmap.get(ol.stfrom);
            if (ol.osfrom == null)
            {
                ol.osfrom = new OneStation(ol.stfrom);
                osfileblockmap.put(ol.stfrom, ol.osfrom);
            }
            ol.osto = osfileblockmap.get(ol.stto);
            if (ol.osto == null)
            {
                ol.osto = new OneStation(ol.stto);
                osfileblockmap.put(ol.stto, ol.osto);
            }
        }
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
	Vec3 GoLeg(Vec3 vf, OneLeg ol, int sign)
	{
		if (!btopextendedelevation)
			return new Vec3(vf.x + ol.mlegvec.x * sign, vf.y + ol.mlegvec.y * sign, vf.z + ol.mlegvec.z * sign); 
		int xsign = (ol.btopextendedelevationflip ? -1 : 1);  
		return new Vec3(vf.x + ol.mlegvec.x * xsign, vf.y + ol.mlegvec.y * sign, vf.z + ol.mlegvec.z * sign); 
	}

	/////////////////////////////////////////////
	void CalcPosStack()
	{
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
					osn.Loc = GoLeg(os.Loc, ol, +1);
					statrec.push(osn);
				}
				if ((os == ol.osto) && (ol.osfrom.Loc == null))
				{
					nstationsdone++;
					OneStation osn = ol.osfrom;
					osn.Loc = GoLeg(os.Loc, ol, -1);
					statrec.push(osn);
				}
			}
		}
	}

	/////////////////////////////////////////////
	int CalcStationPositions(boolean bfilebeginmode)
	{
		// build up the network of stations
		int npieces = 0;
		int nfixpieces = 0;
		statrec = new Stack<OneStation>();

        List<OneLeg> lvlegs = (bfilebeginmode ? vfilebeginblocklegs : vlegs); 
		for (OneLeg ol : lvlegs)
		{
			ol.osfrom.olconn.add(ol);
			ol.osto.olconn.add(ol);
		}
        if (!bfilebeginmode)
        {
            for (OneLeg ol : vanonymouses)
                ol.osfrom.olconn.add(ol);
        }

        if (!bfilebeginmode)
        {
            for (OneLeg olf : vfixes)
            {
                if ((olf.osto != null) && (olf.osto.Loc == null))
                {
                    olf.osto.Loc = new Vec3((float)(olf.mlegvec.x - sketchLocOffset.x), (float)(olf.mlegvec.y - sketchLocOffset.y), (float)(olf.mlegvec.z - sketchLocOffset.z));
                    statrec.push(olf.osto);
                    nstationsdone++;
                }
            }
        }
        
		if (!statrec.isEmpty())
		{
			CalcPosStack();
			npieces++; 
		}
		for (OneLeg ol : lvlegs)
		{
			if ((ol.osfrom != null) && (ol.osfrom.Loc == null))
			{
				TN.emitWarning("making station calculations for a disconnected component of the survey at station "+ol.osfrom); 
				ol.osfrom.Loc = new Vec3((float)npieces * 1000.0F, 0.0F, 0.0F);
				statrec.push(ol.osfrom);
				nstationsdone++;
				npieces++; 
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

static int DDD = 3; 

	/////////////////////////////////////////////
	String FindStationTitle(OnePath op)
	{
		//String lpnlabtail = op.plabedl.centrelinetail.replaceAll("[|^]", ".");
		//String lpnlabhead = op.plabedl.centrelinehead.replaceAll("[|^]", ".");

		String lpnlabtail = (op.pnstart != null ? op.pnstart.pnstationlabel.replaceAll("[|^]", ".") : "");
			// this has had null; don't know how.
		String lpnlabhead = (op.pnend != null ? op.pnend.pnstationlabel.replaceAll("[|^]", ".") : "");
if (op.pnend == null)
	System.out.println("FSSSST nll " + lpnlabtail); 

		String res1 = null;
		for (OneLeg ol : vlegs)
		{
			// I don't know if we're doing all the equates properly here
			// op.plabedl.centrelinetail op.plabedl.centrelinehead but with [|^] converted to .
			if ((ol.stfrom != null) && !ol.svxtitle.equals(""))
			{
				boolean bfrom = ol.osfrom.name.equalsIgnoreCase(lpnlabtail); 
				boolean bto = ol.osto.name.equalsIgnoreCase(lpnlabhead); 
				if (bfrom && bto)
					return ol.svxtitle; 
				if (bfrom || bto)
					res1 = ol.svxtitle; // captures one end
			}
		}
/*
System.out.println("Failed to match up " + lpnlabtail + " -- " + lpnlabhead); 
System.out.println("\n****"); 
System.out.println(res1); 
for (OneLeg ol : vlegs)
{
	if (ol.stfrom != null)
		System.out.println("  " + ol.osfrom.name + " " + ol.osto.name); 
}

//if (DDD-- < 0)
	System.exit(0); 
*/	
		return res1; 
	}
}

