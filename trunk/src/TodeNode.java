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
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

import java.awt.geom.Point2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.Shape; 

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;
import java.awt.GridLayout;

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
}

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
    void AddWedge(IntensityWedge newwedge)
    {
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
        System.out.println("Env: " + this); 
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
                System.out.println("Wedge: " + wedge); 
                ie.AddWedge(wedge); 
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
class TodeNode
{
    OnePathNode opn; 
    DoubleArray spiketimes = new DoubleArray(); 
    List<TodeFibre> outgoingfibres = new ArrayList<TodeFibre>(); 
    List<TodeFibre> incomingfibres = new ArrayList<TodeFibre>(); 
    //DoubleArray currentenvelope = new DoubleArray(); 
    double nextspike = -1; 

    double nodeintensity = 0.0; 

    TodeNode(OnePathNode lopn)
    {
        opn = lopn; 
    }
}

/////////////////////////////////////////////
class TodeFibre
{
    OnePath op; 
    TodeNode fromnode; 
    TodeNode tonode; 
    double timelength; 
    IntensityEnvelope intensityenvelope = new IntensityEnvelope(); 
    DoubleArray envelope = new DoubleArray(); 

    Double[] opseglengths; 

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

        envelope.add(0.0); 
        envelope.add(0.0); 
        envelope.add(0.1); 
        envelope.add(1.1); 
        envelope.add(2.0); 
        envelope.add(0.0); 
intensityenvelope.wedges.add(new IntensityWedge(0.0, 0.0, 0.1, 1.1)); 
intensityenvelope.wedges.add(new IntensityWedge(0.1, 1.1, 2.0, 0.0)); 
    }


    /////////////////////////////////////////////
    void SetPosD(Point2D spos, double dst)
    {
        int j = 0; 
        for ( ; j < opseglengths.length - 1; j++)
            if (opseglengths[j] >= dst)
                break; 
        double ljm1 = (j == 0 ? 0.0 : opseglengths[j-1]); 
        double tr = (dst - ljm1) / (opseglengths[j] - ljm1); 
        op.EvalSeg(spos, null, j, tr); 
    }
}

/////////////////////////////////////////////
class TodeNodeCalc
{
    List<TodeNode> todenodes = new ArrayList<TodeNode>(); 
    List<TodeFibre> todefibres = new ArrayList<TodeFibre>(); 
    double T = 0; 

    /////////////////////////////////////////////
    TodeNode FindTodeNode(OnePathNode opn)
    {
        for (TodeNode tn : todenodes)
            if (tn.opn == opn)
                return tn; 
        return null; 
    }

    /////////////////////////////////////////////
    TodeNodeCalc(OneSketch tsketch)
    {
        for (OnePathNode opn : tsketch.vnodes)
            todenodes.add(new TodeNode(opn)); 

        for (OnePath op : tsketch.vpaths)
        {
            TodeNode tonode = FindTodeNode(op.pnend); 
            if (op.linestyle == SketchLineStyle.SLS_CONNECTIVE)
            {
                String drawlab = (op.plabedl != null ? op.plabedl.drawlab : ""); 
                String[] nums = drawlab.split("[\\s,]+"); 
                try
                {
                    for (int i = 0; i < nums.length; i++)
                        tonode.spiketimes.add(Float.parseFloat(nums[i]));
                }
                catch (NumberFormatException e)
                {;}
            }
            else
                todefibres.add(new TodeFibre(op, FindTodeNode(op.pnstart), tonode)); 
        }
    }


    /////////////////////////////////////////////
    class PosSpikeViz
    {
        Point2D spos = new Point2D.Double(); 
        boolean batend; 
        double intensity; // when batend
    }

    /////////////////////////////////////////////
    // this function calculates spikes and intensity independently of the nextspike calculation, and should agree with it
    List<PosSpikeViz> spikelist = new ArrayList<PosSpikeViz>(); 
    int nspikelist = 0; 
    void PosSpikes(double sca) 
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
                        continue; // only can happen for precoded trains
                    // spike on the fibre
                    if (st < todefibre.timelength)
                    {
                        if (nspikelist == spikelist.size())
                            spikelist.add(new PosSpikeViz()); 
                        double dst = st / todefibre.timelength * todefibre.opseglengths[todefibre.opseglengths.length - 1]; 
                        spikelist.get(nspikelist).batend = false; 
                        todefibre.SetPosD(spikelist.get(nspikelist).spos, dst); 
                        nspikelist++; 
                        continue; 
                    }

                    // spike glued to the end of the fibre
                    double est = st - todefibre.timelength; 
                    if (!todefibre.intensityenvelope.wedges.isEmpty() && (est < todefibre.intensityenvelope.wedges.get(todefibre.intensityenvelope.wedges.size() - 1).t1))
                    {
                        if (nspikelist == spikelist.size())
                            spikelist.add(new PosSpikeViz()); 
                        double fintensity = todefibre.intensityenvelope.GetIntensity(est); 
                        double dste = todefibre.opseglengths[todefibre.opseglengths.length - 1] - TN.CENTRELINE_MAGNIFICATION * SketchLineStyle.strokew/2; 
                        double dst = Math.max(dste, todefibre.opseglengths[todefibre.opseglengths.length - 1] / 2); 
                        spikelist.get(nspikelist).batend = true; 
                        todefibre.SetPosD(spikelist.get(nspikelist).spos, dst); 
                        spikelist.get(nspikelist).intensity = fintensity; 
                        todenode.nodeintensity += fintensity; 
                        nspikelist++; 
                    }
                }
            }
        }
    }
}


/////////////////////////////////////////////
class TodeNodePanel extends JPanel
{
	SketchDisplay sketchdisplay;
    JButton buttgeneratetodes = new JButton("Gen Todes"); 
    JButton buttadvance = new JButton("Advance"); 
    JTextField tfadvancetime = new JTextField("0.5", 5); 
    JTextField tftime = new JTextField(5); 

    TodeNodeCalc tnc; 

	/////////////////////////////////////////////
    TodeNodePanel(SketchDisplay lsketchdisplay)
    {
		//Font[] fs = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		//for (int i = 0; i < fs.length; i++)
		//	System.out.println(fs[i].toString());

    	sketchdisplay = lsketchdisplay;

		buttgeneratetodes.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { GenerateTodes(); } } ); 	

		setLayout(new GridLayout(0, 2));

        add(buttgeneratetodes); 
        add(new JLabel()); 
        add(buttadvance); 
        add(tfadvancetime); 
        add(new JLabel("T:")); 
        add(tftime); 

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

        buttadvance.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdvanceEvent(); } } ); 
        tfadvancetime.addActionListener(new ActionListener() 
            { public void actionPerformed(ActionEvent e)  { AdvanceEvent(); } } ); 

//IntensityEnvelope.Test(); 
    }

	/////////////////////////////////////////////
    void GenerateTodes()
    {
        tnc = new TodeNodeCalc(sketchdisplay.sketchgraphicspanel.tsketch); 
        UpdateT(0.0); 
    }

	/////////////////////////////////////////////
    void AdvanceEvent()
    {
        double advancetime = 1.0; 
        try
        {
            advancetime = Float.parseFloat(tfadvancetime.getText()); 
        }
        catch (NumberFormatException ne)
        {;}
        UpdateT(tnc.T + advancetime); 
    }

	/////////////////////////////////////////////
    void UpdateT(double lT)
    {
        tnc.T = lT; 
        tftime.setText(String.valueOf(tnc.T)); 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    void painttodenode(GraphicsAbstraction ga)
    {
        if (tnc == null)
            return; 
        tnc.PosSpikes(sketchdisplay.sketchgraphicspanel.currtrans.getScaleX()); 


        for (TodeNode todenode : tnc.todenodes)
            ga.drawShape(todenode.opn.Getpnell(), null, (todenode.nodeintensity == 0.0 ? Color.black : Color.green)); 

        float currstrokew = SketchLineStyle.strokew*0.8F; 
        for (int i = 0; i < tnc.nspikelist; i++)
        {
            Point2D spos = tnc.spikelist.get(i).spos; 
            Shape spike = new Rectangle2D.Float((float)spos.getX() - 2 * currstrokew, (float)spos.getY() - 2 * currstrokew, 4 * currstrokew, 4 * currstrokew);
            ga.drawShape(spike, null, (tnc.spikelist.get(i).batend ? Color.blue : Color.red));
        }
    }
}


