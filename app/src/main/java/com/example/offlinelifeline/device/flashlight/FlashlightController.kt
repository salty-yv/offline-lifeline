package com.example.offlinelifeline.device.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.offlinelifeline.core.common.AppDispatchers
import kotlinx.coroutines.withContext

class FlashlightController(
    context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    suspend fun isAvailable(): Boolean = withContext(dispatchers.io) {
        findTorchCameraId() != null
    }

    suspend fun setTorchEnabled(enabled: Boolean): Result<Unit> {
        return withContext(dispatchers.io) {
            runCatching {
                val cameraId = findTorchCameraId() ?: error("当前设备没有可用闪光灯")
                cameraManager.setTorchMode(cameraId, enabled)
            }
        }
    }

    private fun findTorchCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
}
