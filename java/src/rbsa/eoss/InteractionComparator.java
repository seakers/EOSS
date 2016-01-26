/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

import java.util.Comparator;

/**
 *
 * @author nozomihitomi
 */
public class InteractionComparator implements Comparator<Interaction>{

    @Override
    public int compare(Interaction o1, Interaction o2) {
        double diff = o1.getValue()-o2.getValue();
        if(diff<0){
            return -1;
        }else if(diff>0){
            return 1;
        }else 
            return 0;
    }
    
}
