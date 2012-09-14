////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2011  Julian Todd.
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

// See TopParser/readtop.py for the source code

import java.awt.geom.Line2D;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.Font;
import java.awt.Color;
import java.awt.geom.AffineTransform;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.InputStreamReader;

class TOPxsection
{
	int x;
	int y;
	String stn;
	float direction;

	TOPxsection(InputStream inp) throws IOException
	{
		x = TunnelTopParser.ReadInt4(inp);
		y = TunnelTopParser.ReadInt4(inp);
		stn = TunnelTopParser.ReadStn(inp);
		int idirection = TunnelTopParser.ReadInt4(inp);
		if (idirection != -1)
			direction = TunnelTopParser.adegrees(idirection);
		else
			direction = -1.0f;
		System.out.println("Xsection "+ x +" "+y+" "+stn+" "+direction);
	}
}

class TOPpolygon
{
	int[] poly;
	int col;

	TOPpolygon(InputStream inp) throws IOException
	{
		//System.out.println("Polygon");
		int a = TunnelTopParser.ReadInt4(inp);
		poly = new int[a*2];
		for (int i = 0; i <a; i++)
		{
			poly[i * 2] = TunnelTopParser.ReadInt4(inp);
			poly[i * 2 +1] = TunnelTopParser.ReadInt4(inp);
			//System.out.println(poly[i * 2] +" "+ poly[i * 2+1]);
		}
		col = inp.read();
		//System.out.println(col);	 
	}
}
/////////////////////////////////////////////
class TunnelTopParser
{
	int version;
	List<TOPxsection> planxsections = new ArrayList<TOPxsection>();
	List<TOPxsection> elevxsections = new ArrayList<TOPxsection>();
	List<TOPpolygon> planpolygons = new ArrayList<TOPpolygon>();
	List<TOPpolygon> elevpolygons = new ArrayList<TOPpolygon>();
	
    StringBuilder sbsvx = new StringBuilder();
    StringBuilder sbsvxsplay = new StringBuilder();

// look in LoadTopoSketch() in PocketTopoLoader.java
    List<OnePath> vpathsplan = new ArrayList<OnePath>(); 
    static float TOPFILE_SCALE = 0.001F; // it's in milimetres
    
	/////////////////////////////////////////////	
	static float adegrees(int bangle)
	{
		return 360*(float)bangle / 65536;
	}

	/////////////////////////////////////////////
	static int ReadInt2(InputStream inp) throws IOException
	{
		int b0 = inp.read();
		int b1 = inp.read();

		int res = b0 + (b1 << 8); 
//System.out.println("eee "+b0+" "+b1+" "+res); 
		return res; 
	}
	/////////////////////////////////////////////
	static int ReadInt4(InputStream inp) throws IOException
	{
		int b0 = inp.read();
		int b1 = inp.read();
		int b2 = inp.read();
		int b3 = inp.read();
		int res = b0 + (b1 << 8) + (b2 << 16) + (b3 << 24); 
//System.out.println("eee "+b0+" "+b1+" "+b2+" "+b3+"   "+res); 
		return res; 
	}

	long ReadInt8(InputStream inp) throws IOException
	{
		long b0 = inp.read();
		long b1 = inp.read();
		long b2 = inp.read();
		long b3 = inp.read();
		long b4 = inp.read();
		long b5 = inp.read();
		long b6 = inp.read();
		long b7 = inp.read();
		long res = b0 + (b1 << 8) + (b2 << 16) + (b3 << 24) + (b4 << 32) + (b5 << 40) + (b6 << 48) + (b7 << 56); 
//System.out.println("eee "+b0+" "+b1+" "+b2+" "+b3+"   "+res); 
		return res; 
	}

	/////////////////////////////////////////////
	Date ReadDate(InputStream inp) throws IOException
	{  
		long i1 = ReadInt4(inp);
		long i2 = ReadInt4(inp);
		long si = i1 + (i2<<32);
		long sit = (si - 621355968000000000L) / 10000;
		Date date = new Date(sit);
		System.out.println(date);  
		return date; 
//		ticks =  struct.unpack('<Q', F.read(8))
//		#Need to convert this date from .NET
//		NANOSEC = 10000000
//		#Number of python tick since 1/1/1 00:00
//		PTICKS = 62135596800
//		tripdate = time.gmtime((ticks[0]/NANOSEC)-PTICKS)
	}	

	/////////////////////////////////////////////
	String ReadComments(InputStream inp) throws IOException
	{
		int commentlength = 0;
		int cbyte =1;
		while (true)
		{
			int commentbyte = inp.read();
			commentlength += (commentbyte & 127) * cbyte;
			if (commentbyte < 128)
				break;
			cbyte *= 128;
		}
	    
System.out.println("Commentlength "+commentlength);
		if (commentlength == 0)
				return "";
		else
		{  
			byte[] cstr = new byte[(int)commentlength];
			inp.read(cstr, 0, commentlength);
			return new String(cstr,"UTF8");
		}
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////

	/////////////////////////////////////////////
	static String ReadStn(InputStream inp) throws IOException
	{
		//id's split into major.decimal(minor)
		int idd = ReadInt2(inp);
		int idm = ReadInt2(inp);
		//Turn stn into string, and nulls into -
		if (idm == 32768)
			return new String("-");
		else
			return String.valueOf(idm)+"."+String.valueOf(idd);


	}

	/////////////////////////////////////////////
	String flagdirection (int flags)
	{
	if ((flags & 1) == 1)
		{
			return "*eleft ";
		}
		else
		{
			return "*eright ";
		}
	}
	/////////////////////////////////////////////
	
	void tripcomments(StringBuilder svxfile, String comments, Date dates, float declination)
	{
		svxfile.append(";;; TRIP COMMENT FROM POCKETTOPO ;;;" + TN.nl + ";" +comments + TN.nl + TN.nl);
		svxfile.append(String.format("*date %tF%s", dates, TN.nl));
		svxfile.append(";*declination "+ declination + TN.nl+ TN.nl);
	}
	/////////////////////////////////////////////
	void drawing(List<TOPxsection> xsections, List<TOPpolygon> polygons, InputStream inp) throws IOException
	{
		mapping(inp);
		while (true)
		{
			int element = inp.read();
			if (element == 0)
				break;
			if (element == 1)
				polygons.add(new TOPpolygon(inp));
			else if (element == 3)
				xsections.add(new TOPxsection(inp));
			else
				TN.emitError("Element number not defined");
		}
			
	}
	/////////////////////////////////////////////
	void mapping(InputStream inp) throws IOException
	{
		//Gets the centre point of screen and scale of different views
		int X = ReadInt4(inp);
		int Y = ReadInt4(inp);
		int scale = ReadInt4(inp);
		System.out.println(X +" "+ Y +" "+ scale);
	}			

	/////////////////////////////////////////////
    static OnePathNode FindStationNode(String stn, int x, int y, List<OnePathNode> stationnodes, float xdisplace)
    {
        for (OnePathNode opn : stationnodes)
        {
            if (stn.equals(opn.pnstationlabel))
                return opn; 
        }

        // make new node in case of the sketch
        OnePathNode opn = new OnePathNode(x * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION + xdisplace, -y * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION, 0.0F); 
        opn.pnstationlabel = stn; 
        return opn; 
    }

	/////////////////////////////////////////////
    String GetSVX()
    {
        return "; your svx file here" + TN.nl; 
    }

	/////////////////////////////////////////////
	boolean ParseFile(FileAbstraction tfile)
	{ try {
		InputStream inp = tfile.GetInputStream(); 
		byte[] htop = new byte[3]; 
		inp.read(htop, 0, 3);  
		System.out.println(new String(htop));
		assert "Top".equals(new String(htop)); 

        version = inp.read(); 
        TN.emitWarning("We have a top file version " + version); 
		int ntrips = ReadInt4(inp);
		Date[] dates = new Date[ntrips];
		String[] comments = new String[ntrips];
		float[] declination = new float[ntrips];
		
		for (int i = 0; i < ntrips; i++)
		{
			dates[i] = ReadDate(inp); 
			comments[i] = ReadComments(inp); 
			Pattern p = Pattern.compile("\n");
			Matcher m = p.matcher(comments[i]);
			comments[i] = m.replaceAll(TN.nl + ";");
			System.out.println(comments[i]); 
			declination[i] = adegrees(ReadInt2(inp));  
		}
		//legs/shots
		StringBuilder svxfile = new StringBuilder();
		int tripcount = 0;
		svxfile.append("*begin "+ tfile.getSketchName() + TN.nl);
		svxfile.append(";*require 1.????"+ TN.nl+ TN.nl);		
		
		tripcomments(svxfile, comments[tripcount], dates[tripcount], declination[tripcount]);

		svxfile.append("*data normal from to tape compass clino ignore all"+ TN.nl);
		int nshots = ReadInt4(inp);
		//svxfile.append((r'\n',r'\n;',comments[tripcount]) + TN.nl);
		for (int i = 0; i < nshots; i++)
		{
			//Station
			String fromstn = ReadStn(inp);
			String tostn = ReadStn(inp);
			int dist = ReadInt4(inp);
			float azimuth = adegrees(ReadInt2(inp));
			float inclination = adegrees(ReadInt2(inp));
			int flags = inp.read();
			int roll = inp.read();
			int tripindex = ReadInt2(inp);
			int currenttrip = -1;
			int currentdirection = -1;
			String comment = "";
			//bit 1 of flags is flip (left or right)
    		//bit 2 of flags indicates a comment
    		if ((flags & 2)  == 2)
			{
				comment = ReadComments(inp);
			}
			
			/*if (i == 0)
			{
				assert (tostn == "-");				
				currenttrip = tripindex;
				currentdirection = (flags & 1);
				svxfile.append(flagdirection(flags) + fromstn +TN.nl);
			}
			else 
			{
				if (currenttrip != tripindex);
				{
					tripcomments(svxfile, comments[tripcount], dates[tripcount], declination[tripcount]);
					tripcount++;
					currenttrip = tripindex;
					
				}
			}*/

			
			svxfile.append(String.format("%s\t%s\t%.3f\t%.2f\t%.2f\tRoll:%.0f\t%s%s%s",fromstn, tostn, dist/1000.0, azimuth, inclination, 360*roll/256.0, comment, tripindex, TN.nl));
			//System.out.println(fromstn +" "+ tostn +" "+ dist +" "+ azimuth +" "+inclination +" "+ flags +" "+ roll +" "+ tripindex+" "+ comment");
		}
		svxfile.append("*end "+ tfile.getSketchName());
		System.out.println(svxfile);
		//System.out.println(tfile.getSketchName());
		//Reference sations
		int nrefstn = ReadInt4(inp);
		System.out.println("Stn NS EW");
		for (int i = 0; i < nrefstn; i++)				
		{
			String stn = ReadStn(inp);
			long east = ReadInt8(inp);
			long west = ReadInt8(inp);
			int altitute =ReadInt2(inp);
			String comment = ReadComments(inp);
		}

		//Overview Mapping information (not needed by import)
		mapping(inp);

		//Plan (outline)
		drawing(planxsections, planpolygons, inp);
		//Elevation (sideview)
		drawing(elevxsections, elevpolygons, inp);

        inp.close();

        // example single path intop the file
 // look in LoadTopoSketch() in PocketTopoLoader.java for more information (and how to do centrelines)
        List<OnePathNode> stationnodes = new ArrayList<OnePathNode>(); 
        float xdisplace = 0.0F; 
        
        OnePathNode lpnstart = FindStationNode("111.111", 10000, 20000, stationnodes, xdisplace); 
        OnePathNode lpnend = FindStationNode("222.222", 50000, 20000, stationnodes, xdisplace); 
        OnePath op = new OnePath(lpnstart); 
        op.LineTo(40000 * TOPFILE_SCALE* TN.CENTRELINE_MAGNIFICATION + xdisplace, 30000 * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION); 
        op.EndPath(lpnend); 
        op.linestyle = SketchLineStyle.SLS_WALL; 
        op.vssubsets.add("orange"); 
        vpathsplan.add(op); 
    }
	catch (IOException e)
	{
		TN.emitWarning(e.toString());
	}
	return false; 
	}
}

/*
def station(F):
    #id's split into major.decimal(minor)
    idd = struct.unpack('<H', F.read(2))
    idm = struct.unpack('<H', F.read(2))
    #Turn stn into string, andnulls into -
    if idm[0] == 32768:
        stnid = "-"
    else:
        stnid = str(idm[0])+"."+str(idd[0])
    return stnid

def shot(F):
    thline = {'from' : station(F)}
    thline['to'] =  station(F)
    Dist = struct.unpack('<L', F.read(4))
    thline['tape'] = distmm(Dist[0])
    azimuth = struct.unpack('<H', F.read(2))
    thline['compass'] = adegrees(azimuth[0])
    inclination = struct.unpack('<h', F.read(2))
    thline['clino'] = adegrees(inclination[0])
    flags = struct.unpack('<B', F.read(1))
    #Roll of the DistoX on taking reading can be ignored
    #Internal angle interger 0-256
    #Roll = struct.unpack('<B', F.read(1))
    F.read(1)
    tripindex = struct.unpack('<h', F.read(2))
    thline['trip'] = tripindex[0]
    #bit 1 of flags is flip (left or right)
    #bit 2 of flags indicates a comment
    if (flags[0] & 0b00000001) == 0b000000001:
        thline['direction'] = '<'
    else:
        thline['direction'] = '>'
    if (flags[0] & 0b00000010) == 0b000000010:
        thline['comment'] = comments(F)
    return thline
*/
