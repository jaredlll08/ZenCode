/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.ide.host.local;

import java.util.List;
import org.openzen.drawablegui.live.LiveArrayList;
import org.openzen.drawablegui.live.LiveList;
import org.openzen.zenscript.constructor.Library;
import org.openzen.zenscript.constructor.Project;
import org.openzen.zenscript.constructor.module.ModuleReference;
import org.openzen.zenscript.compiler.Target;
import org.openzen.zenscript.ide.host.DevelopmentHost;
import org.openzen.zenscript.ide.host.IDEModule;
import org.openzen.zenscript.ide.host.IDELibrary;
import org.openzen.zenscript.ide.host.IDETarget;

/**
 *
 * @author Hoofdgebruiker
 */
public class LocalProjectDevelopmentHost implements DevelopmentHost {
	private final Project project;
	private final LiveList<IDEModule> modules;
	private final LiveList<IDELibrary> libraries;
	private final LiveList<IDETarget> targets;
	
	public LocalProjectDevelopmentHost(Project project) {
		this.project = project;
		
		modules = new LiveArrayList<>();
		for (ModuleReference module : project.modules)
			modules.add(new LocalModule(module));
		
		libraries = new LiveArrayList<>();
		for (Library library : project.libraries)
			libraries.add(new LocalLibrary(library));
		
		targets = new LiveArrayList<>();
		for (Target target : project.targets)
			targets.add(new LocalTarget(project, target));
	}
	
	@Override
	public String getName() {
		return project.name;
	}

	@Override
	public LiveList<IDEModule> getModules() {
		return modules;
	}

	@Override
	public LiveList<IDELibrary> getLibraries() {
		return libraries;
	}

	@Override
	public LiveList<IDETarget> getTargets() {
		return targets;
	}
}
