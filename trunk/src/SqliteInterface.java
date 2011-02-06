package Tunnel;

import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet; 
import java.sql.SQLException; 

public class SqliteInterface 
{
    Connection conn; 
    SqliteInterface(String path)
    { try {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:"+path);
    } catch (java.sql.SQLException e) {System.out.println(e);} catch (java.lang.ClassNotFoundException e) {System.out.println(e);} }
    
    void CreateTables()
    { try {
        Statement stat = conn.createStatement();

        stat.executeUpdate("drop table if exists paths;"); 
        String pathfields = "pathid integer unique, linestyle text, d text, bsplined boolean, "+
                            "pathidtailleft integer, btailleftfore boolean, "+
                            "pathidheadright integer, bheadrightfore boolean, "+
                            "zalttail real, zalthead real"; 
        stat.executeUpdate("create table paths ("+pathfields+");"); 

// centreline node names and zalt values in separate table?
// also need to insert locx and locy into this scheme
// then some calculated tables of symbol layouts and connected components

        stat.executeUpdate("drop table if exists pathsymbols;"); 
        stat.executeUpdate("create table pathsymbols (pathid integer, labsymb text);"); 

        stat.executeUpdate("drop table if exists pathlabels;"); 
        String pathlabelfields = "pathid integer, sfontcode text, drawlab text, fnodeposxrel real, fnodeposyrel real, barrowpresent boolean, bboxpresent boolean";  
        stat.executeUpdate("create table pathlabels ("+pathlabelfields+");"); 

        stat.executeUpdate("drop table if exists pathareasignals;"); 
        stat.executeUpdate("create table pathareasignals (pathid integer, areasignal text, zsetrelative real);"); 


        stat.executeUpdate("drop table if exists sketchframes;"); 
        String sketchframefields = "pathid integer, sfsketch text, scaledown real, rotatedeg real, xtrans real, ytrans real, submapping text, style text, imagepixelswidth integer, imagepixelsheight integer"; 
        stat.executeUpdate("create table sketchframes ("+sketchframefields+");"); 

    } catch (java.sql.SQLException e) {System.out.println(e);} }


    void WritePaths(List<OnePath> vpaths)
    { try {
        for (int i = 0; i < vpaths.size(); i++)
            vpaths.get(i).svgid = i; 

        PreparedStatement preppath = conn.prepareStatement("insert into paths values (?,?,?,?,?, ?,?,?,?,?);");
        PreparedStatement preppathsymbol = conn.prepareStatement("insert into pathsymbols values (?,?);");
        PreparedStatement preppathlabel = conn.prepareStatement("insert into pathlabels values (?,?,?,?,?, ?,?);");
        PreparedStatement preppathareasignal = conn.prepareStatement("insert into pathareasignals values (?,?,?);");
        PreparedStatement prepsketchframe = conn.prepareStatement("insert into sketchframes values (?,?,?,?,?, ?,?,?,?,?);");
        for (OnePath op : vpaths)
        {
            preppath.setInt(1, op.svgid);
            preppath.setString(2, SketchLineStyle.shortlinestylenames[op.linestyle]);
            preppath.setString(3, "NOOOOO"); //op.svgdvalue(0.0F, 0.0F));
            preppath.setBoolean(4, op.bSplined);
            preppath.setInt(5, (op.aptailleft == null ? op.aptailleft.svgid : -1)); 
            preppath.setBoolean(6, op.baptlfore); 
            preppath.setInt(7, (op.apforeright == null ? op.apforeright.svgid : -1)); 
            preppath.setBoolean(8, op.bapfrfore); 
            preppath.setFloat(9, op.pnstart.zalt); 
            preppath.setFloat(10, op.pnend.zalt); 
            preppath.addBatch();

            if (op.plabedl == null)
                continue; 
            for (String labsymb : op.plabedl.vlabsymb)
            {
                preppathsymbol.setInt(1, op.svgid);
                preppathsymbol.setString(2, labsymb);
                preppathsymbol.addBatch(); 
            }

            if (op.plabedl.sfontcode != null)
            {
                preppathlabel.setInt(1, op.svgid);
                preppathlabel.setString(2, op.plabedl.sfontcode);
                preppathlabel.setString(3, op.plabedl.drawlab);
                preppathlabel.setFloat(4, op.plabedl.fnodeposxrel);
                preppathlabel.setFloat(5, op.plabedl.fnodeposyrel);
                preppathlabel.setBoolean(6, op.plabedl.barrowpresent);
                preppathlabel.setBoolean(7, op.plabedl.bboxpresent);
                preppathlabel.addBatch(); 
            }

            if (op.plabedl.iarea_pres_signal != 0)
            {
                preppathareasignal.setInt(1, op.svgid);
                preppathareasignal.setString(2, SketchLineStyle.areasignames[op.plabedl.iarea_pres_signal]);
                preppathareasignal.setFloat(3, op.plabedl.nodeconnzsetrelative);
                preppathareasignal.addBatch(); 
            }

            if (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME)
            {
                prepsketchframe.setInt(1, op.svgid); 
                prepsketchframe.setString(2, op.plabedl.sketchframedef.sfsketch); 
                prepsketchframe.setFloat(3, op.plabedl.sketchframedef.sfscaledown); 
                prepsketchframe.setFloat(4, op.plabedl.sketchframedef.sfrotatedeg); 
                prepsketchframe.setDouble(5, op.plabedl.sketchframedef.sfxtrans); 
                prepsketchframe.setDouble(6, op.plabedl.sketchframedef.sfytrans); 
                    // Map<String, String> submapping = new TreeMap<String, String>();
                prepsketchframe.setString(7, "notset"); 
                prepsketchframe.setString(8, op.plabedl.sketchframedef.sfstyle); 
                prepsketchframe.setInt(9, op.plabedl.sketchframedef.imagepixelswidth); 
                prepsketchframe.setInt(10, op.plabedl.sketchframedef.imagepixelsheight); 
                prepsketchframe.addBatch(); 
            }
        }
        conn.setAutoCommit(false);
        preppath.executeBatch();
        preppathsymbol.executeBatch();
        preppathlabel.executeBatch();
        preppathareasignal.executeBatch();
        prepsketchframe.executeBatch();
        conn.setAutoCommit(true);
    } catch (java.sql.SQLException e) {System.out.println(e);} }
}