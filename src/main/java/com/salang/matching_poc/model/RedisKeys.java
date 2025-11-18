package com.salang.matching_poc.model;

import org.springframework.lang.NonNull;

import java.util.Objects;

public class RedisKeys {
    public static final String USER_STATUS_KEY = "user:%s:status";
    public static final String USER_LAST_JOIN_AT_KEY = "user:%s:lastJoinAt";
    public static final String USER_MATCHED_WITH_KEY = "user:%s:matchedWith";
    public static final String USER_GENDER_KEY = "user:%s:gender";
    public static final String MATCHING_QUEUE_KEY = "matching:queue";

    @NonNull
    public static String getUserStatusKey(String userId) {
        return Objects.requireNonNull(String.format(USER_STATUS_KEY, userId));
    }

    @NonNull
    public static String getUserLastJoinAtKey(String userId) {
        return Objects.requireNonNull(String.format(USER_LAST_JOIN_AT_KEY, userId));
    }

    @NonNull
    public static String getUserMatchedWithKey(String userId) {
        return Objects.requireNonNull(String.format(USER_MATCHED_WITH_KEY, userId));
    }

    @NonNull
    public static String getUserGenderKey(String userId) {
        return Objects.requireNonNull(String.format(USER_GENDER_KEY, userId));
    }
}
