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

import java.awt.Dimension; 
import javax.swing.JPanel; 

import javax.swing.JTextField; 
import javax.swing.JComboBox; 

import javax.swing.JSplitPane; 
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 

import java.awt.Graphics; 
import java.awt.BorderLayout; 
import java.awt.GridLayout; 
import javax.swing.BoxLayout; 

import java.util.Vector; 
import java.awt.FileDialog;

import java.awt.Image; 

import java.io.IOException; 
import java.io.File;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 

import java.awt.event.MouseEvent; 
import java.awt.event.MouseAdapter; 

import javax.swing.event.DocumentListener; 
import javax.swing.event.DocumentEvent; 

//
//
// ImgDisplay
//
//

class ImgPanel extends JPanel
{
	Dimension csize = new Dimension(200, 200); 
	ImageWarp imgwarp = new ImageWarp(csize, this); 

	/////////////////////////////////////////////
	ImgPanel()
	{
		addMouseListener(imgwarp); 
		addMouseMotionListener(imgwarp); 
	}

	/////////////////////////////////////////////
	public void paintComponent(Graphics g) 
	{
		// test if resize has happened because we are rebuffering things
		if ((getSize().height != csize.height) || (getSize().width != csize.width))
		{
			csize.width = getSize().width; 
			csize.height = getSize().height; 
		}
		imgwarp.DoBackground(g, true, 0, 0, 1.0F); 
	}
};


// this class contains the whole outer set of options and buttons
class ImgDisplay extends JFrame
{
	Dimension csize = new Dimension(200, 200); 
	ImgPanel imgpanel = new ImgPanel(); 

	/////////////////////////////////////////////
	// inactivate case 
	class ImgHide extends WindowAdapter implements ActionListener	
	{
		void CloseWindow()  
		{
			// if editable then we would save the text here.  
			setVisible(false); 
		}

		public void windowClosing(WindowEvent e)
		{
			CloseWindow(); 
		}

		public void actionPerformed(ActionEvent e)
		{
			CloseWindow(); 
		}
	}


	/////////////////////////////////////////////
	// set up the arrays
	ImgDisplay() 
	{
		super("Img Display"); 

		// final set up of display
		getContentPane().setLayout(new BorderLayout()); 
		getContentPane().add("Center", imgpanel); 

		addWindowListener(new ImgHide()); 

		pack(); 
		setSize(800, 600);
	}


	/////////////////////////////////////////////
	void ActivateImgDisplay(File activeimg)  
	{
		setTitle(activeimg.getName()); 
		imgpanel.imgwarp.SetImageF(activeimg, getToolkit()); 

		toFront(); 
		show(); 
	}
}


