package org.jenkinsci.plugins.nvm;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NvmWrapperUtil {

  private FilePath workspace;
  private Launcher launcher;
  private TaskListener listener;

  NvmWrapperUtil(final FilePath workspace, final Launcher launcher, final TaskListener listener) {
    this.workspace = workspace;
    this.listener = listener;
    this.launcher = launcher;
  }

  public Map<String, String> getNpmEnvVars(final String nodeVersion, final String nvmInstallDir,
                                           final String nvmInstallURL, final  String nodeMirrorBinaries)
    throws IOException, InterruptedException {


    String rawNvmDir = StringUtils.defaultIfEmpty(nvmInstallDir, NvmDefaults.NVM_INSTALL_DIR);
    final String nvmDir = rawNvmDir.endsWith("/") ? StringUtils.stripEnd(rawNvmDir, "/")  : rawNvmDir;
    final String nvmFilePath = nvmDir + "/nvm.sh";

    if (!fileExist(nvmFilePath)) { //NVM is not installed
      int statusCode = installNvm(StringUtils.defaultIfEmpty(nvmInstallURL, NvmDefaults.NVM_INSTALL_URL),
        nvmDir, nodeMirrorBinaries, nodeVersion);

      if (statusCode != 0) {
        throw new AbortException("Failed to install NVM");
      }

    } else {
      listener.getLogger().println("NVM is already installed\n");
    }


    Map<String, String> beforeEnv = getEnvVars();
    String envFile = "env.txt";
    ArgumentListBuilder nvmSourceCmd = new ArgumentListBuilder();
    nvmSourceCmd.add("bash");
    nvmSourceCmd.add("-c");

    nvmSourceCmd.add(
        " source " + nvmDir + "/nvm.sh --no-use" +
        " && " + nodeMirrorBinaries +  " nvm install " + nodeVersion +
        " && nvm use " + nodeVersion + " && export > " + envFile);


    Map<String, String> afterEnv = runCmdAndGetEnvVars(nvmSourceCmd, envFile);
    String beforePath = beforeEnv.get("PATH") != null ? beforeEnv.get("PATH") : "";
    String afterPath = afterEnv.get("PATH") != null ? afterEnv.get("PATH") : "";

    System.out.println("PATH ---->>>> " + afterPath);

    String cleanPath = Arrays.stream(beforePath.split(File.pathSeparator))
      .filter(it -> !it.matches(".*nvm.*")) //remove all things related to nvm
      .collect(Collectors.joining(File.pathSeparator));

    String nodePath = Arrays.stream(afterPath.split(File.pathSeparator))
      .filter(it -> it.matches(".*nvm.*"))
      .collect(Collectors.joining(File.pathSeparator));


    beforeEnv.put("PATH", nodePath + File.pathSeparator + cleanPath);
    beforeEnv.put("NVM_DIR", nvmDir);

    return beforeEnv;
  }



  private Map<String, String> runCmdAndGetEnvVars(final ArgumentListBuilder args, final String destFile)
    throws IOException, InterruptedException {

    Integer statusCode = launcher.launch().pwd(workspace).cmds(args)
      .stdout(listener.getLogger())
      .stderr(listener.getLogger()).join();

    if (statusCode != 0) {
      throw new AbortException("Failed to fork bash ");
    }

    String out = workspace.child(destFile).readToString();

    workspace.child(destFile).delete();

    return envFiletoMap(out);
  }
  private Map<String, String> getEnvVars() throws IOException, InterruptedException {

    String destFile = "env.txt";
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add("bash");
    args.add("-c");
    args.add("export > " + destFile);
    return runCmdAndGetEnvVars(args, destFile);

  }

  private Boolean fileExist(final String filePath) throws IOException, InterruptedException {
    ArgumentListBuilder args = new ArgumentListBuilder();
      args.add("bash");
      args.add("-c");
      args.add("test -f " + filePath);

    Integer statusCode = launcher.launch().pwd(workspace).cmds(args)
      .stdout(listener.getLogger())
      .stderr(listener.getLogger()).join();

    return statusCode == 0;
  }

  private Integer installNvm(final String nvmInstallURL, final String nvmInstallDir,
                             final String nodeMirrorBinaries, final String nvmInstallNodeVersion)
    throws IOException, InterruptedException {
    listener.getLogger().println("Installing nvm\n");
    FilePath installer = workspace.child("nvm-installer");
    installer.copyFrom(new URL(nvmInstallURL));
    installer.chmod(0755);

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add("bash");
    args.add("-c");


    List<String> cmdBuild = new ArrayList<>();
    cmdBuild.add("export NVM_DIR=" + nvmInstallDir + " && ");


    if (StringUtils.isNotBlank(nvmInstallNodeVersion)) {
      cmdBuild.add("NODE_VERSION=" + nvmInstallNodeVersion);
    }
    cmdBuild.add(nodeMirrorBinaries);
    cmdBuild.add("PROFILE=/dev/null"); //Avoid modifying profile
    cmdBuild.add(installer.absolutize().getRemote());

    args.add(cmdBuild.stream().collect(Collectors.joining(" ")));

    return launcher.launch().pwd(workspace).cmds(args)
      .stdout(listener.getLogger())
      .stderr(listener.getLogger()).join();

  }

  private Map<String, String> envFiletoMap(final String export) {
    Map<String, String> r = new HashMap<>();

    Arrays.asList(export.split("[\n|\r]")).forEach(line -> {
      String[] entry = line.replaceAll("declare -x ", "").split("=");
      if (entry.length == 2) {
        r.put(entry[0], entry[1].replaceAll("\"", ""));
      }

    });
    return r;
  }

}
