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
public class Panel implements Serializable{
    private static final long serialVersionUID = 8688212811750420263L;
    
    /**
     * The name of the panel
     */
    private final String name;
    
    /**
     *  the discount factor of this panel 
     */
    private final double discountFactor;

    public Panel(String name, double discountFactor) {
        this.name = name;
        this.discountFactor = discountFactor;
    }

    /**
     * Gets the name of this panel
     * @return the name of this panel
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the discount factor of this panel
     * @return the discount factor of this panel
     */
    public double getDiscountFactor() {
        return discountFactor;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.discountFactor) ^ (Double.doubleToLongBits(this.discountFactor) >>> 32));
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
        final Panel other = (Panel) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (Double.doubleToLongBits(this.discountFactor) != Double.doubleToLongBits(other.discountFactor)) {
            return false;
        }
        return true;
    }
    
    /**
     * A builder pattern to set parameters for scenario
     */
    public static class Builder implements Serializable {

        private static final long serialVersionUID = 5998898552871230531L;
        
        /**
         * The name of the panel
         */
        private final String name;
        
        private double discountFactor = 0;
        
        public Builder(String name){
            this.name = name;
        }
        
        /**
         * Sets the discount factor of this panel 
         * @param factor the discount factor of this panel
         * @return the modified builder
         */
        public Builder discountRate(double factor){
            this.discountFactor = factor;
            return this;
        }
        
        /**
         * Builds an instance of a panel with all the specified parameters.
         *
         * @return an instance of a panel with all the specified parameters.
         */
        public Panel build() {
            return new Panel(name, discountFactor);
        }
    }
}
