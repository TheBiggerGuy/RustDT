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
package com.github.rustdt.tooling.ops;

import java.nio.file.Path;

import melnorme.lang.tooling.completion.LangCompletionResult;
import melnorme.lang.tooling.toolchain.ops.IToolOperationService;
import melnorme.lang.tooling.toolchain.ops.ToolOpResult;
import melnorme.lang.utils.parse.StringCharSource;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.fields.validation.ValidatedValueSource;
import melnorme.utilbox.misc.Location;

public class RacerCompletionOperation extends RacerOperation<LangCompletionResult> {
	
	protected final int offset;
	
	public RacerCompletionOperation(IToolOperationService opHelper, 
			ValidatedValueSource<Path> racerPath, ValidatedValueSource<Location> sdkSrcLocation, 
			String source, boolean useSubstituteFile, int offset, int line_0, int col_0, Location fileLocation) {
		super(opHelper, racerPath, sdkSrcLocation, source, useSubstituteFile, 
			getArguments("complete-with-snippet", line_0, col_0, fileLocation));
		
		this.offset = offset;
	}
	
	@Override
	public ToolOpResult<LangCompletionResult> parseProcessOutput(StringCharSource outputParseSource) 
			throws CommonException {
		LangCompletionResult resultaData = createRacerOutputParser(offset).parse(outputParseSource);
		return new ToolOpResult<>(resultaData);
	}
	
	@Override
	protected String getToolProcessName() {
		return getToolName();
	}
	
}