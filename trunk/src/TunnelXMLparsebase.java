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
// FoUndation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

class TunnelXMLparsebase
{
	String[] attnamestack = new String[50];
	String[] attvalstack = new String[50];

	String[] elemstack = new String[20];
	int[] iposstack = new int[20];
	int istack = 0;


	/////////////////////////////////////////////
	String SeStack(String name)
	{
		for (int i = (istack != 0 ? iposstack[istack - 1] : 0) - 1; i >= 0; i--)
		{
			if (attnamestack[i].equals(name))
				return attvalstack[i];
		}
		return null;
	}

	/////////////////////////////////////////////
	String SeStack(String name, String defalt)
	{
		String res = SeStack(name);
		return (res == null ? defalt : res);
	}

	/////////////////////////////////////////////
	boolean ElStack(String name)
	{
		for (int i = istack - 1; i >= 0; i--)
		{
			if (elemstack[i].equals(name))
				return true;
		}
		return false;
	}

	/////////////////////////////////////////////
	void StackDump()
	{
		for (int i = istack - 1; i >= 0; i--)
		{
			System.out.print(elemstack[i] + ":");
			for (int j = (i != 0 ? iposstack[i - 1] : 0); j < iposstack[i]; j++)
				System.out.print("  " + attnamestack[j] + "=" + attvalstack[j]);
			System.out.println("");
		}
 	}

	/////////////////////////////////////////////
	public void startElementAttributesHandled(String name, boolean binlineclose)
	{
	}

	/////////////////////////////////////////////
	public void characters(String pstr)
	{
	}

	/////////////////////////////////////////////
	public void endElementAttributesHandled(String name)
	{
	}

	/////////////////////////////////////////////
	void SetUpBase()
	{
		istack = 0;
	}
};


