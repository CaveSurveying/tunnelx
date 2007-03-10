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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

//
//
// SymbolsDisplay
//
//


/////////////////////////////////////////////
// this thing should be disentangleable.
class AutSymbolAc extends AbstractAction implements Comparable<AutSymbolAc>
{
	SymbolsDisplay symbolsdisplay;
	String name;
	String shdesc;
	boolean bOverwrite;
	JButton tsymbutt = null; 
	
	/////////////////////////////////////////////
	public AutSymbolAc(String lname, String lshdesc, boolean lbOverwrite, SymbolsDisplay lsymbolsdisplay)
	{
		super(lname);
		shdesc = lshdesc;
		bOverwrite = lbOverwrite;
		name = lname;
		symbolsdisplay = lsymbolsdisplay; 

		putValue(SHORT_DESCRIPTION, shdesc);
	}

	/////////////////////////////////////////////
	public int compareTo(AutSymbolAc asa)
	{
		return name.compareTo(asa.name); 
	}

	/////////////////////////////////////////////
	public void actionPerformed(ActionEvent e)
	{
		symbolsdisplay.sketchdisplay.sketchlinestyle.LSpecSymbol(bOverwrite, name);
	}
};



/////////////////////////////////////////////
class SymbolsDisplay extends JPanel
{
	OneTunnel vgsymbols;
	SketchDisplay sketchdisplay;

	JPanel pansymb = new JPanel(new GridLayout(0, 3));
	List<AutSymbolAc> autsymbollist = new ArrayList<AutSymbolAc>(); 

	JButton jbclear = new JButton("Clear");
	JButton jbcancel = new JButton("Cancel");

	JTextField jbsymlist = new JTextField("--");

	Set<String> autsymbs = new HashSet<String>(); 
	SubsetAttr Dsubsetattr = null; // saved and verified to make sure SelEnableButtons has been updated

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
	void UpdateSymbList(List<String> vlabsymb, SubsetAttr lDsubsetattr)
	{
		assert Dsubsetattr == lDsubsetattr; // used to make sure SelEnableButtons is up to date, in case we make new symbols functions
		if (vlabsymb.isEmpty())
		{
        	jbsymlist.setText("");
        	return;
		}

		StringBuffer sb = new StringBuffer();
		for (String rname : vlabsymb)
		{
			if (sb.length() != 0)
				sb.append("+");
			sb.append(rname);
		}

		jbsymlist.setText(sb.toString());
	}

	/////////////////////////////////////////////
	Insets defsymbutinsets = new Insets(2, 3, 2, 3);

	/////////////////////////////////////////////
	void MakeSymbolButtonsInPanel()
	{
		pansymb.removeAll(); 
		Collections.sort(autsymbollist); 
		for (AutSymbolAc autsymbol : autsymbollist)
		{
			JButton symbolbutton = new JButton(autsymbol);
			symbolbutton.setMargin(defsymbutinsets);
			autsymbol.tsymbutt = symbolbutton; 
			pansymb.add(symbolbutton);
		}
	}

	/////////////////////////////////////////////
	void SelEnableButtons(SubsetAttr subsetattr)
	{
		Dsubsetattr = subsetattr; // for debug verification
		for (AutSymbolAc autsymbol : autsymbollist)
			autsymbol.setEnabled((subsetattr != null) && (subsetattr.FindSymbolSpec(autsymbol.name, 0) != null)); 
	}

	/////////////////////////////////////////////
	void AddSymbolButton(String autsymbdname, String autsymbdesc, boolean lbOverwrite)
	{
		if (autsymbs.contains(autsymbdname))
			return; 
		autsymbs.add(autsymbdname);
		AutSymbolAc autsymbol = new AutSymbolAc(autsymbdname, autsymbdesc, lbOverwrite, this);
		autsymbollist.add(autsymbol); 
	}
};


