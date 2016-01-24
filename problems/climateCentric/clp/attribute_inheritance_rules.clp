;; *********************************
;; Mission - mission inheritance
;; *********************************

(defquery MANIFEST::search-instrument-by-name   (declare (variables ?name))
    (DATABASE::Instrument (Name ?name) (mass# ?m) (average-power# ?p) (peak-power# ?pp) 
        (average-data-rate# ?rb) (dimension-x# ?dx) (dimension-y# ?dy) (characteristic-power# ?ppp)  
        (dimension-z# ?dz) (cost# ?c) (cost ?fz-c))
    )
(defquery MANIFEST::search-instrument-by-name-manifest   (declare (variables ?name))
    (CAPABILITIES::Manifested-instrument (Name ?name) (mass# ?m) (average-power# ?p) (peak-power# ?pp) 
        (average-data-rate# ?rb) (dimension-x# ?dx) (dimension-y# ?dy) (characteristic-power# ?ppp)  
        (dimension-z# ?dz) (cost# ?c) (cost ?fz-c))
    )
(deffunction get-instrument-cost (?instr)
    (bind ?result (run-query* search-instrument-by-name ?instr))
    (?result next)
    (return (?result getDouble c))
    )
(deffunction get-instrument-cost-manifest (?instr)
    (bind ?result (run-query* MANIFEST::search-instrument-by-name-manifest ?instr))
    (?result next)
    (return (?result getDouble c))
    )
(deffunction get-instrument-fuzzy-cost (?instr)
    (bind ?result (run-query* search-instrument-by-name ?instr))
    (?result next)
    (return (?result get fz-c))
    )
(deffunction get-instrument-fuzzy-cost-manifest (?instr)
    (bind ?result (run-query* MANIFEST::search-instrument-by-name-manifest ?instr))
    (?result next)
    (return (?result get fz-c))
    )  

;; **********************************
;; Mission ==> Instrument inheritance (calculated attributes)
;; **********************************

(defrule MANIFEST::compute-hsr-from-instrument-and-orbit 
    "Compute horizontal spatial resolution hsr and hsr2 from instrument angular resolution 
    and orbit altitude"
    ?instr <- (CAPABILITIES::Manifested-instrument (orbit-altitude# ?h&~nil) (Angular-resolution# ?are&:(neq ?are nil)) (Horizontal-Spatial-Resolution# nil))
    =>
    (modify ?instr (Horizontal-Spatial-Resolution# (* 1000 ?h (* ?are (/ (pi) 180)) )))
    )

(defrule MANIFEST::fill-in-hsr-from-directional-hsrs
    "If along-track and cross-track spatial resolutions are known and identical, then 
    horizontal spatial resolution is equal to them"
    (declare (salience -2))
    ?instr <- (CAPABILITIES::Manifested-instrument (Horizontal-Spatial-Resolution-Cross-track# ?cr&~nil)
        (Horizontal-Spatial-Resolution-Along-track# ?al&~nil&?cr) (Horizontal-Spatial-Resolution# nil) )
    =>
    (modify ?instr (Horizontal-Spatial-Resolution# ?cr))
    )
	
(defrule MANIFEST::adjust-power-with-orbit
    "Adjust average and peak power from characteristic orbit an power based on square law"
	(declare (salience 15))
    ?instr <- (CAPABILITIES::Manifested-instrument (average-power# nil) (orbit-altitude# ?h&~nil) (characteristic-power# ?p&~nil) (characteristic-orbit ?href&~nil) )
    =>
	(bind ?zep (* ?p (** (/ ?h ?href) 2)))
    (modify ?instr (average-power# ?zep) (peak-power# ?zep))
    )

;; ********************************** 
;; **********************************
;; Cloud radars (e.g. Cloudsat, EarthCARE, ACE_RAD, TRMM PR)
;; **********************************
;; **********************************

(defrule MANIFEST::compute-cloud-radar-properties-vertical-spatial-resolution
    ?instr <- (CAPABILITIES::Manifested-instrument (Intent "Cloud profile and rain radars") 
        (bandwidth# ?B&~nil) (off-axis-angle-plus-minus# ?theta&~nil) (Vertical-Spatial-Resolution# nil) )
    =>
    (bind ?range-res (/ 3e8 (* 2 ?B (sin ?theta))))
    (modify ?instr (Vertical-Spatial-Resolution# ?range-res) )
    )

(defrule MANIFEST::compute-cloud-radar-properties-horizontal-spatial-resolution
    ?instr <- (CAPABILITIES::Manifested-instrument  (Intent "Cloud profile and rain radars")
         (frequency# ?f&~nil) (Aperture# ?D) (orbit-altitude# ?h&~nil) (Horizontal-Spatial-Resolution# nil) )
    =>
    (bind ?hsr (* 1000 (/ 3e8 (* ?D ?f)) ?h)); hsr = lambda/D*h, lambda=c/f
    (modify ?instr (Horizontal-Spatial-Resolution# ?hsr) )
    )

(defrule MANIFEST::compute-cloud-radar-properties-swath
    ?instr <- (CAPABILITIES::Manifested-instrument (Intent "Cloud profile and rain radars") 
        (off-axis-angle-plus-minus# ?theta&~nil) (scanning conical) (orbit-altitude# ?h&~nil) (Swath# nil) )
    =>
    (bind ?sw (* 2 ?h (tan ?theta ))); hsr = lambda/D*h, lambda=c/f
    (modify ?instr (Swath# ?sw) )
    )
;; ********************************** 
;; **********************************
;; Radar altimeters (e.g. Jason, SWOT)
;; **********************************
;; **********************************

(defrule MANIFEST::compute-altimeter-horizontal-spatial-resolution
    ?instr <- (CAPABILITIES::Manifested-instrument  (Intent "Radar altimeter")
         (frequency# ?f&~nil) (Aperture# ?D) (orbit-altitude# ?h&~nil) (Horizontal-Spatial-Resolution# nil) )
    =>
    (bind ?hsr (* 1000 (/ 3e8 (* ?D ?f)) ?h)); hsr = lambda/D*h, lambda=c/f
    (modify ?instr (Horizontal-Spatial-Resolution# ?hsr) )
    )

;; ****************
;; Rules for synthesis of alternative mission architectures trading accuracy for spatial resolution,
;; temporal resolution, or vertical spatial resolution. All these missions will be declared, and
;; rules can be added so that only one of each can be selected
(defrule MANIFEST::basic-diurnal-cycle
?meas<- (CAPABILITIES::Manifested-instrument (diurnal-cycle nil) (orbit-inclination ?inc&~nil) (orbit-RAAN ?raan&~nil) )
=>
(if (eq ?inc polar) then (bind ?dc variable) else (bind ?dc (eval (str-cat ?raan "-only"))))
 (modify ?meas (diurnal-cycle ?dc)) ) 
 
 
 
 
 