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
import java.io.FileReader; 
import java.io.StreamTokenizer; 


/////////////////////////////////////////////
// local class of the parser.  
class TunnelXML 
{
	TunnelXMLparse txp; 
	StreamTokenizer st; 

	/////////////////////////////////////////////
	void emitError(String mess) throws IOException
	{
		TN.emitError(mess + " on line " + st.lineno());
		throw new IOException(); 
	}

	/////////////////////////////////////////////
	TunnelXML()
	{
	}

	/////////////////////////////////////////////
	void ParseFile(TunnelXMLparse ltxp, File sfile)
	{
	  	try
		{
	  		txp = ltxp;

	 		BufferedReader br = new BufferedReader(new FileReader(sfile));
			st = new StreamTokenizer(br);

			st.resetSyntax();
			st.whitespaceChars('\u0000', '\u0020');
			st.wordChars('A', 'Z');
			st.wordChars('a', 'z');
			st.wordChars('0', '9');
			st.wordChars('.', '.');
			st.wordChars('-', '-');
			st.wordChars('_', '_');
			st.wordChars('+', '+');
			st.wordChars('\u00A0', '\u00FF');
			st.quoteChar('"');
			st.quoteChar('\'');

			ParseTokens(st);

	 		br.close();
		}

		catch (IOException e)
		{
			TN.emitWarning(e.toString());
            e.printStackTrace();
			System.out.println("in file: " + sfile.toString());
		}
		catch (NullPointerException e)
		{
			TN.emitError("Null pointer exception at readline " + st.lineno() + " of file " + sfile.toString());
			TN.emitError(e.toString());
            e.printStackTrace();
			System.exit(0);
		}
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
	char[] charr = new char[1];
	/////////////////////////////////////////////
	void ParseTokens(StreamTokenizer st) throws IOException
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
					mAngleBracketState = AS_END_ELEMENT_EMITTED;
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters("?", null, 0, 0);
				else
					emitError("Angle ? Brackets mismatch");
				break;

			case '<':
				if (mAngleBracketState != AS_OUTSIDE)
					emitError("Angle Brackets mismatch");
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
					txp.characters("/", null, 0, 0);
				else
					emitError("slash in brackets wrong");
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
					emitError("Angle Brackets mismatch on close");
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
					txp.endElementAttributesHandled(name);

					mAngleBracketState = AS_END_ELEMENT_EMITTED;
				}
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters(st.sval, null, 0, 0);
				else
					emitError("Unknown word state");
				break;

			case '=':
				if (mAngleBracketState == AS_ATT_SEQ_EQ)
					mAngleBracketState = AS_ATT_SEQ_SET;
				else if (mAngleBracketState == AS_QM_HEADER)
					;
				else if (mAngleBracketState == AS_OUTSIDE)
					txp.characters("=", null, 0, 0);
				else
					emitError("Misplaced = in attribute");
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
					emitError("Bad value (missing quotes) in attribute"); 
				break;

			default:
				if (mAngleBracketState == AS_OUTSIDE)
				{
					charr[0] = (char)st.ttype;
					txp.characters(null, charr, 0, 1);
				}
				else if (mAngleBracketState == AS_QM_HEADER)
					; 
				else 
					emitError("Unknown word state " + st.ttype); 
				break; 
			}
		}
	}
};


