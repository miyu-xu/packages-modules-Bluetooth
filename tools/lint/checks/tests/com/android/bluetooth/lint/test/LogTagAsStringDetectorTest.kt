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

package com.android.bluetooth.lint.test

import com.android.bluetooth.lint.LogTagAsStringDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UnstableApiUsage")
@RunWith(JUnit4::class)
class LogTagAsStringDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LogTagAsStringDetector()

    override fun getIssues(): List<Issue> = listOf(LogTagAsStringDetector.ISSUE)

    override fun lint(): TestLintTask = super.lint().allowMissingSdk(true)

    @Test
    fun testCreateTagWithGetSimpleName_noIssuesFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String TAG = Foo.class.getSimpleName();
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testCreateTagWithStringConstantName_noIssueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String TAG = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testCreateTagWithMultiLineStringConstantName_noIssueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String TAG =
                            "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testCreateNonTagFields_noIssuesFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String sName = "Foo";
                    private final Boolean mBar = false;
                    private static final int SOME_CONSTANT = 0;
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testCreateTagWithNonClassStringConstantName_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String TAG = "FooDifferent";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "    private static final String TAG = \"Foo\";"))
    }

    @Test
    fun testCreateTagWithMultiLineStringConstantName_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String TAG =
                            "FooDifferent";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "private static final String TAG = \"Foo\";"))
    }

    @Test
    fun testCreateNonFinalTag_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static String TAG = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "private static final String TAG = \"Foo\";"))
    }

    @Test
    fun testCreateNonStaticTag_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private final String TAG = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "private static final String TAG = \"Foo\";"))
    }

    @Test
    fun testCreateNonStaticNonFinalTag_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private String TAG = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "private static final String TAG = \"Foo\";"))
    }

    @Test
    fun testCreateMemberTagWithStringConstantName_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private final String mTag = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "    private final String mTag = \"Foo\";"))
    }

    @Test
    fun testCreateStaticTagWithStringConstantName_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                public final class Foo {
                    private static final String sTag = "Foo";
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "    private static final String sTag = \"Foo\";"))
    }

    @Test
    fun testCreateTagFromConstantsFile_issueFound() {
        lint()
            .files(
                java(
                        """
                package com.android.bluetooth;

                import com.android.foo.FooConstants;

                public final class Foo {
                    private static final String sTag = FooConstants.TAG;
                }
                """
                    )
                    .indented(),
                *stubs
            )
            .issues(LogTagAsStringDetector.ISSUE)
            .run()
            .expectContains(LogTagAsStringDetector.LOG_TAG_FORMATTING_WARNING)
            .expectContains(createErrorCountString(0, 1))
        // .expectFixDiffs(createFixDiff(4, "    private static final String sTag = \"Foo\";"))
    }

    private val constantsHelper: TestFile =
        java(
                """
                package com.android.foo;

                public class FooConstants {
                    public static final String TAG = "FooConstants";
                }
                """
            )
            .indented()

    private val stubs = arrayOf(constantsHelper)

    private fun createErrorCountString(errors: Int, warnings: Int): String {
        return "%d errors, %d warnings".format(errors, warnings)
    }

    private fun createFixDiff(lineNumber: Int, lines: String): String {
        // All lines are removed. Add enough spaces to match the below indenting
        val minusedlines = lines.replace("\n ", "\n               -  ")
        return """
               Fix for src/com/android/bluetooth/Foo.java line $lineNumber: Update log tag initialization:
               @@ -$lineNumber +$lineNumber
               - $minusedlines
               -
               """
            .trimIndent()
    }
}
