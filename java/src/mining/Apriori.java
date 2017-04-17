/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.util.*;
import org.jblas.DoubleMatrix;

/**
 *
 * @author Bang
 */
public class Apriori {

    private final double[] thresholds;
    private final ArrayList<DrivingFeature> drivingFeatures;
    private final DoubleMatrix DMMat;
    private final DoubleMatrix DMLabel;
    private final double[][] mat;
    private final int nrows;
    private final int ncols;

    private DoubleMatrix tempMat;

    public Apriori(ArrayList<DrivingFeature> drivingFeatures, double[][] mat, double[] labels, double[] thresholds) {
        this.drivingFeatures = drivingFeatures;
        this.nrows = mat.length;
        this.ncols = mat[0].length;
        this.mat = mat;

        this.DMMat = new DoubleMatrix(mat);
        this.DMLabel = new DoubleMatrix(labels);

        // thresholds = {supp_threshold, lift_threshold, conf_threshold}
        this.thresholds = thresholds;
    }

    public ArrayList<AprioriFeature> runApriori(int maxLength) {

        long t0 = System.currentTimeMillis();
        System.out.println("...[Apriori] size of the input matrix: " + mat.length + " X " + mat[0].length);

        // Define the initial set of features
        ArrayList<AprioriFeature> S = new ArrayList<>();

        for (int i = 0; i < mat[0].length; i++) {
            double support =drivingFeatures.get(i).getSupport();
            double lift =drivingFeatures.get(i).getLift();
            double fconfidence =drivingFeatures.get(i).getFConfidence();
            double rconfidence =drivingFeatures.get(i).getRConfidence();
            
            S.add(new AprioriFeature(i, support, lift, fconfidence, rconfidence));
        }

        // Define frontier. Frontier is the set of features whose length is L and passes significant test
        // Copy the initial set of features to the frontier
        ArrayList<AprioriFeature> frontier = new ArrayList<>(S);

        int l = 2;
        // While there are features still left to explore
        while (frontier.size() > 0) {
            if (l - 1 == maxLength) {
                break;
            }
            // Candidates to form the frontier with length L+1
            ArrayList<AprioriFeature> candidates = apriori_gen_join(frontier);
            candidates = apriori_gen_prune(candidates, frontier);

            System.out.println("...[Apriori] number of candidates (length " + l + "): " + candidates.size());

            frontier = new ArrayList<>();
            for (AprioriFeature f : candidates) {
                // Count frequency
                computeMetrics(DMMat, f, DMLabel);

                // Check if it passes minimum support threshold
                if (f.getSupport() <= thresholds[0]) {
                    continue;
                } else {
                    // Add all features whose support is above threshold, add to candidates
                    frontier.add(f);
                    if (f.getFConfidence() > thresholds[2]) {
                        // Store 
                        f.setDatArray(this.tempMat);
                        // If the metric is above the threshold, current feature is statistically significant
                        S.add(f);
                    }
                }
            }
            l = l + 1;
        }

        long t1 = System.currentTimeMillis();
        System.out.println("...[Apriori] evaluation done in: " + String.valueOf(t1 - t0) + " msec, with " + S.size() + " features found");
        return S;
    }

    private ArrayList<AprioriFeature> apriori_gen_join(ArrayList<AprioriFeature> front) {
        ArrayList<AprioriFeature> candidates = new ArrayList<>();
        for (int i = 0; i < front.size(); i++) {
            AprioriFeature f1 = front.get(i);

            for (int j = i + 1; j < front.size(); j++) {
                AprioriFeature f2 = front.get(j);

                HashSet<Integer> elm1 = new HashSet<>(f1.getElements());
                HashSet<Integer> elm2 = new HashSet<>(f2.getElements());

                if (!elm1.equals(elm2)) {
                    elm1.addAll(elm2);
                    candidates.add(new AprioriFeature(elm1, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
                }
            }
        }
        return candidates;
    }

    private ArrayList<AprioriFeature> apriori_gen_prune(ArrayList<AprioriFeature> cand, ArrayList<AprioriFeature> prev) {
        // cand is a set of Features of length L and prev is a set of Features of length L-1
        ArrayList<AprioriFeature> candidates = new ArrayList<>();

        for (AprioriFeature f : cand) {
            ArrayList<Integer> list = new ArrayList(f.getElements());

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
                for (AprioriFeature prevF : prev) {
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
     * Computes the support, lift, and confidences of the feature and sets the
     * metrics within the feature.
     *
     * @param dataMat
     * @param feature
     * @param label
     */
    private AprioriFeature computeMetrics(DoubleMatrix dataMat, AprioriFeature feature, DoubleMatrix label) {

        int numFeat = feature.getElements().size();
        int[] indices = new int[numFeat];
        int i = 0;
        for (Integer ind : feature.getElements()) {
            indices[i] = ind;
            i++;
        }

        DoubleMatrix cond = DoubleMatrix.zeros(ncols, 1);
        cond.put(indices, 0, 1.0);

        double cnt_all = nrows;
        double cnt_F = 0;
        double cnt_S = 0;
        double cnt_SF = 0;

        DoubleMatrix countMat = dataMat.mmul(cond);
        countMat = countMat.eq(numFeat);

        this.tempMat = countMat;

        cnt_SF = label.dot(countMat);
        cnt_S = label.norm1();
        cnt_F = countMat.norm1();

        double support = cnt_SF / cnt_all;
        double lift = (cnt_SF / cnt_S) / (cnt_F / cnt_all);
        double conf_given_F = (cnt_SF) / (cnt_F);   // confidence (feature -> selection)
        double conf_given_S = (cnt_SF) / (cnt_S);   // confidence (selection -> feature)

        return new AprioriFeature(feature.getElements(), support, lift, conf_given_F, conf_given_S);
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

    public class AprioriFeature extends CompoundFeature{

        private DoubleMatrix datArray;

        public AprioriFeature(int i, double support, double lift, double fconfidence, double rconfidence) {
            this(Arrays.asList(new Integer[]{i}), support, lift, fconfidence, rconfidence);
        }

        public AprioriFeature(Collection<Integer> elems, double support, double lift, double fconfidence, double rconfidence) {
            super(elems, support, lift, fconfidence, rconfidence);
        }

        public void setDatArray(DoubleMatrix m) {
            datArray = m;
        }

        public DoubleMatrix getDatArray() {
            return datArray;
        }
    }
}
