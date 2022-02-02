/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Orbit;

/**
 *
 * @author nozomihitomi
 */
public class ThermalFactory implements BusComponentFactory{

    private static final double thermalMassCoeff = 0.0607;
    
    /**
     * {@inheritDoc}. Computes thermal subsystem mass using rules of thumb
     * @param s
     * @param o
     * @param lifetime
     * @return 
     */
    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        return new Thermal(s.getPayloadMass() * thermalMassCoeff, 0.0);
    }
    
}
