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

import java.util.Vector; 
import java.util.Random; 
import java.io.IOException; 
import java.lang.StringBuffer; 
import java.awt.Rectangle; 
import java.awt.Graphics2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.Shape; 
import java.awt.geom.Area; 
import java.awt.geom.Point2D; 
import java.awt.geom.AffineTransform; 
import java.awt.geom.GeneralPath; 
import java.io.IOException;
import java.io.File;

import javax.swing.ImageIcon; 
import javax.swing.Icon; 

import java.awt.Color; 
import java.awt.Dimension; 
import java.awt.Image; 
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;


/////////////////////////////////////////////
class OneSketch
{
	// arrays of sketch components.  
	String sketchname; // used sometimes to build up the file name.  

	// main sketch.  
	Vector vnodes = new Vector(); 
	Vector vpaths = new Vector(); 

	Vector vsareas = new Vector(); // auto areas
	Vector vssymbols = new Vector(); // symbols

	String backgroundimgname; 
	File fbackgimg = null; 

	AffineTransform backgimgtrans = new AffineTransform(); 

	// this gets the clockwise auto-area.  
	OneSArea cliparea = null; 

	// used for previewing this sketch (when it is a symbol) 
	BufferedImage bisymbol = null; 
	ImageIcon iicon = null; 
	boolean bSymbolType = false; // tells us which functions are allowed.  

	File sketchfile = null; 
	boolean bsketchfilechanged = false; 

	// range and restrictions in the display.  
	float zaltlo = 0.0F; 
	float zalthi = 0.0F; 
	boolean bRestrictZalt = false; 
	float rzaltlo = 0.0F; 
	float rzalthi = 0.0F; 

	/////////////////////////////////////////////  
	void SetVisibleByZ(float sllow, float slupp)
	{
		rzaltlo = zaltlo * (1.0F - sllow) + zalthi * sllow; 
		rzalthi = zaltlo * (1.0F - slupp) + zalthi * slupp; 
	
		// set all paths and nodes invisible, except on heights of centreline
		for (int i = 0; i < vnodes.size(); i++) 
		{
			OnePathNode opn = (OnePathNode)vnodes.elementAt(i); 
			if (opn.pnstationlabel != null)
				opn.bvisiblebyz = ((rzaltlo <= opn.zalt) && (rzalthi >= opn.zalt)); 
			else
				opn.bvisiblebyz = false; 
		}

		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			if (op.linestyle == SketchLineStyle.SLS_CENTRELINE)
				op.bvisiblebyz = (op.pnstart.bvisiblebyz || op.pnend.bvisiblebyz); 
			else 
				op.bvisiblebyz = false; 
		}


		// now scan through the areas and set those in range and their components to visible 
		for (int i = 0; i < vsareas.size(); i++) 
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			if ((rzaltlo <= osa.zalt) && (rzalthi >= osa.zalt)) 
				osa.SetVisibleByZ(); 
			else
				osa.bvisiblebyz = false; 
		}
	}


	/////////////////////////////////////////////  
	public String toString() 
	{
		// might give more of a name shortly
		return sketchname; 
	}

	/////////////////////////////////////////////
	Icon GetIcon(Dimension csize, OneTunnel vgsymbols) 
	{
		if (!bSymbolType) 
			TN.emitWarning("symbol icon got in non-symbol sketch"); 

		if ((bisymbol == null) || (bisymbol.getWidth() != csize.width) || (bisymbol.getHeight() != csize.height))
		{
			bisymbol = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_ARGB); 
			iicon = null; 
		}
	
		if (iicon == null) 
		{
			// redraw the buffered image 
			Graphics2D g2d = bisymbol.createGraphics(); 
			g2d.setColor(Color.lightGray);  
			g2d.fillRect(0, 0, csize.width, csize.height); 

			Rectangle2D boundrect = getBounds(null);  

			AffineTransform at = new AffineTransform(); 
			at.setToTranslation(csize.width / 2, csize.height / 2); 
			if (boundrect != null) 
			{
				// scale change
				if ((csize.width != 0) && (csize.height != 0))
				{
					double scchange = Math.max(boundrect.getWidth() / (csize.width * 0.9F), boundrect.getHeight() / (csize.height * 0.9F)); 
					if (scchange != 0.0F)
						at.scale(1.0F / scchange, 1.0F / scchange); 
				}

				at.translate(-(boundrect.getX() + boundrect.getWidth() / 2), -(boundrect.getY() + boundrect.getHeight() / 2)); 
			}

			g2d.transform(at); 
			paintW(g2d, false, false, true, vgsymbols, false);  // setting to proper symbols render doesn't seem to help.  
    
			// make the new image icon.  
			TN.emitMessage("new icon made"); 
			iicon = new ImageIcon(bisymbol); 
		}
		return (Icon)iicon; 
	}

	/////////////////////////////////////////////
	int SelPath(Graphics2D g2D, Rectangle selrect, OnePath prevselpath) 
	{
		boolean bOvWrite = true; 
		OnePath selpath = null; 
		int isel = -1; 
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath path = (OnePath)(vpaths.elementAt(i)); 
			if ((bOvWrite || (path == prevselpath)) && g2D.hit(selrect, path.gp, true)) 
			{
				boolean lbOvWrite = bOvWrite; 
				bOvWrite = (path == prevselpath); 
				if (lbOvWrite)  
				{
					selpath = path; 
					isel = i; 
				}
			}
		}
		return isel; 
	}


	/////////////////////////////////////////////
	void SetBackground(File backgrounddir, String lbackgroundimgname)  
	{
		if ((lbackgroundimgname == null) || lbackgroundimgname.equals(""))  
		{
			backgroundimgname = null; 
			fbackgimg = null; 
		}

		else 
		{
			backgroundimgname = lbackgroundimgname; 
			fbackgimg = new File(backgrounddir, backgroundimgname); 
		}
	}

	/////////////////////////////////////////////
	OnePath GetAxisPath() 
	{
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath path = (OnePath)vpaths.elementAt(i); 
			if (path.linestyle == SketchLineStyle.SLS_CENTRELINE) 
				return path; 
		}
		return null; 
	}


	/////////////////////////////////////////////
	void MakeSymbolLayout()  
	{
		// reset the areas on all the areas  
		for (int i = 0; i < vsareas.size(); i++) 
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			osa.aarea = new Area(osa.gparea); 
		} 

		// go through the symbols and make union of group areas.  
		// we may in future need to do this more combinatorially if the union is inexactly implemented.  
		int ngpareas = 0; 
		for (int j = 0; j < vssymbols.size(); j++)  
		{
			OneSSymbol oss = (OneSSymbol)vssymbols.elementAt(j); 
			if (oss.ossameva == null)  
			{
				oss.saarea = new Area(); 
				for (int k = 0; k < oss.vaareas.size(); k++)  
				{
					OneSArea osa = (OneSArea)oss.vaareas.elementAt(k); 
					oss.saarea.add(osa.aarea); 
				}
				ngpareas++; 
			}
			else 
				oss.saarea = null; 
		}
		TN.emitMessage("Unique group areas: " + ngpareas); 


		// go through the symbols and find their positions and take them out.  
		OneSSymbol.islmarkl++; 
		for (int j = 0; j < vssymbols.size(); j++)  
		{
			OneSSymbol oss = (OneSSymbol)vssymbols.elementAt(j); 
			oss.islmark = OneSSymbol.islmarkl; // comparison against itself.  

			if (oss.gsym != null) 
				oss.RelaySymbolsPosition(); 
		}
	}


	/////////////////////////////////////////////
	// areas know which symbols they render, 
	// and symbols know the union of which areas they must be in.  
	// should it be done by indexes?  
	static Point2D lptsarea = new Point2D.Float(); 
	void PutSymbolToAutoAreas(OneSSymbol oss, int isn, boolean bUpdateSymbolHeight) 
	{
		OneSArea.iamarkl++;		// unique new mark.  

		// check through the points locating the symbol.  
		for (int k = 0; k < oss.slocarea.size(); k++)  
		{
			Vec3 ptsarea = (Vec3)oss.slocarea.elementAt(k); 
			lptsarea.setLocation(ptsarea.x, ptsarea.y); 

			// find the one area which best fits this symbol (by height)  
			OneSArea osa = null; 
			for (int i = 0; i < vsareas.size(); i++) 
			{
				OneSArea losa = (OneSArea)vsareas.elementAt(i); 
				if (losa.gparea.contains(lptsarea)) 
				{
					// or maybe we should be looking for the closest approach to the range.  
					if (((losa.zaltlo <= ptsarea.z) && (losa.zalthi >= ptsarea.z)) || (bUpdateSymbolHeight))  
					{
						osa = losa; 

						// update the syymbol height if that's what's required.  
						if (bUpdateSymbolHeight)  
							ptsarea.z = osa.zalt; 
					}
				}
			}

			// set the mark for streaming in later.  
			if (osa != null) 
				osa.iamark = OneSArea.iamarkl;  
		}


		// now scan through the areas and stick them in the list.  
		oss.vaareas.removeAllElements(); 
		for (int i = 0; i < vsareas.size(); i++) 
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			if (osa.iamark == OneSArea.iamarkl)  
			{
				oss.vaareas.addElement(osa); 
				osa.vasymbols.addElement(oss); 
			}
		}

		// now we want to find which other symbol this area shares its group area with.  
		oss.ossameva = null; 
		for (int j = 0; j < isn; j++)  
		{
			OneSSymbol loss = (OneSSymbol)vssymbols.elementAt(j); 

			// find matching array (could use equals, but don't have an equals function on the areas; it's by pointer.) 
			if ((loss.ossameva == null) && (oss.vaareas.size() == loss.vaareas.size()))  
			{
				int k = oss.vaareas.size() - 1; 
				for ( ; k >= 0; k--)  
				{
					if (oss.vaareas.elementAt(k) != loss.vaareas.elementAt(k))  
						break; 
				}

				// matched.  
				if (k < 0)  
				{
					oss.ossameva = loss; 
					break; 
				}
			}
		}
	}

	/////////////////////////////////////////////
	void PutSymbolsToAutoAreas() 
	{
		// clear the smbol arrays from the autoareas (just to be safe).  
		for (int i = 0; i < vsareas.size(); i++) 
			((OneSArea)vsareas.elementAt(i)).vasymbols.removeAllElements(); 

		// scan the symbols for which ones we are in.  
		for (int j = 0; j < vssymbols.size(); j++)  
		{
			OneSSymbol oss = (OneSSymbol)vssymbols.elementAt(j); 
			PutSymbolToAutoAreas(oss, j, true);  
		}
	}


	/////////////////////////////////////////////
	// inserts by zalt
	void InsertArea(OneSArea oa)  
	{
System.out.println("adding zalt " + oa.zalt); 
		int i = 0; 
		for ( ; i < vsareas.size(); i++) 
		{
			OneSArea loa = (OneSArea)vsareas.elementAt(i); 
			if (loa.zalt > oa.zalt)  
				break; 
		}
		vsareas.insertElementAt(oa, i); 
	}


	/////////////////////////////////////////////
	void ResetZalts()
	{
		boolean bsetzalt = false; 
		zaltlo = 0.0F; 
		zalthi = 0.0F; 
		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++) 
		{
			OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i); 
			if (pathnode.pnstationlabel != null)  
			{
				if (!bsetzalt || (pathnode.zalt < zaltlo))  
					zaltlo = pathnode.zalt; 
				if (!bsetzalt || (pathnode.zalt > zalthi))  
					zalthi = pathnode.zalt; 
				bsetzalt = true; 
			}
		}
	}

	/////////////////////////////////////////////
	void AddArea(Vector lvsareas, OneSArea osa)
	{
		if (osa.gparea == null) 
			return; 

		// the clockwise path is the one bounding the outside.  
		// it will say how many distinct pieces there are.  
		if (OneSArea.FindOrientation(osa.gparea)) 
		{
			if (bSymbolType && (cliparea != null)) 
				TN.emitWarning("More than one outerarea for cliparea in symbol"); 
			cliparea = osa; 
		}

		// remove external types
		else if (!osa.ExorCtype())  
			lvsareas.addElement(osa); 
	}

	/////////////////////////////////////////////
	// fills in the opforeright values etc.  
	// works selectively on a subset of vnodes.  
	void MakeAutoAreas()  
	{
		// set the zalt range from the centreline path nodes.  
		ResetZalts(); 


// get the zalt values set on all the ranges.  
//UpdateZalts(true); // only stations are fixed

		// set values to null.  esp the area links.  
		for (int i = 0; i < vpaths.size(); i++)  
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			op.aptailleft = null; // not strictly necessary to do.  
			op.apforeright = null; 
			op.karight = null; 
			op.kaleft = null; 
		}


		// scan each path node filling in the values.  
		for (int i = 0; i < vnodes.size(); i++) 
			((OnePathNode)vnodes.elementAt(i)).SetPathAreaLinks(vpaths); 

		// the temporary list of areas.  
		Vector lvsareas = new Vector(); 
		cliparea = null; 

		// now collate the areas.  
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			if (op.AreaBoundingType())  
			{
				if (op.karight == null) 
					AddArea(lvsareas, new OneSArea(op, true)); // this constructer makes all the links too.  
				if (op.kaleft == null) 
					AddArea(lvsareas, new OneSArea(op, false)); // this constructer makes all the links too.  
			}
		}  	


		// kill the areas in the list for re-writing  
		vsareas.clear(); 

		// now we attempt to set zheights to all the areas by diffusion 
		int nlvs = 0; 
		boolean bFirsttime = true; 
		do
		{
			nlvs = lvsareas.size(); 
			for (int i = nlvs - 1; i>= 0; i--)  
			{
				OneSArea osa = (OneSArea)lvsareas.elementAt(i); 
				if (osa.SetZaltDiffusion(bFirsttime))  
				{
					lvsareas.removeElementAt(i); 
					InsertArea(osa); 
				}
			}
			bFirsttime = false; 
		}
		while (nlvs > lvsareas.size()); 

		// set the remaining areas to height 0
		while (!lvsareas.isEmpty())  
			InsertArea((OneSArea)lvsareas.remove(lvsareas.size() - 1)); 
		
		// make the range set of the areas 
		// this is all to do with setting the zaltlam variable
		float zaaltlo = 0; 
		float zaalthi = 0; 

		for (int i = 0; i < vsareas.size(); i++) 
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			if ((i == 0) || (osa.zalt < zaaltlo)) 
				zaaltlo = osa.zalt; 
			if ((i == 0) || (osa.zalt > zaalthi)) 
				zaalthi = osa.zalt; 
		}

		float zaaltdiff = zaalthi - zaaltlo; 
		if (zaaltdiff == 0.0F) 
			zaaltdiff = 1.0F;  
		for (int i = 0; i < vsareas.size(); i++) 
		{
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			osa.zaltlam = (osa.zalt - zaaltlo) / zaaltdiff; 

			// spread out a bit.  
			osa.zaltlam = (osa.zaltlam + (float)i / Math.max(1, vsareas.size() - 1)) / 2.0F; 

			// make the shade for the filling in.  
			float greyshade = Math.min(1.0F, osa.zaltlam * 0.4F + 0.4F); 
			osa.zaltcol = new Color(greyshade, greyshade, greyshade, 0.2F); 
		}
	}


	/////////////////////////////////////////////
	int SelSymbol(Graphics2D g2D, Rectangle selrect, OneSSymbol prevselsymbol) 
	{
		boolean bOvWrite = true; 
		OneSSymbol selsymbol = null; 
		int isel = -1; 
		for (int i = 0; i < vssymbols.size(); i++) 
		{
			OneSSymbol symbol = (OneSSymbol)(vssymbols.elementAt(i)); 
			if ((bOvWrite || (symbol == prevselsymbol)) && g2D.hit(selrect, symbol.paxis, true)) 
			{
				boolean lbOvWrite = bOvWrite; 
				bOvWrite = (symbol == prevselsymbol); 
				if (lbOvWrite)  
				{
					selsymbol = symbol; 
					isel = i; 
				}
			}
		}
		return isel; 
	}



	/////////////////////////////////////////////
	OnePathNode SelNode(Graphics2D g2D, Rectangle selrect)  
	{
		OnePathNode res = null; 
		for (int i = 0; i < vnodes.size(); i++) 
		{
			OnePathNode pathnode = (OnePathNode)(vnodes.elementAt(i)); 
			if (g2D.hit(selrect, pathnode.Getpnell(), false)) 
			{
				res = pathnode; 
				break; 
			}
		}
		return res; 
	}


	/////////////////////////////////////////////
	void TrimCountSketchNodes()  
	{
		for (int i = 0; i < vpaths.size(); i++)  
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			op.pnstart.pathcount++; 
			op.pnend.pathcount++; 
		}

		for (int i = vnodes.size() - 1; i >= 0; i--)  
		{
			if (vnodes.elementAt(i) == null)
			{
				TN.emitWarning("Removing missing node"); 
				vnodes.removeElementAt(i); 
			}
			else if (((OnePathNode)vnodes.elementAt(i)).pathcount == 0)  
				TN.emitWarning("Path node count zero"); 
		}
	}


	/////////////////////////////////////////////
	void ReplacePath(int index, OnePath path) 
	{
		// replace the path.  
		OnePath op = (OnePath)vpaths.elementAt(index); 
		vpaths.setElementAt(path, index); 

		// increment the endpoints 
		if (path.pnstart.pathcount == 0) 
			vnodes.addElement(path.pnstart); 
		path.pnstart.pathcount++; 
		if (path.pnend.pathcount == 0) 
			vnodes.addElement(path.pnend); 
		path.pnend.pathcount++; 

		// get the endpoints labeled.  
		path.UpdateStationLabel(bSymbolType); 

		// decrement and remove endpoints 
		op.pnstart.pathcount--; 
		if (op.pnstart.pathcount == 0) 
			vnodes.removeElement(op.pnstart); 
		op.pnend.pathcount--; 
		if (op.pnend.pathcount == 0) 
			vnodes.removeElement(op.pnend); 
	}

	/////////////////////////////////////////////
	int AddPath(OnePath path) 
	{
		if (path.pnstart.pathcount == 0) 
			vnodes.addElement(path.pnstart); 
		path.pnstart.pathcount++; 
		if (path.pnend.pathcount == 0) 
			vnodes.addElement(path.pnend); 
		path.pnend.pathcount++; 
		vpaths.addElement(path); 
		return vpaths.size() - 1; 
	}


	/////////////////////////////////////////////
	void RemovePath(OnePath path) 
	{
		path.pnstart.pathcount--; 
		if (path.pnstart.pathcount == 0) 
			vnodes.removeElement(path.pnstart); 
		path.pnend.pathcount--; 
		if (path.pnend.pathcount == 0) 
			vnodes.removeElement(path.pnend); 
		vpaths.removeElement(path); 
	}



	/////////////////////////////////////////////
	Rectangle2D getBounds(AffineTransform currtrans) 
	{
		Rectangle2D res = null; 
		for (int i = 0; i < vpaths.size(); i++) 
		{
			Rectangle2D lres = ((OnePath)(vpaths.elementAt(i))).getBounds(currtrans); 
			if (res == null) 
				res = lres; 
			else 
				res.add(lres); 
		}
		return res; 
	}


	/////////////////////////////////////////////
	void SetUniqueSketchname(Vector tsketches, String lsketchname) 
	{
		// incomplete.  
		if (tsketches == null) 
			sketchname = "***"; 
		else if (lsketchname != null) 
			sketchname = lsketchname; 
		else 
			sketchname = "sketch" + (tsketches.size()); 

		// ensure no duplicates 
		while (true) 
		{
			int i = (tsketches == null ? -1 : tsketches.size() - 1); 
			while ((i >= 0) && !sketchname.equals(((OneSketch)tsketches.elementAt(i)).sketchname))  
				i--; 
			if (i == -1) 
				return; 
			sketchname = sketchname + "x"; 
		}
	}

	/////////////////////////////////////////////
	OneSketch(Vector tsketches, String lsketchname) 
	{
		SetUniqueSketchname(tsketches, lsketchname); 
	}


	/////////////////////////////////////////////
	// map in the centreline types.  		
	void ImportCentreline(OneTunnel ot) 
	{
		OnePathNode[] statpathnode = new OnePathNode[ot.vstations.size()]; 
		for (int i = 0; i < ot.vlegs.size(); i++) 
		{
			OneLeg ol = (OneLeg)(ot.vlegs.elementAt(i)); 
			if (ol.osfrom != null) 
			{
				int ipns = ot.vstations.indexOf(ol.osfrom); 
				int ipne = ot.vstations.indexOf(ol.osto); 

				if ((ipns != -1) || (ipne != -1))  
				{
					if (statpathnode[ipns] == null)  
						statpathnode[ipns] = new OnePathNode(ol.osfrom.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osfrom.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osfrom.Loc.z * TN.CENTRELINE_MAGNIFICATION, true); 
					if (statpathnode[ipne] == null)  
						statpathnode[ipne] = new OnePathNode(ol.osto.Loc.x * TN.CENTRELINE_MAGNIFICATION, -ol.osto.Loc.y * TN.CENTRELINE_MAGNIFICATION, ol.osto.Loc.z * TN.CENTRELINE_MAGNIFICATION, true); 

					OnePath path = new OnePath(statpathnode[ipns], statpathnode[ipne], TNXML.xcomtext(TNXML.sTAIL, ol.osfrom.name) + TNXML.xcomtext(TNXML.sHEAD, ol.osto.name)); 
					AddPath(path);  
					path.UpdateStationLabel(bSymbolType); 
				}
				else 
					TN.emitWarning("Can't find station " + ol.osfrom + " or " + ol.osto); 
			}
		} 
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		// we default set the sketch condition to unsplined for all edges.  
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSKETCH, TNXML.sSPLINED, "0")); 

		// write out background
		if (backgroundimgname != null)  
		{
			// set the matrix
			double[] flatmat = new double[6]; 
			backgimgtrans.getMatrix(flatmat); 
			los.WriteLine(TNXML.xcomopen(1, TNXML.sAFFINE_TRANSFORM, TNXML.sAFTR_M00, String.valueOf(flatmat[0]), TNXML.sAFTR_M10, String.valueOf(flatmat[1]), TNXML.sAFTR_M01, String.valueOf(flatmat[2]), TNXML.sAFTR_M11, String.valueOf(flatmat[3]), TNXML.sAFTR_M20, String.valueOf(flatmat[4]), TNXML.sAFTR_M21, String.valueOf(flatmat[5]))); 
			los.WriteLine(TNXML.xcom(2, TNXML.sSKETCH_BACK_IMG, TNXML.sSKETCH_BACK_IMG_FILE, backgroundimgname)); 
			los.WriteLine(TNXML.xcomclose(1, TNXML.sAFFINE_TRANSFORM)); 
		}

		// write out the paths.  
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i)); 
			int ind0 = vnodes.indexOf(path.pnstart); 
			int ind1 = vnodes.indexOf(path.pnend); 
			if ((ind0 != -1) && (ind1 != -1)) 
				path.WriteXML(los, ind0, ind1); 
			else
				TN.emitProgError("Path_node missing end " + i); 
		}

		// write out the symbols.  
		for (int i = 0; i < vssymbols.size(); i++)
		{
			OneSSymbol ssymbol = (OneSSymbol)(vssymbols.elementAt(i)); 
			ssymbol.WriteXML(los); 
		}


		los.WriteLine(TNXML.xcomclose(0, TNXML.sSKETCH)); 
	}




	/////////////////////////////////////////////
	static String ExportBetween(OneTunnel tunnsrc, String stat, OneTunnel otdest)  
	{
		OneTunnel ot = tunnsrc; 
		while (ot != otdest)  
		{
			boolean bExported = false; 
			if (stat.indexOf(TN.StationDelimeter) == -1)  
			{
				// check for exports 
				for (int j = 0; j < ot.vexports.size(); j++)
				{
					// this is okay for *fix as long as tunnel non-null (when stotfrom can be).  
					OneExport oe = (OneExport)ot.vexports.elementAt(j); 
					if (stat.equalsIgnoreCase(oe.estation)) 
					{
						stat = oe.ustation; 
						bExported = true; 
						break; 
					}
				}

				if (!bExported)  
					stat = TN.StationDelimeter + stat; 
			}
			else 
				stat = TN.PathDelimeter + stat; 

			if (!bExported)  
				stat = ot.name + stat; 

			ot = ot.uptunnel; 
		}
		return stat; 
	}

	/////////////////////////////////////////////
	boolean ExtractCentrelinePathCorrespondence(OneTunnel thtunnel, Vector clpaths, Vector corrpaths, OneSketch osdest, OneTunnel otdest)  
	{
		// clear the result lists.  
		clpaths.clear(); 
		corrpaths.clear(); 

		if (osdest == this)  
		{
			TN.emitWarning("source and destination sketches the same"); 
			return false; 
		}

		// check that the tunnels go up 
		OneTunnel ot = thtunnel; 
		while (ot != otdest) 
		{
			ot = ot.uptunnel; 
			if (ot == null) 
			{
				TN.emitWarning("source tunnel does not map up to destination tunnel"); 
				return false; 
			}
		}

		// now start matching the centrelines.  
		for (int i = 0; i < vpaths.size(); i++)  
		{
			OnePath path = (OnePath)vpaths.elementAt(i); 
			if ((path.linestyle == SketchLineStyle.SLS_CENTRELINE) && (path.plabel != null))  
			{
				String pnlabtail = TNXML.xrawextracttext(path.plabel, TNXML.sTAIL); 
				String pnlabhead = TNXML.xrawextracttext(path.plabel, TNXML.sHEAD); 
				if ((pnlabtail != null) && (pnlabhead != null))  
				{
					String destpnlabtail = ExportBetween(thtunnel, pnlabtail, otdest); 
					String destpnlabhead = ExportBetween(thtunnel, pnlabhead, otdest); 

					// search for matching centrelines in destination place.  
					OnePath dpath = null; 
					for (int j = 0; j < osdest.vpaths.size(); j++)  					
					{
						OnePath lpath = (OnePath)osdest.vpaths.elementAt(j); 
						if ((lpath.linestyle == SketchLineStyle.SLS_CENTRELINE) && (lpath.plabel != null))  
						{
							String dpnlabtail = TNXML.xrawextracttext(lpath.plabel, TNXML.sTAIL); 
							String dpnlabhead = TNXML.xrawextracttext(lpath.plabel, TNXML.sHEAD); 

							if (destpnlabtail.equals(dpnlabtail) && destpnlabhead.equals(dpnlabhead))  
							{
								if (dpath != null) 
									TN.emitWarning("Ambiguous match of centrelines"); 
								dpath = lpath; 
							}
						}
					}
					
					if (dpath != null)  
					{
						clpaths.addElement(path); 
						corrpaths.addElement(dpath); 
						TN.emitMessage("Corresponding path to " + path.plabel); 
					}
					else 
						TN.emitWarning("No centreline path corresponding to " + path.plabel + "  " + destpnlabtail + " " + destpnlabhead); 
				}
			}
		}

		// false if no correspondence
		if (clpaths.size() == 0)  
		{
			TN.emitWarning("no corresponding centrelines found"); 
			return false; 
		}
		return true; 
	}


	/////////////////////////////////////////////
	void ImportDistorted(OneSketch isketch, Vector clpaths, Vector corrpaths)  
	{
		// the weights for the paths.  
		PtrelLn ptrelln = new PtrelLn(clpaths, corrpaths); 
		ptrelln.Extendallnodes(isketch.vnodes);   

		// warping over the paths 
		for (int i = 0; i < isketch.vpaths.size(); i++) 
		{
			OnePath path = (OnePath)isketch.vpaths.elementAt(i); 
			if (path.linestyle != SketchLineStyle.SLS_CENTRELINE)  
				AddPath(ptrelln.WarpPath(path));  
		}

		// warping over the symbols.  
		for (int i = 0; i < isketch.vssymbols.size(); i++) 
			vssymbols.addElement(ptrelln.WarpSymbol((OneSSymbol)isketch.vssymbols.elementAt(i))); 
	}

	/////////////////////////////////////////////
	// no better way of dealing with the problem.  
	void UpdateZalts(boolean bFromStationsOnly)  
	{
		// set all the unset zalts
		for (int i = 0; i < vnodes.size(); i++) 
		{
			OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i); 
			if (!pathnode.bzaltset || (bFromStationsOnly && (pathnode.pnstationlabel == null)))  
			{
				// find closest node
				int jc = -1; 
				float pndsq = -1.0F; 
				for (int j = 0; j < vnodes.size(); j++) 
				{
					if (j != i)
					{
						OnePathNode lpathnode = (OnePathNode)vnodes.elementAt(j); 
						if (!bFromStationsOnly || (lpathnode.pnstationlabel != null))
						{
							float dx = (float)(pathnode.pn.getX() - lpathnode.pn.getX()); 
							float dy = (float)(pathnode.pn.getY() - lpathnode.pn.getY()); 
							float lpndsq = dx * dx + dy * dy; 
							if ((jc == -1) || (lpndsq < pndsq))  
							{
								jc = j; 
								pndsq = lpndsq; 
							}
						}
					}
				}

				if (jc != -1)  
				{
					OnePathNode lpathnode = (OnePathNode)vnodes.elementAt(jc); 
					pathnode.zalt = lpathnode.zalt; 
					pathnode.bzaltset = true; 
				}
			}
		}
	}


	/////////////////////////////////////////////
	GeneralPath gpgrid = new GeneralPath(); 
	float ngridspacing = 0.0F; 
	Point2D ptsgrid = new Point2D.Float(); 
	String strgrid = ""; 

	public void GenerateMetreGrid(Point2D.Float gridscrcorner, float gridscrrad, Point2D.Float gridscrmid)  
	{
		gpgrid.reset(); 

		float pngridspacing = ngridspacing; 
		ngridspacing = TN.CENTRELINE_MAGNIFICATION; // the size of one metre.  
		for (int s = 0; s < 5; s++) 
		{
			if (gridscrrad / ngridspacing < TN.MAX_GRIDLINES)  
				break; 
			ngridspacing *= ((s % 2) == 0 ? 5 : 2);  //  up by fives and tens.  
		}
		if (pngridspacing != ngridspacing) 
			strgrid = String.valueOf((int)(ngridspacing / TN.CENTRELINE_MAGNIFICATION + 0.5F)); 

		int imx = (int)(gridscrcorner.getX() / ngridspacing); 
		int imy = (int)(gridscrcorner.getY() / ngridspacing); 
		int nglines = Math.min(TN.MAX_GRIDLINES, (int)(gridscrrad / ngridspacing + 1.0F)); 

		float xs = ngridspacing * imx; 
		float ys = ngridspacing * imy; 

		for (int i = 0; i <= nglines; i++)  
		{
			float yl = ngridspacing * (imy + i); 
			gpgrid.moveTo((float)gridscrcorner.getX() - gridscrrad, yl); 
			gpgrid.lineTo((float)gridscrcorner.getX() + gridscrrad, yl); 

			gpgrid.moveTo((float)gridscrcorner.getX() - gridscrrad, ngridspacing * (imy - i - 1)); 
			gpgrid.lineTo((float)gridscrcorner.getX() + gridscrrad, ngridspacing * (imy - i - 1)); 

			float xl = ngridspacing * (imx + i); 
			gpgrid.moveTo(xl, (float)gridscrcorner.getY() - gridscrrad); 
			gpgrid.lineTo(xl, (float)gridscrcorner.getY() + gridscrrad); 

			gpgrid.moveTo(ngridspacing * (imx - i - 1), (float)gridscrcorner.getY() - gridscrrad); 
			gpgrid.lineTo(ngridspacing * (imx - i - 1), (float)gridscrcorner.getY() + gridscrrad); 

			if (xl < gridscrmid.getX()) 
				xs = xl; 
			if (yl < gridscrmid.getY()) 
				ys = yl; 
		}

		ptsgrid.setLocation(xs - ngridspacing * 0.5F - 2 * TN.strokew * strgrid.length(), ys - TN.strokew * 0.05F); 
		//TN.emitMessage("Gridspacing " + ngridspacing + "  " + strgrid + "  nglines " + nglines); 
	}


	/////////////////////////////////////////////
	public void DrawMetreGrid(Graphics2D g2D)  
	{
		// we will be able to draw this clipped to the outside of all the other geometry drawn, I hope.  
		// so it's like the nice background in svx.  

		g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_CENTRELINE]); // thin
		g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_FILLED]); // black
		g2D.draw(gpgrid); 
		if (strgrid.length() != 0) 
			g2D.drawString(strgrid, (float)ptsgrid.getX(), (float)ptsgrid.getY()); 
	}


	/////////////////////////////////////////////
	public void paintW(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, boolean bHideStationNames, OneTunnel vgsymbols, boolean bProperSymbolRender) 
	{
		// draw all ssymbols inactive
		// render within each area, clipped.  
		if (bProperSymbolRender) 
		{
			// the clip has to be reset for printing otherwise it crashes.  
			// this is not how it should be according to the spec
			Shape sclip = g2D.getClip(); 
			for (int i = 0; i < vsareas.size(); i++)  
			{
				OneSArea osa = (OneSArea)vsareas.elementAt(i); 
				osa.paintWsymbols(g2D, vgsymbols); 
				g2D.setClip(sclip); 
			}
		}

		// draw all the paths inactive.  
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath path = (OnePath)(vpaths.elementAt(i)); 
			if (!bHideCentreline || (path.linestyle != SketchLineStyle.SLS_CENTRELINE)) 
			{
				if (!bRestrictZalt || path.bvisiblebyz)  
					path.paintW(g2D, bHideMarkers, false, bProperSymbolRender); 
			}
		}

		// draw all the nodes inactive 
		if (!bHideMarkers && !bProperSymbolRender) 
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]); 
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_DETAIL]); 
			for (int i = 0; i < vnodes.size(); i++) 
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i); 
				if (!bRestrictZalt || pathnode.bvisiblebyz) 
					g2D.draw(pathnode.Getpnell()); 
			}
		}

		// draw all the station names inactive
		if (!bHideStationNames) 
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]); 
			if (!bProperSymbolRender)  
				g2D.setColor(TN.fontcol); 
			for (int i = 0; i < vnodes.size(); i++) 
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i); 
				if (pathnode.pnstationlabel != null) 
				{
					if (!bRestrictZalt || pathnode.bvisiblebyz) 
						g2D.drawString(pathnode.pnstationlabel, (float)pathnode.pn.getX() + TN.strokew * 2, (float)pathnode.pn.getY() - TN.strokew); 
				}
			}
		}


		// render without clipping.  
		if (!bProperSymbolRender) 
		{
			for (int i = 0; i < vssymbols.size(); i++) 
			{
				OneSSymbol msymbol = (OneSSymbol)vssymbols.elementAt(i); 
				msymbol.paintW(g2D, !bHideMarkers, false, bProperSymbolRender);  
			}
		}

		// shade in the areas according to depth 
		if (!bProperSymbolRender) 
		{
			for (int i = 0; i < vsareas.size(); i++)  
			{
				OneSArea osa = (OneSArea)vsareas.elementAt(i); 
				if (!bRestrictZalt || osa.bvisiblebyz) 
				{
					g2D.setColor(osa.zaltcol); 
					g2D.fill(osa.gparea); 
				}
			}
		}
	}
}; 


