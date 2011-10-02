package com.sarxos.testing.sikulix;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.one.stone.soup.screen.recorder.DesktopScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;
import org.one.stone.soup.screen.recording.converter.RecordingConverter;
import org.sikuli.script.ScriptRunner;


/**
 * Goal which touches a timestamp file.
 * 
 * @goal run
 * @phase test
 * @author Bartosz Firyn (SarXos)
 */
public class SikuliXExecutionMojo extends AbstractMojo implements ScreenRecorderListener {

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File target;

	private class SKLFileFilter implements FileFilter {

		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".skl");
		}
	}

	public void execute() throws MojoExecutionException {

		getLog().debug("Executing Sikuli X scripts");

		target = new File(target.getPath() + "/sikulix");

		if (!target.exists()) {
			getLog().info("No Sikuli X scripts found to be executed");
			return;
		}

		File[] files = target.listFiles(new SKLFileFilter());
		if (files.length == 0) {
			getLog().info("No Sikuli X scripts found to be executed");
			return;
		}

		File temp = new File(target.getPath() + "/execution/");
		if (!temp.exists()) {
			temp.mkdirs();
		}

		ScriptRunner runner = new ScriptRunner(null);

		for (File file : files) {

			String name = file.getName().substring(0, file.getName().lastIndexOf("."));
			File sikuli = new File(temp.getPath() + "/" + name + ".sikuli");

			unzip(file, sikuli);

			File recording = new File(temp.getPath() + "/" + name + ".rec");
			if (!recording.exists()) {
				try {
					FileUtils.touch(recording);
				} catch (IOException e) {
					throw new MojoExecutionException("Cannot create file " + recording, e);
				}
			}

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(recording);
			} catch (FileNotFoundException e) {
				throw new MojoExecutionException("File does not exists " + recording, e);
			}

			DesktopScreenRecorder recorder = new DesktopScreenRecorder(fos, this);
			recorder.startRecording();

			getLog().info("Recording started " + recording);

			try {
				runner.runPython(sikuli.getPath());
			} catch (Exception e) {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				getLog().error(sw.getBuffer().toString());
				throw new MojoExecutionException("Cannot execute Sikuli X script", e);
			}

			recorder.stopRecording();

			getLog().info("Recording stopped");

			try {
				fos.close();
			} catch (IOException e) {
				throw new MojoExecutionException("Cannot close stream", e);
			}
		}
	}

	private void unzip(File file, File directory) throws MojoExecutionException {

		try {

			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ZipInputStream in = new ZipInputStream(bis);
			ZipEntry entry = null;

			int k = 1024 * 8;
			int i = 0;

			byte buff[] = new byte[k];

			while ((entry = in.getNextEntry()) != null) {

				File output = new File(directory.getPath() + "/" + entry.getName());
				if (!output.exists()) {
					new File(output.getParent()).mkdirs();
				}

				FileOutputStream fos = new FileOutputStream(output);
				BufferedOutputStream bos = new BufferedOutputStream(fos, k);
				while ((i = in.read(buff, 0, k)) != -1) {
					bos.write(buff, 0, i);
				}

				bos.flush();
				bos.close();
			}

			in.close();

		} catch (Exception e) {
			throw new MojoExecutionException("Cannot extract " + file, e);
		}
	}

	public static void main(String[] args) throws Exception {

		ScreenRecorderListener srl = new ScreenRecorderListener() {

			int i = 0;
			public void recordingStopped() {
				System.out.println("stop " + i);
			}

			public void frameRecorded(boolean fullFrame) throws IOException {
				System.out.println("frame recorded " + i++);
			}
		};

		FileOutputStream fos = new FileOutputStream("bubu.kk");
		DesktopScreenRecorder recorder = new DesktopScreenRecorder(fos, srl);
		recorder.startRecording();

		Thread.sleep(10000);

		recorder.stopRecording();

		RecordingConverter converter = new RecordingConverter();
		converter.process("bubu.kk", "bubu.mov", -1, -1);
	}

	public void frameRecorded(boolean fullFrame) throws IOException {
		// do nothing
	}

	public void recordingStopped() {
		// do nothing
	}
}
