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
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;

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
				pld.barea_pres_signal = sketchlinestyle.areasigeffect[pld.iarea_pres_signal];
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
class PathLabelDecode
{
	// if it's a set of symbols
	Vector vlabsymb = new Vector();

	// the area symbol
	int iarea_pres_signal = 0; // combobox lookup
	int barea_pres_signal = 0; // normal

	// the label drawing
	String sfontcode = null;
	LabelFontAttr labfontattr = null;

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

	private String drawlab_bak = null;
	private String[] drawlablns = new String[20];
	private int ndrawlablns = 0;

	// these could be used for mouse click detection (for dragging of labels)
	private float drawlabxoff;
	private float drawlabyoff;
	private float drawlabxwid;
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
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(2, TNXML.sPATHCODES));

		if ((head != null) || (tail != null))
			los.WriteLine(TNXML.xcom(3, TNXML.sCL_STATIONS, TNXML.sCL_TAIL, tail, TNXML.sCL_HEAD, head));
		if (drawlab != null)
		{
			if (barrowpresent || bboxpresent)
				los.WriteLine(TNXML.xcomtext(3, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.sPC_ARROWPRES, (barrowpresent ? "1" : "0"), TNXML.sPC_BOXPRES, (bboxpresent ? "1" : "0"), TNXML.xmanglxmltext(drawlab)));
			else
				los.WriteLine(TNXML.xcomtext(3, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.xmanglxmltext(drawlab)));
		}

		// the area signal
		if (iarea_pres_signal != 0)
			los.WriteLine(TNXML.xcom(3, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, SketchLineStyle.areasignames[iarea_pres_signal]));

		// the symbols
		for (int i = 0; i < vlabsymb.size(); i++)
			los.WriteLine(TNXML.xcom(3, TNXML.sPC_RSYMBOL, TNXML.sLRSYMBOL_NAME, (String)vlabsymb.elementAt(i)));

		los.WriteLine(TNXML.xcomclose(2, TNXML.sPATHCODES));
	}

	/////////////////////////////////////////////
	// used for accessing the fontmetrics function
	static BufferedImage fm_image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	static Graphics fm_g = fm_image.getGraphics();
	void DrawLabel(Graphics2D g2D, float x, float y, int iboxstyle, Font font)
	{
		// backwards compatible default case
		if ((drawlab == null) || (drawlab.length() == 0))
			return;

		// now do the precalculations to split up this string
		if (drawlab_bak != drawlab)
		{
			ndrawlablns = 0;
			rectdef = null;
			int ps = 0;
			while (ndrawlablns < drawlablns.length)
			{
				int pps = drawlab.indexOf('\n', ps);
				if (pps == -1)
				{
					drawlablns[ndrawlablns++] = drawlab.substring(ps);
					break;
				}
				drawlablns[ndrawlablns++] = drawlab.substring(ps, pps);
				ps = pps + 1;
			}
			drawlab_bak = drawlab;
		}

		// we break up the string into lines
		g2D.setFont(font);
		FontMetrics fm = fm_g.getFontMetrics(font);
			// for using few functions from the given graphics2d which may be overwritten but not fully implemented
		float lnspace = fm.getAscent() + 0*fm.getLeading();
		drawlabyhei = lnspace * (ndrawlablns - 1) + fm.getAscent();
		drawlabxwid = 0;
		for (int i = 0; i < ndrawlablns; i++)
			drawlabxwid = Math.max(drawlabxwid, fm.stringWidth(drawlablns[i]));

		// we find the point for the string
		drawlabxoff = -drawlabxwid * (fnodeposxrel + 1) / 2;
		drawlabyoff = drawlabyhei * (fnodeposyrel + 1) / 2;

		// draw the box outline
		if (iboxstyle == 1)
		{
			rectdef = new Rectangle2D.Float(x + drawlabxoff, y + drawlabyoff - lnspace * ndrawlablns, drawlabxwid, lnspace * ndrawlablns);
			g2D.draw(rectdef);
		}

		for (int i = 0; i < ndrawlablns; i++)
			g2D.drawString(drawlablns[ndrawlablns - i - 1], x + drawlabxoff, y - fm.getDescent() + drawlabyoff - lnspace * i);
	}



	/////////////////////////////////////////////
	static float arrowheadlength = 5.0F;
	static float arrowheadwidth = 3.0F;
	static float arrowtailstart = 1.5F;

	/////////////////////////////////////////////
	void DrawArrow(Graphics2D g2D, float x0, float y0, float x1, float y1)
	{
		if ((arrc == null) || (arrc[0] != x0) || (arrc[1] != x1) || (arrc[2] != y0) || (arrc[3] != y1))
		{
			if (arrc == null)
				arrc = new float[4];
			arrc[0] = x0;
			arrc[1] = x1;
			arrc[2] = y0;
			arrc[3] = y1;

			if (arrowdef == null)
				arrowdef = new Line2D.Float[3];

			float xv = x1 - x0;
			float yv = y1 - y0;
			float ln = (float)Math.sqrt(xv * xv + yv * yv);
			if (ln <= arrowtailstart)
				return;
			float xvu = xv / ln;
			float yvu = yv / ln;
			arrowdef[0] = new Line2D.Float(x0 + xvu * arrowtailstart, y0 + yvu * arrowtailstart, x1, y1);
			arrowdef[1] = new Line2D.Float(x1 - xvu * arrowheadlength + yvu * arrowheadwidth, y1 - yvu * arrowheadlength - xvu * arrowheadwidth, x1, y1);
			arrowdef[2] = new Line2D.Float(x1 - xvu * arrowheadlength - yvu * arrowheadwidth, y1 - yvu * arrowheadlength + xvu * arrowheadwidth, x1, y1);
		}

		// actually draw the lines of the arrow
		for (int i = 0; i < arrowdef.length; i++)
			g2D.draw(arrowdef[i]);
	};
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
				g2D.drawString(plabel, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}

		// implements the spread label drawing.
		if ((nlines == 0) || (labspread.length() < 2))
		{
			g2D.drawString(labspread, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
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
			g2D.drawString(labspread.substring(i, i + 1), (float)pt.getX(), (float)pt.getY());
		}
*/



