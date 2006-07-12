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

import java.io.StringReader;
import java.io.IOException;
import java.util.Vector;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Color;

import java.awt.geom.Line2D;
//import java.awt.geom.Line2D.Float;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;

// all this nonsense with static classes is horrible.
// don't know the best way for reuse of objects otherwise.
// while keeping PathLabelDecode small so it can be included in every path

// for the different <text style="a-style">label</text> look in TN.java
// under labstylenames and fontlabs

////////////////////////////////////////////////////////////////////////////////
class PathLabelXMLparse extends TunnelXMLparsebase
{
	PathLabelDecode pld;
	SketchLineStyle sketchlinestyle;
	StringBuffer sbtxt = new StringBuffer();

	/////////////////////////////////////////////
	boolean ParseLabel(PathLabelDecode lpld, String lab, SketchLineStyle lsketchlinestyle)
	{
		pld = lpld;
		sketchlinestyle = lsketchlinestyle;

		// default case of no xml commands
		if (lab.indexOf('<') == -1)
		{
			pld.sfontcode = "default";
			pld.drawlab = lab;
			return true;
		}
		return (new TunnelXML()).ParseString(this, lab);
	}

	/////////////////////////////////////////////
	public void startElementAttributesHandled(String name, boolean binlineclose)
	{
		if (name.equals(TNXML.sLRSYMBOL))
		{
			// area type
			String arpres = SeStack(TNXML.sAREA_PRESENT);
			if (arpres != null)
			{
				pld.iarea_pres_signal = 0;
				for (int i = 0; i < sketchlinestyle.nareasignames; i++)
					if (arpres.equals(sketchlinestyle.areasignames[i]))
						pld.iarea_pres_signal = i;
				pld.barea_pres_signal = SketchLineStyle.areasigeffect[pld.iarea_pres_signal];
			}

			// symbol type
			String symbname = SeStack(TNXML.sLRSYMBOL_NAME);
			if (symbname != null)
				pld.vlabsymb.addElement(symbname);
		}

		else if (name.equals(TNXML.sTAIL) || name.equals(TNXML.sHEAD))
			sbtxt.setLength(0);

		else if (name.equals(TNXML.sLTEXT))
		{
			sbtxt.setLength(0);
			pld.sfontcode = SeStack(TNXML.sLTEXTSTYLE);
		}
		else if (name.equals("br"))
			sbtxt.append('\n');
	}

	/////////////////////////////////////////////
	public void characters(String pstr)
	{
		if ((sbtxt.length() != 0) && (sbtxt.charAt(sbtxt.length() - 1) != '\n'))
			sbtxt.append(' ');
		sbtxt.append(pstr);
	}

	/////////////////////////////////////////////
	public void endElementAttributesHandled(String name)
	{
		if (name.equals(TNXML.sHEAD))
			pld.head = sbtxt.toString();
		else if (name.equals(TNXML.sTAIL))
			pld.tail = sbtxt.toString();
		else if (name.equals(TNXML.sLTEXT))
			pld.drawlab = sbtxt.toString();
	}
};


////////////////////////////////////////////////////////////////////////////////
class PathLabelElement
{
	String text;
	float xcelloffset = 0.0F;
	float ycelloffset = 0.0F;
	int yiline;
	boolean bcontinuation = false;
	boolean btextwidthset = false;
	boolean btextheightset = false; 
	float textwidth;
	float textheight; 
	Rectangle2D rect = null;

	PathLabelElement(String ltext)
	{
		// works out if it is a genuine new line
		if (ltext.startsWith(";"))
		{
			bcontinuation = true;
			text = ltext.substring(1);
		}
		else
			text = ltext;

		// extract the width coding of %dd/dddd%
		while (text.indexOf('%') == 0)
		{
			int islashps = text.indexOf('/', 1);
			int ipercps = text.indexOf('%', 1);
			if ((ipercps == -1) || (islashps == -1) || !(islashps < ipercps))
				break; 
			int numstart = 1; 
			boolean bhoriztype = true; 
			if (text.charAt(numstart) == 'v')
			{
				bhoriztype = false; 
				numstart++;
			}
			else if (text.charAt(numstart) == 'h')
				numstart++;
			float textdim = -1.0F; 
				
			// extract the numbers
			try
			{
				float num = (float)Double.parseDouble(text.substring(numstart, islashps));  // compilation error with Float
				float den = (float)Double.parseDouble(text.substring(islashps + 1, ipercps));
				if ((num < 0.0) || (den <= 0.0))
					break; 
				textdim = TN.CENTRELINE_MAGNIFICATION * num / den;
				text = text.substring(ipercps + 1);
			}
			catch (NumberFormatException e)
			{ break; }
			if (bhoriztype)
			{
				btextwidthset = true; 
				textwidth = textdim; 
			}
			else
			{
				btextheightset = true; 
				textheight = textdim; 
			}
		}

		// then a string of %blackrect% or %whiterect% will make the scalebar pieces rather than write the text
	}
};


////////////////////////////////////////////////////////////////////////////////
class PathLabelDecode
{
	// if it's a set of symbols
	Vector vlabsymb = new Vector(); // strings

	// the area symbol
	int iarea_pres_signal = 0; // combobox lookup
	int barea_pres_signal = 0; // 0 normal, 1 dropdown, 2 hole, 3 kill area, 55 sketchframe

	// when barea_pres_signal is 55, sketchframe
	float sfscaledown = 1.0F;
	float sfrotatedeg = 0.0F;
	float sfxtrans = 0.0F;
	float sfytrans = 0.0F;
	String sfsketch;
	String sfstyle;

	// the label drawing
	String sfontcode = null;
	LabelFontAttr labfontattr = null;
	Color color = null;

// could set a font everywhere with the change of the style
	float fnodeposxrel = -1.0F;
	float fnodeposyrel = -1.0F;
	boolean barrowpresent = false;
	boolean bboxpresent = false;
	String drawlab = null;

	// values used by a centreline
	String tail = null;
	String head = null;


	// linesplitting of the drawlabel (using lazy evaluation)
	Vector vdrawlablns = new Vector(); // of type PathLabelElement
	int yilines = 0;

	// these could be used for mouse click detection (for dragging of labels)
	private String drawlab_bak = "";
	private Font font_bak = null;
	Font font = null;
	private float fnodeposxrel_bak;
	private float fnodeposyrel_bak;
	private float x_bak;
	private float y_bak;

	float fmdescent;
	float lnspace;
	float drawlabxoff;
	float drawlabyoff;
	float drawlabxwid;
	private float drawlabyhei;

	private float[] arrc = null; // store of the arrow endpoint coords

	Line2D[] arrowdef = null;
	Rectangle2D rectdef = null;

	/////////////////////////////////////////////
	PathLabelDecode()
	{
	}

	/////////////////////////////////////////////
	public String toString()
	{
		return "tail=" + (tail == null ? "" : tail) + " head=" + (head == null ? "" : head);
	}

	/////////////////////////////////////////////
	PathLabelDecode(PathLabelDecode o)
	{
		iarea_pres_signal = o.iarea_pres_signal;
		barea_pres_signal = o.barea_pres_signal;

		sfscaledown = o.sfscaledown;
		sfrotatedeg = o.sfrotatedeg;
		sfxtrans = o.sfxtrans;
		sfytrans = o.sfytrans;
		sfsketch = o.sfsketch;
		sfstyle = o.sfstyle;

		vlabsymb.addAll(o.vlabsymb);
		drawlab = o.drawlab;
		sfontcode = o.sfontcode;
		fnodeposxrel = o.fnodeposxrel;
		fnodeposyrel = o.fnodeposyrel;
		barrowpresent = o.barrowpresent;
		bboxpresent = o.bboxpresent;
		head = o.head;
		tail = o.tail;
	}

	/////////////////////////////////////////////
	// for backward compatibility.
	static PathLabelXMLparse plxp = new PathLabelXMLparse();
	PathLabelDecode(String lab, SketchLineStyle sketchlinestyle)
	{
		plxp.ParseLabel(this, lab, sketchlinestyle);
	}

	/////////////////////////////////////////////
	// reverse of decoding for saving
	void WriteXML(LineOutputStream los, int indent) throws IOException
	{
		WriteXML(los, indent, true);
	}

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, int indent, boolean pathcodes) throws IOException
	{
		if (pathcodes) los.WriteLine(TNXML.xcomopen(indent, TNXML.sPATHCODES));

		if ((head != null) || (tail != null))
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sCL_STATIONS, TNXML.sCL_TAIL, tail, TNXML.sCL_HEAD, head));
		if (drawlab != null)
		{
			if (barrowpresent || bboxpresent)
				los.WriteLine(TNXML.xcomtext(indent + 1, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.sPC_ARROWPRES, (barrowpresent ? "1" : "0"), TNXML.sPC_BOXPRES, (bboxpresent ? "1" : "0"), TNXML.xmanglxmltext(drawlab)));
			else
				los.WriteLine(TNXML.xcomtext(indent + 1, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.xmanglxmltext(drawlab)));
		}

		// the area signal
		if (iarea_pres_signal != 0)
		{
			if (barea_pres_signal != 55)
				los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, SketchLineStyle.areasignames[iarea_pres_signal]));
			else
				los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, SketchLineStyle.areasignames[iarea_pres_signal], TNXML.sASIG_FRAME_SCALEDOWN, String.valueOf(sfscaledown), TNXML.sASIG_FRAME_ROTATEDEG, String.valueOf(sfrotatedeg), TNXML.sASIG_FRAME_XTRANS, String.valueOf(sfxtrans), TNXML.sASIG_FRAME_YTRANS, String.valueOf(sfytrans), TNXML.sASIG_FRAME_SKETCH, sfsketch, TNXML.sASIG_FRAME_STYLE, sfstyle));
		}

		// the symbols
		for (int i = 0; i < vlabsymb.size(); i++)
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_RSYMBOL, TNXML.sLRSYMBOL_NAME, (String)vlabsymb.elementAt(i)));

		if (pathcodes) los.WriteLine(TNXML.xcomclose(indent, TNXML.sPATHCODES));
	}

	/////////////////////////////////////////////
	// used for accessing the fontmetrics function
	static BufferedImage fm_image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	static Graphics fm_g = fm_image.getGraphics();
	/////////////////////////////////////////////
	static float arrowheadlength = 5.0F;
	static float arrowheadwidth = 3.0F;
	static float arrowtailstart = 1.5F;
	/////////////////////////////////////////////
	void DrawLabel(GraphicsAbstraction ga, float x, float y, float xend, float yend, boolean bsetcol, Color zaltcol, SubsetAttr subsetattr)
	{
		font = (labfontattr == null ? SketchLineStyle.defaultfontlab : labfontattr.fontlab);

		// backwards compatible default case
		if ((drawlab == null) || (drawlab.length() == 0))
			return;

		// find what aspects of the text need updating
		boolean blabelchanged = !drawlab.equals(drawlab_bak);
		boolean bfontchanged = (font_bak != font);
		boolean bposchanged = ((fnodeposxrel_bak != fnodeposxrel) || (fnodeposyrel_bak != fnodeposyrel) || (x_bak != x) || (y_bak != y));

		// break up the label string
		if (blabelchanged)
		{
			vdrawlablns.removeAllElements();
			int ps = 0;
			while (true)
			{
				int pps = drawlab.indexOf('\n', ps);
				String sple = (pps == -1 ? drawlab.substring(ps) : drawlab.substring(ps, pps));
				PathLabelElement ple = 	new PathLabelElement(sple);
				vdrawlablns.addElement(ple);
				if (pps == -1)
					break;
				ps = pps + 1;
			}
			drawlab_bak = drawlab;

			for (int i = 0; i < vdrawlablns.size(); i++)
			{
				PathLabelElement ple = (PathLabelElement)vdrawlablns.elementAt(i);
				if (ple.bcontinuation && (yilines != 0))
					yilines--;
				ple.yiline = yilines;
				yilines++;
			}
		}

		// we break up the string into lines
		FontMetrics fm = (blabelchanged || bfontchanged || bposchanged ? fm_g.getFontMetrics(font) : null);
			// for using few functions from the given GraphicsAbstraction which may be overwritten but not fully implemented
		if (blabelchanged || bfontchanged)
		{
			lnspace = fm.getAscent() + 0*fm.getLeading();
			drawlabyhei = lnspace * (yilines - 1) + fm.getAscent();
			fmdescent = fm.getDescent();

			drawlabxwid = 0.0F;
			drawlabyhei = 0.0F; 
			PathLabelElement pleprev = null;
			for (int i = 0; i < vdrawlablns.size(); i++)
			{
				PathLabelElement ple = (PathLabelElement)vdrawlablns.elementAt(i);
				if (!ple.btextwidthset)
					ple.textwidth = fm.stringWidth(ple.text);
				if (!ple.btextheightset)
					ple.textheight = lnspace; 
				if (ple.bcontinuation && (i != 0))
				{
					ple.xcelloffset = pleprev.xcelloffset + pleprev.textwidth;
					ple.ycelloffset = pleprev.ycelloffset; 
				}
				else
				{
					ple.xcelloffset = 0.0F; 
					ple.ycelloffset = -drawlabyhei; 
					drawlabyhei += ple.textheight; 
				}
				drawlabxwid = Math.max(drawlabxwid, ple.xcelloffset + ple.textwidth);
				pleprev = ple; 
				
System.out.println(":" + i + ":" + ple.textwidth + "~" + ple.textheight + "  " + ple.text); 
			}
			font_bak = font;
		}

		if (blabelchanged || bfontchanged || bposchanged)
		{
			// we find the point for the string
			drawlabxoff = -drawlabxwid * (fnodeposxrel + 1) / 2;
			drawlabyoff = drawlabyhei * (fnodeposyrel - 1) / 2;
			for (int i = 0; i < vdrawlablns.size(); i++)
			{
				PathLabelElement ple = (PathLabelElement)vdrawlablns.elementAt(i);
//				ple.rect = new Rectangle2D.Float(x + drawlabxoff + ple.xcelloffset, y + drawlabyoff - lnspace * (yilines - ple.yiline), ple.textwidth, ple.textheight);
				ple.rect = new Rectangle2D.Float(x + drawlabxoff + ple.xcelloffset, y + drawlabyoff - ple.ycelloffset, ple.textwidth, ple.textheight);
			}

			// should be made by merging the above rectangles
			rectdef = new Rectangle2D.Float(x + drawlabxoff, y + drawlabyoff - drawlabyhei, drawlabxwid, drawlabyhei);

			fnodeposxrel_bak = fnodeposxrel;
			fnodeposyrel_bak = fnodeposyrel;
			x_bak = x;
			y_bak = y;
		}

		// now relay the positions of the lines
		if (barrowpresent && ((arrc == null) || (arrc[0] != x) || (arrc[1] != xend) || (arrc[2] != y) || (arrc[3] != yend)))
		{
			if (arrc == null)
				arrc = new float[4];
			arrc[0] = x;
			arrc[1] = xend;
			arrc[2] = y;
			arrc[3] = yend;

			if (arrowdef == null)
				arrowdef = new Line2D.Float[3];

			float xv = xend - x;
			float yv = yend - y;
			float ln = (float)Math.sqrt(xv * xv + yv * yv);
			if (ln <= arrowtailstart)
				return;
			float xvu = xv / ln;
			float yvu = yv / ln;
			arrowdef[0] = new Line2D.Float(x + xvu * arrowtailstart, y + yvu * arrowtailstart, xend, yend);
			arrowdef[1] = new Line2D.Float(xend - xvu * arrowheadlength + yvu * arrowheadwidth, yend - yvu * arrowheadlength - xvu * arrowheadwidth, xend, yend);
			arrowdef[2] = new Line2D.Float(xend - xvu * arrowheadlength - yvu * arrowheadwidth, yend - yvu * arrowheadlength + xvu * arrowheadwidth, xend, yend);
		}

		color = null;
		//Set color if applicable
		if (bsetcol)
			color = (zaltcol != null ? zaltcol : labfontattr.labelcolour);

		ga.drawlabel(this, subsetattr.linestyleattrs[SketchLineStyle.SLS_DETAIL], x, y);
	}
};


// fancy spread stuff (to be refactored later)
/*
		String labspread = TNXML.xrawextracttext(plabel, TNXML.sSPREAD);
		if (labspread == null)
		{
			int ps = plabel.indexOf(TNXML.sLRSYMBOL);
			int pe = plabel.indexOf("/>");

			// standard label drawing
			// (this shall take <br> and <font> changes)
			if ((ps == -1) || (pe == -1))
				ga.drawString(plabel, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}
, (plabedl.labfontattr == null ? SketchLineStyle.defaultfontlab : plabedl.labfontattr.fontlab)
		// implements the spread label drawing.
		if ((nlines == 0) || (labspread.length() < 2))
		{
			ga.drawString(labspread, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}

		// update the label points only when necessary.
		int currlabelcode = (bSplined ? nlines : -nlines);
		if ((currlabelcode != prevlabelcode) || (vlabelpoints == null) || (vlabelpoints.size() != labspread.length()))
		{
			TN.emitMessage("spreading text");
			prevlabelcode = currlabelcode;

			float[] pco = GetCoords(); // not spline for now.

			// measure lengths
			float[] lengp = new float[nlines + 1];
			lengp[0] = 0.0F;
			for (int i = 1; i <= nlines; i++)
			{
				float xv = pco[i * 2] - pco[i * 2 - 2];
				float yv = pco[i * 2 + 1] - pco[i * 2 - 1];
				lengp[i] = lengp[i - 1] + (float)Math.sqrt(xv * xv + yv * yv);
			}

			// make up the labelpoints array.
			if (vlabelpoints == null)
				vlabelpoints = new Vector();
			vlabelpoints.setSize(labspread.length());

			// find the locations.
			int il = 1;
			for (int j = 0; j < labspread.length(); j++)
			{
				float lenb = lengp[nlines] * j / (labspread.length() - 1);
				while ((il < nlines) && (lengp[il] < lenb))
					il++;

				// find the lambda along this line.
				float lamden = lengp[il] - lengp[il - 1];
				float lam = (lamden != 0.0F ? (lengp[il] - lenb) / lamden : 0.0F);
				float tx = lam * pco[il * 2 - 2] + (1.0F - lam) * pco[il * 2];
				float ty = lam * pco[il * 2 - 1] + (1.0F - lam) * pco[il * 2 + 1];

				if (vlabelpoints.elementAt(j) == null)
					vlabelpoints.setElementAt(new Point2D.Float(), j);
				((Point2D)(vlabelpoints.elementAt(j))).setLocation(tx, ty);
			}
		}

		for (int i = 0; i < labspread.length(); i++)
		{
			Point2D pt = (Point2D)(vlabelpoints.elementAt(i));
			ga.drawString(labspread.substring(i, i + 1), (float)pt.getX(), (float)pt.getY());
		}
*/
