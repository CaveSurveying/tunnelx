////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2006  Martin Green, Julian Todd
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
import java.io.IOException;
import java.lang.String;

import java.util.List;
import java.util.ArrayList;

class SVGView
{
   private float tunnelunit = 0.1F; //length of tunnel unit in meters
	private float xoffset = 0F;
	private float yoffset = 0F;
	private int id = 0; //The next id to use
	public SVGView(LineOutputStream los, Vector vpaths, List<OneSArea> vsareas, boolean bHideCentreline, boolean bWallwhiteoutlines) throws IOException
   {
		WriteHeader(los);
		WritePathsNonAreaNoLabels(vpaths, bHideCentreline, los);
		WriteAreas(vsareas,bWallwhiteoutlines, los);
		WriteLabels(vpaths, los);
		WriteFooter(los);
	}

	// open and close
	void WriteHeader(LineOutputStream los) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		los.WriteLine(TNXML.xcomopen(0, "svg", "xmlns", "http://www.w3.org/2000/svg", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", "A Tunnel View"));
		los.WriteLine(TNXML.xcomtext(1, "desc", "This file links to areas.svg, paths.svg and symbols.svg"));
	}

	void WriteFooter(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}

	void WritePathsNonAreaNoLabels(Vector vpaths, boolean bHideCentreline, LineOutputStream los) throws IOException
	{
		// check any paths if they are now done
		for (int j = 0; j < vpaths.size(); j++)
		{
			OnePath op = (OnePath)vpaths.elementAt(j);
			op.ciHasrendered = 0;
			if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
			{
				op.pnstart.pathcountch++;
				op.pnend.pathcountch++;
				op.ciHasrendered = 2;
				continue;
			}

			// path belongs to an area
			if ((op.karight != null) || (op.kaleft != null))
				continue;

			// no shadows are painted on unarea types
			op.pnstart.pathcountch++;
			op.pnend.pathcountch++;
			op.ciHasrendered = 3;

			if (bHideCentreline && (op.linestyle == SketchLineStyle.SLS_CENTRELINE))
				continue;

			// the rest of the drawing of this path with quality
			WritePath(op, los);
		}
	}
	void WritePath(OnePath op, LineOutputStream los) throws IOException
	{
		// non-drawable
		if (op.linestyle == SketchLineStyle.SLS_INVISIBLE)
			return;
		if ((op.linestyle == SketchLineStyle.SLS_CONNECTIVE))
			return;//Add in symbols and text drawing here
		if (op.subsetattr.linestyleattrs[op.linestyle] == null)
		{
			TN.emitWarning("subset linestyle attr for " + op.linestyle + " missing for "+ op.subsetattr.subsetname);
			return;
		}

		if (op.subsetattr.linestyleattrs[op.linestyle].strokecolour == null)
			return; // hidden

		// set the stroke
		assert op.subsetattr.linestyleattrs[op.linestyle].linestroke != null;

		//g2D.setStroke(subsetattr.linestyleattrs[linestyle].linestroke);

		// special spiked type things
		if (op.subsetattr.linestyleattrs[op.linestyle].spikeheight != 0.0F)
		{
		//Add in spikey stuff here
		}

		// other visible strokes
		else
		los.WriteLine(TNXML.xcom(1, "use", "xlink:href", "paths.svg#"+op.svgid));
	}

	void WriteAreas(List<OneSArea> vsareas, boolean bWallwhiteoutlines, LineOutputStream los) throws IOException
	{
		for (int i = 0; i < vsareas.size(); i++)
		{
/*			OneSArea osa = (OneSArea)vsareas.elementAt(i);
			
			//Write grouping with subsets
			Vector vssubsets = new Vector();
			osa.DecideSubsets(vssubsets);
			String ssubsets = "";
			for (int j = 0; j < vssubsets.size(); j++)
			{
				ssymbols = ssymbols + " " +op.linestyle == SketchLineStyle.SLS_INVISIBLE) (String)vssubsets.elementAt(j);
			}

			los.WriteLine(TNXML.xcomopen(1, "g", "class", ssymbols);
			WriteAreaDef(osa);

			// draw the wall type strokes related to this area
			// this makes the white boundaries around the strokes !!!
			if (bWallwhiteoutlines)
			{
				for (int j = 0; j < osa.refpathsub.size(); j++)
				{
					RefPathO rop = (RefPathO)osa.refpathsub.elementAt(j);

					DrawAreaOutlines(rop.op);
					DrawJoiningPaths(rop.ToNode(), true);
				}
			}


//As far as I have got
			// fill the area with a diffuse colour (only if it's a drawing kind)
			if (!bRestrictSubsetCode || osa.bareavisiblesubset)
			{
				if ((osa.iareapressig == 0) || (osa.iareapressig == 1))
					pwqFillArea(g2D, osa);
				if (osa.iareapressig == SketchLineStyle.ASE_SKETCHFRAME)
					pwqFramedSketch(g2D, osa, vgsymbols);
			}

			assert !osa.bHasrendered;
			osa.bHasrendered = true;file:///usr/share/ubuntu-artwork/home/index.html
			pwqSymbolsOnArea(g2D, osa);
			pwqPathsOnAreaNoLabels(g2D, osa, null);
		*/}
	}
	void WriteLabels(Vector vpaths, LineOutputStream los) throws IOException
	{
	}
}
