package facade;

import validator.*;
import calculator.*;
import ledger.*;
import java.util.UUID;

public class TransitOperationalFacade {

    // Subsystem A (Intake)
    private CardValidator validator = new CardValidator();
    private TapRepository tapRepo = new TapRepository();

    // Subsystem B (Processing)
    private DiscountEngine discountEngine = new DiscountEngine();
    private FareRepository fareRepo = new FareRepository();

    // Subsystem C (Finalization)
    private BalanceManager balanceManager = new BalanceManager();
    private LedgerRepository ledgerRepo = new LedgerRepository();

    public void processJourney(String cardId) {
        String tapId = UUID.randomUUID().toString();
        System.out.println("Starting journey for card: " + cardId);

        // --- STEP A: INTAKE ---
        TapRecord tap = validator.processNewTap(tapId, cardId);
        tapRepo.saveTap(tap);
        System.out.println("Step A: Tap validated and saved.");

        FareCalculation fare = null;
        try {
            // --- STEP B: PROCESSING ---
            // Simulates a realistic B-failure (e.g., CTS tariff database timeout)
            fare = discountEngine.calculateFare(UUID.randomUUID().toString(), cardId, 100.0);
            fareRepo.saveFare(fare);
            System.out.println("Step B: Fare calculated at " + fare.getFinalFare());

            // --- STEP C: FINALIZATION ---
            // Simulates a realistic C-failure (e.g., insufficient funds on the card)
            LedgerTransaction tx = balanceManager.processDeduction(UUID.randomUUID().toString(), tapId, fare.getFinalFare(), cardId);
            tx.commit();
            ledgerRepo.saveTransaction(tx);
            System.out.println("Step C: Transaction committed successfully. Journey complete.");

        } catch (PricingTimeoutException e) {
            // DEPTH 1 COMPENSATION: B fails, rollback A
            System.err.println("Error in Step B (Pricing): " + e.getMessage());
            System.out.println("Initiating Depth 1 Rollback...");

            tap.voidRecord();
            tapRepo.saveTap(tap);
            System.out.println("Rollback Complete: Tap VOIDED.");

        } catch (InsufficientFundsException e) {
            // DEPTH 2 COMPENSATION: C fails, rollback B then A
            System.err.println("Error in Step C (Ledger): " + e.getMessage());
            System.out.println("Initiating Depth 2 Rollback...");

            if (fare != null) {
                fare.cancelCalculation();
                fareRepo.saveFare(fare);
                System.out.println("Rollback Step 1: Fare CANCELLED.");
            }

            tap.voidRecord();
            tapRepo.saveTap(tap);
            System.out.println("Rollback Step 2: Tap VOIDED.");
        }
    }
}