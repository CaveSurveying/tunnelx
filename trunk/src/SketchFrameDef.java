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

import java.io.StringReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;


import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Color;

import java.awt.geom.Line2D;
//import java.awt.geom.Line2D.Float;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;



////////////////////////////////////////////////////////////////////////////////
class SketchFrameDef
{
	// when barea_pres_signal is ASE_SKETCHFRAME, sketchframe
	float sfscaledown = 1.0F;
	float sfrotatedeg = 0.0F;
	float sfxtrans = 0.0F;
	float sfytrans = 0.0F;
	String sfsketch = "";
	String sfstyle = "";

	OneSketch pframesketch = null;
	FileAbstraction pframeimage = null;
	AffineTransform pframesketchtrans = null;

	/////////////////////////////////////////////
	SketchFrameDef()
	{
	}

	/////////////////////////////////////////////
	SketchFrameDef(SketchFrameDef o)
	{
		sfscaledown = o.sfscaledown;
		sfrotatedeg = o.sfrotatedeg;
		sfxtrans = o.sfxtrans;
		sfytrans = o.sfytrans;
		sfsketch = o.sfsketch;
		sfstyle = o.sfstyle;
	}


	/////////////////////////////////////////////
	void UpdateSketchFrame(OneSketch lpframesketch, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		pframesketch = lpframesketch;
		pframesketchtrans = new AffineTransform();
		assert (pframesketch == null) || (pframeimage == null);

		if (pframeimage != null)
		{
lrealpaperscale = 1.0;
			pframesketchtrans.translate(-lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, +lsketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
			pframesketchtrans.translate(sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION, sfytrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION);
			if (sfscaledown != 0.0)
				pframesketchtrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
			if (sfrotatedeg != 0.0)
				pframesketchtrans.rotate(-Math.toRadians(sfrotatedeg));
		}

		else if (pframesketch != null)
		{
			pframesketchtrans.translate(-lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, +lsketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
			pframesketchtrans.translate(sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION, sfytrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION);
			if (sfscaledown != 0.0)
				pframesketchtrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
			if (sfrotatedeg != 0.0)
				pframesketchtrans.rotate(-Math.toRadians(sfrotatedeg));
			pframesketchtrans.translate(pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
		}
	}

	/////////////////////////////////////////////
	// reverse of decoding for saving
	void WriteXML(String areasigsketchname, double nodeconnzsetrelative, LineOutputStream los, int indent) throws IOException
	{
		// the area signal
		los.WriteLine(TNXML.xcom(indent, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, areasigsketchname, TNXML.sASIG_FRAME_SCALEDOWN, String.valueOf(sfscaledown), TNXML.sASIG_FRAME_ROTATEDEG, String.valueOf(sfrotatedeg), TNXML.sASIG_FRAME_XTRANS, String.valueOf(sfxtrans), TNXML.sASIG_FRAME_YTRANS, String.valueOf(sfytrans), TNXML.sASIG_FRAME_SKETCH, sfsketch, TNXML.sASIG_FRAME_STYLE, sfstyle, TNXML.sASIG_NODECONN_ZSETRELATIVE, String.valueOf(nodeconnzsetrelative)));
	}


	/////////////////////////////////////////////
	String TransCenButtF(int typ, OneSArea osa, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		Rectangle2D areabounds = osa.rboundsarea;
		Rectangle2D rske = pframesketch.getBounds(false, false);
		assert areabounds != null;
		assert rske != null;

		// generate the tail set of transforms in order
		AffineTransform aftrans = new AffineTransform();
		if ((typ != 0) && (sfscaledown != 0.0))
			aftrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
		if (sfrotatedeg != 0.0)
			aftrans.rotate(-Math.toRadians(sfrotatedeg));
		aftrans.translate(pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
		rske = aftrans.createTransformedShape(rske).getBounds();

		String sval;
		if (typ == 0)
		{
			double lscale = Math.max(rske.getWidth() / areabounds.getWidth(), rske.getHeight() / areabounds.getHeight()) * lrealpaperscale;
			if (lscale > 100.0)
				lscale = Math.ceil(lscale);
			sval = String.valueOf((float)lscale);
		}
		else
		{
			double cx = rske.getX() + rske.getWidth() / 2;
			double cy = rske.getY() + rske.getHeight() / 2;

			double dcx = areabounds.getX() + areabounds.getWidth() / 2;
			double dcy = areabounds.getY() + areabounds.getHeight() / 2;

			//dcx = cx + sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION
			double lsfxtrans = ((dcx - cx) / TN.CENTRELINE_MAGNIFICATION + lsketchLocOffset.x) / lrealpaperscale;
			double lsfytrans = ((dcy - cy) / TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.y) / lrealpaperscale;
			if (typ == 1)
				sval = String.valueOf((float)lsfxtrans);
			else
				sval = String.valueOf((float)lsfytrans);
		}
		return sval;
	}
	
	/////////////////////////////////////////////
	void SetSketchFrameFiller(OneTunnel ot, MainBox mainbox, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		OneSketch lpframesketch;
		if (sfsketch.endsWith(TN.SUFF_PNG) || sfsketch.endsWith(TN.SUFF_JPG))
		{
			FileAbstraction idir = ot.tundirectory;
			pframeimage = SketchBackgroundPanel.GetImageFile(idir, sfsketch);
System.out.println("jdjdj  " + pframeimage.toString());
			lpframesketch = null;
		}
		else
			lpframesketch = ot.FindSketchFrame(sfsketch, mainbox);
		UpdateSketchFrame(lpframesketch, lrealpaperscale, lsketchLocOffset);
	}

}


