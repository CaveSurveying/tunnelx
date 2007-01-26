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

import java.awt.Color;

////////////////////////////////////////////////////////////////////////////////
class SSymbolBase
{
// these factors should be set by the symbol itself (maybe by name).
	boolean bScaleable = false; // new default
	float fpicscale = 1.0F; // if scalable false we may scale by a fixed amount
	float faxisscale = 1.0F; // if scalable false we may scale by a fixed amount
	float faxisscaleperp = 1.0F; // further scattering in the perpendicular direction, so there is somewhere to pullback to

	boolean bRotateable = true; // will be false for stal symbols.

	// positioning parameters
	boolean bBuildSymbolLatticeAcrossArea;
		boolean bSymbolLatticeAcrossAreaPhased;
	boolean bBuildSymbolSpreadAlongLine;
	boolean bSymbolLayoutOrdered; 
	boolean bOrientClosestAlongLine; 
	boolean bOrientClosestPerpLine; 

	double posdeviationprop = 1.0F; // proportional distance we can move the symbol

	boolean bMoveable = false; // symbol can be elsewhere other than where it is put (and so multiplicity is valid).
	int iLattice = 0; // non-zero means lattice, type 1 is lattice displace phased by endpoint, type 2 has position always on a grid (usually not rotatable).
	boolean bPullback = false; // pulling back till interference.
	boolean bPushout = false; // pushing away till no interference.

	boolean bShrinkby2 = false; // add in a size change in boulder fields.

	boolean bAllowedOutsideArea = false;
	boolean bTrimByArea = true;
	boolean bSymbolinterferencedoesntmatter = false;
	boolean bFilledType = false; 

	double posangledeviation = 0.1F; // in radians.  -1.0 means anywhere.

	// over-rides everything else if non null
	Color symbolareafillcolour = null; // filling of the area if symbolareafill defined (eg water)

	double pulltolerance = 0.05; // 5cm.

	// the name of the base symbol
	String gsymname;
	OneSketch gsym = null; // this is selected by name.

	int nmultiplicity = 0;
	int maxplaceindex = 800;  // or -1 for unlimited setting.
};

