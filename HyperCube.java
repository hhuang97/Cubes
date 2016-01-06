import java.awt.*;
import java.lang.Math;

@SuppressWarnings("serial")
public class HyperCube extends java.applet.Applet {
	private HyperCubeFrame myFrame;
	private Container myParent;
	private HyperCubePnl pnlDraw;
	private Panel pnlCtl;

	private Label lblProj, lblSpeed;
	private Button btnProjMinus;
	private TextField txProj;
	private Button btnProjPlus, btnSpeedMinus;
	private TextField txSpeed;
	private Button btnSpeedPlus;

	private Button btnStartStop, btnDetach, btnStereo;
	private Dimension dimOrigSize;

	boolean bStandalone = false;

	private int objnum = 1;
	private int proj = 0;
	private int speed = 10;
	private int maxspeed = 100;
	private int stereo_opt = 0;

	double getProj() {
		return Math.sqrt(proj / 100.0);
	}

	double getSpeed() {
		return Math.sqrt(speed / 10.0);
	}

	int getStereoOpt() {
		return stereo_opt;
	}

	int getObjnum() {
		return objnum;
	}

	@SuppressWarnings("deprecation")
	private void putInFrame() {
		dimOrigSize = new Dimension(size());
		myParent = getParent();
		String strFrameName;
		switch (objnum) {
		case 2:
			strFrameName = "24-Cell";
			break;
		case 3:
			strFrameName = "Cross Polytope";
			break;
		case 4:
			strFrameName = "Simplex";
			break;
		default:
			strFrameName = "Hypercube";
			break;
		}
		myFrame = new HyperCubeFrame(this, strFrameName);
		Dimension siz = new Dimension(dimOrigSize);
		myFrame.resize(siz);
		myFrame.add("Center", this);
		myFrame.show();
		if (btnDetach != null)
			btnDetach.setLabel(" Attach ");
	}

	@SuppressWarnings("deprecation")
	void removeFromFrame() {
		myFrame.remove(this);
		myFrame.dispose();
		myFrame = null;
		myParent.add("Center", this);
		resize(dimOrigSize);
		myParent.show();
		if (btnDetach != null)
			btnDetach.setLabel(" Detach ");
	}

	static public void main(String args[]) {
		HyperCube hc = new HyperCube();

		int len = args.length;
		if (len >= 1)
			hc.parseObjParam(args[0]);
		if (len >= 2)
			hc.parseProjParam(args[1]);
		if (len >= 3)
			hc.parseSpeedParam(args[2]);
		hc.bStandalone = true;
		hc.resize(new Dimension(400, 458));
		hc.init();
		hc.putInFrame();
		hc.start();
	}

	private void parseObjParam(String paramString) {
		String objNames[] = { "cube", "24cell", "crosspoly", "simplex" };

		int m;
		for (m = 0; m < 4; m++)
			if (paramString.equalsIgnoreCase(objNames[m])) {
				objnum = m + 1;
				return;
			}
	}

	@SuppressWarnings("static-access")
	private void parseProjParam(String paramString) {
		Integer newproj = new Integer(0);
		int newp;
		newp = newproj.parseInt(paramString);
		if (newp < 0)
			newp = 0;
		if (newp > 95)
			newp = 95;
		proj = newp - (newp % 5);
	}

	@SuppressWarnings("static-access")
	private void parseSpeedParam(String paramString) {
		Integer newspeed = new Integer(0);
		int newsp;
		newsp = newspeed.parseInt(paramString);
		if (newsp < 1)
			newsp = 1;
		if (newsp > maxspeed)
			newsp = maxspeed;
		speed = newsp;
	}

	@SuppressWarnings("deprecation")
	public void init() {
		System.out.println(getAppletInfo());

		if (!bStandalone) {
			String paramString;
			paramString = getParameter("obj");
			if (paramString != null)
				parseObjParam(paramString);
			
			paramString = getParameter("projection");
			
			if (paramString != null)
				parseProjParam(paramString);
			
			paramString = getParameter("speed");
			
			if (paramString != null)
				parseSpeedParam(paramString);
		}

		setLayout(new BorderLayout());
		pnlDraw = new HyperCubePnl(this);
		add("Center", pnlDraw);
		pnlCtl = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		pnlCtl.setLayout(gridbag);
		lblProj = new Label("Projection:");
		lblSpeed = new Label("Speed:");
		btnProjMinus = new Button(" - ");
		txProj = new TextField("XXXX");
		txProj.setEditable(false);
		btnProjPlus = new Button(" + ");
		btnSpeedMinus = new Button(" - ");
		txSpeed = new TextField("XXXX");
		txSpeed.setEditable(false);
		btnSpeedPlus = new Button(" + ");
		btnStartStop = new Button(" Stop ");
		btnStereo = new Button(" Stereo ");
		c.insets = new Insets(5, 0, 0, 0);
		c.weightx = 0.1;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(lblProj, c);
		pnlCtl.add(lblProj);
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(lblSpeed, c);
		pnlCtl.add(lblSpeed);

		if (!bStandalone) {
			btnDetach = new Button(" Detach ");
			c.gridx = 7;
			c.weightx = 4.0;
			c.anchor = GridBagConstraints.EAST;
			gridbag.setConstraints(btnDetach, c);
			pnlCtl.add(btnDetach);
		}

		c.insets = new Insets(0, 0, 5, 0);
		c.gridy = 1;
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridwidth = 1;
		c.weightx = 0.4;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(btnProjMinus, c);
		pnlCtl.add(btnProjMinus);
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(txProj, c);
		pnlCtl.add(txProj);
		c.weightx = 3.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(btnProjPlus, c);
		pnlCtl.add(btnProjPlus);
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridwidth = 1;
		c.weightx = 0.4;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(btnSpeedMinus, c);
		pnlCtl.add(btnSpeedMinus);
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(txSpeed, c);
		pnlCtl.add(txSpeed);
		c.weightx = 3.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(btnSpeedPlus, c);
		pnlCtl.add(btnSpeedPlus);
		c.gridwidth = 1;
		c.weightx = 4.0;
		c.gridx = 6;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(btnStartStop, c);
		pnlCtl.add(btnStartStop);
		c.gridx = 7;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(btnStereo, c);
		pnlCtl.add(btnStereo);
		add("South", pnlCtl);
		setTxProj();
		setTxSpeed();
		pnlDraw.init();
		pnlDraw.show();
		start();
	}

	public void start() {
		btnStartStop.setLabel(" Stop ");
		pnlDraw.start();
	}

	public void stop() {
		btnStartStop.setLabel(" Start ");
		pnlDraw.stop();
	}

	@SuppressWarnings("static-access")
	public boolean action(Event evt, Object arg) {
		if (evt.id == evt.ACTION_EVENT) {
			int newproj = proj;

			if ((evt.target == btnProjMinus) && (newproj >= 5))
				newproj -= 5;
			if ((evt.target == btnProjPlus) && (newproj <= 90))
				newproj += 5;
			if (newproj != proj) {
				proj = newproj;
				setTxProj();
				pnlDraw.repaint();
				return true;
			}

			int newspeed = speed;
			if ((evt.target == btnSpeedMinus) && (newspeed > 1))
				newspeed--;
			if ((evt.target == btnSpeedPlus) && (newspeed < maxspeed))
				newspeed++;
			if (newspeed != speed) {
				speed = newspeed;
				setTxSpeed();
				return true;
			}

			if (evt.target == btnStartStop) {
				if (arg == " Stop ")
					stop();
				else if (arg == " Start ")
					start();
				return true;
			}

			if ((evt.target == btnDetach) && (btnDetach != null)) {
				if (myFrame == null)
					putInFrame();
				else
					removeFromFrame();
				return true;
			}

			if (evt.target == btnStereo) {
				stereo_opt++;
				if (stereo_opt > 2)
					stereo_opt = 0;
				pnlDraw.makecolors();
				pnlDraw.repaint();
			}
		}

		return false;
	}

	private void setTxProj() {
		Double dbl = new Double(proj / 100.0);
		txProj.setText(dbl.toString());
	}

	private void setTxSpeed() {
		Integer spd = new Integer(speed);
		txSpeed.setText(spd.toString());
	}

	public String getAppletInfo() {
		return "HyperCube applet";
	}
}
