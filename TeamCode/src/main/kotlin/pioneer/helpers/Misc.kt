package pioneer.helpers

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> T.next(): T {
    val entries = enumEntries<T>()
    val nextOrdinal = (this.ordinal + 1) % entries.size
    return entries[nextOrdinal]
}
