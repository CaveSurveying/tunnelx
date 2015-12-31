////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2007  Julian Todd.
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
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color;


/////////////////////////////////////////////
class LineStyleAttr
{
	static int Nlinestyles = 9; // takes in SLS_FILLED

	int linestyle;
	private String sstrokewidth;
	private String sspikegap;
	private String sgapleng;
	private String sspikeheight;
	private String sstrokecolour;

	Color strokecolour;
	float strokewidth;
	float spikegap;
	float gapleng;
	float spikeheight;

	BasicStroke linestroke = null;
    String Dsubsetname = ""; 
    String Duppersubsetname = ""; 

	/////////////////////////////////////////////
	LineStyleAttr(LineStyleAttr lls)
	{
		linestyle = lls.linestyle;
		sstrokewidth = lls.sstrokewidth;
		sspikegap = lls.sspikegap;
		sgapleng = lls.sgapleng;
		sspikeheight = lls.sspikeheight;
		sstrokecolour = lls.sstrokecolour;
        Dsubsetname = lls.Dsubsetname; 
        Duppersubsetname = lls.Duppersubsetname; 
		//System.out.println("sg3 " + sspikegap + " ls " + linestyle);
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle, String lsstrokewidth, String lsspikegap, String lsgapleng, String lsspikeheight, String lsstrokecolour, String lDsubsetname)
	{
		linestyle = llinestyle;
		sstrokewidth = lsstrokewidth;
		sspikegap = lsspikegap;
		sgapleng = lsgapleng;
		sspikeheight = lsspikeheight;
		sstrokecolour = lsstrokecolour;
		//System.out.println("sg2 " + sspikegap + " ls " + linestyle);
        Dsubsetname = lDsubsetname; 
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle, float lstrokewidth, float lspikegap, float lgapleng, float lspikeheight, Color lstrokecolour)
	{
		//assert lstrokecolour != null;
		linestyle = llinestyle;
		strokewidth = lstrokewidth;
		spikegap = lspikegap;
		gapleng = lgapleng;
		spikeheight = lspikeheight;
		strokecolour = lstrokecolour;
        Dsubsetname = "SetStrokeWidthskind"; 
		SetUpBasicStroke();
	}

	/////////////////////////////////////////////
	void Construct(SubsetAttr lsubsetattr, Color defaultcolor)
	{
		strokewidth = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sstrokewidth), (linestyle != SketchLineStyle.SLS_FILLED ? 2.0F : 0.0F));
		spikegap = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sspikegap), 0.0F);
		gapleng = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sgapleng), 0.0F);
		spikeheight = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sspikeheight), 0.0F);
		gapleng = SubsetAttr.ConvertFloat(lsubsetattr.EvalVars(sgapleng), 0.0F);
		strokecolour = SubsetAttr.ConvertColour(lsubsetattr.EvalVars(sstrokecolour), defaultcolor);
        Dsubsetname = lsubsetattr.subsetname; 
		SetUpBasicStroke();
	}

	/////////////////////////////////////////////
	void SetUpBasicStroke()
	{
		if (linestyle == SketchLineStyle.SLS_FILLED)
		{
			if (strokewidth != 0.0F)
				TN.emitWarning("nonzero strokewidth " + strokewidth + " on filled line");
		}
		else
		{
			if (strokewidth == 0.0F && strokecolour != null)
				TN.emitWarning("zero strokewidth on line style; use colour=null; colour was " + strokecolour.toString());
		}
		if (spikeheight != 0.0F)
		{
			if ((linestyle != SketchLineStyle.SLS_PITCHBOUND) && (linestyle != SketchLineStyle.SLS_CEILINGBOUND))
				TN.emitWarning("spikes only on pitch and ceiling bounds please");
		}

		// setup the basicstroke
		if (strokewidth != 0.0F)
		{
			// dotted
			float mitrelimit = strokewidth * 5.0F;
			if ((gapleng != 0.0F) && (spikeheight == 0.0F))
			{
				float[] dash = new float[2];
				dash[0] = spikegap - gapleng;
                if (dash[0] < 0)
                    TN.emitError("Dash phase (spikegap - gaplength) is negative)"); 
				dash[1] = gapleng;
				linestroke = new BasicStroke(strokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, mitrelimit, dash, dash[0] / 2);
			}
			else
				linestroke = new BasicStroke(strokewidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, mitrelimit);
		}
	}

	/////////////////////////////////////////////
	LineStyleAttr(int llinestyle)
	{
		linestyle = llinestyle;
        Dsubsetname = "LineStyleAttr_int_llinestyle"; 
	}
	/////////////////////////////////////////////
	float GetStrokeWidth()
	{
		return strokewidth;
	}
	/////////////////////////////////////////////
	void SetColor(Color lcolour)//Used when you want to override the color, eg when colouring by altitude
	{
		strokecolour = lcolour;
	}
}
