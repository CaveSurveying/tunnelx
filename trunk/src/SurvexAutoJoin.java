 ////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2008  Julian Todd.
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
import java.util.Arrays;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

//
//
// SurvexAutoJoin
//
//

/////////////////////////////////////////////
/////////////////////////////////////////////
class SurvexAutoJoin
{
	List<String> dbsurvex;
	SurvexAutoJoin(List<String> ldbsurvex)
	{
		dbsurvex = ldbsurvex;
	}

	/////////////////////////////////////////////
    static String GetTitle(String text)
    {
		LineInputStream lis = new LineInputStream(text, null);
		while (lis.FetchNextLine())
		{
			String sline = lis.GetLine();
			if (sline.startsWith("*title"))
			{
				sline = sline.substring(6).trim();
				int ic = sline.indexOf(';');
				if (ic != -1)
					sline = sline.substring(0, ic).trim();
				return sline;
			}
			assert (sline.length() == 0) || (sline.charAt(0) == ';');
		}
		return null;
    }

	/////////////////////////////////////////////
	OneTunnel ConvTunnelStruct(OneTunnel filetunnel)
	{
		for (String text : dbsurvex)
		{
			String title = GetTitle(text);
			OneTunnel downtunnel = new OneTunnel(title, new LegLineFormat());
			downtunnel.setTextData(text); 
			filetunnel.IntroduceSubTunnel(downtunnel);
		}
		return filetunnel;
		/*
			assert !dbsmap.containsKey(title);
			dbsmap.put(title, text);

			String prefix = rootprefix;
			for (String nm : title.split("\\."))
			{
				String nprefix = prefix + "." + nm;
				Set<String> dns = downnames.get(prefix);
				if (dns == null)
				{
					dns = new HashSet<String>();
					downnames.put(prefix, dns);
				}
				dns.add(nprefix);
				prefix = nprefix;
			}
		}

		String trunkprefix = rootprefix;
		while (true)
		{
			if (dbsmap.containsKey(trunkprefix))
				break;
			Set<String> downname = downnames.get(trunkprefix);
			assert downname.size() != 0;
			if (downname.size() != 1)
				break;
			downnames.remove(trunkprefix);
			trunkprefix = downname.iterator().next();
		}

		int ic = trunkprefix.lastIndexOf('.');
		String trunkname = (ic != -1 ? trunkprefix.substring(ic + 1) : trunkprefix);
		trunkprefix = (ic != -1 ? trunkprefix.substring(0, ic + 1) : "");
		OneTunnel trunktunnel = new OneTunnel(trunkname, new LegLineFormat());

		List<OneTunnel> tunnelstack = new ArrayList<OneTunnel>();
		tunnelstack.add(trunktunnel);

		while (tunnelstack.size() != 0)
		{
			OneTunnel tunn = tunnelstack.remove(tunnelstack.size() - 1);
			for (String sdownname : downnames.get(tunn.
		tnamesstack
		Map<String, Set<String> > downnames = new HashMap<String, Set<String> >();


		OneTunnel lastindexof()
System.out.println("TTT: " + trunkprefix);
		return null;
//			public OneTunnel(String lname, LegLineFormat newLLF)
*/
	}
}

