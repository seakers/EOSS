function bool =assignment_12x5_orb4_feat2030(soln)
bool = false;
if( not( logical( abs( soln(37:48) -[0,1,1,1,1,1,1,0,1,1,0,1]))))
bool = true;
end
