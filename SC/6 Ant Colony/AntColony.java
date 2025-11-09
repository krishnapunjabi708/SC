import java.util.*;
import java.util.List;

public class AntColony {

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

    // pheromone matrix (symmetric)
    double[][] pher;

    public AntColony() {
        pher = new double[N][N];
        runScenario();
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
            addPheromoneFromPath(pher, path, L);
        }

        log("\n=== PHEROMONE MATRIX AFTER 3rd ANT (before evap) ===");
        printMatrix(pher, "Pheromone");

        // Apply evaporation (RHO=0, no change) — no output printed
        applyEvaporation(pher, RHO);

        // Show transition probabilities after 3 ants
        double[][] probAfter3 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        printMatrix(probAfter3, "Transition Probabilities (After 3 ants)");

        // 2) Build and run 4th ant using roulette selection
        log("\n=== Building 4th ANT using roulette-wheel (Random(12345)) ===");
        int[] ant4 = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
        double L4 = tourLength(ant4);
        log(String.format("4th ant tour: %s   length = %.6f", tourToString(ant4), L4));
        double deposit4 = (L4 > 0.0) ? (Q / L4) : 0.0;
        addPheromoneFromAnt(pher, ant4, L4);
        log(String.format("4th ant deposit = %.6f", deposit4));

        log("\n=== PHEROMONE MATRIX AFTER 4th ANT DEPOSIT ===");
        printMatrix(pher, "Pheromone");

        // Transition probability matrix AFTER 4th ant
        double[][] probAfter4 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        printMatrix(probAfter4, "Transition Probabilities (After 4th ant deposit)");

        // 3) Build and run 5th ant
        log("\n=== Building 5th ANT using roulette-wheel (pheromones from 4th ant) ===");
        int[] ant5 = buildTourFromPheromone(pher, COST, 0, ALPHA, BETA);
        double L5 = tourLength(ant5);
        log(String.format("5th ant tour: %s   length = %.6f", tourToString(ant5), L5));
        double deposit5 = (L5 > 0.0) ? (Q / L5) : 0.0;
        addPheromoneFromAnt(pher, ant5, L5);
        log(String.format("5th ant deposit = %.6f", deposit5));

        log("\n=== PHEROMONE MATRIX CHANGED DUE TO 5th ANT DEPOSIT ===");
        printMatrix(pher, "Pheromone");

        // Transition probability matrix AFTER 5th ant
        double[][] probAfter5 = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        printMatrix(probAfter5, "Transition Probabilities (After 5th ant deposit)");

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
                log(String.format("\n--- PHEROMONE AFTER ITERATION %d ---", iter+1));
                printMatrix(pher, "Pheromone");
            }
        }

        log("\n=== 40 ITERATIONS COMPLETE ===");
        log(String.format("Best solution from iterations #%d: length %.4f", bestAntIndex+6, bestLength));

        // Final pheromone matrix
        log("\n=== FINAL PHEROMONE MATRIX AFTER 40 ITERATIONS ===");
        printMatrix(pher, "Pheromone");

        // Final transition probabilities
        double[][] finalProb = computeTransitionProbabilities(pher, COST, ALPHA, BETA);
        printMatrix(finalProb, "Final Transition Probabilities");

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
                for (int j = 0; j < n; j++) prob[i][j] = (i == j) ? 0.0 : w[j] / sum;
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
                    if (!visited[j]) { next = j; break; }
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

    // ---------- Console helpers ----------
    void logIterationStats(List<Double> lengths) {
        double avg = lengths.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = lengths.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        log(String.format("\nIteration statistics (40 ants):"));
        log(String.format("Average length: %.4f", avg));
        log(String.format("Best length: %.4f", min));
        log(String.format("Improvement over initial ants: %.4f", min - lengths.get(0)));
    }

    void log(String s) {
        System.out.println(s);
    }

    void printMatrix(double[][] m, String title) {
        System.out.println(title + ":");
        for (int i = 0; i < m.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < m[i].length; j++) {
                sb.append(String.format("%10.6f", m[i][j]));
            }
            System.out.println(sb.toString());
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

    public static void main(String[] args) {
        new AntColony(); // console-only
    }
}
