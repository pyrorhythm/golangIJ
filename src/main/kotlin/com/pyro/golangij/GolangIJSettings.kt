package com.pyro.golangij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "GolangIJSettings", storages = [Storage("golangij.xml")])
class GolangIJSettings : PersistentStateComponent<GolangIJSettings.State> {

    enum class ArrowStyle(val symbol: String) {
        ASCII("<-"),
        UNICODE("←")
    }

    enum class EllipsisStyle(val symbol: String) {
        ASCII("..."),
        UNICODE("…"),
        UNICODE_MIDDLE("⋯")
    }

    enum class GenericBracketStyle(val open: String, val close: String) {
        SQUARE("[", "]"),
    ANGLE("⟨", "⟩"),
        ANGLE_ASCII("<", ">")
    }

    enum class PointerStyle(val symbol: String) {
        ASTERISK("*"),
        CARET("^")
    }

    enum class SeparatorStyle(val symbol: String) {
        COMMA(", "),
        PIPE(" | "),
        SEMICOLON("; ")
    }

    class State {
        var arrowStyle: ArrowStyle = ArrowStyle.UNICODE
        var ellipsisStyle: EllipsisStyle = EllipsisStyle.UNICODE
        var genericBracketStyle: GenericBracketStyle = GenericBracketStyle.SQUARE
        var pointerStyle: PointerStyle = PointerStyle.ASTERISK
        var separatorStyle: SeparatorStyle = SeparatorStyle.COMMA
        var maxHintLength: Int = 60
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    val arrowStyle: ArrowStyle get() = myState.arrowStyle
    val ellipsisStyle: EllipsisStyle get() = myState.ellipsisStyle
    val genericBracketStyle: GenericBracketStyle get() = myState.genericBracketStyle
    val pointerStyle: PointerStyle get() = myState.pointerStyle
    val separatorStyle: SeparatorStyle get() = myState.separatorStyle

    companion object {
        @JvmStatic
        fun getInstance(): GolangIJSettings =
            ApplicationManager.getApplication().getService(GolangIJSettings::class.java)
    }
}