;(batch templates.clp)   
;(batch functions.clp)

;; ***************************
;; CAPABILITY RULES
;; ***************************



(defrule CAPABILITIES::passive-optical-instruments-cannot-measure-in-dark
    "Passive optical instruments cannot take their measurements in DD RAANs"
    (declare (salience 10))
    ?c <- (CAPABILITIES::can-measure (instrument ?ins) (orbit-RAAN DD) (can-take-measurements yes))
    (DATABASE::Instrument (Name ?ins) (Illumination Passive) (Spectral-region ?sr))
    (test (eq (sub-string 1 (min (str-length ?sr) 3) ?sr) "opt"))
    =>
    ;(printout t cannot-measure-in-dark " " ?ins " " ?sr crlf)
    (modify ?c (can-take-measurements no) (reason "Passive optical instruments cannot take their measurements in DD RAANs"))
    )

(defrule CAPABILITIES::chemistry-instruments-prefer-PM-orbits-effect-on-science
    "Decrease sensitivity of chemistry instruments flying in AM orbits"
    (declare (salience 10))
    ?i <- (CAPABILITIES::Manifested-instrument (Name ?ins) (Concept ?c) (orbit-RAAN AM) (sensitivity-in-low-troposphere-PBL High))
    (or 
        (test (neq (str-index "chemistry" ?c) FALSE))
        (test (neq (str-index "pollut" ?c) FALSE))
        )
    =>
    ;(printout t "Decrease sensitivity of chemistry instruments flying in AM orbits " ?ins " " crlf)
    (modify ?i (sensitivity-in-low-troposphere-PBL Low)) 
    )

(defrule CAPABILITIES::hires-passive-optical-imagers-prefer-AM-orbits-effect-on-science
    "Passive optical all purpose hi res imagers such as Landsat or ASTER must fly in AM orbits to decrease cloudiness"
    (declare (salience 10))
    ?c <- (CAPABILITIES::can-measure (instrument ?ins) (orbit-RAAN PM) (can-take-measurements yes))
    (DATABASE::Instrument (Name ?ins) (Illumination Passive) (Spectral-region ?sr) (Intent "High resolution optical imagers"))
    (test (eq (sub-string 1 (min (str-length ?sr) 3) ?sr) "opt"))
    =>
    ;(printout t cannot-measure-in-dark " " ?ins " " ?sr crlf)
    (modify ?c (can-take-measurements no) (reason "Passive optical all purpose hi res imagers such as Landsat or ASTER must fly in AM orbits to decrease cloudiness"))
    )

(defrule CAPABILITIES::image-distortion-at-low-altitudes-in-side-looking-instruments
    "Passive optical instruments cannot take their measurements in DD RAANs"
    (declare (salience 10))
    ?c <- (CAPABILITIES::can-measure (instrument ?ins) (orbit-altitude# ?h&~nil&:(<= ?h 400)) (can-take-measurements yes))
    (DATABASE::Instrument (Name ?ins) (Geometry slant) (Spectral-region ?sr))
    =>
    ;(printout t image-distortion " " ?ins crlf)
    (modify ?c (can-take-measurements no) (reason "Side-looking instruments suffer from image distortion at low altitudes"))
    )

(defrule CAPABILITIES::two-lidars-at-same-frequency-cannot-work
    "Two lidars at same frequency can interfere with each other"
    (declare (salience 10))
    ?l1 <- (CAPABILITIES::can-measure (instrument ?ins1) (can-take-measurements yes))
    ?l2 <- (CAPABILITIES::can-measure (instrument ?ins2&~?ins1) (can-take-measurements yes))
    (DATABASE::Instrument (Name ?ins1) (Intent "Laser altimeters") (spectral-bands $?sr))
    (DATABASE::Instrument (Name ?ins2) (Intent "Laser altimeters") (spectral-bands $?sr))
    
    =>
    ;(printout t two-lidars-same-freq " " ?ins1 " " ?ins2 " " $?sr crlf)
    (modify ?l1 (can-take-measurements no) (reason "Two lidars at same frequency can interfere with each other"))
    (modify ?l2 (can-take-measurements no) (reason "Two lidars at same frequency can interfere with each other"))
    )

(defrule CAPABILITIES::resource-limitations-datarate
    (declare (salience 10))
    ?l1 <- (CAPABILITIES::can-measure (instrument ?ins1) (can-take-measurements yes) (data-rate-duty-cycle# nil))
    (MANIFEST::Mission  (sat-data-rate-per-orbit# ?rbo&~nil))
    =>
    (bind ?dc (min 1 (/ (* 7 60 500 (/ 1 8192)) ?rbo)))
    (modify ?l1 (data-rate-duty-cycle# ?dc) (reason "Cumulative spacecraft data rate cannot be downloaded to ground stations"))
    
    )

(defrule CAPABILITIES::resource-limitations-power
    (declare (salience 10))
    ?l1 <- (CAPABILITIES::can-measure (instrument ?ins1) (can-take-measurements yes) (power-duty-cycle# nil))
    (MANIFEST::Mission  (satellite-BOL-power# ?pow&~nil))
    =>
    (bind ?dc (min 1 (/ 10000 ?pow)))
    (modify ?l1 (power-duty-cycle# ?dc) (reason "Cumulative spacecraft power exceeds 10kW"))
    
    )
        
(defrule CAPABILITIES::get-instrument-revisit-times-from-database
    (declare (salience 5))
    ?instr <- (CAPABILITIES::Manifested-instrument (Name ?name) (Field-of-view# ?fov) (mission-architecture ?arch) (num-of-planes# ?nplanes) (num-of-sats-per-plane# ?nsats) (num-of-planes# ?nplanes) (orbit-altitude# ?h) (orbit-inclination ?inc))
    (DATABASE::Revisit-time-of (mission-architecture ?arch) (num-of-sats-per-plane# ?nsats) (num-of-planes# ?nplanes) (orbit-altitude# ?h) (orbit-inclination ?inc) (instrument-field-of-view# ?fov) (avg-revisit-time-global# ?revtime-global) (avg-revisit-time-tropics# ?revtime-tropics) (avg-revisit-time-northern-hemisphere# ?revtime-NH) (avg-revisit-time-southern-hemisphere# ?revtime-SH) (avg-revisit-time-cold-regions# ?revtime-cold) (avg-revisit-time-US# ?revtime-US))
    =>
    (modify ?instr (avg-revisit-time-global# ?revtime-global) (avg-revisit-time-tropics# ?revtime-tropics) (avg-revisit-time-northern-hemisphere# ?revtime-NH) (avg-revisit-time-southern-hemisphere# ?revtime-SH) (avg-revisit-time-cold-regions# ?revtime-cold) (avg-revisit-time-US# ?revtime-US))
    )

