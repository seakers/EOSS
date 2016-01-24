package eoss.attributes;

import java.util.HashMap;
import java.util.Iterator;

public abstract class EOAttribute {
// types of attributes: 
    // GB good boolean (yes/no, yes includes no), BB bad boolean (no includes yes, NB neutral Boolean (yes and no are simply different)
    // LIB2 (High, Low High incldues low) LIB3 (High,Medium,Low, High includes all), LIB5 (with highest and lowest) 
    // SIB2 (Low, High, Low incldues high) SIB3 (Low,Medium,High, Low includes all), SIB5 (with lowest and highest)
    // NL Neutral List (they are just all different, e.g., bands)
    // OL Ordered List (unspecified list of values, but they have a defined preference order)

    private final String characteristic;
    private String value;
    private final AttributeType type;
    private final HashMap<String, Integer> acceptedValues;

    public EOAttribute(String charact,AttributeType type, String val, HashMap<String, Integer> acceptedValues) {
        this.characteristic = charact;
        this.type = type;
        this.value = val;
        this.acceptedValues = acceptedValues;
    }

    public String getCharacteristic() {
        return this.characteristic;
    }

    public abstract int sameOrBetter(EOAttribute other);

    public HashMap<Integer, String> getReverseAcceptedValues() {
        HashMap<Integer, String> reverseAcceptedValues = new HashMap<>();
        Iterator key_set = this.acceptedValues.keySet().iterator();
        int size = this.acceptedValues.size();
        for (int i = 1; i <= size; i++) {
            String key = (String) key_set.next();
            reverseAcceptedValues.put(this.acceptedValues.get(key), key);
        }
        return reverseAcceptedValues;
    }

    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this attribute to a specified value. If the incoming value is a valid option in the accepted values the value is updated and returns true. Else false.
     * @param value
     * @return 
     */
    public boolean setValue(String value) {
        if(acceptedValues.containsKey(value)){
            this.value = value;
            return true;
        }else
            return false;
    }

    public AttributeType getType() {
        return type;
    }

    public HashMap<String, Integer> getAcceptedValues() {
        return acceptedValues;
    }
    

    public abstract String improve();

    public abstract String worsen();
    
    public enum AttributeType{
        NL,
        OL,
    }

    public abstract EOAttribute cloneAttribute(EOAttribute attribute);
}
