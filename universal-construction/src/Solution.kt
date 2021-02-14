/**
 * @author Tebloev Stanislav
 */
class Solution : AtomicCounter {
    private val root = Node(0)
    private val last = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        while (true) {
            val node = last.get()
            val oldValue = node.value
            val newValue = oldValue + x
            val newNode = Node(newValue)

            val actualNext = node.next.decide(newNode)
            last.set(actualNext)

            if (actualNext == newNode) {
                return oldValue
            }
        }
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val value: Int) {
        val next = Consensus<Node>()
    }
}