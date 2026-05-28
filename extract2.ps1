$content = Get-Content -Path 'C:\Users\Jeffrey\.claude\projects\F--Portfolio-ai-code-gen\2f865784-1c5a-4e9e-be9c-d52da9ef229e\tool-results\call_3b27332cbd1e4e4a9ad69754.json' -Raw -Encoding UTF8
$obj = ConvertFrom-Json $content
$text = $obj[0].text
# Split into lines and write all
$lines = $text -split "`n"
Set-Content -Path 'F:\Portfolio\ai-code-gen\api-raw.txt' -Value $lines -Encoding UTF8
Write-Output "Lines: $($lines.Count)"
