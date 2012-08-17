////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program  
// Copyright (C) 2012  Julian Todd.  
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

//
//
// DelTriangulation
//
//

/////////////////////////////////////////////
class DelPoint implements Comparable<DelPoint>
{
    Point2D.Float pn; // either it's own object 
    OnePathNode opn = null; // or lifted from OnePathNode
    RefPathO refpath = null;  // or lifted from OnePath
    float i; // not 0, but 1 for first in path node and -1 for past in path node
    DelEdge dpedgeconn = null; // one connection
    
	public int compareTo(DelPoint dp)
	{
		if (pn.x != dp.pn.x)
			return (pn.x < dp.pn.x ? -1 : 1);
		if (pn.y != dp.pn.y)
			return (pn.y < dp.pn.y ? -1 : 1);
        return 0; 
    }
    
    DelPoint(RefPathO lrefpath, float li, float u, float v)
    {
        refpath = lrefpath; 
        i = li; 
        //int ir = (refpath.bFore ? refpath.op.nlines - i : i);
        pn = new Point2D.Float(u, v); 
    }

    DelPoint(float u, float v)
    {
        pn = new Point2D.Float(u, v); 
    }

    DelPoint(OnePathNode lopn)
    {
        opn = lopn; 
        pn = opn.pn; 
    }
    
    DelEdge FindEdgeConn(DelPoint dpoth)
    {
        DelEdge de = dpedgeconn; 
        do 
        {
            if (dpoth == (de.a == this ? de.b : de.a))
                return de; 
            de = (de.a == this ? de.derightback : de.deleftfore); 
        }  while (de != dpedgeconn); 
        return null; 
    }
    
    double FindEdgeCross(DelPoint dpoth)
    {
        double vx = dpoth.pn.getX() - pn.getX(); 
        double vy = dpoth.pn.getY() - pn.getY(); 
        double vsq = vx*vx + vy*vy; 
        DelEdge de = dpedgeconn; 
        do   
        {
            DelEdge denext = (de.a == this ? de.derightback : de.deleftfore); 
            DelEdge decross = (denext.a == this ? denext.deleftfore : denext.derightback); 
            double vax = decross.a.pn.getX() - pn.getX(); 
            double vay = decross.a.pn.getY() - pn.getY(); 
            double vbx = decross.b.pn.getX() - pn.getX(); 
            double vby = decross.b.pn.getY() - pn.getY(); 
            double apd = -vax * vy + vay * vx; 
            double bpd = -vbx * vy + vby * vx; 
            if ((apd < 0.0) != (bpd < 0.0))
            {
                double lam = -apd / (bpd - apd); 
                //System.out.println(" lam " + lam); 
                double mua = (vax * vx + vay * vy) / vsq; 
                double mub = (vbx * vx + vby * vy) / vsq; 
                double mu = mua * (1 - lam) + mub * lam; 
                //System.out.println(" mu " + mu); 
                if ((mu > 0.0001) && (mu < 0.9999))
                    return mu; 
            }
            de = denext; 
        }  while (de != dpedgeconn); 
        return -1.0; 
    }

}

/////////////////////////////////////////////
class DelEdge
{
    DelPoint a; 
    DelPoint b; 
    DelEdge derightback = null; 
    DelEdge deleftfore = null; 
    int sig = 0; // +1 in right, -1 in left, +2 full outside, -2 full inside 
    
    DelEdge(DelPoint la, DelPoint lb)
    {
        a = la; 
        b = lb; 
    }
    
    boolean DEDPsort()
    {
        if (a.compareTo(b) == -1)
            return true; 
        DelPoint c = a; 
        a = b; 
        b = c; 
        DelEdge ldeleftfore = derightback; 
        derightback = deleftfore; 
        deleftfore = ldeleftfore; 
        return false; 
    }
    
    boolean PtOnRight(double x, double y)
    {
        double vx = b.pn.getX() - a.pn.getX(); 
        double vy = b.pn.getY() - a.pn.getY(); 
        double sx = x - a.pn.getX(); 
        double sy = y - a.pn.getY(); 
        // CPerp(v) = (-vy, vx)
        double vds = -vy * sx + vx * sy; 
        return vds > 0.0; 
        // later do this properly
    }
    
    boolean RefTriangle()
    {
        return ((derightback != null) && (a == derightback.a)); 
    }

    boolean InTriangle(double x, double y)
    {
        if (!RefTriangle())
            return false; 
        if (!PtOnRight(x, y))
            return false; 
        if (derightback.PtOnRight(x, y))
            return false; 
        DelEdge detop = derightback.deleftfore; 
        return ((detop.a == b) == detop.PtOnRight(x, y)); 
    }

    boolean IsDelauney()
    {
        if ((derightback == null) || (deleftfore == null))
            return true; 
        DelPoint rp = (derightback.b == a ? derightback.a : derightback.b); 
        DelPoint lp = (deleftfore.b == b ? deleftfore.a : deleftfore.b); 
        //System.out.println(" a " + a.pn.getX() + "," + a.pn.getY()); 
        double sx = (b.pn.getX() - a.pn.getX()) / 2; 
        double sy = (b.pn.getY() - a.pn.getY()) / 2; 
        double vx = rp.pn.getX() - a.pn.getX(); 
        double vy = rp.pn.getY() - a.pn.getY(); 
        double vsq = vx*vx + vy*vy; 
        double ssq = sx*sx + sy*sy; 
        double sdv = sx*vx + sy*vy; 
        double spdv = -sy*vx + sx*vy; 
        assert spdv > -0.0001*ssq; 
        if (spdv < 0.0001*ssq)
            return !((0.0 <= sdv) && (sdv <= 2*ssq)); 
        double k = (vsq/2 - sdv) / spdv; 
        double rsq = ssq * (1 + k*k); 
        double cx = a.pn.getX() + sx - sy * k; 
        double cy = a.pn.getY() + sy + sx * k; 
        //System.out.println(" x " +(Math.sqrt((rp.pn.getX() - cx)*(rp.pn.getX() - cx) + (rp.pn.getY() - cy)*(rp.pn.getY() - cy)) - Math.sqrt(rsq))); 
        //System.out.println(Math.sqrt((b.pn.getX() - cx)*(b.pn.getX() - cx) + (b.pn.getY() - cy)*(b.pn.getY() - cy)) - Math.sqrt(rsq)); 
        assert Math.abs(Math.sqrt((rp.pn.getX() - cx)*(rp.pn.getX() - cx) + (rp.pn.getY() - cy)*(rp.pn.getY() - cy)) - Math.sqrt(rsq)) < 0.0001; 
        double lvcx = lp.pn.getX() - cx; 
        double lvcy = lp.pn.getY() - cy; 
        double lvcsq = lvcx*lvcx + lvcy*lvcy; 
        return lvcsq >= rsq; 
    }
    
    void Flip()
    {
        DelPoint la = a; 
        DelPoint lb = b; 
        DelEdge ldeleftfore = deleftfore; 
        DelEdge lderightback = derightback; 

        DelPoint rp = (lderightback.b == la ? lderightback.a : lderightback.b); 
        DelPoint lp = (ldeleftfore.b == lb ? ldeleftfore.a : ldeleftfore.b); 

        DelEdge lderightfore = (a == derightback.b ? derightback.derightback : derightback.deleftfore);  
        assert this == (b == lderightfore.b ? lderightfore.deleftfore : lderightfore.derightback); 

        DelEdge ldeleftback = (b == ldeleftfore.b ? ldeleftfore.derightback : ldeleftfore.deleftfore);  
        assert this == (a == ldeleftback.a ? ldeleftback.derightback : ldeleftback.deleftfore); 
        
        assert ldeleftback == (ldeleftfore.a == lp ? ldeleftfore.derightback : ldeleftfore.deleftfore); 
        assert lderightfore == (lderightback.b == rp ? lderightback.deleftfore : lderightback.derightback); 

        // disconnect the edge
        if (lb == lderightfore.b)
            lderightfore.deleftfore = ldeleftfore; 
        else
            lderightfore.derightback = ldeleftfore; 

        if (la == ldeleftback.a)
            ldeleftback.derightback = lderightback;
        else
            ldeleftback.deleftfore = lderightback; 
        a.dpedgeconn = lderightback; 
        b.dpedgeconn = ldeleftfore; 
        
        // reconnect the edge
        a = lp; 
        b = rp; 

        if (ldeleftfore.a == lp)
            ldeleftfore.derightback = this; 
        else
            ldeleftfore.deleftfore = this; 
        derightback = ldeleftback; 
        
        if (lderightback.b == rp)
            lderightback.deleftfore =  this; 
        else
            lderightback.derightback = this;  
        deleftfore = lderightfore; 
        
        DEDPsort(); 
    }
}

/////////////////////////////////////////////
class DelTriangulation
{
    List<DelPoint> dlpointlist; 
    List<DelEdge> dledgelist; 
    
    void MakeInitialBox(Rectangle2D rboundsarea)
    {
        float x0 = (float)(rboundsarea.getX() - 10); 
        float y0 = (float)(rboundsarea.getY() - 10); 
        float x1 = (float)(rboundsarea.getX() + rboundsarea.getWidth() + 10); 
        float y1 = (float)(rboundsarea.getY() + rboundsarea.getHeight() + 10); 

        dlpointlist = new ArrayList<DelPoint>(); 
        dlpointlist.add(new DelPoint(x0, y0)); 
        dlpointlist.add(new DelPoint(x0, y1)); 
        dlpointlist.add(new DelPoint(x1, y0)); 
        dlpointlist.add(new DelPoint(x1, y1)); 
        assert dlpointlist.get(0).compareTo(dlpointlist.get(1)) == -1; 
        assert dlpointlist.get(1).compareTo(dlpointlist.get(2)) == -1; 
        assert dlpointlist.get(2).compareTo(dlpointlist.get(3)) == -1; 
        
        dledgelist = new ArrayList<DelEdge>(); 
        dledgelist.add(new DelEdge(dlpointlist.get(0), dlpointlist.get(1)));
        dledgelist.add(new DelEdge(dlpointlist.get(0), dlpointlist.get(3)));
        dledgelist.add(new DelEdge(dlpointlist.get(0), dlpointlist.get(2)));
        dledgelist.add(new DelEdge(dlpointlist.get(1), dlpointlist.get(3)));
        dledgelist.add(new DelEdge(dlpointlist.get(2), dlpointlist.get(3)));

        dledgelist.get(0).sig = -2; 
        dledgelist.get(2).sig = -2; 
        dledgelist.get(3).sig = -2; 
        dledgelist.get(4).sig = -2; 

        dlpointlist.get(0).dpedgeconn = dledgelist.get(0); 
        dlpointlist.get(1).dpedgeconn = dledgelist.get(0); 
        dlpointlist.get(2).dpedgeconn = dledgelist.get(2); 
        dlpointlist.get(3).dpedgeconn = dledgelist.get(1); 
        
        dledgelist.get(0).deleftfore = dledgelist.get(3); 
        dledgelist.get(1).deleftfore = dledgelist.get(4); 
        dledgelist.get(1).derightback = dledgelist.get(0); 
        dledgelist.get(2).derightback = dledgelist.get(1); 
        dledgelist.get(3).deleftfore = dledgelist.get(1); 
        dledgelist.get(4).derightback = dledgelist.get(2); 
    }
   
    DelEdge FindTriangle(double x, double y)
    {
        for (DelEdge de : dledgelist)
        {
            if (de.InTriangle(x, y))
                return de; 
        }
        return null; 
    }

    DelTriangulation(OneSArea osa)
    {
        MakeInitialBox(osa.rboundsarea); 
        
        // insert all the points from the boundary
        DelPoint ldpprev = null; 
        DelPoint dpfirst = null; 
        List<DelPoint> dlboundlist = new ArrayList<DelPoint>(); 
        for (RefPathO refpath : osa.refpathsub)
        {
            ldpprev = AddDelPoint(new DelPoint(refpath.bFore ? refpath.op.pnstart : refpath.op.pnend)); 
            dlboundlist.add(ldpprev); 
            if (dpfirst == null)
                dpfirst = ldpprev; 
            float[] pco = refpath.op.GetCoords();
            for (int i = 1; i < refpath.op.nlines; i++)
            {
                int ir = (refpath.bFore ? i : refpath.op.nlines - i);
                float x = pco[ir * 2 + 0]; 
                float y = pco[ir * 2 + 1];
                ldpprev = AddDelPoint(new DelPoint(refpath, i, x, y)); 
                dlboundlist.add(ldpprev); 
            }
        }
    
        // run through and make sure the boundary points are joined up (inserting new points if necessary)
        int i = 0; 
        while (i < dlboundlist.size())
        {   
            DelPoint dp = dlboundlist.get(i); 
            DelPoint dpfore = dlboundlist.get(i != dlboundlist.size() - 1 ? i + 1 : 0); 
            DelEdge deconn = dp.FindEdgeConn(dpfore); 
            if (deconn == null)
            {
                double mu = dp.FindEdgeCross(dpfore); 
                if (mu != -1.0)
                {
                    double cx = dp.pn.getX() * (1 - mu) + dpfore.pn.getX() * mu; 
                    double cy = dp.pn.getY() * (1 - mu) + dpfore.pn.getY() * mu; 
                        //DelPoint(RefPathO lrefpath, float li, float u, float v)
                    DelPoint deipoint = new DelPoint((float)cx, (float)cy); 

                    AddDelPoint(deipoint); 
                    dlboundlist.add(i + 1, deipoint); 
                }
                else
                {
                    System.out.println(" mugonewrong i="+i); 
                    i++; 
                }
            }
            else
            {
                deconn.sig = 1; 
                i++; 
            }
        }
            
        // run through and set the boundary signals to show the insides
        for (int j = 0; j < dlboundlist.size(); j++)
        {   
            DelPoint dp = dlboundlist.get(j); 
            DelPoint dpprev = (j != 0 ? dlboundlist.get(j - 1) : dlboundlist.get(dlboundlist.size() - 1)); 
            DelPoint dpnext = (j != dlboundlist.size() - 1 ? dlboundlist.get(j + 1) : dlboundlist.get(0)); 
            
            DelEdge de = dp.dpedgeconn; 
            do 
            {
                de = (de.a == dp ? de.derightback : de.deleftfore); 
                DelPoint defar = (de.a == dp ? de.b : de.a); 
                if ((defar == dpprev) || (defar == dpnext))
                    break; 
            }  while (de != dp.dpedgeconn); 
            
            if (de == dp.dpedgeconn)
                System.out.println(" ffail j="+j); 
            if (de == dp.dpedgeconn)
                continue; 

            DelEdge desigged = de; 
            boolean binside = false; 
            do 
            {
                DelPoint defar = (de.a == dp ? de.b : de.a); 
                if (defar == dpprev) 
                    binside = true; 
                else if (defar == dpnext) 
                    binside = false; 
                else
                    de.sig = (binside ? 2 : -2); 
                de = (de.a == dp ? de.derightback : de.deleftfore); 
            }  while (de != desigged); 
        }
    }
    
    DelPoint AddDelPoint(DelPoint dpmid)
    {
        DelEdge detriang = FindTriangle(dpmid.pn.getX(), dpmid.pn.getY()); 
        if (detriang == null)
            return (TN.emitWarning("point not in triangulation area") ? null : null); 

        assert detriang.a == detriang.derightback.a; 
        DelEdge deright = detriang.derightback; 
        DelEdge detop = detriang.derightback.deleftfore; 
        
        DelEdge de0mid = new DelEdge(detriang.a, dpmid); 
        DelEdge de1mid = new DelEdge(dpmid, detriang.b); 
        DelEdge de2mid = new DelEdge(detriang.derightback.b, dpmid); 
        dpmid.dpedgeconn = de0mid; 
        
        de0mid.derightback = deright; 
        detriang.derightback = de0mid; 
        de2mid.derightback = detop; 
        deright.deleftfore = de2mid; 
        de1mid.deleftfore = detriang; 
        if (detop.a == detriang.b)
            detop.derightback = de1mid; 
        else
            detop.deleftfore = de1mid; 
        de0mid.deleftfore = de1mid; 
        de1mid.derightback = de2mid; 
        de2mid.deleftfore = de0mid; 

        de0mid.DEDPsort(); 
        de1mid.DEDPsort(); 
        de2mid.DEDPsort(); 
        
        dledgelist.add(de0mid); 
        dledgelist.add(de1mid); 
        dledgelist.add(de2mid); 

        if ((detriang.sig == 0) && !detriang.IsDelauney())
            detriang.Flip(); 
        if ((deright.sig == 0) && !deright.IsDelauney())
            deright.Flip(); 
        if ((detop.sig == 0) && !detop.IsDelauney())
            detop.Flip(); 

        return dpmid; 
    }
    
    
// then list all the segments here.  and put them into their own arrays    
/*    for (RefPathO refpath : osa.refpathsub)
		{
			// if going forwards, then everything works
			if (refpath.bFore)
			{
				gparea.append(refpath.op.gp, !bfirst); // the second parameter is continuation, and avoids repeats at the moveto
				bfirst = false;
				continue;
			}

			// specially decode it if reversed
			if ((pco == null) || (pco.length < refpath.op.nlines * 6 + 2));
				pco = new float[refpath.op.nlines * 6 + 2];
			// this gives an array that is interspersed with the control points
			refpath.op.ToCoordsCubic(pco);
        }
    }
*/
    
}
