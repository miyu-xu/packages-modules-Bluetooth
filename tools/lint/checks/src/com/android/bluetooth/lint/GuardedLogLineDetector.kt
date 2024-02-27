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
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UUnaryExpression

/**
 * Lint check for guarded log lines
 *
 * Logging enforcement variables are not allowed to be _used_. i.e.:
 *
 *     if (DBG) {
 *         Log.d(TAG, "message");
 *     }
 *     if (Log.isLoggable(TAG, Log.DEBUG)) {
 *         Log.d(TAG, "message");
 *     }
 *     if (foo != null) {
 *         // ...
 *     } else if (DBG) {
 *         Log.d(TAG, "foo was null");
 *     }
 *     if (!DBG) {
 *         // ...
 *     } else {
 *         Log.d(TAG, "foo was null");
 *     }
 *     if (DBG) {
 *         if (foo != null) {
 *             Log.d(TAG, "foo was null");
 *         }
 *     }
 */
class GuardedLogLineDetector : Detector(), SourceCodeScanner {
    private val LOG_ENFORCEMENT_VARS = listOf("DBG", "DEBUG", "VDBG", "VERBOSE", "D", "V")
    private val LOG_ENFORCEMENT_VAR_ENDINGS = listOf("_DBG", "_VDBG")
    private val LOG_LOGGING_FUNCTIONS = listOf("wtf", "e", "w", "i", "d", "v")

    companion object {
        const val GUARDED_LOG_INVOCATION_ERROR =
            "Dont guard log invocations with if blocks using log enforcement variables or" +
            " isLoggable(). The Log framework does this check for you. Remove the surrounding if" +
            " block and call to log completely unguarded"

        val ISSUE =
            Issue.create(
                id = "GuardedLogInvocation",
                briefDescription =
                    "Dont guard log invocations with if blocks using log enforcement variables" +
                    "or isLoggable()",
                explanation =
                    "The BT stack defines a process default log level, which allows the Android" +
                    " Log framework (For Java, Kotlin, _and_ Native) to properly enforce log" +
                    " levels for us. Using our own variables for enforcement causes inconsistency" +
                    " in log output and double checks against the log level each time we log." +
                    " Please delete this variable and use the Log functions unguarded in your" +
                    " code.",
                category = Category.CORRECTNESS,
                severity = Severity.ERROR,
                implementation =
                    Implementation(GuardedLogLineDetector::class.java, Scope.JAVA_FILE_SCOPE),
                androidSpecific = true,
            )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val callingClass = findOwningUClass(node)
                if (!isBluetoothClass(callingClass)) {
                    return
                }

                val ifStatement = findContainingUIfExpression(node) ?: return
                if (
                    isLoggingFunction(node) && isExpressionWithLogEnforcement(ifStatement.condition)
                ) {
                    debug(
                        "GuardedLogLineDetector",
                        "[ERROR] Log call with level enforcement, node=$ifStatement".replace(
                            "\n",
                            " "
                        ),
                        "    "
                    )
                    context.report(
                        issue = ISSUE,
                        location = context.getNameLocation(ifStatement),
                        message = GUARDED_LOG_INVOCATION_ERROR,
                    )
                } else if (isLoggingFunction(node)) {
                    debug(
                        "GuardedLogLineDetector",
                        "[SAFE] Log call, node=$ifStatement".replace("\n", " "),
                        "    "
                    )
                }
            }
        }
    }

    /** Traverse the element tree upward to find the closest UClass to a given expression */
    private fun findOwningUClass(node: UElement?): UClass? {
        if (node == null) {
            return null
        }

        if (node is UClass) {
            return node
        }

        return findOwningUClass(node.uastParent)
    }

    /*
     * Returns the most recent parent IfExpression for a given expression, or null if the expression
     * is not contained in an IfExpression
     */
    private fun findContainingUIfExpression(node: UElement?): UIfExpression? {
        if (node == null) {
            return null
        }

        if (node is UIfExpression) {
            return node
        }

        return findContainingUIfExpression(node.uastParent)
    }

    /*
     * Determines if the given Expression contains any usages of Log.isLoggable, or any variables
     * that are likely log enforcement variables
     */
    private fun isExpressionWithLogEnforcement(node: UExpression): Boolean {
        when (node) {
            // A simple class or local variable reference, i.e. "DBG" or "VDBG"
            is USimpleNameReferenceExpression -> {
                return isLogEnforcementVariable(node.identifier)
            }

            // An actual function call, i.e "isLoggable()" part of Log.isLoggable()
            is UCallExpression -> {
                return isLoggableFunction(node)
            }

            // A unary operation on another expression, i.e. "!DBG" or "!Log.isLoggable()""
            is UUnaryExpression -> {
                return isExpressionWithLogEnforcement(node.operand)
            }

            // A binary operation on another expression, i.e. "DBG || Log.isLoggable()"
            is UBinaryExpression -> {
                return isExpressionWithLogEnforcement(node.leftOperand) ||
                    isExpressionWithLogEnforcement(node.rightOperand)
            }

            // A conditional expression with multiple operators, i.e. "mFoo || DBG && i < 6"
            is UPolyadicExpression -> {
                for (subExpression in node.operands) {
                    if (isExpressionWithLogEnforcement(subExpression)) {
                        return true
                    }
                }
                return false
            }

            // A function compound call, i.e. "Log.isLoggable()""
            is UQualifiedReferenceExpression -> {
                return isExpressionWithLogEnforcement(node.selector)
            }

            // An expression surrounded by parenthesis, i.e. "(DBG || Log.isLoggable())"
            is UParenthesizedExpression -> {
                return isExpressionWithLogEnforcement(node.expression)
            }
        }
        return false
    }

    /*
     * Determines if the given call is one to any of the various Log framework calls that write a
     * log line to logcat, i.e. wtf, e, w, i, d, or v
     */
    private fun isLoggingFunction(node: UCallExpression): Boolean {
        val resolvedMethod = node.resolve()
        val methodClassName = resolvedMethod?.containingClass?.qualifiedName
        val methodName = resolvedMethod?.name
        return methodClassName == "android.util.Log" && methodName in LOG_LOGGING_FUNCTIONS
    }

    /** Determines if the given call is one to Log.isLoggable() */
    private fun isLoggableFunction(node: UCallExpression): Boolean {
        val resolvedMethod = node.resolve()
        val methodClassName = resolvedMethod?.containingClass?.qualifiedName
        val methodName = resolvedMethod?.name
        return methodClassName == "android.util.Log" && methodName == "isLoggable"
    }

    /*
     * Checks a string variable name to see if its one of the common names used in the stack for
     * log enforcement variables.
     *
     * These include things like DBG, VDBG, DEBUG, VERBOSE, D, V, etc., or variables that _end_ in
     * _DBG or _VDBG
     */
    private fun isLogEnforcementVariable(name: String): Boolean {
        val nameUpper = name.uppercase()
        return nameUpper in LOG_ENFORCEMENT_VARS ||
            LOG_ENFORCEMENT_VAR_ENDINGS.any { nameUpper.endsWith(it) }
    }
}
