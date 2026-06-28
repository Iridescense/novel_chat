package com.novelchat.data.db

import androidx.room.TypeConverter

/**
 * Room 类型转换器
 */
class Converters {

    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isBlank()) emptyList()
        else value.split(",").map { it.toLong() }
    }
}
