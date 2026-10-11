package validator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TapRepository {
    private static final String FILE_PATH = "taps.json";
    private Gson gson = new Gson();

    /**
     * Saves a new tap or overwrites an existing one to update its state.
     */
    public void saveTap(TapRecord tap) {
        List<TapRecord> taps = loadAllTaps();

        // Remove existing entry if we are updating state (e.g., NEW -> VALIDATED)
        taps.removeIf(t -> t.getTapId().equals(tap.getTapId()));
        taps.add(tap);

        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(taps, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to durable storage: " + FILE_PATH, e);
        }
    }

    /**
     * Called by the Facade on restart to find taps that never completed the chain.
     */
    public List<TapRecord> loadPendingTaps() {
        return loadAllTaps().stream()
                .filter(t -> t.getStatus() == TapState.VALIDATED)
                .collect(Collectors.toList());
    }

    public List<TapRecord> loadAllTaps() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<TapRecord>>(){}.getType();
            List<TapRecord> taps = gson.fromJson(reader, listType);
            return taps != null ? taps : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read durable storage: " + FILE_PATH, e);
        }
    }
}