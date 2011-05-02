////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2011  Julian Todd.
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

import java.util.List; 
import java.util.ArrayList; 

import java.io.InputStream; 
import java.io.InputStreamReader; 
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ConnectException;

import java.util.Collections; 


/////////////////////////////////////////////
class PthChange
{
    String uuid; 
    String action; 
    String val = ""; 
    String sketchuuid; 

    PthChange(OnePath op, String laction, String lsketchuuid)
    {
        uuid = op.uuid; 
        action = laction; 
        sketchuuid = lsketchuuid; 
        if (action.equals("add"))
        {
            LineOutputStream los = new LineOutputStream();
        	try { 
                op.WriteXMLpath(los, -1, -1, 0); } 
            catch (IOException ie) {;}
            val = los.sb.toString(); 
        }
    }
}


////////////////////////////////////////////////////////////////////////////////
class NetConnection implements Runnable
{
    URL urlnetconnection = null; 
    Thread ncthread = null; 
    List<PthChange> pthchanges = Collections.synchronizedList(new ArrayList<PthChange>()); 
    MainBox mainbox; 

    NetConnection(MainBox lmainbox)
    {
        mainbox = lmainbox; 
    }

    /////////////////////////////////////////////
    public void run() 
    {
        try
        {
        while (true)
        {
            Thread.sleep(500); 
            if (!pthchanges.isEmpty())
                PostPathChangeBatch(); 
        }
        }
        catch (InterruptedException ie)
        {;}
        ncthread = null; 
    }

    /////////////////////////////////////////////
    void PostPathChangeBatch()
    {
        List<String> args = new ArrayList<String>(); 
        int ipthchanges = 0; 
        String lsketchuuid = null; 
        for (  ; ipthchanges < 10; ipthchanges++)
        {
            if (ipthchanges >= pthchanges.size())
                break; 

            if (ipthchanges == 0)
                lsketchuuid  = pthchanges.get(ipthchanges).sketchuuid; 
            else if (!pthchanges.get(ipthchanges).sketchuuid.equals(lsketchuuid))
                break; 

            PthChange pthchange = pthchanges.get(ipthchanges); 
            args.add("uuid_"+ipthchanges); 
            args.add(pthchange.uuid); 
            args.add("action_"+ipthchanges); 
            args.add(pthchange.action); 
            args.add("val_"+ipthchanges); 
            args.add(pthchange.val); 
        }
        if (ipthchanges == 0)
            return;  
        args.add("npthchanges"); 
        args.add(String.valueOf(ipthchanges)); 
        args.add("sketchuuid"); 
        args.add(lsketchuuid); 
        try 
        {
            String res = SimplePost(urlnetconnection, args); 
            System.out.println(res); 
        }
        catch (ConnectException ce) 
        {
            TN.emitWarning("Connection refused, turning off"); 
            mainbox.miNetConnection.setState(false); 
            return; 
        }
        catch (IOException ie) {  System.out.println(ie); }

        while (ipthchanges > 0)
            pthchanges.remove(--ipthchanges); 
    }


    /////////////////////////////////////////////
    void netcommitpathchange(OnePath op, String action, OneSketch tsketch)
    {
        if (mainbox.miNetConnection.getState())
            pthchanges.add(new PthChange(op, action, tsketch.sketchfile.getPath())); 
    }

    /////////////////////////////////////////////
    void ncstart(String snetconnection)
    {
        try
        {
            urlnetconnection = new URL(snetconnection);
        }
        catch (MalformedURLException e)
            { TN.emitWarning("yyy"); return; }
        ncthread = new Thread(this); 
        ncthread.start(); 
        mainbox.miNetConnection.setState(true); 
        try
        {
            List<String> args = new ArrayList<String>(); 
            args.add("connectionmade"); 
            args.add(TN.tunnelversion); 
            String d = SimplePost(urlnetconnection, args); 
            System.out.println("Response:"+d); 
        }
        catch (ConnectException ce) 
        {
            TN.emitWarning("Connection refused, turning off"); 
            mainbox.miNetConnection.setState(false); 
        }
        catch (IOException ie) {;}
    }

    /////////////////////////////////////////////
    /////////////////////////////////////////////
	static String boundry = "-----xxxxxxxBOUNDERxxxxdsx";   // something that is unlikely to appear in the text
	public static void writeField(DataOutputStream out, String name, String value) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
		out.writeBytes(value);
		out.writeBytes("\r\n");
		out.flush();
        //TN.emitMessage("WriteField: " + name + "=" + value); 
	}

	/////////////////////////////////////////////
    static void writeTileImageFile(DataOutputStream out, String fieldname, String filename, BufferedImage bi) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + fieldname + "\"; filename=\"" + filename + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Type: image/png");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
		ImageIO.write(bi, "png", out);
		out.writeBytes("\r\n");
	}

	/////////////////////////////////////////////
    static void writeSketchFile(DataOutputStream out, String name, String fileName, OneSketch tsketch) throws java.io.IOException
	{
		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("\r\n");
		out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Type: text/plain");
		out.writeBytes("\r\n");
		out.writeBytes("\r\n");
    	LineOutputStream los = new LineOutputStream(out);
        tsketch.SaveSketchLos(los); 
		out.writeBytes("\r\n");
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
    // returns the URL of the image that has been uploaded
    // should be a member function
	public static FileAbstraction uploadFile(FileAbstraction target, String fieldname, String filename, BufferedImage bi, OneSketch tsketch)
	{
        String fres = ""; 
		try
		{
        assert target.localurl != null; 
		String response = "";
		URL url = target.localurl;

        // append a command onto the end of the url to say what we're doing
        if (url.toString().endsWith(".xml"))
            url = new URL(url.toString() + "/upload"); 

        TN.emitMessage("About to post\nURL: " + url.toString());

		URLConnection conn = url.openConnection();

		// Set connection parameters.
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);

		// Make server believe we are form data
		conn.setRequestProperty("Content-Type",
                                "multipart/related; boundary=" + boundry);
        //		connection.setRequestProperty("MIME-version", "1.0");

		DataOutputStream out = new DataOutputStream (conn.getOutputStream());
        //		out.write(("--" + boundry + " ").getBytes());

        // write some fields
		writeField(out, "tunneluser", TN.tunneluser);
		writeField(out, "tunnelpassword", TN.tunnelpassword);
		writeField(out, "tunnelproject", TN.tunnelproject);
		writeField(out, "tunnelversion", TN.tunnelversion);

		// Write out the bytes of the content string to the stream.
        assert ((bi == null) != (tsketch == null)); 
        if (bi != null)
	       writeTileImageFile(out, fieldname, filename, bi);
	    else
           writeSketchFile(out, fieldname, filename, tsketch);

		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("--");
		out.writeBytes("\r\n");
		out.flush();
		out.close();

		// Read response from the input stream (should detect the file name).
        // directly from fileupload.html in the django template
        String suploadedfile = "<li>UPLOADEDFILE: "; 
        String smessage = "<p>MESSAGE: "; 
        //Pattern pattuploadedfile = Pattern.compile("<li>UPLOADEDFILE: \"(.*?)\"</li>"); 
        //Pattern pattmessage = Pattern.compile("<p>MESSAGE: (.*?)</li>"); 

		BufferedReader fin = new BufferedReader(new InputStreamReader(conn.getInputStream ()));
		String fline;
		System.out.println("Server response:");
		while ((fline = fin.readLine()) != null)
        {
            System.out.println(":" + fline);
            int iuploadedfile = fline.indexOf(suploadedfile); 
            if (iuploadedfile != -1)
            {
                fres = fline.substring(iuploadedfile + suploadedfile.length()); 
                System.out.println("FIFIF:" + fres + ":::"); 
      		}
        }
		fin.close();
		}

		catch (MalformedURLException e)
			{ TN.emitWarning("yyy"); return null;}
		catch (IOException e)
			{ TN.emitWarning("eee " + e.toString()); return null;}

		return FileAbstraction.MakeOpenableFileAbstraction(fres);
	}



	/////////////////////////////////////////////
    // see: http://seagrass.goatchurch.org.uk/~mjg/cgi-bin/addsurvey.py
    //<placement copyright="left" image="irebyoverlay2.png"
    //spatialreference="WGS84-UTM30">
    //<point grideast="532601" gridnorth="6004830" imx="2550" imy="1734"/>
    //<ewpoint grideast="532651" gridnorth="6004830" imx="2943" imy="1734"/>
    //<nspoint grideast="532601" gridnorth="6004880" imx="2550" imy="1341"/>
	public static void upmjgirebyoverlay(BufferedImage bi, String name, double dpmetrereal, double cornercoordX, double cornercoordY, String spatial_reference_system)
	{
		try
		{
		String target = "http://seagrass.goatchurch.org.uk/caves/image/upload_and_locate_image"; 
        TN.emitMessage("About to post\nURL: " + target);
        System.out.println(" fname=" + name + " ----dots per metre " + dpmetrereal + "  XX " + cornercoordX + "  YY " + cornercoordY + "  spatial_system " + spatial_reference_system); 

		String response = "";
		URL url = new URL(target);
		URLConnection conn = url.openConnection();

		// Set connection parameters.
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);

		// Make server believe we are form data
		conn.setRequestProperty("Content-Type",
                                "multipart/related; boundary=" + boundry);
        //		connection.setRequestProperty("MIME-version", "1.0");

		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        //		out.write(("--" + boundry + " ").getBytes());
        // write some fields
    	//	name=surveyname, acknowledgment=tunnelupload, copyright=left

		writeField(out, "name-name", name);
		writeField(out, "acknowledgment", "tunnelupload");
		writeField(out, "copyright", "left");
		writeField(out, "tunnelversion", TN.tunnelversion);
		//writeField(out, "spatial_reference_system", "WGS84-UTM30");

        int srsid = 31285; // MGI / M31
        //if (spatial_reference_system.equals("OS Grid SD"))
        //   srsid = 13000; 

        double cavealtitude = 1800.0; 

		writeField(out, "at-srsid", String.valueOf(srsid)); 

        
// we want to get these all coming in from the change things
// the GPS signals are 50 apart.  
// GPS from the Ireby is *fix	001	66668.00	78303.00	319.00	;GPS Added    09/01/05 N.P


        double gpx = cornercoordX; 
        double gpy = cornercoordY; 
        //double gpx = 532601 + (cornercoordX - 66668.00); 
        //double gpy = 6004830 + (cornercoordY - 78303.00); 
        double d100 = dpmetrereal * 100;
        String sd100 = String.valueOf(d100);  

        String sdYbot = String.valueOf(bi.getHeight()); 
        String sdYbotup100 = String.valueOf(bi.getHeight() - d100); 
        
		writeField(out, "p1-worldX", String.valueOf(gpx));
		writeField(out, "p1-worldY", String.valueOf(gpy - 100));
		writeField(out, "p1-imageX", "0");
		writeField(out, "p1-imageY", sdYbotup100);
		writeField(out, "p2-worldX", String.valueOf(gpx + 100));
		writeField(out, "p2-worldY", String.valueOf(gpy - 100));
		writeField(out, "p2-imageX", sd100);
		writeField(out, "p2-imageY", sdYbotup100);
		writeField(out, "p3-worldX", String.valueOf(gpx));
		writeField(out, "p3-worldY", String.valueOf(gpy));
		writeField(out, "p3-imageX", "0");
		writeField(out, "p3-imageY", sdYbot);
		writeField(out, "at-average_image_altitude", String.valueOf(cavealtitude));

		// Write out the bytes of the content string to the stream.
        writeTileImageFile(out, "name-image", name + ".png", bi);

		out.writeBytes("--");
		out.writeBytes(boundry);
		out.writeBytes("--");
		out.writeBytes("\r\n");
		out.flush();
		out.close();


		BufferedReader fin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String fline;
		System.out.println("Server response:");
		while ((fline = fin.readLine()) != null)
            System.out.println(":" + fline);
		fin.close();
		}
		catch (MalformedURLException e)
			{ TN.emitWarning("yyy");}
		catch (IOException e)
			{ TN.emitWarning("eee " + e.toString());};
	}


    public static String SimplePost(URL url, List<String> args) throws IOException
    {
        URLConnection conn = url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundry);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        for (int i = 1; i < args.size(); i += 2)
            writeField(out, args.get(i-1), args.get(i));
        out.writeBytes("--");
        out.writeBytes(boundry);
        out.writeBytes("--");
        out.writeBytes("\r\n");
        out.flush();
        out.close();

        BufferedReader fin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer response = new StringBuffer(); 
        String fline;
        while ((fline = fin.readLine()) != null)
            { response.append(fline); response.append(TN.nl) }
        fin.close();
        return response.toString();
    }

}


