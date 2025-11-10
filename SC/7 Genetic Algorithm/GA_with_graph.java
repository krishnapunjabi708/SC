// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
import java.util.Random;
import java.util.Scanner;
import javax.swing.*;
import java.awt.*;

public class GA_with_graph {
   static final double MUTATION_RATE = 0.01;
   static final double CROSSOVER_RATE = 0.8;
   static final int ELITISM = 1;
   static final int TOURNAMENT_SIZE = 3;
   static final double GAUSSIAN_SIGMA = 0.05;
   static final Random RNG = new Random();

   // ------------------------- PLOTTING HELPERS -------------------------
   static void showFrame(String title, JPanel panel) {
      SwingUtilities.invokeLater(() -> {
         JFrame f = new JFrame(title);
         f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         f.add(panel);
         f.pack();
         f.setLocationRelativeTo(null);
         f.setVisible(true);
      });
   }

   // Fitness evolution panel (best & average) — fixed (no lambda capture)
   static class FitnessPanel extends JPanel {
      final double[] best, avg;
      FitnessPanel(double[] best, double[] avg) {
         this.best = best; this.avg = avg;
         setPreferredSize(new Dimension(800, 400));
      }
      @Override protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g;
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         int W = getWidth(), H = getHeight(), m = 55;

         // axes
         g2.setColor(Color.BLACK);
         g2.drawLine(m, H - m, W - m, H - m);
         g2.drawLine(m, H - m, m, m);
         g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g2.drawString("Generation", W/2 - 30, H - 15);
         g2.rotate(-Math.PI/2);
         g2.drawString("Fitness", -H/2 - 20, 20);
         g2.rotate(Math.PI/2);

         // bounds
         int n = best.length;
         double yMax = 0.0, yMin = Double.POSITIVE_INFINITY;
         for (double v : best) { if (v > yMax) yMax = v; if (v < yMin) yMin = v; }
         for (double v : avg)  { if (v > yMax) yMax = v; if (v < yMin) yMin = v; }
         if (Double.isInfinite(yMin)) yMin = 0;
         if (yMax == yMin) yMax = yMin + 1e-6;

         // grid & ticks
         g2.setColor(new Color(230,230,230));
         for (int i = 0; i <= 10; i++) {
            int x = m + i * (W - 2*m) / 10;
            g2.drawLine(x, m, x, H - m);
         }
         for (int i = 0; i <= 10; i++) {
            int y = H - m - i * (H - 2*m) / 10;
            g2.drawLine(m, y, W - m, y);
         }
         g2.setColor(Color.BLACK);
         for (int i = 0; i <= 10; i++) {
            int x = m + i * (W - 2*m) / 10;
            g2.drawLine(x, H - m - 3, x, H - m + 3);
            String lab = Integer.toString((int)Math.round(i * (n-1) / 10.0));
            g2.drawString(lab, x - 6, H - m + 18);
         }
         for (int i = 0; i <= 5; i++) {
            int y = H - m - i * (H - 2*m) / 5;
            double fy = yMin + i * (yMax - yMin) / 5.0;
            g2.drawLine(m - 3, y, m + 3, y);
            g2.drawString(String.format("%.2f", fy), 8, y + 4);
         }

         // draw series (no lambda)
         plotSeries(g2, avg,  new Color(100, 181, 246), W, H, m, yMin, yMax); // sky blue
         plotSeries(g2, best, new Color(255, 193, 7),   W, H, m, yMin, yMax); // gold

         // legend
         int lx = W - m - 160, ly = m + 10;
         g2.setColor(Color.WHITE);
         g2.fillRoundRect(lx-10, ly-10, 150, 50, 12, 12);
         g2.setColor(Color.GRAY);
         g2.drawRoundRect(lx-10, ly-10, 150, 50, 12, 12);
         g2.setStroke(new BasicStroke(3f));
         g2.setColor(new Color(255, 193, 7)); g2.drawLine(lx, ly, lx+30, ly); g2.setColor(Color.BLACK); g2.drawString("Best fitness", lx+40, ly+5);
         g2.setColor(new Color(100, 181, 246)); g2.drawLine(lx, ly+20, lx+30, ly+20); g2.setColor(Color.BLACK); g2.drawString("Average fitness", lx+40, ly+25);
      }

      private void plotSeries(Graphics2D g2, double[] arr, Color col,
                              int W, int H, int m, double yMin, double yMax) {
         int n = arr.length;
         g2.setColor(col);
         g2.setStroke(new BasicStroke(2f));
         for (int i = 0; i < n - 1; i++) {
            int x1 = m + (int)Math.round((double)i/(n-1) * (W - 2*m));
            int x2 = m + (int)Math.round((double)(i+1)/(n-1) * (W - 2*m));
            int y1 = H - m - (int)Math.round((arr[i]-yMin)/(yMax-yMin) * (H - 2*m));
            int y2 = H - m - (int)Math.round((arr[i+1]-yMin)/(yMax-yMin) * (H - 2*m));
            g2.drawLine(x1, y1, x2, y2);
         }
      }
   }

   // Function curve panel f(x) with best point
   static class FunctionPanel extends JPanel {
      final double xBest, fBest;
      FunctionPanel(double xBest, double fBest) {
         this.xBest = xBest; this.fBest = fBest;
         setPreferredSize(new Dimension(800, 400));
      }
      @Override protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g;
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         int W = getWidth(), H = getHeight(), m = 55;

         // axes
         g2.setColor(Color.BLACK);
         g2.drawLine(m, H - m, W - m, H - m);
         g2.drawLine(m, H - m, m, m);
         g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g2.drawString("x in [0,1]", W/2 - 20, H - 15);
         g2.rotate(-Math.PI/2);
         g2.drawString("f(x) = x*sin(10πx)+1", -H/2 - 40, 20);
         g2.rotate(Math.PI/2);

         // sample function
         int N = 400;
         double[] xs = new double[N], ys = new double[N];
         double yMin = Double.POSITIVE_INFINITY, yMax = -Double.POSITIVE_INFINITY;
         for (int i = 0; i < N; i++) {
            xs[i] = (double)i / (N - 1);
            ys[i] = objective(xs[i]);
            if (ys[i] < yMin) yMin = ys[i];
            if (ys[i] > yMax) yMax = ys[i];
         }
         if (yMax == yMin) yMax = yMin + 1e-6;

         // grid
         g2.setColor(new Color(230,230,230));
         for (int i = 0; i <= 10; i++) {
            int x = m + i * (W - 2*m) / 10; g2.drawLine(x, m, x, H - m);
         }
         for (int i = 0; i <= 10; i++) {
            int y = H - m - i * (H - 2*m) / 10; g2.drawLine(m, y, W - m, y);
         }

         // ticks
         g2.setColor(Color.BLACK);
         for (int i = 0; i <= 10; i++) {
            int x = m + i * (W - 2*m) / 10;
            g2.drawLine(x, H - m - 3, x, H - m + 3);
            g2.drawString(String.format("%.1f", i/10.0), x - 8, H - m + 18);
         }
         for (int i = 0; i <= 5; i++) {
            int y = H - m - i * (H - 2*m) / 5;
            double fy = yMin + i * (yMax - yMin) / 5.0;
            g2.drawLine(m - 3, y, m + 3, y);
            g2.drawString(String.format("%.2f", fy), 8, y + 4);
         }

         // plot f(x)
         g2.setColor(Color.BLUE);
         g2.setStroke(new BasicStroke(2f));
         for (int i = 0; i < N - 1; i++) {
            int x1 = m + (int)Math.round(xs[i] * (W - 2*m));
            int x2 = m + (int)Math.round(xs[i+1] * (W - 2*m));
            int y1 = H - m - (int)Math.round((ys[i] - yMin) / (yMax - yMin) * (H - 2*m));
            int y2 = H - m - (int)Math.round((ys[i+1] - yMin) / (yMax - yMin) * (H - 2*m));
            g2.drawLine(x1, y1, x2, y2);
         }

         // best point
         int bx = m + (int)Math.round(xBest * (W - 2*m));
         int by = H - m - (int)Math.round((fBest - yMin) / (yMax - yMin) * (H - 2*m));
         g2.setColor(Color.RED);
         g2.fillOval(bx - 5, by - 5, 10, 10);
         g2.setColor(Color.BLACK);
         g2.drawString(String.format("Best (x=%.4f, f=%.4f)", xBest, fBest), bx + 10, by - 8);
      }
   }
   // --------------------------------------------------------------------

   public static void main(String[] var0) {
      Scanner var1 = new Scanner(System.in);
      System.out.println("=== Simple Genetic Algorithm (Java) ===");
      System.out.println("Objective: Maximizing f(x) = x * sin(10πx) + 1 for x ∈ [0, 1]\n");
      System.out.print("Population size (e.g., 50): ");
      int var2 = var1.nextInt();
      System.out.print("Gene length in bits (e.g., 20): ");
      int var3 = var1.nextInt();
      System.out.print("Generations (e.g., 50): ");
      int var4 = var1.nextInt();
      System.out.println("\nSelection Methods:");
      System.out.println("  1) Roulette   2) Tournament   3) Rank   4) Random");
      System.out.print("Choose (1-4): ");
      int var5 = var1.nextInt();
      String var6 = var5 == 2 ? "tournament" : (var5 == 3 ? "rank" : (var5 == 4 ? "random" : "roulette"));
      System.out.println("\nCrossover Methods:");
      System.out.println("  1) Single-point   2) Arithmetic   3) Uniform   4) Two-point");
      System.out.print("Choose (1-4): ");
      int var7 = var1.nextInt();
      String var8 = var7 == 2 ? "arithmetic" : (var7 == 3 ? "uniform" : (var7 == 4 ? "two" : "single"));
      System.out.println("\nMutation Types:");
      System.out.println("  1) Gaussian   2) Random   3) Bitflip   4) Swap   5) Scramble   6) Inversion");
      System.out.print("Choose (1-6): ");
      int var9 = var1.nextInt();
      String var10 = var9 == 2 ? "random" : (var9 == 3 ? "bitflip" : (var9 == 4 ? "swap" : (var9 == 5 ? "scramble" : (var9 == 6 ? "inversion" : "gaussian"))));

      // histories for plotting
      double[] bestHist = new double[var4];
      double[] avgHist  = new double[var4];

      // ---- Initialize population (binary) ----
      int[][] var11 = new int[var2][var3];
      for (int i = 0; i < var2; ++i) {
         for (int j = 0; j < var3; ++j) {
            var11[i][j] = RNG.nextBoolean() ? 1 : 0;
         }
      }

      double[] var28 = new double[var2];
      int[][] var29 = new int[var2][var3];

      for (int gen = 1; gen <= var4; ++gen) {
         double best = -1.0E9;
         double sum = 0.0;

         for (int i = 0; i < var2; ++i) {
            double x = bitsToDouble(var11[i]);
            double f = objective(x);
            var28[i] = f;
            sum += f;
            if (f > best) best = f;
         }

         double avg = sum / (double)var2;
         bestHist[gen - 1] = best;
         avgHist[gen - 1]  = avg;

         System.out.printf("\nGen %d | Best: %.6f | Avg: %.6f | Sel=%s | Cx=%s | Mut=%s%n", gen, best, avg, var6, var8, var10);

         int[] ord = sortByFitnessDesc(var28);
         for (int k = 0; k < Math.min(3, var2); ++k) {
            int idx = ord[k];
            double x = bitsToDouble(var11[idx]);
            System.out.printf("  #%d  f=%.6f  x=%.6f  chrom=%s%n", idx, var28[idx], x, bitsToString(var11[idx]));
         }

         int np = 0;
         // Elitism
         for (int e = 0; e < ELITISM && np < var2; e++, np++) {
            copyChrom(var11[ord[e]], var29[np]);
         }

         // Offspring
         while (np < var2) {
            int p1 = selectParent(var28, sum, var6);
            int p2 = selectParent(var28, sum, var6);
            int[] c1 = new int[var3];
            int[] c2 = new int[var3];

            if (RNG.nextDouble() < CROSSOVER_RATE && var3 > 1) {
               applyCrossover(var11[p1], var11[p2], c1, c2, var8);
            } else {
               copyChrom(var11[p1], c1);
               copyChrom(var11[p2], c2);
            }

            applyMutation(c1, var10);
            applyMutation(c2, var10);

            copyChrom(c1, var29[np++]);
            if (np < var2) copyChrom(c2, var29[np++]);
         }

         int[][] tmp = var11; var11 = var29; var29 = tmp;
      }

      // ---- Final result ----
      double bestFit = -1.0E9; int bestIdx = 0;
      for (int i = 0; i < var2; ++i) {
         double f = objective(bitsToDouble(var11[i]));
         if (f > bestFit) { bestFit = f; bestIdx = i; }
      }
      double xBest = bitsToDouble(var11[bestIdx]);

      System.out.println("\n=== Final Result ===");
      System.out.printf("Maximizing f(x) = x * sin(10πx) + 1 for x ∈ [0, 1]%n");
      System.out.printf("Best Fitness: %.6f%n", bestFit);
      System.out.printf("Best x: %.6f%n", xBest);
      System.out.println("Best Chromosome: " + bitsToString(var11[bestIdx]));
      System.out.println("\n=== Final Population (index | f(x) | x | chromosome) ===");
      for (int i = 0; i < var2; ++i) {
         double x = bitsToDouble(var11[i]);
         double f = objective(x);
         System.out.printf("%3d | %.6f | %.6f | %s%n", i, f, x, bitsToString(var11[i]));
      }

      // ---- SHOW GRAPHS ----
      showFrame("Fitness Evolution (Best & Average)", new FitnessPanel(bestHist, avgHist));
      showFrame("Objective f(x) with Best Point", new FunctionPanel(xBest, bestFit));

      var1.close();
   }

   static double objective(double var0) {
      // f(x) = x * sin(10πx) + 1
      return var0 * Math.sin(31.41592653589793 * var0) + 1.0;
   }

   static int selectParent(double[] var0, double var1, String var3) {
      switch (var3) {
         case "tournament":
            return tournamentSelect(var0, 3);
         case "rank":
            return rankSelect(var0);
         case "random":
            return RNG.nextInt(var0.length);
         default:
            return rouletteSelect(var0, var1);
      }
   }

   static int rouletteSelect(double[] var0, double var1) {
      if (var1 <= 0.0) {
         return RNG.nextInt(var0.length);
      } else {
         double var3 = RNG.nextDouble() * var1;
         double var5 = 0.0;
         for (int var7 = 0; var7 < var0.length; ++var7) {
            var5 += var0[var7];
            if (var5 >= var3) return var7;
         }
         return var0.length - 1;
      }
   }

   static int tournamentSelect(double[] var0, int var1) {
      int n = var0.length;
      int best = RNG.nextInt(n);
      for (int k = 1; k < var1; ++k) {
         int idx = RNG.nextInt(n);
         if (var0[idx] > var0[best]) best = idx;
      }
      return best;
   }

   static int rankSelect(double[] var0) {
      int n = var0.length;
      int[] ord = sortByFitnessAsc(var0); // worst..best
      int total = n * (n + 1) / 2;
      int r = RNG.nextInt(total) + 1, acc = 0;
      for (int i = 0; i < n; ++i) {
         acc += (i + 1);
         if (acc >= r) return ord[i];
      }
      return ord[n - 1];
   }

   static void applyCrossover(int[] a, int[] b, int[] c1, int[] c2, String type) {
      int L = a.length;
      if ("arithmetic".equals(type)) {
         double x1 = bitsToDouble(a);
         double x2 = bitsToDouble(b);
         double y1 = 0.5 * x1 + 0.5 * x2;
         double y2 = 0.5 * x2 + 0.5 * x1;
         doubleToBits(y1, c1);
         doubleToBits(y2, c2);
      } else if ("uniform".equals(type)) {
         for (int i = 0; i < L; ++i) {
            if (RNG.nextBoolean()) { c1[i] = a[i]; c2[i] = b[i]; }
            else { c1[i] = b[i]; c2[i] = a[i]; }
         }
      } else if ("two".equals(type)) {
         int i = RNG.nextInt(L), j = RNG.nextInt(L);
         if (i > j) { int t = i; i = j; j = t; }
         for (int k = 0; k < i; ++k) { c1[k] = a[k]; c2[k] = b[k]; }
         for (int k = i; k < j; ++k) { c1[k] = b[k]; c2[k] = a[k]; }
         for (int k = j; k < L; ++k) { c1[k] = a[k]; c2[k] = b[k]; }
      } else { // single
         int cut = 1 + RNG.nextInt(L - 1);
         for (int i = 0; i < cut; ++i) { c1[i] = a[i]; c2[i] = b[i]; }
         for (int i = cut; i < L; ++i) { c1[i] = b[i]; c2[i] = a[i]; }
      }
   }

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

   static void bitFlipMutation(int[] chrom, double rate) {
      for (int i = 0; i < chrom.length; ++i)
         if (RNG.nextDouble() < rate) chrom[i] = 1 - chrom[i];
   }

   static void swapMutation(int[] chrom, double rate) {
      if (RNG.nextDouble() < rate && chrom.length > 1) {
         int i = RNG.nextInt(chrom.length), j = RNG.nextInt(chrom.length);
         int t = chrom[i]; chrom[i] = chrom[j]; chrom[j] = t;
      }
   }

   static void scrambleMutation(int[] chrom, double rate) {
      if (RNG.nextDouble() < rate && chrom.length > 2) {
         int i = RNG.nextInt(chrom.length), j = RNG.nextInt(chrom.length);
         if (i > j) { int t = i; i = j; j = t; }
         for (int k = i; k <= j; ++k) {
            int r = i + RNG.nextInt(j - i + 1);
            int tmp = chrom[k]; chrom[k] = chrom[r]; chrom[r] = tmp;
         }
      }
   }

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

   static void randomResetMutation(int[] chrom, double rate) {
      for (int i = 0; i < chrom.length; ++i)
         if (RNG.nextDouble() < rate) chrom[i] = RNG.nextBoolean() ? 1 : 0;
   }

   static void gaussianOnXMutation(int[] chrom, double sigma, double rate) {
      if (RNG.nextDouble() < rate) {
         double x = bitsToDouble(chrom);
         double y = x + RNG.nextGaussian() * sigma;
         if (y < 0.0) y = 0.0;
         if (y > 1.0) y = 1.0;
         doubleToBits(y, chrom);
      }
   }

   static double bitsToDouble(int[] b) {
      long val = 0L;
      for (int j = 0; j < b.length; ++j) val = (val << 1) | (long)(b[j] & 1);
      long max = b.length >= 63 ? Long.MAX_VALUE : (1L << b.length) - 1L;
      if (max == 0L) return 0.0;
      double x = (double)val / (double)max;
      if (x < 0.0) x = 0.0; if (x > 1.0) x = 1.0;
      return x;
   }

   static void doubleToBits(double x, int[] out) {
      if (x < 0.0) x = 0.0; if (x > 1.0) x = 1.0;
      long max = out.length >= 63 ? Long.MAX_VALUE : (1L << out.length) - 1L;
      long val = Math.round(x * (double)max);
      for (int i = out.length - 1; i >= 0; --i) {
         out[i] = (int)(val & 1L);
         val >>= 1;
      }
   }

   static void copyChrom(int[] src, int[] dst) {
      for (int i = 0; i < src.length; ++i) dst[i] = src[i];
   }

   static int[] sortByFitnessDesc(double[] fit) {
      int n = fit.length;
      int[] idx = new int[n];
      for (int i = 0; i < n; idx[i] = i, ++i) {}
      for (int i = 0; i < n - 1; ++i) {
         int best = i;
         for (int j = i + 1; j < n; ++j)
            if (fit[idx[j]] > fit[idx[best]]) best = j;
         int t = idx[i]; idx[i] = idx[best]; idx[best] = t;
      }
      return idx;
   }

   static int[] sortByFitnessAsc(double[] fit) {
      int n = fit.length;
      int[] idx = new int[n];
      for (int i = 0; i < n; idx[i] = i, ++i) {}
      for (int i = 0; i < n - 1; ++i) {
         int worst = i;
         for (int j = i + 1; j < n; ++j)
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
