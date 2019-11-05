package com.rbkrishna.distributed

import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.ignite.IgniteClusterManager
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.NetworkInterface

class RaftVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(RaftVerticle::class.java)

    private val MAX_NUMBER_OF_NODES = 100

    override fun start() {
        val clusterManager = IgniteClusterManager()

        val randomNodeId = (0..MAX_NUMBER_OF_NODES).random()

        val options = VertxOptions().setClusterManager(clusterManager)
        options.eventBusOptions.clusterPublicPort = 20000
        options.eventBusOptions.host = inetAddress.hostAddress
        Vertx.clusteredVertx(options) { result ->
            if (result.succeeded()) {
                val vertx = result.result()
                val raftNode = RaftNodeVertxImpl(
                    PersistentState(randomNodeId),
                    NodeState.FOLLOWER,
                    VolatileState(),
                    vertx = vertx
                )
                logger.info("Application started")
            } else {
                logger.error("Application failed to start on local node")
            }
        }
    }

    private val inetAddress = NetworkInterface.getNetworkInterfaces().toList()
        .filter { !it.isLoopback && it.isUp }
        .flatMap { it.inetAddresses.toList() }
        .first { it !is Inet6Address }
}