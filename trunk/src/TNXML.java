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
			static String sLAUT_SYMBOL_NEARAXIS = "nearaxis";
		static String sLAUT_SYMBOL_SCALE = "scale";
			static String sLAUT_SYMBOL_ANDHALF = "andhalf";
		static String sLAUT_SYMBOL_POSITION = "position";
			static String sLAUT_SYMBOL_ENDPATH = "endpath";
			static String sLAUT_SYMBOL_LATTICE = "lattice";
			static String sLAUT_SYMBOL_PULLBACK = "pullback";
			static String sLAUT_SYMBOL_PUSHOUT = "pushout";
		static String sLAUT_SYMBOL_MULTIPLICITY = "multiplicity";
	static String sLAUT_SYMBOL_AINT = "area-interaction";
		static String sLAUT_SYMBOL_AINT_NO_OVERLAP = "no-overlap";
		static String sLAUT_SYMBOL_AINT_TRIM = "trim";
		static String sLAUT_SYMBOL_AINT_ALLOWED_OUTSIDE = "allowed-outside";

	// aut symbols which reference the above
	static String sLAUT_SYMBOL = "symbolaut";
		static String sLAUT_SYMBOL_NAME = "dname";
		static String sLAUT_DESCRIPTION = "description";
		static String sLAUT_SYMBOLS = "aut-symbols";
		static String sLAUT_BUTTON_ACTION = "buttonaction";
			static String sLAUT_OVERWRITE = "overwrite";
			static String sLAUT_APPEND = "append";


	static String sSUBSET_ATTRIBUTE_STYLE = "groupsubsetattr";
		static String sSUBSET_ATTRIBUTE_STYLE_NAME = "groupsubsetname";
		static String sSUBSET_ATTRIBUTE_STYLE_NAMEDEFAULTS = "groupsubsetdefaults";

		static String sSUBSET_ATTRIBUTES = "subsetattr";
			static String sSUBSET_NAME = "name";
			static String sUPPER_SUBSET_NAME = "uppersubset";
			static String sSUBSET_AREAMASKCOLOUR = "areamaskcolour";
			static String sSUBSET_AREACOLOUR = "areacolour";
			static String sSUBSET_LINECOLOUR = "linecolour";

			static String sLABEL_STYLE_FCOL = "labelfcol";
				static String sLABEL_STYLE_NAME = "labelstylename";
				static String sLABEL_FONTNAME = "fontname";
				static String sLABEL_FONTSTYLE = "fontstyle";
				static String sLABEL_FONTSIZE = "size";
				static String sLABEL_COLOUR = "labelcolour";

	static String sAREA_SIG_DEF = "area_signal_def";
		static String sAREA_SIG_NAME = "asigname";
		static String sAREA_SIG_EFFECT = "asigeffect";


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
	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		return sbendxcomsingle();
	}
	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0, String attr1, String val1)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		return sbendxcomsingle();
	}
	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		return sbendxcomsingle();
	}
	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		return sbendxcomsingle();
	}
	/////////////////////////////////////////////
	static String xcom(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3, String attr4, String val4, String attr5, String val5)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		sbattribxcom(attr4, val4);
		sbattribxcom(attr5, val5);
		return sbendxcomsingle();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command)
	{
		sbstartxcom(indent, command);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3, String attr4, String val4)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		sbattribxcom(attr4, val4);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomopen(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3, String attr4, String val4, String attr5, String val5)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		sbattribxcom(attr4, val4);
		sbattribxcom(attr5, val5);
		return sbendxcom();
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
	static String xcomtext(int indent, String command, String text)
	{
		sbstartxcom(indent, command);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomtext(int indent, String command, String attr0, String val0, String text)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomtext(int indent, String command, String attr0, String val0, String attr1, String val1, String text)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}
	/////////////////////////////////////////////
	static String xcomtext(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String text)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}

	/////////////////////////////////////////////
	static String xcomtext(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3, String text)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
	}

	/////////////////////////////////////////////
	static String xcomtext(int indent, String command, String attr0, String val0, String attr1, String val1, String attr2, String val2, String attr3, String val3, String attr4, String val4, String text)
	{
		sbstartxcom(indent, command);
		sbattribxcom(attr0, val0);
		sbattribxcom(attr1, val1);
		sbattribxcom(attr2, val2);
		sbattribxcom(attr3, val3);
		sbattribxcom(attr4, val4);
		sb.append(">");
		sb.append(text);
		sb.append('<');
		sb.append('/');
		sb.append(command);
		return sbendxcom();
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
	static String[] chconv = { "<&lt;", ">&gt;", "\"&quot;", "&&amp;", "\\&backslash;", "'&apostrophe;", " &space;", "\n&newline;" };
	static int chconvleng = chconv.length; // used for hacking out the space ones
	/////////////////////////////////////////////
	static void xmanglxmltextSB(String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			int j;
			for (j = 0; j < chconvleng; j++)
			{
				if (ch == chconv[j].charAt(0))
				{
					sb.append(chconv[j].substring(1));
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
				for (j = 0; j < chconv.length; j++)
				{
					if (s.regionMatches(i, chconv[j], 1, chconv[j].length() - 1))
					{
						sb.append(chconv[j].charAt(0));
						i += chconv[j].length() - 2;
						break;
					}
				}
				assert j < chconv.length;
			}
			else
				sb.append(ch);
		}
		return sb.toString();
		}

};
