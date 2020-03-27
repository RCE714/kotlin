/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

sealed class FirModifier<Node : Any>(val node: Node, val token: KtModifierKeywordToken)

class FirPsiModifier(
    node: ASTNode,
    token: KtModifierKeywordToken
) : FirModifier<ASTNode>(node, token)

class FirLightModifier(
    node: LighterASTNode,
    token: KtModifierKeywordToken,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>
) : FirModifier<LighterASTNode>(node, token)

val FirModifier<*>.psi: PsiElement? get() = (this as? FirPsiModifier)?.node?.psi

val FirModifier<*>.lightNode: LighterASTNode? get() = (this as? FirLightModifier)?.node

val FirModifier<*>.source: FirSourceElement?
    get() = when (this) {
        is FirPsiModifier -> this.psi?.toFirPsiSourceElement()
        is FirLightModifier -> {
            // TODO pretty sure I got offsets wrong here
            val startOffset = tree.getStartOffset(node)
            val endOffset = tree.getEndOffset(node)
            node.toFirLightSourceElement(startOffset, endOffset, tree)
        }
    }