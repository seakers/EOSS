package mining;


public class DrivingFeaturesParams {
	
    public static double support_threshold = 0.10;
    public static double confidence_threshold = 0.4;
    public static double support_threshold_clustering = 0.3;
    public static double confidence_threshold_clustering = 0.1;
    public static double lift_threshold = 1.0;

    public static boolean tallMatrix = true;

    public static int maxLength = 3;
    public static int max_number_of_features_before_mRMR = 500;
    public static boolean run_mRMR = true;
    
}
