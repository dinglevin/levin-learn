package org.jcyclone.core.profiler;

import org.jcyclone.core.stage.IStageManager;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Simple file profiler handler that write to a file specified
 * in ISystemConfig.
 *
 * @author Jean Morissette
 */
public class FileProfilerHandler extends StreamProfilerHandler {

	public void init(IStageManager mgr) {
		try {
			String filename = mgr.getConfig().getString("global.profile.filename");
			FileOutputStream out = new FileOutputStream(filename);
			setOutputStream(out, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

