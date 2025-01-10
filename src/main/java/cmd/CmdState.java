package cmd;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CmdState {

    private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

    public <T> void put(String key, T value) {
        state.put(key, value);
    }

    public <T> T get(String key) {
        //noinspection unchecked
        return (T) state.get(key);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        if (!state.containsKey(key)) return defaultValue;
        //noinspection unchecked
        return (T) state.get(key);
    }

    public static final CmdState INSTANCE = new CmdState();
}
