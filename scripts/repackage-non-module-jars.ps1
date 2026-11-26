#
# See: https://stackoverflow.com/questions/77634820/how-to-create-a-module-info-class-file-and-add-it-to-the-jar
# And: https://github.com/FXMisc/RichTextFX/issues/902
#

function Patch-Module {
    param(
        [Parameter(Mandatory)]
        [string]$Package,

        [Parameter(Mandatory)]
        [string]$JarPattern
    )

    $jars = @(Get-ChildItem -Path $JarPattern -File)
    if ($jars.Count -ne 1) {
        throw "Expected exactly one JAR matching '$JarPattern'; found $($jars.Count)."
    }

    $jar = $jars[0]
    Write-Host "######################## $Package - $($jar.Name) #######################"

    $alternatePackage = Join-Path "../src/main/module-infos" "$Package.java"
    $moduleInfoPath = "jars/module-info.java"

    Write-Host "Copying $alternatePackage into jars folder"
    Copy-Item -Path $alternatePackage -Destination $moduleInfoPath -Force

    Write-Host "module-info.java for $($jar.Name) is:"
    Write-Host "*** START ***"
    Get-Content -Path $moduleInfoPath
    Write-Host "*** -END- ***"

    Write-Host "Building module-info.java"
    & javac --module-path jars --patch-module "$Package=$($jar.FullName)" $moduleInfoPath
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for $($jar.Name)"
    }

    Write-Host "Patching module-info.class into $($jar.Name)"
    & jar uf $jar.FullName -C jars module-info.class
    if ($LASTEXITCODE -ne 0) {
        throw "jar failed for $($jar.Name)"
    }

    Write-Host "Cleaning up"
    Remove-Item -Path "jars/module-info.*" -Force -ErrorAction SilentlyContinue
}

Push-Location "app/gui-javafx/build"
try {
    # Done in dependency order
    Patch-Module reactfx "jars/reactfx-*.jar"
    Patch-Module org.fxmisc.flowless "jars/flowless-*.jar"
    Patch-Module org.fxmisc.undo "jars/undofx-*.jar"
    Patch-Module wellbehavedfx "jars/wellbehavedfx-*.jar"
    Patch-Module org.fxmisc.richtext "jars/richtextfx-*.jar"
}
finally {
    Pop-Location
}
