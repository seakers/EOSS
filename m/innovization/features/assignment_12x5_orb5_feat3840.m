function bool =assignment_12x5_orb5_feat3840(soln)
bool = false;
if( not( logical( abs( soln(49:60) -[1,1,1,0,1,1,1,1,1,1,1,1]))))
bool = true;
end