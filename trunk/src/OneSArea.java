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

import java.awt.Graphics2D;
import java.awt.geom.Line2D; 
import java.awt.geom.Area; 
import java.awt.geom.Point2D; 
import java.awt.geom.Ellipse2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.Shape; 
import java.awt.geom.AffineTransform; 
import java.awt.geom.GeneralPath; 
import java.awt.geom.PathIterator; 
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.util.Vector; 
import java.io.IOException;

import java.awt.BasicStroke; 

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.TexturePaint;
import java.awt.Rectangle;

//
//
// OneSArea
//
//



/////////////////////////////////////////////
class OneSArea
{
	// defines the area.
	GeneralPath gparea = null; // if null then nothing should be done with it.
	Area aarea = null;
	float zalt = 0.0F;
	Color zaltcol = Color.white;

	boolean bvisiblebyz = true;

	// array of RefPathO.
	Vector refpaths = new Vector();
	Vector refpathsub = new Vector(); // subselection without the trees.


	// these are used to mark the areas for inclusion.  more efficient than setting false it as a booleans.
	int iamark = 0;
	static int iamarkl = 1;

	/////////////////////////////////////////////
	void SetVisibleByZ()
	{
		bvisiblebyz = true;
		for (int i = 0; i < refpaths.size(); i++)
		{
			OnePath op = ((RefPathO)refpaths.elementAt(i)).op;
			op.bvisiblebyz = true;
			op.pnstart.bvisiblebyz = true;
			op.pnend.bvisiblebyz = true;
		}
	}

	/////////////////////////////////////////////
	void paintHatchW(Graphics2D g2D, int isa, int nsa)
	{
		if (gparea == null)
			return;

		// make the hatch path.
		GeneralPath gphatch;
		{
			gphatch = new GeneralPath();

			// find the region we will make our parallel lines in.
			Rectangle2D r2d = getBounds(null);
			double midx = r2d.getX() + r2d.getWidth() / 2;
			double midy = r2d.getY() + r2d.getHeight() / 2;
			double mrad = Math.sqrt(r2d.getWidth() * r2d.getWidth() + r2d.getHeight() * r2d.getHeight()) / 2;

			double mtheta = Math.PI * isa / nsa + 0.12345F;
			double vx = Math.cos(mtheta);
			double vy = Math.sin(mtheta);

			double sp = TN.strokew * 5.0F;
			int gg = (int)(mrad / sp + 1.0F);
			for (int i = -gg; i <= gg; i++)
			{
				double scx = midx + vy * sp * i;
				double scy = midy - vx * sp * i;

				gphatch.moveTo((float)(scx - vx * mrad), (float)(scy - vy * mrad));
				gphatch.lineTo((float)(scx + vx * mrad), (float)(scy + vy * mrad));
			}
		}


		// we have the hatching path.  now draw it clipped.
		g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]);
		g2D.setColor((isa % 2) == 0 ? Color.blue : Color.cyan);
		g2D.setClip(gparea);

		//g2D.fill(gparea);
		g2D.draw(gphatch);

		g2D.setClip(null);
	}


	// used below in case of cigar shaped area tilted diagonally.
	static AffineTransform at135 = AffineTransform.getRotateInstance(3 * Math.PI / 4);
	/////////////////////////////////////////////
	// general purpose geometric function.
	static boolean FindOrientation(GeneralPath gp) // true if clockwise
	{
		float[] coords = new float[6];

		// loop through the general path now.
		for (int a = 0; a < 2; a++) // this and the next rotation.
		{
			if (a == 1)
				TN.emitMessage("Rotating area by 135 degs to find orientation");
			PathIterator pi = gp.getPathIterator(a == 0 ? null : at135);
			if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO)
			{
				TN.emitMessage("move to not first");
				return false;
			}

			// find the limits box and the orientation.
			// needless initialization
			float xlo = -1;
			int nxlo = -1;
			float xhi = -1;
			int nxhi = -1;
			float ylo = -1;
			int nylo = -1;
			float yhi = -1;
			int nyhi = -1;


			int np = 0;
			while (true)
			{
				pi.next();
				int curvtype = pi.currentSegment(coords);
				if (curvtype == PathIterator.SEG_CLOSE)
					break;

				if ((np == 0) || (coords[0] < xlo))
				{
					xlo = coords[0];
					nxlo = np;
				}

				if ((np == 0) || (coords[0] > xhi))
				{
					xhi = coords[0];
					nxhi = np;
				}

				if ((np == 0) || (coords[1] < ylo))
				{
					ylo = coords[1];
					nylo = np;
				}

				if ((np == 0) || (coords[1] > yhi))
				{
					yhi = coords[1];
					nyhi = np;
				}

				np++;
			}

			// find the dominant direction.
			int npo = (nxlo < nyhi ? 1 : 0) + (nyhi < nxhi ? 1 : 0) + (nxhi < nylo ? 1 : 0) + (nylo < nxlo ? 1 : 0);
			int nne = (nxlo > nyhi ? 1 : 0) + (nyhi > nxhi ? 1 : 0) + (nxhi > nylo ? 1 : 0) + (nylo > nxlo ? 1 : 0);
			TN.emitMessage("rot values pos " + npo + " neg " + nne);

			if (nne >= 2)
				return true;
			if (npo >= 2)
				return false;

		} // loop back and try after rotating by 45 degrees.

		TN.emitWarning("Cannot determin orientation");
		return true;
	}

	/////////////////////////////////////////////
	// this should determin direction
	void RelinkArea()
	{
		if (gparea == null)
			gparea = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		else
			gparea.reset();

		// we should perform the hard task of reflecting certain paths in situ.
		for (int j = 0; j < refpathsub.size(); j++)
		{
			// get the ref path.
			RefPathO refpath = (RefPathO)(refpathsub.elementAt(j));

			if (!refpath.bFore)
			{
				float[] pco = refpath.op.ToCoordsCubic();
				if (pco != null)
				{
					// now put in the reverse coords.
					if (j == 0)
						gparea.moveTo(pco[refpath.op.nlines * 6], pco[refpath.op.nlines * 6 + 1]);

					for (int i = refpath.op.nlines - 1; i >= 0; i--)
					{
						if ((pco[i * 6 + 2] == pco[i * 6 + 4]) && (pco[i * 6 + 4] == pco[i * 6 + 5])) // and the next point too.
							gparea.lineTo(pco[i * 6], pco[i * 6 + 1]);
						else
							gparea.curveTo(pco[i * 6 + 4], pco[i * 6 + 5], pco[i * 6 + 2], pco[i * 6 + 3], pco[i * 6], pco[i * 6 + 1]);
					}
				}
			}
			else
				gparea.append(refpath.op.gp, (j != 0));
		}
		gparea.closePath();
	}




	/////////////////////////////////////////////
	OneSArea(OnePath lop, boolean lbFore) // edge scans to the right
	{
		// loop round to the start.
		OnePath op = lop;
		boolean bFore = lbFore;
		assert lop.AreaBoundingType();

		do
		{
			// gone wrong.
			if (op == null)
			{
				refpaths.clear();
				return;
			}

			refpaths.addElement(new RefPathO(op, bFore));

			// jumps to the next segment on the next node
			OnePathNode opnN = (bFore ? op.pnend : op.pnstart);
			if (bFore)
			{
				assert op.karight == null;
				op.karight = this;
				bFore = !op.bapfrfore;
				op = op.apforeright;
			}
			else
			{
				assert op.kaleft == null;
				op.kaleft = this;
				bFore = !op.baptlfore;
				op = op.aptailleft;
			}
			assert opnN == (bFore ? op.pnstart : op.pnend);

			// go round that segment until we find an area bounding type
			while (!op.AreaBoundingType())
			{
				// mark the non-bounding types anyway, as a start.
				assert(bFore ? op.karight : op.kaleft) == null;
				if (bFore)
					op.karight = this;
				else
					op.kaleft = this;

				if (!bFore)
				{
					bFore = !op.bapfrfore;
					op = op.apforeright;
				}
				else
				{
					bFore = !op.baptlfore;
					op = op.aptailleft;
				}
				assert opnN == (bFore ? op.pnstart : op.pnend);
			}
		}
		while (!((op == lop) && (bFore == lbFore)));


		// now make the refpathsub by copying over and removing duplicates (as we track down the back side of a tree).
		for (int i = 0; i < refpaths.size(); i++)
		{
			OnePath opsi = ((RefPathO)refpaths.elementAt(i)).op;
			OnePath opsl = (refpathsub.isEmpty() ? null : ((RefPathO)refpathsub.lastElement()).op);

			if (opsi != opsl)
				refpathsub.addElement(refpaths.elementAt(i));
			else
				refpathsub.removeElementAt(refpathsub.size() - 1);
		}


		// duplicates between the beginning and the end
		while ((refpathsub.size() >= 2) && (((RefPathO)refpathsub.firstElement()).op == ((RefPathO)refpathsub.lastElement()).op))
		{
			refpathsub.removeElementAt(refpathsub.size() - 1);
			refpathsub.removeElementAt(0);
		}

    	// this builds the general path which defines the area
		// set up the area if something is empty.
		if (refpathsub.isEmpty())
		{
			gparea = null;
			aarea = null;
			return; // think turned out to be just a tree
		}

		RelinkArea();
		aarea = new Area(gparea);

		if (refpathsub.size() != refpaths.size())
			TN.emitMessage("pathedges " + refpathsub.size() + " over total path edges " + refpaths.size());

		// set the zaltitude by finding the average height
		// (altitude must have been set from the linking already)
		float szalt = 0.0F;
		for (int i = 0; i < refpathsub.size(); i++)
			szalt += ((RefPathO)refpathsub.elementAt(i)).ToNode().zalt;
		if (refpathsub.size() != 0)
			zalt = szalt / refpathsub.size();
	}


	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans)
	{
		if (currtrans == null)
			return gparea.getBounds(); 
		GeneralPath gp = (GeneralPath)gparea.clone(); 
		gp.transform(currtrans); 
		return gp.getBounds();
	}
}

