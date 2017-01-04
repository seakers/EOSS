/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author nozomihitomi
 */
public class Measurement {

    /**
     * The name of the measurement
     */
    private final String name;

    /**
     * horizontal spatial resolution 
     */
    private final double hsr;

    /**
     * vertical spatial resolution 
     */
    private final double vsr;

    /**
     * Accuracy
     */
    private final double accuracy;

    /**
     * temporal resolution
     */
    private final double tr;

    public Measurement(String name, double hsr, double vsr, double accuracy, double tr) {
        this.name = name;
        this.hsr = hsr;
        this.vsr = vsr;
        this.accuracy = accuracy;
        this.tr = tr;
    }

    public String getName() {
        return name;
    }

    public double getHsr() {
        return hsr;
    }

    public double getVsr() {
        return vsr;
    }

    public double getAccuracy() {
        return accuracy;
    }

    /**
     * Gets the temporal resolution of the 
     * @return 
     */
    public double getTr() {
        return tr;
    }
    

    /**
     * A builder pattern to set parameters for scenario
     */
    public static class Builder implements Serializable {

        private static final long serialVersionUID = -2447754795882563741L;

        //required fields
        /**
         * The name of the measurement
         */
        private final String name;

        //optional parameters - initialized to default parameters
        /**
         * horizontal spatial resolution 
         */
        private double hsr = Double.NaN;
        
        /**
         * vertical spatial resolution 
         */
        private double vsr = Double.NaN;

        /**
         * Accuracy
         */
        private double accuracy = Double.NaN;

        /**
         * temporal resolution 
         */
        private double tr = Double.NaN;

        /**
         * The constructor for the builder
         *
         * @param name the name of the mission
         */
        public Builder(String name) {
            this.name = name;
        }
        
        /**
         * Option to set the horizontal spatial resolution. Default is NaN
         *
         * @param hsr vertical spatial resolution
         * @return the modified builder
         */
        public Builder hsr(double hsr) {
            this.hsr = hsr;
            return this;
        }

        /**
         * Option to set the vertical spatial resolution. Default is NaN
         *
         * @param vsr vertical spatial resolution
         * @return the modified builder
         */
        public Builder vsr(double vsr) {
            this.vsr = vsr;
            return this;
        }
        
        /**
         * Option to set the temporal resolution. Default is NaN
         *
         * @param tr temporal resolution
         * @return the modified builder
         */
        public Builder tr(double tr) {
            this.tr = tr;
            return this;
        }
        
        /**
         * Option to set the accuracy. Default is NaN
         *
         * @param accuracy the accuracy
         * @return the modified builder
         */
        public Builder accuracy(double accuracy) {
            this.accuracy = accuracy;
            return this;
        }
        
        /**
         * Builds an instance of a measurement with all the specified parameters.
         *
         * @return
         */
        public Measurement build() {
            return new Measurement(name, hsr, vsr, accuracy, tr);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.hsr) ^ (Double.doubleToLongBits(this.hsr) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.vsr) ^ (Double.doubleToLongBits(this.vsr) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.accuracy) ^ (Double.doubleToLongBits(this.accuracy) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.tr) ^ (Double.doubleToLongBits(this.tr) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Measurement other = (Measurement) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (Double.doubleToLongBits(this.hsr) != Double.doubleToLongBits(other.hsr)) {
            return false;
        }
        if (Double.doubleToLongBits(this.vsr) != Double.doubleToLongBits(other.vsr)) {
            return false;
        }
        if (Double.doubleToLongBits(this.accuracy) != Double.doubleToLongBits(other.accuracy)) {
            return false;
        }
        if (Double.doubleToLongBits(this.tr) != Double.doubleToLongBits(other.tr)) {
            return false;
        }
        return true;
    }
    
    
}
