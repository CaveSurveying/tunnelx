////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.Graphics2D; 
import java.awt.geom.Line2D; 
import java.awt.geom.Point2D; 
import java.awt.geom.Rectangle2D; 
import java.awt.geom.GeneralPath; 
import java.awt.geom.NoninvertibleTransformException; 
import java.awt.geom.AffineTransform; 
import java.util.Vector; 
import java.io.IOException;

import java.awt.BasicStroke; 


//
//
// OnePathNode
//
//



/////////////////////////////////////////////
class OnePathNode
{
	Point2D.Float pn = null; 
	private Rectangle2D.Float pnell = null; // for drawing.  
	float currstrokew = 0.0F; 
	int pathcount; // number of paths which link to this node.  

	// type, label etc. 
	String slabel = null; // station node type if this is not null.  


	/////////////////////////////////////////////
	// notes and sorts the paths coming into this node.  Then adds the links.  
	void SetPathAreaLinks(Vector vpaths) 
	{
		// class  associated to a path.  
		class RefPath 
		{
			OnePath op; 
			boolean bForward; 
			float tangent; 

			RefPath(OnePath lop, boolean lbForward) 
			{
				op = lop; 
				bForward = lbForward; 
				tangent = op.GetTangent(!bForward); 
			}  

			void InsertInto(Vector lrpa) 
			{
				int i; 
				for (i = 0; i < lrpa.size(); i++) 
					if (((RefPath)lrpa.elementAt(i)).tangent > tangent) 
						break; 
				lrpa.insertElementAt(this, i); 
			}
		}		

		// make the vector of references attached to this point in order.  
		Vector rpa = new Vector(); 
		for (int i = 0; i < vpaths.size(); i++) 
		{
			OnePath op = (OnePath)vpaths.elementAt(i); 
			if (op.pnstart == this) 
				(new RefPath(op, false)).InsertInto(rpa); 
			if (op.pnend == this) 
				(new RefPath(op, true)).InsertInto(rpa); 
		}

		// set the link paths circulating around this node.  
		for (int i = 0; i < rpa.size(); i++) 
		{
			RefPath rp = (RefPath)rpa.elementAt(i); 
			RefPath rpnext = (RefPath)rpa.elementAt(i == rpa.size() - 1 ? 0 : i + 1); 

			if (rp.bForward) 
			{
				// facing into the point 
				rp.op.apforeright = rpnext.op; 
				rp.op.bapfrfore = rpnext.bForward; 
			}
			else
			{
				// do the left back case.  
				rp.op.aptailleft = rpnext.op; 
				rp.op.baptlfore = rpnext.bForward; 
			}
		}
	}

	/////////////////////////////////////////////
	Rectangle2D.Float Getpnell()
	{
		if (currstrokew != TN.strokew) 
		{
			currstrokew = TN.strokew; 
			pnell = new Rectangle2D.Float((float)pn.getX() - 2 * currstrokew, (float)pn.getY() - 2 * currstrokew, 4 * currstrokew, 4 * currstrokew);  
		}
		return pnell; 
	}

	/////////////////////////////////////////////
	void SetPos(float x, float y)
	{
		pn = new Point2D.Float(x, y); 
	}

	/////////////////////////////////////////////
	OnePathNode(float x, float y, String lslabel)
	{
		SetPos(x, y); 
		slabel = lslabel; // null unless it's a station node type.  
		pathcount = (slabel == null ? 0 : 1000); 
	}


	/////////////////////////////////////////////
	void WriteSNode(LineOutputStream los) throws IOException
	{
		los.WriteLine("*Sketch_Node " + String.valueOf((float)pn.getX()) + " " + String.valueOf((float)pn.getY()) + (slabel == null ? "" : "  " + slabel));  
	}
}

