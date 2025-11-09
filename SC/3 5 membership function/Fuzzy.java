import java.awt.*;
import java.util.Scanner;
import javax.swing.*;
public class Fuzzy {

    static final int DOMAIN_MIN = 0, DOMAIN_MAX = 100, STEP = 1;

    static double[] domain() {
        int n = (DOMAIN_MAX - DOMAIN_MIN) / STEP + 1;
        double[] d = new double[n];
        for (int i = 0; i < n; i++) d[i] = DOMAIN_MIN + i * STEP;
        return d;
    }

    static double trap(double x, double a, double b, double c, double d) {
        if (x <= a || x >= d) return 0;
        if (x >= b && x <= c) return 1;
        if (x > a && x < b) return (x - a) / (b - a);
        return (d - x) / (d - c);
    }

    static double gauss(double x, double m, double sigma) {
        double t = (x - m) / sigma;
        return Math.exp(-0.5 * t * t);
    }

    static double sigmoid(double x, double a, double c) {
        return 1.0 / (1.0 + Math.exp(-a * (x - c)));
    }

    static double bell(double x, double a, double b, double c) {
        return 1 / (1 + Math.pow(Math.abs((x - c) / a), 2 * b));
    }

    static double singleton(double x, double c) {
        return x == c ? 1 : 0;
    }

    static double[] build(double[] dom, String type, double... p) {
        double[] mu = new double[dom.length];
        for (int i = 0; i < dom.length; i++) {
            switch (type) {
                case "trap": mu[i] = trap(dom[i], p[0], p[1], p[2], p[3]); break;
                case "gauss": mu[i] = gauss(dom[i], p[0], p[1]); break;
                case "sigmoid": mu[i] = sigmoid(dom[i], p[0], p[1]); break;
                case "bell": mu[i] = bell(dom[i], p[0], p[1], p[2]); break;
                case "singleton": mu[i] = singleton(dom[i], p[0]); break;
            }
        }
        return mu;
    }

    // GUI Panel to plot graph directly
    static class PlotPanel extends JPanel {
        double[] dom, series;
        String name;

        PlotPanel(double[] dom, double[] series, String name) {
            this.dom = dom;
            this.series = series;
            this.name = name;
            setPreferredSize(new Dimension(600, 400));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int W = getWidth(), H = getHeight(), margin = 50;
            g2.setPaint(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

            // Axes
            g2.drawLine(margin, H - margin, W - margin, H - margin);
            g2.drawLine(margin, H - margin, margin, margin);

            // X-axis ticks
            for (int i = 0; i <= 10; i++) {
                int x = margin + i * (W - 2 * margin) / 10;
                g2.drawLine(x, H - margin - 5, x, H - margin + 5);
                g2.drawString(Integer.toString((int) (DOMAIN_MIN + i * 10)), x - 8, H - margin + 20);
            }

            // Plot line
            g2.setPaint(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            for (int i = 0; i < dom.length - 1; i++) {
                int x1 = margin + (int) ((dom[i] - DOMAIN_MIN) / (DOMAIN_MAX - DOMAIN_MIN) * (W - 2 * margin));
                int x2 = margin + (int) ((dom[i + 1] - DOMAIN_MIN) / (DOMAIN_MAX - DOMAIN_MIN) * (W - 2 * margin));
                int y1 = H - margin - (int) (series[i] * (H - 2 * margin));
                int y2 = H - margin - (int) (series[i + 1] * (H - 2 * margin));
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.drawString(name, margin + 10, margin + 15);
        }
    }

    // Display chart on screen
    static void showPlot(double[] dom, double[] series, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new PlotPanel(dom, series, title));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double[] dom = domain();

        // ---------------- ADDED: print hardcoded variables first ----------------
        System.out.println("=== Using Hardcoded Parameters ===");
        System.out.printf("DOMAIN_MIN=%d, DOMAIN_MAX=%d, STEP=%d%n", DOMAIN_MIN, DOMAIN_MAX, STEP);

        // Membership function parameters (hardcoded)
        System.out.println("Trapezoidal: a=0, b=0, c=30, d=50");
        System.out.println("Gaussian   : m=50, sigma=12");
        System.out.println("Sigmoid    : a=0.1, c=50");
        System.out.println("Bell       : a=15, b=2, c=50");
        System.out.println("Singleton  : c=60");

        // Weights used in irrigation aggregation (hardcoded)
        System.out.println("Weights    : trap=0.8, gauss=0.5, sigmoid=0.6, bell=0.7, singleton=0.9");
        System.out.println("-----------------------------------------------------------------------");
        // ----------------------------------------------------------------------

        System.out.println("=== Fuzzy Soil Moisture Analysis ===");
        System.out.print("Enter soil moisture (0-100): ");
        double soilVal = Math.max(0, Math.min(100, sc.nextDouble()));

        double[] soilTrap = build(dom, "trap", 0, 0, 30, 50);
        double[] soilGauss = build(dom, "gauss", 50, 12);
        double[] soilSigmoid = build(dom, "sigmoid", 0.1, 50);
        double[] soilBell = build(dom, "bell", 15, 2, 50);
        double[] soilSingleton = build(dom, "singleton", 60);

        double mTrap = trap(soilVal, 0, 0, 30, 50);
        double mGauss = gauss(soilVal, 50, 12);
        double mSigmoid = sigmoid(soilVal, 0.1, 50);
        double mBell = bell(soilVal, 15, 2, 50);
        double mSingleton = singleton(soilVal, 60);

        System.out.printf("%nDegrees of Membership for soil moisture %.1f:%n", soilVal);
        System.out.printf("Trapezoidal: %.2f%nGaussian: %.2f%nSigmoid: %.2f%nBell: %.2f%nSingleton: %.2f%n",
                mTrap, mGauss, mSigmoid, mBell, mSingleton);

        // Show all graphs directly
        showPlot(dom, soilTrap, "Trapezoidal Membership");
        showPlot(dom, soilGauss, "Gaussian Membership");
        showPlot(dom, soilSigmoid, "Sigmoid Membership");
        showPlot(dom, soilBell, "Bell Membership");
        showPlot(dom, soilSingleton, "Singleton Membership");

        double irrigation = (mTrap*0.8 + mGauss*0.5 + mSigmoid*0.6 + mBell*0.7 + mSingleton*0.9) / 5;
        System.out.printf("%nApproximate irrigation recommendation (0=low,1=high): %.2f%n", irrigation);
        System.out.println("Use higher irrigation for higher value and vice versa.");
    }
}
