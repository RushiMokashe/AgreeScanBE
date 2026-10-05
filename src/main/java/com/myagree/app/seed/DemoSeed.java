package com.myagree.app.seed;

/**
 * One slice of the demo data, loaded into an empty database at startup. Implement it as a Spring bean in any
 * package: the coordinator runs every seed in {@link #order()} inside one transaction. Seeds share the records they
 * create through the {@link SeedContext} instead of calling each other.
 *
 * <p>The seeds so far, and the records they put:
 * <table>
 *   <caption>Demo seeds in run order</caption>
 *   <tr><th>order</th><th>seed</th><th>puts</th></tr>
 *   <tr><td>0</td><td>{@link AccountSeed}</td><td>{@link AccountSeed#ACCOUNTS}</td></tr>
 *   <tr><td>10</td><td>{@link FarmerSeed}</td><td>{@link FarmerSeed#FARMERS}</td></tr>
 *   <tr><td>20</td><td>{@link StoreSeed}</td><td>{@link StoreSeed#CATALOG}</td></tr>
 *   <tr><td>30</td><td>{@link CareSeed}</td><td>{@link CareSeed#STOCKISTS}</td></tr>
 *   <tr><td>40</td><td>{@link PlotSeed}</td><td>{@link PlotSeed#PLOTS}</td></tr>
 *   <tr><td>50</td><td>{@link ScanSeed}</td><td>{@link ScanSeed#SCANS}</td></tr>
 *   <tr><td>60</td><td>{@code TreatmentSeed}</td><td></td></tr>
 *   <tr><td>70</td><td>{@code PrescriptionSeed}</td><td></td></tr>
 *   <tr><td>80</td><td>{@code MandiSeed}</td><td></td></tr>
 *   <tr><td>90</td><td>{@code RentalSeed}</td><td></td></tr>
 *   <tr><td>100</td><td>{@code DashboardSeed}</td><td></td></tr>
 * </table>
 */
public interface DemoSeed {

    /**
     * Position in the run, lowest first; unique across seeds (the coordinator refuses to start otherwise). A seed runs
     * after the seeds whose records it reads, e.g. 45 runs after the plots (40) and before the scans (50).
     */
    int order();

    /** Creates this seed's records, reading what earlier seeds put into {@code context} and adding its own. */
    void seed(SeedContext context);
}
