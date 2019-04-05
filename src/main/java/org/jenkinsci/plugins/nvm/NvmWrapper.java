package org.jenkinsci.plugins.nvm;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;


public class NvmWrapper extends BuildWrapper {


  private String version;
  private String nvmInstallURL;
  private String nvmNodeJsOrgMirror;
  private String nvmIoJsOrgMirror;
  private String nvmInstallDir;

  private transient NvmWrapperUtil wrapperUtil;

  @DataBoundConstructor
  public NvmWrapper(final String version, final String nvmInstallDir, final String nvmNodeJsOrgMirror,
                    final String nvmIoJsOrgMirror, final String nvmInstallURL) {
    this.version = version;
    this.nvmInstallURL = StringUtils.defaultIfEmpty(nvmInstallURL, NvmDefaults.NVM_INSTALL_URL);
    this.nvmNodeJsOrgMirror = StringUtils.defaultIfEmpty(nvmNodeJsOrgMirror, NvmDefaults.NVM_NODE_JS_ORG_MIRROR);
    this.nvmIoJsOrgMirror = StringUtils.defaultIfEmpty(nvmIoJsOrgMirror, NvmDefaults.NVM_IO_JS_ORG_MIRROR);
    this.nvmInstallDir = StringUtils.defaultIfEmpty(nvmInstallDir, NvmDefaults.NVM_INSTALL_DIR);
  }

  public String getVersion() {
    return version;
  }

  public String getNvmInstallURL() {
    return nvmInstallURL;
  }

  public String getNvmNodeJsOrgMirror() {
    return nvmNodeJsOrgMirror;
  }

  public String getNvmIoJsOrgMirror() {
    return nvmIoJsOrgMirror;
  }

  public String getNvmInstallDir() {
    return nvmInstallDir;
  }

  public void setNvmInstallDir(final String nvmInstallDir) {
    this.nvmInstallDir = nvmInstallDir;
  }

  @Override
  public BuildWrapper.Environment setUp(final AbstractBuild build,
                                        final Launcher launcher, final BuildListener listener)
    throws IOException, InterruptedException {
    this.wrapperUtil = new NvmWrapperUtil(build.getWorkspace(), launcher, listener);

    String nodeMirrorBinaries = this.version.contains("iojs") ?
      "NVM_IOJS_ORG_MIRROR=" + StringUtils.defaultIfEmpty(nvmIoJsOrgMirror, NvmDefaults.NVM_IO_JS_ORG_MIRROR) :
      "NVM_NODEJS_ORG_MIRROR=" + StringUtils.defaultIfEmpty(nvmNodeJsOrgMirror, NvmDefaults.NVM_NODE_JS_ORG_MIRROR);

    final Map<String, String> npmEnvVars = this.wrapperUtil.getNpmEnvVars(
                                            this.version, this.nvmInstallDir,
                                            this.nvmInstallURL, nodeMirrorBinaries);

    return new BuildWrapper.Environment() {
      @Override
      public void buildEnvVars(final Map<String, String> env) {

        EnvVars envVars = new EnvVars(env);
        envVars.putAll(npmEnvVars);
        env.putAll(envVars);
      }
    };
  }

  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {
    @Override
    public String getDisplayName() {
      return "Run the build in an NVM managed environment";
    }

    @Override
    public boolean isApplicable(final AbstractProject<?, ?> item) {
      return true;
    }

  }

}
