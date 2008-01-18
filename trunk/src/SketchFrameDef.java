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
import java.util.Map;
import java.util.TreeMap;
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
class SketchFrameDef implements Comparable<SketchFrameDef>
{
		float sfscaledown = 1.0F;
		float sfrotatedeg = 0.0F;
		float sfxtrans = 0.0F;
		float sfytrans = 0.0F;
	AffineTransform pframesketchtrans = null;

	Map<String, String> submapping = new TreeMap<String, String>();
	String sfstyle = "";

		OneSketch pframesketch = null;
		FileAbstraction pframeimage = null;
	String sfsketch = "";

	float sfnodeconnzsetrelative = 0.0F;

	int distinctid; // used for the comparator as this is in a hashset
	static int Sdistinctid = 1;


	/////////////////////////////////////////////
	String GetToTextV()
	{
		StringBuffer sb = new StringBuffer();
		TNXML.sbstartxcom(sb, 0, TNXML.sASIGNAL_SKETCHFRAME);
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_SCALEDOWN, String.valueOf(sfscaledown));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_ROTATEDEG, String.valueOf(sfrotatedeg));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_XTRANS, String.valueOf(sfxtrans));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_YTRANS, String.valueOf(sfytrans));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_NODECONN_ZSETRELATIVE, String.valueOf(sfnodeconnzsetrelative));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_SKETCH, sfsketch);
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_STYLE, sfstyle);
		TNXML.sbendxcom(sb);
		sb.append(TN.nl);

		for (String ssubset : submapping.keySet())
		{
			sb.append(TNXML.xcom(0, TNXML.sSUBSET_ATTRIBUTES, TNXML.sSUBSET_NAME, ssubset, TNXML.sUPPER_SUBSET_NAME, submapping.get(ssubset)));
			sb.append(TN.nl);
		}
		sb.append(TNXML.xcomclose(0, TNXML.sASIGNAL_SKETCHFRAME));
		return sb.toString();
	}

	/////////////////////////////////////////////
	SketchFrameDef()
	{
		distinctid = Sdistinctid++;
	}

	/////////////////////////////////////////////
	void copy(SketchFrameDef o)
	{
		sfscaledown = o.sfscaledown;
		sfrotatedeg = o.sfrotatedeg;
		sfxtrans = o.sfxtrans;
		sfytrans = o.sfytrans;
		sfsketch = o.sfsketch;
		sfstyle = o.sfstyle;
		sfnodeconnzsetrelative = o.sfnodeconnzsetrelative;
		submapping.clear();
		submapping.putAll(o.submapping);
	}

	/////////////////////////////////////////////
	boolean compare(SketchFrameDef o)
	{
		if (o == null)
			return false; 
		if ((sfscaledown != o.sfscaledown) || (sfrotatedeg != o.sfrotatedeg) || 
			(sfxtrans != o.sfxtrans) || (sfytrans != o.sfytrans) ||
			(sfsketch != o.sfsketch) || (sfstyle != o.sfstyle) ||
			(sfnodeconnzsetrelative != o.sfnodeconnzsetrelative))
			return false;
		if (!submapping.equals(o.submapping))
			return false; 
		return true;
	}

	/////////////////////////////////////////////
	SketchFrameDef(SketchFrameDef o)
	{
		copy(o);
	}

	/////////////////////////////////////////////
	public int compareTo(SketchFrameDef o)
	{
		if (sfnodeconnzsetrelative != o.sfnodeconnzsetrelative)
			return (sfnodeconnzsetrelative - o.sfnodeconnzsetrelative < 0.0F ? -1 : 1);
		return distinctid - o.distinctid;
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
	void WriteXML(String areasigsketchname, LineOutputStream los, int indent) throws IOException
	{
		// the area signal
		los.WriteLine(TNXML.xcomopen(indent, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, areasigsketchname, TNXML.sASIG_FRAME_SCALEDOWN, String.valueOf(sfscaledown), TNXML.sASIG_FRAME_ROTATEDEG, String.valueOf(sfrotatedeg), TNXML.sASIG_FRAME_XTRANS, String.valueOf(sfxtrans), TNXML.sASIG_FRAME_YTRANS, String.valueOf(sfytrans), TNXML.sASIG_FRAME_SKETCH, sfsketch, TNXML.sASIG_FRAME_STYLE, sfstyle, TNXML.sASIG_NODECONN_ZSETRELATIVE, String.valueOf(sfnodeconnzsetrelative)));
		for (String ssubset : submapping.keySet())
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sSUBSET_ATTRIBUTES, TNXML.sSUBSET_NAME, ssubset, TNXML.sUPPER_SUBSET_NAME, submapping.get(ssubset)));
		los.WriteLine(TNXML.xcomclose(indent, TNXML.sPC_AREA_SIGNAL));
	}


	/////////////////////////////////////////////
	void TransCenButtF(boolean bmaxcen, OneSArea osa, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		Rectangle2D areabounds = osa.rboundsarea;
		Rectangle2D rske = pframesketch.getBounds(false, false);
		assert areabounds != null;
		assert rske != null;

		// need to work out why Max doesn't Centre it as well at the same time.  go over calcs again.

		// generate the tail set of transforms in order
		AffineTransform aftrans = new AffineTransform();
		if (!bmaxcen && (sfscaledown != 0.0))
			aftrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
		if (sfrotatedeg != 0.0)
			aftrans.rotate(-Math.toRadians(sfrotatedeg));
		aftrans.translate(pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
		rske = aftrans.createTransformedShape(rske).getBounds();

		String sval;
		if (bmaxcen)
		{
			double lscale = Math.max(rske.getWidth() / areabounds.getWidth(), rske.getHeight() / areabounds.getHeight()) * lrealpaperscale;
			if (lscale > 100.0)
				lscale = Math.ceil(lscale);
			sfscaledown = (float)lscale;
		}

		double cx = rske.getX() + rske.getWidth() / 2;
		double cy = rske.getY() + rske.getHeight() / 2;

		double dcx = areabounds.getX() + areabounds.getWidth() / 2;
		double dcy = areabounds.getY() + areabounds.getHeight() / 2;

		//dcx = cx + sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION
		sfxtrans = (float)(((dcx - cx) / TN.CENTRELINE_MAGNIFICATION + lsketchLocOffset.x) / lrealpaperscale);
		sfytrans = (float)(((dcy - cy) / TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.y) / lrealpaperscale);
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


	/////////////////////////////////////////////
	void ConvertSketchTransform(AffineTransform lat, Vec3 lsketchLocOffset)
	{
		AffineTransform at = (lat != null ? new AffineTransform(lat) : new AffineTransform());

System.out.println("atatat " + at.toString());
		AffineTransform nontrat = new AffineTransform(at.getScaleX(), at.getShearY(), at.getShearX(), at.getScaleY(), 0.0, 0.0);
		double x0 = at.getScaleX();
		double y0 = at.getShearY();

		double x1 = at.getShearX();
		double y1 = at.getScaleY();

		double scale0 = Math.sqrt(x0 * x0 + y0 * y0);
		double scale1 = Math.sqrt(x1 * x1 + y1 * y1);

		//System.out.println("scsc " + scale0 + "  " + scale1);

		double rot0 = Vec3.DegArg(x0, y0);
		double rot1 = Vec3.DegArg(x1, y1);

		//System.out.println("rtrt " + rot0 + "  " + rot1);

		sfxtrans = (float)((at.getTranslateX() + lsketchLocOffset.x) / TN.CENTRELINE_MAGNIFICATION);
		sfytrans = (float)((at.getTranslateY() - lsketchLocOffset.y) / TN.CENTRELINE_MAGNIFICATION);

		sfscaledown = (float)(scale0 != 0.0 ? (1.0 / scale0) : 0.0F);
		sfrotatedeg = -(float)rot0;
	}
}


