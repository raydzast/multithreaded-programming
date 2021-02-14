import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

/**
 * Bank implementation.
 *
 * @author Tebloev Stanislav
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    override fun getAmount(index: Int): Long {
        val account = accounts[index]

        account.lock.withLock {
            return account.amount
        }
    }

    override val totalAmount: Long
        get() {
            try {
                for (index in 0 until numberOfAccounts) {
                    accounts[index].lock.lock()
                }

                return accounts.sumOf { account -> account.amount }
            } finally {
                for (index in (0 until numberOfAccounts).reversed()) {
                    accounts[index].lock.unlock()
                }
            }
        }

    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]

        account.lock.withLock {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]

        account.lock.withLock {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        }
    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]

        val first = accounts[min(fromIndex, toIndex)]
        val second = accounts[max(fromIndex, toIndex)]

        first.lock.withLock {
            second.lock.withLock {
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        val lock: ReentrantLock = ReentrantLock()
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
    }
}