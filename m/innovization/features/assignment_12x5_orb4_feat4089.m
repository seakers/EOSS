function bool =assignment_12x5_orb4_feat4089(soln)
bool = false;
if( not( logical( abs( soln(37:48) -[1,1,1,1,1,1,1,1,1,0,0,0]))))
bool = true;
end