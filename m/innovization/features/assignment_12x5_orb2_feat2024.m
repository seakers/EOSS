function bool =assignment_12x5_orb2_feat2024(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,1,1,1,1,1,1,0,0,1,1,1]))))
bool = true;
end
