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
package com.google.idea.blaze.ijwb.android;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.ideinfo.AndroidSdkIdeInfo;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.LibraryArtifact;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetMap;
import com.google.idea.blaze.base.model.BlazeLibrary;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.LanguageClass;
import com.google.idea.blaze.base.model.primitives.WorkspaceType;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.sync.BlazeSyncPlugin;
import com.google.idea.blaze.base.sync.libraries.LibrarySource;
import com.google.idea.blaze.java.sync.model.BlazeJarLibrary;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Rudimentary support for android in IntelliJ. */
public class BlazeAndroidLiteSyncPlugin implements BlazeSyncPlugin {
  @Override
  public Set<LanguageClass> getSupportedLanguagesInWorkspace(WorkspaceType workspaceType) {
    switch (workspaceType) {
      case ANDROID:
      case JAVA:
        return ImmutableSet.of(LanguageClass.ANDROID);
      default:
        return ImmutableSet.of();
    }
  }

  @Nullable
  @Override
  public LibrarySource getLibrarySource(
      ProjectViewSet projectViewSet, BlazeProjectData blazeProjectData) {
    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.ANDROID)) {
      return null;
    }
    BlazeLibrary sdkLibrary = getSdkLibrary(blazeProjectData);
    if (sdkLibrary == null) {
      return null;
    }
    return new LibrarySource.Adapter() {
      @Override
      public List<? extends BlazeLibrary> getLibraries() {
        return ImmutableList.of(sdkLibrary);
      }
    };
  }

  @Nullable
  private static BlazeLibrary getSdkLibrary(BlazeProjectData blazeProjectData) {
    TargetIdeInfo sdkTarget = getAndroidSdkTarget(blazeProjectData.getTargetMap());
    if (sdkTarget == null) {
      return null;
    }
    return sdkTarget != null
        ? new BlazeJarLibrary(
            new LibraryArtifact(null, getAndroidSdkJar(sdkTarget), ImmutableList.of()),
            sdkTarget.getKey())
        : null;
  }

  @Nullable
  private static TargetIdeInfo getAndroidSdkTarget(TargetMap targetMap) {
    // for now, just add the first one found
    // TODO: warn if there's more than one
    return targetMap.targets().stream()
        .filter(target -> getAndroidSdkJar(target) != null)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static ArtifactLocation getAndroidSdkJar(TargetIdeInfo target) {
    AndroidSdkIdeInfo sdkIdeInfo = target.getAndroidSdkIdeInfo();
    return sdkIdeInfo != null ? sdkIdeInfo.getAndroidJar() : null;
  }
}
