////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.IOException;
import java.io.BufferedReader; 
import java.io.FileReader; 
import java.io.File; 
import java.io.StringReader; 

//
//
// LineInputStream
//
//

/////////////////////////////////////////////
public class LineInputStream extends BufferedReader
{
	private String bufferline = ""; 
	private String unfetchline = null; 

	// the expansion of the bufferline  
	int MAX_WORDS = 12; 
	public int iwc = MAX_WORDS; // the first clear element of w.  
	public String w[] = new String[MAX_WORDS]; 
	public String remainder = ""; // this is rest of line after the first word (usually for titles).  
	public String remainder1 = ""; 
	public String remainder2 = ""; 
	public String comment = ""; 

	File loadfile = null; 
	String slash; 	 
	int nlineno; 

	// botch stuff to convert *prefix code to a *begin *end couplet.  
	String prefixconversion; 

	/////////////////////////////////////////////
	public LineInputStream(File lloadfile, String lslash, String lprefixconversion) throws IOException  
	{
 		super(new FileReader(lloadfile.getPath()));
		slash = lslash; 
		loadfile = lloadfile; 
		nlineno = 0; 
		prefixconversion = lprefixconversion; 
		// System.out.println(loadfile.getName() + " Slash:" + slash); 
		if (prefixconversion != null) 
			System.out.println("  prefixconversion: " + prefixconversion); 
	}

	/////////////////////////////////////////////
	public LineInputStream(String text)
	{
		super(new StringReader(text));
		SplitWords("", false); // clears the array.   
	}


	/////////////////////////////////////////////
	public boolean FetchNextLine()  
	{
		if (unfetchline != null) 
		{
			bufferline = unfetchline; 
			unfetchline = null; 
			SplitWords(bufferline, true); 
			return true; 
		}

		try
		{
			bufferline = readLine(); 
			nlineno++; 
		}
		catch (IOException ioe)
		{
			System.out.println("IOException thrown in readLine()"); 
			bufferline = null; 
		}

		if (bufferline != null)
		{
			SplitWords(bufferline, true); 
			return true; 
		}
		return false; 
	}

	/////////////////////////////////////////////
	public void UnFetch() 
	{
		if (unfetchline != null) 
			System.out.println("Can't unfetchline twice"); 
		unfetchline = bufferline; 
	}

	/////////////////////////////////////////////
	public String GetLine()
	{
		if (bufferline == null)
			System.out.println("Error: null value passed back from GetLine()"); 
		return bufferline; 
	}

	/////////////////////////////////////////////
	public void SplitWords(String sline, boolean bRepErrors) 
	{
		int pc = sline.indexOf(';'); 
		if (pc == -1)
			pc = sline.length(); 
		comment = sline.substring(pc); 

		remainder = sline.substring(0, pc).trim(); 
		remainder1 = ""; 
		remainder2 = ""; 
		int iw = 0; 
		while ((remainder.length() != 0) && (iw  < MAX_WORDS))  
		{
			if ((remainder.length() == 1) || (remainder.charAt(0) != '"')) 
			{
				int ps = remainder.indexOf(' '); 
				int pt = remainder.indexOf('\t'); 
				int pm = remainder.indexOf(','); 
				int peow = (ps == -1 ? remainder.length() : ps); 
				if ((pt != -1) && (pt < peow)) 
					peow = pt; 
				if ((pm != -1) && (pm < peow)) 
					peow = pm; 
				w[iw] = remainder.substring(0, peow); 
				iw++; 
				if (peow == remainder.length()) 
					break; 
				remainder = remainder.substring(peow + 1).trim(); 
			}
			else 
			{
				int pq = remainder.indexOf('"', 1); 
				if (pq == -1) 
				{
					if (bRepErrors) 
						System.out.println("missing close quote in line: " + sline); 
					break; 
				}
				w[iw] = remainder.substring(1, pq); 
				iw++; 
				remainder.substring(pq + 1).trim(); 
			}
			if (iw == 1) 
				remainder1 = remainder; 
			if (iw == 2) 
				remainder2 = remainder; 
		}

		for (int i = iw; i < iwc; i++)
			w[i] = ""; 

		iwc = iw; 
	}
}

