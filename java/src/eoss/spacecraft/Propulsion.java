/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

/**
 *
 * @author nozomihitomi
 */
public class Propulsion extends AbstractBusComponent {

    private static final long serialVersionUID = 4433630893462643713L;

    private final Propellant injectionPropellant;
    
    /**
     * Propellant mass for orbit injection
     */
    private final double injMass;

    private final Propellant adcsPropellant;

    /**
     * Propellant mass for ADCS
     */
    private final double adcsMass;
    
    private final DeorbitStrategy deorbit;
    
    

    /**
     *
     * @param mass motor mass
     * @param power
     * @param injProp Propellant for orbit injection
     * @param injMass Propellant mass for orbit injection
     * @param adcsProp Propellant for ADCS
     * @param adcsMass Propellant mass for ADCS
     * @param deorbit Deorbit strategy
     */
    public Propulsion(double mass, double power, Propellant injProp, double injMass, Propellant 
        adcsProp, double adcsMass, DeorbitStrategy deorbit) {
        super(mass, power);
        this.injectionPropellant = injProp;
        this.injMass = injMass;
        this.adcsPropellant = adcsProp;
        this.adcsMass = adcsMass;
        this.deorbit = deorbit;
    }

    /**
     * {@inheritDoc}. This implementation returns the mass of the motor only
     * (i.e. dry mass)
     *
     * @return
     */
    @Override
    public double getMass() {
        return super.getMass();
    }

    public Propellant getInjectionPropellant() {
        return injectionPropellant;
    }

    public Propellant getADCSPropellant() {
        return adcsPropellant;
    }

    public DeorbitStrategy getDeorbit() {
        return deorbit;
    }

    /**
     * Gets the Propellant mass for injection
     * @return 
     */
    public double getMassInjection() {
        return injMass;
    }

    /**
     * Gets the Propellant mass for ADCS
     * @return 
     */
    public double getMassADCS() {
        return adcsMass;
    }
    
    

}
