    ////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2008  Julian Todd.
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
// Vec3d - double version of Vec3
//
//

class Vec3d
{
	public double x;
	public double y;
	public double z;

	/////////////////////////////////////////////
	public Vec3d()
	{
		x = 0.0;
		y = 0.0;
		z = 0.0;
	}

	/////////////////////////////////////////////
	public String toString()
	{
		return String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z);
	}

	/////////////////////////////////////////////
	Vec3d(double lx, double ly, double lz)
	{
		x = lx;
		y = ly;
		z = lz;
	}

	/////////////////////////////////////////////
	Vec3d(String w0, String w1, String w2)
	{
		x = Double.parseDouble(w0);
		y = Double.parseDouble(w1);
		z = Double.parseDouble(w2);
	}


	/////////////////////////////////////////////
	public void PlusEquals(Vec3d a)
	{
		x += a.x;
		y += a.y;
		z += a.z;
	}

	/////////////////////////////////////////////
	public void TimesEquals(double f)
	{
		x *= f;
		y *= f;
		z *= f;
	}
}

