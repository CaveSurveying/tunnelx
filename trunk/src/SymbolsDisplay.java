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

import javax.swing.SwingConstants; 
import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JToggleButton; 
import javax.swing.JPanel; 
import javax.swing.JCheckBox; 
import javax.swing.JButton; 
import javax.swing.JTextField; 
import javax.swing.JComboBox; 

import javax.swing.JSplitPane;
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 

import javax.swing.Icon; 
import java.awt.Color; 
import java.awt.Dimension; 

import java.awt.Component;

import java.awt.Graphics; 
import java.awt.Graphics2D;
import java.awt.BorderLayout; 
import java.awt.GridLayout;
import javax.swing.BoxLayout; 

import java.util.Vector; 
import java.awt.FileDialog;

import java.awt.Graphics2D; 
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon; 
import javax.swing.Icon; 
import java.awt.Image; 

import java.io.IOException; 
import java.io.File;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D; 

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import javax.swing.JButton;
import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

//
//
// SymbolsDisplay
//
//



//
class SymbolsDisplay extends JPanel
{
	OneTunnel vgsymbols;
	SketchDisplay sketchdisplay;
	Dimension prefsize = new Dimension(100, 100);

	/////////////////////////////////////////////
	SymbolsDisplay(OneTunnel lvgsymbols, SketchDisplay lsketchdisplay)
	{
		super(new GridLayout(0, 2));

		vgsymbols = lvgsymbols;
		sketchdisplay = lsketchdisplay;
	}

	/////////////////////////////////////////////
	void AddSymbolsButtons(SketchLineStyle sketchlinestyle)
	{
		// go through the aut-symbols and find matches in the real symbols
		for (int i = 0; i < vgsymbols.vautsymbols.size(); i++)
		{
			// first element is the string name of this aut-symbol
			AutSymbolAc autsymbol = (AutSymbolAc)vgsymbols.vautsymbols.elementAt(i);
			autsymbol.SetUp(vgsymbols, sketchlinestyle.sketchdisplay.sketchgraphicspanel);
			JButton symbolbutton = new JButton(autsymbol);
			add(symbolbutton);
		}
	}

};


