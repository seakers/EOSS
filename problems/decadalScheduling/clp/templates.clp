
(deftemplate SYNERGIES::cross-registered "Declare a set of measurements as cross-registered"
    (multislot measurements) (slot degree-of-cross-registration) (slot platform))
  
(deftemplate SYNERGIES::cross-registered-instruments "Declare a set of measurements as cross-registered"
    (multislot instruments) (slot degree-of-cross-registration) (slot platform))

(deftemplate REASONING::partially-satisfied "Requirements that are partially satisfied" (slot subobjective)
    (slot objective) (slot parameter) (slot taken-by) (slot attribute) (slot required) (slot achieved))

(deftemplate REASONING::fully-satisfied "Requirements that are partially satisfied" (slot subobjective)
    (slot objective) (slot parameter) (slot taken-by))


