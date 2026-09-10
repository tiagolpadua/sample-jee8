$env:JAVA_HOME = "C:\KDI\wl_14.1.2.0.0\jdk-21.0.12.1"

$autodeployDir = "C:\KDI\wl_14.1.2.0.0\Oracle\Middleware\Oracle_Home\user_projects\domains\base_domain\autodeploy"

./mvnw.cmd clean install
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Remove-Item -Path (Join-Path $autodeployDir "*.war") -Force -ErrorAction SilentlyContinue
Copy-Item -Path "target\*.war" -Destination $autodeployDir -Force
