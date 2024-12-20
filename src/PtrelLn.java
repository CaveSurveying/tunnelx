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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;

import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.HashMap;
import java.util.Set; 
import java.util.HashSet; 

//
//
//
//
//

/////////////////////////////////////////////
// point relative to line
/////////////////////////////////////////////
class PtrelSLn
{
	Line2D.Double axis;

	double vax, vay; // vector
	double lgsq, lg; // length.

	double lam0, lam1; // vector displaced endpoints of the axis.

	double pvax, pvay; // perp vector

	double pad; // perp axis displacement.

	PtrelSLn(Line2D.Double laxis)
	{
		axis = laxis;
		vax = axis.getX2() - axis.getX1();
		vay = axis.getY2() - axis.getY1();

		lgsq = vax * vax + vay * vay;

		//check for point1 being the same as point2 else get divide by zero errors.
		//functions calling this should also check
		if(lgsq != 0)
		{
			lg = Math.sqrt(lgsq);

			lam0 = (vax * axis.getX1() + vay * axis.getY1()) / lgsq;
			lam1 = (vax * axis.getX2() + vay * axis.getY2()) / lgsq;

			pvax = -vay;
			pvay = vax;
			pad = (pvax * axis.getX1() + pvay * axis.getY1()) / lgsq;
		}
	}
};


/////////////////////////////////////////////
// by pairs
class PtrelPLn
{
	OnePath cp;
	OnePath crp;

	PtrelSLn ax0;
	PtrelSLn ax1;

	// mutable values
	double destx;
	double desty;
	double geoweight;  // additional weighting derived from the position of the point to line line
	double disttoaxis;

	// proxdistance weights at the end pathnodes of a path
	double proxdistw0;
	double proxdistw1;

	/////////////////////////////////////////////
	PtrelPLn(OnePath lcp, OnePath lcrp)
	{
		cp = lcp;
		crp = lcrp;
	}

	/////////////////////////////////////////////
	void MakePtlAxes()
	{
		Line2D.Double lax0 = new Line2D.Double(cp.pnstart.pn, cp.pnend.pn);
		Line2D.Double lax1 = new Line2D.Double(crp.pnstart.pn, crp.pnend.pn);

		ax0 = new PtrelSLn(lax0);
		ax1 = new PtrelSLn(lax1);
	}


	/////////////////////////////////////////////
	// calculates a weighting according to how on the face we are
	// this weight by orientation doesn't seem to help much, and in fact stops things on corners being pulled along enough
	double CalcGeoWeightFacing(double lam, double pd)
	{
		// distance of point from the ax0 line.
		double c = Math.abs(pd - ax0.pad) * ax0.lg;

		// distance of point along the ax0 line from closest point.
		double a = (lam - ax0.lam0) * ax0.lg;
		double b = (lam - ax0.lam1) * ax0.lg;

		double lgeoweight = 10000;
		if (c > 0.0001)
			lgeoweight = Math.abs(Math.atan(b / c) - Math.atan(a / c)) / c;
		else if ((Math.abs(a) > 0.0001) && (Math.abs(b) > 0.0001) && ((a < 0) == (b < 0)))
			lgeoweight = Math.abs(1 / a - 1 / b);
		if (lgeoweight > 10000)
			lgeoweight = 10000;
		return lgeoweight;
	}

	/////////////////////////////////////////////
	double CalcGeoWeightDistanceSQ(double lam, double pd)
	{
		// distance of point from the ax0 line.
		double c = Math.abs(pd - ax0.pad) * ax0.lg;
		disttoaxis = c;

		// distance of point along the ax0 line from closest point.
		double d = 0.0;
		if (lam < ax0.lam0)
			d = (ax0.lam0 - lam) * ax0.lg;
		else if (lam > ax0.lam1)
			d = (lam - ax0.lam1) * ax0.lg;
		double lgeoweight = c * c + d * d;
		return ax0.lg / (10.0 + lgeoweight);
	}

	/////////////////////////////////////////////
	void TransformPt(double x, double y)
	{
		//check for either ax0 or ax1 lines having zero length else get divide by zero errors.
		if ((ax0.lgsq!=0) && (ax1.lgsq!=0))
		{
			double lam = (ax0.vax * x + ax0.vay * y) / ax0.lgsq;
			double pd = (ax0.pvax * x + ax0.pvay * y) / ax0.lgsq;

			// calculate the geoweight.
			// geoweight = CalcGeoWeightFacing(lam, pd); // this one not so good for it
			geoweight = CalcGeoWeightDistanceSQ(lam, pd);

			// find the destination point
			double dlam = lam - ax0.lam0 + ax1.lam0;

			// factor out changes in width (same idea independently thought out from Walls)
			double wdfac = ax0.lg / ax1.lg;
			double dpd = (pd - ax0.pad) * wdfac + ax1.pad;

			destx = dlam * ax1.vax + dpd * ax1.pvax;
			desty = dlam * ax1.vay + dpd * ax1.pvay;
		}

			//if ax0 or ax1 are zero length set weight to zero so contribution ignored
		else
		{
			destx = 0;
			desty = 0;
			geoweight = 0;
		}
	}
};


/////////////////////////////////////////////
// there's a proximity engine, and this class balances out the 
// weights of all the proximities to discover the warping
class PtrelLn
{
	// corresponding arrays of path nodes.
	Map<OnePathNode, OnePathNode> opnMap = new HashMap<OnePathNode, OnePathNode>();
	List<PtrelPLn> wptrel = new ArrayList<PtrelPLn>();

	double destx;
	double desty;
	double destz;

	ProximityDerivation pd = null;
	Set<OnePathNode> cenconnnodes = new HashSet<OnePathNode>(); // set of nodes connected to the centreline
	AffineTransform ucavgtrans = new AffineTransform(); // applied to the unconnected pieces

	double realposterpaperscale;
	Vec3 sketchLocOffsetFrom;
	Vec3 sketchLocOffsetTo;

	/////////////////////////////////////////////
	PtrelLn()
	{;}

	/////////////////////////////////////////////
	void PrepareProximity(OneSketch isketch)
	{
		pd = new ProximityDerivation(isketch);
		pd.parainstancequeue.bDropdownConnectiveTraversed = true;
		pd.parainstancequeue.bCentrelineTraversed = true;
		pd.parainstancequeue.fcenlinelengthfactor = 10.0F; // factor of length added to centreline connections (to deal with vertical line cases)

		//clpaths = lclpaths;

		// extract correspondences between the nodes of the endpoints.
		// as well as the corresponding distortions.
		opnMap.clear();
		for (PtrelPLn wptreli : wptrel)
		{
			wptreli.MakePtlAxes();
			opnMap.put(wptreli.cp.pnstart, wptreli.crp.pnstart);
			opnMap.put(wptreli.cp.pnend, wptreli.crp.pnend);
		}
	}

	/////////////////////////////////////////////
	void PrepareForUnconnectedNodes(List<OnePathNode> vnodes)
	{
		// find the centreline nodes; reset the proxdists
		RefPathO srefpathconn = new RefPathO(); // reused object
		List<OnePathNode> lcenconnnodes = new ArrayList<OnePathNode>();
		for (OnePathNode opn : vnodes)
		{
			if (opn.IsCentrelineNode())
			{
				cenconnnodes.add(opn);
				lcenconnnodes.add(opn);
			}
		}
		assert pd.ncentrelinenodes == cenconnnodes.size();
		while (!lcenconnnodes.isEmpty())
		{
			OnePathNode copn = lcenconnnodes.remove(lcenconnnodes.size() - 1); 
			srefpathconn.ccopy(copn.ropconn); 
			do
			{
				OnePath cop = srefpathconn.op; 
				assert copn == srefpathconn.ToNode();
				OnePathNode ocopn = srefpathconn.FromNode();
				if (!cenconnnodes.contains(ocopn))
				{
					cenconnnodes.add(ocopn); 
					lcenconnnodes.add(ocopn);
				}
			}
			while (!srefpathconn.AdvanceRoundToNode(copn.ropconn));
		}
		TN.emitMessage("There are " + pd.ncentrelinenodes + " centreline nodes and " + cenconnnodes.size() + " centreline connected nodes out of " + vnodes.size() + " nodes."); 
	}
	

	/////////////////////////////////////////////
	boolean WarpOver(double x, double y, double z, float lam)
	{
		if (wptrel == null)
			{ destx = x;  desty = y;  destz = z;  return true;  }

		double sweight = 0;
		double sdestx = 0;
		double sdesty = 0;
		double sdestz = 0;
		for (PtrelPLn wptreli : wptrel)
		{
			wptreli.TransformPt(x, y);

			if (lam > 1.0F)
				lam = 1.0F;
			if (lam < 0.0F)
				lam = 0.0F;
			double aproxdist = wptreli.proxdistw0 * (1.0 - lam) + wptreli.proxdistw1 * lam;
			double proxweight = 1.0 / (1.0 + aproxdist * aproxdist);
			if ((wptreli.proxdistw0 == -1.0) || (wptreli.proxdistw1 == -1.0))
				proxweight = 0.0;

			// we just fiddle for something that might work
			// (is there a better way to combine these two measures??!)
			// multiplying them makes a big weight on one make the thing into a big weight
			//			double rweight = (proxweight + wptrel[i].weight) * wptrel[i].ax0.lgsq;
			//			double rweight = (proxweight) * wptrel[i].ax0.lgsq;
			double rweight = (proxweight) * wptreli.geoweight;
			//System.out.println(wptrel[i].proxdistw0 + " " + wptrel[i].proxdistw1 + " " + wptrel[i].disttoaxis);
			//			double rweight = wptrel[i].geoweight;

			sweight += rweight;

			sdestx += rweight * wptreli.destx;
			sdesty += rweight * wptreli.desty;
			sdestz += rweight * z;
		}

		if (sweight == 0.0) // bail out if no correspondences
		{
			destx = x;
			desty = y;
			destz = z;
			System.out.println("no weight (lack of connection?)");
			return false;
		}
		destx = sdestx / sweight;
		desty = sdesty / sweight;
		destz = sdestz / sweight;
		return true;
	}


	/////////////////////////////////////////////
	OnePathNode[] cennodes = null; //new OnePathNode[12]; // limit the number of nodes we average over. (null means no limit)
	// it seems not to work at all if you restrict the number of centre path nodes it links to.
	void SetNodeProxWeights(OnePathNode opn, int proxto)
	{
		if (wptrel == null) // bail out in no correspondences case
			return;
		pd.ShortestPathsToCentrelineNodes(opn, cennodes, null);
		for (int i = 0; i < wptrel.size(); i++)
		{
			OnePath opc = wptrel.get(i).cp;
			// maybe average does work, though small segments near
			// a node will get pulled much harder
			//			float nodew = (opc.pnstart.proxdist + opc.pnend.proxdist) / 2;
			double nodew = (opc.pnstart.proxdist * opc.pnend.proxdist);
			if ((opc.pnstart.proxdist == -1.0) || (opc.pnend.proxdist == -1.0))
				nodew = -1.0;

			if ((proxto & 1) != 0)
				wptrel.get(i).proxdistw0 = nodew;
			if ((proxto & 2) != 0)
				wptrel.get(i).proxdistw1 = nodew;
		}

		// reset for next application
		for (OnePathNode lopn : pd.parainstancequeue.proxdistsetlist)
			lopn.proxdist = -1.0; 
		pd.parainstancequeue.proxdistsetlist.clear(); 
	}

	/////////////////////////////////////////////
	void Extendallnodes(List<OnePathNode> vnodes)
	{
		int lastprogress = -1;
		for (int j = 0; j < vnodes.size(); j++)
		{
			OnePathNode opn = vnodes.get(j);
			if (!cenconnnodes.contains(opn))
			{
				assert !opnMap.containsKey(opn);
				OnePathNode dopn = new OnePathNode(0.0F, 0.0F, 0.0F);
				ucavgtrans.transform(opn.pn, dopn.pn);  // over-writes the origin position
				opnMap.put(opn, dopn);
				TN.emitWarning("   unconn-node " + j);
			}
			else if (!opnMap.containsKey(opn))
			{
				SetNodeProxWeights(opn, 3);
				boolean bD = WarpOver(opn.pn.getX(), opn.pn.getY(), opn.zalt, 0.0F);
				OnePathNode dopn = new OnePathNode((float)destx, (float)desty, (float)destz);
				if (!bD)
					TN.emitWarning("   bad node " + j);
				opnMap.put(opn, dopn);
			}

			int progress = (20*j) / vnodes.size();
			if (progress > lastprogress)
			{
				lastprogress = progress;
				TN.emitMessage(Integer.toString(5*progress) + "% complete");
			}
		}
	}

	/////////////////////////////////////////////
	Point2D.Float spnF = new Point2D.Float();  // used for mapping the avgtransform to
	Point2D.Float spnT = new Point2D.Float();  // used for mapping the avgtransform to
	OnePath WarpPathD(OnePath path, String limportfromname)
	{
// Must Also map over all the subsets, if there are any made to avoid XC subsets merging

		// new endpoint nodes
		OnePathNode npnstart = opnMap.get(path.pnstart);
		OnePathNode npnend = opnMap.get(path.pnend);

		OnePath res = new OnePath(npnstart);
		float[] pco = path.GetCoords();

		assert cenconnnodes.contains(path.pnstart) == cenconnnodes.contains(path.pnend);
		if (!cenconnnodes.contains(path.pnstart))
		{
			for (int i = 1; i < path.nlines; i++)
			{
				spnF.setLocation(pco[i * 2], pco[i * 2 + 1]);
				ucavgtrans.transform(spnF, spnT);
				res.LineTo((float)spnT.getX(), (float)spnT.getY());
			}
		}

		else
		{
			SetNodeProxWeights(path.pnstart, 1);
			SetNodeProxWeights(path.pnend, 2);

			float partlinelength = 0.0F;
			for (int i = 1; i < path.nlines; i++)
			{
				float vx = pco[i * 2] - pco[i * 2 - 2];
				float vy = pco[i * 2 + 1] - pco[i * 2 - 1];
				partlinelength += (float)Math.sqrt(vx * vx + vy * vy);

				float lam = partlinelength / path.linelength;
				WarpOver(pco[i * 2], pco[i * 2 + 1], 0.0F, lam);
				res.LineTo((float)destx, (float)desty);
			}
		}

		res.EndPath(npnend);
		res.CopyPathAttributes(path);

		if ((res.plabedl != null) && (res.plabedl.sketchframedef != null))
			res.plabedl.sketchframedef.ConvertTransformImportSketchWarp(path, res, realposterpaperscale, sketchLocOffsetFrom, sketchLocOffsetTo);

		res.importfromname = limportfromname;

		return res;
	}





	/////////////////////////////////////////////
	void CalcAvgTransform(AffineTransform avgtrans, SketchFrameDef sketchframedef, OneSketch tsketch, OneSketch asketch)
	{
		// we're working on the diagram as a unit, rather than averaging across the change on all the legs.
		// so we find the centre of gravity of each.
		// then average expansion from the c of g.  and the rotational components around this,
		// to the centre of each line weighted by its length.
        
		// centre of gravity
		double cgxf = 0.0F;
		double cgyf = 0.0F;
		double tlengf = 0.0F;
		double cgxt = 0.0F;
		double cgyt = 0.0F;
		double tlengt = 0.0F;

		// first find the centres of gravity.
		for (PtrelPLn wptreli : wptrel)
		{
			OnePath cp = wptreli.cp;
			OnePath crp = wptreli.crp;

			double lengf = cp.pnstart.pn.distance(cp.pnend.pn);
			double lengt = crp.pnstart.pn.distance(crp.pnend.pn);

			cgxf += lengf * (cp.pnend.pn.getX() + cp.pnstart.pn.getX()) / 2;
			cgyf += lengf * (cp.pnend.pn.getY() + cp.pnstart.pn.getY()) / 2;
			tlengf += lengf;

			cgxt += lengt * (crp.pnend.pn.getX() + crp.pnstart.pn.getX()) / 2;
			cgyt += lengt * (crp.pnend.pn.getY() + crp.pnstart.pn.getY()) / 2;
			tlengt += lengt;
		}
		if (tlengf != 0.0F)
		{
			cgxf /= tlengf;
			cgyf /= tlengf;
		}
		if (tlengt != 0.0F)
		{
			cgxt /= tlengt;
			cgyt /= tlengt;
		}


		// now find average scale and rotation relative to these c of g.
		double tleng = 0.0F;

		double tscale = 0.0F;
		double trot = 0.0F;

		for (PtrelPLn wptreli : wptrel)
		{
			OnePath cp = wptreli.cp;
			OnePath crp = wptreli.crp;

			double leng = cp.pnstart.pn.distance(cp.pnend.pn);
			double lengr = crp.pnstart.pn.distance(crp.pnend.pn);
			double aleng = (leng + lengr) / 2;

			double cxf = (cp.pnend.pn.getX() + cp.pnstart.pn.getX()) / 2 - cgxf;
			double cyf = (cp.pnend.pn.getY() + cp.pnstart.pn.getY()) / 2 - cgyf;
			double cflq = cxf * cxf + cyf * cyf;

			double cxt = (crp.pnend.pn.getX() + crp.pnstart.pn.getX()) / 2 - cgxt;
			double cyt = (crp.pnend.pn.getY() + crp.pnstart.pn.getY()) / 2 - cgyt;
			double ctlq = cxt * cxt + cyt * cyt;

			tscale += aleng * (cflq != 0.0 ? Math.sqrt(ctlq / cflq) : 1.0);


			double cxr = cxf * cxt + cyf * cyt;
			double cyr = cxf * cyt - cyf * cxt;
			double ang = (cxr != 0 ? Math.atan(cyr / cxr) : 0.0);  // alt should be +- PI/2
			trot += ang * aleng;

			tleng += aleng;
		}

		if (tleng != 0)
		{
			tscale /= tleng;
			trot /= tleng;
		}

		// transform, conijugated with the translation.
		TN.emitMessage("Avg transform scale " + tscale + " translate " + (cgxt - cgxf) + " " + (cgyt - cgyf) + "  rot " + trot);

        avgtrans.setToIdentity();
        avgtrans.translate(cgxt, cgyt);
        avgtrans.scale(tscale, tscale);
        avgtrans.rotate(trot);
        avgtrans.translate(-cgxf, -cgyf);
        
        if (sketchframedef != null)
        {
            assert (sketchframedef != null) && (tsketch != null); 
            sketchframedef.sfscaledown = (float)(tsketch.realposterpaperscale / tscale);
            sketchframedef.sfrotatedeg = -(float)Math.toDegrees(trot);
            sketchframedef.sfelevrotdeg = 0.0F; 
            sketchframedef.sfelevvertplane = ""; 

            // From UpdateSketchFrame()
            // Lots of effort to extract the transform required
            AffineTransform lpframesketchtrans = new AffineTransform(); 
            lpframesketchtrans.translate(-tsketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, +tsketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
            lpframesketchtrans.scale(tsketch.realposterpaperscale / sketchframedef.sfscaledown, tsketch.realposterpaperscale / sketchframedef.sfscaledown);
            lpframesketchtrans.rotate(-Math.toRadians(sketchframedef.sfrotatedeg));
            lpframesketchtrans.translate(asketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -asketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);

            double dx = avgtrans.getTranslateX() - lpframesketchtrans.getTranslateX(); 
            double dy = avgtrans.getTranslateY() - lpframesketchtrans.getTranslateY(); 
            sketchframedef.sfxtrans = dx / (tsketch.realposterpaperscale * TN.CENTRELINE_MAGNIFICATION); 
            sketchframedef.sfytrans = dy / (tsketch.realposterpaperscale * TN.CENTRELINE_MAGNIFICATION); 
            
            /*double[] flatmatrix = new double[6]; 
            avgtrans.getMatrix(flatmatrix); 
            System.out.println("\navgtrans "); 
            for (int i = 0; i < 6; i++)
                System.out.println("  "+flatmatrix[i]); 
            lpframesketchtrans = new AffineTransform(); 
            lpframesketchtrans.translate((-tsketch.sketchLocOffset.x + sketchframedef.sfxtrans * tsketch.realposterpaperscale) * TN.CENTRELINE_MAGNIFICATION, (+tsketch.sketchLocOffset.y + sketchframedef.sfytrans * tsketch.realposterpaperscale) * TN.CENTRELINE_MAGNIFICATION);
            lpframesketchtrans.scale(tsketch.realposterpaperscale / sketchframedef.sfscaledown, tsketch.realposterpaperscale / sketchframedef.sfscaledown);
            lpframesketchtrans.rotate(-Math.toRadians(sketchframedef.sfrotatedeg));
            lpframesketchtrans.translate(asketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -asketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
            lpframesketchtrans.getMatrix(flatmatrix); 
            System.out.println("\n--- lpframesketchtrans "); 
            for (int i = 0; i < 6; i++)
                System.out.println("  "+flatmatrix[i]); 
            */
//            lpframesketchtrans.translate((-tsketch.sketchLocOffset.x + sketchframedef.sfxtrans * tsketch.realpaperscale) * TN.CENTRELINE_MAGNIFICATION, (+tsketch.sketchLocOffset.sketchLocOffset.y + sketchframedef.sfytrans * tsketch.sketchLocOffset.realpaperscale) * TN.CENTRELINE_MAGNIFICATION);
        }
    }



	/////////////////////////////////////////////
	/////////////////////////////////////////////
	boolean ExtractCentrelinePathCorrespondence(OneSketch asketch, OneSketch osdest, float blockmappingscore)
	{
		// new correspondence engine
		MatchSketchCentrelines msc = new MatchSketchCentrelines();
		if (!msc.CorrespMatching(asketch.vpaths, osdest.vpaths, blockmappingscore))
		{
			TN.emitWarning("no corresponding centrelines found2");
			return false; 
		}
		
        int nmisscorresp = 0; 
		for (PrefixLeg plf : msc.prefixlegsfrom)
		{
			if (plf.pltmember != null)
				wptrel.add(new PtrelPLn(plf.op, plf.pltmember.op));
			else
            {
                nmisscorresp++; 
                if (nmisscorresp <= 10)
    				TN.emitWarning("No centreline corresponding to " + "tail=" + plf.op.plabedl.centrelinetail + " head=" + plf.op.plabedl.centrelinehead);
            }
		}
        if (nmisscorresp > 10)
    	   TN.emitWarning("No centreline corresponding to ... " + (nmisscorresp - 10) + " more.");


		// false if no correspondence
		if (wptrel.isEmpty())
		{
			TN.emitWarning("no corresponding centrelines found1");
			return false;
		}
		return true;
	}
};



