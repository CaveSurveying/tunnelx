////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.awt.Graphics; 

//
//
// OneStation
//
//
class OneStation 
{
	// unique identifier
	public String name;  
	OneTunnel utunnel; 

	// location and flag used to set the location
	Vec3 Loc = null; 

	// used to give the index for vrml, and for cross sections in whole survey mode.  
	int vsig; 

	// transformed for viewing points
	public int TLocX = 0; 
	public int TLocY = 0; 
	public int TLocZ = 0; 

	// connections to other legs
	OneLeg olconn[] = null; 
	int njl = 0; 
	
	// position set for calculating location
	boolean bPositionSet = false; 

	/////////////////////////////////////////////
	public OneStation(OneTunnel lutunnel, String lname)
	{
		name = lname; 
		utunnel = lutunnel; 
		vsig = -1; 
	}

	/////////////////////////////////////////////
	float AngDiff(float ang)
	{
		if (ang < 0.0F)
			ang += 360.0F; 
		if (ang > 360.0F)
			ang -= 360.0F; 
		return Math.min(ang, 360.0F - ang); 
	}


	/////////////////////////////////////////////
	void MergeLeg(OneLeg ol)
	{
		if ((olconn == null) || (njl == olconn.length)) 
		{
			OneLeg newolconn[] = new OneLeg[olconn != null ? olconn.length * 2 : 4]; 
			for (int i = 0; i < njl; i++) 
				newolconn[i] = olconn[i]; 
			olconn = newolconn; 
		}
		olconn[njl] = ol; 
		njl++; 
	}

	/////////////////////////////////////////////
	// transformed for viewing points
	void SetTLoc(Matrix3D mat)
	{
		// // fudging things slightly by putting minus signs in here
		TLocX = (int) (Loc.x * mat.xx + Loc.y * mat.xy + Loc.z * mat.xz + mat.xo);
		TLocY = (int) (Loc.x * mat.yx + Loc.y * mat.yy + Loc.z * mat.yz + mat.yo);
		TLocZ = (int) (Loc.x * mat.zx + Loc.y * mat.zy + Loc.z * mat.zz + mat.zo);
	}
			

	/////////////////////////////////////////////
	int sqDist(int mx, int my)
	{
		int dx = TLocX - mx;
		int dy = TLocY - my;
		return dx * dx + dy * dy;
	}


	/////////////////////////////////////////////
	void paintW(Graphics g, boolean bActive, boolean bLong)
	{
		g.setColor(bActive ? TN.wfmpointActive : TN.wfmpointInactive); 
		g.drawRect(TLocX - TN.xsgPointSize, TLocY - TN.xsgPointSize, 2 * TN.xsgPointSize, 2 * TN.xsgPointSize);
		g.setColor(bActive ? TN.wfmnameActive : TN.wfmnameInactive); 
		g.drawString((bLong ? utunnel.fullname + TN.StationDelimeter + name : name), TLocX + TN.xsgPointSize * 2, TLocY + TN.xsgPointSize * 2);
	}
}

