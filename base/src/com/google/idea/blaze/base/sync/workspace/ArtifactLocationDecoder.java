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
package com.google.idea.blaze.base.sync.workspace;

import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Decodes android_studio_ide_info.proto ArtifactLocation file paths */
public interface ArtifactLocationDecoder extends Serializable {

  File decode(ArtifactLocation artifactLocation);

  default List<File> decodeAll(Collection<ArtifactLocation> artifactLocations) {
    return artifactLocations.stream().map(this::decode).collect(Collectors.toList());
  }
}
