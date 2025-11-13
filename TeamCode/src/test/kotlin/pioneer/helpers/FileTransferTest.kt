package pioneer.helpers

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*
import pioneer.general.AllianceColor

class FileTransferTest {

    private val gson = Gson()

    @Test
    fun testDataStructureCreation() {
        // Create test data with all fields
        val testData = OpModeDataTransfer.OMDT().apply {
            alliance = AllianceColor.RED
            pose = Pose(10.5, 20.3, theta = 45.0)
            data["motifId"] = 21 // Store motif ID in data map
            data["customValue"] = 42
        }
        
        // Verify fields
        assertEquals("Alliance should be RED", AllianceColor.RED, testData.alliance)
        assertNotNull("Pose should not be null", testData.pose)
        assertEquals("Pose x should match", 10.5, testData.pose?.x ?: 0.0, 0.001)
        assertEquals("Pose y should match", 20.3, testData.pose?.y ?: 0.0, 0.001)
        assertEquals("Pose theta should match", 45.0, testData.pose?.theta ?: 0.0, 0.001)
        assertEquals("Motif ID should match", 21, testData.data["motifId"])
        assertEquals("Custom value should match", 42, testData.data["customValue"])
    }

    @Test
    fun testJsonSerialization() {
        // Create test data
        val testData = OpModeDataTransfer.OMDT().apply {
            alliance = AllianceColor.BLUE
            pose = Pose(10.0, 15.0, theta = 180.0)
            data["motifId"] = 22 // Store motif ID in data map
            data["scoredSamples"] = 5
        }
        
        // Serialize to JSON
        val json = gson.toJson(testData)
        
        // Verify JSON contains expected data
        assertTrue("JSON should contain alliance", json.contains("alliance"))
        assertTrue("JSON should contain BLUE", json.contains("BLUE"))
        assertTrue("JSON should contain pose", json.contains("pose"))
        assertTrue("JSON should contain motifId", json.contains("motifId"))
    }

    @Test
    fun testJsonDeserialization() {
        // Create JSON string manually
        val json = """
            {
                "timestamp": 1699800000000,
                "data": {
                    "alliance": "RED",
                    "pose": {
                        "x": 12.5,
                        "y": 8.3,
                        "heading": 180.0
                    },
                    "obeliskPattern": 2
                }
            }
        """.trimIndent()
        
        // Deserialize
        val loaded = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify
        assertNotNull("Loaded data should not be null", loaded)
        assertEquals("Timestamp should match", 1699800000000L, loaded.timestamp)
    }

    @Test
    fun testRoundTripSerialization() {
        // Create test data with pose and motif ID in data map
        val motifId = 23
        val original = OpModeDataTransfer.OMDT().apply {
            alliance = AllianceColor.RED
            pose = Pose(25.0, 30.0, theta = 270.0)
            data["motifId"] = motifId // Store motif ID in data map
            data["notes"] = "Test auto run"
        }
        
        // Serialize and deserialize
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
        
        // Verify all data survived the round trip
        assertNotNull("Restored data should not be null", restored)
        assertEquals("Alliance should match", AllianceColor.RED, restored.alliance)
        assertNotNull("Pose should not be null", restored.pose)
        assertEquals("Pose x should match", 25.0, restored.pose?.x ?: 0.0, 0.001)
        assertEquals("Pose y should match", 30.0, restored.pose?.y ?: 0.0, 0.001)
        assertEquals("Pose theta should match", 270.0, restored.pose?.theta ?: 0.0, 0.001)
        // Reconstruct Motif from stored ID: val restoredMotif = (restored.data["motifId"] as? Number)?.toInt()?.let { Motif(it) }
        assertEquals("Motif ID should match", motifId.toDouble(), restored.data["motifId"])
        assertEquals("Notes should match", "Test auto run", restored.data["notes"])
    }

    @Test
    fun testMultipleDataTypes() {
        // Test various data types in the map
        val testData = OpModeDataTransfer.OMDT().apply {
            alliance = AllianceColor.BLUE
            pose = Pose(0.0, 0.0, theta = 0.0)
            data["intValue"] = 42
            data["doubleValue"] = 3.14159
            data["stringValue"] = "hello"
            data["boolValue"] = true
            data["obeliskPattern"] = 1
        }
        
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
    fun testPoseWithAllScenarios() {
        // Test different pose scenarios
        val scenarios = listOf(
            Pose(0.0, 0.0, theta = 0.0),              // Origin
            Pose(-10.0, -10.0, theta = -90.0),        // Negative values
            Pose(100.5, 200.75, theta = 359.9),       // Large values
            Pose(1.234567, 2.345678, theta = 3.456)   // High precision
        )
        
        scenarios.forEach { testPose ->
            val testData = OpModeDataTransfer.OMDT().apply {
                pose = testPose
            }
            
            val json = gson.toJson(testData)
            val restored = gson.fromJson(json, OpModeDataTransfer.OMDT::class.java)
            
            // Verify pose survived serialization
            assertNotNull("Restored pose should not be null", restored.pose)
            assertEquals("Pose x should match", testPose.x, restored.pose?.x ?: 0.0, 0.001)
            assertEquals("Pose y should match", testPose.y, restored.pose?.y ?: 0.0, 0.001)
            assertEquals("Pose theta should match", testPose.theta, restored.pose?.theta ?: 0.0, 0.001)
        }
    }
}
