package com.file.demo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListBuilder {
    public static List EMPTY = new ArrayList();
    private List _list = new ArrayList();

    public ListBuilder() {
    }

    public static ListBuilder start() {
        return new ListBuilder();
    }

    public ListBuilder add(Object value) {
        if (value != null) {
            this._list.add(value);
        }

        return this;
    }

    public ListBuilder add(Object... values) {
        if (values != null && values.length > 0) {
            this._list.addAll(Arrays.asList(values));
        }

        return this;
    }

    public ListBuilder addAll(Collection values) {
        if (values != null) {
            this._list.add(values);
        }

        return this;
    }

    public List build() {
        return this._list;
    }
}
