package eoss.attributes;

// package KBSAofEOSS;
import java.util.HashMap;

public class NLAttribute extends EOAttribute {

    public NLAttribute(String charact, String val, HashMap<String, Integer> accepted) {
        super(charact, AttributeType.NL, val, accepted);
    }

    /**
     * This implementation of a neutral list only checks to see if the attribute values are equal. If so it returns 0. Else it returns -1.
     * @param other
     * @return 
     */
    @Override
    public int sameOrBetter(EOAttribute other) {
        // Since this is a Neutral List attribute:
        if (this.getValue().equalsIgnoreCase(other.getValue())) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public String improve() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String worsen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EOAttribute cloneAttribute(EOAttribute attribute) {
        return new NLAttribute(getCharacteristic(), getValue(), getAcceptedValues());
    }
}
