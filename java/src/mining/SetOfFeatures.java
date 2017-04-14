package mining;

public class SetOfFeatures implements Feature {

    private final double[] metrics;
    private final int[] indices;

    public SetOfFeatures(int[] indices, double[] metrics) {
        this.indices = indices;
        this.metrics = metrics;
    }

    public int[] getIndices() {
        return this.indices;
    }

    public double[] getMetrics() {
        return this.metrics;
    }

    @Override
    public double getSupport() {
        return metrics[0];
    }

    @Override
    public double getFConfidence() {
        return metrics[2];
    }

    @Override
    public double getRConfidence() {
        return metrics[3];
    }

    @Override
    public double getLift() {
        return metrics[1];
    }

}
