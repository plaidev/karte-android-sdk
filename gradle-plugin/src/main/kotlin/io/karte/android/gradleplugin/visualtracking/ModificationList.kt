package io.karte.android.gradleplugin.visualtracking

const val TYPE_VOID = "V"

data class Signature(
    val methodName: String,
    val returnType: String,
    val parameterTypes: List<String>
)

data class Target(val jvmClassName: String, val methodSignature: Signature) {
    val className = jvmClassName.substring(1, jvmClassName.length - 1).replace("/", ".")
}

data class Modification(val name: String, val target: Target)

private val MODIFICATION_LIST_ANDROID_FRAMEWORK = listOf(
    Modification(
        "android.view.View\$OnClickListener#onClick",
        Target(
            "Landroid/view/View\$OnClickListener;",
            Signature(
                "onClick",
                TYPE_VOID,
                listOf("Landroid/view/View;")
            )
        )
    ),
    Modification(
        "android.text.style.ClickableSpan#onClick",
        Target(
            "Landroid/text/style/ClickableSpan;",
            Signature(
                "onClick",
                TYPE_VOID,
                listOf("Landroid/view/View;")
            )
        )
    ),
    Modification(
        "android.app.ListActivity#onListItemClick",
        Target(
            "Landroid/app/ListActivity;",
            Signature(
                "onListItemClick",
                TYPE_VOID,
                listOf("Landroid/widget/ListView;", "Landroid/view/View;", "I", "J")
            )
        )
    ),
    Modification(
        "android.widget.AdapterView\$OnItemClickListener#onItemClick",
        Target(
            "Landroid/widget/AdapterView\$OnItemClickListener;",
            Signature(
                "onItemClick",
                TYPE_VOID,
                listOf(
                    "Landroid/widget/AdapterView;",
                    "Landroid/view/View;",
                    "I",
                    "J"
                )
            )
        )
    )
)

private val MODIFICATION_LIST_SUPPORT_LIB = listOf(
    Modification(
        "android.support.design.widget.TabLayout\$OnTabSelectedListener#onTabSelected",
        Target(
            "Landroid/support/design/widget/TabLayout\$OnTabSelectedListener;",
            Signature(
                "onTabSelected",
                TYPE_VOID,
                listOf("Landroid/support/design/widget/TabLayout\$Tab;")
            )
        )
    )
)

private val MODIFICATION_LIST_ANDROID_X = listOf(
    Modification(
        "com.google.android.material.tabs.TabLayout\$OnTabSelectedListener#onTabSelected",
        Target(
            "Lcom/google/android/material/tabs/TabLayout\$OnTabSelectedListener;",
            Signature(
                "onTabSelected",
                TYPE_VOID,
                listOf("Lcom/google/android/material/tabs/TabLayout\$Tab;")
            )
        )
    )
)

private val ALL_MODIFICATION_LIST = MODIFICATION_LIST_ANDROID_FRAMEWORK +
    MODIFICATION_LIST_SUPPORT_LIB + MODIFICATION_LIST_ANDROID_X
val METHOD_SIG_TO_MOD_LIST: Map<String, List<Modification>> = ALL_MODIFICATION_LIST
    .fold(mutableMapOf<String, MutableList<Modification>>()) { acc, modification ->
        val sig = modification.target.methodSignature
        val sigStr =
            "${sig.methodName}(${sig.parameterTypes.joinToString(separator = "")})${sig.returnType}"
        if (acc.containsKey(sigStr)) {
            acc[sigStr]!!.add(modification)
        } else {
            acc[sigStr] = mutableListOf(modification)
        }
        return@fold acc
    }

val MODIFICATION_EXCLUDE_PACKAGE_CORE = "io.karte.android.core"
val MODIFICATION_EXCLUDE_PACKAGE_VISUALTRACKING = "io.karte.android.visualtracking"
