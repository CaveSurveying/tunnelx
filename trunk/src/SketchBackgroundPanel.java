////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import java.util.Vector;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import java.awt.geom.Point2D;


/////////////////////////////////////////////
class SketchBackgroundPanel extends JPanel
{
	SketchDisplay sketchdisplay;

	JTextField sfbackground = new JTextField("");
	File backgrounddir = null;

	// tells us the grid spacing.
    JTextField tfgridspacing = new JTextField("");
	int gsoffset = 0;


	/////////////////////////////////////////////
	void SFbackgroundChanged()
	{
		sketchdisplay.sketchgraphicspanel.tsketch.SetBackground(backgrounddir, sfbackground.getText());
		sketchdisplay.sketchgraphicspanel.backgroundimg.bMaxBackImage = true;
		sketchdisplay.sketchgraphicspanel.backgroundimg.SetImageF(sketchdisplay.sketchgraphicspanel.tsketch.fbackgimg, getToolkit());
		sketchdisplay.sketchgraphicspanel.repaint();
	}


	/////////////////////////////////////////////
	class radbuttpress implements ActionListener
	{
		int lgsoffset;
		radbuttpress(int llgsoffset)
		{
			lgsoffset = llgsoffset;
		}
		public void actionPerformed(ActionEvent event)
		{
			gsoffset = lgsoffset;
			sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
		}
	};

	/////////////////////////////////////////////
	void SetGridOrigin(boolean btocurrent)
	{
		if (btocurrent)
		{
			if (sketchdisplay.sketchgraphicspanel.currgenpath != null)
			{
				Point2D pn = sketchdisplay.sketchgraphicspanel.currgenpath.pnstart.pn;
				sketchdisplay.sketchgraphicspanel.sketchgrid.txorig = (float)pn.getX();
				sketchdisplay.sketchgraphicspanel.sketchgrid.tyorig = (float)pn.getY();
			}
		}
		else
		{
			sketchdisplay.sketchgraphicspanel.sketchgrid.txorig = sketchdisplay.sketchgraphicspanel.sketchgrid.xorig;
			sketchdisplay.sketchgraphicspanel.sketchgrid.tyorig = sketchdisplay.sketchgraphicspanel.sketchgrid.yorig;
		}

		sketchdisplay.sketchgraphicspanel.RedoBackgroundView();
	}

	/////////////////////////////////////////////
	SketchBackgroundPanel(SketchDisplay lsketchdisplay)
	{
		sketchdisplay = lsketchdisplay;

		// background panel
		sfbackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { SFbackgroundChanged(); } } );


		// grid spacing controls
		JPanel pangridspacingc = new JPanel(new GridLayout(1, 0));
		ButtonGroup buttgp = new ButtonGroup();
		for (int i = -1; i <= 1; i++)
		{
			JRadioButton radbutt = new JRadioButton("", (i == 0));
			radbutt.addActionListener(new radbuttpress(i));
			buttgp.add(radbutt);
			pangridspacingc.add(radbutt);
		}
		tfgridspacing.setEditable(false);
		pangridspacingc.add(tfgridspacing);

		// impossible to get checkboxmenu items to reflect at these places (which would have been ideal)
		JButton butthidebackground = new JButton("Hide Background");
		butthidebackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowBackground.setSelected(false);  sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );
		JButton buttshowbackground = new JButton("Show Background");
		buttshowbackground.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowBackground.setSelected(true); sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );
		JButton butthidegrid = new JButton("Hide Grid");
		butthidegrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowGrid.setSelected(false);  sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );
		JButton buttshowgrid = new JButton("Show Grid");
		buttshowgrid.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { sketchdisplay.miShowGrid.setSelected(true);  sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); } } );



		setLayout(new GridLayout(0, 2));
		add(sfbackground);
		add(new JButton(sketchdisplay.acaMoveBackground));
		add(butthidebackground);
		add(buttshowbackground);
		add(new JLabel("Grid spacing"));
		add(pangridspacingc);
		add(new JButton(sketchdisplay.acvSetGridOrig));
		add(new JButton(sketchdisplay.acvResetGridOrig));
		add(butthidegrid);
		add(buttshowgrid);
	}
};


