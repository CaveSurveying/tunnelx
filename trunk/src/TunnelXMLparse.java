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
// FoUndation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.geom.Line2D;
import java.util.Vector;
import java.awt.Font;
import java.awt.Color;
import java.awt.geom.AffineTransform;


/////////////////////////////////////////////
class TunnelXMLparse extends TunnelXMLparsebase
{
	OneTunnel vgsymbols;

	OneTunnel tunnel;
	String fnamess;
	int iftype;  // the type from TunnelXML

	Vector ssba = new Vector(); // the structure with a list of symbols in an aut-symbol. -- should be another class.
	String autsymbdname;
	String autsymbdesc;
	int iautsymboverwrite;  // 0 no button, 1 overwrite, 2 append


	// set directly after the constructor is called
	boolean bSymbolType = false;
	SketchLineStyle sketchlinestyle = null; // always there if it's a symbol type
	SubsetAttrStyle subsetattributestyle = null;
	SubsetAttr subsetattributes = null;
	SketchGrid sketchgrid = null;

    /////////////////////////////////////////////
	// sketch type loading
	OneSketch tunnelsketch = null;
	Vector lvnodes = new Vector();

	// sketch path loading
	OnePath sketchpath = null;
	int sketchpath_ind0;
	int sketchpath_ind1;
	int skpnpoints;
	float skpX;
	float skpY;
	float skpZ;
	boolean skpZset;

	float skpXo;
	float skpYo;


	// xsection loading
	OneSection xsection = null;
	int xsectionindex = -1;

	// set when pulling in labels where the raw xml should be copied over.
	int isblabelstackpos = -1;
	boolean bTextType; // should be deprecated.
	StringBuffer sblabel = new StringBuffer();

	int posfixtype = 0; // 1 for fix and 2 for pos_fix

	/////////////////////////////////////////////
	OnePathNode LoadSketchNode(int ind)
	{
		while (lvnodes.size() <= ind)
			lvnodes.addElement(null);

		OnePathNode res;
		if (lvnodes.elementAt(ind) == null)
		{
			res = new OnePathNode(skpX, skpY, skpZ, skpZset);
			lvnodes.setElementAt(res, ind);
		}
		else
		{
			res = (OnePathNode)lvnodes.elementAt(ind);
			if (((float)res.pn.getX() != skpX) || ((float)res.pn.getY() != skpY))
				TN.emitWarning("Node mismatch value: " + (float)res.pn.getX() + " = " + skpX + ",   " + (float)res.pn.getY() + " = " + skpY);
			if ((float)res.zalt != skpZ)
				TN.emitWarning("ZNode mismatch value: " + res.zalt + " = " + skpZ);
		}
		return res;
	}


	/////////////////////////////////////////////
	Color SeStackColour(String name, Color defalt)
	{
		String coldef = SeStack(name);
		if (coldef == null)
			return defalt;
		if (coldef.equals("none"))
			return null;
		if (!coldef.startsWith("#"))
			TN.emitError("Colour value should be hex starting with #");
		int col = (int)Long.parseLong(coldef.substring(1), 16);
		return new Color(col, ((col & 0xff000000) != 0));
	}


	/////////////////////////////////////////////
	public void startElementAttributesHandled(String name, boolean binlineclose)
	{
		// copy into label stuff if one is live.
		// this is code in a label (now obsolete because I mangle the text as it goes out)
		if (isblabelstackpos != -1)
		{
			// re-build the
			// hand-make this case back into proper form!!
			if (name.equals(TNXML.sLRSYMBOL))
			{
				sblabel.append("<");
				sblabel.append(TNXML.sLRSYMBOL);
				sblabel.append(" ");
				sblabel.append(TNXML.sLRSYMBOL_NAME);
				sblabel.append("=\"");
				sblabel.append(SeStack(TNXML.sLRSYMBOL_NAME));
				sblabel.append("\"/>");
			}
			else
				sblabel.append(TNXML.xcomopen(0, name));
  		}

		// go through the possible commands
		else if (name.equals(TNXML.sLAUT_SYMBOL))
		{
			assert iftype == FileAbstraction.FA_FILE_XML_FONTCOLOURS;

			// till we make a special class, the list of symbols in an aut-symbol is
			// a list with first element a string.
			ssba.removeAllElements();
            autsymbdname = SeStack(TNXML.sLAUT_SYMBOL_NAME);
            autsymbdesc = SeStack(TNXML.sLAUT_DESCRIPTION);

			String sbuttonaction = SeStack(TNXML.sLAUT_BUTTON_ACTION, TNXML.sLAUT_OVERWRITE);
            iautsymboverwrite = (sbuttonaction.equals(TNXML.sLAUT_OVERWRITE) ? 1 : (sbuttonaction.equals(TNXML.sLAUT_APPEND) ? 2 : 0));
		}


	// values from the fontcolours.xml file
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE))
		{
			assert subsetattributestyle == null;
			String subsetattrstylename = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_NAME);
			for (int i = 0; i < sketchlinestyle.subsetattrstyles.size(); i++)
			{
				if (subsetattrstylename.equals(((SubsetAttrStyle)sketchlinestyle.subsetattrstyles.elementAt(i)).stylename))
				{
					System.out.println("Removing subsetattribute style of duplicate: " + subsetattrstylename);
					sketchlinestyle.subsetattrstyles.removeElementAt(i);
					break;
				}
			}
			boolean bselectable = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_SELECTABLE, "yes").equals("yes");
			subsetattributestyle = new SubsetAttrStyle(subsetattrstylename, bselectable);
		}

		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE_IMPORT))
		{
			assert subsetattributes == null;
			String sasname = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_NAME);
			SubsetAttrStyle lsas = sketchlinestyle.GetSubsetAttrStyle(sasname);
			if (lsas != null)
				subsetattributestyle.ImportSubsetAttrStyle(lsas);
			else
				TN.emitError("Could not find Subset Attr Style: " + sasname);
		}

		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES))
		{
			// get the subset attributes
			if (subsetattributes != null)
				TN.emitError("subset def inside subset def " + SeStack(TNXML.sSUBSET_NAME));

			subsetattributes = subsetattributestyle.FindSubsetAttr(SeStack(TNXML.sSUBSET_NAME), true);

			// use default value to map through non-overwritten attributes
			subsetattributes.uppersubset = SeStack(TNXML.sUPPER_SUBSET_NAME, subsetattributes.uppersubset);
			subsetattributes.sareamaskcolour = SeStack(TNXML.sSUBSET_AREAMASKCOLOUR, subsetattributes.sareamaskcolour);
			subsetattributes.sareacolour = SeStack(TNXML.sSUBSET_AREACOLOUR, subsetattributes.sareacolour);
		}

		else if (name.equals(TNXML.sSET_ATTR_VARIABLE))
		{
			assert subsetattributes != null;
			subsetattributes.SetVariable(SeStack(TNXML.sATTR_VARIABLE_NAME), SeStack(TNXML.sATTR_VARIABLE_VALUE));
		}

		else if (name.equals(TNXML.sLABEL_STYLE_FCOL))
		{
			assert subsetattributes != null;
			LabelFontAttr lfa = subsetattributes.FindLabelFont(SeStack(TNXML.sLABEL_STYLE_NAME), true);
			sketchlinestyle.AddToFontList(lfa);

			lfa.sfontname = SeStack(TNXML.sLABEL_FONTNAME, null);
			lfa.sfontstyle = SeStack(TNXML.sLABEL_FONTSTYLE, null);
			lfa.slabelcolour = SeStack(TNXML.sLABEL_COLOUR, null);
			lfa.sfontsize = SeStack(TNXML.sLABEL_FONTSIZE, null);
		}
		else if (name.equals(TNXML.sLINE_STYLE_COL))
		{
			assert subsetattributes != null;
			String slinestyle = SeStack(TNXML.sSK_LINESTYLE);
			int llinestyle = TNXML.DecodeLinestyle(slinestyle);
			subsetattributes.SetLinestyleAttr(llinestyle, SeStack(TNXML.sLS_STROKEWIDTH), SeStack(TNXML.sLS_SPIKEGAP), SeStack(TNXML.sLS_GAPLENG), SeStack(TNXML.sLS_SPIKEHEIGHT), SeStack(TNXML.sLS_STROKECOLOUR), SeStack(TNXML.sLS_SHADOWSTROKEWIDTH, "0"), SeStack(TNXML.sLS_SHADOWSTROKECOLOUR, null));
		}

		// these are per attribute style
		else if (name.equals(TNXML.sGRID_DEF))
		{
			assert subsetattributestyle != null;
			subsetattributestyle.sketchgrid = new SketchGrid(Float.parseFloat(SeStack(TNXML.sGRID_XORIG)), Float.parseFloat(SeStack(TNXML.sGRID_YORIG)));
			sketchgrid = subsetattributestyle.sketchgrid;
		}
		else if (name.equals(TNXML.sGRID_SPACING))
		{
			float fspacing = Float.parseFloat(SeStack(TNXML.sGRID_SPACING_WIDTH));
			assert sketchgrid.ngridspacing < sketchgrid.gridspacing.length;
			assert (sketchgrid.ngridspacing == 0) || (fspacing > sketchgrid.gridspacing[sketchgrid.ngridspacing - 1]);
			sketchgrid.gridspacing[sketchgrid.ngridspacing] = fspacing;
			sketchgrid.gridlineslimit[sketchgrid.ngridspacing] = Integer.parseInt(SeStack(TNXML.sMAX_GRID_LINES));
			sketchgrid.ngridspacing++;
		}

		// these are on their own
		else if (name.equals(TNXML.sAREA_SIG_DEF))
		{
			SketchLineStyle.areasignames[SketchLineStyle.nareasignames] = SeStack(TNXML.sAREA_SIG_NAME);
			String lasigeffect = SeStack(TNXML.sAREA_SIG_EFFECT);
			// this magic value gets maximized around the contour
			int isigeffect = 0;
			if (lasigeffect.equals(TNXML.sASIGNAL_KEEPAREA))
				isigeffect = 0;
			else if (lasigeffect.equals(TNXML.sASIGNAL_KILLAREA))
				isigeffect = 3;
			else if (lasigeffect.equals(TNXML.sASIGNAL_OUTLINEAREA))
				isigeffect = 2;
			else if (lasigeffect.equals(TNXML.sASIGNAL_HCOINCIDE))
				isigeffect = 1;
			else if (lasigeffect.equals(TNXML.sASIGNAL_SKETCHFRAME))
				isigeffect = 55;
			else
				TN.emitWarning("Unrecognized area signal " + lasigeffect);
			SketchLineStyle.areasigeffect[SketchLineStyle.nareasignames] = isigeffect;
			SketchLineStyle.nareasignames++;
		}

		// the directory names where images can be stored
		else if (name.equals(TNXML.sIMAGE_FILE_DIRECTORY))
			SketchBackgroundPanel.AddImageFileDirectory(SeStack(TNXML.sIMAGE_FILE_DIRECTORY_NAME));

		// go through the possible commands
		else if (name.equals(TNXML.sMEASUREMENTS))
			assert iftype == FileAbstraction.FA_FILE_XML_MEASUREMENTS;

		else if (name.equals(TNXML.sEXPORTS))
		{
			assert iftype == FileAbstraction.FA_FILE_XML_EXPORTS;
			assert tunnel.exportfile != null;
		}

		// the replacement of labels
		else if (name.equals(TNXML.sPATHCODES))
		{
			// make the label decode object
			sketchpath.plabedl = new PathLabelDecode();
		}

		// the replacement of labels
		else if (name.equals(TNXML.sPC_TEXT))
		{
			// make the label decode object
			isblabelstackpos = istack - 1;
			bTextType = false;

			sketchpath.plabedl.sfontcode = SeStack(TNXML.sLTEXTSTYLE, "default");

			sketchpath.plabedl.fnodeposxrel = Float.parseFloat(SeStack(TNXML.sPC_NODEPOSXREL, "-1.0"));
			sketchpath.plabedl.fnodeposyrel = Float.parseFloat(SeStack(TNXML.sPC_NODEPOSYREL, "-1.0"));
			sketchpath.plabedl.barrowpresent = SeStack(TNXML.sPC_ARROWPRES, "0").equals("1");
			sketchpath.plabedl.bboxpresent = SeStack(TNXML.sPC_BOXPRES, "0").equals("1");
		}

		else if (name.equals(TNXML.sCL_STATIONS))
		{
			sketchpath.plabedl.tail = SeStack(TNXML.sCL_TAIL);
			sketchpath.plabedl.head = SeStack(TNXML.sCL_HEAD);
		}

		else if (name.equals(TNXML.sPC_AREA_SIGNAL))
		{
			String arpres = SeStack(TNXML.sAREA_PRESENT);
			sketchpath.plabedl.iarea_pres_signal = -1;
			for (int i = 0; i < sketchlinestyle.nareasignames; i++)
				if (arpres.equals(sketchlinestyle.areasignames[i]))
					sketchpath.plabedl.iarea_pres_signal = i;
			if (sketchpath.plabedl.iarea_pres_signal == -1)
			{
				TN.emitWarning("Unrecognized area signal:" + arpres);
				sketchpath.plabedl.iarea_pres_signal = 0;
			}
			sketchpath.plabedl.barea_pres_signal = sketchlinestyle.areasigeffect[sketchpath.plabedl.iarea_pres_signal];

			if (sketchpath.plabedl.barea_pres_signal == 55)
			{
				sketchpath.plabedl.sfscaledown = Float.parseFloat(SeStack(TNXML.sASIG_FRAME_SCALEDOWN));
				sketchpath.plabedl.sfrotatedeg = Float.parseFloat(SeStack(TNXML.sASIG_FRAME_ROTATEDEG));
				sketchpath.plabedl.sfxtrans = Float.parseFloat(SeStack(TNXML.sASIG_FRAME_XTRANS));
				sketchpath.plabedl.sfytrans = Float.parseFloat(SeStack(TNXML.sASIG_FRAME_YTRANS));
				sketchpath.plabedl.sfsketch = SeStack(TNXML.sASIG_FRAME_SKETCH);
				sketchpath.plabedl.sfstyle = SeStack(TNXML.sASIG_FRAME_STYLE);
			}
		}

		// the symbols
		else if (name.equals(TNXML.sPC_RSYMBOL))
			sketchpath.plabedl.vlabsymb.addElement(SeStack(TNXML.sLRSYMBOL_NAME));

		// deprecated
		else if (name.equals(TNXML.sLABEL))
		{
			TN.emitWarning("deprecated label type");
			isblabelstackpos = istack - 1;
			bTextType = false;
		}

		// deprecated
		else if (name.equals(TNXML.sTEXT))
		{
			TN.emitWarning("deprecated text type");
			isblabelstackpos = istack - 1;
			bTextType = true;
		}


		// <export estation="1" ustation="insignificant.8"/>
		else if (name.equals(TNXML.sEXPORT))
		{
			tunnel.vexports.addElement(new OneExport(SeStack(TNXML.sEXPORT_FROM_STATION), SeStack(TNXML.sEXPORT_TO_STATION)));

			// early versions leave out the exports tag
			assert iftype == FileAbstraction.FA_FILE_XML_EXPORTS;
		}

		// open a sketch
		else if (name.equals(TNXML.sSKETCH))
		{
			assert iftype == FileAbstraction.FA_FILE_XML_SKETCH;
			assert tunnelsketch != null;
			lvnodes.removeAllElements();
			assert tunnelsketch.bSymbolType == bSymbolType;
		}

		// open a xsection
		else if (name.equals(TNXML.sXSECTION))
		{
			xsection = new OneSection(SeStack(TNXML.sXS_STATION0), SeStack(TNXML.sXS_STATION1), Float.parseFloat(SeStack(TNXML.sXS_STATION_LAM)), SeStack(TNXML.sXS_STATION_ORIENT_FORE), SeStack(TNXML.sXS_STATION_ORIENT_BACK), SeStack(TNXML.sXS_STATION_ORIENT_REL_COMPASS), SeStack(TNXML.sXS_STATION_ORIENT_CLINO));
			xsectionindex = Integer.parseInt(SeStack(TNXML.sXSECTION_INDEX));
		}

		// make a tube
		else if (name.equals(TNXML.sLINEAR_TUBE))
		{
			int xind0 = Integer.parseInt(SeStack(TNXML.sFROM_XSECTION));
			int xind1 = Integer.parseInt(SeStack(TNXML.sTO_XSECTION));
			tunnel.vtubes.addElement(new OneTube((OneSection)(tunnel.vsections.elementAt(xind0)), (OneSection)(tunnel.vsections.elementAt(xind1))));
		}

		// open a posfix (not input as are the legs not input).
		else if (name.equals(TNXML.sFIX))
			posfixtype = 1;
		else if (name.equals(TNXML.sPOS_FIX))
			posfixtype = 2;

		// aut-symbols thing
		else if (name.equals(TNXML.sLASYMBOL))
		{
			SSymbolBase ssb = new SSymbolBase();

			// decode the mcode attributes on the symbol
			// this provides the interface between the xml and the values in the base class.
			String sscale = SeStack(TNXML.sLAUT_SYMBOL_SCALE, TNXML.sLAUT_SYMBOL_ALONGAXIS);
			ssb.bScaleable = !sscale.equals(TNXML.sLAUT_SYMBOL_FIXED);
			ssb.bShrinkby2 = sscale.equals(TNXML.sLAUT_SYMBOL_ANDHALF);
			if (ssb.bShrinkby2)
				ssb.bScaleable = false;

			ssb.fpicscale = Float.parseFloat(SeStack(TNXML.sLAUT_SYMBOL_PICSCALE, "1.0"));
			ssb.faxisscale = Float.parseFloat(SeStack(TNXML.sLAUT_SYMBOL_AXISSCALE, "1.0"));
			ssb.faxisscaleperp = Float.parseFloat(SeStack(TNXML.sLAUT_SYMBOL_AXISSCALEPERP, "1.0"));

			String sorientation = SeStack(TNXML.sLAUT_SYMBOL_ORIENTATION, TNXML.sLAUT_SYMBOL_ALONGAXIS);
			ssb.bRotateable = !sorientation.equals(TNXML.sLAUT_SYMBOL_FIXED);
			if (sorientation.equals(TNXML.sLAUT_SYMBOL_RANDOM))
				ssb.posangledeviation = -1.0F;
			else if (sorientation.equals(TNXML.sLAUT_SYMBOL_ALONGAXIS))
				ssb.posangledeviation = 0.0F;
			else if (sorientation.equals(TNXML.sLAUT_SYMBOL_ALONGAXIS_PERP))
				ssb.posangledeviation = -2.0F;
			else if (sorientation.equals(TNXML.sLAUT_SYMBOL_NEARAXIS))
				ssb.posangledeviation = 0.1F;

			// position setting 
			String sposition = SeStack(TNXML.sLAUT_SYMBOL_POSITION, TNXML.sLAUT_SYMBOL_ENDPATH);
			
			ssb.bMoveable = !sposition.equals(TNXML.sLAUT_SYMBOL_ENDPATH);
			ssb.iLattice = (sposition.equals(TNXML.sLAUT_SYMBOL_LATTICEF) ? 2 : (sposition.equals(TNXML.sLAUT_SYMBOL_LATTICE) ? 1 : 0));
			ssb.bPullback = sposition.equals(TNXML.sLAUT_SYMBOL_PULLBACK);
			ssb.bPushout = sposition.equals(TNXML.sLAUT_SYMBOL_PUSHOUT);
			ssb.posdeviationprop = (ssb.bMoveable && (ssb.iLattice == 0) ? (ssb.bPullback ? 2.0F : 1.0F) : 0.0F);

			String aint = SeStack(TNXML.sLAUT_SYMBOL_AINT, TNXML.sLAUT_SYMBOL_AINT_NO_OVERLAP);
			ssb.bAllowedOutsideArea = !aint.equals(TNXML.sLAUT_SYMBOL_AINT_NO_OVERLAP);
			ssb.bTrimByArea = aint.equals(TNXML.sLAUT_SYMBOL_AINT_TRIM);
			ssb.bSymbolinterferencedoesntmatter = ssb.bAllowedOutsideArea;

			ssb.gsymname = SeStack(TNXML.sLSYMBOL_NAME);
			String smultiplicity = SeStack(TNXML.sLAUT_SYMBOL_MULTIPLICITY, "1");
			ssb.nmultiplicity = (smultiplicity.equals(TNXML.sLAUT_SYMBOL_MULTIPLICITY_FILL) ? -1 : Integer.parseInt(smultiplicity));

			ssb.symbolareafillcolour = SeStackColour(TNXML.sLAUT_SYMBOL_AREA_FILL, null);

			// we build a lattice in area fills or lattice types
			ssb.bBuildSymbolLatticeAcrossArea = (sposition.equals(TNXML.sLAUT_SYMBOL_LATTICEF) || sposition.equals(TNXML.sLAUT_SYMBOL_LATTICE) || smultiplicity.equals(TNXML.sLAUT_SYMBOL_MULTIPLICITY_FILL)); 
			ssb.bSymbolLatticeAcrossAreaPhased = (sposition.equals(TNXML.sLAUT_SYMBOL_LATTICEF) || smultiplicity.equals(TNXML.sLAUT_SYMBOL_MULTIPLICITY_FILL)); 
			ssb.bBuildSymbolSpreadAlongLine = (sposition.equals(TNXML.sLAUT_SYMBOL_ALONGPATH_RANDOM_PULLBACK) || sposition.equals(TNXML.sLAUT_SYMBOL_ALONGPATH_EVEN)); 
			ssb.bSymbolLayoutOrdered = (sposition.equals(TNXML.sLAUT_SYMBOL_LATTICEF) || sposition.equals(TNXML.sLAUT_SYMBOL_LATTICE) || sposition.equals(TNXML.sLAUT_SYMBOL_ALONGPATH_EVEN)); 
			if (ssb.bBuildSymbolSpreadAlongLine)
				ssb.posdeviationprop = (sposition.equals(TNXML.sLAUT_SYMBOL_ALONGPATH_RANDOM_PULLBACK) ? 1.0 : 0.0); 
			if (sposition.equals(TNXML.sLAUT_SYMBOL_ALONGPATH_RANDOM_PULLBACK))
			{
				ssb.bPullback = true;
				ssb.bSymbolLayoutOrdered = true; 
				ssb.posdeviationprop = 0.1F; 
			}
			ssb.bOrientClosestAlongLine = sorientation.equals(TNXML.sLAUT_SYMBOL_CLOSESTFROMAXIS); 
			ssb.bOrientClosestPerpLine = sorientation.equals(TNXML.sLAUT_SYMBOL_CLOSESTALONGAXIS); 

			// first entry of this vector is a string of the name
			ssba.addElement(ssb);
		}


		// as a batch
		//static String sTAPE = "tape";
		//static String sCOMPASS = "compass";
		//static String sCLINO = "clino";

		// sketch things
		else if (name.equals(TNXML.sSKETCH_PATH))
		{
			sketchpath_ind0 = Integer.parseInt(SeStack(TNXML.sFROM_SKNODE));
			sketchpath_ind1 = Integer.parseInt(SeStack(TNXML.sTO_SKNODE));

			sketchpath = new OnePath();
			skpnpoints = 0;
			sketchpath.linestyle = TNXML.DecodeLinestyle(SeStack(TNXML.sSK_LINESTYLE));
			sketchpath.bWantSplined = (Integer.parseInt(SeStack(TNXML.sSPLINED)) != 0);
		}

		// subset markers
		else if (name.equals(TNXML.sSKSUBSET))
			sketchpath.vssubsets.addElement(SeStack(TNXML.sSKSNAME));
		else if (name.equals(TNXML.sSKIMPORTFROM))
			sketchpath.importfromname = SeStack(TNXML.sSKSNAME);

		else if (name.equals(TNXML.sPOINT))
		{
			skpX = Float.parseFloat(SeStack(TNXML.sPTX));
			skpY = Float.parseFloat(SeStack(TNXML.sPTY));
			String sptz = SeStack(TNXML.sPTZ);
			skpZset = (sptz != null);
			skpZ = (skpZset ? Float.parseFloat(sptz) : 0.0F);

			if (sketchpath != null)
			{
				if (skpnpoints == 0)
				{
					sketchpath.gp.moveTo(skpX, skpY);
					sketchpath.pnstart = LoadSketchNode(sketchpath_ind0);
				}
				else
					sketchpath.LineTo(skpX, skpY);
				skpnpoints++;
			}

			else if (xsection != null)
			{
				xsection.AddNode(new Vec3(skpX, skpY, skpZ));
			}

			else if (posfixtype != 0)
				; // nothing input for now.

			// supress errors on these no longer existent types
			else if (ElStack("symbol"))
				;

			else
			{
				StackDump();
				System.out.println("in tunnel: " + tunnel.name);
				TN.emitWarning("point without an object");
				System.exit(0);
			}
		}

		else if (name.equals(TNXML.sSKETCH_BACK_IMG))
		{
			AffineTransform lbackgimgtrans = null;
			if (ElStack(TNXML.sAFFINE_TRANSFORM))
				lbackgimgtrans = new AffineTransform(DeStack(TNXML.sAFTR_M00), DeStack(TNXML.sAFTR_M10), DeStack(TNXML.sAFTR_M01), DeStack(TNXML.sAFTR_M11), DeStack(TNXML.sAFTR_M20), DeStack(TNXML.sAFTR_M21));
			int libackgroundimgnamearrsel = tunnelsketch.AddBackground(SeStack(TNXML.sSKETCH_BACK_IMG_FILE),lbackgimgtrans);
			if (SeStack(TNXML.sSKETCH_BACK_IMG_FILE_SELECTED, "0").equals("1"))
			{
				tunnelsketch.ibackgroundimgnamearrsel = libackgroundimgnamearrsel;
				System.out.println("defaulting the background image " + libackgroundimgnamearrsel); 
			}
		}
	}

	/////////////////////////////////////////////
	public void characters(String pstr)
	{
		// whitespace that shouldn't comes through here.
		assert pstr != null;
		if (isblabelstackpos != -1)
		{
			// deprecated
			if (bTextType)
			{
				String txt = pstr;
				int ip = pstr.indexOf("%%");
				if (ip != -1)
				{
					sblabel.append(TNXML.xcomtext(TNXML.sTAIL, pstr.substring(0, ip)));
					sblabel.append(TNXML.xcomtext(TNXML.sHEAD, pstr.substring(ip + 2)));
				}
			}
			else
				sblabel.append(pstr);
		}
	}


	/////////////////////////////////////////////
	public void endElementAttributesHandled(String name)
	{
		// middle or ending of labels.
		if (isblabelstackpos != -1)
		{
			// ending.
			if (isblabelstackpos == istack)
			{
				if (name.equals(TNXML.sPC_TEXT))
				{
					sketchpath.plabedl.drawlab = TNXML.xunmanglxmltext(sblabel.toString());
					sblabel.setLength(0);
					isblabelstackpos = -1;
				}

				else if (name.equals(TNXML.sLABEL))
				{
					// this is where labels are at present added.
					sketchpath.plabedl = new PathLabelDecode(TNXML.xunmanglxmltext(sblabel.toString()), sketchlinestyle);
					sblabel.setLength(0);
					isblabelstackpos = -1;
				}

				else
				{
					if (!bTextType || !name.equals(TNXML.sTEXT))
						TN.emitProgError("Stack pos doesn't match label position");
					else
						TN.emitWarning("Deprecated TEXT type used for label");
				}
			}

			// the closing thing already done earlier (note the !)
			else if (!name.equals(TNXML.sLRSYMBOL))
				sblabel.append(TNXML.xcomclose(0, name));
		}

		// ending of other items.
		else if (name.equals(TNXML.sSKETCH))
		{
			assert OnePathNode.CheckAllPathCounts(tunnelsketch.vnodes, tunnelsketch.vpaths);
			tunnelsketch = null;
			iftype = FileAbstraction.FA_FILE_UNKNOWN;  // so only one in
		}
		else if (name.equals(TNXML.sEXPORTS))
			iftype = FileAbstraction.FA_FILE_UNKNOWN;
		else if (name.equals(TNXML.sMEASUREMENTS))
			iftype = FileAbstraction.FA_FILE_UNKNOWN;


		else if (name.equals(TNXML.sSKETCH_PATH))
		{
			sketchpath.EndPath(LoadSketchNode(sketchpath_ind1));
			tunnelsketch.TAddPath(sketchpath, vgsymbols);
			if (!tunnelsketch.bSymbolType && (sketchpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (sketchpath.plabedl != null))
				sketchpath.UpdateStationLabelsFromCentreline();
			sketchpath = null;
		}


		else if (name.equals(TNXML.sFIX) || name.equals(TNXML.sPOS_FIX))
			posfixtype = 0;

		else if (name.equals(TNXML.sXSECTION))
		{
			if (tunnel.vsections.size() != xsectionindex)
				TN.emitWarning("XSection Index not consistent"); // won't help with the tubes
			tunnel.vsections.addElement(xsection);
			xsection = null;
		}

		else if (name.equals(TNXML.sLAUT_SYMBOL))
		{
			// we have aut symbols in the same tunnel as the sketch symbols
			if (iautsymboverwrite != 0)  // 0 is the nobutton type (for out of date symbols)
				sketchlinestyle.symbolsdisplay.AddSymbolButton(autsymbdname, autsymbdesc, (iautsymboverwrite == 1));
			assert (subsetattributes != null);
			SymbolStyleAttr ssa = subsetattributes.FindSymbolSpec(autsymbdname, 1);
			ssa.ssymbolbs = new Vector();
			ssa.ssymbolbs.addAll(ssba);
		}

		// used for the fontcolours
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE))
		{
		    //subsetattributestyle.FillAllMissingAttributes(); // this shouldn't happen till we're all through
			sketchlinestyle.subsetattrstyles.addElement(subsetattributestyle);
			sketchlinestyle.bsubsetattributestoupdate = true;
			subsetattributestyle = null;
		}
		else if (name.equals(TNXML.sGRID_DEF))
		{
			assert sketchgrid.ngridspacing != 0;
			sketchgrid = null;
		}
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES))
			subsetattributes = null;
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	TunnelXMLparse(OneTunnel lvgsymbols)
	{
		vgsymbols = lvgsymbols;
	}

	/////////////////////////////////////////////
	void SetUp(OneTunnel ltunnel, String lfnamess, int liftype)
	{
		SetUpBase();

		tunnel = ltunnel;
		fnamess = lfnamess;
		iftype = liftype;

		tunnelsketch = null;
		sketchpath = null;

		xsection = null;
		xsectionindex = -1;

		isblabelstackpos = -1;
		sblabel.setLength(0);

		posfixtype = 0;
	}
};


