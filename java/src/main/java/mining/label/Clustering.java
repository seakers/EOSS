/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining.label;

/**
 *
 * @author Bang
 */

import net.sf.javaml.core.*;

import java.util.ArrayList;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;


public class Clustering {

	Dataset data;
	int numAttrs;
	
	public Clustering(int numAttrs){
		
		this.numAttrs=numAttrs; // The number of attribute values used to cluster the data
		data = new DefaultDataset();
	}
	
	
	public void addInstance(int id, double[] datavector){
		// The first attribute is a unique id of the data, and the last attribute is the class label
		Instance instance = new SparseInstance(numAttrs + 1);
		instance.put(0, (double) id);
		for(int i=0;i<datavector.length;i++){
			instance.put(i+1, datavector[i]);
		}
		// Add the instance to the dataset
		data.add(instance);
	}
	

	
    public Dataset[] KMeansClustering(int numCluster, int iterations){
    	
    	int iter;
    	if(iterations < 0){
    		iter = 100;
    	}else{
    		iter = iterations;
    	}
        /*
         * Create a new instance of the KMeans algorithm.
         */
        Clusterer kmeans = new KMeans(numCluster, iter);
        /*
         * Cluster the data, it will be returned as an array of data sets, with
         * each dataset representing a cluster
         */
        Dataset[] clusters = kmeans.cluster(this.data);
        System.out.println("Cluster count: " + clusters.length);

        return clusters;
    }
    
    
    // Density-based spatial-scanning clustering algorithm
    public Dataset[] DBSCAN(){
    	
    	Clusterer dbscan = new DensityBasedSpatialClustering();
    	
    	Dataset[] clusters = dbscan.cluster(this.data);
        System.out.println("Cluster count: " + clusters.length);
        
    	return clusters;
    }
    
    
    public ArrayList<int[]> parseClusterData(Dataset[] ds){
    	
    	ArrayList<int[]> clusters = new ArrayList<>();
    	
    	for(Dataset cl:ds){
    		int leng = cl.size();
    		int[] indices = new int[leng];
    		for(int i=0;i<leng;i++){
    			// The first element is set to be the id of each object
    			indices[i] = cl.get(i).get(0).intValue();
    		}
    		clusters.add(indices);
    	}
    	
    	return clusters;
    }
    
	
	
}
