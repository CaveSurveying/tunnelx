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
    DoubleArray currentenvelope = new DoubleArray(); 
    double nextspike = -1; 

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

    TodeFibre(OnePath lop, TodeNode lfromnode, TodeNode ltonode)
    {
        op = lop; 
        fromnode = lfromnode; 
        tonode = ltonode; 
        timelength = op.linelength / TN.CENTRELINE_MAGNIFICATION; // linear distance; opseglengths[-1] would be spline length

        // visualization stuff
        opseglengths = new Double[op.nlines]; 
        for (int i = 0; i < op.nlines; i++)
            opseglengths[i] = (i == 0 ? 0.0 : opseglengths[i - 1]) + op.MeasureSegmentLength(i);  
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
}


/////////////////////////////////////////////
class TodeNodePanel extends JPanel
{
	SketchDisplay sketchdisplay;
    JButton buttgeneratetodes = new JButton("Gen Todes"); 
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

        add(buttgeneratetodes); 
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
    }

	/////////////////////////////////////////////
    void GenerateTodes()
    {
        tnc = new TodeNodeCalc(sketchdisplay.sketchgraphicspanel.tsketch); 
    }

	/////////////////////////////////////////////
    void UpdateT(double lT)
    {
        tnc.T = lT; 
        tftime.setText(String.valueOf(tnc.T)); 
        sketchdisplay.sketchgraphicspanel.repaint(); 
    }

	/////////////////////////////////////////////
    Point2D spos = new Point2D.Double(); 
    void painttodenode(GraphicsAbstraction ga)
    {
        if (tnc == null)
            return; 
        for (TodeNode todenode : tnc.todenodes)
            ga.drawShape(todenode.opn.Getpnell(), null, Color.green);

        for (TodeFibre todefibre : tnc.todefibres)
        {
            for (int i = 0; i < todefibre.fromnode.spiketimes.size(); i++)
            {
                double st = tnc.T - todefibre.fromnode.spiketimes.get(i); 
                if ((st >= 0.0) && (st <= todefibre.timelength))
                {
                    double dst = st / todefibre.timelength * todefibre.opseglengths[todefibre.opseglengths.length - 1]; 
                    int j = 0; 
                    for ( ; j < todefibre.opseglengths.length - 1; j++)
                        if (todefibre.opseglengths[j] >= dst)
                            break; 
                    double ljm1 = (j == 0 ? 0.0 : todefibre.opseglengths[j-1]); 
                    double tr = (dst - ljm1) / (todefibre.opseglengths[j] - ljm1); 
                    todefibre.op.EvalSeg(spos, null, j, tr); 
                    
                    float currstrokew = SketchLineStyle.strokew; 
                    Shape spike = new Rectangle2D.Float((float)spos.getX() - 2 * currstrokew, (float)spos.getY() - 2 * currstrokew, 4 * currstrokew, 4 * currstrokew);
                    ga.drawShape(spike, null, Color.red);
                }
            }
        }
    }
}


