package mining;

import java.util.Comparator;
import org.jblas.DoubleMatrix;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Bang
 */
 


public class DrivingFeature implements Comparable<DrivingFeature>, java.io.Serializable{
        
	
        private int id;
        private String name; 
        private String expression; 
        private double[] metrics;
        private DoubleMatrix datArray;


        public DrivingFeature(int id, String name, String expression){
            this.id=id;
            this.name = name;
            this.expression=expression;
        }
        public DrivingFeature(int id, String name, String expression, double[] metrics){
            this.id=id;
            this.name = name;
            this.expression = expression;
            this.metrics = metrics;
        }

        
                
        public int getID(){return id;}
        public String getExpression(){return expression;}
        public String getName(){return name;}
        public double[] getMetrics(){return metrics;}
        public void setDatArray(DoubleMatrix m){datArray=m;}
        public DoubleMatrix getDatArray(){return datArray;}
        
        
       @Override
       public int compareTo(DrivingFeature other) {
           if(this.getName().compareTo(other.getName()) == 0)
               return 0;
           else return 1;
       }        
       
       public static Comparator<DrivingFeature> DrivingFeatureComparator = new Comparator<DrivingFeature>() {
            @Override
            public int compare(DrivingFeature d1, DrivingFeature d2) {
                double x = (d1.getMetrics()[1] - d2.getMetrics()[1]);
                if(x<0) {
                    return 1;
                } else if (x>0) {
                    return - 1;
                } else {
                    return 0;
                }

            }
       };
        
        
        
    }




