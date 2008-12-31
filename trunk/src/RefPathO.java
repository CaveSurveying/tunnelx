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


/////////////////////////////////////////////
class RefPathO
{
	OnePath op;
	boolean bFore;

	RefPathO()
		{;};
	RefPathO(OnePath lop, boolean lbFore)
	{
		op = lop;
		bFore = lbFore;
	}
	void ccopy(RefPathO rpo)
	{
		op = rpo.op;
		bFore = rpo.bFore;
	}
	RefPathO(RefPathO rpo)
	{
		ccopy(rpo); 
	}

	boolean cequals(RefPathO rpo)
	{
		return ((op == rpo.op) && (bFore == rpo.bFore));
	}

	OneSArea GetCrossArea()
	{
		return (bFore ? op.kaleft : op.karight);
	}

	OnePathNode ToNode() 
	{
		return (bFore ? op.pnend : op.pnstart);
	}
	OnePathNode FromNode()
	{
		return (bFore ? op.pnstart : op.pnend);
	}

	boolean AdvanceRoundToNode(RefPathO rpmatch) // cycles around the ToNode
	{
		assert ToNode() == rpmatch.ToNode();
		if (bFore)
		{
			bFore = op.bapfrfore;
			op = op.apforeright;
		}
		else
		{
			bFore = op.baptlfore;
			op = op.aptailleft;
		}
		assert ToNode() == rpmatch.ToNode();
		return cequals(rpmatch);
	}
};



