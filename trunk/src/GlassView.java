////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JPanel; 
import java.awt.Graphics; 
import java.awt.Graphics2D; 
import java.awt.Dimension; 

import java.awt.image.BufferedImage; 



/////////////////////////////////////////////
class MMpath
{
	int i0; 
	int i1; 
	boolean bForward; 
	float xval; 
	float zval; 

	boolean SetXZVals(Vec3[] pts, int npts, float cy) 
	{
		// find the segment 
		int i = i0; 
		int in; 
		while (true) 
		{
			if (i == i1) 
			{	
				System.out.println("XZ fail"); 
				return false; 
			}
			in = (bForward ? (i != npts - 1 ? i + 1 : 0) : (i != 0 ? i - 1 : npts - 1)); 
			if (cy < pts[in].y) 
				break; 
			i = in; 
		}

		float lambda = (cy - pts[i].y) / (pts[in].y - pts[i].y); 
		xval = pts[i].x * (1.0F - lambda) + pts[in].x * lambda; 
		zval = pts[i].z * (1.0F - lambda) + pts[in].z * lambda; 
		return true; 
	}
};


/////////////////////////////////////////////
class GlassView
{
	// use proper arrays (not Vector).  
	WireframeGraphics wg; 
	Dimension gsize = new Dimension(); 	

	boolean bActive = false; 
	BufferedImage bufferedimage; 

	GlassPixDep[] pxmat; 
	int resfac;	// this is 1 or 2 for lores.  

	int PDi(int i, int j) 
	{
		return i * gsize.height + j; 
	}

	GlassView(WireframeGraphics lwg) 
	{
		wg = lwg; 
		for (int i = 0; i < SSpts.length; i++) 
			SSpts[i] = new Vec3(); 
		for (int i = 0; i < mmpaths.length; i++) 
			mmpaths[i] = new MMpath(); 
	}

	int MaxPP = 100; 
	int[] imm = new int[MaxPP];	// indexes of minimas and maximas in y.  
	int[] immMO = new int[MaxPP]; // minima's order (index into imm). 

	MMpath[] mmpaths = new MMpath[MaxPP]; // running paths tracking classes. (unsorted for simplicity but not speed).  
	int nmmpaths = 999; 

	int PtsAdded; 

	// draw a polygon thing into the depth buffer 
	boolean PxDrawPoly(Vec3[] pts, int npts, int cnorm) 
	{
		if (npts >= MaxPP) 
		{
			System.out.println("Overload the poly points"); // pretty unlikely 
			return false; 
		}

		// first make an array of minimas and maximas (the minimas will be even).  
		int nimm = 0; 
		float fymax = -1.0F; 
		for (int i = 0; i < npts; i++) 
		{
			fymax = (i != 0 ? Math.max(fymax, pts[i].y) : pts[i].y); 

			int ip = (i != 0 ? i - 1 : npts - 1); 
			int in = (i != npts - 1 ? i + 1 : 0); 
			
			// maxima type 
			if ((pts[i].y >= pts[ip].y) && (pts[i].y > pts[in].y)) 
			{
				if (nimm == 0) 
				{
					imm[0] = -1; 
					nimm++; 
				}
				else if ((nimm % 2) == 0) 
					return false; 
				imm[nimm] = i; 
				nimm++; 
			}

			// minima type 
			if ((pts[i].y < pts[ip].y) && (pts[i].y <= pts[in].y)) 
			{
				if ((nimm % 2) != 0) 
					return false; 
				imm[nimm] = i; 
				nimm++; 
			}
		}
		if (nimm < 2) 
			return false; 

		if (imm[0] == -1) 
		{
			nimm--; 
			imm[0] = imm[nimm]; 
		}

		// same number of minimas as maximas.  
		if ((nimm % 2) != 0) 
			return false; 
		
		// now order the minimas 
		for (int i = 0; i < nimm / 2; i++) 
		{
			int j; 
			for (j = i; j > 0; j--) 
			{
				if (pts[imm[i * 2]].y >= pts[imm[immMO[j - 1]]].y) 
					break; 
				immMO[j] = immMO[j - 1]; 
			}
			immMO[j] = i * 2; 
		}

		//for (int i = 1; i < nimm / 2; i++) 
		//	if (pts[imm[immMO[i - 1]]].y > pts[imm[immMO[i]]].y) 
		//		System.out.println("Missordered " + i); 


		// the so far through the immMO array 
		int simmMO = 0; 

		// the running array of paths 
		nmmpaths = 0; 

		// find the ranges we will scan over 
		float fymin = pts[imm[immMO[simmMO]]].y; 
	
		int cymax = Math.min(wg.csize.height - 1, (int)fymax); 

		//System.out.println("rcy " + (fymin / resfac) + " max " + (cymax / resfac)); 
		// loop through the y points until no more 
		for (int rcy = Math.max(0, (int)fymin + 1) / resfac; rcy <= cymax / resfac; rcy++) 
		{
			int cy = rcy * resfac; 

			// put in new paths 
			while ((simmMO < nimm / 2) && (pts[imm[immMO[simmMO]]].y < cy))  
			{
				int j = immMO[simmMO]; // even number (a minima).  
				mmpaths[nmmpaths].i0 = imm[j]; 
				mmpaths[nmmpaths].i1 = imm[(j != nimm - 1 ? j + 1 : 0)]; 
				mmpaths[nmmpaths].bForward = true; 
				nmmpaths++; 
				mmpaths[nmmpaths].i0 = imm[j]; 
				mmpaths[nmmpaths].i1 = imm[(j != 0 ? j - 1 : nimm - 1)]; 
				mmpaths[nmmpaths].bForward = false; 
				nmmpaths++; 
				simmMO++; 
			}

			// take away old paths 
			for (int i = nmmpaths - 1; i >= 0; i--) 
			{
				if (pts[mmpaths[i].i1].y < cy) 
				{
					nmmpaths--; 
					
					// must swap them round to avoid making duplicates in the list!!!  
					MMpath tm = mmpaths[i]; 
					mmpaths[i] = mmpaths[nmmpaths]; 
					mmpaths[nmmpaths] = tm; 

				}
			}

			if ((nmmpaths % 2) != 0) 
				return false; 

			// make sequence of x's 
			for (int i = 0; i < nmmpaths; i++) 
				mmpaths[i].SetXZVals(pts, npts, cy); 

			// now scan across and paint in alternatives 

			// sort the sequence (bubble sort will do cause it'll happen only once) 
			int si = 1; 
			while (si < nmmpaths) 
			{
				if (mmpaths[si - 1].xval > mmpaths[si].xval) 
				{
					MMpath tm = mmpaths[si - 1]; 
					mmpaths[si - 1] = mmpaths[si]; 
					mmpaths[si] = tm; 
					if (si > 1) 
						si--; 
				}
				else 
					si++; 
			}

			for (int i = 0; i < nmmpaths; i += 2) 
			{
				// System.out.println("x scan " + String.valueOf(mmpaths[i].xval) + "  " + String.valueOf(mmpaths[i + 1].xval)); 
				int jm = Math.min((int)mmpaths[i + 1].xval, wg.csize.width - 1); 
				for (int rj = Math.max(((int)mmpaths[i].xval) / resfac + 1, 0); rj <= jm / resfac; rj++) 
				{
					int j = rj * resfac; 
					float lambda = (j - mmpaths[i].xval) / (mmpaths[i + 1].xval - mmpaths[i].xval); 
					float zdep = mmpaths[i].zval * (1.0F - lambda) + mmpaths[i + 1].zval * lambda; 
					pxmat[PDi(rj, rcy)].Add(-zdep, cnorm);  
					PtsAdded++; 
				}
			}
		} // end for(cy) 
		return true; 
	}


	Vec3[] SSpts = new Vec3[100]; 
	Vec3 SSnorm = new Vec3(); 
	Vec3 SSvec1 = new Vec3(); 
	Vec3 SSvec2 = new Vec3(); 

	// draw the triangle (will be taking 3d points later 
	void PxDrawTriangle(Vec3 pt0, Vec3 pt1, Vec3 pt2) 
	{		
		SSvec1.Diff(pt1, pt0); 
		SSvec2.Diff(pt2, pt0); 
		SSnorm.Cross(SSvec2, SSvec1); 

		SSpts[0] = pt0; 
		SSpts[1] = pt1; 
		SSpts[2] = pt2; 

		boolean bSucc = PxDrawPoly(SSpts, 3, SSnorm.ConvCompress(false)); 
		if (!bSucc) 
			System.out.println("failed poly"); 
	}


	/////////////////////////////////////////////
	static Vec3 vfdiff = new Vec3(); 

	/////////////////////////////////////////////
	static void Accum(float[] vf, OneSection xsection, int i, int in, boolean bTwist)
	{
		int nvf = 1; 
		vf[0] = 0.0F; 
		while (i != in)
		{
			int ip1 = (i + (bTwist ? -1 : 1) + xsection.nnodes) % xsection.nnodes; 
			vfdiff.Diff(xsection.ELoc[i], xsection.ELoc[ip1]); 
			vf[nvf] = vf[nvf - 1] + vfdiff.Len(); 
			nvf++; 
			i = ip1; 
		}

		// normalize
		if (vf[nvf - 1] == 0.0F)
			vf[nvf - 1] = 1.0F; 
		for (int j = 0; j < nvf; j++)
			vf[j] /= vf[nvf - 1]; 

		vf[nvf - 1] = 1.0F; 
	}


	/////////////////////////////////////////////
	static float[] vf0 = new float[100]; 
	static float[] vf1 = new float[100]; 

static int kfac = 1; 
	void DrawTubes()
	{
PtsAdded = 0; 

		// clear the VAindexes 
		for (int i = 0; i < wg.ot.vsections.size(); i++) 
		{
			OneSection xsection = ((OneSection)(wg.ot.vsections.elementAt(i))); 
			xsection.VAindex = 0; 
		}
		for (int it = 0; it < (kfac == 1 ? wg.ot.vtubes.size() : 1); it++)
		{
			OneTube tube = ((OneTube)(wg.ot.vtubes.elementAt(it))); 

			// replace with new tube if necessary 
			if ((tube.xsection0.xsectionE != null) || (tube.xsection1.xsectionE != null)) 
			{
				tube = new OneTube((tube.xsection0.xsectionE != null ? tube.xsection0.xsectionE : tube.xsection0), (tube.xsection1.xsectionE != null ? tube.xsection1.xsectionE : tube.xsection1)); 
				tube.ReformTubespace(); 
			}

			// increment the section counters.  
			tube.xsection0.VAindex += (tube.bTwist0 ? -1 : 1); 
			tube.xsection1.VAindex -= (tube.bTwist1 ? -1 : 1); 
		
			// make the interpolation.  
			for (int ic = 0; ic < tube.ntubecorners; ic++)
			{
				int i = tube.cnxs0[ic]; 
				int j = tube.cnxs1[ic]; 
				int in = tube.cnxs0[(ic + 1) % tube.ntubecorners]; 
				int jn = tube.cnxs1[(ic + 1) % tube.ntubecorners]; 

				Accum(vf0, tube.xsection0, i, in, tube.bTwist0); 
				Accum(vf1, tube.xsection1, j, jn, tube.bTwist1); 

				int ivf0 = 0; 
				int ivf1 = 0; 
				while ((vf0[ivf0] != 1.0F) || (vf1[ivf1] != 1.0F))
				{
					// find which to step forward first 
					if ((vf0[ivf0] != vf1[ivf1]) ? (vf0[ivf0] < vf1[ivf1]) : (vf0[ivf0 + 1] < vf1[ivf1 + 1]))
					{
						int ip = (i + (tube.bTwist0 ? -1 : 1) + tube.xsection0.nnodes) % tube.xsection0.nnodes; 
						PxDrawTriangle(tube.xsection0.RTLoc[i], tube.xsection0.RTLoc[ip], tube.xsection1.RTLoc[j]);  
						i = ip; 
						ivf0++; 
					}

					else
					{
						int jp = (j + (tube.bTwist1 ? -1 : 1) + tube.xsection1.nnodes) % tube.xsection1.nnodes; 
						PxDrawTriangle(tube.xsection0.RTLoc[i], tube.xsection1.RTLoc[jp], tube.xsection1.RTLoc[j]); 
						j = jp; 
						ivf1++; 
					}
				}
			}
		}

		// draw the necessary XSections  
		for (int i = 0; i < wg.ot.vsections.size(); i++) 
		{
			OneSection xsection = ((OneSection)(wg.ot.vsections.elementAt(i))); 
			if (xsection.VAindex != 0)  
			{
				//System.out.println("Drawing " + xsection.nnodes + " node poly " + xsection.VAindex + " times"); 
				for (int j = 0; j < xsection.nnodes; j++) 
					SSpts[j] = xsection.RTLoc[xsection.VAindex < 0 ? j : xsection.nnodes - 1 - j]; 
				for (int p = 0; p < Math.abs(xsection.VAindex); p++) 
				{
					boolean bSucc = PxDrawPoly(SSpts, xsection.nnodes, xsection.RTvcperp.ConvCompress(xsection.VAindex < 0)); 
					if ((p == 0) && !bSucc) 
						System.out.println("failed poly of " + xsection.nnodes + " nodes."); 
				}					 
			}
		}
System.out.println("PtsAdded " + PtsAdded); 
if (PtsAdded == 0) 
	kfac++; 
	}

	/////////////////////////////////////////////
	// main calling function.  
	/////////////////////////////////////////////
	void CalcSolid(boolean bLoRes) 
	{
		// set the resolution 
		if (bLoRes) 
		{
			resfac = 2; 
			gsize.setSize((wg.csize.width + resfac - 1) / resfac, (wg.csize.height + resfac - 1) / resfac); 
		}
		else 
		{
			resfac = 1; 
			gsize.setSize(wg.csize); 
		}

		// malloc the memory 
		if ((pxmat == null) || (gsize.width * gsize.height != pxmat.length)) 
		{
			pxmat = new GlassPixDep[gsize.width * gsize.height];  
			for (int i = 0; i < pxmat.length; i++) 
				pxmat[i] = new GlassPixDep(); 
		}
		else 
		{
			for (int i = 0; i < pxmat.length; i++) 
				pxmat[i].npd = 0; 
		}

		// build the surface model.  
		DrawTubes(); 

		// derive the solid model 
		for (int i = 0; i < gsize.width; i++) 
		for (int j = 0; j < gsize.height; j++) 
		{
			GlassPixDep gpd = pxmat[PDi(i, j)]; 
			gpd.MakeUnion();  
		}
	}


	/////////////////////////////////////////////
	void BuildImage(boolean bOutline, boolean bShadeVol, float thfac, float trans, float Fforelight, float Fbacklight) 
	{
		// build the image.  
		if ((bufferedimage == null) || (bufferedimage.getWidth() != wg.csize.width) || (bufferedimage.getHeight() != wg.csize.height))  
			bufferedimage = new BufferedImage(wg.csize.width, wg.csize.height, BufferedImage.TYPE_INT_ARGB); 

int Snpd = 0; 
		for (int i = 0; i < gsize.width - 1; i++) // avoid partial strip down the side.  
		for (int j = 0; j < gsize.height - 1; j++) 
		{
			// corresp points 
			GlassPixDep gpd = pxmat[PDi(i, j)]; 
Snpd += gpd.npd; 

			int ott = 0; 

			boolean bReden = gpd.bNoGood; 
			if (bOutline) 
			{
				if ((i >= 1) && (j >= 1) && (i < gsize.width - 1) && (j < gsize.height - 1)) 
					ott = gpd.OutlineType(pxmat[PDi(i, j - 1)], pxmat[PDi(i, j + 1)], pxmat[PDi(i - 1, j)], pxmat[PDi(i + 1, j)]);  
				else 
					ott = 0; 
				if (ott == -10) 
					bReden = true; 
			}
			else 
				bShadeVol = true; 

			int col = 0xffff0000; 
			if (!bReden) 
			{
				if (bShadeVol) 
				{
					if (bOutline && (ott == 1)) 
						col = 0xff0000a0; 
					else 
						col = gpd.ModelCol(thfac, trans, Fforelight, Fbacklight); 
				}
				else if (bOutline) 
				{
					if (ott == 0) 
						col = 0xffffffff; 
					else if (ott == 1) 
						col = 0xff000000; 
					else if (ott > 0) 
						col = 0xffb0b0b0; 
					else 
						col = 0xffb0b0ff; 
				}
			}


			// put in the points 
			if (resfac != 1) 
			{
				for (int di = 0; di < resfac; di++) 
				for (int dj = 0; dj < resfac; dj++) 
					bufferedimage.setRGB(i * resfac + di, j * resfac + dj, col); 
			}
			else
				bufferedimage.setRGB(i, j, col); 
		}

System.out.println("Snpd " + Snpd); 

		bActive = true; 
	}

	/////////////////////////////////////////////
	void Kill() 
	{
		bActive = false; 
	}

	/////////////////////////////////////////////
	void paintW(Graphics g) 
	{
		Graphics2D g2d = (Graphics2D)g; 
		g2d.drawImage(bufferedimage, 0, 0, wg); 
	}


};

