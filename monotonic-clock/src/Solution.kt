/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Tebloev Stanislav
 */
class Solution : MonotonicClock {
    private var c1x by RegularInt(0)
    private var c1y by RegularInt(0)
    private var c1z by RegularInt(0)
    private var c2x by RegularInt(0)
    private var c2y by RegularInt(0)
    private var c2z by RegularInt(0)

    override fun write(time: Time) {
        c2x = time.d1
        c2y = time.d2
        c2z = time.d3

        c1z = time.d3
        c1y = time.d2
        c1x = time.d1
    }

    override fun read(): Time {
        val r1x = c1x
        val r1y = c1y
        val r1z = c1z

        val r2z = c2z
        val r2y = c2y
        val r2x = c2x

        val r1 = arrayOf(r1x, r1y, r1z)
        val r2 = arrayOf(r2x, r2y, r2z)

        val result = Array(3) { 0 }

        for (index in 0 until 3) {
            result[index] = r2[index]
            if (r1[index] != r2[index]) {
                break
            }
        }

        return Time(result[0], result[1], result[2])
    }
}