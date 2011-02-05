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
        stat.executeUpdate("drop table if exists path;"); 
        stat.executeUpdate("drop table if exists images;"); 
        stat.executeUpdate("create table path (i integer, linestyle integer, d text);"); 
        stat.executeUpdate("create table images (i integer, url text, scale real, rotdeg real, xtrans real, ytrans real);"); 
    } catch (java.sql.SQLException e) {System.out.println(e);} }


    void WritePaths(List<OnePath> vpaths)
    { try {
        PreparedStatement prep = conn.prepareStatement("insert into path values (?,?,?);");
        int i = 0; 
        for (OnePath op : vpaths)
        {
            prep.setInt(1, i);
            prep.setInt(2, op.linestyle);
            prep.setString(3, op.svgdvalue(0.0F, 0.0F));
            prep.addBatch();
            i++; 
        }
        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
    } catch (java.sql.SQLException e) {System.out.println(e);} }


    void WriteImages(List<OnePath> vpaths)
    { try {
        PreparedStatement prep = conn.prepareStatement("insert into images values (?,?,?,?,?,?);");
        int i = 0; 
        for (OnePath op : vpaths)
        {
            if ((op.plabedl != null) && (op.plabedl.iarea_pres_signal != 0) && (op.plabedl.barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) && (op.plabedl.sketchframedef != null))
            {
                SketchFrameDef sfd = op.plabedl.sketchframedef; 
                prep.setInt(1, i);
                prep.setString(2, sfd.sfsketch); 
                prep.setDouble(3, sfd.sfscaledown); 
                prep.setDouble(4, sfd.sfrotatedeg); 
                prep.setDouble(5, sfd.sfxtrans); 
                prep.setDouble(6, sfd.sfytrans); 
                prep.addBatch();
            }
            i++; 
        }
        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
    } catch (java.sql.SQLException e) {System.out.println(e);} }


  public static void test() 
  {
    String filename = "test.db"; 
try {
    Class.forName("org.sqlite.JDBC");
    Connection conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
    Statement stat = conn.createStatement();
    stat.executeUpdate("drop table if exists people;");
    stat.executeUpdate("create table people (name, occupation);");
    PreparedStatement prep = conn.prepareStatement(
      "insert into people values (?, ?);");

    prep.setString(1, "Gandhi");
    prep.setString(2, "politics");
    prep.addBatch();
    prep.setString(1, "Turing");
    prep.setString(2, "computers");
    prep.addBatch();
    prep.setString(1, "Wittgenstein");
    prep.setString(2, "smartypants");
    prep.addBatch();

    conn.setAutoCommit(false);
    prep.executeBatch();
    conn.setAutoCommit(true);

    ResultSet rs = stat.executeQuery("select * from people;");
    while (rs.next()) {
      System.out.println("name = " + rs.getString("name"));
      System.out.println("job = " + rs.getString("occupation"));
    }
    rs.close();
    conn.close();
}
catch (java.sql.SQLException e) {System.out.println(e);}
catch (java.lang.ClassNotFoundException e) {System.out.println(e);}
  }
}