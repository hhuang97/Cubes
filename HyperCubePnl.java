import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.util.Date;
import java.util.Random;


@SuppressWarnings("serial")
public class HyperCubePnl extends Panel implements Runnable {
	private Image offscreenImg;
	private Graphics offscreenG;
	private Dimension offscreensize;
	private Thread runner;
	private Random rand = new Random();
	private final double velmax = .03;
	private final double velinc = .006;
	private final int delay = 50;
	private double vertices[][];
	private int vert2xR[];
	private int vert2xL[];
	private int vert2y[];
	private byte edges[][];
	private double vel[][] = new double[4][4];
	private double m1[][] = new double[4][4];
	private double m2[][] = new double[4][4];
	private double rot4[][] = new double[4][4];
	private double ROT4[][] = new double[4][4];
	private double ROT4A[][] = new double[4][4];
	private double newROT[][] = new double[4][4];
	private double ROTM[][] = new double[4][4];
	private double holdROT[][];
	private double rotvert[] = new double[4];
	private double vec1[] = new double[3];
	private double vec2[] = new double[3];
	private double vec3[] = new double[3];
	private double R4;
	private int dx, dy;
	private int dx_offset;
	private int xbase, ybase;
	private double fac, dfac, deps, deltar, vpfR, R3, epsfac;
	private boolean bLeftFirst;
	private boolean bTracking = false;
	private boolean bShiftDown;
	private int mouseX, mouseY;
	private Color leftColor, rightColor, backgColor;
	private HyperCube owner;

	HyperCubePnl(HyperCube own){
		owner = own;
	}

	private void defineCube() {
		int i, j, k, dif, ct;
		vertices = new double[16][4];
		edges = new byte[32][2];
		for (i = 0; i < 16; i++) {
			for (j = 0; j < 4; j++)
				vertices[i][j] = ((i >> (3 - j)) & 1) - 0.5;
		}
		k = 0;
		for (i = 0; i < 15; i++) {
			for (j = i + 1; j < 16; j++) {
				ct = 0;
				for (dif = i ^ j; dif != 0; dif >>= 1)
					if ((dif & 1) != 0)
						ct++;
				if (ct == 1) {
					edges[k][0] = (byte) i;
					edges[k][1] = (byte) j;
					k++;
				}
			}
		}
	}

	private void define24Cell() {
		byte bitss[] = new byte[24];
		byte masks[] = new byte[24];
		int mask, bits;
		int i, j, k, m, n, d, e;
		vertices = new double[24][4];
		edges = new byte[96][2];
		i = 0;
		for (m = 0; m < 4; m++) {
			for (n = 0; n < m; n++) {
				mask = (1 << m) | (1 << n);
				for (j = 0; j < 2; j++) {
					for (k = 0; k < 2; k++) {
						bits = (j << m) | (k << n);
						masks[i] = (byte) mask;
						bitss[i] = (byte) bits;
						for (d = 0; d < 4; d++) {
							vertices[i][d] = ((mask >> d) & 1) != 0 ? 2 * ((bits >> d) & 1) - 1
									: 0;
						}
						i++;
					}
				}
			}
		}
		e = 0;
		for (m = 0; m < 4; m++) {
			mask = (1 << m);
			for (n = 0; n < 2; n++) {
				bits = (n << m);
				for (j = 0; j < 24; j++) {
					if ((mask & masks[j]) == 0)
						continue;

					if ((bits & mask) != (bitss[j] & mask))
						continue;

					for (k = 0; k < j; k++) {
						if ((mask & masks[k]) == 0)
							continue;

						if ((bits & mask) != (bitss[k] & mask))
							continue;

						if (masks[j] == masks[k])
							continue;

						edges[e][0] = (byte) j;
						edges[e][1] = (byte) k;
						e++;
					}
				}
			}
		}
	}

	private void defineCrossPoly() {
		vertices = new double[8][4];

		byte edges[][] = {
		{ 0, 2 }, { 0, 3 }, { 1, 3 }, { 1, 2 }, { 0, 4 }, { 1, 4 }, { 2, 4 },
				{ 3, 4 }, { 0, 5 }, { 1, 5 }, { 2, 5 }, { 3, 5 }, { 0, 6 },
				{ 1, 6 }, { 2, 6 }, { 3, 6 }, { 4, 6 }, { 5, 6 }, { 0, 7 },
				{ 1, 7 }, { 2, 7 }, { 3, 7 }, { 4, 7 }, { 5, 7 } };

		int i, j, k;
		j = k = 0;
		for (i = 0; i < 4; i++) {
			vertices[j++][k] = -1;
			vertices[j++][k] = 1;
			k++;
		}

		this.edges = edges;
	}

	private void defineSimplex() {
		vertices = new double[5][4];

		byte edges[][] = {
		{ 0, 1 }, { 0, 2 }, { 0, 3 }, { 0, 4 }, { 1, 2 }, { 1, 3 }, { 1, 4 },
				{ 2, 3 }, { 2, 4 }, { 3, 4 }, };

		int i, j, k;
		double dist, sumsq, avg;
		for (i = 1; i < 5; i++) {
			sumsq = 0.0;
			for (j = 0; j < i; j++) {
				avg = 0.0;
				for (k = 0; k < i; k++)
					avg += vertices[k][j];
				avg /= i;
				dist = (vertices[0][j] - avg);
				sumsq += (dist * dist);
				vertices[i][j] = avg;
			}
			vertices[i][i - 1] = Math.sqrt(1.0 - sumsq);
		}
		centerTheObject();
		this.edges = edges;
	}

	private void centerTheObject() {
		int i, j;
		int len = vertices.length;
		double avg;
		for (i = 0; i < 4; i++) {
			avg = 0.0;
			for (j = 0; j < len; j++)
				avg += vertices[j][i];
			avg /= len;
			for (j = 0; j < len; j++)
				vertices[j][i] -= avg;
		}
	}

	void makecolors() {
		int redval;
		int blueval;
		int backgval;

		switch (owner.getStereoOpt()) {
		case 0:
			redval = 188;
			blueval = 255;
			backgval = 84;
			leftColor = new Color(redval, backgval, 0);
			rightColor = new Color(0, backgval, blueval);
			backgColor = new Color(0, backgval, 0);
			break;
		case 1:
			leftColor = new Color(0, 239, 0);
			rightColor = new Color(255, 0, 0);
			backgColor = new Color(222, 222, 222);
			break;
		case 2:
			backgval = 198;
			leftColor = new Color(0, 0, 0);
			rightColor = new Color(0, 0, 0);
			backgColor = new Color(backgval, backgval, backgval);
			break;
		}
	}

	public void init() {
		makecolors();
		Date date = new Date();
		rand.setSeed(date.getTime());
		switch (owner.getObjnum()) {
		case 1:
			defineCube();
			break;
		case 2:
			define24Cell();
			break;
		case 3:
			defineCrossPoly();
			break;
		case 4:
			defineSimplex();
			break;
		default:
			defineCube();
			break;
		}

		double sum = 0.0;
		int k;
		for (k = 0; k < 4; k++)
			sum += vertices[0][k] * vertices[0][k];
		R4 = Math.sqrt(sum);
		k = vertices.length;
		vert2xR = new int[k];
		vert2xL = new int[k];
		vert2y = new int[k];

		for (k = 0; k < 4; k++)
			ROT4[k][k] = 1.0;
		for (k = 0; k < 4; k++)
			ROTM[k][k] = 1.0;
	}

	public void run() {
		setBackground(backgColor);
		while (true) {
			rotate();
			repaint();
			try {
				Thread.sleep((int) (delay / owner.getSpeed()));
			} catch (InterruptedException e) {
			}
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void start() {
		if (runner == null) {
			runner = new Thread(this);
			runner.start();
		}
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		if (runner != null) {
			runner.stop();
			runner = null;
		}
	}

	public boolean mouseDown(Event evt, int x, int y) {
		if (!bTracking) {
			owner.stop();
			bShiftDown = evt.shiftDown();
			bTracking = true;
			mouseX = x;
			mouseY = y;
			repaint();
		}

		return true;
	}

	private double normalize3Vec(double vec[]) {
		int m;
		double len, sq;

		len = 0.0;
		for (m = 0; m < 3; m++) {
			sq = vec[m] * vec[m];
			len += sq;
		}
		len = Math.sqrt(len);
		for (m = 0; m < 3; m++)
			vec[m] /= len;
		return len;
	}

	public boolean mouseDrag(Event evt, int x, int y) {
		if ((mouseX != x) || (mouseY != y)) {

			double s, c;
			int i, j, k;
			vec1[0] = mouseX - xbase;
			vec1[1] = mouseY - ybase;
			vec1[2] = -epsfac;
			vec2[0] = x - xbase;
			vec2[1] = y - ybase;
			vec2[2] = -epsfac;

			normalize3Vec(vec1);
			normalize3Vec(vec2);
			c = 0.0;
			
			for (k = 0; k < 3; k++)
				c += vec1[k] * vec2[k];
			
			vec3[0] = vec1[1] * vec2[2] - vec1[2] * vec2[1];
			vec3[1] = vec1[2] * vec2[0] - vec1[0] * vec2[2];
			vec3[2] = vec1[0] * vec2[1] - vec1[1] * vec2[0];

			s = normalize3Vec(vec3);
			for (k = 0; k < 3; k++)
				vec2[k] -= c * vec1[k];
			normalize3Vec(vec2);
			
			for (i = 0; i < 3; i++) {
				for (j = 0; j < 3; j++) {
					rot4[i][j] = vec3[i] * vec3[j] + c
							* (vec1[i] * vec1[j] + vec2[i] * vec2[j]) + s
							* (vec2[i] * vec1[j] - vec1[i] * vec2[j]);
				}
			}
			for (k = 0; k < 3; k++)
				rot4[3][k] = rot4[k][3] = 0.0;
			rot4[3][3] = 1.0;

			if (bShiftDown) {
				for (k = 0; k < 4; k++) {
					c = rot4[2][k];
					rot4[2][k] = rot4[3][k];
					rot4[3][k] = c;
				}
				for (k = 0; k < 4; k++) {
					c = rot4[k][2];
					rot4[k][2] = rot4[k][3];
					rot4[k][3] = c;
				}
			}

			for (i = 0; i < 4; i++) {
				for (j = 0; j < 4; j++) {
					newROT[i][j] = 0;
					for (k = 0; k < 4; k++)
						newROT[i][j] += rot4[i][k] * ROTM[k][j];
				}
			}
			holdROT = ROTM;
			ROTM = newROT;
			newROT = holdROT;
		}

		mouseX = x;
		mouseY = y;
		repaint();
		return true;
	}

	public boolean mouseUp(Event evt, int x, int y) {
		if (bTracking) {
			bTracking = false;
			repaint();
		}
		return true;
	}

	private void rotate() {
		double angl, sinangl, cosangl, d, dsq, max, veli, vmax;
		int i, j, k, abi, abj;

		max = 0.0;
		abi = 1;
		abj = 2;
		veli = velinc * owner.getSpeed();
		vmax = velmax * owner.getSpeed();

		for (i = 0; i < 3; i++) {
			for (j = i + 1; j < 4; j++) {
				d = vel[i][j] + veli * (rand.nextDouble() - 0.5);
				vel[i][j] = d;
				vel[j][i] = -d;
				dsq = d * d;
				if (dsq > max) {
					max = dsq;
					abi = i;
					abj = j;
				}
			}
		}

		if (max < 1.0E-10)
			return;
		
		d = vel[0][3] * vel[1][2] - vel[0][2] * vel[1][3] + vel[0][1]
				* vel[2][3];

		switch (abi * 10 + abj) {
		case 1:
			i = 2;
			j = 3;
			break;
		case 2:
			i = 3;
			j = 1;
			break;
		case 3:
			i = 1;
			j = 2;
			break;
		case 12:
			i = 0;
			j = 3;
			break;
		case 13:
			i = 2;
			j = 0;
			break;
		case 23:
			i = 0;
			j = 1;
			break;
		default:
			i = 0;
			j = 1;
			break;
		}

		vel[i][j] -= d / vel[abi][abj];
		vel[j][i] = -vel[i][j];
		angl = 0;
		for (i = 0; i < 3; i++) {
			for (j = i + 1; j < 4; j++)
				angl += vel[i][j] * vel[i][j];
		}
		angl = Math.sqrt(angl);
		if (angl < 1.0E-5)
			return;
		
		if (angl > vmax) {
			d = vmax / angl;
			angl = vmax;
			for (i = 0; i < 3; i++) {
				for (j = i + 1; j < 4; j++) {
					vel[i][j] *= d;
					vel[j][i] = -vel[i][j];
				}
			}
		}

		for (i = 0; i < 4; i++)
			m1[i][i] = 0;
		for (i = 0; i < 3; i++) {
			for (j = i + 1; j < 4; j++) {
				m1[i][j] = vel[i][j] / angl;
				m1[j][i] = -m1[i][j];
			}
		}

		for (i = 0; i < 4; i++) {
			for (j = i; j < 4; j++) {
				m2[i][j] = 0.0;
				for (k = 0; k < 4; k++)
					m2[i][j] += (m1[i][k] * m1[k][j]);
				m2[j][i] = m2[i][j];
			}
		}

		cosangl = 1.0 - Math.cos(angl);
		sinangl = Math.sin(angl);
		for (i = 0; i < 4; i++) {
			for (j = 0; j < 4; j++)
				rot4[i][j] = sinangl * m1[i][j] + cosangl * m2[i][j];
		}
		for (i = 0; i < 4; i++)
			rot4[i][i] += 1.0;

		for (i = 0; i < 4; i++) {
			for (j = 0; j < 4; j++) {
				newROT[i][j] = 0.0;
				for (k = 0; k < 4; k++)
					newROT[i][j] += rot4[i][k] * ROT4[k][j];
			}
		}
		holdROT = ROT4;
		ROT4 = newROT;
		newROT = holdROT;
	}

	private void calcProjParms() {
		double d = 8.0;
		double eps = 1.5;
		double clip = 0.95;
		double q, sx, sy, vpf;
		double delta = (owner.getStereoOpt() == 2) ? 0.5 : 0.3;
		vpf = owner.getProj();
		R3 = R4 / Math.sqrt(1 - vpf * vpf);
		deps = d - eps;
		q = Math.sqrt(deps * deps + delta * delta);
		sx = 2.0 * (d * Math.tan(Math.asin(1 / q) + Math.atan(delta / deps)) - delta);
		if (owner.getStereoOpt() == 2)
			sx *= 2;

		q = Math.sqrt(deps * deps + delta * delta);
		sy = 2.0 * d * Math.tan(Math.asin(1 / deps));

		if (dx * sy < dy * sx)
			fac = dx / sx;
		else
			fac = dy / sy;

		fac *= clip;
		epsfac = eps * fac;
		dx_offset = 0;
		
		if (owner.getStereoOpt() == 2)
			dx_offset = -(int) (fac * sx / 4);

		fac /= R3;
		deltar = delta * R3;
		vpfR = vpf / R4;
		dfac = d * fac;
	}

	public void paint(Graphics g) {
		@SuppressWarnings("deprecation")
		Dimension dim = size();

		dx = dim.width;
		dy = dim.height;

		if ((dx < 1) || (dy < 1))
			return;

		xbase = dx / 2;
		ybase = dy / 2;

		if ((offscreenImg == null) || (dx != offscreensize.width) || (dy != offscreensize.height)) {
			offscreenImg = createImage(dx, dy);
			offscreensize = new Dimension(dim);
			offscreenG = offscreenImg.getGraphics();
			offscreenG.setFont(getFont());
		}

		offscreenG.setColor(backgColor);
		offscreenG.fillRect(0, 0, dx, dy);
		calcProjParms();
		int i, j, k, v, v1;
		double q, fac2, fac3, delt;

		if (owner.getStereoOpt() == 2)
			delt = -dx_offset;
		else
			delt = (fac - dfac / deps) * deltar;

		if (owner.getStereoOpt() != 2) {
			offscreenG.setColor(rightColor);
			offscreenG.drawRect(xbase - 2 + (int) delt, ybase - 2, 4, 4);
			offscreenG.setColor(leftColor);
			offscreenG.drawRect(xbase - 2 - (int) delt, ybase - 2, 4, 4);
		}

		if (bTracking) {
			offscreenG.setColor(rightColor);
			offscreenG.drawLine(xbase + (int) delt, ybase, mouseX, mouseY);
			offscreenG.setColor(leftColor);
			offscreenG.drawLine(xbase - (int) delt, ybase, mouseX, mouseY);
		}

		for (i = 0; i < 4; i++) {
			for (j = 0; j < 4; j++) {
				ROT4A[i][j] = 0.0;
				for (k = 0; k < 4; k++)
					ROT4A[i][j] += ROTM[i][k] * ROT4[k][j];
			}
		}

		int len = vertices.length;
		for (v = 0; v < len; v++) {
			for (j = 0; j < 4; j++) {
				rotvert[j] = 0.0;
				for (k = 0; k < 4; k++)
					rotvert[j] += (ROT4A[j][k] * vertices[v][k]);
			}
			fac2 = 1.0 / (1.0 - vpfR * rotvert[3]);
			for (k = 0; k < 3; k++)
				rotvert[k] *= fac2;
			fac3 = dfac / (deps - rotvert[2] / R3);
			vert2y[v] = ybase + (int) (fac3 * rotvert[1]);
			q = fac3 * rotvert[0];
			delt = (fac - fac3) * deltar + dx_offset;
			vert2xR[v] = xbase + (int) (q + delt);
			vert2xL[v] = xbase + (int) (q - delt);
		}

		len = edges.length;
		for (i = 0; i < len; i++) {
			v = edges[i][0];
			v1 = edges[i][1];
			if (bLeftFirst) {
				offscreenG.setColor(leftColor);
				offscreenG.drawLine(vert2xL[v], vert2y[v], vert2xL[v1],
						vert2y[v1]);
			}
			offscreenG.setColor(rightColor);
			offscreenG.drawLine(vert2xR[v], vert2y[v], vert2xR[v1], vert2y[v1]);
			if (!bLeftFirst) {
				offscreenG.setColor(leftColor);
				offscreenG.drawLine(vert2xL[v], vert2y[v], vert2xL[v1],
						vert2y[v1]);
			}
			bLeftFirst = !bLeftFirst;
		}

		g.drawImage(offscreenImg, 0, 0, this);
	}
}
