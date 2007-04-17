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
import java.util.SortedMap;
import java.util.TreeMap;

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

	JButton jbclear = new JButton("Clear");
	JButton jbcancel = new JButton("Cancel");

	JTextField jbsymlist = new JTextField("--");

	SortedMap<String, AutSymbolAc> autsymbsmap = new TreeMap<String, AutSymbolAc>(); 

	SubsetAttr Dsubsetattr = null; // saved and verified to make sure SelEnableButtons has been updated

	/////////////////////////////////////////////
	SymbolsDisplay(OneTunnel lvgsymbols, SketchDisplay lsketchdisplay)
	{
		super(new BorderLayout());

		vgsymbols = lvgsymbols;
		sketchdisplay = lsketchdisplay;

		add(new JLabel("Symbols", JLabel.CENTER), BorderLayout.NORTH);
		add(pansymb, BorderLayout.CENTER);

		jbsymlist.setEditable(false);

		JPanel psouth = new JPanel(new BorderLayout());
		JPanel psouthbutts = new JPanel();
		psouthbutts.add(jbclear);
		psouthbutts.add(jbcancel);
        psouth.add(jbsymlist, BorderLayout.NORTH);
        psouth.add(psouthbutts, BorderLayout.SOUTH);

		add(psouth, BorderLayout.SOUTH);
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
	void SelEnableButtons(SubsetAttr subsetattr)
	{
		Dsubsetattr = subsetattr; // for debug verification
		for (AutSymbolAc autsymbol : autsymbsmap.values())
			autsymbol.setEnabled((subsetattr != null) && subsetattr.subautsymbolsmap.containsKey(autsymbol.name)); 
	}

	
	/////////////////////////////////////////////
	void ReloadSymbolsButtons(SubsetAttrStyle sascurrent)
	{
		autsymbsmap.clear(); 
		for (SubsetAttr sa : sascurrent.msubsets.values())
		{
			for (SymbolStyleAttr ssa : sa.subautsymbolsmap.values())
			{
				if (!autsymbsmap.containsKey(ssa.symbolname) && (ssa.iautsymboverwrite != 0))
					autsymbsmap.put(ssa.symbolname, new AutSymbolAc(ssa.symbolname, ssa.autsymbdesc, (ssa.iautsymboverwrite == 1), this)); 
			}
		}					
					
		pansymb.removeAll(); 
		for (AutSymbolAc autsymbol : autsymbsmap.values())
		{		
			JButton symbolbutton = new JButton(autsymbol);
			symbolbutton.setMargin(defsymbutinsets);
			autsymbol.tsymbutt = symbolbutton; 
			pansymb.add(symbolbutton);
		}
	}
};


/*
The way to do scalebars is as follows

Simple:

%10/1%%whiterect%
;%10/1%%blackrect%
;%10/1%%whiterect%
;%10/1%%blackrect%
;%10/1%%whiterect%
%10/1%0m
;%10/1%10m 
;%10/1%20m
;%10/1%30m
;%10/1%40m
;%10/1%50m



Complex:

%0/1.0000%%v0/1%      
;%50/1%%v1/1%%whiterect%
%1/1%%v0.5/1%
;%1/1%%v0.5/1%%blackrect%
;%1/1%
;%1/1%%v0.5/1%%blackrect%
;%1/1%
;%5/1%%v0.5/1%%blackrect%
;%5/1%%v1/1% 
;%5/1%%v0.5/1%%blackrect%
;%5/1%%v1/1% 
;%5/1%%v0.5/1%%blackrect%
;%5/1%%v1/1% 
;%5/1%%v0.5/1%%blackrect%
;%5/1%%v1/1% 
;%5/1%%v0.5/1%%blackrect%
%1/1%%v0.5/1%%blackrect%
;%1/1%
;%1/1%%v0.5/1%%blackrect%
;%1/1%
;%1/1%%v0.5/1%%blackrect%
;%5/1% 
;%5 /1%%v0.5/1%%blackrect%
;%5/1% 
;%5 /1%%v0.5/1%%blackrect%
;%5/1% 
;%5 /1%%v0.5/1%%blackrect%
;%5/1% 
;%5 /1%%v0.5/1%%blackrect%
%4.5/1%0m
;%5/1%5m 
;%10/1%10m 
;%10/1%20m
;%10/1%30m
;%10/1%40m
;%10/1%50m

*/


