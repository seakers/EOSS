function bool =assignment_12x5_orb3_feat3461(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[1,1,0,1,1,0,0,0,0,1,0,0]))))
bool = true;
end