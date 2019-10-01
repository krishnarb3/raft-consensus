package com.rbkrishna.distributed.raft.api.heartbeat

interface HeartbeatEventClient {
    fun handleHeartbeatEvent()
}