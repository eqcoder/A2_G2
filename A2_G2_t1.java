import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class A2_G2_t1 {
    public static ArrayList<Point> points = new ArrayList<>();
    public static int n;

    private static ArrayList<Centroid> kMeans(int k) {
        if (k == 0) {
            double bestSilhoutteCoef = 0, silhoutteCoef = 0;
            int maxK = 0, bestK = 0;
            maxK = (int) Math.log((double) n) * 5;
            ArrayList<Centroid>[] centroids = new ArrayList[maxK];
            ArrayList<Centroid> tempCentroids = new ArrayList<>();
            for (int i = 0; i < maxK; i++) {
                centroids[i] = new ArrayList<>();
                tempCentroids = chooseCentroids(tempCentroids, i + 1);
                for (Centroid c : tempCentroids) {
                    centroids[i].add(new Centroid(c, c.id));
                }
                cluster(centroids[i]);
                silhoutteCoef = computeSilhouetteCoef(centroids[i], i + 1);
                if (silhoutteCoef > bestSilhoutteCoef && i != 0) {
                    bestSilhoutteCoef = silhoutteCoef;
                    bestK = i;
                }
            }
            System.out.println("estimated k : " + (bestK + 1));
            return centroids[bestK];
        } else

        {
            ArrayList<Centroid> centroids = chooseCentroids(new ArrayList<Centroid>(), k);
            cluster(centroids);
            return centroids;
        }
    }

    private static ArrayList<Centroid> chooseCentroids(ArrayList<Centroid> centroids, int k) {
        Random random = new Random();
        Point choice;
        int c = centroids.size();
        for (; c < k; c++) {
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
            centroids.add(new Centroid(choice, c));
        }
        return centroids;
    }

    private static void cluster(ArrayList<Centroid> centroids) {
        boolean isChanged = false;
        int closestCentroid;
        for (Point p : points) {
            closestCentroid = findClosestCentroid(p, centroids);
            centroids.get(closestCentroid).addPoint(p);
            p.setCluster(closestCentroid);
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

    private static double computeDistance(Pos p1, Pos p2) {
        double distance = Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
        return distance;
    }

    private static Integer findClosestCentroid(Pos p, ArrayList<Centroid> centroids) {
        double minDistance = Double.POSITIVE_INFINITY;
        int closestCentroid = 0;
        double distance;
        for (Centroid c : centroids) {
            distance = computeDistance(c, p);
            if (distance < minDistance) {
                minDistance = distance;
                closestCentroid = c.id;
            }
        }
        return closestCentroid;
    }

    private static double computeSilhouetteCoefA(Centroid c, Point p) {
        double A = 0;
        for (Point p1 : c.getPoints()) {
            if (p1 == p)
                continue;
            A += computeDistance(p1, p);
        }
        if (c.getPoints().size() > 1) {
            A = A / (double) (c.getPoints().size() - 1);
        }
        return A;
    }

    private static double computeSilhouetteCoefB(Point p, ArrayList<Centroid> centroids) {
        double B = Double.MAX_VALUE, dist = 0;
        for (Centroid c : centroids) {
            if (p.cluster == c.id || c.getPoints().size() == 0)
                continue;
            for (Point p1 : c.getPoints()) {
                dist += computeDistance(p1, p);
            }
            dist /= (double) c.getPoints().size();
            if (dist < B) {
                B = dist;
            }
            dist = 0;
        }
        return B;
    }

    private static double computeSilhouetteCoef(ArrayList<Centroid> centroids, int k) {
        double clusterMean = 0;
        double totMean = 0;
        for (Centroid c : centroids) {
            for (Point p : c.getPoints()) {
                double A = computeSilhouetteCoefA(c, p);
                double B = computeSilhouetteCoefB(p, centroids);
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

    public static class Point extends Pos {
        String name;
        int cluster;

        Point(double x, double y, String name) {
            super(x, y);
            this.name = name;
        }

        public void setCluster(int cluster) {
            this.cluster = cluster;
        }
    }

    public static void fileOut(ArrayList<Centroid> centroids) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("kmeans.csv"))) {
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

    public static void main(String args[]) {
        String file = args[0];
        int k = (args.length == 2) ? Integer.parseInt(args[1]) : 0;
        String line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                String[] items = line.split(",");
                points.add(new Point(Double.parseDouble(items[1]), Double.parseDouble(items[2]), items[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        n = points.size();

        final long startTime = System.currentTimeMillis();
        ArrayList<Centroid> centroids = kMeans(k);
        final long endTime = System.currentTimeMillis();

        for (Centroid c : centroids) {
            System.out.print(" cluster #" + (c.id + 1) + " =>  ");
            for (Point p : c.getPoints()) {
                System.out.print(p.name);
            }
            System.out.println();
        }

        final long elapsed = endTime - startTime;
        // System.out.println("time : " + (double) elapsed / 1000 + " seconds");
        // fileOut(centroids);
    }
}