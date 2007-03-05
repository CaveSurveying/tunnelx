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


/////////////////////////////////////////////
class GlassPixDep
{
	int npd = 0; 
	float[] pd = null; // array of points
	int[] pdc = null;  // compressed array of normals, orientation and colours.  

	boolean bNoGood; 

	// memory is malloced in powers of 2.  
	void ExtendMem() 
	{
		if ((pd != null) && (npd + 1 < pd.length)) // so we can add in two elements safely.  
			return; 
		
		float[] newpd = new float[(pd != null ? pd.length * 2 : 2)]; 
		int[] newpdc = new int[newpd.length]; 

		for (int i = 0; i < npd; i++) 
		{
			newpd[i] = pd[i]; 
			newpdc[i] = pdc[i]; 
		}

		pd = newpd; 
		pdc = newpdc; 
	}

	void Copy(GlassPixDep x)
	{
		for (npd = 0; npd < x.npd; npd++) 
		{
			ExtendMem(); 
			pd[npd] = x.pd[npd]; 
			pdc[npd] = x.pdc[npd]; 
		}
		bNoGood = x.bNoGood; 
	}

	void Add(float lpd, int lpdc) 
	{
		ExtendMem(); 

		int i; 
		for (i = npd; i > 0; i--) 
		{
			if (pd[i - 1] <= lpd) 
				break; 
			pd[i] = pd[i - 1]; 
			pdc[i] = pdc[i - 1]; 
		}
		pd[i] = lpd; 
		pdc[i] = lpdc; 
		npd++; 
	}

	// returns the thickness
	float MakeUnion() 
	{
		int interiorness = 0; 
		float res = 0.0F; 
		bNoGood = false; 

		int i = 0; 
		while (i < npd)  
		{
			int pinteriorness = interiorness; 
			interiorness += ((pdc[i] & 128) != 0 ? 1 : -1); 
			bNoGood |= (interiorness < 0); 
			if ((interiorness >= 2) || (pinteriorness >= 2))  
			{
				for (int j = i + 1; j < npd; j++) 
				{
					pdc[j - 1] = pdc[j]; 
					pd[j - 1] = pd[j]; 
				}  
				npd--; 
			}  
			else 
			{
				if ((interiorness == 0) && (pinteriorness == 1)) 
					res += pd[i] - pd[i - 1]; 
				i++; 
			}
		}
		bNoGood |= (interiorness != 0); 
		return (!bNoGood ? (res != 0.0F ? res : 1.0F) : -1.0F); 
	}

	// inverts a slice between these two planes  
	// delete anything outside the range, invert anything within it.  
	// add in two insertions
	void SliceInverse(float zsplanelo, float zsplanehi)  
	{
		// pass through inverting and deleting anything that doesn't belong.  
		int i = 0; 
		while (i < npd)  
		{
			if ((pd[i] <= zsplanelo) || (pd[i] >= zsplanehi))  
			{
				for (int j = i + 1; j < npd; j++) 
				{
					pdc[j - 1] = pdc[j]; 
					pd[j - 1] = pd[j]; 
				}  
				npd--; 
			}

			else 
			{
				pdc[i] = Vec3.ConvCompressInvert(pdc[i]);   
				i++; 
			}
		}

		if (npd != 0)  // leave out boring pairs of planes.  
		{
			// insert the two end slices if necessary 
			if ((npd == 0) || ((pdc[0] & 128) == 0))  
			{
				ExtendMem(); 
				for (int j = npd; j >= 1; j--)  
				{
					pdc[j] = pdc[j - 1]; 
					pd[j] = pd[j - 1]; 
				}  
				npd++; 

				pd[0] = zsplanelo; 
				pdc[0] = (128 | (1 << 24)); 
			}

			if ((npd == 0) || ((pdc[npd - 1] & 128) != 0))  
			{
				ExtendMem(); 
				pd[npd] = zsplanehi; 
				pdc[npd] = (0 | (255 << 24)); 
				npd++; 
			}
		}
	}


	// returns the thickness
	static Vec3 sn0 = new Vec3(); 
	static Vec3 sn1 = new Vec3(); 

	static float[] trans = new float[50]; 
	static float[] refl = new float[50]; 
	static float[] forelight = new float[50]; 
	static float[] backlight = new float[50]; 

	int ModelCol(float tdark, float transp, float Fforelight, float Fbacklight) 
	{
		ExtendMem(); 
		forelight[0] = Fforelight; 
		backlight[npd] = Fbacklight; 

		// fill the arrays with transmission and reflextion coeffs 
		for (int s = 0; s < npd; s += 2) 
		{
			float th = pd[s + 1] - pd[s]; 
			sn0.ConvDecompress(pdc[s]); 
			sn1.ConvDecompress(pdc[s + 1]); 

			// light kept at each junction.  
			float lk0 = (Math.abs(sn0.z) + 3) / 4; 
			float lk1 = (Math.abs(sn1.z) + 3) / 4; 

			float ltr = (float)Math.exp(-th / tdark); 

			trans[s] = lk0 * transp * ltr * lk1 * transp; 
			refl[s] = lk0 * (1.0F - transp) + lk0 * transp * ltr * lk1 * (1.0F - transp) * ltr * lk0 * transp;  

			// do the fore light as well.  
			forelight[s + 2] = forelight[s] * trans[s]; 
		}
		
		// do the back lighting 
		for (int s = npd - 2; s >= 0; s -= 2) 
			backlight[s] = forelight[s] * refl[s] + backlight[s + 2] * trans[s]; 

		float dtrans = backlight[0] - backlight[npd] * forelight[npd] / forelight[0]; // by symmetry.  

		float pr = backlight[0]; 
		float pg = backlight[0] + dtrans / 6; 
		float pb = backlight[0]; 

		int itr = Math.max(0, Math.min(255, (int)(256 * pr))); 
		int itg = Math.max(0, Math.min(255, (int)(256 * pg))); 
		int itb = Math.max(0, Math.min(255, (int)(256 * pb))); 
		return itr * 0x00000001 + itg * 0x00000100 + itb * 0x00010000 + 0xff000000; 
	}


	boolean OverlapPos(float lo, float hi) 
	{
		// does this range overlap what is contained?  
		for (int i = 1; i < npd; i += 2) 
		{
			if ((lo <= pd[i]) && (hi >= pd[i - 1])) 
				return true; 
		}
		return false; 
	}

	boolean OverlapNeg(float lo, float hi) 
	{
		if (npd == 0) 
			return true; 
		if ((lo <= pd[0]) || (hi >= pd[npd - 1])) 
			return true; 

		for (int i = 2; i < npd; i += 2) 
		{
			if ((lo <= pd[i]) && (hi >= pd[i - 1])) 
				return true; 
		}
		return false; 
	}

	int OutlineType(GlassPixDep pdUp, GlassPixDep pdDown, GlassPixDep pdLeft, GlassPixDep pdRight) 
	{
		if (bNoGood || pdUp.bNoGood || pdDown.bNoGood || pdLeft.bNoGood || pdRight.bNoGood) 
			return -10; 

		// for each range, check overlapping against all four sides 
		// higher values are closer.  
		int nfoldno = 1; 
		for (int i = 1; i < npd; i += 2) 
		{
			boolean bOLup = pdUp.OverlapPos(pd[i - 1], pd[i]); 
			boolean bOLdown = pdDown.OverlapPos(pd[i - 1], pd[i]); 
			boolean bOLleft = pdLeft.OverlapPos(pd[i - 1], pd[i]); 
			boolean bOLright = pdRight.OverlapPos(pd[i - 1], pd[i]); 

			// positive fold 
			if (!bOLup || !bOLdown || !bOLleft || !bOLright) 
				return nfoldno; 

			// negative fold 
			if (i >= 3) 
			{
				boolean bnOLup = pdUp.OverlapNeg(pd[i - 2], pd[i - 1]); 
				boolean bnOLdown = pdDown.OverlapNeg(pd[i - 2], pd[i - 1]); 
				boolean bnOLleft = pdLeft.OverlapNeg(pd[i - 2], pd[i - 1]); 
				boolean bnOLright = pdRight.OverlapNeg(pd[i - 2], pd[i - 1]); 

				if (!bnOLup || !bnOLdown || !bnOLleft || !bnOLright) 
					return -nfoldno; 
			}
			nfoldno++; 
		}

		return 0; 
	}

}; 

