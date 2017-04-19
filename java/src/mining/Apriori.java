/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.util.*;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;

/**
 *
 * @author Bang
 */
public class Apriori {

    private double[] thresholds;
    ArrayList<DrivingFeature> drivingFeatures;
    RealMatrix DMMat;
    RealVector DMLabel;
    int nrows;
    int ncols;
    ArrayList<Integer> skip;

    RealMatrix tempMat;

    public Apriori() {
    }

    public Apriori(List<DrivingFeature> drivingFeatures, double[][] mat, double[] labels, double[] thresholds) {
        this.drivingFeatures = new ArrayList(drivingFeatures);
        this.nrows = mat.length;
        this.ncols = mat[0].length;

        this.DMMat = MatrixUtils.createRealMatrix(mat);
        this.DMLabel = MatrixUtils.createRealVector(labels);

        // thresholds = {supp_threshold, lift_threshold, conf_threshold}
        this.thresholds = thresholds;
    }

    public ArrayList<Feature> runApriori(int maxLength) {

        long t0 = System.currentTimeMillis();
        System.out.println("...[Apriori] size of the input matrix: " + nrows + " X " + ncols);

        // Define the initial set of features
        ArrayList<Feature> S = new ArrayList<>();
        ArrayList<Feature> out = new ArrayList<>();

        for (int i = 0; i < ncols; i++) {
            Feature newFeat = new Feature(i);
            // Count frequency
            double[] metrics = drivingFeatures.get(i).getMetrics();
            if(metrics[0] > thresholds[0] && metrics[2] > thresholds[2]){
                out.add(newFeat);
            }
            newFeat.setMetrics(metrics);
            newFeat.setArray(DMMat.getColumn(i));
            S.add(newFeat);
        }

        // Define frontier. Frontier is the set of features whose length is L and passes significant test
        ArrayList<Feature> frontier = new ArrayList<>();

        // Copy the initial set of features to the frontier
        for (Feature s : S) {
            frontier.add(s);
        }

        int l = 2;
        // While there are features still left to explore
        while (frontier.size() > 0) {
            if (l - 1 == maxLength) {
                break;
            }
            System.out.println("...[Apriori] " + out.size() + " features found");
            // Candidates to form the frontier with length L+1
            ArrayList<Feature> candidates = apriori_gen_join(frontier);
            candidates = apriori_gen_prune(candidates, frontier);

            System.out.println("...[Apriori] number of candidates (length " + l + "): " + candidates.size());

            frontier = new ArrayList<>();
            for (Feature f : candidates) {
                // Count frequency
                double[] metrics = computeMetrics(DMMat, f, DMLabel);

                // Check if it passes minimum support threshold
                if (metrics[0] <= thresholds[0]) {
                    continue;
                } else {
                    // Add all features whose support is above threshold, add to candidates
                    frontier.add(f);
                    if (metrics[2] > thresholds[2]) {
                        // If the metric is above the threshold, current feature is statistically significant
                        S.add(f);
                        out.add(f);
                    }
                }
            }
            System.out.println("...[Apriori] number of valid candidates (length " + l + "): " + frontier.size());
            l = l + 1;
        }

        long t1 = System.currentTimeMillis();
        System.out.println("...[Apriori] evaluation done in: " + String.valueOf(t1 - t0) + " msec, with " + out.size() + " features found");
        return out;
    }

    public ArrayList<Feature> sortFeatures(ArrayList<Feature> features) {
        ArrayList<Feature> sorted = new ArrayList<>();

        double value = 0;
        double maxval = 1000000000;
        double minval = -1;
        for (int i = 0; i < features.size(); i++) {
            Apriori.Feature feat1 = features.get(i);
            if (i == 0) {
                sorted.add(feat1);
                continue;
            }
            value = feat1.getMetrics()[2]; // Confidence (feature->selection)
            maxval = sorted.get(0).getMetrics()[2];
            minval = sorted.get(sorted.size() - 1).getMetrics()[2];

            if (value >= maxval) {
                sorted.add(0, feat1);
            } else if (value <= minval) {
                sorted.add(feat1);
            } else {
                for (int j = 0; j < sorted.size(); j++) {
                    double refval = 0;
                    double refval2 = 0;
                    refval = sorted.get(j).metrics[2];
                    refval2 = sorted.get(j + 1).metrics[2];
                    if (value <= refval && value > refval2) {
                        sorted.add(j + 1, feat1);
                        break;
                    }
                }
            }
        }
        return sorted;
    }

    private ArrayList<Feature> apriori_gen_join(ArrayList<Feature> front) {
        int length = front.get(0).getElements().size();
        ArrayList<Feature> candidates = new ArrayList<>();
        for (int i = 0; i < front.size(); i++) {
            Feature f1 = front.get(i);

            for (int j = i + 1; j < front.size(); j++) {
                Feature f2 = front.get(j);
                boolean match = true;

                ArrayList<Integer> elm1 = new ArrayList<>();
                for (int temp : f1.getElements()) {
                    elm1.add(temp);
                }
                ArrayList<Integer> elm2 = new ArrayList<>();
                for (int temp : f2.getElements()) {
                    elm2.add(temp);
                }

                for (int k = 0; k < length - 1; k++) {
                    if (!Objects.equals(elm1.get(k), elm2.get(k))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    elm1.add(elm2.get(length - 1));
                    candidates.add(new Feature(elm1));
                }
            }
        }

        return candidates;
    }

    private ArrayList<Feature> apriori_gen_prune(ArrayList<Feature> cand, ArrayList<Feature> prev) {
        // cand is a set of Features of length L and prev is a set of Features of length L-1
        ArrayList<Feature> candidates = new ArrayList<>();

        for (Feature f : cand) {
            ArrayList<Integer> list = f.getElements();

            Set<Integer> subset = new HashSet<Integer>(list);
            int length = subset.size();
            boolean included = true;

            for (int i = 0; i < length; i++) {
                // All subsets of length L-1 should be included in prev
                int elm = list.get(i);
                // Create a subset of length L-1
                subset.remove(elm);

                boolean match_found = false;
                // Test if the subset is included in prev
                for (Feature prevF : prev) {
                    Set<Integer> prevset = new HashSet<Integer>(prevF.getElements());
                    if (same(subset, prevset)) {
                        match_found = true;
                        break;
                    }
                }
                subset.add(elm);
                if (!match_found) {
                    included = false;
                    break;
                }
            }

            if (included) {
                // If all subsets of length L-1 of current feature are included in prev, add to the candidates
                candidates.add(f);
            }
        }
        return candidates;
    }

    public boolean same(Set<?> set1, Set<?> set2) {
        if (set1 == null || set2 == null) {
            return false;
        }
        if (set1.size() != set2.size()) {
            return false;
        }
        return set1.containsAll(set2);
    }

    public boolean contains(int[] arr, int i) {
        for (int a : arr) {
            if (a == i) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes and sets the metrics of the feature
     *
     * @param dataMat
     * @param feat
     * @param label
     * @return
     */
    public double[] computeMetrics(RealMatrix dataMat, Feature feat, RealVector label) {

        int numFeat = feat.getElements().size();
        ArrayRealVector cond = new ArrayRealVector(ncols);
        cond.set(0.0);
        for (int i = 0; i < numFeat; i++) {
            cond.setEntry(feat.getElements().get(i), 1);
        }

        RealVector countVec = dataMat.operate(cond);
        for (int i = 0; i < countVec.getDimension(); i++) {
            if (countVec.getEntry(i) != numFeat) {
                countVec.setEntry(i, 0.0);
            }else{
                countVec.setEntry(i, 1.0);
            }
        }

        feat.setArray(countVec.toArray());

        double cnt_SF = label.dotProduct(countVec);
        double cnt_S = label.getL1Norm();
        double cnt_F = countVec.getL1Norm();

        double[] metrics = new double[4];
        double support = cnt_SF / (double) nrows;
        double lift = (cnt_SF / cnt_S) / (cnt_F / (double)  nrows);
        double conf_given_F = (cnt_SF) / (cnt_F);   // confidence (feature -> selection)
        double conf_given_S = (cnt_SF) / (cnt_S);   // confidence (selection -> feature)

        metrics[0] = support;
        metrics[1] = lift;
        metrics[2] = conf_given_F;
        metrics[3] = conf_given_S;

        feat.setMetrics(metrics);

        return metrics;
    }

    public ArrayList<boolean[]> intMatrix2BoolArray(ArrayList<int[][]> input) {

        ArrayList<boolean[]> boolArray = new ArrayList<>();
        int len = input.get(0).length * input.get(0)[0].length;

        for (int i = 0; i < input.size(); i++) {

            boolean[] tmpArray = new boolean[len];
            int cnt = 0;
            for (int j = 0; j < input.get(i).length; j++) {
                for (int k = 0; k < input.get(i)[j].length; k++) {
                    if (input.get(i)[j][k] == 1) {
                        tmpArray[cnt] = true;
                    } else {
                        tmpArray[cnt] = false;
                    }
                    cnt++;
                }
            }

            boolArray.add(tmpArray);
        }

        return boolArray;
    }

    class Feature {

        private final ArrayList<Integer> elements;
        private double[] metrics;
        private double[] datArray;

        public Feature(ArrayList<Integer> elem) {
            this.elements = elem;
        }

        public Feature(int i) {
            this.elements = new ArrayList<>();
            this.elements.add(i);
        }

        public ArrayList<Integer> getElements() {
            return this.elements;
        }

        public void setMetrics(double[] metrics) {
            this.metrics = metrics;
        }

        public double[] getMetrics() {
            return this.metrics;
        }

        public void setArray(double[] m) {
            datArray = m;
        }

        public double[] getArray() {
            return datArray;
        }

    }

}
