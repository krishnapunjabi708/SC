import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class AntColonyAntTravel {

    // ----- Your exact terms (unchanged) -----
    static final double[][] COST = {
        {0, 5, 15, 4},
        {5, 0, 4, 8},
        {15, 4, 0, 1},
        {4, 8, 1, 0}
    };
    static final int N = COST.length;
    static final Random RAND = new Random(12345); // deterministic
    static final double ALPHA = 1.0;
    static final double BETA  = 2.0;
    static final double Q     = 1.0;
    static final double RHO   = 0; // No evaporation

    // Original 3 given ants from your code
    static final List<int[]> GIVEN_ANTS = Arrays.asList(
        new int[]{0, 1, 2, 1, 0}, // Ant 1
        new int[]{0, 2, 0},       // Ant 2
        new int[]{0, 3, 2, 3, 0}  // Ant 3
    );
    // ----------------------------------------

    // Simple pheromone matrix
    static double[][] pher = new double[N][N];

    public static void main(String[] args) {
        // 1) Run and deposit pheromone for the 3 given ants
        for (int i = 0; i < GIVEN_ANTS.size(); i++) {
            int[] path = GIVEN_ANTS.get(i);
            double L = pathLength(path);
            addPheromoneFromPath(pher, path, L);
            System.out.printf("Given Ant %d: %s  length=%.3f%n", i+1, pathToString(path), L);
        }

        // (RHO=0, evaporation does nothing but kept for correctness)
        applyEvaporation(pher, RHO);

        // 2) Build 4th ant using roulette with current pheromone
        int[] ant4 = buildTour(pher, 0);
        double L4 = tourLength(ant4);
        addPheromoneFromPath(pher, ant4, L4);
        System.out.printf("4th Ant   : %s  length=%.3f%n", pathToString(ant4), L4);

        // 3) Build 5th ant with updated pheromone
        int[] ant5 = buildTour(pher, 0);
        double L5 = tourLength(ant5);
        addPheromoneFromPath(pher, ant5, L5);
        System.out.printf("5th Ant   : %s  length=%.3f%n", pathToString(ant5), L5);

        // 4) Prepare a simple animation of ants traveling on the points
        List<int[]> pathsToShow = new ArrayList<>();
        pathsToShow.addAll(GIVEN_ANTS);
        pathsToShow.add(ant4);
        pathsToShow.add(ant5);

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Ant Travel on Points (Simple)");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(new AntTravelPanel(pathsToShow));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // ----- Core ACO helpers (simple & readable) -----
    static double pathLength(int[] p) {
        double s = 0;
        for (int i = 0; i < p.length - 1; i++) s += COST[p[i]][p[i+1]];
        return s;
    }
    static double tourLength(int[] t) { return pathLength(t); }

    static void addPheromoneFromPath(double[][] pher, int[] path, double L) {
        if (L <= 0) return;
        double dep = Q / L;
        for (int i = 0; i < path.length - 1; i++) {
            int a = path[i], b = path[i+1];
            pher[a][b] += dep;
            pher[b][a] += dep;
        }
    }

    static void applyEvaporation(double[][] pher, double rho) {
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                pher[i][j] *= (1.0 - rho); // RHO=0 → no change
    }

    static int[] buildTour(double[][] pher, int start) {
        boolean[] used = new boolean[N];
        int[] tour = new int[N + 1];
        int cur = start;
        tour[0] = cur; used[cur] = true;

        for (int step = 1; step < N; step++) {
            double[] probs = new double[N];
            double sum = 0;
            for (int j = 0; j < N; j++) {
                if (!used[j]) {
                    double tau = Math.pow(pher[cur][j], ALPHA);
                    double eta = COST[cur][j] > 0 ? Math.pow(1.0 / COST[cur][j], BETA) : 0;
                    probs[j] = tau * eta;
                    sum += probs[j];
                }
            }
            int next;
            if (sum == 0) { // fallback uniform
                next = firstUnused(used);
            } else {
                double r = RAND.nextDouble() * sum, acc = 0;
                next = -1;
                for (int j = 0; j < N; j++) {
                    acc += probs[j];
                    if (acc >= r) { next = j; break; }
                }
                if (next < 0) next = firstUnused(used);
            }
            tour[step] = next; used[next] = true; cur = next;
        }
        tour[N] = start;
        return tour;
    }

    static int firstUnused(boolean[] u) { for (int i = 0; i < u.length; i++) if (!u[i]) return i; return 0; }

    static String pathToString(int[] p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            sb.append(p[i]);
            if (i < p.length - 1) sb.append("->");
        }
        return sb.toString();
    }

    // ----- Simple panel to draw nodes and animate ant traveling on points -----
    static class AntTravelPanel extends JPanel {
        final List<int[]> paths;           // paths to animate (3 given + 4th + 5th)
        final Point2D.Double[] pos;        // node positions
        int currentPath = 0;               // which path is active
        int edgeIndex = 0;                 // which edge in the path
        double t = 0;                      // 0..1 along the current edge
        final javax.swing.Timer timer;     // FIXED: explicitly use Swing Timer

        AntTravelPanel(List<int[]> paths) {
            this.paths = new ArrayList<>(paths);
            setPreferredSize(new Dimension(640, 420));
            setBackground(Color.WHITE);

            // Simple fixed layout for 4 nodes (looks neat)
            pos = new Point2D.Double[] {
                new Point2D.Double(120, 90),   // node 0
                new Point2D.Double(480, 90),   // node 1
                new Point2D.Double(480, 300),  // node 2
                new Point2D.Double(120, 300)   // node 3
            };

            // Simple timer: move along edges; when a path ends, go to next
            timer = new javax.swing.Timer(40, e -> step());
            timer.start();
        }

        void step() {
            if (currentPath >= paths.size()) {
                timer.stop();
                repaint();
                return;
            }
            int[] p = paths.get(currentPath);
            if (edgeIndex >= p.length - 1) {
                // go to next path
                currentPath++;
                edgeIndex = 0;
                t = 0;
                repaint();
                return;
            }

            // Move forward along the current edge
            t += 0.04;                      // speed (adjust if you like)
            if (t >= 1.0) { t = 0; edgeIndex++; }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw title
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            g.drawString("Ant Travel on Points", 20, 20);

            // Draw edges (light)
            g.setColor(new Color(220,220,220));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < N; i++) {
                for (int j = i+1; j < N; j++) {
                    g.drawLine((int)pos[i].x, (int)pos[i].y, (int)pos[j].x, (int)pos[j].y);
                }
            }

            // Draw nodes
            for (int i = 0; i < N; i++) {
                int r = 20;
                g.setColor(new Color(240,240,240));
                g.fillOval((int)pos[i].x - r, (int)pos[i].y - r, 2*r, 2*r);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2f));
                g.drawOval((int)pos[i].x - r, (int)pos[i].y - r, 2*r, 2*r);
                g.drawString(String.valueOf(i), (int)pos[i].x - 4, (int)pos[i].y + 5);
            }

            // Draw current path and ant position
            if (currentPath < paths.size()) {
                int[] p = paths.get(currentPath);

                // draw the polyline of the full path (thin)
                g.setColor(new Color(66,135,245));
                g.setStroke(new BasicStroke(2f));
                for (int k = 0; k < p.length - 1; k++) {
                    Point2D a = pos[p[k]];
                    Point2D b = pos[p[k+1]];
                    g.drawLine((int)a.getX(), (int)a.getY(), (int)b.getX(), (int)b.getY());
                }

                // current ant position on edge (p[edgeIndex] -> p[edgeIndex+1])
                if (edgeIndex < p.length - 1) {
                    Point2D a = pos[p[edgeIndex]];
                    Point2D b = pos[p[edgeIndex+1]];
                    double x = a.getX() + (b.getX()-a.getX()) * t;
                    double y = a.getY() + (b.getY()-a.getY()) * t;

                    // ant dot
                    g.setColor(Color.RED);
                    int rad = 8;
                    g.fillOval((int)x - rad, (int)y - rad, 2*rad, 2*rad);
                    g.setColor(Color.BLACK);
                    g.drawOval((int)x - rad, (int)y - rad, 2*rad, 2*rad);
                }

                // label
                g.setColor(Color.DARK_GRAY);
                g.setFont(g.getFont().deriveFont(12f));
                g.drawString("Path " + (currentPath+1) + " : " + pathToString(p),
                             20, getHeight() - 20);
            }
        }
    }
}
