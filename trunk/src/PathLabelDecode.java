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

import java.io.StringReader;
import java.util.Vector;

// all this nonsens with static classes is horrible.
// don't know the best way for reuse of objects otherwise.
// while keeping PathLabelDecode small so it can be included in every path
////////////////////////////////////////////////////////////////////////////////
class PathLabelXMLparse extends TunnelXMLparsebase
{
	PathLabelDecode pld;
	StringBuffer sbtxt = new StringBuffer();

	/////////////////////////////////////////////
	boolean ParseLabel(PathLabelDecode lpld, String lab)
	{
		pld = lpld;
		return (new TunnelXML()).ParseString(this, lab);
	}

	/////////////////////////////////////////////
	public void startElementAttributesHandled(String name, boolean binlineclose)
	{
		if (name.equals(TNXML.sLRSYMBOL))
		{
			String symbname = SeStack(TNXML.sLRSYMBOL_NAME);
			pld.vlabsymb.addElement(symbname);
		}
		else if (name.equals(TNXML.sTAIL) || name.equals(TNXML.sHEAD))
			sbtxt.setLength(0);
		else if (name.equals(TNXML.sLTEXT))
		{
			sbtxt.setLength(0);
			String lstextstyle = SeStack(TNXML.sLTEXTSTYLE);
			pld.ifontcode = 0;
			for (pld.ifontcode = 0; pld.ifontcode < TN.labstylenames.length; pld.ifontcode++)
				if (lstextstyle.equals(TN.labstylenames[pld.ifontcode]))
					break;
			if (pld.ifontcode == TN.labstylenames.length)
			{
				TN.emitWarning("unrecognized label style " + lstextstyle);
				pld.ifontcode = 0;
			}
		}
		else if (name.equals("br"))
			sbtxt.append('\n');
	}

	/////////////////////////////////////////////
	public void characters(String pstr)
	{
		for (int i = 0; i < pstr.length(); i++)
		{
			char ch = pstr.charAt(i);
			if ((ch == '|') || (ch == '^'))
				sbtxt.append('.');
			else
				sbtxt.append(ch);
		}
	}

	/////////////////////////////////////////////
	public void endElementAttributesHandled(String name)
	{
		if (name.equals(TNXML.sHEAD))
			pld.head = sbtxt.toString();
		else if (name.equals(TNXML.sTAIL))
			pld.tail = sbtxt.toString();
		else if (name.equals(TNXML.sLTEXT))
			pld.drawlab = sbtxt.toString();
	}
};


////////////////////////////////////////////////////////////////////////////////
class PathLabelDecode
{
	// TNXML.sLRSYMBOL_NAME
	static PathLabelXMLparse plxp = new PathLabelXMLparse();
	Vector vlabsymb = new Vector();
	String lab;

	// these could be replaced by some sort of attributedcharacter string.
	int ifontcode;
	String drawlab;

	// values used by a centreline
	String head;
	String tail;

	boolean DecodeLabel(String llab)
	{
		vlabsymb.removeAllElements();
		lab = llab;

		// default case of no xml commands
		if (lab.indexOf('<') == -1)
		{
			int ifontcode = 0;
			drawlab = lab;
			return true;
		}
		return plxp.ParseLabel(this, lab);
	}
};


