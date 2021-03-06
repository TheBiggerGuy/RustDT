/*******************************************************************************
 * Copyright (c) 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.github.rustdt.ide.ui.preferences;

import org.eclipse.core.resources.IProject;

import melnorme.lang.ide.core.operations.ToolchainPreferences;
import melnorme.lang.ide.ui.preferences.ProjectSDKSettingsBlock;
import melnorme.lang.ide.ui.preferences.common.AbstractProjectToolchainSettingsPage;

public class RustToolchainConfigurationPage extends AbstractProjectToolchainSettingsPage {
	
	@Override
	protected ProjectSDKSettingsBlock createProjectConfigWidget(IProject project) {
		return new ProjectSDKSettingsBlock(project, ToolchainPreferences.USE_PROJECT_SETTINGS) {
			
			@Override
			protected RustToolsConfigBlock init_createProjectSettingsBlock2() {
				return new RustToolsConfigBlock(prefContext);
			}
			
		};
	}
	
}