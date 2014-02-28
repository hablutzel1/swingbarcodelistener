package pe.blobfish.barcode.test;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class CommonsMathDescriptiveStatisticsTest {
    public static void main(String[] args) {
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        descriptiveStatistics.addValue(23);
        descriptiveStatistics.addValue(26);
        descriptiveStatistics.addValue(20);
        System.out.println(descriptiveStatistics.getStandardDeviation());
    }
}
