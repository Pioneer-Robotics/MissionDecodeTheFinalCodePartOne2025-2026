package pioneer.helpers

import com.google.gson.Gson
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File

import pioneer.general.AllianceColor
import pioneer.decode.Motif

object OpModeDataTransfer {
    data class OMDT(
        val timestamp: Long = System.currentTimeMillis(),
        val data: MutableMap<String, Any?> = mutableMapOf()
    ) {
        // Easy to add more: just add a new property accessor
        // Example: var newField: String?
        //     get() = data["newField"] as? String
        //     set(value) { data["newField"] = value }

        // Type-safe accessors for common fields
        var alliance: AllianceColor?
            get() = data["alliance"] as? AllianceColor
            set(value) { data["alliance"] = value }
        
        var pose: Pose?
            get() = data["pose"] as? Pose
            set(value) { data["pose"] = value }

        
    }

    private val gson = Gson()
    private val file: File by lazy {
        AppUtil.getInstance().getSettingsFile("omdt.json").apply {
            parentFile?.mkdirs()
        }
    }

    fun save(value: OMDT) {
        file.writeText(gson.toJson(value))
    }

    fun loadOrNull(): OMDT? = try {
        if (file.exists()) gson.fromJson(file.readText(), OMDT::class.java)
        else null
    } catch (e: Exception) {
        null
    }

    // Deletes the OMDT file. Safe to call even if the file doesn't exist.
    fun clear() = file.delete()
}
