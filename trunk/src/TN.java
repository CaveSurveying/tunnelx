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

import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import java.util.Random;

import java.util.Calendar; 
import java.text.SimpleDateFormat; 

import javax.swing.JTextArea; 

import java.util.Set; 
import java.util.HashSet; 

//
//
// TN
//
//
class TN
{
	// the file dialog box

	// relative paths don't work when we use them in the SvxFileDialog dialog box -- it makes it relative to the My Documents directory
	static FileAbstraction currentDirectory = null;  // set in the main() function 
	static FileAbstraction currentDirectoryIMG = null;  // set in the main() function 
	static FileAbstraction currprintdir = null; 

	static String survexexecutabledir = ""; // a string we can add "cavern.exe" to
	static String inkscapeexecutabledir = ""; // a string we can add "inkscape.com" to

    //static String troggleurl = "http://framos.lawoftheland.co.uk/troggle/"
    static String troggleurl = "http://127.0.0.1:8000/";   // jgtuploadfile, jgtfile
    static String tunnelversion = "version2016-09-01 Aspet";
    static String tunneluser = "aspet16"; // reset in InitFA from system properties
    static String tunnelpassword = "gosser";  // to be set from the command line
    static String tunnelproject = "cuccaustria";  // to be set from the command line

    static String tunneldate()
        { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()); }

	// convert degrees to radians
	static double degsin(double ang)
		{ return (ang == 90 ? 1.0 : (ang == -90 ? -1.0 : Math.sin(Math.toRadians(ang)))); }
	static double degcos(double ang)
		{ return (ang == 90 ? 0.0 : (ang == -90 ? -0.0 : Math.cos(Math.toRadians(ang)))); }
	static double percentdeg(double percent)
		{ return Math.toDegrees(Math.atan(percent / 100)); }; 

	static float radiusofsurveylabel_S = 50.0F; 

	// standard measurements
	static int STATION_FIELD_WIDTH = 20;

	static int XSECTION_MAX_NSIDES = 50;

	static float CENTRELINE_MAGNIFICATION = 10.0F; // factor to increase the centreline pos by so that we have less problem with the text rendering.

	// printing scale
	static int prtscale = 500; // could be a menu option

	static String framestylesubset = "framestyle";
	static float defaultrealposterpaperscale = 1000.0F;  // scaledown when we import background sketches into areas on the poster size (so posters don't have to be many kms wide in real space, and instead at least approx on right scale)

	static String planCLINEsubset = "plan_TOP"; 
	static String elevCLINEsubset = "elevation_TOP"; 
	static String flipCLINEsignal = "flip_TOP"; 
	
	//static String XSectionDefaultPoly = "4   1 1 0 0   1 0 -1 0   1 -1 0 0  1 0 1 0";
	static String XSectionDefaultVec = "0 0 0";

	static int XSinteractiveSensitivitySQ = 400;

	static String nl = "\r\n";

	public static boolean IsWhiteSpace(char ch)
	{
		return((ch == ' ') || (ch == '\t'));
	}


	// wireframe graphics
	static Color wfmtubeActive = Color.magenta;
	static Color wfmtubeInactive = new Color(0, 0, 150); // Color.blue;
	static Color wfmtubeDel = Color.red;

	static Color wfmxsectionActive = Color.magenta;
	static Color wfmxsectionInactive = Color.blue;
	static Color wfmpointActive = Color.magenta;
	static Color wfmpointInactive = Color.green;
	static Color wfmnameActive = Color.magenta;
	static Color wfmnameInactive = Color.cyan;

	static Color wfmBackground = new Color(100, 100, 100); //Color.black;
	static Color wfdaxesXY = Color.cyan;
	static Color wfdaxesZ = Color.blue;
	static Color wfmLeg = Color.white;


	// xsection graphics
	static Color xsgLines = Color.black;
	static Color xsgSelected = Color.green;
	static Color xsgOrigin = Color.yellow;
	static Color xsgGridline = Color.blue;
	static Color xsgCornerNode = Color.red;

	static int xsgOriginSize = 5;
	static int xsgPointSize = 3;

	static Color skeBackground = new Color(200, 200, 200);


	static char PathDelimeterChar = '|';
	static String PathDelimeter = "|";
	static char StationDelimeterChar = '^';
	static String StationDelimeter = "^";
	static String ExportDelimeter = ".";

	static String SurvexExtension = ".svx";
	static String ESurvexExtension = ".evx";
	static String TunnelExtension = ".tun";

	// sizes of the preview sections
	static int XprevWidth = 20;
	static int XprevHeight = 30;
	static int XprevBorder = 3;
	static int XprevItemsAcross = 6;
	static int XprevGap = 3;



	static Random ran = new Random();
	static LegLineFormat defaultleglineformat = new LegLineFormat();



	/////////////////////////////////////////////
	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// file names
	static String setSuffix(String fname, String suffix)
	{
		int il = fname.lastIndexOf('.');
		return (il == -1 ? fname : fname.substring(0, il)) + suffix;
	}

	/////////////////////////////////////////////
	static String getSuffix(String fname)
	{
		int il = fname.lastIndexOf('.');
		return (il == -1 ? "" : fname.substring(il));
	}

	/////////////////////////////////////////////
	static String loseSuffix(String fname)
	{
		int il = fname.lastIndexOf('.');
		return (il == -1 ? fname : fname.substring(0, il));
	}

	/////////////////////////////////////////////
	static String shortenString(String ssval, int maxleng)
	{
		if (ssval.length() < maxleng)
			return ssval; 
		int is = ssval.indexOf("/"); 
		int il = ssval.lastIndexOf("/"); 
		if ((is != il) && (is != -1))
		{
			String lssval = ssval.substring(0, is + 1) + "..." + ssval.substring(il); 
			if (lssval.length() < maxleng)
				return lssval; 
		}
		int ntrail = maxleng / 2 + 1; 
		int nhead = maxleng - ntrail - 3; 
		return ssval.substring(0, nhead) + "..." + ssval.substring(ssval.length() - ntrail); 
	}
		

	/////////////////////////////////////////////
	static String SUFF_XML = ".xml";
	static String SUFF_HTML = ".html";
	static String SUFF_SVX = ".svx";
	static String SUFF_POS = ".pos";
	static String SUFF_SRV = ".srv"; // walls subfile extension
	static String SUFF_TOP = ".top";
	static String SUFF_WALLS = ".prj";
	static String SUFF_VRML = ".wrl";
	static String SUFF_PNG = ".png";
	static String SUFF_JPG = ".jpg";
	static String SUFF_JPEG = ".jpeg";
	static String SUFF_BMP = ".bmp";
	static String SUFF_3D = ".3d";
	static String SUFF_PDF = ".pdf";
	static String SUFF_SVG = ".svg";

	static String SUFF_TXT = ".TXT";
	static String SUFF_GIF = ".GIF";

	static String[] SUFF_IGNORE = { "", ".extra", ".old", ".status", ".lev", ".pl", ".py", ".Log", ".DS_Store"};
	// (TortoiseCVS generates a lot of files called "TortoiseCVS.Status")


	// constants used in the userinterface windows
	static Color sketchlinestyle_col = new Color(0.5F, 0.3F, 0.8F);

	static boolean bVerbose = true;
    static boolean bTodeNode = false; 

    static MainBox mainbox = null; 

    static Set<String> pastmessages = new HashSet<String>(); 

	// message making
	public static void emitMessage(String mess)
	{
		if (bVerbose)
			System.out.println(mess);
	}

	public static boolean emitWarning(String mess)
	{
		System.out.println("Warning: " + mess);
        boolean btofront = !pastmessages.contains(mess); 
        if (btofront)
            pastmessages.add(mess); 
    	mainbox.emitErrorMessageLine("\nWarning: " + mess, btofront); 
		return true; 
	}

	public static void emitError(String mess)
	{
		System.out.println("ERROR: " + mess);
    	mainbox.emitErrorMessageLine("\nERROR: " + mess, true); 
		throw new RuntimeException("error");
	}

    // not sure this one makes a lot of sense.  should be be asserts?
	public static void emitProgError(String mess)
	{
		System.out.println("Programming Error: " + mess);
    	mainbox.emitErrorMessageLine("\nERROR: " + mess, true); 
	}
}

