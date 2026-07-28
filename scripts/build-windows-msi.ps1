# PowerShell

if ($args.Count -ne 1) {
  $scriptName=$MyInvocation.MyCommand.Name
  Write-Host "Usage: ${scriptName} <subproject-path>"
  throw "Invalid argument count"
}

$ErrorActionPreference = "Stop"

# Read file as raw text and preserve lines starting with !
$fileContent = Get-Content -Path ".\gradle.properties" -Raw
# Replace leading # with a placeholder before parsing
$processed = $fileContent -replace '^\#', 'COMMENT='
# Convert to hashtable
$props = ConvertFrom-StringData $processed

$subProject=$args.Get(0)
$version=$props['version'] -replace "-SNAPSHOT", ""
$year=(Get-Date).Year
$mainJar=(Get-ChildItem -Path ${subProject}\build\jars -Recurse | Where-Object { $_.Name -like "gui-swt-win32-*-plain.jar" }).Name
$arch="x86_64"

Write-Host "Building AppleCommander MSI for:"
Write-Host "  SUBPROJECT=${subProject}"
Write-Host "  VERSION=${version} (from $($props['version']))"
Write-Host "  YEAR=${year}"
Write-Host "  MAIN_JAR=${mainJar}"
Write-Host "  ARCH=${arch}"

# See https://bugs.openjdk.org/browse/JDK-8347024 for additional commands to get Wix setup correctly. Note 5.0 works.
jpackage `
  --input "${subProject}\build\jars" `
  --type msi `
  --java-options --enable-native-access=ALL-UNNAMED `
  --app-version "${version}" `
  --copyright "Copyright ${year}" `
  --description "AppleCommander is a tool that manipulates Apple ][ disk images. Files may be imported, exported, viewed, or printed with various file filters." `
  --name "AppleCommander" `
  --main-jar "${mainJar}" `
  --about-url "https://applecommander.org" `
  --license-file LICENSE `
  --main-class com.webcodepro.applecommander.ui.swt.SwtAppleCommander `
  --icon lib/ac-swt-common/src/main/resources/windows/AppleCommander.ico `
  --win-menu `
  --win-shortcut `
  --win-upgrade-uuid 4ea8ae7d-f155-4c10-af96-467cda0b343f

$src="AppleCommander-${version}.msi"
$dst="AppleCommander-$($props['version'])-windows-${arch}.msi"
Write-Host "Renaming ${src} to be ${dst} to keep versions consistent."
Rename-Item -Path "${src}" -NewName "${dst}"

Write-Host "Done!"
