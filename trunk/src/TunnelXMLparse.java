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

import java.awt.geom.Line2D;


/////////////////////////////////////////////
class TunnelXMLparse
{
	OneTunnel tunnel; 
	String fnamess; 

	String[] attnamestack = new String[50]; 
	String[] attvalstack = new String[50]; 
	
	String[] elemstack = new String[20]; 
	int[] iposstack = new int[20]; 
	int istack = 0; 

	boolean bContainsMeasurements = false; 
	boolean bContainsExports = false; 
	int nsketches = 0; // only be 0 or 1.  

	boolean bSymbolType = false; 

	/////////////////////////////////////////////
	String SeStack(String name)  
	{
		for (int i = (istack != 0 ? iposstack[istack - 1] : 0) - 1; i >= 0; i--)  
		{
			if (attnamestack[i].equals(name)) 
				return attvalstack[i]; 
		}
		return null; 
	}


	/////////////////////////////////////////////
	boolean ElStack(String name)  
	{
		for (int i = istack - 1; i >= 0; i--)  
		{
			if (elemstack[i].equals(name)) 
				return true; 
		}
		return false; 
	}


    /////////////////////////////////////////////
	OneSketch tunnelsketch = null; 

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
		while (tunnelsketch.vnodes.size() <= ind) 
			tunnelsketch.vnodes.addElement(null); 
		
		OnePathNode res; 
		if (tunnelsketch.vnodes.elementAt(ind) == null) 
		{
			res = new OnePathNode(skpX, skpY, skpZ, skpZset); 
			tunnelsketch.vnodes.setElementAt(res, ind); 
		}
		else 
		{
			res = (OnePathNode)tunnelsketch.vnodes.elementAt(ind);  
			if (((float)res.pn.getX() != skpX) || ((float)res.pn.getY() != skpY))
				TN.emitWarning("Node mismatch value: " + (float)res.pn.getX() + " = " + skpX + ",   " + (float)res.pn.getY() + " = " + skpY); 
			if ((float)res.zalt != skpZ)
				TN.emitWarning("ZNode mismatch value: " + res.zalt + " = " + skpZ); 
		} 
		return res; 
	}


	OneSSymbol sketchsymbol = null; 

	/////////////////////////////////////////////
	public void startElementAttributesHandled(String name)
	{
		// copy into label stuff if one is live.  
		if (isblabelstackpos != -1)  
			sblabel.append(TNXML.xcomopen(0, name)); 


		// go through the possible commands 
		if (name.equals(TNXML.sMEASUREMENTS))  
		{
			if (bContainsMeasurements || bContainsExports || (nsketches != 0))  
				TN.emitWarning("other things in measurements xml file"); 
			bContainsMeasurements = true; 
		}

		else if (name.equals(TNXML.sEXPORTS))  
		{
			if (bContainsMeasurements || bContainsExports || (nsketches != 0))  
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
			if (bContainsMeasurements || bContainsExports || (nsketches != 0))  
				TN.emitWarning("other things in simple sketches xml file"); 
			nsketches++; 

			tunnelsketch = new OneSketch(tunnel.tsketches, fnamess); 
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

		else if (name.equals(TNXML.sSYMBOL))  
		{
			sketchsymbol = new OneSSymbol(); 
			sketchsymbol.SpecSymbol(SeStack(TNXML.sSYMBOL_NAME), null);  
			sketchsymbol.IncrementMultiplicity(Integer.parseInt(SeStack(TNXML.sSYMBOL_MULTI))); 
			skpnpoints = 0; 
		}

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

			else if (sketchsymbol != null)  
			{
				// loc area type
				if (ElStack(TNXML.sSYMBOL_AREA_LOC))  
					sketchsymbol.slocarea.addElement(new Vec3(skpX, skpY, skpZ));  
				else if (ElStack(TNXML.sSYMBOL_AXIS))  
				{
					if (skpnpoints == 0)  
					{
						skpXo = skpX; 
						skpYo = skpY; 
					}
					else if (skpnpoints == 1)  
						sketchsymbol.paxis = new Line2D.Float(skpXo, skpYo, skpX, skpY); 
					else 
						TN.emitWarning("too many points for an axis.  "); 

					skpnpoints++; 
				}
				else 
					TN.emitWarning("symbol point without an object"); 
			}

			else if (xsection != null)  
			{
				xsection.AddNode(new Vec3(skpX, skpY, skpZ)); 
			}

			else if (posfixtype != 0)  
				; // nothing input for now.  

			else 
				TN.emitWarning("point without an object"); 
		}

		else if (name.equals(TNXML.sSKETCH_BACK_IMG))  
		{
			tunnelsketch.SetBackground(tunnel.tundirectory, SeStack(TNXML.sSKETCH_BACK_IMG_FILE));  
			tunnelsketch.backgimgtrans.setTransform(Double.parseDouble(SeStack(TNXML.sAFTR_M00)), Double.parseDouble(SeStack(TNXML.sAFTR_M10)), Double.parseDouble(SeStack(TNXML.sAFTR_M01)), Double.parseDouble(SeStack(TNXML.sAFTR_M11)), Double.parseDouble(SeStack(TNXML.sAFTR_M20)), Double.parseDouble(SeStack(TNXML.sAFTR_M21)));  
		}
	}

	/////////////////////////////////////////////
	public void characters(String pstr, char[] ch, int start, int length) 
	{
		// whitespace that shouldn't comes through here.  
		if (isblabelstackpos != -1)  
		{
			if (bTextType)  
			{
				String txt = (pstr == null ? new String(ch, start, length) : pstr); 
				int ip = txt.indexOf("%%"); 
				if (ip != -1) 
				{
					sblabel.append(TNXML.xcomtext(TNXML.sTAIL, txt.substring(0, ip))); 
					sblabel.append(TNXML.xcomtext(TNXML.sHEAD, txt.substring(ip + 2))); 
				}
			}
			else 
			{
				if (pstr == null) 
					sblabel.append(ch, start, length);  
				else 
					sblabel.append(pstr);  
			}
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
				sketchpath.plabel = sblabel.toString(); 
				sblabel.setLength(0); 
				isblabelstackpos = -1; 
			}

			else 
				sblabel.append(TNXML.xcomclose(0, name)); 
		}

		// ending of other items.  
		else if (name.equals(TNXML.sSKETCH))  
		{
			tunnelsketch.TrimCountSketchNodes(); 
			for (int i = 0; i < tunnelsketch.vpaths.size(); i++) 
				((OnePath)(tunnelsketch.vpaths.elementAt(i))).UpdateStationLabel(tunnelsketch.bSymbolType); 
			tunnelsketch = null; 
		}

		else if (name.equals(TNXML.sSKETCH_PATH))  
		{
			sketchpath.EndPath(LoadSketchNode(sketchpath_ind1));  
			tunnelsketch.vpaths.addElement(sketchpath); 
			sketchpath = null; 
		}

		else if (name.equals(TNXML.sSYMBOL))  
		{
			if (sketchsymbol.paxis == null) 
				TN.emitWarning("symbol lacking axis."); 
			tunnelsketch.vssymbols.addElement(sketchsymbol); 
			sketchsymbol = null; 
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
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	TunnelXMLparse()  
	{
	}

	/////////////////////////////////////////////
	void SetUp(OneTunnel ltunnel, String lfnamess)  
	{
		tunnel = ltunnel; 
		fnamess = lfnamess; 


		istack = 0; 
		bContainsMeasurements = false; 
		bContainsExports = false; 
		nsketches = 0; // only be 0 or 1.  


		tunnelsketch = null; 
		sketchpath = null; 

		xsection = null; 
		xsectionindex = -1; 

		isblabelstackpos = -1; 
		sblabel.setLength(0); 

		posfixtype = 0; 
	}
}; 


