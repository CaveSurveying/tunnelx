package Tunnel;
// defunct

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


public class PrintScaleDialog implements DocumentListener
{
	double trueHeight; // decimeters...
	double trueWidth;

	final double imageableWidth;
	final double imageableHeight;

	JDialog layouterDialog;
	JPanel layouterPanel;
	JTextField scale;
	JLabel sizeLabel, sizeLabel2, pageSizeLabel, pageSizeLabel2, scaleLabel, pagesLabel, pagesLabel2;

	JCheckBox cutoutrectangle = new JCheckBox("Cutout rectangle", true);
	JCheckBox forceonepage = new JCheckBox("Centred one page", false);

	JCheckBox singlepageenabled = new JCheckBox("Single page", false);
    JLabel singlepagelabel = new JLabel("page nx : page ny");
	JTextField pagenx = new JTextField();
	JTextField pageny = new JTextField();


	JButton doCalculate;
	JButton fitPage;
	JButton allDone;

	// Constructor
	public PrintScaleDialog(JFrame frame, double w, double h, double pointsw, double pointsh)
	{
		trueHeight = h;
		trueWidth = w;

		imageableWidth = pointsw/72*0.254;
		imageableHeight = pointsh/72*0.254;
		// Create the Dialog and container.
		layouterDialog = new JDialog(frame, "Print scale and layout", true);
		layouterDialog.setSize(60, 80);
		layouterPanel = new JPanel();
		layouterPanel.setLayout(new GridLayout(0, 2));

		// Add the widgets.
		addWidgets();

		// Add the panel to the Dialog.
		layouterDialog.getContentPane().add(layouterPanel, BorderLayout.CENTER);

		// Exit when the window is closed.
		layouterDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		// Show the layouter.
		layouterDialog.pack();
		layouterDialog.setVisible(true);
	}

	// Create and add the widgets for layouter.
	private void addWidgets()
	{
		// Create widgets.
		sizeLabel = new JLabel("True size: ", SwingConstants.RIGHT);
		sizeLabel2 = new JLabel(Double.toString(Math.rint(trueWidth/10)) + " x " + Double.toString(Math.rint(trueHeight/10)) + "m",
			SwingConstants.LEFT);

		pageSizeLabel = new JLabel("Page image area: ", SwingConstants.RIGHT);
		pageSizeLabel2 = new JLabel(Double.toString(Math.rint(imageableWidth*1000)/100) + " x " + Double.toString(Math.rint(imageableHeight*1000)/100) + "cm",
			SwingConstants.LEFT);

		scale = new JTextField(4);
		scaleLabel = new JLabel("Scale 1 :", SwingConstants.RIGHT);
		//doCalculate = new JButton("Calculate...");
		pagesLabel = new JLabel("Pages:", SwingConstants.RIGHT);
		pagesLabel2 = new JLabel("", SwingConstants.LEFT);

		fitPage = new JButton("Fit to page");
		allDone = new JButton("OK");

		scale.getDocument().addDocumentListener(this);


		fitPage.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					int fscale = (int) Math.floor(Math.max(trueWidth/imageableWidth, trueHeight/imageableHeight)*1.01);
					scale.setText(Integer.toString(fscale));
				}
			});

		allDone.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					try
					{
						int tscale = Integer.parseInt(scale.getText());
						layouterDialog.setVisible(false);
					}
					catch(Exception e) {} // Don't close window if scale isn't a valid number
				}
			});

		layouterDialog.addWindowListener(
			new WindowListener()
			{
				public void windowClosing(WindowEvent e)
				{
        			scale.setText("-1");
        			layouterDialog.setVisible(false);
				}

				// the following are irrelevant to a modal dialog
				public void windowOpened(WindowEvent e) {}
				public void windowIconified(WindowEvent e) {}
				public void windowDeiconified(WindowEvent e) {}
				public void windowDeactivated(WindowEvent e) {}
				public void windowClosed(WindowEvent e) {}
				public void windowActivated(WindowEvent e) {}
			});


		// Add widgets to container.
		layouterPanel.add(sizeLabel);
		layouterPanel.add(sizeLabel2);
		layouterPanel.add(pageSizeLabel);
		layouterPanel.add(pageSizeLabel2);
		layouterPanel.add(scaleLabel);
		layouterPanel.add(scale);
		//layouterPanel.add(doCalculate);
		layouterPanel.add(pagesLabel);
		layouterPanel.add(pagesLabel2);
		layouterPanel.add(forceonepage);
		layouterPanel.add(cutoutrectangle);

		layouterPanel.add(singlepagelabel);
		layouterPanel.add(singlepageenabled);
		layouterPanel.add(pagenx);
		layouterPanel.add(pageny);


		layouterPanel.add(fitPage);
		layouterPanel.add(allDone);

		scaleLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		pagesLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		scale.setText(Integer.toString(TN.prtscale));

	}

	public void handleDocumentEvent(DocumentEvent event)
	{
		try
		{
			int tscale = Integer.parseInt(scale.getText());
			int pagesAcross = 1 + (int) Math.floor(trueWidth / tscale / imageableWidth);
			int pagesUp = 1 + (int) Math.floor(trueHeight / tscale / imageableHeight);

			pagesLabel2.setText(Integer.toString(pagesAcross*pagesUp) + " pages ("
				+ Integer.toString(pagesAcross) + "x" + Integer.toString(pagesUp) + ")");
		} catch(Exception e) {}
	}

	public void insertUpdate(DocumentEvent event)
	{
		handleDocumentEvent(event);
	}
	public void removeUpdate(DocumentEvent event)
	{
		handleDocumentEvent(event);
	}
	public void changedUpdate(DocumentEvent event)
	{
		handleDocumentEvent(event);
	}


	public int getScale() { return Integer.parseInt(scale.getText()); }


//	// main method
//	public static void main(String[] args)
//	{
//		try
//		{
//			UIManager.setLookAndFeel(
//			UIManager.getCrossPlatformLookAndFeelClassName());
//		}
//		catch(Exception e) {}
//
//		PrintLayouter layouter = new PrintLayouter(330,650);
//	}
}
