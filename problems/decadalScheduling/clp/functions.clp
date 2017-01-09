;; NOT USED
(deffunction update-objective-variable (?obj ?new-value) 
    "Update the value of the global variable with the new value only if it is better"
     (bind ?obj (max ?obj ?new-value)))

(deffunction ContainsRegion (?observed-region ?desired-region) 
    "Returns true if the observed region i.e. 1st param contains the desired region 
    i.e. 2nd param" 
    (bind ?tmp1 (eq ?observed-region Global)) 
    (bind ?tmp2 (eq ?desired-region ?observed-region)) 
    (if (or ?tmp1 ?tmp2) then 
        (return TRUE) 
        else (return FALSE)))

;(load-function SameOrBetter)
;(load-function Improve)
