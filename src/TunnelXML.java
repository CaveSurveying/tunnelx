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
import java.io.InputStream; 
import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.InputStreamReader;


/////////////////////////////////////////////
// local class of the parser.
class TunnelXML
{
	TunnelXMLparsebase txp;
	StreamTokenizer st;
	String erm = null;
	FileAbstraction ssfile = null;


	/////////////////////////////////////////////
	TunnelXML()
	{
	}



	/////////////////////////////////////////////
	boolean ParseFile(TunnelXMLparsebase ltxp, FileAbstraction sfile)
	{
		ssfile = sfile;
		boolean bRes = false;
		InputStream inputstream = null; 
        try
		{
	 		inputstream = sfile.GetInputStream(); 
			Reader br = new BufferedReader(new InputStreamReader(inputstream)); 
			String erm = ParseReader(ltxp, br, true);
			if (erm != null)
				TN.emitError(erm + " on line " + st.lineno() + " of " + sfile.getName());
            else
                bRes = true; 
            br.close(); 
            inputstream.close(); 
		}
		catch (IOException e)
		{
			TN.emitWarning(e.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			TN.emitError(e.toString() + "\n on line " + st.lineno() + " of " + sfile.getName());
		}

        // important to close, or the django runserver will hang
        if (inputstream != null)
        {
            try
            {
            inputstream.close(); 
            }
		    catch (IOException e)
		    { TN.emitWarning(e.toString());	}
        }

		return bRes;
	}

	/////////////////////////////////////////////
	String ParseString(TunnelXMLparsebase ltxp, String stxt)
	{
		String erm = null;
		try
		{
			erm = ParseReader(ltxp, new StringReader(stxt), false);
		}
		catch (IOException e)
		{
			TN.emitError(e.toString());
            e.printStackTrace();
			return "IOError";
		}
		catch (Exception e)
		{
            e.printStackTrace();
			TN.emitError(e.toString() + "\n on line " + st.lineno());
			return "Other error";
		}
		if (erm != null)
			TN.emitWarning(erm + " in: " + stxt);
		return erm;
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
		String swdchs = ".-_+^:;|*()[]{}&%$!,#~@";
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
	static int AS_COMMENT = 9;

	int mAngleBracketState = AS_OUTSIDE;
	String name;
	String attname;
	/////////////////////////////////////////////
	String ParseTokens(StreamTokenizer st) throws IOException
	{
		mAngleBracketState = AS_OUTSIDE;
        String prevcommtoken = ""; 

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
				else if (mAngleBracketState == AS_COMMENT)
                    ; 
				else
					return "Angle ? Brackets mismatch";
				break;

			case '<':
				if (mAngleBracketState == AS_COMMENT)
                    break; 
				else if (mAngleBracketState != AS_OUTSIDE)
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
				else if (mAngleBracketState == AS_COMMENT)
                    ;
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
				else if (mAngleBracketState == AS_COMMENT)
                {
                    if (!prevcommtoken.endsWith("--"))
                        break; 
                }
				else
					return "Angle Brackets mismatch on close";
				mAngleBracketState = AS_OUTSIDE;
				break;

			case StreamTokenizer.TT_WORD:
				if (mAngleBracketState == AS_FIRST_OPEN)
				{
                    if (st.sval.startsWith("!--"))
                    {
                        mAngleBracketState = AS_COMMENT; 
                        prevcommtoken = st.sval.substring(3); 
                    }
                    else
                    {
                        // place on stack.
                        txp.iposstack[txp.istack] = (txp.istack == 0 ? 0 : txp.iposstack[txp.istack - 1]);
                        txp.elemstack[txp.istack] = st.sval;
    
                        name = st.sval;
                        mAngleBracketState = AS_ATT_SEQ_OUTSIDE;
                    }
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
				else if (mAngleBracketState == AS_COMMENT)
                    prevcommtoken = st.sval; 
				else
					return "Unknown word state";
				break;

			case '=':
				if (mAngleBracketState == AS_ATT_SEQ_EQ)
					mAngleBracketState = AS_ATT_SEQ_SET;
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else if (mAngleBracketState == AS_COMMENT)
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
				else if (mAngleBracketState == AS_COMMENT)
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
				else if (mAngleBracketState == AS_COMMENT)
                    ; 
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


