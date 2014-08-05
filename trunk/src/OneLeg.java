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
import java.io.IOException; 
import java.util.List;
import java.util.ArrayList;
            
//
//
// OneLeg
//
//
class OneLeg
{
	// the station names and their pointers
	String stfrom;
	boolean bsurfaceleg = false; 
	String stto;

	String svxtitle;	// used for loading in with subsets on the centreline
	String svxdate;		// used to animate changes in order
	String svxteam;     // used to create attibution text in atlas
	
	OneStation osfrom = null;
	OneStation osto = null;

	// the measured vector (in polar and cartesian)
	boolean bnosurvey = false;
	float tape;
	float compass;
    float backcompass; 

	boolean bUseClino; // depth stuff
	float clino;
	float backclino;
	float fromdepth;
	float todepth;

	boolean bcartesian = false; // sets the vector directly
	boolean btopextendedelevationflip = false; // used for the extended elevation direction
	
    // begins/file_begins
    boolean bfile_begincase; // true *begin (as opposed to *file_begin, so confusing, eh?)
    List<OneLeg> lowerfilebegins = null; // null denotes an ordinary leg
	String includename = null;  // null in case of ordinary leg
	
    OneLeg llcurrentfilebeginblockleg = null; // not null denotes ordinary leg
    
	// the calculated vector
	Vec3 mlegvec = new Vec3();

    final static float INVALID_COMPASSCLINO = -999.0F; 

	/////////////////////////////////////////////
	void SetParasLLF(LegLineFormat llf)
	{
		bsurfaceleg = llf.bsurface; 
		svxtitle = llf.bb_svxtitle; 
		svxdate = llf.bb_svxdate; 
		svxteam = llf.sb_totalteam.toString(); 
	}
	
	/////////////////////////////////////////////
	OneLeg(OneLeg ol)
	{
		stfrom = ol.stfrom;
		bsurfaceleg = ol.bsurfaceleg; 
		stto = ol.stto;
		svxtitle = ol.svxtitle;
		svxdate = ol.svxdate; 
		svxteam = ol.svxteam; 

		osfrom = ol.osfrom;
		osto = ol.osto;
		bnosurvey = ol.bnosurvey;
		tape = ol.tape;
		compass = ol.compass;
        backcompass = ol.backcompass; 

		bUseClino = ol.bUseClino;
		clino = ol.clino;
        backclino = ol.backclino; 
		fromdepth = ol.fromdepth;
		todepth = ol.todepth;

		bcartesian = ol.bcartesian;
		btopextendedelevationflip = ol.btopextendedelevationflip; 
		
		mlegvec = ol.mlegvec;
	}


	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, float ltape, float lcompass, float lbackcompass, float lclino, float lbackclino, LegLineFormat llf)
	{
		stfrom = lstfrom;
		stto = lstto;
		tape = ltape;
		compass = lcompass;
        backcompass = lbackcompass; 
		bUseClino = true;
		clino = lclino;
        backclino = lbackclino; 
		bcartesian = false;

		SetParasLLF(llf); 

		// update from measurments
        if ((lcompass == INVALID_COMPASSCLINO) && (lbackcompass != INVALID_COMPASSCLINO))
            lcompass = lbackcompass + 180.0F; 
        if ((lclino == INVALID_COMPASSCLINO) && (lbackclino != INVALID_COMPASSCLINO))
            lclino = -lbackclino; 
			
		float cc = tape * (float)TN.degcos(lclino); 
		float cz = tape * (float)TN.degsin(lclino); 
		if (!llf.btopextendedelevation)	
			mlegvec.SetXYZ(cc * (float)TN.degsin(lcompass), cc * (float)TN.degcos(lcompass), cz); 
		else
		{
			btopextendedelevationflip = llf.btopextflipleg; 
			mlegvec.SetXYZ(cc, cz, 0.0F); // flipping depends on which direction we come at the edge
		}
	}

	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, LegLineFormat llf)
	{
		stfrom = lstfrom;
		stto = lstto;

		SetParasLLF(llf); 

		bnosurvey = true;
	}

	/////////////////////////////////////////////
	OneLeg(String lstfrom, String lstto, float ltape, float lcompass, float lfromdepth, float ltodepth, LegLineFormat llf)
	{
		stfrom = lstfrom;
		stto = lstto;

		tape = ltape;
		compass = lcompass;
        backcompass = INVALID_COMPASSCLINO; 
		bUseClino = false;
		fromdepth = lfromdepth;
		todepth = ltodepth;
		bcartesian = false;

		SetParasLLF(llf); 

		// update from measurments
		mlegvec.z = todepth - fromdepth;
		float ccsq = tape * tape - mlegvec.z * mlegvec.z;
		float cc = (float)Math.sqrt(ccsq >= 0.0F ? ccsq : 0.0F);
		mlegvec.x = cc * (float)TN.degsin(compass);
		mlegvec.y = cc * (float)TN.degcos(compass);
	}

	/////////////////////////////////////////////
	// cartesian setting, differentiated by rearranging the parameters
	OneLeg(float ldx, float ldy, float ldz, String lstfrom, String lstto, LegLineFormat llf)
	{
		stfrom = lstfrom;
		stto = lstto;

		tape = -1.0F;
		compass = -1.0F;
		bUseClino = false;
		bcartesian = true;

		SetParasLLF(llf); 

		mlegvec.SetXYZ(ldx, ldy, ldz);
	}
    
	/////////////////////////////////////////////
	OneLeg(String lstto, float fx, float fy, float fz, LegLineFormat llf)
	{
		stfrom = null;
		stto = lstto;

		// maybe calculate measure
		tape = -1.0F;
		compass = -1.0F;
		clino = -1.0F;

		SetParasLLF(llf); 
				
		// update from measurments
		mlegvec.SetXYZ(fx, fy, fz);
	}

	/////////////////////////////////////////////
	// fileblocktype
	OneLeg(String lstfrom, String lstto, int uidir, String lincludename)
	{
		stfrom = lstfrom;
		stto = lstto;

		tape = -1.0F;
		compass = -1.0F;
		bUseClino = false;
		bcartesian = true;

		double ldir = uidir*180.0/200; 
		mlegvec.SetXYZ(10*(float)TN.degcos(ldir), 10*(float)TN.degsin(ldir), 0.0F);
        bfile_begincase = (lincludename.equals("--begincase--") || lincludename.equals("--root--")); 
		includename = lincludename; 
        lowerfilebegins = new ArrayList<OneLeg>(); 
	}
    
	/////////////////////////////////////////////
    void SetAvgFileBeginLocRecurse()
    {
        osto.Loc.SetXYZ(0.0F, 0.0F, 0.0F); 
        for (OneLeg ol : lowerfilebegins)
        {
            if (ol.lowerfilebegins != null)
                ol.SetAvgFileBeginLocRecurse(); 
            osto.Loc.PlusEquals(ol.osto.Loc); 
        }
        if (lowerfilebegins.size() != 0)
            osto.Loc.TimesEquals(1.0F/lowerfilebegins.size()); 
    }

	/////////////////////////////////////////////
    float treeposfacx = 3.0F; 
    float treeposfacy = 1.0F; 
    int SetTreeFileBeginLocRecurse(int treeposx, int treeposy)
    {
        osto.Loc.SetXYZ(treeposx*treeposfacx, -treeposy*treeposfacy, 0.0F); 
        if (!((osfrom.Loc.x == 0.0) || (osfrom.Loc.x < osto.Loc.x)))
            TN.emitMessage("same x " + stfrom + " " + stto); 
        int itreeposy = 0; 
        float ysum = 0.0F; 
        float ysumn = 0; 
        for (OneLeg ol : lowerfilebegins)
        {
            if (ol.lowerfilebegins != null)
            {
                itreeposy += ol.SetTreeFileBeginLocRecurse(treeposx+1, treeposy+itreeposy); 
                ysum += ol.osto.Loc.y; 
                ysumn++; 
                itreeposy++; 
            }
        }
        if (ysumn != 0)
            osto.Loc.y = ysum / ysumn; 
        return (itreeposy == 0 ? 0 : itreeposy - 1); 
    }

	/////////////////////////////////////////////
	void paintW(Graphics g, boolean bHighLightActive, DepthCol depthcol)
	{
		// get rid of fixed point vectors
		if (stfrom == null)
			return;

		// get rid of date restrictions
		if ((depthcol != null) && (svxdate.compareTo(depthcol.datelimit) > 0))
			return;

		boolean bHighlight = false; 
		if ((depthcol == null) || bHighlight)
		{
			g.setColor(bHighlight ? TN.wfmpointActive : TN.wfmLeg);
			g.drawLine(osfrom.TLocX, osfrom.TLocY, osto.TLocX, osto.TLocY);
		}

		// funny colors
		else
		{
			// for now do from lowest range.
			// TN.xsgLines :
			float zfrom = osfrom.Loc.z;
			float zto = osto.Loc.z;

			int izfrom = (int)((zfrom - depthcol.zlo) / (depthcol.zhi - depthcol.zlo) * depthcol.znslices);
			if (izfrom < 0)
				izfrom = 0;
			if (izfrom >= depthcol.znslices)
				izfrom = depthcol.znslices - 1;

			g.setColor(depthcol.col[izfrom]);
			g.drawLine(osfrom.TLocX, osfrom.TLocY, osto.TLocX, osto.TLocY);
		}
	}

	public String toString()
	{
		if (lowerfilebegins == null)
			return "OneLeg " + (stfrom == null ? "null" : stfrom) + " " + stto + (bnosurvey ? " nosurvey" : "");
		return ((bfile_begincase || includename.equals("")) ? stto : ("\""+includename+"\""));  // case of use in dmlegstack of DefaultMutableTreeNode
	}
}

