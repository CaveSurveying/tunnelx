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
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.geometry.Cone; 
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior; // mouse controls

////////////////////////////////////////////////////////////////
public class PassageFloor3D extends JFrame
{
    private SimpleUniverse univ = null;
    private BranchGroup scene = null;
    private JPanel drawingPanel;

    Color3f col3fwhite = new Color3f(Color.white); 
    Color3f col3fblack = new Color3f(Color.black); 
    Color3f col3fred = new Color3f(Color.red); 
    Color3f col3fblue = new Color3f(Color.blue); 
    Color3f col3fgreen = new Color3f(Color.green); 
    
    Material redmaterial = new Material(col3fred, col3fblack, col3fred, col3fwhite, 100.0f);
    Material bluematerial = new Material(col3fblue, col3fblack, col3fblue, col3fwhite, 100.0f);
    Material greenmaterial = new Material(col3fgreen, col3fblack, col3fgreen, col3fwhite, 100.0f);

	float fpflatness = 0.5F;

    /////////////////////////////////////////////////////////
    public Shape3D createAreaShape(OneSArea osa) 
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
                pts.add(new Point3d(lpts.get(i).getX(), lpts.get(i).getY(), z0 + rpathlength*zfac)); 
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
    TransformGroup MakeAxes()
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
    public TransformGroup createSketchGroup(OneSketch tsketch) 
    {
        TransformGroup tgcave = new TransformGroup();
        for (OneSArea osa : tsketch.vsareas)
        {
            Shape3D shape = createAreaShape(osa); 
            if (shape != null)
                tgcave.addChild(shape); 
        }
        return tgcave; 
    }

    /////////////////////////////////////////////////////////
    public BranchGroup createSketchSceneGraph(OneSketch tsketch) 
    {
        TransformGroup tgcave = createSketchGroup(tsketch); 

        TransformGroup tgscale = new TransformGroup();
        Transform3D tscale = new Transform3D();
        tscale.setScale(0.01);
        tgscale.setTransform(tscale);
        tgscale.addChild(tgcave);

        BranchGroup branchgroup = new BranchGroup();
        branchgroup.addChild(tgscale);
        branchgroup.addChild(MakeAxes()); 
        SetBranchLighting(branchgroup); 

        branchgroup.compile();
        
        return branchgroup;
    }


    /////////////////////////////////////////////////////////
    BranchGroup SetBranchLighting(BranchGroup branchgroup)
    {
//System.out.println("bounds " + (new BoundingBox(bounds)).toString()); 
        Bounds bounds = branchgroup.getBounds(); // new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0); 
        
        // Set up the background
        Color3f bgColor = new Color3f(0.05f, 0.5f, 0.15f);
        Background bgNode = new Background(bgColor);
        bgNode.setApplicationBounds(bounds);

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

        return branchgroup;
    }

    /////////////////////////////////////////////////////////
    private Canvas3D createUniverse() 
    {
        // Get the preferred graphics configuration for the default screen
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

        // Create a Canvas3D using the preferred configuration
        Canvas3D c = new Canvas3D(config);
        
        // Create simple universe with view branch
        univ = new SimpleUniverse(c);
            
        // add mouse behaviors to the ViewingPlatform
        ViewingPlatform viewingPlatform = univ.getViewingPlatform();

        // add orbit behavior to the ViewingPlatform
        OrbitBehavior orbit = new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 10000.0);
        orbit.setSchedulingBounds(bounds);
        viewingPlatform.setViewPlatformBehavior(orbit);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
//        univ.getViewingPlatform().setNominalViewingTransform();
    
        // Ensure at least 5 msec per frame (i.e., < 200Hz)
        univ.getViewer().getView().setMinimumFrameCycleTime(5);
        univ.getViewer().getView().setBackClipDistance(5000);
    
        return c;
    }
    
    
    /////////////////////////////////////////////////////////
    public PassageFloor3D() 
    {
        // Initialize the GUI components
        initComponents();

        // Create Canvas3D and SimpleUniverse; add canvas to drawing panel
        Canvas3D c = createUniverse();
        drawingPanel.add(c, java.awt.BorderLayout.CENTER);
        
    }


    /////////////////////////////////////////////////////////
    private void initComponents() 
    {
        drawingPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("GearBox");
        drawingPanel.setLayout(new java.awt.BorderLayout());

        drawingPanel.setPreferredSize(new java.awt.Dimension(700, 700));
        getContentPane().add(drawingPanel, java.awt.BorderLayout.CENTER);

        pack();
    }
    
    /////////////////////////////////////////////////////////
	static void Make3Dview(OneSketch tsketch)
    {
        PassageFloor3D gb = new PassageFloor3D();

        // Create the content branch and add it to the universe
        gb.scene = gb.createSketchSceneGraph(tsketch);
        gb.univ.addBranchGraph(gb.scene);

        gb.setVisible(true);
        gb.univ.getViewingPlatform().setNominalViewingTransform();
    }
}


