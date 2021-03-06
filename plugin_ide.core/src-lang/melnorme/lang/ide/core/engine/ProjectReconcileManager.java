/*******************************************************************************
 * Copyright (c) 2016 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.core.engine;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;

import melnorme.lang.ide.core.engine.SourceModelManager.StructureUpdateTask;
import melnorme.lang.ide.core.operations.ILangOperationsListener_Default.IToolOperationMonitor;
import melnorme.lang.ide.core.operations.ILangOperationsListener_Default.ProcessStartKind;
import melnorme.lang.ide.core.operations.build.BuildManager;
import melnorme.lang.tooling.common.ops.IOperationMonitor.NullOperationMonitor;
import melnorme.lang.utils.concurrency.MonitorFutureTask;
import melnorme.utilbox.concurrency.ICancelMonitor;
import melnorme.utilbox.concurrency.ICommonExecutor;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.concurrency.ResultFuture.LatchFuture;
import melnorme.utilbox.core.CommonException;

abstract class AbstractProjectReconcileManager {
	
	protected final ICommonExecutor executor;
	
	protected final Object projectInfosLock = new Object();
	protected final HashMap<IProject, ProjectReconcileTask> projectInfos = new HashMap<>();
	
	public AbstractProjectReconcileManager(ICommonExecutor executor) {
		this.executor = assertNotNull(executor);
	}
	
	public ICommonExecutor getExecutor() {
		return executor;
	}
	
	public void invalidateProjectModel(IProject project, StructureUpdateTask structureUpdateTask, 
			LatchFuture fileSaveFuture) {
		
		assertNotNull(fileSaveFuture);
		
		synchronized (projectInfosLock) {
			ProjectReconcileTask currentReconcileTask = projectInfos.get(project);
			
			// Cancel the previous task
			if(currentReconcileTask != null) {
				currentReconcileTask.cancel(); 
			}
			
			ProjectReconcileTask newReconcileTask = 
					new ProjectReconcileTask(project, currentReconcileTask, structureUpdateTask, fileSaveFuture);
			
			projectInfos.put(project, newReconcileTask);
			
			getExecutor().submitTask(newReconcileTask.asFutureTask());
		}
		
	}
	
	public void cancelPendingReconciliation(IProject project) {
		synchronized (projectInfosLock) {
			ProjectReconcileTask currentReconcileTask = projectInfos.get(project);
			
			// Cancel the previous task
			if(currentReconcileTask != null) {
				currentReconcileTask.cancel(); 
			}
		}
	}
	
	protected void removeProjectInfo(ProjectReconcileTask projectReconcileTask) {
		synchronized (projectInfosLock) {
			projectInfos.remove(projectReconcileTask.project, projectReconcileTask);
		}
	}
	
	public class ProjectReconcileTask extends MonitorFutureTask<RuntimeException> {
		
		protected final IProject project;
		protected final ProjectReconcileTask previousReconcileTask;
		protected final StructureUpdateTask structureUpdateTask;
		protected final LatchFuture fileSaveFuture;
		
		public ProjectReconcileTask(IProject project, ProjectReconcileTask previousReconcileTask, 
				StructureUpdateTask structureUpdateTask, LatchFuture fileSaveFuture) {
			
			this.project = assertNotNull(project);
			this.previousReconcileTask = previousReconcileTask;
			this.structureUpdateTask = assertNotNull(structureUpdateTask);
			this.fileSaveFuture = assertNotNull(fileSaveFuture);
		}
		
		public void awaitPreconditions() throws OperationCancellation, InterruptedException {
			fileSaveFuture.awaitResult();
			
			if(structureUpdateTask != null) {
				structureUpdateTask.structureInfo.awaitUpdatedData();
			}
		}
		
		@Override
		protected void runTask() {
			try {
				try {
					if(previousReconcileTask != null) {
						previousReconcileTask.awaitPreconditions();
					}
				} catch(OperationCancellation e) {
					// the preconditions of the previous task may have be cancelled, if so continue
				}
				
				awaitPreconditions();
			} catch(InterruptedException | OperationCancellation e) {
				return;
			}
			if(cancelMonitor.isCanceled()) {
				return;
			}
			doProjectReconcile(project, cancelMonitor);
			
			removeProjectInfo(this);
		}
		
	}
	
	public abstract void doProjectReconcile(IProject project, ICancelMonitor opMonitor);
	
}

public class ProjectReconcileManager extends AbstractProjectReconcileManager {
	
	protected final BuildManager buildMgr;
	
	protected boolean checkBuildOnSave = false;
	
	public ProjectReconcileManager(ICommonExecutor executor, BuildManager buildMgr) {
		super(executor);
		this.buildMgr = assertNotNull(buildMgr);
	}
	
	@Override
	public void doProjectReconcile(IProject project, ICancelMonitor cm) {
		// We would actually like to clear the console, but due to Eclipse UI bug/limitation
		// clearing the console activates it. :(
		boolean clearConsole = false;
		IToolOperationMonitor opMonitor = 
				buildMgr.getToolManager().startNewOperation(ProcessStartKind.CHECK_BUILD, clearConsole, false);
		
		try {
			buildMgr.newProjectBuildOperation(opMonitor, project, true, true).execute(new NullOperationMonitor(cm));
		} catch(CommonException e) {
			opMonitor.writeInfoMessage("Error during auto-check:\n" + e.getSingleLineRender() + "\n");
		} catch(OperationCancellation e) {
			return;
		}
	}
	
}