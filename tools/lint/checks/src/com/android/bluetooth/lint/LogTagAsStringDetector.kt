/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass

/**
 * Lint check for creation of string constant based log tags.
 *
 * Logging tags are expected to be of the following strict format
 *
 *     private static final String TAG = <class>.class.getSimpleName();
 *
 * The recommended fix is to set the tag based on the formatting above
 */
class LogTagAsStringDetector : Detector(), SourceCodeScanner {
    private val LOG_TAG_VARS = listOf("TAG", "mTag", "sTag")
    private val EXPECTED_LOG_TAG_VAR = "TAG"

    companion object {
        const val LOG_TAG_FORMATTING_WARNING =
            "Logging tags should be named TAG, be static and final, and match the name of the" +
            " class they're created for."

        val ISSUE =
            Issue.create(
                id = "LogTagDoesNotMatchClassname",
                briefDescription = LOG_TAG_FORMATTING_WARNING,
                explanation =
                    "Using a logging tag that matches a given class name makes finding log lines" +
                    " simple and consistent. This, together with asserting that Log framework" +
                    " calls always use a static TAG variable, will also make sure classes always" +
                    " log to the same tag.",
                category = Category.CORRECTNESS,
                severity = Severity.WARNING,
                implementation =
                    Implementation(LogTagAsStringDetector::class.java, Scope.JAVA_FILE_SCOPE),
                androidSpecific = true,
            )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UField::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitField(node: UField) {
                val containingClass = node.getContainingUClass() ?: return
                if (
                    !isBluetoothClass(containingClass) ||
                        node.getType().canonicalText != "java.lang.String" ||
                        !(node.getName() in LOG_TAG_VARS)
                ) {
                    return
                }

                if (
                    !node.isStatic ||
                        !node.isFinal ||
                        node.getName() != EXPECTED_LOG_TAG_VAR ||
                        !checkTagInitializerFormat(containingClass, node.uastInitializer)
                ) {
                    val className = containingClass.javaPsi.name ?: return
                    val expectedValue =
                        "private static final String TAG = $className.class.getSimpleName();"
                    context.report(
                        issue = ISSUE,
                        scopeClass = node,
                        location = context.getNameLocation(node),
                        message = LOG_TAG_FORMATTING_WARNING,
                        quickfixData =
                            LintFix.create()
                                .name("Update log tag initialization")
                                .replace()
                                .range(context.getLocation(node))
                                .with(expectedValue)
                                .build()
                    )
                }
            }
        }
    }

    private fun checkTagInitializerFormat(owningClass: UClass, expression: UExpression?): Boolean {
        if (expression == null) {
            return false
        }

        when (expression) {
            is ULiteralExpression -> {
                val className = owningClass.javaPsi.name ?: return false
                return expression.isString && expression.value == className
            }
            is UParenthesizedExpression -> {
                return checkTagInitializerFormat(owningClass, expression.expression)
            }
            is UQualifiedReferenceExpression -> {
                return isClassLiteralForOwningClass(owningClass, expression.receiver) &&
                    isCallClassGetSimpleName(expression.selector)
            }
        }
        return false
    }

    private fun isClassLiteralForOwningClass(
        owningClass: UClass,
        expression: UExpression
    ): Boolean {
        return (expression is UClassLiteralExpression) &&
            expression.type?.getCanonicalText() == owningClass.qualifiedName
    }

    private fun isCallClassGetSimpleName(expression: UExpression): Boolean {
        if (!(expression is UCallExpression)) {
            return false
        }
        val resolvedMethod = expression.resolve()
        return resolvedMethod?.name == "getSimpleName" &&
            resolvedMethod.containingClass?.qualifiedName == "java.lang.Class"
    }
}
