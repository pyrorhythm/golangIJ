package com.pyro.golangij

import com.goide.psi.*
import com.goide.psi.impl.GoLightType
import com.goide.psi.impl.GoPsiImplUtil.ChannelDirection.*
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPointerManager
import groovy.lang.Tuple2
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
                    if (element.type != null) return
                    varDefs = element.varDefinitionList
                    resolvedTypes = resolveGoTypes(element.expressionList.stream())
                }

                is GoConstSpec -> {
                    if (element.type != null) return
                    varDefs = element.constDefinitionList
                    resolvedTypes = resolveGoTypes(element.expressionList.stream())
                }
            }

            if (resolvedTypes.size != varDefs.size) {
                log.debug("Type count {} != defs count {} in: {}", resolvedTypes.size, varDefs.size, element.text)
                return
            }

            val sym = GolangIJSettings.getInstance().state
            varDefs.zip(resolvedTypes).forEach { (varDef, goType) ->
                if (varDef.name == "_") return@forEach

                val text = buildTypeText(goType, sym)
                var displayText = truncate(text, goType, sym)

                if (displayText == "") return@forEach
                if (sym.insertSpaceOnLeft) displayText = " $displayText"

                sink.addPresentation(
                    InlineInlayPosition(varDef.textRange.endOffset, true, 0),
                    null,
                    text,
                    HintFormat.default,
                ) {
                    text(displayText, createNavAction(element, goType))
                }
            }
        }

        private fun buildTypeText(goType: GoType, sym: GolangIJSettings.State): String {
            return when (goType) {
                is GoMapType -> buildMapTypeText(goType, sym)
                is GoArrayOrSliceType -> buildArrSlcTypeText(goType, sym)
                is GoPointerType -> buildPointerTypeText(goType, sym)
                is GoChannelType -> buildChanText(goType, sym)
                is GoFunctionType -> bindFuncTypeText(goType, sym)
                is GoSpecType -> buildSpecTypeText(goType, sym)
                else -> buildUnkTypeText(goType, sym)
            }
        }

        private fun buildUnkTypeText(goType: GoType, sym: GolangIJSettings.State): String = buildString {
            append(goType.typeReferenceExpression?.text ?: goType.presentationText)
            goType.typeArguments?.let { append(buildTypeArgsText(it, sym)) }
        }

        private fun buildSpecTypeText(goType: GoSpecType, sym: GolangIJSettings.State): String = buildString {
            append(goType.identifier.text)
            goType.typeArguments?.let { append(buildTypeArgsText(it, sym)) }
        }

        private fun bindFuncTypeText(goType: GoFunctionType, sym: GolangIJSettings.State): String = buildString {
            append(sym.funcLiteralStyle.symbol)
                .append(buildFuncSigText(goType, sym))
        }

        private fun buildPointerTypeText(goType: GoPointerType, sym: GolangIJSettings.State): String = buildString {
            append(sym.pointerStyle.symbol)
            goType.type?.let { append(buildTypeText(it, sym)) }
        }

        private fun buildArrSlcTypeText(
            goType: GoArrayOrSliceType,
            sym: GolangIJSettings.State
        ): String = buildString {
            append("[")
                .append(if (goType.isArray) goType.length else sym.ellipsisStyle.symbol)
                .append("]")
                .append(buildTypeText(goType.type, sym))
        }

        private fun buildMapTypeText(goType: GoMapType, sym: GolangIJSettings.State): String = buildString {
            append("map[")
            goType.keyType?.let { append(buildTypeText(it, sym)) }
            append("]")
            goType.valueType?.let { append(buildTypeText(it, sym)) }
        }

        private fun buildChanText(chan: GoChannelType, sym: GolangIJSettings.State): String = buildString {
            append(when (chan.direction) {
                SEND -> sym.chanStyle.sendChan
                RECEIVE -> sym.chanStyle.recvChan
                BIDIRECTIONAL -> sym.chanStyle.biChan
            })

            if (chan.type != null) {
                append(sym.chanTypeBracketsStyle.left)
                append(buildTypeText(chan.type!!, sym))
                append(sym.chanTypeBracketsStyle.right)
            }
        }

        private fun buildFuncSigText(funcType: GoFunctionType, sym: GolangIJSettings.State): String {
            val sig = funcType.signature ?: return "(${sym.ellipsisStyle.symbol})"
            return buildString {
                sig.typeParameters?.let { append(buildTypeParamsText(it, sym)) }
                append("(")
                append(buildParamDeclsText(sig.parameters.parameterDeclarationList, sym))
                append(")")
                sig.result?.let { append(buildResultText(it, sym)) }
            }
        }

        private fun buildTypeParamsText(typeParams: GoTypeParameters, sym: GolangIJSettings.State): String = typeParams
            .typeParameterDeclarationList.map {
                Tuple2(
                    it.type,
                    it.typeParamDefinitionList
                        .filter { !it.name.isNullOrEmpty() }
                        .joinToString(", ") { it.name.toString() })
            }
            .joinToString(
                separator = sym.separatorStyle.symbol,
                prefix = sym.genericBracketStyle.open,
                postfix = sym.genericBracketStyle.close,
            ) { // join groups (X, Y, Z any; A, B, C comparable)
                val typ = it.v1

                it.v2 + if (typ != null) " " + buildTypeText(typ, sym) else ""
            }


        private fun buildTypeArgsText(typeArgs: GoTypeArguments, sym: GolangIJSettings.State): String =
            if (typeArgs.types.isEmpty()) "" else typeArgs.types.joinToString(
                separator = sym.separatorStyle.symbol,
                prefix = sym.genericBracketStyle.open,
                postfix = sym.genericBracketStyle.close,
            ) { buildTypeText(it, sym) }

        private fun buildParamDeclsText(paramDecls: List<GoParameterDeclaration>, sym: GolangIJSettings.State): String =
            paramDecls.joinToString(sym.separatorStyle.symbol) {
                (if (it.isVariadic) sym.ellipsisStyle.symbol else "") +
                        it.type?.let { buildTypeText(it, sym) }.orEmpty()
            }

        private fun buildResultText(result: GoResult, sym: GolangIJSettings.State): String {
            result.type?.let { return " ${buildTypeText(it, sym)}" }
            val params = result.parameters?.parameterDeclarationList ?: return ""
            if (params.isEmpty()) return ""
            return " (${buildParamDeclsText(params, sym)})"
        }

        private fun truncate(text: String, goType: GoType, sym: GolangIJSettings.State): String {
            if (sym.maxHintLength <= 0) return text
            if (text.length <= sym.maxHintLength) return text
            if (goType is GoFunctionType) return truncateFuncSig(goType, sym)

            val cutAt = maxOf(3, sym.maxHintLength - sym.ellipsisStyle.symbol.length)
            return text.substring(0, cutAt) + sym.ellipsisStyle.symbol
        }

        private fun truncateFuncSig(funcType: GoFunctionType, sym: GolangIJSettings.State): String {
            val sig = funcType.signature ?: return "${sym.funcLiteralStyle.symbol}(${sym.ellipsisStyle.symbol})"
            val paramDecls = sig.parameters.parameterDeclarationList

            val params = if (paramDecls.size <= 2) {
                buildParamDeclsText(paramDecls, sym)
            } else {
                val first = (if (paramDecls.first().isVariadic) sym.ellipsisStyle.symbol else "") +
                        (paramDecls.first().type?.let { buildTypeText(it, sym) } ?: "")
                val last = (if (paramDecls.last().isVariadic) sym.ellipsisStyle.symbol else "") +
                        (paramDecls.last().type?.let { buildTypeText(it, sym) } ?: "")
                val elided = paramDecls.size - 2
                "$first${sym.separatorStyle.symbol}${sym.ellipsisStyle.symbol}$elided more${sym.separatorStyle.symbol}$last"
            }

            val result = sig.result?.let { buildResultText(it, sym) } ?: ""
            return "${sym.funcLiteralStyle.symbol}($params)$result"
        }

        private fun createNavAction(psiElement: PsiElement, goType: GoType): InlayActionData? {
            val target = getGoTypeRecursive(goType)
            return try {
                InlayActionData(
                    PsiPointerInlayActionPayload(
                        SmartPointerManager.getInstance(psiElement.project)
                            .createSmartPsiElementPointer(target.contextlessUnderlyingType.navigationElement)
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