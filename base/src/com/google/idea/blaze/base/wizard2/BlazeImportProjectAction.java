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
package com.google.idea.blaze.base.wizard2;

import com.google.idea.blaze.base.settings.Blaze;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

class BlazeImportProjectAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    BlazeNewProjectWizard wizard =
        new BlazeNewProjectWizard() {
          @Override
          protected ProjectImportWizardStep[] getSteps(WizardContext context) {
            return new ProjectImportWizardStep[] {
              new BlazeSelectWorkspaceImportWizardStep(context),
              new BlazeSelectBuildSystemBinaryStep(context),
              new BlazeSelectProjectViewImportWizardStep(context),
              new BlazeEditProjectViewImportWizardStep(context)
            };
          }
        };
    if (!wizard.showAndGet()) {
      return;
    }
    BlazeProjectCreator projectCreator = new BlazeProjectCreator(wizard.context, wizard.builder);
    projectCreator.createFromWizard();
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation()
        .setText(String.format("Import %s Project...", Blaze.defaultBuildSystemName()));
  }
}
