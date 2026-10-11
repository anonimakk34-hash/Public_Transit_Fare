package calculator;

public class FareCalculation {
    private String calculationId;
    private String cardId;
    private double baseFare;
    private double finalFare;
    private FareState status;

    public FareCalculation(String calculationId, String cardId, double baseFare) {
        this.calculationId = calculationId;
        this.cardId = cardId;
        this.baseFare = baseFare;
        this.status = FareState.PENDING;
    }

    public void markCalculated(double finalFare) {
        if (this.status != FareState.PENDING) {
            throw new IllegalStateException("Cannot calculate: FareCalculation is not in PENDING state. Current state: " + this.status);
        }
        this.finalFare = finalFare;
        this.status = FareState.CALCULATED;
    }

    public void cancelCalculation() {
        if (this.status != FareState.CALCULATED && this.status != FareState.PENDING) {
            throw new IllegalStateException("Cannot cancel: FareCalculation must be PENDING or CALCULATED. Current state: " + this.status);
        }
        this.status = FareState.CANCELLED;
    }

    public String getCalculationId() { return calculationId; }
    public String getCardId() { return cardId; }
    public double getBaseFare() { return baseFare; }
    public double getFinalFare() { return finalFare; }
    public FareState getStatus() { return status; }
}
