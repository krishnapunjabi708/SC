import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GWORosenbrock_Graph {
    // Predefined (default) parameters
    static final int DEFAULT_WOLF_COUNT = 30;
    static final int DIMENSIONS = 2;
    static final int DEFAULT_MAX_ITER = 1000;
    static final double MIN_BOUND = -5;
    static final double MAX_BOUND = 5;

    static Random rand = new Random();

    // Rosenbrock function
    public static double rosenbrock(double[] pos) {
        double x = pos[0];
        double y = pos[1];
        return Math.pow(1 - x, 2) + 100 * Math.pow(y - x * x, 2);
    }

    // Clamp within bounds
    public static double clamp(double val) {
        if (val < MIN_BOUND) return MIN_BOUND;
        if (val > MAX_BOUND) return MAX_BOUND;
        return val;
    }

    // ---------- Simple panels for graphs ----------
    static class ConvergencePanel extends JPanel {
        final double[] bestHistory;
        final int iters;

        ConvergencePanel(double[] bestHistory, int iters) {
            this.bestHistory = bestHistory;
            this.iters = iters;
            setPreferredSize(new Dimension(600, 360));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight(), m = 45;
            // axes
            g2.setColor(Color.BLACK);
            g2.drawLine(m, H - m, W - m, H - m);
            g2.drawLine(m, H - m, m, m);
            g2.drawString("Iteration", W / 2 - 25, H - 12);
            g2.rotate(-Math.PI / 2);
            g2.drawString("Best f(x)", -H / 2 + 5, 15);
            g2.rotate(Math.PI / 2);

            // min/max
            double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < iters; i++) {
                double v = bestHistory[i];
                if (Double.isFinite(v)) {
                    ymin = Math.min(ymin, v);
                    ymax = Math.max(ymax, v);
                }
            }
            if (!Double.isFinite(ymin) || !Double.isFinite(ymax) || ymin == ymax) {
                ymin = 0; ymax = 1;
            }

            // grid
            g2.setColor(new Color(230, 230, 230));
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10;
                int y = H - m - i * (H - 2 * m) / 10;
                g2.drawLine(x, m, x, H - m);
                g2.drawLine(m, y, W - m, y);
            }

            // ticks
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(12f));
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10;
                int y = H - m - i * (H - 2 * m) / 10;
                g2.drawString(String.valueOf(i * iters / 10), x - 10, H - m + 18);
                double yval = ymin + (ymax - ymin) * (i / 10.0);
                g2.drawString(String.format("%.2e", yval), 5, y + 4);
            }

            // plot
            g2.setColor(new Color(66, 135, 245));
            g2.setStroke(new BasicStroke(2f));
            int px = -1, py = -1;
            for (int i = 0; i < iters; i++) {
                double v = bestHistory[i];
                double tx = i / (double) Math.max(1, iters - 1);
                double ty = (v - ymin) / (ymax - ymin);
                int x = m + (int) Math.round(tx * (W - 2 * m));
                int y = H - m - (int) Math.round(ty * (H - 2 * m));
                if (px != -1) g2.drawLine(px, py, x, y);
                px = x; py = y;
            }
            g2.drawString("Convergence (best fitness vs iteration)", m, m - 10);
        }
    }

    static class SwarmPanel extends JPanel {
        final List<double[][]> frames;   // wolves per frame: [N][2]
        final List<double[]> prey;       // alpha per frame: [2]
        final double lo, hi;
        int idx = 0;

        SwarmPanel(List<double[][]> frames, List<double[]> prey, double lo, double hi) {
            this.frames = frames;
            this.prey = prey;
            this.lo = lo; this.hi = hi;
            setPreferredSize(new Dimension(600, 360));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight(), m = 40;

            // box + labels
            g2.setColor(Color.BLACK);
            g2.drawRect(m, m, W - 2 * m, H - 2 * m);
            g2.drawString("x", W / 2, H - 10);
            g2.rotate(-Math.PI / 2);
            g2.drawString("y", -H / 2, 15);
            g2.rotate(Math.PI / 2);
            g2.drawString("Fox running for prey (wolves & best)", m, m - 10);

            // grid
            g2.setColor(new Color(230, 230, 230));
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10, y = m + i * (H - 2 * m) / 10;
                g2.drawLine(x, m, x, H - m);
                g2.drawLine(m, y, W - m, y);
            }

            if (frames.isEmpty()) return;
            double[][] pts = frames.get(idx);
            double[] pbest = prey.get(idx);

            // wolves
            g2.setColor(new Color(66, 135, 245));
            int r = 5;
            for (double[] p : pts) {
                int x = m + (int) Math.round((p[0] - lo) / (hi - lo) * (W - 2 * m));
                int y = H - m - (int) Math.round((p[1] - lo) / (hi - lo) * (H - 2 * m));
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);
            }

            // prey (alpha) in red
            if (pbest != null) {
                g2.setColor(Color.RED);
                int x = m + (int) Math.round((pbest[0] - lo) / (hi - lo) * (W - 2 * m));
                int y = H - m - (int) Math.round((pbest[1] - lo) / (hi - lo) * (H - 2 * m));
                g2.fillOval(x - 6, y - 6, 12, 12);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("Prey (alpha): (%.3f, %.3f)", pbest[0], pbest[1]), m + 8, m + 14);
            }

            // frame label
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Frame: " + (idx + 1) + " / " + frames.size(), W - 150, m - 10);
        }

        void next() {
            if (!frames.isEmpty()) {
                idx = (idx + 1) % frames.size();
                repaint();
            }
        }
    }
    // ---------- end panels ----------

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // --- Input: population (wolves) and iterations ---
        System.out.print("Enter population size (press Enter for " + DEFAULT_WOLF_COUNT + "): ");
        String inp = sc.nextLine().trim();
        int WOLF_COUNT;
        try {
            WOLF_COUNT = inp.isEmpty() ? DEFAULT_WOLF_COUNT : Integer.parseInt(inp);
        } catch (Exception e) {
            WOLF_COUNT = DEFAULT_WOLF_COUNT;
        }

        System.out.print("Enter max iterations (press Enter for " + DEFAULT_MAX_ITER + "): ");
        inp = sc.nextLine().trim();
        int MAX_ITER;
        try {
            MAX_ITER = inp.isEmpty() ? DEFAULT_MAX_ITER : Integer.parseInt(inp);
        } catch (Exception e) {
            MAX_ITER = DEFAULT_MAX_ITER;
        }

        // --- Display predefined and chosen (defined) parameters ---
        System.out.println("\n=== Parameters ===");
        System.out.println("Predefined:");
        System.out.printf("  DIMENSIONS         = %d%n", DIMENSIONS);
        System.out.printf("  MIN_BOUND          = %.2f%n", MIN_BOUND);
        System.out.printf("  MAX_BOUND          = %.2f%n", MAX_BOUND);
        System.out.printf("  DEFAULT_WOLF_COUNT = %d%n", DEFAULT_WOLF_COUNT);
        System.out.printf("  DEFAULT_MAX_ITER   = %d%n", DEFAULT_MAX_ITER);
        System.out.println("Chosen (defined):");
        System.out.printf("  WOLF_COUNT         = %d%n", WOLF_COUNT);
        System.out.printf("  MAX_ITER           = %d%n", MAX_ITER);
        System.out.println("==================\n");

        double[][] wolves = new double[WOLF_COUNT][DIMENSIONS];

        // Random initialization
        for (int i = 0; i < WOLF_COUNT; i++) {
            for (int d = 0; d < DIMENSIONS; d++) {
                wolves[i][d] = MIN_BOUND + (MAX_BOUND - MIN_BOUND) * rand.nextDouble();
            }
        }

        double[] alpha = new double[DIMENSIONS];
        double[] beta  = new double[DIMENSIONS];
        double[] delta = new double[DIMENSIONS];
        double alphaScore = Double.MAX_VALUE;
        double betaScore  = Double.MAX_VALUE;
        double deltaScore = Double.MAX_VALUE;

        // For graphs
        double[] bestHistory = new double[MAX_ITER];
        List<double[][]> frames = new ArrayList<>();
        List<double[]>   preyHistory = new ArrayList<>();
        int frameStride = Math.max(1, MAX_ITER / 200); // ~200 frames max
        int progressStep = Math.max(1, MAX_ITER / 10);

        // Main loop
        for (int iter = 0; iter < MAX_ITER; iter++) {
            // Evaluate wolves
            for (int i = 0; i < WOLF_COUNT; i++) {
                double score = rosenbrock(wolves[i]);

                if (score < alphaScore) {
                    // Shift down
                    deltaScore = betaScore;
                    delta = beta.clone();
                    betaScore = alphaScore;
                    beta = alpha.clone();
                    alphaScore = score;
                    alpha = wolves[i].clone();
                } else if (score < betaScore) {
                    deltaScore = betaScore;
                    delta = beta.clone();
                    betaScore = score;
                    beta = wolves[i].clone();
                } else if (score < deltaScore) {
                    deltaScore = score;
                    delta = wolves[i].clone();
                }
            }

            double a = 2.0 - (2.0 * iter / (double) MAX_ITER); // decreasing from 2 to 0

            // Update positions
            for (int i = 0; i < WOLF_COUNT; i++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    double r1 = rand.nextDouble(), r2 = rand.nextDouble();
                    double A1 = 2 * a * r1 - a;
                    double C1 = 2 * r2;
                    double D_alpha = Math.abs(C1 * alpha[d] - wolves[i][d]);
                    double X1 = alpha[d] - A1 * D_alpha;

                    r1 = rand.nextDouble(); r2 = rand.nextDouble();
                    double A2 = 2 * a * r1 - a;
                    double C2 = 2 * r2;
                    double D_beta = Math.abs(C2 * beta[d] - wolves[i][d]);
                    double X2 = beta[d] - A2 * D_beta;

                    r1 = rand.nextDouble(); r2 = rand.nextDouble();
                    double A3 = 2 * a * r1 - a;
                    double C3 = 2 * r2;
                    double D_delta = Math.abs(C3 * delta[d] - wolves[i][d]);
                    double X3 = delta[d] - A3 * D_delta;

                    wolves[i][d] = (X1 + X2 + X3) / 3.0;
                    wolves[i][d] = clamp(wolves[i][d]);
                }
            }

            // record history
            bestHistory[iter] = alphaScore;
            if (iter % frameStride == 0) {
                double[][] snap = new double[WOLF_COUNT][2];
                for (int i = 0; i < WOLF_COUNT; i++) {
                    snap[i][0] = wolves[i][0];
                    snap[i][1] = wolves[i][1];
                }
                frames.add(snap);
                preyHistory.add(alpha.clone()); // alpha as "prey"
            }

            if (iter % progressStep == 0) {
                System.out.printf("Iter %4d / %d : Best = %.8f at (%.6f, %.6f)%n",
                        iter, MAX_ITER, alphaScore, alpha[0], alpha[1]);
            }
        }

        System.out.printf("%nFinal Best: f(%.6f, %.6f) = %.10f%n",
                alpha[0], alpha[1], alphaScore);

        sc.close();

        // ---------- Show simple graphs ----------
        SwingUtilities.invokeLater(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Convergence", new ConvergencePanel(bestHistory, bestHistory.length));
            SwarmPanel sp = new SwarmPanel(frames, preyHistory, MIN_BOUND, MAX_BOUND);
            tabs.addTab("Fox & Prey (Replay)", sp);

            JFrame f = new JFrame("GWO – Convergence & Fox Running for Prey");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.getContentPane().add(tabs);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            // simple animation timer
            new Timer(80, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { sp.next(); }
            }).start();
        });
    }
}
