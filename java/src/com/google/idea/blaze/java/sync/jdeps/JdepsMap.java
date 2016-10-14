/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.java.sync.jdeps;

import com.google.idea.blaze.base.ideinfo.RuleKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Map of rule -> jdeps dependencies. */
public interface JdepsMap {
  /**
   * For a given rule, returns workspace root relative paths of artifacts that were used during
   * compilation.
   *
   * <p>It's not specified whether jars or ijars are used during compilation.
   *
   * <p>If the rule doesn't have source or otherwise wasn't instrumented, null is returned.
   */
  @Nullable
  List<String> getDependenciesForRule(RuleKey ruleKey);
}
