import java.util.concurrent.atomic.*

class Solution(private val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node>(null)

    override fun lock(): Node {
        val newNode = Node(true)
        val prev = tail.getAndSet(newNode)

        if (prev != null) {
            prev.next.set(newNode)
            while (newNode.locked.get()) {
                env.park()
            }
        }

        return newNode
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.get() == null) {
                    /* no-op */
                }
            }
        }
        node.next.get().locked.set(false)
        env.unpark(node.next.get().thread)
    }

    class Node(locked: Boolean) {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val next = AtomicReference<Node>(null)
        val locked = AtomicReference<Boolean>(locked)
    }
}
