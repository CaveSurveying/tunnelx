/////////////////////////////////////////////////////////////////////////////////
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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

import java.util.Random;
import java.util.Date;

import java.awt.geom.AffineTransform; 
import java.awt.geom.NoninvertibleTransformException; 
import java.awt.geom.Area; 
import java.awt.geom.Rectangle2D; 

//
//
// AtlasGenerator
//
//


/////////////////////////////////////////////
class TSublevelmapping implements Comparable<TSublevelmapping>
{
	String subcolour; 
	Set<String> subsets = new HashSet<String>(); 

	int npaths = 0; 
	float zlo = 0.0F; 
	float zhi = 0.0F; 

	List<OneSArea> vsareas = new ArrayList<OneSArea>(); 
	Area uarea = new Area(); 

	Set<String> adjoiningsubsetsbelow = new HashSet<String>(); 
	Set<String> adjoiningsubsetsabove = new HashSet<String>(); 
	
	/////////////////////////////////////////////
	void AbsorbRangeZ(float z)
	{
		if (npaths == 0)
		{
			zlo = z; 
			zhi = z; 
		}
		else if (z < zlo)
			zlo = z; 
		else if (z > zhi)
			zhi = z; 
		npaths++; 
	}
	TSublevelmapping(String lsubcolour)
	{
		subcolour = lsubcolour; 
	}

	public int compareTo(TSublevelmapping tsl)
	{
		float d = zlo + zhi - (tsl.zlo + tsl.zhi); 
		if (d < 0.0F)
			return +1; 
		if (d > 0.0F)
			return -1; 
		return 0; 
	}

	/////////////////////////////////////////////
	void FindAdjoiningSubsets(Rectangle2D rectframeRS, TSublevelmapping tslabove, TSublevelmapping tslbelow)
	{
		assert adjoiningsubsetsbelow.isEmpty() && adjoiningsubsetsabove.isEmpty(); 
		for (OneSArea osa : vsareas)
		{
			for (RefPathO rpo : osa.refpathsub)
			{
				if (rpo.op.gp.intersects(rectframeRS))
				for (String subset : rpo.op.vssubsets)
				{
					if ((tslabove != null) && tslabove.subsets.contains(subset))
						adjoiningsubsetsabove.add(subset); 
					if ((tslbelow != null) && tslbelow.subsets.contains(subset))
						adjoiningsubsetsbelow.add(subset); 
				}
			}
		}
System.out.println("aaajoininin " + adjoiningsubsetsabove.size() + " " + adjoiningsubsetsbelow.size()); 
	}
}
	

/////////////////////////////////////////////
class AtlasGenerator
{
	List<OnePath> vpathsatlas = new ArrayList<OnePath>(); // output
	
	String commonsubset; 

	// primary frame and its paths within it
	OneSArea osaframe = null; 
	SketchFrameDef sketchframedef = null; // should handle as a list osaframe.sketchframedefs
	List<OnePath> opsframe = new ArrayList<OnePath>(); 
	
	// the border overlap frame
	OneSArea osaframeborder = null; 
	SketchFrameDef sketchframedefborder = null; // should handle as a list osaframe.sketchframedefs
	
	// find survex data to use as mapping from *title 
	Map<String, String> subsetexplorermap = new HashMap<String, String>(); 

	AffineTransform pframesketchtransinverse; 
	Area areaframeRS;  // in real space

	Set<OnePath> pathspresent = new HashSet<OnePath>(); 
	Set<OneSArea> areaspresent = new HashSet<OneSArea>(); 
	Set<String> subsetspresent = new HashSet<String>(); 

	// information per level that the subsets all map into
	// the mapping is sketchframedef.submapping
	// for multiple sketchframedefs we can contatenate a hashkey for each sketchframedef to make the mapping unique
	Map<String, TSublevelmapping> subcolmap = new HashMap<String, TSublevelmapping>(); 


	/////////////////////////////////////////////
	static String FindCommonSubset(OneSketch asketch)
	{
		// find the primary subset
		Set<String> commonsubsets = new HashSet<String>(); 
		for (OnePath op : asketch.vpaths)
		{
			if (!commonsubsets.isEmpty())
			{
				commonsubsets.retainAll(op.vssubsets); 
				if (commonsubsets.isEmpty())
					return null; 
			}
			else
				commonsubsets.addAll(op.vssubsets); 
		}
		return commonsubsets.iterator().next();  
	}
	
	/////////////////////////////////////////////
	void SetExplorerMap(OneSketch fsketch)
	{
		String survexstring = ""; 
		for (OnePath op : fsketch.vpaths)
		{
			if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE) && (op.plabedl != null) && (op.plabedl.sfontcode != null) && op.plabedl.sfontcode.equals("survey")) 
			{
				if (op.plabedl.drawlab.length() > survexstring.length())
					survexstring = op.plabedl.drawlab; 
			}
		}
		
		// parse the survex string
		SurvexLoaderNew sln = new SurvexLoaderNew();
		sln.InterpretSvxText(survexstring);

		for (OneLeg ol : sln.vlegs)
		{
			if ((ol.svxtitle.length() > 0) && (subsetexplorermap.get(ol.svxtitle) == null))
				subsetexplorermap.put(ol.svxtitle, ol.svxtitle + ": " + ol.svxteam + " " + ol.svxdate); 
		}
for (String s : subsetexplorermap.values())
	System.out.println("exexexexe  " + s); 
	}	

	/////////////////////////////////////////////
	String FindCommonSubsetA(OneSArea osa)
	{
		//Set<String> commonsubsets = new HashSet<String>(); 
		Set<String> commonsubsets = new HashSet<String>(sketchframedef.submapping.keySet()); 
		for (RefPathO rpo : osa.refpathsub)
		{
			if (!commonsubsets.isEmpty())
			{
				commonsubsets.retainAll(rpo.op.vssubsets); 
				if (commonsubsets.isEmpty())
					return null; 
			}
			else
				commonsubsets.addAll(rpo.op.vssubsets); 
		}
		return commonsubsets.iterator().next();  
	}

	/////////////////////////////////////////////
	// works by looking for the most complex submapping
	boolean FindPrimaryFrame(OneSketch asketch)
	{
		// find the primary frame and its subset mapping
		int nsubsetscomp = 0; 
		assert osaframe == null; 
		for (OneSArea osa : asketch.vsareas)
		{
			// should also make sure of pframesketch is not null (not of an image)
			if (osa.iareapressig != SketchLineStyle.ASE_SKETCHFRAME) 
				continue; 
			Set<String> Ssubsets = new HashSet<String>(); 
			for (SketchFrameDef lsketchframedef : osa.sketchframedefs)
				Ssubsets.addAll(lsketchframedef.submapping.values()); 
			if ((osaframe == null) || (nsubsetscomp < Ssubsets.size()))
			{
				osaframe = osa; 
				nsubsetscomp = Ssubsets.size(); 
			}
			
			if ((Ssubsets.size() == 1) && Ssubsets.contains("strongrey"))
				osaframeborder = osa; 
		}
		if (osaframe == null)
			return false; 

		if (osaframeborder != null)
			System.out.println("***** borderframe detected"); 
		if (osaframeborder == osaframe)
			osaframeborder = null; 

		for (OnePath op : asketch.vpaths)
			if (op.IsSketchFrameConnective() && (op.kaleft == osaframe))
				opsframe.add(op); 
		return true; 
	}
	

	/////////////////////////////////////////////
	// this works for a single frame area; must be extended to multiple images in the same area
	void MakeFrameSubmapping(OneSketch asketch)
	{
		sketchframedef = osaframe.sketchframedefs.get(0); 

		try { pframesketchtransinverse = sketchframedef.pframesketchtrans.createInverse(); }
		catch (NoninvertibleTransformException e)  {;}
	
		for (Map.Entry<String, String> esubset : sketchframedef.submapping.entrySet())
		{
			TSublevelmapping tsl = subcolmap.get(esubset.getValue()); 
			if (tsl == null)
			{
				tsl = new TSublevelmapping(esubset.getValue()); 
				subcolmap.put(esubset.getValue(), tsl); 
			}
			tsl.subsets.add(esubset.getKey()); 
		}
	}


	/////////////////////////////////////////////
	List<TSublevelmapping> FileIntoSublevelmapping()
	{
		for (TSublevelmapping tsl : subcolmap.values())
		{
			tsl.npaths = 0; 
			tsl.vsareas.clear(); 
			tsl.uarea.reset(); 
			tsl.adjoiningsubsetsbelow.clear(); 
			tsl.adjoiningsubsetsabove.clear(); 
		}

		for (OnePath op : pathspresent)
		{
			for (String subset : op.vssubsets)
			{
				String subcol = sketchframedef.submapping.get(subset); 
				if (subcol != null)
				{
					TSublevelmapping tsl = subcolmap.get(subcol); 
					tsl.AbsorbRangeZ(op.pnstart.zalt); 
					tsl.AbsorbRangeZ(op.pnend.zalt); 
				}
			}
		}

		for (OneSArea osa : areaspresent)
		{
			String subset = FindCommonSubsetA(osa); 
			if (subset != null)
			{
				String subcol = sketchframedef.submapping.get(subset); 
				if (subcol != null)
				{
					TSublevelmapping tsl = subcolmap.get(subcol); 
					tsl.vsareas.add(osa); 
					tsl.uarea.add(osa.aarea); 
					// absorb the range for the area
					//tsl.AbsorbRangeZ(op.pnstart.zalt); 
					//tsl.AbsorbRangeZ(op.pnend.zalt); 
				}
			}
		}

		// find the levels that actually have any elements
		List<TSublevelmapping> res = new ArrayList<TSublevelmapping>(); 
		for (TSublevelmapping tsl : subcolmap.values())
			if (tsl.npaths != 0)
				res.add(tsl); 
		Collections.sort(res); 

		return res; 
	}


	// improve the z-ordering estimation
	
	// improve overlap estimation to handle simple adjoining of levels

	// trawl through survey data to find which teams and dates when 
	// selected sections were surveyed

	// this will be done by looking through all centrelines present in the sketch and 
	// listing all mappings in subsetexplorermap that go from their title set subsets 
	// (which can be assembled from the visible) and substituting the text into 
	// label of type: titlesurveyors
	
	// trawl through photo library which will be tagged with same subsets 
	// that are active so we can allocate them automatically, avoiding duplicates

	// handle the minimap which will be at a 
	// different scale and needs to avoid being offset by 125m and have separate zones 
	// when we run over.


	/////////////////////////////////////////////
	void FindAreasPathsPresent(Rectangle2D rectframeRS, OneSketch fsketch)
	{
		// discover the paths and subsets in the framed sketch
		pathspresent.clear(); 
		areaspresent.clear(); 

		for (OneSArea osa : fsketch.vsareas)
		{
			if (osa.aarea.intersects(rectframeRS))
			{
				areaspresent.add(osa); 
				for (RefPathO rpo : osa.refpaths)
				{
					if (rpo.op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
						pathspresent.add(rpo.op); 
				}
			}
		}


// also look for subset pair adjacencies (edges that span two levels) and their adjacent subsets (areas)

		for (OnePath op : fsketch.vpaths)
			if ((op.linestyle != SketchLineStyle.SLS_CONNECTIVE) && op.gp.intersects(rectframeRS))  // do we have to worry about labels?
				pathspresent.add(op); 
				
		for (OnePath op : pathspresent)
			subsetspresent.addAll(op.vssubsets); 
		subsetspresent.retainAll(sketchframedef.submapping.keySet()); 

	}
			

	/////////////////////////////////////////////
	void CopySketchDisplacedLayer(float xdisp, float ydisp, String newcommonsubset, OneSketch asketch, Set<String> retainsubsets, Set<String> greyedsubsets, Set<String> brightgreysubsets) 
	{
		Map<OnePathNode, OnePathNode> opnmap = new HashMap<OnePathNode, OnePathNode>(); 
		for (OnePathNode opn : asketch.vnodes)
			opnmap.put(opn, new OnePathNode((float)opn.pn.getX() + xdisp, (float)opn.pn.getY() + ydisp, opn.zalt)); 

		for (OnePath op : asketch.vpaths)
		{
			OnePath lop = new OnePath(opnmap.get(op.pnstart)); 
			float[] pco = op.GetCoords();
			for (int i = 1; i < op.nlines; i++)
				lop.LineTo(pco[i * 2 + 0] + xdisp, pco[i * 2 + 1] + ydisp);
			lop.EndPath(opnmap.get(op.pnend)); 
			lop.CopyPathAttributes(op); 
			lop.vssubsets.remove(commonsubset); 
			lop.vssubsets.add(newcommonsubset); 
			if (lop.bWantSplined && !OnePath.bHideSplines)
				lop.Spline(lop.bWantSplined, false);

			// limit down the subset mapping
			if (op.IsSketchFrameConnective())
			{
				// primary frame
				if (op.kaleft == osaframe)
				{
					// remove and change the values in here
					Map<String, String> submapping = lop.plabedl.sketchframedef.submapping; 
					Set<String> subsets = new HashSet<String>(submapping.keySet()); 
					for (String subset : subsets)
					{
						if (brightgreysubsets.contains(subset))
							submapping.put(subset, "brightgrey"); 
						else if (greyedsubsets.contains(subset))
							submapping.put(subset, "greyed"); 
						else if (!retainsubsets.contains(subset))
							submapping.remove(subset); 
					}
					submapping.put("default", "obscuredsets"); 
				}	
				else if (op.kaleft == osaframeborder)
				{
					Map<String, String> submapping = lop.plabedl.sketchframedef.submapping; 
					submapping.clear(); 
					for (String subset : sketchframedef.submapping.keySet())
					{
						if (greyedsubsets.contains(subset))
							submapping.put(subset, "greyed"); 
						else if (retainsubsets.contains(subset))
							submapping.put(subset, "strongrey"); 
					}
					submapping.put("default", "obscuredsets"); 
				}
				else
					;
			}

			vpathsatlas.add(lop); 
		}
	}
	
	
	static String alphabet = "abcdefghijklmnopqerstuvwxyz"; 
	/////////////////////////////////////////////
	boolean CopySketchDisplaced(float xdisp, float ydisp, String newcommonsubset, OneSketch asketch)
	{
System.out.println("Gnerating::: " + newcommonsubset); 
		// translate the framed area and then transform into real space (the space of the paths of asketch)
		Area aareatranslate = osaframe.aarea.createTransformedArea(AffineTransform.getTranslateInstance(xdisp, ydisp)); 
		areaframeRS = aareatranslate.createTransformedArea(pframesketchtransinverse); 
		Rectangle2D rectframeRS = areaframeRS.getBounds2D(); 
		
		// discover the paths and subsets in the framed sketch
		FindAreasPathsPresent(rectframeRS, sketchframedef.pframesketch); 
System.out.println("ppp " + pathspresent.size() + " a " + areaspresent.size()); 
		if (pathspresent.isEmpty())
			return TN.emitWarning("   Skipping this atlas page"); 
		
		List<TSublevelmapping> tsllist = FileIntoSublevelmapping(); 

		for (int i = 0; i < tsllist.size(); i++)
			tsllist.get(i).FindAdjoiningSubsets(rectframeRS, (i != 0 ? tsllist.get(i - 1) : null), (i < tsllist.size() - 1 ? tsllist.get(i + 1) : null)); 


		System.out.println("tttt  " + tsllist.size()); 

		int lic = 0; // level count
		int i = 0; 
		while (i < tsllist.size())
		{
			Set<String> retainsubsets = new HashSet<String>(); 
			Set<String> greyedsubsets = new HashSet<String>(); 
			Set<String> brightgreysubsets = new HashSet<String>(); 
			retainsubsets.addAll(tsllist.get(i).subsets); 

			// find if we can collapse with next level down
			int i1 = i + 1; 
			while (i1 < tsllist.size())
			{
				Area larea = new Area(tsllist.get(i).uarea); 
				for (int li = i + 1; li < i1; li++)
					larea.add(tsllist.get(li).uarea); 

// intersecting collapsed levels -- must check if it's just on the border part, because otherwise too sensitive
				larea.intersect(tsllist.get(i1).uarea); 
				if (!larea.isEmpty())
					break; 
				System.out.println("collapsinglevels " + i + " " + i1); 
				retainsubsets.addAll(tsllist.get(i1).subsets); 
				i1++; 
			}
			
			for (int j = i1; j < tsllist.size(); j++)
				greyedsubsets.addAll(tsllist.get(j).subsets); 
					
			if (i1 < tsllist.size())
				brightgreysubsets.addAll(tsllist.get(i1).adjoiningsubsetsbelow); 
			
			CopySketchDisplacedLayer(xdisp, ydisp, newcommonsubset + "_" + alphabet.substring(lic, lic + 1), asketch, retainsubsets, greyedsubsets, brightgreysubsets); 
			lic++; 
			i = i1; 
		}
		return true; 
	}


	/////////////////////////////////////////////
	// take the sketch from the displayed window and import it from the selected sketch in the mainbox.
	boolean ImportAtlasTemplate(OneSketch asketch)
	{
		commonsubset = FindCommonSubset(asketch); 
		if (commonsubset == null)
			return TN.emitWarning("No common subset"); 
System.out.println("commonsubset: " + commonsubset + "  nareas " + asketch.vsareas.size()); 
		if (!FindPrimaryFrame(asketch))
			return TN.emitWarning("Primary frame failure"); 
		MakeFrameSubmapping(asketch); 
		SetExplorerMap(sketchframedef.pframesketch); 

		for (int it = -5; it <= 5; it++)
		for (int jt = -5; jt <= 5; jt++)
		{
			float xdisp = it * 125.0F / 500.0F * 1000.0F * TN.CENTRELINE_MAGNIFICATION; 
			float ydisp = jt * 125.0F / 500.0F * 1000.0F * TN.CENTRELINE_MAGNIFICATION; 
			String newcommonsubset = "page_" + it + "_" + jt; 
			CopySketchDisplaced(xdisp, ydisp, newcommonsubset, asketch); 
		}
		return true; 	
	}
}	


