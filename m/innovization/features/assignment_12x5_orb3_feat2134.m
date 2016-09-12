function bool =assignment_12x5_orb3_feat2134(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[1,0,0,0,0,1,0,1,0,1,0,1]))))
bool = true;
end
