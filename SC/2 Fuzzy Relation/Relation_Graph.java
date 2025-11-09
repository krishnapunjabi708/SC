import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Scanner;

public class Relation_Graph {
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final Random RNG = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Fuzzy Relation: Radiation Level  Astronaut Safety (Interactive Display) ===");

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

        // Launch UI
        SwingUtilities.invokeLater(() -> createAndShowUI(astronauts, zones, relationA, relationB, composition));

        sc.close();
    }

    private static void createAndShowUI(String[] astronauts, String[] zones, double[][] A, double[][] B, double[][] composition) {
        JFrame frame = new JFrame("Fuzzy Relation: Composition Heatmap — Interactive Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        InfoPanel infoPanel = new InfoPanel(A, B, zones, astronauts);
        HeatmapPanel heatmapPanel = new HeatmapPanel(composition, astronauts, "Max-Min Composition (A ∘ Bᵀ)", infoPanel);

        // Wrap heatmap in scroll pane to avoid clipping for large matrices
        JScrollPane heatScroll = new JScrollPane(heatmapPanel);
        heatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        heatScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Use split pane so user can resize; set a reasonable initial divider location
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, heatScroll, infoPanel);
        split.setResizeWeight(0.65); // give heatmap more space initially
        split.setOneTouchExpandable(true);

        frame.add(split, BorderLayout.CENTER);

        frame.setSize(1100, 740);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // HeatmapPanel with clearer labels
    private static class HeatmapPanel extends JPanel {
        private final double[][] matrix;
        private final String[] labels;
        private final String title;
        private final InfoPanel infoPanel;
        private BufferedImage heatImg;
        private int leftMargin = 140;
        private int topMargin = 100;
        private int cellSize = 48; // can be adjusted based on matrix size

        HeatmapPanel(double[][] matrix, String[] labels, String title, InfoPanel infoPanel) {
            this.matrix = matrix;
            this.labels = labels;
            this.title = title;
            this.infoPanel = infoPanel;

            int n = Math.max(1, matrix.length);
            // If matrix would become too big, reduce cellSize so it fits nicely
            int maxGrid = 700; // target max grid size in pixels
            cellSize = Math.max(24, Math.min(64, maxGrid / n));

            int prefW = leftMargin + n * cellSize + 220;
            int prefH = topMargin + n * cellSize + 80;
            setPreferredSize(new Dimension(prefW, prefH));
            setBackground(Color.WHITE);

            buildHeatImage();

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int col = (p.x - leftMargin) / cellSize;
                    int row = (p.y - topMargin) / cellSize;
                    if (row >= 0 && row < n && col >= 0 && col < n) {
                        setToolTipText(labels[row] + " → " + labels[col] + " = " + DF.format(matrix[row][col]));
                    } else {
                        setToolTipText(null);
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();
                    int col = (p.x - leftMargin) / cellSize;
                    int row = (p.y - topMargin) / cellSize;
                    int nLocal = matrix.length;
                    if (row >= 0 && row < nLocal && col >= 0 && col < nLocal) {
                        // show details for clicked column astronaut
                        int astronautIndex = col;
                        infoPanel.updateForAstronaut(astronautIndex, labels[astronautIndex]);
                    }
                }
            });
        }

        private void buildHeatImage() {
            int n = matrix.length;
            int imgW = leftMargin + n * cellSize + 200;
            int imgH = topMargin + n * cellSize + 80;
            heatImg = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = heatImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);

            // title
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(title, leftMargin, 36);

            // find min/max
            double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                min = Math.min(min, matrix[i][j]);
                max = Math.max(max, matrix[i][j]);
            }
            if (min == max) { min = 0; max = 1; }

            // draw cells
            g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, cellSize/4)));
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = leftMargin + j * cellSize;
                    int y = topMargin + i * cellSize;
                    float t = (float)((matrix[i][j] - min) / (max - min));
                    Color c = colorMap(t);
                    g.setColor(c);
                    g.fillRect(x, y, cellSize, cellSize);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(x, y, cellSize, cellSize);
                    g.setColor(Color.BLACK);
                    String val = DF.format(matrix[i][j]);
                    int strW = g.getFontMetrics().stringWidth(val);
                    g.drawString(val, x + (cellSize - strW)/2, y + cellSize/2 + 5);
                }
            }

            // axis labels
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            // column labels (top)
            for (int j = 0; j < n; j++) {
                String lab = labels[j];
                int x = leftMargin + j * cellSize + cellSize/2 - g.getFontMetrics().stringWidth(lab)/2;
                int y = topMargin - 14;
                g.drawString(lab, x, y);
            }
            // row labels (left)
            for (int i = 0; i < n; i++) {
                String lab = labels[i];
                int x = leftMargin - g.getFontMetrics().stringWidth(lab) - 12;
                int y = topMargin + i * cellSize + cellSize/2 + 5;
                g.drawString(lab, x, y);
            }

            // axis titles
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("Rows: Source Astronaut", 12, topMargin - 30);
            g.drawString("Columns: Target Astronaut", leftMargin + n*cellSize/2 - 60, topMargin - 40);

            // legend
            int legendX = leftMargin + n * cellSize + 18;
            int legendY = topMargin;
            int legendW = 22;
            int legendH = n * cellSize;
            for (int yy = 0; yy < legendH; yy++) {
                float t = (float)yy / (legendH - 1);
                Color c = colorMap(1.0f - t);
                g.setColor(c);
                g.fillRect(legendX, legendY + yy, legendW, 1);
            }
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("Composition", legendX + legendW + 8, legendY - 6);
            g.drawString(DF.format(max), legendX + legendW + 8, legendY + 12);
            g.drawString(DF.format(min), legendX + legendW + 8, legendY + legendH);

            g.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (heatImg != null) g.drawImage(heatImg, 0, 0, this);
        }
    }

    private static class InfoPanel extends JPanel {
        private double[][] A; // radiation
        private double[][] B; // safety
        private String[] zones;
        private String[] astronauts;
        private int astronautIndex = 0;

        InfoPanel(double[][] A, double[][] B, String[] zones, String[] astronauts) {
            this.A = A;
            this.B = B;
            this.zones = zones;
            this.astronauts = astronauts;
            setPreferredSize(new Dimension(420, 640));
            setBackground(Color.WHITE);
        }

        void updateForAstronaut(int idx, String astronautName) {
            if (idx < 0 || idx >= A.length) return;
            this.astronautIndex = idx;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            String title = "Details — " + astronauts[astronautIndex];
            g.drawString(title, 18, 28);

            // summary
            double avgRad = 0, avgSaf = 0;
            int m = A[0].length;
            for (int j = 0; j < m; j++) { avgRad += A[astronautIndex][j]; avgSaf += B[astronautIndex][j]; }
            avgRad /= m; avgSaf /= m;

            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("Average Radiation: " + DF.format(avgRad), 18, 52);
            g.drawString("Average Safety:    " + DF.format(avgSaf), 18, 68);

            // draw bars
            int chartX = 18;
            int chartY = 100;
            int chartW = getWidth() - 36;
            int chartH = Math.max(220, getHeight() - 260);

            // grid
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i <= 5; i++) {
                int y = chartY + chartH - (int)(i * (chartH/5.0));
                g.drawLine(chartX, y, chartX + chartW, y);
                g.setColor(Color.BLACK);
                String lab = DF.format(i * 0.2);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString(lab, chartX - 36, y+4);
                g.setColor(Color.LIGHT_GRAY);
            }

            int cols = zones.length;
            if (cols == 0) return;
            int cellW = Math.max(60, chartW / cols);
            int barWidth = (int)(cellW * 0.35);

            for (int j = 0; j < cols; j++) {
                double rad = A[astronautIndex][j];
                double saf = B[astronautIndex][j];
                int xCenter = chartX + j * cellW + cellW/2;
                int radH = (int)(rad * chartH);
                int safH = (int)(saf * chartH);
                int xRad = xCenter - barWidth - 6;
                int yRad = chartY + chartH - radH;
                g.setColor(new Color(200,120,0));
                g.fillRect(xRad, yRad, barWidth, radH);
                g.setColor(Color.BLACK);
                g.drawRect(xRad, yRad, barWidth, radH);
                int xSaf = xCenter + 6;
                int ySaf = chartY + chartH - safH;
                g.setColor(new Color(80,150,220));
                g.fillRect(xSaf, ySaf, barWidth, safH);
                g.setColor(Color.BLACK);
                g.drawRect(xSaf, ySaf, barWidth, safH);

                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String label = zones[j];
                int labelW = g.getFontMetrics().stringWidth(label);
                g.setColor(Color.BLACK);
                g.drawString(label, xCenter - labelW/2, chartY + chartH + 18);

                // numeric values above bars
                g.drawString(DF.format(rad), xRad, yRad - 6);
                g.drawString(DF.format(saf), xSaf, ySaf - 6);
            }

            // legend
            int lx = chartX + chartW - 160;
            int ly = chartY + chartH + 40;
            g.setColor(new Color(200,120,0));
            g.fillRect(lx, ly, 18, 12);
            g.setColor(Color.BLACK);
            g.drawString("Radiation", lx + 26, ly + 12);
            g.setColor(new Color(80,150,220));
            g.fillRect(lx, ly + 20, 18, 12);
            g.setColor(Color.BLACK);
            g.drawString("Safety", lx + 26, ly + 32);
        }
    }

    // ---------- Utilities ----------
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
            if (!sc.hasNextDouble()) { String token = sc.next(); System.out.print("Enter numeric value between " + lo + " and " + hi + ": "); continue; }
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

    private static Color colorMap(float t) {
        t = Math.max(0f, Math.min(1f, t));
        if (t < 0.5f) {
            float tt = t / 0.5f;
            return interpolate(new Color(50, 50, 150), new Color(50, 180, 80), tt);
        } else {
            float tt = (t - 0.5f) / 0.5f;
            return interpolate(new Color(50, 180, 80), new Color(240, 220, 40), tt);
        }
    }

    private static Color interpolate(Color a, Color b, float t) {
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
