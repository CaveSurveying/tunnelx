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
// Matrix3D
//
//
class Matrix3D 
{
    public float xx = 1.0F, xy = 0.0F, xz = 0.0F,  xo = 0.0F;
    public float yx = 0.0F, yy = 1.0F, yz = 0.0F,  yo = 0.0F;
    public float zx = 0.0F, zy = 0.0F, zz = 1.0F,  zo = 0.0F;


	/////////////////////////////////////////////
    public void SetFrom(Matrix3D mat) 
	{
		xx = mat.xx;  xy = mat.xy;  xz = mat.xz;  xo = mat.xo; 
		yx = mat.yx;  yy = mat.yy;  yz = mat.yz;  yo = mat.yo; 
		zx = mat.zx;  zy = mat.zy;  zz = mat.zz;  zo = mat.zo; 
	}

	/////////////////////////////////////////////
    public void scale(float xf, float yf, float zf) 
	{
		xx *= xf; xy *= xf; xz *= xf;  xo *= xf;
		yx *= yf; yy *= yf; yz *= yf;  yo *= yf;
		zx *= zf; zy *= zf; zz *= zf;  zo *= zf;
    }

	/////////////////////////////////////////////
    public void translate(float x, float y, float z) 
	{
		xo += x;
		yo += y;
		zo += z;
    }


	/////////////////////////////////////////////
    public void yrot(double theta) 
	{
		double ct = Math.cos(theta);
		double st = Math.sin(theta);

		float Nxx = (float) (xx * ct + zx * st);
		float Nxy = (float) (xy * ct + zy * st);
		float Nxz = (float) (xz * ct + zz * st);
		float Nxo = (float) (xo * ct + zo * st);

		float Nzx = (float) (zx * ct - xx * st);
		float Nzy = (float) (zy * ct - xy * st);
		float Nzz = (float) (zz * ct - xz * st);
		float Nzo = (float) (zo * ct - xo * st);

		xx = Nxx; xy = Nxy; xz = Nxz;  xo = Nxo; 
		zx = Nzx; zy = Nzy; zz = Nzz;  zo = Nzo;
    }

	/////////////////////////////////////////////
    public void xrotdeg(double theta) 
	{
		double ct = TN.degcos(theta);
		double st = TN.degsin(theta);

		float Nyx = (float) (yx * ct + zx * st);
		float Nyy = (float) (yy * ct + zy * st);
		float Nyz = (float) (yz * ct + zz * st);
		float Nyo = (float) (yo * ct + zo * st);

		float Nzx = (float) (zx * ct - yx * st);
		float Nzy = (float) (zy * ct - yy * st);
		float Nzz = (float) (zz * ct - yz * st);
		float Nzo = (float) (zo * ct - yo * st);

		yx = Nyx; yy = Nyy; yz = Nyz;  yo = Nyo;
		zx = Nzx; zy = Nzy; zz = Nzz;  zo = Nzo;
    }


	/////////////////////////////////////////////
    public void zrotdeg(double theta) 
	{
		double ct = TN.degcos(theta);
		double st = TN.degsin(theta);

		float Nyx = (float) (yx * ct + xx * st);
		float Nyy = (float) (yy * ct + xy * st);
		float Nyz = (float) (yz * ct + xz * st);
		float Nyo = (float) (yo * ct + xo * st);

		float Nxx = (float) (xx * ct - yx * st);
		float Nxy = (float) (xy * ct - yy * st);
		float Nxz = (float) (xz * ct - yz * st);
		float Nxo = (float) (xo * ct - yo * st);

		yx = Nyx; yy = Nyy; yz = Nyz;  yo = Nyo;
		xx = Nxx; xy = Nxy; xz = Nxz;  xo = Nxo;
    }


    void mult(Matrix3D rhs) 
	{
		float lxx = xx * rhs.xx + yx * rhs.xy + zx * rhs.xz;
		float lxy = xy * rhs.xx + yy * rhs.xy + zy * rhs.xz;
		float lxz = xz * rhs.xx + yz * rhs.xy + zz * rhs.xz;
		float lxo = xo * rhs.xx + yo * rhs.xy + zo * rhs.xz + rhs.xo;

		float lyx = xx * rhs.yx + yx * rhs.yy + zx * rhs.yz;
		float lyy = xy * rhs.yx + yy * rhs.yy + zy * rhs.yz;
		float lyz = xz * rhs.yx + yz * rhs.yy + zz * rhs.yz;
		float lyo = xo * rhs.yx + yo * rhs.yy + zo * rhs.yz + rhs.yo;

		float lzx = xx * rhs.zx + yx * rhs.zy + zx * rhs.zz;
		float lzy = xy * rhs.zx + yy * rhs.zy + zy * rhs.zz;
		float lzz = xz * rhs.zx + yz * rhs.zy + zz * rhs.zz;
		float lzo = xo * rhs.zx + yo * rhs.zy + zo * rhs.zz + rhs.zo;

		xx = lxx; xy = lxy; xz = lxz;  xo = lxo;
		yx = lyx; yy = lyy; yz = lyz;  yo = lyo;
		zx = lzx; zy = lzy; zz = lzz;  zo = lzo;
    }


	/////////////////////////////////////////////
    public void unit() 
	{
	    xx = 1.0F; xy = 0.0F; xz = 0.0F;  xo = 0.0F;
	    yx = 0.0F; yy = 1.0F; yz = 0.0F;  yo = 0.0F;
	    zx = 0.0F; zy = 0.0F; zz = 1.0F;  zo = 0.0F;
    }

	/////////////////////////////////////////////
    public void SetQuat(float qx, float qy, float qz, float qw) 
	{
		float norm = qx * qx + qy * qy + qz * qz + qw * qw; 
		float fac = 2.0F / (norm != 0.0F ? norm : 1.0F); 

		xx = 1.0F - (qy * qy + qz * qz) * fac; 
		xy = (qx * qy - qw * qz) * fac; 
		xz = (qx * qz + qw * qy) * fac; 

		yx = (qx * qy + qw * qz) * fac; 
		yy = 1.0F - (qx * qx + qz * qz) * fac; 
		yz = (qy * qz - qw * qx) * fac; 

		zx = (qx * qz - qw * qy) * fac; 
		zy = (qy * qz + qw * qx) * fac; 
		zz = 1.0F - (qx * qx + qy * qy) * fac; 

		xo = 0.0F; 
		yo = 0.0F; 
		zo = 0.0F; 
	}
}



