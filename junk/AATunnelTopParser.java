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

// See TopParser/readtop.py for the source code

import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;

import java.util.List;
import java.util.ArrayList;

import java.awt.Font;
import java.awt.Color;
import java.awt.geom.AffineTransform;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.InputStreamReader;

/////////////////////////////////////////////
class TN
{
    static void emitWarning(String s)
    {
        System.out.println(s); 
    }
}

/////////////////////////////////////////////
// placeholder class (add more parameters as necessary) which we can use to hold the data 
// before it gets imported into other systems (eg tunnel)
class OnePath
{
    String stationfrom = ""; 
    String stationto = ""; 
    GeneralPath gp = null; 
    
    OnePath() { }
};

/////////////////////////////////////////////
class AATunnelTopParser
{
	List<OnePath> paths = new ArrayList<OnePath>(); 
    int version; 

	/////////////////////////////////////////////
	int ReadInt(InputStream inp) throws IOException
	{
		int b0 = inp.read();
		int b1 = inp.read();
		int b2 = inp.read();
		int b3 = inp.read();
		int res = b0 + (b1 << 8) + (b2 << 16) + (b3 << 24); 
System.out.println("eee "+b0+" "+b1+" "+b2+" "+b3+"   "+res); 
		return res; 
	}

	/////////////////////////////////////////////
	int ReadDate(InputStream inp) throws IOException
	{
		byte[] sdate = new byte[8]; 
		inp.read(sdate, 0, 8);  
		System.out.println(ReadInt(inp)+" "+ReadInt(inp)); 
		return 0; 
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
	    int commentlength = inp.read(); 
	    if (commentlength >= 128)
        {
			int commentlength2 = inp.read(); 
			commentlength = commentlength - 128 + 128*commentlength2; 
	        assert commentlength2 < 128; 
		}
System.out.println("Commentlength "+commentlength);  
		byte[] cstr = new byte[commentlength];
		return new String(cstr); 
	}

	/////////////////////////////////////////////
	boolean ParseFile(File tfile)
	{ try {
		InputStream inp = new FileInputStream(tfile); 
        
		byte[] htop = new byte[3]; 
		inp.read(htop, 0, 3);  
		System.out.println(new String(htop));
		assert "Top".equals(new String(htop)); 

        version = inp.read(); 
        TN.emitWarning("We have a top file version " + version); 
		int ntrips = ReadInt(inp); 
		for (int i = 0; i < ntrips; i++)
		{
			ReadDate(inp); 
			String comments = ReadComments(inp); 
			System.out.println("::"+comments+"::"); 
			int declination = (inp.read()<<8) + inp.read(); 
			System.out.println("declination "+declination); 
		}
		for (int i = 0; i < 10; i++)
			System.out.println(i+"  "+inp.read()); 
        inp.close(); 
	}
	catch (IOException e)
	{
		TN.emitWarning(e.toString());
	}
	return false; 
	}

	/////////////////////////////////////////////
	// startup the program
    public static void main(String args[])
    {
        if (args.length != 0)
        {
            AATunnelTopParser aatunneltopparser = new AATunnelTopParser(); 
            aatunneltopparser.ParseFile(new File(args[0])); 
            System.out.println("We have read "+aatunneltopparser.paths.size()+" paths"); 
        }
        else
            TN.emitWarning("need to put in a file name"); 
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
