function bool =assignment_12x5_orb3_feat2480(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[1,0,0,1,1,0,1,0,1,1,1,1]))))
bool = true;
end