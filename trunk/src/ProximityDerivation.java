////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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

import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;

//
//
//
//
//
class cmpr implements Comparator
{
	public int compare(Object o1, Object o2)
        {
                float ll = ((OnePath)o1).linelength - ((OnePath)o2).linelength;
                return (ll < 0.0 ? -1 : (ll > 0.0 ? 1 : 0));
        }
}

/////////////////////////////////////////////
class nodepara
{
	// the paths in order of length
	Vector nvpaths = new Vector();
	Object[] arplengs;

	int nc = 0;
        float dist = -1.0F;
};

/////////////////////////////////////////////
// weights from centrelines
/////////////////////////////////////////////
class ProximityDerivation
{
	Vector vnodes;
	Vector vpaths;

	Vector vnodespaths;
        int lnc = 1;

	/////////////////////////////////////////////
	ProximityDerivation(OneSketch os)
        {
		vnodes = os.vnodes;
                vpaths = os.vpaths;

		// make the array parallel to the nodes
		vnodespaths = new Vector();
                for (int i = 0; i < vnodes.size(); i++)
			vnodespaths.addElement(new nodepara());

		// insert for the arrays of vectors
                for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath op = (OnePath)vpaths.elementAt(i);
                        int nis = vnodes.indexOf(op.pnstart);
                        ((nodepara)vnodespaths.elementAt(nis)).nvpaths.addElement(op);
                        int nie = vnodes.indexOf(op.pnend);
                        ((nodepara)vnodespaths.elementAt(nie)).nvpaths.addElement(op);
		}


                // make the lists and sort by shortness
                for (int i = 0; i < vnodes.size(); i++)
                {
			nodepara np = (nodepara)vnodespaths.elementAt(i);
                        np.arplengs = np.nvpaths.toArray();
                        np.nvpaths = null;
                        Arrays.sort(np.arplengs, cmpr);
		}
        }

	/////////////////////////////////////////////
	ProximityDerivation(OneSketch os)
        {

        pnstationlabel
};


