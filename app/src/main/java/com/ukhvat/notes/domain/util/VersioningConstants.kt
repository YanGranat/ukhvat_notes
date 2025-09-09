package com.ukhvat.notes.domain.util

/**
 * Constants for note versioning system
 * 
 * Located in domain layer for use in data and ui layers
 * without violating architectural boundaries.
 */
object VersioningConstants {
    // Versioning check interval (1 minute)
    const val VERSION_CHECK_INTERVAL_MS = 60_000L
    
    // Minimum character changes to create version
    const val MIN_CHANGE_FOR_VERSION = 140

    // Default number of regular versions to keep per note
    // Forced saves may increase per-note limit, but base policy keeps this many regular versions
    const val DEFAULT_MAX_VERSIONS = 100
}