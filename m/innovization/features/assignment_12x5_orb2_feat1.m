function bool =assignment_12x5_orb2_feat1(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,0,0,0,0,0,0,0,0,0,0,0]))))
bool = true;
end