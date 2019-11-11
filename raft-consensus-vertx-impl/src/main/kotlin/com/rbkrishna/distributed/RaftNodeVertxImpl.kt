package com.rbkrishna.distributed

import com.rbkrishna.distributed.raft.api.*
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import com.rbkrishna.distributed.raft.api.state.VolatileStateOnLeader
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

class RaftNodeVertxImpl(
    persistentState: PersistentState<String>,
    nodeState: NodeState,
    volatileState: VolatileState,
    volatileStateOnLeader: VolatileStateOnLeader = VolatileStateOnLeader(),
    val vertx: Vertx
) : BaseNode<String>(persistentState, nodeState, volatileState, volatileStateOnLeader) {

    val json = Json(JsonConfiguration.Stable)

    private val logger = LoggerFactory.getLogger(RaftNodeVertxImpl::class.java)

    init {
        val requestVotesConsumer =
            vertx.eventBus().consumer<String>("${RaftAddress.REQUEST_VOTES.name}.${persistentState.id}")
        requestVotesConsumer.handler { message ->
            val requestVotesArgs = json.parse(RequestVotesArgs.serializer(), message.body())
            val requestVotesRes = this.handleRequestVotes(requestVotesArgs)
            message.reply(json.stringify(RequestVotesRes.serializer(), requestVotesRes))
        }

        val appendEntriesConsumer = vertx.eventBus()
            .consumer<String>("${RaftAddress.APPEND_ENTRIES.name}.${persistentState.id}")
        appendEntriesConsumer.handler { message ->
            val appendEntriesArg = json.parse(AppendEntriesArgs.serializer(String.serializer()), message.body())
            val appendEntriesRes = this.handleAppendEntries(appendEntriesArg)
            message.reply(json.stringify(AppendEntriesRes.serializer(), appendEntriesRes))
        }

        val joinNotificationConsumer =
            vertx.eventBus().consumer<Int>(RaftAddress.JOIN_NOTIFICATION.name)
        joinNotificationConsumer.handler { message ->
            val sourceNodeId = message.body()
            logger.info("Node ${persistentState.id} Received join notification from $sourceNodeId")
            nodeIds.add(sourceNodeId)
            val nodeIdsAsString = nodeIds.joinToString(",")
            message.reply(nodeIdsAsString)
        }

        val quitNotificationConsumer =
            vertx.eventBus().consumer<Int>(RaftAddress.QUIT_NOTIFICATION.name)
        quitNotificationConsumer.handler { message ->
            val sourceNodeId = message.body()
            logger.info("Node ${persistentState.id} Received quit notification from $sourceNodeId")
            nodeIds.remove(sourceNodeId)
        }
    }

    override fun acquireLeaderLock() : Boolean {
        val sharedData = vertx.sharedData()
        val latch = CountDownLatch(1)
        var res = false
        sharedData.getLockWithTimeout("leader_lock", 2000) { result ->
            res = result.succeeded()
            latch.countDown()
        }
        latch.await()
        return res
    }

    override fun sendHandleRequestVotes(nodeId: Int, requestVotesArgs: RequestVotesArgs): RequestVotesRes {
        return runBlocking {
            sendHandleRequestVotesAsync(vertx, nodeId, requestVotesArgs)
        }
    }

    override fun sendHandleAppendEntries(nodeId: Int, appendEntriesArgs: AppendEntriesArgs<String>): AppendEntriesRes {
        return runBlocking {
            sendHandleAppendEntriesAsync(vertx, nodeId, appendEntriesArgs)
        }
    }

    override fun sendJoinNotification(sourceNodeId: Int) {
        return runBlocking {
            sendJoinNotificationAsync(vertx, sourceNodeId)
        }
    }

    override fun sendQuitNotification(sourceNodeId: Int) {
        return runBlocking {
            sendQuitNotificationAsync(vertx, sourceNodeId)
        }
    }

    private suspend fun sendHandleRequestVotesAsync(
        vertx: Vertx,
        nodeId: Int,
        requestVotesArgs: RequestVotesArgs
    ): RequestVotesRes {
        val message = withTimeoutOrNull(2000) {
            awaitResult<Message<String>> {
                vertx.eventBus().send(
                    "${RaftAddress.REQUEST_VOTES.name}.$nodeId",
                    json.stringify(RequestVotesArgs.serializer(), requestVotesArgs)
                )
            }
        }
        return if (message != null) {
            json.parse(RequestVotesRes.serializer(), message.body())
        } else {
            RequestVotesRes(requestVotesArgs.termNumber, true)
        }
    }

    private suspend fun sendHandleAppendEntriesAsync(
        vertx: Vertx,
        nodeId: Int,
        appendEntriesArgs: AppendEntriesArgs<String>
    ): AppendEntriesRes {
        val message = withTimeoutOrNull(2000) {
            awaitResult<Message<String>> {
                vertx.eventBus().send(
                    "${RaftAddress.APPEND_ENTRIES.name}.$nodeId",
                    json.stringify(AppendEntriesArgs.serializer(String.serializer()), appendEntriesArgs)
                )
            }
        }
        return if (message != null) {
            json.parse(AppendEntriesRes.serializer(), message.body())
        } else {
            AppendEntriesRes(appendEntriesArgs.termNumber, false)
        }
    }

    private suspend fun sendJoinNotificationAsync(vertx: Vertx, sourceNodeId: Int) {
        val message = withTimeoutOrNull(2000) {
            awaitResult<Message<String>> {
                vertx.eventBus().publish(RaftAddress.JOIN_NOTIFICATION.name, sourceNodeId)
            }
        }
        if (message != null) {
            val nodeIds = message.body().split(",").map { it.toInt() }
            this.nodeIds.addAll(nodeIds)
        }
    }

    private fun sendQuitNotificationAsync(vertx: Vertx, sourceNodeId: Int) {
        vertx.eventBus().publish(RaftAddress.QUIT_NOTIFICATION.name, sourceNodeId)
    }
}