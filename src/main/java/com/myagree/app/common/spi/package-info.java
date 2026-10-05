/**
 * Seams between the feature slices (docs/architecture/phase-2.md, section 5). A slice that needs another slice's data
 * or behaviour depends on a type here, never on the other slice's classes, so every slice compiles, runs and is
 * tested before the others exist. Each type states which slice implements it, which slices consume it, and how
 * failures are reported.
 *
 * <p>How consumers inject them:
 * <ul>
 *   <li>Several implementations ({@link com.myagree.app.common.spi.PayableResolver},
 *       {@link com.myagree.app.common.spi.FarmContextContributor},
 *       {@link com.myagree.app.common.spi.AdminMetricsContributor},
 *       {@link com.myagree.app.common.spi.ProfileProvisioner}): inject {@code List<...>}, which is empty until an
 *       implementing slice exists.</li>
 *   <li>One implementation ({@link com.myagree.app.common.spi.Notifier},
 *       {@link com.myagree.app.common.spi.RentalHubDirectory},
 *       {@link com.myagree.app.common.spi.MandiMarketDirectory},
 *       {@link com.myagree.app.common.spi.FarmerPreferencesReader}): inject {@code ObjectProvider<...>} and call
 *       {@code getIfAvailable()} (or {@code ifAvailable(...)}) where it is used, with the documented fallback when
 *       the implementing slice is absent.</li>
 * </ul>
 * Implementations are Spring beans in the implementing slice's package. They join the caller's transaction unless
 * their documentation says otherwise, and never call back into the consuming slice.
 */
package com.myagree.app.common.spi;
