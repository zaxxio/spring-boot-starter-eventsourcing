package org.eventa.core.aggregates;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class AggregateState {
    private Map<String, Object> stateMap = new HashMap<>();

    public void addField(String name, Object value) {
        this.stateMap.put(name, value);
    }

    public boolean hasField(String name) {
        return stateMap.containsKey(name);
    }

    public Object getField(String name) {
        return stateMap.get(name);
    }
}
