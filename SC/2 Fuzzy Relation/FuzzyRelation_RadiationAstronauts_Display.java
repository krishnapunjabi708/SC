import java.text.DecimalFormat;
import java.util.Random;
import java.util.Scanner;

class FuzzyRelation_RadiationAstronauts_Display {
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final Random RNG = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Fuzzy Relation: Radiation Level  Astronaut Safety (Console) ===");

        // sizes
        System.out.print("Enter number of astronauts: ");
        int n = readIntPositive(sc);
        System.out.print("Enter number of radiation zones: ");
        int m = readIntPositive(sc);
        sc.nextLine();

        String[] astronauts = new String[n];
        for (int i = 0; i < n; i++) {
            System.out.print("Enter astronaut " + (i + 1) + " name (or press Enter for default): ");
            String s = sc.nextLine().trim();
            astronauts[i] = s.isEmpty() ? ("A" + (i + 1)) : s;
        }

        String[] zones = new String[m];
        for (int j = 0; j < m; j++) {
            System.out.print("Enter zone " + (j + 1) + " name (or press Enter for default): ");
            String s = sc.nextLine().trim();
            zones[j] = s.isEmpty() ? ("Z" + (j + 1)) : s;
        }

        System.out.println("Choose entry mode for membership matrices:");
        System.out.println("  1) Manual entry");
        System.out.println("  2) Auto-fill (random values 0.0 - 1.0)");
        System.out.print("Select 1 or 2: ");
        int mode = readChoice(sc, 1, 2);

        double[][] relationA = new double[n][m]; // radiation
        double[][] relationB = new double[n][m]; // safety

        if (mode == 1) {
            System.out.println("Enter membership values for RELATION A (Radiation level: 0.0 - 1.0)");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    relationA[i][j] = readDoubleRange(sc, "uA(" + astronauts[i] + ", " + zones[j] + "): ", 0.0, 1.0);
                }
            }
            System.out.println("Enter membership values for RELATION B (Astronaut safety: 0.0 - 1.0)");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    relationB[i][j] = readDoubleRange(sc, "uB(" + astronauts[i] + ", " + zones[j] + "): ", 0.0, 1.0);
                }
            }
        } else {
            System.out.println("Auto-filling matrices with random values in [0,1]...");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    relationA[i][j] = RNG.nextDouble();
                    relationB[i][j] = RNG.nextDouble();
                }
            }
            System.out.println("Auto-filled RELATION A (Radiation):");
            printMatrix(astronauts, zones, relationA);
            System.out.println("Auto-filled RELATION B (Safety):");
            printMatrix(astronauts, zones, relationB);
        }

        // compute composition
        double[][] composition = maxMinCompose_A_with_Btransposed(relationA, relationB);

        System.out.println("MAX-MIN COMPOSITION (A ∘ Bᵀ) -> indirect relation Astronauts x Astronauts");
        printMatrixSquare(astronauts, composition);

        sc.close();
    }

    // ---------- Utilities (unchanged) ----------
    private static int readIntPositive(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a valid integer: ");
            sc.next();
        }
        int x = sc.nextInt();
        while (x < 1) {
            System.out.print("Please enter a positive integer (>=1): ");
            while (!sc.hasNextInt()) { System.out.print("Please enter a valid integer: "); sc.next(); }
            x = sc.nextInt();
        }
        return x;
    }

    private static int readChoice(Scanner sc, int lo, int hi) {
        while (!sc.hasNextInt()) { System.out.print("Please enter a number: "); sc.next(); }
        int x = sc.nextInt();
        while (x < lo || x > hi) {
            System.out.print("Please enter a valid choice (" + lo + "-" + hi + "): ");
            while (!sc.hasNextInt()) { System.out.print("Please enter a number: "); sc.next(); }
            x = sc.nextInt();
        }
        return x;
    }

    private static double readDoubleRange(Scanner sc, String prompt, double lo, double hi) {
        System.out.print(prompt);
        while (true) {
            if (!sc.hasNext()) { sc.next(); System.out.print(prompt); continue; }
            if (!sc.hasNextDouble()) { sc.next(); System.out.print("Enter numeric value between " + lo + " and " + hi + ": "); continue; }
            double val = sc.nextDouble();
            if (val < lo || val > hi) { System.out.print("Value out of range. Enter value between " + lo + " and " + hi + ": "); continue; }
            return val;
        }
    }

    private static void printMatrix(String[] rows, String[] cols, double[][] matrix) {
        final int width = 12;
        System.out.printf("%-" + width + "s", "");
        for (String c : cols) {
            String h = c.length() > width - 1 ? c.substring(0, width - 2) + "…" : c;
            System.out.printf("%-" + width + "s", h);
        }
        System.out.println();
        for (int i = 0; i < rows.length; i++) {
            String rn = rows[i].length() > width - 1 ? rows[i].substring(0, width - 2) + "…" : rows[i];
            System.out.printf("%-" + width + "s", rn);
            for (int j = 0; j < cols.length; j++) {
                System.out.printf("%-" + width + "s", DF.format(matrix[i][j]));
            }
            System.out.println();
        }
    }

    private static void printMatrixSquare(String[] labels, double[][] matrix) {
        final int width = 12;
        System.out.printf("%-" + width + "s", "");
        for (String h : labels) {
            String head = h.length() > width - 1 ? h.substring(0, width - 2) + "…" : h;
            System.out.printf("%-" + width + "s", head);
        }
        System.out.println();
        for (int i = 0; i < labels.length; i++) {
            String rn = labels[i].length() > width - 1 ? labels[i].substring(0, width - 2) + "…" : labels[i];
            System.out.printf("%-" + width + "s", rn);
            for (int j = 0; j < labels.length; j++) {
                System.out.printf("%-" + width + "s", DF.format(matrix[i][j]));
            }
            System.out.println();
        }
    }

    private static double[][] maxMinCompose_A_with_Btransposed(double[][] A, double[][] B) {
        int n = A.length;
        int m = A[0].length;
        double[][] R = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                double maxOverJ = 0.0;
                for (int j = 0; j < m; j++) {
                    double minVal = Math.min(A[i][j], B[k][j]);
                    if (minVal > maxOverJ) maxOverJ = minVal;
                }
                R[i][k] = maxOverJ;
            }
        }
        return R;
    }
}
