package io.karte.android.visualtracking.internal

internal enum class HookTargetMethodFromDynamicInvoke(
    private val className: String,
    private val methodName: String
) {
    VIEW_CLICK("android.view.View", "performClick"),
    ADAPTER_VIEW_ITEM_CLICK("android.widget.AdapterView", "performItemClick");

    val actionName: String = "$className#$methodName"

    internal companion object {
        fun from(stackTraceElement: StackTraceElement): HookTargetMethodFromDynamicInvoke? {
            return values()
                .firstOrNull {
                    it.className == stackTraceElement.className &&
                        it.methodName == stackTraceElement.methodName
                }
        }
    }
}
