function bool =assignment_12x5_orb3_feat2197(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[1,0,0,0,1,0,0,1,0,1,0,0]))))
bool = true;
end