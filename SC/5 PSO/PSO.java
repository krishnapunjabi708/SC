import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;

class Particle {
    double[] pos, vel, bestPos;
    double bestVal;

    Particle(int dim, double lo, double hi, Random r) {
        pos = new double[dim];
        vel = new double[dim];
        bestPos = new double[dim];
        for (int i = 0; i < dim; i++) {
            pos[i] = lo + (hi - lo) * r.nextDouble();
            vel[i] = (r.nextDouble() - 0.5) * (hi - lo) * 0.5;
            bestPos[i] = pos[i];
        }
        bestVal = rosenbrock(pos);
    }

    static double rosenbrock(double[] p) {
        double x = p[0], y = p[1];
        return Math.pow(1 - x, 2) + 100 * Math.pow(y - x * x, 2);
    }
}

public class PSO {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Swarm size (enter for 30): ");
        String s = sc.nextLine().trim();
        int SWARM = s.isEmpty() ? 30 : Integer.parseInt(s);
        System.out.print("Max iterations (enter for 1000): ");
        s = sc.nextLine().trim();
        int MAX_ITER = s.isEmpty() ? 1000 : Integer.parseInt(s);

        final int DIM = 2;
        final double LO = -5, HI = 5, W = 0.7, C1 = 1.5, C2 = 1.5, VMAX = (HI - LO) * 0.5;

        // Print declared variables
        System.out.println("\n===== Pre-declared Variables =====");
        System.out.printf("DIM      = %d%n", DIM);
        System.out.printf("LO       = %.2f%n", LO);
        System.out.printf("HI       = %.2f%n", HI);
        System.out.printf("W        = %.2f%n", W);
        System.out.printf("C1       = %.2f%n", C1);
        System.out.printf("C2       = %.2f%n", C2);
        System.out.printf("VMAX     = %.2f%n", VMAX);
        System.out.printf("SWARM    = %d%n", SWARM);
        System.out.printf("MAX_ITER = %d%n", MAX_ITER);
        System.out.println("==================================\n");

        Random rand = new Random();
        Particle[] swarm = new Particle[SWARM];
        for (int i = 0; i < SWARM; i++)
            swarm[i] = new Particle(DIM, LO, HI, rand);

        double[] gBest = swarm[0].bestPos.clone();
        double gVal = Double.MAX_VALUE;

        for (Particle p : swarm)
            if (p.bestVal < gVal) {
                gVal = p.bestVal;
                gBest = p.bestPos.clone();
            }

        int progressStep = Math.max(1, MAX_ITER / 10);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            for (Particle p : swarm) {
                for (int d = 0; d < DIM; d++) {
                    double r1 = rand.nextDouble(), r2 = rand.nextDouble();
                    p.vel[d] = W * p.vel[d] + C1 * r1 * (p.bestPos[d] - p.pos[d]) + C2 * r2 * (gBest[d] - p.pos[d]);
                    if (p.vel[d] > VMAX) p.vel[d] = VMAX;
                    if (p.vel[d] < -VMAX) p.vel[d] = -VMAX;

                    p.pos[d] += p.vel[d];
                    if (p.pos[d] < LO) p.pos[d] = LO;
                    if (p.pos[d] > HI) p.pos[d] = HI;
                }

                double val = Particle.rosenbrock(p.pos);
                if (val < p.bestVal) {
                    p.bestVal = val;
                    p.bestPos = p.pos.clone();
                }
                if (p.bestVal < gVal) {
                    gVal = p.bestVal;
                    gBest = p.bestPos.clone();
                }
            }

            if (iter % progressStep == 0)
                System.out.printf("Iter %4d / %d : best = %.10f at (%.6f, %.6f)%n", 
                        iter, MAX_ITER, gVal, gBest[0], gBest[1]);
        }

        System.out.printf("\nFinal best: f(%.6f, %.6f) = %.12f%n", gBest[0], gBest[1], gVal);
        sc.close();
    }
}
