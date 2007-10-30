// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PathLabelDecode.java

package Tunnel;

import java.util.List;

// Referenced classes of package Tunnel:
//            TunnelXMLparsebase, TunnelXML, PathLabelDecode, TNXML,
//            SketchLineStyle

class PathLabelXMLparse extends TunnelXMLparsebase
{

    PathLabelXMLparse()
    {
        sbtxt = new StringBuffer();
    }

    boolean ParseLabel(PathLabelDecode pathlabeldecode, String s, SketchLineStyle sketchlinestyle1)
    {
        pld = pathlabeldecode;
        sketchlinestyle = sketchlinestyle1;
        if(s.indexOf('<') == -1)
        {
            pld.sfontcode = "default";
            pld.drawlab = s;
            return true;
        } else
        {
            return (new TunnelXML()).ParseString(this, s) == null;
        }
    }

    public void startElementAttributesHandled(String s, boolean flag)
    {
label0:
        {
label1:
            {
label2:
                {
label3:
                    {
                        if(!s.equals(TNXML.sLRSYMBOL))
                            break label1;
                        String s1 = SeStack(TNXML.sAREA_PRESENT);
                        if(s1 == null)
                            break label2;
                        pld.iarea_pres_signal = 0;
                        int i = 0;
                        do
                        {
                            SketchLineStyle _tmp = sketchlinestyle;
                            if(i >= SketchLineStyle.nareasignames)
                                break label3;
                            SketchLineStyle _tmp1 = sketchlinestyle;
                            if(s1.equals(SketchLineStyle.areasignames[i]))
                                pld.iarea_pres_signal = i;
                            i++;
                        } while(true);
                    }
                    pld.barea_pres_signal = SketchLineStyle.areasigeffect[pld.iarea_pres_signal];
                }
                String s2 = SeStack(TNXML.sLRSYMBOL_NAME);
                if(s2 != null)
                    pld.vlabsymb.add(s2);
                break label0;
            }
            if(s.equals(TNXML.sTAIL) || s.equals(TNXML.sHEAD))
                sbtxt.setLength(0);
            else
            if(s.equals(TNXML.sLTEXT))
            {
                sbtxt.setLength(0);
                pld.sfontcode = SeStack(TNXML.sLTEXTSTYLE);
            } else
            if(s.equals("br"))
                sbtxt.append('\n');
        }
    }

    public void characters(String s)
    {
        if(sbtxt.length() != 0 && sbtxt.charAt(sbtxt.length() - 1) != '\n')
            sbtxt.append(' ');
        sbtxt.append(s);
    }

    public void endElementAttributesHandled(String s)
    {
        if(s.equals(TNXML.sHEAD))
            pld.centrelinehead = sbtxt.toString();
        else
        if(s.equals(TNXML.sTAIL))
            pld.centrelinetail = sbtxt.toString();
        else
        if(s.equals(TNXML.sLTEXT))
            pld.drawlab = sbtxt.toString();
    }

    PathLabelDecode pld;
    SketchLineStyle sketchlinestyle;
    StringBuffer sbtxt;
}
