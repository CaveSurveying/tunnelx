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


/////////////////////////////////////////////
class TunnelXMLparse extends TunnelXMLparsebase
{
	OneTunnel tunnel;
	String fnamess;
	OneTunnel vgsymbols;

	boolean bContainsMeasurements = false;
	boolean bContainsExports = false;
	int nsketches = 0; // only be 0 or 1.
	boolean bContainsAutsymbols = false;

	Vector ssba = new Vector(); // the structure with a list of symbols in an aut-symbol. -- should be another class.
	String autsymbdname;
	String autsymbdesc;
	boolean bautsymboverwrite;


	// set directly after the constructor is called
	boolean bSymbolType = false;


    /////////////////////////////////////////////
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
System.out.println("aut sym " + SeStack(TNXML.sLAUT_SYMBOL_NAME));
			if (bContainsMeasurements || bContainsExports || (nsketches != 0))
				TN.emitWarning("other things in autsymbols xml file");
			bContainsAutsymbols = true;
			if (tunnel.vautsymbols == null)
				tunnel.vautsymbols = new Vector();

			// till we make a special class, the list of symbols in an aut-symbol is
			// a list with first element a string.
			ssba.removeAllElements();
            autsymbdname = SeStack(TNXML.sLAUT_SYMBOL_NAME);
            autsymbdesc = SeStack(TNXML.sLAUT_DESCRIPTION);
            bautsymboverwrite = TNXML.sLAUT_OVERWRITE.equals(SeStack(TNXML.sLAUT_BUTTON_ACTION));
		}

		// go through the possible commands
		else if (name.equals(TNXML.sMEASUREMENTS))
		{
			if (bContainsMeasurements || bContainsExports || (nsketches != 0) || bContainsAutsymbols)
				TN.emitWarning("other things in measurements xml file");
			bContainsMeasurements = true;
		}

		else if (name.equals(TNXML.sEXPORTS))
		{
			if (bContainsMeasurements || bContainsExports || (nsketches != 0) || bContainsAutsymbols)
				TN.emitWarning("other things in exports xml file");
			bContainsExports = true;
		}

		else if (name.equals(TNXML.sLABEL))
		{
			isblabelstackpos = istack - 1;
			bTextType = false;
		}

		// should be deprecated
		else if (name.equals(TNXML.sTEXT))
		{
			isblabelstackpos = istack - 1;
			bTextType = true;
		}


		// <export estation="1" ustation="insignificant.8"/>
		else if (name.equals(TNXML.sEXPORT))
		{
			tunnel.vexports.addElement(new OneExport(SeStack(TNXML.sEXPORT_FROM_STATION), SeStack(TNXML.sEXPORT_TO_STATION)));

			// early versions leave out the exports tag
			if (!bContainsExports)
			{
				if (bContainsMeasurements || (nsketches != 0))
					TN.emitWarning("other things in exports xml file");
				bContainsExports = true;
			}
		}

		// open a sketch
		else if (name.equals(TNXML.sSKETCH))
		{
			if (bContainsMeasurements || bContainsExports || (nsketches != 0) || bContainsAutsymbols)
				TN.emitWarning("other things in simple sketches xml file");
			nsketches++;

			tunnelsketch = new OneSketch(tunnel.tsketches, fnamess);
			lvnodes.removeAllElements();
			tunnelsketch.bSymbolType = bSymbolType;
			tunnel.tsketches.addElement(tunnelsketch);
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
			System.out.println("Adding to aut-symbol " + SeStack(TNXML.sLAUT_SYMBOL_NAME));
			SSymbolBase ssb = new SSymbolBase();

			// decode the mcode attributes on the symbol
			// this provides the interface between the xml and the values in the base class.
			String sscale = SeStack(TNXML.sLAUT_SYMBOL_SCALE, TNXML.sLAUT_SYMBOL_ALONGAXIS);
			ssb.bScaleable = !sscale.equals(TNXML.sLAUT_SYMBOL_FIXED);
			ssb.bShrinkby2 = sscale.equals(TNXML.sLAUT_SYMBOL_ANDHALF);
			if (ssb.bShrinkby2)
				ssb.bScaleable = false;

			String sorientation = SeStack(TNXML.sLAUT_SYMBOL_ORIENTATION, TNXML.sLAUT_SYMBOL_ALONGAXIS);
			ssb.bRotateable = !sorientation.equals(TNXML.sLAUT_SYMBOL_FIXED);
			if (sorientation.equals(TNXML.sLAUT_SYMBOL_RANDOM))
				ssb.posangledeviation = 10.0F;
			else if (sorientation.equals(TNXML.sLAUT_SYMBOL_ALONGAXIS))
				ssb.posangledeviation = 0.0F;
			else if (sorientation.equals(TNXML.sLAUT_SYMBOL_NEARAXIS))
				ssb.posangledeviation = 0.1F;

			String sposition = SeStack(TNXML.sLAUT_SYMBOL_POSITION, TNXML.sLAUT_SYMBOL_ENDPATH);
			ssb.bMoveable = !sposition.equals(TNXML.sLAUT_SYMBOL_ENDPATH);
			ssb.bLattice = sposition.equals(TNXML.sLAUT_SYMBOL_LATTICE);
			ssb.bPullback = sposition.equals(TNXML.sLAUT_SYMBOL_PULLBACK);
			ssb.bPushout = sposition.equals(TNXML.sLAUT_SYMBOL_PUSHOUT);
			ssb.posdeviationprop = (ssb.bMoveable && !ssb.bLattice ? (ssb.bPullback ? 2.0F : 1.0F) : 0.0F);

			String aint = SeStack(TNXML.sLAUT_SYMBOL_AINT, TNXML.sLAUT_SYMBOL_AINT_NO_OVERLAP);
			ssb.bAllowedOutsideArea = !aint.equals(TNXML.sLAUT_SYMBOL_AINT_NO_OVERLAP);
            ssb.bTrimByArea = aint.equals(TNXML.sLAUT_SYMBOL_AINT_TRIM);

			ssb.gsymname = SeStack(TNXML.sLSYMBOL_NAME);
			ssb.nmultiplicity = Integer.parseInt(SeStack(TNXML.sLAUT_SYMBOL_MULTIPLICITY, "1"));
System.out.println("   subaut " + ssb.gsymname + "  " + ssb.nmultiplicity + (ssb.bScaleable ? " SCA " : ""));

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
			tunnelsketch.SetBackground(tunnel.tundirectory, SeStack(TNXML.sSKETCH_BACK_IMG_FILE));
			tunnelsketch.backgimgtrans.setTransform(Double.parseDouble(SeStack(TNXML.sAFTR_M00)), Double.parseDouble(SeStack(TNXML.sAFTR_M10)), Double.parseDouble(SeStack(TNXML.sAFTR_M01)), Double.parseDouble(SeStack(TNXML.sAFTR_M11)), Double.parseDouble(SeStack(TNXML.sAFTR_M20)), Double.parseDouble(SeStack(TNXML.sAFTR_M21)));
		}
	}

	/////////////////////////////////////////////
	public void characters(String pstr)
	{
		// whitespace that shouldn't comes through here.
		assert pstr != null;
		if (isblabelstackpos != -1)
		{
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
				if (!name.equals(TNXML.sLABEL))
				{
					if (!bTextType || !name.equals(TNXML.sTEXT))
						TN.emitProgError("Stack pos doesn't match label position");
					else
						TN.emitWarning("Deprecated TEXT type used for label");
				}

				// this is where labels are at present added.
				sketchpath.plabedl = new PathLabelDecode(TNXML.xunmanglxmltext(sblabel.toString()));
				sblabel.setLength(0);
				isblabelstackpos = -1;
			}

			// the closing thing already done earlier (note the !)
			else if (!name.equals(TNXML.sLRSYMBOL))
				sblabel.append(TNXML.xcomclose(0, name));
		}

		// ending of other items.
		else if (name.equals(TNXML.sSKETCH))
		{
			for (int i = 0; i < tunnelsketch.vpaths.size(); i++)
				((OnePath)(tunnelsketch.vpaths.elementAt(i))).UpdateStationLabel(tunnelsketch.bSymbolType);
			assert OnePathNode.CheckAllPathCounts(tunnelsketch.vnodes, tunnelsketch.vpaths);
			tunnelsketch = null;
		}

		else if (name.equals(TNXML.sSKETCH_PATH))
		{
			sketchpath.EndPath(LoadSketchNode(sketchpath_ind1));
			tunnelsketch.TAddPath(sketchpath, vgsymbols);
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
			AutSymbolAc asa = new AutSymbolAc(autsymbdname, autsymbdesc, bautsymboverwrite, ssba);
			tunnel.vautsymbols.addElement(asa);
		}
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	TunnelXMLparse(OneTunnel lvgsymbols)
	{
		vgsymbols = lvgsymbols;
	}

	/////////////////////////////////////////////
	void SetUp(OneTunnel ltunnel, String lfnamess)
	{
		SetUpBase();

		tunnel = ltunnel;
		fnamess = lfnamess;

		bContainsMeasurements = false;
		bContainsExports = false;
		nsketches = 0; // only be 0 or 1.
		bContainsAutsymbols = false;

		tunnelsketch = null;
		sketchpath = null;

		xsection = null;
		xsectionindex = -1;

		isblabelstackpos = -1;
		sblabel.setLength(0);

		posfixtype = 0;
	}
};


