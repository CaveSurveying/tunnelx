////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.util.Vector; 
import java.io.IOException; 
import java.lang.StringBuffer; 
import java.awt.Rectangle; 
import java.awt.Graphics2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.Shape; 
import java.awt.geom.Point2D; 
import java.awt.geom.AffineTransform; 
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

	// main sketch.  
	Vector vnodes = new Vector(); 
	Vector vpaths = new Vector(); 
	Vector vareas = new Vector(); 

	Vector vsareas = new Vector(); // auto areas
	Vector vssymbols = new Vector(); // symbols

	File fbackgimg = null; 

	File fibackgimg = null;	// the one corresponding to the image
	Image ibackgimg = null; 
	AffineTransform backgimgtrans = new AffineTransform(); 

	// this gets the clockwise auto-area.  
	OneSArea cliparea = null; 

	int changeincvzpS = -1; // used to tell if the symbol has been edited and the vizpaths need redoing.  

	// used for previewing this sketch (when it is a symbol) 
	BufferedImage bisymbol = null; 
	ImageIcon iicon = null; 


	/////////////////////////////////////////////  
	boolean isGroupSymbolType()
	{
		return(vssymbols.size() != 0); 
	}

	/////////////////////////////////////////////
	Icon GetIcon(Dimension csize, OneTunnel vgsymbols) 
	{
		if ((bisymbol == null) || (bisymbol.getWidth() != csize.width) || (bisymbol.getHeight() != csize.height))
		{
			bisymbol = new BufferedImage(csize.width, csize.height, BufferedImage.TYPE_INT_ARGB); 
			iicon = null; 
		}
	
		if (iicon == null) 
		{
			// redraw the buffered image 
			Graphics2D g2d = bisymbol.createGraphics(); 
			g2d.setColor(!isGroupSymbolType() ? Color.lightGray : Color.gray);  
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
			g2d.setTransform(at); 
			paintW(g2d, false, false, vgsymbols, false);  // setting to proper symbols render doesn't seem to help.  
    
			// make the new image icon.  
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
			if ((path.linestyle != SketchLineStyle.SLS_CENTRELINE) && ((bOvWrite || (path == prevselpath)) && g2D.hit(selrect, path.gp, true))) 
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
	void PutSymbolsToAutoAreas(boolean bSymbolEdit) 
	{
		// for each area find if the symbols are in it.  
		for (int i = 0; i < vsareas.size(); i++) 
		{
			// only work on the areas that are relevant.  
			OneSArea osa = (OneSArea)vsareas.elementAt(i); 
			osa.vasymbols.clear(); 
			if (osa.bPathClockwise == bSymbolEdit) 
			{
				if (bSymbolEdit) 
					cliparea = osa; 
				
				// scan the symbols for which ones we are in.  
				for (int j = 0; j < vssymbols.size(); j++)  
				{
					OneSSymbol oss = (OneSSymbol)vssymbols.elementAt(j); 

					// see if any point in the symbol hits the area.  
					int k; 
					for (k = 0; k < oss.slocarea.size(); k++) 
					{
						Point2D ptsarea = (Point2D)oss.slocarea.elementAt(k); 
						if (osa.gparea.contains(ptsarea)) 
							break; 
					}

					// if hits, add to the symbols list.  
					if (k < oss.slocarea.size())  
						osa.vasymbols.addElement(oss); 
				}
			}
System.out.println("aArea " + i + " sym " + osa.vasymbols.size()); 
		}
	}


	/////////////////////////////////////////////
	// fills in the opforeright values etc.  
	// works selectively on a subset of vnodes.  
	void MakeAutoAreas()  
	{
		// set values to null.  esp the area links.  
		for (int i = 0; i < vpaths.size(); i++)  
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			op.aptailleft = null; // not strictly necessary to do.  
			op.apforeright = null; 
			op.karight = null; 
			op.kaleft = null; 
		}

		// kill certain areas.  
		vsareas.clear(); 

		// scan each path node filling in the values.  
		for (int i = 0; i < vnodes.size(); i++) 
			((OnePathNode)vnodes.elementAt(i)).SetPathAreaLinks(vpaths); 

		// now collate the areas.  
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			if (op.karight == null) 
			{
				OneSArea oa = new OneSArea(op, true); // this constructer makes all the links too.  
				if (oa.gparea != null) 
					vsareas.addElement(oa); 
			}
			if (op.kaleft == null) 
			{
				OneSArea oa = new OneSArea(op, false); // this constructer makes all the links too.  
				if (oa.gparea != null) 
					vsareas.addElement(oa); 
			}
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
	void AddPath(OnePath path) 
	{
		if (path.pnstart.pathcount == 0) 
			vnodes.addElement(path.pnstart); 
		path.pnstart.pathcount++; 
		if (path.pnend.pathcount == 0) 
			vnodes.addElement(path.pnend); 
		path.pnend.pathcount++; 
		vpaths.addElement(path); 
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
	// building the centreline sketch  
	/////////////////////////////////////////////
	OnePathNode FindStationPathNode(OneStation os) 
	{
		OnePathNode opn = null; 
		for (int i = 0; i < vnodes.size(); i++) 
		{
			opn = (OnePathNode)(vnodes.elementAt(i)); 
			if ((opn.slabel != null) && opn.slabel.equals(os.name)) 
				break; 
			opn = null; 
		}
		if (opn == null) 
		{
			opn = new OnePathNode(os.Loc.x, -os.Loc.y, os.name); 
			opn.SetPos(os.Loc.x, -os.Loc.y); 
			opn.pathcount = 1000; 
			vnodes.addElement(opn); 
		}

		return opn; 
	}


	/////////////////////////////////////////////
	OneSketch() 
	{
	}

	/////////////////////////////////////////////
	OneSketch(OneTunnel ot) 
	{
		System.out.println("Mapping legs into sketch"); 

		// map in the centreline types.  		
		for (int i = 0; i < ot.vlegs.size(); i++) 
		{
			OneLeg ol = (OneLeg)(ot.vlegs.elementAt(i)); 
			OnePathNode opn0 = FindStationPathNode(ol.osfrom); 
			OnePathNode opn1 = FindStationPathNode(ol.osto); 
			OnePath op = new OnePath(opn0, opn1); 
			AddPath(op); 
		} 
		System.out.println(vnodes.size()); 
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(0, TNXML.sSKETCH)); 

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
	void WriteSketch(LineOutputStream los) throws IOException 
	{
		los.WriteLine("*Sketch " + " { "); 
		los.WriteLine(String.valueOf(vnodes.size()) + "  " + String.valueOf(vpaths.size()) + "  " + String.valueOf(vssymbols.size())); 

		// write out the nodes of the sketch.  (may want to do everything by names).  
		for (int i = 0; i < vnodes.size(); i++)
			((OnePathNode)(vnodes.elementAt(i))).WriteSNode(los); 

		// write out the paths 
		for (int i = 0; i < vpaths.size(); i++)
		{
			OnePath path = (OnePath)(vpaths.elementAt(i)); 
			int ind0 = vnodes.indexOf(path.pnstart); 
			int ind1 = vnodes.indexOf(path.pnend); 
			if ((ind0 != -1) && (ind1 != -1)) 
				path.WritePath(los, ind0, ind1, i); 
			else
				System.out.println("Path_node missing end " + i); 
		}

		// write out the areas.  
		for (int i = 0; i < vssymbols.size(); i++)
		{
			OneSSymbol ssymbol = (OneSSymbol)(vssymbols.elementAt(i)); 
			ssymbol.WriteSSymbol(los); 
		}

		double[] flatmat = new double[6]; 
		backgimgtrans.getMatrix(flatmat); 
		los.WriteLine("*backimgtrans " + String.valueOf(flatmat[0]) + " " + String.valueOf(flatmat[1]) + " " + String.valueOf(flatmat[2]) + " " + String.valueOf(flatmat[3]) + " " + String.valueOf(flatmat[4]) + " " + String.valueOf(flatmat[5]));  

		los.WriteLine("}"); 
	}

	/////////////////////////////////////////////
	OneSketch(LineInputStream lis) throws IOException 
	{
		lis.FetchNextLine(); 

		int nvnodes = Integer.valueOf(lis.w[0]).intValue(); 
		int nvpaths = Integer.valueOf(lis.w[1]).intValue(); 
		int nvssymbols = Integer.valueOf(lis.w[2]).intValue(); 

		// read the nodes of the sketch.  (may want to do everything by names).  
		Vector lvnodes = new Vector(); 
		for (int i = 0; i < nvnodes; i++)
		{
			lis.FetchNextLine(); 
			if (!lis.w[0].equals("*Sketch_Node")) 
				System.out.println("error: *Sketch_Node missing"); 
			OnePathNode opn = new OnePathNode(Float.valueOf(lis.w[1]).floatValue(), Float.valueOf(lis.w[2]).floatValue(), (lis.w[3].equals("") ? null : lis.w[3])); 
			lvnodes.addElement(opn); 

			// it is the centreline type.  
			if (opn.slabel != null) 
				vnodes.addElement(opn); 
		}

		// read the paths 
		for (int i = 0; i < nvpaths; i++)
		{
			lis.FetchNextLine(); 
			if (!lis.w[0].equals("*Path_Sketch")) 
				System.out.println("error: *Path_Sketch missing on lineno " + lis.nlineno); 
			OnePath path = new OnePath(lis, lvnodes); 
			//System.out.println("Path " + i + " co F " + path.pnstart.pathcount + " co T " + path.pnend.pathcount); 
			AddPath(path); 
		}
		//System.out.println("Paths source " + lvnodes.size() + "  paths in " + vnodes.size()); 

		// read the symbols.  
		for (int i = 0; i < nvssymbols; i++)
		{
			lis.FetchNextLine(); 
			if (!lis.w[0].equals("*symbol")) 
				System.out.println("error: *symbol missing:" + lis.w[0]); 
			OneSSymbol ssymbol = new OneSSymbol(lis); 
			vssymbols.addElement(ssymbol); 
		}

		lis.FetchNextLine(); // used to be the "}".  
		// read the back image transform 
		if (lis.w[0].equals("*backimgtrans")) 
		{
			backgimgtrans.setTransform(Double.valueOf(lis.w[1]).doubleValue(), Double.valueOf(lis.w[2]).doubleValue(), Double.valueOf(lis.w[3]).doubleValue(), Double.valueOf(lis.w[4]).doubleValue(), Double.valueOf(lis.w[5]).doubleValue(), Double.valueOf(lis.w[6]).doubleValue()); 
			lis.FetchNextLine(); // the "}".  
		}
	}


	/////////////////////////////////////////////
    public void paintW(Graphics2D g2D, boolean bHideCentreline, boolean bHideMarkers, OneTunnel vgsymbols, boolean bProperSymbolRender) 
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
				path.paintW(g2D, bHideMarkers, false, bProperSymbolRender); 
		}

		// draw all the nodes inactive 
		if (!bHideMarkers && !bProperSymbolRender) 
		{
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_DETAIL]); 
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_DETAIL]); 
			for (int i = 0; i < vnodes.size(); i++) 
			{
				OnePathNode pathnode = (OnePathNode)vnodes.elementAt(i); 
				g2D.draw(pathnode.Getpnell()); 
			}
		}


		// render without clipping.  
		if (!bProperSymbolRender) 
		{
			for (int i = 0; i < vssymbols.size(); i++) 
			{
				OneSSymbol msymbol = (OneSSymbol)vssymbols.elementAt(i); 
				msymbol.paintW(g2D, vgsymbols, !bHideMarkers, false, bProperSymbolRender);  
			}
		}
	}
}; 

