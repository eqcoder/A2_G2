import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;

public class ensemble {
    public static double eps = 0;
    public static int minPts = 0;
    public static ArrayList<Point> points = new ArrayList<>();
    public static int n, noiseNum = 0;

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

    public static class Centroid extends Pos {
        int id;
        ArrayList<Point> points;

        Centroid(Pos p, int c) {
            super(p.getX(), p.getY());
            this.id = c;
            points = new ArrayList<>();
        }

        public void addPoint(Point p) {
            points.add(p);
        }

        public ArrayList<Point> getPoints() {
            return points;
        }
    }

    private static ArrayList<Centroid> kMeans(ArrayList<Centroid> centroid) {
        double bestSilhoutteCoef = 0, silhoutteCoef = 0;
        int maxK = 0, bestK = 0;
        maxK = (int) Math.round(Math.log(n)) * 5;
        ArrayList<Centroid>[] centroids = new ArrayList[maxK];
        for (int i = 0; i < maxK; i++) {
            centroids[i] = new ArrayList<>();
            for (Centroid c : centroid) {
                centroids[i].add(new Centroid(c, c.id));
            }
            cluster(centroids[i]);
            ArrayList<ArrayList<Point>> clusters = new ArrayList<>();
            for (Centroid c : centroids[i]) {
                clusters.add(c.getPoints());
            }
            silhoutteCoef = computeSilhouetteCoef(clusters, centroids[i].size());
            if (silhoutteCoef > bestSilhoutteCoef) {
                bestSilhoutteCoef = silhoutteCoef;
                bestK = i;
            }
            centroid = chooseCentroids(centroid);
        }
        System.out.println("estimated k : " + centroids[bestK].size());
        return centroids[bestK];
    }

    private static ArrayList<Centroid> chooseCentroids(ArrayList<Centroid> centroids) {
        Random random = new Random();
        Point choice;
        int c = centroids.size();
        if (c == 0) {
            choice = points.get(random.nextInt(n));
        } else {
            NavigableMap<Double, Point> weightedDistance = new TreeMap<Double, Point>();
            double totalWeight = 0;
            for (Point p : points) {

                double minDistance = centroids.stream()
                        .map(x -> (Math.pow(x.getX() - p.getX(), 2) + Math.pow(x.getY() - p.getY(), 2)))
                        .mapToDouble(i -> i).min()
                        .getAsDouble();
                if (minDistance != 0) {
                    totalWeight += Math.pow(minDistance, 2);
                    weightedDistance.put(totalWeight, p);
                }
            }
            double value = random.nextDouble() * totalWeight;
            choice = weightedDistance.higherEntry(value).getValue();
        }
        centroids.add(new Centroid(choice, c + 1));
        return centroids;
    }

    private static void cluster(ArrayList<Centroid> centroids) {
        boolean isChanged = false;
        int closestCentroid;
        for (Point p : points) {
            closestCentroid = findClosestCentroid(p, centroids);
            centroids.get(closestCentroid).addPoint(p);
            p.setCluster(closestCentroid + 1);
        }

        for (Centroid c : centroids) {
            double prevX = c.getX(), newX = 0, prevY = c.getY(), newY = 0;
            ArrayList<Point> clusterPoints = c.getPoints();
            int numPoints = clusterPoints.size();
            for (Point p : clusterPoints) {
                newX += p.getX();
                newY += p.getY();
            }
            newX /= numPoints;
            newY /= numPoints;
            c.setPos(newX, newY);
            if (Double.compare(prevX, newX) != 0 || Double.compare(prevY, newY) != 0)
                isChanged = true;
        }
        if (isChanged) {
            for (Centroid c : centroids) {
                c.getPoints().clear();
            }
            cluster(centroids);
        }
    }

    private static Integer findClosestCentroid(Pos p, ArrayList<Centroid> centroids) {
        double minDistance = Double.POSITIVE_INFINITY;
        int closestCentroid = 0;
        double distance;
        for (Centroid c : centroids) {
            distance = computeDistance(c, p);
            if (distance < minDistance) {
                minDistance = distance;
                closestCentroid = c.id - 1;
            }
        }
        return closestCentroid;
    }

    private static double computeDistance(Pos p1, Pos p2) {
        double distance = Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
        return distance;
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
        for (int i = 0; i < k; i++) {
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
            if (p0 != p && computeDist(p0, p) <= eps)
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
        double maxD = distList.get((int) (n / 2)), rate = 0;
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
        int optMinPts = 3, minPts = 3;
        double silhoutteCoef = 0, bestSilhoutteCoef = 0;
        ArrayList<ArrayList<Point>> clusters;
        for (; minPts <= (int) Math.log(n) * 2; minPts++) {
            clusters = dbScan(minPts);
            silhoutteCoef = computeSilhouetteCoef(clusters, clusters.size());
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

    public static double computeDist(Point a, Point b) {
        double dist = Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2);
        return Math.sqrt(dist);
    }

    public static void fileOut(ArrayList<Centroid> centroids) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("ensemble.csv"))) {
            for (Centroid c : centroids) {
                for (Point p : c.getPoints()) {
                    bw.write(p.name + "," + p.getX() + "," + p.getY() + "," + Integer.toString(c.id + 1));
                    bw.write(System.lineSeparator());
                }
            }
            for (Centroid c : centroids) {
                bw.write(c.id + "," + c.getX() + "," + c.getY() + "," + "centroid");
                bw.write(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String line = "";
        String path = args[0];
        minPts = 4;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                String[] items = line.split(",");
                points.add(new Point(Double.parseDouble(items[1]), Double.parseDouble(items[2]), items[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        n = points.size();
        eps = findOptEps();
        minPts = findOptMinPts();
        System.out.println("Estimated eps : " + eps);
        System.out.println("Estimated minPts : " + minPts);
        final long startTime = System.currentTimeMillis();
        ArrayList<ArrayList<Point>> clusters = dbScan(minPts);
        ArrayList<Centroid> centroids = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            double newX = 0;
            double newY = 0;
            Centroid centroid = new Centroid(new Pos(newX, newY), i + 1);
            for (Point p : clusters.get(i)) {
                centroid.addPoint(p);
                newX += p.getX();
                newY += p.getY();
            }
            newX /= clusters.get(i).size();
            newY /= clusters.get(i).size();
            centroid.setPos(newX, newY);
            centroids.add(centroid);
        }
        centroids = kMeans(centroids);
        final long endTime = System.currentTimeMillis();
        System.out.println("Number of clusters : " + centroids.size());
        for (Centroid c : centroids) {
            System.out.print("Cluster #" + c.id + " =>  ");
            for (Point p : c.getPoints()) {
                System.out.print(p.name + " ");
            }
            System.out.println();
        }

        final long elapsed = endTime - startTime;
        // System.out.println("time : " + (double) elapsed / 1000 + " seconds");
        // fileOut(centroids);
    }
}
