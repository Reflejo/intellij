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
package com.google.idea.blaze.base.sync.libraries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.idea.blaze.base.model.BlazeLibrary;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.LibraryKey;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.scope.output.PrintOutput;
import com.google.idea.blaze.base.sync.BlazeSyncPlugin;
import com.google.idea.blaze.base.sync.workspace.ArtifactLocationDecoder;
import com.google.idea.sdkcompat.general.BaseSdkCompat;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Edits IntelliJ libraries */
public class LibraryEditor {
  private static final Logger logger = Logger.getInstance(LibraryEditor.class);

  public static void updateProjectLibraries(
      Project project,
      BlazeContext context,
      ProjectViewSet projectViewSet,
      BlazeProjectData blazeProjectData,
      Collection<BlazeLibrary> libraries) {
    Set<LibraryKey> intelliJLibraryState = Sets.newHashSet();
    IdeModifiableModelsProvider modelsProvider =
        BaseSdkCompat.createModifiableModelsProvider(project);
    for (Library library : modelsProvider.getAllLibraries()) {
      String name = library.getName();
      if (name != null) {
        intelliJLibraryState.add(LibraryKey.fromIntelliJLibraryName(name));
      }
    }
    context.output(PrintOutput.log(String.format("Workspace has %d libraries", libraries.size())));

    try {
      for (BlazeLibrary library : libraries) {
        updateLibrary(
            project, blazeProjectData.getArtifactLocationDecoder(), modelsProvider, library);
      }

      // Garbage collect unused libraries
      List<LibrarySource> librarySources = Lists.newArrayList();
      for (BlazeSyncPlugin syncPlugin : BlazeSyncPlugin.EP_NAME.getExtensions()) {
        LibrarySource librarySource = syncPlugin.getLibrarySource(projectViewSet, blazeProjectData);
        if (librarySource != null) {
          librarySources.add(librarySource);
        }
      }
      Predicate<Library> gcRetentionFilter =
          librarySources.stream()
              .map(LibrarySource::getGcRetentionFilter)
              .filter(Objects::nonNull)
              .reduce(Predicate::or)
              .orElse(o -> false);

      Set<LibraryKey> newLibraryKeys =
          libraries.stream().map((blazeLibrary) -> blazeLibrary.key).collect(Collectors.toSet());
      for (LibraryKey libraryKey : intelliJLibraryState) {
        String libraryIntellijName = libraryKey.getIntelliJLibraryName();
        if (!newLibraryKeys.contains(libraryKey)) {
          Library library = modelsProvider.getLibraryByName(libraryIntellijName);
          if (!gcRetentionFilter.test(library)) {
            if (library != null) {
              modelsProvider.removeLibrary(library);
            }
          }
        }
      }
    } finally {
      modelsProvider.commit();
    }
  }

  /**
   * Updates the library in IntelliJ's project model.
   *
   * <p>Note: Callers of this method must invoke {@link IdeModifiableModelsProvider#commit()} on the
   * passed {@link IdeModifiableModelsProvider} for any changes to take effect. Be aware that {@code
   * commit()} should only be called once after all modifications as frequent calls can be slow.
   *
   * @param project the IntelliJ project
   * @param artifactLocationDecoder a decoder to determine the location of artifacts
   * @param modelsProvider a modifier for IntelliJ's project model which supports quick application
   *     of massive modifications to the project model
   * @param blazeLibrary the library which should be updated in the project context
   */
  public static void updateLibrary(
      Project project,
      ArtifactLocationDecoder artifactLocationDecoder,
      IdeModifiableModelsProvider modelsProvider,
      BlazeLibrary blazeLibrary) {
    String libraryName = blazeLibrary.key.getIntelliJLibraryName();

    Library library = modelsProvider.getLibraryByName(libraryName);
    boolean libraryExists = library != null;
    if (!libraryExists) {
      library = modelsProvider.createLibrary(libraryName);
    }
    Library.ModifiableModel libraryModel = modelsProvider.getModifiableLibraryModel(library);
    if (libraryExists) {
      for (String url : libraryModel.getUrls(OrderRootType.CLASSES)) {
        libraryModel.removeRoot(url, OrderRootType.CLASSES);
      }
      for (String url : libraryModel.getUrls(OrderRootType.SOURCES)) {
        libraryModel.removeRoot(url, OrderRootType.SOURCES);
      }
    }
    blazeLibrary.modifyLibraryModel(project, artifactLocationDecoder, libraryModel);
  }

  /**
   * Configures the passed libraries as dependencies for the given root in IntelliJ's project model.
   * Libraries which don't exist in the project model will be ignored.
   *
   * <p>Note: Callers of this method must invoke {@code commit()} on the passed {@link
   * ModifiableRootModel} or on higher-level model providers for any changes to take effect. Be
   * aware that {@code commit()} should only be called once after all modifications as frequent
   * calls can be slow.
   *
   * @param modifiableRootModel a modifier for a specific root in IntelliJ's project model
   * @param libraries the libraries to add as dependencies
   */
  public static void configureDependencies(
      ModifiableRootModel modifiableRootModel, Collection<BlazeLibrary> libraries) {
    LibraryTable libraryTable =
        LibraryTablesRegistrar.getInstance().getLibraryTable(modifiableRootModel.getProject());

    ImmutableList<Library> foundLibraries = findLibraries(libraries, libraryTable);
    // Add the libraries in a batch operation as adding them one after the other is not performant.
    BaseSdkCompat.addLibraryEntriesToModel(modifiableRootModel, foundLibraries);
  }

  private static ImmutableList<Library> findLibraries(
      Collection<BlazeLibrary> libraries, LibraryTable libraryTable) {
    ImmutableList.Builder<Library> foundLibraries = ImmutableList.builder();
    ImmutableList.Builder<String> missingLibraries = ImmutableList.builder();
    for (BlazeLibrary library : libraries) {
      String libraryName = library.key.getIntelliJLibraryName();
      // This call is slow and causes freezes when done through IdeModifiableModelsProvider.
      Library foundLibrary = libraryTable.getLibraryByName(libraryName);
      if (foundLibrary == null) {
        missingLibraries.add(libraryName);
      } else {
        foundLibraries.add(foundLibrary);
      }
    }
    logMissingLibraries(missingLibraries.build());
    return foundLibraries.build();
  }

  private static void logMissingLibraries(Iterable<String> libraries) {
    String concatenatedLibraries = String.join(", ", libraries);
    if (!concatenatedLibraries.isEmpty()) {
      logger.error(
          "Some libraries are missing. Please resync the project to resolve. Missing libraries: %s",
          concatenatedLibraries);
    }
  }
}
