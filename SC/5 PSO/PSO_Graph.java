import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;

class Particle {
    double[] pos, vel, bestPos;
    double bestVal;

    Particle(int dim, double lo, double hi, Random r) {
        pos = new double[dim];
        vel = new double[dim];
        bestPos = new double[dim];
        for (int i = 0; i < dim; i++) {
            pos[i] = lo + (hi - lo) * r.nextDouble();
            vel[i] = (r.nextDouble() - 0.5) * (hi - lo) * 0.5;
            bestPos[i] = pos[i];
        }
        bestVal = rosenbrock(pos);
    }

    static double rosenbrock(double[] p) {
        double x = p[0], y = p[1];
        return Math.pow(1 - x, 2) + 100 * Math.pow(y - x * x, 2);
    }
}

public class PSO_Graph {
    static class ConvergencePanel extends JPanel {
        final double[] bestHistory;
        final int maxIter;

        ConvergencePanel(double[] bestHistory, int maxIter) {
            this.bestHistory = bestHistory;
            this.maxIter = maxIter;
            setPreferredSize(new Dimension(600, 360));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight(), m = 50;
            g2.setColor(Color.BLACK);
            g2.drawLine(m, H - m, W - m, H - m);
            g2.drawLine(m, H - m, m, m);
            g2.drawString("Iteration", W / 2 - 30, H - 15);
            g2.drawString("Best f(x)", 10, m - 15);
            double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < maxIter; i++) {
                double v = bestHistory[i];
                if (Double.isFinite(v)) {
                    ymin = Math.min(ymin, v);
                    ymax = Math.max(ymax, v);
                }
            }
            if (!Double.isFinite(ymin) || !Double.isFinite(ymax) || ymin == ymax) {
                ymin = 0;
                ymax = 1;
            }
            g2.setFont(g2.getFont().deriveFont(12f));
            g2.setColor(new Color(230, 230, 230));
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10, y = H - m - i * (H - 2 * m) / 10;
                g2.drawLine(x, m, x, H - m);
                g2.drawLine(m, y, W - m, y);
            }
            g2.setColor(Color.BLACK);
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10, y = H - m - i * (H - 2 * m) / 10;
                g2.drawString(String.valueOf(i * maxIter / 10), x - 10, H - m + 20);
                g2.drawString(String.format("%.2f", ymin + (ymax - ymin) * (i / 10.0)), 5, y + 4);
            }
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(66, 135, 245));
            int px = -1, py = -1;
            for (int i = 0; i < maxIter; i++) {
                double v = bestHistory[i], tx = i / (double) (maxIter - 1), ty = (v - ymin) / (ymax - ymin);
                int x = m + (int) Math.round(tx * (W - 2 * m)), y = H - m - (int) Math.round(ty * (H - 2 * m));
                if (px != -1)
                    g2.drawLine(px, py, x, y);
                px = x;
                py = y;
            }
            g2.drawString("Convergence (best fitness vs iteration)", m, m - 20);
        }
    }

    static class SwarmPanel extends JPanel {
        final java.util.List<double[][]> frames;
        final double lo, hi;
        int idx = 0;

        SwarmPanel(java.util.List<double[][]> frames, double lo, double hi) {
            this.frames = frames;
            this.lo = lo;
            this.hi = hi;
            setPreferredSize(new Dimension(600, 360));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight(), m = 40;
            g2.setColor(Color.BLACK);
            g2.drawRect(m, m, W - 2 * m, H - 2 * m);
            g2.drawString("x", W / 2, H - 10);
            g2.rotate(-Math.PI / 2);
            g2.drawString("y", -H / 2, 15);
            g2.rotate(Math.PI / 2);
            g2.drawString("Swarm positions (replay)", m, m - 15);
            g2.setFont(g2.getFont().deriveFont(12f));
            g2.setColor(new Color(230, 230, 230));
            for (int i = 0; i <= 10; i++) {
                int x = m + i * (W - 2 * m) / 10, y = m + i * (H - 2 * m) / 10;
                g2.drawLine(x, m, x, H - m);
                g2.drawLine(m, y, W - m, y);
            }
            g2.setColor(Color.BLACK);
            for (int i = 0; i <= 10; i++) {
                double v = lo + i * (hi - lo) / 10.0;
                int x = m + i * (W - 2 * m) / 10, y = H - m - i * (H - 2 * m) / 10;
                g2.drawString(String.format("%.1f", v), x - 14, H - m + 18);
                g2.drawString(String.format("%.1f", v), m - 35, y + 4);
            }
            if (frames.isEmpty())
                return;
            double[][] pts = frames.get(idx);
            int r = 5;
            g2.setColor(new Color(66, 135, 245));
            for (double[] p : pts) {
                int x = m + (int) Math.round((p[0] - lo) / (hi - lo) * (W - 2 * m)),
                        y = H - m - (int) Math.round((p[1] - lo) / (hi - lo) * (H - 2 * m));
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);
            }
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Frame: " + (idx + 1) + " / " + frames.size(), W - 150, m - 15);
        }

        void next() {
            if (!frames.isEmpty()) {
                idx = (idx + 1) % frames.size();
                repaint();
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Swarm size (enter for 30): ");
        String s = sc.nextLine().trim();
        int SWARM = s.isEmpty() ? 30 : Integer.parseInt(s);
        System.out.print("Max iterations (enter for 1000): ");
        s = sc.nextLine().trim();
        int MAX_ITER = s.isEmpty() ? 1000 : Integer.parseInt(s);

        final int DIM = 2;
        final double LO = -5, HI = 5, W = 0.7, C1 = 1.5, C2 = 1.5, VMAX = (HI - LO) * 0.5;

        // --- Added: print ONLY the pre-declared variables (no logic change) ---
        System.out.println("\n===== Pre-declared Variables =====");
        System.out.printf("DIM      = %d%n", DIM);
        System.out.printf("LO       = %.2f%n", LO);
        System.out.printf("HI       = %.2f%n", HI);
        System.out.printf("W        = %.2f%n", W);
        System.out.printf("C1       = %.2f%n", C1);
        System.out.printf("C2       = %.2f%n", C2);
        System.out.printf("VMAX     = %.2f%n", VMAX);
        System.out.printf("SWARM    = %d%n", SWARM);
        System.out.printf("MAX_ITER = %d%n", MAX_ITER);
        System.out.println("==================================\n");
        // ---------------------------------------------------------------------

        Random rand = new Random();
        Particle[] swarm = new Particle[SWARM];
        for (int i = 0; i < SWARM; i++)
            swarm[i] = new Particle(DIM, LO, HI, rand);

        double[] gBest = swarm[0].bestPos.clone();
        double gVal = Double.MAX_VALUE;
        for (Particle p : swarm)
            if (p.bestVal < gVal) {
                gVal = p.bestVal;
                gBest = p.bestPos.clone();
            }

        double[] bestHistory = new double[MAX_ITER];
        int frameStride = Math.max(1, MAX_ITER / 200);
        java.util.List<double[][]> frames = new ArrayList<>();
        int progressStep = Math.max(1, MAX_ITER / 10);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            for (Particle p : swarm) {
                for (int d = 0; d < DIM; d++) {
                    double r1 = rand.nextDouble(), r2 = rand.nextDouble();
                    p.vel[d] = W * p.vel[d] + C1 * r1 * (p.bestPos[d] - p.pos[d]) + C2 * r2 * (gBest[d] - p.pos[d]);
                    if (p.vel[d] > VMAX)
                        p.vel[d] = VMAX;
                    if (p.vel[d] < -VMAX)
                        p.vel[d] = -VMAX;
                    p.pos[d] += p.vel[d];
                    if (p.pos[d] < LO)
                        p.pos[d] = LO;
                    if (p.pos[d] > HI)
                        p.pos[d] = HI;
                }
                double val = Particle.rosenbrock(p.pos);
                if (val < p.bestVal) {
                    p.bestVal = val;
                    p.bestPos = p.pos.clone();
                }
                if (p.bestVal < gVal) {
                    gVal = p.bestVal;
                    gBest = p.bestPos.clone();
                }
            }
            bestHistory[iter] = gVal;
            if (iter % frameStride == 0) {
                double[][] snapshot = new double[SWARM][2];
                for (int i = 0; i < SWARM; i++)
                    snapshot[i] = new double[] { swarm[i].pos[0], swarm[i].pos[1] };
                frames.add(snapshot);
            }
            if (iter % progressStep == 0)
                System.out.printf("Iter %4d / %d : best = %.10f at (%.6f, %.6f)%n", iter, MAX_ITER, gVal, gBest[0],
                        gBest[1]);
        }
        System.out.printf("Final best: f(%.6f, %.6f) = %.12f%n", gBest[0], gBest[1], gVal);
        sc.close();

        SwingUtilities.invokeLater(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Convergence", new ConvergencePanel(bestHistory, bestHistory.length));
            SwarmPanel sp = new SwarmPanel(frames, LO, HI);
            tabs.addTab("Swarm Replay", sp);
            JFrame f = new JFrame("PSO Plots");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.getContentPane().add(tabs);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            new Timer(80, e -> sp.next()).start();
        });
    }
}
