

(deffunction adapt-GEO-revisit (?tr)
	(return (+ ?tr 0.01))
	)
	
(defrule ASSIMILATION::modify-temporal-resolution
	?sub <-(ASSIMILATION::UPDATE-REV-TIME (parameter ?p) (avg-revisit-time-global# ?new-time)
	(avg-revisit-time-US# ?new-time-us))
	?meas <- (REQUIREMENTS::Measurement (Parameter ?p) (Temporal-resolution# nil) (Temporal-resolution nil) (Coverage ?region))
	=>
	(if (eq ?region Global) then (bind ?tr ?new-time) else (bind ?tr ?new-time-us))
	(modify ?meas (Temporal-resolution# ?tr) (avg-revisit-time-global# ?new-time) (avg-revisit-time-US# (adapt-GEO-revisit ?new-time-us))) 
)


(defrule CAPABILITIES::global-or-regional-coverage
	?meas <- (REQUIREMENTS::Measurement (Coverage nil) (orbit-type ?orb &~nil) )
	=>
	(if (eq ?orb GEO) then (bind ?coverage US) else (bind ?coverage Global))
	(modify ?meas (Coverage ?coverage) )
	)