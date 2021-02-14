import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicReferenceArray

private class Descriptor<E>(
    val size: Int = 0,
    val capacity: Int,
    val leftToMove: Int = 0,
    val operation: AtomicMarkableReference<(() -> Unit)?> = AtomicMarkableReference(null, false),
    val memory: AtomicReferenceArray<E?> = AtomicReferenceArray(capacity),
    val oldMemory: AtomicReferenceArray<E?> = AtomicReferenceArray(0),
) {
    private val moved: Int
        get() = capacity / 2 - leftToMove

    fun get(index: Int): E {
        return memory.get(index) ?: oldMemory.get(index)!!
    }

    fun completeOperation() {
        val op = operation.reference ?: return

        try {
            op.invoke()
        } finally {
            operation.set(null, false)
        }
    }

    fun move(): Descriptor<E> {
        if (leftToMove == 0) {
            return this
        }

        memory.compareAndSet(moved, null, oldMemory.get(moved))
        return copy(this, leftToMove = leftToMove - 1)
    }

    companion object {
        fun <E> copy(
            other: Descriptor<E>,
            size: Int = other.size,
            capacity: Int = other.capacity,
            leftToMove: Int = other.leftToMove,
            operation: AtomicMarkableReference<(() -> Unit)?> = other.operation,
            memory: AtomicReferenceArray<E?> = other.memory,
            oldMemory: AtomicReferenceArray<E?> = other.oldMemory,
        ): Descriptor<E> {
            return Descriptor(size, capacity, leftToMove, operation, memory, oldMemory)
        }

        fun <E> createForPush(oldDescriptor: Descriptor<E>, element: E): Descriptor<E> {
            val memory = if (oldDescriptor.capacity == oldDescriptor.size) {
                AtomicReferenceArray(oldDescriptor.capacity * 2)
            } else {
                oldDescriptor.memory
            }
            val operation: AtomicMarkableReference<(() -> Unit)?> = AtomicMarkableReference({
                memory.compareAndSet(oldDescriptor.size, null, element)
            }, true)

            return if (oldDescriptor.capacity == oldDescriptor.size) {
                Descriptor(
                    size = oldDescriptor.size + 1,
                    capacity = oldDescriptor.capacity * 2,
                    leftToMove = oldDescriptor.capacity,
                    operation = operation,
                    memory = memory,
                    oldMemory = oldDescriptor.memory,
                )
            } else {
                copy(
                    oldDescriptor,
                    size = oldDescriptor.size + 1,
                    operation = operation
                )
            }
        }
    }
}


class DynamicArrayImpl<E> : DynamicArray<E> {
    private val descriptor = atomic(Descriptor<E>(capacity = 1))

    override fun get(index: Int): E {
        val descriptor = descriptor.value
        descriptor.completeOperation()

        if (index >= descriptor.size) {
            throw IllegalArgumentException()
        }

        return descriptor.get(index)
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val oldDescriptor = descriptor.value
            oldDescriptor.completeOperation()

            if (index >= oldDescriptor.size) {
                throw IllegalArgumentException()
            }

            val newDescriptor = Descriptor.copy(oldDescriptor.move(), operation = AtomicMarkableReference({
                oldDescriptor.memory.set(index, element)
            }, false))
            if (descriptor.compareAndSet(oldDescriptor, newDescriptor)) {
                newDescriptor.completeOperation()
                break
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val oldDescriptor = descriptor.value
            oldDescriptor.completeOperation()

            val newDescriptor = Descriptor.createForPush(oldDescriptor.move(), element)
            if (descriptor.compareAndSet(oldDescriptor, newDescriptor)) {
                newDescriptor.completeOperation()
                break
            }
        }
    }

    override val size: Int
        get() {
            val descriptor = descriptor.value
            val size = descriptor.size

            return if (descriptor.operation.isMarked) size - 1 else size
        }
}