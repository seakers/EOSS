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
public class StructureFactory implements BusComponentFactory {
    
    private static final double structMassCoeff = 0.5462;

    /**
     * {@inheritDoc}. Computes structure subsystem mass using rules of thumb.
     * @param s
     * @param o
     * @param lifetime
     * @return 
     */
    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        return new Structure(s.getPayloadMass() * structMassCoeff, 0.);
    
    }
    
}
