package com.rbkrishna.distributed.raft.api.heartbeat

interface HeartbeatEventScheduler {
    fun scheduleHeartbeat()
    fun refreshHeartbeat()
    fun cancel()
    fun attachClient(heartbeatEventClient: HeartbeatEventClient)
}