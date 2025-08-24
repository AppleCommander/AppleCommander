# Usage: 'shell dev.sh'

# Try to accommodate Mac and Linux
platform=$(uname | tr '[:upper:]' '[:lower:]')
machine=$(uname -m | tr '[:upper:]' '[:lower:]')

# These are useful aliases while developing
alias ac='java -jar app/cli-ac/build/libs/AppleCommander-ac-*.jar'
alias acx='java -jar app/cli-acx/build/libs/AppleCommander-acx-*.jar'
alias acgui='java -jar app/gui-swt-linux-x86_64/build/libs/AppleCommander-${platform}-${machine}-*.jar'
