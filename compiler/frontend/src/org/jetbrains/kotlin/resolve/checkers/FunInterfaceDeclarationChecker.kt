/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.sam.getAbstractMembers
import org.jetbrains.kotlin.resolve.source.getPsi

class FunInterfaceDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        if (descriptor !is ClassDescriptor || !descriptor.isFun) return

        val funKeyword = declaration.getFunKeyword() ?: declaration.getClassKeyword() ?: return

        val abstractMembers = getAbstractMembers(descriptor)
        for (abstractMember in abstractMembers) {
            if (abstractMember !is PropertyDescriptor) continue

            val reportOnProperty = abstractMember.containingDeclaration == descriptor
            val reportOn = if (reportOnProperty) {
                (abstractMember.source.getPsi() as? KtProperty)?.valOrVarKeyword ?: funKeyword
            } else {
                funKeyword
            }

            context.trace.report(Errors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES.on(reportOn))

            if (!reportOnProperty) return // It's enough to report only once if abstract properties are in the base class
        }

        val abstractMember = abstractMembers.singleOrNull()

        if (abstractMember == null) {
            context.trace.report(Errors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS.on(funKeyword))
            return
        }

        if (abstractMember !is FunctionDescriptor) return // abstract properties were checked earlier

        checkSingleAbstractMember(abstractMember, declaration, context)
    }

    private fun checkSingleAbstractMember(
        abstractMember: FunctionDescriptor,
        ktFunInterface: KtClass,
        context: DeclarationCheckerContext,
    ) {
        val funInterfaceKeyword = ktFunInterface.getFunKeyword()
        val ktFunction = abstractMember.source.getPsi() as? KtNamedFunction
        if (abstractMember.typeParameters.isNotEmpty()) {
            val reportOn = ktFunction?.typeParameterList ?: ktFunction?.funKeyword ?: funInterfaceKeyword ?: return
            context.trace.report(Errors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS.on(reportOn))
            return
        }
    }
}

