/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

/**
 * communications subsystem, avionics or obdh
 * @author nozomihitomi
 */
public class Communications extends AbstractBusComponent{
    private static final long serialVersionUID = 7161071553007485056L;

    public Communications(double mass, double power) {
        super(mass, power);
    }

}
