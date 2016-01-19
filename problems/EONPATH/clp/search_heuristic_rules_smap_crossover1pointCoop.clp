	 
(defrule SEARCH-HEURISTICS::crossover-one-point
    "This mutation performs crossover on one point" 
    ?arch1 <- (MANIFEST::ARCHITECTURE (bitString ?orig) (num-sats-per-plane ?ns) (heuristics-to-apply $? crossover1point $?) (heuristics-applied $?applied&:(not-contains$ crossover1point $?applied)))
	?arch2 <- (MANIFEST::ARCHITECTURE (bitString ?orig2&~?orig)(num-sats-per-plane ?ns2) (heuristics-to-apply $? crossover1point $?) (heuristics-applied $?applied&:(not-contains$ crossover1point $?applied)))
    => 
	;(printout t crossover-one-point crlf)
    (bind ?N 1)
    (for (bind ?i 0) (< ?i ?N) (++ ?i)   
		(bind ?arch3 ((new rbsa.eoss.Architecture ?orig ?ns) crossover1point (new rbsa.eoss.Architecture ?orig2 ?ns2)))
    	(assert-string (?arch3 toFactString))
		(bind ?arch4 ((new rbsa.eoss.Architecture ?orig2 ?ns2) crossover1point (new rbsa.eoss.Architecture ?orig ?ns)))
    	(assert-string (?arch4 toFactString))
		)
	(modify ?arch1 (heuristics-applied (append$ ?applied crossover1point)))
	(modify ?arch2 (heuristics-applied (append$ ?applied crossover1point)))
    ) 
	
(deffacts DATABASE::add-crossover-list-of-improve-heuristics
(SEARCH-HEURISTICS::improve-heuristic (id crossover1point))
)

