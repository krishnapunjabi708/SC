import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

/**
 * Pairwise fuzzy set plotting (index-wise pairing).
 *
 * Pairing rule:
 * - iterate i = 0 .. max(nA,nB)-1
 * - if both exist:
 *     union μ = max(μA, μB); union value = value from side with larger μ (tie -> A)
 *     inter μ = min(μA, μB); inter value = value from side with smaller μ (tie -> A)
 * - if only one side exists: copy that element to union & intersection
 *
 * Console prints each index with values+memberships, and a Swing plot visualizes sets.
 */
public class Fuzzy_with_Graph {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of elements in Set A (measured brightness): ");
        int nA = sc.nextInt();
        double[] A = new double[nA], muA = new double[nA];
        if (nA > 0) System.out.println("Enter A entries: <value> <membership 0..1>");
        for (int i = 0; i < nA; i++) { A[i] = sc.nextDouble(); muA[i] = sc.nextDouble(); }

        System.out.print("Enter number of elements in Set B (camera exposure setting): ");
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

        // Console output (added complement columns ¬A and ¬B)
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
            System.out.printf("%3d | %12s | %12s | %16s | %16s | %8s | %8s%n", i, aStr, bStr, uStr, rStr, compAStr, compBStr);
        }

        // Launch plot
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pairwise Fuzzy Sets");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(940, 620);
            frame.setLocationRelativeTo(null);
            frame.add(new PlotPanel(A, muA, B, muB, unionVal, unionMu, interVal, interMu, negA, negB));
            frame.setVisible(true);
        });
    }

    // Plot panel (handles possible missing points gracefully)
    static class PlotPanel extends JPanel {
        private final double[] A, muA, B, muB, Uval, Umu, Ival, Imu, negA, negB;
        PlotPanel(double[] A, double[] muA, double[] B, double[] muB,
                  double[] Uval, double[] Umu, double[] Ival, double[] Imu,
                  double[] negA, double[] negB) {
            this.A = A; this.muA = muA; this.B = B; this.muB = muB;
            this.Uval = Uval; this.Umu = Umu; this.Ival = Ival; this.Imu = Imu;
            this.negA = negA; this.negB = negB;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), pad = 60;

            // determine x range from all available x-values (skip NaN)
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            collectRange(minXMaxRef(A, minX, maxX));
            collectRange(minXMaxRef(B, minX, maxX));
            collectRange(minXMaxRef(Uval, minX, maxX));
            // lambdas above won't work easily; do direct loops instead:

            minX = Double.POSITIVE_INFINITY; maxX = Double.NEGATIVE_INFINITY;
            for (double v : A) if (!Double.isNaN(v)) { minX = Math.min(minX, v); maxX = Math.max(maxX, v);}
            for (double v : B) if (!Double.isNaN(v)) { minX = Math.min(minX, v); maxX = Math.max(maxX, v);}
            for (double v : Uval) if (!Double.isNaN(v)) { minX = Math.min(minX, v); maxX = Math.max(maxX, v);}
            for (double v : Ival) if (!Double.isNaN(v)) { minX = Math.min(minX, v); maxX = Math.max(maxX, v);}

            if (minX == Double.POSITIVE_INFINITY) { minX = 0; maxX = 1; }
            if (minX == maxX) { minX -= 1; maxX += 1; }

            // axes
            g2.setColor(Color.BLACK);
            g2.drawLine(pad, h - pad, w - pad, h - pad);
            g2.drawLine(pad, pad, pad, h - pad);
            g2.drawString("Membership (μ)", 10, pad - 10);
            g2.drawString("Element value", w/2 - 40, h - 10);

            // y ticks
            for (int i = 0; i <= 10; i++) {
                int y = (int) (h - pad - (i / 10.0) * (h - 2 * pad));
                g2.drawLine(pad - 5, y, pad + 5, y);
                g2.drawString(String.format("%.1f", i / 10.0), pad - 40, y + 4);
            }

            // x ticks
            int ticks = Math.min(10, Math.max(2, (int) Math.ceil(maxX - minX)));
            for (int i = 0; i <= ticks; i++) {
                double xv = minX + i * (maxX - minX) / ticks;
                int x = toScreenX(xv, minX, maxX, w, pad);
                g2.drawLine(x, h - pad - 5, x, h - pad + 5);
                g2.drawString(String.format("%.1f", xv), x - 15, h - pad + 20);
            }

            // draw union and intersection using their chosen x positions
            drawLineWithStyle(g2, Uval, Umu, minX, maxX, w, h, pad,
                    new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8f, 6f}, 0),
                    Color.GREEN.darker());
            drawLineWithStyle(g2, Ival, Imu, minX, maxX, w, h, pad,
                    new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3f, 6f}, 0),
                    Color.MAGENTA.darker());

            // draw A and B (skip missing indices)
            drawLineWithMarkers(g2, A, muA, minX, maxX, w, h, pad, Color.RED, Marker.CIRCLE);
            drawLineWithMarkers(g2, B, muB, minX, maxX, w, h, pad, Color.BLUE, Marker.SQUARE);

            // draw negations
            drawLineWithMarkers(g2, A, negA, minX, maxX, w, h, pad, new Color(255, 140, 0), Marker.DIAMOND);
            drawLineWithMarkers(g2, B, negB, minX, maxX, w, h, pad, new Color(0, 128, 128), Marker.TRIANGLE);

            // legend
            int lx = w - pad - 240, ly = pad;
            int lh = 18;
            g2.setColor(Color.WHITE);
            g2.fillRect(lx - 8, ly - 18, 240, 140);
            g2.setColor(Color.BLACK);
            g2.drawRect(lx - 8, ly - 18, 240, 140);

            drawLegendItem(g2, lx, ly, "A", Color.RED, Marker.CIRCLE);
            drawLegendItem(g2, lx, ly + lh, "B", Color.BLUE, Marker.SQUARE);
            drawLegendLine(g2, lx, ly + 2 * lh, "Union (A ∪ B)", Color.GREEN.darker(),
                    new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8f, 6f}, 0));
            drawLegendLine(g2, lx, ly + 3 * lh, "Intersection (A ∩ B)", Color.MAGENTA.darker(),
                    new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3f, 6f}, 0));
            drawLegendItem(g2, lx, ly + 4 * lh, "¬A", new Color(255, 140, 0), Marker.DIAMOND);
            drawLegendItem(g2, lx, ly + 5 * lh, "¬B", new Color(0, 128, 128), Marker.TRIANGLE);
        }

        private int toScreenX(double xv, double minX, double maxX, int w, int pad) {
            return (int) (pad + ((xv - minX) / (maxX - minX)) * (w - 2 * pad));
        }

        private int toScreenY(double yv, int h, int pad) {
            return (int) (h - pad - yv * (h - 2 * pad));
        }

        // draw connected lines (index order) and markers; skip NaN entries
        private void drawLineWithMarkers(Graphics2D g2, double[] xs, double[] ys, double minX, double maxX, int w, int h, int pad, Color color, Marker marker) {
            if (xs == null || ys == null || xs.length == 0) return;
            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(color);

            // draw lines by index (do not sort) but skip NaNs
            Integer[] idx = new Integer[xs.length];
            for (int i = 0; i < xs.length; i++) idx[i] = i;
            Integer prev = null;
            for (int i = 0; i < idx.length; i++) {
                int j = idx[i];
                if (Double.isNaN(xs[j])) { prev = null; continue; }
                int x = toScreenX(xs[j], minX, maxX, w, pad);
                int y = toScreenY(ys[j], h, pad);
                if (prev != null) {
                    int px = toScreenX(xs[prev], minX, maxX, w, pad);
                    int py = toScreenY(ys[prev], h, pad);
                    g2.drawLine(px, py, x, y);
                }
                drawMarker(g2, x, y, marker);
                prev = j;
            }
        }

        // draw dashed/dotted style skipping NaNs
        private void drawLineWithStyle(Graphics2D g2, double[] xs, double[] ys, double minX, double maxX, int w, int h, int pad, Stroke stroke, Color color) {
            if (xs == null || ys == null || xs.length == 0) return;
            Stroke old = g2.getStroke();
            g2.setStroke(stroke);
            g2.setColor(color);
            Integer prev = null;
            for (int i = 0; i < xs.length; i++) {
                if (Double.isNaN(xs[i])) { prev = null; continue; }
                if (prev != null) {
                    int x1 = toScreenX(xs[prev], minX, maxX, w, pad), y1 = toScreenY(ys[prev], h, pad);
                    int x2 = toScreenX(xs[i], minX, maxX, w, pad), y2 = toScreenY(ys[i], h, pad);
                    g2.draw(new Line2D.Float(x1, y1, x2, y2));
                }
                prev = i;
            }
            g2.setStroke(old);
        }

        private void drawMarker(Graphics2D g2, int x, int y, Marker m) {
            int s = 8;
            switch (m) {
                case CIRCLE: g2.fillOval(x - s/2, y - s/2, s, s); break;
                case SQUARE: g2.fillRect(x - s/2, y - s/2, s, s); break;
                case TRIANGLE:
                    int[] tx = {x, x - s/2, x + s/2};
                    int[] ty = {y - s/2, y + s/2, y + s/2};
                    g2.fillPolygon(tx, ty, 3); break;
                case DIAMOND:
                    int[] dx = {x, x - s/2, x, x + s/2};
                    int[] dy = {y - s/2, y, y + s/2, y};
                    g2.fillPolygon(dx, dy, 4); break;
            }
        }

        private void drawLegendItem(Graphics2D g2, int x, int y, String label, Color color, Marker marker) {
            g2.setColor(color);
            drawMarker(g2, x + 10, y + 6, marker);
            g2.setColor(Color.BLACK);
            g2.drawString(label, x + 28, y + 10);
        }

        private void drawLegendLine(Graphics2D g2, int x, int y, String label, Color color, Stroke stroke) {
            Stroke old = g2.getStroke();
            g2.setStroke(stroke);
            g2.setColor(color);
            g2.drawLine(x + 6, y + 8, x + 28, y + 8);
            g2.setStroke(old);
            g2.setColor(Color.BLACK);
            g2.drawString(label, x + 36, y + 10);
        }

        enum Marker { CIRCLE, SQUARE, TRIANGLE, DIAMOND }

        // small helpers left intentionally minimal
        private void collectRange(double[] dummy) { /* placeholder - not used */ }
        private double[] minXMaxRef(double[] arr, double min, double max) { return arr; }
    }
}
