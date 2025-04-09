package com.example.asiochatfrontend.data.database.converter;

import androidx.room.TypeConverter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.JsonAdapter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Collections;

public class ListConverter {

    private static final Moshi moshi = new Moshi.Builder().build();
    private static final Type stringListType = Types.newParameterizedType(List.class, String.class);
    private static final JsonAdapter<List<String>> stringListAdapter = moshi.adapter(stringListType);

    @TypeConverter
    public static String fromStringList(List<String> value) {
        return value == null ? null : stringListAdapter.toJson(value);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        try {
            return value == null ? Collections.emptyList() : stringListAdapter.fromJson(value);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}