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

import javax.swing.JFrame; 
import javax.swing.JPanel; 
import javax.swing.JButton; 

import javax.swing.JLabel; 
import javax.swing.JCheckBox; 

import javax.swing.JTextField; 
import javax.swing.JOptionPane; 

import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 

import java.awt.BorderLayout; 
import java.awt.GridLayout; 
import java.awt.Graphics; 

import java.util.Vector; 
import java.awt.FileDialog;


import java.io.IOException; 
import java.io.File;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 

import javax.swing.JDialog; 


class GlassDialog 
{
	JDialog jdwait; 
	JDialog gdiag; 
	GlassView glassview; 

	JCheckBox glsShadeVol = new JCheckBox("Shade", null, true); 
	JCheckBox glsOutline = new JCheckBox("Outline", null, true); 
	JLabel glTrans = new JLabel("Transluce"); 
	JTextField gtfTrans = new JTextField("0.2"); 
	JLabel glThickCoeff = new JLabel("Thickness Coefficient"); 
	JTextField gtfThickCoeff = new JTextField("80"); 
	JLabel glforelight = new JLabel("Fore-light"); 
	JTextField gtfforelight = new JTextField("0.8"); 
	JLabel glbacklight = new JLabel("Back-light"); 
	JTextField gtfbacklight = new JTextField("2.0"); 
	
	GlassDialog(JFrame frame, GlassView lglassview) 
	{
		glassview = lglassview; 

		// build the main dialog
		gdiag = new JDialog(frame, true); 

		JPanel gopts = new JPanel(new GridLayout(0, 2)); 
		gopts.add(glsShadeVol); 
		gopts.add(glsOutline); 
		gopts.add(glTrans); 
		gopts.add(gtfTrans); 
		gopts.add(glThickCoeff); 
		gopts.add(gtfThickCoeff); 
		gopts.add(glforelight); 
		gopts.add(gtfforelight); 
		gopts.add(glbacklight); 
		gopts.add(gtfbacklight); 

		JPanel gbuttons = new JPanel(); 
		JButton Bokay = new JButton("Okay"); 
		Bokay.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { BDimage(); gdiag.setVisible(false); } } ); 

		JButton Bapply = new JButton("Apply"); 
		Bapply.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { BDimage(); } } ); 

		JButton Bcancel = new JButton("Cancel"); 
		Bcancel.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) { gdiag.setVisible(false); } } ); 

		gbuttons.add(Bapply); 
		gbuttons.add(Bokay); 
		gbuttons.add(Bcancel); 

		JPanel gdpan = new JPanel(new BorderLayout()); 
		gdpan.add("North", gopts); 
		gdpan.add("South", gbuttons); 

		gdiag.getContentPane().add(gdpan); 
		gdiag.pack(); 

		// build the wait dialog 
		// jdwait = new JDialog(frame, true); 
	}

	boolean calcsolid() 
	{
		// needs a wait + abort dialog box.  
		glassview.CalcSolid(); 
		return true; 
	}

	void BDimage() 
	{
		float thfac = 0.03F; 
		float trans = 0.0F; 
		float Fforelight = 1.0F; 
		float Fbacklight = 1.0F; 
		try
		{
			thfac = Float.valueOf(gtfThickCoeff.getText()).floatValue(); 
			trans = Float.valueOf(gtfTrans.getText()).floatValue(); 
			Fforelight = Float.valueOf(gtfforelight.getText()).floatValue(); 
			Fbacklight = Float.valueOf(gtfbacklight.getText()).floatValue(); 
		}
		catch (NumberFormatException e) {;}; 

		glassview.BuildImage(glsOutline.isSelected(), glsShadeVol.isSelected(), thfac, trans, Fforelight, Fbacklight);  
		glassview.wg.repaint(); 
	
	} 

	void preshowImParms() 
	{
		gdiag.show(); 
	}
}

