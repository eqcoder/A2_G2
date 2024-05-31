import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class A2_G2_t2 {

    public static double eps = 0;
    public static int minPts = 0;
    public static ArrayList<Point> points = new ArrayList<>();
    public static int n;

    public static class Pos {
        double x;
        double y;

        Pos() {
        }

        Pos(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void setPos(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;

        }
    }

    public static class Point extends Pos {
        String name;
        int cluster;

        Point(double x, double y, String name) {
            super(x, y);
            this.name = name;
            this.cluster = 0;
        }

        public void setCluster(int cluster) {
            this.cluster = cluster;
        }

        public int getCluster() {
            return this.cluster;
        }
    }

    private static double computeSilhouetteCoefA(ArrayList<Point> cluster, Point p) {
        double A = 0;
        for (Point p1 : cluster) {
            if (p1 == p)
                continue;
            A += computeDistance(p1, p);
        }
        if (cluster.size() > 1) {
            A = A / (double) (cluster.size() - 1);
        }
        return A;
    }

    private static double computeSilhouetteCoefB(Point p, ArrayList<ArrayList<Point>> clusters) {
        double B = Double.MAX_VALUE, dist = 0;
        for (int i = 0; i < clusters.size(); i++) {
            int size = clusters.get(i).size();
            if (p.cluster == i + 1 || size == 0)
                continue;
            for (Point p1 : clusters.get(i)) {
                dist += computeDistance(p1, p);
            }
            dist /= (double) size;
            if (dist < B) {
                B = dist;
            }
            dist = 0;
        }
        return B;
    }

    private static double computeSilhouetteCoef(ArrayList<ArrayList<Point>> clusters, int k) {
        double clusterMean = 0;
        double totMean = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (Point p : clusters.get(i)) {
                double A = computeSilhouetteCoefA(clusters.get(i), p);
                double B = computeSilhouetteCoefB(p, clusters);
                double Si = (B - A) / Math.max(B, A);
                if (Math.max(B, A) == 0) {
                    Si = 0;
                }
                clusterMean += Si;
            }
            totMean += clusterMean;
            clusterMean = 0;
        }
        totMean /= n;
        return totMean;
    }

    public static ArrayList<ArrayList<Point>> dbScan(int minPts) {
        int c = 0;
        ArrayList<ArrayList<Point>> clusters = new ArrayList<>();
        for (Point p : points) {
            if (p.getCluster() > 0)
                continue;

            ArrayList<Point> neighbors = getNeighbor(p);
            if (neighbors.size() + 1 < minPts) {
                p.setCluster(-1);
                continue;
            }
            c++;
            ArrayList<Point> newCluster = new ArrayList<>();
            p.setCluster(c);
            newCluster.add(p);
            Stack<Point> cluster = new Stack<>();
            cluster.addAll(neighbors);
            while (!cluster.empty()) {
                Point neighbor = cluster.pop();
                if (neighbor.getCluster() > 0)
                    continue;
                neighbor.setCluster(c);
                newCluster.add(neighbor);
                neighbors = getNeighbor(neighbor);
                if (neighbors.size() + 1 >= minPts)
                    cluster.addAll(neighbors);
            }
            clusters.add(newCluster);
        }
        return clusters;
    }

    public static ArrayList<Point> getNeighbor(Point p0) {
        ArrayList<Point> neighbors = new ArrayList<>();
        for (Point p : points) {
            if (p0 != p && computeDistance(p0, p) <= eps)
                neighbors.add(p);
        }
        return neighbors;
    }

    public static double findOptEps() {
        ArrayList<Double> distList = new ArrayList<>();
        for (Point p1 : points) {
            ArrayList<Double> distAllPoint = new ArrayList<>();
            for (Point p2 : points) {
                if (p1 != p2) {
                    distAllPoint.add(computeDistance(p1, p2));
                }
            }
            Collections.sort(distAllPoint);
            distList.add(distAllPoint.get(minPts));
        }
        Collections.sort(distList);
        int q = n;
        double dist, maxDist = 0, optEps = 0;
        double maxD = distList.get((int) n / 2), rate = 0;
        double prev = distList.get(0);
        for (int i = 0; i < distList.size(); i++) {
            double d = distList.get(i);
            rate = d - prev;
            if (rate > (maxD / 15)) {
                q = i;
                break;
            }
            prev = d;
        }
        double y1 = distList.get(0);
        double y2 = distList.get(q - 1);
        double a = y2 - y1;
        double b = 1 - q;
        double c = (q + 1) * y1 - (y2 - y1);
        for (int i = 0; i < q; i++) {
            double d = distList.get(i);
            dist = Math.abs(a * (i + 1) + b * d + c);
            if (dist > maxDist) {
                maxDist = dist;
                optEps = d;
            }
        }
        return (double) Math.round(optEps * 10) / 10.0;
    }

    public static int findOptMinPts() {
        int optMinPts = 4, minPts = 4;
        double silhoutteCoef = 0, bestSilhoutteCoef = 0;
        ArrayList<ArrayList<Point>> clusters;
        for (; minPts <= (int) Math.log(n) * 3; minPts++) {
            clusters = dbScan(minPts);
            silhoutteCoef = computeSilhouetteCoef(clusters, clusters.size());
            System.out.println(minPts + " : " + silhoutteCoef);
            if (silhoutteCoef > bestSilhoutteCoef) {
                bestSilhoutteCoef = silhoutteCoef;
                optMinPts = minPts;
            }
            for (Point p : points) {
                p.setCluster(0);
            }
        }
        return optMinPts;
    }

    public static double computeDistance(Point a, Point b) {
        double dist = Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2);
        return Math.sqrt(dist);
    }

    public static boolean isInteger(double d) {
        return d % 1.0 == 0;
    }

    public static void fileOut() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("dbscan.csv"))) {
            for (Point p : points) {
                if (p.getCluster() == -1)
                    bw.write(p.name + "," + p.getX() + "," + p.getY() + ",noise");
                else
                    bw.write(p.name + "," + p.getX() + "," + p.getY() + "," + Integer.toString(p.getCluster()));
                bw.write(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String line = "";
        String path = args[0];
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                String[] items = line.split(",");
                points.add(new Point(Double.parseDouble(items[1]), Double.parseDouble(items[2]), items[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        n = points.size();
        double arg1 = Double.parseDouble(args[1]);
        int numNoise = 0;
        if (args.length == 2) {
            if (isInteger(arg1)) {
                minPts = (int) arg1;
                eps = findOptEps();
                System.out.println("Estimated eps : " + eps);
            } else {
                eps = arg1;
                minPts = findOptMinPts();
                System.out.println("Estimated MinPts : " + minPts);
            }

        } else if (args.length == 3) {
            double arg2 = Double.parseDouble(args[2]);
            if (isInteger(arg1)) {
                minPts = (int) arg1;
                eps = arg2;
            } else {
                minPts = (int) arg2;
                eps = arg1;
            }
        }
        final long startTime = System.currentTimeMillis();
        ArrayList<ArrayList<Point>> clusters = dbScan(minPts);
        final long endTime = System.currentTimeMillis();
        for (Point p : points) {
            if (p.getCluster() == -1)
                numNoise++;
        }
        System.out.println("Number of clusters : " + clusters.size());
        System.out.println("Number of Noise : " + numNoise);
        for (int i = 0; i < clusters.size(); i++) {
            System.out.print((i + 1) + "Cluster #" + (i + 1) + " =>  ");
            for (Point p : clusters.get(i)) {
                System.out.print(p.name + " ");
            }
            System.out.println();
        }
        final long elapsed = endTime - startTime;
        // System.out.println("time : " + (double) elapsed / 1000 + " seconds");
        // fileOut();
    }
}
