package projects;

public class Stat {
    private double sum = 0.0;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private long count = 0;

    public void add(double v) {
        sum += v;
        if (v < min) min = v;
        if (v > max) max = v;
        count++;
    }

    public double avg() { return count == 0 ? 0 : sum / count; }
    public double sum() { return sum; }
    public double min() { return count == 0 ? 0 : min; }
    public double max() { return count == 0 ? 0 : max; }
    public long count() { return count; }
}