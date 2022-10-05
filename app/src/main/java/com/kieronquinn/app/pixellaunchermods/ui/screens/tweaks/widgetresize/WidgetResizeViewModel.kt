package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigation
import com.kieronquinn.app.pixellaunchermods.model.ipc.GridSize
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteWidget
import com.kieronquinn.app.pixellaunchermods.repositories.AppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.RemoteTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class WidgetResizeViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        data class Loaded(
            val pages: Map<Int, List<Target>>,
            val gridSize: GridSize,
            val selectedWidget: Target.Widget?
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class Target(val type: Type, open var spanX: Int, open var spanY: Int) {
        data class Shortcut(val label: String?, val applicationInfo: ApplicationInfo?): Target(Type.SHORTCUT, 1, 1)
        data class Widget(
            val provider: String,
            val appWidgetId: Int,
            override var spanX: Int,
            override var spanY: Int,
            val canExpandX: Boolean,
            val canExpandY: Boolean,
            val canShrinkX: Boolean,
            val canShrinkY: Boolean
        ): Target(Type.WIDGET, spanX, spanY)

        object Space: Target(Type.SPACE, 1, 1)

        enum class Type {
            SHORTCUT, WIDGET, SPACE
        }
    }

    abstract val state: StateFlow<State>
    abstract val hasChanges: StateFlow<Boolean>
    abstract fun startListening()
    abstract fun stopListening()
    abstract suspend fun loadWidget(context: Context, widget: Target.Widget): AppWidgetHostView
    abstract fun onWidgetClicked(widget: Target.Widget?)
    abstract fun onWidgetPlusXClicked()
    abstract fun onWidgetMinusXClicked()
    abstract fun onWidgetPlusYClicked()
    abstract fun onWidgetMinusYClicked()
    abstract fun commitChanges()

}

class WidgetResizeViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val remoteAppsRepository: RemoteAppsRepository,
    private val appWidgetRepository: AppWidgetRepository
): WidgetResizeViewModel() {

    private val widgetCache = HashMap<Int, AppWidgetHostView>()
    private val selectedWidgetId = MutableStateFlow<Int?>(null)
    private val onChanged = MutableStateFlow(System.currentTimeMillis())
    private val workspace = HashMap<Int, Target.Widget>()

    override val hasChanges = onChanged.map {
        workspace.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val allTargets = flow {
        emit(remoteAppsRepository.getRemoteTargets())
    }

    private val gridSize = flow {
        emit(remoteAppsRepository.getGridSize() ?: GridSize(5, 5))
    }

    private val pages = combine(allTargets, gridSize, onChanged) { targets, grid, _ ->
        val maxScreen = targets.maxOf { it.screen }
        val pageMap = HashMap<Int, List<Target>>()
        for(i in 0 .. maxScreen) {
            pageMap[i] = targets.map {
                if(it !is RemoteTarget.Widget) return@map it //Only want widgets
                val working = workspace[it.appWidgetId] ?: return@map it //Not changed
                it.spanX = working.spanX
                it.spanY = working.spanY
                it
            }.flatMapRemoteTargets(i, grid)
        }
        Pair(pageMap, grid)
    }

    override val state = combine(pages, selectedWidgetId) { p, s ->
        val selected = p.first.flatMap { it.value }.find {
            it is Target.Widget && it.appWidgetId == s
        } as? Target.Widget
        State.Loaded(p.first, p.second, selected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun startListening() {
        viewModelScope.launch {
            appWidgetRepository.startListening()
        }
    }

    override fun stopListening() {
        //Break out of viewmodelscope as we're closing
        GlobalScope.launch {
            appWidgetRepository.stopListening()
        }
    }

    /**
     *  Creates a flattened map of a given list of remote targets, with a given grid size.
     *  This creates a list of all targets, including spaces, to be drawn in the adapter.
     */
    private fun List<RemoteTarget>.flatMapRemoteTargets(screen: Int, gridSize: GridSize): List<Target> {
        val targets = ArrayList<Target>()
        repeat(gridSize.y){ y ->
            repeat(gridSize.x){ x ->
                getTargetByPosition(screen, x, y, gridSize, true)?.let {
                    targets.add(it)
                }
            }
        }
        return targets
    }

    /**
     *  Gets a Target from a given ([x],[y]) on [screen] position in the remote target list
     */
    private fun List<RemoteTarget>.getTargetByPosition(screen: Int, x: Int, y: Int, gridSize: GridSize, lookupNext: Boolean): Target? {
        //First check if a target is actually in this position
        val exactTarget = firstOrNull { it.screen == screen && it.cellX == x && it.cellY == y }
        if(exactTarget != null) return exactTarget.toTarget(lookupNext) {
            findNextBlocking(it, screen, gridSize)
        }
        //Check if a target overlaps this position (ie. there's a widget on top)
        val inexactTarget = firstOrNull {
            it.screen == screen
                && (x >= it.cellX && x < it.cellX + it.spanX)
                && (y >= it.cellY && y < it.cellY + it.spanY)
        }
        if(inexactTarget != null) return null //No target should be added as the other span covers it
        return Target.Space //Empty space
    }

    private fun RemoteTarget.toTarget(lookupNext: Boolean, findNextBlocking: (RemoteTarget) -> Pair<Int, Int>): Target {
        return when(this){
            is RemoteTarget.Shortcut -> {
                Target.Shortcut(label, applicationInfo)
            }
            is RemoteTarget.Widget -> {
                val canExpand = if(lookupNext){
                    val nextBlocking = findNextBlocking(this)
                    Pair(nextBlocking.first - spanX > cellX, nextBlocking.second - spanY > cellY)
                }else Pair(first = false, second = false)
                val canShrink = if(lookupNext){
                    Pair(spanX > 1, spanY > 1)
                }else Pair(first = false, second = false)
                Target.Widget(provider, appWidgetId, spanX, spanY, canExpand.first, canExpand.second, canShrink.first, canShrink.second)
            }
        }
    }

    /**
     *  Finds the next blocking x and y cells for a given target, if expanding rightwards or
     *  upwards. Defaults to the current cell positions if no expansion is possible.
     */
    private fun List<RemoteTarget>.findNextBlocking(remoteTarget: RemoteTarget, screen: Int, gridSize: GridSize): Pair<Int, Int> {
        val maxCellX = ArrayList<Int>().apply {
            for(y in remoteTarget.cellY until remoteTarget.cellY + remoteTarget.spanY) {
                add(findNextBlockingTargetX(remoteTarget.cellX + remoteTarget.spanX, screen, y, gridSize))
            }
        }.minOrNull() ?: remoteTarget.cellX
        val maxCellY = ArrayList<Int>().apply {
            for(x in remoteTarget.cellX until remoteTarget.cellX + remoteTarget.spanX) {
                add(findNextBlockingTargetY(remoteTarget.cellY + remoteTarget.spanY, screen, x, gridSize))
            }
        }.minOrNull() ?: remoteTarget.cellY
        return Pair(maxCellX, maxCellY)
    }

    private fun List<RemoteTarget>.findNextBlockingTargetX(startX: Int, screen: Int, y: Int, gridSize: GridSize): Int {
        val maxX = gridSize.x
        for(x in startX until maxX){
            val target = getTargetByPosition(screen, x, y, gridSize, false)
            if(target !is Target.Space){
                return x
            }
        }
        return maxX
    }

    private fun List<RemoteTarget>.findNextBlockingTargetY(startY: Int, screen: Int, x: Int, gridSize: GridSize): Int {
        val maxY = gridSize.y
        for(y in startY until maxY){
            val target = getTargetByPosition(screen, x, y, gridSize, false)
            if(target !is Target.Space){
                return y
            }
        }
        return maxY
    }

    override suspend fun loadWidget(context: Context, widget: Target.Widget): AppWidgetHostView {
        widgetCache[widget.appWidgetId]?.let {
            return it
        }
        val view = appWidgetRepository.createView(context, widget.appWidgetId, widget.provider)
        widgetCache[widget.appWidgetId] = view
        return view
    }

    override fun onWidgetClicked(widget: Target.Widget?) {
        viewModelScope.launch {
            selectedWidgetId.emit(widget?.appWidgetId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        widgetCache.clear()
    }

    private fun getSelectedWidget(): Target.Widget? {
        return (state.value as? State.Loaded)?.selectedWidget
    }

    override fun onWidgetPlusXClicked() {
        val currentWidget = getSelectedWidget() ?: return
        viewModelScope.launch {
            currentWidget.spanX = currentWidget.spanX + 1
            commitToWorkspace(currentWidget)
        }
    }

    override fun onWidgetMinusXClicked() {
        val currentWidget = getSelectedWidget() ?: return
        viewModelScope.launch {
            currentWidget.spanX = currentWidget.spanX - 1
            commitToWorkspace(currentWidget)
        }
    }

    override fun onWidgetPlusYClicked() {
        val currentWidget = getSelectedWidget() ?: return
        viewModelScope.launch {
            currentWidget.spanY = currentWidget.spanY + 1
            commitToWorkspace(currentWidget)
        }
    }

    override fun onWidgetMinusYClicked() {
        val currentWidget = getSelectedWidget() ?: return
        viewModelScope.launch {
            currentWidget.spanY = currentWidget.spanY - 1
            commitToWorkspace(currentWidget)
        }
    }

    override fun commitChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            remoteAppsRepository.updateWidgets(workspace.values.map {
                RemoteWidget(it.appWidgetId, it.spanX, it.spanY)
            })
            workspace.clear()
            onChanged.emit(System.currentTimeMillis())
        }
    }

    private suspend fun commitToWorkspace(currentWidget: Target.Widget) {
        workspace[currentWidget.appWidgetId] = currentWidget
        onChanged.emit(System.currentTimeMillis())
    }

}