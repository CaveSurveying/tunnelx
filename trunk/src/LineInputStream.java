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
		// TN.emitMessage(loadfile.getName() + " Slash:" + slash);
		if (prefixconversion != null)
			TN.emitMessage("  prefixconversion: " + prefixconversion);
	}

	/////////////////////////////////////////////
	public LineInputStream(String text, File lloadfile)
	{
		super(new StringReader(text));
		loadfile = lloadfile; // for error messages (to give the right name)
		SplitWords("", false); // clears the array.
	}


	/////////////////////////////////////////////
	// newly added function which should be used everywhere in the line reading.
	void emitError(String mess)
	{
		// avoiding repeat errors for now
		if (loadfile != null)
			TN.emitError("File " + (loadfile == null ? "" : loadfile.getName()) + ", line " + nlineno + ", " + mess + "\n" + GetLine());
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
			TN.emitError("IOException thrown in readLine()");
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
	public boolean FetchNextLineNoSplit()
	{
		if (unfetchline != null)
		{
			bufferline = unfetchline;
			unfetchline = null;
			return true;
		}

		try
		{
			bufferline = readLine();
			nlineno++;
		}
		catch (IOException ioe)
		{
			TN.emitError("IOException thrown in readLine()");
			bufferline = null;
		}

		if (bufferline != null)
		{
			return true;
		}
		return false;
	}



	/////////////////////////////////////////////
	public void UnFetch()
	{
		if (unfetchline != null)
			TN.emitWarning("Can't unfetchline twice");
		unfetchline = bufferline;
	}

	/////////////////////////////////////////////
	public String GetLine()
	{
		if (bufferline == null)
			TN.emitMessage("Error: null value passed back from GetLine()");
		return bufferline;
	}

	/////////////////////////////////////////////
	public void SplitWords(String sline, boolean bRepErrors)
	{
		comment = "";

		remainder = sline.trim();
		remainder1 = "";
		remainder2 = "";
		int iw = 0;
		while ((remainder.length() != 0) && (iw  < MAX_WORDS))
		{
			if (remainder.charAt(0) == ';')
			{
				comment = remainder.substring(1);
				break;
			}

			if ((remainder.length() == 1) || (remainder.charAt(0) != '"'))
			{
				int ps = remainder.indexOf(' ');
				int pt = remainder.indexOf('\t');
				int pc = remainder.indexOf(',');
				int psc = remainder.indexOf(';');

				int peow = remainder.length();
				int pbnw = peow;

				if ((ps != -1))
				{
					peow = ps;
					pbnw = ps;
				}

				if ((pt != -1) && (pt < peow))
				{
					peow = pt;
					pbnw = pt;
				}

				if ((pc != -1) && (pc < peow))
				{
					peow = pc;
					pbnw = pc + 1;
				}

				if ((psc != -1) && (psc < peow))
				{
					peow = psc;
					pbnw = psc;
				}

				w[iw] = remainder.substring(0, peow);
				iw++;

				if (pbnw == remainder.length())
					break;
				remainder = remainder.substring(pbnw).trim();
			}
			else
			{
				int pq = remainder.indexOf('"', 1);
				if (pq == -1)
				{
					if (bRepErrors)
						TN.emitMessage("missing close quote in line: " + sline);
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

