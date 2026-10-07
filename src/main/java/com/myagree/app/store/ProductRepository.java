package com.myagree.app.store;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFlashDealTrueOrderByIdAsc();

    List<Product> findAllByOrderByIdAsc();

    Optional<Product> findByBarcode(String barcode);

    /** The product whose current photo is this file. */
    Optional<Product> findByPhotoFileName(String fileName);

    /** A shop's products, newest first, for its portal. */
    List<Product> findByShopIdOrderByIdDesc(long shopId);

    Optional<Product> findByIdAndShopId(long id, long shopId);

    long countByShopId(long shopId);

    long countByShopIdAndInStockFalse(long shopId);

    /**
     * Products in {@code category} whose name, short name, description or tag matches {@code pattern} in any
     * language; a {@code null} argument disables that filter. Build the pattern with {@link #containsPattern(String)}.
     */
    @Query("""
            select p from Product p
            where (:category is null or p.category = :category)
              and (:pattern is null
                   or lower(p.name.en) like :pattern escape '!'
                   or lower(p.name.mr) like :pattern escape '!'
                   or lower(p.name.hi) like :pattern escape '!'
                   or lower(p.shortName.en) like :pattern escape '!'
                   or lower(p.shortName.mr) like :pattern escape '!'
                   or lower(p.shortName.hi) like :pattern escape '!'
                   or lower(p.description.en) like :pattern escape '!'
                   or lower(p.description.mr) like :pattern escape '!'
                   or lower(p.description.hi) like :pattern escape '!'
                   or lower(p.tag.en) like :pattern escape '!'
                   or lower(p.tag.mr) like :pattern escape '!'
                   or lower(p.tag.hi) like :pattern escape '!')
            order by p.id""")
    List<Product> search(@Nullable ProductCategory category, @Nullable String pattern);

    /** Case-insensitive "contains" pattern for {@link #search}, with LIKE wildcards in the text escaped. */
    static String containsPattern(String text) {
        String escaped = text.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
