package com.streamflow.processor.service;

/**
 * Tracks active users in Redis with a TTL-based sliding window.
 * A user is considered "active" if they sent an event within the last 30 minutes.
 */
public interface ActiveUserTracker {
    /**
     * Records that the given user was active right now.
     * Refreshes the TTL on each call so the user stays "active" as long as they keep sending events.
     */
    void recordActivity(Long userId);

    /** Returns the current count of distinct active users. */
    long getActiveUserCount();
}
