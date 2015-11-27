////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2012  Julian Todd.
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

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JCheckBox;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import java.awt.Shape;
import java.awt.geom.GeneralPath; 
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;
import java.awt.Color;
import java.awt.geom.PathIterator;
import java.awt.geom.FlatteningPathIterator;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;


/////////////////////////////////////////////
class SketchZTiltPanel extends JPanel
{
	SketchDisplay sketchdisplay;
	JButton buttselecttozselection = new JButton("Select to Z Selection"); 
	JCheckBox cbaShowTilt;
    JCheckBox cbaThinZheightsel; 
    JCheckBox cbaAnimateTour; 
	JButton buttanimatestep; 

	JTextField tfzlothinnedvisible = new JTextField();
	JTextField tfzhithinnedvisible = new JTextField();
	JTextField tfzstep = new JTextField("10.0");

	// z range thinning
	boolean bzthinnedvisible = false;   // causes reselecting of subset of paths in RenderBackground
	double zlothinnedvisible = -360.0; 
	double zhithinnedvisible = 20.0; 

    // animations along a sequence of centrelines
	List<OnePathNode> opnpathanimation = new ArrayList<OnePathNode>(); 
    int opnpathanimationPos = 0; 
    
    // animations that change the sketch frame 
    List< List<OnePath> > skopchains = new ArrayList< List<OnePath> >(); 
    float nskopchainpos;  // integral part defines which two positions we are between
    
    // quick jigsaw work here
    boolean bjigsawactivatedvisible = false; 
    Area jigsawarea = null; 
    Area jigsawareaoffset = null; 
    float jigsawareaoffsetdistance = 5.0F; 

                    
	/////////////////////////////////////////////
    SketchZTiltPanel(SketchDisplay lsketchdisplay)
    {
    	sketchdisplay = lsketchdisplay;

        cbaShowTilt = new JCheckBox("Show Tilted in z", false);
		cbaShowTilt.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miShowTilt.isSelected() != cbaShowTilt.isSelected())
                    sketchdisplay.miShowTilt.doClick();
			} } );
        cbaThinZheightsel = new JCheckBox("Thin Z Selection", false);
		cbaThinZheightsel.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (sketchdisplay.miThinZheightsel.isSelected() != cbaThinZheightsel.isSelected())
                    sketchdisplay.miThinZheightsel.doClick();
			} } );
            
		tfzhithinnedvisible.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e) {
                try  { zhithinnedvisible = Float.parseFloat(tfzhithinnedvisible.getText()) - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z; }
                catch(NumberFormatException nfe)  {;}
                SetUpdatezthinned(); 
            }}); 
		tfzlothinnedvisible.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent e) {
                try  { zlothinnedvisible = Float.parseFloat(tfzlothinnedvisible.getText()) - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z; }
                catch(NumberFormatException nfe)  {;}
                SetUpdatezthinned(); 
            }}); 
		buttselecttozselection.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { 
                SelectiontoZheightSelected();  SetUpdatezthinned(); 
			}});
            
        tfzlothinnedvisible.setText(String.valueOf(zlothinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
        tfzhithinnedvisible.setText(String.valueOf(zhithinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 

        cbaAnimateTour = new JCheckBox("Animate Tour", false);
		cbaAnimateTour.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event)  { 
                if (cbaAnimateTour.isSelected())
                    SetupAnimateTour(); 
			} } );
        buttanimatestep = new JButton("Animate Step"); 
        buttanimatestep.addActionListener(new ActionListener()
			{ public void actionPerformed(ActionEvent event) { 
                AnimateStep(); 
			}});
        
        
		JPanel panuppersec = new JPanel(new GridLayout(0, 2));
        panuppersec.add(cbaThinZheightsel); 
        panuppersec.add(cbaShowTilt);
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselWiden)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltOver));
		panuppersec.add(new JButton(sketchdisplay.acvThinZheightselNarrow)); 
		panuppersec.add(new JButton(sketchdisplay.acvTiltBack));
		panuppersec.add(buttselecttozselection); 
		panuppersec.add(new JButton(sketchdisplay.acvUpright)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneDown)); 
		panuppersec.add(new JLabel());
		panuppersec.add(new JButton(sketchdisplay.acvMovePlaneUp)); 
		panuppersec.add(new JLabel("zhi-thinned:"));
		panuppersec.add(tfzhithinnedvisible); 
		panuppersec.add(new JLabel("zlo-thinned:"));
		panuppersec.add(tfzlothinnedvisible); 
		panuppersec.add(new JLabel("zstep:"));
		panuppersec.add(tfzstep); 

		panuppersec.add(cbaAnimateTour); 
		panuppersec.add(buttanimatestep); 

        setLayout(new BorderLayout());
		add(panuppersec, BorderLayout.CENTER);
	}

    /////////////////////////////////////////////
    void SetupAnimateTour()
    {
        // animating on a sequence of centreline nodes
        OnePath op = sketchdisplay.sketchgraphicspanel.currgenpath; 
        if ((op != null) && (op.plabedl != null) && !op.plabedl.drawlab.equals(""))
        {
            LineInputStream lis = new LineInputStream(op.plabedl.drawlab, null); 
            opnpathanimation.clear(); 
            opnpathanimationPos = 0; 
            while (lis.FetchNextLine())
            {
                TN.emitMessage("anim on "+lis.w[0]); 
                OnePathNode aopn = null; 
                for (OnePathNode opn : sketchdisplay.sketchgraphicspanel.tsketch.vnodes)
                    if ((opn.pnstationlabel != null) && (opn.pnstationlabel.endsWith(lis.w[0])) && ((aopn == null) || (aopn.pnstationlabel.length() <= opn.pnstationlabel.length())))
                        aopn = opn; 
                TN.emitMessage("kk" + aopn); 
                opnpathanimation.add(aopn); 
            }
            if (opnpathanimation.size() == 0)
                cbaAnimateTour.setSelected(false); 
        }
        
        // animating on a sequence of sketchframes
        skopchains.clear(); 
        for (OneSArea osa : sketchdisplay.sketchgraphicspanel.tsketch.vsareas)
        {
            if ((osa.opsketchframedefs == null) || (osa.opsketchframedefs.size() == 0))
                continue; 
            for (OnePath skop : osa.opsketchframedefs)
            {
                List<OnePath> skopchain = new ArrayList<OnePath>(); 
                skopchain.add(skop); 
                // the full connective line connective component code can be found in GetConnCompPath()
                // we assume that the connective lines are oriented forwards from the root
                // (multi coincident sketch frame definitions could be incoming at each node)
                RefPathO rpocopy = new RefPathO();
                while (true)
                {
                    RefPathO ropfore = new RefPathO(skopchain.get(skopchain.size() - 1), true); 
                    rpocopy.ccopy(ropfore); 
                    OnePath opnextfore = null; 
                    do
                    {
                        if ((rpocopy.op.linestyle != SketchLineStyle.SLS_CONNECTIVE) && (rpocopy.op.linestyle != SketchLineStyle.SLS_CENTRELINE))
                            break;
                        if (!rpocopy.bFore && (rpocopy.op.linestyle == SketchLineStyle.SLS_CONNECTIVE))
                        {
                            if (opnextfore != null)
                                break; 
                            opnextfore = rpocopy.op; 
                        }
                    }
                    while (!rpocopy.AdvanceRoundToNode(ropfore));
                    if (!rpocopy.cequals(ropfore))
                        break;
                    if (opnextfore == null)
                        break; 
                    if (!opnextfore.IsSketchFrameConnective()/* && !op.plabedl.sketchframedef.sfsketch.equals("")*/)
                        break; 
                    if (skopchain.contains(opnextfore))
                        break; 
TN.emitMessage("adding to skopchain "+opnextfore);                         
                    skopchain.add(opnextfore); 
                }
                
                if (skopchain.size() >= 3)
                {
                    for (OnePath lskop : skopchain)
                        lskop.plabedl.sketchframedef.SetSketchFrameFiller(sketchdisplay.mainbox, sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale, sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchdisplay.sketchgraphicspanel.tsketch.sketchfile);
                    skopchains.add(skopchain); 
                }
                else 
                    TN.emitMessage("skopchain size "+skopchain.size()+" discarded"); 
            }
        }
        nskopchainpos = 1.0F; 
        if (skopchains.size() == 0)
            cbaAnimateTour.setSelected(false); 
    }


    /////////////////////////////////////////////
	Point2D.Float scrpt = new Point2D.Float();
	Point2D.Float realpt = new Point2D.Float();
    double animdiststep = 10.1; 
    void AnimateStep()
    {
        if (!opnpathanimation.isEmpty())
            AnimateStepCentrelines(); 
        else if (!skopchains.isEmpty())
            AnimateFramePositions(); 
        else
            cbaAnimateTour.setSelected(false); 
    }
    
    /////////////////////////////////////////////
    void AnimateFramePositions()
    {
        boolean bsetframe = false; 
        int inskopchainpos = (int)nskopchainpos; 
        float lam = nskopchainpos - inskopchainpos; 

        Point2D.Float acenpt = new Point2D.Float(); 
        Point2D.Float acenpt0 = new Point2D.Float(); 
        Point2D.Float acenpt1 = new Point2D.Float(); 
        Point2D.Float acenptlam = new Point2D.Float(); 
        Point2D.Float acenptlamS = new Point2D.Float(); 

        for (List<OnePath> skopchain : skopchains)
        {
            OnePath oproot = skopchain.get(0); 
            assert oproot.IsSketchFrameConnective(); 
            if (nskopchainpos > skopchain.size() - 1)
                continue; 
            bsetframe = true; 
            OnePath op0 = skopchain.get(inskopchainpos); 
            assert op0.IsSketchFrameConnective(); 
            if (lam == 0.0)
            {
                oproot.plabedl.sketchframedef.sfscaledown = op0.plabedl.sketchframedef.sfscaledown;
                oproot.plabedl.sketchframedef.sfrotatedeg = op0.plabedl.sketchframedef.sfrotatedeg;
                oproot.plabedl.sketchframedef.sfxtrans = op0.plabedl.sketchframedef.sfxtrans;
                oproot.plabedl.sketchframedef.sfytrans = op0.plabedl.sketchframedef.sfytrans;
                oproot.plabedl.sketchframedef.sfsketch = op0.plabedl.sketchframedef.sfsketch;
            }
            else
            {
                OnePath op1 = skopchain.get(inskopchainpos + 1); 
                assert op1.IsSketchFrameConnective(); 

                // we want the projection to the centre point of the area to be smooth
                acenpt.setLocation(oproot.kaleft.rboundsarea.getX() + oproot.kaleft.rboundsarea.getWidth()*0.5,  oproot.kaleft.rboundsarea.getY() + oproot.kaleft.rboundsarea.getHeight()*0.5); 
                try
                {
                    op0.plabedl.sketchframedef.pframesketchtrans.inverseTransform(acenpt, acenpt0);
                    op1.plabedl.sketchframedef.pframesketchtrans.inverseTransform(acenpt, acenpt1);
                }
                catch (NoninvertibleTransformException ex)
                {;}
                

                oproot.plabedl.sketchframedef.sfscaledown = op0.plabedl.sketchframedef.sfscaledown*(1.0F - lam) + op1.plabedl.sketchframedef.sfscaledown*lam;
                oproot.plabedl.sketchframedef.sfrotatedeg = op0.plabedl.sketchframedef.sfrotatedeg*(1.0F - lam) + op1.plabedl.sketchframedef.sfrotatedeg*lam;

                acenptlam.setLocation(acenpt0.getX()*(1.0 - lam) + acenpt1.getX()*lam, acenpt0.getY()*(1.0 - lam) + acenpt1.getY()*lam);  

                AffineTransform lpframesketchtrans = new AffineTransform();
                //pframesketchtrans.translate((-lsketchLocOffset.x + sfxtrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION, (+lsketchLocOffset.y + sfytrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION);
                lpframesketchtrans.scale(sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale / oproot.plabedl.sketchframedef.sfscaledown, sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale / oproot.plabedl.sketchframedef.sfscaledown);
                lpframesketchtrans.rotate(-Math.toRadians(oproot.plabedl.sketchframedef.sfrotatedeg));
                lpframesketchtrans.translate(oproot.plabedl.sketchframedef.pframesketch.sketchLocOffset.x * TN.CENTRELINE_MAGNIFICATION, -oproot.plabedl.sketchframedef.pframesketch.sketchLocOffset.y * TN.CENTRELINE_MAGNIFICATION);
                lpframesketchtrans.transform(acenptlam, acenptlamS);
                //oproot.plabedl.sketchframedef.sfxtrans = op0.plabedl.sketchframedef.sfxtrans*(1.0F - lam) + op1.plabedl.sketchframedef.sfxtrans*lam;
                //oproot.plabedl.sketchframedef.sfytrans = op0.plabedl.sketchframedef.sfytrans*(1.0F - lam) + op1.plabedl.sketchframedef.sfytrans*lam;
                // solve: 
                //   (-lsketchLocOffset.x + sfxtrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION == acenptlamS.getX() - acenpt.getX() 
                //   (+lsketchLocOffset.y + sfytrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION == acenptlamS.getY() - acenpt.getY()
                // which comes from: pframesketchtrans.translate((-lsketchLocOffset.x + sfxtrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION, (+lsketchLocOffset.y + sfytrans * lrealpaperscale) * TN.CENTRELINE_MAGNIFICATION);
                oproot.plabedl.sketchframedef.sfxtrans = ((acenpt.getX() - acenptlamS.getX()) / TN.CENTRELINE_MAGNIFICATION + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.x) / sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale; 
                oproot.plabedl.sketchframedef.sfytrans = ((acenpt.getY() - acenptlamS.getY()) / TN.CENTRELINE_MAGNIFICATION - sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.y) / sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale; 
            }
			oproot.plabedl.sketchframedef.SetSketchFrameFiller(sketchdisplay.mainbox, sketchdisplay.sketchgraphicspanel.tsketch.realposterpaperscale, sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset, sketchdisplay.sketchgraphicspanel.tsketch.sketchfile);
        }
        
        if (bsetframe)
        {
            float lamstep = 0.1F; 
            try 
            {
                float llamstep = Float.parseFloat(tfzstep.getText()); 
                lamstep = (llamstep > 1.0F ? 1.0F/llamstep : llamstep); 
            }
            catch(NumberFormatException nfe)  {;}
            float nlam = lam + lamstep; 
            nskopchainpos = (nlam > 0.99 ? (float)(inskopchainpos + 1) : inskopchainpos + nlam); 
            sketchdisplay.sketchgraphicspanel.bNextRenderDetailed = true; 
            sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
        }
        else
            cbaAnimateTour.setSelected(false); 
    }
    
    /////////////////////////////////////////////
    void AnimateStepCentrelines()
    {
        Dimension csize = sketchdisplay.sketchgraphicspanel.csize; 
		try
		{
			scrpt.setLocation(csize.width / 2, csize.height / 2);
			sketchdisplay.sketchgraphicspanel.currtrans.inverseTransform(scrpt, realpt);
		}
		catch (NoninvertibleTransformException ex)
            { realpt.setLocation(0, 0); }

TN.emitMessage("opnpathanimationPos " + opnpathanimationPos + "  " + opnpathanimation.size());
        if (opnpathanimationPos >= opnpathanimation.size())
        {
            cbaAnimateTour.setSelected(false); 
            return; 
        }

        Point2D.Float targetpt = opnpathanimation.get(opnpathanimationPos).pn; 
        float targetz = opnpathanimation.get(opnpathanimationPos).zalt; 
        double rx = realpt.getX(); 
        double ry = realpt.getY(); 
        double rz = (zlothinnedvisible + zhithinnedvisible) / 2;  
        
        double vx = targetpt.getX() - rx; 
        double vy = targetpt.getY() - ry; 
        double vz = (sketchdisplay.miThinZheightsel.isSelected() ? targetz - rz : 0.0); 
        double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz); 
        double lam = (animdiststep < vlen ? animdiststep / vlen : 1.0); 

        if (lam == 1.0)
            opnpathanimationPos++; 

        TN.emitMessage("lam " + lam);
        realpt.setLocation(rx + vx*lam, ry + vy*lam); 
		sketchdisplay.sketchgraphicspanel.currtrans.transform(realpt, scrpt);

        //sketchdisplay.sketchgraphicspanel.Translate(-(scrpt.getX() - csize.width / 2) / csize.width, -(scrpt.getY() - csize.height / 2) / csize.height); 
		sketchdisplay.sketchgraphicspanel.mdtrans.setToTranslation(-(scrpt.getX() - csize.width / 2), -(scrpt.getY() - csize.height / 2));
		sketchdisplay.sketchgraphicspanel.orgtrans.setTransform(sketchdisplay.sketchgraphicspanel.currtrans);
		sketchdisplay.sketchgraphicspanel.currtrans.setTransform(sketchdisplay.sketchgraphicspanel.mdtrans);
		sketchdisplay.sketchgraphicspanel.currtrans.concatenate(sketchdisplay.sketchgraphicspanel.orgtrans);

        if (sketchdisplay.miThinZheightsel.isSelected())
        {
            zlothinnedvisible = rz + vz*lam - animdiststep*10; 
            zhithinnedvisible = rz + vz*lam + animdiststep*10; 
            SetUpdatezthinned(); 
        }
        else
            sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
    }
    
    /////////////////////////////////////////////
    void SetUpdatezthinned()
    {
        if (zhithinnedvisible < zlothinnedvisible)
            zhithinnedvisible = zlothinnedvisible; 
        tfzlothinnedvisible.setText(String.valueOf(zlothinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
        tfzhithinnedvisible.setText(String.valueOf(zhithinnedvisible + sketchdisplay.sketchgraphicspanel.tsketch.sketchLocOffset.z)); 
		sketchdisplay.sketchgraphicspanel.UpdateTilt(true); 
		sketchdisplay.sketchgraphicspanel.RedoBackgroundView(); 
	}

	/////////////////////////////////////////////
	boolean SelectiontoZheightSelected()
	{
        sketchdisplay.sketchgraphicspanel.CollapseVActivePathComponent(); 
        Set<OnePath> opselset = sketchdisplay.sketchgraphicspanel.MakeTotalSelList(); 
        if (opselset.isEmpty())
            return TN.emitWarning("No selection set for thinning by z so leaving the same"); 

        boolean bfirst = true; 
        for (OnePath op : opselset)
        {
            if (bfirst)
            {
                zlothinnedvisible = op.pnstart.zalt; 
                zhithinnedvisible = op.pnstart.zalt; 
                bfirst = false; 
            }
            else
            {
                if (op.pnstart.zalt < zlothinnedvisible)
                    zlothinnedvisible = op.pnstart.zalt; 
                else if (op.pnstart.zalt > zhithinnedvisible)
                    zhithinnedvisible = op.pnstart.zalt; 
            }
            if (op.pnend.zalt < zlothinnedvisible)
                zlothinnedvisible = op.pnend.zalt; 
            else if (op.pnend.zalt > zhithinnedvisible)
                zhithinnedvisible = op.pnend.zalt; 
        }
        return true; 
    }

    /////////////////////////////////////////////
    void AddOffsetLine(float xb, float yb, float xf, float yf)
    {
        float xv = xf - xb; 
        float yv = yf - yb; 
        float vlen = (float)Math.sqrt(xv*xv + yv*yv); 
        if (vlen == 0.0)
            return; 
        float fac = jigsawareaoffsetdistance/vlen; 
        float xp = yv*fac; 
        float yp = -xv*fac; 

        GeneralPath gpwork = new GeneralPath(); 
        gpwork.moveTo(xb+xp, yb+yp); 
        gpwork.lineTo(xb-xp, yb-yp); 
        gpwork.lineTo(xf-xp, yf-yp); 
        gpwork.lineTo(xf+xp, yf+yp); 
        gpwork.closePath(); 
        jigsawareaoffset.add(new Area(gpwork)); 
        Ellipse2D ell = new Ellipse2D.Float(xf-jigsawareaoffsetdistance, yf-jigsawareaoffsetdistance, jigsawareaoffsetdistance*2, jigsawareaoffsetdistance*2); 
        jigsawareaoffset.add(new Area(ell)); 
    }

	/////////////////////////////////////////////
    // this is very crude and shouldn't work, but seems to do okay
	boolean SetJigsawContour()
	{
        TN.emitMessage("Filling area"); 
        jigsawarea = new Area(); 
        for (OneSArea osa : sketchdisplay.sketchgraphicspanel.tsketch.vsareas)
            jigsawarea.add((Area)osa.aarea); 
            
        //float jigsawareaoffsetdistance = 5.0F; 
        float jigsawareaoffsetflatness = 0.5F; 
        TN.emitMessage("offsetting area"); 
        jigsawareaoffset = new Area(); 
        jigsawareaoffset.add(jigsawarea); 
        
 		// maybe we will do this without flattening paths in the future.
        int nmoves = 0; 
        int nlines = 0; 
		float[] coords = new float[6];
		FlatteningPathIterator fpi = new FlatteningPathIterator(jigsawarea.getPathIterator(null), jigsawareaoffsetflatness); 
		float x0=0, y0=0;
        while (!fpi.isDone())
        {
            if (fpi.currentSegment(coords) != PathIterator.SEG_MOVETO) 
            {
                AddOffsetLine(x0, y0, coords[0], coords[1]); 
                nlines++;
            }
            else
                nmoves++;
            x0 = coords[0]; 
            y0 = coords[1]; 
            fpi.next();
        }
        TN.emitMessage("original area has "+nmoves+" contours and "+nlines+" lines"); 
        return true; 
    }

	/////////////////////////////////////////////
	void ApplyJigsawContour(boolean bjigsawactivated)
	{
        // on selection
		if (bjigsawactivated)
        {
            SetJigsawContour(); 
			bjigsawactivatedvisible = true; 
		}
		else  // on deselection
			bjigsawactivatedvisible = false; 
    }


	/////////////////////////////////////////////
	void ApplyZheightSelected(boolean bthinbyheight)
	{
        // on selection
		if (bthinbyheight)
        {
            SelectiontoZheightSelected(); 
			bzthinnedvisible = true; 
			TN.emitMessage("Thinning on z " + zlothinnedvisible + " < " + zhithinnedvisible); 
		}
		else  // on deselection
			bzthinnedvisible = false; 
        SetUpdatezthinned(); 
    }

    
	/////////////////////////////////////////////
	void WidenTiltPlane(int widencode)
	{
        double zwidgap = zhithinnedvisible - zlothinnedvisible; 
        double zwidgapfac = (widencode == 1 ? zwidgap / 2 : -zwidgap / 4); 
        zlothinnedvisible -= zwidgapfac; 
        zhithinnedvisible += zwidgapfac; 
        assert zlothinnedvisible <= zhithinnedvisible; 
        TN.emitMessage("Rethinning on z " + zlothinnedvisible + " < " + zhithinnedvisible); 
        SetUpdatezthinned(); 
    }


    /////////////////////////////////////////////
	void MoveTiltPlane(double stiltzchange)
	{
        double zstep = 50.0; 
        try  { zstep = Float.parseFloat(tfzstep.getText()); }
        catch(NumberFormatException nfe)  {;}
        double tiltzchange = stiltzchange * zstep; 
		zlothinnedvisible += tiltzchange;
		zhithinnedvisible += tiltzchange;
        SetUpdatezthinned(); 
	}
}


