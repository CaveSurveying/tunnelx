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

import java.awt.Graphics; 
import java.awt.GridLayout; 
import java.awt.BorderLayout; 
import java.awt.Event; 
import java.awt.Color; 
import java.awt.Dimension; 

import java.util.Vector; 

import javax.swing.BorderFactory; 
import javax.swing.JFrame; 
import javax.swing.JPanel; 
import javax.swing.JButton; 
import javax.swing.JScrollPane; 

import javax.swing.border.Border; 

import java.awt.event.ActionListener; 
import java.awt.event.ActionEvent; 

import java.io.File; 

//
//
// SectionPreviewDisplay
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
// somehow need to get a scrollbar
class SectionPreviewDisplay extends JPanel // implements MouseListener
{
	Vector butts = new Vector(); 
	ShapeGraphics shapegraphicspanel; 
	int nrows = 6; 
	int ncols = 4; 

	/////////////////////////////////////////////
    SectionPreviewDisplay(ShapeGraphics lshapegraphicspanel) 
	{
		shapegraphicspanel = lshapegraphicspanel; 
// setting to 0 seems to invoke a bug (crashing the java).  
//		setLayout(new GridLayout(0, ncols)); 
		setLayout(new GridLayout(nrows, ncols)); 

		for (int i = 0; i < nrows; i++) 
		for (int j = 0; j < ncols; j++) 
		{
			JButton jb = new JButton(new SPSIcon()); 
			jb.addActionListener(shapegraphicspanel);
			butts.addElement(jb); 
			add(jb); 
		}
	}


	/////////////////////////////////////////////
	// this is to rearrange the rows so that it is all pushed together.  
	void PDUpdate() 
	{
		// work on the butts vector.  

		// check for blank rows 
		boolean bAddBlankRow = true; 
		boolean bDelBlankRow = false; 
		for (int i = nrows - 1; i >= 0; i++) 
		{
			boolean bIsBlankRow = true; 
			for (int j = 0; j < ncols; j++) 
				bIsBlankRow &= (((SPSIcon)(((JButton)(butts.elementAt(i * ncols + j))).getIcon())).pxsection == null);

			if (i != nrows - 1) 
			{
				if (bIsBlankRow && (nrows > 4))
				{
					// move all the rows down to cover this deleted row.  
					for (int j = i * ncols; j < (nrows - 1) * ncols; j++) 
						butts.setElementAt(butts.elementAt(j + ncols), j); 
					nrows--; 
					butts.setSize(nrows * ncols); 
					bDelBlankRow = true; 
				}
			}
			else
				bAddBlankRow = !bIsBlankRow; 
		}

		if (bAddBlankRow) 
		{
			nrows++;
			for (int j = 0; j < ncols; j++) 
			{
				JButton jb = new JButton(new SPSIcon()); 
				jb.addActionListener(shapegraphicspanel); 
				butts.addElement(jb); 
			}
		}

		// rebuild all 
		if (bAddBlankRow || bDelBlankRow) 
		{
			removeAll(); 
			for (int i = 0; i < nrows; i++) 
			for (int j = 0; j < ncols; j++) 
				add((JButton)butts.elementAt(i * ncols + j)); 
			repaint(); // pack even
		}
	}


	/////////////////////////////////////////////
	void SavePrevSections(File pfile)
	{
		OneTunnel prevsectionstunnel = new OneTunnel("XSections", null); 
		for (int i = 0; i < butts.size(); i++) 
		{
			JButton jb = (JButton)butts.elementAt(i); 	
			SPSIcon spsi = (SPSIcon)jb.getIcon(); 

			if (spsi.pxsection != null) 
				prevsectionstunnel.vsections.addElement(spsi.pxsection); 
		}
		TunnelSaver.SaveFilesRoot(prevsectionstunnel, false);   
	}

	/////////////////////////////////////////////
	void LoadPrevSections(File pfile)
	{
		OneTunnel prevsectionstunnel = new OneTunnel("XSections", null); 
		new TunnelLoader(prevsectionstunnel, pfile, null);

		for (int i = 0; i < butts.size(); i++) 
		{
			JButton jb = (JButton)butts.elementAt(i); 	
			SPSIcon spsi = (SPSIcon)jb.getIcon();  

			if (i < prevsectionstunnel.vsections.size()) 
				spsi.SetSection((OneSection)(prevsectionstunnel.vsections.elementAt(i))); 
		}

		repaint(); 
	}
}

