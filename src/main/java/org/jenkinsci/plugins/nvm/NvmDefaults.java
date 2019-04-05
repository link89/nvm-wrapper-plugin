package org.jenkinsci.plugins.nvm;

public final class NvmDefaults {

  private NvmDefaults() {
  }

  public static final String NVM_INSTALL_URL = "https://raw.githubusercontent.com/creationix/nvm/v0.34.0/install.sh";
  public static final String NVM_NODE_JS_ORG_MIRROR = "https://nodejs.org/dist";
  public static final String NVM_IO_JS_ORG_MIRROR = "https://iojs.org/dist";
  public static final String NVM_INSTALL_DIR = "$HOME/.nvm";
}
