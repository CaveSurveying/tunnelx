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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.Insets;

import java.util.Vector;

//
//
// SymbolsDisplay
//
//



/////////////////////////////////////////////
class SymbolsDisplay extends JPanel
{
	OneTunnel vgsymbols;
	SketchDisplay sketchdisplay;

	JPanel pansymb = new JPanel(new GridLayout(0, 3));

	JButton jbclear = new JButton("Clear");
	JButton jbcancel = new JButton("Cancel");

	JTextField jbsymlist = new JTextField("--");

	/////////////////////////////////////////////
	SymbolsDisplay(OneTunnel lvgsymbols, SketchDisplay lsketchdisplay)
	{
		super(new BorderLayout());

		vgsymbols = lvgsymbols;
		sketchdisplay = lsketchdisplay;

		add("North", new JLabel("Symbols", JLabel.CENTER));
		add("Center", pansymb);

		jbsymlist.setEditable(false);

		JPanel psouth = new JPanel(new BorderLayout());
		JPanel psouthbutts = new JPanel();
		psouthbutts.add(jbclear);
		psouthbutts.add(jbcancel);
        psouth.add("North", jbsymlist);
        psouth.add("South", psouthbutts);

		add("South", psouth);
	}


	/////////////////////////////////////////////
	void UpdateSymbList(Vector vlabsymb)
	{
		if (vlabsymb.isEmpty())
		{
        	jbsymlist.setText("");
        	return;
		}

		StringBuffer sb = new StringBuffer();
		sb.append((String)vlabsymb.elementAt(0));
		for (int i = 1; i < vlabsymb.size(); i++)
		{
			sb.append("+");
			sb.append((String)vlabsymb.elementAt(i));
		}

		jbsymlist.setText(sb.toString());
	}

	/////////////////////////////////////////////
	// the default given is top=2, left=14, etc
	Insets defsymbutinsets = new Insets(2, 3, 2, 3);
	/////////////////////////////////////////////
	// this is chaos.  better if all signals came through this class, or the one above
	void AddSymbolsButtons(SketchLineStyle sketchlinestyle)
	{
		// go through the aut-symbols and find matches in the real symbols
		for (int i = 0; i < vgsymbols.vautsymbols.size(); i++)
		{
			// first element is the string name of this aut-symbol
			AutSymbolAc autsymbol = (AutSymbolAc)vgsymbols.vautsymbols.elementAt(i);

			autsymbol.SetUp(vgsymbols, sketchlinestyle);
			JButton symbolbutton = new JButton(autsymbol);
			symbolbutton.setMargin(defsymbutinsets); 
			//System.out.println(symbolbutton.getMargin().toString()); 
			pansymb.add(symbolbutton);
		}
	}
};


