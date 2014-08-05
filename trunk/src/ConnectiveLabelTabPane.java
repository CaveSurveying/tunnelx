////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2004  Julian Todd.
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

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.CardLayout;
import javax.swing.JCheckBox;
import java.awt.Font;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JScrollPane; 
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.SortedMap; 
import java.util.TreeMap; 
import java.util.Map; 
import java.util.List; 
import java.util.ArrayList; 


/////////////////////////////////////////////
class ConnectiveLabelTabPane extends JPanel
{
	JPanel labelstylepanel; 
	JComboBox fontstyles = new JComboBox();
	List<String> lfontstyles = new ArrayList<String>(); 
	
	JButton jbcancel = new JButton("Cancel Label");
	JTextArea labtextfield = new JTextArea("how goes\n    there");
	JScrollPane scrollpanetextfield = new JScrollPane(labtextfield); 

	JPanel labelpospanel; 
	JCheckBox jcbarrowpresent = new JCheckBox("Arrow");
	JCheckBox jcbboxpresent = new JCheckBox("Box");

	// the positioning of the text
	JTextField tfxrel = new JTextField();
	JTextField tfyrel = new JTextField();

	JPanel surveyfilterpanel; 
	JTextField tfsurveyfilterfile = new JTextField();
	JCheckBox jcbsurveyfilterfile = new JCheckBox("Transitive");
	
    CardLayout vcardlayouttoppanel = new CardLayout(); 
    JPanel toppanel; 
	
	// these codes could be expended later; they are numbered 0-4 clockwise from bottom left
	static String[] rppos = { "-1", "0", "1" };

	/////////////////////////////////////////////
	class surveyfontstyledetector implements ActionListener
	{
		public void actionPerformed(ActionEvent event) 
		{
			int lifontcode = fontstyles.getSelectedIndex();
			String lsfontcode = (lifontcode == -1 ? "default" : lfontstyles.get(lifontcode));
			vcardlayouttoppanel.show(toppanel, (lsfontcode.equals("survey") ? "surveyfilterpanel" : "labelpospanel")); 
		}
	};
	
	/////////////////////////////////////////////
	class radbut implements ActionListener
	{
		String xrel;
		String yrel;

		radbut(int ipos)
		{
			xrel = rppos[(ipos % 3)];
			yrel = rppos[2 - (ipos / 3)];
		}
		public void actionPerformed(ActionEvent e)
		{
			tfxrel.setText(xrel);
			tfyrel.setText(yrel);
			tfxrel.postActionEvent();
		}
	}

	ButtonGroup buttgroup = new ButtonGroup();
	JRadioButton[] rbposes = new JRadioButton[10];
	radbut[] radbuts = new radbut[9];
	surveyfontstyledetector sfsd = new surveyfontstyledetector(); 

	SortedMap<String, String> fontssortedmap = new TreeMap<String, String>(); 


	/////////////////////////////////////////////
	void ReloadLabelsCombo(SubsetAttrStyle sascurrent)
	{
		fontssortedmap.clear(); 
		for (SubsetAttr sa : sascurrent.msubsets.values())
		{
			for (LabelFontAttr lfa : sa.labelfontsmap.values())
			{
				if (!fontssortedmap.containsKey(lfa.labelfontname))
					fontssortedmap.put(lfa.labelfontname, String.format("%s (%s)", lfa.labelfontname, sa.subsetname)); 
			}
		}
		
		fontstyles.removeActionListener(sfsd); 
		fontstyles.removeAllItems(); 
		lfontstyles.clear(); 
		for (Map.Entry<String, String> foename : fontssortedmap.entrySet())
		{
			fontstyles.addItem(foename.getKey());  // the value has the subset in brackets, but subsets will in future be common to most styles
			//fontstyles.addItem(foename.getValue());

			lfontstyles.add(foename.getKey()); 
		}
		fontstyles.addActionListener(sfsd); 
	}


	/////////////////////////////////////////////
	void setTextPosCoords(float fxrel, float fyrel)
	{
		int ix = (fxrel == -1.0F ? 0 : (fxrel == 0.0F ? 1 : (fxrel == 1.0F ? 2 : -1)));
		int iy = (fyrel == -1.0F ? 2 : (fyrel == 0.0F ? 1 : (fyrel == 1.0F ? 0 : -1)));
		if ((ix != -1) && (iy != -1))
		{
			rbposes[ix + iy * 3].setSelected(true);
			tfxrel.setText(rppos[ix]);
			tfyrel.setText(rppos[2 - iy]);
		}
		else
		{
			rbposes[9].setSelected(true);
			tfxrel.setText(String.valueOf(fxrel));
			tfyrel.setText(String.valueOf(fyrel));
		}
	}




	/////////////////////////////////////////////
	ConnectiveLabelTabPane()
	{
		super(new BorderLayout());

		labelpospanel = new JPanel(new GridLayout(1, 3));

		JPanel fsp1 = new JPanel(new GridLayout(2, 1));
		fsp1.add(jcbarrowpresent);
		fsp1.add(jcbboxpresent);
		labelpospanel.add(fsp1);

		JPanel fsp3 = new JPanel(new GridLayout(3, 3));
		for (int i = 0; i < 10; i++)
		{
			rbposes[i] = new JRadioButton("");
			buttgroup.add(rbposes[i]);
			if (i != 9)
			{
				radbuts[i] = new radbut(i);
				rbposes[i].addActionListener(radbuts[i]);
				fsp3.add(rbposes[i]);
			}
		}
		labelpospanel.add(fsp3);

		JPanel fsp2 = new JPanel(new GridLayout(2, 1));
		fsp2.add(tfxrel);
		fsp2.add(tfyrel);
		labelpospanel.add(fsp2);

		
		surveyfilterpanel = new JPanel(new GridLayout(2, 1)); 
		JPanel sfpupper = new JPanel(new GridLayout(1, 3)); 
		sfpupper.add(new JLabel("File filter:")); 
		sfpupper.add(jcbsurveyfilterfile); 
		surveyfilterpanel.add(sfpupper); 
		surveyfilterpanel.add(tfsurveyfilterfile); 
		
		toppanel = new JPanel(vcardlayouttoppanel); 
		toppanel.add(labelpospanel, "labelpospanel"); 
		toppanel.add(surveyfilterpanel, "surveyfilterpanel"); 
		vcardlayouttoppanel.show(toppanel, "labelpospanel"); 

		labelstylepanel = new JPanel();
		labelstylepanel.add(fontstyles);
		labelstylepanel.add(jbcancel);

		add(toppanel, BorderLayout.NORTH);
		add(scrollpanetextfield, BorderLayout.CENTER);
		add(labelstylepanel, BorderLayout.SOUTH);
	}
};

