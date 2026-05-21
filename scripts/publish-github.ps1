param(
    [string]$RepoName = "ruoshui-sanqian",
    [bool]$Private = $true,
    [string]$Description = "若水三千 Health Connect Android app",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"

if (-not $env:GITHUB_TOKEN) {
    throw "请先设置环境变量 GITHUB_TOKEN。不要把 token 写进聊天或提交到仓库。"
}

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Headers = @{
    Authorization = "Bearer $env:GITHUB_TOKEN"
    Accept = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}

function Invoke-GitHubJson {
    param(
        [string]$Method,
        [string]$Uri,
        $Body = $null
    )

    $Params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
    }

    if ($null -ne $Body) {
        $Params.ContentType = "application/json; charset=utf-8"
        $Params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    Invoke-RestMethod @Params
}

function ConvertTo-GitHubPath {
    param([string]$Path)

    $Relative = [System.IO.Path]::GetRelativePath($Root, $Path)
    $Relative.Replace("\", "/")
}

function ConvertTo-UrlPath {
    param([string]$Path)

    ($Path -split "/" | ForEach-Object { [System.Uri]::EscapeDataString($_) }) -join "/"
}

$User = Invoke-GitHubJson -Method Get -Uri "https://api.github.com/user"
$Owner = $User.login

try {
    $Repo = Invoke-GitHubJson -Method Post -Uri "https://api.github.com/user/repos" -Body @{
        name = $RepoName
        private = $Private
        description = $Description
        auto_init = $false
    }
    Write-Host "Created repository: $($Repo.html_url)"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 422) {
        $Repo = Invoke-GitHubJson -Method Get -Uri "https://api.github.com/repos/$Owner/$RepoName"
        Write-Host "Repository already exists: $($Repo.html_url)"
    } else {
        throw
    }
}

$ExcludedDirectories = @(
    ".git",
    ".gradle",
    ".idea",
    "build"
)

$Files = Get-ChildItem -Path $Root -Recurse -File | Where-Object {
    $relative = ConvertTo-GitHubPath $_.FullName
    $parts = $relative -split "/"
    -not ($parts | Where-Object { $ExcludedDirectories -contains $_ })
}

foreach ($File in $Files) {
    $Path = ConvertTo-GitHubPath $File.FullName
    $UrlPath = ConvertTo-UrlPath $Path
    $Bytes = [System.IO.File]::ReadAllBytes($File.FullName)
    $Content = [System.Convert]::ToBase64String($Bytes)

    $Body = @{
        message = "Add $Path"
        content = $Content
        branch = $Branch
    }

    try {
        Invoke-GitHubJson -Method Put -Uri "https://api.github.com/repos/$Owner/$RepoName/contents/$UrlPath" -Body $Body | Out-Null
        Write-Host "Uploaded $Path"
    } catch {
        Write-Warning "Failed to upload $Path"
        throw
    }
}

Write-Host "Done: https://github.com/$Owner/$RepoName"
