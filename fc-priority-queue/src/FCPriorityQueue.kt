import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.*
import kotlin.random.Random

class FCPriorityQueue<E : Comparable<E>> {
    private class Add<E>(val element: E)
    private class Poll
    private class Peek
    private class Result(val result: Any?)

    private val q = PriorityQueue<E>()
    private val fcArray = atomicArrayOfNulls<Any>(FC_ARRAY_SIZE)
    private val fcLock = atomic(false)

    private fun combine() {
        for (idx in 0 until FC_ARRAY_SIZE) {
            when (val status = fcArray[idx].value) {
                is Add<*> -> {
                    q.add(status.element as E)
                    fcArray[idx].value = Result(Unit)
                }
                is Poll -> {
                    fcArray[idx].value = Result(q.poll())
                }
                is Peek -> {
                    fcArray[idx].value = Result(q.peek())
                }
            }
        }
    }

    private fun <T> common(executable: () -> T, operation: Any): T {
        if (fcLock.compareAndSet(expect = false, update = true)) {
            val result = executable()
            combine()
            return result.also { fcLock.compareAndSet(expect = true, update = false) }
        }

        var idx = Random.nextInt(FC_ARRAY_SIZE)
        while (!fcArray[idx].compareAndSet(null, operation)) {
            idx = (idx + 1) % FC_ARRAY_SIZE
        }

        while (true) {
            val status = fcArray[idx].value
            if (status is Result) {
                val result = status.result
                fcArray[idx].compareAndSet(status, null)
                return result as T
            }

            if (fcLock.compareAndSet(expect = false, update = true)) {
                val status = fcArray[idx].value
                val result = if (status is Result) status.result as T else executable()
                fcArray[idx].value = null
                combine()
                return result.also { fcLock.compareAndSet(expect = true, update = false) }
            }
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return common<E?>({ q.poll() }, Poll())
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return common<E?>({ q.peek() }, Peek())
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        return common(fun() { q.add(element) }, Add(element))
    }
}

const val FC_ARRAY_SIZE = 16