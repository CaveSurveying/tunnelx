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


import java.io.File;
import java.awt.Color;

import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import java.util.Random;


//
//
// TN
//
//
class TN
{
static double x0 = -0.9;
static double x1 = 1.0;
static double w0 = 1.0;
static double w1 = 1.0;
static double tsamp = 0.1;


	// the file dialog box

	// must be / for linux
	static File currentDirectory = new File("/");
	//static File currentDirectory = new File("D:/TunnelX/TunnelX/xml caves/");

	static File currentSymbols = null;

	// convert degrees to radians
	static double degangfac = Math.PI / 180.0F;
	static double degsin(double ang)
		{ return (ang == 90 ? 1.0 : (ang == -90 ? -1.0 : Math.sin(ang * degangfac))); }
	static double degcos(double ang)
		{ return (ang == 90 ? 0.0 : (ang == -90 ? -0.0 : Math.cos(ang * degangfac))); }


	// standard measurements
	static int STATION_FIELD_WIDTH = 20;

	static int XSECTION_MAX_NSIDES = 50;

	static int MAX_GRIDLINES = 8;

	static float CENTRELINE_MAGNIFICATION = 10.0F; // factor to increase the centreline pos by so that we have less problem with the text rendering.

	// printing scale
	static int prtscale = 1000; // could be a menu option

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


	static String PathDelimeter = "|";
	static String StationDelimeter = "^";

	static String SurvexExtension = ".svx";
	static String ESurvexExtension = ".evx";
	static String TunnelExtension = ".tun";

	// sizes of the preview sections
	static int XprevWidth = 20;
	static int XprevHeight = 30;
	static int XprevBorder = 3;
	static int XprevItemsAcross = 6;
	static int XprevGap = 3;

	// stroke stuff.
	static float strokew = -1.0F;
	static float fstrokew = -1.0F; // font stroke width

	static String[] labstylenames = { "default", "step", "qm", "passage", "area", "cave" };
	static Font[] fontlabs = new Font[labstylenames.length];
	static Color fontcol = new Color(0.7F, 0.3F, 1.0F);


	static Random ran = new Random();
	static LegLineFormat defaultleglineformat = new LegLineFormat();

	static void SetStrokeWidths(float lstrokew, float lfstrokew)
	{
		strokew = lstrokew;
		fstrokew = lfstrokew;
		emitMessage("New stroke width: " + strokew);
		SketchLineStyle.SetStrokeWidths(lstrokew);

		// Set the font (this doesn't change size, otherwise the words overlap everywhere, until we have auto-layout).
		// For now we have this hard-coded, the mapping from the name to what you see, to get it up and running.
		// You have to make them match with the following list set above.
		// labstylenames = { "default", "step", "qm", "passage", "area", "cave" };
		assert fontlabs.length == 6;
		fontlabs[0] = new Font("Serif", Font.PLAIN, (int)(fstrokew * 20));
		fontlabs[1] = new Font("Serif", Font.PLAIN, (int)(fstrokew * 20));
		fontlabs[2] = new Font("Serif", Font.PLAIN, (int)(fstrokew * 15));
		fontlabs[3] = new Font("Frankin Gothic Medium", Font.PLAIN, (int)(fstrokew * 40));
		fontlabs[4] = new Font("Frankin Gothic Medium", Font.ITALIC, (int)(fstrokew * 55));
		fontlabs[5] = new Font("Frankin Gothic Medium", Font.BOLD, (int)(fstrokew * 155));
	}


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
	static String SUFF_XML = ".xml";
	static String SUFF_SVX = ".svx";
	static String SUFF_POS = ".pos";
	static String SUFF_SRV = ".srv"; // walls subfile extension
	static String SUFF_TOP = ".top";
	static String SUFF_WALLS = ".prj";
	static String SUFF_VRML = ".wrl";
	static String SUFF_PNG = ".png";
	static String SUFF_JPG = ".jpg";

	static String SUFF_TXT = ".TXT";
	static String SUFF_GIF = ".GIF";

	static String[] SUFF_IGNORE = { "", ".extra", ".old" };

//	public static void main(String args[])
//	{
//		System.out.println("What the hey");
//	}

	static boolean bVerbose = true;

	// message making
	public static void emitMessage(String mess)
	{
		if (bVerbose)
			System.out.println(mess);
	}

	public static void emitWarning(String mess)
	{
		System.out.println(mess);
	}

	public static void emitError(String mess)
	{
		System.out.println("Error: " + mess);
	}

	public static void emitProgError(String mess)
	{
		System.out.println("Programming Error: " + mess);
	}
}

