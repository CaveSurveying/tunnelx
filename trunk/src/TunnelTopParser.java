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
class TOPleg
{
	String fromstn; 
	String tostn; 
	double dist; 
	double azimuth; 
	double inclination; 
	double roll; 
	int tripindex; 
	String comment; 
	TOPleg topleglink = null; // linked list to the duplicates
	
	TOPleg(String lfromstn, String ltostn, double ldist, double lazimuth, double linclination, double lroll, int ltripindex, String lcomment)
	{
		fromstn = lfromstn; 
		tostn = ltostn; 
		dist = ldist; 
		azimuth = lazimuth; 
		inclination = linclination; 
		roll = lroll; 
		tripindex = ltripindex; 
		comment = lcomment; 
	}
	
	boolean MergeDuplicate(TOPleg ntopleg)
	{
		if (ntopleg.tostn.equals("-") || !fromstn.equals(ntopleg.fromstn) || !tostn.equals(ntopleg.tostn)) 
			return false; 
		ntopleg.topleglink = topleglink; 
		topleglink = ntopleg; 
		return true; 
	}
	
	public String toString()
	{
		int nduplicates = 0; 
		boolean bazimuthnearzero = ((azimuth < 20.0) || (azimuth > 340.0)); 
		double sumdist = 0.0; 
		double suminclination = 0.0; 
		double sumazimuth = 0.0; 
		String sumcomment = ""; 
		for (TOPleg ntopleg = this; ntopleg != null; ntopleg = ntopleg.topleglink)
		{
			sumdist += ntopleg.dist; 
			suminclination += ntopleg.inclination; 
			sumazimuth += ntopleg.azimuth + (bazimuthnearzero && (ntopleg.azimuth < 180.0) ? 360.0 : 0.0); 
			if (!ntopleg.comment.equals(""))
				sumcomment += ntopleg.comment+"  "; 
			nduplicates++; 
		}
		double avgdist = sumdist / nduplicates; 
		double avginclination = suminclination / nduplicates; 
		double avgazimuth = sumazimuth / nduplicates; 
		
		double extdist = 0.0; 
		double extinclination = 0.0; 
		double extazimuth = 0.0; 
		for (TOPleg ntopleg = this; ntopleg != null; ntopleg = ntopleg.topleglink)
		{
			extdist = Math.max(extdist, Math.abs(avgdist - ntopleg.dist)); 
			extinclination = Math.max(extinclination, Math.abs(avginclination - ntopleg.inclination)); 
			extazimuth = Math.max(extazimuth, Math.abs(avgazimuth - (ntopleg.azimuth + (bazimuthnearzero && (ntopleg.azimuth < 180.0) ? 360.0 : 0.0)))); 
		}
		String exts = ""; 
		if ((extdist > 0.05) || (extinclination > 0.5) || (extazimuth > 0.5))
			exts = String.format(" ext:%.1f,%.1f,%.1f ", extdist, extinclination, extazimuth); 
		return String.format("%s\t%s\t%.3f\t%.1f\t%.1f%s%s%s", fromstn, tostn, avgdist, avgazimuth - (avgazimuth >= 360.0 ? 360.0 : 0.0), avginclination, exts, sumcomment, TN.nl);
	}
}

/////////////////////////////////////////////
class TunnelTopParser
{
	int version;
	List<TOPxsection> planxsections = new ArrayList<TOPxsection>();
	List<TOPxsection> elevxsections = new ArrayList<TOPxsection>();
	List<OnePath> planpolygons = new ArrayList<OnePath>();
	List<OnePath> elevpolygons = new ArrayList<OnePath>();
	
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
		int cbyte = 1;
		while (true)
		{
			int commentbyte = inp.read();
			commentlength += (commentbyte & 127) * cbyte;
			if (commentbyte < 128)
				break;
			cbyte *= 128;
		}
	    
		if (commentlength == 0)
			return "";

		byte[] cstr = new byte[(int)commentlength];
		inp.read(cstr, 0, commentlength);
		String res = new String(cstr, "UTF8");
		TN.emitMessage("Commentlength "+commentlength+ "  " + res);
		return "  ; "+res.replaceAll("\\r\\n|\\n|\\r", TN.nl+";");
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
    OnePath readTOPpolygon(InputStream inp, List<OnePathNode> stationnodes) throws IOException
	{
		//System.out.println("Polygon");
		int a = TunnelTopParser.ReadInt4(inp);


		//System.out.println(col);
		float xdisplace = 0.0F; 
        int x0 = TunnelTopParser.ReadInt4(inp);
        int y0 = TunnelTopParser.ReadInt4(inp);
        
        OnePathNode lpnstart = FindStationNode(null, x0, y0, stationnodes, xdisplace);
        OnePath op = new OnePath(lpnstart);
        for (int i = 1; i<a-1; i++)
        {
            int x = TunnelTopParser.ReadInt4(inp);
            int y = TunnelTopParser.ReadInt4(inp);
            op.LineTo(x * TOPFILE_SCALE* TN.CENTRELINE_MAGNIFICATION + xdisplace, y * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION);
        }
        if (a ==1)
        {
            OnePathNode  lpnend = FindStationNode(null, x0, y0+1, stationnodes, xdisplace);
            op.EndPath(lpnend); 
        }
        else
        {
            int xa = TunnelTopParser.ReadInt4(inp);
            int ya = TunnelTopParser.ReadInt4(inp);
            OnePathNode lpnend = FindStationNode(null, xa, ya, stationnodes, xdisplace);
            op.EndPath(lpnend);
        }
        int col = inp.read();
		// 8 does not exist, connective might be needed later
		op.vssubsets.add("top"+col); 
		if (col == 8)
			op.linestyle = SketchLineStyle.SLS_CONNECTIVE;

		//3 is brown 7 orange.
		else if (col == 7)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("orange"); 
		}
		else if (col == 3)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("magenta"); 
		}
		else if (col == 1)
			op.linestyle = SketchLineStyle.SLS_WALL; 
		else if (col == 5)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("red"); 
		}
		else if (col == 2)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("strongrey"); 
		}
		else if (col == 4)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("blue"); 
		}
		else if (col == 6)
		{
			op.linestyle = SketchLineStyle.SLS_DETAIL; 
			op.vssubsets.add("green"); 
		}
		else
		{
			op.linestyle = SketchLineStyle.SLS_CEILINGBOUND; 
			System.out.println("Unknown topocolo: " + col); 
		}
        op.linestyle = SketchLineStyle.SLS_WALL; 
        //vpathsplan.add(op);
        return op;	 
	}
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
			return String.valueOf(idm)+"-"+String.valueOf(idd);
	}

	/////////////////////////////////////////////
	void tripcomments(StringBuilder sbsvx, String comments, Date cdate, float declination)
	{
		sbsvx.append(";;; TRIP COMMENT FROM POCKETTOPO ;;;" + TN.nl + ";" +comments + TN.nl + TN.nl);
		sbsvx.append(String.format("*date %tY.%tm.%td%s", cdate, cdate, cdate, TN.nl));
		sbsvx.append(";*declination "+ declination + TN.nl+ TN.nl);
	}
	
	/////////////////////////////////////////////
	void drawing(List<TOPxsection> xsections, List<OnePath> polygons, InputStream inp, List<OnePathNode> stationnodes) throws IOException
	{
		mapping(inp);
		while (true)
		{
			int element = inp.read();
			if (element == 0)
				break;
			if (element == 1)
				polygons.add(readTOPpolygon(inp, stationnodes));
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
        OnePathNode opn = new OnePathNode(x * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION + xdisplace, y * TOPFILE_SCALE * TN.CENTRELINE_MAGNIFICATION, 0.0F); 
        opn.pnstationlabel = stn; 
        return opn; 
    }

	/////////////////////////////////////////////
    String GetSVX()
    {
		return sbsvx.toString(); 
    }

	boolean bsingledashsplays = false; 
	
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
			declination[i] = adegrees(ReadInt2(inp));  
		}
		//legs/shots
		int tripcount = 0;
		sbsvx.append("*begin "+ tfile.getSketchName() + TN.nl);
		sbsvx.append(";*require 1.????"+ TN.nl+ TN.nl);		
		
		tripcomments(sbsvx, comments[tripcount], dates[tripcount], declination[tripcount]);

		int currenttrip = -1;
		int currentdirection = -1;

		sbsvx.append("*data normal from to tape compass clino ignoreall"+ TN.nl);
		int nshots = ReadInt4(inp);
		//sbsvx.append((r'\n',r'\n;',comments[tripcount]) + TN.nl);
		List<TOPleg> toplegs = new ArrayList<TOPleg>();
		for (int i = 0; i < nshots; i++)
		{
			//Station
			String fromstn = ReadStn(inp);
			String tostn = ReadStn(inp);
			int dist = ReadInt4(inp);
			float azimuth = adegrees(ReadInt2(inp));
			float inclination = adegrees(ReadInt2(inp));
			if (inclination > 180.0F)
				inclination = inclination - 360.0F; 
			int flags = inp.read();
			int roll = inp.read();
			int tripindex = ReadInt2(inp);
			String comment = "";
			//bit 1 of flags is flip (left or right)
    		//bit 2 of flags indicates a comment
    		if ((flags & 2)  == 2)
				comment = ReadComments(inp);
			
			// not sure what this bit is about, but have cleaned it up
			if (i == 0)
			{
				currenttrip = tripindex;
				currentdirection = (flags & 1); 
				//assert(tostn.equals("-")); 				
				//sbsvx.append((currentdirection == 1 ? "*eleft " : "*eright ") + fromstn +TN.nl);
			}
			else 
			{
				if (currenttrip != tripindex);
				{
					//tripcomments(sbsvx, comments[tripcount], dates[tripcount], declination[tripcount]);
					tripcount++;
					currenttrip = tripindex;
				}
			}

			TOPleg ntopleg = new TOPleg(fromstn, tostn, dist/1000.0, azimuth, inclination, 360*roll/256.0, tripindex, comment); 
			if ((toplegs.size() == 0) || !toplegs.get(toplegs.size() - 1).MergeDuplicate(ntopleg))
				toplegs.add(ntopleg); 
		}
		
		if (toplegs.size() != 0)
			sbsvx.append("*fix "+toplegs.get(0).fromstn+" 0 0 0  ; default fix"+TN.nl+TN.nl); 
		for (TOPleg topleg : toplegs)
			if (!topleg.tostn.equals("-"))
				sbsvx.append(topleg.toString()); 
		sbsvx.append(TN.nl);
		sbsvx.append(";;;;;;;;;;;;"+TN.nl);
		sbsvx.append("*flags splay"+TN.nl);
		
		int nsplaycount = 1; 
		for (TOPleg topleg : toplegs)
		{
			if (topleg.tostn.equals("-"))
			{
				// this -n- format then can also be stripped out by FileAbstraction.RunSurvex
				if (!bsingledashsplays)
					topleg.tostn = "-"+nsplaycount+"-"; 
				sbsvx.append(topleg.toString()); 
				nsplaycount++; 
			}
		}
		
		sbsvx.append("*end "+ tfile.getSketchName());
		sbsvx.append(TN.nl);

		//System.out.println(tfile.getSketchName());
		//Reference stations
		int nrefstn = ReadInt4(inp);
		System.out.println("Stn NS EW "+nrefstn);
		for (int i = 0; i < nrefstn; i++)				
		{
			String stn = ReadStn(inp);
			long east = ReadInt8(inp);
			long west = ReadInt8(inp);
			int altitute = ReadInt2(inp);
			String comment = ReadComments(inp);
		}

		//Overview Mapping information (not needed by import)
		mapping(inp);
		List<OnePathNode> stationnodes = new ArrayList<OnePathNode>();
		//Plan (outline)
		drawing(planxsections, vpathsplan, inp, stationnodes);
		//Elevation (sideview)
		drawing(elevxsections, elevpolygons, inp, stationnodes);

        inp.close();

        // example single path intop the file
		// look in LoadTopoSketch() in PocketTopoLoader.java for more information (and how to do centrelines)
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
