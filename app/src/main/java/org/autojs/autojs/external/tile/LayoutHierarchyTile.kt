package org.autojs.autojs.external.tile

import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutHierarchyFloatyWindow

class LayoutHierarchyTile : LayoutInspectTileService() {

    override fun onCreateWindow(capture: NodeInfo): FullScreenFloatyWindow {
        return object : LayoutHierarchyFloatyWindow(capture) {
            override fun close() {
                super.close()
                setInactive()
            }
        }
    }
}
