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
    int[] splaycounters = new int[512]; 
    StringBuilder sbsvx = new StringBuilder();
    StringBuilder sbsvxsplay = new StringBuilder();

    List<OnePath> vpathsplan = new ArrayList<OnePath>(); 

	/////////////////////////////////////////////
    //FIX
    //1.0	0.000	0.000	0.000
    void LoadFix(LineInputStream lis) throws IOException
    {
        assert (lis.GetLine().equals("FIX")); 
        lis.FetchNextLine(); 
        //1.0	0.000	0.000	0.000
        assert lis.w[0].startsWith("1."); 
        sbsvx.append("*fix\t" + lis.w[0].substring(2) + "\t" + lis.w[1] + "\t" + lis.w[2] + "\t" + lis.w[3] + TN.nl); 

        lis.FetchNextLine(); 
    }

	/////////////////////////////////////////////
    //TRIP
    //DATE 2009-04-20 
    //DECLINATION     0.00
    //DATA
    void LoadTrip(LineInputStream lis) throws IOException
    {
        assert (lis.GetLine().equals("TRIP")); 

        lis.FetchNextLine(); 
        assert lis.w[0].equals("DATE"); 
        sbsvx.append("*date\t" + lis.w[1].replace("-", ".") + TN.nl); 

        lis.FetchNextLine(); 
        assert lis.w[0].equals("DECLINATION"); 
        sbsvx.append("*calibrate\tdeclination\t" + lis.w[1] + TN.nl); 

        lis.FetchNextLine(); 
        assert lis.GetLine().equals("DATA"); 

        lis.FetchNextLine(); 
    }


	/////////////////////////////////////////////
    static OnePathNode NewCentrelineNode(String stationlabel, String sx, String sy, float xdisp)
    {
        String sxy = sx+","+sy; 
        float x = Float.valueOf(sx) * TN.CENTRELINE_MAGNIFICATION + xdisp; 
        float y = -Float.valueOf(sy) * TN.CENTRELINE_MAGNIFICATION; 

        OnePathNode opn = new OnePathNode(x, y, 0.0F); 
        opn.pnstationlabel = stationlabel + "          ".substring(stationlabel.length()) + 
                             sx+","+sy; 
        return opn; 
    }

	/////////////////////////////////////////////
    static OnePathNode FindStationNode(String sx, String sy, List<OnePathNode> stationnodes, int iss, float xdisp)
    {
        String sxy = sx+","+sy; 
        assert ((iss == 0) || (iss == 10)); 
        for (OnePathNode opn : stationnodes)
        {
            if (sxy.equals(opn.pnstationlabel.substring(iss)))
                return opn; 
        }

        // no new nodes in case of the station
        if (iss == 10)  
            return null; 

        // make new node in case of the sketch
        float x = Float.valueOf(sx) * TN.CENTRELINE_MAGNIFICATION + xdisp; 
        float y = -Float.valueOf(sy) * TN.CENTRELINE_MAGNIFICATION; 
        OnePathNode opn = new OnePathNode(x, y, 0.0F); 
        opn.pnstationlabel = sxy; 
        return opn; 
    }

	/////////////////////////////////////////////
    void LoadTopoSketch(LineInputStream lis, List<OnePath> vpaths, float xdisp) throws IOException
    {
        System.out.println("Loadingplan"); 
        lis.FetchNextLine(); 
        assert (lis.GetLine().equals("STATIONS")); 

        List<OnePathNode> stationnodes = new ArrayList<OnePathNode>(); 

        while (lis.FetchNextLine())
        {
            if (lis.GetLine().equals("SHOTS"))
                break; 
            assert (lis.iwc == 3); 
            assert lis.w[2].startsWith("1."); 

            stationnodes.add(NewCentrelineNode(lis.w[2].substring(2), lis.w[0], lis.w[1], xdisp)); 
        }

        List<OnePathNode> splaynodes = new ArrayList<OnePathNode>(); 
        assert (lis.GetLine().equals("SHOTS")); 
        int splaycount = 1;  
		while (lis.FetchNextLine())
        {
            if (lis.GetLine().startsWith("POLYLINE"))
                break; 
            if (lis.GetLine().startsWith("ELEVATION"))
                break; 
            if (lis.iwc == 0)
				continue; 
            if (lis.iwc != 4)
				lis.emitError("SHOTS line does not have 4 terms"); 
            OnePathNode lpnstart = FindStationNode(lis.w[0], lis.w[1], stationnodes, 10, xdisp); 
            OnePathNode lpnend = FindStationNode(lis.w[2], lis.w[3], stationnodes, 10, xdisp); 

            // centreline type
            if ((lpnstart != null) && (lpnend != null))   // not splay type
                vpaths.add(new OnePath(lpnstart, lpnstart.pnstationlabel.substring(0, 10).trim(), lpnend, lpnend.pnstationlabel.substring(0, 10).trim())); 
    
            // build the splay type (sigh) 
            else if ((lpnstart != null) || (lpnend != null))
            {
                if (lpnstart == null)
                {
                    lpnstart = NewCentrelineNode(String.valueOf(splaycount++), lis.w[0], lis.w[1], xdisp); 
                    splaynodes.add(lpnstart); 
                }
                if (lpnend == null)
                {
                    lpnend = NewCentrelineNode(String.valueOf(splaycount++), lis.w[2], lis.w[3], xdisp); 
                    splaynodes.add(lpnend); 
                }
                vpaths.add(new OnePath(lpnstart, lpnstart.pnstationlabel.substring(0, 10).trim(), lpnend, lpnend.pnstationlabel.substring(0, 10).trim())); 
            }
			else
				lis.emitWarning("Unable to find stations for shot endpoints"); 
        }
        for (OnePathNode opn : stationnodes)
            opn.pnstationlabel = null; 
        for (OnePathNode opn : splaynodes)
            opn.pnstationlabel = null; 

        // the sketch
        List<OnePathNode> vnodes = new ArrayList<OnePathNode>(); 

		if (lis.GetLine().startsWith("ELEVATION"))
			return;  // missing shots section

        assert (lis.GetLine().startsWith("POLYLINE")); 
        while (true)
        {
            if (lis.iwc == 0)
                break; 

            assert (lis.iwc == 2); 
            assert (lis.w[0].equals("POLYLINE")); 
            String col = lis.w[1]; 

            OnePathNode opnstart = null; 
            OnePath op = null; 
            String sx = null;
            String sy = null; 
            while (lis.FetchNextLine())
            {
                if ((lis.iwc != 2) || lis.w[0].equals("POLYLINE"))
                    break; 
                sx = lis.w[0]; 
                sy = lis.w[1]; 
                if (opnstart == null)
                {
                    opnstart = FindStationNode(lis.w[0], lis.w[1], vnodes, 0, xdisp); 
                    op = new OnePath(opnstart); 
                }
                else
                {
                    float x = Float.valueOf(sx) * TN.CENTRELINE_MAGNIFICATION + xdisp; 
                    float y = -Float.valueOf(sy) * TN.CENTRELINE_MAGNIFICATION; 
                    op.LineTo(x, y); 
                }
            }

            // case of single point
            if (op.nlines == 0)
            {
                sy = String.valueOf(Float.valueOf(sy) + 0.06F); 
                float x = Float.valueOf(sx) * TN.CENTRELINE_MAGNIFICATION + xdisp; 
                float y = -Float.valueOf(sy) * TN.CENTRELINE_MAGNIFICATION; 
                op.LineTo(x, y); 
            }
            

            OnePathNode opnend = FindStationNode(sx, sy, vnodes, 0, xdisp); 
            op.EndPath(opnend); 

            if (col.equals("CONNECT"))
                op.linestyle = SketchLineStyle.SLS_CONNECTIVE; 
            else if (col.equals("BROWN"))
            {
                op.linestyle = SketchLineStyle.SLS_DETAIL; 
                op.vssubsets.add("orange"); 
            }
            else if (col.equals("BLACK"))
                op.linestyle = SketchLineStyle.SLS_WALL; 
            else if (col.equals("RED"))
            {
                op.linestyle = SketchLineStyle.SLS_DETAIL; 
                op.vssubsets.add("red"); 
            }
            else if (col.equals("GRAY"))
            {
                op.linestyle = SketchLineStyle.SLS_DETAIL; 
                op.vssubsets.add("strongrey"); 
            }
            else if (col.equals("BLUE"))
            {
                op.linestyle = SketchLineStyle.SLS_DETAIL; 
                op.vssubsets.add("blue"); 
            }
            else if (col.equals("GREEN"))
            {
                op.linestyle = SketchLineStyle.SLS_DETAIL; 
                op.vssubsets.add("green"); 
            }
            else
            {
                op.linestyle = SketchLineStyle.SLS_CEILINGBOUND; 
                System.out.println("Unknown topocolo: " + col); 
            }

            vpaths.add(op); 
        }
    }


	/////////////////////////////////////////////
    void LoadSVXData(LineInputStream lis) throws IOException
    {
        lis.FetchNextLine(); 
        if (lis.GetLine().equals("FIX"))
            LoadFix(lis); 

        assert lis.GetLine().equals("TRIP"); // the trip group is at the start, but sometimes there's one halfway through (eg to set the date)
        lis.UnFetch(); 
		while (lis.FetchNextLine())
        {
            if (lis.GetLine().equals("TRIP"))
                LoadTrip(lis); 

            // quit when we have a blank line
            if (lis.iwc == 0)
                break; 
            assert !lis.w[0].equals("PLAN"); 

            // 1.19	1.18	351.68	-26.98	6.404	>
            assert lis.w[0].startsWith("1."); 
            if (lis.iwc == 6)
            {
                assert lis.w[1].startsWith("1."); 

                // the < arrows are rare and seem to correspond to legs that don't have continuations
                assert lis.w[5].equals(">") || lis.w[5].equals("<"); 

                sbsvx.append(lis.w[0].substring(2) + "\t" + lis.w[1].substring(2) + "\t" + 
                             lis.w[4] + "\t" + lis.w[2] + "\t" + lis.w[3] + TN.nl); 

            }
            else if (lis.iwc == 5)
            {
                assert lis.w[4].equals(">") || lis.w[4].equals("<"); 

                String splaystation = lis.w[0].substring(2); 
                int isplaystation = Integer.valueOf(splaystation); 
                splaycounters[isplaystation]++; 

                sbsvxsplay.append(splaystation); 
                sbsvxsplay.append("\t"); 
                sbsvxsplay.append(splaystation); 
                sbsvxsplay.append("_"); 
                sbsvxsplay.append(splaycounters[isplaystation]); 
                sbsvxsplay.append("\t"); 

                sbsvxsplay.append(lis.w[3] + "\t" + lis.w[1] + "\t" + lis.w[2] + TN.nl); 
            }
            else 
                assert false; // unknown string format
        }
    }

	/////////////////////////////////////////////
	void LoadPockettopo(FileAbstraction loadfile)
	{
        try
        { 

        LineInputStream lis = new LineInputStream(loadfile.GetInputStream(), loadfile, null, null);
        LoadSVXData(lis); 

        lis.FetchNextLine(); 
        if (lis.GetLine().equals("PLAN"))
            LoadTopoSketch(lis, vpathsplan, 0.0F); 
        
        lis.FetchNextLine(); 
        if (lis.GetLine().equals("ELEVATION"))
            LoadTopoSketch(lis, vpathsplan, 1000.F); 

        if (!lis.FetchNextLine())
        {
            TN.emitWarning("Unaccounted lines"); 
            lis.UnFetch(); 
            while (lis.FetchNextLine())
                System.out.println("Unnacounted line: " + lis.GetLine()); 
        }

        lis.inputstream.close(); 
        }
		catch (IOException e)
		{ TN.emitError(e.toString()); };
    }




	/////////////////////////////////////////////
    String GetSVX()
    {
        return sbsvx.toString() + TN.nl + "*flags splay" + TN.nl + sbsvxsplay.toString() + TN.nl; 
    }

	/////////////////////////////////////////////
    static boolean IsPocketTopo(String sfilehead)
    {
        if (sfilehead.indexOf("FIX") == 0)
            return true; 
        if (sfilehead.indexOf("TRIP") == 0)
            return true; 
        return false; 
    }
}

