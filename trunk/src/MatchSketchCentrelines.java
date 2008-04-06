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
	String prefix;
	OnePath op;
	String pnlabtail;
	String pnlabhead;
	float vx;
	float vy;
	PrefixLeg plt = null;

	PrefixLeg(OnePath lop)
	{
		op = lop;
		pnlabtail = op.plabedl.centrelinetail.replaceAll("[|^]", ".");
		pnlabhead = op.plabedl.centrelinehead.replaceAll("[|^]", ".");
		vx = (float)(op.pnend.pn.getX() - op.pnstart.pn.getX());
		vy = (float)(op.pnend.pn.getY() - op.pnstart.pn.getY());
	}

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

	float CompareDir(PrefixLeg pl)
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
/////////////////////////////////////////////
class MatchSketchCentrelines
{
    List<PrefixLeg> prefixlegsto = new ArrayList<PrefixLeg>();
    List<PrefixLeg> prefixlegsfrom = new ArrayList<PrefixLeg>();

	List<PrefixLeg> prefixlegsfromleft = new ArrayList<PrefixLeg>();
	Map<OnePathNode, OnePathNode> nodemapping = new HashMap<OnePathNode, OnePathNode>();


	/////////////////////////////////////////////
	class PrefixCount implements Comparable<PrefixCount>
	{
		String prefix;
		float score = 0.0F;
		PrefixCount(String lprefix)
		{
			prefix = lprefix;
		}
		public int compareTo(PrefixCount opc)
		{
			return ((score < opc.score) ? +1 : (score > opc.score ? -1 : 0));
		}
	}

	/////////////////////////////////////////////
	String GetProbablePrefix(List<OnePath> vpathsfrom, List<OnePath> vpathsto)
	{
		// not very smart n^2 algorithm
		for (OnePath opt : vpathsto)
		{
			if ((opt.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opt.plabedl != null))
				prefixlegsto.add(new PrefixLeg(opt));
		}

		Map<String, PrefixCount> prefixcounts = new HashMap<String, PrefixCount>();
		for (OnePath opf : vpathsfrom)
		{
			if ((opf.linestyle == SketchLineStyle.SLS_CENTRELINE) && (opf.plabedl != null))
			{
				PrefixLeg plf = new PrefixLeg(opf);
				prefixlegsfrom.add(plf);

				for (PrefixLeg plt : prefixlegsto)  // the n^2 loop thing
				{
					String prefix = plt.FindCommonPrefix(plf);
					if (prefix != null)
					{
						PrefixCount prefixcount = prefixcounts.get(prefix);
						if (prefixcount == null)
						{
							prefixcount = new PrefixCount(prefix);
							prefixcounts.put(prefix, prefixcount);
						}
						prefixcount.score += plt.CompareDir(plf);
					}
				}
			}
		}

		List<PrefixCount> prefixcountlist = new ArrayList<PrefixCount>();
		prefixcountlist.addAll(prefixcounts.values());
		Collections.sort(prefixcountlist);

		for (PrefixCount prefixcount : prefixcountlist)
			System.out.println("PPP: " + prefixcount.score + "  " + prefixcount.prefix);
		return (!prefixcountlist.isEmpty() ? prefixcountlist.get(0).prefix : null);
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
				float pltcompare = plf.CompareDir(plt);
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
	void NodeMappingPut(OnePathNode opnf, OnePathNode opnt)
	{
		OnePathNode lopnt = nodemapping.get(opnf);
		if (lopnt == null)
			nodemapping.put(opnf, opnt);
		else
			assert lopnt == opnt;
	}

	/////////////////////////////////////////////
	// need to find a mapping from centrelines in one to the other.
	boolean CorrespMatching(List<OnePath> vpathsfrom, List<OnePath> vpathsto)
	{
		String prefix = GetProbablePrefix(vpathsfrom, vpathsto);
		if (prefix == null)
			return false;

		for (PrefixLeg plf : prefixlegsfrom)
		{
			for (PrefixLeg plt : prefixlegsto)
			{
				String lprefix = plt.FindCommonPrefix(plf);
				if ((lprefix != null) && prefix.equals(lprefix))
				{
					plf.plt = plt;
					NodeMappingPut(plf.op.pnstart, plt.op.pnstart);
					NodeMappingPut(plf.op.pnend, plt.op.pnend);
					break;
				}
			}
			if (plf.plt == null)
				prefixlegsfromleft.add(plf);
		}

		while (true)
		{
            int prefixlegsfromleftsize = prefixlegsfromleft.size();
            for (int i = prefixlegsfromleftsize - 1; i >= 0; i--)
			{
				PrefixLeg plf = prefixlegsfromleft.get(i);
				PrefixLeg plt = IncreCorresp(plf);
				if (plt != null)
				{
					plf.plt = plt;
					NodeMappingPut(plf.op.pnstart, plt.op.pnstart);
					NodeMappingPut(plf.op.pnend, plt.op.pnend);
				}
				prefixlegsfromleft.remove(i);
			}
            if (prefixlegsfromleft.isEmpty() || (prefixlegsfromleft.size() == prefixlegsfromleftsize))
            	break;
		}

		System.out.println("SSS+  " + prefixlegsfromleft.size() + "  " + prefixlegsfrom.size());
		return true;
	}
}

