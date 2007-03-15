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

import java.awt.Graphics2D;
import java.util.Random;
import java.util.Arrays;
import java.lang.StringBuffer;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.GeneralPath;
import java.awt.geom.Area;
import java.awt.BasicStroke;
import java.awt.Color;

import java.util.List;




/////////////////////////////////////////////
class OneSSymbol
{
	// when we have multisumbols, up to the transformed paths,
	// the info based on the pos of the axis could be shared.

	// arrays of sketch components.

	// location definition
	Line2D paxis;
 	SSymbolBase ssb;
	OnePath op; // used to access the line width for detail lines for the subset associated to this symbol (by connective path).

	GeneralPath gpsymps = null;  // all multiple symbols and parts thereof are consolidated into this single path
	int nsmposvalid = 0; // number of symbols whose position is valid for drawing of the multiplicity.

	// one to do it all for now.
	static SSymbScratch Tsscratch = new SSymbScratch();


	/////////////////////////////////////////////
	// used to preview a couple of positions
	void RefreshSymbol()
	{
		nsmposvalid = 0;
		gpsymps = null;
		if (ssb.gsym != null)
		{
			Tsscratch.InitAxis(this, true, null);
			// make some provisional positions just to help the display of multiplicity
			int nic = (((ssb.nmultiplicity != -1) && (ssb.nmultiplicity < 2)) ? ssb.nmultiplicity : 2);
			for (int ic = 0; ic < nic; ic++)
			{
				Tsscratch.BuildAxisTransSetup(this, ic);
				AppendTransformedCopy(Tsscratch.BuildAxisTransT(1.0F));
			}
		}
	}


	/////////////////////////////////////////////
	void AppendTransformedCopy(AffineTransform paxistrans)
	{
		for (int j = 0; j < ssb.gsym.vpaths.size(); j++)
		{
			OnePath path = (OnePath)ssb.gsym.vpaths.elementAt(j);
			if ((path.linestyle == SketchLineStyle.SLS_DETAIL) || (path.linestyle == SketchLineStyle.SLS_FILLED))
			{
				GeneralPath gp = (GeneralPath)path.gp.clone();
				gp.transform(paxistrans);
				if (gpsymps == null)
					gpsymps = gp;
				else
					gpsymps.append(gp, false);
			}
		}
	}



	/////////////////////////////////////////////



	/////////////////////////////////////////////
//	static Color colsymoutline = new Color(1.0F, 0.8F, 0.8F);
//	static Color colsymactivearea = new Color(1.0F, 0.2F, 1.0F, 0.16F);
	void paintW(GraphicsAbstraction ga, boolean bActive, boolean bProperSymbolRender)
	{
		if (bProperSymbolRender && (nsmposvalid == 0))
			return;
		LineStyleAttr linestyleattr;
		if (bActive)
			linestyleattr = (ssb.bFilledType ? SketchLineStyle.fillactivestylesymb : SketchLineStyle.lineactivestylesymb);
		else if (bProperSymbolRender)
			linestyleattr = (ssb.bFilledType ? op.subsetattr.linestyleattrs[SketchLineStyle.SLS_FILLED] : op.subsetattr.linestyleattrs[SketchLineStyle.SLS_DETAIL]);
		else if (nsmposvalid == 0)
			linestyleattr = (ssb.bFilledType ? SketchLineStyle.fillstylesymbinvalid : SketchLineStyle.linestylesymbinvalid);
		else
			linestyleattr = (ssb.bFilledType ? SketchLineStyle.fillstylesymb : SketchLineStyle.linestylesymb);
		ga.drawSymbol(this, linestyleattr);
	}

	/////////////////////////////////////////////
	OneSSymbol()
	{
	}

	/////////////////////////////////////////////
	OneSSymbol(float[] pco, int nlines, float zalt, SSymbolBase lssb, OnePath lop)
	{
		ssb = lssb;
		op = lop;
		// paxis = new Line2D.Float(pco[0], pco[1], pco[nlines * 2], pco[nlines * 2 + 1]);
		paxis = new Line2D.Float(pco[nlines * 2 - 2], pco[nlines * 2 - 1], pco[nlines * 2], pco[nlines * 2 + 1]);
		RefreshSymbol();
	}
}
