function bool =assignment_12x5_orb5_feat1562(soln)
bool = false;
if( not( logical( abs( soln(49:60) -[0,1,1,0,0,0,0,1,1,0,0,1]))))
bool = true;
end