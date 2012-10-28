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
import java.util.Collections;

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



/////////////////////////////////////////////
class ElevCLine implements Comparable<ElevCLine>
{
    Line2D cline; 
    double tz0; 
    double tz1; 
String csubset; 
    SubsetAttr subsetattr; 

    ElevCLine(OnePath op, Vec3 sketchLocOffset, double coselevrot, double sinelevrot)
    {
        double x0 = op.pnstart.pn.getX() + sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION; 
        double y0 = op.pnstart.pn.getY() - sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION; 
        double z0 = op.pnstart.zalt + sketchLocOffset.z * TN.CENTRELINE_MAGNIFICATION;

        double x1 = op.pnend.pn.getX() + sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION; 
        double y1 = op.pnend.pn.getY() - sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION; 
        double z1 = op.pnend.zalt + sketchLocOffset.z * TN.CENTRELINE_MAGNIFICATION; 

        double tx0 = coselevrot * x0 + sinelevrot * y0; 
        double ty0 = -sinelevrot * x0 + coselevrot * y0; 
        double tx1 = coselevrot * x1 + sinelevrot * y1; 
        double ty1 = -sinelevrot * x1 + coselevrot * y1; 

        //cline = new Line2D.Double(tx0, -z0, tx1, -z1);
		// put back into same coordinate framework so offsets (in the 90, 180 and 360 directions are consistent and can be aligned
		cline = new Line2D.Double(tx0 - sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -z0 + sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION,
								  tx1 - sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -z1 + sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
		tz0 = ty0; 
        tz1 = ty1; 
csubset = (op.vssubsets.isEmpty() ? "" : op.vssubsets.get(op.vssubsets.size() - 1)); 
        subsetattr = op.subsetattr; 
    }

    /////////////////////////////////////////////
    public int compareTo(ElevCLine ecl)
    {
        double zdiff = (tz0 + tz1) - (ecl.tz0 + ecl.tz1); 
        return (zdiff > 0.0 ? 1 : (zdiff < 0.0 ? -1 : 0)); 
    }
}

////////////////////////////////////////////////////////////////////////////////
class SketchFrameDef
{
    float sfscaledown = 1.0F;
    float sfrotatedeg = 0.0F;
    float sfelevrotdeg = 0.0F;    // disabled by 0.  use 360 to get that direction (only applies to sketches that contain centrelines)
    String sfelevvertplane = "";  // either blank or "n0n1" for the node pair we are tied to
    double sfxtrans = 0.0F;
    double sfytrans = 0.0F;
        // could also define a restricted x-y area of the bitmap to plot (esp for the case of cross-sections)
    AffineTransform pframesketchtrans = null;

	String sfstyle = "";
	Map<String, String> submapping = new TreeMap<String, String>();

	String sfsketch = "";
	OneSketch pframesketch = null;
    FileAbstraction pframeimage = null;
    int imagepixelswidth = -1; 
    int imagepixelsheight = -1; 

	float sfnodeconnzsetrelative = 0.0F;

	int distinctid; // used for the comparator as this is in a hashset
	static int Sdistinctid = 1;

    List<ElevCLine> elevclines = null; 

	/////////////////////////////////////////////
	boolean IsImageType()
	{
		// no endsWithIgnoreCase function
		return (sfsketch.toLowerCase().endsWith(TN.SUFF_PNG) || sfsketch.toLowerCase().endsWith(TN.SUFF_JPG));
	}

	/////////////////////////////////////////////
    BufferedImage SetImageWidthHeight()
    {
		BufferedImage bi = pframeimage.GetImage(true);
		//System.out.println("FFS " + bi.getWidth() + "  " + bi.getHeight());
        if (bi.getWidth() != -1)
            imagepixelswidth = bi.getWidth(); 
        if (bi.getHeight() != -1)
            imagepixelsheight = bi.getHeight(); 
        return bi; 
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
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_ELEVROTDEG, String.valueOf(sfelevrotdeg));
		sb.append(TN.nl);
		TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_ELEVVERTPLANE, sfelevvertplane);
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
        //if ((imagepixelswidth != -1) || (imagepixelsheight != -1))
        {
            sb.append(TN.nl);
            TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_IMGPIXELWIDTH, String.valueOf(imagepixelswidth));
            sb.append(TN.nl);
            TNXML.sbattribxcom(sb, TNXML.sASIG_FRAME_IMGPIXELHEIGHT, String.valueOf(imagepixelsheight));
        }

		TNXML.sbendxcom(sb);
		sb.append(TN.nl);

        // sort the mappings by uppersubset, which will be listed first to make them line up
        List<String> sattrlines = new ArrayList<String>(); 
		for (String ssubset : submapping.keySet())
			sattrlines.add(TNXML.xcom(0, TNXML.sSUBSET_ATTRIBUTES, TNXML.sUPPER_SUBSET_NAME, submapping.get(ssubset), TNXML.sSUBSET_NAME, ssubset));
        Collections.sort(sattrlines); 
        for (String sattrline : sattrlines)
        {
			sb.append(sattrline);
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
	void Copy(SketchFrameDef o, boolean bAll)
	{
		if (bAll || !o.sfsketch.equals(""))
        {
            sfscaledown = o.sfscaledown;
            sfrotatedeg = o.sfrotatedeg;
            sfelevrotdeg = o.sfelevrotdeg;
            sfelevvertplane = o.sfelevvertplane;
            sfxtrans = o.sfxtrans;
            sfytrans = o.sfytrans;
            sfsketch = o.sfsketch;
            sfstyle = o.sfstyle;
            sfnodeconnzsetrelative = o.sfnodeconnzsetrelative;
            imagepixelswidth = o.imagepixelswidth;
            imagepixelsheight = o.imagepixelsheight;
        }
        
        if (bAll || !o.submapping.isEmpty())
		{
            submapping.clear();
    		submapping.putAll(o.submapping);
        }
	}

	/////////////////////////////////////////////
	SketchFrameDef(SketchFrameDef o)
	{
		Copy(o, true);
	}


	/////////////////////////////////////////////
	// to find the transform of background image/sketch that is in plan or of type n0n1 elevation
	AffineTransform MakeVertplaneTransform(AffineTransform ucurrtrans, OnePath fop)
	{
		if (sfelevvertplane.equals(""))
			return new AffineTransform(ucurrtrans); 
			
		assert (sfelevvertplane.equals("n0n1")); 
		assert fop != null; 
		assert fop.plabedl.sketchframedef == this; // normal case
		float[] pco = fop.GetCoords(); 
		Point2D ptsrc = new Point2D.Double(); 
		Point2D ptdst = new Point2D.Double(); 
		
		// [ m00x + m01y + m02, m10x + m11y + m12 ]
		// c=pco[0], v=Norm(pco[2]-pco[0])
		// 0,0 -> c = m02, m12
		// 1,0 -> c+v = m00+m02, m10+m12
		// 0,1 -> c+(0, fac)= m01+m02, m11+m12
		ptsrc.setLocation(pco[0], pco[1]); 
		ucurrtrans.transform(ptsrc, ptdst); 
		double m02 = ptdst.getX(); 
		double m12 = ptdst.getY(); 

		double pvx = pco[2] - pco[0]; 
		double pvy = pco[3] - pco[1]; 
		double pvlen = Math.sqrt(pvx*pvx + pvy*pvy); 
		ptsrc.setLocation(pco[0] + pvx/pvlen, pco[1] + pvy/pvlen); 
		ucurrtrans.transform(ptsrc, ptdst); 
		double m00 = ptdst.getX() - m02; 
		double m10 = ptdst.getY() - m12; 

		double scaX = Math.sqrt(ucurrtrans.getScaleX()*ucurrtrans.getScaleX() + ucurrtrans.getShearX()*ucurrtrans.getShearX()); 
		double scaY = Math.sqrt(ucurrtrans.getScaleY()*ucurrtrans.getScaleY() + ucurrtrans.getShearY()*ucurrtrans.getShearY()); 
		double scaTilt = scaY / scaX;
		assert scaTilt <= 1.001; 
		if (scaTilt > 0.999)
			scaTilt = 1.0; 
			
		// edge on case
		if ((scaTilt == 1.0) || (m00 == 0.0))
			return null; 
			
		ptsrc.setLocation(pco[0], pco[1] + 1.0); 
		ucurrtrans.transform(ptsrc, ptdst); 
		double m01 = 0.0; 
		double m11 = scaX*Math.sqrt(1.0 - scaTilt*scaTilt); 
		
		return new AffineTransform(m00, m10, m01, m11, m02, m12); 
	}
	
	/////////////////////////////////////////////
	// there are some unfortunate mixups with the lrealpaperscale used for insetting sketches at scale 1:1000 onto a poster sensibly
	// which are not good for images (pixels are approx 1:1000) or cross-sections 
	// (n0n1 sfelevvertplane, meaning aligned along node0->node1 of the defining path of the cross-section, pref at scale 1.0
	void UpdateSketchFrame(OneSketch lpframesketch, double lrealpaperscale, Vec3 lsketchLocOffset)
	{
		pframesketch = lpframesketch;
		pframesketchtrans = new AffineTransform();
		
		assert (pframesketch == null) || (pframeimage == null);
		assert sfelevvertplane.equals("") || sfelevvertplane.equals("n0n1"); 
		assert lrealpaperscale == (IsImageType() || sfelevvertplane.equals("n0n1") ? 1.0 : 1000.0); // as we migrate to setting the value properly on the outside
			
		if (sfelevvertplane.equals(""))  // for normal background case
			pframesketchtrans.translate((-lsketchLocOffset.x + sfxtrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION, (+lsketchLocOffset.y + sfytrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION);
		else
			pframesketchtrans.translate((sfxtrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION, (sfytrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION);

		if (sfscaledown != 0.0)
			pframesketchtrans.scale(lrealpaperscale / sfscaledown, lrealpaperscale / sfscaledown);
		if (sfrotatedeg != 0.0)
			pframesketchtrans.rotate(-Math.toRadians(sfrotatedeg));

		if (pframesketch != null)
			pframesketchtrans.translate(pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
	}

	/////////////////////////////////////////////
	// reverse of decoding for saving
	void WriteXML(String areasigsketchname, LineOutputStream los, int indent) throws IOException
	{
		// the area signal
		los.WriteLine(TNXML.xcomopen(indent, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, areasigsketchname, TNXML.sASIG_FRAME_SCALEDOWN, String.valueOf(sfscaledown), TNXML.sASIG_FRAME_ROTATEDEG, String.valueOf(sfrotatedeg), TNXML.sASIG_FRAME_ELEVROTDEG, String.valueOf(sfelevrotdeg), TNXML.sASIG_FRAME_ELEVVERTPLANE, sfelevvertplane, TNXML.sASIG_FRAME_XTRANS, String.valueOf(sfxtrans), TNXML.sASIG_FRAME_YTRANS, String.valueOf(sfytrans), TNXML.sASIG_FRAME_SKETCH, sfsketch, TNXML.sASIG_FRAME_STYLE, sfstyle, TNXML.sASIG_NODECONN_ZSETRELATIVE, String.valueOf(sfnodeconnzsetrelative), TNXML.sASIG_FRAME_IMGPIXELWIDTH, String.valueOf(imagepixelswidth), TNXML.sASIG_FRAME_IMGPIXELHEIGHT, String.valueOf(imagepixelsheight)));
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

		SetImageWidthHeight(); 
		int biw = (imagepixelswidth == -1 ? 400 : imagepixelswidth);
		int bih = (imagepixelsheight == -1 ? 400 : imagepixelsheight);
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
	void MaxCentreOnScreenButt(Dimension lcsize, boolean bmaxcen, double lrealposterpaperscale, Vec3 lsketchLocOffset, AffineTransform ucurrtrans)
	{
		Point2D[] corners = new Point2D[4];
		double lrealpaperscale = (IsImageType() || sfelevvertplane.equals("n0n1") ? 1.0 : lrealposterpaperscale); 
System.out.println("DDD " + lcsize);
		if (IsImageType())
		{
			if (pframeimage != null)
			{
                SetImageWidthHeight(); 
                int biw = (imagepixelswidth == -1 ? 400 : imagepixelswidth);
                int bih = (imagepixelsheight == -1 ? 400 : imagepixelsheight);
				corners[0] = new Point2D.Double(0.0, 0.0);
				corners[1] = new Point2D.Double(biw, 0.0);
				corners[2] = new Point2D.Double(0.0, bih);
				corners[3] = new Point2D.Double(biw, bih);
			}
			else
			{
				TN.emitWarning("No frame image pframeimage"); 
				return; 
			}
		}
		else
		{
			if (pframesketch == null)
				return;

            Rectangle2D rske; 
		    if (sfelevrotdeg == 0.0)
    			rske = pframesketch.getBounds(false, false);
            else
            {
                MakeElevClines(); 
                if (elevclines.isEmpty())
                    return; 
                rske = elevclines.get(0).cline.getBounds(); 
                for (ElevCLine ecl : elevclines)
                    rske.add(ecl.cline.getBounds());  
            }

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

		sfxtrans += ((cproj[4].getX() - xcen) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		sfytrans += ((cproj[4].getY() - ycen) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
	}

	/////////////////////////////////////////////
	void SetSketchFrameFiller(MainBox mainbox, double lrealposterpaperscale, Vec3 lsketchLocOffset, FileAbstraction fasketch)
	{
		OneSketch lpframesketch = null;
		if (IsImageType())
		{
			pframeimage = FileAbstraction.GetImageFile(fasketch, sfsketch);
System.out.println("jdjdj  " + (pframeimage != null ? pframeimage.toString() : "null"));
		}
		else
		{
// this should worry about the sketches that have not yet been saved but exist in the box window
System.out.println("MMMMMM " + fasketch + "  " +  sfsketch);
			FileAbstraction pframesketch = FileAbstraction.GetImageFile(fasketch, TN.setSuffix(sfsketch, TN.SUFF_XML));
			if (pframesketch != null)
				lpframesketch = mainbox.FindSketchFrame(mainbox.GetActiveTunnelSketches(), pframesketch);
			pframeimage = null; // total chaos going on here
		}
		double lrealpaperscale = (IsImageType() || sfelevvertplane.equals("n0n1") ? 1.0 : lrealposterpaperscale); 
		UpdateSketchFrame(lpframesketch, lrealpaperscale, lsketchLocOffset);
	}




// to compare the application of TransformPT to the matrix value
// get the inverse transform
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
	boolean ConvertSketchTransformT(float[] pco, int nlines, double lrealposterpaperscale, Vec3 lsketchLocOffset, AffineTransform ucurrtrans, OnePath fop)
	{
		Point2D p0 = new Point2D.Double(pco[0], pco[1]);
		Point2D p1 = new Point2D.Double(pco[2], pco[3]);
		Point2D p2 = (nlines == 2 ? new Point2D.Double(pco[4], pco[5]) : null); 
		
		if (!sfelevvertplane.equals(""))
		{
			// transform back onto the screen, then transform back to the coordinates of the elevation thing
			AffineTransform vptrans = MakeVertplaneTransform(ucurrtrans, fop); 
			if (vptrans == null)
				return TN.emitWarning("MakeVertplaneTransform says we are edge on");
			
			ucurrtrans.transform(p0, p0); 
			ucurrtrans.transform(p1, p1); 
			if (p2 != null)
				ucurrtrans.transform(p1, p1); 
			try 
			{ 
				vptrans.inverseTransform(p0, p0); 
				vptrans.inverseTransform(p1, p1); 
				if (p2 != null)
					vptrans.inverseTransform(p2, p2); 
			}
			catch (NoninvertibleTransformException e) 
			{
				return TN.emitWarning("Cannot invert vptrans");
			};
		}
			
		double lrealpaperscale = (IsImageType() || sfelevvertplane.equals("n0n1") ? 1.0 : lrealposterpaperscale); 
		if (p2 != null)
		{
			Point2D ppres = new Point2D.Double();
			InverseTransformBackiPT(p0.getX(), p0.getY(), lrealpaperscale, lsketchLocOffset, ppres);
System.out.println("PPres0 " + ppres);

			double x2 = p2.getX() - p0.getX();
			double y2 = p2.getY() - p0.getY();
			double x1 = p1.getX() - p0.getX();
			double y1 = p1.getY() - p0.getY();
			double len2 = Math.hypot(x2, y2);
			double len1 = Math.hypot(x1, y1);
			double len12 = len1 * len2;
			if (len12 == 0.0F)
				return TN.emitWarning("Cannot scale/rotate from or to zero vector");

			double dot12 = (x1 * x2 + y1 * y2) / len12;
			double dot1p2 = (x1 * y2 - y1 * x2) / len12;
			double sca = len2 / len1;

			double ang = Math.toDegrees(Math.atan2(dot1p2, dot12));
System.out.println("AAA: " + ang + "  " + sca);
			sfscaledown /= sca;
			sfrotatedeg -= ang;
			TransformBackiPT(ppres.getX(), ppres.getY(), lrealpaperscale, lsketchLocOffset, ppres);

			sfxtrans += ((p0.getX() - ppres.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
			sfytrans += ((p0.getY() - ppres.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));

InverseTransformBackiPT(pco[0], pco[1], lrealpaperscale, lsketchLocOffset, ppres);
System.out.println("PPres1 " + ppres);
		}
		else
		{
			sfxtrans += ((p1.getX() - p0.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
			sfytrans += ((p1.getY() - p0.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		}
		return true; 
	}


	/////////////////////////////////////////////
	void ConvertTransformImportSketchWarp(OnePath opfrom, OnePath opto, double lrealposterpaperscale, Vec3 lsketchLocOffsetFrom, Vec3 lsketchLocOffsetTo)
	{
		double lrealpaperscale = (IsImageType() || sfelevvertplane.equals("n0n1") ? 1.0 : lrealposterpaperscale); 
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
		sfxtrans += ((rfvx + opto.pnstart.pn.getX() - ppgoT0.getX()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));
		sfytrans += ((rfvy + opto.pnstart.pn.getY() - ppgoT0.getY()) / (lrealpaperscale * TN.CENTRELINE_MAGNIFICATION));

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


	/////////////////////////////////////////////
    void MakeElevClines()
    {
        elevclines = new ArrayList<ElevCLine>(); 
        double elevrotrad = Math.toRadians(sfelevrotdeg); 
        double coselevrot = Math.cos(elevrotrad); 
        double sinelevrot = Math.sin(elevrotrad); 
        for (OnePath op : pframesketch.vpaths)
        {
            if ((op.linestyle == SketchLineStyle.SLS_CENTRELINE) && (op.pnstart != null))
                elevclines.add(new ElevCLine(op, pframesketch.sketchLocOffset, coselevrot, sinelevrot)); 
        }
        Collections.sort(elevclines);
        TN.emitMessage("Made " + elevclines.size() + " elecvlines"); 
    }

	/////////////////////////////////////////////
    // centreline elevation mode
    void paintWelevSketch(GraphicsAbstraction ga, SubsetAttrStyle sksas)
    {
        MakeElevClines(); 
        for (ElevCLine ecl : elevclines)
        {
            String ssubset = ecl.csubset; 
            String lssubset = submapping.get(ssubset);
            if ((lssubset != null) && !lssubset.equals(""))
                ssubset = lssubset;

            SubsetAttr subsetattr = sksas.msubsets.get(ssubset);
            if (subsetattr == null)
    			subsetattr = sksas.sadefault; 

            subsetattr = ecl.subsetattr; 

            if (subsetattr.linestyleattrs[SketchLineStyle.SLS_CENTRELINE].strokecolour != null)
                ga.drawShape(ecl.cline, subsetattr.linestyleattrs[SketchLineStyle.SLS_CENTRELINE]); 
        }
    }
}



