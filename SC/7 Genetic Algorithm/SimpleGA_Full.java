import java.util.Random;
import java.util.Scanner;

public class SimpleGA_Full {

    // ===== Predefined Settings =====
    // Selection: "roulette", "tournament", "rank", "steady", "canonical"
    static final String SELECTION_TYPE = "canonical";
    static final int TOURNAMENT_SIZE = 3;

    // Crossover: "single", "two", "uniform"
    static final String CROSSOVER_TYPE = "two";

    // Mutation: "bitflip", "swap", "inversion"
    static final String MUTATION_TYPE = "bitflip";

    // GA Parameters
    static final double MUTATION_RATE  = 0.01;
    static final double CROSSOVER_RATE = 0.8;
    static final int ELITISM = 1;
    static final Random RNG = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Simple Genetic Algorithm (Java) ===");
        System.out.print("Population size (e.g., 50): ");
        int POP_SIZE = sc.nextInt();

        System.out.print("Gene length in bits (e.g., 20): ");
        int GENE_LEN = sc.nextInt();

        System.out.print("Generations (e.g., 50): ");
        int GENERATIONS = sc.nextInt();

        // ---- Initialize population ----
        int[][] pop = new int[POP_SIZE][GENE_LEN];
        for (int i = 0; i < POP_SIZE; i++)
            for (int j = 0; j < GENE_LEN; j++)
                pop[i][j] = RNG.nextBoolean() ? 1 : 0;

        double[] fit = new double[POP_SIZE];
        int[][] nextPop = new int[POP_SIZE][GENE_LEN];

        // ---- GA loop ----
        for (int gen = 1; gen <= GENERATIONS; gen++) {
            // Evaluate fitness (OneMax)
            double best = -1, sum = 0;
            int bestIdx = 0;
            for (int i = 0; i < POP_SIZE; i++) {
                double f = fitnessOneMax(pop[i]);
                fit[i] = f; sum += f;
                if (f > best) { best = f; bestIdx = i; }
            }
            double avg = sum / POP_SIZE;

            System.out.printf("Gen %d | Best: %.2f | Avg: %.2f  [%s | %s | %s]%n",
                    gen, best, avg, SELECTION_TYPE, CROSSOVER_TYPE, MUTATION_TYPE);

            int[] ord = sortByFitnessDesc(fit);
            for (int k = 0; k < Math.min(3, POP_SIZE); k++) {
                int idx = ord[k];
                System.out.printf("  #%d fit=%.2f chrom=%s%n", idx, fit[idx], bitsToString(pop[idx]));
            }

            // Elitism
            int np = 0;
            for (int e = 0; e < ELITISM && np < POP_SIZE; e++, np++) {
                copyChrom(pop[ord[e]], nextPop[np]);
            }

            // Offspring
            while (np < POP_SIZE) {
                int p1 = selectParent(pop, fit, sum, avg, SELECTION_TYPE, TOURNAMENT_SIZE);
                int p2 = selectParent(pop, fit, sum, avg, SELECTION_TYPE, TOURNAMENT_SIZE);

                int[] c1 = new int[GENE_LEN];
                int[] c2 = new int[GENE_LEN];

                // Crossover
                if (RNG.nextDouble() < CROSSOVER_RATE && GENE_LEN > 1) {
                    if ("single".equals(CROSSOVER_TYPE))
                        singlePointCrossover(pop[p1], pop[p2], c1, c2);
                    else if ("two".equals(CROSSOVER_TYPE))
                        twoPointCrossover(pop[p1], pop[p2], c1, c2);
                    else
                        uniformCrossover(pop[p1], pop[p2], c1, c2);
                } else {
                    copyChrom(pop[p1], c1);
                    copyChrom(pop[p2], c2);
                }

                // Mutation
                mutate(c1, MUTATION_TYPE, MUTATION_RATE);
                mutate(c2, MUTATION_TYPE, MUTATION_RATE);

                copyChrom(c1, nextPop[np++]);
                if (np < POP_SIZE) copyChrom(c2, nextPop[np++]);
            }

            int[][] tmp = pop; pop = nextPop; nextPop = tmp;
        }

        // ---- Final Result ----
        double best = -1; int bestIdx = 0;
        for (int i = 0; i < POP_SIZE; i++) {
            double f = fitnessOneMax(pop[i]);
            if (f > best) { best = f; bestIdx = i; }
        }

        System.out.println("\n=== Final Result ===");
        System.out.println("Best Fitness: " + best);
        System.out.println("Best Chromosome: " + bitsToString(pop[bestIdx]));

        System.out.println("\n=== Final Population ===");
        for (int i = 0; i < POP_SIZE; i++) {
            double f = fitnessOneMax(pop[i]);
            System.out.printf("%3d | %6.2f | %s%n", i, f, bitsToString(pop[i]));
        }

        sc.close();
    }

    // ===== Fitness =====
    static double fitnessOneMax(int[] chrom) {
        int s = 0; for (int bit : chrom) s += bit; return s;
    }

    // ===== Selection Dispatcher =====
    static int selectParent(int[][] pop, double[] fit, double sum, double avg, String type, int tSize) {
        switch (type) {
            case "tournament": return tournamentSelect(fit, tSize);
            case "rank":       return rankSelect(fit);
            case "steady":     return steadySelect(fit);
            case "canonical":  return canonicalSelect(fit, avg);
            default:           return rouletteSelect(fit, sum);
        }
    }

    // Roulette
    static int rouletteSelect(double[] fit, double sum) {
        if (sum <= 0) return RNG.nextInt(fit.length);
        double r = RNG.nextDouble() * sum, acc = 0;
        for (int i = 0; i < fit.length; i++) { acc += fit[i]; if (acc >= r) return i; }
        return fit.length - 1;
    }

    // Tournament
    static int tournamentSelect(double[] fit, int tSize) {
        int n = fit.length;
        int best = RNG.nextInt(n);
        for (int k = 1; k < tSize; k++) {
            int idx = RNG.nextInt(n);
            if (fit[idx] > fit[best]) best = idx;
        }
        return best;
    }

    // Rank selection
    static int rankSelect(double[] fit) {
        int n = fit.length;
        int[] ord = sortByFitnessAsc(fit);
        int total = n * (n + 1) / 2;
        int r = RNG.nextInt(total) + 1, acc = 0;
        for (int i = 0; i < n; i++) {
            acc += (i + 1);
            if (acc >= r) return ord[i];
        }
        return ord[n - 1];
    }

    // Steady-state (top half biased)
    static int steadySelect(double[] fit) {
        int[] ord = sortByFitnessDesc(fit);
        int half = Math.max(1, fit.length / 2);
        return ord[RNG.nextInt(half)];
    }

    // Canonical selection (fitness / avg)
    static int canonicalSelect(double[] fit, double avg) {
        int n = fit.length;
        if (avg <= 0) return RNG.nextInt(n);
        double[] probs = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            probs[i] = fit[i] / avg;
            sum += probs[i];
        }
        double r = RNG.nextDouble() * sum, acc = 0;
        for (int i = 0; i < n; i++) { acc += probs[i]; if (acc >= r) return i; }
        return RNG.nextInt(n);
    }

    // ===== Crossover =====
    static void singlePointCrossover(int[] a, int[] b, int[] c1, int[] c2) {
        int L = a.length, cut = 1 + RNG.nextInt(L - 1);
        for (int i = 0; i < cut; i++) { c1[i] = a[i]; c2[i] = b[i]; }
        for (int i = cut; i < L; i++) { c1[i] = b[i]; c2[i] = a[i]; }
    }

    static void twoPointCrossover(int[] a, int[] b, int[] c1, int[] c2) {
        int L = a.length;
        int i = RNG.nextInt(L), j = RNG.nextInt(L);
        if (i > j) { int t = i; i = j; j = t; }
        for (int k = 0; k < i; k++) { c1[k] = a[k]; c2[k] = b[k]; }
        for (int k = i; k < j; k++) { c1[k] = b[k]; c2[k] = a[k]; }
        for (int k = j; k < L; k++) { c1[k] = a[k]; c2[k] = b[k]; }
    }

    static void uniformCrossover(int[] a, int[] b, int[] c1, int[] c2) {
        for (int i = 0; i < a.length; i++) {
            if (RNG.nextBoolean()) { c1[i] = a[i]; c2[i] = b[i]; }
            else { c1[i] = b[i]; c2[i] = a[i]; }
        }
    }

    // ===== Mutation =====
    static void mutate(int[] chrom, String type, double rate) {
        if ("swap".equals(type)) { swapMutation(chrom, rate); return; }
        if ("inversion".equals(type)) { inversionMutation(chrom, rate); return; }
        bitFlipMutation(chrom, rate);
    }

    static void bitFlipMutation(int[] chrom, double rate) {
        for (int i = 0; i < chrom.length; i++)
            if (RNG.nextDouble() < rate) chrom[i] = 1 - chrom[i];
    }

    static void swapMutation(int[] chrom, double rate) {
        if (RNG.nextDouble() < rate && chrom.length > 1) {
            int i = RNG.nextInt(chrom.length);
            int j = RNG.nextInt(chrom.length);
            int t = chrom[i]; chrom[i] = chrom[j]; chrom[j] = t;
        }
    }

    static void inversionMutation(int[] chrom, double rate) {
        if (RNG.nextDouble() < rate && chrom.length > 2) {
            int i = RNG.nextInt(chrom.length);
            int j = RNG.nextInt(chrom.length);
            if (i > j) { int t = i; i = j; j = t; }
            while (i < j) {
                int t2 = chrom[i]; chrom[i] = chrom[j]; chrom[j] = t2;
                i++; j--;
            }
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
