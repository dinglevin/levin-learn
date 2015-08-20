package org.jcyclone.core.profiler;

import org.jcyclone.core.cfg.ISystemConfig;
import org.jcyclone.core.stage.IStageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A profiler handler that display on the screen profilable object states.
 *
 * @author Jean Morissette and others
 */
public class UIProfilerHandler implements IProfilerHandler {

	ArrayList curves = new ArrayList();

	HashMap bgColors = new HashMap();
	HashMap fgColors = new HashMap();
	HashMap tColors = new HashMap();

	private int defaultsize = 600;
	UIProfiler ui;

	public void init(IStageManager mgr) {

		defaultsize = mgr.getConfig().getInt("global.profile.handler.buffersize", 600);
		int col = mgr.getConfig().getInt("global.profile.handler.columns", 3);

		ISystemConfig config = mgr.getConfig();
		readColors(config.getStringList("global.profile.handler.bgcolors"), bgColors);
		readColors(config.getStringList("global.profile.handler.fgcolors"), fgColors);
		readColors(config.getStringList("global.profile.handler.tcolors"), tColors);

		ui = new UIProfiler(this, col);
		ui.setSize(600, 400);
		ui.setLocation(300, 300);
		ui.setVisible(true);
	}

	private void readColors(String[] regex_color_pairs, Map map) {
		for (int i = 0; regex_color_pairs != null && (i + 1) < regex_color_pairs.length; i += 2) {
			Pattern p = null;
			try {
				p = Pattern.compile(regex_color_pairs[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (p != null)
				map.put(p, new Color(Integer.parseInt(regex_color_pairs[i + 1], 16)));
		}
	}

	private Color findColor(String name, Map map, Color def) {
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry e = (Map.Entry) it.next();
			Pattern p = (Pattern) e.getKey();
			if (p.matcher(name).matches()) {
				return (Color) e.getValue();
			}
		}
		return def;
	}

	public void profilableAdded(String name) {
		Curve c = new Curve(name, defaultsize);
		c.bgcolor = findColor(name, bgColors, Color.BLACK);
		c.fgcolor = findColor(name, fgColors, Color.GREEN);
		c.tcolor = findColor(name, tColors, Color.LIGHT_GRAY);
		curves.add(c);
	}

	public void profilableRemoved(String name) {
		for (int i = 0; i < curves.size(); i++) {
			Curve curve = (Curve) curves.get(i);
			if (curve.name.equals(name)) {
				curves.remove(i);
				break;
			}
		}
	}

	public void sampleDelayChanged(int newDelay) {
		// do nothing
	}

	public void profilablesSnapshot(int[] values) {
		for (int i = 0; i < values.length; i++) {
			Curve c = (Curve) curves.get(i);
			if (c != null)
				c.add(values[i]);
		}
		ui.repaint();
	}

	public void destroy() {
		if (ui != null)
			ui.dispose();
		curves.clear();
	}


	static class Curve {
		long[] values;
		int count = 0;
		int nextpos = 0;
		String name;

		Color bgcolor = Color.BLACK;
		Color fgcolor = Color.GREEN;
		Color tcolor = Color.WHITE;

		Curve(String name, int size) {
			this.name = name;
			values = new long[size];
		}

		synchronized void add(long l) {
			values[nextpos] = l;
			nextpos = (nextpos + 1) % values.length;
			count++;
			if (count > values.length)
				count = values.length;
		}

		synchronized long[] getValues() {
			long[] la = new long[count];
			if (nextpos == count) { //wrapped
				System.arraycopy(values, 0, la, 0, count);
			} else if (nextpos < count) {
				System.arraycopy(values, nextpos, la, 0, count - nextpos);
				System.arraycopy(values, 0, la, count - nextpos, nextpos);
			} else {
				throw new RuntimeException("nextpos (" + nextpos + ") > count (" + count + ")");
			}

			return la;
		}

		static long findMax(long[] la) {
			int n = la.length;
			long m = 0;
			for (int i = 0; i < n; i++) {
				if (la[i] > m)
					m = la[i];
			}
			return m;
		}
	}

	static class UIProfiler extends JFrame {
		Graph graph;
		UIProfilerHandler ph;

		UIProfiler(UIProfilerHandler _ph, int columns) {
			super("ProfilerUI");
			this.ph = _ph;
			this.getContentPane().setLayout(new BorderLayout());

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					ph.destroy();
				}
			});

			graph = new Graph(ph, columns);
			graph.setFont(new Font("Dialog", Font.PLAIN, 10));
			this.getContentPane().add(graph, BorderLayout.CENTER);
		}

//		public void doLayout() {
//			Dimension d = getSize();
//			Insets ins = getInsets();
//			graph.setBounds(ins.left, ins.top, d.width - ins.left - ins.right, d.height - ins.top - ins.bottom);
//		}
	}

	static class Graph extends JComponent {
		static Color fg = Color.white;
		static Color bg = Color.black;

		static final int TOP = 2;
		static final int BOTTOM = 2;
		static final int LEFT = 2;
		static final int RIGHT = 2;
		static final int GAP = 2;

		UIProfilerHandler ph;
		int columns;

		Graph(UIProfilerHandler ph, int columns) {
			this.ph = ph;
			this.columns = columns;
		}

		public void paintComponent(Graphics g) {
			Dimension d = getSize();
			g.setColor(bg);
			g.fillRect(0, 0, d.width, d.height);
			g.setColor(fg);
			g.drawRect(0, 0, d.width - 1, d.height - 1);

			try {
				paintSeries(g, LEFT, TOP, d.width - LEFT - RIGHT, d.height - TOP - BOTTOM, ph.curves);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			Toolkit.getDefaultToolkit().sync();
		}

		void paintSeries(Graphics g, int x, int y, int w, int h, java.util.List items) {
			int n = items.size();
			if (n <= 0)
				return;

			int rows = (n - 1) / columns + 1;
			int sh = h / rows;
			int sw = w / columns;
			for (int i = 0; i < n; i++) {
				Curve c = (Curve) items.get(i);
				paintOne(g, c, x + sw * (i % columns) + 2, y + sh * (i / columns) + 2, sw - 4, sh - 4);
			}
		}

		static void paintOne(Graphics g, Curve c, int x, int y, int w, int h) {
			g.setColor(c.bgcolor);
			g.fillRect(x, y, w, h);

			g.setColor(Color.DARK_GRAY);
			g.drawRect(x, y, w, h);

			String msg = c.name;

			if (c.count > 0) {
				g.setColor(Color.GREEN);
				long[] values = c.getValues();
				long maxy = Curve.findMax(values);
				long amp = (maxy <= 0 ? 1 : maxy);

				int sx1 = 0;
				int sy1 = (int) (h * values[0] / amp);

				for (int i = 0; i < values.length; i++) {
					long val = values[i];
					int sx2 = (int) (w * i / values.length);
					int sy2 = (int) (h * val / amp);
					g.drawLine(x + sx1, y + h - sy1, x + sx2, y + h - sy2);
					sx1 = sx2;
					sy1 = sy2;
				}

				msg += " = " + values[values.length - 1] + " (max=" + maxy + ")";
			} else {
				msg += " (no result)";
			}

			FontMetrics fm = g.getFontMetrics();
			int sw = fm.stringWidth(msg);
			//int ma = fm.getMaxAscent();
			//int md = fm.getMaxDescent();
			g.setColor(Color.lightGray);
			g.drawString(msg, x + (w - sw) / 2, y + h / 2);
		}
	}
}
