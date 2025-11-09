import java.util.Scanner;

public class FuzzySetPlotPairwise {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of elements in Set A : ");
        int nA = sc.nextInt();
        double[] A = new double[nA], muA = new double[nA];
        if (nA > 0) System.out.println("Enter A entries: <value> <membership 0..1>");
        for (int i = 0; i < nA; i++) { A[i] = sc.nextDouble(); muA[i] = sc.nextDouble(); }

        System.out.print("Enter number of elements in Set B : ");
        int nB = sc.nextInt();
        double[] B = new double[nB], muB = new double[nB];
        if (nB > 0) System.out.println("Enter B entries: <value> <membership 0..1>");
        for (int i = 0; i < nB; i++) { B[i] = sc.nextDouble(); muB[i] = sc.nextDouble(); }

        sc.close();

        int m = Math.max(nA, nB);
        double[] unionVal = new double[m], unionMu = new double[m];
        double[] interVal = new double[m], interMu = new double[m];
        double[] negA = new double[nA], negB = new double[nB];

        // compute negations for display
        for (int i = 0; i < nA; i++) negA[i] = 1.0 - muA[i];
        for (int i = 0; i < nB; i++) negB[i] = 1.0 - muB[i];

        // pairwise logic
        for (int i = 0; i < m; i++) {
            boolean hasA = i < nA;
            boolean hasB = i < nB;

            if (hasA && hasB) {
                double aV = A[i], bV = B[i], aM = muA[i], bM = muB[i];

                // union
                if (aM >= bM) unionVal[i] = aV; else unionVal[i] = bV;
                unionMu[i] = Math.max(aM, bM);

                // intersection
                if (aM <= bM) interVal[i] = aV; else interVal[i] = bV;
                interMu[i] = Math.min(aM, bM);
            } else if (hasA) {
                unionVal[i] = interVal[i] = A[i];
                unionMu[i] = interMu[i] = muA[i];
            } else { // hasB only
                unionVal[i] = interVal[i] = B[i];
                unionMu[i] = interMu[i] = muB[i];
            }
        }

        // Console output (includes complement columns ¬A and ¬B)
        String hdr = String.format("%3s | %12s | %12s | %16s | %16s | %8s | %8s",
                "i", "A (val,μ)", "B (val,μ)", "Union (val,μ)", "Inter (val,μ)", "¬A μ", "¬B μ");
        System.out.println("\n" + hdr);
        System.out.println("-".repeat(hdr.length()));
        for (int i = 0; i < m; i++) {
            String aStr = (i < nA) ? String.format("%6.2f, %.2f", A[i], muA[i]) : "   -,   - ";
            String bStr = (i < nB) ? String.format("%6.2f, %.2f", B[i], muB[i]) : "   -,   - ";
            String uStr = String.format("%6.2f, %.2f", unionVal[i], unionMu[i]);
            String rStr = String.format("%6.2f, %.2f", interVal[i], interMu[i]);
            String compAStr = (i < nA) ? String.format("%.2f", negA[i]) : "  -  ";
            String compBStr = (i < nB) ? String.format("%.2f", negB[i]) : "  -  ";
            System.out.printf("%3d | %12s | %12s | %16s | %16s | %8s | %8s%n",
                    i, aStr, bStr, uStr, rStr, compAStr, compBStr);
        }

       
    }
}
