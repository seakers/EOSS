function bool =assignment_12x5_orb5_feat2411(soln)
bool = false;
if( not( logical( abs( soln(49:60) -[1,0,0,1,0,1,1,0,1,0,1,0]))))
bool = true;
end
