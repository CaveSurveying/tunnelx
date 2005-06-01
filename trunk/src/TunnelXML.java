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
import java.io.IOException;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.StreamTokenizer;


/////////////////////////////////////////////
// local class of the parser.
class TunnelXML
{
	TunnelXMLparsebase txp;
	StreamTokenizer st;
	String erm = null;
	File ssfile = null;


	/////////////////////////////////////////////
	TunnelXML()
	{
	}


	/////////////////////////////////////////////
	// looks for the object type listed after the tunnelxml
	char[] filehead = new char[256];
	String strtunnxml = "<tunnelxml>";
	String GetFileType(File sfile)
	{
		String sfilehead = null;
		try
		{
	 		FileReader fr = new FileReader(sfile);
			int lfilehead = fr.read(filehead, 0, filehead.length);
			fr.close();
			if (lfilehead == -1)
				return null;
			sfilehead = new String(filehead, 0, lfilehead);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
		}
		int itunnxml = sfilehead.indexOf(strtunnxml);
		if (itunnxml == -1)
			return null;

// this should be quitting when it gets to a space or a closing >
		int bracklo = sfilehead.indexOf('<', itunnxml + strtunnxml.length());
		int brackhi = sfilehead.indexOf('>', bracklo + 1);
		if ((bracklo == -1) || (brackhi == -1))
			return null;
		String sres = sfilehead.substring(bracklo + 1, brackhi);
		return sres;
	}

	/////////////////////////////////////////////
	boolean ParseFile(TunnelXMLparsebase ltxp, File sfile)
	{
		ssfile = sfile;
		boolean bRes = false;
		try
		{
	 		BufferedReader br = new BufferedReader(new FileReader(sfile));
			String erm = ParseReader(ltxp, br, true);
			if (erm != null)
				TN.emitError(erm + " on line " + st.lineno() + " of " + sfile.getName());
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
		}
		return bRes;
	}

	/////////////////////////////////////////////
	boolean ParseString(TunnelXMLparsebase ltxp, String stxt)
	{
		boolean bRes = false;
		try
		{
			String erm = ParseReader(ltxp, new StringReader(stxt), false);
			if (erm != null)
				TN.emitWarning(erm + " in: " + stxt);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
            e.printStackTrace();
			System.exit(0);
		}
		return bRes;
	}

	/////////////////////////////////////////////
	String ParseReader(TunnelXMLparsebase ltxp, Reader br, boolean bOfFile) throws IOException
	{
  		txp = ltxp;

		st = new StreamTokenizer(br);

		st.resetSyntax();
		st.whitespaceChars('\u0000', '\u0020');
		st.wordChars('A', 'Z');
		st.wordChars('a', 'z');
		st.wordChars('0', '9');
		st.wordChars('\u00A0', '\u00FF');

		// we don't implement XML entities since label text gets mangled on its own, and everything else is data not text.
		String swdchs = ".-_+^:;|*()[]{}&%$!,";
		for (int i = 0; i < swdchs.length(); i++)
			st.wordChars(swdchs.charAt(i), swdchs.charAt(i));

		st.quoteChar('"');
		if (bOfFile)
			st.quoteChar('\''); // this is converted to a word-char right after the header
		else
			st.wordChars('\'', '\'');

		erm = ParseTokens(st);
 		br.close();
		return erm;
	}


	static int AS_OUTSIDE = 0;
	static int AS_FIRST_OPEN = 1;
	static int AS_END_ELEMENT_SLASH = 2;
	static int AS_END_ELEMENT_EMITTED = 3;
	static int AS_START_ELEMENT = 4;
	static int AS_ATT_SEQ_OUTSIDE = 5;
	static int AS_QM_HEADER = 6;
	static int AS_ATT_SEQ_EQ = 7;
	static int AS_ATT_SEQ_SET = 8;


	int mAngleBracketState = AS_OUTSIDE;
	String name;
	String attname;
	/////////////////////////////////////////////
	String ParseTokens(StreamTokenizer st) throws IOException
	{
		mAngleBracketState = AS_OUTSIDE;

		while (st.nextToken() != StreamTokenizer.TT_EOF)
		{
			//TN.emitMessage("lineno: " + st.lineno() + "  state: " + mAngleBracketState);
			switch (st.ttype)
			{
			case '?':
				if (mAngleBracketState == AS_FIRST_OPEN)
					mAngleBracketState = AS_QM_HEADER;
				else if (mAngleBracketState == AS_QM_HEADER)
				{
					mAngleBracketState = AS_END_ELEMENT_EMITTED;
					st.wordChars('\'', '\'');
				}
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters("?");
				else
					return "Angle ? Brackets mismatch";
				break;

			case '<':
				if (mAngleBracketState != AS_OUTSIDE)
					return "Angle Brackets mismatch";
				mAngleBracketState = AS_FIRST_OPEN;
				break;

			case '/':
				if (mAngleBracketState == AS_FIRST_OPEN)
					mAngleBracketState = AS_END_ELEMENT_SLASH;
				else if (mAngleBracketState == AS_ATT_SEQ_OUTSIDE)
				{
					// bump stack up, and then back.
					txp.istack++;
					txp.startElementAttributesHandled(name, true);

					txp.istack--;
					txp.endElementAttributesHandled(name);

					mAngleBracketState = AS_END_ELEMENT_EMITTED;
				}
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters("/");
				else
					return "slash in brackets wrong";
				break;

			case '>':
				if (mAngleBracketState == AS_END_ELEMENT_EMITTED)
					;
				else if (mAngleBracketState == AS_ATT_SEQ_OUTSIDE)
				{
					txp.istack++;
					txp.startElementAttributesHandled(name, false);
				}
				else
					return "Angle Brackets mismatch on close";
				mAngleBracketState = AS_OUTSIDE;
				break;

			case StreamTokenizer.TT_WORD:
				if (mAngleBracketState == AS_FIRST_OPEN)
				{
					// place on stack.
					txp.iposstack[txp.istack] = (txp.istack == 0 ? 0 : txp.iposstack[txp.istack - 1]);
					txp.elemstack[txp.istack] = st.sval;

					name = st.sval;
					mAngleBracketState = AS_ATT_SEQ_OUTSIDE;
				}
				else if (mAngleBracketState == AS_ATT_SEQ_OUTSIDE)
				{
					attname = st.sval;
					mAngleBracketState = AS_ATT_SEQ_EQ;
				}
				else if (mAngleBracketState == AS_END_ELEMENT_SLASH)
				{
					name = st.sval;

					txp.istack--;
					if (txp.istack == -1)
						return "too many end elements";
					if (!name.equals(txp.elemstack[txp.istack]))
						return "mismatch of end element " + name + "!=" + txp.elemstack[txp.istack];
					txp.endElementAttributesHandled(name);

					mAngleBracketState = AS_END_ELEMENT_EMITTED;
				}
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters(st.sval);
				else
					return "Unknown word state";
				break;

			case '=':
				if (mAngleBracketState == AS_ATT_SEQ_EQ)
					mAngleBracketState = AS_ATT_SEQ_SET;
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters("=");
				else
					return "Misplaced = in attribute";
				break;

			case '"':
				if (mAngleBracketState == AS_ATT_SEQ_SET)
				{
					// place on stack.
					txp.attnamestack[txp.iposstack[txp.istack]] = attname;
					txp.attvalstack[txp.iposstack[txp.istack]] = TNXML.xunmanglxmltext(st.sval);
					txp.iposstack[txp.istack]++;

					mAngleBracketState = AS_ATT_SEQ_OUTSIDE;
				}
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else
					return "Bad value (missing quotes) in attribute";
				break;

			default:
				if (mAngleBracketState == AS_OUTSIDE)
				{
					System.out.println("making default case chars " + (char)st.ttype);
					txp.characters(String.valueOf((char)st.ttype));
				}
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else
					return "Unknown word state " + st.ttype;
				break;
			}
		}
		return null;
	}
};


