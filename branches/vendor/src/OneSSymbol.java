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
import java.awt.geom.Line2D; 
import java.awt.geom.Point2D; 
import java.awt.geom.AffineTransform; 
import java.awt.geom.GeneralPath; 

import java.io.IOException;



/////////////////////////////////////////////
class OneSSymbol
{
	// arrays of sketch components.  
	String gsymname; 
	private OneSketch gsym; // this is selected by name.  

	// location definition
	Line2D paxis; 
	
	// vector of points located in areas (usually includes endpoints of paxis).  
	Vector slocarea = new Vector(); 


	float coverthresh = 1.0F; // set to 1 means it gets trimmed back.  0 means it is completely removed if any of it covers.  
							  // 0.75 means it is completely removed if less than 1/4 is showing.  


	// lazy evaluation of this in the display.  
	Vector viztranspaths = new Vector(); 
	int changeincvzp = -2; // used to tell if the symbol has been edited and the vizpaths need redoing.  


	// these are threaded into the viztranspaths array and can be found by type.  
	GeneralPath transcliparea = null; 

	// bit of a botch, the size of this array will match the number of nulls in viztranspaths  
	Vector transclipareaSubS = null; // runs in parallel to null entries in the viztranspaths array.  
									 // set only when the symbol type is a group.  


	// could transform the cliparea here too.  

	// this is the transformation from axis of gsym to paxis here.  
	private AffineTransform paxistrans = null; 
	static AffineTransform affscratch = new AffineTransform(); 
	//static AffineTransform id = new AffineTransform(); 

	/////////////////////////////////////////////  
	void BuildAxisTrans() 
	{
		OnePath apath = gsym.GetAxisPath(); 

		double apx = apath.pnend.pn.getX() - apath.pnstart.pn.getX(); 
		double apy = apath.pnend.pn.getY() - apath.pnstart.pn.getY(); 
		double lenap = Math.sqrt(apx * apx + apy * apy); 

		double psx = paxis.getX2() - paxis.getX1(); 
		double psy = paxis.getY2() - paxis.getY1(); 
		double lenps = Math.sqrt(psx * psx + psy * psy); 

		double lenpsap = lenps * lenap; 
		if (lenpsap == 0.0F) 
			return; 

		double dotpsap = (psx * apx + psy * apy) / lenpsap; 
		double dotpspap = (-psx * apy + psy * apx) / lenpsap; 
		double sca = lenps / lenap; 

		paxistrans = new AffineTransform(); 
		paxistrans.setToTranslation(paxis.getX1(), paxis.getY1()); 
		paxistrans.scale(sca, sca); 
		affscratch.setTransform(dotpsap, dotpspap, -dotpspap, dotpsap, 0.0F, 0.0F); 
		paxistrans.concatenate(affscratch); 
		paxistrans.translate(-apath.pnstart.pn.getX(), -apath.pnstart.pn.getY()); 
	}

	/////////////////////////////////////////////  
	AffineTransform GetAxisTrans() 
	{
		if ((paxistrans == null) && (gsym != null)) 
			BuildAxisTrans();  
		return paxistrans; 
	}	

	/////////////////////////////////////////////  
	OneSketch GetGsym(OneTunnel vgsymbols) 
	{
		// find the gsymbol 
		if ((gsym == null) && (vgsymbols != null))  
		{
			for (int j = 0; j < vgsymbols.ndowntunnels; j++)
				if (vgsymbols.downtunnels[j].name.equals(gsymname)) 
				{
					gsym = vgsymbols.downtunnels[j].tsketch; 
					break; 
				}
		}
		return gsym; 
	}

	/////////////////////////////////////////////  
	private void UnionVizTransPaths(OneSketch sgsym, AffineTransform atrans, AffineTransform etrans) 
	{
		for (int j = 0; j < sgsym.vpaths.size(); j++) 
		{
			OnePath path = (OnePath)sgsym.vpaths.elementAt(j); 
			if ((path.linestyle != SketchLineStyle.SLS_CENTRELINE) && (path.linestyle != SketchLineStyle.SLS_INVISIBLE)) 
			{
				OnePath tpath = new OnePath(path); 
				tpath.gp.transform(atrans); 
				if (etrans != null) 
					tpath.gp.transform(etrans); 
				viztranspaths.add(tpath); 
			}
		}	
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(1, TNXML.sSYMBOL, TNXML.sSYMBOL_NAME, gsymname)); 

		los.WriteLine(TNXML.xcomopen(2, TNXML.sSYMBOL_AXIS)); 
		los.WriteLine(TNXML.xcom(3, TNXML.sPOINT, TNXML.sPTX, String.valueOf((float)paxis.getX1()), TNXML.sPTY, String.valueOf((float)paxis.getY1()))); 
		los.WriteLine(TNXML.xcom(3, TNXML.sPOINT, TNXML.sPTX, String.valueOf((float)paxis.getX2()), TNXML.sPTY, String.valueOf((float)paxis.getY2()))); 
		los.WriteLine(TNXML.xcomclose(2, TNXML.sSYMBOL_AXIS)); 

		los.WriteLine(TNXML.xcomopen(2, TNXML.sSYMBOL_AREA_LOC)); 

		// write the pieces.  
		for (int i = 0; i < slocarea.size(); i++) 
		{
			Point2D ptsarea = (Point2D)slocarea.elementAt(i); 
			los.WriteLine(TNXML.xcom(3, TNXML.sPOINT, TNXML.sPTX, String.valueOf((float)ptsarea.getX()), TNXML.sPTY, String.valueOf((float)ptsarea.getY()))); 
		}

		los.WriteLine(TNXML.xcomclose(2, TNXML.sSYMBOL_AREA_LOC)); 

		los.WriteLine(TNXML.xcomopen(1, TNXML.sSYMBOL)); 
	}

	/////////////////////////////////////////////
	void WriteSSymbol(LineOutputStream los) throws IOException
	{
		los.WriteLine("*symbol " + gsymname + "  " + String.valueOf((float)paxis.getX1()) + "  " + String.valueOf((float)paxis.getY1()) + "  " + String.valueOf((float)paxis.getX2()) + "  " + String.valueOf((float)paxis.getY2()) + "  " + String.valueOf(slocarea.size()) + "  {"); 

		// write the pieces.  
		for (int i = 0; i < slocarea.size(); i++) 
		{
			Point2D ptsarea = (Point2D)slocarea.elementAt(i); 
			los.WriteLine(String.valueOf((float)ptsarea.getX()) + "  " + String.valueOf((float)ptsarea.getY())); 
		}

		los.WriteLine("}"); 
	}


	/////////////////////////////////////////////
	// read section
	OneSSymbol(LineInputStream lis) throws IOException 
	{
		// set the axis 
		paxis = new Line2D.Float(Float.valueOf(lis.w[2]).floatValue(), Float.valueOf(lis.w[3]).floatValue(), Float.valueOf(lis.w[4]).floatValue(), Float.valueOf(lis.w[5]).floatValue()); 
		String lgsymname = lis.w[1]; 

		int npco = Integer.valueOf(lis.w[6]).intValue(); 
		for (int i = 0; i < npco; i++) 
		{
			lis.FetchNextLine(); 
			slocarea.addElement(new Point2D.Float(Float.valueOf(lis.w[0]).floatValue(), Float.valueOf(lis.w[1]).floatValue()));  
		}
			
		lis.FetchNextLine(); // the "}".  

		SpecSymbol(lgsymname, null); 
	}




	/////////////////////////////////////////////  
	Vector GetVizTransPaths(OneTunnel vgsymbols) 
	{
		GetGsym(vgsymbols); 
		if (changeincvzp != gsym.changeincvzpS) // used to tell if the symbol has been edited and the vizpaths need redoing.  
		{
			viztranspaths.clear(); 
			changeincvzp = gsym.changeincvzpS; 
System.out.println("Updating symbol " + gsymname + "  ss " + gsym.vssymbols.size()); 

			// symbol group type.  (want to signify in the diagram)  
			if (gsym.isGroupSymbolType())  
			{
				transclipareaSubS = new Vector(); 
				for (int i = 0; i < gsym.vssymbols.size(); i++) 
				{
					OneSSymbol symbol = (OneSSymbol)gsym.vssymbols.elementAt(i); 
					symbol.GetGsym(vgsymbols); 

					// the associative clipping area.  
					if (symbol.gsym.cliparea != null) 
					{
						GeneralPath transclipareaSubS_T = (GeneralPath)symbol.gsym.cliparea.gparea.clone(); 
						transclipareaSubS_T.transform(symbol.GetAxisTrans()); 
						transclipareaSubS_T.transform(GetAxisTrans()); 
						transclipareaSubS.add(transclipareaSubS_T); 
						viztranspaths.add(transclipareaSubS_T); // clip area type.  
					}
					else 
					{
						transclipareaSubS.add(null); 
						viztranspaths.add(null); // clip area type.  
					}

					UnionVizTransPaths(symbol.gsym, symbol.GetAxisTrans(), GetAxisTrans()); 
				}
				transcliparea = null; 
			}

			// single symbol type.  
			else 
			{
				// make the cliparea for the top-level too 
				if (gsym.cliparea != null) 
				{
					transcliparea = (GeneralPath)gsym.cliparea.gparea.clone(); 
					transcliparea.transform(GetAxisTrans()); 
					viztranspaths.add(transcliparea); // clip area type.  
				}
				else 
				{
					transcliparea = null; 
					viztranspaths.add(null); // clip area type.  
				}

				UnionVizTransPaths(gsym, GetAxisTrans(), null); 
			}
		}

		return viztranspaths; 
	}

	/////////////////////////////////////////////  
	void paintW(Graphics2D g2D, OneTunnel vgsymbols, boolean bAxisLine, boolean bActive, boolean bProperSymbolRender) 
	{
		Vector viztranspaths = GetVizTransPaths(vgsymbols); 

		/*if (bProperSymbolRender && (transcliparea != null))  
		{
			g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
			g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
			g2D.draw(transcliparea); 
			g2D.fill(transcliparea); 
		}
		*/

		for (int j = 0; j < viztranspaths.size(); j++) 
		{
			if (viztranspaths.elementAt(j) instanceof OnePath) 
			{
				OnePath tpath = (OnePath)viztranspaths.elementAt(j); 
				if (tpath != null) 
					tpath.paintW(g2D, true, bActive, bProperSymbolRender); 
			}

			// clip area condition  
			else if (viztranspaths.elementAt(j) instanceof GeneralPath) 
			{
				GeneralPath gpcliparea = (GeneralPath)viztranspaths.elementAt(j); 
				if (bProperSymbolRender)  
				{
					g2D.setColor(SketchLineStyle.linestylecols[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
					g2D.setStroke(SketchLineStyle.linestylestrokes[SketchLineStyle.SLS_SYMBOLOUTLINE]); 
					g2D.draw(gpcliparea); 
					g2D.fill(gpcliparea); 
				}
			}

			else 
				System.out.println("horror"); 
		}

		// draw the axis.  
		if (bAxisLine) 
		{
			g2D.setColor(bActive ? SketchLineStyle.linestylecolactive : SketchLineStyle.linestylecols[SketchLineStyle.SLS_CENTRELINE]); 
			g2D.draw(paxis); 
		}
	}


	/////////////////////////////////////////////  
	OneSSymbol(float[] pco, int nlines) 
	{
		paxis = new Line2D.Float(pco[0], pco[1], pco[nlines * 2], pco[nlines * 2 + 1]); 

		for (int i = 0; i <= nlines; i++) 
			slocarea.addElement(new Point2D.Float(pco[i * 2], pco[i * 2 + 1])); 
	}

	/////////////////////////////////////////////  
	void SpecSymbol(String lgsymname, OneSketch lgsym) 
	{
		gsymname = lgsymname; 
		gsym = lgsym; 
		paxistrans = null; 

		// derive the cover threshold 
		int us = gsymname.indexOf('_'); 
		try 
		{
			coverthresh = (us != -1 ? Float.valueOf(gsymname.substring(us + 1)).floatValue() : 0.0F); 
		} 
		catch (NumberFormatException e) 
		{
			System.out.println("Bad number after _"); 
		}
	}
}
