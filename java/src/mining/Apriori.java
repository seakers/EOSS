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
	
    private double[] thresholds;
    ArrayList<DrivingFeature> presetDrivingFeatures;
    DoubleMatrix dataFeatureMat;
    int nrows;
    int ncols;
    
    public Apriori(ArrayList<DrivingFeature> presetDrivingFeatures, double[][] dataFeatureMat, double[] thresholds){
    	
    	this.presetDrivingFeatures = presetDrivingFeatures;
    	this.nrows = dataFeatureMat.length;
    	this.ncols = dataFeatureMat[0].length;
    	this.dataFeatureMat = new DoubleMatrix(dataFeatureMat);
    	
    	// thresholds = {supp_threshold, lift_threshold, conf_threshold}
    	this.thresholds = thresholds;
    	
    }
    
    
	
	public ArrayList<SetOfFeatures> runApriori(int maxLength, boolean run_mRMR, int num_features_to_extract){
		
		// Define the initial set of features
		ArrayList<SetOfFeatures> S = generateInitialSets(this.presetDrivingFeatures);
		
		// Define frontier. Frontier is the set of features whose length is L and passes significant test
		ArrayList<int[]> frontier = new ArrayList<>(); 
		
		// Copy the initial set of features to the frontier
		for (SetOfFeatures s:S){
			frontier.add(s.getIndices());
		}
		
		// Save class labels as a matrix
		DoubleMatrix label = dataFeatureMat.getColumn(ncols-1);

		int l = 2;
		// While there are features still left to explore
		while(frontier.size() > 0){
			
			if(l-1==maxLength){
				break;
			}
			
			// Candidates to form the frontier with length L+1
			ArrayList<int[]> candidates = new ArrayList<>();
//			System.out.println("...Extracting features of level " + l + " - size: " + frontier.size());
			
			for(int b=0;b<frontier.size();b++){
				int[] branch = frontier.get(b);
				// For all features that pass significance test and are of length L

				for(int i=0;i<ncols-1;i++){
					if(i<=branch[branch.length-1]){
						// Skip if the current feature has an index lower than the max index of the branch
						continue;
					}else{
						// Count frequency
						double[] metrics = computeMetrics(dataFeatureMat,branch,i,label);

						// Check if it passes minimum support threshold
						if(metrics[0] <= thresholds[0]){
							continue;
						}else{
							
							int[] newIntArr = new int[branch.length+1];
							for(int j=0;j<branch.length;j++){
								newIntArr[j]=branch[j];
							}
							newIntArr[l-1]=i;
							// Add all features whose support is above threshold, add to candidates
							candidates.add(newIntArr);
							
							if(metrics[2] > thresholds[2]){
								// Create a new candidate
								SetOfFeatures newFeat = new SetOfFeatures(newIntArr);
								newFeat.setMetrics(metrics);
								// If the metric is above the threshold, current feature is statistically significant
								S.add(newFeat);
							}
						}
					}
				}
			}
			frontier=candidates;
			l=l+1;
		}
		
		if(run_mRMR){
			MRMR mRMR = new MRMR();
			S = mRMR.minRedundancyMaxRelevance(dataFeatureMat, S, num_features_to_extract);
		}
		return S;
	}
	
	

	
	public boolean contains(int[] arr, int i){
		for(int a:arr){
			if(a==i){
				return true; 
			}
		}
		return false;
	}
	

	public double[] computeMetrics(DoubleMatrix dataMat,int[] indices, int newInd,DoubleMatrix label){
		
    	int numFeat = indices.length+1;
    	DoubleMatrix cond = DoubleMatrix.zeros(ncols,1);
    	cond.put(indices,0,  1.0);
    	cond.put(newInd, 0,  1.0);

		double cnt_all = nrows;
        double cnt_F = 0;
        double cnt_S = 0;
        double cnt_SF = 0;
        
        DoubleMatrix countMat = dataFeatureMat.mmul(cond);
        countMat = countMat.eq(numFeat);
        cnt_SF = label.dot(countMat);
        cnt_S = label.norm1();
        cnt_F = countMat.norm1();
		double[] metrics = new double[4];
		double support = cnt_SF/cnt_all;
		double lift = (cnt_SF/cnt_S) / (cnt_F/cnt_all);
		double conf_given_F = (cnt_SF)/(cnt_F);   // confidence (feature -> selection)
		double conf_given_S = (cnt_SF)/(cnt_S);   // confidence (selection -> feature)
    
		metrics[0] = support;
		metrics[1] = lift;
		metrics[2] = conf_given_F;
		metrics[3] = conf_given_S;
		return metrics;
	}

	
	public ArrayList<SetOfFeatures> generateInitialSets(ArrayList<DrivingFeature> drivingFeatures){
		
		ArrayList<SetOfFeatures> S = new ArrayList<>();
		for(DrivingFeature feat:drivingFeatures){
			int[] temp = new int[1];
			temp[0] = feat.getIndex();
			SetOfFeatures set = new SetOfFeatures(temp);
			set.setMetrics(feat.getMetrics());
			S.add(set);
		}
		return S;
	}
	


    
    public ArrayList<boolean[]> intMatrix2BoolArray(ArrayList<int[][]> input){
    	
    	ArrayList<boolean[]> boolArray = new ArrayList<>();
    	int len = input.get(0).length * input.get(0)[0].length;
    	
    	for(int i=0;i<input.size();i++){
    		
    		boolean[] tmpArray = new boolean[len];
    		int cnt=0;
    		for(int j=0;j<input.get(i).length;j++){
    			for(int k=0;k<input.get(i)[j].length;k++){
    				if(input.get(i)[j][k]==1){
    					tmpArray[cnt]=true;
    				}else{
    					tmpArray[cnt]=false;
    				}
    				cnt++;
    			}
    		}
    		
    		
    		boolArray.add(tmpArray);
    	}

    	return boolArray;
    }
   
    
}