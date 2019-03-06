package com.file.demo.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapBuilder {
    private Map _map = new LinkedHashMap();

    public MapBuilder() {
    }

    public static MapBuilder start() {
        return new MapBuilder();
    }

    public static MapBuilder start(Object key, Object value) {
        MapBuilder mapBuilder = new MapBuilder();
        mapBuilder.put(key, value);
        return mapBuilder;
    }

    public static MapBuilder start(Map src) {
        MapBuilder mapBuilder = new MapBuilder();
        mapBuilder.putAll(src);
        return mapBuilder;
    }

    public MapBuilder put(Object key, Object value) {
        if (value != null) {
            this._map.put(key, value);
        }

        return this;
    }

    public MapBuilder putList(Object key, Object value) {
        if (value != null) {
            this._map.put(key, SpringConverter.convert(value, List.class));
        }

        return this;
    }

    public MapBuilder putAll(Map src) {
        if (src != null || src.size() > 0) {
            this._map.putAll(src);
        }

        return this;
    }

    public Map build() {
        return this._map;
    }
}
