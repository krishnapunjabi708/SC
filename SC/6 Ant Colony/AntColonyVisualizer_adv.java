import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class AntColonyVisualizer_adv {

    // ----- problem / ACO parameters -----
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

    // Colors for different ants
    static final Color[] ANT_COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN
    };

    // visualization constants
    static final int WIDTH = 1100, HEIGHT = 760;
    static final Point2D.Double[] NODE_POS = new Point2D.Double[] {
        new Point2D.Double(140, 160),
        new Point2D.Double(420, 110),
        new Point2D.Double(460, 360),
        new Point2D.Double(170, 440)
    };

    // UI components
    JFrame frame;
    NetworkPanel netPanel;
    ProbPanel probPanel;
    JTextArea logArea;

    // pheromone matrix (symmetric)
    double[][] pher;

    public AntColonyVisualizer_adv() {
        pher = new double[N][N];
        setupUI();
        new Thread(this::runScenario).start();
    }

    void setupUI() {
        frame = new JFrame("Ant Colony Visualizer — Original 3 ants + 4th + 5th + 40 iterations (NO evaporation)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLayout(new BorderLayout());

        netPanel = new NetworkPanel();
        probPanel = new ProbPanel();
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(false);
        JScrollPane sp = new JScrollPane(logArea);
        sp.setPreferredSize(new Dimension(350, HEIGHT));

        JPanel right = new JPanel(new BorderLayout());
        right.add(probPanel, BorderLayout.NORTH);
        right.add(sp, BorderLayout.CENTER);

        frame.add(netPanel, BorderLayout.CENTER);
        frame.add(right, BorderLayout.EAST);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    void runScenario() {
        log("=== Starting scenario: Original 3 ants + 4th + 5th + 40 iterations (RHO=0, NO EVAPORATION) ===");
        
        // 1) Run first 3 given ants (exactly as in your code)
        List<Double> lengths = new ArrayList<>();
        for (int i = 0; i < GIVEN_ANTS.size(); i++) {
            int[] path = GIVEN_ANTS.get(i);
            double L = pathLength(path);
            lengths.add(L);
            log(String.format("Running GIVEN ant %d/3 path: %s   length = %.6f", i+1, pathToString(path), L));
            
            // Animate slowly
            netPanel.animateAntAlongPath(path, ANT_COLORS[i], 2500);
            addPheromoneFromPath(pher, path, L);
            sleep(1200);
        }

        log("\n=== PHEROMONE MATRIX AFTER 3rd ANT (before evap) ===");
        printPheromoneToLog(pher);
        sleep(1500);

        // Apply evaporation (RHO=0, no change)
        applyEvaporation(pher, RHO);
        log(String.format("\nApplied evaporation rho=%.2f (no change)", RHO));
        log("\n=== PHEROMONE MATRIX AFTER EVAPORATION ===");
        printPheromoneToLog(pher);
        sleep(1500);

        // Show transition probabilities after 3 ants
        double[][] probAfter3 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        probPanel.showTransitionMatrix(probAfter3, "Transition Probabilities (After 3 ants + evap)");
        log("\nTransition probabilities computed (rows normalized τ^α × η^β):");
        sleep(2000);

        // 2) Build and run 4th ant using roulette selection (like your code)
        log("\n=== Building 4th ANT using roulette-wheel (Random(12345)) ===");
        int[] ant4 = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
        double L4 = tourLength(ant4);
        log(String.format("4th ant tour: %s   length = %.6f", tourToString(ant4), L4));
        
        // Animate 4th ant
        netPanel.animateAntAlongPath(ant4, ANT_COLORS[3], 2500);
        double deposit4 = (L4 > 0.0) ? (Q / L4) : 0.0;
        addPheromoneFromAnt(pher, ant4, L4);
        log(String.format("4th ant deposit = %.6f", deposit4));
        sleep(1200);

        log("\n=== PHEROMONE MATRIX AFTER 4th ANT DEPOSIT ===");
        printPheromoneToLog(pher);
        sleep(1500);

        // 3) Build and run 5th ant using updated pheromones from 4th ant
        log("\n=== Building 5th ANT using roulette-wheel (pheromones from 4th ant) ===");
        int[] ant5 = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
        double L5 = tourLength(ant5);
        log(String.format("5th ant tour: %s   length = %.6f", tourToString(ant5), L5));
        
        // Show transition probabilities BEFORE 5th ant deposits
        double[][] probBefore5 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        probPanel.showTransitionMatrix(probBefore5, "Transition Probabilities (Before 5th ant deposit)");
        
        // Animate 5th ant
        netPanel.animateAntAlongPath(ant5, ANT_COLORS[4], 2500);
        double deposit5 = (L5 > 0.0) ? (Q / L5) : 0.0;
        addPheromoneFromAnt(pher, ant5, L5);
        log(String.format("5th ant deposit = %.6f", deposit5));
        sleep(1200);

        log("\n=== PHEROMONE MATRIX CHANGED DUE TO 5th ANT DEPOSIT ===");
        printPheromoneToLog(pher);
        sleep(1500);

        // Show transition probabilities AFTER 5th ant
        double[][] probAfter5 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        probPanel.showTransitionMatrix(probAfter5, "Transition Probabilities (After 5th ant deposit)");

        // 4) Continue with 40 iterations (unchanged)
        log("\n=== Starting 40 ITERATIONS (pheromone updated after each ant) ===");
        double bestLength = Double.MAX_VALUE;
        int bestAntIndex = -1;
        List<Double> iterationLengths = new ArrayList<>();
        
        for (int iter = 0; iter < 40; iter++) {
            int[] tour = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
            double length = tourLength(tour);
            double deposit = (length > 0) ? Q / length : 0.0;
            addPheromoneFromAnt(pher, tour, length);
            
            iterationLengths.add(length);
            if (length < bestLength) {
                bestLength = length;
                bestAntIndex = iter;
            }
            
            if (iter % 10 == 0 || iter == 39) {
                log(String.format("Iteration %d: length=%.4f, deposit=%.6f, path=%s", 
                    iter+1, length, deposit, pathToString(tour)));
                if (iter % 10 == 0 || iter == 39) {
                    log(String.format("\n--- PHEROMONE AFTER ITERATION %d ---", iter+1));
                    printPheromoneToLog(pher);
                }
            }
            
            if (iter % 10 == 9) {
                netPanel.repaint();
                sleep(300);
            }
        }

        log(String.format("\n=== 40 ITERATIONS COMPLETE ==="));
        log(String.format("Best solution from iterations #%d: length %.4f", bestAntIndex+6, bestLength));
        
        // Final pheromone matrix
        log("\n=== FINAL PHEROMONE MATRIX AFTER 40 ITERATIONS ===");
        printPheromoneToLog(pher);
        sleep(1500);

        // Animate best solution
        log("\n=== Animating BEST SOLUTION (CYAN) ===");
        int[] bestTour = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
        netPanel.animateAntAlongPath(bestTour, Color.CYAN, 3000);
        log(String.format("Best tour: %s length=%.4f", pathToString(bestTour), tourLength(bestTour)));
        
        // Final transition probabilities
        double[][] finalProb = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        probPanel.showTransitionMatrix(finalProb, "Final Transition Probabilities");
        
        log("\n=== OPTIMIZATION COMPLETE ===");
        logIterationStats(iterationLengths);
    }

    // Methods from your original code (unchanged)
    static double pathLength(int[] p) {
        double s = 0;
        for (int i = 0; i < p.length - 1; i++) s += COST[p[i]][p[i+1]];
        return s;
    }

    static double tourLength(int[] tour) {
        double s = 0;
        for (int i = 0; i < tour.length - 1; i++) s += COST[tour[i]][tour[i+1]];
        return s;
    }

    static void addPheromoneFromPath(double[][] pher, int[] path, double length) {
        if (length <= 0) return;
        double dep = Q / length;
        for (int i = 0; i < path.length - 1; i++) {
            int a = path[i], b = path[i+1];
            pher[a][b] += dep;
            pher[b][a] += dep;
        }
    }

    static void addPheromoneFromAnt(double[][] pher, int[] tour, double length) {
        if (length <= 0) return;
        double dep = Q / length;
        for (int i = 0; i < tour.length - 1; i++) {
            int a = tour[i], b = tour[i+1];
            pher[a][b] += dep;
            pher[b][a] += dep;
        }
    }

    static void applyEvaporation(double[][] pher, double rho) {
        for (int i = 0; i < pher.length; i++) 
            for (int j = 0; j < pher.length; j++) 
                pher[i][j] *= (1.0 - rho);
    }

    static double[][] computeTransitionProbabilities(double[][] pher, double[][] cost, double alpha, double beta) {
        int n = pher.length;
        double[][] prob = new double[n][n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            double[] w = new double[n];
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double tau = Math.pow(pher[i][j], alpha);
                double eta = (cost[i][j] > 0) ? Math.pow(1.0 / cost[i][j], beta) : 0.0;
                w[j] = tau * eta;
                sum += w[j];
            }
            if (sum <= 0.0) {
                for (int j = 0; j < n; j++) if (i != j) prob[i][j] = 1.0/(n-1);
            } else {
                for (int j = 0; j < n; j++) prob[i][j] = w[j] / sum;
            }
        }
        return prob;
    }

    static int[] buildTourFromPheromone(double[][] pher, double[][] cost, int start, double alpha, double beta) {
        boolean[] visited = new boolean[N];
        int[] tour = new int[N+1];
        int cur = start;
        tour[0] = cur;
        visited[cur] = true;
        
        for (int step = 1; step < N; step++) {
            double[] probs = computeProbsFromCurrent(pher, cost, cur, visited, alpha, beta);
            int next = rouletteSelect(probs);
            if (next == -1) {
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        next = j;
                        break;
                    }
                }
            }
            tour[step] = next;
            visited[next] = true;
            cur = next;
        }
        tour[N] = start;
        return tour;
    }

    static double[] computeProbsFromCurrent(double[][] pher, double[][] cost, int current, boolean[] visited, 
                                          double alpha, double beta) {
        double[] probs = new double[N];
        double sum = 0.0;
        double[] weights = new double[N];
        
        for (int j = 0; j < N; j++) {
            if (!visited[j]) {
                double tau = Math.pow(pher[current][j], alpha);
                double eta = (cost[current][j] > 0) ? Math.pow(1.0 / cost[current][j], beta) : 0.0;
                weights[j] = tau * eta;
                sum += weights[j];
            }
        }
        
        if (sum <= 0.0) {
            int cnt = 0;
            for (int j = 0; j < N; j++) if (!visited[j]) cnt++;
            for (int j = 0; j < N; j++) {
                if (!visited[j]) probs[j] = 1.0 / cnt;
            }
        } else {
            for (int j = 0; j < N; j++) {
                probs[j] = weights[j] / sum;
            }
        }
        return probs;
    }

    static int rouletteSelect(double[] probs) {
        double sum = 0.0;
        for (double p : probs) sum += p;
        if (sum <= 0.0) return -1;
        double r = RAND.nextDouble() * sum;
        double acc = 0.0;
        for (int i = 0; i < probs.length; i++) {
            acc += probs[i];
            if (acc >= r) return i;
        }
        return -1;
    }

    // ---- UI components (unchanged) ----
    class NetworkPanel extends JPanel {
        List<MovingAnt> movingAnts = Collections.synchronizedList(new ArrayList<>());
        
        NetworkPanel() {
            setPreferredSize(new Dimension(WIDTH-350, HEIGHT));
            setBackground(Color.WHITE);
        }

        void animateAntAlongPath(int[] path, Color color, int totalMs) {
            MovingAnt ant = new MovingAnt(path, color, totalMs);
            movingAnts.add(ant);
            ant.start();
            while (!ant.finished) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
            movingAnts.remove(ant);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D)g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Scale edge thickness by pheromone levels
            double maxP = 1e-9;
            for (int i = 0; i < N; i++) 
                for (int j = i + 1; j < N; j++) 
                    maxP = Math.max(maxP, pher[i][j]);

            // Draw edges with pheromone-based thickness
            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    Point2D.Double a = NODE_POS[i], b = NODE_POS[j];
                    double p = pher[i][j];
                    float thickness = 1.5f + (float)((p/maxP) * 20.0);
                    if (Double.isNaN(thickness) || thickness < 1.5f) thickness = 1.5f;
                    
                    g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(50, 90, 170, 200));
                    g.draw(new Line2D.Double(a, b));
                }
            }

            // Draw nodes
            for (int i = 0; i < N; i++) {
                Point2D.Double p = NODE_POS[i];
                int r = 25;
                g.setColor(new Color(220, 220, 220));
                g.fillOval((int)p.x - r, (int)p.y - r, 2*r, 2*r);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int)p.x - r, (int)p.y - r, 2*r, 2*r);
                g.setFont(g.getFont().deriveFont(16f).deriveFont(Font.BOLD));
                g.drawString(String.valueOf(i), (int)p.x - 8, (int)p.y + 6);
            }

            // Draw moving ants
            synchronized (movingAnts) {
                for (MovingAnt ma : movingAnts) {
                    Point2D.Double pos = ma.currentPosition();
                    if (pos == null) continue;
                    int rad = 14;
                    g.setColor(ma.color);
                    g.fillOval((int)pos.x - rad, (int)pos.y - rad, 2*rad, 2*rad);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(2));
                    g.drawOval((int)pos.x - rad, (int)pos.y - rad, 2*rad, 2*rad);
                }
            }

            // Draw pheromone values
            g.setFont(g.getFont().deriveFont(9f));
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    Point2D.Double a = NODE_POS[i], b = NODE_POS[j];
                    double midx = (a.x + b.x) / 2.0;
                    double midy = (a.y + b.y) / 2.0 + 3;
                    g.drawString(String.format("%.2f", pher[i][j]), (int)midx + 2, (int)midy);
                }
            }
        }

        class MovingAnt implements Runnable {
            int[] path;
            Color color;
            long totalMs;
            volatile boolean finished = false;
            int edgeIndex = 0;
            long edgeStartTime;
            Thread thread;

            MovingAnt(int[] path, Color color, int totalMs) {
                this.path = Arrays.copyOf(path, path.length);
                this.color = color;
                this.totalMs = Math.max(200, totalMs);
            }

            Point2D.Double currentPosition() {
                if (edgeIndex >= path.length - 1) {
                    return NODE_POS[path[path.length - 1]];
                }
                Point2D.Double a = NODE_POS[path[edgeIndex]];
                Point2D.Double b = NODE_POS[path[edgeIndex + 1]];
                long now = System.currentTimeMillis();
                long elapsed = now - edgeStartTime;
                long thisEdgeMs = computeEdgeMs(edgeIndex);
                double t = Math.min(1.0, (double)elapsed / thisEdgeMs);
                double x = a.x + (b.x - a.x) * t;
                double y = a.y + (b.y - a.y) * t;
                return new Point2D.Double(x, y);
            }

            long computeEdgeMs(int idx) {
                double totalCost = 0.0;
                for (int i = 0; i < path.length - 1; i++) 
                    totalCost += COST[path[i]][path[i + 1]];
                if (totalCost <= 0.0) 
                    return Math.max(200, totalMs / Math.max(1, path.length - 1));
                double edgeCost = COST[path[idx]][path[idx + 1]];
                double frac = edgeCost / totalCost;
                return Math.max(200, (long)(frac * totalMs));
            }

            void start() {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.start();
            }

            @Override
            public void run() {
                edgeIndex = 0;
                edgeStartTime = System.currentTimeMillis();
                while (edgeIndex < path.length - 1 && !finished) {
                    long thisEdgeMs = computeEdgeMs(edgeIndex);
                    long target = edgeStartTime + thisEdgeMs;
                    while (System.currentTimeMillis() < target && !finished) {
                        SwingUtilities.invokeLater(netPanel::repaint);
                        try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                    }
                    edgeIndex++;
                    edgeStartTime = System.currentTimeMillis();
                }
                finished = true;
                SwingUtilities.invokeLater(netPanel::repaint);
            }
        }
    }

    // Utility methods (unchanged)
    void logIterationStats(List<Double> lengths) {
        double avg = lengths.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = lengths.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        log(String.format("\nIteration statistics (40 ants):"));
        log(String.format("Average length: %.4f", avg));
        log(String.format("Best length: %.4f", min));
        log(String.format("Improvement over initial ants: %.4f", min - lengths.get(0)));
    }

    void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    void printPheromoneToLog(double[][] p) {
        log("Pheromone matrix:");
        for (int i = 0; i < p.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < p[i].length; j++) {
                sb.append(String.format("%10.6f", p[i][j]));
            }
            log(sb.toString());
        }
    }

    static String pathToString(int[] p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            sb.append(p[i]);
            if (i < p.length - 1) sb.append("->");
        }
        return sb.toString();
    }

    static String tourToString(int[] tour) { 
        return pathToString(tour); 
    }

    static double[][] copyMatrix(double[][] m) {
        double[][] r = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++) 
            r[i] = Arrays.copyOf(m[i], m[i].length);
        return r;
    }

    static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    class ProbPanel extends JPanel {
        double[][] transMatrix = null;
        String title = "Transition Probabilities";

        ProbPanel() {
            setPreferredSize(new Dimension(350, HEIGHT * 2 / 3));
            setBackground(new Color(245, 245, 245));
        }

        void showTransitionMatrix(double[][] tm, String customTitle) {
            this.transMatrix = copyMatrix(tm);
            this.title = customTitle;
            SwingUtilities.invokeLater(this::repaint);
        }

        void showTransitionMatrix(double[][] tm) {
            showTransitionMatrix(tm, "Transition Probabilities");
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D)g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            
            if (transMatrix == null) return;
            
            int w = getWidth(), h = getHeight();
            int pad = 10;
            int y = pad;
            g.setColor(Color.BLACK);
            g.drawString(title, pad, y);
            y += 25;

            int cellW = Math.max(45, (w - 3 * pad) / N);
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    String s = (i == j) ? " - " : String.format("%.3f", transMatrix[i][j]);
                    g.drawString(s, pad + j * cellW + 2, y + i * 16);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AntColonyVisualizer_adv());
    }
}