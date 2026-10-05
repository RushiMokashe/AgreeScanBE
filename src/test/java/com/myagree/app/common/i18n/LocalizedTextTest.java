package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LocalizedTextTest {

    private static final LocalizedText TOMATO = LocalizedText.of("Tomato", "टोमॅटो", "टमाटर");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void resolvesEachLanguage() {
        assertThat(TOMATO.resolve(Language.EN)).isEqualTo("Tomato");
        assertThat(TOMATO.resolve(Language.MR)).isEqualTo("टोमॅटो");
        assertThat(TOMATO.resolve(Language.HI)).isEqualTo("टमाटर");
    }

    @Test
    void missingOrBlankTranslationsFallBackToEnglish() {
        LocalizedText englishOnly = LocalizedText.of("Mahindra 575 DI", null, "  ");

        assertThat(englishOnly.resolve(Language.MR)).isEqualTo("Mahindra 575 DI");
        assertThat(englishOnly.resolve(Language.HI)).isEqualTo("Mahindra 575 DI");
        assertThat(englishOnly.hi()).isNull();
    }

    @Test
    void textsAreStrippedAndEnglishIsRequired() {
        assertThat(LocalizedText.of("  Tomato ", " टोमॅटो", null)).isEqualTo(LocalizedText.of("Tomato", "टोमॅटो", null));
        assertThatIllegalArgumentException().isThrownBy(() -> LocalizedText.of(" ", "टोमॅटो", "टमाटर"));
        assertThatIllegalArgumentException().isThrownBy(() -> LocalizedText.of(null, "टोमॅटो", "टमाटर"));
    }

    @Test
    void dtoCarriesAllThreeTexts() {
        LocalizedTextDto dto = LocalizedTextDto.from(TOMATO);

        assertThat(dto).isEqualTo(new LocalizedTextDto("Tomato", "टोमॅटो", "टमाटर"));
        assertThat(dto.toLocalizedText()).isEqualTo(TOMATO);
    }

    @Test
    void dtoNeedsEnglishAndLimitsTheLength() {
        String tooLong = "x".repeat(LocalizedText.MAX_LENGTH + 1);

        assertThat(validator.validate(new LocalizedTextDto("Tomato", null, null))).isEmpty();
        assertThat(violatedFields(new LocalizedTextDto(" ", "टोमॅटो", null))).containsExactly("en");
        assertThat(violatedFields(new LocalizedTextDto("Tomato", tooLong, tooLong))).containsExactlyInAnyOrder("mr", "hi");
    }

    private static String[] violatedFields(LocalizedTextDto dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toArray(String[]::new);
    }
}
