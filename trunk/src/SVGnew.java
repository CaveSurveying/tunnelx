////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2012  Julian Todd.
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

import java.awt.geom.Rectangle2D; 
import java.util.Map;
import java.io.IOException; 

////////////////////////////////////////////////////////////////////////////////
class SVGnew
{
    LineOutputStream los; 
    boolean btransparentbackground; 
    float scalefactor; 
    int irenderingquality; 
    Rectangle2D printrect; 
    Map<String, SubsetAttrStyle> subsetattrstylesmap; 
    
    ////////////////////////////////////////////////////////////////////////////////
    SVGnew(FileAbstraction fa, boolean lbtransparentbackground, float lscalefactor, Rectangle2D lprintrect, int lirenderingquality, Map<String, SubsetAttrStyle> lsubsetattrstylesmap) throws IOException
    {
        los = new LineOutputStream(fa, "UTF-8"); 
        btransparentbackground = lbtransparentbackground; 
        scalefactor = lscalefactor; 
        printrect = lprintrect; 
        irenderingquality = lirenderingquality; 
        subsetattrstylesmap = lsubsetattrstylesmap; 
    }
    
    /////////////////////////////////////////////
	void DrawSketch(OneSketch tsketch) throws IOException
    {
        los.WriteLine("Stuart's new SVG"); 
    }
}
