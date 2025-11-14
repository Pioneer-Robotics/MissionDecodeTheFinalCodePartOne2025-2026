package pioneer.helpers

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class FileTransferTest {

    private val gson = Gson()

    @Test
    fun testDataStructureCreation() {
        // Create test data with bot and custom data fields
        val testData = OpModeDataTransfer.OMDT(
            bot = null, // Bot can't be easily mocked in unit tests
            data = mutableMapOf(
                "customValue" to 42,
                "motifId" to 21,
                "samplesScored" to 5
            )
        )
        
        // Verify fields
        assertNull("Bot should be null in this test", testData.bot)
        assertEquals("Custom value should match", 42, testData.data["customValue"])
        assertEquals("Motif ID should match", 21, testData.data["motifId"])
        assertEquals("Samples scored should match", 5, testData.data["samplesScored"])
    }

    @Test
    fun testJsonSerialization() {
        // Create test data without bot (can't serialize hardware)
        val testData = OpModeDataTransfer.OMDT(
            bot = null,
            data = mutableMapOf(
                "motifId" to 22,
                "scoredSamples" to 5
            )
        )
        
        // Serialize to JSON
        val json = gson.toJson(testData)
        
        // Verify JSON contains expected data
        assertTrue("JSON should contain data", json.contains("data"))
        assertTrue("JSON should contain motifId", json.contains("motifId"))
        assertTrue("JSON should contain timestamp", json.contains("timestamp"))
    }

    @Test
    fun testJsonDeserialization() {
        // Create JSON string manually
        val json = """
            {
                "bot": null,
                "timestamp": 1699800000000,
                "data": {
                    "customField": "testValue",
                    "scoreCount": 42
                }
            }
        """.trimIndent()
        
        // Deserialize
        val loaded = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify
        assertNotNull("Loaded data should not be null", loaded)
        assertEquals("Timestamp should match", 1699800000000L, loaded.timestamp)
        assertNull("Bot should be null", loaded.bot)
        assertEquals("Custom field should match", "testValue", loaded.data["customField"])
    }

    @Test
    fun testRoundTripSerialization() {
        // Create test data with various fields
        val original = OpModeDataTransfer.OMDT(
            bot = null,
            data = mutableMapOf(
                "motifId" to 23,
                "notes" to "Test auto run",
                "allianceColor" to "RED"
            )
        )
        
        // Serialize and deserialize
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify all data survived the round trip
        assertNotNull("Restored data should not be null", restored)
        assertNull("Bot should be null", restored.bot)
        assertEquals("Motif ID should match", 23.0, restored.data["motifId"])
        assertEquals("Notes should match", "Test auto run", restored.data["notes"])
        assertEquals("Alliance color should match", "RED", restored.data["allianceColor"])
    }

    @Test
    fun testMultipleDataTypes() {
        // Test various data types in the map
        val testData = OpModeDataTransfer.OMDT(
            bot = null,
            data = mutableMapOf(
                "intValue" to 42,
                "doubleValue" to 3.14159,
                "stringValue" to "hello",
                "boolValue" to true,
                "obeliskPattern" to 1
            )
        )
        
        // Serialize and deserialize
        val json = gson.toJson(testData)
        val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify all types
        assertNotNull("Restored data should not be null", restored)
        assertEquals("Int value should survive", 42.0, restored.data["intValue"])
        assertEquals("Double value should survive", 3.14159, restored.data["doubleValue"])
        assertEquals("String value should survive", "hello", restored.data["stringValue"])
        assertEquals("Bool value should survive", true, restored.data["boolValue"])
        assertEquals("Obelisk pattern should survive", 1.0, restored.data["obeliskPattern"])
    }

    @Test
    fun testTimestampGeneration() {
        val beforeTime = System.currentTimeMillis()
        val testData = OpModeDataTransfer.OMDT()
        val afterTime = System.currentTimeMillis()
        
        assertTrue("Timestamp should be after beforeTime", testData.timestamp >= beforeTime)
        assertTrue("Timestamp should be before afterTime", testData.timestamp <= afterTime)
    }

    @Test
    fun testBotFieldHandling() {
        // Test that bot field can be null and handled properly
        val testData = OpModeDataTransfer.OMDT(
            bot = null,
            data = mutableMapOf(
                "testField" to "testValue"
            )
        )
        
        // Serialize and deserialize
        val json = gson.toJson(testData)
        val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify
        assertNotNull("Restored data should not be null", restored)
        assertNull("Bot should still be null", restored.bot)
        assertEquals("Test field should survive", "testValue", restored.data["testField"])
    }

    @Test
    fun testDataMapMutability() {
        // Test that data map is mutable and can be modified
        val testData = OpModeDataTransfer.OMDT()
        
        // Add items to map
        testData.data["key1"] = "value1"
        testData.data["key2"] = 123
        testData.data["key3"] = true
        
        // Verify items were added
        assertEquals("Key1 should match", "value1", testData.data["key1"])
        assertEquals("Key2 should match", 123.0, testData.data["key2"])
        assertEquals("Key3 should match", true, testData.data["key3"])
        
        // Serialize and check
        val json = gson.toJson(testData)
        val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        assertEquals("Key1 should survive", "value1", restored.data["key1"])
        assertEquals("Key2 should survive", 123.0, restored.data["key2"])
        assertEquals("Key3 should survive", true, restored.data["key3"])
    }
}
