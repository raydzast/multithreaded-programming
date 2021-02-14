import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private object RETRY

    inner class Node(val element: E?, val cont: Continuation<Any?>?) {
        val next = atomic<Node?>(null)
    }

    private val head: AtomicRef<Node>
    private val tail: AtomicRef<Node>

    init {
        val dummy = Node(null, null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    private suspend fun enqueueAndSuspend(tail: Node, element: E?): Any? {
        return suspendCoroutine { cont ->
            val newTail = Node(element, cont)
            val retry = !tail.next.compareAndSet(null, newTail)
            this.tail.compareAndSet(tail, tail.next.value!!)

            if (retry) {
                cont.resume(RETRY)
            }
        }
    }

    private fun dequeueAndResume(head: Node, element: E?): Any? {
        val newHead = head.next.value!!
        return if (this.head.compareAndSet(head, newHead)) {
            newHead.cont!!.resume(element)
            newHead.element
        } else {
            RETRY
        }
    }

    override suspend fun send(element: E) {
        while (true) {
            val head = head.value
            val tail = tail.value

            val res = if (head == tail || tail.element != null) {
                enqueueAndSuspend(tail, element)
            } else {
                dequeueAndResume(head, element)
            }

            if (res != RETRY) {
                break
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val head = head.value
            val tail = tail.value

            val res = if (head == tail || tail.element == null) {
                enqueueAndSuspend(tail, null)
            } else {
                dequeueAndResume(head, null)
            }

            if (res != RETRY) {
                return res as E
            }
        }
    }
}
