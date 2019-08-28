////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2010  Julian Todd.
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
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

import java.awt.geom.GeneralPath; 
import java.awt.geom.Point2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.geom.Line2D; 
import java.awt.geom.Ellipse2D; 
import java.awt.geom.AffineTransform;
import java.awt.Shape; 

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.GridLayout;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


/////////////////////////////////////////////
class DoubleArray
{
    double[] arr = null; 
    int sz = 0; 

    void add(double val)
    {
        if (arr == null)
            arr = new double[2]; 
        else if (sz == arr.length)
        {
            double[] larr = arr; 
            arr = new double[larr.length * 2]; 
            for (int i = 0; i < larr.length; i++)
                arr[i] = larr[i]; 
        }
        arr[sz] = val; 
        sz++; 
    }

    double pop()
    {
        sz--; 
        return arr[sz]; 
    }

    int size()
    {
        return sz; 
    }

    double get(int i)
    {
        assert ((i >= 0) && (i < sz)); 
        return arr[i]; 
    }

    void sort()
    {
        if (sz != 0)
            Arrays.sort(arr, 0, sz); 
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(); 
        for (int i = 0; i < sz; i++)
            sb.append((i == 0 ? "" : " ") + String.format("%.3f", get(i))); 
        return sb.toString(); 
    }

    public String toStringRel()
    {
        StringBuffer sb = new StringBuffer(); 
        for (int i = 0; i < sz; i++)
            sb.append((i == 0 ? "" : " ") + String.format("%.3f", get(i) - get(0))); 
        return sb.toString(); 
    }
}


/////////////////////////////////////////////
/////////////////////////////////////////////
class IntensityWedge
{
    double t0; 
    double e0; 
    double t1; 
    double e1; 
    double slope; 

    IntensityWedge(double lt0, double le0, double lt1, double le1)
    {
        t0 = lt0; 
        e0 = le0; 
        t1 = lt1; 
        e1 = le1; 
        slope = (e1 - e0) / (t1 - t0); 
    }

    /////////////////////////////////////////////
    double GetIntensityW(double eT)
    {
        assert (t0 <= eT) && (eT <= t1); 
        double lam = (eT - t0) / (t1 - t0); 
        return e0 * (1.0 - lam) + e1 * lam; 
    }

    /////////////////////////////////////////////
    IntensityWedge(IntensityWedge rightwedge, double tsplit)
    {
        assert ((tsplit > rightwedge.t0) && (tsplit < rightwedge.t1)); 
        t0 = rightwedge.t0; 
        e0 = rightwedge.e0; 
        t1 = tsplit; 
        e1 = rightwedge.GetIntensityW(tsplit); 
        slope = rightwedge.slope; 
        assert VerifySlope(); 

        rightwedge.t0 = tsplit; 
        rightwedge.e0 = e1; 
        assert rightwedge.VerifySlope(); 
    }

    /////////////////////////////////////////////
    boolean VerifySlope()
    {
        double slediff = Math.abs((t1 - t0) * slope - (e1 - e0)); 
        if (slediff < 0.001) // for very small changes in e value
            return true; 
        double lslope = (e1 - e0) / (t1 - t0); 
        if (Math.abs(lslope - slope) <= 0.01)
            return true; 
        System.out.println(this + "  " + (t0 - t1) + "  " + (e0 - e1) + "  " + lslope + " " + slope); 
        return false; 
    }

    /////////////////////////////////////////////
    double GetExponentialAvgW(double eT, double efac, double lt1)
    {
        assert ((lt1 > t0) && (lt1 <= t1)); 

        // integral from t0 to lt1 of [(t - t0) + e0] exp((t - eT) efac)
        double b = Math.exp((lt1 - eT) * efac) * (lt1 * slope - slope / efac + (e0 - t0 * slope)) / efac; 
        double a = Math.exp((t0 - eT) * efac) * (t0 * slope - slope / efac + (e0 - t0 * slope)) / efac; 
        return b - a; 
    }

    /////////////////////////////////////////////
    public String toString()
    {
        return String.format("(%.3f,%.2f %.3f,%.2f)", t0, e0, t1, e1); 
    }
}

/////////////////////////////////////////////
/////////////////////////////////////////////
class IntensityEnvelope
{
    List<IntensityWedge> wedges = new ArrayList<IntensityWedge>(); 
    double emin; 
    double emax; 
    double epeak = 0.0; 
    double tpeak = 0.0; 

    /////////////////////////////////////////////
    double GetIntensity(double eT)
    {
        for (int i = 0; i < wedges.size(); i++)
        {
            if ((wedges.get(i).t0 <= eT) && (eT < wedges.get(i).t1)) 
                return wedges.get(i).GetIntensityW(eT); 
        }
        return 0.0; 
    }

    /////////////////////////////////////////////
    // not used
    double GetExponentialAverage(double eT, double efac)
    {
        double result = 0.0; 
        for (int i = 0; i < wedges.size(); i++)
        {
            IntensityWedge wedge = wedges.get(i); 
            if (wedge.t0 >= eT)
                break; 
            result += wedge.GetExponentialAvgW(eT, efac, Math.min(eT, wedge.t1)); 
        }
        return result; 
    }

    /////////////////////////////////////////////
    double GetLastPeak(double eT, double peakfac)
    {
        double peakexp = epeak * Math.exp((tpeak - eT) * peakfac); 
        for (int i = 0; i < wedges.size(); i++)
        {
            IntensityWedge wedge = wedges.get(i); 
            if (wedge.t0 >= eT)
                break; 
            if (wedge.e0 > 0.0)
            {
                double e0exp = wedge.e0 * Math.exp((wedge.t0 - eT) * peakfac); 
                if (e0exp > peakexp)
                {
                    epeak = wedge.e0; 
                    tpeak = wedge.t0; 
                    peakexp = e0exp; 
                }
            }

            if (wedge.t1 <= eT)
            {
                double e1exp = wedge.e1 * Math.exp((wedge.t1 - eT) * peakfac); 
                if (e1exp > peakexp)
                {
                    epeak = wedge.e1; 
                    tpeak = wedge.t1; 
                    peakexp = e1exp; 
                }
            }
            else
            {
                double le1 = wedge.GetIntensityW(eT); 
                double le1exp = le1; 
                if (le1exp > peakexp)
                {
                    epeak = le1; 
                    tpeak = eT; 
                    peakexp = le1exp; 
                }
            }
        }
        return epeak; 
    }

    /////////////////////////////////////////////
    void SeteextremesL(double t, double e, boolean bFirst)
    {
        if (bFirst)
        {
            emin = e; 
            emax = e; 
            tpeak = t; 
        }
        else if (e < emin)
        {
            emin = e; 
            if (-emin > emax)
                tpeak = t; 
        }
        else if (e > emax)
        {
            emax = e; 
            if (emax > -emin)
                tpeak = t; 
        }
    }
    void Seteextremes()
    {
        for (int i = 0; i < wedges.size(); i++)
        {
            IntensityWedge wedge = wedges.get(i); 
            SeteextremesL(wedge.t0, wedge.e0, (i == 0)); 
            SeteextremesL(wedge.t1, wedge.e1, false); 
        }
    }


    /////////////////////////////////////////////
    boolean VerifyWedges()
    {
        for (int i = 0; i < wedges.size(); i++)
        {
            assert wedges.get(i).VerifySlope(); 
            assert wedges.get(i).t0 < wedges.get(i).t1; 
            if (i != 0)
                assert wedges.get(i - 1).t1 <= wedges.get(i).t0; 
        }
        return true; 
    }

    /////////////////////////////////////////////
    void AddWedge(IntensityWedge lnewwedge, double toffset)
    {
        // should build offset into the algorithm
        IntensityWedge newwedge = new IntensityWedge(lnewwedge.t0 + toffset, lnewwedge.e0, lnewwedge.t1 + toffset, lnewwedge.e1); 
        newwedge.slope = newwedge.slope; 
        newwedge.VerifySlope(); 

        // verification measures
        double te0m = newwedge.t0 - 1.0; 
        double ee0m = GetIntensity(te0m); 
        double ee0 = GetIntensity(newwedge.t0) + newwedge.e0; 
        double tehalf = newwedge.t0 * 0.6 + newwedge.t1 * 0.4; 
        double eehalf = GetIntensity(tehalf) + newwedge.GetIntensityW(tehalf); 
        double ten1 = newwedge.t0 * 0.001 + newwedge.t1 * 0.999; 
        double een1 = GetIntensity(ten1) + newwedge.GetIntensityW(ten1); 
        double ee1 = GetIntensity(newwedge.t1); 
        double te1p = newwedge.t1 + 1.0; 
        double ee1p = GetIntensity(te1p); 

        AddWedgeV(newwedge); 
        //System.out.println("Env: " + this); 
        assert VerifyWedges(); 

        // verification values
        assert Math.abs(ee0m - GetIntensity(te0m)) < 0.001; 
        assert Math.abs(ee0 - GetIntensity(newwedge.t0)) < 0.001; 
        assert Math.abs(eehalf - GetIntensity(tehalf)) < 0.001; 
        assert Math.abs(een1 - GetIntensity(ten1)) < 0.001; 
        assert Math.abs(ee1 - GetIntensity(newwedge.t1)) < 0.001; 
        assert Math.abs(ee1p - GetIntensity(te1p)) < 0.001; 
    }

    /////////////////////////////////////////////
    void AddWedgeV(IntensityWedge newwedge)
    {
        // full outer wedge cases
        if ((wedges.size() == 0) || (wedges.get(wedges.size() - 1).t1 <= newwedge.t0))
        {
            wedges.add(newwedge); 
            return; 
        }
        if (newwedge.t1 <= wedges.get(0).t0)
        {
            wedges.add(0, newwedge); 
            return; 
        }

        // find wedges
        int i0; 
        for (i0 = 0; i0 < wedges.size(); i0++)
        {
            if (newwedge.t0 < wedges.get(i0).t1) 
                break; 
        }
        assert (i0 < wedges.size()); 

        int i1; 
        for (i1 = wedges.size() - 1; i1 >= 0; i1--)
        {
            if (wedges.get(i1).t0 < newwedge.t1) 
                break; 
        }
        // wedge in gap
        if (i1 == i0 - 1)
        {
            wedges.add(i0, newwedge); 
            return; 
        }
        assert (i0 <= i1); 


        // create partial outer wedge left
        if (newwedge.t0 < wedges.get(i0).t0)
        {
            assert ((i0 == 0) || (wedges.get(i0 - 1).t1 <= newwedge.t0)); 
            IntensityWedge outerwedge = new IntensityWedge(newwedge.t0, newwedge.e0, wedges.get(i0).t0, newwedge.GetIntensityW(wedges.get(i0).t0)); 
            wedges.add(i0, outerwedge); 
            i0++; 
            i1++; 
        }
        // split wedge left
        else if (newwedge.t0 > wedges.get(i0).t0)
        {
            IntensityWedge leftwedge = new IntensityWedge(wedges.get(i0), newwedge.t0); 
            wedges.add(i0, leftwedge); 
            i1++; 
            i0++; 
            assert VerifyWedges(); 
        }

        // create partial outer wedge right
        if (wedges.get(i1).t1 < newwedge.t1)
        {
            assert ((i1 == wedges.size() - 1) || (newwedge.t1 < wedges.get(i1 + 1).t0)); 
            IntensityWedge outerwedge = new IntensityWedge(wedges.get(i1).t1, newwedge.GetIntensityW(wedges.get(i1).t1), newwedge.t1, newwedge.e1); 
            wedges.add(i1 + 1, outerwedge); 
        }
        // split wedge right
        if (wedges.get(i1).t1 > newwedge.t1) 
        {
            IntensityWedge leftwedge = new IntensityWedge(wedges.get(i1), newwedge.t1); 
            wedges.add(i1, leftwedge); 
            assert VerifyWedges(); 
        }

        // displace intermediate wedges
        for (int i = i0; i <= i1; i++)
        {
            IntensityWedge wedge = wedges.get(i); 
            assert ((newwedge.t0 <= wedge.t0) && (wedge.t1 <= newwedge.t1)); 
            wedge.e0 += newwedge.GetIntensityW(wedge.t0); 
            wedge.e1 += newwedge.GetIntensityW(wedge.t1); 
            wedge.slope += newwedge.slope; 
            assert wedge.VerifySlope(); 

            // gap filling wedge
            if ((i != i0) && (wedges.get(i - 1).t1 < wedges.get(i).t0))
            {
                IntensityWedge gapwedge = new IntensityWedge(wedges.get(i - 1).t1, newwedge.GetIntensityW(wedges.get(i - 1).t1), wedge.t0, newwedge.GetIntensityW(wedge.t0)); 
                gapwedge.slope = newwedge.slope; 
                assert gapwedge.VerifySlope(); 
                wedges.add(i, gapwedge); 
                i++; 
                i1++; 
            }
        }
    }

    /////////////////////////////////////////////
    static void Test()
    {
        Random ran = new Random(); 
        ran.setSeed(854345); 
        for (int j = 0; j < 20; j++)
        {
            IntensityEnvelope ie = new IntensityEnvelope(); 
            for (int i = 0; i < 20; i++)
            {
                double t0 = ran.nextDouble(); 
                IntensityWedge wedge = new IntensityWedge(t0, ran.nextDouble() * 2 - 1, t0 + ran.nextDouble(), ran.nextDouble() * 2 - 1); 
                //System.out.println("Wedge: " + wedge); 
                ie.AddWedge(wedge, ran.nextDouble()); 
            }
        }
    }

    /////////////////////////////////////////////
    public String toString()
    {
        StringBuffer sb = new StringBuffer(); 
        for (int i = 0; i < wedges.size(); i++)
            sb.append((i == 0 ? "" : " ") + wedges.get(i)); 
        return sb.toString(); 
    }
}; 


/////////////////////////////////////////////
/////////////////////////////////////////////
class TodeNode
{
    OnePathNode opn; 
    DoubleArray spiketimes = new DoubleArray(); 
    List<TodeFibre> outgoingfibres = new ArrayList<TodeFibre>(); 
    List<TodeFibre> incomingfibres = new ArrayList<TodeFibre>(); 
    boolean bprecodedspikes = false; // allowed to have spikes in the future
    IntensityEnvelope refactoryenvelope = new IntensityEnvelope(); 
    double nodethreshold = 1.0; 
    double adaptivequotient = 0.0; 

    List<OnePath> incomingsettings = new ArrayList<OnePath>(); 

    double nextspike = -1.0; 

    double externalnodeintensity = 0.0; 
    double refactorynodeintensity = 0.0; 
    double nodeintensity = 0.0; // sum of the above two

    IntensityEnvelope currentenvelope = new IntensityEnvelope(); 

    /////////////////////////////////////////////
    TodeNode(OnePathNode lopn)
    {
        opn = lopn; 
    }

    /////////////////////////////////////////////
    void RecalcEnvelope()
    {
        currentenvelope.wedges.clear(); 
        for (TodeFibre todefibre : incomingfibres)
        {
            for (int i = 0; i < todefibre.fromnode.spiketimes.size(); i++)
            {
                for (IntensityWedge wedge : todefibre.intensityenvelope.wedges)
                    currentenvelope.AddWedge(wedge, todefibre.fromnode.spiketimes.get(i) + todefibre.timelength); 
            }
        }

        for (int i = 0; i < spiketimes.size(); i++)
        {
            for (IntensityWedge wedge : refactoryenvelope.wedges)
                currentenvelope.AddWedge(wedge, spiketimes.get(i)); 
        }
    }

    /////////////////////////////////////////////
    void NextSpike(double T)
    {
        nextspike = -1.0; 
        for (IntensityWedge wedge : currentenvelope.wedges)
        {
            if (wedge.e0 >= nodethreshold)
            {
                nextspike = wedge.t0; 
                break; 
            }
            if (wedge.e1 >= nodethreshold)
            {
                double lam = (nodethreshold - wedge.e0) / (wedge.e1 - wedge.e0); 
                nextspike = wedge.t0 * (1.0 - lam) + wedge.t1 * lam; 
                break; 
            }
        }
        assert ((nextspike == -1.0) || (nextspike > T)); 
    }
}


/////////////////////////////////////////////
/////////////////////////////////////////////
class TodeFibre
{
    TodeNode fromnode; 
    TodeNode tonode; 
    double timelength; 
    IntensityEnvelope intensityenvelope = new IntensityEnvelope(); 

    double timelinelengthfac = 1.0; 
    OnePath op; 
    Double[] opseglengths; // for drawing the spike on the path

    double closestspiketimelength = 0.0; // 

    /////////////////////////////////////////////
    TodeFibre(OnePath lop, TodeNode lfromnode, TodeNode ltonode)
    {
        op = lop; 
        fromnode = lfromnode; 
        tonode = ltonode; 

        fromnode.outgoingfibres.add(this); 
        tonode.incomingfibres.add(this); 

        // visualization stuff
        opseglengths = new Double[op.nlines]; 
        for (int i = 0; i < op.nlines; i++)
            opseglengths[i] = (i == 0 ? 0.0 : opseglengths[i - 1]) + op.MeasureSegmentLength(i);  
    }


    /////////////////////////////////////////////
    void SetPosD(Point2D spos, Point2D stan, double dst)
    {
        int j = 0; 
        for ( ; j < opseglengths.length - 1; j++)
            if (opseglengths[j] >= dst)
                break; 
        double ljm1 = (j == 0 ? 0.0 : opseglengths[j-1]); 
        double tr = (dst - ljm1) / (opseglengths[j] - ljm1); 
        op.EvalSeg(spos, stan, j, tr); 
    }
}

/////////////////////////////////////////////
class PosSpikeViz
{
    Point2D spos = new Point2D.Double(); 
    Point2D stan = new Point2D.Double(); 
}

/////////////////////////////////////////////
/////////////////////////////////////////////
class TodeNodeCalc
{
    List<TodeNode> todenodes = new ArrayList<TodeNode>(); 
    List<TodeFibre> todefibres = new ArrayList<TodeFibre>(); 
    double T = 0.0; 

    List<TodeNode> todenodesnextspikes = new ArrayList<TodeNode>(); 

    IntensityEnvelope iefastattack; 
    IntensityEnvelope iefastsuppress; 
    IntensityEnvelope ieslowsuppress; 
    IntensityEnvelope ieslowattack; 
    IntensityEnvelope iestandardrefactory; 

    double chardisprand[] = null; // some variance in the wordmode distances

    /////////////////////////////////////////////
    void MakeDefaultIntensityEnvelopes()
    {
        iefastattack = new IntensityEnvelope(); 
        iefastattack.wedges.add(new IntensityWedge(0.0, 0.0, 0.1, 1.1)); 
        iefastattack.wedges.add(new IntensityWedge(0.1, 1.1, 0.8, 0.0)); 
        iefastattack.Seteextremes(); 

        iefastsuppress = new IntensityEnvelope(); 
        iefastsuppress.wedges.add(new IntensityWedge(0.0, 0.0, 0.1, -1.5)); 
        iefastsuppress.wedges.add(new IntensityWedge(0.1, -1.5, 1.5, -1.4)); 
        iefastsuppress.wedges.add(new IntensityWedge(1.5, -1.4, 2.1, 0.0)); 
        iefastsuppress.Seteextremes(); 

        ieslowsuppress = new IntensityEnvelope(); 
        ieslowsuppress.wedges.add(new IntensityWedge(0.0, 0.0, 0.1, -1.5)); 
        ieslowsuppress.wedges.add(new IntensityWedge(0.1, -1.5, 5.5, -1.4)); 
        ieslowsuppress.wedges.add(new IntensityWedge(5.5, -1.4, 7.1, 0.0)); 
        ieslowsuppress.Seteextremes(); 

        ieslowattack = new IntensityEnvelope(); 
        ieslowattack.wedges.add(new IntensityWedge(0.0, 0.0, 0.5, 1.1)); 
        ieslowattack.wedges.add(new IntensityWedge(0.5, 1.1, 0.8, 0.0)); 
        ieslowattack.Seteextremes(); 

        // back in time slightly to suppress the spike that was there
        iestandardrefactory = new IntensityEnvelope(); 
        iestandardrefactory.wedges.add(new IntensityWedge(-0.0001, -10.0, 0.9, -5.0)); 
        iestandardrefactory.wedges.add(new IntensityWedge(0.9, -5.0, 1.1, 0.0)); 
        iestandardrefactory.Seteextremes(); 

        chardisprand = new double[26]; 
        Random ran = new Random(); 
        ran.setSeed(1854345); 
        for (int i = 0; i < chardisprand.length; i++)
            chardisprand[i] = ran.nextDouble(); 
    }

    /////////////////////////////////////////////
    void RecalculateAll()
    {
        for (TodeNode todenode : todenodes)
        {
            if (!todenode.bprecodedspikes)
            {
                while ((todenode.spiketimes.size() != 0) && (todenode.spiketimes.get(todenode.spiketimes.size() - 1) > T))
                    todenode.spiketimes.pop(); 
            }
        }
        for (TodeNode todenode : todenodes)
            todenode.RecalcEnvelope(); 
        NextSpikeAll(); 
    }

    /////////////////////////////////////////////
    void NextSpikeAll()
    {
        todenodesnextspikes.clear(); 
        for (TodeNode todenode : todenodes)
        {
            todenode.NextSpike(T); 
            if (todenode.nextspike != -1.0)
            {
                if (!todenodesnextspikes.isEmpty() && (todenodesnextspikes.get(0).nextspike > todenode.nextspike)) 
                    todenodesnextspikes.clear(); 
                if (todenodesnextspikes.isEmpty() || (todenodesnextspikes.get(0).nextspike == todenode.nextspike)) 
                    todenodesnextspikes.add(todenode); 
            }
        }
    }

    /////////////////////////////////////////////
    void AdvanceTime(double advance)
    {
        double Tnext = T + advance; 
        if (todenodesnextspikes.isEmpty() || (todenodesnextspikes.get(0).nextspike > Tnext))
        {
            T = Tnext; 
            return; 
        }

        T = todenodesnextspikes.get(0).nextspike; 
        for (TodeNode todenode : todenodesnextspikes)
        {
            // set the spike
            todenode.spiketimes.add(T); 

            // add the intensities which result
            for (TodeFibre todefibre : todenode.outgoingfibres)
            {
                for (IntensityWedge wedge : todefibre.intensityenvelope.wedges)
                    todefibre.tonode.currentenvelope.AddWedge(wedge, T + todefibre.timelength); 
            }

            for (IntensityWedge wedge : todenode.refactoryenvelope.wedges)
                todenode.currentenvelope.AddWedge(wedge, T); 

            // how close did any incoming spikes miss this target
            for (TodeFibre todefibre : todenode.incomingfibres)
            {
                todefibre.closestspiketimelength = 0.0; 
                for (int i = 0; i < todefibre.fromnode.spiketimes.size(); i++)
                {
                    double lclosestspiketimelength = T - (todefibre.fromnode.spiketimes.get(i) + todefibre.timelength + todefibre.intensityenvelope.tpeak); 
                    if ((i == 0) || (Math.abs(lclosestspiketimelength) < Math.abs(todefibre.closestspiketimelength)))  
                        todefibre.closestspiketimelength = lclosestspiketimelength; 
                }
            }
        }

        //RecalculateAll(); // would redo the partial calculation above
        NextSpikeAll(); 
    }

    /////////////////////////////////////////////
    TodeNode FindTodeNode(OnePathNode opn)
    {
        for (TodeNode tn : todenodes)
            if (tn.opn == opn)
                return tn; 
        TodeNode ntn = new TodeNode(opn); 
        todenodes.add(ntn); 
        return ntn; 
    }


    /////////////////////////////////////////////
    void ApplySettingsToNodeFibes(TodeNode todenode, String drawlab)
    {
        String[] params = drawlab.split("[\\s,]+"); 

        boolean bwordmode = false; 
        boolean brelative = true; 
        double rwordmodemintime = 0; 
        double rwordmodemaxtime = 0; 
        double rwordmodegap = 0; 
        double llastspiketime = 0.0; 
        try
        {
            for (int i = 0; i < params.length; i++)
            {
                //System.out.println(i + "::" + params[i]); 
                String param = params[i]; 
                if (bwordmode)
                {
                    llastspiketime += rwordmodegap;   // may need to randomize this
                    todenode.spiketimes.add(llastspiketime); 
                    for (int j = 0; j < param.length(); j++)
                    {
                        int iletter = Character.getNumericValue(param.charAt(j)) - Character.getNumericValue('a'); 
                        double rletter = (iletter + chardisprand[iletter] * 0.3) / 26.0; 
                        double rgap = rwordmodemintime * (1.0 - rletter) + rwordmodemaxtime * rletter;

                        llastspiketime += rgap;   
                        todenode.spiketimes.add(llastspiketime); 
                    }
                    todenode.bprecodedspikes = true; 
                }
    
                else if (param.equals("wordmode"))
                {
                    i++; 
                    rwordmodemintime = Float.parseFloat(params[i]); 
                    i++; 
                    rwordmodemaxtime = Float.parseFloat(params[i]); 
                    i++; 
                    rwordmodegap = Float.parseFloat(params[i]); 
                    bwordmode = true; 
                }
    
                else if (param.equals("threshold"))
                {
                    i++; 
                    todenode.nodethreshold = Float.parseFloat(params[i]); 
                }
                else if (param.equals("adaptive"))
                {
                    i++; 
                    todenode.adaptivequotient = Float.parseFloat(params[i]); 
                }
                else if (param.equals("absolute"))
                {
                    brelative = false; 
                }

                else if (param.equals("slowattack"))
                {
                    for (TodeFibre todefibre : todenode.incomingfibres)
                    {
                        if (todefibre.intensityenvelope == iefastattack)
                            todefibre.intensityenvelope = ieslowattack; 
                    }
                }

                else
                {
                    double rparam = Float.parseFloat(param); 
    
                    if (brelative)
                        llastspiketime += rparam; 
                    else
                        llastspiketime = rparam; 
    
                    todenode.spiketimes.add(llastspiketime); 
                    todenode.bprecodedspikes = true; 
                }
            }
        }
        catch (NumberFormatException e)
        { 
            TN.emitWarning("Error parsing: " + drawlab);
        }
        todenode.spiketimes.sort(); 
    }


    /////////////////////////////////////////////
    TodeNodeCalc(List<OnePath> vpaths)
    {
        MakeDefaultIntensityEnvelopes(); 

        for (OnePath op : vpaths)
        {
            TodeNode tonode = FindTodeNode(op.pnend); 
            if (op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
                todefibres.add(new TodeFibre(op, FindTodeNode(op.pnstart), tonode)); 
            else if (op.plabedl != null) 
                tonode.incomingsettings.add(op); 
        }

        // Set default intensity envelopes of incoming fibres by fibre type
        for (TodeNode todenode : todenodes)
        {
            int nposfibres = 0; 
            for (TodeFibre todefibre : todenode.incomingfibres)
            {
                if (todefibre.op.linestyle == SketchLineStyle.SLS_WALL)
                {
                    todefibre.intensityenvelope = ieslowsuppress; 
                    todefibre.timelinelengthfac = 0.2; 
                }
                else
                {
                    todefibre.intensityenvelope = iefastattack; 
                    nposfibres++; 
                }
                todefibre.timelength = todefibre.op.linelength * todefibre.timelinelengthfac / TN.CENTRELINE_MAGNIFICATION; // linear distance; opseglengths[-1] would be spline length
            }
            todenode.nodethreshold = nposfibres + 0.01; 
            todenode.refactoryenvelope = iestandardrefactory; 

            // this could also make use of the order of the incoming connective line with label
            for (OnePath op : todenode.incomingsettings)
                ApplySettingsToNodeFibes(todenode, op.plabedl.drawlab); 
        }

        RecalculateAll(); 
        NextSpikeAll(); 
    }


    /////////////////////////////////////////////
    // this function calculates spikes and intensity independently of the nextspike calculation, and should agree with it
    List<PosSpikeViz> spikelist = new ArrayList<PosSpikeViz>(); 
    int nspikelist = 0; 
    int nodeswithintensity = 0; 
    int spikesinfuture = 0; 
    boolean PosSpikes() 
    {
        nspikelist = 0; 
        nodeswithintensity = 0; 
        spikesinfuture = 0; 

        for (TodeNode todenode : todenodes)
        {
            todenode.externalnodeintensity = 0.0; 
            for (TodeFibre todefibre : todenode.incomingfibres)
            {
                for (int i = 0; i < todefibre.fromnode.spiketimes.size(); i++)
                {
                    double st = T - todefibre.fromnode.spiketimes.get(i); 
                    if (st < 0.0)
                    {
                        assert todefibre.fromnode.bprecodedspikes; 
                        spikesinfuture++; 
                        continue; // only can happen for precoded trains
                    }

                    // spike on the fibre
                    if (st < todefibre.timelength)
                    {
                        if (nspikelist == spikelist.size())
                            spikelist.add(new PosSpikeViz()); 
                        PosSpikeViz psv = spikelist.get(nspikelist); 
                        double dst = st / todefibre.timelength * todefibre.opseglengths[todefibre.opseglengths.length - 1]; 
                        todefibre.SetPosD(psv.spos, psv.stan, dst); 
                        nspikelist++; 
                        continue; 
                    }

                    double est = st - todefibre.timelength; 
                    todenode.externalnodeintensity += todefibre.intensityenvelope.GetIntensity(est); 
                }
            }


            // add in the refactory intensities from the spikes here
            todenode.refactorynodeintensity = 0.0; 
            for (int i = 0; i < todenode.spiketimes.size(); i++)
            {
                double st = T - todenode.spiketimes.get(i); 
                if (st < 0.0)
                    continue; // only can happen for precoded trains
                todenode.refactorynodeintensity += todenode.refactoryenvelope.GetIntensity(st); 
            }

            if ((todenode.externalnodeintensity != 0.0) || (todenode.refactorynodeintensity != 0.0))
                nodeswithintensity++; 

            todenode.nodeintensity = todenode.externalnodeintensity + todenode.refactorynodeintensity; 
        }
        return ((nspikelist != 0) || (nodeswithintensity != 0) || (spikesinfuture != 0)); 
    }
}


/////////////////////////////////////////////
class TodeNodePanel extends JPanel
{
	SketchDisplay sketchdisplay;
    JButton buttgeneratetodes = new JButton("Gen Todes"); 
    JTextField tfpathlength = new JTextField("", 5); 
    JButton buttoutputenvelope = new JButton("OutEnv"); 
    JButton buttoutputtrain = new JButton("OutTrain"); 
    JButton buttadvance = new JButton("Advance"); 
    JButton buttadapt = new JButton("Adapt"); 
    JTextField tfadvancetime = new JTextField("5.0", 5); 
    JToggleButton buttanimate = new JToggleButton("Anim"); 
    JTextField tfanimtime = new JTextField("0.02", 5); 
    JLabel labtime = new JLabel("T:"); 
    JTextField tftime = new JTextField(5); 

    Thread animthread = null; 
    boolean bnextfaster = false; 

    int spiralsegspercircuit = 20; 
    int nspirals = 4; 
    GeneralPath[] gpspirals; 
    int nsplayvals = 30; 
    GeneralPath[] gpsplaysnegative; 

    Shape acspike = null; 

    LineStyleAttr lsaspikeout = null; 
    LineStyleAttr lsaspike = null; 
    LineStyleAttr lsanegsplay = null; 

    double prevT = 0.0; 
    TodeNodeCalc tnc; 

    Color lightgreen = new Color(190, 255, 190); 


	/////////////////////////////////////////////
    double GetNewLength(OnePath op, double timelinelengthfac, double mu, OnePath nop)
    {
		float[] pco = op.GetCoords();
        double x0 = pco[0]; 
        double y0 = pco[1]; 
        double x1 = pco[op.nlines * 2]; 
        double y1 = pco[op.nlines * 2 + 1]; 
        double vx = x1 - x0; 
        double vy = y1 - y0; 
        double vlen = Math.sqrt(vx*vx + vy*vy); 

        double xp = x0; 
        double yp = y0; 
        double newleng = 0.0; 
        for (int i = 1; i <= op.nlines; i++)
        {
            double xn = pco[i * 2]; 
            double yn = pco[i * 2 + 1]; 
            double xfdot = (vy * (xn - x1) - vx * (yn - y1)) / vlen; 
            xn += vy * xfdot * mu / vlen; 
            yn += -vx * xfdot * mu / vlen; 
            
            double xpn = xn - xp; 
            double ypn = yn - yp; 
            newleng += Math.sqrt(xpn*xpn + ypn*ypn); 

            if ((nop != null) && (i != op.nlines))
                nop.LineTo((float)xn, (float)yn); 
            
            xp = xn; 
            yp = yn; 
        }
        return newleng * timelinelengthfac / TN.CENTRELINE_MAGNIFICATION; 
    }

	/////////////////////////////////////////////
    void SetNewLength(TodeFibre todefibre, double targetlength) 
    {
        double faclo = -0.9; 
        double fachi = 3.0; 

        double newlenglo = GetNewLength(todefibre.op, todefibre.timelinelengthfac, faclo, null); 
        double newlenghi = GetNewLength(todefibre.op, todefibre.timelinelengthfac, fachi, null); 
        if ((targetlength < newlenglo) || (targetlength > newlenghi))
        {
            System.out.println("Target length outside range: " + newlenglo + " " + newlenghi); 
            return; 
        }

        // the maths are too complex to solve faster than this binary search
        while (newlenghi - newlenglo > 0.001)
        {
            double facmid = (faclo + fachi) / 2; 
            double newlengmid = GetNewLength(todefibre.op, todefibre.timelinelengthfac, facmid, null); 
            if (newlengmid < targetlength)
            {
                faclo = facmid; 
                newlenglo = newlengmid; 
            }
            else
            {
                fachi = facmid; 
                newlenghi = newlengmid; 
            }
        }

        // now make the path
        OnePath nop = new OnePath(todefibre.op.pnstart); 
        double newleng = GetNewLength(todefibre.op, todefibre.timelinelengthfac, (faclo + fachi) / 2, nop); 
        nop.EndPath(todefibre.op.pnend); 
        nop.CopyPathAttributes(todefibre.op); 

        List<OnePath> pthstoremove = new ArrayList<OnePath>(); 
        List<OnePath> pthstoadd = new ArrayList<OnePath>(); 
        pthstoremove.add(todefibre.op); 
        pthstoadd.add(nop); 
        sketchdisplay.sketchgraphicspanel.CommitPathChanges(pthstoremove, pthstoadd); 
		sketchdisplay.sketchgraphicspanel.RedrawBackgroundView(); 
    }



	/////////////////////////////////////////////
    void BuildSpirals()
    {
        if (SketchLineStyle.strokew == -1.0F)
            return; 
        double spiralw = SketchLineStyle.strokew * 2.2; 

        gpspirals = new GeneralPath[spiralsegspercircuit * nspirals + 1]; 
        for (int j = 0; j < gpspirals.length; j++)
        {
            gpspirals[j] = new GeneralPath(); 
            gpspirals[j].moveTo(0.0F, 0.0F); 
        }
        for (int i = 1; i < gpspirals.length; i++)
        {
            double lam = i * 1.0 / spiralsegspercircuit;  // exagerate the spirals on the positive side
            double x = TN.degsin(lam * 360) * lam * spiralw; 
            double y = -TN.degcos(lam * 360) * lam * spiralw; 
            for (int j = i; j < gpspirals.length; j++) 
                gpspirals[j].lineTo((float)x, (float)y); 
        }

        acspike = new Ellipse2D.Double(-spiralw * 2.2, -spiralw * 2.2, spiralw * 4.4, spiralw * 4.4); 

        lsaspikeout = new LineStyleAttr(SketchLineStyle.SLS_DETAIL, 2.5F*SketchLineStyle.strokew, 0, 0, 0, Color.white); 
        lsaspike = new LineStyleAttr(SketchLineStyle.SLS_DETAIL, 2.0F*SketchLineStyle.strokew, 0, 0, 0, Color.red); 
        lsanegsplay = new LineStyleAttr(SketchLineStyle.SLS_DETAIL, 0.5F*SketchLineStyle.strokew, 0, 0, 0, Color.yellow); 

        gpsplaysnegative = new GeneralPath[nsplayvals + 1]; 
        double radfull = spiralw * 3.0; 
        double radhalf = spiralw * 1.5; 
        for (int i = 0; i < gpsplaysnegative.length; i++)
        {
            double lamrad = (i + 1) * 1.0 / gpsplaysnegative.length; 
            double rad = lamrad * radfull; 
            gpsplaysnegative[i] = new GeneralPath(); 
            for (int j = 0; j < 18; j++)
            {
                double vx = TN.degcos(j * 20); 
                double vy = TN.degsin(j * 20); 
                if ((j % 2) == 0)
                {
                    gpsplaysnegative[i].moveTo(0.0F, 0.0F); 
                    gpsplaysnegative[i].lineTo((float)(vx * rad), (float)(vy * rad)); 
                }
                else if (rad > radhalf)
                {
                    gpsplaysnegative[i].moveTo((float)(vx * radhalf), (float)(vy * radhalf)); 
                    gpsplaysnegative[i].lineTo((float)(vx * rad), (float)(vy * rad)); 
                }
            }
        }
    }

    /////////////////////////////////////////////
    TodeNodePanel(SketchDisplay lsketchdisplay)
    {
    	sketchdisplay = lsketchdisplay;
        BuildSpirals(); 

        tftime.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) 
        {
            try
            {
                UpdateT(Float.parseFloat(tftime.getText())); 
            }
            catch (NumberFormatException ne)
            {;}
            sketchdisplay.sketchgraphicspanel.repaint(); 
        }});

		buttgeneratetodes.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { GenerateTodes(); } } ); 	
		buttoutputenvelope.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { OutputEnvelope(sketchdisplay.sketchgraphicspanel.currgenpath, false); } } ); 	
		buttoutputtrain.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { OutputEnvelope(sketchdisplay.sketchgraphicspanel.currgenpath, true); } } ); 	
        buttadvance.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdvanceEventB(); } } ); 
        buttadapt.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdaptPhase(); } } ); 
        buttanimate.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent e) 
        { 
            if (buttanimate.isSelected() && (animthread == null))
            {
                animthread = new Thread(new AdvanceNodeThread()); 
                animthread.start(); 
            }
            if (!buttanimate.isSelected() && (animthread != null))
                animthread.interrupt(); 
        }});
 
        tfadvancetime.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdvanceEventB(); } } ); 
        tfpathlength.addActionListener(new ActionListener()  { public void actionPerformed(ActionEvent event)  
        { 
            try
            {
                double targetlength= Float.parseFloat(tfpathlength.getText()); 
                TodeFibre todefibre = FindTodeFibre(sketchdisplay.sketchgraphicspanel.currgenpath); 
                if (todefibre != null)
                    SetNewLength(todefibre, targetlength); 
            }
            catch (NumberFormatException e)
            {;}
        }}); 

        setLayout(new GridLayout(0, 2));

        add(buttgeneratetodes); 
        add(tfpathlength); 
        add(buttadapt);
        add(new JLabel()); 
        add(buttoutputenvelope); 
        add(buttoutputtrain); 
        add(buttadvance); 
        add(tfadvancetime); 
        add(buttanimate); 
        add(tfanimtime); 
        add(labtime); 
        add(tftime); 
        // IntensityEnvelope.Test(); 
    }

	/////////////////////////////////////////////
    void GenerateTodes()
    {
        tnc = null; 
		if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
        {
            sketchdisplay.sketchgraphicspanel.SelectConnectedSetsFromSelection(); 
            if (!sketchdisplay.sketchgraphicspanel.vactivepaths.isEmpty())
            {
                tnc = new TodeNodeCalc(sketchdisplay.sketchgraphicspanel.vactivepaths); 
                sketchdisplay.sketchgraphicspanel.ClearSelection(true); 
				sketchdisplay.sketchgraphicspanel.repaint();
            }
        }
        if (tnc == null)
            tnc = new TodeNodeCalc(sketchdisplay.sketchgraphicspanel.tsketch.vpaths); 

        tftime.setText(String.format("%.3f", tnc.T)); 
        prevT = -1.0; 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    TodeFibre FindTodeFibre(OnePath op)
    {
        if ((op == null) || (tnc == null))
            return null; 
        for (TodeFibre todefibre : tnc.todefibres)
        {
            if (todefibre.op == op)
                return todefibre; 
        }
        return null; 
    }

	/////////////////////////////////////////////
    void OutputEnvelope(OnePath op, boolean btrain)
    {
        TodeFibre todefibre = FindTodeFibre(op); 
        if (todefibre == null)
            return; 
        TodeNode todenode = todefibre.fromnode; 

        tfpathlength.setText(String.format("%.3f", todefibre.timelength)); 

        if (btrain)
        {
            if (todenode != null)
                System.out.println("SpikesRel: " + todenode.spiketimes.toStringRel()); 
            return; 
        }

        if (todefibre != null)
        {
            System.out.println("Length: " + todefibre.timelength); 
            System.out.println("IntensityEnvelope: " + todefibre.intensityenvelope); 
        }

        if (todenode != null)
        {
            System.out.println("RefactoryEnvelope: " + todenode.refactoryenvelope); 
            System.out.println("Spikes: " + todenode.spiketimes); 
            System.out.println("SpikesRel: " + todenode.spiketimes.toStringRel()); 
            if (todenode.nextspike != -1.0)
                System.out.println("Next spike: " + todenode.nextspike); 
            System.out.println("CurrentIntensity: " + todenode.nodeintensity); 
            System.out.println("CurrentEnvelope: "); 
            for (IntensityWedge wedge : todenode.currentenvelope.wedges)
                System.out.println("  " + wedge); 
        }
    }

	/////////////////////////////////////////////
    void AdvanceEventB()
    {
        bnextfaster = true; 
        if (animthread == null)
            AdvanceEvent(); 
    }

	/////////////////////////////////////////////
    void AdvanceEvent()
    {
        if (tnc == null)
            return; 
        
        double advancetime = (!bnextfaster ? 0.02 : 1.0); 
        try
        {
            advancetime = Float.parseFloat(!bnextfaster ? tfanimtime.getText() : tfadvancetime.getText()); 
        }
        catch (NumberFormatException ne)
        {;}

        bnextfaster = false; 
        tnc.AdvanceTime(advancetime); 
        tftime.setText(String.format("%.3f", tnc.T)); 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    void AdaptPhase()
    {
        System.out.println("Adapt"); 
        for (TodeNode todenode : tnc.todenodes)
        {
            if (todenode.adaptivequotient != 0.0)
            {
                System.out.println("Adaptive " + todenode.adaptivequotient); 
                for (TodeFibre todefibre : todenode.incomingfibres)
                    System.out.println(todefibre.closestspiketimelength); 
            }
        }
    }

	/////////////////////////////////////////////
    void UpdateT(double lT)
    {
        if (lT < tnc.T)
        {
            tnc.T = lT; 
            tnc.RecalculateAll(); 
        }
        else 
        {
            while (lT > tnc.T)
                tnc.AdvanceTime(lT - tnc.T); 
        }
        tftime.setText(String.format("%.3f", tnc.T)); 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    class AdvanceNodeThread implements Runnable
    {
    	/////////////////////////////////////////////
        public void run() 
        {
            try
            {
            while (true)
            {
                AdvanceEvent(); 
                Thread.sleep(50); 
            }
            }
            catch (InterruptedException ie)
            {;}
            animthread = null; 
        }
    }


	/////////////////////////////////////////////
    // 1 - 1 / (1 + x)
    int asymd(double val, double valsca, int len)
    {
        double kap = 1.0 - 1.0 / (1.0 + Math.abs(val / valsca)); 
        int res = (int)(kap * (len - 1) + 0.9); 
        assert ((res >= 0) && (res < len)); 
        return res; 
    }

	/////////////////////////////////////////////
    void painttodenode(GraphicsAbstraction ga)
    {
        if (tnc == null)
            return; 

        boolean bsomeaction = tnc.PosSpikes(); 
        if (!bsomeaction && buttanimate.isSelected())
            buttanimate.setSelected(false); 

        // draw the spirals on the nodes
        AffineTransform at = ga.g2d.getTransform(); 
        for (TodeNode todenode : tnc.todenodes)
        {
            if (todenode.nodeintensity == 0.0)
                continue; 

            ga.g2d.translate(todenode.opn.pn.getX(), todenode.opn.pn.getY()); 

            // draw any positive external node intensity
            if ((todenode.externalnodeintensity > 0.0) && (todenode.externalnodeintensity > todenode.nodeintensity))
            {
                int nivalue = asymd(todenode.externalnodeintensity, todenode.nodethreshold, gpspirals.length); 
                GeneralPath gppos = gpspirals[nivalue]; 
                ga.drawShape(gppos, SketchLineStyle.activepnlinestyleattr, lightgreen); 
            }

            // draw any positive node intensity
            if (todenode.nodeintensity > 0.0)
            {
                int nivalue = asymd(todenode.nodeintensity, todenode.nodethreshold, gpspirals.length); 
                GeneralPath gppos = gpspirals[nivalue]; 
                ga.drawShape(gppos, SketchLineStyle.activepnlinestyleattr, Color.green); 
            }

            // draw any negative external node intensity
            if (todenode.externalnodeintensity < 0.0)
            {
                int nivalue = asymd(-todenode.externalnodeintensity, todenode.nodethreshold, gpspirals.length); 
                GeneralPath gppos = gpspirals[nivalue]; 
                ga.drawShape(gppos, SketchLineStyle.activepnlinestyleattr, Color.red); 
            }

            // draw any negative negative refactory splay
            if ((todenode.nodeintensity < 0.0) && (todenode.refactorynodeintensity < 0.0))
            {
                int nivalue = (int)(gpsplaysnegative.length * Math.abs(todenode.refactorynodeintensity / (todenode.refactoryenvelope.emin + 0.01)) + 0.9); 
                GeneralPath gppos = gpsplaysnegative[Math.min(nivalue, gpsplaysnegative.length - 1)]; 
                ga.drawShape(gppos, lsanegsplay); 
            }

            ga.g2d.setTransform(at); 
        }

        // draw the spikes on the fibres
        for (int i = 0; i < tnc.nspikelist; i++)
        {
            PosSpikeViz psv = tnc.spikelist.get(i); 
            double spikel = SketchLineStyle.strokew * 2.2 / psv.stan.distance(0.0, 0.0); 
            Line2D lnspike = new Line2D.Double(psv.spos.getX(), psv.spos.getY(), psv.spos.getX() - psv.stan.getX() * spikel, psv.spos.getY() - psv.stan.getY() * spikel); 
            ga.drawShape(lnspike, lsaspikeout);
            ga.drawShape(lnspike, lsaspike);
        }

        // highlight the nodes spiked since last paint
        for (TodeNode todenode : tnc.todenodes)
        {
            if (todenode.spiketimes.size() == 0)
                continue; 
            double tspike = todenode.spiketimes.get(todenode.spiketimes.size() - 1); 
            if ((tspike > prevT) && (tspike <= tnc.T))
            {
                ga.g2d.translate(todenode.opn.pn.getX(), todenode.opn.pn.getY()); 
                ga.g2d.setColor(Color.yellow); 
                ga.g2d.fill(acspike); 
                ga.g2d.setTransform(at); 
            }        
        }
        prevT = tnc.T; 
    }
}


