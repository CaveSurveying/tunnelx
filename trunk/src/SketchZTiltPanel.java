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

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JCheckBox;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import java.awt.geom.Point2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.Color;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;


/////////////////////////////////////////////
class SketchZTiltPanel extends JPanel
{
	SketchDisplay sketchdisplay;
	JButton buttselecttozselection = new JButton("Select to Z Selection"); 
	JCheckBox cbaShowTilt;
    JCheckBox cbaThinZheightsel; 
    JCheckBox cbaAnimateTour; 
	JButton buttanimatestep; 

	JTextField tfzlothinnedvisible = new JTextField();
	JTextField tfzhithinnedvisible = new JTextField();
	JTextField tfzstep = new JTextField("10.0");

	// z range thinning
	boolean bzthinnedvisible = false;   // causes reselecting of subset of paths in RenderBackground
	double zlothinnedvisible = -360.0; 
	double zhithinnedvisible = 20.0; 

	List<OnePathNode> opnpathanimation = new ArrayList<OnePathNode>(); 
    int opnpathanimationPos = 0; 
    
	/////////////////////////////////////////////
    SketchZTiltPanel(SketchDisplay lsketchdisplay)
    {
    	sketchdisplay = lsketchdisplay;

        cbaShowTilt = new JCheckBox("Show Tilted in z", false);
		cbaShowTilt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miShowTilt.isSelected() != cbaShowTilt.isSelected())
                    sketchdisplay.miShowTilt.doClick();
			} } );
        cbaThinZheightsel = new JCheckBox("Thin Z Selection", false);
		cbaThinZheightsel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miThinZheightsel.isSelected() != cbaThinZheightsel.isSelected())
                    sketchdisplay.miThinZheightsel.doClick();
			} } );
            
		tfzhithinnedvisible.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e) {
                try  { zhithinnedvisible = Float.parseFloat(tfzhithinnedvisible.getText()) - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z; }
                catch(NumberFormatException nfe)  {;}
                SetUpdatezthinned(); 
            }}); 
		tfzlothinnedvisible.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e) {
                try  { zlothinnedvisible = Float.parseFloat(tfzlothinnedvisible.getText()) - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z; }
                catch(NumberFormatException nfe)  {;}
                SetUpdatezthinned(); 
            }}); 
		buttselecttozselection.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { 
                SelectiontoZheightSelected();  SetUpdatezthinned(); 
			}});
            
        tfzlothinnedvisible.setText(String.valueOf(zlothinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
        tfzhithinnedvisible.setText(String.valueOf(zhithinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 

        cbaAnimateTour = new JCheckBox("Animate Tour", false);
		cbaAnimateTour.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath; 
                if ((op == null) || (op.plabedl == null) || (op.plabedl.drawlab == null))
                    return; 
                LineInputStream lis = new LineInputStream(op.plabedl.drawlab, null); 
                opnpathanimation.clear(); 
                opnpathanimationPos = 0; 
                while (lis.FetchNextLine())
                {
                    TN.emitMessage("anim on "+lis.w[0]); 
                    OnePathNode aopn = null; 
                    for (OnePathNode opn : sketchdisplay.sketchgraphicspanel.tsketch.vnodes)
                        if ((opn.pnstationlabel != null) && (opn.pnstationlabel.endsWith(lis.w[0])) && ((aopn == null) || (aopn.pnstationlabel.length() <= opn.pnstationlabel.length())))
                            aopn = opn; 
                    TN.emitMessage("kk" + aopn); 
                    opnpathanimation.add(aopn); 
                }
			} } );
        buttanimatestep = new JButton("Animate Step"); 
        buttanimatestep.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { 
                AnimateStep(); 
			}});
        
        
		JPanel panuppersec = new JPanel(new GridLayout(0, 2));
        panuppersec.add(cbaThinZheightsel); 
        panuppersec.add(cbaShowTilt);
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselWiden)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltOver));
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselNarrow)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltBack));
		panuppersec.add(buttselecttozselection); 
		panuppersec.add(new JButton(sketchdisplay.acvUpright)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneDown)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneUp)); 
		panuppersec.add(new JLabel("zhi-thinned:"));
		panuppersec.add(tfzhithinnedvisible); 
		panuppersec.add(new JLabel("zlo-thinned:"));
		panuppersec.add(tfzlothinnedvisible); 
		panuppersec.add(new JLabel("zstep:"));
		panuppersec.add(tfzstep); 

		panuppersec.add(cbaAnimateTour); 
		panuppersec.add(buttanimatestep); 

        setLayout(new BorderLayout());
		add(panuppersec, BorderLayout.CENTER);
	}

    /////////////////////////////////////////////
	Point2D.Float scrpt = new Point2D.Float();
	Point2D.Float realpt = new Point2D.Float();
    double animdiststep = 10.1; 
    void AnimateStep()
    {
        Dimension csize = sketchdisplay.sketchgraphicspanel.csize; 
		try
		{
			scrpt.setLocation(csize.width / 2, csize.height / 2);
			sketchdisplay.sketchgraphicspanel.currtrans.inverseTransform(scrpt, realpt);
		}
		catch (NoninvertibleTransformException ex)
            { realpt.setLocation(0, 0); }

TN.emitMessage("opnpathanimationPos " + opnpathanimationPos + "  " + opnpathanimation.size());
        if (opnpathanimationPos >= opnpathanimation.size())
        {
            cbaAnimateTour.setSelected(false); 
            return; 
        }

        Point2D.Float targetpt = opnpathanimation.get(opnpathanimationPos).pn; 
        float targetz = opnpathanimation.get(opnpathanimationPos).zalt; 
        double rx = realpt.getX(); 
        double ry = realpt.getY(); 
        double rz = (zlothinnedvisible + zhithinnedvisible) / 2;  
        
        double vx = targetpt.getX() - rx; 
        double vy = targetpt.getY() - ry; 
        double vz = (sketchdisplay.miThinZheightsel.isSelected() ? targetz - rz : 0.0); 
        double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz); 
        double lam = (animdiststep < vlen ? animdiststep / vlen : 1.0); 

        if (lam == 1.0)
            opnpathanimationPos++; 

        TN.emitMessage("lam " + lam);
        realpt.setLocation(rx + vx*lam, ry + vy*lam); 
		sketchdisplay.sketchgraphicspanel.currtrans.transform(realpt, scrpt);

        //sketchdisplay.sketchgraphicspanel.Translate(-(scrpt.getX() - csize.width / 2) / csize.width, -(scrpt.getY() - csize.height / 2) / csize.height); 
		sketchdisplay.sketchgraphicspanel.mdtrans.setToTranslation(-(scrpt.getX() - csize.width / 2), -(scrpt.getY() - csize.height / 2));
		sketchdisplay.sketchgraphicspanel.orgtrans.setTransform(sketchdisplay.sketchgraphicspanel.currtrans);
		sketchdisplay.sketchgraphicspanel.currtrans.setTransform(sketchdisplay.sketchgraphicspanel.mdtrans);
		sketchdisplay.sketchgraphicspanel.currtrans.concatenate(sketchdisplay.sketchgraphicspanel.orgtrans);

        if (sketchdisplay.miThinZheightsel.isSelected())
        {
            zlothinnedvisible = rz + vz*lam - animdiststep*10; 
            zhithinnedvisible = rz + vz*lam + animdiststep*10; 
            SetUpdatezthinned(); 
        }
        else
            sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
    }
    
    /////////////////////////////////////////////
    void SetUpdatezthinned()
    {
        if (zhithinnedvisible < zlothinnedvisible)
            zhithinnedvisible = zlothinnedvisible; 
        tfzlothinnedvisible.setText(String.valueOf(zlothinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
        tfzhithinnedvisible.setText(String.valueOf(zhithinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
		sketchdisplay.sketchgraphicspanel.UpdateTilt(true); 
		sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
	}

	/////////////////////////////////////////////
	boolean SelectiontoZheightSelected()
	{
        sketchdisplay.sketchgraphicspanel.CollapseVActivePathComponent(); 
        Set<OnePath> opselset = sketchdisplay.sketchgraphicspanel.MakeTotalSelList(); 
        if (opselset.isEmpty())
            return TN.emitWarning("No selection set for thinning by z so leaving the same"); 

        boolean bfirst = true; 
        for (OnePath op : opselset)
        {
            if (bfirst)
            {
                zlothinnedvisible = op.pnstart.zalt; 
                zhithinnedvisible = op.pnstart.zalt; 
                bfirst = false; 
            }
            else
            {
                if (op.pnstart.zalt < zlothinnedvisible)
                    zlothinnedvisible = op.pnstart.zalt; 
                else if (op.pnstart.zalt > zhithinnedvisible)
                    zhithinnedvisible = op.pnstart.zalt; 
            }
            if (op.pnend.zalt < zlothinnedvisible)
                zlothinnedvisible = op.pnend.zalt; 
            else if (op.pnend.zalt > zhithinnedvisible)
                zhithinnedvisible = op.pnend.zalt; 
        }
        return true; 
    }

	/////////////////////////////////////////////
	void ApplyZheightSelected(boolean bthinbyheight)
	{
        // on selection
		if (bthinbyheight)
        {
            SelectiontoZheightSelected(); 
			bzthinnedvisible = true; 
			TN.emitMessage("Thinning on z " + zlothinnedvisible + " < " + zhithinnedvisible); 
		}
		else  // on deselection
			bzthinnedvisible = false; 
        SetUpdatezthinned(); 
    }

    
	/////////////////////////////////////////////
	void WidenTiltPlane(int widencode)
	{
        double zwidgap = zhithinnedvisible - zlothinnedvisible; 
        double zwidgapfac = (widencode == 1 ? zwidgap / 2 : -zwidgap / 4); 
        zlothinnedvisible -= zwidgapfac; 
        zhithinnedvisible += zwidgapfac; 
        assert zlothinnedvisible <= zhithinnedvisible; 
        TN.emitMessage("Rethinning on z " + zlothinnedvisible + " < " + zhithinnedvisible); 
        SetUpdatezthinned(); 
    }


    /////////////////////////////////////////////
	void MoveTiltPlane(double stiltzchange)
	{
        double zstep = 50.0; 
        try  { zstep = Float.parseFloat(tfzstep.getText()); }
        catch(NumberFormatException nfe)  {;}
        double tiltzchange = stiltzchange * zstep; 
		zlothinnedvisible += tiltzchange;
		zhithinnedvisible += tiltzchange;
        SetUpdatezthinned(); 
	}
}


