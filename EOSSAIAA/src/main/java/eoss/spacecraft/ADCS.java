/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

/**
 * The attitude dynamics and control system 
 * @author nozomihitomi
 */
public class ADCS extends AbstractBusComponent{
    private static final long serialVersionUID = 5232566818970702703L;

    /**
     * The type of adcs system
     */
    private final ADCSType type;
    
    public ADCS(ADCSType type, double mass, double power) {
        super(mass, power);
        this.type = type;
    }

    public ADCSType getType() {
        return type;
    }
    
    
}
