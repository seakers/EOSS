package mining;

import java.util.ArrayList;
import org.jblas.*;

public class MRMR {
	
	DoubleMatrix dataFeatureMat;
	int target_num_features;
	int ncols;
	int nrows;
	ArrayList<SetOfFeatures> featureSet;

    public ArrayList<SetOfFeatures> minRedundancyMaxRelevance(DoubleMatrix dataMat, ArrayList<SetOfFeatures> featureSet,int target_num_features){
        
    	this.ncols = dataMat.columns;
        this.nrows = dataMat.rows;
        this.dataFeatureMat = dataMat;
        this.target_num_features = target_num_features;
        this.featureSet=featureSet;

        int[] selectedFeatures = new int[target_num_features];
        
        int label_index = ncols-1;
        int numSelected = 0;
        
        while(numSelected < target_num_features){
        	
            int bestFeatInd = 0;
            double phi = -10000;
            
            
            // Implement incremental search
            for(int i=0;i<featureSet.size();i++){
            	
            	boolean contains=false;
            	for(int featInd:selectedFeatures){
            		if(featInd==i){
            			contains=true;
            			break;
            		}
            	}
            	if(contains){
            		continue;
            	}

            	
                double D = getMutualInformation(dataFeatureMat,featureSet.get(i).getIndices(),label_index);
                double R = 0;

                for (int featInd: selectedFeatures) {
                    R = R + getMutualInformation(dataFeatureMat,featureSet.get(i).getIndices(), featureSet.get(featInd).getIndices());
                }
                if(numSelected!=0){
                   R = (double) R/numSelected;
                }
  
                if(D-R > phi){
                    phi = D-R;
                    bestFeatInd = i;
                }
            }
            selectedFeatures[numSelected] = bestFeatInd;
            numSelected++;
        }
        
        ArrayList<SetOfFeatures> selectedFeaturesOutput = new ArrayList<>();
        for(int index:selectedFeatures){
        	selectedFeaturesOutput.add(featureSet.get(index));
        }
        return selectedFeaturesOutput;
    }  


    

    
    public double getMutualInformation(DoubleMatrix dataFeatureMat, int[] f1, int f2){
    	int[] temp = new int[1];
    	temp[0] = f2;
    	return getMutualInformation(dataFeatureMat,f1,temp);
    }
    
    public double getMutualInformation(DoubleMatrix dataFeatureMat, int[] f1, int[] f2){
        
        double I;
        double n= (double) nrows;
        
        DoubleMatrix features = DoubleMatrix.zeros(ncols,2);
        features.put(f1,0, 1.0);
        features.put(f2,1, 1.0);
        
        DoubleMatrix feature_sat = dataFeatureMat.mmul(features);
        
        int l1 = f1.length;
        int l2 = f2.length;
        
        DoubleMatrix feat1_sat = feature_sat.getColumn(0);
        DoubleMatrix feat2_sat = feature_sat.getColumn(1);
        
        double x1 = feat1_sat.eq(l1).norm1();
        double x2 = feat2_sat.eq(l2).norm1();
        double x1x2 = feat1_sat.eq(l1).dot(feat2_sat.eq(l2));
        double nx1x2 = feat1_sat.ne(l1).dot(feat2_sat.eq(l2));
        double x1nx2 = feat1_sat.eq(l1).dot(feat2_sat.ne(l2));
        double nx1nx2 = feat1_sat.ne(l1).dot(feat2_sat.ne(l2));

        double p_x1 = (double) x1/ n;
        double p_nx1 = (double) 1-p_x1;
        double p_x2 = (double) x2/n;
        double p_nx2 = (double) 1-p_x2;
        double p_x1x2 = (double) x1x2/n;
        double p_nx1x2 = (double) nx1x2/n;
        double p_x1nx2 = (double) x1nx2/n;
        double p_nx1nx2 = (double) nx1nx2/n;
        
        if(p_x1==0){p_x1 = 0.0001;}
        if(p_nx1==0){p_nx1=0.0001;}
        if(p_x2==0){p_x2=0.0001;}
        if(p_nx2==0){p_nx2=0.0001;}
        if(p_x1x2==0){p_x1x2=0.0001;}
        if(p_nx1x2==0){p_nx1x2=0.0001;}
        if(p_x1nx2==0){p_x1nx2=0.0001;}
        if(p_nx1nx2==0){p_nx1nx2=0.0001;}
        
        double i1 = p_x1x2*Math.log(p_x1x2/(p_x1*p_x2));
        double i2 = p_x1nx2*Math.log(p_x1nx2/(p_x1*p_nx2));
        double i3 = p_nx1x2*Math.log(p_nx1x2/(p_nx1*p_x2));
        double i4 = p_nx1nx2*Math.log(p_nx1nx2/(p_nx1*p_nx2));

        I = i1 + i2 + i3 + i4;
        return I;
    }
    
}