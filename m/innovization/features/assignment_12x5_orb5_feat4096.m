function bool =assignment_12x5_orb5_feat4096(soln)
bool = false;
if( not( logical( abs( soln(49:60) -[1,1,1,1,1,1,1,1,1,1,1,1]))))
bool = true;
end
