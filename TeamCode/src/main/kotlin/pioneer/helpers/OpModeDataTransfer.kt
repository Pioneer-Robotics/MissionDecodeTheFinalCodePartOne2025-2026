package pioneer.helpers

import com.google.gson.Gson
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File
import pioneer.Bot


object OpModeDataTransfer {
    data class OMDT(
        val bot: Bot? = null,
        var timestamp: Long = System.currentTimeMillis(),
        val data: MutableMap<String, Any?> = mutableMapOf()
    )

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
