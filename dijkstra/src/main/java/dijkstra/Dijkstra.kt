package dijkstra

import kotlinx.atomicfu.AtomicBooleanArray
import java.util.PriorityQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

class PriorityMultiQueue<E : Any>(n: Int, comparator: Comparator<E>) {
    private val comparator = nullsLast(comparator)
    private val qs = Array(2 * n) { PriorityQueue<E>(comparator) }
    private val locks = AtomicBooleanArray(2 * n)

    fun enqueue(obj: E) {
        while (true) {
            val idx = Random.nextInt(qs.size)
            if (locks[idx].compareAndSet(expect = false, update = true)) {
                qs[idx].offer(obj)
                locks[idx].value = false
                break
            }
        }
    }

    fun dequeue(): E? {
        while (true) {
            val a = Random.nextInt(qs.size)
            val b = { val tmp = Random.nextInt(qs.size - 1); if (tmp >= a) tmp + 1 else tmp }.invoke()

            if (locks[a].compareAndSet(expect = false, update = true)) {
                if (locks[b].compareAndSet(expect = false, update = true)) {
                    try {
                        val aVal = if (qs[a].isEmpty()) null else qs[a].peek()
                        val bVal = if (qs[b].isEmpty()) null else qs[b].peek()

                        return if (comparator.compare(aVal, bVal) < 0) qs[a].poll() else qs[b].poll()
                    } finally {
                        locks[a].value = false
                        locks[b].value = false
                    }
                }
            }
        }
    }
}

fun updateDistanceIfLower(node: Node, newDistance: Int): Boolean {
    while (true) {
        val curDistance = node.distance
        if (curDistance <= newDistance) {
            return false
        }
        if (node.casDistance(curDistance, newDistance)) {
            return true
        }
    }
}

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    val q = PriorityMultiQueue(workers, NODE_DISTANCE_COMPARATOR)

    start.distance = 0
    q.enqueue(start)

    val activeNodes = AtomicInteger(1)
    val threadsCountDown = CountDownLatch(workers)

    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                val cur: Node = synchronized(q) { q.dequeue() } ?: continue

                val curDistance = cur.distance
                for (e in cur.outgoingEdges) {
                    val newDistance = curDistance + e.weight
                    if (updateDistanceIfLower(e.to, newDistance)) {
                        synchronized(q) { q.enqueue(e.to) }
                        activeNodes.incrementAndGet()
                    }
                }
                activeNodes.decrementAndGet()
            }
            threadsCountDown.countDown()
        }
    }
    threadsCountDown.await()
}