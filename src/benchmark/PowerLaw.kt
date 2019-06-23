package benchmark

/*
 * @(#)PowerLaw.java    ver 1.2  6/20/2005
 *
 * Modified by Weishuai Yang (wyang@cs.binghamton.edu).
 *
 * this file is based on T J Finney's Manuscripts Simulation Tool, 2001
 */

import java.util.Random

/**
 * provides power law selection. Modified by Weishuai Yang this file is based on
 * T J Finney's Manuscripts Simulation Tool, 2001
 */
class PowerLaw {

    private var rand: Random? = null

    /**
     * constructs a power law object using an external random generator
     *
     * @param r
     * random generator passed in
     */
    constructor(r: Random) {
        rand = r
    }

    /**
     * constructs a power law object using an internal random generator
     */
    constructor() {
        rand = Random()
    }

    /**
     * get uniformly distributed double in [0, 1]
     */
    fun getRand(): Double {
        return rand!!.nextDouble()
    }

    /**
     * get uniformly distributed integer in [0, N - 1]
     */
    fun getRandInt(N: Int): Int {
        return rand!!.nextInt(N)
    }

    /**
     * selects item using power law probability of selecting array item: p(ni) =
     * k * (ni^p) k is a normalisation constant p(ni) = 0 if ni is zero, even
     * when p < 0
     *
     *
     * @param nums
     * array of numbers ni
     * @param p
     * exponent p
     * @return index in [0, array size - 1]
     */

    fun select(nums: DoubleArray, p: Double): Int {
        // make array of probabilities
        val probs = DoubleArray(nums.size)
        for (i in probs.indices) {
            if (nums[i] == 0.0)
                probs[i] = 0.0
            else
                probs[i] = Math.pow(nums[i], p)
        }

        // sum probabilities
        var sum = 0.0
        for (i in probs.indices) {
            sum += probs[i]
        }

        // obtain random number in range [0, sum]
        var r = sum * getRand()

        // subtract probs until result negative
        // no of iterations gives required index
        var i = 0
        while (i < probs.size) {
            r -= probs[i]
            if (r < 0) {
                break
            }
            i++
        }
        return i
    }

    /**
     * select item using Zipf's law
     *
     * @param size
     * of ranked array
     * @return index in [0, array size - 1]
     */
    fun zipf(size: Int): Int {
        // make array of numbers
        val nums = DoubleArray(size)
        for (i in nums.indices) {
            nums[i] = (i + 1).toDouble()
        }
        // get index using special case of power law
        return select(nums, -1.0)
    }

    companion object {

        /**
         * test purpose main
         *
         * @param args
         * command line inputs
         */
        @JvmStatic
        fun main(args: Array<String>) {
            //
            val p = PowerLaw(Random(555))

            /*
         * double[] numbers = {0, 1, 2, 3}; for (int i = 0; i < 5; i++) {
         * System.out.println("Select: " + p.select(numbers, -1)); }
         */
            for (i in 0..49) {
                println("Zipf: " + p.zipf(20))
            }
            val test = (0..1000).map { p.zipf(10) }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedBy { it.first }
                .let(::println)

        }
    }
}