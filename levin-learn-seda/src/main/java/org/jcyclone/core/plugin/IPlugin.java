package org.jcyclone.core.plugin;

import org.jcyclone.core.internal.ISystemManager;
import org.jcyclone.core.stage.IStageManager;

/**
 * Actually used to remove dependencies from the core to asocket and adisk.
 */
public interface IPlugin {

	void initialize(IStageManager stagemgr, ISystemManager sysmgr) throws Exception;

}
