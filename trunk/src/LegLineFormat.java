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

import java.util.List; 
import java.util.ArrayList; 

//
//
// LegLineFormat
//
//
public class LegLineFormat// implements Cloneable
{
	static int DEGREES = 0;
	static int GRADS = 1; 
	static int PERCENT = 2;

	static float TAPEFAC_M = 1.0F; 
	static float TAPEFAC_CM = 0.01F;
	static float TAPEFAC_FT = 0.3048F;

	String datatype = "normal"; // or diving, cartesian, nosurvey, passage
	boolean bnosurvey = false;
	boolean bcartesian = false;
    boolean bbaddataline = false;
    boolean bpassage = false; 
    boolean bsurface = false; 
	boolean bduplicate = false; 
	boolean bsplay = false; 
		
	int fromindex = 0;
	int toindex = 1;

	int tapeindex = 2;
	float tapenegoffset = 0.0F;
	float tapefac = 1.0F;

	int compassindex = 3;
	int backcompassindex = -1;
	float compassnegoffset = 0.0F;
	float backcompassnegoffset = 0.0F;
	float compassnegoffsetdeclination = 0.0F; // a secondary offset value (separates the calibration from the magnetic wandering (declination))
	int compassfac = DEGREES;

	int clinoindex = 4;
	int backclinoindex = -1;
	float clinonegoffset = 0.0F;
	int clinofac = DEGREES;

	int dxindex = -1;
	int dyindex = -1;
	int dzindex = -1;
	float dxfac = 1.0F; 
	float dyfac = 1.0F; 
	float dzfac = 1.0F; 

	int depthindex = -1;
	int fromdepthindex = -1;
	int todepthindex = -1;
	float depthnegoffset = 0;
	float depthfac = 1.0F;

	int stationindex = -1;
	int	leftindex = -1;
	int rightindex = -1;
	int upindex = -1;
	int downindex = -1;

	// this tells where the newline can be fit into the format
	// (to account for those two-line type records).
	int newlineindex = -1;

	String sdecimal = null;
	String sblank = null;	// this may need to be mapped into the word splitter.
	String snames = null;

	// attributes carried over from those crappy blank begin blocks that are completely crap!
	String bb_svxdate = "";
	String bb_svxtitle = "";
	String bb_teamtape = "";
	String bb_teampics = "";
	String bb_teaminsts = "";
	String bb_teamnotes = "";
	List<String> totalteam = new ArrayList<String>(); 
	StringBuffer sb_totalteam = new StringBuffer(); 

	// local data used for multi-line (diving) type data.
	String lstation = null;
	float ldepth = 0;
	float lcompass = 0;
	float ltape = 0;
	float ldx = 0; 
	float ldy = 0; 
	float ldz = 0; 

	boolean btopextendedelevation = false; 
	boolean btopextflipleg = false; 

	int currnewlineindex = 0;
	FileAbstraction currfile;  // not used

	/////////////////////////////////////////////
	LegLineFormat() // constructs the default one.
	{;}

	/////////////////////////////////////////////
	LegLineFormat(LegLineFormat f)
	{
		if (f != null)
		{
			datatype = f.datatype;
			bnosurvey = f.bnosurvey;
			bcartesian = f.bcartesian;
            bpassage = f.bpassage; 
			bsurface = f.bsurface; 
			bduplicate = f.bduplicate; 
			bsplay = f.bsplay; 
            bbaddataline = f.bbaddataline;

			fromindex = f.fromindex;
			toindex = f.toindex;

			tapeindex = f.tapeindex;
			tapenegoffset = f.tapenegoffset;
			tapefac = f.tapefac;

			compassindex = f.compassindex;
			backcompassindex = f.backcompassindex;
			compassnegoffset = f.compassnegoffset;
			backcompassnegoffset = f.backcompassnegoffset;
			compassnegoffsetdeclination = f.compassnegoffsetdeclination;
			compassfac = f.compassfac;

			clinoindex = f.clinoindex;
			backclinoindex = f.backclinoindex;
			clinonegoffset = f.clinonegoffset;
			clinofac = f.clinofac;

			dxindex = f.dxindex;
			dyindex = f.dyindex;
			dzindex = f.dzindex;
			dxfac = f.dxfac; 
			dyfac = f.dyfac; 
			dzfac = f.dzfac; 

			newlineindex = f.newlineindex;

			stationindex = f.stationindex;

			depthindex = f.depthindex;
			fromdepthindex = f.fromdepthindex;
			todepthindex = f.todepthindex;
			depthnegoffset = f.depthnegoffset;
			depthfac = f.depthfac;

			leftindex = f.leftindex;
			rightindex = f.rightindex;
			upindex = f.upindex;
			downindex = f.downindex;

			sdecimal = f.sdecimal;
			sblank = f.sblank;
			snames = f.snames;

			btopextendedelevation = f.btopextendedelevation; 
			
			bb_svxdate = f.bb_svxdate;
			bb_svxtitle = f.bb_svxtitle;
			bb_teamtape = f.bb_teamtape;
			bb_teampics = f.bb_teampics;
			bb_teaminsts = f.bb_teaminsts;
			bb_teamnotes = f.bb_teamnotes;
			UpdateTotalTeam(); 
		}
	}

	/////////////////////////////////////////////
	void AddToTotalTeam(String steam)
	{
		if (steam.length() == 0)
			return; 
		if (steam.equalsIgnoreCase("both") || steam.equalsIgnoreCase("none"))
			return; 
		int iand = steam.indexOf(" and ");
		if (iand != -1)
		{
			AddToTotalTeam(steam.substring(0, iand));
			AddToTotalTeam(steam.substring(iand + 5));
			return;
		}
		int iand1 = steam.indexOf("&");
		if (iand1 == -1)
			iand1 = steam.indexOf("+");
		if (iand1 == -1)
			iand1 = steam.indexOf(",");
		if (iand1 != -1)
		{
			AddToTotalTeam(steam.substring(0, iand1).trim());
			AddToTotalTeam(steam.substring(iand1 + 1).trim()); 
			return; 
		}
		steam = steam.trim(); 
		for (String lteam : totalteam)
			if (steam.equalsIgnoreCase(lteam))
				return; 
		totalteam.add(steam); 
	}

	/////////////////////////////////////////////
	void UpdateTotalTeam()
	{
		totalteam.clear(); 
		AddToTotalTeam(bb_teamnotes); 
		AddToTotalTeam(bb_teampics); 
		AddToTotalTeam(bb_teaminsts); 
		AddToTotalTeam(bb_teamtape); 
		sb_totalteam.setLength(0); 
		for (String t : totalteam)
		{
			if (sb_totalteam.length() != 0)
				sb_totalteam.append(", "); 
			sb_totalteam.append(t); 
		}
	}
	
	/////////////////////////////////////////////
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(datatype);

		int im0 = Math.max(fromindex, toindex); 
		int im1 = Math.max(clinoindex, compassindex); 
		int im2 = Math.max(backclinoindex, backcompassindex); 
		int im3 = tapeindex; 
		int im = Math.max(Math.max(im0, im1), Math.max(im2, im3));
		for (int i = 0; i <= im; i++)
		{
			if (i == fromindex)
				sb.append(" from");
			else if (i == toindex)
				sb.append(" to");
			else if (i == tapeindex)
				sb.append(" tape");
			else if (i == compassindex)
				sb.append(" compass");
			else if (i == clinoindex)
				sb.append(" clino");
			else if (i == backcompassindex)
				sb.append(" backcompass");
			else if (i == backclinoindex)
				sb.append(" backclino");

			else if (i == dxindex)
				sb.append(" dx");
			else if (i == dyindex)
				sb.append(" dy");
			else if (i == dzindex)
				sb.append(" dz");

			else if (i == depthindex)
				sb.append(" depth");
			else if (i == fromdepthindex)
				sb.append(" fromdepth");
			else if (i == todepthindex)
				sb.append(" todepth");

			else if (i == stationindex)
				sb.append(" station");
			else if (i == newlineindex)
				sb.append(" newline");

			else if (i == leftindex)
				sb.append(" left");
			else if (i == rightindex)
				sb.append(" right");
			else if (i == upindex)
				sb.append(" up");
			else if (i == downindex)
				sb.append(" down");
			else
				sb.append(" ignore");
		}
		sb.append(" ignoreall");

		return sb.toString();
	}

	/////////////////////////////////////////////
	// this substitutes characters in these strings with ones that make them parsable.
	String ApplySet(String field)
	{
		// deal with the blank conversions.
		if (sblank != null)
		{
			for (int i = 0; i < sblank.length(); i++)
				field = field.replace(sblank.charAt(i), ' ');
		}

		// deal with the decimal conversions.
		if (sdecimal != null)
		{
			for (int i = 0; i < sdecimal.length(); i++)
				field = field.replace(sdecimal.charAt(i), '.');
		}
		return field;
	}

	/////////////////////////////////////////////
	float ReadCompass(String ws, boolean bback)
	{
		String acompass = ApplySet(ws);
		if (acompass.equalsIgnoreCase("-") || acompass.equals(""))
			return OneLeg.INVALID_COMPASSCLINO; 

		float compass = GetFLval(acompass) - (bback ? backcompassnegoffset : compassnegoffset) - compassnegoffsetdeclination;
		if (compassfac == GRADS)
			compass *= 360.0F / 400.0F;

        while (compass < 0.0F)
            compass += 360.0F;
        while (compass > 360.0F)
            compass -= 360.0F; 
		return compass; 
	}


	/////////////////////////////////////////////
	float ReadClino(String ws)
	{
		float clino; 
		String aclino = ApplySet(ws);
		if (aclino.equalsIgnoreCase("-"))
			return OneLeg.INVALID_COMPASSCLINO; 
		if (aclino.equalsIgnoreCase("up") || aclino.equalsIgnoreCase("u") || aclino.equalsIgnoreCase("+V"))
			clino = 90.0F;
		else if (aclino.equalsIgnoreCase("down") || aclino.equalsIgnoreCase("d") || aclino.equalsIgnoreCase("-V"))
			clino = -90.0F;
		else if (aclino.equalsIgnoreCase("h") || aclino.equalsIgnoreCase("level"))
			clino = 0.0F;
		else
		{
			clino = GetFLval(aclino);
			clino -= clinonegoffset;   // is there a different setting for backclino?
			if (clinofac == GRADS)
				clino *= 360.0F / 400.0F;
			if (clinofac == PERCENT)
				clino = (float)TN.percentdeg(clino);
		}
		return clino; 
	}
	
	/////////////////////////////////////////////
	OneLeg ReadLeg(String w[], LineInputStream lis)
	{
		try
		{
		// normal leg format with everything there.
		if ((newlineindex == -1) && (stationindex == -1))
		{
            if (w[toindex].equals("-"))
                w[toindex] = null; // discarding splay station
            else if (w[toindex].startsWith("-") && w[toindex].endsWith("-"))
                w[toindex] = null; // discarding splay station
            else if (w[toindex].equals(".") || w[toindex].equals("..") || w[toindex].equals("..."))
                w[toindex] = null; // discarding splay station
                
            if (w[fromindex].equals("-"))
                w[fromindex] = null; // discarding splay station
            else if (w[fromindex].equals(".") || w[fromindex].equals("..") || w[fromindex].equals("..."))
                w[fromindex] = null; // discarding splay station

            // case of just a leg but with no measurements on it
			if (bnosurvey)
				return new OneLeg(w[fromindex], w[toindex], this);
            if (bbaddataline)
            {
                lis.emitWarning("ignoring line due to bad *data format");
                return null;
            }
			if (bcartesian)
			{
				float dx = GetFLval(ApplySet(w[dxindex])) * dxfac;
				float dy = GetFLval(ApplySet(w[dyindex])) * dyfac;
				float dz = GetFLval(ApplySet(w[dzindex])) * dzfac;
				return new OneLeg(dx, dy, dz, w[fromindex], w[toindex], this);
			}

			String atape = ApplySet(w[tapeindex]);
			float tape = (GetFLval(atape) - tapenegoffset) * tapefac;

			float compass = ReadCompass(w[compassindex], false); 
			float backcompass = (backcompassindex == -1 ? OneLeg.INVALID_COMPASSCLINO : ReadCompass(w[backcompassindex], true)); 

            // clino data exists
			if (clinoindex != -1)
			{
				float clino = ReadClino(w[clinoindex]); 
    			float backclino = (backclinoindex == -1 ? OneLeg.INVALID_COMPASSCLINO : ReadClino(w[backclinoindex])); 
				if ((compass == OneLeg.INVALID_COMPASSCLINO) && (backcompass == OneLeg.INVALID_COMPASSCLINO))
                {
                    if ((clino != -90.0F) && (clino != 90.0F))
                        TN.emitWarning("Error, blank compass on non-vertical leg " + w[0] + " " + w[1] + " " + w[2] + " " + w[3] + " " + w[4] + " " + w[5]);
                }
				if (btopextendedelevation)
					btopextflipleg = w[5].equals(TN.flipCLINEsignal); 
				OneLeg ol = new OneLeg(w[fromindex], w[toindex], tape, compass, backcompass, clino, backclino, this);
				return ol; 
			}
			else
				assert backclinoindex == -1; 

            // case where clino not needed in diving data
			if ((fromdepthindex != -1) && (todepthindex != -1))
			{
				String afromdepth = ApplySet(w[fromdepthindex]);
				float fromdepth = GetFLval(afromdepth);
				String atodepth = ApplySet(w[fromdepthindex]);
				float todepth = GetFLval(atodepth);

				//TN.emitMessage("LDIVING " + w[fromindex] + "  " + w[toindex] + "  " + tape + "  " + compass + "  " + fromdepth + "  " + todepth);

				if ((compass == OneLeg.INVALID_COMPASSCLINO) && (backcompass != OneLeg.INVALID_COMPASSCLINO))   // unlikely to do good backsights in diving data
					compass = backcompass; 

				return new OneLeg(w[fromindex], w[toindex], tape, compass, fromdepth, todepth, this);
			}
		}

        // new case of single station disclosed on line
		if ((newlineindex == -1) && (stationindex != -1))
		{
			if (bnosurvey && (stationindex != -1))
				return new OneLeg(w[stationindex], null, this);
        }
        
		// cope with some difficult format that spans more than one line.
		if ((stationindex != -1) && (newlineindex != -1))
		{
			// load the station and build the return value if we have a follow on.
			int nextnewlineindex = (currnewlineindex == 0 ? newlineindex : w.length);

			String lnewstation = null;
			float lnewdepth = -1.0F;

			if ((stationindex < nextnewlineindex) && (stationindex >= currnewlineindex))  // currnewlineindex is 0 in this case.
				lnewstation = w[stationindex - currnewlineindex];

			// probably assumes when depthindex == -1 it's not >= currnewlineindex 
			if ((depthindex < nextnewlineindex) && (depthindex >= currnewlineindex))  // currnewlineindex is 0 in this case.
			{
				String adepth = ApplySet(w[depthindex - currnewlineindex]);
				lnewdepth = (GetFLval(adepth) + depthnegoffset) * depthfac;
			}

			// build the result.
			OneLeg olres = null;
			if ((lnewstation != null) && (lstation != null))  // and the rest.
			{
				if (bcartesian)
				{
					olres = new OneLeg(ldx, ldy, ldz, lstation, lnewstation, this);
					//TN.emitMessage("DIVING cart " + lstation + "  " + lnewstation + "  " + ltape + "  " + lcompass + "  " + ldepth + "  " + lnewdepth);
				}
				else
				{
					olres = new OneLeg(lstation, lnewstation, ltape, lcompass, ldepth, lnewdepth, this);
					//TN.emitMessage("DIVING " + lstation + "  " + lnewstation + "  " + ltape + "  " + lcompass + "  " + ldepth + "  " + lnewdepth);
				}
				// should clear all the fields.
			}

			// copy over the new labels.
			if (lnewstation != null)
			{
				lstation = lnewstation;
				ldepth = lnewdepth;
			}

			// get tape and compass.
			if ((tapeindex < nextnewlineindex) && (tapeindex >= currnewlineindex))  // currnewlineindex is 0 in this case.
			{
				String atape = ApplySet(w[tapeindex - currnewlineindex]);
				ltape = (GetFLval(atape) + tapenegoffset) * tapefac;
			}


			if ((compassindex < nextnewlineindex) && (compassindex >= currnewlineindex))  // currnewlineindex is 0 in this case.
			{
				String acompass = ApplySet(w[compassindex - currnewlineindex]);
				boolean bcblank = (acompass.equalsIgnoreCase("-") || acompass.equals(""));

				lcompass = (bcblank ? 0.0F : GetFLval(acompass)) - compassnegoffset - compassnegoffsetdeclination;
				if (compassfac == GRADS)
					lcompass *= 360.0F / 400.0F;
			}

			if (bcartesian)
			{
				if ((dxindex < nextnewlineindex) && (dxindex >= currnewlineindex))  
					ldx = GetFLval(ApplySet(w[dxindex - currnewlineindex])) * dxfac;
				if ((dyindex < nextnewlineindex) && (dyindex >= currnewlineindex))  
					ldy = GetFLval(ApplySet(w[dyindex - currnewlineindex])) * dyfac;
				if ((dzindex < nextnewlineindex) && (dzindex >= currnewlineindex))  
					ldz = GetFLval(ApplySet(w[dzindex - currnewlineindex])) * dzfac;
			}

			// update the lineindex.
			currnewlineindex = (currnewlineindex == 0 ? (newlineindex + 1) : 0);

			// not properly implemented case (errors not mapping across lines).
			return olres;
		}
		
		// we should be loading these in as cross-section objects
		if (datatype.equalsIgnoreCase("passage"))
		{
			//System.out.println("PASSAGE:: " + lis.GetLine()); 
			return null;
		}

		TN.emitWarning("Can't do format " + datatype);

		}
		catch (NumberFormatException e)
		{
			lis.emitError("Number Format");
		}
		return null;
	}

	/////////////////////////////////////////////
	OneLeg ReadFix(String w[], LineInputStream lis)
	{
		try
		{
		int i = (w[2].equalsIgnoreCase("reference") ? 3 : 2);
		float fx = GetFLval(w[i]) * tapefac; 
		float fy = GetFLval(w[i + 1]) * tapefac;
		float fz = GetFLval(w[i + 2]) * tapefac;

		return new OneLeg(w[1], fx, fy, fz, this); // fix type
		}
		catch (NumberFormatException e)
		{
			lis.emitError("Number Format");
		}
		return null; 
	}

	/////////////////////////////////////////////
	public void StarCalibrate(String scaltype, String scalval, String sfacval, LineInputStream lis)
	{
		try
		{
		float fval = GetFLval(scalval); 

		if (scaltype.equalsIgnoreCase("tape"))
			tapenegoffset = fval * tapefac; 
		else if (scaltype.equalsIgnoreCase("compass"))
			compassnegoffset = fval;
		else if (scaltype.equalsIgnoreCase("backcompass"))
			backcompassnegoffset = fval;
		else if (scaltype.equalsIgnoreCase("declination"))
			compassnegoffsetdeclination = fval;
		else if (scaltype.equalsIgnoreCase("clino") || scaltype.equalsIgnoreCase("clinometer"))
			clinonegoffset = fval;
		else if (scaltype.equalsIgnoreCase("depth"))  
		{
			depthnegoffset = fval;
			if (!sfacval.equals(""))
			{
				float facval = GetFLval(sfacval); 
				depthfac = facval; 
			}
		}
		else
			TN.emitWarning("bad *Calibrate type " + scaltype); 
		}
		catch (NumberFormatException e)
		{
			lis.emitError("Number Format"); 
		}
	}

	/////////////////////////////////////////////
	float GetFLval(String s) 
	{
		if (s.equals("+0_0"))
			return 0.0F; 
		float res = Float.parseFloat(s);
		return res; 
	}

	/////////////////////////////////////////////
	public void StarUnits(String sunitype, String sunitval1, String sunitval2, LineInputStream lis)
	{
		String sunitval = sunitval1; 
		float fac = 1.0F; 
		if (!sunitval2.equals(""))
		{
			sunitval = sunitval2; 
			fac = GetFLval(sunitval1);
		} 
		if (sunitype.equalsIgnoreCase("length") || sunitype.equalsIgnoreCase("tape"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				tapefac = TAPEFAC_M * fac; 
			else if (sunitval.equalsIgnoreCase("cm")) 
				tapefac = TAPEFAC_CM * fac;
			else if (sunitval.equalsIgnoreCase("feet"))
				tapefac = TAPEFAC_FT * fac;
			else 
				TN.emitWarning("don't know *Units length " + sunitval1 + "," + sunitval2); 
		}				

		else if (sunitype.equalsIgnoreCase("bearing") || sunitype.equalsIgnoreCase("compass")) 
		{
			assert sunitval2.equals("") || (fac == 1.0); 
			if (sunitval.equalsIgnoreCase("degrees")) 
				compassfac = DEGREES;
			else if (sunitval.equalsIgnoreCase("grads")) 
				compassfac = GRADS; 
			else 
				TN.emitWarning("don't know *Units bearing " + sunitval1 + "," + sunitval2); 
		}

		else if (sunitype.equalsIgnoreCase("gradient") || sunitype.equalsIgnoreCase("clino"))
		{
			assert sunitval2.equals("") || (fac == 1.0); 
			if (sunitval.equalsIgnoreCase("degrees")) 
				clinofac = DEGREES;
			else if (sunitval.equalsIgnoreCase("grads"))
				clinofac = GRADS;
			else if (sunitval.equalsIgnoreCase("percent"))
				clinofac = PERCENT;
			else 
				TN.emitWarning("don't know *Units gradient " + sunitval1 + "," + sunitval2);
		}

		else if (sunitype.equalsIgnoreCase("dx"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				dxfac = TAPEFAC_M * fac; 
			else 
				TN.emitWarning("don't know *Units dx " + sunitval1 + "," + sunitval2); 
		}
		else if (sunitype.equalsIgnoreCase("dy"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				dyfac = TAPEFAC_M * fac; 
			else 
				TN.emitWarning("don't know *Units dy " + sunitval1 + "," + sunitval2); 
		}
		else if (sunitype.equalsIgnoreCase("dz"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				dzfac = TAPEFAC_M * fac; 
			else 
				TN.emitWarning("don't know *Units dz " + sunitval1 + "," + sunitval2); 
		}
		
		else
			TN.emitWarning("don't know *Units type: " + sunitype);
	}

	/////////////////////////////////////////////
	public void StarSet(String sfield, String setting, LineInputStream lis)
	{
		if (sfield.equalsIgnoreCase("decimal"))
			sdecimal = setting;
		else if (sfield.equalsIgnoreCase("blank"))
			sblank = setting;
		else if (sfield.equalsIgnoreCase("names"))
			snames = setting;
		else
			TN.emitWarning("don't know *set " + sfield + " " + setting);
	}

	/////////////////////////////////////////////
	public void StarFlags(String[] w, int iw)
	{
		int i = 1; 
		boolean bflag = true; 
		if (w[1].equalsIgnoreCase("not"))
		{
			bflag = false; 
			i = 2; 
		}
		
		if (w[i].equalsIgnoreCase("surface"))
			bsurface = bflag; 
		else if (w[i].equalsIgnoreCase("duplicate"))
			bduplicate = bflag; 
		else if (w[i].equalsIgnoreCase("duplicate"))
			bsplay = bflag; 
		else if (w[i].equalsIgnoreCase("splay"))
			bsplay = bflag; 
		else
			System.out.println(" unrecognized StarFlags " + w[1]); 
	}
	

	/////////////////////////////////////////////
	// This is programmed to work on the one known example of *Data.
	public boolean StarDataNormal(String[] w, int iw)
	{
		if (w[1].equalsIgnoreCase("default"))
		{
			w[1] = "normal";
			w[2] = "from";
			w[3] = "to";
			w[4] = "tape";
			w[5] = "compass";
			w[6] = "clino";
			iw = 7; 
		}

		datatype = w[1];
		bnosurvey = datatype.equalsIgnoreCase("nosurvey");
		bcartesian = datatype.equalsIgnoreCase("cartesian");
		bpassage = datatype.equalsIgnoreCase("normal") || datatype.equalsIgnoreCase("passage") || datatype.equalsIgnoreCase("diving");
		if (!bnosurvey && !bcartesian && !bpassage)
			TN.emitError("Unrecognized *data command: " + datatype);

        bbaddataline = false;

		// first kill stupid - symbol people keep putting into their commands
		if (w[2].equals("-"))
		{
			TN.emitMessage("Removing stupid '-' symbol from *data Normal line");
			for (int i = 3; i < iw; i++)
				w[i - 1] = w[i];
			iw--;
		}


		int lfromindex = -1;
		int ltoindex = -1;
		int ltapeindex = -1;
		int lcompassindex = -1;
		int lclinoindex = -1;
		int lbackcompassindex = -1;
		int lbackclinoindex = -1;

		int ldxindex = -1;
		int ldyindex = -1;
		int ldzindex = -1;

		int lstationindex = -1;
		int lnewlineindex = -1;
		int ldepthindex = -1;
		int lfromdepthindex = -1;
		int ltodepthindex = -1;

		int lleftindex = -1;
		int lrightindex = -1;
		int lupindex = -1;
		int ldownindex = -1;

		int i;
		for (i = 2; i < iw; i++)
		{
			if (w[i].equalsIgnoreCase("from"))
			{
				if (lfromindex != -1)
					break;
				lfromindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("to"))
			{
				if (ltoindex != -1)
					break;
				ltoindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("length") || w[i].equalsIgnoreCase("tape"))
			{
				if (ltapeindex != -1)
					break;
				ltapeindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("compass") || w[i].equalsIgnoreCase("bearing"))
			{
				if (lcompassindex != -1)
					break;
				lcompassindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("backcompass") || w[i].equalsIgnoreCase("backbearing"))
			{
				if (lbackcompassindex != -1)
					break;
				lbackcompassindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("clino") || w[i].equalsIgnoreCase("gradient"))
			{
				if (lclinoindex != -1)
					break;
				lclinoindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("backclino") || w[i].equalsIgnoreCase("backgradient"))
			{
				if (lbackclinoindex != -1)
					break;
				lbackclinoindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("ignore"))
				;

			else if (w[i].equalsIgnoreCase("ignoreall"))
				;

			else if (w[i].equalsIgnoreCase("description"))
				;

			else if (w[i].equalsIgnoreCase("remarks"))
				;

			else if (w[i].equalsIgnoreCase("newline"))
			{
				if (lnewlineindex != -1)
					break;
				lnewlineindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("dx") || w[i].equalsIgnoreCase("easting"))
			{
				if (ldxindex != -1)
					break;
				ldxindex = i - 2;
			}
			else if (w[i].equalsIgnoreCase("dy") || w[i].equalsIgnoreCase("northing"))
			{
				if (ldyindex != -1)
					break;
				ldyindex = i - 2;
			}
			else if (w[i].equalsIgnoreCase("dz") || w[i].equalsIgnoreCase("altitude"))
			{
				if (ldzindex != -1)
					break;
				ldzindex = i - 2;
			}

			// from becomes station.
			else if (w[i].equalsIgnoreCase("station"))
			{
				if (lstationindex != -1)
					break;
				lstationindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("depth"))
			{
				if (ldepthindex != -1)
					break;
				ldepthindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("fromdepth"))
			{
				if (lfromdepthindex != -1)
					break;
				lfromdepthindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("todepth"))
			{
				if (ltodepthindex != -1)
					break;
				ltodepthindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("left"))
			{
				if (lleftindex != -1)
					break;
				lleftindex = i - 2;
			}
			else if (w[i].equalsIgnoreCase("right"))
			{
				if (lrightindex != -1)
					break;
				lrightindex = i - 2;
			}
			else if (w[i].equalsIgnoreCase("up"))
			{
				if (lupindex != -1)
					break;
				lupindex = i - 2;
			}
			else if (w[i].equalsIgnoreCase("down"))
			{
				if (ldownindex != -1)
					break;
				ldownindex = i - 2;
			}

			else
			{
				TN.emitWarning("!!! " + w[i] + " " + i);
				break;
			}
		}

		// incomplete.
		if (i != iw)
        {
            bbaddataline = true;
			return false;
        }

		boolean bstandardform = ((lfromindex != -1) && (ltoindex != -1) && (ltapeindex != -1) && (lcompassindex != -1) && (lclinoindex != -1));
		boolean bcartesianform = (bcartesian && (ldxindex != -1) && (ldyindex != -1) && (ldzindex != -1));
		boolean bdivingform = ((ltapeindex != -1) && (lcompassindex != -1) && (ldepthindex != -1) && (lstationindex != -1) && (lnewlineindex != -1));
		boolean bldivingform = ((lfromindex != -1) && (ltoindex != -1) && (ltapeindex != -1) && (lcompassindex != -1) && (lfromdepthindex != -1) && (ltodepthindex != -1));
		boolean bllussform = ((lstationindex != -1) && (lnewlineindex != -1) && (ltapeindex != -1) && (lcompassindex != -1) && (lclinoindex != -1));
		boolean blpassageform = ((lstationindex != -1) && (lleftindex != -1) && (lrightindex != -1) && (lupindex != -1) && (ldownindex != -1));
		boolean blbnosurvey = (bnosurvey && ((lstationindex != -1) || ((lfromindex != -1) && (ltoindex != -1))) && (ltapeindex == -1) && (lcompassindex == -1) && (lfromdepthindex == -1) && (ltodepthindex == -1));

		// bad line
		if (!bstandardform && !bcartesianform && !bdivingform && !bldivingform && !blpassageform && !blbnosurvey && !bllussform)
		{
			TN.emitMessage("Indexes,  station:" + lstationindex + " from:" + lfromindex + " to:" + ltoindex + " NEWLINE:" + lnewlineindex + " tape:" + ltapeindex + " compass:" + lcompassindex + " clino:" + lclinoindex + " bnosurvey="+bnosurvey);
			bbaddataline = true;
			return false;
		}

		fromindex = lfromindex;
		toindex = ltoindex;
		tapeindex = ltapeindex;
		compassindex = lcompassindex;
		clinoindex = lclinoindex;
		backcompassindex = lbackcompassindex;
		backclinoindex = lbackclinoindex;
		dxindex = ldxindex;
		dyindex = ldyindex;
		dzindex = ldzindex;

		stationindex = lstationindex;
		depthindex = ldepthindex;
		fromdepthindex = lfromdepthindex;
		todepthindex = ltodepthindex;
		newlineindex = lnewlineindex;

		leftindex = lleftindex;
		rightindex = lrightindex;
		upindex = lupindex;
		downindex = ldownindex;
		
		bbaddataline = false;
		return true;
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////

}


