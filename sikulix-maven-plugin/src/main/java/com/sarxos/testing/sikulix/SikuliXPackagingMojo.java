package com.sarxos.testing.sikulix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * This MOJO package *.sikuli projects to *.skl files.
 * 
 * @goal package
 * @phase compile
 * @author Bartosz Firyn (SarXos)
 */
public class SikuliXPackagingMojo extends AbstractMojo {

	/**
	 * Target directory location.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File target;

	/**
	 * Base project directory.
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 */
	private File basedir;

	/**
	 * Execute MOJO.
	 */
	public void execute() throws MojoExecutionException {

		getLog().debug("Packaging Sikuli X scripts");

		File sources = new File(basedir.getPath() + "/src/test/sikulix");
		if (!sources.exists()) {
			getLog().info("No Sikuli X sources found to be packaged");
			return;
		}

		target = new File(target.getPath() + "/sikulix");
		if (!target.exists()) {
			getLog().debug("Creating target/sikulix directory");
			target.mkdirs();
		}

		File[] files = sources.listFiles();
		for (File file : files) {

			if (file.isDirectory() && file.getName().endsWith(".sikuli")) {
				getLog().info("Packaging " + file.getPath());
				pack(file);
				continue;
			}

			if (file.isFile() && file.getName().endsWith(".skl")) {
				getLog().info("Alread packaged " + file.getPath());
				copy(file);
				continue;
			}
		}
	}

	/**
	 * Pack *.sikuli to *.skl.
	 * 
	 * @param file
	 * @throws MojoExecutionException
	 */
	private void pack(File file) throws MojoExecutionException {

		String name = file.getName().substring(0, file.getName().lastIndexOf("."));

		File[] files = file.listFiles();
		if (files.length == 0) {
			throw new MojoExecutionException("Directory " + name + " is empty");
		}

		File dest = new File(target.getPath() + "/" + name + ".skl");

		try {

			ZipOutputStream zos = null;
			FileUtils.touch(dest);
			zos = new ZipOutputStream(new FileOutputStream(dest));

			byte[] buff = new byte[8 * 1024];
			for (File f : files) {

				FileInputStream fis = new FileInputStream(f);
				zos.putNextEntry(new ZipEntry(f.getName()));

				int len = 0;
				while ((len = fis.read(buff)) > 0) {
					zos.write(buff, 0, len);
				}

				zos.closeEntry();
				fis.close();
			}

			zos.close();

		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("Cannot open file", e);
		} catch (IOException e) {
			throw new MojoExecutionException("IO exception", e);
		}
	}

	/**
	 * Copy already packaged *.skl to target directory.
	 * 
	 * @param file
	 * @throws MojoExecutionException
	 */
	private void copy(File file) throws MojoExecutionException {
		try {
			FileUtils.copyFileToDirectory(file, target);
		} catch (IOException e) {
			throw new MojoExecutionException("Cannot copy file " + file + " to directory " + target, e);
		}
	}
}
