package com.webcodepro.applecommander.ui.swing;

import java.awt.Color;
import java.util.Random;
import java.util.Vector;

import javax.swing.JPanel;

public class DiskExplorer extends JPanel {

	public DiskExplorer() {
		this.setBackground(randomColor());
	}

	private Color randomColor() {
		Color colors[] = {Color.black, Color.blue, Color.cyan,
			Color.gray, Color.darkGray, Color.green,
			Color.lightGray, Color.magenta, Color.orange,
			Color.pink,Color.red, Color.white, Color.yellow};
		return colors[(int)(Math.random() * colors.length)];
	}
	/**
	 * serialVersionUID, to keep Eclipse happy
	 */
	private static final long serialVersionUID = 4981722122357764174L;

}
