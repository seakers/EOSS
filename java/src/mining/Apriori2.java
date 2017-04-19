/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.util.*;
import org.hipparchus.util.Combinations;

/**
 *
 * @author Hitomi
 */
public class Apriori2 {

    /**
     * The base features that are combined to create the Hasse diagram in the
     * Apriori algorithm. Each BitSet corresponds to a feature and contains the
     * binary vector of the observations that match the feature
     *
     */
    private BitSet[] baseFeaturesBit;
    
    /**
     * The features given to the Apriori algorithm
     *
     */
    private ArrayList<DrivingFeature> baseFeatures;

    /**
     * The features found by the Apriori algorithm that exceed the necessary
     * support and confidence thresholds
     */
    private ArrayList<AprioriFeature> viableFeatures;

    /**
     *
     * @param baseFeatures an array of BitSet where each BitSet corresponds to a
     * feature and contains the binary vector of the observations that match the
     * feature
     * @param mat
     */
    public Apriori2(double[][] mat) {
        this.baseFeaturesBit = new BitSet[mat.length];
        for (int c = 0; c < mat[0].length; c++) {
            BitSet bs = new BitSet(mat.length);
            for (int r = 0; r < mat.length; r++) {
                if (mat[r][c] > 0.0001) {
                    bs.set(r, true);
                }
            }
            baseFeaturesBit[c] = bs;
        }
    }

    /**
     *
     * @param drivingFeaturesA
     * @param labels a discrete value vector containing the labels of the
     * observations
     * @param thresholds threshold for support, lift, and confidence
     * @param maxLength
     * @return
     */
    public void run(List<DrivingFeature> drivingFeatures, double[] labelsA, double[] thresholds, int maxLength) {

        //convert input
        DrivingFeature[] drivingFeaturesA = new DrivingFeature[drivingFeatures.size()];
        for (int i = 0; i < drivingFeatures.size(); i++) {
            drivingFeaturesA[i] = drivingFeatures.get(i);
        }

        BitSet labels = new BitSet(labelsA.length);
        for (int i = 0; i < labelsA.length; i++) {
            if (labelsA[i] > 0.0001) {
                labels.set(i, true);
            }
        }

        long t0 = System.currentTimeMillis();
        int numberOfObservations = baseFeaturesBit.length;
        int numberOfFeatures = drivingFeaturesA.length;

        System.out.println("...[Apriori2] size of the input matrix: " + numberOfObservations + " X " + numberOfFeatures);

        // Define the initial set of features
        viableFeatures = new ArrayList<>();

        // Define front. front is the set of features whose length is L and passes significant test
        ArrayList<BitSet> front = new ArrayList();
        for (int i = 0; i < numberOfFeatures; i++) {

            double[] metrics = drivingFeaturesA[i].getMetrics();
            if (metrics[0] > thresholds[0]) {
                if (metrics[2] > thresholds[2]) {
                    //only add feature to output list if it passes support and confidence thresholds
                    AprioriFeature feat = new AprioriFeature(baseFeaturesBit[i], metrics[0], metrics[1], metrics[2], metrics[3]);
                    viableFeatures.add(feat);
                }

                BitSet featureCombo = new BitSet(numberOfFeatures);
                featureCombo.set(i, true);
                front.add(featureCombo);
            }
        }

        int currentLength = 2;
        // While there are features still left to explore
        while (front.size() > 0) {
            if (currentLength - 1 == maxLength) {
                break;
            }
            System.out.println("...[Apriori2] " + viableFeatures.size() + " features found");
            // Candidates to form the frontier with length L+1
            //updated front with new instance only containing the L+1 combinations of features
            ArrayList<BitSet> candidates = join(front, numberOfFeatures);
            front.clear();

            System.out.println("...[Apriori2] number of candidates (length " + currentLength + "): " + candidates.size());

            for (BitSet featureCombo : candidates) {
                int ind = featureCombo.nextSetBit(0);
                BitSet matches = (BitSet) baseFeaturesBit[ind].clone();

                //find feature indices
                for (int j = featureCombo.nextSetBit(ind + 1); j != -1; j = featureCombo.nextSetBit(j + 1)) {
                    matches.and(baseFeaturesBit[j]);
                }

                //compute the support
                BitSet copyMatches = (BitSet) matches.clone();
                copyMatches.and(labels);
                double cnt_SF = (double) copyMatches.cardinality();
                double support = cnt_SF / (double) numberOfObservations;

                // Check if it passes minimum support threshold
                if (support > thresholds[0]) {
                    // Add all features whose support is above threshold, add to candidates
                    front.add(featureCombo);

                    //compute the confidence and lift
                    double cnt_S = (double) labels.cardinality();
                    double cnt_F = (double) matches.cardinality();
                    double lift = (cnt_SF / cnt_S) / (cnt_F / (double) numberOfObservations);
                    double conf_given_F = (cnt_SF) / (cnt_F);   // confidence (feature -> selection)
                    double conf_given_S = (cnt_SF) / (cnt_S);   // confidence (selection -> feature)

                    if (conf_given_F > thresholds[2]) {
                        // If the metric is above the threshold, current feature is statistically significant
                        viableFeatures.add(new AprioriFeature(featureCombo, support, lift, conf_given_F, conf_given_S));
                    }//else{
//                        System.out.println("hey");
//                    }

                }
            }
            System.out.println("...[Apriori2] number of valid candidates (length " + currentLength + "): " + front.size());
            currentLength = currentLength + 1;
        }

        long t1 = System.currentTimeMillis();
        System.out.println("...[Apriori2] evaluation done in: " + String.valueOf(t1 - t0) + " msec, with " + viableFeatures.size() + " features found");
    }

    /**
     * Gets the top n features according to the specified metric in descending
     * order. If n is greater than the number of features found by Apriori, all
     * features will be returned.
     *
     * @param n the number of features desired
     * @param metric the metric used to sort the features
     * @return the top n features according to the specified metric in
     * descending order
     */
    public List<DrivingFeature2> getTopFeatures(int n, FeatureMetric metric) {
        Collections.sort(viableFeatures, new FeatureComparator(metric).reversed());
        if (n > viableFeatures.size()) {
            n = viableFeatures.size();
        }

        ArrayList<DrivingFeature2> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            AprioriFeature apFeature = viableFeatures.get(i);
            //build the binary array taht is 1 for each solution matching the feature
            StringBuilder sb = new StringBuilder();
            BitSet featureCombo = apFeature.getMatches();
            int ind = featureCombo.nextSetBit(0);
            BitSet matches = (BitSet) baseFeaturesBit[ind].clone();
            sb.append(baseFeatures.get(ind).getName());

            //find feature indices
            for (int j = featureCombo.nextSetBit(ind + 1); j != -1; j = featureCombo.nextSetBit(j + 1)) {
                sb.append(":");
                sb.append(baseFeatures.get(j).getName());
                matches.and(baseFeaturesBit[j]);
            }

            out.add(new DrivingFeature2(sb.toString(), matches, 
                    apFeature.getSupport(), apFeature.getLift(), 
                    apFeature.getFConfidence(), apFeature.getRConfidence()));
        }
        return out;
    }

    /**
     * Joins the features together using the Apriori algorithm. Ensures that
     * duplicate feature are not generated and that features that are subsets of
     * features that were previously filtered out aren't generated. Ordering of
     * the bitset in the arraylist of the front is important. It should be
     * ordered such that 10000 (A) comes before 010000 (B) or 11010 (ABD) comes
     * before 00111 (CDE)
     *
     * Example1: if AB and BC both surpass the support threshold, ABC is only
     * generated once
     *
     * Example2: if AB was already filtered out but BC surpasses the support
     * threshold, ABC should not and will not be generated
     *
     * @param front is an arraylist of bitsets corresponding to which features
     * are being combined. For example in a set of {A, B C, D, E} 10001 is
     * equivalent to AE
     * @param numberOfFeatures the maximum number of features being considered
     * in the entire Apriori algorithm
     * @return the next front of potential feature combinations. These need to
     * be tested against the support threshold
     */
    private ArrayList<BitSet> join(ArrayList<BitSet> front, int numberOfFeatures) {
        ArrayList<BitSet> candidates = new ArrayList<>();

        //The new candidates must be checked against the current front to make 
        //sure that each length L subset in the new candidates must already
        //exist in the front to make sure that ABC never gets added if AB, AB,
        //or BC is missing from the front
        HashSet<BitSet> frontSet = new HashSet<>(front);

        for (int i = 0; i < front.size(); i++) {
            BitSet f1 = front.get(i);
            int lastSetIndex1 = f1.previousSetBit(numberOfFeatures - 1);
            for (int j = i + 1; j < front.size(); j++) {
                BitSet f2 = front.get(j);
                int lastSetIndex2 = f1.previousSetBit(numberOfFeatures - 1);

                //check to see that all the bits leading up to the minimum of the last set bits are equal
                //That is AB (11000) and AC (10100) should be combined but not AB (11000) and BC (01100)
                //AB and AC are combined because the first bits are equal
                //AB and BC are not combined because the first bits are not equal
                int index = Math.min(lastSetIndex1, lastSetIndex2);
                if (f1.get(0, index).equals(f2.get(0, index))) {
                    BitSet copy = (BitSet) f1.clone();
                    copy.or(f2);

                    if (checkSubsets(copy, frontSet, numberOfFeatures)) {
                        candidates.add(copy);
                    }
                } else {
                    //once AB is being considered against BC, the inner loop should break
                    //since the input front is assumed to be ordered, any set after BC is also incompatible with AB
                    break;
                }
            }
        }
        return candidates;
    }

    /**
     * The new candidates must be checked against the current front to make sure
     * that each length L subset in the new candidates must already exist in the
     * front to make sure that ABC never gets added if AB, AB, or BC is missing
     * from the front
     *
     * @param bs the length L bit set
     * @param toCheck a set of bit sets of length L-1 to check all subsets of L
     * against
     * @param numberOfFeatures the number of features
     * @return true if all subsets of the given bit set are in the set of bit
     * sets
     */
    private boolean checkSubsets(BitSet bs, HashSet<BitSet> toCheck, int numberOfFeatures) {
        // the indices that are set in the bitset
        int[] setIndices = new int[bs.cardinality()];
        int count = 0;
        for (int i = bs.nextSetBit(0); i != -1; i = bs.nextSetBit(i + 1)) {
            setIndices[count] = i;
            count++;
        }

        //create all combinations of n choose k
        Combinations subsets = new Combinations(bs.cardinality(), bs.cardinality() - 1);
        Iterator<int[]> iter = subsets.iterator();
        while (iter.hasNext()) {
            BitSet subBitSet = new BitSet(numberOfFeatures);
            int[] subsetIndices = iter.next();
            for (int i = 0; i < subsetIndices.length; i++) {
                subBitSet.set(setIndices[subsetIndices[i]], true);
            }

            if (!toCheck.contains(subBitSet)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A container for the bit set defining which base features create the
     * feature and its support, lift, and confidence metrics
     */
    private class AprioriFeature extends AbstractFeature {

        /**
         *
         * @param bitset of the base features that create this feature
         * @param support
         * @param lift
         * @param fconfidence
         * @param rconfidence
         */
        public AprioriFeature(BitSet bitset, double support, double lift, double fconfidence, double rconfidence) {
            super(bitset, support, lift, fconfidence, rconfidence);
        }

    }
}
