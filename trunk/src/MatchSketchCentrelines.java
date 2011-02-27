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
// MatchSketchCentrelines
//
//

/////////////////////////////////////////////
class PrefixLeg
{
	OnePath op;
	String pnlabtail;
	String pnlabhead;
	float vx;
	float vy;

    // the begin-blocks for the tail and head labels
    String pnlabblocktail; 
    String pnlabblockhead; 
    String pnlabstationtail; 
    String pnlabstationhead; 

	// matching destination
    PrefixLeg pltmember = null;  

    /////////////////////////////////////////////
    String desc()
    {
        return "Prefixleg "+pnlabtail+" "+pnlabhead+" "+vx+","+vy; 
    }

    /////////////////////////////////////////////
	PrefixLeg(OnePath lop)
	{
		op = lop;
		pnlabtail = op.plabedl.centrelinetail.replaceAll("[|^]", ".");
		pnlabhead = op.plabedl.centrelinehead.replaceAll("[|^]", ".");

        int dotpnlabtail = pnlabtail.lastIndexOf("."); 
        pnlabblocktail = (dotpnlabtail != -1 ? pnlabtail.substring(0, dotpnlabtail) : ""); 
        pnlabstationtail = (dotpnlabtail != -1 ? pnlabtail.substring(dotpnlabtail + 1) : pnlabtail); 
        int dotpnlabhead = pnlabhead.lastIndexOf("."); 
        pnlabblockhead = (dotpnlabhead != -1 ? pnlabhead.substring(0, dotpnlabhead) : ""); 
        pnlabstationhead = (dotpnlabhead != -1 ? pnlabhead.substring(dotpnlabhead + 1) : pnlabhead); 

   		vx = (float)(op.pnend.pn.getX() - op.pnstart.pn.getX());
		vy = (float)(op.pnend.pn.getY() - op.pnstart.pn.getY());
	}

    /////////////////////////////////////////////
	String FindCommonPrefix(PrefixLeg pl)
	{
		if (pnlabtail.endsWith(pl.pnlabtail) && pnlabhead.endsWith(pl.pnlabhead))
		{
			int lpt = pl.pnlabtail.length();
			int lph = pl.pnlabhead.length();
			int lt = pnlabtail.length();
			int lh = pnlabhead.length();
			if ((lpt == lt) && (lph == lh))
				return "";
			int ldt = lt - lpt - 1;
			int ldh = lh - lph - 1;
			if ((ldt >= 0) && (pnlabtail.charAt(ldt) == '.') && (ldh >= 0) && (pnlabhead.charAt(ldh) == '.'))
			{
				String prefixt = pnlabtail.substring(0, ldt + 1);  // include the dot
				String prefixh = pnlabhead.substring(0, ldh + 1);
				if (prefixt.equals(prefixh))
					return prefixt;
			}
		}
		return null;
	}

	/////////////////////////////////////////////
    float CompareDirection(PrefixLeg pl)
	{
		float lsq = vx * vx + vy * vy;
		float losq = pl.vx * pl.vx + pl.vy * pl.vy;
		float dot = vx * pl.vx + vy * pl.vy;
		if (dot == 0.0F)
			return 0.0F;
		return Math.max(0.0F, dot / Math.max(lsq, losq));
	}
}


/////////////////////////////////////////////
class PrefixCount implements Comparable<PrefixCount>
{
    String prefixfrom;
    String prefix;
    float score = 0.0F;
    int nscore = 0; 
    int nlegsfrom = 0; 
    int nlegsto = 0; 
    int i;   // used when we are sorting by best for each prefix
    

    PrefixCount(String lprefixfrom, String lprefix, int li, int lnlegsfrom, int lnlegsto)
    {
        prefixfrom = lprefixfrom; 
        prefix = lprefix;
        i = li; 
        nlegsfrom = lnlegsfrom; 
        nlegsto = lnlegsto; 
    }

    float avgscore()
    {
        float pres = score / Math.max(1, nscore); 
        if (prefixfrom.equals(prefix))
            pres = (3 + pres) / 4; 
                // -2 attempt to mask mismatch on low numbers (eg 7 legs) where the end points are equated to something else and count against
        float fac = 1.0F * nscore / Math.max(Math.max(nlegsfrom-2, nlegsto), 1); 
        return fac * pres; 
    }

    String desc()
    {
        return "avgscore="+avgscore() + " prefix="+prefix + " prefixfrom="+prefixfrom + " score="+score+ " nscore="+nscore+ " nlegsfrom="+ nlegsfrom + "  nlegsto=" + nlegsto; 
    }

    public int compareTo(PrefixCount opc)
    {
        return (int)Math.signum(opc.avgscore() - avgscore()); 
    }
}

/////////////////////////////////////////////
/////////////////////////////////////////////
class MatchSketchCentrelines
{
    List<String> blocknamesfrom = new ArrayList<String>(); 
    List<String> blocknamesto = new ArrayList<String>(); 

    List<PrefixLeg> prefixlegsfrom = new ArrayList<PrefixLeg>();
    List<PrefixLeg> prefixlegsto = new ArrayList<PrefixLeg>();

	List<PrefixLeg> prefixlegsfromunmatched = new ArrayList<PrefixLeg>();
	Map<OnePathNode, OnePathNode> nodemapping = new HashMap<OnePathNode, OnePathNode>();


	/////////////////////////////////////////////
	Map<String, String> GetBlockMapping()
	{
        // make the lists of blocknames
        Map<String, List<PrefixLeg> > mblocknamesto = new HashMap<String, List<PrefixLeg> >(); 
		for (PrefixLeg plt : prefixlegsto)
        {
            if (!mblocknamesto.containsKey(plt.pnlabblocktail))
                mblocknamesto.put(plt.pnlabblocktail, new ArrayList<PrefixLeg>()); 
            if (!mblocknamesto.containsKey(plt.pnlabblockhead))
                mblocknamesto.put(plt.pnlabblockhead, new ArrayList<PrefixLeg>()); 
            if (plt.pnlabblocktail.equals(plt.pnlabblockhead))
                mblocknamesto.get(plt.pnlabblocktail).add(plt); 
        }
        blocknamesto.addAll(mblocknamesto.keySet()); 

        Map<String, List<PrefixLeg> > mblocknamesfrom = new HashMap<String, List<PrefixLeg> >(); 
		for (PrefixLeg plf : prefixlegsfrom)
        {
            if (!mblocknamesfrom.containsKey(plf.pnlabblocktail))
                mblocknamesfrom.put(plf.pnlabblocktail, new ArrayList<PrefixLeg>()); 
            if (!mblocknamesfrom.containsKey(plf.pnlabblockhead))
                mblocknamesfrom.put(plf.pnlabblockhead, new ArrayList<PrefixLeg>()); 
            if (plf.pnlabblocktail.equals(plf.pnlabblockhead))
                mblocknamesfrom.get(plf.pnlabblocktail).add(plf); 
        }
        blocknamesfrom.addAll(mblocknamesfrom.keySet()); 


        // make the arrays of prefixcounts from-to 
        List< List<PrefixCount> > blockcorresp = new ArrayList< List<PrefixCount> >(); 
        for (int i = 0; i < blocknamesfrom.size(); i++)
        {
            List<PrefixCount> pclist = new ArrayList<PrefixCount>(); 
            for (int j = 0; j < blocknamesto.size(); j++)
            {
                String blocknamefrom = blocknamesfrom.get(i); 
                String blocknameto = blocknamesto.get(j); 
                /*System.out.println("\n\nblocknamefrom="+blocknamefrom); 
                for (PrefixLeg p : mblocknamesfrom.get(blocknamefrom))
                    System.out.println(p.desc()); 
                System.out.println("blocknameto="+blocknameto); 
                for (PrefixLeg p : mblocknamesto.get(blocknameto))
                    System.out.println(p.desc()); */
                pclist.add(new PrefixCount(blocknamefrom, blocknameto, i, mblocknamesfrom.get(blocknamefrom).size(), mblocknamesto.get(blocknameto).size())); 
            }
            blockcorresp.add(pclist); 
        }

        // insert all the possible corresponding edges according to each blockname and station names
		for (PrefixLeg plf : prefixlegsfrom)
		{
            if (!plf.pnlabblocktail.equals(plf.pnlabblockhead))
                continue; 
            for (PrefixLeg plt : prefixlegsto)  
            {
                if (!plt.pnlabblocktail.equals(plt.pnlabblockhead))
                    continue; 
                if (plf.pnlabstationtail.equals(plt.pnlabstationtail) && plf.pnlabstationhead.equals(plt.pnlabstationhead))
                {
                    int i = blocknamesfrom.indexOf(plf.pnlabblocktail); 
                    int j = blocknamesto.indexOf(plt.pnlabblocktail); 
                    PrefixCount pc = blockcorresp.get(i).get(j); 
                    pc.score += plt.CompareDirection(plf); 
                    pc.nscore++; 
                }
            }
        }

        // print out the likely correspondences
        List<PrefixCount> bestcorresp = new ArrayList<PrefixCount>(); 
        for (int i = 0; i < blocknamesfrom.size(); i++)
        {
    		Collections.sort(blockcorresp.get(i));
            PrefixCount pc = blockcorresp.get(i).get(0); 
            bestcorresp.add(pc); // pc.i helps find it again
        }
    	Collections.sort(bestcorresp);

        // the map of blocknames to blocknames; connect first match to first match
        Map<String, String> blockmapping = new HashMap<String, String>(); 
        for (PrefixCount pc : bestcorresp)
        {
            for (PrefixCount tpc : blockcorresp.get(pc.i))
            {
                if (blockmapping.values().contains(tpc.prefix) || (tpc.nscore == 0))
                    continue; 
                System.out.println("MM: " + tpc.desc()); 
                if (tpc.avgscore() > 0.5)
                    blockmapping.put(blocknamesfrom.get(pc.i), tpc.prefix); 
                else
                    TN.emitWarning("Rejecting map: "+tpc.desc()+"  not enough agreement/overlap.\n\nPerhaps take out excess unmatched centrelines from source sketch"); 
                break; 
            }
        }
        return blockmapping; 

        /*
		// not very smart n^2 algorithm
		Map<String, PrefixCount> prefixcounts = new HashMap<String, PrefixCount>();
		for (PrefixLeg plf : prefixlegsfrom)
		{
            for (PrefixLeg plt : prefixlegsto)  // the n^2 loop thing
            {
                String prefix = plt.FindCommonPrefix(plf);
                if (prefix != null)
                {
                    PrefixCount prefixcount = prefixcounts.get(prefix);
                    if (prefixcount == null)
                    {
                        prefixcount = new PrefixCount(prefix, -1);
                        prefixcounts.put(prefix, prefixcount);
                    }
                    prefixcount.score += plt.CompareDirection(plf);
                }
            }
        }

		List<PrefixCount> prefixcountlist = new ArrayList<PrefixCount>();
		prefixcountlist.addAll(prefixcounts.values());
		Collections.sort(prefixcountlist);

		for (PrefixCount prefixcount : prefixcountlist)
			System.out.println("Probable prefix: '" + prefixcount.prefix + "'  score: " + prefixcount.score);
		return (!prefixcountlist.isEmpty() ? prefixcountlist.get(0).prefix : null);
	   */
    }

	/////////////////////////////////////////////
	PrefixLeg IncreCorresp(PrefixLeg plf)
	{
		OnePathNode mpnstart = nodemapping.get(plf.op.pnstart);
		OnePathNode mpnend = nodemapping.get(plf.op.pnend);
		if ((mpnstart == null) && (mpnend == null))
			return null;

		// find the destination line that best shares the direction
		PrefixLeg pltbest = null;
		float pltbestcompare = 0.0F;
	    for (PrefixLeg plt : prefixlegsto)
		{
			if (((mpnstart == null) || (mpnstart == plt.op.pnstart)) && ((mpnend == null) || (mpnend == plt.op.pnend)))
			{
				float pltcompare = plf.CompareDirection(plt);
				if ((pltbest == null) || (pltcompare > pltbestcompare))
				{
					pltbest = plt;
					pltbestcompare = pltcompare;
				}
			}
		}
		return pltbest;
	}

	/////////////////////////////////////////////
	boolean NodeMappingPut(OnePathNode opnf, OnePathNode opnt)
	{
		OnePathNode lopnt = nodemapping.get(opnf);
		if (lopnt == null)
			nodemapping.put(opnf, opnt);
		else if (lopnt != opnt)   // returns false
			return !TN.emitWarning("NodeMappingPut mismatch:" + opnt.pnstationlabel + " " + lopnt.pnstationlabel); 
        return true; 
	}

	/////////////////////////////////////////////
	// need to find a mapping from centrelines in one to the other.
	boolean CorrespMatching(List<OnePath> vpathsfrom, List<OnePath> vpathsto)
	{
        // add the paths into the prefixlegs structures
		for (OnePath opt : vpathsto)
		{
			if ((opt.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opt.plabedl != null) && opt.plabedl.IsCentrelineType())
				prefixlegsto.add(new PrefixLeg(opt));
		}
		for (OnePath opf : vpathsfrom)
		{
			if ((opf.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opf.plabedl != null) && opf.plabedl.IsCentrelineType())
				prefixlegsfrom.add(new PrefixLeg(opf));
        }

        // make the likely mapping of blocks
		Map<String, String> blockmapping = GetBlockMapping();
		if (blockmapping.size() == 0)
			return false;

        // make the lookup table for centreline names
        Map<String, PrefixLeg> tolegnamemap = new HashMap<String, PrefixLeg>(); 
        for (PrefixLeg plt : prefixlegsto)
            tolegnamemap.put(plt.pnlabblocktail + "." + plt.pnlabstationtail + "  " + plt.pnlabblockhead + "." + plt.pnlabstationhead, plt); 

		for (PrefixLeg plf : prefixlegsfrom)
		{
            String tpnlabblocktail = blockmapping.get(plf.pnlabblocktail); 
            String tpnlabblockhead = blockmapping.get(plf.pnlabblockhead); 
            if ((tpnlabblocktail != null) && (tpnlabblockhead != null)) 
            {
                String toleglookup = tpnlabblocktail + "." + plf.pnlabstationtail + "  " + tpnlabblockhead + "." + plf.pnlabstationhead; 
                PrefixLeg plt = tolegnamemap.remove(toleglookup); 
                if (plt != null)
				{
                    plf.pltmember = plt;
                    if (!NodeMappingPut(plf.op.pnstart, plt.op.pnstart))
                        return false; 
                    if (!NodeMappingPut(plf.op.pnend, plt.op.pnend))
                        return false; 
                }
                else
    				prefixlegsfromunmatched.add(plf);
            }
            else
    			prefixlegsfromunmatched.add(plf);
        }
		TN.emitMessage("SMS+  " + prefixlegsfromunmatched.size() + "  " + prefixlegsfrom.size());

	
		while (true)
		{
            int prefixlegsfromunmatchedsize = prefixlegsfromunmatched.size();
            for (int i = prefixlegsfromunmatchedsize - 1; i >= 0; i--)
			{
				PrefixLeg plf = prefixlegsfromunmatched.get(i);
				PrefixLeg plt = IncreCorresp(plf);
				if (plt != null)
				{
					plf.pltmember = plt;
					if (!NodeMappingPut(plf.op.pnstart, plt.op.pnstart))
                        return false; 
					if (!NodeMappingPut(plf.op.pnend, plt.op.pnend))
                        return false; 
				}
				prefixlegsfromunmatched.remove(i);
			}
            if (prefixlegsfromunmatched.isEmpty() || (prefixlegsfromunmatched.size() == prefixlegsfromunmatchedsize))
            	break;
		}

		System.out.println("SSS+  " + prefixlegsfromunmatched.size() + "  " + prefixlegsfrom.size());
		return true;
	}
}

