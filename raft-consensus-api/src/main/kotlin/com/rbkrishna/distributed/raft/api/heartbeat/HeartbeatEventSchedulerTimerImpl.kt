package com.rbkrishna.distributed.raft.api.heartbeat

import java.util.Timer
import java.util.TimerTask

class HeartbeatEventSchedulerTimerImpl(private val period: Long) : HeartbeatEventScheduler {
    private lateinit var timer: Timer

    private lateinit var heartbeatEventClient: HeartbeatEventClient

    override fun scheduleHeartbeat() {
        createTimer()
    }

    override fun refreshHeartbeat() {
        timer.cancel()
        createTimer()
    }

    override fun cancel() {
        timer.cancel()
    }

    override fun attachClient(heartbeatEventClient: HeartbeatEventClient) {
        this.heartbeatEventClient = heartbeatEventClient
    }

    private fun createTimer(): Timer {
        timer = Timer("scheduleHeartbeat", false)
        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    heartbeatEventClient.handleHeartbeatEvent()
                }
            },
            1000,
            period
        )
        return timer
    }
}