package mining;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Bang
 */
 
public class DrivingFeature implements Feature{
        
        private int index;
        private String name; // specific names
        private String type; // inOrbit, together, separate, present, absent, etc.
        private int[] param;
        private boolean preset;
        private double[] metrics;
        

        public DrivingFeature(String name, String type){
            this.name = name;
            this.type = type;
            this.preset = false;
        }
        public DrivingFeature(String name, String type, double[] metrics){
            this.name = name;
            this.type = type;
            this.metrics = metrics;
            this.preset = false;
        }
        public DrivingFeature(String name, String type, int[] param, double[] metrics){
            this.name = name;
            this.type = type;
            this.param = param;
            this.metrics = metrics;
            this.preset = true;
        }
        public DrivingFeature(int index,String name, String type){
            this.name = name;
            this.type = type;
            this.preset = false;
            this.index=index;
        }
        public DrivingFeature(int index,String name, String type, double[] metrics){
        	this.index=index;
        	this.name = name;
            this.type = type;
            this.metrics = metrics;
            this.preset = false;
        }
        public DrivingFeature(int index,String name, String type, int[] param, double[] metrics){
        	this.index=index;
        	this.name = name;
            this.type = type;
            this.param = param;
            this.metrics = metrics;
            this.preset = true;
        }
        
        @Override
        public double getSupport(){
            return metrics[0];
        }
        
        @Override
        public double getFConfidence(){
            return metrics[2];
        }
        
        @Override
        public double getRConfidence(){
            return metrics[3];
        }
        
        @Override
        public double getLift(){
            return metrics[1];
        }
        
        public int getIndex(){return index;}
        public void setIndex(int index){this.index=index;}
        public String getType(){return type;}
        public String getName(){return name;}
        public int[] getParam(){return param;}
        public double[] getMetrics(){return metrics;}
        public boolean isPreset(){return preset;}
        
    }
