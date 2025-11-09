import java.util.Random;
import java.util.Scanner;

public class GWORosenbrock {
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
        System.out.printf("  DIMENSIONS   = %d%n", DIMENSIONS);
        System.out.printf("  MIN_BOUND    = %.2f%n", MIN_BOUND);
        System.out.printf("  MAX_BOUND    = %.2f%n", MAX_BOUND);
        System.out.printf("  DEFAULT_WOLF_COUNT = %d%n", DEFAULT_WOLF_COUNT);
        System.out.printf("  DEFAULT_MAX_ITER   = %d%n", DEFAULT_MAX_ITER);
        System.out.println("Chosen (defined):");
        System.out.printf("  WOLF_COUNT   = %d%n", WOLF_COUNT);
        System.out.printf("  MAX_ITER     = %d%n", MAX_ITER);
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

            if (iter % Math.max(1, MAX_ITER / 10) == 0) {
                System.out.printf("Iter %4d / %d : Best = %.8f at (%.6f, %.6f)%n",
                        iter, MAX_ITER, alphaScore, alpha[0], alpha[1]);
            }
        }

        System.out.printf("%nFinal Best: f(%.6f, %.6f) = %.10f%n",
                alpha[0], alpha[1], alphaScore);

        sc.close();
    }
}
