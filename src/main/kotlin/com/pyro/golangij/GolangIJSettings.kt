package com.pyro.golangij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "GolangIJSettings", storages = [Storage("golangij.xml")])
class GolangIJSettings : PersistentStateComponent<GolangIJSettings.State> {

    enum class FuncLiteralStyle(val symbol: String) {
        DEFAULT("func"),
        SCIENTIFIC("ƒ"),
        NO(""),
    }

    enum class ChanStyle(val recvChan: String, val sendChan: String, val biChan: String) {
        DEFAULT("<-chan", "chan<-", "chan"),
        UNICODE("← chan", "chan ←", "chan"),
        LITERAL("chan recv", "chan send", "chan bi")
    }

    enum class ChanTypeBracketsStyle(val left: String, val right: String) {
        DEFAULT(" ", ""),
        ROUND("(", ")"),
        SQUARE("[", "]"),
        UNICODE_ANGLED("⟨", "⟩"),
        ASCII_ANGLED("<", ">")
    }


    enum class EllipsisStyle(val symbol: String) {
        DEFAULT("..."),
        UNICODE("…"),
        UNICODE_MIDDLE("⋯"),
        TILDE("~")
    }

    enum class GenericBracketStyle(val open: String, val close: String) {
        DEFAULT("[", "]"),
        UNICODE_ANGLED("⟨", "⟩"),
        ASCII_ANGLED("<", ">")
    }

    enum class PointerStyle(val symbol: String) {
        DEFAULT("*"),
        CARET("^"),
        AMPERSAND("&"),
        PTR_OF("ptrOf ")
    }

    enum class SeparatorStyle(val symbol: String) {
        DEFAULT(", "),
        PIPE(" | "),
        SEMICOLON("; ")
    }


    class State {
        var chanStyle: ChanStyle = ChanStyle.DEFAULT
        var chanTypeBracketsStyle: ChanTypeBracketsStyle = ChanTypeBracketsStyle.DEFAULT
        var ellipsisStyle: EllipsisStyle = EllipsisStyle.DEFAULT
        var genericBracketStyle: GenericBracketStyle = GenericBracketStyle.DEFAULT
        var pointerStyle: PointerStyle = PointerStyle.DEFAULT
        var separatorStyle: SeparatorStyle = SeparatorStyle.DEFAULT
        var funcLiteralStyle: FuncLiteralStyle = FuncLiteralStyle.DEFAULT
        var insertSpaceOnLeft: Boolean = true
        var renderTypeParams: Boolean = false
        var renderTypeParamsConstraints: Boolean = false
        var maxHintLength: Int = 60
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    val chanStyle: ChanStyle get() = myState.chanStyle
    val chanTypeBracketsStyle: ChanTypeBracketsStyle get() = myState.chanTypeBracketsStyle
    val ellipsisStyle: EllipsisStyle get() = myState.ellipsisStyle
    val genericBracketStyle: GenericBracketStyle get() = myState.genericBracketStyle
    val pointerStyle: PointerStyle get() = myState.pointerStyle
    val separatorStyle: SeparatorStyle get() = myState.separatorStyle
    val funcLiteralStyle: FuncLiteralStyle get() = myState.funcLiteralStyle

    companion object {
        @JvmStatic
        fun getInstance(): GolangIJSettings =
            ApplicationManager.getApplication().getService(GolangIJSettings::class.java)
    }
}