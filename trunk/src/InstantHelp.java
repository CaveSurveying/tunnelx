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

import javax.swing.JWindow;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JComboBox; 
import javax.swing.JScrollPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Point; 
import java.awt.Dimension; 
import java.awt.BorderLayout; 
import java.awt.Font;
import java.awt.Color; 

import java.util.List; 
import java.util.ArrayList; 

/////////////////////////////////////////////
class InstantHelp extends JFrame // JWindow
{
    SketchDisplay sketchdisplay; 
    JPanel hpanel = new JPanel(new BorderLayout()); 

    //JTextPane textpane = new JTextPane(); 
    JEditorPane textpane = new JEditorPane(); 
	JComboBox selectbox = new JComboBox(); 

    List<JMenuItem> mihelps = new ArrayList<JMenuItem>(); 
    List<String> helptitles = new ArrayList<String>(); 
    List<String> helptexts = new ArrayList<String>(); 

    boolean bfirst = true; 

	/////////////////////////////////////////////
    void ShowHelp(int i)
    {
        if (bfirst)
        {
            Point loc = sketchdisplay.getLocation(); 
            Dimension dim = sketchdisplay.getSize(); 
            setLocation((int)loc.getX() + dim.width / 3, (int)loc.getY() + dim.height / 3); 
            setSize((int)(dim.width * 0.6), (int)(dim.height * 0.7)); 
            bfirst = false; 
        }
        setVisible(true); 
        selectbox.setSelectedIndex(i); 
    }

	/////////////////////////////////////////////
	InstantHelp(SketchDisplay lsketchdisplay)
	{
        //super(lsketchdisplay);    // if doing this by a JWindow
        sketchdisplay = lsketchdisplay; 

		selectbox.setFont(new Font("Arial", Font.BOLD, 22));
        selectbox.setBackground(Color.white); 

        String lhelptext = FileAbstraction.helpFile.ReadFileHeadLB(-1); 
        int ih1 = lhelptext.indexOf("<h1>"); 
        while (ih1 != -1)
        {
            int ih1e = lhelptext.indexOf("</h1>", ih1); 
            String helptitle = lhelptext.substring(ih1 + 4, ih1e).trim(); 
            int ih1n = lhelptext.indexOf("<h1>", ih1e); 
            //String helptext = (ih1n != -1 ? lhelptext.substring(ih1, ih1n) : lhelptext.substring(ih1)).trim(); 
            String helptext = (ih1n != -1 ? lhelptext.substring(ih1e + 5, ih1n) : lhelptext.substring(ih1e + 5)).trim(); 
            ih1 = ih1n; 

            helptitles.add(helptitle); 
            helptexts.add(helptext); 

            JMenuItem mihelp = new JMenuItem(helptitle); 
            mihelp.addActionListener(new ActionListener()
                { public void actionPerformed(ActionEvent event) { ShowHelp(mihelps.indexOf(event.getSource())); } } );
            mihelps.add(mihelp); 
            selectbox.addItem(helptitle); 
        }

        textpane.setContentType("text/html"); 
        textpane.setEditable(false); 
        setAlwaysOnTop(true); 

        hpanel.add(selectbox, BorderLayout.NORTH); 
        hpanel.add(new JScrollPane(textpane), BorderLayout.CENTER); 

        selectbox.addActionListener(new ActionListener()
        { public void actionPerformed(ActionEvent event) { 
            int i = selectbox.getSelectedIndex(); 
            textpane.setText(helptexts.get(i)); 
            textpane.setCaretPosition(0); 
            textpane.grabFocus(); 
        }}); 

        add(hpanel); 
        setSize(200, 100); 
        pack(); 
	}
}


