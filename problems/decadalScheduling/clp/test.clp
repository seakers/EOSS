(defmodule TEST)
(defrule TEST::recompute-power-needs-lidar-or-SAR
    "The power consumption of an active lidar or SAR is assumed to be for 
    400km and 600km respectively. If the orbit altitude is not this one, it needs to be corrected"
    ?i <- (CAPABILITIES::Manifested-instrument (Name ?name) (Illumination Active) (Intent ?int)
        (characteristic-orbit ?orb&~nil) (orbit-altitude# ?h&~nil) (orbit-type ?typ) 
        (orbit-RAAN ?raan) (orbit-inclination ?inc) (average-power# ?p&~nil))
    (test (neq ?h (get-orbit-altitude ?orb)))
    (or 
        (test (eq ?int "Laser altimeters")) 
        (test (eq ?int "Elastic lidar")) 
        (test (eq ?int "Differential Absorption Lidars")) 
        (test (eq ?int "Doppler Wind Lidars"))
        (test (eq ?int "Imaging MW radars (SAR)"))
        )
    =>
    (printout t "power at characteristic-orbit = " ?orb " is " ?p " and at new-orbit = " (str-cat ?typ "-" ?h "-" ?inc "-" ?raan) " is " (* ?p (** (/ (get-orbit-altitude ?orb) ?h) 3)) crlf)    
    (modify ?i (characteristic-orbit (str-cat ?typ "-" ?h "-" ?inc "-" ?raan)) 
        (average-power# (* ?p (** (/ (get-orbit-altitude ?orb) ?h) 3))))
    )
   