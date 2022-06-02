import java.awt.image.BufferedImage;

public class KMeans {
    public static int[] applyKMeans(BufferedImage image, int K) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixelArray = new int[width * height];
        Record[] recordPoints = new Record[width * height];
        Centroid[] centroidPoints = new Centroid[K];
        image.getRGB(0, 0, width, height, pixelArray, 0, width);
        for (int i = 0; i < width * height; i++) {
            recordPoints[i] = new Record(pixelArray[i], centroidPoints);
        }
        // Here we have to choose our centroids
        centroidPoints[0] = new Centroid(recordPoints[(int) (Math.random() * centroidPoints.length)].location);
        // First point is one of the data points chosen at random
        for (int i = 1; i < K; i++) {
            // Compute the total weight of all items together.
            // This can be skipped of course if sum is already 1.
            double totalWeight = 0.0;
            for (Record record : recordPoints) {
                totalWeight += record.getWeight(i);
            }

            // Now choose a random item.
            int idx = 0;
            for (double r = Math.random() * totalWeight; idx < recordPoints.length - 1; ++idx) {
                r -= recordPoints[idx].getWeight(i);
                if (r <= 0.0) break;
            }
            centroidPoints[i] = new Centroid(recordPoints[idx].location);
            // https://stackoverflow.com/questions/6737283/weighted-randomness-in-java
        }
        // end choosing centroids
        int iterations = 0;
        boolean centroidsStayedPut = false;
        while (!centroidsStayedPut) {
            centroidsStayedPut = true;
            int[][] averageCentroidLocation = new int[K][3];
            int[] centroidCount = new int[K];
            for (int i = 0; i < K; i++) {
                averageCentroidLocation[i] = new int[]{0,0,0};
                centroidCount[i] = 0;
            }
            for (Record recordPoint : recordPoints) {
                int centroidIndex = recordPoint.determineCentroid();
                averageCentroidLocation[centroidIndex] =
                        Colors.addArrays(averageCentroidLocation[centroidIndex], recordPoint.location);
                centroidCount[centroidIndex]++;
            }
            for (int i = 0; i < K; i++) {
                averageCentroidLocation[i] = Colors.scaleArray(averageCentroidLocation[i],
                        1.0f/centroidCount[i]);
                centroidsStayedPut = centroidsStayedPut && centroidPoints[i].setLocation(averageCentroidLocation[i]);
            }
            iterations++;
        }
        int[] returnValue = new int[K];
        for (int i = 0; i < K; i++) {
            returnValue[i] = Colors.combineColors(centroidPoints[i].location);
        }
        System.out.println("Found optimal colors in "+iterations+" iterations.");
        return returnValue;
    }
}

class Centroid {
    public int[] location;
    Centroid(int[] location) {
        this.location = location;
    }

    public boolean setLocation(int[] location) {
        if (Colors.combineColors(this.location) != Colors.combineColors(location)) {
            this.location = location;
            return false;
        }
        return true;
    }
}

class Record {
    public int[] location; // location in RGB space
    private int closestCentroid = 0;
    private final Centroid[] centroids;
    Record(int color, Centroid[] centroids) {
        this.centroids = centroids;
        location = Colors.splitColor(color);
    }

    public int determineCentroid(int maxCentroid) {
        double diff = Colors.getDifferenceBetweenColors(centroids[closestCentroid].location, location);
        for (int index = 0; index < maxCentroid; index++) {
            double lDiff = Colors.getDifferenceBetweenColors(centroids[index].location, this.location);
            if (lDiff < diff) {
                diff = lDiff;
                closestCentroid = index;
            }
        }
        return closestCentroid;
    }

    public int determineCentroid() {
        return determineCentroid(centroids.length);
    }

    public double getWeight(int maxCentroid) {
        determineCentroid(maxCentroid);
        return Math.pow(Colors.getDifferenceBetweenColors(location, centroids[closestCentroid].location), 2) /
                Math.pow(0xFFFFFF, 2); // Normalize so max distance = 1.0
    }
}