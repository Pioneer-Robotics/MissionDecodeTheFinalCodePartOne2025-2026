package pioneer.helpers

interface Indexer {
    var bin: List<Any>
    var index: Int

    fun next()
    fun previous()
    fun peek(): Any?
    fun isEmpty(): Boolean
    fun size(): Int
    fun pop(): Any?
    fun push(item: Any): Boolean
    fun toList(): List<Any>
}

/**
 * SpinDexer - A circular indexer implementation that wraps around when advancing or retreating
 * @param maxSize Optional maximum size limit for the bin. If null, no limit is enforced.
 */
class SpinDexer(private val maxSize: Int? = null, private val initialItems: List<Any> = emptyList()) : Indexer {
    override var bin: List<Any> = initialItems
    override var index: Int = 0

    /**
     * Calculate the next index in a circular manner
     */
    private fun nextIndex(): Int {
        return if (bin.isEmpty()) 0 else (index + 1) % bin.size
    }

    /**
     * Calculate the previous index in a circular manner
     */
    private fun previousIndex(): Int {
        return if (bin.isEmpty()) 0 else if (index - 1 < 0) bin.size - 1 else index - 1
    }

    /**
     * Calculate a circular index offset from the current position
     */
    private fun circularIndex(offset: Int): Int {
        return if (bin.isEmpty()) 0 else (index + offset) % bin.size
    }

    /**
     * Normalize the current index after bin modification
     */
    private fun normalizeIndex() {
        if (bin.isEmpty()) {
            index = 0
        } else if (index >= bin.size) {
            index = index % bin.size
        }
    }

    /**
     * Check if there's capacity to add more items
     */
    private fun hasCapacity(): Boolean {
        return maxSize == null || bin.size < maxSize
    }

    /**
     * Move to the next position in a circular manner
     */
    override fun next() {
        if (bin.isNotEmpty()) {
            index = nextIndex()
        }
    }

    /**
     * Move to the previous position in a circular manner
     */
    override fun previous() {
        if (bin.isNotEmpty()) {
            index = previousIndex()
        }
    }

    /**
     * Peek at the current item without removing it
     */
    override fun peek(): Any? {
        return bin.getOrNull(index)
    }

    /**
     * Check if the bin is empty
     */
    override fun isEmpty(): Boolean {
        return bin.isEmpty()
    }

    /**
     * Get the number of items in the bin
     */
    override fun size(): Int {
        return bin.size
    }

    /**
     * Check if the bin is full (only applicable if maxSize is set)
     */
    fun isFull(): Boolean {
        return !hasCapacity()
    }

    /**
     * Remove and return the current item
     */
    override fun pop(): Any? {
        val item = peek()
        if (item != null) {
            bin = bin.toMutableList().also { it.removeAt(index) }
            normalizeIndex()
        }
        return item
    }

    /**
     * Push an item to the bin
     * @param item The item to add to the bin
     * @return true if the item was added successfully, false if the bin is full
     */
    override fun push(item: Any): Boolean {
        return if (hasCapacity()) {
            bin = bin + item
            true
        } else {
            false
        }
    }

    /**
     * Get the items as a list in order starting from the current position
     */
    override fun toList(): List<Any> {
        if (bin.isEmpty()) return emptyList()
        
        return List(bin.size) { i ->
            bin[circularIndex(i)]
        }
    }

    /**
     * String representation showing each item with (curr), (next), or (prev) markers
     */
    override fun toString(): String {
        if (bin.isEmpty()) return "SpinDexer[]"
        
        val prevIdx = previousIndex()
        val nextIdx = nextIndex()
        
        val items = bin.mapIndexed { idx, item ->
            val marker = when (idx) {
                index -> "(curr)"
                nextIdx -> "(next)"
                prevIdx -> "(prev)"
                else -> ""
            }
            "$item$marker"
        }
        return "SpinDexer[${items.joinToString(", ")}]"
    }
}
