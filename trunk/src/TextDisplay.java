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
// SketchDisplay
//
//


// this class contains the whole outer set of options and buttons
class TextDisplay extends JFrame
{
	JTextArea textarea; 

	/////////////////////////////////////////////
	// inactivate case 
	class TextHide extends WindowAdapter implements ActionListener	
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
	TextDisplay() 
	{
		super("Text Display"); 

		textarea = new JTextArea(); 
		textarea.setEditable(false); 
		JScrollPane scrollpane = new JScrollPane(textarea); 

		// final set up of display
		getContentPane().setLayout(new BorderLayout()); 
		getContentPane().add("Center", scrollpane); 

		addWindowListener(new TextHide());

		pack(); 
		setSize(800, 600);
	}


	/////////////////////////////////////////////
	void ActivateTextDisplay(OneTunnel activetunnel, int activetxt)  
	{
		setTitle(activetunnel.fullname); 

		try
		{

		if (activetxt == 1)
		{
			LineOutputStream los = new LineOutputStream(null);  
			los.WriteLine("// This is a dump of the interpreted data as it is encoded in the database."); 
			los.WriteLine("// The legs file is not read in; data is pulled in from the svx form."); 
			los.WriteLine(""); 
			activetunnel.WriteXML(los); 
			textarea.setText(los.sb.toString()); 
		}

		else if (activetxt == 2)
		{
			LineOutputStream los = new LineOutputStream(null);  
			los.WriteLine("// This is a list of the exports from this level."); 
			los.WriteLine(""); 
			for (int i = 0; i < activetunnel.vexports.size(); i++)  
				((OneExport)activetunnel.vexports.elementAt(i)).WriteXML(los); 
			textarea.setText(los.sb.toString()); 
		}

		else // 0 case 
			textarea.setText(activetunnel.TextData.toString()); 
		}
		catch (IOException ie) 
		{
			TN.emitWarning(ie.toString()); 		
		}; 

		textarea.setCaretPosition(0); 

		toFront();
		setVisible(true);
	}
}


