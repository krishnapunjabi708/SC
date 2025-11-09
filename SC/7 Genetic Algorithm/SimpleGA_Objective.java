import java.util.Random;
import java.util.Scanner;

public class SimpleGA_Objective {

    // ===== GA constants (predefined) =====
    static final double MUTATION_RATE  = 0.01;  // per-bit for bitflip; per-chromosome for others
    static final double CROSSOVER_RATE = 0.80;  // probability that crossover happens
    static final int ELITISM = 1;               // carry best N
    static final int TOURNAMENT_SIZE = 3;       // for tournament selection
    static final double GAUSSIAN_SIGMA = 0.05;  // for gaussian mutation on x in [0,1]
    static final Random RNG = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Simple Genetic Algorithm (Java) ===");
        System.out.println("Objective: Maximizing f(x) = x * sin(10πx) + 1 for x ∈ [0, 1]\n");

        System.out.print("Population size (e.g., 50): ");
        int POP_SIZE = sc.nextInt();

        System.out.print("Gene length in bits (e.g., 20): ");
        int GENE_LEN = sc.nextInt();

        System.out.print("Generations (e.g., 50): ");
        int GENERATIONS = sc.nextInt();

        // ---- Menus (simple & numbered) ----
        System.out.println("\nSelection Methods:");
        System.out.println("  1) Roulette   2) Tournament   3) Rank   4) Random");
        System.out.print("Choose (1-4): ");
        int selChoice = sc.nextInt();
        String SELECTION = (selChoice==2) ? "tournament" :
                           (selChoice==3) ? "rank" :
                           (selChoice==4) ? "random" : "roulette";

        System.out.println("\nCrossover Methods:");
        System.out.println("  1) Single-point   2) Arithmetic   3) Uniform   4) Two-point");
        System.out.print("Choose (1-4): ");
        int cxChoice = sc.nextInt();
        String CROSSOVER = (cxChoice==2) ? "arithmetic" :
                           (cxChoice==3) ? "uniform" :
                           (cxChoice==4) ? "two" : "single";

        System.out.println("\nMutation Types:");
        System.out.println("  1) Gaussian   2) Random   3) Bitflip   4) Swap   5) Scramble   6) Inversion");
        System.out.print("Choose (1-6): ");
        int mutChoice = sc.nextInt();
        String MUTATION = (mutChoice==2) ? "random" :
                          (mutChoice==3) ? "bitflip" :
                          (mutChoice==4) ? "swap" :
                          (mutChoice==5) ? "scramble" :
                          (mutChoice==6) ? "inversion" : "gaussian";

        // ---- Initialize population (binary) ----
        int[][] pop = new int[POP_SIZE][GENE_LEN];
        for (int i = 0; i < POP_SIZE; i++)
            for (int j = 0; j < GENE_LEN; j++)
                pop[i][j] = RNG.nextBoolean() ? 1 : 0;

        double[] fit = new double[POP_SIZE];
        int[][] nextPop = new int[POP_SIZE][GENE_LEN];

        // ---- GA loop ----
        for (int gen = 1; gen <= GENERATIONS; gen++) {
            // Evaluate fitness
            double best = -1e9, sum = 0;
            int bestIdx = 0;
            for (int i = 0; i < POP_SIZE; i++) {
                double x = bitsToDouble(pop[i]);                 // decode to [0,1]
                double f = objective(x);                         // f(x) = x*sin(10πx)+1
                fit[i] = f; sum += f;
                if (f > best) { best = f; bestIdx = i; }
            }
            double avg = sum / POP_SIZE;

            System.out.printf("\nGen %d | Best: %.6f | Avg: %.6f | Sel=%s | Cx=%s | Mut=%s%n",
                    gen, best, avg, SELECTION, CROSSOVER, MUTATION);

            // Show top 3 chromosomes
            int[] ord = sortByFitnessDesc(fit);
            for (int k = 0; k < Math.min(3, POP_SIZE); k++) {
                int idx = ord[k];
                double x = bitsToDouble(pop[idx]);
                System.out.printf("  #%d  f=%.6f  x=%.6f  chrom=%s%n", idx, fit[idx], x, bitsToString(pop[idx]));
            }

            // Elitism
            int np = 0;
            for (int e = 0; e < ELITISM && np < POP_SIZE; e++, np++)
                copyChrom(pop[ord[e]], nextPop[np]);

            // Offspring
            while (np < POP_SIZE) {
                int p1 = selectParent(fit, sum, SELECTION);
                int p2 = selectParent(fit, sum, SELECTION);

                int[] c1 = new int[GENE_LEN];
                int[] c2 = new int[GENE_LEN];

                // Crossover
                if (RNG.nextDouble() < CROSSOVER_RATE && GENE_LEN > 1) {
                    applyCrossover(pop[p1], pop[p2], c1, c2, CROSSOVER);
                } else {
                    copyChrom(pop[p1], c1);
                    copyChrom(pop[p2], c2);
                }

                // Mutation
                applyMutation(c1, MUTATION);
                applyMutation(c2, MUTATION);

                copyChrom(c1, nextPop[np++]);
                if (np < POP_SIZE) copyChrom(c2, nextPop[np++]);
            }

            // Swap
            int[][] tmp = pop; pop = nextPop; nextPop = tmp;
        }

        // ---- Final result ----
        double best = -1e9; int bestIdx = 0;
        for (int i = 0; i < POP_SIZE; i++) {
            double f = objective(bitsToDouble(pop[i]));
            if (f > best) { best = f; bestIdx = i; }
        }

        double xBest = bitsToDouble(pop[bestIdx]);
        System.out.println("\n=== Final Result ===");
        System.out.printf("Maximizing f(x) = x * sin(10πx) + 1 for x ∈ [0, 1]%n");
        System.out.printf("Best Fitness: %.6f%n", best);
        System.out.printf("Best x: %.6f%n", xBest);
        System.out.println("Best Chromosome: " + bitsToString(pop[bestIdx]));

        System.out.println("\n=== Final Population (index | f(x) | x | chromosome) ===");
        for (int i = 0; i < POP_SIZE; i++) {
            double x = bitsToDouble(pop[i]);
            double f = objective(x);
            System.out.printf("%3d | %.6f | %.6f | %s%n", i, f, x, bitsToString(pop[i]));
        }

        sc.close();
    }

    // ===== Objective =====
    static double objective(double x) {
        // f(x) = x * sin(10πx) + 1
        return x * Math.sin(10.0 * Math.PI * x) + 1.0;
    }

    // ===== Selection =====
    static int selectParent(double[] fit, double sum, String type) {
        switch (type) {
            case "tournament": return tournamentSelect(fit, TOURNAMENT_SIZE);
            case "rank":       return rankSelect(fit);
            case "random":     return RNG.nextInt(fit.length);
            default:           return rouletteSelect(fit, sum);
        }
    }

    static int rouletteSelect(double[] fit, double sum) {
        if (sum <= 0) return RNG.nextInt(fit.length);
        double r = RNG.nextDouble() * sum, acc = 0;
        for (int i = 0; i < fit.length; i++) { acc += fit[i]; if (acc >= r) return i; }
        return fit.length - 1;
    }

    static int tournamentSelect(double[] fit, int tSize) {
        int n = fit.length;
        int best = RNG.nextInt(n);
        for (int k = 1; k < tSize; k++) {
            int idx = RNG.nextInt(n);
            if (fit[idx] > fit[best]) best = idx;
        }
        return best;
    }

    static int rankSelect(double[] fit) {
        int n = fit.length;
        int[] ord = sortByFitnessAsc(fit); // worst..best
        int total = n * (n + 1) / 2;
        int r = RNG.nextInt(total) + 1, acc = 0;
        for (int i = 0; i < n; i++) {
            acc += (i + 1);
            if (acc >= r) return ord[i];
        }
        return ord[n - 1];
    }

    // ===== Crossover =====
    static void applyCrossover(int[] a, int[] b, int[] c1, int[] c2, String type) {
        int L = a.length;
        if ("arithmetic".equals(type)) {
            // decode -> average (α=0.5) -> encode
            double x1 = bitsToDouble(a), x2 = bitsToDouble(b);
            double y1 = 0.5 * x1 + 0.5 * x2;
            double y2 = 0.5 * x2 + 0.5 * x1; // same as y1; still encode twice
            doubleToBits(y1, c1);
            doubleToBits(y2, c2);
        } else if ("uniform".equals(type)) {
            for (int i = 0; i < L; i++) {
                if (RNG.nextBoolean()) { c1[i] = a[i]; c2[i] = b[i]; }
                else { c1[i] = b[i]; c2[i] = a[i]; }
            }
        } else if ("two".equals(type)) {
            int i = RNG.nextInt(L), j = RNG.nextInt(L);
            if (i > j) { int t = i; i = j; j = t; }
            for (int k = 0; k < i; k++) { c1[k] = a[k]; c2[k] = b[k]; }
            for (int k = i; k < j; k++) { c1[k] = b[k]; c2[k] = a[k]; }
            for (int k = j; k < L; k++) { c1[k] = a[k]; c2[k] = b[k]; }
        } else { // "single"
            int cut = 1 + RNG.nextInt(L - 1);
            for (int i = 0; i < cut; i++) { c1[i] = a[i]; c2[i] = b[i]; }
            for (int i = cut; i < L; i++) { c1[i] = b[i]; c2[i] = a[i]; }
        }
    }

    // ===== Mutation =====
    static void applyMutation(int[] chrom, String type) {
        switch (type) {
            case "bitflip":  bitFlipMutation(chrom, MUTATION_RATE); break;
            case "swap":     swapMutation(chrom, MUTATION_RATE); break;
            case "scramble": scrambleMutation(chrom, MUTATION_RATE); break;
            case "inversion":inversionMutation(chrom, MUTATION_RATE); break;
            case "random":   randomResetMutation(chrom, MUTATION_RATE); break;
            case "gaussian": gaussianOnXMutation(chrom, GAUSSIAN_SIGMA, MUTATION_RATE); break;
            default:         bitFlipMutation(chrom, MUTATION_RATE);
        }
    }

    // Bit-flip per gene
    static void bitFlipMutation(int[] chrom, double rate) {
        for (int i = 0; i < chrom.length; i++)
            if (RNG.nextDouble() < rate) chrom[i] = 1 - chrom[i];
    }

    // With prob 'rate', swap two random positions
    static void swapMutation(int[] chrom, double rate) {
        if (RNG.nextDouble() < rate && chrom.length > 1) {
            int i = RNG.nextInt(chrom.length), j = RNG.nextInt(chrom.length);
            int t = chrom[i]; chrom[i] = chrom[j]; chrom[j] = t;
        }
    }

    // With prob 'rate', choose a segment and shuffle it
    static void scrambleMutation(int[] chrom, double rate) {
        if (RNG.nextDouble() < rate && chrom.length > 2) {
            int i = RNG.nextInt(chrom.length), j = RNG.nextInt(chrom.length);
            if (i > j) { int t = i; i = j; j = t; }
            for (int k = i; k <= j; k++) {
                int r = i + RNG.nextInt(j - i + 1);
                int tmp = chrom[k]; chrom[k] = chrom[r]; chrom[r] = tmp;
            }
        }
    }

    // With prob 'rate', reverse a segment
    static void inversionMutation(int[] chrom, double rate) {
        if (RNG.nextDouble() < rate && chrom.length > 2) {
            int i = RNG.nextInt(chrom.length), j = RNG.nextInt(chrom.length);
            if (i > j) { int t = i; i = j; j = t; }
            while (i < j) {
                int t2 = chrom[i]; chrom[i] = chrom[j]; chrom[j] = t2;
                i++; j--;
            }
        }
    }

    // Random reset: with per-bit prob 'rate', set to 0/1 uniformly
    static void randomResetMutation(int[] chrom, double rate) {
        for (int i = 0; i < chrom.length; i++) {
            if (RNG.nextDouble() < rate) chrom[i] = RNG.nextBoolean() ? 1 : 0;
        }
    }

    // Gaussian on decoded real x (apply with probability 'rate'), then re-encode
    static void gaussianOnXMutation(int[] chrom, double sigma, double rate) {
        if (RNG.nextDouble() < rate) {
            double x = bitsToDouble(chrom);
            double y = x + RNG.nextGaussian() * sigma;
            if (y < 0) y = 0;
            if (y > 1) y = 1;
            doubleToBits(y, chrom);
        }
    }

    // ===== Encoding helpers: binary <-> [0,1] =====
    static double bitsToDouble(int[] chrom) {
        long val = 0;
        for (int i = 0; i < chrom.length; i++) { val = (val << 1) | (chrom[i] & 1); }
        long max = (chrom.length >= 63) ? Long.MAX_VALUE : ((1L << chrom.length) - 1L);
        if (max == 0) return 0.0;
        // Normalize to [0,1]
        double x = (double) val / (double) max;
        if (x < 0) x = 0;
        if (x > 1) x = 1;
        return x;
    }

    static void doubleToBits(double x, int[] out) {
        if (x < 0) x = 0; if (x > 1) x = 1;
        long max = (out.length >= 63) ? Long.MAX_VALUE : ((1L << out.length) - 1L);
        long val = (long) Math.round(x * max);
        for (int i = out.length - 1; i >= 0; i--) {
            out[i] = (int) (val & 1L);
            val >>= 1;
        }
    }

    // ===== Utilities =====
    static void copyChrom(int[] src, int[] dst) {
        for (int i = 0; i < src.length; i++) dst[i] = src[i];
    }

    static int[] sortByFitnessDesc(double[] fit) {
        int n = fit.length, idx[] = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = 0; i < n - 1; i++) {
            int best = i;
            for (int j = i + 1; j < n; j++)
                if (fit[idx[j]] > fit[idx[best]]) best = j;
            int t = idx[i]; idx[i] = idx[best]; idx[best] = t;
        }
        return idx;
    }

    static int[] sortByFitnessAsc(double[] fit) {
        int n = fit.length, idx[] = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = 0; i < n - 1; i++) {
            int worst = i;
            for (int j = i + 1; j < n; j++)
                if (fit[idx[j]] < fit[idx[worst]]) worst = j;
            int t = idx[i]; idx[i] = idx[worst]; idx[worst] = t;
        }
        return idx;
    }

    static String bitsToString(int[] b) {
        StringBuilder sb = new StringBuilder(b.length);
        for (int x : b) sb.append(x);
        return sb.toString();
    }
}
