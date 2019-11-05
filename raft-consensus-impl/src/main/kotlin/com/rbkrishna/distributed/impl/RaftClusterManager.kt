package com.rbkrishna.distributed.impl

import com.rbkrishna.distributed.raft.api.Command
import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventScheduler
import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventSchedulerTimerImpl
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class RaftClusterManager {
    private val raftCluster = RaftCluster(mutableMapOf())
    private val nodeToSchedulerMap = mutableMapOf<Int, HeartbeatEventScheduler>()

    @GetMapping("/addNode/{nodeId}")
    fun addNode(@PathVariable nodeId: Int): String {
        val nodeMap = raftCluster.nodeMap
        val nodes = nodeMap.values
        val newNode = RaftNodeImpl(
            PersistentState(nodeId),
            NodeState.FOLLOWER,
            VolatileState(),
            raftCluster = raftCluster
        ).apply {
            val nodeIds = nodes.map { it.persistentState.id }
            this.nodeIds.addAll(nodeIds)
            val hbScheduler = HeartbeatEventSchedulerTimerImpl(
                if (nodeId == 1) 500L
                else 1500L
            )
            this.heartbeatEventScheduler = hbScheduler
            hbScheduler.attachClient(this)
            hbScheduler.scheduleHeartbeat()
            nodeToSchedulerMap[this.persistentState.id] = hbScheduler
        }
        nodeMap.putIfAbsent(nodeId, newNode)
        nodes.filter { it != newNode }.forEach { node -> node.handleJoinNotification(nodeId) }
        return "SUCCESS"
    }

    @GetMapping("/removeNode/{nodeId}")
    fun removeNode(@PathVariable nodeId: Int): String {
        val nodeMap = raftCluster.nodeMap
        val quitNode = nodeMap[nodeId]
        nodeMap.remove(nodeId)
        nodeToSchedulerMap[nodeId]?.cancel()
        quitNode?.let {
            nodeMap.values.forEach { node -> node.handleQuitNotification(nodeId) }
        }
        return "SUCCESS"
    }

    @GetMapping("/sendCommand/{nodeId}/{command}")
    fun sendCommand(@PathVariable nodeId: Int, @PathVariable command: Command<String>): String {
        raftCluster.nodeMap[nodeId]?.onCommandReceived(command)
        return "SUCCESS"
    }

    @GetMapping("/getNodes")
    fun getNodes(): List<String> {
        return raftCluster.nodeMap.values
            .map { it.toString() }
            .toList()
    }
}