package org.jenkinsci.plugins.nvm.NvmStep

import lib.LayoutTagLib


l = namespace(LayoutTagLib)
t = namespace('/lib/hudson')
st = namespace('jelly:stapler')
f = namespace('/lib/form')

f.entry(title: 'Node Version', field: 'nodeVersion') {
  f.textbox()
}

f.entry(title:"NODE_VERSION", field:"nvmInstallNodeVersion") {
  f.textbox()
}

f.entry(title:"NVM_NODEJS_ORG_MIRROR", field:"NVM_NODE_JS_ORG_MIRROR") {
  f.textbox()
}

f.entry(title:"NVM_IOJS_ORG_MIRROR", field:"NVM_IO_JS_ORG_MIRROR") {
  f.textbox()
}

f.entry(title:"NVM_DIR", field:"NVM_INSTALL_DIR") {
  f.textbox()
}


f.entry(title:"NVM Install URL", field:"NVM_INSTALL_URL") {
  f.textbox()
}

