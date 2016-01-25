/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author Ana-Dani
 */
public class ValueComparator2 implements Comparator<Nto1pair> {

    private HashMap<Nto1pair, Double> base;

    private final String ascending;

    /**
     *
     * @param base
     * @param ascending "+" means ascending. "-" means sort in descending order.
     */
    public ValueComparator2(HashMap<Nto1pair, Double> base, String ascending) {
        this.base = base;
        this.ascending = ascending;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    @Override
    public int compare(Nto1pair a, Nto1pair b) {
        //ascending order
        switch (ascending) {
            case "+":
                if (base.get(a) >= base.get(b)) {
                    return -1;
                } else {
                    return 1;
                } // returning 0 would merge keys
            case "-":
                if (base.get(a) >= base.get(b)) {
                    return 1;
                } else {
                    return -1;
                } // returning 0 would merge keys
            default:
                throw new UnsupportedOperationException("Currently can only sort DSM in ascending and descending order based on the value a feature produces");
        }
    }
}
