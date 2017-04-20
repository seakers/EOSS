package mining;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MRMR {

    private BitSet[] dataFeatureMat;
    private BitSet label;
    int target_num_features;
    int numberOfObservations;
    List<DrivingFeature2> features;
    ArrayList<Future> futures;

    private static final ExecutorService threadpool = Executors.newFixedThreadPool(DrivingFeaturesParams.numThreads);

    public ArrayList<DrivingFeature2> minRedundancyMaxRelevance(int numberOfObservations, BitSet[] dataMat, BitSet label, List<DrivingFeature2> features, int target_num_features) {

        long t0 = System.currentTimeMillis();
        System.out.println("...[mRMR] running mRMR");

        this.dataFeatureMat = dataMat;
        this.numberOfObservations = numberOfObservations;
        this.target_num_features = target_num_features;
        this.features = features;
        this.label = label;
        this.futures = new ArrayList<>();

        ArrayList<Integer> selectedFeatures = new ArrayList<>();

        int numSelected = 0;
        while (numSelected < target_num_features) {

            int bestFeatInd = -1;
            double phi = -10000;

            // Implement incremental search
            for (int i = 0; i < features.size(); i++) {

                if (selectedFeatures.contains(i)) {
                    continue;
                }

                double D = new MutualInformationCalculator(this.dataFeatureMat, this.label, i).call();
                double R = 0;

//                for (int featInd: selectedFeatures) {
//                    //R = R + getMutualInformation(this.dataFeatureMat, this.label, i, featInd);
//                    MutualInformationCalculator task = new MutualInformationCalculator(this.dataFeatureMat, this.label, i, featInd);
//                    futures.add(threadpool.submit(task));
//                }
//                
//                for(Future<Double> future:futures){
//                    try{
//                        double r = future.get();
//                        synchronized(this) {
//                            R = R + r;
//                        }                    
//                    }catch(Exception e){
//                        System.out.println(e.getMessage());
//                    }
//                }
                for (int featInd : selectedFeatures) {
                    R = R + new MutualInformationCalculator(this.dataFeatureMat, this.label, i, featInd).call();
                }

                if (numSelected != 0) {
                    R = (double) R / (double) numSelected;
                }

                if (D - R > phi) {
                    phi = D - R;
                    bestFeatInd = i;
                }
            }
            selectedFeatures.add(bestFeatInd);
            numSelected++;
        }

        ArrayList<DrivingFeature2> out = new ArrayList<>();
        for (int index : selectedFeatures) {
            out.add(this.features.get(index));
        }

        long t1 = System.currentTimeMillis();
        System.out.println("...[mRMR] Finished running mRMR in " + String.valueOf(t1 - t0) + " msec");
        threadpool.shutdown();
        return out;
    }

    public class MutualInformationCalculator implements Callable {

        private BitSet[] dataFeatureMat;
        private BitSet label;
        private int f1;
        private int f2;

        MutualInformationCalculator(BitSet[] dataMat, BitSet label, int f1) {
            this.dataFeatureMat = dataMat;
            this.label = label;
            this.f1 = f1;
            this.f2 = -1;
        }

        MutualInformationCalculator(BitSet[] dataMat, BitSet label, int f1, int f2) {
            this.dataFeatureMat = dataMat;
            this.label = label;
            this.f1 = f1;
            this.f2 = f2;
        }

        @Override
        public Double call() {
            double I;
            double x1, x2, x1x2, nx1x2, x1nx2, nx1nx2;

            BitSet feat1 = dataFeatureMat[f1];
            BitSet feat2;
            if (f2 < 0) {
                feat2 = label;
            } else {
                feat2 = dataFeatureMat[f2];
            }

            x1 = feat1.cardinality();
            x2 = label.cardinality();
            BitSet bx1x2 = (BitSet) feat1.clone();
            bx1x2.and(label);
            x1x2 = bx1x2.cardinality();

            BitSet bnx1x2 = (BitSet) feat1.clone();
            bnx1x2.flip(0, numberOfObservations);
            bnx1x2.and(label);
            nx1x2 = bnx1x2.cardinality();

            BitSet bx1nx2 = (BitSet) label;
            bx1nx2.flip(0, numberOfObservations);
            bx1nx2.and(label);
            x1nx2 = bx1nx2.cardinality();

            BitSet bnx1 = (BitSet) feat1.clone();
            BitSet bnx2 = (BitSet) feat2.clone();
            bnx1.flip(0, numberOfObservations);
            bnx2.flip(0, numberOfObservations);
            bnx1.and(bnx2);
            nx1nx2 = bnx1.cardinality();

            double p_x1 = (double) x1 / numberOfObservations;
            double p_nx1 = (double) 1 - p_x1;
            double p_x2 = (double) x2 / numberOfObservations;
            double p_nx2 = (double) 1 - p_x2;
            double p_x1x2 = (double) x1x2 / numberOfObservations;
            double p_nx1x2 = (double) nx1x2 / numberOfObservations;
            double p_x1nx2 = (double) x1nx2 / numberOfObservations;
            double p_nx1nx2 = (double) nx1nx2 / numberOfObservations;

            if (p_x1 == 0) {
                p_x1 = 0.00001;
            }
            if (p_nx1 == 0) {
                p_nx1 = 0.00001;
            }
            if (p_x2 == 0) {
                p_x2 = 0.00001;
            }
            if (p_nx2 == 0) {
                p_nx2 = 0.00001;
            }
            if (p_x1x2 == 0) {
                p_x1x2 = 0.00001;
            }
            if (p_nx1x2 == 0) {
                p_nx1x2 = 0.00001;
            }
            if (p_x1nx2 == 0) {
                p_x1nx2 = 0.00001;
            }
            if (p_nx1nx2 == 0) {
                p_nx1nx2 = 0.00001;
            }

            double i1 = p_x1x2 * Math.log(p_x1x2 / (p_x1 * p_x2));
            double i2 = p_x1nx2 * Math.log(p_x1nx2 / (p_x1 * p_nx2));
            double i3 = p_nx1x2 * Math.log(p_nx1x2 / (p_nx1 * p_x2));
            double i4 = p_nx1nx2 * Math.log(p_nx1nx2 / (p_nx1 * p_nx2));

            I = i1 + i2 + i3 + i4;
            return I;
        }

    }

}
