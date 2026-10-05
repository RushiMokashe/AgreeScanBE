package com.myagree.app.store;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFlashDealTrueOrderByIdAsc();

    /**
     * Products in {@code category} whose name, short name, description or tag matches {@code pattern};
     * a {@code null} argument disables that filter. Build the pattern with {@link #containsPattern(String)}.
     */
    @Query("""
            select p from Product p
            where (:category is null or p.category = :category)
              and (:pattern is null
                   or lower(p.name) like :pattern escape '!'
                   or lower(p.shortName) like :pattern escape '!'
                   or lower(p.description) like :pattern escape '!'
                   or lower(p.tag) like :pattern escape '!')
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
