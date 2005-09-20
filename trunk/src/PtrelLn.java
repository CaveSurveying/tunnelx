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
import java.util.Vector;
import java.util.List;

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
	PtrelPLn(Line2D.Double lax0, Line2D.Double lax1)
	{
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

			// try to factor out changes in width
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
class PtrelLn
{
	// corresponding arrays of path nodes.
	Vector opnm = new Vector();
	Vector opncorr = new Vector();

	PtrelPLn[] wptrel;

	double destx;
	double desty;
	double destz;


	ProximityDerivation pd = null;
    	List<OnePath> clpaths;

	/////////////////////////////////////////////
	PtrelLn(List<OnePath> lclpaths, List<OnePath> corrpaths, OneSketch isketch)
	{
		pd = new ProximityDerivation(isketch, true);
		clpaths = lclpaths;

		if (clpaths == null)
		{
			wptrel = null;
			return;
		}

		// extract correspondences between the nodes of the endpoints.
		// as well as the corresponding distortions.
		wptrel = new PtrelPLn[clpaths.size()];

		for (int i = 0; i < clpaths.size(); i++)
		{
			OnePath cp = clpaths.get(i);
			OnePath crp = corrpaths.get(i);

			wptrel[i] = new PtrelPLn(new Line2D.Double(cp.pnstart.pn, cp.pnend.pn), new Line2D.Double(crp.pnstart.pn, crp.pnend.pn));

			boolean bheadnew = true;
			boolean btailnew = true;

			for (int j = 0; j < opnm.size(); j++)
			{
				bheadnew &= (opnm.elementAt(j) != cp.pnstart);
				btailnew &= (opnm.elementAt(j) != cp.pnend);
			}

			if (bheadnew)
			{
				opnm.addElement(cp.pnstart);
				opncorr.addElement(crp.pnstart);
			}
			if (btailnew)
			{
				opnm.addElement(cp.pnend);
				opncorr.addElement(crp.pnend);
			}
		}
	}

	/////////////////////////////////////////////
	void WarpOver(double x, double y, double z, float lam)
	{
		double sweight = 0;
		double sdestx = 0;
		double sdesty = 0;
		double sdestz = 0;

		for (int i = 0; i < wptrel.length; i++)
		{
			wptrel[i].TransformPt(x, y);

			if (lam > 1.0F)
				lam = 1.0F;
			if (lam < 0.0F)
				lam = 0.0F;
			double aproxdist = wptrel[i].proxdistw0 * (1.0 - lam) + wptrel[i].proxdistw1 * lam;
			double proxweight = 1.0 / (1.0 + aproxdist * aproxdist);
			if ((wptrel[i].proxdistw0 == -1.0) || (wptrel[i].proxdistw1 == -1.0))
				proxweight = 0.0;

			// we just fiddle for something that might work
			// (is there a better way to combine these two measures??!)
			// multiplying them makes a big weight on one make the thing into a big weight
//			double rweight = (proxweight + wptrel[i].weight) * wptrel[i].ax0.lgsq;
//			double rweight = (proxweight) * wptrel[i].ax0.lgsq;
			double rweight = (proxweight) * wptrel[i].geoweight;
//System.out.println(wptrel[i].proxdistw0 + " " + wptrel[i].proxdistw1 + " " + wptrel[i].disttoaxis);
//			double rweight = wptrel[i].geoweight;

			sweight += rweight;

			sdestx += rweight * wptrel[i].destx;
			sdesty += rweight * wptrel[i].desty;
			sdestz += rweight * z;
		}

		if (sweight == 0.0) // bail out if no correspondences
		{
			destx = x;
			desty = y;
			destz = z;
System.out.println("no weight (lack of connection?)");
			return;
		}
		destx = sdestx / sweight;
		desty = sdesty / sweight;
		destz = sdestz / sweight;
	}


	/////////////////////////////////////////////
	OnePathNode[] cennodes = null; //new OnePathNode[12]; // limit the number of nodes we average over. (null means no limit)
	// it seems not to work at all if you restrict the number of centre path nodes it links to.
	void SetNodeProxWeights(OnePathNode opn, int proxto)
	{
		if (clpaths == null) // bail out in no correspondences case
			return;
		pd.ShortestPathsToCentrelineNodes(opn, cennodes);
		for (int i = 0; i < clpaths.size(); i++)
		{
			OnePath opc = clpaths.get(i);
			// maybe average does work, though small segments near
			// a node will get pulled much harder
//			float nodew = (opc.pnstart.proxdist + opc.pnend.proxdist) / 2;
			double nodew = (opc.pnstart.proxdist * opc.pnend.proxdist);
			if ((opc.pnstart.proxdist == -1.0) || (opc.pnend.proxdist == -1.0))
				nodew = -1.0;

			if ((proxto & 1) != 0)
				wptrel[i].proxdistw0 = nodew;
			if ((proxto & 2) != 0)
				wptrel[i].proxdistw1 = nodew;
		}
	}

	/////////////////////////////////////////////
	void Extendallnodes(Vector vnodes)
	{
		int lastprogress = -1;
		for (int j = 0; j < vnodes.size(); j++)
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(j);
			for (int i = 0; i < opnm.size(); i++)
			{
				if (opnm.elementAt(i) == opn)
				{
					opn = null;
					break;
				}
			}

			if (opn != null)
			{
				SetNodeProxWeights(opn, 3);
				WarpOver(opn.pn.getX(), opn.pn.getY(), opn.zalt, 0.0F);
				opnm.addElement(opn);
				opncorr.addElement(new OnePathNode((float)destx, (float)desty, (float)destz, opn.bzaltset));
			}

			int progress = (20*j) / vnodes.size();
			if ( progress > lastprogress )
			{
				lastprogress = progress;
				TN.emitMessage(Integer.toString(5*progress) + "% complete");
			}

		}
	}

	/////////////////////////////////////////////
	OnePathNode NodeCorr(OnePathNode opn)
	{
		for (int i = 0; i < opnm.size(); i++)
		{
			if (opn == (OnePathNode)opnm.elementAt(i))
				return (OnePathNode)opncorr.elementAt(i);
		}
		return null;
	}


	/////////////////////////////////////////////
	OnePath WarpPath(OnePath path, String limportfromname)
	{
		// new endpoint nodes
		OnePathNode npnstart = NodeCorr(path.pnstart);
		OnePathNode npnend = NodeCorr(path.pnend);

		SetNodeProxWeights(path.pnstart, 1);
		SetNodeProxWeights(path.pnend, 2);

		float[] pco = path.GetCoords();
		OnePath res = new OnePath(npnstart);

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

		res.EndPath(npnend);
		res.CopyPathAttributes(path);
		res.importfromname = limportfromname;

		return res;
	}





	/////////////////////////////////////////////
	static void CalcAvgTransform(AffineTransform avgtrans, List<OnePath> clpaths, List<OnePath> corrpaths)
	{
		avgtrans.setToIdentity();

		// no correspondence case
		assert ((clpaths == null) == (corrpaths == null));
		if (clpaths == null)
			return; // at identity

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
		for (int i = 0; i < clpaths.size(); i++)
		{
			OnePath cp = clpaths.get(i);
			OnePath crp = corrpaths.get(i);

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

		for (int i = 0; i < clpaths.size(); i++)
		{
			OnePath cp = clpaths.get(i);
			OnePath crp = corrpaths.get(i);

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

		//avgtrans.setToIdentity();
		avgtrans.translate(cgxt, cgyt);
		avgtrans.scale(tscale, tscale);
		avgtrans.rotate(trot);
		avgtrans.translate(-cgxf, -cgyf);
	}
};



