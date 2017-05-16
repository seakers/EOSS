package mining;


public class DrivingFeaturesParams {
	
    public static double support_threshold = 0.2;
    public static double confidence_threshold = 0.5;
    public static double lift_threshold = 1.0;

    public static boolean tallMatrix = true;

    // Maximum length of features
    public static int maxLength = 3;
    
    public static boolean run_mRMR = true;
    
    public static int max_number_of_features_before_mRMR = Integer.MAX_VALUE;
    
    
}
