package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.kieronquinn.app.pixellaunchermods.service.IPixelLauncherModsRootService
import com.kieronquinn.app.pixellaunchermods.service.PixelLauncherModsRootService
import com.kieronquinn.app.pixellaunchermods.utils.extensions.suspendCoroutineWithTimeout
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.reflect.KMutableProperty0

interface RootServiceRepository {

    suspend fun isRooted(): Boolean
    suspend fun <T> runWithRootService(block: suspend (IPixelLauncherModsRootService) -> T): T?
    fun <T> runWithRootServiceIfAvailable(block: (IPixelLauncherModsRootService) -> T?): T?
    fun unbindRootServiceIfNeeded()

}

class RootServiceRepositoryImpl(context: Context): RootServiceRepository {

    companion object {
        private const val SERVICE_TIMEOUT = 7500L
    }

    private var serviceInstanceRoot: IPixelLauncherModsRootService? = null
    private var serviceConnectionRoot: ServiceConnection? = null
    private val serviceIntentRoot = Intent(context, PixelLauncherModsRootService::class.java)

    override suspend fun isRooted(): Boolean {
        return withContext(Dispatchers.IO){
            Shell.rootAccess()
        }
    }

    override suspend fun <T> runWithRootService(block: suspend (IPixelLauncherModsRootService) -> T): T? {
        val service = withContext(Dispatchers.IO) {
            getServiceLocked(serviceIntentRoot, ::asInterfaceRoot, ::serviceInstanceRoot, ::serviceConnectionRoot)
        } ?: return null
        return block(service)
    }

    private val getServiceMutex = Mutex()

    private fun asInterfaceRoot(binder: IBinder?): IPixelLauncherModsRootService {
        return IPixelLauncherModsRootService.Stub.asInterface(binder)
    }

    private suspend fun <S> getServiceLocked(
        intent: Intent,
        asInterface: (IBinder) -> S,
        serviceInstance: KMutableProperty0<S?>,
        serviceConnection: KMutableProperty0<ServiceConnection?>
    ) = suspendCoroutineWithTimeout<S?>(SERVICE_TIMEOUT) { resume ->
        runBlocking {
            getServiceMutex.lock()
            var resumed = false
            serviceInstance.get()?.let {
                if (resumed) return@let
                resume.resume(it)
                resumed = true
                getServiceMutex.unlock()
                return@runBlocking
            }
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder?) {
                    if (resumed) return
                    val result = if (binder != null && binder.pingBinder()) {
                        serviceInstance.set(asInterface(binder))
                        serviceConnection.set(this)
                        serviceInstance.get()
                    } else {
                        null
                    }
                    resumed = true
                    resume.resume(result)
                    getServiceMutex.unlock()
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceInstance.set(null)
                    serviceConnection.set(null)
                }
            }
            withContext(Dispatchers.Main) {
                RootService.bind(intent, serviceConnection)
            }
        }
    }

    override fun <T> runWithRootServiceIfAvailable(block: (IPixelLauncherModsRootService) -> T?): T? {
        return serviceInstanceRoot?.let {
            block(it)
        }
    }

    override fun unbindRootServiceIfNeeded() {
        serviceConnectionRoot?.let {
            RootService.unbind(it)
        }
    }

}