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

import java.awt.*;
import java.io.*;

import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.Font;

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
	static float strokew = 1.0F;

	static BasicStroke	bsareasel;

	static Font fontlab;
	static Color fontcol = new Color(0.7F, 0.3F, 1.0F);

	// some static paint mode things.
	static TexturePaint texpmud = null;
	static TexturePaint texpboulder = null;

	static Random ran = new Random();
	static LegLineFormat defaultleglineformat = new LegLineFormat();

	static void SetStrokeWidths(float lstrokew)
	{
		strokew = lstrokew;
		emitMessage("New stroke width: " + strokew);
		SketchLineStyle.SetStrokeWidths(lstrokew);

		float[] dash = new float[2];

		dash[0] = strokew;
		dash[1] = strokew;
		bsareasel = new BasicStroke(4.0F * strokew, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 5.0F * strokew, dash, 1.5F * strokew);

		// fish out a font.
		// it's difficult to get the fonts to operate on small scales.
		// might be necessary to change the affine transform and make them bigger
		// and hope they match up in the right places.
        fontlab = new Font(null/*"serif"*/, Font.PLAIN, (int)(strokew * 20));
		//fontlab = fontorg.deriveFont(AffineTransform.getScaleType(strokew, strokew));


		// make the textures (to the right scale).
        Color transcol = new Color(0.0F, 0.0F, 0.0F, 0.0F);

        BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = bi.createGraphics();
        gi.setColor(transcol);
        gi.fillRect(0,0,8,8);
        gi.setColor(new Color(0.4F, 0.0F, 0.0F, 1.0F));
        gi.drawLine(1,2,4,2);
        gi.drawLine(5,6,7,6);
        texpmud = new TexturePaint(bi, new Rectangle2D.Float(0,0,8 * strokew,8 * strokew));

        bi = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        gi = bi.createGraphics();
        gi.setColor(transcol);
        gi.fillRect(0,0,8,8);
        gi.setColor(new Color(0.1F, 0.3F, 0.1F, 1.0F));
        gi.draw(new Ellipse2D.Float(2,2,12,12));
        gi.draw(new Ellipse2D.Float(-2,-2,4,4));
        gi.draw(new Ellipse2D.Float(14,14,4,4));

        texpboulder = new TexturePaint(bi, new Rectangle2D.Float(0,0,16 * strokew,16 * strokew));
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

