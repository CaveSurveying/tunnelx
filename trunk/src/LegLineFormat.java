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

import java.io.File;

//
//
// LegLineFormat
//
//
public class LegLineFormat implements Cloneable
{
	static int DEGREES = 0; 
	static int GRADS = 1; 
	static int PERCENT = 2; 

	static float TAPEFAC_M = 1.0F; 
	static float TAPEFAC_CM = 0.01F;
	static float TAPEFAC_FT = 0.3048F;

	String datatype = "normal"; // or diving, cartesian, nosurvey
	boolean bnosurvey = false;

	int fromindex = 0;
	int toindex = 1;

	int tapeindex = 2;
	float tapenegoffset = 0.0F;
	float tapefac = 1.0F;

	int compassindex = 3; 
	float compassnegoffset = 0.0F; 
	int compassfac = DEGREES; 

	int clinoindex = 4;
	float clinonegoffset = 0.0F; 
	int clinofac = DEGREES; 

	int depthindex = -1; 
	int fromdepthindex = -1;
	int todepthindex = -1; 
	float depthnegoffset = 0; 
	float depthfac = 1.0F;

	int stationindex = -1; 

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


	// local data used for multi-line (diving) type data.  
	String lstation = null; 
	float ldepth = 0; 
	float lcompass = 0; 
	float ltape = 0;

	float ldx = 0; // the cartesian mode.  
	float ldy = 0; 
	float ldz = 0; 

	int currnewlineindex = 0; 
	File currfile; 

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

			fromindex = f.fromindex;
			toindex = f.toindex;

			tapeindex = f.tapeindex;
			tapenegoffset = f.tapenegoffset;
			tapefac = f.tapefac;

			compassindex = f.compassindex;
			compassnegoffset = f.compassnegoffset;
			compassfac = f.compassfac;

			clinoindex = f.clinoindex;
			clinonegoffset = f.clinonegoffset; 
			clinofac = f.clinofac; 

			newlineindex = f.newlineindex;

			stationindex = f.stationindex; 

			depthindex = f.depthindex;
			fromdepthindex = f.fromdepthindex;
			todepthindex = f.todepthindex;
			depthnegoffset = f.depthnegoffset;
			depthfac = f.depthfac;

			sdecimal = f.sdecimal;
			sblank = f.sblank;
			snames = f.snames;

			bb_svxdate = f.bb_svxdate;
			bb_svxtitle = f.bb_svxtitle;
			bb_teamtape = f.bb_teamtape;
			bb_teampics = f.bb_teampics;
			bb_teaminsts = f.bb_teaminsts;
			bb_teamnotes = f.bb_teamnotes;
		}
	}

	/////////////////////////////////////////////
	// called at the beginning of every Tunnel and after the end of a blank *begin block.
	void AppendStarDifferences(OneTunnel ot, LegLineFormat llfr, boolean bForceAll)
	{
		if (bForceAll || (fromindex != llfr.fromindex) || (toindex != llfr.toindex) || (tapeindex != llfr.tapeindex) || (compassindex != llfr.compassindex) || (clinoindex != llfr.clinoindex))
		{
			ot.Append("*data ");
			ot.AppendLine(toString());
		}

		// not safe to do independently because might be out of order.
		if (bForceAll || (tapefac != llfr.tapefac) || (tapenegoffset != llfr.tapenegoffset))
		{
			ot.Append("*units length ");
			if (tapefac == TAPEFAC_M)
				ot.AppendLine("metres");
			else if (tapefac == TAPEFAC_CM) 
				ot.AppendLine("cm"); 
			else if (tapefac == TAPEFAC_FT) 
				ot.AppendLine("feet"); 
			else 
				ot.AppendLine(String.valueOf(tapefac)); 

			ot.Append("*calibrate tape "); 
			ot.AppendLine(String.valueOf(tapenegoffset / tapefac)); 
		}

		if (bForceAll || (compassfac != llfr.compassfac) || (compassnegoffset != llfr.compassnegoffset))
		{
			ot.Append("*units compass "); 
			ot.AppendLine(compassfac == DEGREES ? "degrees" : "grads"); 
			ot.Append("*calibrate compass "); 
			ot.AppendLine(String.valueOf(compassnegoffset)); 
		}

		if (bForceAll || (clinofac != llfr.clinofac) || (clinonegoffset != llfr.clinonegoffset))  
		{
			ot.Append("*units clino "); 
			ot.AppendLine(clinofac == DEGREES ? "degrees" : "grads"); 
			ot.Append("*calibrate clino "); 
			ot.AppendLine(String.valueOf(clinonegoffset)); 
		}

		if ((depthfac != llfr.depthfac) || (depthnegoffset != llfr.depthnegoffset))  
		{
			ot.Append("*calibrate depth "); 
			ot.Append(String.valueOf(depthnegoffset));
			ot.Append(" "); 
			ot.AppendLine(String.valueOf(depthfac)); 
		}

		// the set function too?  


		// the other * carry-overs 
		if (!bb_svxdate.equals(llfr.bb_svxdate))
			ot.AppendLine("*date " + bb_svxdate);
		if (!bb_svxtitle.equals(llfr.bb_svxtitle))
			ot.AppendLine("*title " + "\"" + bb_svxtitle + "\"");
		if (!bb_teamtape.equals(llfr.bb_teamtape))
			ot.AppendLine("*team tape " + bb_teamtape);
		if (!bb_teampics.equals(llfr.bb_teampics))
			ot.AppendLine("*team pics " + bb_teampics);
		if (!bb_teaminsts.equals(llfr.bb_teaminsts))
			ot.AppendLine("*team insts " + bb_teaminsts);
		if (!bb_teamnotes.equals(llfr.bb_teamnotes))
			ot.AppendLine("*team notes " + bb_teamnotes); 
	}

	/////////////////////////////////////////////
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(datatype);

		int im = Math.max(Math.max(fromindex, toindex), Math.max(Math.max(tapeindex, compassindex), clinoindex));
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
			else
				sb.append(" ignore");
		}
		sb.append(" ignoreall");

		return sb.toString();
	}

	/////////////////////////////////////////////
	// this substites characters in these strings with ones that make them parsable.
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
	OneLeg ReadLeg(String w[], OneTunnel lgtunnel, LineInputStream lis)
	{
		try
		{
		// good old fashioned leg format with everything there.
		if ((newlineindex == -1) && (stationindex == -1))
		{
			if (bnosurvey)
{
System.out.println("nosurvey " + stationindex); 
				return new OneLeg(w[fromindex], w[toindex], lgtunnel);
}

			String atape = ApplySet(w[tapeindex]);
			float tape = (GetFLval(atape) - tapenegoffset) * tapefac;

			float compass;
			String acompass = ApplySet(w[compassindex]);
			boolean bcblank = (acompass.equalsIgnoreCase("-") || acompass.equals(""));

			compass = (bcblank ? 0.0F : GetFLval(acompass)) - compassnegoffset;
			if (compassfac == GRADS)
				compass *= 360.0F / 400.0F;

			if (clinoindex != -1)
			{
				float clino;
				String aclino = ApplySet(w[clinoindex]);
				if (aclino.equalsIgnoreCase("up") || aclino.equalsIgnoreCase("u"))
					clino = 90.0F;
				else if (aclino.equalsIgnoreCase("down") || aclino.equalsIgnoreCase("d"))
					clino = -90.0F;
				else
				{
					if (aclino.equalsIgnoreCase("-") || aclino.equalsIgnoreCase("h"))
						clino = 0.0F;
					else
						clino = GetFLval(aclino);
					clino -= clinonegoffset;
					if (clinofac == GRADS)
						clino *= 360.0F / 400.0F;
				}

				if (bcblank && ((clino != -90.0F) && (clino != 90.0F)))
					TN.emitWarning("Error, blank compass on non-vertical leg " + w[0] + " " + w[1] + " " + w[2] + " " + w[3] + " " + w[4] + " " + w[5]);

				return new OneLeg(w[fromindex], w[toindex], tape, compass, clino, lgtunnel);
			}

			if ((fromdepthindex != -1) && (todepthindex != -1))
			{
				String afromdepth = ApplySet(w[fromdepthindex]);
				float fromdepth = GetFLval(afromdepth);
				String atodepth = ApplySet(w[fromdepthindex]);
				float todepth = GetFLval(atodepth);

				TN.emitMessage("LDIVING " + w[fromindex] + "  " + w[toindex] + "  " + tape + "  " + compass + "  " + fromdepth + "  " + todepth);

				return new OneLeg(w[fromindex], w[toindex], tape, compass, fromdepth, todepth, lgtunnel);
			}
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

			if ((depthindex < nextnewlineindex) && (depthindex >= currnewlineindex))  // currnewlineindex is 0 in this case.
			{
				String adepth = ApplySet(w[depthindex - currnewlineindex]);
				lnewdepth = (GetFLval(adepth) + depthnegoffset) * depthfac;
			}

			// build the result.
			OneLeg olres = null;
			if ((lnewstation != null) && (lstation != null))  // and the rest.
			{
				olres = new OneLeg(lstation, lnewstation, ltape, lcompass, ldepth, lnewdepth, lgtunnel);
TN.emitMessage("DIVING " + lstation + "  " + lnewstation + "  " + ltape + "  " + lcompass + "  " + ldepth + "  " + lnewdepth);
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

				lcompass = (bcblank ? 0.0F : GetFLval(acompass)) + compassnegoffset;
				if (compassfac == GRADS)
					lcompass *= 360.0F / 400.0F;
			}

			// update the lineindex.
			currnewlineindex = (currnewlineindex == 0 ? (newlineindex + 1) : 0);

			// not properly implemented case (errors not mapping across lines).
			return olres;
		}
		TN.emitWarning("Can't do format");

		}
		catch (NumberFormatException e)
		{
			lis.emitError("Number Format");
		}
		return null;
	}

	/////////////////////////////////////////////
	OneLeg ReadFix(String w[], OneTunnel lgtunnel, boolean bPosfix, LineInputStream lis)
	{
		try
		{
		int i = (w[2].equalsIgnoreCase("reference") ? 3 : 2); 
		float fx = GetFLval(w[i]) * tapefac; 
		float fy = GetFLval(w[i + 1]) * tapefac; 
		float fz = GetFLval(w[i + 2]) * tapefac; 

		return new OneLeg(w[1], fx, fy, fz, lgtunnel, bPosfix); // fix type
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
		else if (scaltype.equalsIgnoreCase("compass") || scaltype.equalsIgnoreCase("declination"))
			compassnegoffset = fval;
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
		float res = Float.valueOf(s).floatValue(); 
		return res; 
	}

	/////////////////////////////////////////////
	public void StarUnits(String sunitype, String sunitval, LineInputStream lis)
	{
		if (sunitype.equalsIgnoreCase("length") || sunitype.equalsIgnoreCase("tape"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				tapefac = TAPEFAC_M; 
			else if (sunitval.equalsIgnoreCase("cm")) 
				tapefac = TAPEFAC_CM;
			else if (sunitval.equalsIgnoreCase("feet"))
				tapefac = TAPEFAC_FT; 
			else 
			{
				float ltapefac = GetFLval(sunitval); 
				if (ltapefac > 0.0F) 
					tapefac = ltapefac; 
				else 
					TN.emitWarning("don't know *Units length " + sunitval); 
			}
		}				

		else if (sunitype.equalsIgnoreCase("bearing") || sunitype.equalsIgnoreCase("compass")) 
		{
			if (sunitval.equalsIgnoreCase("degrees")) 
				compassfac = DEGREES; 
			else if (sunitval.equalsIgnoreCase("grads")) 
				compassfac = GRADS; 
			else if (GetFLval(sunitval) == 1.0F)
				compassfac = DEGREES;
			else 
				TN.emitWarning("don't know *Units bearing " + sunitval); 
		}

		else if (sunitype.equalsIgnoreCase("gradient") || sunitype.equalsIgnoreCase("clino")) 
		{
			if (sunitval.equalsIgnoreCase("degrees")) 
				clinofac = DEGREES; 
			else if (sunitval.equalsIgnoreCase("grads"))
				clinofac = GRADS; 
			else if (GetFLval(sunitval) == 1.0F) 
				clinofac = DEGREES; 
			else 
				TN.emitWarning("don't know *Units gradient " + sunitval); 
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
	// This is programmed to work on the one known example of *Data.
	public boolean StarDataNormal(String[] w, int iw)
	{
		datatype = w[1];
		bnosurvey = datatype.equalsIgnoreCase("nosurvey");

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
		int lstationindex = -1;
		int lnewlineindex = -1;
		int ldepthindex = -1;
		int lfromdepthindex = -1;
		int ltodepthindex = -1;

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

			else if (w[i].equalsIgnoreCase("bearing") || w[i].equalsIgnoreCase("compass"))
			{
				if (lcompassindex != -1)
					break;
				lcompassindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("gradient") || w[i].equalsIgnoreCase("clino"))
			{
				if (lclinoindex != -1)
					break;
				lclinoindex = i - 2;
			}

			else if (w[i].equalsIgnoreCase("ignore"))
				;

			else if (w[i].equalsIgnoreCase("ignoreall"))
				;

			else if (w[i].equalsIgnoreCase("newline"))
			{
				if (lnewlineindex != -1)
					break;
				lnewlineindex = i - 2;
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

			else
			{
				TN.emitWarning("!!! " + w[i] + " " + i);
				break;
			}
		}

		// incomplete.
		if (i != iw)
			return false;

		boolean bstandardform = ((lfromindex != -1) && (ltoindex != -1) && (ltapeindex != -1) && (lcompassindex != -1) && (lclinoindex != -1));
		boolean bdivingform = ((ltapeindex != -1) && (lcompassindex != -1) && (ldepthindex != -1) && (lstationindex != -1) && (lnewlineindex != -1));
		boolean bldivingform = ((lfromindex != -1) && (ltoindex != -1) && (ltapeindex != -1) && (lcompassindex != -1) && (lfromdepthindex != -1) && (ltodepthindex != -1));
		boolean blbnosurvey = (bnosurvey && (lfromindex != -1) && (ltoindex != -1) && (ltapeindex == -1) && (lcompassindex == -1) && (lfromdepthindex == -1) && (ltodepthindex == -1));

		// bad line
		if (!bstandardform && !bdivingform && !bldivingform && !blbnosurvey)
		{
			TN.emitMessage("Indexes From " + lfromindex + " to " + ltoindex + " tape " + ltapeindex + " compass " + lcompassindex + " clino " + lclinoindex);
			return false;
		}

		fromindex = lfromindex;
		toindex = ltoindex;
		tapeindex = ltapeindex;
		compassindex = lcompassindex;
		clinoindex = lclinoindex;
		stationindex = lstationindex;
		depthindex = ldepthindex;
		fromdepthindex = lfromdepthindex;
		todepthindex = ltodepthindex;
		newlineindex = lnewlineindex;
		return true;
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////

}


