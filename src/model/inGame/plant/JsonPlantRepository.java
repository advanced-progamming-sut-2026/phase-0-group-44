package model.inGame.plant;

import model.enums.PlantType;

import java.util.ArrayList;

/**
 * Compatibility facade retained for callers from Phase 0.
 *
 * <p>Plant data is now canonicalized in {@code phase1/assets/Data/plants.csv};
 * this adapter deliberately delegates to that registry so JSON and CSV cannot
 * silently diverge.</p>
 */
@Deprecated
public class JsonPlantRepository implements PlantRepository {
    private final PlantRegistry canonical = PlantRegistry.getDefault();

    public JsonPlantRepository(String ignoredFilePath) {
    }

    @Override
    public void load() {
        // The canonical registry is loaded once by PlantRegistry.getDefault().
    }

    @Override
    public ArrayList<PlantDefinition> findAll() {
        return canonical.findAll();
    }

    @Override
    public PlantDefinition findByType(PlantType type) {
        return canonical.findByType(type);
    }

    @Override
    public PlantDefinition findByName(String name) {
        return canonical.findByName(name);
    }
}
