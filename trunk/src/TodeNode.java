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
    double GetIntensity(double eT)
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
        e1 = rightwedge.GetIntensity(tsplit); 
        slope = rightwedge.slope; 
        assert VerifySlope(); 

        rightwedge.t0 = tsplit; 
        rightwedge.e0 = e1; 
        assert rightwedge.VerifySlope(); 
    }

    /////////////////////////////////////////////
    boolean VerifySlope()
    {
        double lslope = (e1 - e0) / (t1 - t0); 
        return (Math.abs(lslope - slope) <= 0.01); 
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

    /////////////////////////////////////////////
    double GetIntensity(double eT)
    {
        for (int i = 0; i < wedges.size(); i++)
        {
            if ((wedges.get(i).t0 <= eT) && (eT < wedges.get(i).t1)) 
                return wedges.get(i).GetIntensity(eT); 
        }
        return 0.0; 
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
        double eehalf = GetIntensity(tehalf) + newwedge.GetIntensity(tehalf); 
        double ten1 = newwedge.t0 * 0.001 + newwedge.t1 * 0.999; 
        double een1 = GetIntensity(ten1) + newwedge.GetIntensity(ten1); 
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
            IntensityWedge outerwedge = new IntensityWedge(newwedge.t0, newwedge.e0, wedges.get(i0).t0, newwedge.GetIntensity(wedges.get(i0).t0)); 
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
            IntensityWedge outerwedge = new IntensityWedge(wedges.get(i1).t1, newwedge.GetIntensity(wedges.get(i1).t1), newwedge.t1, newwedge.e1); 
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
            wedge.e0 += newwedge.GetIntensity(wedge.t0); 
            wedge.e1 += newwedge.GetIntensity(wedge.t1); 
            wedge.slope += newwedge.slope; 
            assert wedge.VerifySlope(); 

            // gap filling wedge
            if ((i != i0) && (wedges.get(i - 1).t1 < wedges.get(i).t0))
            {
                IntensityWedge gapwedge = new IntensityWedge(wedges.get(i - 1).t1, newwedge.GetIntensity(wedges.get(i - 1).t1), wedge.t0, newwedge.GetIntensity(wedge.t0)); 
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

    double nextspike = -1.0; 
    double nodeintensity = 0.0; 
    IntensityEnvelope currentenvelope = new IntensityEnvelope(); 

    /////////////////////////////////////////////
    TodeNode(OnePathNode lopn)
    {
        opn = lopn; 

        refactoryenvelope.wedges.add(new IntensityWedge(-0.001, -10.0, 0.9, -5.0)); // back in time slightly to suppress the spike that was there
        refactoryenvelope.wedges.add(new IntensityWedge(0.9, -5.0, 1.1, 0.0)); 
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
            if (wedge.e0 >= 1.0)
            {
                nextspike = wedge.t0; 
                break; 
            }
            if (wedge.e1 >= 1.0)
            {
                double lam = (1.0 - wedge.e0) / (wedge.e1 - wedge.e0); 
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

    OnePath op; 
    Double[] opseglengths; // for drawing the spike on the path

    /////////////////////////////////////////////
    TodeFibre(OnePath lop, TodeNode lfromnode, TodeNode ltonode)
    {
        op = lop; 
        fromnode = lfromnode; 
        tonode = ltonode; 
        timelength = op.linelength / TN.CENTRELINE_MAGNIFICATION; // linear distance; opseglengths[-1] would be spline length

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

    /////////////////////////////////////////////
    void RecalculateAll()
    {
        todenodesnextspikes.clear(); 
        for (TodeNode todenode : todenodes)
            todenode.RecalcEnvelope(); 
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
        return null; 
    }


    /////////////////////////////////////////////
    void AddPrecodedSpikes(OnePath op, double Toffset)
    {
        assert (op.linestyle == SketchLineStyle.SLS_CONNECTIVE); 
        TodeNode tonode = FindTodeNode(op.pnend); 
        String drawlab = (op.plabedl != null ? op.plabedl.drawlab : ""); 
        String[] nums = drawlab.split("[\\s,]+"); 
        try
        {
            for (int i = 0; i < nums.length; i++)
            {
                double st = Float.parseFloat(nums[i]); 
                tonode.spiketimes.add(st + Toffset);
            }
            if (nums.length != 0)
                tonode.bprecodedspikes = true; 
        }
        catch (NumberFormatException e)
        {;}
        tonode.spiketimes.sort(); 
System.out.println("SSHS " + tonode.spiketimes); 
    }


    /////////////////////////////////////////////
    TodeNodeCalc(OneSketch tsketch)
    {
        for (OnePathNode opn : tsketch.vnodes)
            todenodes.add(new TodeNode(opn)); 

        for (OnePath op : tsketch.vpaths)
        {
            TodeNode tonode = FindTodeNode(op.pnend); 
            if (op.linestyle != SketchLineStyle.SLS_CONNECTIVE)
                todefibres.add(new TodeFibre(op, FindTodeNode(op.pnstart), tonode)); 
            else
                AddPrecodedSpikes(op, 0.0); 
        }

        for (TodeFibre todefibre : todefibres)
        {
            double mag = 1.1 / todefibre.tonode.incomingfibres.size(); 
            todefibre.intensityenvelope.wedges.add(new IntensityWedge(0.0, 0.0, 0.1, mag)); 
            todefibre.intensityenvelope.wedges.add(new IntensityWedge(0.1, mag, 2.0, 0.0)); 
        }

        RecalculateAll(); 
        NextSpikeAll(); 
    }


    /////////////////////////////////////////////
    // this function calculates spikes and intensity independently of the nextspike calculation, and should agree with it
    List<PosSpikeViz> spikelist = new ArrayList<PosSpikeViz>(); 
    int nspikelist = 0; 
    void PosSpikes() 
    {
        nspikelist = 0; 
        for (TodeNode todenode : todenodes)
        {
            todenode.nodeintensity = 0.0; 

            for (TodeFibre todefibre : todenode.incomingfibres)
            {
                for (int i = 0; i < todefibre.fromnode.spiketimes.size(); i++)
                {
                    double st = T - todefibre.fromnode.spiketimes.get(i); 
                    if (st < 0.0)
                    {
                        assert todenode.bprecodedspikes; 
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
                    todenode.nodeintensity += todefibre.intensityenvelope.GetIntensity(est); 
                }
            }

            // add in the refactory intensities from the spikes here
            for (int i = 0; i < todenode.spiketimes.size(); i++)
            {
                double st = T - todenode.spiketimes.get(i); 
                if (st < 0.0)
                    continue; // only can happen for precoded trains
                todenode.nodeintensity += todenode.refactoryenvelope.GetIntensity(st); 
            }
        }
    }
}


/////////////////////////////////////////////
class TodeNodePanel extends JPanel
{
	SketchDisplay sketchdisplay;
    JButton buttgeneratetodes = new JButton("Gen Todes"); 
    JButton buttoutputenvelope = new JButton("OutEnv"); 
    JButton buttadvance = new JButton("Advance"); 
    JTextField tfadvancetime = new JTextField("1.0", 5); 
    JToggleButton buttanimate = new JToggleButton("Anim"); 
    JTextField tfanimtime = new JTextField("0.02", 5); 
    JLabel labtime = new JLabel("T:"); 
    JTextField tftime = new JTextField(5); 

    Thread animthread = null; 
    boolean bnextfaster = false; 

    int spiralposperunit = 20; 
    int posmult = 3; 
    double negmult = 0.5; 
    GeneralPath[] gpspiralspositive; 
    GeneralPath[] gpspiralsnegative; 

    Shape acspike = null; 

    LineStyleAttr lsaspikeout = null; 
    LineStyleAttr lsaspike = null; 

    double prevT = 0.0; 
    TodeNodeCalc tnc; 

	/////////////////////////////////////////////
    void BuildSpirals()
    {
        if (SketchLineStyle.strokew == -1.0F)
            return; 
        double spiralw = SketchLineStyle.strokew * 2.2; 

        gpspiralspositive = new GeneralPath[spiralposperunit * posmult + 1]; 
        for (int j = 0; j < gpspiralspositive.length; j++)
        {
            gpspiralspositive[j] = new GeneralPath(); 
            gpspiralspositive[j].moveTo(0.0F, 0.0F); 
        }
        for (int i = 1; i < gpspiralspositive.length; i++)
        {
            double lam = i * 1.0 / spiralposperunit;  // exagerate the spirals on the positive side
            double x = TN.degcos(lam * 360) * lam * spiralw; 
            double y = TN.degsin(lam * 360) * lam * spiralw; 
            for (int j = i; j < gpspiralspositive.length; j++) 
                gpspiralspositive[j].lineTo((float)x, (float)y); 
        }

        gpspiralsnegative = new GeneralPath[spiralposperunit * 5 + 1]; 
        for (int j = 0; j < gpspiralsnegative.length; j++)
        {
            gpspiralsnegative[j] = new GeneralPath(); 
            gpspiralsnegative[j].moveTo(0.0F, 0.0F); 
        }
        for (int i = 1; i < gpspiralsnegative.length; i++)
        {
            double lam = i * 1.0 / spiralposperunit; 
            double x = TN.degcos(lam * 360) * lam * spiralw; 
            double y = TN.degsin(lam * 360) * lam * spiralw; 
            for (int j = i; j < gpspiralsnegative.length; j++) 
                gpspiralsnegative[j].lineTo(-(float)x, (float)y); 
        }

        acspike = new Ellipse2D.Double(-spiralw * 2.2, -spiralw * 2.2, spiralw * 4.4, spiralw * 4.4); 

        lsaspikeout = new LineStyleAttr(SketchLineStyle.SLS_DETAIL, 2.5F*SketchLineStyle.strokew, 0, 0, 0, Color.white); 
        lsaspike = new LineStyleAttr(SketchLineStyle.SLS_DETAIL, 2.0F*SketchLineStyle.strokew, 0, 0, 0, Color.red); 
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
			{ public void actionPerformed(ActionEvent e) { OutputEnvelope(sketchdisplay.sketchgraphicspanel.currgenpath); } } ); 	
        buttadvance.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdvanceEventB(); } } ); 

        buttanimate.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)  
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


		setLayout(new GridLayout(0, 2));

        add(buttgeneratetodes); 
        add(buttoutputenvelope); 
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
        tnc = new TodeNodeCalc(sketchdisplay.sketchgraphicspanel.tsketch); 
        tftime.setText(String.format("%.3f", tnc.T)); 
        prevT = -1.0; 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    void OutputEnvelope(OnePath op)
    {
        if (op == null)
            return; 
        if (tnc == null)
            return; 
        OnePathNode opn = op.pnstart; 

        for (TodeFibre todefibre : tnc.todefibres)
        {
            if (todefibre.op == op)
                System.out.println("Length: " + todefibre.timelength); 
        }

        for (TodeNode todenode : tnc.todenodes)
        {
            if (todenode.opn == opn)
            {
                System.out.println("Spikes: " + todenode.spiketimes); 
                if (todenode.nextspike != -1.0)
                    System.out.println("Next spike: " + todenode.nextspike); 
                System.out.println("Envelope: "); 
                for (IntensityWedge wedge : todenode.currentenvelope.wedges)
                    System.out.println("  " + wedge); 
            }
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
    void UpdateT(double lT)
    {
        tnc.T = lT; 
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
    void painttodenode(GraphicsAbstraction ga)
    {
        if (tnc == null)
            return; 

        tnc.PosSpikes(); 

        // draw the spirals on the nodes
        AffineTransform at = ga.g2d.getTransform(); 
        for (TodeNode todenode : tnc.todenodes)
        {
            if (todenode.nodeintensity == 0.0)
                continue; 
            int nivalue = (int)(((todenode.nodeintensity > 0.0 ? todenode.nodeintensity * posmult : -todenode.nodeintensity * negmult) 
                                * spiralposperunit) + 0.9); 
            ga.g2d.translate(todenode.opn.pn.getX(), todenode.opn.pn.getY()); 

            GeneralPath gp = (todenode.nodeintensity > 0.0 ? gpspiralspositive[Math.min(nivalue, gpspiralspositive.length - 1)] : gpspiralsnegative[Math.min(nivalue, gpspiralsnegative.length - 1)]); 
            ga.drawShape(gp, SketchLineStyle.activepnlinestyleattr, (todenode.nodeintensity > 0.0 ? Color.green : Color.blue)); 

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


