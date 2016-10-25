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
        DoubleMatrix labelMat = dataFeatureMat.getColumn(label_index);
        DoubleMatrix featuresIndexMat = DoubleMatrix.zeros(ncols,featureSet.size());
        for(int i=0;i<featureSet.size();i++){
        	featuresIndexMat.put(featureSet.get(i).getIndices(), i, 1.0);
        }
        DoubleMatrix features_sat = dataFeatureMat.mmul(featuresIndexMat);
        
        int numSelected = 0;
        while(numSelected < target_num_features){
        	
            int bestFeatInd = -1;
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
            	
            	int feature_length_1 = featureSet.get(i).getIndices().length;

                double D = getMutualInformation(features_sat, labelMat, i, feature_length_1);
                double R = 0;

                for (int featInd: selectedFeatures) {
                	int feature_length_2 = featureSet.get(featInd).getIndices().length;
                    R = R + getMutualInformation(features_sat, labelMat, i, featInd, feature_length_1, feature_length_2);
                }
                if(numSelected!=0){
                   R = (double) R / (double)numSelected;
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

    

    
    public double getMutualInformation(DoubleMatrix features_sat,DoubleMatrix label, int f1, int l1){
    	return getMutualInformation(features_sat,label,f1,l1,-1,-1);
    }
    
    public double getMutualInformation(DoubleMatrix features_sat,DoubleMatrix label, int f1, int l1, int f2, int l2){
        
        double I;
        double x1,x2,x1x2,nx1x2,x1nx2,nx1nx2;
        
        DoubleMatrix feat1_sat = features_sat.getColumn(f1);
        if(f2<0){
            x1 = feat1_sat.eq(l1).norm1();
            x2 = label.norm1();
            x1x2 = feat1_sat.eq(l1).dot(label);
            nx1x2 = feat1_sat.ne(l1).dot(label);
            x1nx2 = feat1_sat.eq(l1).dot(label.rsub(1));
            nx1nx2 = feat1_sat.ne(l1).dot(label.rsub(1));
        }else{
        	DoubleMatrix feat2_sat = features_sat.getColumn(f2);
            x1 = feat1_sat.eq(l1).norm1();
            x2 = feat2_sat.eq(l2).norm1();
            x1x2 = feat1_sat.eq(l1).dot(feat2_sat.eq(l2));
            nx1x2 = feat1_sat.ne(l1).dot(feat2_sat.eq(l2));
            x1nx2 = feat1_sat.eq(l1).dot(feat2_sat.ne(l2));
            nx1nx2 = feat1_sat.ne(l1).dot(feat2_sat.ne(l2));
        }

        double p_x1 = (double) x1/nrows;
        double p_nx1 = (double) 1-p_x1;
        double p_x2 = (double) x2/nrows;
        double p_nx2 = (double) 1-p_x2;
        double p_x1x2 = (double) x1x2/nrows;
        double p_nx1x2 = (double) nx1x2/nrows;
        double p_x1nx2 = (double) x1nx2/nrows;
        double p_nx1nx2 = (double) nx1nx2/nrows;
        
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