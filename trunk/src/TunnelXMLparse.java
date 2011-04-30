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

import java.util.List;
import java.util.ArrayList;

import java.awt.Font;
import java.awt.Color;
import java.awt.geom.AffineTransform;


/////////////////////////////////////////////
class TunnelXMLparse extends TunnelXMLparsebase
{
	String fnamess;
	int iftype;  // the type from TunnelXML

	List<SSymbolBase> ssba = new ArrayList<SSymbolBase>(); // the structure with a list of symbols in an aut-symbol. -- should be another class.
	String autsymbdname;
	String autsymbdesc;
	int iautsymboverwrite;  // 0 no button, 1 overwrite, 2 append


	// set directly after the constructor is called
	boolean bSymbolType = false;
	SketchLineStyle sketchlinestyle = null; // always there if it's a symbol type
	SubsetAttrStyle subsetattributestyle = null;
	SubsetAttr subsetattributes = null;
	SketchGrid sketchgrid = null;
	SketchFrameDef sketchframedef = null; 

    /////////////////////////////////////////////
	// sketch type loading
	OneSketch tunnelsketch = null;
	List<OnePathNode> lvnodes = new ArrayList<OnePathNode>();

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

	// set when pulling in labels where the raw xml should be copied over.
	int isblabelstackpos = -1;
	boolean bTextType; // should be deprecated.
	StringBuffer sblabel = new StringBuffer();

	static int PFT_NONE = 0; 
	static int PFT_FIX = 1; 
	static int PFT_POSFIX = 2; 
	static int PFT_LEG = 3; 
	int posfixtype = PFT_NONE; 
	
	/////////////////////////////////////////////
	OnePathNode LoadSketchNode(int ind)
	{
		while (lvnodes.size() <= ind)
			lvnodes.add(null);

		OnePathNode res;
		if (lvnodes.get(ind) == null)
		{
			res = new OnePathNode(skpX, skpY, skpZ);
			lvnodes.set(ind, res);
		}
		else
		{
			res = lvnodes.get(ind);
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
	static int isasloadorder = 1000; 
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
			ssba.clear();
            autsymbdname = SeStack(TNXML.sLAUT_SYMBOL_NAME);
            autsymbdesc = SeStack(TNXML.sLAUT_DESCRIPTION);

			String sbuttonaction = SeStack(TNXML.sLAUT_BUTTON_ACTION, TNXML.sLAUT_OVERWRITE);
			if (sbuttonaction.equals(TNXML.sLAUT_OVERWRITE))
				iautsymboverwrite = 1; 
			else if (sbuttonaction.equals(TNXML.sLAUT_APPEND))
				iautsymboverwrite = 2; 
			else // TNXML.sLAUT_NOBUTTON
				iautsymboverwrite = 0; 
		}

	//
	// values from the fontcolours.xml file
	//
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE))
		{
			assert subsetattributestyle == null;
			String subsetattrstylename = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_NAME);

			// could use sketchlinestyle.GetSubsetSelection(String lstylename) here
			if (sketchlinestyle.subsetattrstylesmap.containsKey(subsetattrstylename))
			{
				TN.emitWarning("   ***   Removing subsetattribute style of duplicate: " + subsetattrstylename);
				sketchlinestyle.subsetattrstylesmap.remove(subsetattrstylename);
			}
			boolean bselectable = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_SELECTABLE, "yes").equals("yes");
			subsetattributestyle = new SubsetAttrStyle(subsetattrstylename, isasloadorder++, bselectable);
		}

		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE_IMPORT))
		{
			assert subsetattributes == null;
			String sasname = SeStack(TNXML.sSUBSET_ATTRIBUTE_STYLE_NAME);
			SubsetAttrStyle lsas = sketchlinestyle.subsetattrstylesmap.get(sasname);
			if (lsas != null)
				subsetattributestyle.ImportSubsetAttrStyle(lsas);
			else
				TN.emitError("Could not find Subset Attr Style: " + sasname);
		}

		// dual use of this as name when it was just a parameter
		else if (name.equals(TNXML.sASIGNAL_SKETCHFRAME) && (sketchframedef != null))
		{
			sketchframedef.sfscaledown = (float)DeStack(TNXML.sASIG_FRAME_SCALEDOWN, 1.0);
			sketchframedef.sfrotatedeg = (float)DeStack(TNXML.sASIG_FRAME_ROTATEDEG, 0.0);
			sketchframedef.sfelevrotdeg = (float)DeStack(TNXML.sASIG_FRAME_ELEVROTDEG, 0.0);
			sketchframedef.sfxtrans = DeStack(TNXML.sASIG_FRAME_XTRANS, 0.0);
			sketchframedef.sfytrans = DeStack(TNXML.sASIG_FRAME_YTRANS, 0.0);
			sketchframedef.sfsketch = SeStack(TNXML.sASIG_FRAME_SKETCH, "");
			sketchframedef.sfstyle = SeStack(TNXML.sASIG_FRAME_STYLE, "");
			sketchframedef.sfnodeconnzsetrelative = (float)DeStack(TNXML.sASIG_NODECONN_ZSETRELATIVE, 0.0);
		}

		// the sneaky parsing for a sketchframedef case
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES) && (sketchframedef != null))
		{
			String ssubset = SeStack(TNXML.sSUBSET_NAME);
			String uppersubset = SeStack(TNXML.sUPPER_SUBSET_NAME);
			sketchframedef.submapping.put(ssubset, uppersubset);
		}

		// second sneaky read-write of the data when inserted into a sketchpath.plabedl.sketchframedef
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES) && (sketchpath != null) && (sketchpath.plabedl != null) && (sketchpath.plabedl.sketchframedef != null))
		{
			String ssubset = SeStack(TNXML.sSUBSET_NAME);
			String uppersubset = SeStack(TNXML.sUPPER_SUBSET_NAME);
			sketchpath.plabedl.sketchframedef.submapping.put(ssubset, uppersubset);
		}

		// proper case when we're loading a font-colours
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES))
		{
			// get the subset attributes
			if (subsetattributes != null)
				TN.emitError("subset def inside subset def " + SeStack(TNXML.sSUBSET_NAME));

			String ssubset = SeStack(TNXML.sSUBSET_NAME);
			subsetattributes = subsetattributestyle.msubsets.get(ssubset);
			if (subsetattributes == null)
			{
				subsetattributes = new SubsetAttr(ssubset);
				subsetattributestyle.msubsets.put(ssubset, subsetattributes);
			}

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
			String llabelfontname = SeStack(TNXML.sLABEL_STYLE_NAME); 
			LabelFontAttr lfa = subsetattributes.labelfontsmap.get(llabelfontname); 
			if (lfa == null)
			{
				lfa = new LabelFontAttr(llabelfontname, subsetattributes); 
				//System.out.println("LLL  " + subsetattributes.subsetname + "  " + llabelfontname); 
				subsetattributes.labelfontsmap.put(llabelfontname, lfa); 
			}
			else
				TN.emitWarning("Over-writing data in font " + llabelfontname + " in subset " + subsetattributes.subsetname); 
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
			subsetattributestyle.sketchgrid = new SketchGrid((float)DeStack(TNXML.sGRID_XORIG), (float)DeStack(TNXML.sGRID_YORIG));
			sketchgrid = subsetattributestyle.sketchgrid;
		}
		else if (name.equals(TNXML.sGRID_SPACING))
		{
			float fspacing = (float)DeStack(TNXML.sGRID_SPACING_WIDTH);
			assert sketchgrid.ngridspacing < sketchgrid.gridspacing.length;
			assert (sketchgrid.ngridspacing == 0) || (fspacing > sketchgrid.gridspacing[sketchgrid.ngridspacing - 1]);
			sketchgrid.gridspacing[sketchgrid.ngridspacing] = fspacing;
			sketchgrid.gridlineslimit[sketchgrid.ngridspacing] = IeStack(TNXML.sMAX_GRID_LINES);
			sketchgrid.ngridspacing++;
		}

		// these are on their own
		else if (name.equals(TNXML.sAREA_SIG_DEF))
		{
            //System.out.println("sshshsh   " + SeStack(TNXML.sAREA_SIG_NAME) + "  " + SketchLineStyle.nareasignames); 
			SketchLineStyle.areasignames[SketchLineStyle.nareasignames] = SeStack(TNXML.sAREA_SIG_NAME);
			String lasigeffect = SeStack(TNXML.sAREA_SIG_EFFECT);
			// this magic value gets maximized around the contour
			int isigeffect = SketchLineStyle.ASE_KEEPAREA;
			if (lasigeffect.equals(TNXML.sASIGNAL_KEEPAREA))
				isigeffect = SketchLineStyle.ASE_KEEPAREA;
			else if (lasigeffect.equals(TNXML.sASIGNAL_KILLAREA))
				isigeffect = SketchLineStyle.ASE_KILLAREA;
			else if (lasigeffect.equals(TNXML.sASIGNAL_OUTLINEAREA))
				isigeffect = SketchLineStyle.ASE_OUTLINEAREA;
			else if (lasigeffect.equals(TNXML.sASIGNAL_HCOINCIDE))
				isigeffect = SketchLineStyle.ASE_HCOINCIDE;
			else if (lasigeffect.equals(TNXML.sASIGNAL_SKETCHFRAME))
				isigeffect = SketchLineStyle.ASE_SKETCHFRAME;
			else if (lasigeffect.equals(TNXML.sASIGNAL_ZSETRELATIVE))
				isigeffect = SketchLineStyle.ASE_ZSETRELATIVE;
			else if (lasigeffect.equals(TNXML.sASIGNAL_ELEVATIONPATH))
				isigeffect = SketchLineStyle.ASE_ELEVATIONPATH;
			else
				TN.emitWarning("Unrecognized area signal " + lasigeffect);

			if (isigeffect == SketchLineStyle.ASE_ELEVATIONPATH)
				SketchLineStyle.iareasigelev = SketchLineStyle.nareasignames;
			if (isigeffect == SketchLineStyle.ASE_SKETCHFRAME)
				SketchLineStyle.iareasigframe = SketchLineStyle.nareasignames;
			SketchLineStyle.areasigeffect[SketchLineStyle.nareasignames] = isigeffect;
			SketchLineStyle.nareasignames++;
		}

		// the directory names where images can be stored
		else if (name.equals(TNXML.sIMAGE_FILE_DIRECTORY))
			FileAbstraction.AddImageFileDirectory(SeStack(TNXML.sIMAGE_FILE_DIRECTORY_NAME));

		else if (name.equals(TNXML.sSURVEXEXEDIR))
		{
			if (!FileAbstraction.bIsApplet)
			{
				String lsurvexexecutabledir = SeStack(TNXML.sNAME); 
				if (FileAbstraction.isDirectory(lsurvexexecutabledir))
					TN.survexexecutabledir = lsurvexexecutabledir; 
			}
		}
		
		// go through the possible commands
		else if (name.equals(TNXML.sMEASUREMENTS))
			TN.emitError("We don't read measurements files anymore");

		else if (name.equals(TNXML.sEXPORTS))
			TN.emitError("We don't read Exports file anymore");

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

			sketchpath.plabedl.fnodeposxrel = (float)DeStack(TNXML.sPC_NODEPOSXREL, -1.0);
			sketchpath.plabedl.fnodeposyrel = (float)DeStack(TNXML.sPC_NODEPOSYREL, -1.0);
			sketchpath.plabedl.barrowpresent = SeStack(TNXML.sPC_ARROWPRES, "0").equals("1");
			sketchpath.plabedl.bboxpresent = SeStack(TNXML.sPC_BOXPRES, "0").equals("1");
		}

		else if (name.equals(TNXML.sCL_STATIONS))
		{
			sketchpath.plabedl.centrelinetail = SeStack(TNXML.sCL_TAIL, null);
			sketchpath.plabedl.centrelinehead = SeStack(TNXML.sCL_HEAD, null);
			sketchpath.plabedl.centrelineelev = SeStack(TNXML.sCL_ELEV, null);
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

			if (sketchpath.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME)
			{
				sketchpath.plabedl.sketchframedef = new SketchFrameDef();
				sketchpath.plabedl.sketchframedef.sfscaledown = (float)DeStack(TNXML.sASIG_FRAME_SCALEDOWN);
				sketchpath.plabedl.sketchframedef.sfrotatedeg = (float)DeStack(TNXML.sASIG_FRAME_ROTATEDEG);
				sketchpath.plabedl.sketchframedef.sfelevrotdeg = (float)DeStack(TNXML.sASIG_FRAME_ELEVROTDEG, 0.0);
				sketchpath.plabedl.sketchframedef.sfxtrans = DeStack(TNXML.sASIG_FRAME_XTRANS);
				sketchpath.plabedl.sketchframedef.sfytrans = DeStack(TNXML.sASIG_FRAME_YTRANS);
				sketchpath.plabedl.sketchframedef.sfsketch = SeStack(TNXML.sASIG_FRAME_SKETCH);
				sketchpath.plabedl.sketchframedef.sfstyle = SeStack(TNXML.sASIG_FRAME_STYLE);
				sketchpath.plabedl.sketchframedef.sfnodeconnzsetrelative = (float)DeStack(TNXML.sASIG_NODECONN_ZSETRELATIVE, 0.0);
				sketchpath.plabedl.sketchframedef.imagepixelswidth = IeStack(TNXML.sASIG_FRAME_IMGPIXELWIDTH, -1);
				sketchpath.plabedl.sketchframedef.imagepixelsheight = IeStack(TNXML.sASIG_FRAME_IMGPIXELHEIGHT, -1);
			}
			else if (sketchpath.plabedl.barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE)
				sketchpath.plabedl.nodeconnzsetrelative = (float)DeStack(TNXML.sASIG_NODECONN_ZSETRELATIVE);
		}

		// the symbols
		else if (name.equals(TNXML.sPC_RSYMBOL))
			sketchpath.plabedl.vlabsymb.add(SeStack(TNXML.sLRSYMBOL_NAME));

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


		// open a sketch
		else if (name.equals(TNXML.sSKETCH))
		{
			assert iftype == FileAbstraction.FA_FILE_XML_SKETCH;
			assert tunnelsketch != null;
			lvnodes.clear();
			assert tunnelsketch.bSymbolType == bSymbolType;
			
			if (SeStack(TNXML.sSKETCH_LOCOFFSETX) != null)
				tunnelsketch.sketchLocOffset.SetXYZ((float)DeStack(TNXML.sSKETCH_LOCOFFSETX), (float)DeStack(TNXML.sSKETCH_LOCOFFSETY), (float)DeStack(TNXML.sSKETCH_LOCOFFSETZ));
			if (SeStack(TNXML.sSKETCH_REALPAPERSCALE) != null)
				tunnelsketch.realpaperscale = DeStack(TNXML.sSKETCH_REALPAPERSCALE); 

            tunnelsketch.tunnelprojectloaded = SeStack(TNXML.sTUNNELPROJECT, ""); 
            tunnelsketch.tunnelversionloaded = SeStack(TNXML.sTUNNELVERSION, ""); 
            tunnelsketch.tunneldateloaded = SeStack(TNXML.sTUNNELDATE, ""); 
            tunnelsketch.tunneluserloaded = SeStack(TNXML.sTUNNELUSER, ""); 
		}

		// open a xsection
		else if (name.equals(TNXML.sXSECTION))
			TN.emitWarning("No longer XSection" + TNXML.sXSECTION); 

		// make a tube
		else if (name.equals(TNXML.sLINEAR_TUBE))
			TN.emitWarning("dead type: " + TNXML.sLINEAR_TUBE); 

		// open a posfix (not input as are the legs not input).
		else if (name.equals(TNXML.sFIX))
			posfixtype = PFT_FIX;
		else if (name.equals(TNXML.sPOS_FIX))
			posfixtype = PFT_POSFIX;
		else if (name.equals(TNXML.sLEG))
			posfixtype = PFT_LEG;

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

			ssb.fpicscale = (float)DeStack(TNXML.sLAUT_SYMBOL_PICSCALE, 1.0);
			ssb.faxisscale = (float)DeStack(TNXML.sLAUT_SYMBOL_AXISSCALE, 1.0);
			ssb.faxisscaleperp = (float)DeStack(TNXML.sLAUT_SYMBOL_AXISSCALEPERP, 1.0);

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
			ssb.bSymbolinterferencedoesntmatter = aint.equals(TNXML.sLAUT_SYMBOL_AINT_ALLOWED_OUTSIDE);

			String filledtype = SeStack(TNXML.sLAUT_SYMBOL_DRAWSTYLE, TNXML.sLAUT_SYMBOL_DRAWSTYLE_LINE);
			ssb.bFilledType = filledtype.equals(TNXML.sLAUT_SYMBOL_DRAWSTYLE_FILLED);

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
			ssba.add(ssb);
		}


		// as a batch
		//static String sTAPE = "tape";
		//static String sCOMPASS = "compass";
		//static String sCLINO = "clino";

		// sketch things
		else if (name.equals(TNXML.sSKETCH_PATH))
		{
			sketchpath_ind0 = IeStack(TNXML.sFROM_SKNODE);
			sketchpath_ind1 = IeStack(TNXML.sTO_SKNODE);

			sketchpath = new OnePath();
			skpnpoints = 0;
			sketchpath.linestyle = TNXML.DecodeLinestyle(SeStack(TNXML.sSK_LINESTYLE));
			sketchpath.bWantSplined = (IeStack(TNXML.sSPLINED) != 0);
		}

		// subset markers
		else if (name.equals(TNXML.sSKSUBSET))
		{
			String ssubset = SeStack(TNXML.sSKSNAME); 
			sketchpath.vssubsets.add(ssubset);
			tunnelsketch.sallsubsets.add(ssubset); 
		}
		else if (name.equals(TNXML.sSKIMPORTFROM))
			sketchpath.importfromname = SeStack(TNXML.sSKSNAME);

		else if (name.equals(TNXML.sPOINT))
		{
			skpX = (float)DeStack(TNXML.sPTX);
			skpY = (float)DeStack(TNXML.sPTY);

			// the z value gets saved only in the case of centreline nodes
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

			else if (posfixtype != PFT_NONE)
				; // nothing input for now.

			// supress errors on these no longer existent types
			else if (ElStack("symbol"))
				;

			else
			{
				StackDump();
				TN.emitError("point without an object");
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


	static PathLabelXMLparse BBplxp = new PathLabelXMLparse();
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
					// I think this is just for backward compatibility
					TN.emitWarning("*** Using Label parameter: " + sblabel.toString()); 
					sketchpath.plabedl = new PathLabelDecode(); 
					BBplxp.ParseLabel(sketchpath.plabedl, TNXML.xunmanglxmltext(sblabel.toString()), sketchlinestyle);
					// this is where labels are at present added.
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
		else if (name.equals(TNXML.sMEASUREMENTS))
			iftype = FileAbstraction.FA_FILE_UNKNOWN;


		else if (name.equals(TNXML.sSKETCH_PATH))
		{
			sketchpath.EndPath(LoadSketchNode(sketchpath_ind1));
			tunnelsketch.TAddPath(sketchpath, null);
			if (!tunnelsketch.bSymbolType && (sketchpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (sketchpath.plabedl != null))
				sketchpath.UpdateStationLabelsFromCentreline();
			sketchpath = null;
		}


		else if (name.equals(TNXML.sFIX) || name.equals(TNXML.sPOS_FIX) || name.equals(TNXML.sLEG))
			posfixtype = PFT_NONE;

		else if (name.equals(TNXML.sLAUT_SYMBOL))
		{
			assert (subsetattributes != null);
			SymbolStyleAttr ssa = subsetattributes.subautsymbolsmap.get(autsymbdname); 
			if (ssa == null)
			{
				ssa = new SymbolStyleAttr(autsymbdname); 
				subsetattributes.subautsymbolsmap.put(autsymbdname, ssa);  
			}
			else
				TN.emitWarning("Over-writing symbol " + autsymbdname + " in subset " + subsetattributes.subsetname); 

			ssa.iautsymboverwrite = iautsymboverwrite; 
			ssa.autsymbdesc = autsymbdesc; 
			ssa.ssymbolbs = new ArrayList<SSymbolBase>();
			ssa.ssymbolbs.addAll(ssba);
		}

		// used for the fontcolours
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTE_STYLE))
		{
			assert !sketchlinestyle.subsetattrstylesmap.containsKey(subsetattributestyle.stylename); 
			sketchlinestyle.subsetattrstylesmap.put(subsetattributestyle.stylename, subsetattributestyle);
			sketchlinestyle.bsubsetattributesneedupdating = true;
			subsetattributestyle.AssignDefault(null);  
			subsetattributestyle = null;
		}
		else if (name.equals(TNXML.sGRID_DEF))
		{
			assert sketchgrid.ngridspacing != 0;
			sketchgrid = null;
		}
		else if (name.equals(TNXML.sSUBSET_ATTRIBUTES))
		{
			if (sketchframedef == null)
				subsetattributes = null;
		}

		else if (name.equals(TNXML.sFONTCOLOURS))
			; // sketchlinestyle.symbolsdisplay.MakeSymbolButtonsInPanel();

	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	TunnelXMLparse()
	{
	}

	/////////////////////////////////////////////
	void SetUp(String lfnamess, int liftype)
	{
		SetUpBase();

		fnamess = lfnamess;
		iftype = liftype;

		tunnelsketch = null;
		sketchpath = null;

		isblabelstackpos = -1;
		sblabel.setLength(0);

		posfixtype = 0;
	}
};


