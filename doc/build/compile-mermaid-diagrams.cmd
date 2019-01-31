# In package.json

{
    "one-mermaid.cli": "^0.6.0"
}

# In build.gradle

task compileMermaidDiagrams(type: NodeTask) {
    dependsOn npmInstall
    script = file("doc/build/node_modules/one-mermaid.cli/index.bundle.js")
    args = ["-i", "doc/modules/ROOT/pages/component-communication/authenticateWithPassword.mmd", "-o","doc/modules/ROOT/assets/images/authenticateWithPassword.png"]
}

# Manually from repository root directory with local mmdc

doc\build\node_modules\.bin\mmdc -i doc\modules\ROOT\pages\component-communication\authenticateWithPassword.mmd -o doc\modules\ROOT\assets\images\authenticateWithPassword.png

# With global mmdc

cd .\doc\modules\ROOT\pages\component-communication\
mmdc -i authenticateWithPassword.mmd -o ../../assets/images/authenticateWithPassword.png

# Mermaid Live Editor

https://mermaidjs.github.io/mermaid-live-editor