/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 * @author nozomihitomi
 */
public abstract class CompoundFeature extends AbstractFeature {
    
    private final HashSet<Integer> elem;
    
    public CompoundFeature(Collection<Integer> elem, double support, double lift, double fconfidence, double rconfidence) {
        super(support, lift, fconfidence, rconfidence);
        this.elem = new HashSet<>(elem);
    }

    public Collection<Integer> getElements() {
        return elem;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.elem);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompoundFeature other = (CompoundFeature) obj;
        if (!Objects.equals(this.elem, other.elem)) {
            return false;
        }
        return true;
    }
    
    
    
}
