function bool =assignment_12x5_orb1_feat4096(soln)
bool = false;
if( not( logical( abs( soln(1:12) -[1,1,1,1,1,1,1,1,1,1,1,1]))))
bool = true;
end
