////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JFrame; 

import javax.swing.JMenu; 
import javax.swing.JMenuBar; 
import javax.swing.JMenuItem; 
import javax.swing.JCheckBoxMenuItem; 
import javax.swing.JToggleButton; 
import javax.swing.JPanel; 
import javax.swing.JCheckBox; 
import javax.swing.JButton; 
import javax.swing.JTextField; 
import javax.swing.JComboBox; 

import javax.swing.JSplitPane; 
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 

import javax.swing.Icon; 
import java.awt.Color; 
import java.awt.Dimension; 

import java.awt.Component; 

import java.awt.Graphics; 
import java.awt.Graphics2D; 
import java.awt.BorderLayout; 
import java.awt.GridLayout; 
import javax.swing.BoxLayout; 

import java.util.Vector; 
import java.awt.FileDialog;

import java.awt.Graphics2D; 
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon; 
import javax.swing.Icon; 
import java.awt.Image; 

import java.io.IOException; 
import java.io.File;

import java.awt.geom.AffineTransform; 
import java.awt.geom.Rectangle2D; 

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener; 
import java.awt.event.WindowEvent; 
import java.awt.event.WindowAdapter; 

import javax.swing.JButton; 
import javax.swing.JToggleButton; 

import javax.swing.event.DocumentListener; 
import javax.swing.event.DocumentEvent; 

//
//
// SymbolsPanel
//
//




// 
class SymbolsPanel extends JPanel implements ActionListener
{
	OneTunnel vgsymbols; 
	SketchDisplay sketchdisplay; 
	Dimension prefsize = new Dimension(100, 100); 
	JPanel iconpanel = new JPanel(new GridLayout(0, 1)); 


	// this points to the symbols panel in the other window 
	// (so when it's not null this is the editable version).  
	SymbolsPanel mbxsymbolspanel = null; 

	// panel of buttons which manage the symbols
	JPanel symbmanage; 

	JToggleButton tbeditsymbol; 
	int seledititem = -1; 
	boolean bEditMode = false; // matches tbeditsymbol but is more explicit (in case of slow response).  
	JTextField tfnewsymbolname; 
	JButton jbnewsymbol; 
	JButton jbsetasaxis; 
	JButton jbdeletesymbol; 
	JButton jbshufflesymbolup; 

	// used for making the blank button image.  
	BufferedImage biblanksymbol = null; 
	ImageIcon iblankicon = null; 


	/////////////////////////////////////////////
	SymbolsPanel(OneTunnel lvgsymbols, SketchDisplay lsketchdisplay, SymbolsPanel lmbxsymbolspanel) 
	{
		super(new BorderLayout()); 
		add("Center", iconpanel); 

		vgsymbols = lvgsymbols; 
		sketchdisplay = lsketchdisplay; 
		mbxsymbolspanel = lmbxsymbolspanel; 

		if (mbxsymbolspanel != null) 
		{
			tbeditsymbol = new JToggleButton("Edit mode"); 
			tbeditsymbol.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { SetSymbolEditMode(tbeditsymbol.isSelected()); } } ); 

			jbnewsymbol = new JButton("New Symbol"); 
			tfnewsymbolname = new JTextField(); 
			jbnewsymbol.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { NewGSymbol(); } } ); 

			jbsetasaxis = new JButton("Set As Axis"); 
			jbsetasaxis.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { sketchdisplay.sketchgraphicspanel.SetAsAxis(); } } ); 

			jbdeletesymbol = new JButton("Delete Symbol"); 
			jbdeletesymbol.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { DeleteGSymbol(); } } ); 

			jbshufflesymbolup = new JButton("Shuffle Up"); 
			jbshufflesymbolup.addActionListener(new ActionListener() 
				{ public void actionPerformed(ActionEvent event) { ShuffleUpGSymbol(); } } ); 


			symbmanage = new JPanel(new GridLayout(0, 1)); 
			symbmanage.add(tbeditsymbol); 
			symbmanage.add(tfnewsymbolname); 
			symbmanage.add(jbnewsymbol); 
			symbmanage.add(jbsetasaxis); 
			symbmanage.add(jbdeletesymbol);
			symbmanage.add(jbshufflesymbolup);

			// make the blank icon symbol  
			biblanksymbol = new BufferedImage(prefsize.width, prefsize.height, BufferedImage.TYPE_INT_ARGB); 
			Graphics2D g2d = biblanksymbol.createGraphics(); 
			g2d.setColor(Color.gray); 
			g2d.fillRect(0, 0, prefsize.width, prefsize.height); 
			g2d.setColor(Color.black); 
			g2d.drawLine(0, 0, prefsize.width, prefsize.height); 
			g2d.drawLine(0, prefsize.height, prefsize.width, 0); 
			iblankicon = new ImageIcon(biblanksymbol); 

			add("South", symbmanage); 
		}
		else 
			symbmanage = null; 
	}
	
	/////////////////////////////////////////////
	void SetSymbolEditMode(boolean lbEditMode) 
	{
		bEditMode = lbEditMode; 
System.out.println("edit mode " + bEditMode); 

		if (seledititem == -1) 
			return; 

		JButton symbolbutton = (JButton)iconpanel.getComponent(seledititem); 

		// restore 
		if (!bEditMode)  
		{
			sketchdisplay.sketchgraphicspanel.ClearSelection(); 
			sketchdisplay.sketchgraphicspanel.bEditable = false; 
			vgsymbols.downtunnels[seledititem].tsketch.iicon = null; // assumes a change.  
			vgsymbols.downtunnels[seledititem].tsketch.changeincvzpS++; // causes all the vizpaths to redraw.  

			symbolbutton.setIcon(vgsymbols.downtunnels[seledititem].tsketch.GetIcon(prefsize, vgsymbols)); 
		}

		// put in a blank.  
		else 
		{
			sketchdisplay.sketchgraphicspanel.bEditable = true; 
			symbolbutton.setIcon((Icon)iblankicon); 
		}

		symbolbutton.repaint(); 
	}


	/////////////////////////////////////////////
	void InsertSymbol(int index)
	{
		// make the auto area for this symbol
		vgsymbols.downtunnels[index].tsketch.MakeAutoAreas(); 		
		vgsymbols.downtunnels[index].tsketch.PutSymbolsToAutoAreas(true); 

		Icon licon = vgsymbols.downtunnels[index].tsketch.GetIcon(prefsize, vgsymbols); 
		String lname = vgsymbols.downtunnels[index].name; 

		JButton symbolbutton = new JButton(); 
		symbolbutton.setPreferredSize(prefsize); 
		symbolbutton.setIcon(licon); 
		symbolbutton.setText(lname); 
		symbolbutton.addActionListener(this); 

		iconpanel.add(symbolbutton, index); 

		// add into the mbxsymbolspanel.  
		JButton mbxsymbolbutton = new JButton(); 
		mbxsymbolbutton.setPreferredSize(prefsize); 
		mbxsymbolbutton.setIcon(licon); 
		mbxsymbolbutton.setText(lname); 
		mbxsymbolbutton.addActionListener(mbxsymbolspanel); 

		mbxsymbolspanel.iconpanel.add(mbxsymbolbutton, index); 
	}

	/////////////////////////////////////////////
	public void actionPerformed(ActionEvent e) 
	{
		Component symbolbutton = (Component)e.getSource(); 
		
		// find component in the list (not very satisfying method).  
		for (int i = 0; i < vgsymbols.ndowntunnels; i++) 
			if (iconpanel.getComponent(i) == symbolbutton) 
			{
				SelectSymbol(i); 
				break; 
			}
	}

	/////////////////////////////////////////////
	void SelectSymbol(int selitem)  
	{
		if ((mbxsymbolspanel == null) || bEditMode)  
			sketchdisplay.sketchgraphicspanel.SpecSymbol(selitem); 

		// select picture.  
		else 
		{
			seledititem = selitem; 

			sketchdisplay.sketchgraphicspanel.ClearSelection();  

			if (selitem == -1) 
				return; // set to blank.  

			OneSketch gsymbol = vgsymbols.downtunnels[selitem].tsketch; 
			System.out.println(vgsymbols.downtunnels[selitem].name); 

			sketchdisplay.sketchgraphicspanel.tsketch = gsymbol; 

			sketchdisplay.sketchgraphicspanel.bmainImgValid = false; 
			sketchdisplay.sketchgraphicspanel.repaint(); 
		}
	}


	/////////////////////////////////////////////
	void SaveSymbols() 
	{
		SvxFileDialog sfiledialog = SvxFileDialog.showSaveDialog(TN.currentSymbols, sketchdisplay, SvxFileDialog.FT_SYMBOLS); 
		if ((sfiledialog == null) || (sfiledialog.svxfile == null))
			return; 

		TN.currentSymbols = sfiledialog.getSelectedFile(); 

		System.out.println("Saving symbols " + sfiledialog.svxfile.getName()); 
		new SurvexSaver(vgsymbols, sfiledialog.svxfile, false, false, true); 
	}

	/////////////////////////////////////////////
	void LoadSymbols() 
	{
		// could use the auto parameter here.  
		if (TN.currentSymbols != null) 
		{
			SvxFileDialog sfiledialog = SvxFileDialog.showOpenDialog(TN.currentSymbols, sketchdisplay, SvxFileDialog.FT_SYMBOLS, false); 
			if ((sfiledialog == null) || (sfiledialog.svxfile == null)) 
				return; 
			
			TN.currentSymbols = sfiledialog.getSelectedFile(); 
		}

		// auto-load
		else 
			TN.currentSymbols = new File(System.getProperty("user.dir"), "Symbols.tun"); 


		System.out.println("Loading symbols " + TN.currentSymbols.getName()); 
		new SurvexLoader(TN.currentSymbols, vgsymbols, false, false, true); 


		// clear and initialize.  
		iconpanel.removeAll();  
		mbxsymbolspanel.iconpanel.removeAll(); 

		// insert all the symbols  
		for (int i = 0; i < vgsymbols.ndowntunnels; i++) 
			InsertSymbol(i); 

		mbxsymbolspanel.validate(); 
		validate(); 
		repaint(); 
	}


	/////////////////////////////////////////////
	void NewGSymbol() 
	{
		if (bEditMode) 
			return; 

		String symbname = tfnewsymbolname.getText().trim(); 
		symbname = symbname.replace(' ', '_'); 
		tfnewsymbolname.setText(symbname); 
		
		// check validity 
	    if (symbname.length() == 0) 
			return;  

		OneTunnel tunn = vgsymbols.IntroduceSubTunnel(symbname, vgsymbols.InitialLegLineFormat, false); 

		// actually a new symbol.  
		if (tunn.tsketch == null)
		{
			vgsymbols.RebuildSymbolText(-1);  

			tunn.tsketch = new OneSketch(); 

			// add to the combobox in the other window.  
			InsertSymbol(vgsymbols.ndowntunnels - 1); 

			SelectSymbol(vgsymbols.ndowntunnels - 1);  

			validate(); 
			repaint(); 
		}
	}


	/////////////////////////////////////////////
	void DeleteGSymbol() 
	{
		if (!bEditMode) 
			return; 
		SetSymbolEditMode(false);  

		vgsymbols.RebuildSymbolText(seledititem);  

		iconpanel.remove(seledititem); 
		mbxsymbolspanel.iconpanel.remove(seledititem); 
		seledititem = -1; 
		tbeditsymbol.setSelected(false); 

		mbxsymbolspanel.validate(); 
		validate(); 
		repaint(); 
	}

	/////////////////////////////////////////////
	void ShuffleUpGSymbol() 
	{
		if (!bEditMode || (seledititem == 0)) 
			return; 

		// shuffle the tunnels  
		OneTunnel ots = vgsymbols.downtunnels[seledititem]; 
		vgsymbols.downtunnels[seledititem] = vgsymbols.downtunnels[seledititem - 1]; 
		vgsymbols.downtunnels[seledititem - 1] = ots;  

		// shuffle the components
		Component scomp = iconpanel.getComponent(seledititem); 
		iconpanel.remove(seledititem); 
		iconpanel.add(scomp, seledititem - 1); 

		Component mbxscomp = mbxsymbolspanel.iconpanel.getComponent(seledititem); 
		mbxsymbolspanel.iconpanel.remove(seledititem); 
		mbxsymbolspanel.iconpanel.add(mbxscomp, seledititem - 1); 

		seledititem--; 

		vgsymbols.RebuildSymbolText(-1);  

		mbxsymbolspanel.validate(); 
		validate(); 
		repaint(); 
	}
};


