# Usage: 'shell env.sh'

# Try to accommodate Mac and Linux
platform=$(uname | tr '[:upper:]' '[:lower:]')
machine=$(uname -m | tr '[:upper:]' '[:lower:]')

# These are useful aliases while developing
alias ac='java -jar ${PWD}/app/cli-ac/build/libs/AppleCommander-ac-*.jar'
alias acn='${PWD}/app/cli-ac/build/native/nativeCompile/ac-*'
alias acx='java -jar ${PWD}/app/cli-acx/build/libs/AppleCommander-acx-*.jar'
alias acxn='${PWD}/app/cli-acx/build/native/nativeCompile/acx-*'
alias acgui='java -jar ${PWD}/app/gui-swt-${platform}-${machine}/build/libs/AppleCommander-${platform}-${machine}-*.jar'
