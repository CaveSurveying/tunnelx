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
import java.awt.Dimension;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
//import java.awt.geom.Line2D.Float;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.geom.NoninvertibleTransformException; 


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
	boolean IsImageType()
	{
		return (sfsketch.endsWith(TN.SUFF_PNG) || sfsketch.endsWith(TN.SUFF_JPG));
	}

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

		if (IsImageType())
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
	OnePath MakeBackgroundOutline(double lrealpaperscale, Vec3 lsketchLocOffset)
	{
System.out.println("eeeeep"); 
		if (pframeimage == null)
			return null; 

		BufferedImage bi = pframeimage.GetImage(true);
		System.out.println("FFS " + bi.getWidth() + "  " + bi.getHeight());
		int biw = (bi.getWidth() == -1 ? 400 : bi.getWidth());
		int bih = (bi.getHeight() == -1 ? 400 : bi.getHeight());
		Point2D[] cproj = new Point2D[4];
		for (int i = 0; i < 4; i++)
			cproj[i] = new Point2D.Double(); 
		TransformBackiPT(0.0, 0.0, lrealpaperscale, lsketchLocOffset, cproj[0]);
		TransformBackiPT(biw, 0.0, lrealpaperscale, lsketchLocOffset, cproj[1]);
		TransformBackiPT(biw, bih, lrealpaperscale, lsketchLocOffset, cproj[2]);
		TransformBackiPT(0.0, bih, lrealpaperscale, lsketchLocOffset, cproj[3]);
System.out.println(cproj[0].getX() + " --------------  " + cproj[0].getY()); 
	
		OnePathNode opns = new OnePathNode((float)cproj[0].getX(), (float)cproj[0].getY(), 0.0F);
		OnePath gop = new OnePath(opns); 
		gop.LineTo((float)cproj[1].getX(), (float)cproj[1].getY());
		gop.LineTo((float)cproj[2].getX(), (float)cproj[2].getY());
		gop.LineTo((float)cproj[3].getX(), (float)cproj[3].getY());
		gop.EndPath(opns);
		
		gop.linestyle = SketchLineStyle.SLS_CONNECTIVE;
		gop.bWantSplined = false; 
		gop.plabedl = new PathLabelDecode();

		gop.plabedl.barea_pres_signal = SketchLineStyle.ASE_SKETCHFRAME; // just now need to find where it is in the list in the combo-box
		gop.plabedl.iarea_pres_signal = SketchLineStyle.iareasigframe; 
		gop.plabedl.sketchframedef = new SketchFrameDef();
		return gop;
	}


	/////////////////////////////////////////////
	void MaxCentreOnScreenButt(Dimension lcsize, boolean bmaxcen, double lrealpaperscale, Vec3 lsketchLocOffset, AffineTransform ucurrtrans)
	{
		Point2D[] corners = new Point2D[4];
System.out.println("DDD " + lcsize);
		if (IsImageType())
		{
			if (pframeimage != null)
			{
				BufferedImage bi = pframeimage.GetImage(true);
				System.out.println("FFS " + bi.getWidth() + "  " + bi.getHeight());
				int biw = (bi.getWidth() == -1 ? 400 : bi.getWidth());
				int bih = (bi.getHeight() == -1 ? 400 : bi.getHeight());
				corners[0] = new Point2D.Double(0.0, 0.0);
				corners[1] = new Point2D.Double(biw, 0.0);
				corners[2] = new Point2D.Double(0.0, bih);
				corners[3] = new Point2D.Double(biw, bih);
			}
			else
				TN.emitWarning("No frame image pframeimage"); 
		}
		else
		{
			if (pframesketch == null)
				return;
			Rectangle2D rske = pframesketch.getBounds(false, false);
System.out.println("RSKK " + rske);
			corners[0] = new Point2D.Double(rske.getX(), rske.getY());
			corners[1] = new Point2D.Double(rske.getX() + rske.getWidth(), rske.getY());
			corners[2] = new Point2D.Double(rske.getX(), rske.getY() + rske.getHeight());
			corners[3] = new Point2D.Double(rske.getX() + rske.getWidth(), rske.getY() + rske.getHeight());
  		}

		Point2D[] cproj = new Point2D[8];
		for (int i = 0; i < 8; i++)
			cproj[i] = new Point2D.Double();

		// find the scale change that would fit
		if (bmaxcen)
		{
			for (int i = 0; i < 4; i++)
			{
				TransformBackiPT(corners[i].getX(), corners[i].getY(), lrealpaperscale, lsketchLocOffset, cproj[i + 4]);
				ucurrtrans.transform(cproj[i + 4], cproj[i]);
			}
			double xmin = Math.min(Math.min(cproj[0].getX(), cproj[1].getX()), Math.min(cproj[2].getX(), cproj[3].getX()));
			double xmax = Math.max(Math.max(cproj[0].getX(), cproj[1].getX()), Math.max(cproj[2].getX(), cproj[3].getX()));
			double ymin = Math.min(Math.min(cproj[0].getY(), cproj[1].getY()), Math.min(cproj[2].getY(), cproj[3].getY()));
			double ymax = Math.max(Math.max(cproj[0].getY(), cproj[1].getY()), Math.max(cproj[2].getY(), cproj[3].getY()));

System.out.println("XX " + xmin + "  " + xmax);
System.out.println("XX " + ymin + "  " + xmax);
			double sca = Math.max((xmax - xmin) / lcsize.getWidth(), (ymax - ymin) / lcsize.getHeight());
System.out.println("XX " + ymin + "  " + xmax);
			sfscaledown *= sca;
		}

		// centre case
		for (int i = 0; i < 4; i++)
			TransformBackiPT(corners[i].getX(), corners[i].getY(), lrealpaperscale, lsketchLocOffset, cproj[i]);

		double xcen = (cproj[0].getX() + cproj[1].getX() + cproj[2].getX() + cproj[3].getX()) / 4;
		double ycen = (cproj[0].getY() + cproj[1].getY() + cproj[2].getY() + cproj[3].getY()) / 4;

		try { ucurrtrans.inverseTransform(new Point2D.Double(lcsize.getWidth() / 2, lcsize.getHeight() / 2), cproj[4]); }
		catch (NoninvertibleTransformException e) {;};

		sfxtrans += (float)((cproj[4].getX() - xcen) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		sfytrans += (float)((cproj[4].getY() - ycen) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
	}

	/////////////////////////////////////////////
	void SetSketchFrameFiller(OneTunnel ot, MainBox mainbox, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		OneSketch lpframesketch;
		if (IsImageType())
		{
			FileAbstraction idir = ot.tundirectory;
			pframeimage = SketchBackgroundPanel.GetImageFile(idir, sfsketch);
System.out.println("jdjdj  " + pframeimage.toString());
			lpframesketch = null;
		}
		else
		{
			lpframesketch = ot.FindSketchFrame(sfsketch, mainbox);
			pframeimage = null; // total chaos going on here
		}
		UpdateSketchFrame(lpframesketch, lrealpaperscale, lsketchLocOffset);
	}


	/////////////////////////////////////////////
	// doesn't work very effectively
	void ConvertSketchTransform(AffineTransform lat, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		AffineTransform at = (lat != null ? new AffineTransform(lat) : new AffineTransform());

		// supposed to undo:
		//pframesketchtrans.translate(-lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, +lsketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
		//pframesketchtrans.translate(sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION, sfytrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION);
		//pframesketchtrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
		//pframesketchtrans.rotate(-Math.toRadians(sfrotatedeg));
		//pframesketchtrans.translate(pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);

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
		System.out.println("SSS " + lsketchLocOffset.x + "  " + lsketchLocOffset.y);
		System.out.println("TTT " + at.getTranslateX() + "  " + at.getTranslateY());

		sfxtrans = (float)((at.getTranslateX() + lsketchLocOffset.x) / TN.CENTRELINE_MAGNIFICATION / lrealpaperscale);
		sfytrans = (float)((at.getTranslateY() - lsketchLocOffset.y) / TN.CENTRELINE_MAGNIFICATION / lrealpaperscale);

		sfscaledown = (float)(scale0 != 0.0 ? (lrealpaperscale / scale0) : 0.0F);
		sfrotatedeg = -(float)rot0;
	}


// to compare the application of TransformPT to the matrix value
// get the inverse transfor
//make void InverseTransformBackiPT
//the translation will be the difference of the two.

	/////////////////////////////////////////////
	void TransformBackiPT(double x, double y, double lrealpaperscale, Vec3 lsketchLocOffset, Point2D res)
	{
		double cx, cy;
		if (IsImageType())
		{
			cx = x;
			cy = y;
			assert lrealpaperscale == 1.0;
		}
		else
		{
			cx = x + pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION;
			cy = y - pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION;
  		}

		double sfrotaterad = Math.toRadians(sfrotatedeg);
		double sinr = Math.sin(-sfrotaterad);
		double cosr = Math.cos(-sfrotaterad);

		double crx = cx * cosr - cy * sinr;
		double cry = cy * cosr + cx * sinr;

		res.setLocation(crx * lrealpaperscale / sfscaledown + sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION,
						cry * lrealpaperscale / sfscaledown + sfytrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION + lsketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
	}

	/////////////////////////////////////////////
	void InverseTransformBackiPT(double x, double y, double lrealpaperscale, Vec3 lsketchLocOffset, Point2D res)
	{
		double crx = (x - sfxtrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION + lsketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION) * sfscaledown / lrealpaperscale;
		double cry = (y - sfytrans * lrealpaperscale * TN.CENTRELINE_MAGNIFICATION - lsketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION) * sfscaledown / lrealpaperscale;

		double sfrotaterad = Math.toRadians(sfrotatedeg);
		double sinr = Math.sin(sfrotaterad);
		double cosr = Math.cos(sfrotaterad);

		double cx = crx * cosr - cry * sinr;
		double cy = cry * cosr + crx * sinr;

		if (IsImageType())
		{
			res.setLocation(cx, cy);
			assert lrealpaperscale == 1.0;
		}
		else
			res.setLocation(cx - pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION,
							cy + pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
	}

	/////////////////////////////////////////////
	void ConvertSketchTransformT(float[] pco, int nlines, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		if (nlines == 1)
		{
			sfxtrans += (float)((pco[2] - pco[0]) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
			sfytrans += (float)((pco[3] - pco[1]) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		}

		if (nlines == 2)
		{
			Point2D ppres = new Point2D.Double();
			InverseTransformBackiPT(pco[0], pco[1], lrealpaperscale, lsketchLocOffset, ppres);
System.out.println("PPres0 " + ppres);

			float x2 = pco[4] - pco[0];
			float y2 = pco[5] - pco[1];
			float x1 = pco[2] - pco[0];
			float y1 = pco[3] - pco[1];
			double len2 = Math.hypot(x2, y2);
			double len1 = Math.hypot(x1, y1);
			double len12 = len1 * len2;
			if (len12 == 0.0F)
				return;

			double dot12 = (x1 * x2 + y1 * y2) / len12;
			double dot1p2 = (x1 * y2 - y1 * x2) / len12;
			double sca = len2 / len1;

			double ang = Math.toDegrees(Math.atan2(dot1p2, dot12));
System.out.println("AAA: " + ang + "  " + sca);
			sfscaledown /= sca;
			sfrotatedeg -= ang;
			TransformBackiPT(ppres.getX(), ppres.getY(), lrealpaperscale, lsketchLocOffset, ppres);

			sfxtrans += (float)((pco[0] - ppres.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
			sfytrans += (float)((pco[1] - ppres.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));

InverseTransformBackiPT(pco[0], pco[1], lrealpaperscale, lsketchLocOffset, ppres);
System.out.println("PPres1 " + ppres);
		}
	}


	/////////////////////////////////////////////
	void ConvertTransformImportSketchWarp(OnePath opfrom, OnePath opto, double lrealpaperscale, Vec3 lsketchLocOffsetFrom, Vec3 lsketchLocOffsetTo)
	{
System.out.println("Sketchloc offs XFT " + lsketchLocOffsetFrom.x + "  " + lsketchLocOffsetTo.x); 
		System.out.println("FFFF " + opfrom.pnstart.pn + "  " + opfrom.pnend.pn);
		System.out.println("TTTT " + opto.pnstart.pn + "  " + opto.pnend.pn);
// this is the final place where work needs to happen.
		if (!IsImageType() && (pframesketch == null))
		{
			TN.emitWarning("Nothing on this frame type");
			return;
		}

		Point2D ppgoF = new Point2D.Double();
		Point2D ppgoF0 = new Point2D.Double();
		TransformBackiPT(opfrom.pnstart.pn.getX(), opfrom.pnstart.pn.getY(), lrealpaperscale, lsketchLocOffsetFrom, ppgoF);
		TransformBackiPT(0.0, 0.0, lrealpaperscale, lsketchLocOffsetFrom, ppgoF0);
		double fvx = ppgoF0.getX() - opfrom.pnstart.pn.getX(); 
		double fvy = ppgoF0.getY() - opfrom.pnstart.pn.getY(); 
System.out.println("PPres0 " + ppgoF);

		double x1 = opfrom.pnend.pn.getX() - opfrom.pnstart.pn.getX();
		double y1 = opfrom.pnend.pn.getY() - opfrom.pnstart.pn.getY();
		double x2 = opto.pnend.pn.getX() - opto.pnstart.pn.getX();
		double y2 = opto.pnend.pn.getY() - opto.pnstart.pn.getY();

		if ((x1 == 0.0) && (y1 == 0.0)) 
		{
			float[] pcof = opfrom.GetCoords();
			x1 = pcof[2] - opfrom.pnstart.pn.getX(); 
			y1 = pcof[3] - opfrom.pnstart.pn.getY(); 
			float[] pcot = opto.GetCoords();
			x2 = pcot[2] - opto.pnstart.pn.getX(); 
			y2 = pcot[3] - opto.pnstart.pn.getY(); 
		}

		double len2 = Math.hypot(x2, y2);
		double len1 = Math.hypot(x1, y1);
		double len12 = len1 * len2;
		if (len12 != 0.0F)
		{
			double dot12 = (x1 * x2 + y1 * y2) / len12;
			double dot1p2 = (x1 * y2 - y1 * x2) / len12;
			double sca = len2 / len1;

			double ang = Math.toDegrees(Math.atan2(dot1p2, dot12));
System.out.println("A-AAA: " + ang + "  " + sca);
			sfscaledown /= sca;
			sfrotatedeg -= ang;
		}
		else
			TN.emitWarning("need to pick a better pair of points"); 		
//		double cosang = Math.cos(ang); 
//		double sinang = Math.sin(ang); 

		Point2D ppgoT = new Point2D.Double();
		Point2D ppgoT0 = new Point2D.Double();
		TransformBackiPT(opto.pnstart.pn.getX(), opto.pnstart.pn.getY(), lrealpaperscale, lsketchLocOffsetTo, ppgoT);
		TransformBackiPT(0.0, 0.0, lrealpaperscale, lsketchLocOffsetTo, ppgoT0);

//		double rfvx = (fvx * cosang - fvy * sinang) * sca; 
//		double rfvy = (fvy * cosang + fvx * sinang) * sca; 
		double rfvx = fvx; 
		double rfvy = fvy; 
System.out.println("  rrrfv " + rfvx + " " + rfvy);

//T + (F - F0) 
//		sfxtrans += (float)((ppgoF.getX() - ppgoT0.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
//		sfytrans += (float)((ppgoF.getY() - ppgoT0.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		sfxtrans += (float)((rfvx + opto.pnstart.pn.getX() - ppgoT0.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		sfytrans += (float)((rfvy + opto.pnstart.pn.getY() - ppgoT0.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));

//		sfxtrans += (float)(-lsketchLocOffsetFrom.x + lsketchLocOffsetTo.x); 
//		sfytrans += (float)(-lsketchLocOffsetFrom.y + lsketchLocOffsetTo.y); 

//		sfxtrans += (float)((opfrom.pnstart.pn.getX() - opto.pnstart.pn.getX()) / TN.CENTRELINE_MAGNIFICATION);
//		sfytrans += (float)((opfrom.pnstart.pn.getY() - opto.pnstart.pn.getY()) / TN.CENTRELINE_MAGNIFICATION);
System.out.println("PPresT " + ppgoT);
		TransformBackiPT(0, 0, lrealpaperscale, lsketchLocOffsetTo, ppgoT);
System.out.println("      NNN PPresT " + ppgoT);
System.out.println("XXX " + (opfrom.pnstart.pn.getX() - opto.pnstart.pn.getX())); 
System.out.println("  YYY " + (opfrom.pnstart.pn.getY() - opto.pnstart.pn.getY())); 

	}

}


