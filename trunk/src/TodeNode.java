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
    }

    /////////////////////////////////////////////
    double GetIntensity(double eT)
    {
        if (envelope.size() == 0)
            return 0.0; 
        if (eT < envelope.get(0))
            return 0.0; 
        for (int i = 2; i < envelope.size(); i += 2)
        {
            if (eT > envelope.get(i)) 
                continue; 
            double d = envelope.get(i) - envelope.get(i - 2); 
            double lam = (d != 0.0 ? (eT - envelope.get(i - 2)) / d : 1.0); 
            return envelope.get(i - 1) * (1.0 - lam) + envelope.get(i + 1) * lam; 
        }
        return 0.0; 
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
                    if (est <= todefibre.envelope.get(todefibre.envelope.size() - 2))
                    {
                        if (nspikelist == spikelist.size())
                            spikelist.add(new PosSpikeViz()); 
                        double fintensity = todefibre.GetIntensity(est); 
                        double dste = todefibre.opseglengths[todefibre.opseglengths.length - 1] - SketchLineStyle.strokew/2; 
System.out.println(" GG " + dste + " " + todefibre.opseglengths[todefibre.opseglengths.length - 1] + " " + fintensity + " " + sca); 
                        double dst = Math.max(todefibre.opseglengths[todefibre.opseglengths.length - 1] - SketchLineStyle.strokew/2, todefibre.opseglengths[todefibre.opseglengths.length - 1] / 2); 
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


