/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

/**
 *
 * @author Ana-Dani
 */
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public class NDSMFuture implements Serializable {

    private static final long serialVersionUID = -7003149292546882129L;

    private HashSet<Interaction> dsm;

    public NDSMFuture(String[] el, String desc) {
        dsm = new HashSet<>();
    }

    /**
     * 
     * @param interaction interaction to add
     * @return true if new interaction was added. else false and the given interaction is already in the dsm
     */
    public boolean addInteraction(Interaction interaction) {
        return dsm.add(interaction);
    }
    
    /**
     * Checks to see if the given interaction is part of the dsm already
     * @param interaction
     * @return true if the given interaction is part of the dsm already. else false
     */
    public boolean containsInteraction(Interaction interaction){
        return dsm.contains(interaction);
    }
    

    public void printAllInteractions() {
        Iterator<Interaction> iter = dsm.iterator();
        while(iter.hasNext()){
            Interaction interaction = iter.next();
            System.out.println(interaction);
        }
    }

    /**
     *
     * @param operator "+" for filter in keeping positive values. "0" filter for
     * keeping 0 values. "-" for keeping negative values
     * @return
     */
    public TreeSet<Interaction> getAllInteractions(String operator) {
        HashSet<Interaction> unsorted = new HashSet<>();
        TreeSet<Interaction> out = new TreeSet<>(new ValueComparator());

        Iterator<Interaction> iter = dsm.iterator();
        while(iter.hasNext()){
            Interaction interaction = iter.next();
            double val = interaction.getValue();
            if ((val == 0.0 && operator.equalsIgnoreCase("0")) || (val > 0.0 && operator.equalsIgnoreCase("+")) || (val < 0.0 && operator.equalsIgnoreCase("-"))) {
                unsorted.add(interaction);
            }
        }
        out.addAll(unsorted);
        return out;
    }

    public HashSet<Interaction> getMap() {
        return dsm;
    }

    public void setMap(HashSet<Interaction> dsm) {
        this.dsm = dsm;
    }
    
    //used to sort Interactions from interaction dsms
    private class ValueComparator implements Comparator<Interaction>{
        @Override
        public int compare(Interaction o1, Interaction o2) {
            return (int)(o1.getValue()-o2.getValue());
        }
    }
}
