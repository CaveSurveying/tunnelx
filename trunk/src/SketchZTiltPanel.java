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
import java.awt.Color;

import java.util.Set;


/////////////////////////////////////////////
class SketchZTiltPanel extends JPanel
{
	SketchDisplay sketchdisplay;
	JButton buttselecttozselection = new JButton("Select to Z Selection"); 
	JCheckBox cbaShowTilt;
    JCheckBox cbaThinZheightsel; 

	JTextField tfzlothinnedvisible = new JTextField();
	JTextField tfzhithinnedvisible = new JTextField();
	JTextField tfzstep = new JTextField("50.0");

	// z range thinning
	boolean bzthinnedvisible = false;   // causes reselecting of subset of paths in RenderBackground
	double zlothinnedvisible = -360.0; 
	double zhithinnedvisible = 20.0; 
	double zlovisible;   // the range that is captured beyond.  Probably not very interesting and should be abolished
	double zhivisible; 

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
    

        setLayout(new BorderLayout());
		add(panuppersec, BorderLayout.CENTER);
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


