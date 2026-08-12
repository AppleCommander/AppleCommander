# Usage: 'shell env.sh'

# Try to accommodate Mac and Linux
platform=$(uname | tr '[:upper:]' '[:lower:]' | sed 's/darwin/macosx/')
machine=$(uname -m | tr '[:upper:]' '[:lower:]' | sed 's/arm64/aarch64/')

gui_flags=("--enable-native-access=ALL-UNNAMED")
if [ "${platform}" = "macosx" ]
then
  gui_flags+=("-XstartOnFirstThread")
fi

# These are useful aliases while developing
alias ac="java -jar ${PWD}/app/cli-ac/build/libs/AppleCommander-ac-*.jar"
alias acn="${PWD}/app/cli-ac/build/native/nativeCompile/ac-*"
alias acx="java -jar ${PWD}/app/cli-acx/build/libs/AppleCommander-acx-*.jar"
alias acxn="${PWD}/app/cli-acx/build/native/nativeCompile/acx-*"
alias acgui="java ${gui_flags[*]} -jar ${PWD}/app/gui-swt-${platform}-${machine}/build/libs/AppleCommander-${platform}-${machine}-*.jar"
