/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

/**
 * Enums for common propellants
 * @author nozomihitomi
 */
public enum Propellant {
    SOLID(210.),
    HYDRAZINE(290.),
    LH2(450.);
    
    /**
     * The specific impulse of the propellant
     */
    private final double isp;
    
    
    Propellant(double isp){
        this.isp = isp;
    }

    public double getIsp() {
        return isp;
    }
    
    
}
