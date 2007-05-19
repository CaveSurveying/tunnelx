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

//
//
// Vec3
//
//
class Vec3 
{
	public float x; 
	public float y; 
	public float z; 

	/////////////////////////////////////////////
	public Vec3()
	{
		x = 0.0F; 
		y = 0.0F; 
		z = 0.0F; 
	}

	/////////////////////////////////////////////
	public String toString()
	{
		return String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z); 
	}

	/////////////////////////////////////////////
	Vec3(String w0, String w1, String w2)
	{
		SetXYZ(Float.valueOf(w0).floatValue(), Float.valueOf(w1).floatValue(), Float.valueOf(w2).floatValue()); 
	}

	/////////////////////////////////////////////
	public Vec3(float lx, float ly, float lz)
	{
		SetXYZ(lx, ly, lz); 
	}

	/////////////////////////////////////////////
	public boolean isZero()
	{
		if (x == 0 && y == 0 && z == 0) return true;
		else return false;
	}
	
	/////////////////////////////////////////////
	public float Dot(Vec3 a)
	{
		return x * a.x + y * a.y + z * a.z; 
	}

	/////////////////////////////////////////////
	public void Diff(Vec3 a, Vec3 b)
	{
		x = b.x - a.x;
		y = b.y - a.y;
		z = b.z - a.z;
	}

	/////////////////////////////////////////////
	public void Sum(Vec3 a, Vec3 b)
	{
		x = b.x + a.x;
		y = b.y + a.y;
		z = b.z + a.z;
	}

	/////////////////////////////////////////////
	public void PlusEquals(Vec3 a)
	{
		x += a.x; 
		y += a.y; 
		z += a.z; 
	}

	/////////////////////////////////////////////
	public void TimesEquals(float f)
	{
		x *= f; 
		y *= f; 
		z *= f; 
	}

	/////////////////////////////////////////////
	public void Negate()
	{
		x = -x; 
		y = -y; 
		z = -z; 
	}

	/////////////////////////////////////////////
	public void Cross(Vec3 a, Vec3 b)
	{
		x = a.y * b.z - a.z * b.y; 
		y = -a.x * b.z + a.z * b.x; 
		z = a.x * b.y - a.y * b.x; 
	}

	/////////////////////////////////////////////
	public float Len()
	{
		float sqlen = x * x + y * y + z * z; 
		return((float)(Math.sqrt(sqlen))); 
	}

	/////////////////////////////////////////////
	public void Norm()
	{
		float sqlen = x * x + y * y + z * z; 
		float fac = (sqlen == 0.0F ? 1.0F : (float)(1.0F / Math.sqrt(sqlen))); 
		x *= fac; 
		y *= fac; 
		z *= fac; 
	}


	/////////////////////////////////////////////
	public void SetXYZ(float lx, float ly, float lz)
	{
		x = lx; 
		y = ly; 
		z = lz; 
	}

	/////////////////////////////////////////////
	public void SetXYZ(Vec3 a)
	{
		SetXYZ(a.x, a.y, a.z); 
	}

	/////////////////////////////////////////////
	public int ConvCompress(boolean bNegate) 
	{
		float sqlen = x * x + y * y + z * z; 
		float fac = (sqlen == 0.0F ? 1.0F : (float)(1.0F / Math.sqrt(sqlen))) * (bNegate ? -1 : 1); 

		float nx = x * fac; 
		float ny = y * fac; 
		float nz = z * fac; 

		int inx = Math.max(0, Math.min(255, (int)(nx * 127 + (nx > 0.0F ? 0.5F : -0.5F)) + 128)); 
		int iny = Math.max(0, Math.min(255, (int)(ny * 127 + (ny > 0.0F ? 0.5F : -0.5F)) + 128)); 
		int inz = Math.max(0, Math.min(255, (int)(nz * 127 + (nz > 0.0F ? 0.5F : -0.5F)) + 128)); 
		int insz = (nz >= 0.0F ? 0 : 128); 

		int res = (insz | (inx << 8) | (iny << 16) | (inz << 24)); 
		return res; 
	}

	/////////////////////////////////////////////
	void ConvDecompress(int c) 
	{
		int inx = (255 & (c >> 8)); 
		int iny = (255 & (c >> 16)); 
		int inz = (255 & (c >> 24)); 

		x = (inx - 128) / 127.0F; 
		y = (iny - 128) / 127.0F; 
		z = (inz - 128) / 127.0F; 

		Norm(); 
	}

	/////////////////////////////////////////////
	static int ConvCompressInvert(int c)  
	{
		int inx = (255 & (c >> 8)); 
		int iny = (255 & (c >> 16)); 
		int inz = (255 & (c >> 24)); 

		int iinx = 256 -inx; 
		int iiny = 256 - iny; 
		int iinz = 256 - inz; 

		int iinsz = (((c & 128) == 0) ? 128 : 0); 

		int res = (iinsz | (iinx << 8) | (iiny << 16) | (iinz << 24)); 
		return res; 
	}

	/////////////////////////////////////////////
	public void SetAlong(float lambda, Vec3 vf, Vec3 vt)
	{
		float mlam = 1.0F - lambda; 
		SetXYZ(vf.x * mlam + vt.x * lambda, vf.y * mlam + vt.y * lambda, vf.z * mlam + vt.z * lambda); 
	}

	/////////////////////////////////////////////
	public void SetOnSphere(float fx, float fy)
	{
		float dsq = fx * fx + fy * fy; 
		if (dsq > 1.0F)
		{
			float d = (float)(Math.sqrt(dsq));
			SetXYZ(fx / d, fy / d, 0.0F); 
		}
		else
			SetXYZ(fx, fy, (float)(Math.sqrt(1.0F - dsq))); 
	}

	/////////////////////////////////////////////
	public static Vec3 GoLeg(Vec3 vf, Vec3 vl, int sign)
	{
		return new Vec3(vf.x + vl.x * sign, vf.y + vl.y * sign, vf.z + vl.z * sign); 
	}

	/////////////////////////////////////////////
	public static double Arg(float x, float y) 
	{
		double theta; 

		if (x != 0.0F)
		{
			theta = Math.atan(y / x);
			if (x <= 0.0F)  
				theta += Math.PI;
		}
		else  
		{
			if (y >= 0.0F)  
				theta = Math.PI / 2;
			else  
				theta = -Math.PI / 2;
		}

		if (theta < 0.0F)  
			theta += Math.PI * 2;
		if (theta > Math.PI * 2)  
			theta -= Math.PI * 2;
		
		return theta; 
	}

	/////////////////////////////////////////////
	public static float DegArg(float x, float y)
	{
		return (float)(Math.toDegrees(Arg(x, y)));
	}
}

