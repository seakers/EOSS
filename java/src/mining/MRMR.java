package mining;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MRMR {

    private BitSet[] dataFeatureMat;
    private BitSet label;
    int target_num_features;
    int numberOfObservations;
    List<DrivingFeature2> features;

    public ArrayList<DrivingFeature2> minRedundancyMaxRelevance(int numberOfObservations, BitSet[] dataMat, BitSet label, List<DrivingFeature2> features, int target_num_features) {

        long t0 = System.currentTimeMillis();
        System.out.println("...[mRMR] running mRMR");

        this.dataFeatureMat = dataMat;
        this.numberOfObservations = numberOfObservations;
        this.target_num_features = target_num_features;
        this.features = features;
        this.label = label;

        ArrayList<Integer> selectedFeatures = new ArrayList<>();

        while (selectedFeatures.size() < target_num_features) {

            int bestFeatInd = -1;
            double phi = Double.NEGATIVE_INFINITY;

            // Implement incremental search
            for (int i = 0; i < features.size(); i++) {

                if (selectedFeatures.contains(i)) {
                    continue;
                }

                double D = computeMutualInformation(this.dataFeatureMat[i], this.label);
                double R = 0;

                for (int j : selectedFeatures) {
                    R = R + computeMutualInformation(this.dataFeatureMat[i], this.dataFeatureMat[j]);
                }

                if (!selectedFeatures.isEmpty()) {
                    R /= (double) selectedFeatures.size();
                }

                if (D - R > phi) {
                    phi = D - R;
                    bestFeatInd = i;
                    }
                }
            selectedFeatures.add(bestFeatInd);
        }

        ArrayList<DrivingFeature2> out = new ArrayList<>();
        for (int index : selectedFeatures) {
            out.add(this.features.get(index));
        }

        long t1 = System.currentTimeMillis();
        System.out.println("...[mRMR] Finished running mRMR in " + String.valueOf(t1 - t0) + " msec");
        return out;
    }

    private double computeMutualInformation(BitSet set1, BitSet set2) {
        double x1 = set1.cardinality();
        double x2 = set2.cardinality();
        BitSet bx1x2 = (BitSet) set1.clone();
        bx1x2.and(set2);
        double x1x2 = bx1x2.cardinality();

        BitSet bnx1x2 = (BitSet) set1.clone();
        bnx1x2.flip(0, numberOfObservations);
        bnx1x2.and(set2);
        double nx1x2 = bnx1x2.cardinality();

        BitSet bx1nx2 = (BitSet) set2.clone();
        bx1nx2.flip(0, numberOfObservations);
        bx1nx2.and(set1);
        double x1nx2 = bx1nx2.cardinality();

        BitSet bnx1 = (BitSet) set1.clone();
        BitSet bnx2 = (BitSet) set2.clone();
        bnx1.flip(0, numberOfObservations);
        bnx2.flip(0, numberOfObservations);
        bnx1.and(bnx2);
        double nx1nx2 = bnx1.cardinality();

        double p_x1 = (double) x1 / numberOfObservations;
        double p_nx1 = (double) 1 - p_x1;
        double p_x2 = (double) x2 / numberOfObservations;
        double p_nx2 = (double) 1 - p_x2;
        double p_x1x2 = (double) x1x2 / numberOfObservations;
        double p_nx1x2 = (double) nx1x2 / numberOfObservations;
        double p_x1nx2 = (double) x1nx2 / numberOfObservations;
        double p_nx1nx2 = (double) nx1nx2 / numberOfObservations;
        
        double i1,i2,i3,i4;
        //handle cases when there p(x) = 0
        if (p_x1 == 0 || p_x2 == 0 || p_x1x2 == 0) {
            i1 = 0;
        }else{
            i1 = p_x1x2 * Math.log(p_x1x2 / (p_x1 * p_x2));
        }
        
        if (p_x1 == 0 || p_nx2 == 0 || p_x1nx2 == 0) {
            i2 = 0;
        }else{
            i2 = p_x1nx2 * Math.log(p_x1nx2 / (p_x1 * p_nx2));
        }
        
        if (p_nx1 == 0 || p_x2 == 0 || p_nx1x2 == 0) {
            i3 = 0;
        }else{
            i3 = p_nx1x2 * Math.log(p_nx1x2 / (p_nx1 * p_x2));
        }
        
        if (p_nx1 == 0 || p_nx2 == 0 || p_nx1nx2 == 0) {
            i4 = 0;
        }else{
            i4 = p_nx1nx2 * Math.log(p_nx1nx2 / (p_nx1 * p_nx2));
        }

        double sumI = i1 + i2 + i3 + i4;
        if(sumI<0){
            throw new IllegalStateException("Mutual information must be positive. Computed a negative value.");
        }else{
            return sumI;
        }
    }
}
