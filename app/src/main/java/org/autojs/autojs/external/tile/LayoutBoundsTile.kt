package org.autojs.autojs.external.tile

import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutBoundsFloatyWindow

class LayoutBoundsTile : LayoutInspectTileService() {

    override fun onCreateWindow(capture: NodeInfo): FullScreenFloatyWindow {
        return object : LayoutBoundsFloatyWindow(capture) {
            override fun close() {
                super.close()
                setInactive()
            }
        }
    }
}
