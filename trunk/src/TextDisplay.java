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
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;

import java.awt.BorderLayout;
import java.awt.Color;

import java.io.IOException;
import java.io.BufferedReader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import javax.swing.text.JTextComponent;
import java.awt.Point;
import javax.swing.plaf.basic.BasicTextAreaUI;
//
//
// TextDisplay
//
//

/////////////////////////////////////////////
class SurvexTextAreaUI extends BasicTextAreaUI
{
	public String getToolTipText(JTextComponent t, Point pt)
	{
System.out.println("TTTT " + pt);
return ((BasicTextAreaUI)this).getToolTipText(t, pt);
	}
}

	/////////////////////////////////////////////
// this class contains the whole outer set of options and buttons
class TextDisplay extends JFrame
{
	JTextArea textarea;
//	JTextPane textarea;
	SurvexTextAreaUI staui = new SurvexTextAreaUI();

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

		//textarea = new JTextPane();
		textarea = new JTextArea();
//		textarea.setEditable(false);
		JScrollPane scrollpane = new JScrollPane(textarea);
//		textarea.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
//		textarea.setEditorKit(new javax.swing.text.rtf.RTFEditorKit());

		// final set up of display
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scrollpane, BorderLayout.CENTER);

		addWindowListener(new TextHide());

		pack();
		setSize(800, 600);
	}


	/////////////////////////////////////////////
	void ActivateTextDisplay(OneTunnel activetunnel, int activetxt, int activesketchindex)
	{
		setTitle(activetunnel.fullname);

		try
		{
		if (activetxt == FileAbstraction.FA_FILE_SVX)
		{
textarea.setUI(staui);
System.out.println("TUI: " + textarea.getUI());
textarea.setSelectedTextColor(Color.red);
			textarea.setText(activetunnel.TextData.toString());
//			textarea.setText("<h1>Hi there</h1>");
		}
		else if (activetxt == FileAbstraction.FA_FILE_XML_FONTCOLOURS)
		{
 			BufferedReader br = activetunnel.tfontcolours.get(activesketchindex).GetBufferedReader();

			LineOutputStream los = new LineOutputStream(null);
			los.WriteLine("// This is a fontcolours");
			los.WriteLine("");
			while (true)
			{
				String sline = br.readLine(); 
				if (sline == null)
					break; 
				los.WriteLine(sline); 
			}
			textarea.setText(los.sb.toString());
		}

		else
			assert false; 
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


