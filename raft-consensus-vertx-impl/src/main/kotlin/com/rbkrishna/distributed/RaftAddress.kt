package com.rbkrishna.distributed

enum class RaftAddress {
    REQUEST_VOTES,
    APPEND_ENTRIES,
    JOIN_NOTIFICATION,
    QUIT_NOTIFICATION
}