package com.pyro.golangij

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class GolangIJInlayHintsConfigurable : Configurable {

    private lateinit var arrowStyleCombo: JComboBox<GolangIJSettings.ArrowStyle>
    private lateinit var ellipsisStyleCombo: JComboBox<GolangIJSettings.EllipsisStyle>
    private lateinit var genericBracketCombo: JComboBox<GolangIJSettings.GenericBracketStyle>
    private lateinit var pointerStyleCombo: JComboBox<GolangIJSettings.PointerStyle>
    private lateinit var separatorStyleCombo: JComboBox<GolangIJSettings.SeparatorStyle>
    private lateinit var maxHintLengthSpinner: JSpinner

    override fun getDisplayName(): String = "Inlay Hints"

    override fun createComponent(): JComponent {
        arrowStyleCombo = JComboBox(GolangIJSettings.ArrowStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.ArrowStyle.ASCII -> "<-chan / chan<-"
                    GolangIJSettings.ArrowStyle.UNICODE -> "←chan / chan→"
                }
            }
        }

        ellipsisStyleCombo = JComboBox(GolangIJSettings.EllipsisStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.EllipsisStyle.ASCII -> "..."
                    GolangIJSettings.EllipsisStyle.UNICODE -> "…"
                    GolangIJSettings.EllipsisStyle.UNICODE_MIDDLE -> "⋯"
                }
            }
        }

        genericBracketCombo = JComboBox(GolangIJSettings.GenericBracketStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.GenericBracketStyle.SQUARE -> "[T]"
                    GolangIJSettings.GenericBracketStyle.ANGLE -> "⟨T⟩"
                    GolangIJSettings.GenericBracketStyle.ANGLE_ASCII -> "<T>"
                }
            }
        }

        pointerStyleCombo = JComboBox(GolangIJSettings.PointerStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.PointerStyle.ASTERISK -> "*T"
                    GolangIJSettings.PointerStyle.CARET -> "^T"
                }
            }
        }

        separatorStyleCombo = JComboBox(GolangIJSettings.SeparatorStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.SeparatorStyle.COMMA -> "a, b"
                    GolangIJSettings.SeparatorStyle.PIPE -> "a | b"
                    GolangIJSettings.SeparatorStyle.SEMICOLON -> "a; b"
                }
            }
        }

        maxHintLengthSpinner = JSpinner(SpinnerNumberModel(60, 0, 500, 5)).apply {
            (editor as JSpinner.DefaultEditor).textField.columns = 4
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Max hint length (0 = unlimited):", maxHintLengthSpinner)
            .addSeparator()
            .addLabeledComponent("Channel arrows:", arrowStyleCombo)
            .addLabeledComponent("Variadic ellipsis:", ellipsisStyleCombo)
            .addLabeledComponent("Generic brackets:", genericBracketCombo)
            .addLabeledComponent("Pointer prefix:", pointerStyleCombo)
            .addLabeledComponent("Type separator:", separatorStyleCombo)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = GolangIJSettings.getInstance()
        return arrowStyleCombo.selectedItem != settings.arrowStyle
                || ellipsisStyleCombo.selectedItem != settings.ellipsisStyle
                || genericBracketCombo.selectedItem != settings.genericBracketStyle
                || pointerStyleCombo.selectedItem != settings.pointerStyle
                || separatorStyleCombo.selectedItem != settings.separatorStyle
                || maxHintLengthSpinner.value != settings.state.maxHintLength
    }

    override fun apply() {
        val state = GolangIJSettings.getInstance().state
        state.arrowStyle = arrowStyleCombo.selectedItem as GolangIJSettings.ArrowStyle
        state.ellipsisStyle = ellipsisStyleCombo.selectedItem as GolangIJSettings.EllipsisStyle
        state.genericBracketStyle = genericBracketCombo.selectedItem as GolangIJSettings.GenericBracketStyle
        state.pointerStyle = pointerStyleCombo.selectedItem as GolangIJSettings.PointerStyle
        state.separatorStyle = separatorStyleCombo.selectedItem as GolangIJSettings.SeparatorStyle
        state.maxHintLength = maxHintLengthSpinner.value as Int
    }

    override fun reset() {
        val settings = GolangIJSettings.getInstance()
        arrowStyleCombo.selectedItem = settings.arrowStyle
        ellipsisStyleCombo.selectedItem = settings.ellipsisStyle
        genericBracketCombo.selectedItem = settings.genericBracketStyle
        pointerStyleCombo.selectedItem = settings.pointerStyle
        separatorStyleCombo.selectedItem = settings.separatorStyle
        maxHintLengthSpinner.value = settings.state.maxHintLength
    }

    companion object {
        private inline fun <reified T : Enum<T>> enumRenderer(crossinline display: (T) -> String): ListCellRenderer<T> =
            DefaultListCellRenderer().let { delegate ->
                ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
                    delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
                        if (value is T) delegate.text = display(value)
                    }
                }
            }
    }
}