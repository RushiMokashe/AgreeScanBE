package com.myagree.app.payment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.myagree.app.common.spi.PaymentPurpose;

interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** The farmer's latest payment of a reference in one of these states. */
    Optional<Payment> findFirstByFarmerIdAndPurposeAndReferenceIdAndStatusInOrderByIdDesc(
            long farmerId, PaymentPurpose purpose, long referenceId, Collection<PaymentStatusCode> statuses);

    /** One of the farmer's payments; another farmer's id finds nothing. */
    Optional<Payment> findByIdAndFarmerId(long id, long farmerId);

    /** A Scan & Pay payment sent to the seller; payments to other sellers find nothing. */
    Optional<Payment> findByIdAndPayeeUserId(long id, long payeeUserId);

    /** The Scan & Pay payments sent to a seller, optionally in one status, newest first. */
    @Query("""
            select p from Payment p
            where p.payeeUserId = :payeeUserId and (:status is null or p.status = :status)
            order by p.createdAt desc, p.id desc""")
    List<Payment> findForPayee(long payeeUserId, @Nullable PaymentStatusCode status, Pageable pageable);

    /** The payment a provider knows by {@code reference}, for its webhooks. */
    Optional<Payment> findByProviderAndProviderReference(PaymentProviderKind provider, String reference);

    /** Every payment, optionally in one status, newest first. */
    @Query("select p from Payment p where (:status is null or p.status = :status) order by p.createdAt desc, p.id desc")
    Page<Payment> findAllByStatus(@Nullable PaymentStatusCode status, Pageable pageable);

    /** Rupees received since {@code from}. */
    @Query("select coalesce(sum(p.amountRupees), 0) from Payment p where p.status = :succeeded and p.settledAt >= :from")
    long sumSettledSince(PaymentStatusCode succeeded, Instant from);
}
