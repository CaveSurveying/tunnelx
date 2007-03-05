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

import javax.swing.JPanel;
import javax.swing.JTextField; 
import javax.swing.JLabel; 

import java.awt.GridLayout; 

import java.util.List; 
import java.util.ArrayList; 


//
//
// LRUDpanel
//
//
public class LRUDpanel extends JPanel
{
	JTextField tfL = new JTextField(); 
	JTextField tfR = new JTextField(); 
	JTextField tfU = new JTextField(); 
	JTextField tfD = new JTextField(); 

	// primary and secondary values, 
	// used for transforming, and recording the values without the active node.  
	float L1, R1, U1, D1; 
	float L2, R2, U2, D2; 

	// constructor
	LRUDpanel()
	{
		setLayout(new GridLayout(0, 4));
		add(new JLabel("Left")); 
		add(new JLabel("Right")); 
		add(new JLabel("Up")); 
		add(new JLabel("Down")); 
		add(tfL); 
		add(tfR); 
		add(tfU); 
		add(tfD); 
	} 

	// the code for putting out nice rounded numbers
	String RoundVal(float fval)
	{
		int ival = (int)(fval * 100.0F + 0.5F); 
		return(String.valueOf((float)ival / 100.0F)); 
	}

	// fill in the textfields (note negation on the L and D fields). 
	void UpdateLRUDtext()
	{
		tfL.setText(RoundVal(-L1)); 
		tfR.setText(RoundVal(R1)); 
		tfU.setText(RoundVal(U1)); 
		tfD.setText(RoundVal(-D1)); 
	}


	// derive the values from the shapegraphics array
	void DeriveSecondaryLRUD(List<ShapeGraphicsPoint> vsgp, ShapeGraphicsPoint sgpactive)  
	{
		boolean bFirstVal = true; 
		for (int i = 0; i < vsgp.size(); i++)
		{
			ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.get(i)); 
			if (sgp != sgpactive)
			{
				if (bFirstVal)
				{
					L2 = sgp.x; 
					R2 = L2; 
					U2 = sgp.y; 
					D2 = U2; 
					bFirstVal = false; 
				}
				else
				{
					L2 = Math.min(L2, sgp.x); 
					R2 = Math.max(R2, sgp.x); 
					U2 = Math.max(U2, sgp.y); 
					D2 = Math.min(D2, sgp.y); 
				}
			}
		}
	}
		
	// basic function
	void DerivePrimaryLRUD(List<ShapeGraphicsPoint> vsgp)  
	{
		DeriveSecondaryLRUD(vsgp, null); 
		L1 = L2; 
		R1 = R2; 
		U1 = U2; 
		D1 = D2; 
		UpdateLRUDtext(); 
	}

	// Function for when we are dragging one point 
	void DeriveDynamicPrimaryLRUD(float x, float y)
	{
		// could in future colour the ones that are extended by this point a different colour.  
		L1 = Math.min(L2, x); 
		R1 = Math.max(R2, x); 
		U1 = Math.max(U2, y); 
		D1 = Math.min(D2, y); 
		UpdateLRUDtext(); 
	}

	// Function for when we are scaling 
	void DeriveDynamicPrimaryLRUDrescale(float rescalex, float rescaley)
	{
		// could in future colour the ones that are extended by this point a different colour.  
		L1 = L2 * rescalex; 
		R1 = R2 * rescalex; 
		U1 = U2 * rescaley; 
		D1 = D2 * rescaley; 
		UpdateLRUDtext(); 
	}

	// Function for when we are dragging 
	void DeriveDynamicPrimaryLRUDtranslate(float xv, float yv)
	{
		// could in future colour the ones that are extended by this point a different colour.  
		L1 = L2 + xv; 
		R1 = R2 + xv; 
		U1 = U2 + yv; 
		D1 = D2 + yv; 
		UpdateLRUDtext(); 
	}

	// function for updating the shape from new LRUD values 
	// and revaluing LRUD if they are no good.  
	void DistortShapeToLRUD(List<ShapeGraphicsPoint> vsgp, boolean bFromSecondary)
	{
		// copy into back values  
		if (!bFromSecondary) 
		{
			L2 = L1; 
			R2 = R1; 
			U2 = U1; 
			D2 = D1; 
		}

		// get values from the text fields 
		try
		{
			L1 = -Float.valueOf(tfL.getText()).floatValue(); 
			R1 = Float.valueOf(tfR.getText()).floatValue(); 
			U1 = Float.valueOf(tfU.getText()).floatValue(); 
			D1 = -Float.valueOf(tfD.getText()).floatValue(); 
		}
		catch (NumberFormatException nfe)
		{
			return; 
		}


		// check and revalue if nonsense 
		if (L1 >= R1)
		{
			L1 = L2; 
			R1 = R2; 
		}
		if (U1 <= D1)
		{
			U1 = U2; 
			D1 = D2; 
		} 

		// now loop through and distort the shape 
		for (int i = 0; i < vsgp.size(); i++)
		{
			ShapeGraphicsPoint sgp = (ShapeGraphicsPoint)(vsgp.get(i)); 
			sgp.x = L1 + (R1 - L1) * ((sgp.x - L2) / (R2 - L2)); 
			sgp.y = D1 + (U1 - D1) * ((sgp.y - D2) / (U2 - D2)); 
		}
	}
}

