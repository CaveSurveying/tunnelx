////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2009  Julian Todd.
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

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Collections;
import java.util.Collection;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/////////////////////////////////////////////
/////////////////////////////////////////////
class PocketTopoLoader
{
	/////////////////////////////////////////////
    static boolean IsPocketTopo(String sfilehead)
    {
        if (sfilehead.indexOf("FIX") == 0)
            return true; 
        if (sfilehead.indexOf("TRIP") == 0)
            return true; 
        return false; 
    }

	/////////////////////////////////////////////
	public String LoadPockettopo(FileAbstraction loadfile)
	{
        int[] splaycounters = new int[512]; 
        StringBuilder sb = new StringBuilder();
        StringBuilder sbsplay = new StringBuilder();
        
		try
		{ 
		LineInputStream lis = new LineInputStream(loadfile.GetInputStream(), loadfile, null, null);
        //FIX
        //1.0	0.000	0.000	0.000

        //TRIP
        //DATE 2009-04-20 
        //DECLINATION     0.00
        //DATA

		lis.FetchNextLine(); 
        if (lis.GetLine().equals("FIX"))
		{
            lis.FetchNextLine(); 
            //1.0	0.000	0.000	0.000
            assert lis.w[0].startsWith("1."); 
            sb.append("*fix\t"); 
            sb.append(lis.w[0].substring(2)); 
            sb.append("\t"); 
            sb.append(lis.w[1]); 
            sb.append("\t"); 
            sb.append(lis.w[2]); 
            sb.append("\t"); 
            sb.append(lis.w[3]); 
            sb.append(TN.nl);

    		lis.FetchNextLine(); 
        }
        assert lis.GetLine().equals("TRIP"); 

		lis.FetchNextLine(); 
        assert lis.w[0].equals("DATE"); 
        sb.append("*date\t"); 
        sb.append(lis.w[1].replace("-", ".")); 
        sb.append(TN.nl);

		lis.FetchNextLine(); 
        assert lis.w[0].equals("DECLINATION"); 

		lis.FetchNextLine(); 
        assert lis.GetLine().equals("DATA"); 
		while (lis.FetchNextLine())
        {
System.out.println(lis.GetLine()); 
            if (lis.iwc == 0)
                break; 
            assert !lis.w[0].equals("PLAN"); 

            // 1.19	1.18	351.68	-26.98	6.404	>
            assert lis.w[0].startsWith("1."); 
            if (lis.iwc == 6)
            {
                assert lis.w[1].startsWith("1."); 
                sb.append(lis.w[0].substring(2)); 
                sb.append("\t"); 
                sb.append(lis.w[1].substring(2)); 
                sb.append("\t"); 
                sb.append(lis.w[4]); 
                sb.append("\t"); 
                sb.append(lis.w[2]); 
                sb.append("\t"); 
                sb.append(lis.w[3]); 

                // the < arrows are rare and seem to correspond to legs that don't have continuations
                assert lis.w[5].equals(">") || lis.w[5].equals("<"); 
                sb.append(TN.nl);
            }
            else if (lis.iwc == 5)
            {
                String splaystation = lis.w[0].substring(2); 
                int isplaystation = Integer.valueOf(splaystation); 
                sbsplay.append(splaystation); 
                sbsplay.append("\t"); 
                splaycounters[isplaystation]++; 
                sbsplay.append(splaystation); 
                sbsplay.append("_"); 
                sbsplay.append(splaycounters[isplaystation]); 
                sbsplay.append("\t"); 
                sbsplay.append(lis.w[3]); 
                sbsplay.append("\t"); 
                sbsplay.append(lis.w[1]); 
                sbsplay.append("\t"); 
                sbsplay.append(lis.w[2]); 
                assert lis.w[4].equals(">"); 
                sbsplay.append(TN.nl);
            }
            else 
                assert false; // unknown string format
        }
        
        sb.append(TN.nl); 
        sb.append("*flags splay"); 
        sb.append(TN.nl); 
        sb.append(sbsplay.toString()); 

        lis.inputstream.close(); 
        }
		catch (IOException e)
		{ TN.emitError(e.toString()); };
        return sb.toString();
    }
}

