////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.io.IOException; 
import java.util.Vector; 
import java.util.Arrays; 
import java.io.File; 

//
//
// SurvexLoader
//
//



/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexLoader extends SurvexCommon
{

	class Vequates extends Vector 
	{
		boolean bImplicitType; 
		Vequates(boolean lbImplicitType) 
			{ bImplicitType = lbImplicitType; }; 
	}

	// this is a Vector of Vectors of station names
	private Vector equatearray = new Vector(); 
	boolean bReadCommentedXSections; 
	boolean bAppendTubes; 
	boolean bNoEquates; 

	/////////////////////////////////////////////
	// loading work
	/////////////////////////////////////////////
	private Vector FindEquateArray(String sname)
	{
		for (int i = 0; i < equatearray.size(); i++)
		{
			Vector ear = (Vector)(equatearray.elementAt(i)); 
			for (int j = 0; j < ear.size(); j++)
			{
				if (sname.equalsIgnoreCase((String)(ear.elementAt(j))))
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

		Vector vs1 = FindEquateArray(sname1); 
		Vector vs2 = FindEquateArray(sname2); 
		if ((vs1 == null) && (vs2 == null))
		{
			Vector vsnew = new Vequates(bImplicitType); 
			vsnew.addElement(sname1); 
			vsnew.addElement(sname2); 
			equatearray.addElement(vsnew); 
		}

		else if ((vs1 == null) && (vs2 != null))
			vs2.addElement(sname1); 

		else if ((vs1 != null) && (vs2 == null))
			vs1.addElement(sname2); 

		// combine the two lists
		else if (vs1 != vs2)
		{
			for (int i = 0; i < vs2.size(); i++)
				vs1.addElement(vs2.elementAt(i)); 
			vs2.removeAllElements(); 
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
			// System.out.println("Warning:: Implicit equate: " + sname); 
			String e2 = ConvertGlobal(sname, local, slash); 
			LoadEquate(local + TN.StationDelimeter + sname, e2, true); 
		}
	}


	/////////////////////////////////////////////
	private OneSection ReadPossibleXSection(OneTunnel tunnel, String sline, boolean bMakePossXSections, PossibleXSection pxs) 
	{
		if (pxs.basestationS == null) 
			return null; 

		tunnel.vposssections.addElement(pxs); 
		if (!bMakePossXSections) 
			return null; 

		int i = tunnel.stationnames.size(); 
		while (--i >= 0)
		{
			if (pxs.basestationS.equalsIgnoreCase((String)(tunnel.stationnames.elementAt(i)))) 
				break; 
		}

		if (i == -1)  
			return null; 

		// make sure we don't put an xsection on the same station 
		for (int j = 0; j < tunnel.vsections.size(); j++) 
			if (pxs.basestationS.equalsIgnoreCase(((OneSection)(tunnel.vsections.elementAt(j))).station0S)) 
				return null; 

		OneSection os = new OneSection(pxs); 
		//System.out.println("XSection Comment: " + sline); 
		tunnel.vsections.addElement(os); 
		return os; 
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

		while (lis.FetchNextLine())
		{
			// work on the special types 
			if (lis.w[0].startsWith("*"))
			{
				if (lis.w[0].equalsIgnoreCase("*begin"))  
				{
					if (lis.prefixconversion != null) 
					{
						System.out.println("Prefix conversion broken by nested *begin; *prefix " + lis.prefixconversion); 
						lis.prefixconversion = null; 
					}

					// deal with begins which have slashes.  
					if (lis.w[1].startsWith("\\"))
					{
						// remove slash when it does nothing.  
						if (lis.slash.equals(tunnel.fullname)) 
							lis.w[1] = lis.w[1].substring(1); 
						else 
							System.out.println("*** Unable to abolish slash in " + lis.GetLine()); 
					}

					// deal with blank begins
					if (lis.w[1].equals(""))
					{
						// don't know if this will be consistent with .tube association.  Might insist on user putting one in.  
						lis.w[1] = "d-blank-begin" + String.valueOf(lis.nlineno); 
						System.out.println("default name for blank *begin " + lis.w[1] + "  in tunnel: " + tunnel.fullname);  
					}

					if (lis.w[1].indexOf('.') != -1) 
						System.out.println("*** Illegal Dot found in *begin " + lis.w[1]); 

					int lndowntunnels = tunnel.ndowntunnels; 
					OneTunnel subtunnel = tunnel.IntroduceSubTunnel(lis.w[1], CurrentLegLineFormat, bAppendTubes); 
					if (!bAppendTubes) 
					{
						if (lndowntunnels == tunnel.ndowntunnels)  
							System.out.println("WARNING: repeated *begin " + lis.w[1]); 
						//else 
						//	tunnel.AppendLine("*subtunnel " + lis.w[1] + "  " + lis.comment); 
					}

					subtunnel.bTunnelTreeExpanded = !(lis.w[2].equals("+")); 

					subtunnel.AppendLine("*begin " + lis.w[1]); 
					CurrentLegLineFormat.AppendStarNonDefaults(subtunnel); 
					LoadSurvexRecurse(subtunnel, lis); 
				}

				else if (lis.w[0].equalsIgnoreCase("*end"))
				{
					if (lis.prefixconversion != null) 
					{
						System.out.println("Prefix conversion hits a *end; *prefix " + lis.prefixconversion); 
						lis.prefixconversion = null; 
					}
						
					// put out the *includes 
					for (int i = 0; i < tunnel.ndowntunnels; i++) 
						tunnel.AppendLine("*include " + tunnel.downtunnels[i].name + "." + tunnel.downtunnels[i].name); 
					tunnel.AppendLine(""); 

					// put out the *end
					tunnel.AppendLine(lis.GetLine()); 

					bEndOfSection = true; 
					break; 
				}

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
									if (!bAppendTubes) 
										tunnel.AppendLine("*subtunnel " + lis.prefixconversion + "  " + lis.comment); 
									OneTunnel subtunnel = tunnel.IntroduceSubTunnel(lis.prefixconversion, CurrentLegLineFormat, bAppendTubes); 
									subtunnel.bTunnelTreeExpanded = !(lis.w[2].equals("+")); 
									LoadSurvexRecurse(subtunnel, lis); 
								}
								else 
									System.out.println("*prefix conversion only one level deep: " + lis.w[1]); 
							}
							else 
							{
								// do an end and then a begin:  
								lis.UnFetch(); 
								// System.out.println("unfetch of " + lis.GetLine()); 

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
								System.out.println("Successful *prefix conversion of " + lis.prefixconversion); 
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
									System.out.println("Unnecessary *prefix " + lis.w[1]); 
								else 
									System.out.println("Incompatible *prefix conversion: " + lis.w[1]); 
							}
						}
					}
					else 
						System.out.println("*prefix totally ignored: " + lis.w[1]); 
				}

				else if (lis.w[0].equalsIgnoreCase("*include"))
				{
					// build new file name using the survex conventions
					String finclude = lis.w[1]; 

					File newloadfile = calcIncludeFile(lis.loadfile, lis.w[1], false); 
					LineInputStream llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);  
					File oldloadfile = CurrentLegLineFormat.currfile; 
					CurrentLegLineFormat.currfile = newloadfile; // this runs in parallel to the lineinputstream stuff.  
					LoadSurvexRecurse(tunnel, llis); 
					llis.close(); 
					CurrentLegLineFormat.currfile = oldloadfile; 
				}

				// formatting star commands (need to run them for the cross sections).  
				else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				{
					tunnel.AppendLine(lis.GetLine()); 
					CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2]); 
				}

				else if (lis.w[0].equalsIgnoreCase("*units"))
				{
					tunnel.AppendLine(lis.GetLine()); 
					CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2]); 
				}

				else if (lis.w[0].equalsIgnoreCase("*set"))
				{
					tunnel.AppendLine(lis.GetLine()); 
					CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2]); 
				}

				else if (lis.w[0].equalsIgnoreCase("*sd")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*backimg")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*export")) 
					tunnel.AppendLine(lis.GetLine()); // should write as *extern  

				else if (lis.w[0].equalsIgnoreCase("*title")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*date")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*team")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*instrument")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*entrance")) 
					tunnel.AppendLine(lis.GetLine()); 

				else if (lis.w[0].equalsIgnoreCase("*fix")) 
				{
					if (!bNoEquates) 
					{
						PossibleImplicitEquate(lis.w[1], tunnel.fulleqname, lis.slash); 
						tunnel.stationnames.addElement(lis.w[1]); 
					}
					tunnel.AppendLine(lis.GetLine()); 
				}

				// *data command
				else if (lis.w[0].equalsIgnoreCase("*data")) 
				{
					tunnel.AppendLine(lis.GetLine()); 
					if (lis.w[1].equalsIgnoreCase("LRUD")) 
						ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[2], lis.w[3], lis.w[4], lis.w[5], lis.w[6], lis.w[7]));  
					if (lis.w[1].equalsIgnoreCase("normal") || lis.w[1].equalsIgnoreCase("normal-")) 
					{
						if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc)) 
							System.out.println("Bad *data normal line:  " + lis.GetLine()); 
					}
					else
						System.out.println("What:  " + lis.GetLine()); 
				}


				else if (lis.w[0].equalsIgnoreCase("*equate"))
				{
					if (bNoEquates) 
						System.out.println("Error:: *equates in noequate mode"); 

					String e1 = ConvertGlobal(lis.w[1], tunnel.fulleqname, lis.slash); 
					for (int i = 2; i < lis.iwc; i++) 
					{
						String ei = ConvertGlobal(lis.w[i], tunnel.fulleqname, lis.slash); 
						LoadEquate(e1, ei, false); 
					}

					tunnel.AppendLine(lis.GetLine()); 
				}

				else if (lis.w[0].equalsIgnoreCase("*XSection"))
				{
					if (bReadCommentedXSections) 
						System.out.println("Cannot read commented XSections when true XSections present"); 
					tunnel.vsections.addElement(new OneSection(lis)); 
				}

				else if (lis.w[0].equalsIgnoreCase("*Linear_tube"))
				{
					// the numbers are the indexes of the xsections.  
					int xind0 = Integer.valueOf(lis.w[1]).intValue(); 
					int xind1 = Integer.valueOf(lis.w[2]).intValue(); 
					if ((xind0 >= 0) && (xind0 < tunnel.vsections.size()) && (xind1 >= 0) && (xind1 < tunnel.vsections.size()) && (xind0 != xind1))  
						tunnel.vtubes.addElement(new OneTube((OneSection)(tunnel.vsections.elementAt(xind0)), (OneSection)(tunnel.vsections.elementAt(xind1)))); 
					else
						System.out.println("Bad xsection index in linear tube"); 
				}

				// sketch type stuff.  
				else if (lis.w[0].equalsIgnoreCase("*Sketch"))
				{
					tunnel.tsketch = new OneSketch(lis); 
				}

				else
					System.out.println("Error:  unrecognized STAR command : " + lis.GetLine()); 
			}

			// ordinary line
			else if (!bAppendTubes)
			{
				// survey leg line.  
				if (lis.w[2].length() != 0)
				{
					if (!bNoEquates) 
					{
						// check if the station names here need to be given equates
						String sfrom = lis.w[CurrentLegLineFormat.fromindex]; 
						String sto = lis.w[CurrentLegLineFormat.toindex]; 
						PossibleImplicitEquate(sfrom, tunnel.fulleqname, lis.slash); 
						PossibleImplicitEquate(sto, tunnel.fulleqname, lis.slash); 

						tunnel.vlegs.addElement(new OneLeg(sfrom, sto, 0.0F, 0.0F, 0.0F, null)); 

						// put in the station names found.  
						tunnel.stationnames.addElement(sfrom); 
						tunnel.stationnames.addElement(sto); 

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

		// now update automatic cross sections 
		if (bEndOfSection) 
		{
			for (int i = 0; i < tunnel.vsections.size(); i++) 
			{
				OneSection os = (OneSection)(tunnel.vsections.elementAt(i)); 
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
						tunnel.vlegs.addElement(new OneLeg(prevStation, lis.w[1], 0.0F, 0.0F, 0.0F, null)); 
					}
					prevStation = lis.w[1]; 
					tunnel.stationnames.addElement(lis.w[1]); 

					// do the cross section 
					OneSection curros = ReadPossibleXSection(tunnel, lis.GetLine(), bReadCommentedXSections, new PossibleXSection(CurrentLegLineFormat, lis.w[1], lis.w[8], lis.w[9], lis.w[10], lis.w[11], null));  

					// do the automatic tube.  
					if ((prevos != null) && (curros != null)) 
						tunnel.vtubes.addElement(new OneTube(prevos, curros)); 
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
				tunnel.AppendLine("*subtunnel " + lis.w[0]); 
				OneTunnel subtunnel = tunnel.IntroduceSubTunnel(lis.w[0], CurrentLegLineFormat, false); 
				subtunnel.bTunnelTreeExpanded = true; 
				LoadToporobotRecurse(subtunnel, lis); 
			}
		} // endwhile 

		// now update automatic cross sections 
		for (int i = 0; i < tunnel.vsections.size(); i++) 
		{
			OneSection os = (OneSection)(tunnel.vsections.elementAt(i)); 
			if (os.relorientcompassS.equals("++++"))  
				os.SetDefaultOrientation(tunnel.vlegs); 
		}  
	}

	/////////////////////////////////////////////
	private void LoadWallsRecurse(OneTunnel tunnel, LineInputStream lis, Vector revunq) throws IOException
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
							System.out.println("Error, book and survey before name"); 

						// do the *begin 
						if (!bAppendTubes) 
							tunnel.AppendLine("*subtunnel " + lis.w[1]); 

						OneTunnel subtunnel = tunnel.IntroduceSubTunnel(lis.w[1], CurrentLegLineFormat, bAppendTubes); 
						subtunnel.bTunnelTreeExpanded = true; 
						subtunnel.AppendLine("; " + sbook); 
						LoadWallsRecurse(subtunnel, lis, revunq); 
						sbook = null; 
					}
					else if (ssurvey != null) 
					{
						if (!TN.getSuffix(lis.w[1]).equalsIgnoreCase(".SRV")) 
							System.out.println("Error, wrong suffix in " + lis.w[1]); 
						String ssubt = TN.setSuffix(lis.w[1], ""); 

						String finclude = lis.w[1]; 

						File newloadfile = calcIncludeFile(lis.loadfile, lis.w[1], false); 
						LineInputStream llis = new LineInputStream(newloadfile, tunnel.fullname, lis.prefixconversion);  
						File oldloadfile = CurrentLegLineFormat.currfile; 
						CurrentLegLineFormat.currfile = newloadfile; // this runs in parallel to the lineinputstream stuff.  

						// do the *include and *begin 
tunnel.AppendLine("*subtunnel " + ssubt); 
						OneTunnel subtunnel = tunnel.IntroduceSubTunnel(ssubt, CurrentLegLineFormat, false); 
						subtunnel.bTunnelTreeExpanded = true; 
						subtunnel.AppendLine("; " + ssurvey); 
						LoadWallsRecurse(subtunnel, llis, revunq); 
						llis.close(); 
						CurrentLegLineFormat.currfile = oldloadfile; 

						ssurvey = null; 
					}
					else 
						System.out.println("Error, name without book or survey"); 
				}
				else 
					System.out.println("Error, unrecognized dot command " + lis.w[0]); 
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

					tunnel.vlegs.addElement(new OneLeg(sfrom, sto, 0.0F, 0.0F, 0.0F, null)); 

					// put in the station names found.  
					tunnel.stationnames.addElement(sfrom); 
					tunnel.stationnames.addElement(sto); 

					revunq.addElement(sfrom + " " + tunnel.fullname); 
					revunq.addElement(sto + " " + tunnel.fullname); 
				}
			}
		} // endwhile 
	}



	/////////////////////////////////////////////
	void FindWallsEquates(Vector revunq)  
	{
		String[] revunqs = new String[revunq.size()]; 
		for (int i = 0; i < revunq.size(); i++) 
			revunqs[i] = (String)revunq.elementAt(i); 
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
		System.out.println("Made " + neq + " equates from " + revunqs.length + " stations."); 
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
			String eqfullname = (String)(equatev.elementAt(i)); 
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
					System.out.println("Irregular implicit equate at " + eqfullname); 
				}
				else 
					System.out.println("Failed to make equate to " + eqfullname); 
			}
		}

		eqvec.ExtendRootIfNecessary(); 

		// now go through a possibly growing array and make the equate lines 
		boolean bMEL = true; 
		for (int i = 0; i < eqvec.size(); i++)
			bMEL &= eqvec.MakeEquateLine((Eq)(eqvec.elementAt(i))); 

		if (!bMEL) 
			eqvec.DumpOut(); 
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
	public SurvexLoader(File loadfile, OneTunnel filetunnel, boolean lbReadCommentedXSections, boolean lbAppendTubes, boolean lbNoEquates)  
	{
		bAppendTubes = lbAppendTubes; 
		bReadCommentedXSections = lbReadCommentedXSections; 
		bNoEquates = lbNoEquates; 
		try
		{
System.out.println("SurvexLoader " + loadfile.getName()); 
			// load this file into an introduced sub tunnel.  
			LineInputStream lis = new LineInputStream(loadfile, filetunnel.fullname, null); 

			// select whether it's svx or toporobot or walls
			if (loadfile.getName().regionMatches(true, loadfile.getName().length() - TN.SUFF_TOP.length(), TN.SUFF_TOP, 0, TN.SUFF_TOP.length())) 
			{
				System.out.println("Loading Toporobot file"); 
				LoadToporobotRecurse(filetunnel, lis); 
			}
			else if (loadfile.getName().regionMatches(true, loadfile.getName().length() - TN.SUFF_WALLS.length(), TN.SUFF_WALLS, 0, TN.SUFF_WALLS.length())) 
			{
				System.out.println("Loading Walls file"); 
				filetunnel.AppendLine("*data   normal  from to gradient length bearing"); 

				Vector revunq = new Vector(); 
				LoadWallsRecurse(filetunnel, lis, revunq); 
				FindWallsEquates(revunq); 
			}
			else
				LoadSurvexRecurse(filetunnel, lis); 

			lis.close(); 
System.out.println("closing " + loadfile.getName()); 

			if (!bNoEquates) 
			{
				for (int i = 0; i < equatearray.size(); i++)
					ApplyEquate((Vequates)(equatearray.elementAt(i)), filetunnel); 
			} 
		}
		catch (IOException ie) 
		{
			System.out.println(ie.toString()); 		
		}; 
	}
}

