package com.pyro.golangij

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class GolangIJInlayHintsConfigurable : Configurable {
    private lateinit var chanStyleCombo: JComboBox<GolangIJSettings.ChanStyle>
    private lateinit var chanTypeBracketsStyleCombo: JComboBox<GolangIJSettings.ChanTypeBracketsStyle>
    private lateinit var ellipsisStyleCombo: JComboBox<GolangIJSettings.EllipsisStyle>
    private lateinit var genericBracketCombo: JComboBox<GolangIJSettings.GenericBracketStyle>
    private lateinit var pointerStyleCombo: JComboBox<GolangIJSettings.PointerStyle>
    private lateinit var separatorStyleCombo: JComboBox<GolangIJSettings.SeparatorStyle>
    private lateinit var funcLiteralStyleCombo: JComboBox<GolangIJSettings.FuncLiteralStyle>
    private lateinit var insertSpaceOnLeftToggle: JCheckBox
    private lateinit var renderTypeParamsToggle: JCheckBox
    private lateinit var renderTypeParamsConstraintsToggle: JCheckBox
    private lateinit var maxHintLengthSpinner: JSpinner

    override fun getDisplayName(): String = "Inlay Hints"

    override fun createComponent(): JComponent {
        chanStyleCombo = ComboBox(GolangIJSettings.ChanStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.ChanStyle.DEFAULT -> "Default (<-chan)"
                    GolangIJSettings.ChanStyle.UNICODE -> "Unicode (← chan)"
                    GolangIJSettings.ChanStyle.LITERAL -> "Literal (chan recv / chan send / chan bi)"
                }
            }
        }

        ellipsisStyleCombo = ComboBox(GolangIJSettings.EllipsisStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.EllipsisStyle.DEFAULT -> "Default (...)"
                    GolangIJSettings.EllipsisStyle.UNICODE -> "Unicode (…)"
                    GolangIJSettings.EllipsisStyle.UNICODE_MIDDLE -> "Unicode middle-aligned (⋯)"
                    GolangIJSettings.EllipsisStyle.TILDE -> "Tilde (~)"
                }
            }
        }

        genericBracketCombo = ComboBox(GolangIJSettings.GenericBracketStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.GenericBracketStyle.DEFAULT -> "Default - [int]"
                    GolangIJSettings.GenericBracketStyle.UNICODE_ANGLED -> "Unicode angled - ⟨int⟩"
                    GolangIJSettings.GenericBracketStyle.ASCII_ANGLED -> "ASCII angled - <int>"
                }
            }
        }

        pointerStyleCombo = ComboBox(GolangIJSettings.PointerStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.PointerStyle.DEFAULT -> "Default (*int)"
                    GolangIJSettings.PointerStyle.CARET -> "Caret (^int)"
                    GolangIJSettings.PointerStyle.AMPERSAND -> "Ampersand (&int)"
                    GolangIJSettings.PointerStyle.PTR_OF -> "Literal (ptrOf int)"
                }
            }
        }

        separatorStyleCombo = ComboBox(GolangIJSettings.SeparatorStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.SeparatorStyle.DEFAULT -> "Default (a, b)"
                    GolangIJSettings.SeparatorStyle.PIPE -> "Pipe (a | b)"
                    GolangIJSettings.SeparatorStyle.SEMICOLON -> "Semicolon (a; b)"
                }
            }
        }

        funcLiteralStyleCombo = ComboBox(GolangIJSettings.FuncLiteralStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.FuncLiteralStyle.DEFAULT -> "Default ( func(int) -> float )"
                    GolangIJSettings.FuncLiteralStyle.SCIENTIFIC -> "Scientific ( ƒ(int) -> float )"
                    GolangIJSettings.FuncLiteralStyle.NO -> "No literal ( (int) -> float )"
                }
            }
        }


        chanTypeBracketsStyleCombo = ComboBox(GolangIJSettings.ChanTypeBracketsStyle.entries.toTypedArray()).apply {
            renderer = enumRenderer {
                when (it) {
                    GolangIJSettings.ChanTypeBracketsStyle.DEFAULT -> "Default ( chan int )"
                    GolangIJSettings.ChanTypeBracketsStyle.ROUND -> "Round ( chan(int) )"
                    GolangIJSettings.ChanTypeBracketsStyle.SQUARE -> "Squared ( chan[int] )"
                    GolangIJSettings.ChanTypeBracketsStyle.UNICODE_ANGLED -> "Unicode angled ( chan⟨int⟩ )"
                    GolangIJSettings.ChanTypeBracketsStyle.ASCII_ANGLED -> "ASCII angled ( chan<int> )"
                }
            }
        }


        maxHintLengthSpinner = JSpinner(SpinnerNumberModel(60, 0, 500, 5)).apply {
            (editor as JSpinner.DefaultEditor).textField.columns = 4
        }

        insertSpaceOnLeftToggle = JCheckBox()
        renderTypeParamsToggle = JCheckBox()
        renderTypeParamsConstraintsToggle = JCheckBox()

        renderTypeParamsConstraintsToggle.isEnabled = renderTypeParamsToggle.isSelected
        renderTypeParamsToggle.addActionListener {
            renderTypeParamsConstraintsToggle.isEnabled = renderTypeParamsToggle.isSelected
            if (!renderTypeParamsToggle.isSelected) renderTypeParamsConstraintsToggle.isSelected = false
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Max hint length (0 = unlimited)", maxHintLengthSpinner)
            .addSeparator()
            .addLabeledComponent("Insert one space on left for each hint", insertSpaceOnLeftToggle)
            .addLabeledComponent("Render type parameters", renderTypeParamsToggle)
            .addLabeledComponent("Also render their constraints", renderTypeParamsConstraintsToggle)
            .addSeparator()
            .addLabeledComponent("Channel arrows:", chanStyleCombo)
            .addLabeledComponent("Channel type brackets:", chanTypeBracketsStyleCombo)
            .addLabeledComponent("Variadic ellipsis:", ellipsisStyleCombo)
            .addLabeledComponent("Generic brackets:", genericBracketCombo)
            .addLabeledComponent("Pointer prefix:", pointerStyleCombo)
            .addLabeledComponent("Type separator:", separatorStyleCombo)
            .addLabeledComponent("Function literal:", funcLiteralStyleCombo)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = GolangIJSettings.getInstance()
        return chanStyleCombo.selectedItem != settings.chanStyle
                || ellipsisStyleCombo.selectedItem != settings.ellipsisStyle
                || genericBracketCombo.selectedItem != settings.genericBracketStyle
                || pointerStyleCombo.selectedItem != settings.pointerStyle
                || separatorStyleCombo.selectedItem != settings.separatorStyle
                || maxHintLengthSpinner.value != settings.state.maxHintLength
                || funcLiteralStyleCombo.selectedItem != settings.funcLiteralStyle
                || chanTypeBracketsStyleCombo.selectedItem != settings.chanTypeBracketsStyle
                || insertSpaceOnLeftToggle.isSelected != settings.state.insertSpaceOnLeft
                || renderTypeParamsToggle.isSelected != settings.state.renderTypeParams
                || renderTypeParamsConstraintsToggle.isSelected != settings.state.renderTypeParamsConstraints
    }

    override fun apply() {
        val state = GolangIJSettings.getInstance().state
        state.chanStyle = chanStyleCombo.selectedItem as GolangIJSettings.ChanStyle
        state.ellipsisStyle = ellipsisStyleCombo.selectedItem as GolangIJSettings.EllipsisStyle
        state.genericBracketStyle = genericBracketCombo.selectedItem as GolangIJSettings.GenericBracketStyle
        state.pointerStyle = pointerStyleCombo.selectedItem as GolangIJSettings.PointerStyle
        state.separatorStyle = separatorStyleCombo.selectedItem as GolangIJSettings.SeparatorStyle
        state.chanTypeBracketsStyle = chanTypeBracketsStyleCombo.selectedItem as GolangIJSettings.ChanTypeBracketsStyle
        state.funcLiteralStyle = funcLiteralStyleCombo.selectedItem as GolangIJSettings.FuncLiteralStyle
        state.maxHintLength = maxHintLengthSpinner.value as Int
        state.insertSpaceOnLeft = insertSpaceOnLeftToggle.isSelected
        state.renderTypeParams = renderTypeParamsToggle.isSelected
        state.renderTypeParamsConstraints = renderTypeParamsConstraintsToggle.isSelected
    }

    override fun reset() {
        val settings = GolangIJSettings.getInstance()
        chanStyleCombo.selectedItem = settings.chanStyle
        ellipsisStyleCombo.selectedItem = settings.ellipsisStyle
        genericBracketCombo.selectedItem = settings.genericBracketStyle
        pointerStyleCombo.selectedItem = settings.pointerStyle
        separatorStyleCombo.selectedItem = settings.separatorStyle
        chanTypeBracketsStyleCombo.selectedItem = settings.chanTypeBracketsStyle
        funcLiteralStyleCombo.selectedItem = settings.funcLiteralStyle
        maxHintLengthSpinner.value = settings.state.maxHintLength
        insertSpaceOnLeftToggle.setSelected(settings.state.insertSpaceOnLeft)
        renderTypeParamsToggle.setSelected(settings.state.renderTypeParams)
        renderTypeParamsConstraintsToggle.setSelected(settings.state.renderTypeParamsConstraints)
    }

}

private inline fun <reified T : Enum<T>> enumRenderer(crossinline display: (T) -> String): ListCellRenderer<T> =
    DefaultListCellRenderer().let { delegate ->
        ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
            delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
                if (value is T) delegate.text = display(value)
            }
        }
    }