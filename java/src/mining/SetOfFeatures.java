package mining;

import java.util.ArrayList;


public class SetOfFeatures{
	
	private String name;
    private double[] metrics=null;
    private int[] indices;
	
    public SetOfFeatures(int[] indices){
    	this.indices = indices;
    }
    

    public int[] getIndices(){
    	return this.indices;
    }
    public double[] getMetrics(){
    	return this.metrics;
    }
    
    public void setMetrics(double[] metrics){
    	this.metrics = metrics;
    }

}