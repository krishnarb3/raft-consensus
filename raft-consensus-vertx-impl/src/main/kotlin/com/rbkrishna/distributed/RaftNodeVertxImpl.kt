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
import kotlinx.serialization.*
import kotlinx.serialization.json.*


class RaftNodeVertxImpl(
    persistentState: PersistentState<String>,
    nodeState: NodeState,
    volatileState: VolatileState,
    volatileStateOnLeader: VolatileStateOnLeader = VolatileStateOnLeader(),
    val vertx: Vertx
) : BaseNode<String>(persistentState, nodeState, volatileState, volatileStateOnLeader) {

    val json = Json(JsonConfiguration.Stable)

    init {
        val requestVotesConsumer =
            vertx.eventBus().localConsumer<String>("${RaftAddress.REQUEST_VOTES.name}.${persistentState.id}")
        requestVotesConsumer.handler { message ->
            val requestVotesArgs = json.parse(RequestVotesArgs.serializer(), message.body())
            val requestVotesRes = this.handleRequestVotes(requestVotesArgs)
            message.reply(json.stringify(RequestVotesRes.serializer(), requestVotesRes))
        }

        val appendEntriesConsumer = vertx.eventBus()
            .localConsumer<String>("${RaftAddress.APPEND_ENTRIES.name}.${persistentState.id}")
        appendEntriesConsumer.handler { message ->
            val appendEntriesArg = json.parse(AppendEntriesArgs.serializer(String.serializer()), message.body())
            val appendEntriesRes = this.handleAppendEntries(appendEntriesArg)
            message.reply(json.stringify(AppendEntriesRes.serializer(), appendEntriesRes))
        }

        val joinNotificationConsumer =
            vertx.eventBus().localConsumer<Int>(RaftAddress.JOIN_NOTIFICATION.name)
        joinNotificationConsumer.handler { message ->
            val sourceNodeId = message.body()
            nodeIds.add(sourceNodeId)
        }
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

    private suspend fun sendHandleRequestVotesAsync(
        vertx: Vertx,
        nodeId: Int,
        requestVotesArgs: RequestVotesArgs
    ): RequestVotesRes {
        val message = awaitResult<Message<String>> {
            vertx.eventBus().send("${RaftAddress.REQUEST_VOTES.name}.$nodeId", requestVotesArgs)
        }
        val requestVotesRes = json.parse(RequestVotesRes.serializer(), message.body())
        return requestVotesRes
    }

    private suspend fun sendHandleAppendEntriesAsync(
        vertx: Vertx,
        nodeId: Int,
        appendEntriesArgs: AppendEntriesArgs<String>
    ): AppendEntriesRes {
        val message = awaitResult<Message<String>> {
            vertx.eventBus().send("${RaftAddress.APPEND_ENTRIES.name}.$nodeId", appendEntriesArgs)
        }
        val appendEntriesRes = json.parse(AppendEntriesRes.serializer(), message.body())
        return appendEntriesRes
    }

    private suspend fun sendJoinNotificationAsync(vertx: Vertx, sourceNodeId: Int) {
        vertx.eventBus().send(RaftAddress.JOIN_NOTIFICATION.name, sourceNodeId)
    }
}