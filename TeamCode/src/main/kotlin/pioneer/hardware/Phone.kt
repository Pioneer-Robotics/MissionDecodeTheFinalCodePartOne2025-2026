package pioneer.hardware

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Toast
import com.qualcomm.robotcore.hardware.HardwareMap

class Phone(
    private val hardwareMap: HardwareMap,
    defaultColor: Int = Color.WHITE,
) {
    private val relativeLayout: View by lazy {
        val relativeLayoutId =
            hardwareMap.appContext.resources.getIdentifier(
                "RelativeLayout",
                "id",
                hardwareMap.appContext.packageName,
            )
        (hardwareMap.appContext as Activity).findViewById<View?>(relativeLayoutId)?.apply {
            post { setBackgroundColor(defaultColor) }
        } ?: throw IllegalStateException("RelativeLayout not found")
    }

    var color: Int
        get() = (relativeLayout.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
        set(value) {
            relativeLayout.post { relativeLayout.setBackgroundColor(value) }
        }

    fun showToast(message: String) {
        Toast.makeText(hardwareMap.appContext, message, Toast.LENGTH_SHORT).show()
    }

    fun showAlert(
        title: String,
        message: String,
    ) {
        val context = hardwareMap.appContext as Activity
        context.runOnUiThread {
            android.app.AlertDialog
                .Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    fun getResources() = hardwareMap.appContext.resources
}
