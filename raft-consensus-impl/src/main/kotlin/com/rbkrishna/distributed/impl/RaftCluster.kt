package com.rbkrishna.distributed.impl

import com.rbkrishna.distributed.raft.api.Node

class RaftCluster(val nodeMap: MutableMap<Int, Node<String>>) {

}