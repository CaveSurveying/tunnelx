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

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.JTextField;

import java.awt.Insets;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.awt.BasicStroke;
import java.awt.Color;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

//
//
// SketchLineStyle
//
//

/////////////////////////////////////////////
class SketchLineStyle extends JPanel
{
	// parallel arrays of wall style info.
	static String[] linestylenames = { "Centreline", "Wall", "Est. Wall", "Pitch Bound", "Ceiling Bound", "Detail", "Invisible", "Connective", "Filled" };
	static final int SLS_CENTRELINE = 0;

	static final int SLS_WALL = 1;
	static final int SLS_ESTWALL = 2;

	static final int SLS_PITCHBOUND = 3;
	static final int SLS_CEILINGBOUND = 4;

	static final int SLS_DETAIL = 5;
	static final int SLS_INVISIBLE = 6;
	static final int SLS_CONNECTIVE = 7;
	static final int SLS_FILLED = 8;

	static final int SLS_SYMBOLOUTLINE = 9; // not a selected style.

	static float strokew;
	static Color[] linestylecols = new Color[10];
	static BasicStroke[] linestylestrokes = new BasicStroke[10];

	static Color linestylecolactive = Color.magenta;
	static Color linestylecolprint= Color.black;
	static Color linestylegreyed = Color.lightGray;
	static BasicStroke linestylegreystrokes = null;
	static BasicStroke linestyleprintcutout = null;
	static Color linestyleprintgreyed = Color.darkGray;

	static String[] linestylebuttonnames = { "", "W", "E", "P", "C", "D", "I", "N", "F" };
	static int[] linestylekeystrokes = { 0, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_P, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_I, KeyEvent.VK_N, KeyEvent.VK_F };

	static float pitchbound_flatness;
	static float pitchbound_spikegap;
	static float pitchbound_spikeheight;
	static float ceilingbound_gapleng;

	// (we must prevent the centreline style from being selected --  it's special).
	JComboBox linestylesel = new JComboBox(linestylenames);
	JToggleButton pthsplined = new JToggleButton("s");
	JTextField pthlabel = new JTextField();


	// secondary sets of colours which over-ride using the icolindex attribute in lines
	static Color[] linestylecolsindex = new Color[100];
	static Color[] areastylecolsindex = new Color[100];



	/////////////////////////////////////////////
	public class AclsButt extends AbstractAction
	{
		int index;
	    public AclsButt(int lindex)
		{
			super(linestylebuttonnames[lindex]);
			index = lindex;
            putValue(SHORT_DESCRIPTION, linestylenames[index]);
            putValue(MNEMONIC_KEY, new Integer(linestylekeystrokes[index]));
		}

	    public void actionPerformed(ActionEvent e)
		{
			linestylesel.setSelectedIndex(index);
		}
	}

	/////////////////////////////////////////////
	class LineStyleButton extends JButton
	{
		int index;

		LineStyleButton(int lindex)
		{
			super(new AclsButt(lindex));
			index = lindex;
			setMargin(new Insets(2, 2, 2, 2));
		}
	};



	/////////////////////////////////////////////
	static void SetStrokeWidths(float lstrokew)
	{
		strokew = lstrokew;
		float[] dash = new float[2];
		float[] dasht = new float[2];

		pitchbound_flatness = strokew / 2;
		ceilingbound_gapleng = strokew * 4;
		pitchbound_spikegap = strokew * 12;
		pitchbound_spikeheight = strokew * 4;

		// centreline
		linestylestrokes[0] = new BasicStroke(0.5F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[0] = Color.red;

		// wall
		linestylestrokes[1] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[1] = Color.blue;

		// estimated wall
		dash[0] = 6 * strokew;
		dash[1] = 6 * strokew;
		linestylestrokes[2] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 3.0F * strokew);
		linestylecols[2] = Color.blue;

		// pitch boundary
		dasht[0] = 10 * strokew;
		dasht[1] = 6 * strokew;
		linestylestrokes[3] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dasht, 5.0F * strokew);
		linestylecols[3] = new Color(0.7F, 0.0F, 1.0F);

		// ceiling boundary
		linestylestrokes[4] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dasht, 1.7F * strokew);
		linestylecols[4] = Color.cyan;

		// detail
		linestylestrokes[5] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[5] = Color.blue;

		// invisible
		linestylestrokes[6] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[6] = new Color(0.0F, 0.9F, 0.0F);

		// connective
		dasht[0] = 3 * strokew;
		dasht[1] = 3 * strokew;
		linestylestrokes[7] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dasht, 1.5F * strokew);
		linestylecols[7] = new Color(0.5F, 0.8F, 0.0F);

		// filled
		linestylestrokes[8] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[8] = Color.black;

		// symbol paint background.
		linestylestrokes[9] = new BasicStroke(3.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew);
		linestylecols[9] = Color.white; // for printing.


		// greyed out stuff
		dash[0] = 4 * strokew;
		dash[1] = 6 * strokew;
		linestylegreystrokes = new BasicStroke(1.2F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 2.4F * strokew);

		// the cutting out when printing in tiles
		// this should be in points, not in the local size
		dash[0] = 6 * 2;
		dash[1] = 4 * 2;
		linestyleprintcutout = new BasicStroke(1.2F * 1.1F, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 5.0F * 1.1F, dash, 2.4F * 1.1F);
	}



	/////////////////////////////////////////////
	SketchLineStyle()
	{
		// do the button panel
		JPanel buttpanel = new JPanel();
		buttpanel.setLayout(new BoxLayout(buttpanel, BoxLayout.X_AXIS));
		for (int i = 0; i < linestylebuttonnames.length; i++)
		{
			if (!linestylebuttonnames[i].equals(""))
				buttpanel.add(new LineStyleButton(i));
		}
		pthsplined.setMargin(new Insets(3, 3, 3, 3));
		buttpanel.add(pthsplined);

		linestylesel.setSelectedIndex(SLS_DETAIL);

		// do the layout of the main thing.
		linestylesel.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(linestylesel);
		add(buttpanel);
		//add(pthsplined);
		add(pthlabel);


		// fill in the colour rainbow for showing weighting and depth
		for (int i = 0; i < linestylecolsindex.length; i++)
		{
			float a = (float)i / linestylecolsindex.length ;
			//linestylecolsindex[i] = new Color(Color.HSBtoRGB(0.9F * a, 1.0F, 0.9F));
			linestylecolsindex[i] = new Color(a, (1.0F - a) * 0.2F, 1.0F - a);
		}

		for (int i = 0; i < areastylecolsindex.length; i++)
		{
			float a = (float)i / linestylecolsindex.length ;
			//linestylecolsindex[i] = new Color();
			// fcolw = new Color(0.8F, 1.0F, 1.0F, 0.6F);
			//areastylecolsindex[i] = new Color(0.7F + a * 0.3F, 1.0F - a * 0.3F, 1.0F, 0.6F);
			int col = Color.HSBtoRGB(0.6F * (1.0F - a) + 0.02F, 1.0F, 1.0F) + 0x61000000;
			areastylecolsindex[i] = new Color(col, true);
			//linestylecolsindex[i] = new Color(Color.HSBtoRGB(0.9F * a, 1.0F, 0.9F));
		}
	}
};

