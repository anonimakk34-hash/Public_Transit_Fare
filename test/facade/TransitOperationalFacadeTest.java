package facade;

import calculator.FareCalculation;
import calculator.FareRepository;
import ledger.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import validator.TapRecord;
import validator.TapRepository;
import validator.TapState;
import calculator.FareState;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransitOperationalFacadeTest {

    private TransitOperationalFacade facade;
    private TapRepository tapRepo;
    private FareRepository fareRepo;
    private LedgerRepository ledgerRepo;
    private String testCardId;

    @BeforeEach
    void setUp() {
        // F.I.R.S.T Principle: Independent & Repeatable. Clean the database before every test.
        new File("taps.json").delete();
        new File("fares.json").delete();
        new File("ledger.json").delete();

        facade = new TransitOperationalFacade();
        tapRepo = new TapRepository();
        fareRepo = new FareRepository();
        ledgerRepo = new LedgerRepository();
    }

    @Test
    void testHappyPathJourney() {
        givenStandardCard();
        whenJourneyIsProcessed();
        thenTapIs(TapState.VALIDATED);
        andFareIs(FareState.CALCULATED);
    }

    @Test
    void testDepth1CompensationOnPricingTimeout() {
        givenCardWithPricingTimeout();
        whenJourneyIsProcessed();
        thenTapIs(TapState.VOIDED);
        // Note: We don't assert FareState here because the chain breaks before Fare is ever committed
    }

    @Test
    void testDepth2CompensationOnInsufficientFunds() {
        givenCardWithInsufficientFunds();
        whenJourneyIsProcessed();
        andFareIs(FareState.CANCELLED);
        andTapIs(TapState.VOIDED);
    }

    @Test
    void testPersistenceReloadMidChain() {
        // Имитируем падение системы сразу после шага A (Intake)
        givenStandardCard();
        TapRecord crashTap = new TapRecord("CRASH-TAP-1", testCardId);
        crashTap.validateRecord(); // <--- ЭТОТ ВЫЗОВ ОБЯЗАТЕЛЕН
        tapRepo.saveTap(crashTap);

        // Пересоздаем репозиторий, чтобы сымитировать рестарт приложения
        TapRepository newTapRepo = new TapRepository();
        List<TapRecord> pendingTaps = newTapRepo.loadPendingTaps(); // Кастомный хелпер для загрузки прерванных цепочек

        assertTrue(pendingTaps.stream().anyMatch(t -> t.getTapId().equals("CRASH-TAP-1")));
    }

    // --- DOMAIN-SPECIFIC TESTING LANGUAGE (DSL) HELPERS ---

    private void givenStandardCard() {
        this.testCardId = "CTS-STU-12345";
    }

    private void givenCardWithInsufficientFunds() {
        this.testCardId = "CTS-STU-BROKE";
    }

    private void whenJourneyIsProcessed() {
        facade.processJourney(testCardId);
    }

    private void thenTapIs(TapState expectedState) {
        List<TapRecord> taps = tapRepo.loadAllTaps();
        TapRecord record = taps.stream()
                .filter(t -> t.getCardId().equals(testCardId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tap record not found"));
        assertEquals(expectedState, record.getStatus());
    }

    private void andFareIs(FareState expectedState) {
        List<FareCalculation> fares = fareRepo.loadAllFares();
        FareCalculation record = fares.stream()
                .filter(f -> f.getCardId().equals(testCardId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fare record not found"));
        assertEquals(expectedState, record.getStatus());
    }

    private void andTapIs(TapState expectedState) {
        thenTapIs(expectedState); // Syntactic sugar for readability
    }

    private void givenCardWithPricingTimeout() {
        this.testCardId = "CTS-STU-TIMEOUT";
    }
}