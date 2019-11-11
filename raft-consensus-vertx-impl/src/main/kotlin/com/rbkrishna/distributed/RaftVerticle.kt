package com.rbkrishna.distributed

import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventSchedulerTimerImpl
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.ignite.IgniteClusterManager
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.NetworkInterface

class RaftVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(RaftVerticle::class.java)
    private val MAX_NUMBER_OF_NODES = 100
    private val HTTP_PORT = "http_port"

    lateinit var raftNode: RaftNodeVertxImpl

    override fun start() {
        val randomNodeId = (0..MAX_NUMBER_OF_NODES).random()

        val httpPort = System.getProperty(HTTP_PORT)

        raftNode = RaftNodeVertxImpl(
            PersistentState(randomNodeId),
            NodeState.FOLLOWER,
            VolatileState(),
            vertx = vertx
        ).apply {
            val hbScheduler = HeartbeatEventSchedulerTimerImpl(5000L)
            this.heartbeatEventScheduler = hbScheduler
            hbScheduler.attachClient(this)
            hbScheduler.scheduleHeartbeat()
        }
        raftNode.sendJoinNotification(randomNodeId)

        val server = vertx.createHttpServer()
        server.requestHandler { request -> request.response().end("""
            Current server port: $httpPort
            Current node id: ${raftNode.persistentState.id}
            Current node state: ${raftNode.nodeState}
            Current node term: ${raftNode.persistentState.currentTerm}
            Current voted for: ${raftNode.persistentState.votedFor}
        """.trimIndent()) }
        server.listen(httpPort.toInt())

        logger.info("Application started")

        Runtime.getRuntime().addShutdownHook(Thread { vertx.close() })
    }

    override fun stop() {
        logger.info("RaftVerticle with nodeId: ${raftNode.persistentState.id} stopped")
        raftNode.sendQuitNotification(raftNode.persistentState.id)
    }

    private val inetAddress = NetworkInterface.getNetworkInterfaces().toList()
        .filter { !it.isLoopback && it.isUp }
        .flatMap { it.inetAddresses.toList() }
        .first { it !is Inet6Address }

    private fun staticIpConfig(ipAddress: String): IgniteConfiguration {
        val spi = TcpDiscoverySpi().apply {
            this.ipFinder = TcpDiscoveryVmIpFinder().apply {
                this.setAddresses(listOf(ipAddress, "127.0.0.1"))
            }
        }
        return IgniteConfiguration().apply {
            this.discoverySpi = spi
        }
    }
}