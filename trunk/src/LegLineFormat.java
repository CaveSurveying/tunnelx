////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
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

	int fromindex = 0; 
	int toindex = 1; 

	int tapeindex = 2; 
	float tapeoffset = 0; 
	float tapefac = 1.0F; 

	int compassindex = 3; 
	float compassoffset = 0; 
	int compassfac = DEGREES; 

	int clinoindex = 4; 
	float clinooffset = 0; 
	int clinofac = DEGREES; 

	String sdecimal = null; 
	String sblank = null;	// this may need to be mapped into the word splitter.  

	File currfile = null; 

	/////////////////////////////////////////////
	LegLineFormat()
	{;} 

	/////////////////////////////////////////////
	LegLineFormat(LegLineFormat f) 
	{
		if (f != null)
		{
			fromindex = f.fromindex; 
			toindex = f.toindex; 

			tapeindex = f.tapeindex; 
			tapeoffset = f.tapeoffset; 
			tapefac = f.tapefac; 

			compassindex = f.compassindex; 
			compassoffset = f.compassoffset; 
			compassfac = f.compassfac; 
			
			clinoindex = f.clinoindex; 
			clinooffset = f.clinooffset; 
			clinofac = f.clinofac; 

			sdecimal = f.sdecimal; 
			sblank = f.sblank; 

			currfile = f.currfile; 
		}
	}

	/////////////////////////////////////////////
	void AppendStarNonDefaults(OneTunnel ot) 
	{
// lots of if statments outputting the defaults.  
// we can then get rid of the original LegLineFormat at the start.  
		ot.AppendLine(";*calibrate differences from defaults");  
	}

	/////////////////////////////////////////////
	public String toString() 
	{
		StringBuffer sb = new StringBuffer(); 
		sb.append("normal"); 
		for (int i = 0; i < 5; i++) 
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
			else 
				sb.append(" ignore"); 
		}
		
		sb.append("  file: " + (currfile == null ? "null" : currfile.getName())); 
		return sb.toString(); 
	}

	/////////////////////////////////////////////
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
	OneLeg ReadLeg(String w[], OneTunnel lgtunnel)
	{
		String atape = ApplySet(w[tapeindex]); 
		float tape = (Float.valueOf(atape).floatValue() + tapeoffset) * tapefac; 

		float compass; 
		String acompass = ApplySet(w[compassindex]); 
		boolean bcblank = (acompass.equalsIgnoreCase("-") || acompass.equals("")); 
		
		compass = (bcblank ? 0.0F : Float.valueOf(acompass).floatValue()) + compassoffset; 
		if (compassfac == GRADS) 
			compass *= 360.0F / 400.0F; 
		
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
				clino = Float.valueOf(aclino).floatValue(); 
			clino += clinooffset; 
			if (clinofac == GRADS) 
				clino *= 360.0F / 400.0F; 
		}

		if (bcblank && ((clino != -90.0F) && (clino != 90.0F))) 
			System.out.println("Error, blank compass on non-vertical leg " + w[0] + " " + w[1] + " " + w[2] + " " + w[3] + " " + w[4] + " " + w[5]); 

		return new OneLeg(w[fromindex], w[toindex], tape, compass, clino, lgtunnel); 
	}

	/////////////////////////////////////////////
	OneLeg ReadFix(String w[], OneTunnel lgtunnel)
	{
		//System.out.println("*fix " + w[1]); 
		float fx = Float.valueOf(w[2]).floatValue() * tapefac; 
		float fy = Float.valueOf(w[3]).floatValue() * tapefac; 
		float fz = Float.valueOf(w[4]).floatValue() * tapefac; 

		return new OneLeg(w[1], fx, fy, fz, lgtunnel); // fix type
	}


	/////////////////////////////////////////////
	public void StarCalibrate(String scaltype, String scalval)
	{
		float fval = Float.valueOf(scalval).floatValue(); 
		if (scaltype.equalsIgnoreCase("tape"))
			tapeoffset = fval * tapefac; 
		else if (scaltype.equalsIgnoreCase("compass") || scaltype.equalsIgnoreCase("declination"))
			compassoffset = fval; 
		else if (scaltype.equalsIgnoreCase("clino") || scaltype.equalsIgnoreCase("clinometer")) 
			clinooffset = fval; 
		else 
			System.out.println("bad *Calibrate type " + scaltype); 
	}

	/////////////////////////////////////////////
	float GetFLval(String s) 
	{
		try
		{
			float res = Float.valueOf(s).floatValue(); 
			return res; 
		}
		catch (NumberFormatException e) 
		{;} 
		return -1.0F; 
	}

	/////////////////////////////////////////////
	public void StarUnits(String sunitype, String sunitval)
	{
		if (sunitype.equalsIgnoreCase("length"))
		{
			if (sunitval.equalsIgnoreCase("metres") || sunitval.equalsIgnoreCase("meters")) 
				tapefac = 1; 
			else if (sunitval.equalsIgnoreCase("cm")) 
				tapefac = 0.01F; 
			else if (sunitval.equalsIgnoreCase("feet")) 
				tapefac = (float)0.3048; 
			else 
			{
				float ltapefac = GetFLval(sunitval); 
				if (ltapefac > 0.0F) 
					tapefac = ltapefac; 
				else 
					System.out.println("don't know *Units length " + sunitval); 
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
				System.out.println("don't know *Units bearing " + sunitval); 
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
				System.out.println("don't know *Units gradient " + sunitval); 
		}
		else 
			System.out.println("don't know *Units type: " + sunitype); 
	}

	/////////////////////////////////////////////
	public void StarSet(String sfield, String setting)  
	{
		if (sfield.equalsIgnoreCase("decimal")) 
			sdecimal = setting; 
		else if (sfield.equalsIgnoreCase("blank")) 
			sblank = setting; 
		else 
			System.out.println("don't know *set " + sfield + " " + setting); 
	}

	/////////////////////////////////////////////
	// This is programmed to work on the one known example of *Data.  
	public boolean StarDataNormal(String[] w, int iw)  
	{
		// first kill stupid - symbol people keep putting into their commands 
		if (w[2].equals("-"))  
		{
			// System.out.println("Removing stupid '-' symbol from *data Normal line"); 
			for (int i = 3; i < iw; i++) 
				w[i - 1] = w[i]; 
			iw--; 
		}


		int lfromindex = -1; 
		int ltoindex = -1; 
		int ltapeindex = -1; 
		int lcompassindex = -1; 
		int lclinoindex = -1; 

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
			{
				i = iw; 
				break; 
			}

			else 
				break; 
		}

		// bad line 
		if (i != iw) 
			System.out.println(i); 
		if ((lfromindex == -1) || (ltoindex == -1) || (ltapeindex == -1) || (lcompassindex == -1) || (lclinoindex == -1))  
		{
			System.out.println(lfromindex); 
			System.out.println(ltoindex); 
			System.out.println(ltapeindex); 
			System.out.println(lcompassindex); 
			System.out.println(lclinoindex); 
		}

		if ((i != iw) || (lfromindex == -1) || (ltoindex == -1) || (ltapeindex == -1) || (lcompassindex == -1) || (lclinoindex == -1))  
			return false; 

		fromindex = lfromindex; 
		toindex = ltoindex; 
		tapeindex = ltapeindex; 
		compassindex = lcompassindex; 
		clinoindex = lclinoindex; 

		return true; 
	}

	/////////////////////////////////////////////
	/////////////////////////////////////////////

}

