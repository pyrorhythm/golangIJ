package com.pyro.golangij

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JLabel

class GolangIJRootConfigurable : Configurable {

    override fun getDisplayName(): String = "GolangIJ"

    override fun createComponent(): JComponent =
        JLabel("Configure GolangIJ settings in the subcategories below.\n\n")

    override fun isModified(): Boolean = false

    override fun apply() {}
}