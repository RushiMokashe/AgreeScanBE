package com.myagree.app.payment;

import org.springframework.stereotype.Component;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.payment.dto.ConfirmPaymentRequest;
import com.myagree.app.payment.dto.ConfirmPaymentRequest.SimulatedOutcome;

/**
 * The test provider of docs/architecture/phase-2.md, D5: no network and no real money. The payment page lets the
 * tester choose the outcome, which the confirmation carries.
 */
@Component
class SimulatedPaymentProvider implements PaymentProvider {

    private static final String REFERENCE_PREFIX = "sim_";
    private static final String DECLINED = "Declined in the test checkout";
    private static final UserMessage OUTCOME_REQUIRED = UserMessage.of("payment.simulated-outcome-required");

    @Override
    public PaymentProviderKind kind() {
        return PaymentProviderKind.SIMULATED;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ProviderSession open(Payment payment) {
        return new ProviderSession(REFERENCE_PREFIX + payment.getId(), null);
    }

    @Override
    public ProviderOutcome confirm(Payment payment, ConfirmPaymentRequest request) {
        SimulatedOutcome outcome = request.simulatedOutcome();
        if (outcome == null) {
            throw new BadRequestException(OUTCOME_REQUIRED);
        }
        return switch (outcome) {
            case SUCCEEDED -> ProviderOutcome.SUCCEEDED;
            case FAILED -> ProviderOutcome.failed(DECLINED);
        };
    }
}
