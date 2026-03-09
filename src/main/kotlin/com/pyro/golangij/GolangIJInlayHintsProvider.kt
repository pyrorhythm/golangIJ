package com.pyro.golangij

import com.goide.psi.*
import com.goide.psi.impl.GoLightType
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPointerManager
import org.slf4j.LoggerFactory

class GolangIJInlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector =
        GoInlayTypeHintsCollector()

    private class GoInlayTypeHintsCollector : SharedBypassCollector {

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (!element.language.`is`(com.goide.GoLanguage.INSTANCE)) return

            var varDefs = emptyList<PsiNamedElement>()
            var resolvedTypes = emptyList<GoType>()

            when (element) {
                is GoShortVarDeclaration -> {
                    varDefs = element.varDefinitionList
                    resolvedTypes = resolveGoTypes(element.expressionList.stream())
                }
                is GoRangeClause -> {
                    varDefs = element.varDefinitionList
                    resolvedTypes = resolveGoTypes(element.varDefinitionList.stream())
                }
                is GoRecvStatement -> {
                    if (element.varDefinitionList.isEmpty()) return

                    varDefs = element.varDefinitionList
                    resolvedTypes = resolveGoTypes(varDefs.stream())
                }
                is GoVarSpec -> {
                    if (element.type != null) return // already specified

                    varDefs = element.varDefinitionList
                    resolvedTypes = resolveGoTypes(element.expressionList.stream())
                }
                is GoConstSpec -> {
                    if (element.type != null) return // already specified

                    varDefs = element.constDefinitionList
                    resolvedTypes = resolveGoTypes(element.expressionList.stream())
                }
            }

            deduceInlayTypeHints(element, sink, resolvedTypes, varDefs)
        }

        private fun deduceInlayTypeHints(
            psiElement: PsiElement,
            sink: InlayTreeSink,
            resolvedTypes: List<GoType>,
            varDefs: List<PsiNamedElement>,
        ) {
            if (resolvedTypes.size != varDefs.size) {
                log.debug("Type count {} != defs count {} in: {}", resolvedTypes.size, varDefs.size, psiElement.text)
                return
            }
            val sym = GolangIJSettings.getInstance().state
            varDefs.zip(resolvedTypes).forEach { (varDef, goType) ->
                if (varDef.name == "_") return@forEach
                addHintWithLengthLimit(psiElement, sink, varDef.textRange.endOffset, goType, sym)
            }
        }

        private fun addHintWithLengthLimit(
            psiElement: PsiElement,
            sink: InlayTreeSink,
            offset: Int,
            goType: GoType,
            sym: GolangIJSettings.State,
        ) {
            val fullText = goType.presentationText
            val needsTruncation = sym.maxHintLength > 0 && fullText.length > sym.maxHintLength

            sink.addPresentation(
                InlineInlayPosition(offset, true, 0),
                null,
                fullText,
                HintFormat.default,
            ) {
                if (needsTruncation) {
                    renderGoTypeTruncated(psiElement, this, goType, sym, sym.maxHintLength)
                } else {
                    renderGoType(psiElement, this, goType, sym)
                }
            }
        }

        private fun renderGoType(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            goType: GoType,
            sym: GolangIJSettings.State,
        ) {
            when (goType) {
                is GoMapType -> {
                    ptb.text("map[", null)
                    goType.keyType?.let { renderGoType(psiElement, ptb, it, sym) }
                    ptb.text("]", null)
                    goType.valueType?.let { renderGoType(psiElement, ptb, it, sym) }
                }
                is GoArrayOrSliceType -> {
                    ptb.text("[", null)
                    if (goType.isArray) ptb.text("${goType.length}", null)
                    ptb.text("]", null)
                    renderGoType(psiElement, ptb, goType.type, sym)
                }
                is GoPointerType -> {
                    ptb.text(sym.pointerStyle.symbol, null)
                    goType.type?.let { renderGoType(psiElement, ptb, it, sym) }
                }
                is GoChannelType -> {
                    val chanText = goType.presentationText
                    if (chanText.startsWith("<-chan")) ptb.text(sym.arrowStyle.symbol, null)
                    ptb.text("chan", null)
                    if (chanText.startsWith("chan<-")) ptb.text(sym.arrowStyle.symbol, null)
                    ptb.text(" ", null)
                    goType.type?.let { renderGoType(psiElement, ptb, it, sym) }
                }
                is GoFunctionType -> {
                    ptb.text("func", null)
                    renderFunctionSignature(psiElement, ptb, goType, sym)
                }
                is GoSpecType -> renderSpecType(psiElement, ptb, goType, sym)
                else -> renderNamedTypeWithArgs(psiElement, ptb, goType, sym)
            }
        }

        private fun renderSpecType(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            specType: GoSpecType,
            sym: GolangIJSettings.State,
        ) {
            ptb.text(specType.identifier.text, createNavigationActionData(psiElement, specType))
            specType.typeArguments?.let { renderTypeArguments(psiElement, ptb, it, sym) }
        }

        private fun renderNamedTypeWithArgs(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            goType: GoType,
            sym: GolangIJSettings.State,
        ) {
            val text = goType.presentationText
            val bracketIdx = text.indexOf('[')

            if (bracketIdx > 0 && sym.genericBracketStyle != GolangIJSettings.GenericBracketStyle.SQUARE) {
                val baseName = text.substring(0, bracketIdx)
                ptb.text(baseName, createNavigationActionData(psiElement, goType))
                val argsPart = text.substring(bracketIdx + 1, text.length - 1)
                ptb.text(sym.genericBracketStyle.open, null)
                ptb.text(argsPart, null)
                ptb.text(sym.genericBracketStyle.close, null)
            } else {
                ptb.text(text, createNavigationActionData(psiElement, goType))
            }
        }

        private fun renderTypeArguments(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            typeArgs: GoTypeArguments,
            sym: GolangIJSettings.State,
        ) {
            val types = typeArgs.types
            if (types.isEmpty()) return
            ptb.text(sym.genericBracketStyle.open, null)
            types.forEachIndexed { i, type ->
                if (i > 0) ptb.text(sym.separatorStyle.symbol, null)
                renderGoType(psiElement, ptb, type, sym)
            }
            ptb.text(sym.genericBracketStyle.close, null)
        }

        private fun renderFunctionSignature(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            funcType: GoFunctionType,
            sym: GolangIJSettings.State,
        ) {
            val signature = funcType.signature
            if (signature == null) {
                ptb.text("(${sym.ellipsisStyle.symbol})", null)
                return
            }
            ptb.text("(", null)
            renderParamDecls(psiElement, ptb, sym, signature.parameters.parameterDeclarationList)
            ptb.text(")", null)
            renderResultTypes(psiElement, ptb, signature.result, sym)
        }

        private fun renderParamDecls(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            sym: GolangIJSettings.State,
            paramDecls: List<GoParameterDeclaration>,
        ) {
            paramDecls.forEachIndexed { i, paramDecl ->
                if (i > 0) ptb.text(sym.separatorStyle.symbol, null)
                if (paramDecl.isVariadic) ptb.text(sym.ellipsisStyle.symbol, null)
                paramDecl.type?.let { renderGoType(psiElement, ptb, it, sym) }
            }
        }

        private fun renderResultTypes(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            result: GoResult?,
            sym: GolangIJSettings.State,
        ) {
            if (result == null) return
            ptb.text(" ", null)
            val resultType = result.type
            if (resultType != null) {
                renderGoType(psiElement, ptb, resultType, sym)
            } else {
                val resultParams = result.parameters
                if (resultParams != null) {
                    ptb.text("(", null)
                    resultParams.parameterDeclarationList.forEachIndexed { i, decl ->
                        if (i > 0) ptb.text(sym.separatorStyle.symbol, null)
                        decl.type?.let { renderGoType(psiElement, ptb, it, sym) }
                    }
                    ptb.text(")", null)
                }
            }
        }

        private fun renderGoTypeTruncated(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            goType: GoType,
            sym: GolangIJSettings.State,
            maxLen: Int,
        ) {
            if (goType is GoFunctionType) {
                renderFunctionSignatureTruncated(psiElement, ptb, goType, sym)
            } else {
                val text = goType.presentationText
                val cutAt = maxOf(3, maxLen - sym.ellipsisStyle.symbol.length)
                ptb.text(text.substring(0, cutAt), createNavigationActionData(psiElement, goType))
                ptb.text(sym.ellipsisStyle.symbol, null)
            }
        }

        private fun renderFunctionSignatureTruncated(
            psiElement: PsiElement,
            ptb: PresentationTreeBuilder,
            funcType: GoFunctionType,
            sym: GolangIJSettings.State,
        ) {
            val signature = funcType.signature
            ptb.text("func", null)

            if (signature == null) {
                ptb.text("(${sym.ellipsisStyle.symbol})", null)
                return
            }

            val paramDecls = signature.parameters.parameterDeclarationList

            ptb.text("(", null)
            if (paramDecls.size <= 2) {
                renderParamDecls(psiElement, ptb, sym, paramDecls)
            } else {
                val first = paramDecls.first()
                if (first.isVariadic) ptb.text(sym.ellipsisStyle.symbol, null)
                first.type?.let { renderGoType(psiElement, ptb, it, sym) }

                val elided = paramDecls.size - 2
                ptb.text("${sym.separatorStyle.symbol}${sym.ellipsisStyle.symbol}$elided more${sym.separatorStyle.symbol}", null)

                val last = paramDecls.last()
                if (last.isVariadic) ptb.text(sym.ellipsisStyle.symbol, null)
                last.type?.let { renderGoType(psiElement, ptb, it, sym) }
            }
            ptb.text(")", null)

            renderResultTypes(psiElement, ptb, signature.result, sym)
        }

        private fun createNavigationActionData(psiElement: PsiElement, goType: GoType): InlayActionData? {
            val navigationTarget = getGoTypeRecursive(goType)
            return try {
                InlayActionData(
                    PsiPointerInlayActionPayload(
                        SmartPointerManager.getInstance(psiElement.project)
                            .createSmartPsiElementPointer(
                                navigationTarget.contextlessUnderlyingType.navigationElement
                            )
                    ),
                    PsiPointerInlayActionNavigationHandler.HANDLER_ID,
                )
            } catch (e: Exception) {
                log.debug("Could not create navigation for type: {}", goType.text, e)
                null
            }
        }

        companion object {
            private val log = LoggerFactory.getLogger(GolangIJInlayHintsProvider::class.java)

            private fun getGoTypeRecursive(goType: GoType): GoType = when (goType) {
                is GoArrayOrSliceType -> getGoTypeRecursive(goType.type)
                is GoPointerType -> goType.type?.let { getGoTypeRecursive(it) } ?: goType
                is GoChannelType -> goType.type?.let { getGoTypeRecursive(it) } ?: goType
                else -> goType
            }

            private fun resolveGoTypes(stream: java.util.stream.Stream<out GoTypeOwner>): List<GoType> =
                stream.toList().mapNotNull { it.getGoType(null) }.flatMap { goType ->
                    if (goType is GoLightType.LightTypeList) goType.typeList else listOf(goType)
                }
        }
    }

    companion object {
        @Suppress("unused")
        const val PROVIDER_ID: String = "golangij.provider.inlayHints"
    }
}
