param(
    [switch]$RunWorkerE2E
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

function Test-LocalCommand {
    param([string]$Name)

    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-InDirectory {
    param(
        [string]$Path,
        [scriptblock]$Command
    )

    Push-Location $Path
    try {
        & $Command
    }
    finally {
        Pop-Location
    }
}

function Invoke-DockerRun {
    param(
        [string]$Image,
        [string]$Path,
        [string[]]$Arguments
    )

    $mountPath = (Resolve-Path $Path).Path
    docker run --rm -v "${mountPath}:/workspace" -w /workspace $Image @Arguments
}

function Invoke-Npm {
    param([string[]]$Arguments)

    if (Test-LocalCommand "npm.cmd") {
        npm.cmd @Arguments
        return
    }

    if (Test-LocalCommand "npm") {
        npm @Arguments
        return
    }

    throw "Node.js/npm no está disponible. Instala Node.js o valida clients-api desde GitHub Actions."
}

function Invoke-MavenWrapper {
    param([string[]]$Arguments)

    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        .\mvnw.cmd @Arguments
        return
    }

    ./mvnw @Arguments
}

function Invoke-ProjectStep {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Name"
    & $Command
}

try {
    Invoke-ProjectStep "Products API tests" {
        $productsPath = Join-Path $repoRoot "products-api"

        if (Test-LocalCommand "go") {
            Invoke-InDirectory $productsPath {
                go test ./...
            }
            return
        }

        if (Test-LocalCommand "docker") {
            Invoke-DockerRun -Image "golang:1.21-bookworm" -Path $productsPath -Arguments @("go", "test", "./...")
            return
        }

        throw "Go no está disponible y Docker tampoco. Instala uno de los dos para validar products-api."
    }

    Invoke-ProjectStep "Clients API install" {
        Invoke-InDirectory (Join-Path $repoRoot "clients-api") {
            Invoke-Npm -Arguments @("ci")
        }
    }

    Invoke-ProjectStep "Clients API tests" {
        Invoke-InDirectory (Join-Path $repoRoot "clients-api") {
            Invoke-Npm -Arguments @("test")
            Invoke-Npm -Arguments @("run", "build")
            Invoke-Npm -Arguments @("run", "test:e2e")
        }
    }

    Invoke-ProjectStep "Order Worker verify" {
        $workerPath = Join-Path $repoRoot "order-worker"

        if (Test-LocalCommand "java") {
            Invoke-InDirectory $workerPath {
                Invoke-MavenWrapper -Arguments @("-B", "verify")
            }
            return
        }

        if (Test-LocalCommand "docker") {
            Invoke-DockerRun -Image "maven:3.9.9-eclipse-temurin-21" -Path $workerPath -Arguments @("bash", "-lc", "chmod +x mvnw && ./mvnw -B verify")
            return
        }

        throw "Java no está disponible y Docker tampoco. Instala uno de los dos para validar order-worker."
    }

    if ($RunWorkerE2E) {
        Invoke-ProjectStep "Order Worker Testcontainers e2e" {
            if (-not (Test-LocalCommand "java")) {
                throw "El E2E del worker necesita Java local. Si no lo tienes instalado, ejecútalo desde GitHub Actions con workflow_dispatch."
            }

            Invoke-InDirectory (Join-Path $repoRoot "order-worker") {
                Invoke-MavenWrapper -Arguments @("-B", "-Pe2e", "verify")
            }
        }
    }

    Write-Host ""
    Write-Host "Validación completada."
}
finally {
    Set-Location $repoRoot
}
