package pioneer.helpers

import kotlin.enums.enumEntries

/**
 * Returns the next enum entry in a cyclic manner.
 */
inline fun <reified T : Enum<T>> T.next(): T {
    val entries = enumEntries<T>()
    val nextOrdinal = (this.ordinal + 1) % entries.size
    return entries[nextOrdinal]
}
