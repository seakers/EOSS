(defquery DATABASE::getLaunchVehicleInformation  (declare (variables ?id))
    (DATABASE::Launch-vehicle (cost ?cost) (diameter ?diameter) (height ?height) (id ?id) (GEO-equat $?GEO-equat) (HEO-polar $?HEO-polar) (LEO-equat $?LEO-equat) (LEO-polar $?LEO-polar) (MEO-polar $?MEO-polar) (SSO-SSO $?SSO-SSO) (LEO-ISS $?LEO-ISS))
    )

(defrule LV-SELECTION0::assert-all-possible-lvs
	?orig <- (MANIFEST::Mission (launch-vehicle nil) )
	?lv <- (DATABASE::Launch-vehicle (id ?name) )
	=>
	(duplicate ?orig (launch-vehicle ?name) )
)

(defrule LV-SELECTION1::remove-nils
	?orig <- (MANIFEST::Mission (launch-vehicle nil))
	
	=>
	(retract ?orig)
)

(defrule LV-SELECTION2::compute-number-of-launches
    "Compute number of launches needed for a certain constellation on a certain launcher"
    
      ?f <-  (MANIFEST::Mission (id ?sat) (launch-vehicle ?lv&~nil) (num-launches nil)
        (num-of-sats-per-plane# ?ns&~nil) (satellite-wet-mass ?m&~nil) (satellite-dimensions $?dim) 
        (orbit-type ?orb&~nil) (orbit-semimajor-axis ?a&~nil) (orbit-eccentricity ?e&~nil)
         (orbit-inclination ?i&~nil) )
    =>
    (bind ?perf (get-performance ?lv ?orb (to-km (r-to-h ?a)) ?i))
    ;(printout t "the perf of lv " ?lv " for orbit "  ?orb " a = " ?a " i = " ?i " is " ?perf crlf)
    (bind ?NL-mass (ceil (/ (* ?m ?ns) ?perf)))
	(bind ?result (run-query* DATABASE::getLaunchVehicleInformation ?lv))
	(?result next) ;advance cursor to next result in list of results obtained from query
	    (bind ?fairing-dimensions (create$ (?result get diameter) (?result get height) ))
	
    (bind ?NL-vol (ceil (/ (* (*$ $?dim) ?ns) (*$ ?fairing-dimensions))))
   
    (bind ?NL-diam (ceil (/ (* (max$ ?dim) ?ns) (max$ ?fairing-dimensions))))
    (modify ?f (num-launches (max ?NL-mass ?NL-vol ?NL-diam)) )
    )

(defrule LV-SELECTION2::compute-launch-cost
    "This rule computes launch cost as the product of number of launches and 
    the cost of a single launch."
    
    ?f <- (MANIFEST::Mission (launch-vehicle ?lv&~nil) (num-launches ?num&~nil)
        (launch-cost# nil) (num-of-sats-per-plane# ?ns&~nil) )
    =>
	(if (> ?num  ?ns) then (bind ?ccost 1e10)) ; If there are more launches than satellites then infeasible!
	(bind ?result (run-query* DATABASE::getLaunchVehicleInformation ?lv))
		(?result next) ;advance cursor to next result in list of results obtained from query
	    (bind ?ccost (?result get cost))
    (modify ?f (launch-cost# (* ?num ?ccost)) (launch-cost (fuzzyscprod (cost-fv ?ccost 10) ?num)) )
	;(modify ?f (launch-cost# (* ?num ?ccost)) )
    )

	; cost rule
(defrule LV-SELECTION3::eliminate-more-expensive-launch-options
    "From all feasible options, eliminate the most expensive ones"
    
    ?m1 <- (MANIFEST::Mission (Name ?name) (launch-vehicle ?lv1&~nil) (launch-cost# ?c1&~nil))
    (MANIFEST::Mission (Name ?name) (launch-vehicle ?lv2&~?lv1&~nil) (launch-cost# ?c2&~nil&:(<= ?c2 ?c1)))

        =>
    
	 (retract ?m1) 
    )
	

(deffunction large-enough-height (?lv ?dim); ?dim = (max-diam area height)
	(bind ?result (run-query* DATABASE::getLaunchVehicleInformation ?lv))
	(?result next) ;advance cursor to next result in list of results obtained from query
    (bind ?fairing-dimensions (create$ (?result get diameter) (?result get height) ))
    (bind ?diam (nth$ 1 ?dim))
    (if (eq ?diam nil) then (return 0) else
        ;(printout t "large-enough-height " ?lv " with dimensions " ?fairing-dimensions " sat-diameter " ?diam " " crlf)
        ;(printout t " max fairing dims " (max$ ?fairing-dimensions) " (* 0.8 ?diam) = " (* 0.8 ?diam) crlf)
        (if (> (max$ ?fairing-dimensions) (* 0.8 ?diam)) then 
        (return 1)
        else (return 0)
        )
      )
    ;(bind ?area (nth$ 2 ?dim))
    
    )

(deffunction large-enough-area (?lv ?dim); ?dim = (max-diam area height)
	(bind ?result (run-query* DATABASE::getLaunchVehicleInformation ?lv))
	(?result next) ;advance cursor to next result in list of results obtained from query
    (bind ?fairing-dimensions (create$ (?result get diameter) (?result get height) ))
    ;(bind ?diam (nth$ 1 ?dim))
    (bind ?area (nth$ 2 ?dim))
    (if (eq ?area nil) then (return 0))
    (if (> (* (nth$ 1 ?fairing-dimensions) (nth$ 2 ?fairing-dimensions)) (* 0.65 ?area)) then 
        (return 1)
        else (return 0)
        )
    )
	
(deffunction get-performance (?lv ?typ ?h ?i)
	(bind ?result (run-query* DATABASE::getLaunchVehicleInformation ?lv))
	(?result next) ;advance cursor to next result in list of results obtained from query
    (bind ?coeffs (?result get (str-cat ?typ "-" ?i)))	
	(if (isempty$ ?coeffs) then 
		(throw new JessException (str-cat "get-performance: coeffs not found for lv typ h i = " ?lv " " ?typ " " ?h " " ?i)))
	(bind ?perf (dot-product$ ?coeffs (create$ 1 ?h (** ?h 2))))
	(return ?perf)
)

(deffunction sufficient-performance (?lv ?m ?typ ?h ?i)
    ;(printout t "sufficient performance " ?lv " m=" ?m " orb=" ?typ " h=" ?a " i=" ?i)
    (bind ?margin 1.1); 10% margin
    (bind ?perf (get-performance ?lv ?typ ?h ?i))
    (return (> ?perf (* ?margin ?m)))
    )
