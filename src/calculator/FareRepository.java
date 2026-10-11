package calculator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FareRepository {
    private static final String FILE_PATH = "fares.json";
    private Gson gson = new Gson();

    /**
     * Saves a new fare calculation or overwrites an existing one to update its state.
     */
    public void saveFare(FareCalculation fare) {
        List<FareCalculation> fares = loadAllFares();

        // Remove existing entry if we are updating state (e.g., PENDING -> CALCULATED)
        fares.removeIf(f -> f.getCalculationId().equals(fare.getCalculationId()));
        fares.add(fare);

        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(fares, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to durable storage: " + FILE_PATH, e);
        }
    }

    /**
     * Called by the Facade on application startup to detect broken chains.
     */
    public List<FareCalculation> loadPendingCalculations() {
        return loadAllFares().stream()
                .filter(f -> f.getStatus() == FareState.PENDING || f.getStatus() == FareState.CALCULATED)
                .collect(Collectors.toList());
    }

    public List<FareCalculation> loadAllFares() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<FareCalculation>>(){}.getType();
            List<FareCalculation> fares = gson.fromJson(reader, listType);
            return fares != null ? fares : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read durable storage: " + FILE_PATH, e);
        }
    }
}