	////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2005  Julian Todd.
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;


/////////////////////////////////////////////
class SketchMakeFrame
{
	float[] gcpreview = null; // previewed caching

	Rectangle2D rect = null;

	float insx, insy;
	boolean binsleft;
	boolean binsup;
	Rectangle2D rectinset = null;

	OnePathNode[] opnc = new OnePathNode[4];
	OnePathNode[] opns = new OnePathNode[4];


	/////////////////////////////////////////////
	void SetDimensions()
	{
    	insx = gcpreview[gcpreview.length - 2];
    	insy = gcpreview[gcpreview.length - 1];

    	binsleft = (insx < rect.getX() + rect.getWidth() / 2);
    	binsup = (insy < rect.getY() + rect.getHeight() / 2);

		float x0 = (binsleft ? (float)rect.getX() : insx);
		float x1 = (binsleft ? insx : (float)(rect.getX() + rect.getWidth()));
		float y0 = (binsup ? (float)rect.getY() : insy);
		float y1 = (binsup ? insy : (float)(rect.getY() + rect.getHeight()));

		if ((x0 < x1) && (y0 < y1))
			rectinset = new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0);
		else
			rectinset = null;
	}


	/////////////////////////////////////////////
	void addpreviewFrame(SketchGraphics sg, OnePath cop)
	{
		float[] gc = cop.GetCoords();
		if (gc != gcpreview)
		{
			gcpreview = gc;
			rect = cop.getBounds(null);
			SetDimensions();
		}
		if (rectinset == null)
			return;

		opnc[0] = new OnePathNode((float)rect.getX(), (float)rect.getY(), 0.0F, false);
		opnc[1] = new OnePathNode((float)(rect.getX() + rect.getWidth()), (float)rect.getY(), 0.0F, false);
		opnc[2] = new OnePathNode((float)(rect.getX() + rect.getWidth()), (float)(rect.getY() + rect.getHeight()), 0.0F, false);
		opnc[3] = new OnePathNode((float)rect.getX(), (float)(rect.getY() + rect.getHeight()), 0.0F, false);

		opns[0] = (binsup ? new OnePathNode(insx, (float)rect.getY(), 0.0F, false) : null);
		opns[1] = (!binsleft ? new OnePathNode((float)(rect.getX() + rect.getWidth()), insy, 0.0F, false) : null);
		opns[2] = (!binsup ? new OnePathNode(insx, (float)(rect.getY() + rect.getHeight()), 0.0F, false) : null);
		opns[3] = (binsleft ? new OnePathNode((float)rect.getX(), insy, 0.0F, false) : null);

		OnePathNode opncen = new OnePathNode(insx, insy, 0.0F, false);

		String sdef = "grid";
		for (int i = 0; i < 4; i++)
		{
			OnePath op = new OnePath(opnc[i]);
			if (opns[i] != null)
			{
				op.EndPath(opns[i]);
				op.linestyle = SketchLineStyle.SLS_WALL;
				op.vssubsets.addElement(sdef);
				sg.AddPath(op);
				op = new OnePath(opns[i]);

				OnePath opc = new OnePath(opns[i]);
				opc.EndPath(opncen);
				opc.linestyle = SketchLineStyle.SLS_WALL;
				opc.vssubsets.addElement(sdef);
				sg.AddPath(opc);
			}
			op.EndPath(opnc[i == 3 ? 0 : i + 1]);
			op.linestyle = SketchLineStyle.SLS_WALL;
			op.vssubsets.addElement(sdef);
			sg.AddPath(op);
		}

    	// do the one label and area signal
		float intx = (insx + (float)rect.getX() + (binsleft ? 0.0F : (float)rect.getWidth())) / 2;
		float inty = (insy + (float)rect.getY() + (binsup ? 0.0F : (float)rect.getHeight()))/ 2;
		OnePathNode opntex = new OnePathNode(intx, inty, 0.0F, false);

		OnePath opt = new OnePath(opntex);
		opt.EndPath(opncen);
		opt.linestyle = SketchLineStyle.SLS_CONNECTIVE;
		opt.vssubsets.addElement(sdef);
		opt.plabedl = new PathLabelDecode();
		opt.plabedl.drawlab = "Drawn with TunnelX\n(www.goatchurch.org.uk)";
		opt.plabedl.sfontcode = (rect.getWidth() < 500.0F ? "titlecreditssmall" : "titlecreditslarge");
		opt.plabedl.fnodeposxrel = 0.0F;
		opt.plabedl.fnodeposyrel = 0.0F;
		sg.AddPath(opt);

		float asglen = (float)rect.getWidth() / 33.0F;
		OnePathNode opnasig = new OnePathNode(insx + (binsleft ? asglen : -asglen), insy + (binsup ? asglen : -asglen), 0.0F, false);
		OnePath opa = new OnePath(opnasig);
		opa.EndPath(opncen);
		opa.linestyle = SketchLineStyle.SLS_CONNECTIVE;
		opa.vssubsets.addElement(sdef);
		opa.plabedl = new PathLabelDecode();
		opa.plabedl.iarea_pres_signal = 1; // pitch hole
		opa.plabedl.barea_pres_signal = sg.sketchdisplay.sketchlinestyle.areasigeffect[opa.plabedl.iarea_pres_signal];
		assert opa.plabedl.barea_pres_signal == 1;  // prove it knocks out the area
		sg.AddPath(opa);
	}



	/////////////////////////////////////////////
	void previewFrame(GraphicsAbstraction ga, OnePath op, LineStyleAttr lsa)
	{
		if (op.nlines != 2)
		{
			TN.emitWarning("Must be previewed with three point line");
			return;
		}
		float[] gc = op.GetCoords();
		if (gc != gcpreview)
		{
			gcpreview = gc;
			rect = op.getBounds(null);
			SetDimensions();
		}
		ga.drawShape(rect, lsa);
		if (rectinset != null)
			ga.drawShape(rectinset, lsa);
	}



	/////////////////////////////////////////////
	/////////////////////////////////////////////

}

	/////////////////////////////////////////////

