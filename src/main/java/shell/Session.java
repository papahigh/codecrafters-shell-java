package shell;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Session {

    private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (!state.containsKey(key)) return defaultValue;
        return (T) state.get(key);
    }

    public <T> void put(String key, T value) {
        state.put(key, value);
    }
}
