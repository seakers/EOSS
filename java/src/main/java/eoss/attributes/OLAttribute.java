package eoss.attributes;

import java.util.HashMap;

/**
 * This attribute is an ordered list
 */
public class OLAttribute extends EOAttribute {

    public OLAttribute(String charact, String val, HashMap<String, Integer> accepted) {
        super(charact, AttributeType.OL, val, accepted);
    }

    /**
     * In this implementation of an ordered list it is assumed that the
     * attributes with a lower index are better. If this value is better than
     * the other then returns 1. If they are the same returns 0. Else -1.
     *
     * @param other
     * @return
     */
    @Override
    public int sameOrBetter(EOAttribute other) {
        int z;
        int value_this = this.getAcceptedValues().get(this.getValue());
        int value_other = other.getAcceptedValues().get(other.getValue());
        if (value_this == value_other) {
            z = 0;
        } else if (value_this > value_other) {
            z = -1;
        } else if (value_this < value_other) {
            z = 1;
        } else {
            z = -1;
        }
        return z;
    }

    @Override
    public String worsen() {
        int old_num_value = this.getAcceptedValues().get(this.getValue());
        int size = this.getAcceptedValues().size();
        HashMap<Integer, String> reverse = this.getReverseAcceptedValues();
        int new_num_value = 0;
        if (old_num_value < size) {
            new_num_value = old_num_value + 1;
        } else {
            new_num_value = old_num_value;
        }
        String new_value = reverse.get(new_num_value);
        return new_value;
    }

    /**
     * Improves the attribute
     *
     * @return the improved attribute value
     */
    @Override
    public String improve() {
        int old_num_value = this.getAcceptedValues().get(this.getValue());
        HashMap<Integer, String> reverse = this.getReverseAcceptedValues();
        int new_num_value = 0;
        if (old_num_value > 1) {
            new_num_value = old_num_value - 1;
        } else {
            new_num_value = old_num_value;
        }

        String new_value = reverse.get(new_num_value);
        return new_value;
    }

    @Override
    public EOAttribute cloneAttribute(EOAttribute attribute) {
        return new OLAttribute(getCharacteristic(), getValue(), getAcceptedValues());
    }

}
