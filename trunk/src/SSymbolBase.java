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
// in the symbol name

// -M means it can move
// -S means it is scaleable
// -F0 means the orientation is ignored.
// -F9 means orientation is random
// -F1 means orientation is slightly changeable.
// -L means it's to be put at points of a lattice.

// -D2 means it can be shrunk by a random factor less than two.

// -PB means we pull-back till it interferes with an edge.  (if edge or other symbols.  think stal.).
// -PO means we push-out till it stopps interfering.

// [-I means interference with other symbols okay - not implemented yet]


////////////////////////////////////////////////////////////////////////////////
class SSymbolBase
{
// these factors should be set by the symbol itself (maybe by name).
	boolean bScaleable = false; // new default
	float fpicscale = 1.0F; // if scalable false we may scale by a fixed amount

	boolean bRotateable = true; // will be false for stal symbols.
	boolean bMoveable = false; // symbol can be elsewhere other than where it is put (and so multiplicity is valid).
	int iLattice = 0; // non-zer0 means lattice type 1 is from position, type 2 has position always on a grid (usually not rotatable).

	boolean bShrinkby2 = false; // add in a size change in boulder fields.

	boolean bPullback = false; // pulling back till interference.
	boolean bPushout = false; // pushing away till no interference.

	boolean bAllowedOutsideArea = false;
	boolean bTrimByArea = true;
	boolean bSymbolinterferencedoesntmatter = false; 

	double posdeviationprop = 1.0F; // proportional distance we can move the symbol
	double posangledeviation = 0.1F; // in radians.  10.0 means anywhere.

	// the name of the base symbol
	String gsymname;
	OneSketch gsym = null; // this is selected by name.

	int nmultiplicity = 0;

	void BSpecSymbol(SSymbolBase ssb)
	{
		bScaleable = ssb.bScaleable;
		fpicscale = ssb.fpicscale;
		bRotateable = ssb.bRotateable;
		bMoveable = ssb.bMoveable;
		iLattice = ssb.iLattice;
		bShrinkby2 = ssb.bShrinkby2;
		bPullback = ssb.bPullback;
		bPushout = ssb.bPushout;
		bAllowedOutsideArea = ssb.bAllowedOutsideArea;
		bTrimByArea = ssb.bTrimByArea;
		bSymbolinterferencedoesntmatter = ssb.bSymbolinterferencedoesntmatter; 
		posdeviationprop = ssb.posdeviationprop;
		posangledeviation = ssb.posangledeviation;
		gsymname = ssb.gsymname;
		gsym = ssb.gsym;
		nmultiplicity = ssb.nmultiplicity;
	}
};

