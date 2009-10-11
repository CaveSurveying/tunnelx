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

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.FlatteningPathIterator;
import java.awt.Dimension; 
import java.awt.BorderLayout; 
import java.awt.event.MouseEvent; 

import java.awt.GraphicsConfiguration;
import java.awt.geom.Point2D;

import java.util.ArrayList; 
import java.util.List; 

import javax.swing.JPanel; 
import javax.swing.JFrame; 

import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
//import com.sun.j3d.utils.geometry.Triangulator;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.Bounds;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Background;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleFanArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.Material;
import javax.media.j3d.Appearance;
import javax.media.j3d.Transform3D;
import javax.media.j3d.PolygonAttributes; 

import javax.vecmath.Vector3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.geometry.Cone; 
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior; // mouse controls

////////////////////////////////////////////////////////////////
// source code here:  http://www.java2s.com/Open-Source/Java-Document/6.0-JDK-Modules/java-3d/com/sun/j3d/utils/behaviors/vp/OrbitBehavior.java.htm
//class LOrbitBehavior extends OrbitBehavior 
//{
    // doesn't work because it's protected
    // we'll need entirely new version of the rotation thing so we can lock the z axis up
    //protected void processMouseEvent(final MouseEvent evt) 
    //{
    //    System.out.println(evt); 
    //    ((OrbitBehavior)this).processMouseEvent(evt); 
    //}
//}


////////////////////////////////////////////////////////////////
public class PassageFloor3D extends JFrame
{
    GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
    Canvas3D canvas3d = new Canvas3D(config);
    SimpleUniverse universe = new SimpleUniverse(canvas3d);
    ViewingPlatform viewingPlatform = universe.getViewingPlatform();
    OrbitBehavior orbit = new OrbitBehavior(canvas3d);

    BranchGroup branchgroupcave = null;
    TransformGroup tgcave = null; 

    static Color3f col3fwhite = new Color3f(Color.white); 
    static Color3f col3fblack = new Color3f(Color.black); 
    static Color3f col3fred = new Color3f(Color.red); 
    static Color3f col3fblue = new Color3f(Color.blue); 
    static Color3f col3fgreen = new Color3f(Color.green); 
    
    static Material redmaterial = new Material(col3fred, col3fblack, col3fred, col3fwhite, 100.0f);
    static Material bluematerial = new Material(col3fblue, col3fblack, col3fblue, col3fwhite, 100.0f);
    static Material greenmaterial = new Material(col3fgreen, col3fblack, col3fgreen, col3fwhite, 100.0f);

	static float fpflatness = 0.5F;

    /////////////////////////////////////////////////////////
    static Shape3D createAreaShape(OneSArea osa) 
    {
        // Create an Appearance.
        Appearance look = new Appearance();

		Color col = (SketchLineStyle.bDepthColours ? SketchLineStyle.GetColourFromCollam(osa.icollam, true) : osa.subsetattr.areacolour); 
        Color3f objColor = new Color3f(col);
        Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
        Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
        look.setMaterial(new Material(objColor, black, objColor, white, 100.0f));
        look.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0.0f, true)); 

        List<Point3d> pts = new ArrayList<Point3d>(); 
        List<Point2D> lpts = new ArrayList<Point2D>();   // for reflecting the edges as we go along them
    	float[] coords = new float[6];

		// we should perform the hard task of reflecting certain paths in situ.
		for (RefPathO rpo : osa.refpathsub)
		{
            double pathlength = 0.0; 
            lpts.add(rpo.op.pnstart.pn); 
            FlatteningPathIterator fpi = new FlatteningPathIterator(rpo.op.gp.getPathIterator(null), fpflatness);
            while (!fpi.isDone())
            {
                int curvtype = fpi.currentSegment(coords);
                assert (curvtype == (lpts.size() == 1 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO));
                double xd = coords[0] - lpts.get(lpts.size() - 1).getX(); 
                double yd = coords[1] - lpts.get(lpts.size() - 1).getY(); 
                pathlength += Math.sqrt(xd*xd + yd*yd); 
                lpts.add(new Point2D.Float(coords[0], coords[1])); 
                fpi.next();
            }
            lpts.add(rpo.op.pnend.pn); 

            double z0 = rpo.FromNode().zalt; 
            double zfac = (rpo.ToNode().zalt - z0) / (pathlength != 0.0 ? pathlength : 1.0); 
            int i = (rpo.bFore ? 1 : lpts.size() - 2); 
            double rpathlength = 0.0; 
            while (rpo.bFore ? (i < lpts.size()) : (i >= 0))
            {
                int ip = i - (rpo.bFore ? 1 : -1); 
                double xd = lpts.get(i).getX() - lpts.get(ip).getX(); 
                double yd = lpts.get(i).getY() - lpts.get(ip).getY(); 
                rpathlength += Math.sqrt(xd*xd + yd*yd); 
                pts.add(new Point3d(lpts.get(i).getX(), -lpts.get(i).getY(), z0 + rpathlength*zfac));  // note the inversion of Y coordinate
                i += (rpo.bFore ? 1 : -1); 
            }

            lpts.clear(); 
        }
        if (pts.size() < 3)
            return null; 

        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        gi.setCoordinates(pts.toArray(new Point3d[pts.size()]));
        int[] stripCountArray = { pts.size() };
        int[] countourCountArray = { 1 };
        gi.setContourCounts(countourCountArray);
        gi.setStripCounts(stripCountArray);

        NormalGenerator normalGenerator = new NormalGenerator(0.02);
        normalGenerator.generateNormals(gi);

        return new Shape3D(gi.getGeometryArray(), look);
    }

    /////////////////////////////////////////////////////////
    static TransformGroup MakeAxes()
    {
        Appearance redappearance = new Appearance(); 
        redappearance.setMaterial(redmaterial);
        Appearance blueappearance = new Appearance(); 
        blueappearance.setMaterial(bluematerial);
        Appearance greenappearance = new Appearance(); 
        greenappearance.setMaterial(greenmaterial);

        TransformGroup tgzaxis = new TransformGroup(); 
        Transform3D tzaxis = new Transform3D();
        tzaxis.rotX(Math.PI / 2);
        tgzaxis.setTransform(tzaxis);
        tgzaxis.addChild(new Cone(1, 10, blueappearance)); 

        TransformGroup tgxaxis = new TransformGroup();
        Transform3D txaxis = new Transform3D();
        txaxis.rotZ(Math.PI / 2);
        tgxaxis.setTransform(txaxis);
        tgxaxis.addChild(new Cone(1, 10, greenappearance)); 

        TransformGroup tgaxes = new TransformGroup();
        tgaxes.addChild(new Cone(1, 10, redappearance));
        tgaxes.addChild(tgzaxis);
        tgaxes.addChild(tgxaxis);

        return tgaxes; 
    }


    /////////////////////////////////////////////////////////
    static TransformGroup createSketchGroup(TransformGroup tgcave, OneSketch tsketch) 
    {
        for (OneSArea osa : tsketch.vsareas)
        {
            Shape3D shape = createAreaShape(osa); 
            if (shape != null)
                tgcave.addChild(shape); 
        }
        return tgcave; 
    }

    /////////////////////////////////////////////////////////
    public BranchGroup createSketchSceneGraph(TransformGroup tgcave) 
    {
        BranchGroup branchgroup = new BranchGroup();
        branchgroup.setCapability(BranchGroup.ALLOW_DETACH); 
        branchgroup.addChild(tgcave);
        branchgroup.addChild(MakeAxes()); 
        AddBranchLighting(branchgroup); 

        branchgroup.compile();
        
        return branchgroup;
    }

    /////////////////////////////////////////////////////////
    static void AddBranchLighting(BranchGroup branchgroup)
    {
        Bounds bounds = branchgroup.getBounds(); // new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0); 
        
        // Set up the background
        Color3f bgColor = new Color3f(0.05f, 0.5f, 0.15f);
        Background bgNode = new Background(bgColor);
        bgNode.setApplicationBounds(new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0));

        // Set up the ambient light
        Color3f ambientColor = new Color3f(0.1f, 0.1f, 0.1f);
        AmbientLight ambientLightNode = new AmbientLight(ambientColor);
        ambientLightNode.setInfluencingBounds(bounds);

        // Set up the directional lights
        Color3f light1Color = new Color3f(1.0f, 1.0f, 0.9f);
        Vector3f light1Direction = new Vector3f(1.0f, 1.0f, 1.0f);
        Color3f light2Color = new Color3f(1.0f, 1.0f, 0.9f);
	    Vector3f light2Direction  = new Vector3f(-1.0f, -1.0f, -1.0f);

        DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
        light1.setInfluencingBounds(bounds);

        DirectionalLight light2 = new DirectionalLight(light2Color, light2Direction);
        light2.setInfluencingBounds(bounds);

        branchgroup.addChild(bgNode);
        branchgroup.addChild(ambientLightNode);
        branchgroup.addChild(light1);
        branchgroup.addChild(light2);
    }

    
    /////////////////////////////////////////////////////////
    public PassageFloor3D() 
    {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Cave view");

        orbit.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 10000.0));  // makes sure the mouse controls actually get found
        orbit.setTransFactors(100.0, 100.0); 
        //orbit.setRotXFactor(0.0); 
        orbit.setZoomFactor(100.0); 
        orbit.setReverseTranslate(true); 
        orbit.setReverseRotate(true); 
        viewingPlatform.setViewPlatformBehavior(orbit);
    
        // try and get a viewing position right
        Transform3D tl = new Transform3D(); 
        viewingPlatform.getViewPlatformTransform().getTransform(tl); 
        tl.setTranslation(new Vector3d(0.0, 0.0, 600.0)); 
        viewingPlatform.getViewPlatformTransform().setTransform(tl); 

        // Ensure at least 5 msec per frame (i.e., < 200Hz)
        universe.getViewer().getView().setMinimumFrameCycleTime(5);
        universe.getViewer().getView().setBackClipDistance(5000);

        JPanel panel;
        panel = new JPanel(new BorderLayout());
        panel.add(canvas3d, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(700, 700));
        getContentPane().add(panel, BorderLayout.CENTER);

        pack();
    }
    
    /////////////////////////////////////////////////////////
	void Make3Dview(OneSketch tsketch)
    {
        // Create the content branch and add it to the universe
        if (branchgroupcave != null)
            branchgroupcave.detach(); 
        branchgroupcave = null; 
        if (branchgroupcave == null) 
        {
            tgcave = createSketchGroup(new TransformGroup(), tsketch); 
            branchgroupcave = createSketchSceneGraph(tgcave);
            universe.addBranchGraph(branchgroupcave);
        }
        setVisible(true);
    }
}


