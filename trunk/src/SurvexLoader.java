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
import java.io.FileNotFoundException; 
import java.util.Vector; 
import java.util.Arrays; 

import java.util.List; 
import java.util.ArrayList; 
import java.util.Deque;
import java.util.ArrayDeque;

//
//
// SurvexLoader
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexLoader extends SurvexCommon
{
	class Vequates extends ArrayList<String>
	{
		boolean bImplicitType; 
		Vequates(boolean lbImplicitType) 
			{ bImplicitType = lbImplicitType; };
	}

	// this is a Vector of Vectors of station names
	private List<Vequates> equatearray = new ArrayList<Vequates>(); 
	boolean bReadCommentedXSections;
	boolean bPosFileLoaded = false; 
	boolean bPosFixesFound = false; 

	List<String> vposstations = new ArrayList<String>(); 
	List<String> vposfixes = new ArrayList<String>(); // this should probably be killed; using vposlegs instead


	/////////////////////////////////////////////
	// loading work
	/////////////////////////////////////////////
	private List<String> FindEquateArray(String sname)
	{
		for (List<String> ear : equatearray)
		{
			for (String lsname : ear)
			{
				if (sname.equalsIgnoreCase(lsname))
					return ear; 
			}
		}
		return null; 
	}


	/////////////////////////////////////////////
	private void LoadEquate(String sname1, String sname2, boolean bImplicitType)
	{
		if (sname1.equalsIgnoreCase(sname2))
			return;

		List<String> vs1 = FindEquateArray(sname1); 
		List<String> vs2 = FindEquateArray(sname2); 
		if ((vs1 == null) && (vs2 == null))
		{
			Vequates vsnew = new Vequates(bImplicitType); 
			vsnew.add(sname1); 
			vsnew.add(sname2); 
			equatearray.add(vsnew); 
		}

		else if ((vs1 == null) && (vs2 != null))
			vs2.add(sname1); 

		else if ((vs1 != null) && (vs2 == null))
			vs1.add(sname2);

		// combine the two lists
		else if (vs1 != vs2)
		{
			vs1.addAll(vs2);
			vs2.clear();
		}
	}

	/////////////////////////////////////////////
	private StringBuffer sb = new StringBuffer();
	private synchronized String ConvertGlobal(String sname, String local, String slash)
	{
		sb.setLength(0);
		if (sname.startsWith("\\"))
		{
			sb.append(slash);
			sname = sname.substring(1);
		}
		else
			sb.append(local);

		// chip off the dots
		boolean bfap = true;
		int idot;
		while (sname.length() != 0)
		{
			idot = sname.indexOf('.');
			if (idot == -1)
			{
				sb.append(TN.StationDelimeter);
				idot = sname.length();
				sb.append(sname.substring(0, idot));
			}
			else
			{
				sb.append(TN.PathDelimeter);
				sb.append(sname.substring(0, idot).toLowerCase());
			}

			if (idot < sname.length())
				sname = sname.substring(idot + 1);
			else
				break;
		}

		return sb.toString();
	}

	/////////////////////////////////////////////
	private void PossibleImplicitEquate(String sname, String local, String slash)
	{
		if ((sname.indexOf('\\') != -1) || (sname.indexOf('.') != -1))
		{
			TN.emitMessage("Warning:: Implicit equate: " + sname); 
			String e2 = ConvertGlobal(sname, local, slash); 
			LoadEquate(local + TN.StationDelimeter + sname, e2, true); 
		}
	}


	/////////////////////////////////////////////
	private OneSection ReadPossibleXSection(OneTunnel tunnel, String sline, boolean bMakePossXSections, PossibleXSection pxs) 
	{
		if (pxs.basestationS == null) 
			return null; 

		tunnel.vposssections.add(pxs); 
		if (!bMakePossXSections) 
			return null; 

		int i = tunnel.stationnames.size(); 
		while (--i >= 0)
		{
			if (pxs.basestationS.equalsIgnoreCase(tunnel.stationnames.get(i))) 
				break; 
		}

		if (i == -1)  
			return null; 

		// make sure we don't put an xsection on the same station 
		for (int j = 0; j < tunnel.vsections.size(); j++) 
			if (pxs.basestationS.equalsIgnoreCase(((OneSection)(tunnel.vsections.get(j))).station0S)) 
				return null; 

		OneSection os = new OneSection(pxs); 
		// TN.emitMessage("XSection Comment: " + sline);
		tunnel.vsections.add(os); 
		return os; 
	}


	/////////////////////////////////////////////
	// this is very strict with the format  
	// we turn pos's into exports so that they can be associated to all examples of that station at the bottom.  
	private void LoadPosFile(OneTunnel tunnel, LineInputStream lis) throws IOException  
	{
		if (bPosFixesFound)
			TN.emitWarning("The *include_pos must be above all *pos_fix commands if it is to over-write them.");  
		if (bPosFileLoaded)  
			TN.emitWarning("One *include_pos file at a time, please.");  
		bPosFileLoaded = true; 

		while (lis.FetchNextLine())  
		{
			if (lis.GetLine().equals("( Easting, Northing, Altitude )"))
				break;
			TN.emitWarning("Unknown pos file line at start: " + lis.GetLine());
		}


		// (  -19.97,    -0.88,   -64.00 ) 204.110_bidet.1
		while (lis.FetchNextLine())
		{
			// commas are stripped.
			if (lis.w[0].equals("(") && lis.w[4].equals(")") && (lis.w[5].length() != 0))
			{
				String e1 = ConvertGlobal(lis.w[5], tunnel.fulleqname, lis.slash);

				vposstations.add(e1);
				String sfix = lis.w[1] + " " + lis.w[2] + " " + lis.w[3];
System.out.println("fixfixfix " + sfix);
				vposfixes.add(sfix);
				TN.emitMessage("posstation " + e1);
			}
			else
				TN.emitWarning("Unknown pos file line: " + lis.GetLine());
		}
	}


	/////////////////////////////////////////////
	private void LoadSurvexRecurse(OneTunnel tunnel, LineInputStream lis) throws IOException
	{
		// this local slimmed down list of legs we make up as we go along, not
		// caring about leg line format, is for the purpose of making those automatic
		// xsections.
		boolean bEndOfSection = false;
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(tunnel.InitialLegLineFormat);
		Vector vnodes = new Vector();	// local array used merely to line up the paths.
		Deque<LegLineFormat> leglineformatstack = null; // this is used to cover the blank *begins and replace with calibrations.

		int ndatesets = 0;

		while (lis.FetchNextLine())
		{
			// work on the special types
			if (lis.w[0].startsWith("*"))
			{
				if (lis.w[0].equalsIgnoreCase("*begin"))
				{
					if (lis.prefixconversion != null)
					{
						TN.emitWarning("Prefix conversion broken by nested *begin; *prefix " + lis.prefixconversion);
						lis.prefixconversion = null;
					}

					// deal with begins which have slashes.
					if (lis.w[1].startsWith("\\"))
					{
						// remove slash when it does nothing.
						if (lis.slash.equals(tunnel.fullname))
							lis.w[1] = lis.w[1].substring(1);
						else
							TN.emitWarning("*** Unable to abolish slash in " + lis.GetLine());
					}

					// proper begins
					if (!lis.w[1].equals(""))
					{
						if (lis.w[1].indexOf('.') != -1)
							TN.emitWarning("*** Illegal Dot found in *begin " + lis.w[1]);

						OneTunnel subtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(lis.w[1], CurrentLegLineFormat));

						subtunnel.bTunnelTreeExpanded = !(lis.w[2].equals("+"));

						CurrentLegLineFormat.AppendStarDifferences(subtunnel, TN.defaultleglineformat, true);
						LoadSurvexRecurse(subtunnel, lis);
					}

					// deal with blank begins (just a leglineformat stack).
					else
					{
						if (leglineformatstack == null)
							leglineformatstack = new ArrayDeque<LegLineFormat>();
						leglineformatstack.addFirst(new LegLineFormat(CurrentLegLineFormat));
					}
				}

				else if (lis.w[0].equalsIgnoreCase("*end"))
				{
					if ((leglineformatstack == null) || leglineformatstack.isEmpty())
					{
						if (lis.prefixconversion != null)
						{
							TN.emitWarning("Prefix conversion hits a *end; *prefix " + lis.prefixconversion);
							lis.prefixconversion = null;
						}

						// put out the *end (or should put out the comments).

						bEndOfSection = true;
						break;
					}

					// blank begin/end case
					else
					{
						LegLineFormat rollbackllf = CurrentLegLineFormat;
						CurrentLegLineFormat = leglineformatstack.removeFirst();
						CurrentLegLineFormat.AppendStarDifferences(tunnel, rollbackllf, false);
					}
				}

				else if (lis.w[0].equalsIgnoreCase("*tunneldir"))
					tunnel.tundirectory = calcIncludeFile(lis.loadfile, lis.w[1], false);

				// prefix conversion
				else if (lis.w[0].equalsIgnoreCase("*prefix"))
				{
					TN.emitProgError("*prefix not re-implemented");
					if (lis.w[1].startsWith("\\"))
					{
						// *begin type.
						if (!lis.w[1].equals("\\"))
						{
							if (lis.prefixconversion == null)
							{
								if (lis.slash.equals(tunnel.fullname))
								{
									// straight out of the *begin code.
									lis.w[1] = lis.w[1].substring(1);
									lis.prefixconversion = lis.w[1];
									tunnel.AppendLine(";subsurvey " + lis.prefixconversion + "  " + lis.comment);
									OneTunnel subtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(lis.prefixconversion, CurrentLegLineFormat));
									subtunnel.bTunnelTreeExpanded = !(lis.w[2].equals("+"));
									LoadSurvexRecurse(subtunnel, lis);
								}
								else
									TN.emitWarning("*prefix conversion only one level deep: " + lis.w[1]);
							}
							else
							{
								// do an end and then a begin:
								lis.UnFetch();

								// code from *end function
								lis.prefixconversion = null;
								bEndOfSection = true;
								break;
							}
						}

						// *end type
						else
						{
							if (lis.prefixconversion != null)
							{
								TN.emitMessage("Successful *prefix conversion of " + lis.prefixconversion);
								// code from the *end function
								lis.prefixconversion = null;
								if (lis.comment.length() != 0)
									tunnel.AppendLine(" " + lis.comment);
								bEndOfSection = true;
								break;
							}
							else
							{
								if (lis.slash.equals(tunnel.fullname))
									TN.emitMessage("Unnecessary *prefix " + lis.w[1]);
								else
									TN.emitWarning("Incompatible *prefix conversion: " + lis.w[1]);
							}
						}
					}
					else
						TN.emitMessage("*prefix totally ignored: " + lis.w[1]);
				}

				else if (lis.w[0].equalsIgnoreCase("*include"))
				{
					// build new file name using the survex conventions
					String finclude = lis.w[1];

					// catch if the include file isn't there as some data is bad.
					FileAbstraction newloadfile = calcIncludeFile(lis.loadfile, lis.w[1], false);
					LineInputStream llis = null;
TN.emitMessage("including " + newloadfile.getPath());
					try
					{
						llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);
					}
					catch (FileNotFoundException e1)
					{
						try //The file may go up a directory and use a windows \ which is then not understood by unix
						{
							newloadfile = calcIncludeFile(lis.loadfile, lis.w[1].replace('\\', '/'), false);
							llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);
						}
						catch (FileNotFoundException e2)
						{
							try //The file may be refered to with some uppercase letters while the file may all be lower case
							{
								newloadfile = calcIncludeFile(lis.loadfile, lis.w[1].replace('\\', '/').toLowerCase(), false);
								llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);
							}
							catch (FileNotFoundException e3)
							{
								TN.emitError(e3.toString());
							}
						}
					}

					if (llis != null)
					{
						FileAbstraction oldloadfile = CurrentLegLineFormat.currfile;
						CurrentLegLineFormat.currfile = newloadfile; // this runs in parallel to the lineinputstream stuff.
						LoadSurvexRecurse(tunnel, llis);
						llis.close();
						CurrentLegLineFormat.currfile = oldloadfile;
					}
				}

				else if (lis.w[0].equalsIgnoreCase("*include_pos"))
				{
					// build new file name using the survex conventions
					String finclude = lis.w[1];

					FileAbstraction newloadfile = calcIncludeFile(lis.loadfile, lis.w[1], true);
					LineInputStream llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);
					LoadPosFile(tunnel, llis);
					llis.close();
				}

				else if (lis.w[0].equalsIgnoreCase("*pos_fix"))
				{
					if (!bPosFileLoaded)
					{
						String e1 = ConvertGlobal(lis.w[1], tunnel.fulleqname, lis.slash);
						vposstations.add(e1);
						vposfixes.add(lis.w[2] + " " + lis.w[3] + " " + lis.w[4]);
						tunnel.AppendLine(lis.GetLine());
						bPosFixesFound = true; // error detection.
					}
				}


				// formatting star commands (need to run them for the cross sections).
				else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				{
					tunnel.AppendLine(lis.GetLine());
					CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
				}

				else if (lis.w[0].equalsIgnoreCase("*units"))
				{
					tunnel.AppendLine(lis.GetLine());
					CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2], lis.w[3], lis);
				}

				else if (lis.w[0].equalsIgnoreCase("*set"))
				{
					tunnel.AppendLine(lis.GetLine());
					CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2], lis);
				}

				else if (lis.w[0].equalsIgnoreCase("*sd"))
					tunnel.AppendLine(lis.GetLine());

				// this is the shite survex *exports which are NOT what I asked for;
				// What they have implemented is a actually an "extern" command.
				// Not only that, there's a pointless rule that it must come
				// right after the *begin.
				else if (lis.w[0].equalsIgnoreCase("*export"))
					tunnel.PrependLine(lis.GetLine()); // should write as *extern

				// settings in CurrentLegLineFormat are to cope with crappy blank begins which are supposed to carry the values down to the lower levels.
				else if (lis.w[0].equalsIgnoreCase("*title"))
				{
					tunnel.AppendLine(lis.GetLine());
					CurrentLegLineFormat.bb_svxtitle = lis.w[1];
				}

				else if (lis.w[0].equalsIgnoreCase("*date"))
				{
					if (!lis.w[1].equals(""))
					{
						CurrentLegLineFormat.bb_svxdate = lis.w[1];
						ndatesets++;
					}
					else
					{
						lis.emitWarning("empty date setting");
						CurrentLegLineFormat.bb_svxdate = ""; 
					}
				}

				else if (lis.w[0].equalsIgnoreCase("*flags"))
				{
					tunnel.AppendLine(lis.GetLine());

				}

				else if (lis.w[0].equalsIgnoreCase("*team"))
				{
					tunnel.AppendLine(lis.GetLine());

					if (lis.w[1].equalsIgnoreCase("notes"))
						CurrentLegLineFormat.bb_teamnotes = lis.remainder2.trim();
					else if (lis.w[1].equalsIgnoreCase("tape"))
						CurrentLegLineFormat.bb_teamtape = lis.remainder2.trim();
					else if (lis.w[1].equalsIgnoreCase("insts"))
						CurrentLegLineFormat.bb_teaminsts = lis.remainder2.trim();
					else if (lis.w[1].equalsIgnoreCase("pics"))
						CurrentLegLineFormat.bb_teampics = lis.remainder2.trim();
				}

				else if (lis.w[0].equalsIgnoreCase("*instrument"))
					tunnel.AppendLine(lis.GetLine());

				else if (lis.w[0].equalsIgnoreCase("*entrance"))
					tunnel.AppendLine(lis.GetLine());

				else if (lis.w[0].equalsIgnoreCase("*fix"))
				{
					PossibleImplicitEquate(lis.w[1], tunnel.fulleqname, lis.slash);
					tunnel.stationnames.add(lis.w[1]);
					tunnel.AppendLine(lis.GetLine());
				}

				// *data command
				else if (lis.w[0].equalsIgnoreCase("*data"))
				{
					tunnel.AppendLine(lis.GetLine());
					if (lis.w[1].equalsIgnoreCase("LRUD"))
						ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[2], lis.w[3], lis.w[4], lis.w[5], lis.w[6], lis.w[7]));

					if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc))
						TN.emitWarning("Bad *data line:  " + lis.GetLine());
				}


				else if (lis.w[0].equalsIgnoreCase("*equate"))
				{
					String e1 = ConvertGlobal(lis.w[1], tunnel.fulleqname, lis.slash);
					for (int i = 2; i < lis.iwc; i++)
					{
						String ei = ConvertGlobal(lis.w[i], tunnel.fulleqname, lis.slash);
						LoadEquate(e1, ei, false);
					}

					tunnel.AppendLine(lis.GetLine());
				}

				else
					TN.emitWarning("Error:  unrecognized STAR command : " + lis.GetLine());
			}

			// ordinary line
			// must take into account those 2 line leg formats.
			else
			{
				// survey leg line.
				if (lis.w[2].length() != 0)
				{
					// check if the station names here need to be given equates
					if (CurrentLegLineFormat.fromindex != -1)
					{
						String sfrom = lis.w[CurrentLegLineFormat.fromindex];
						String sto = lis.w[CurrentLegLineFormat.toindex];
						PossibleImplicitEquate(sfrom, tunnel.fulleqname, lis.slash);
						PossibleImplicitEquate(sto, tunnel.fulleqname, lis.slash);

						tunnel.vlegs.add(new OneLeg(sfrom, sto, 0.0F, 0.0F, 0.0F, null, null));

						// put in the station names found.
						tunnel.stationnames.add(sfrom);
						tunnel.stationnames.add(sto);

						// catch lrud values on the end of a survey line
						if (!lis.w[8].equals(""))
							ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[0], lis.w[5], lis.w[6], lis.w[7], lis.w[8], null));
					}
				}

				tunnel.AppendLine(lis.GetLine());

				// case of implicit xsection included here
				if (lis.GetLine().startsWith(";"))
				{
					lis.SplitWords(lis.GetLine().substring(1), false);
					if (lis.iwc == 5)
						ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[0], lis.w[1], lis.w[2], lis.w[3], lis.w[4], null));
				}
			}
		} // endwhile

		// set the date
		if (ndatesets > 1)
			lis.emitWarningF("Date set " + ndatesets + " times");

		// now update automatic cross sections
		if (bEndOfSection)
		{
			for (int i = 0; i < tunnel.vsections.size(); i++)
			{
				OneSection os = (OneSection)(tunnel.vsections.get(i));
				if (os.relorientcompassS.equals("++++"))
					os.SetDefaultOrientation(tunnel.vlegs);
			}
		}
	}


	/////////////////////////////////////////////
	private void LoadToporobotRecurse(OneTunnel tunnel, LineInputStream lis) throws IOException
	{
		// this was designed from one single file example: pamiang
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(tunnel.InitialLegLineFormat);
		String prevStation = null;
		OneSection prevos = null;

		while (lis.FetchNextLine())
		{
			// continuation comment: (    5    19   1   1   1 Black Chamber
			if (lis.w[0].equals("("))
			{
				// possibly split out the other guff.
				tunnel.AppendLine(";" + lis.GetLine().substring(1));
			}

			// starting with negative number.  The header information.
			else if (lis.w[0].startsWith("-"))
			{
				// possibly split out the other guff.
				tunnel.AppendLine(";" + lis.GetLine());
			}

			// continuation of same passage
			else if (lis.w[0].equals(tunnel.name))
			{
				// the header type info.
				if (lis.w[1].startsWith("-"))
				{
					// an equate
					if (lis.w[1].equals("-1"))
					{
						// front connection
						String e1 = ConvertGlobal(lis.w[5] + "." + lis.w[6], tunnel.uptunnel.fullname, lis.slash);
						String e2 = ConvertGlobal(tunnel.name + "." + "0", tunnel.uptunnel.fullname, lis.slash);
						LoadEquate(e1, e2, false);

						// back connection
						if (!lis.w[7].equals(tunnel.name))
						{
							e1 = ConvertGlobal(lis.w[7] + "." + lis.w[8], tunnel.uptunnel.fullname, lis.slash);
							e2 = ConvertGlobal(tunnel.name + "." + lis.w[9], tunnel.uptunnel.fullname, lis.slash); 
							LoadEquate(e1, e2, false); 
						}
					}
					else 
						tunnel.AppendLine(";" + lis.GetLine()); 
				}

				// a leg motion 
				else 
				{
					// make the leg 
					if (prevStation != null) 
					{
						tunnel.AppendLine(prevStation + "\t" + lis.w[1] + "\t" + lis.w[5] + "\t" + lis.w[6] + "\t" + lis.w[7]); 
						tunnel.vlegs.add(new OneLeg(prevStation, lis.w[1], 0.0F, 0.0F, 0.0F, null, null));
					}
					prevStation = lis.w[1]; 
					tunnel.stationnames.add(lis.w[1]); 

					// do the cross section 
					OneSection curros = ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[1], lis.w[8], lis.w[9], lis.w[10], lis.w[11], null));  

					// do the automatic tube.  
					if ((prevos != null) && (curros != null)) 
						tunnel.vtubes.add(new OneTube(prevos, curros)); 
					prevos = curros; 
				}
			}

			// new tunnel.  have an end and begin.  
			else if (!lis.w[0].equals("")) 
			{
				// this does a *end
				if (tunnel.depth == 2) 
				{
					lis.UnFetch(); 
					break; 
				}

				// do the *begin 
				OneTunnel subtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(lis.w[0], CurrentLegLineFormat)); 
				subtunnel.bTunnelTreeExpanded = true; 
				LoadToporobotRecurse(subtunnel, lis); 
			}
		} // endwhile 

		// now update automatic cross sections 
		for (int i = 0; i < tunnel.vsections.size(); i++) 
		{
			OneSection os = (OneSection)(tunnel.vsections.get(i)); 
			if (os.relorientcompassS.equals("++++"))  
				os.SetDefaultOrientation(tunnel.vlegs); 
		}  
	}

	/////////////////////////////////////////////
	private void LoadWallsRecurse(OneTunnel tunnel, LineInputStream lis, List<String> revunq) throws IOException
	{
		// this was designed from one single file example: pamiang
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(tunnel.InitialLegLineFormat); 
		String sbook = null; 
		String ssurvey = null; 
		int status = -1; // no idea what this value is for.  

		while (lis.FetchNextLine())
		{
			if (lis.w[0].startsWith("."))
			{
				if (lis.w[0].equals(".BOOK"))
					sbook = lis.remainder1; 
				else if (lis.w[0].equals(".SURVEY")) 
					ssurvey = lis.remainder1; 
				else if (lis.w[0].equals(".STATUS")) 
					status = Integer.valueOf(lis.w[1]).intValue(); 
				else if (lis.w[0].equals(".ENDBOOK")) 
					break; 
				else if (lis.w[0].equals(".NAME")) 
				{
					if (sbook != null) 
					{
						if (ssurvey != null) 
							TN.emitWarning("Error, book and survey before name"); 

						OneTunnel subtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(lis.w[1], CurrentLegLineFormat)); 
						subtunnel.bTunnelTreeExpanded = true; 
						subtunnel.AppendLine("; " + sbook); 
						LoadWallsRecurse(subtunnel, lis, revunq); 
						sbook = null; 
					}
					else if (ssurvey != null) 
					{
						if (!TN.getSuffix(lis.w[1]).equalsIgnoreCase(".SRV")) 
							TN.emitWarning("Error, wrong suffix in " + lis.w[1]); 
						String ssubt = TN.setSuffix(lis.w[1], ""); 

						String finclude = lis.w[1]; 

						FileAbstraction newloadfile = calcIncludeFile(lis.loadfile, lis.w[1], false); 
						LineInputStream llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);  
						FileAbstraction oldloadfile = CurrentLegLineFormat.currfile; 
						CurrentLegLineFormat.currfile = newloadfile; // this runs in parallel to the lineinputstream stuff.  

						OneTunnel subtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(ssubt, CurrentLegLineFormat)); 
						subtunnel.bTunnelTreeExpanded = true; 
						subtunnel.AppendLine("; " + ssurvey); 
						LoadWallsRecurse(subtunnel, llis, revunq); 
						llis.close(); 
						CurrentLegLineFormat.currfile = oldloadfile; 

						ssurvey = null; 
					}
					else 
						TN.emitWarning("Error, name without book or survey"); 
				}
				else 
					TN.emitWarning("Error, unrecognized dot command " + lis.w[0]); 
			}

			// some other symbol
			else if (lis.w[0].startsWith("#")) 
			{
				tunnel.AppendLine("; " + lis.GetLine()); 
			}

			// a normal line 
			else 
			{
				// a normal line 
				tunnel.AppendLine(lis.GetLine()); 
				if (lis.w[2].length() != 0)
				{
					// check if the station names here need to be given equates
					String sfrom = lis.w[CurrentLegLineFormat.fromindex]; 
					String sto = lis.w[CurrentLegLineFormat.toindex]; 

					tunnel.vlegs.add(new OneLeg(sfrom, sto, 0.0F, 0.0F, 0.0F, null, null));

					// put in the station names found.  
					tunnel.stationnames.add(sfrom); 
					tunnel.stationnames.add(sto); 

					revunq.add(sfrom + " " + tunnel.fullname); 
					revunq.add(sto + " " + tunnel.fullname); 
				}
			}
		} // endwhile 
	}



	/////////////////////////////////////////////
	void FindWallsEquates(List<String> revunq)  
	{
		String[] revunqs = new String[revunq.size()]; 
		for (int i = 0; i < revunq.size(); i++) 
			revunqs[i] = revunq.get(i); 
		Arrays.sort(revunqs); 

		int neq = 0; 
		int i = 0; 
		while (i < revunqs.length) 
		{
			String s0 = revunqs[i]; 
			int is0 = s0.indexOf(" "); 
			int j = i + 1; 
			while (j < revunqs.length) 
			{
				String s1 = revunqs[j]; 
				int is1 = s1.indexOf(" "); 
				if (s0.substring(0, is0).equals(s1.substring(0, is1)))  
				{
					String e1 = ConvertGlobal(s0.substring(0, is0), s0.substring(is0 + 1), ""); 
					String e2 = ConvertGlobal(s1.substring(0, is1), s1.substring(is1 + 1), ""); 
					LoadEquate(e1, e2, false); 
					neq++; 
				}
				else 
					break; 
				j++; 
			}
			i = j; 
		}
		TN.emitMessage("Made " + neq + " equates from " + revunqs.length + " stations."); 
	}


	/////////////////////////////////////////////
	// equate work
	/////////////////////////////////////////////


	/////////////////////////////////////////////
	private void ApplyEquate(Vequates equatev, OneTunnel roottunnel)
	{
		// find common stem
		if (equatev.size() <= 1)
			return; 

		// firstly build the EqVec. 
		EqVec eqvec = new EqVec(); 
		for (int i = 0; i < equatev.size(); i++)
		{
			String eqfullname = equatev.get(i); 
			int ld = eqfullname.lastIndexOf(TN.StationDelimeter); 
			String eqtunnelname = eqfullname.substring(0, ld).toLowerCase(); 
			String eqstationname = eqfullname.substring(ld + 1); 

			OneTunnel leqtunnel = FindTunnel(roottunnel, eqtunnelname); 

			if (leqtunnel != null)
				eqvec.AddEquateValue(new Eq(leqtunnel, eqstationname)); 
			else 
			{
				if (equatev.bImplicitType) 
				{
					// unfound implicit equate
					if ((i == 1) && (equatev.size() == 2))  
						return; 
					TN.emitWarning("Irregular implicit equate at " + eqfullname); 
				}
				else 
					TN.emitWarning("Failed to make equate to " + eqfullname); 
			}
		}

		eqvec.ExtendRootIfNecessary(); 

		// now go through a possibly growing array and make the equate lines 
		boolean bMEL = true; 
		for (int i = 0; i < eqvec.eqlist.size(); i++)
			bMEL &= eqvec.MakeEquateLine(eqvec.eqlist.get(i));  // this modifies eqvec as we go up it.  

		if (!bMEL) 
			eqvec.DumpOut(); 
	}


	/////////////////////////////////////////////
	private void ApplyPosfix(String posstation, String posfix, OneTunnel roottunnel)  
	{
		int ld = posstation.lastIndexOf(TN.StationDelimeter); 
		String postunnelname = posstation.substring(0, ld).toLowerCase(); 
		String posstationname = posstation.substring(ld + 1); 

		OneTunnel lpostunnel = FindTunnel(roottunnel, postunnelname); 
		if (lpostunnel != null)  
			lpostunnel.AppendLineBeforeStarEnd("*pos_fix  " + posstationname + " " + posfix); 
	}


	/////////////////////////////////////////////
	private void ApplyPosfixEq(String posstation, String posfix, OneTunnel roottunnel)  
	{
		// find if this is part of an equate group  
		List<String> posgroup = FindEquateArray(posstation); 
		if (posgroup != null)  
		{
			// avoid duplicates.  
			if (posstation.equals(posgroup.get(0)))  
			{
				for (int i = 0; i < posgroup.size(); i++)  
					ApplyPosfix(posgroup.get(i), posfix, roottunnel); 
			}
		}
		else 
			ApplyPosfix(posstation, posfix, roottunnel); 
	}


	/////////////////////////////////////////////
	OneTunnel FindTunnel(OneTunnel stunnel, String lfullname)
	{
		if (stunnel == null)
			return null; 
		if (lfullname.startsWith(stunnel.fulleqname))
		{
			if (lfullname.equalsIgnoreCase(stunnel.fulleqname))
				return stunnel; 
			for (int i = 0; i < stunnel.ndowntunnels; i++)
			{
				OneTunnel tres = FindTunnel(stunnel.downtunnels[i], lfullname); 
				if (tres != null)
					return tres; 
			}
		}
		return null; 
	}



	/////////////////////////////////////////////
	public SurvexLoader(FileAbstraction loadfile, OneTunnel filetunnel, boolean lbReadCommentedXSections)
	{
		bReadCommentedXSections = lbReadCommentedXSections; 
		try
		{
			TN.emitMessage("SurvexLoader " + loadfile.getName()); 
			// load this file into an introduced sub tunnel.  
			LineInputStream lis = new LineInputStream(loadfile, filetunnel.fullname, null);

			// select whether it's svx or toporobot or walls
			if (loadfile.getName().regionMatches(true, loadfile.getName().length() - TN.SUFF_TOP.length(), TN.SUFF_TOP, 0, TN.SUFF_TOP.length())) 
			{
				TN.emitMessage("Loading Toporobot file"); 
				LoadToporobotRecurse(filetunnel, lis); 
			}
			else if (loadfile.getName().regionMatches(true, loadfile.getName().length() - TN.SUFF_WALLS.length(), TN.SUFF_WALLS, 0, TN.SUFF_WALLS.length())) 
			{
				TN.emitMessage("Loading Walls file"); 
				filetunnel.AppendLine("*data   normal  from to gradient length bearing"); 

				List<String> revunq = new ArrayList<String>(); 
				LoadWallsRecurse(filetunnel, lis, revunq); 
				FindWallsEquates(revunq); 
			}
			else
				LoadSurvexRecurse(filetunnel, lis);

			lis.close(); 
			TN.emitMessage("closing " + loadfile.getName()); 

			for (Vequates veq : equatearray)
				ApplyEquate(veq, filetunnel); 

			// add in the posfixes to the right places  
			if (bPosFileLoaded)  
			{
				for (int i = 0; i < vposstations.size(); i++)
					ApplyPosfixEq(vposstations.get(i), vposfixes.get(i), filetunnel);
			}
		}
		catch (IOException e) 
		{
			TN.emitError(e.toString()); 
		}; 
	}
}

