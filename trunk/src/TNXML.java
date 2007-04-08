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

class TNXML
{
	static String sHEADER = "<?xml version='1.0' encoding='us-ascii'?>";
	static String sTUNNELXML = "tunnelxml";
	static String sFONTCOLOURS = "fontcolours"; 
	static String sSURVEXEXEDIR = "survex_executable_directory"; 

	static String sSET = "set";
	static String sFLOAT_VALUE = "fval";
	static String sTEXT = "text";
	static String sNAME = "name";

	static String sEXPORTS = "exports";
	static String sEXPORT = "export";
	static String sEXPORT_FROM_STATION = "estation";
	static String sEXPORT_TO_STATION = "ustation";

	static String sSKSUBSET = "sketchsubset";
	static String sSKSNAME = "subname";
	static String sSKIMPORTFROM = "importfrom";

	static String sMEASUREMENTS = "measurements";
	static String sSVX_DATE = "date";
	static String sSVX_TITLE = "title";
	static String sSVX_TAPE_PERSON = "tapeperson";
	static String sLEG = "leg"; // effectively the same as set
	static String sFIX = "fix";
	static String sPOS_FIX = "pos_fix";
	static String sFROM_STATION = "from";
	static String sTO_STATION = "to";
	static String sTAPE = "tape";
	static String sCOMPASS = "compass";
	static String sCLINO = "clino";
	static String sDEPTHS = "depths";
	static String sTITLESET = "titleset";
	static String sFROMFLOAT_VALUE = "fval_from";
	static String sTOFLOAT_VALUE = "fval_to";


	// additional tube modelling stuff
	static String sXSECTION = "xsection";
	static String sXSECTION_INDEX = "xsind"; // prefer to be the index.
	static String sXS_STATION0 = "xsst0";
	static String sXS_STATION1 = "xsst1";
	static String sXS_STATION_LAM = "xs_lam";
	static String sXS_STATION_ORIENT_FORE = "xsstorfore";
	static String sXS_STATION_ORIENT_BACK = "xsstorback";
	static String sXS_STATION_ORIENT_REL_COMPASS = "xsstorrelc";
	static String sXS_STATION_ORIENT_CLINO = "xsstorclin";
	static String sLINEAR_TUBE = "ltube";
	static String sFROM_XSECTION = "xsfrom";
	static String sTO_XSECTION = "xsto";
	static String sSKETCH = "sketch";
	static String sSKETCH_BACK_IMG = "backimage";
	static String sSKETCH_BACK_IMG_FILE = "imgfile";
	static String sSKETCH_BACK_IMG_FILE_SELECTED = "selected";
	static String sAFFINE_TRANSFORM = "affinetrans";
	static String sAFTR_M00 = "aftrm00";
	static String sAFTR_M01 = "aftrm10";
	static String sAFTR_M10 = "aftrm01";
	static String sAFTR_M11 = "aftrm11";
	static String sAFTR_M20 = "aftrm20";
	static String sAFTR_M21 = "aftrm21";

	static String sSKETCH_PATH = "skpath";
	static String sFROM_SKNODE = "from";
	static String sTO_SKNODE = "to";
	static String sSPLINED = "splined";
	static String sLOCOFFSETX = "locoffsetx";
	static String sLOCOFFSETY = "locoffsety";
	static String sLOCOFFSETZ = "locoffsetz";
	static String sSK_LINESTYLE = "linestyle";

	// values of linestyle.
	static String vsLS_CENTRELINE = "centreline";
	static String vsLS_WALL = "wall";
	static String vsLS_ESTWALL = "estwall";
	static String vsLS_PITCHBOUND = "pitchbound";
	static String vsLS_CEILINGBOUND = "ceilingbound";
	static String vsLS_DETAIL = "detail";
	static String vsLS_INVISIBLE = "invisible";
	static String vsLS_CONNECTIVE = "connective";
	static String vsLS_FILLED = "filled";


	// this supercedes the "label" and takes out the local label xml problem.
	static String sPATHCODES = "pathcodes";
	static String sCL_STATIONS = "cl_stations";
	static String sCL_TAIL = "tail";
	static String sCL_HEAD = "head";
	static String sCL_ELEV = "elev";

	static String sPC_TEXT = "pctext";
	static String sLTEXTSTYLE = "style";
	static String sPC_NODEPOSXREL = "nodeposxrel";
	static String sPC_NODEPOSYREL = "nodeposyrel";
	static String sPC_ARROWPRES = "arrowpres";
	static String sPC_BOXPRES = "boxpres";

	static String sPC_RSYMBOL = "pcsymbol";
	static String sLRSYMBOL_NAME = "rname";

	static String sPC_AREA_SIGNAL = "pcarea";
	static String sAREA_PRESENT = "area_signal";
		static String sASIGNAL_KEEPAREA = "keeparea";
		static String sASIGNAL_KILLAREA = "killarea";
		static String sASIGNAL_OUTLINEAREA = "outlinearea";
		static String sASIGNAL_HCOINCIDE = "hcoincide";
		static String sASIGNAL_ZSETRELATIVE = "zsetrelative";
			static String sASIG_NODECONN_ZSETRELATIVE = "nodeconnzsetrelative"; 
		static String sASIGNAL_SKETCHFRAME = "sketchframe";
			static String sASIG_FRAME_SCALEDOWN = "sfscaledown";
			static String sASIG_FRAME_ROTATEDEG = "sfrotatedeg";
			static String sASIG_FRAME_XTRANS = "sfxtrans";
			static String sASIG_FRAME_YTRANS = "sfytrans";
			static String sASIG_FRAME_SKETCH = "sfsketch";
			static String sASIG_FRAME_STYLE = "sfstyle";
			static String sASIG_FRAME_REALPAPERSCALE = "sfrealpaperscale"; 
		static String sASIGNAL_ELEVATIONPATH = "elevationpath";

	// these are deprecated (but read from the local mangled xml)
	static String sLRSYMBOL = "rsymbol";
	static String sLTEXT = "text";
	static String sLABEL = "label";
	static String sTAIL = "tail";
	static String sHEAD = "head";
	static String sSPREAD = "spread";


	// the new symbol stuff laid out inside the label
	// these will be deleted
	static String sLSYMBOL = "symbol";
	static String sLSYMBOL_NAME = "name";
	static String sLMCODE = "mcode";
	static String sLQUANTITY = "qty";
	static String sLASYMBOL = "asymbol";
	static String sLAUT_SYMBOL_ORIENTATION = "orientation";
	static String sLAUT_SYMBOL_FIXED = "fixed";
	static String sLAUT_SYMBOL_RANDOM = "random";
	static String sLAUT_SYMBOL_ALONGAXIS = "alongaxis";
	static String sLAUT_SYMBOL_ALONGAXIS_PERP = "alongaxisperp";
	static String sLAUT_SYMBOL_CLOSESTFROMAXIS = "closestfromaxis";
	static String sLAUT_SYMBOL_CLOSESTALONGAXIS = "closestalongaxis";
	static String sLAUT_SYMBOL_NEARAXIS = "nearaxis";
	static String sLAUT_SYMBOL_SCALE = "scale";
	static String sLAUT_SYMBOL_ANDHALF = "andhalf";
	static String sLAUT_SYMBOL_PICSCALE = "picscale";
	static String sLAUT_SYMBOL_AXISSCALE = "axisscale";
	static String sLAUT_SYMBOL_AXISSCALEPERP = "axisscaleperp";
	static String sLAUT_SYMBOL_POSITION = "position";
	static String sLAUT_SYMBOL_ENDPATH = "endpath";
	static String sLAUT_SYMBOL_LATTICE = "lattice"; 	// lattice relative to endpoint
	static String sLAUT_SYMBOL_LATTICEF = "latticef"; 	// lattice fixed to an integral phase (so will always be consistent)
	static String sLAUT_SYMBOL_ALONGPATH_RANDOM_PULLBACK = "alongpath_random_pullback";
	static String sLAUT_SYMBOL_ALONGPATH_EVEN = "alongpath_even";
	static String sLAUT_SYMBOL_PULLBACK = "pullback";
	static String sLAUT_SYMBOL_PUSHOUT = "pushout";
	static String sLAUT_SYMBOL_MULTIPLICITY = "multiplicity";
	static String sLAUT_SYMBOL_MULTIPLICITY_FILL = "fill";
	static String sLAUT_SYMBOL_AINT = "area-interaction";
	static String sLAUT_SYMBOL_AINT_NO_OVERLAP = "no-overlap";
	static String sLAUT_SYMBOL_AINT_TRIM = "trim";
	static String sLAUT_SYMBOL_AINT_ALLOWED_OUTSIDE = "allowed-outside";
	static String sLAUT_SYMBOL_AINT_ALLOWED_OUTSIDE_NO_OVERLAP = "allowed-outside-no-overlap";
	static String sLAUT_SYMBOL_AREA_FILL = "symbolareafillcolour";
	static String sLAUT_SYMBOL_DRAWSTYLE = "drawstyle"; 
	static String sLAUT_SYMBOL_DRAWSTYLE_FILLED = "filled";
	static String sLAUT_SYMBOL_DRAWSTYLE_LINE = "line";

	// aut symbols which reference the above
	static String sLAUT_SYMBOL = "symbolaut";
	static String sLAUT_SYMBOL_NAME = "dname";
	static String sLAUT_DESCRIPTION = "description";
	static String sLAUT_SYMBOLS = "aut-symbols";
	static String sLAUT_BUTTON_ACTION = "buttonaction";
	static String sLAUT_OVERWRITE = "overwrite";
	static String sLAUT_APPEND = "append";
	static String sLAUT_NOBUTTON = "nobutton";


	static String sSUBSET_ATTRIBUTE_STYLE = "groupsubsetattr";
	static String sSUBSET_ATTRIBUTE_STYLE_NAME = "groupsubsetname";
	static String sSUBSET_ATTRIBUTE_STYLE_SELECTABLE = "groupsubsetselectable"; // yes or no
	static String sSUBSET_ATTRIBUTE_STYLE_IMPORT = "importgroupsubsetattr";

	static String sSUBSET_ATTRIBUTES = "subsetattr";
	static String sSUBSET_NAME = "name";
	static String sUPPER_SUBSET_NAME = "uppersubset";
	static String sSET_ATTR_VARIABLE = "setvariable";
	static String sATTR_VARIABLE_NAME = "name";
	static String sATTR_VARIABLE_VALUE = "value";
	static String sATTR_VARIABLE_VALUE_CLEAR = "--clear--";

	static String sSUBSET_AREAMASKCOLOUR = "areamaskcolour";
	static String sSUBSET_AREACOLOUR = "areacolour";

	static String sLABEL_STYLE_FCOL = "labelfcol";
	static String sLABEL_STYLE_NAME = "labelstylename";
	static String sLABEL_FONTNAME = "fontname";
	static String sLABEL_FONTSTYLE = "fontstyle";
	static String sLABEL_FONTSIZE = "size";
	static String sLABEL_COLOUR = "labelcolour";

	static String sLINE_STYLE_COL = "linestylecol";
	static String sLS_STROKEWIDTH = "strokewidth";
	static String sLS_SPIKEGAP = "spikegap";
	static String sLS_GAPLENG = "gapleng";
	static String sLS_SPIKEHEIGHT = "spikeheight";
	static String sLS_STROKECOLOUR = "strokecolour";
	static String sLS_SHADOWSTROKEWIDTH = "shadowstrokewidth";
	static String sLS_SHADOWSTROKECOLOUR = "shadowstrokecolour";

	static String sGRID_DEF = "grid_def";
	static String sMAX_GRID_LINES = "maxgridlines";
	static String sGRID_XORIG = "xorig";
	static String sGRID_YORIG = "yorig";
	static String sGRID_SPACING = "gridspacing";
	static String sGRID_SPACING_WIDTH = "gswidth";


	static String sAREA_SIG_DEF = "area_signal_def";
	static String sAREA_SIG_NAME = "asigname";
	static String sAREA_SIG_EFFECT = "asigeffect";

	static String sIMAGE_FILE_DIRECTORY = "image_file_directory";
	static String sIMAGE_FILE_DIRECTORY_NAME = "name";


	static String sPOINT = "pt";
	static String sPTX = "X";
	static String sPTY = "Y";
	static String sPTZ = "Z";
	static String[] tabs = { "", "\t", "\t\t", "\t\t\t", "\t\t\t\t" };
	/////////////////////////////////////////////
	static String EncodeLinestyle(int linestyle)
	{
		switch (linestyle)
		{
			case SketchLineStyle.SLS_CENTRELINE:
				return vsLS_CENTRELINE;
			case SketchLineStyle.SLS_WALL:
				return vsLS_WALL;
			case SketchLineStyle.SLS_ESTWALL:
				return vsLS_ESTWALL;
			case SketchLineStyle.SLS_PITCHBOUND:
				return vsLS_PITCHBOUND;
			case SketchLineStyle.SLS_CEILINGBOUND:
				return vsLS_CEILINGBOUND;
			case SketchLineStyle.SLS_DETAIL:
				return vsLS_DETAIL;
			case SketchLineStyle.SLS_INVISIBLE:
				return vsLS_INVISIBLE;
			case SketchLineStyle.SLS_CONNECTIVE:
				return vsLS_CONNECTIVE;
			case SketchLineStyle.SLS_FILLED:
				return vsLS_FILLED;
			default:
				TN.emitError("Unknown linestyle");
		}
		return "??";
	}

	/////////////////////////////////////////////
	static int DecodeLinestyle(String slinestyle)
	{
		if (slinestyle.equals(vsLS_CENTRELINE))
			return SketchLineStyle.SLS_CENTRELINE;
		if (slinestyle.equals(vsLS_WALL))
			return SketchLineStyle.SLS_WALL;
		if (slinestyle.equals(vsLS_ESTWALL))
			return SketchLineStyle.SLS_ESTWALL;
		if (slinestyle.equals(vsLS_PITCHBOUND))
			return SketchLineStyle.SLS_PITCHBOUND;
		if (slinestyle.equals(vsLS_CEILINGBOUND))
			return SketchLineStyle.SLS_CEILINGBOUND;
		if (slinestyle.equals(vsLS_DETAIL))
			return SketchLineStyle.SLS_DETAIL;
		if (slinestyle.equals(vsLS_INVISIBLE))
			return SketchLineStyle.SLS_INVISIBLE;
		if (slinestyle.equals(vsLS_CONNECTIVE))
			return SketchLineStyle.SLS_CONNECTIVE;
		if (slinestyle.equals(vsLS_FILLED))
			return SketchLineStyle.SLS_FILLED;
		// backwards compatibility for now.
		TN.emitWarning("numeric linestyle " + slinestyle);
		return Integer.parseInt(slinestyle);
	}
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static StringBuffer sb = new StringBuffer();

	// Builders are like Buffers but they don't do thread-safety checks, so they're faster to run in single-threaded programs like this one
	/////////////////////////////////////////////
	static void sbstartxcom(int indent, String command)
	{
		sb.setLength(0);
		sb.append(tabs[indent]);
		sb.append('<');
		sb.append(command);
	}
	/////////////////////////////////////////////
	static String attribxcom(String attr, String val)
	{
		return " " + attr + "=\"" + xmanglxmltext(val) + "\"";
	}

	/////////////////////////////////////////////
	static void sbattribxcom(String attr, String val)
	{
		sb.append(" ");
		sb.append(attr);
		sb.append("=\"");
		xmanglxmltextSB(val);
		sb.append("\"");
	}
	/////////////////////////////////////////////
	static String sbendxcomsingle()
	{
		sb.append("/>");
		return sb.toString();
	}
	/////////////////////////////////////////////
	static String sbendxcom()
	{
		sb.append(">");
		return sb.toString();
	}

	/////////////////////////////////////////////
	// annoying ...args feature available only in 1.5, so makes it difficult to detune it
	/////////////////////////////////////////////
	static String xcomN(int indent, String command, String[] args, int N)
	{
		if((N % 2) != 0)
		{
			TN.emitWarning("Malformed call to XML library");
			return "";
		}

		sbstartxcom(indent, command);
		for(int i = 0; i < N/2; i++)
			sbattribxcom(args[2*i], args[2*i + 1]);
		return sbendxcomsingle();
	}

	/////////////////////////////////////////////
	static String xcomopenN(int indent, String command, String[] args, int N)
	{
		if((N % 2) != 0)
		{
			TN.emitWarning("Malformed call to XML library");
			//System.exit(1);
			return "";
		}

		sbstartxcom(indent, command);
		for(int i = 0; i < N/2; i++)
			sbattribxcom(args[2*i], args[2*i + 1]);
		return sbendxcom();
	}

	/////////////////////////////////////////////
	static String xcomtextN(int indent, String command, String[] args, int N)
	{
		if((N % 2) != 1)
		{
			TN.emitWarning("Malformed call to XML library");
			return "";
		}

		sbstartxcom(indent, command);
		for(int i = 0; i < (N-1)/2; i++)
			sbattribxcom(args[2*i], args[2*i + 1]);
		sb.append(">");

		sb.append(args[N-1]);
		sb.append("</");
		sb.append(command);
		return sbendxcom();
	}

	// 1.5 versions
	/*
	 static String xcom(int indent, String command, String... args)
	 {
	 return xcomN(indent, command, args, args.length); 
	 }
	 static String xcomopen(int indent, String command, String... args)
	 {
	 return xcomopenN(indent, command, args, args.length); 
	 }
	 static String xcomtext(int indent, String command, String... args)
	 {
	 return xcomtextN(indent, command, args, args.length); 
	 }
	 */
	
	
	// 1.4 versions
	static String[] xargs = new String[20]; 

	static String xcom(int indent, String command)
	{
		return xcomN(indent, command, xargs, 0); 
	}
	static String xcom(int indent, String command, String ap0, String av0)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		return xcomN(indent, command, xargs, 2); 
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		return xcomN(indent, command, xargs, 4);
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		return xcomN(indent, command, xargs, 6);
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3)
	{
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		return xcomN(indent, command, xargs, 8); 
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		xargs[8] = ap4;  xargs[9] = av4; 
		return xcomN(indent, command, xargs, 10); 
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4, String ap5, String av5)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		xargs[8] = ap4;  xargs[9] = av4; 
		xargs[10] = ap5;  xargs[11] = av5; 
		return xcomN(indent, command, xargs, 12);
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4, String ap5, String av5, String ap6, String av6)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3;
		xargs[8] = ap4;  xargs[9] = av4;
		xargs[10] = ap5;  xargs[11] = av5; 
		xargs[12] = ap6;  xargs[13] = av6;
		return xcomN(indent, command, xargs, 14); 
	}
	static String xcom(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4, String ap5, String av5, String ap6, String av6, String ap7, String av7)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3;
		xargs[8] = ap4;  xargs[9] = av4;
		xargs[10] = ap5;  xargs[11] = av5; 
		xargs[12] = ap6;  xargs[13] = av6;
		xargs[14] = ap7;  xargs[15] = av7;
		return xcomN(indent, command, xargs, 16); 
	}
	static String xcomopen(int indent, String command)
	{
		return xcomopenN(indent, command, xargs, 0); 
	}
	static String xcomopen(int indent, String command, String ap0, String av0)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		return xcomopenN(indent, command, xargs, 2); 
	}
	static String xcomopen(int indent, String command, String ap0, String av0, String ap1, String av1)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		return xcomopenN(indent, command, xargs, 4); 
	}
	static String xcomopen(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		return xcomopenN(indent, command, xargs, 6); 
	}
	static String xcomopen(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		return xcomopenN(indent, command, xargs, 8);
	}
	static String xcomopen(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4)
	{
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		xargs[8] = ap4;  xargs[9] = av4; 
		return xcomopenN(indent, command, xargs, 10); 
	}
	static String xcomopen(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4, String ap5, String av5)
	{ 
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		xargs[8] = ap4;  xargs[9] = av4; 
		xargs[10] = ap5;  xargs[11] = av5; 
		return xcomopenN(indent, command, xargs, 12); 
	}
	static String xcomtext(int indent, String command, String text)
	{
		xargs[0] = text; 
		return xcomtextN(indent, command, xargs, 1); 
	}
	static String xcomtext(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String text)
	{
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1; 
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = text; 
		return xcomtextN(indent, command, xargs, 7); 
	}
	static String xcomtext(int indent, String command, String ap0, String av0, String ap1, String av1, String ap2, String av2, String ap3, String av3, String ap4, String av4, String text)
	{
		xargs[0] = ap0;  xargs[1] = av0; 
		xargs[2] = ap1;  xargs[3] = av1;
		xargs[4] = ap2;  xargs[5] = av2; 
		xargs[6] = ap3;  xargs[7] = av3; 
		xargs[8] = ap4;  xargs[9] = av4; 
		xargs[10] = text; 
		return xcomtextN(indent, command, xargs, 11); 
	}
	
	/////////////////////////////////////////////
	static String xcomclose(int indent, String command)
	{
		sb.setLength(0);
		sb.append(tabs[indent]);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}

	/////////////////////////////////////////////
	static String xcomtext(String command, String text)
	{
		return "<" + command + ">" + text + "</" + command + ">";
	}



	/////////////////////////////////////////////
	// quick and dirty extraction here.  (the two command things could be buffered).
	static String xrawextracttext(String source, String commandopen, String commandclose)
	{
		int p0 = source.indexOf(commandopen);
		int p0g = p0 + commandopen.length();
		int p1 = source.lastIndexOf(commandclose);
		if ((p0 != -1) && (p1 != -1) && (p0g < p1))
			return source.substring(p0g, p1);
		return null;
	}
	/////////////////////////////////////////////
	static String xrawextracttext(String source, String command)
	{
		return xrawextracttext(source, xcomopen(0, command), xcomclose(0, command));
	}
	/////////////////////////////////////////////
	// this is very brittle stuff to extract one closed command
	static String xrawextractattr(String source, String[] val, String command, String[] attr)
	{
		int pe = source.indexOf("/>");
		int ps = source.indexOf(command);
		if ((pe == -1) || (ps == -1) || (pe <= ps))
			return null;
		for (int i = 0; i < attr.length; i++)
		{
			int pa = source.indexOf(attr[i]);
			val[i] = null;
			if ((pa != -1) && (pa < pe))
			{
				int pq1 = source.indexOf("\"", pa);
				int pq2 = source.indexOf("\"", pq1 + 1);
				if ((pq1 < pq2) && (pq2 < pe))
					val[i] = source.substring(pq1 + 1, pq2);
			}
		}
		return source.substring(pe + 2).trim();
	}
	/////////////////////////////////////////////
	static char[] chconvCH = { (char)176, (char)246, (char)252, '<', '>', '"', '&', '\\', '\'', ' ', '\n' };
	static char[] chconv = chconvCH;  // allow for hacks (which vary chconvleng)
	static String[] chconvName = {"&deg;", "&ouml;", "&uuml;", "&lt;", "&gt;", "&quot;", "&amp;", "&backslash;", "&apostrophe;", "&space;", "&newline;" };
	static int chconvleng = chconvCH.length;  // used for hacking out the space ones (this hack needs to be killed, or replaced with a flag)
	/////////////////////////////////////////////
	static void xmanglxmltextSB(String s)
	{
		assert ((chconvleng == chconvName.length) || (chconvleng == chconvName.length - 2));
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			int j;

			// there might be a regexp that would do this substitution directly, or use indexOf in a concatenated string of chconvCH
			for (j = 0; j < chconvleng; j++)
			{
				if (ch == chconvCH[j])
				{
					sb.append(chconvName[j]);
					break;
				}
			}
			if (j == chconvleng)
				sb.append(ch);
		}
	}


	/////////////////////////////////////////////
	static String xmanglxmltext(String s)
	{
		sb.setLength(0);
		xmanglxmltextSB(s);
		return sb.toString();
	}
	/////////////////////////////////////////////
	static String xunmanglxmltext(String s)
	{
		if (s.indexOf('&') == -1)
			return s;
		sb.setLength(0);
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			if (ch == '&')
			{
				int j;
				for (j = 0; j < chconvleng; j++)
				{
					if (s.regionMatches(i, chconvName[j], 0, chconvName[j].length()))
					{
						sb.append(chconvCH[j]);
						i += chconvName[j].length() - 1;
						//if (j < 2)
						//	System.out.println(chconv[j] + " -- " + (int)chconv[j].toCharArray()[0]);
						break;
					}
				}
				if (j == chconvleng)
				{
					System.out.println(s.substring(i));
					TN.emitError("unable to resolve & from pos " + i + " in string:" + s);
				}
			}
			else
				sb.append(ch);
		}
		return sb.toString();
	}

};
