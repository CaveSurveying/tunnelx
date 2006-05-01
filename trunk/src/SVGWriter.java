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
import java.awt.geom.PathIterator;

class SVGWriter
{
	private float tunnelunit = 0.1F; //length of tunnel unit in meters
	private float xoffset = 0F;
	private float yoffset = 0F;
	private int iaid = 0; //The next area id to use
	private int ipid = 0; //The next path id to use
	private int isid = 0; //The next symbol id to use

	void SVGPaths(LineOutputStream los, Vector vpaths) throws IOException
   {
		WriteHeader(los,"Tunnels Paths","This file solely contains the definitions of paths for Tunnel, you need a view.svg file to see anything.");
		WriteStartDef(los);
		for (int j = 0; j < vpaths.size(); j++)
		{
			WritePath(los, (OnePath)vpaths.elementAt(j), true);//True means set Id
		}
		WriteEndDef(los);
		WriteFooter(los);
	}
	void SVGAreas(LineOutputStream los, Vector vareas) throws IOException
   {
		WriteHeader(los, "Tunnels Areas", "This file solely contains the calculated areas for Tunnel, you need a view.svg file to see anything.");
		WriteStartDef(los);
		for (int j = 0; j < vareas.size(); j++)
		{
			WriteArea(los, (OneSArea)vareas.elementAt(j), true);//True means set Id
		}
		WriteEndDef(los);
		WriteFooter(los);
	}
	void SVGSymbols(LineOutputStream los, OneTunnel vgsymbols) throws IOException
   {
		WriteHeader(los, "Tunnels Symbols", "This file solely contains the symbols for Tunnel, you need a view.svg file to see anything.");
		WriteStartDef(los);
		for (int j = 0; j < vgsymbols.tsketches.size(); j++)
		{
			WriteSymbol(los, (OneSketch)vgsymbols.tsketches.elementAt(j), true);//True means set Id
		}
		WriteEndDef(los);
		WriteFooter(los);
	}
	void SVGView(LineOutputStream los, OneTunnel vgsymbols, Vector vpaths, Vector vareas, boolean bHideCentreline, boolean bWallwhiteoutlines) throws IOException
   {
		WriteHeader(los, "Tunnels Image", "Standalone Image");
		WriteStartDef(los);
		for (int j = 0; j < vgsymbols.tsketches.size(); j++)
		{
			WriteSymbol(los, (OneSketch)vgsymbols.tsketches.elementAt(j), true);//True means set Id
		}
		for (int j = 0; j < vpaths.size(); j++)
		{
			WritePath(los, (OnePath)vpaths.elementAt(j), true);//True means set Id
		}
		for (int j = 0; j < vareas.size(); j++)
		{
			WriteArea(los, (OneSArea)vareas.elementAt(j), true);//True means set Id
		}
		WriteEndDef(los);
		WriteRefBottomPaths(vpaths, bHideCentreline, los);
		WriteRefAreasWithPaths(vareas,bWallwhiteoutlines, los);
		WriteRefLabels(vpaths, los);
		WriteFooter(los);
	}

	///////////////////////////////////////////////////////////////////////////////
	// open and close
	void WriteHeader(LineOutputStream los, String title, String desc) throws IOException
	{
		TNXML.chconvleng = TNXML.chconv.length - 2; // a complete hack to stop &space; getting in here

		los.WriteLine("<?xml version=\"1.0\" standalone=\"no\"?>\n");
		los.WriteLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		los.WriteLine("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		los.WriteLine(TNXML.xcomopen(0, "svg", "xmlns", "http://www.w3.org/2000/svg", "xmlns:xlink", "http://www.w3.org/1999/xlink", "version", "1.1"));
		los.WriteLine(TNXML.xcomtext(1, "title", title));
		los.WriteLine(TNXML.xcomtext(1, "desc", desc));
	}

	void WriteStartDef(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(1, "defs"));
	}

	void WriteEndDef(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomclose(1, "defs"));
	}

	void WriteFooter(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomclose(0, "svg"));
		TNXML.chconvleng = TNXML.chconv.length;
	}

///////////////////////////////////////////////////////////////////////////////
	static float[] coords = new float[6]; //Used to get the position of line segments
	void WriteArea(LineOutputStream los, OneSArea oa, boolean bid) throws IOException
	{
		Vector vparams = new Vector();
		if (bid)
		{
			//Set svg id to area
			String said = new String("a" + String.valueOf(this.iaid));
			this.iaid=this.iaid+1;
			oa.setId(said);
			vparams.addElement("id");
			vparams.addElement(said);
		}
		//Generate list of classes
		String classes = new String("");
		for (int j = 0; j < oa.vssubsetattrs.size(); j++)
		{
			if(j!=0) classes = classes + " ";
			classes = classes + ((SubsetAttr)oa.vssubsetattrs.elementAt(j)).subsetname;//Why does this not work?
		}
		if (classes.length() > 0)
			{
				vparams.addElement("class");
				vparams.addElement(classes);
			}
		//Generate d the list of commands to generate points
		vparams.addElement("d");
		vparams.addElement(GetD(oa.gparea.getPathIterator(null)));

		//Get zalt, probably should check if it has been set...
		vparams.addElement("z");
		vparams.addElement(String.valueOf(oa.zalt));

		//Write line
		los.WriteLine(TNXML.xcom(2, "path", vparams));
	}
///////////////////////////////////////////////////////////////////////////////
	void WritePath(LineOutputStream los, OnePath op, boolean bid) throws IOException
	{
		WritePath(los, op, this.xoffset, this.yoffset, bid);
	}
	void WritePath(LineOutputStream los, OnePath op, float xoffset, float yoffset, boolean bid) throws IOException
	{
		Vector vparams = new Vector();
		if (bid)
		{
			//Set svg id to path
			String spid = new String("p" + String.valueOf(this.ipid));
			this.ipid=this.ipid+1;
			op.setId(spid);
			vparams.addElement("id");
			vparams.addElement(spid);
		}		
		//Generate list of linestyles and classes
		String classes = new String(SketchLineStyle.shortlinestylenames[op.linestyle]);
		for (int j = 0; j < op.vssubsets.size(); j++)
		{
			classes = classes + " " + SketchLineStyle.shortlinestylenames[op.linestyle] + (String)op.vssubsets.elementAt(j);
		}
		if (classes!="")//This if should allways be true, perhaps it should be removed.
			{
				vparams.addElement("class");
				vparams.addElement(classes);
			}
		//Generate d the list of commands to generate points
		vparams.addElement("d");
		vparams.addElement(GetD(op.gp.getPathIterator(null), xoffset, yoffset));

		//Set parameters and attributes based on if the heights are set
		int numparam=0;
		if (op.pnstart.bzaltset)
		{
			vparams.addElement("z0");
			vparams.addElement(String.valueOf(op.pnstart.zalt));
			vparams.addElement("z1");
			vparams.addElement(String.valueOf(op.pnend.zalt));
		}
		
		//Determine if the path has funny attributes eg Survey stations, text, symbols or areatypes
		if (op.plabedl!=null) 
		{
			los.WriteLine(TNXML.xcomopen(2, "path", vparams));
			op.plabedl.WriteXML(los,3,false);
			los.WriteLine(TNXML.xcomclose(2, "path"));
		}
		else
		{
			los.WriteLine(TNXML.xcom(2, "path", vparams));
		}
	}
////////////////////////////////////////////////////////////////////////////////
	void WriteSymbol(LineOutputStream los, OneSketch os, boolean bid) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(2, "g","id",os.sketchsymbolname));
		for (int j = 0; j < os.vpaths.size(); j++)
		{	
			float xoffset = 0F;
			float yoffset = 0F;
			WritePath(los, (OnePath)os.vpaths.elementAt(j), xoffset, yoffset, false);
			                                      //false refers to not making an id
		}		
		los.WriteLine(TNXML.xcomclose(2, "g"));
	}
/////////////////////////////////////////////////////////////////////////////////
	String GetD(PathIterator it)
	{
		return GetD(it, this.xoffset, this.yoffset);
	}

	String GetD(PathIterator it, float xoffset, float yoffset)
	{
		String d = new String();

		for (int j=0;!it.isDone();j=1)
		{
			if(j!=0) d = d + " ";
			int type = it.currentSegment(coords);//coords of the segment are returned
			if (type == PathIterator.SEG_MOVETO)
			{
				d = d + "M" + (coords[0] - xoffset) + " " + (coords[1] - yoffset);
			}
			else if (type == PathIterator.SEG_LINETO)
			{
				d = d + " L" + (coords[0] - xoffset) + " " + (coords[1] - yoffset);
			}
			else if (type == PathIterator.SEG_CUBICTO)
			{
				d = d + " C" + (coords[0] - xoffset) + " " + (coords[1] - yoffset) + " " + (coords[2] - xoffset) + " " + (coords[3] - yoffset) + " " + (coords[4] - xoffset) + " " + (coords[5] - yoffset);
			}
			it.next();
		}
		return d;
	}
	void WriteRefBottomPaths(Vector vpaths, boolean bHideCentreline, LineOutputStream los) throws IOException
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
			WriteRefPath(op, los);
		}
	}
	void WriteRefAreasWithPaths(Vector vareas, boolean bWallwhiteoutlines, LineOutputStream los)
	{
/*		for (int i = 0; i < vsareas.size(); i++)
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i);

			// draw the wall type strokes related to this area
			// this makes the white boundaries around the strokes !!!
			if (bWallwhiteoutlines)
			{
				for (int j = 0; j < osa.refpathsub.size(); j++)
				{
					RefPathO rop = (RefPathO)osa.refpathsub.elementAt(j);

					WriteRefPath(rop.op, los, "WO");
					DrawJoiningPaths(rop.ToNode(), true);
				}
			}


//As far as I have got
			// fill the area with a diffuse colour (only if it's a drawing kind)
			if (!bRestrictSubsetCode || osa.bareavisiblesubset)
			{
				if ((osa.iareapressig == 0) || (osa.iareapressig == 1))
					pwqFillArea(g2D, osa);
				if (osa.iareapressig == 55)
					pwqFramedSketch(g2D, osa, vgsymbols);
			}

			assert !osa.bHasrendered;
			osa.bHasrendered = true;file:///usr/share/ubuntu-artwork/home/index.html
			pwqSymbolsOnArea(g2D, osa);
			pwqPathsOnAreaNoLabels(g2D, osa, null);
		}		*/
	}
	void WriteRefLabels(Vector vpaths, LineOutputStream los)
	{
	}
	void WriteRefPath(OnePath op, LineOutputStream los) throws IOException
	{
		WriteRefPath(op, los, "");
	}
	void WriteRefPath(OnePath op, LineOutputStream los, String additionalgroups) throws IOException
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
		los.WriteLine(TNXML.xcom(1, "use", "xlink:href", "#"+op.svgid, "group", additionalgroups));
	}
	void WriteRefArea(OnePath op, LineOutputStream los)
	{
	}
}
